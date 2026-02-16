package com.sbm.aoi.ui.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.aoi.data.model.AiModel
import com.sbm.aoi.data.model.HighlightMethod
import com.sbm.aoi.data.repository.ModelRepository
import com.sbm.aoi.inference.OnnxInferenceEngine
import com.sbm.aoi.inference.SbmModelLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val modelLoader: SbmModelLoader,
    private val inferenceEngine: OnnxInferenceEngine,
) : ViewModel() {

    val models = modelRepository.models
    val activeModel = modelRepository.activeModel

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    fun loadModelFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = modelLoader.loadFromUri(uri)) {
                is SbmModelLoader.LoadResult.Success -> {
                    val added = modelRepository.addModel(result.model)
                    if (!added) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Максимум ${ModelRepository.MAX_MODELS} моделей",
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Модель \"${result.model.name}\" добавлена",
                        )
                    }
                }

                is SbmModelLoader.LoadResult.NeedsLabels -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pendingModelId = result.modelId,
                        pendingModelPath = result.modelPath,
                        showLabelsDialog = true,
                    )
                }

                is SbmModelLoader.LoadResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun addLabelsToModel(labelsUri: Uri) {
        val modelId = _uiState.value.pendingModelId ?: return
        val modelPath = _uiState.value.pendingModelPath ?: return

        viewModelScope.launch {
            val labels = modelLoader.addLabelsToModel(modelId, labelsUri)
            val model = AiModel(
                id = modelId,
                name = modelPath.substringAfterLast('/').removeSuffix(".onnx").removeSuffix(".ptl"),
                path = modelPath,
                labels = labels,
            )
            modelRepository.addModel(model)
            _uiState.value = _uiState.value.copy(
                showLabelsDialog = false,
                pendingModelId = null,
                pendingModelPath = null,
                message = "Модель \"${model.name}\" добавлена",
            )
        }
    }

    fun skipLabels() {
        val modelId = _uiState.value.pendingModelId ?: return
        val modelPath = _uiState.value.pendingModelPath ?: return

        val model = AiModel(
            id = modelId,
            name = modelPath.substringAfterLast('/').removeSuffix(".onnx").removeSuffix(".ptl"),
            path = modelPath,
        )
        modelRepository.addModel(model)
        _uiState.value = _uiState.value.copy(
            showLabelsDialog = false,
            pendingModelId = null,
            pendingModelPath = null,
            message = "Модель добавлена без меток",
        )
    }

    fun deleteModel(modelId: String) {
        if (inferenceEngine.getCurrentModelId() == modelId) {
            inferenceEngine.release()
        }
        modelRepository.removeModel(modelId)
    }

    fun activateModel(modelId: String) {
        modelRepository.activateModel(modelId)
        val model = modelRepository.getModelById(modelId) ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = inferenceEngine.loadModel(model)
            val rawError = result.exceptionOrNull()?.message
            val readableError = when {
                rawError == null -> null
                rawError.contains("Unsupported model IR version", ignoreCase = true) -> {
                    "Не удалось загрузить модель: версия ONNX не поддерживается текущим движком. " +
                        "Переэкспортируйте модель в совместимый формат или обновите файл .sbmmodel."
                }

                else -> "Не удалось загрузить модель: $rawError. Проверьте файл .sbmmodel/.onnx и повторите попытку."
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = readableError,
                message = if (result.isSuccess) "Модель \"${model.name}\" активирована" else null,
            )
        }
    }

    fun renameModel(modelId: String, newName: String) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(name = newName))
    }

    fun setModelColor(modelId: String, colorArgb: Int) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(colorArgb = colorArgb))
    }

    fun updateConfidence(modelId: String, value: Float) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(confidenceThreshold = value))
    }

    fun updateIou(modelId: String, value: Float) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(iouThreshold = value))
    }

    fun updateOverlayOpacity(modelId: String, value: Float) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(overlayOpacity = value))
    }

    fun toggleShowLabels(modelId: String) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(showLabels = !model.showLabels))
    }

    fun toggleShowConfidence(modelId: String) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(showConfidence = !model.showConfidence))
    }

    fun setHighlightMethod(modelId: String, method: HighlightMethod) {
        val model = modelRepository.getModelById(modelId) ?: return
        modelRepository.updateModel(model.copy(highlightMethod = method))
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ModelsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val showLabelsDialog: Boolean = false,
    val pendingModelId: String? = null,
    val pendingModelPath: String? = null,
)
