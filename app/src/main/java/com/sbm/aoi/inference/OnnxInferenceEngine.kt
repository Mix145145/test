package com.sbm.aoi.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import com.sbm.aoi.data.model.AiModel
import com.sbm.aoi.data.model.Detection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

class OnnxInferenceEngine {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var currentModelId: String? = null
    private var currentModel: AiModel? = null
    private val mutex = Mutex()

    val isLoaded: Boolean
        get() = ortSession != null

    fun loadModel(model: AiModel): Result<Unit> {
        return runCatching {
            release()

            val env = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                // NNAPI для аппаратного ускорения на поддерживаемых устройствах
                try {
                    addNnapi()
                } catch (_: Exception) {
                    // NNAPI не доступен — используем CPU
                }
            }

            val session = env.createSession(model.path, sessionOptions)
            ortEnvironment = env
            ortSession = session
            currentModelId = model.id
            currentModel = model
        }
    }

    suspend fun runInference(bitmap: Bitmap): List<Detection> = mutex.withLock {
        withContext(Dispatchers.Default) {
            val session = ortSession ?: return@withContext emptyList()
            val model = currentModel ?: return@withContext emptyList()

            val preprocessResult = ImagePreprocessor.preprocess(bitmap, model.inputSize)

            val inputName = session.inputNames.firstOrNull() ?: return@withContext emptyList()
            val shape = longArrayOf(1, 3, model.inputSize.toLong(), model.inputSize.toLong())

            val env = ortEnvironment ?: return@withContext emptyList()
            val tensor = OnnxTensor.createTensor(env, preprocessResult.buffer, shape)

            try {
                val results = session.run(mapOf(inputName to tensor))
                val outputTensor = results[0] as OnnxTensor

                @Suppress("UNCHECKED_CAST")
                val outputData = outputTensor.value as Array<Array<FloatArray>>

                YoloPostProcessor.process(
                    output = outputData,
                    labels = model.labels,
                    confidenceThreshold = model.confidenceThreshold,
                    iouThreshold = model.iouThreshold,
                    preprocessResult = preprocessResult,
                )
            } finally {
                tensor.close()
            }
        }
    }

    fun release() {
        ortSession?.close()
        ortSession = null
        currentModelId = null
        currentModel = null
    }

    fun getCurrentModelId(): String? = currentModelId
}
