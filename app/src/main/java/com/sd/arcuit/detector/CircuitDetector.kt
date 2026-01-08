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

        val lb = letterbox(bitmap, inputSize)
        val resizedBitmap = lb.bitmap

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

            val minConf =
                if (labels[bestClass].endsWith("_pin")) 0.15f else confidenceThreshold

            if (bestScore < minConf) continue

            if (bestClass == -1) continue

            val cx = output[0][0][i] * inputSize
            val cy = output[0][1][i] * inputSize
            val w  = output[0][2][i] * inputSize
            val h  = output[0][3][i] * inputSize

            val x = (cx - lb.padX) / lb.scale
            val y = (cy - lb.padY) / lb.scale
            val bw = w / lb.scale
            val bh = h / lb.scale

            Log.d("BOX_DEBUG", "cx=$cx cy=$cy w=$w h=$h score=$bestScore")

            val label =
                if (bestClass < labels.size) labels[bestClass] else "unknown"

            detections.add(
                Detection(
                    label = label,
                    confidence = bestScore,
                    boundingBox = RectF(
                        x - bw / 2,
                        y - bh / 2,
                        x + bw / 2,
                        y + bh / 2
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

    private fun area(r: RectF): Float {
        return maxOf(0f, r.width()) * maxOf(0f, r.height())
    }

    private val noNmsClasses = setOf(
        "vcc_pin",
        "gnd_pin",
        "wire_endpoint"
    )

    private fun nonMaxSuppression(
        detections: List<Detection>
    ): List<Detection> {

        val result = mutableListOf<Detection>()
        val sorted = detections.sortedByDescending { it.confidence }

        for (det in sorted) {

            // 🔥 Do NOT suppress these classes
            if (det.label in noNmsClasses) {
                result.add(det)
                continue
            }

            var keep = true
            val detArea = area(det.boundingBox)

            for (picked in result) {

                if (det.label != picked.label) continue

                val pickedArea = area(picked.boundingBox)
                val iouVal = iou(det.boundingBox, picked.boundingBox)

                val isSmall = detArea < 32f * 32f
                val isPickedSmall = pickedArea < 32f * 32f

                if (isSmall && isPickedSmall) {
                    if (iouVal > 0.75f) {
                        keep = false
                        break
                    }
                }
                else if (isSmall && !isPickedSmall) {
                    continue
                }
                else {
                    if (iouVal > 0.45f) {
                        keep = false
                        break
                    }
                }
            }

            if (keep) result.add(det)
        }

        return result
    }




}