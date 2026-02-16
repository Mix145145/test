package com.sbm.aoi.inference

import com.sbm.aoi.data.model.Detection

object YoloPostProcessor {

    /**
     * Парсит выход YOLOv8/11 detect.
     *
     * Формат выхода YOLOv8: [1, 4 + numClasses, numDetections]
     * - Строки 0-3: cx, cy, w, h
     * - Строки 4+: confidence по каждому классу
     *
     * @param output Сырой выход модели: массив [1, (4+numClasses), numDetections]
     * @param labels Список имён классов
     * @param confidenceThreshold Минимальная confidence
     * @param iouThreshold Порог IoU для NMS
     * @param preprocessResult Результат препроцессинга для обратного масштабирования
     */
    fun process(
        output: Array<Array<FloatArray>>,
        labels: List<String>,
        confidenceThreshold: Float,
        iouThreshold: Float,
        preprocessResult: ImagePreprocessor.PreprocessResult,
    ): List<Detection> {
        val data = output[0] // [4+numClasses, numDetections]
        val numAttributes = data.size
        if (numAttributes < 5) return emptyList()

        val numClasses = numAttributes - 4
        val numDetections = data[0].size

        val candidates = mutableListOf<Detection>()

        for (i in 0 until numDetections) {
            val cx = data[0][i]
            val cy = data[1][i]
            val w = data[2][i]
            val h = data[3][i]

            // Находим класс с максимальной confidence
            var maxConf = 0f
            var maxClassIdx = 0
            for (c in 0 until numClasses) {
                val conf = data[4 + c][i]
                if (conf > maxConf) {
                    maxConf = conf
                    maxClassIdx = c
                }
            }

            if (maxConf < confidenceThreshold) continue

            // xywh → xyxy (в координатах input_size)
            val x1 = cx - w / 2f
            val y1 = cy - h / 2f
            val x2 = cx + w / 2f
            val y2 = cy + h / 2f

            // Обратный letterbox: убираем padding, делим на scale
            val realX1 = ((x1 - preprocessResult.padX) / preprocessResult.scale)
                .coerceIn(0f, preprocessResult.originalWidth.toFloat())
            val realY1 = ((y1 - preprocessResult.padY) / preprocessResult.scale)
                .coerceIn(0f, preprocessResult.originalHeight.toFloat())
            val realX2 = ((x2 - preprocessResult.padX) / preprocessResult.scale)
                .coerceIn(0f, preprocessResult.originalWidth.toFloat())
            val realY2 = ((y2 - preprocessResult.padY) / preprocessResult.scale)
                .coerceIn(0f, preprocessResult.originalHeight.toFloat())

            val label = labels.getOrElse(maxClassIdx) { "class_$maxClassIdx" }

            candidates.add(
                Detection(
                    classIndex = maxClassIdx,
                    label = label,
                    confidence = maxConf,
                    x1 = realX1,
                    y1 = realY1,
                    x2 = realX2,
                    y2 = realY2,
                )
            )
        }

        return nms(candidates, iouThreshold)
    }

    /**
     * Non-Maximum Suppression.
     */
    private fun nms(detections: List<Detection>, iouThreshold: Float): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)

            sorted.removeAll { other ->
                if (best.classIndex == other.classIndex) {
                    iou(best, other) > iouThreshold
                } else {
                    false
                }
            }
        }

        return result
    }

    private fun iou(a: Detection, b: Detection): Float {
        val interX1 = maxOf(a.x1, b.x1)
        val interY1 = maxOf(a.y1, b.y1)
        val interX2 = minOf(a.x2, b.x2)
        val interY2 = minOf(a.y2, b.y2)

        val interArea = maxOf(0f, interX2 - interX1) * maxOf(0f, interY2 - interY1)
        val aArea = (a.x2 - a.x1) * (a.y2 - a.y1)
        val bArea = (b.x2 - b.x1) * (b.y2 - b.y1)
        val unionArea = aArea + bArea - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }
}
