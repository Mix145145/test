package com.sbm.aoi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import com.sbm.aoi.data.model.Detection
import com.sbm.aoi.data.model.HighlightMethod

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    color: Color,
    opacity: Float,
    showLabels: Boolean,
    showConfidence: Boolean,
    highlightMethod: HighlightMethod,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val animatedAlpha by animateFloatAsState(
        targetValue = if (detections.isNotEmpty()) opacity else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "overlay_alpha",
    )

    val textPaint = remember {
        android.graphics.Paint().apply {
            textSize = 36f
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val scaleX = canvasW / imageWidth
        val scaleY = canvasH / imageHeight

        detections.forEach { det ->
            val left = det.x1 * scaleX
            val top = det.y1 * scaleY
            val right = det.x2 * scaleX
            val bottom = det.y2 * scaleY
            val bboxWidth = right - left
            val bboxHeight = bottom - top

            val drawColor = color.copy(alpha = animatedAlpha)

            // Рамка
            drawRect(
                color = drawColor,
                topLeft = Offset(left, top),
                size = Size(bboxWidth, bboxHeight),
                style = Stroke(width = 3f),
            )

            // Заливка
            if (highlightMethod == HighlightMethod.FRAME_AND_FILL) {
                drawRect(
                    color = drawColor.copy(alpha = animatedAlpha * 0.2f),
                    topLeft = Offset(left, top),
                    size = Size(bboxWidth, bboxHeight),
                )
            }

            // Текст
            if (showLabels || showConfidence) {
                val labelText = buildString {
                    if (showLabels) append(det.label)
                    if (showLabels && showConfidence) append(" ")
                    if (showConfidence) append("%.0f%%".format(det.confidence * 100))
                }

                textPaint.color = android.graphics.Color.argb(
                    (animatedAlpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt(),
                )

                // Фон для текста
                val textWidth = textPaint.measureText(labelText)
                val textHeight = textPaint.textSize

                drawContext.canvas.nativeCanvas.apply {
                    val bgPaint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.argb(
                            (animatedAlpha * 180).toInt(), 0, 0, 0
                        )
                    }
                    drawRect(
                        left, top - textHeight - 4f,
                        left + textWidth + 8f, top,
                        bgPaint,
                    )
                    drawText(labelText, left + 4f, top - 6f, textPaint)
                }
            }
        }
    }
}
