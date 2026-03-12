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

class CircuitDetector(context: Context) {

    private val interpreter: Interpreter
    private val labels = listOf(
        "ic_body",
        "led",
        "neg_rail",
        "pos_rail",
        "push_button",
        "resistor",
        "switch",
        "wire_endpoint"
    )

    private val inputSize = 640
    private val confThreshold = 0.25f

    init {
        interpreter = Interpreter(
            FileUtil.loadMappedFile(context, "arcuit.tflite")
        )
        Log.d("ARCUIT_DEBUG",
            "Output shape=${interpreter.getOutputTensor(0).shape().contentToString()}")
    }

    fun detect(bitmap: Bitmap): List<Detection> {

        val lb = letterbox(bitmap, inputSize)
        val img = lb.bitmap

        val input =
            ByteBuffer.allocateDirect(1 * 3 * inputSize * inputSize * 4)
                .order(ByteOrder.nativeOrder())

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val px = img.getPixel(x, y)

                input.putFloat(((px shr 16) and 0xFF) / 255f) // R
                input.putFloat(((px shr 8) and 0xFF) / 255f)  // G
                input.putFloat((px and 0xFF) / 255f)          // B
            }
        }


        val output = Array(1) { Array(12) { FloatArray(8400) } }
        interpreter.run(input, output)

        val results = mutableListOf<Detection>()

        for (i in 0 until 8400) {

            var bestScore = 0f
            var bestClass = -1

            for (c in 0 until 8) {
                val score = output[0][4 + c][i]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }

            val minConf = when (labels[bestClass]) {
                "wire_endpoint" -> 0.10f
                else -> confThreshold
            }

            if (bestScore < minConf) continue

            val cx = output[0][0][i] * inputSize
            val cy = output[0][1][i] * inputSize
            val w  = output[0][2][i] * inputSize
            val h  = output[0][3][i] * inputSize

            val x = (cx - lb.padX) / lb.scale
            val y = (cy - lb.padY) / lb.scale
            val bw = w / lb.scale
            val bh = h / lb.scale

            val left   = (x - bw / 2).coerceIn(0f, bitmap.width.toFloat())
            val top    = (y - bh / 2).coerceIn(0f, bitmap.height.toFloat())
            val right  = (x + bw / 2).coerceIn(0f, bitmap.width.toFloat())
            val bottom = (y + bh / 2).coerceIn(0f, bitmap.height.toFloat())

            results.add(
                Detection(
                    labels[bestClass],
                    bestScore,
                    RectF(left, top, right, bottom)
                )
            )

        }

        return clusterDetections(
            nonMaxSuppression(results)
        )

    }

    private fun nonMaxSuppression(dets: List<Detection>): List<Detection> {
        val out = mutableListOf<Detection>()
        val sorted = dets.sortedByDescending { it.confidence }

        for (d in sorted) {
            var keep = true

            for (p in out) {
                if (d.label != p.label) continue

                val iouThresh = when (d.label) {
                    "wire_endpoint" -> 0.05f
                    "push_button" -> 0.25f
                    else -> 0.45f
                }

                if (iou(d.boundingBox, p.boundingBox) > iouThresh) {
                    keep = false
                    break
                }
            }

            if (keep) out.add(d)
        }

        return out
    }
    private fun clusterDetections(
        detections: List<Detection>,
        radiusPx: Float = 14f
    ): List<Detection> {

        val result = mutableListOf<Detection>()

        for (d in detections.sortedByDescending { it.confidence }) {
            val cx = d.boundingBox.centerX()
            val cy = d.boundingBox.centerY()

            val exists = result.any {
                if (d.label == "wire_endpoint") return@any false
                val dx = it.boundingBox.centerX() - cx
                val dy = it.boundingBox.centerY() - cy
                dx * dx + dy * dy < radiusPx * radiusPx
            }

            if (!exists) result.add(d)
        }

        return result
    }

    private fun iou(a: RectF, b: RectF): Float {
        val l = maxOf(a.left, b.left)
        val t = maxOf(a.top, b.top)
        val r = minOf(a.right, b.right)
        val btm = minOf(a.bottom, b.bottom)
        val inter = maxOf(0f, r - l) * maxOf(0f, btm - t)
        val ua = a.width() * a.height() + b.width() * b.height() - inter
        return inter / (ua + 1e-6f)
    }
}
