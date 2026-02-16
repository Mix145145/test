package com.sbm.aoi.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.data.model.Annotation
import com.sbm.aoi.data.model.DefectMark
import com.sbm.aoi.data.model.DefectType
import com.sbm.aoi.data.model.Detection
import com.sbm.aoi.data.repository.AnnotationRepository
import com.sbm.aoi.data.repository.ModelRepository
import com.sbm.aoi.data.storage.FileManager
import com.sbm.aoi.inference.OnnxInferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PhotoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inferenceEngine: OnnxInferenceEngine,
    private val modelRepository: ModelRepository,
    private val annotationRepository: AnnotationRepository,
    private val fileManager: FileManager,
) : ViewModel() {

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections: StateFlow<List<Detection>> = _detections.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var currentImagePath: String? = null

    val activeModel = modelRepository.activeModel

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    _selectedBitmap.value = bitmap
                    _detections.value = emptyList()

                    // Сохраняем копию
                    val imageId = UUID.randomUUID().toString()
                    val file = fileManager.getImageFile(imageId)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    currentImagePath = file.absolutePath
                } else {
                    _message.value = "Не удалось загрузить изображение"
                }
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun runDetection() {
        val bitmap = _selectedBitmap.value ?: return
        if (!inferenceEngine.isLoaded) {
            _message.value = "Сначала активируйте модель в разделе \"Модели\""
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val results = inferenceEngine.runInference(bitmap)
                _detections.value = results
                _message.value = "Найдено дефектов: ${results.size}"
            } catch (e: Exception) {
                _message.value = "Ошибка детекции: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveReport() {
        val imagePath = currentImagePath ?: return
        val model = modelRepository.activeModel.value

        viewModelScope.launch {
            val annotation = Annotation(
                id = UUID.randomUUID().toString(),
                imageId = File(imagePath).nameWithoutExtension,
                imagePath = imagePath,
                modelId = model?.id,
                modelName = model?.name,
                defects = _detections.value.map { det ->
                    DefectMark(
                        id = UUID.randomUUID().toString(),
                        x1 = det.x1,
                        y1 = det.y1,
                        x2 = det.x2,
                        y2 = det.y2,
                        defectType = DefectType.OTHER,
                        description = det.label,
                        confidence = det.confidence,
                        isManual = false,
                    )
                },
            )

            annotationRepository.saveAnnotation(annotation)
            _message.value = "Отчёт сохранён"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearSelection() {
        _selectedBitmap.value?.recycle()
        _selectedBitmap.value = null
        _detections.value = emptyList()
        currentImagePath = null
    }
}
