package com.sbm.aoi.ui.storage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.storage.BackupService
import com.sbm.aoi.storage.ItemEntity
import com.sbm.aoi.storage.QrCodeEntity
import com.sbm.aoi.storage.RoomEntity
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

    val uiState: StateFlow<StorageUiState> = combine(
        arrayOf(
            repository.settings,
            repository.rooms,
            repository.items,
            repository.codes,
            query,
            searchResult,
            status,
        ),
    ) { values ->
        StorageUiState(
            settings = values[0] as UserSettingsEntity?,
            rooms = values[1] as List<RoomEntity>,
            items = values[2] as List<ItemEntity>,
            codes = values[3] as List<QrCodeEntity>,
            query = values[4] as String,
            searchResult = values[5] as SearchResult,
            status = values[6] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageUiState())

    fun onNameEntered(name: String) = viewModelScope.launch { repository.ensureSettings(name) }

    fun updateSettings(name: String, prefix: String) = viewModelScope.launch {
        repository.updateSettings(name, prefix)
        status.value = "Настройки сохранены"
    }

    fun addRoom(name: String) = viewModelScope.launch { repository.addRoom(name, uiState.value.rooms.size) }

    fun addItem(name: String) = viewModelScope.launch { repository.addItem(name) }

    fun generateItemQr(itemId: String) = viewModelScope.launch {
        val qr = repository.bindQr("ITEM", itemId)
        status.value = "Создан код ${qr.codeIdString}"
    }

    fun search(input: String) = viewModelScope.launch {
        query.value = input
        searchResult.value = if (input.isBlank()) SearchResult(emptyList(), emptyList(), emptyList()) else repository.search(input)
    }

    fun createBatchCodes(count: Int) = viewModelScope.launch {
        val codes = repository.createBatchFreeCodes(count)
        status.value = "Создано свободных кодов: ${codes.size}"
    }

    fun scanPayload(payload: String) = viewModelScope.launch {
        val result = repository.scan(payload)
        status.value = result?.let { "Скан: найден ${it.codeIdString} (${it.entityType})" } ?: "Неизвестный код"
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

data class StorageUiState(
    val settings: UserSettingsEntity? = null,
    val rooms: List<RoomEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val codes: List<QrCodeEntity> = emptyList(),
    val query: String = "",
    val searchResult: SearchResult = SearchResult(emptyList(), emptyList(), emptyList()),
    val status: String = "",
)
