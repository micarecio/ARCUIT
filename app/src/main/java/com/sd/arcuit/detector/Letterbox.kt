package com.sd.arcuit.detector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

fun letterbox(
    src: Bitmap,
    targetSize: Int = 640
): LetterboxResult {

    val srcW = src.width
    val srcH = src.height

    val scale = minOf(
        targetSize.toFloat() / srcW,
        targetSize.toFloat() / srcH
    )

    val newW = (srcW * scale).toInt()
    val newH = (srcH * scale).toInt()

    val resized = Bitmap.createScaledBitmap(src, newW, newH, true)
    val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(output)
    canvas.drawColor(Color.BLACK)

    val padX = (targetSize - newW) / 2f
    val padY = (targetSize - newH) / 2f

    canvas.drawBitmap(resized, padX, padY, Paint())

    return LetterboxResult(
        bitmap = output,
        scale = scale,
        padX = padX,
        padY = padY
    )
}
