package com.sbm.aoi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AiModel(
    val id: String,
    val name: String,
    val path: String,
    val labels: List<String> = emptyList(),
    val inputSize: Int = 640,
    val type: String = "yolo_detect",
    val colorArgb: Int = 0xFF1E88E5.toInt(),
    val confidenceThreshold: Float = 0.35f,
    val iouThreshold: Float = 0.5f,
    val overlayOpacity: Float = 0.7f,
    val showLabels: Boolean = true,
    val showConfidence: Boolean = true,
    val highlightMethod: HighlightMethod = HighlightMethod.FRAME_ONLY,
    val isActive: Boolean = false,
    val version: Int = 1,
)

@Serializable
enum class HighlightMethod {
    FRAME_ONLY,
    FRAME_AND_FILL,
    MASK,
}
