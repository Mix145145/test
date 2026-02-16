package com.sbm.aoi.inference

import android.content.Context
import android.net.Uri
import com.sbm.aoi.data.model.AiModel
import com.sbm.aoi.data.storage.FileManager
import com.sbm.aoi.util.ZipUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class SbmModelConfig(
    val name: String = "Unknown",
    val input_size: Int = 640,
    val type: String = "yolo_detect",
    val confidence_default: Float = 0.35f,
    val iou_default: Float = 0.5f,
    val version: Int = 1,
)

class SbmModelLoader(
    private val context: Context,
    private val fileManager: FileManager,
) {
    private val json = Json { ignoreUnknownKeys = true }

    sealed class LoadResult {
        data class Success(val model: AiModel) : LoadResult()
        data class Error(val message: String) : LoadResult()
        data class NeedsLabels(val modelId: String, val modelPath: String) : LoadResult()
    }

    fun loadFromUri(uri: Uri): LoadResult {
        val fileName = getFileName(uri) ?: return LoadResult.Error("Не удалось определить имя файла")
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when (extension) {
            "sbmmodel" -> loadSbmModel(uri)
            "onnx" -> loadOnnxModel(uri, fileName)
            "pt" -> LoadResult.Error(
                "Файл .pt является обучающим чекпойнтом. " +
                    "Для Android требуется экспорт в формат .sbmmodel или .onnx."
            )
            "ptl" -> loadPtlModel(uri, fileName)
            else -> LoadResult.Error("Неподдерживаемый формат: .$extension")
        }
    }

    private fun loadSbmModel(uri: Uri): LoadResult {
        val modelId = UUID.randomUUID().toString()
        val modelDir = fileManager.getModelDir(modelId)

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipUtil.extractZip(inputStream, modelDir)
            } ?: return LoadResult.Error("Не удалось открыть файл")

            val onnxFile = File(modelDir, "model.onnx")
            if (!onnxFile.exists()) {
                modelDir.deleteRecursively()
                return LoadResult.Error("В пакете .sbmmodel отсутствует model.onnx")
            }

            val labelsFile = File(modelDir, "labels.txt")
            val labels = if (labelsFile.exists()) {
                labelsFile.readLines().filter { it.isNotBlank() }
            } else {
                emptyList()
            }

            val configFile = File(modelDir, "config.json")
            val config = if (configFile.exists()) {
                json.decodeFromString<SbmModelConfig>(configFile.readText())
            } else {
                SbmModelConfig()
            }

            val model = AiModel(
                id = modelId,
                name = config.name,
                path = onnxFile.absolutePath,
                labels = labels,
                inputSize = config.input_size,
                type = config.type,
                confidenceThreshold = config.confidence_default,
                iouThreshold = config.iou_default,
                version = config.version,
            )

            LoadResult.Success(model)
        } catch (e: Exception) {
            modelDir.deleteRecursively()
            LoadResult.Error("Ошибка загрузки: ${e.message}")
        }
    }

    private fun loadOnnxModel(uri: Uri, fileName: String): LoadResult {
        val modelId = UUID.randomUUID().toString()
        val modelDir = fileManager.getModelDir(modelId)
        val onnxFile = File(modelDir, "model.onnx")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                onnxFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return LoadResult.Error("Не удалось открыть файл")

            // Поиск labels.txt рядом с файлом
            val labelsFile = File(modelDir, "labels.txt")
            // Попробуем найти labels в том же URI-каталоге — не всегда возможно,
            // поэтому вернём NeedsLabels, если labels не найден
            if (!labelsFile.exists()) {
                return LoadResult.NeedsLabels(
                    modelId = modelId,
                    modelPath = onnxFile.absolutePath,
                )
            }

            val labels = labelsFile.readLines().filter { it.isNotBlank() }
            val name = fileName.removeSuffix(".onnx")

            LoadResult.Success(
                AiModel(
                    id = modelId,
                    name = name,
                    path = onnxFile.absolutePath,
                    labels = labels,
                )
            )
        } catch (e: Exception) {
            modelDir.deleteRecursively()
            LoadResult.Error("Ошибка загрузки: ${e.message}")
        }
    }

    private fun loadPtlModel(uri: Uri, fileName: String): LoadResult {
        val modelId = UUID.randomUUID().toString()
        val modelDir = fileManager.getModelDir(modelId)
        val ptlFile = File(modelDir, "model.ptl")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ptlFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return LoadResult.Error("Не удалось открыть файл")

            val name = fileName.removeSuffix(".ptl")

            LoadResult.NeedsLabels(
                modelId = modelId,
                modelPath = ptlFile.absolutePath,
            )
        } catch (e: Exception) {
            modelDir.deleteRecursively()
            LoadResult.Error("Ошибка загрузки: ${e.message}")
        }
    }

    fun addLabelsToModel(modelId: String, labelsUri: Uri): List<String> {
        val modelDir = fileManager.getModelDir(modelId)
        val labelsFile = File(modelDir, "labels.txt")

        context.contentResolver.openInputStream(labelsUri)?.use { input ->
            labelsFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return labelsFile.readLines().filter { it.isNotBlank() }
    }

    private fun getFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment
    }
}
