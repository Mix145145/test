package com.sbm.aoi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Annotation(
    val id: String,
    val imageId: String,
    val imagePath: String,
    val modelId: String? = null,
    val modelName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val defects: List<DefectMark> = emptyList(),
)

@Serializable
data class DefectMark(
    val id: String,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val defectType: DefectType,
    val description: String = "",
    val confidence: Float? = null,
    val isManual: Boolean = true,
)

@Serializable
enum class DefectType(val displayName: String) {
    MISALIGNMENT("Кривое расположение"),
    EXCESS_SOLDER("Излишки олова"),
    MISSING_COMPONENT("Отсутствие компонента"),
    OTHER("Другое"),
    ANOMALY("Аномалия"),
}
