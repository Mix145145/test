package com.sbm.aoi.util

import com.sbm.aoi.data.model.Annotation
import com.sbm.aoi.data.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtil {

    private val json = Json { prettyPrint = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    suspend fun exportAnnotations(
        annotations: List<Annotation>,
        fileManager: FileManager,
    ): File = withContext(Dispatchers.IO) {
        val timestamp = dateFormat.format(Date())
        val exportFile = fileManager.getExportFile("aoi_export_$timestamp")

        val files = mutableListOf<Pair<String, File>>()

        annotations.forEach { annotation ->
            // JSON-файл аннотации
            val annotationFile = fileManager.getAnnotationFile(annotation.id)
            if (annotationFile.exists()) {
                files.add("annotations/${annotation.id}.json" to annotationFile)
            }

            // Связанное изображение
            val imageFile = File(annotation.imagePath)
            if (imageFile.exists()) {
                files.add("images/${imageFile.name}" to imageFile)
            }
        }

        // Сводный файл
        val summaryFile = File(fileManager.exportsDir, "summary_$timestamp.json")
        summaryFile.writeText(json.encodeToString(annotations))
        files.add("summary.json" to summaryFile)

        ZipUtil.createZip(files, exportFile)

        // Удаляем временный summary
        summaryFile.delete()

        exportFile
    }
}
