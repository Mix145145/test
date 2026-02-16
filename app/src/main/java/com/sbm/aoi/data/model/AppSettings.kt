package com.sbm.aoi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val frameAnalysisRate: Int = 2,
    val photoQuality: Int = 90,
    val saveToGallery: Boolean = false,
    val soundOnDetection: Boolean = true,
    val vibrationOnDetection: Boolean = true,
    val autoPauseOnSwitch: Boolean = true,
    val useDeeperDark: Boolean = false,
    val fontScale: Float = 1.0f,
)
