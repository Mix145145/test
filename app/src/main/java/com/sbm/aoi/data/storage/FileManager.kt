package com.sbm.aoi.data.storage

import android.content.Context
import java.io.File

class FileManager(private val context: Context) {

    val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    val annotationsDir: File
        get() = File(context.filesDir, "annotations").also { it.mkdirs() }

    val exportsDir: File
        get() = File(context.filesDir, "exports").also { it.mkdirs() }

    fun getModelDir(modelId: String): File =
        File(modelsDir, modelId).also { it.mkdirs() }

    fun getAnnotationFile(annotationId: String): File =
        File(annotationsDir, "$annotationId.json")

    fun getImageFile(imageId: String, extension: String = "jpg"): File =
        File(imagesDir, "$imageId.$extension")

    fun getExportFile(name: String): File =
        File(exportsDir, "$name.zip")

    fun clearExports() {
        exportsDir.listFiles()?.forEach { it.delete() }
    }
}
