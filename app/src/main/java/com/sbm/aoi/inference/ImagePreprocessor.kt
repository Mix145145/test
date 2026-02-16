package com.sbm.aoi.inference

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.FloatBuffer

object ImagePreprocessor {

    /**
     * Конвертирует Image (YUV_420_888 из CameraX) в Bitmap.
     */
    fun yuv420ToBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)

        val bytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Подготавливает Bitmap для YOLO-инференса:
     * resize + letterbox + normalize [0, 1] + CHW формат.
     *
     * @return FloatBuffer в формате [1, 3, inputSize, inputSize] и параметры letterbox
     */
    fun preprocess(
        bitmap: Bitmap,
        inputSize: Int,
    ): PreprocessResult {
        val srcW = bitmap.width
        val srcH = bitmap.height

        // Letterbox: масштабируем с сохранением пропорций
        val scale = minOf(inputSize.toFloat() / srcW, inputSize.toFloat() / srcH)
        val newW = (srcW * scale).toInt()
        val newH = (srcH * scale).toInt()
        val padX = (inputSize - newW) / 2
        val padY = (inputSize - newH) / 2

        val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        // Создаём letterboxed bitmap
        val letterboxed = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(letterboxed)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114)) // YOLO letterbox grey
        canvas.drawBitmap(resized, padX.toFloat(), padY.toFloat(), null)

        if (resized != bitmap) resized.recycle()

        // Bitmap → FloatBuffer (CHW, normalized)
        val pixels = IntArray(inputSize * inputSize)
        letterboxed.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        letterboxed.recycle()

        val buffer = FloatBuffer.allocate(3 * inputSize * inputSize)
        val area = inputSize * inputSize

        // R channel
        for (i in 0 until area) {
            buffer.put((pixels[i] shr 16 and 0xFF) / 255.0f)
        }
        // G channel
        for (i in 0 until area) {
            buffer.put((pixels[i] shr 8 and 0xFF) / 255.0f)
        }
        // B channel
        for (i in 0 until area) {
            buffer.put((pixels[i] and 0xFF) / 255.0f)
        }

        buffer.rewind()

        return PreprocessResult(
            buffer = buffer,
            scale = scale,
            padX = padX,
            padY = padY,
            originalWidth = srcW,
            originalHeight = srcH,
        )
    }

    data class PreprocessResult(
        val buffer: FloatBuffer,
        val scale: Float,
        val padX: Int,
        val padY: Int,
        val originalWidth: Int,
        val originalHeight: Int,
    )
}
