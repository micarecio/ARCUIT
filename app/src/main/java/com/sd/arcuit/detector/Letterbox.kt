package com.sd.arcuit.detector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Resizes an image using letterbox padding while preserving aspect ratio.
 *
 * This is commonly used in object detection models (like YOLO)
 * to ensure input images match the required square input size
 * without distorting the original image.
 */
fun letterbox(
    src: Bitmap,
    targetSize: Int = 640
): LetterboxResult {

    // Original image dimensions
    val srcW = src.width
    val srcH = src.height

    // Compute scale factor to fit image inside target size
    val scale = minOf(
        targetSize.toFloat() / srcW,
        targetSize.toFloat() / srcH
    )

    // Compute new resized dimensions while preserving aspect ratio
    val newW = (srcW * scale).toInt()
    val newH = (srcH * scale).toInt()

    // Resize original image to new dimensions
    val resized = Bitmap.createScaledBitmap(src, newW, newH, true)

    // Create a square output image filled with black background
    val output = Bitmap.createBitmap(
        targetSize,
        targetSize,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)

    // Fill background (letterbox padding area)
    canvas.drawColor(Color.BLACK)

    // Compute padding offsets to center the image
    val padX = (targetSize - newW) / 2f
    val padY = (targetSize - newH) / 2f

    // Draw resized image onto center of canvas
    canvas.drawBitmap(resized, padX, padY, Paint())

    // Return processed image + transformation metadata
    return LetterboxResult(
        bitmap = output,
        scale = scale,
        padX = padX,
        padY = padY
    )
}
