package com.sbm.aoi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SbmAoiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initAppDirectories()
    }

    private fun initAppDirectories() {
        val dirs = listOf("models", "images", "annotations", "exports")
        dirs.forEach { dir ->
            val file = java.io.File(filesDir, dir)
            if (!file.exists()) {
                file.mkdirs()
            }
        }
    }
}
