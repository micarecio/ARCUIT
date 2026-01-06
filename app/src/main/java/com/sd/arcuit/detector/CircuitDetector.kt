package com.sd.arcuit.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.sd.arcuit.logic.Detection
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

class CircuitDetector(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>

    private val inputSize = 640
    private val confidenceThreshold = 0.25f

    init {
        interpreter = Interpreter(
            FileUtil.loadMappedFile(context, "arcuit.tflite")
        )
        labels = FileUtil.loadLabels(context, "labels.txt")

        val outShape = interpreter.getOutputTensor(0).shape()
        Log.d("ARCUIT_DEBUG", "Model output shape = ${outShape.contentToString()}")
    }

    private fun sigmoid(x: Float): Float {
        return 1f / (1f + exp(-x))
    }

    fun detect(bitmap: Bitmap): List<Detection> {

        val resizedBitmap =
            Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val inputBuffer =
            ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
                .order(ByteOrder.nativeOrder())

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resizedBitmap.getPixel(x, y)
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                inputBuffer.putFloat((pixel and 0xFF) / 255f)
            }
        }

        // YOLOv8 output tensor
        val output = Array(1) { Array(10) { FloatArray(8400) } }
        interpreter.run(inputBuffer, output)

        val detections = mutableListOf<Detection>()

        val numClasses = output[0].size - 4   // YOLOv8: classes start at index 4
        val numBoxes = 8400

        for (i in 0 until numBoxes) {

            var bestScore = 0f
            var bestClass = -1

            for (c in 0 until numClasses) {
                val score = output[0][4 + c][i]   // ❗ NO SIGMOID
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }

            if (bestScore < confidenceThreshold) continue
            if (bestClass == -1) continue

            val cx = output[0][0][i] * inputSize
            val cy = output[0][1][i] * inputSize
            val w  = output[0][2][i] * inputSize
            val h  = output[0][3][i] * inputSize

            val label =
                if (bestClass < labels.size) labels[bestClass] else "unknown"

            detections.add(
                Detection(
                    label = label,
                    confidence = bestScore,
                    boundingBox = RectF(
                        cx - w / 2,
                        cy - h / 2,
                        cx + w / 2,
                        cy + h / 2
                    )
                )
            )
        }


        Log.d("ARCUIT_DEBUG", "Detections count = ${detections.size}")

        val finalDetections = nonMaxSuppression(detections)
        Log.d("ARCUIT_DEBUG", "After NMS = ${finalDetections.size}")
        return finalDetections

    }

    private fun iou(a: RectF, b: RectF): Float {
        val interLeft = maxOf(a.left, b.left)
        val interTop = maxOf(a.top, b.top)
        val interRight = minOf(a.right, b.right)
        val interBottom = minOf(a.bottom, b.bottom)

        val interArea =
            maxOf(0f, interRight - interLeft) *
                    maxOf(0f, interBottom - interTop)

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        return interArea / (areaA + areaB - interArea + 1e-6f)
    }

    private fun nonMaxSuppression(
        detections: List<Detection>,
        iouThreshold: Float = 0.45f
    ): List<Detection> {

        val sorted = detections.sortedByDescending { it.confidence }
        val result = mutableListOf<Detection>()

        for (det in sorted) {
            var keep = true
            for (picked in result) {
                if (det.label == picked.label &&
                    iou(det.boundingBox, picked.boundingBox) > iouThreshold
                ) {
                    keep = false
                    break
                }
            }
            if (keep) result.add(det)
        }
        return result
    }


}
