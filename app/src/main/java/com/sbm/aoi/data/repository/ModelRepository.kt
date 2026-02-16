package com.sbm.aoi.data.repository

import com.sbm.aoi.data.model.AiModel
import com.sbm.aoi.data.storage.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ModelRepository(private val fileManager: FileManager) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val modelsFile: File
        get() = File(fileManager.modelsDir, "models_list.json")

    private val _models = MutableStateFlow<List<AiModel>>(emptyList())
    val models: StateFlow<List<AiModel>> = _models.asStateFlow()

    private val _activeModel = MutableStateFlow<AiModel?>(null)
    val activeModel: StateFlow<AiModel?> = _activeModel.asStateFlow()

    companion object {
        const val MAX_MODELS = 10
    }

    init {
        loadModels()
    }

    private fun loadModels() {
        if (modelsFile.exists()) {
            val content = modelsFile.readText()
            val loaded = json.decodeFromString<List<AiModel>>(content)
            _models.value = loaded
            _activeModel.value = loaded.firstOrNull { it.isActive }
        }
    }

    private fun saveModels() {
        modelsFile.writeText(json.encodeToString(_models.value))
    }

    fun addModel(model: AiModel): Boolean {
        if (_models.value.size >= MAX_MODELS) return false
        _models.value = _models.value + model
        saveModels()
        return true
    }

    fun removeModel(modelId: String) {
        _models.value = _models.value.filter { it.id != modelId }
        if (_activeModel.value?.id == modelId) {
            _activeModel.value = null
        }
        fileManager.getModelDir(modelId).deleteRecursively()
        saveModels()
    }

    fun updateModel(model: AiModel) {
        _models.value = _models.value.map { if (it.id == model.id) model else it }
        if (model.isActive) {
            _activeModel.value = model
        } else if (_activeModel.value?.id == model.id) {
            _activeModel.value = null
        }
        saveModels()
    }

    fun activateModel(modelId: String) {
        _models.value = _models.value.map {
            it.copy(isActive = it.id == modelId)
        }
        _activeModel.value = _models.value.firstOrNull { it.id == modelId }
        saveModels()
    }

    fun getModelById(modelId: String): AiModel? =
        _models.value.firstOrNull { it.id == modelId }
}
