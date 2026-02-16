package com.sbm.aoi.ui.markup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.data.model.Annotation
import com.sbm.aoi.data.model.DefectMark
import com.sbm.aoi.data.model.DefectType
import com.sbm.aoi.data.repository.AnnotationRepository
import com.sbm.aoi.data.repository.ModelRepository
import com.sbm.aoi.data.storage.FileManager
import com.sbm.aoi.util.ExportUtil
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
class MarkupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val annotationRepository: AnnotationRepository,
    private val modelRepository: ModelRepository,
    private val fileManager: FileManager,
) : ViewModel() {

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val _defects = MutableStateFlow<List<DefectMark>>(emptyList())
    val defects: StateFlow<List<DefectMark>> = _defects.asStateFlow()

    private val _selectedDefectId = MutableStateFlow<String?>(null)
    val selectedDefectId: StateFlow<String?> = _selectedDefectId.asStateFlow()

    private val _isDrawing = MutableStateFlow(true)
    val isDrawing: StateFlow<Boolean> = _isDrawing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var currentImagePath: String? = null

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    _bitmap.value = bitmap
                    _defects.value = emptyList()
                    _selectedDefectId.value = null

                    val imageId = UUID.randomUUID().toString()
                    val file = fileManager.getImageFile(imageId)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    currentImagePath = file.absolutePath
                }
            } catch (e: Exception) {
                _message.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun addBbox(x1: Float, y1: Float, x2: Float, y2: Float) {
        val newDefect = DefectMark(
            id = UUID.randomUUID().toString(),
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2,
            defectType = DefectType.OTHER,
            isManual = true,
        )
        _defects.value = _defects.value + newDefect
        _selectedDefectId.value = newDefect.id
        _isDrawing.value = false // Переключаемся на редактирование для выбора типа
    }

    fun selectDefect(id: String?) {
        _selectedDefectId.value = id
    }

    fun updateDefectType(type: DefectType) {
        val id = _selectedDefectId.value ?: return
        _defects.value = _defects.value.map {
            if (it.id == id) it.copy(defectType = type) else it
        }
    }

    fun updateDefectDescription(description: String) {
        val id = _selectedDefectId.value ?: return
        _defects.value = _defects.value.map {
            if (it.id == id) it.copy(description = description) else it
        }
    }

    fun deleteSelectedDefect() {
        val id = _selectedDefectId.value ?: return
        _defects.value = _defects.value.filter { it.id != id }
        _selectedDefectId.value = null
    }

    fun toggleDrawingMode() {
        _isDrawing.value = !_isDrawing.value
        if (_isDrawing.value) {
            _selectedDefectId.value = null
        }
    }

    fun saveAnnotation() {
        val imagePath = currentImagePath ?: return
        val model = modelRepository.activeModel.value

        // Проверяем обязательные поля
        val invalidDefects = _defects.value.filter { defect ->
            (defect.defectType == DefectType.OTHER || defect.defectType == DefectType.ANOMALY)
                && defect.description.isBlank()
        }
        if (invalidDefects.isNotEmpty()) {
            _message.value = "Заполните описание для дефектов типа \"Другое\" или \"Аномалия\""
            return
        }

        viewModelScope.launch {
            val annotation = Annotation(
                id = UUID.randomUUID().toString(),
                imageId = File(imagePath).nameWithoutExtension,
                imagePath = imagePath,
                modelId = model?.id,
                modelName = model?.name,
                defects = _defects.value,
            )

            annotationRepository.saveAnnotation(annotation)
            _message.value = "Разметка сохранена (${_defects.value.size} дефектов)"
        }
    }

    fun exportToZip() {
        viewModelScope.launch {
            try {
                val annotations = annotationRepository.annotations.value
                if (annotations.isEmpty()) {
                    _message.value = "Нет аннотаций для экспорта"
                    return@launch
                }
                val file = ExportUtil.exportAnnotations(annotations, fileManager)
                _message.value = "Экспорт завершён: ${file.name}"
            } catch (e: Exception) {
                _message.value = "Ошибка экспорта: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearAll() {
        _bitmap.value?.recycle()
        _bitmap.value = null
        _defects.value = emptyList()
        _selectedDefectId.value = null
        currentImagePath = null
    }
}
