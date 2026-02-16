package com.sbm.aoi.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.sbm.aoi.data.model.Detection
import com.sbm.aoi.inference.ImagePreprocessor
import com.sbm.aoi.inference.OnnxInferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FrameAnalyzer(
    private val inferenceEngine: OnnxInferenceEngine,
    private val scope: CoroutineScope,
) : ImageAnalysis.Analyzer {

    private val frameCounter = AtomicInteger(0)
    private val isProcessing = AtomicBoolean(false)

    var frameSkipRate: Int = 2
    var isPaused: Boolean = false

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections: StateFlow<List<Detection>> = _detections.asStateFlow()

    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private var lastTimestamp = 0L

    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused || !inferenceEngine.isLoaded) {
            imageProxy.close()
            return
        }

        val count = frameCounter.incrementAndGet()
        if (count % frameSkipRate != 0) {
            imageProxy.close()
            return
        }

        if (isProcessing.get()) {
            imageProxy.close()
            return
        }

        isProcessing.set(true)

        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val bitmap = ImagePreprocessor.yuv420ToBitmap(image)
        imageProxy.close()

        scope.launch(Dispatchers.Default) {
            try {
                val startTime = System.nanoTime()
                val results = inferenceEngine.runInference(bitmap)
                val elapsed = (System.nanoTime() - startTime) / 1_000_000f

                _detections.value = results
                _fps.value = if (elapsed > 0) 1000f / elapsed else 0f
            } catch (_: Exception) {
                _detections.value = emptyList()
            } finally {
                bitmap.recycle()
                isProcessing.set(false)
            }
        }
    }
}
