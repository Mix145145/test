package com.sbm.aoi.data.repository

import com.sbm.aoi.data.model.Annotation
import com.sbm.aoi.data.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnnotationRepository(private val fileManager: FileManager) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()

    init {
        loadAnnotations()
    }

    private fun loadAnnotations() {
        val files = fileManager.annotationsDir.listFiles { file ->
            file.extension == "json"
        } ?: return

        _annotations.value = files.mapNotNull { file ->
            runCatching {
                json.decodeFromString<Annotation>(file.readText())
            }.getOrNull()
        }.sortedByDescending { it.timestamp }
    }

    suspend fun saveAnnotation(annotation: Annotation) = withContext(Dispatchers.IO) {
        val file = fileManager.getAnnotationFile(annotation.id)
        file.writeText(json.encodeToString(annotation))
        loadAnnotations()
    }

    suspend fun deleteAnnotation(annotationId: String) = withContext(Dispatchers.IO) {
        fileManager.getAnnotationFile(annotationId).delete()
        loadAnnotations()
    }

    fun getAnnotationById(annotationId: String): Annotation? =
        _annotations.value.firstOrNull { it.id == annotationId }
}
