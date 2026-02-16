package com.sbm.aoi.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class PrefixGenerator {
    private val transliteration = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "e",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ъ' to "",
        'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
    )

    fun fromDisplayName(displayName: String): String {
        val words = displayName.trim().lowercase(Locale.getDefault()).split(Regex("\\s+"))
        val transformed = words.joinToString("") { part ->
            val cleaned = part.mapNotNull { ch ->
                when {
                    ch.isLetterOrDigit() -> {
                        val l = ch.lowercaseChar()
                        transliteration[l] ?: l.toString()
                    }

                    ch == '-' -> "-"
                    else -> null
                }
            }.joinToString("")
            cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        return transformed.ifBlank { "User" }.take(20).padEnd(3, 'X')
    }
}

class StorageRepository(private val dao: StorageDao) {
    val settings: Flow<UserSettingsEntity?> = dao.observeSettings()
    val rooms = dao.observeRooms()
    val items = dao.observeItems()
    val codes = dao.observeCodes()

    suspend fun ensureSettings(displayName: String) {
        val current = settings.first()
        if (current == null) {
            val prefix = PrefixGenerator().fromDisplayName(displayName)
            dao.upsertSettings(UserSettingsEntity(displayName = displayName, prefix = prefix))
        }
    }

    suspend fun updateSettings(displayName: String, prefix: String) {
        dao.upsertSettings(UserSettingsEntity(displayName = displayName, prefix = prefix))
    }

    suspend fun ensureDefaultLocation(): String {
        val id = "default-location"
        dao.insertLocation(LocationEntity(id = id, name = "Моя локация"))
        return id
    }

    suspend fun addRoom(name: String, order: Int) {
        val locationId = ensureDefaultLocation()
        dao.insertRoom(RoomEntity(locationId = locationId, name = name, roomOrder = order))
    }

    suspend fun addItem(name: String) {
        dao.insertItem(ItemEntity(name = name))
    }

    suspend fun nextCode(prefix: String): String {
        val max = dao.maxCodeNumber(prefix) ?: 0
        return "$prefix${max + 1}"
    }

    suspend fun bindQr(entityType: String, entityId: String, code: String? = null): QrCodeEntity {
        val settingsValue = settings.first() ?: UserSettingsEntity(displayName = "User", prefix = "User")
        val generatedCode = code ?: nextCode(settingsValue.prefix)
        val payload = "app-scheme://qfa/${entityType.lowercase()}/$entityId?id=$generatedCode"
        val qr = QrCodeEntity(codeIdString = generatedCode, payloadString = payload, entityType = entityType, entityId = entityId)
        dao.insertQrCode(qr)
        return qr
    }

    suspend fun createBatchFreeCodes(count: Int): List<QrCodeEntity> {
        val settingsValue = settings.first() ?: UserSettingsEntity(displayName = "User", prefix = "User")
        val result = mutableListOf<QrCodeEntity>()
        var max = dao.maxCodeNumber(settingsValue.prefix) ?: 0
        repeat(count) {
            max += 1
            val code = "${settingsValue.prefix}$max"
            val freeId = UUID.randomUUID().toString()
            val qr = QrCodeEntity(
                codeIdString = code,
                payloadString = "app-scheme://qfa/free/$freeId?id=$code",
                entityType = "FREE",
                entityId = freeId,
                status = "free",
            )
            dao.insertQrCode(qr)
            result += qr
        }
        return result
    }

    suspend fun saveSearch(query: String) = dao.insertSearch(SearchHistoryEntity(query = query))

    suspend fun scan(payload: String): QrCodeEntity? {
        val match = dao.findByPayload(payload)
        dao.insertScan(ScanHistoryEntity(payloadString = payload, resolvedEntityType = match?.entityType, resolvedEntityId = match?.entityId))
        return match
    }

    suspend fun clearScanHistory() = dao.clearScanHistory()

    suspend fun recentScans(limit: Int = 50) = dao.recentScans(limit)

    suspend fun search(query: String): SearchResult {
        saveSearch(query)
        val normalized = query.trim()
        val roomsMap = dao.getAllRooms().associateBy { it.id }
        val unitsMap = dao.getAllUnits().associateBy { it.id }
        val cells = dao.getAllCells()
        val cellMap = cells.associateBy { it.id }

        val itemMatches = dao.getAllItems().mapNotNull { item ->
            val text = listOf(item.name, item.description, item.tags).joinToString(" ")
            val score = fuzzyScore(normalized, text)
            if (score >= 0.35f) {
                val cell = item.cellId?.let { cellMap[it] }
                val unit = cell?.unitId?.let { unitsMap[it] }
                val room = unit?.roomId?.let { roomsMap[it] }
                SearchHit("ITEM", item.id, item.name, roomPath(room?.name, unit?.name, cell?.name), score)
            } else null
        }.sortedByDescending { it.score }

        val placeMatches = cells.mapNotNull { cell ->
            val unit = unitsMap[cell.unitId]
            val room = unit?.roomId?.let { roomsMap[it] }
            val text = listOf(cell.name, cell.description, unit?.name ?: "", room?.name ?: "").joinToString(" ")
            val score = fuzzyScore(normalized, text)
            if (score >= 0.35f) SearchHit("PLACE", cell.id, cell.name, roomPath(room?.name, unit?.name, cell.name), score) else null
        }.sortedByDescending { it.score }

        val codeMatches = dao.getAllCodes().mapNotNull { qr ->
            val score = fuzzyScore(normalized, qr.codeIdString)
            if (score >= 0.45f) SearchHit("CODE", qr.id, qr.codeIdString, "${qr.entityType}:${qr.entityId}", score) else null
        }.sortedByDescending { it.score }

        return SearchResult(itemMatches.take(30), placeMatches.take(30), codeMatches.take(30))
    }

    private fun roomPath(room: String?, unit: String?, cell: String?): String =
        listOfNotNull(room, unit, cell).joinToString(" → ")

    private fun fuzzyScore(query: String, text: String): Float {
        if (query.isBlank()) return 0f
        val q = query.lowercase(Locale.getDefault())
        val words = text.lowercase(Locale.getDefault()).split(Regex("[^\\p{L}\\p{N}]+"))
        if (words.any { it.contains(q) }) return 1f
        val bestDistance = words.filter { it.isNotBlank() }.minOfOrNull { levenshtein(q, it) } ?: Int.MAX_VALUE
        val limit = when {
            q.length <= 4 -> 1
            q.length <= 9 -> 2
            else -> 3
        }
        return if (bestDistance <= limit) 1f - (bestDistance.toFloat() / (limit + 1f)) else 0f
    }

    private fun levenshtein(lhs: String, rhs: String): Int {
        val matrix = Array(lhs.length + 1) { IntArray(rhs.length + 1) }
        for (i in 0..lhs.length) matrix[i][0] = i
        for (j in 0..rhs.length) matrix[0][j] = j
        for (i in 1..lhs.length) {
            for (j in 1..rhs.length) {
                val cost = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,
                    matrix[i][j - 1] + 1,
                    matrix[i - 1][j - 1] + cost,
                )
            }
        }
        return matrix[lhs.length][rhs.length]
    }
}

data class SearchResult(
    val items: List<SearchHit>,
    val places: List<SearchHit>,
    val codes: List<SearchHit>,
)

data class SearchHit(
    val type: String,
    val id: String,
    val title: String,
    val path: String,
    val score: Float,
)

class StickerService {
    private val qrWriter = QRCodeWriter()

    fun qrBitmap(payload: String, size: Int = 360): Bitmap {
        val matrix = qrWriter.encode(payload, BarcodeFormat.QR_CODE, size, size)
        return createBitmap(size, size).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    fun buildPdf(context: Context, labels: List<StickerLabel>, pageType: String = "A4", margin: Float = 2f): File {
        val mmToPt = 72f / 25.4f
        val stickerW = 50f * mmToPt
        val stickerH = 40f * mmToPt
        val marginPt = margin * mmToPt
        val pageW = if (pageType == "ROLL_50") stickerW.toInt() else (210f * mmToPt).toInt()
        val pageH = if (pageType == "ROLL_50") (labels.size * stickerH).toInt() else (297f * mmToPt).toInt()
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        val canvas = page.canvas
        val paint = Paint().apply { color = Color.BLACK; textSize = 16f }

        var x = marginPt
        var y = marginPt
        labels.forEach { label ->
            val bmp = qrBitmap(label.payload, (stickerH - marginPt * 2).toInt().coerceAtLeast(120))
            canvas.drawRect(x, y, x + stickerW - marginPt, y + stickerH - marginPt, Paint().apply { style = Paint.Style.STROKE; color = Color.BLACK })
            canvas.drawBitmap(bmp, x + marginPt, y + marginPt, null)
            canvas.drawText(label.code, x + stickerH, y + stickerH / 2, paint)
            label.subtitle?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, x + stickerH, y + stickerH / 2 + 18f, Paint(paint).apply { textSize = 12f })
            }
            x += stickerW
            if (x + stickerW > pageW) {
                x = marginPt
                y += stickerH
            }
        }
        doc.finishPage(page)
        val fileName = "stickers_${labels.firstOrNull()?.code?.takeWhile { it.isLetter() } ?: "User"}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}_${labels.size}.pdf"
        val output = File(context.filesDir, fileName)
        output.outputStream().use { doc.writeTo(it) }
        doc.close()
        return output
    }
}

data class StickerLabel(
    val code: String,
    val payload: String,
    val subtitle: String? = null,
)

class BackupService(private val dao: StorageDao) {
    suspend fun exportZip(context: Context): File {
        val payload = BackupPayload(
            settings = dao.observeSettings().first(),
            rooms = dao.getAllRooms(),
            units = dao.getAllUnits(),
            cells = dao.getAllCells(),
            items = dao.getAllItems(),
            codes = dao.getAllCodes(),
        )
        val zipFile = File(context.filesDir, "backup_${System.currentTimeMillis()}.zip")
        ZipOutputStream(zipFile.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(Json.encodeToString(payload).toByteArray())
            zos.closeEntry()
        }
        return zipFile
    }

    suspend fun importZip(file: File) {
        ZipInputStream(file.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "backup.json") {
                    val json = zis.readBytes().decodeToString()
                    val payload = Json.decodeFromString<BackupPayload>(json)
                    payload.settings?.let { dao.upsertSettings(it) }
                    payload.rooms.forEach { dao.insertRoom(it) }
                    payload.units.forEach { dao.insertUnit(it) }
                    payload.cells.forEach { dao.insertCell(it) }
                    payload.items.forEach { dao.insertItem(it) }
                    payload.codes.forEach { dao.insertQrCode(it) }
                }
                entry = zis.nextEntry
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class BackupPayload(
    val settings: UserSettingsEntity? = null,
    val rooms: List<RoomEntity> = emptyList(),
    val units: List<StorageUnitEntity> = emptyList(),
    val cells: List<CellEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val codes: List<QrCodeEntity> = emptyList(),
)
