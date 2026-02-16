package com.sbm.aoi.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sbm.aoi.data.model.DefectMark
import com.sbm.aoi.ui.theme.Error
import com.sbm.aoi.ui.theme.Primary
import kotlin.math.max

private data class ImageViewport(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

@Composable
fun BboxDrawer(
    defects: List<DefectMark>,
    selectedDefectId: String?,
    imageWidth: Int,
    imageHeight: Int,
    bitmap: android.graphics.Bitmap,
    isDrawing: Boolean,
    onNewBbox: (x1: Float, y1: Float, x2: Float, y2: Float) -> Unit,
    onSelectDefect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    var zoom by remember(imageWidth, imageHeight) { mutableStateOf(1f) }
    var panX by remember(imageWidth, imageHeight) { mutableStateOf(0f) }
    var panY by remember(imageWidth, imageHeight) { mutableStateOf(0f) }

    val textPaint = remember {
        Paint().apply {
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
            color = android.graphics.Color.WHITE
        }
    }

    fun constrainPan(canvasW: Float, canvasH: Float): Pair<Float, Float> {
        if (imageWidth <= 0 || imageHeight <= 0) return 0f to 0f
        val baseScale = minOf(canvasW / imageWidth, canvasH / imageHeight)
        val scaledW = imageWidth * baseScale * zoom
        val scaledH = imageHeight * baseScale * zoom

        val maxPanX = max(0f, (scaledW - canvasW) / 2f)
        val maxPanY = max(0f, (scaledH - canvasH) / 2f)

        return panX.coerceIn(-maxPanX, maxPanX) to panY.coerceIn(-maxPanY, maxPanY)
    }

    fun buildViewport(canvasW: Float, canvasH: Float): ImageViewport? {
        if (imageWidth <= 0 || imageHeight <= 0) return null
        val baseScale = minOf(canvasW / imageWidth, canvasH / imageHeight)
        val scaledW = imageWidth * baseScale * zoom
        val scaledH = imageHeight * baseScale * zoom

        val (safePanX, safePanY) = constrainPan(canvasW, canvasH)
        panX = safePanX
        panY = safePanY

        return ImageViewport(
            left = ((canvasW - scaledW) / 2f) + panX,
            top = ((canvasH - scaledH) / 2f) + panY,
            width = scaledW,
            height = scaledH,
        )
    }

    fun canvasToImage(point: Offset, viewport: ImageViewport): Offset {
        val iw = imageWidth.toFloat()
        val ih = imageHeight.toFloat()

        val x = ((point.x - viewport.left) / viewport.width * iw).coerceIn(0f, iw)
        val y = ((point.y - viewport.top) / viewport.height * ih).coerceIn(0f, ih)
        return Offset(x, y)
    }

    fun imageToCanvas(point: Offset, viewport: ImageViewport): Offset {
        val iw = imageWidth.toFloat()
        val ih = imageHeight.toFloat()

        val x = viewport.left + (point.x / iw * viewport.width)
        val y = viewport.top + (point.y / ih * viewport.height)
        return Offset(x, y)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isDrawing, zoom, panX, panY) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (isDrawing) {
                                dragStart = offset
                                dragCurrent = offset
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (isDrawing) {
                                dragCurrent = change.position
                            } else {
                                panX += dragAmount.x
                                panY += dragAmount.y
                            }
                        },
                        onDragEnd = {
                            val start = dragStart
                            val end = dragCurrent
                            val viewport = buildViewport(size.width.toFloat(), size.height.toFloat())

                            if (isDrawing && start != null && end != null && viewport != null) {
                                val imageStart = canvasToImage(start, viewport)
                                val imageEnd = canvasToImage(end, viewport)

                                val x1 = minOf(imageStart.x, imageEnd.x)
                                val y1 = minOf(imageStart.y, imageEnd.y)
                                val x2 = maxOf(imageStart.x, imageEnd.x)
                                val y2 = maxOf(imageStart.y, imageEnd.y)

                                if ((x2 - x1) > 5f && (y2 - y1) > 5f) {
                                    onNewBbox(x1, y1, x2, y2)
                                }
                            }

                            dragStart = null
                            dragCurrent = null
                        },
                        onDragCancel = {
                            dragStart = null
                            dragCurrent = null
                        },
                    )
                }
                .pointerInput(isDrawing, zoom, panX, panY) {
                    if (!isDrawing) {
                        detectTapGestures { offset ->
                            val viewport = buildViewport(size.width.toFloat(), size.height.toFloat()) ?: return@detectTapGestures
                            val tap = canvasToImage(offset, viewport)
                            val tapped = defects.firstOrNull { def ->
                                tap.x in def.x1..def.x2 && tap.y in def.y1..def.y2
                            }
                            onSelectDefect(tapped?.id)
                        }
                    }
                },
        ) {
            val viewport = buildViewport(size.width.toFloat(), size.height.toFloat()) ?: return@Canvas

            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = androidx.compose.ui.unit.IntOffset(viewport.left.toInt(), viewport.top.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(viewport.width.toInt(), viewport.height.toInt()),
            )

            defects.forEach { defect ->
                val topLeft = imageToCanvas(Offset(defect.x1, defect.y1), viewport)
                val bottomRight = imageToCanvas(Offset(defect.x2, defect.y2), viewport)
                val w = bottomRight.x - topLeft.x
                val h = bottomRight.y - topLeft.y

                val isSelected = defect.id == selectedDefectId
                val color = if (isSelected) Error else Primary

                drawRect(
                    color = color,
                    topLeft = topLeft,
                    size = Size(w, h),
                    style = Stroke(width = if (isSelected) 4f else 2f),
                )

                drawRect(
                    color = color.copy(alpha = 0.15f),
                    topLeft = topLeft,
                    size = Size(w, h),
                )

                drawContext.canvas.nativeCanvas.apply {
                    val bgPaint = Paint().apply {
                        this.color = android.graphics.Color.argb(180, 0, 0, 0)
                    }
                    val label = defect.defectType.displayName
                    val textW = textPaint.measureText(label)
                    drawRect(topLeft.x, topLeft.y - 36f, topLeft.x + textW + 8f, topLeft.y, bgPaint)
                    drawText(label, topLeft.x + 4f, topLeft.y - 8f, textPaint)
                }
            }

            val start = dragStart
            val end = dragCurrent
            if (isDrawing && start != null && end != null) {
                val left = minOf(start.x, end.x)
                val top = minOf(start.y, end.y)
                val w = maxOf(start.x, end.x) - left
                val h = maxOf(start.y, end.y) - top

                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(width = 3f),
                )
                drawRect(
                    color = Color.Yellow.copy(alpha = 0.1f),
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(4f) }) {
                Icon(Icons.Default.Add, contentDescription = "Увеличить")
            }
            IconButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(1f) }) {
                Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
            }
            IconButton(onClick = {
                zoom = 1f
                panX = 0f
                panY = 0f
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Сбросить масштаб")
            }
        }
    }
}
