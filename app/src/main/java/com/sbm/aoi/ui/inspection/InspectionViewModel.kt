package com.sbm.aoi.ui.inspection

import android.content.Context
import android.graphics.Bitmap
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.camera.FrameAnalyzer
import com.sbm.aoi.data.model.Detection
import com.sbm.aoi.data.repository.ModelRepository
import com.sbm.aoi.data.repository.SettingsRepository
import com.sbm.aoi.data.storage.FileManager
import com.sbm.aoi.inference.OnnxInferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InspectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository,
    private val inferenceEngine: OnnxInferenceEngine,
    private val fileManager: FileManager,
) : ViewModel() {

    val activeModel = modelRepository.activeModel

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.sbm.aoi.data.model.AppSettings(),
    )

    val frameAnalyzer = FrameAnalyzer(inferenceEngine, viewModelScope)

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _lastCapturedPhoto = MutableStateFlow<String?>(null)
    val lastCapturedPhoto: StateFlow<String?> = _lastCapturedPhoto.asStateFlow()

    init {
        // Загружаем активную модель при старте
        viewModelScope.launch {
            modelRepository.activeModel.collect { model ->
                if (model != null && inferenceEngine.getCurrentModelId() != model.id) {
                    inferenceEngine.loadModel(model)
                }
                frameAnalyzer.frameSkipRate = settings.value.frameAnalysisRate
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        frameAnalyzer.isPaused = _isPaused.value
    }

    fun capturePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "AOI_${dateFormat.format(Date())}.jpg"
            val file = File(fileManager.imagesDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    settings.value.photoQuality,
                    out,
                )
            }

            _lastCapturedPhoto.value = file.absolutePath

            // Вибрация при съёмке
            if (settings.value.vibrationOnDetection) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    }

    fun switchModel() {
        viewModelScope.launch {
            val models = modelRepository.models.value
            if (models.size <= 1) return@launch

            val currentId = modelRepository.activeModel.value?.id
            val currentIndex = models.indexOfFirst { it.id == currentId }
            val nextIndex = (currentIndex + 1) % models.size
            modelRepository.activateModel(models[nextIndex].id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        frameAnalyzer.isPaused = true
    }
}
