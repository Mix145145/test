package com.sbm.aoi.ui.storage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.storage.BackupService
import com.sbm.aoi.storage.ItemEntity
import com.sbm.aoi.storage.QrCodeEntity
import com.sbm.aoi.storage.RoomCreateRequest
import com.sbm.aoi.storage.RoomEntity
import com.sbm.aoi.storage.RoomUpdateRequest
import com.sbm.aoi.storage.ScanResult
import com.sbm.aoi.storage.SearchResult
import com.sbm.aoi.storage.StickerLabel
import com.sbm.aoi.storage.StickerService
import com.sbm.aoi.storage.StorageRepository
import com.sbm.aoi.storage.UserSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val repository: StorageRepository,
    private val stickerService: StickerService,
    private val backupService: BackupService,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val searchResult = MutableStateFlow(SearchResult(emptyList(), emptyList(), emptyList()))
    private val status = MutableStateFlow("")
    private val scanResult = MutableStateFlow(ScanResult())
    private val noteResults = MutableStateFlow<List<String>>(emptyList())

    val uiState: StateFlow<StorageUiState> = combine(
        combine(
            repository.settings,
            repository.rooms,
            repository.items,
            repository.codes,
        ) { settings, rooms, items, codes ->
            Quad(settings, rooms, items, codes)
        },
        combine(query, searchResult, status, scanResult, noteResults) { q, result, s, scan, notes ->
            Quint(q, result, s, scan, notes)
        },
    ) { core, ui ->
        StorageUiState(core.first, core.second, core.third, core.fourth, ui.first, ui.second, ui.third, ui.fourth, ui.fifth)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageUiState())

    fun onNameEntered(name: String) = viewModelScope.launch { repository.ensureSettings(name) }

    fun updateSettings(name: String, prefix: String) = viewModelScope.launch {
        repository.updateSettings(name, prefix)
        status.value = "Настройки сохранены"
    }

    fun addRoom(name: String, type: String, colorHex: String) = viewModelScope.launch {
        repository.addRoom(RoomCreateRequest(name, type, colorHex), uiState.value.rooms.size)
        status.value = "Комната добавлена"
    }

    fun saveRoomLayout(roomId: String, x: Float, y: Float, type: String, colorHex: String) = viewModelScope.launch {
        repository.updateRoomLayout(RoomUpdateRequest(roomId = roomId, x = x, y = y, type = type, colorHex = colorHex))
    }

    fun addItem(name: String, roomId: String?, storageType: String, noteText: String, photoUri: String? = null) = viewModelScope.launch {
        repository.addItem(name = name, roomId = roomId, noteText = "$storageType: $noteText", photoUri = photoUri)
        status.value = "Объект добавлен"
    }

    fun generateItemQr(itemId: String) = viewModelScope.launch {
        val qr = repository.bindQr("ITEM", itemId)
        status.value = "Создан код ${qr.codeIdString}"
    }

    fun search(input: String) = viewModelScope.launch {
        query.value = input
        searchResult.value = if (input.isBlank()) SearchResult(emptyList(), emptyList(), emptyList()) else repository.search(input)
    }

    fun createBatchCodes(count: Int) = viewModelScope.launch {
        val safeCount = count.coerceIn(1, 5)
        val codes = repository.createBatchFreeCodes(safeCount)
        status.value = "Создано кодов: ${codes.size}. Готово к сохранению/печати"
    }

    fun scanPayload(payload: String) = viewModelScope.launch {
        val result = repository.scan(payload)
        scanResult.value = result
        if (result.requiresBinding) {
            noteResults.value = emptyList()
            status.value = result.note
            return@launch
        }
        val qr = result.qr
        if (qr == null) {
            noteResults.value = emptyList()
            status.value = "Неизвестный код"
            return@launch
        }
        val notes = repository.notesByCode(qr).map { note ->
            val roomPart = note.room?.name?.let { "Комната: $it" } ?: "Комната не указана"
            val photoPart = if (note.item.photoUri.isNullOrBlank()) "без фото" else "с фото"
            "${note.item.name} • $roomPart • ${note.item.description.ifBlank { "без заметки" }} • $photoPart"
        }
        noteResults.value = notes
        status.value = "Скан: найден ${qr.codeIdString} (${qr.entityType})"
    }

    fun exportPdf(context: Context) = viewModelScope.launch {
        val labels = uiState.value.codes.take(100).map { StickerLabel(it.codeIdString, it.payloadString, it.entityType) }
        if (labels.isEmpty()) {
            status.value = "Нет кодов для печати"
            return@launch
        }
        val file = stickerService.buildPdf(context, labels, uiState.value.settings?.pdfPageType ?: "A4")
        status.value = "PDF сохранен: ${file.name}"
    }

    fun exportBackup(context: Context) = viewModelScope.launch {
        val file = backupService.exportZip(context)
        status.value = "Бэкап создан: ${file.name}"
    }
}


private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

data class StorageUiState(
    val settings: UserSettingsEntity? = null,
    val rooms: List<RoomEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val codes: List<QrCodeEntity> = emptyList(),
    val query: String = "",
    val searchResult: SearchResult = SearchResult(emptyList(), emptyList(), emptyList()),
    val status: String = "",
    val scanResult: ScanResult = ScanResult(),
    val noteResults: List<String> = emptyList(),
)
