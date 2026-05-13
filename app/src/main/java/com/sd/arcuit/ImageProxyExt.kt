package com.sd.arcuit.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Extension function to convert a CameraX ImageProxy (YUV_420_888 format)
 * into a Bitmap usable for rendering or ML processing.
 *
 * Steps:
 * - Extract Y, U, and V planes from ImageProxy
 * - Convert to NV21 format (required by YuvImage)
 * - Compress to JPEG in memory
 * - Decode JPEG into Bitmap
 */
fun ImageProxy.toBitmap(): Bitmap {
    // Extract YUV planes from camera image
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    // Get sizes of each plane
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    // NV21 buffer (Y + V + U format)
    val nv21 = ByteArray(ySize + uSize + vSize)

    // Copy Y plane first
    yBuffer.get(nv21, 0, ySize)

    // NV21 requires V before U
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    // Wrap NV21 data into YuvImage
    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        width,
        height,
        null
    )

    // Compress to JPEG in memory
    val outputStream = ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        Rect(0, 0, width, height),
        100,
        outputStream
    )

    val imageBytes = outputStream.toByteArray()

    // Decode JPEG bytes into Bitmap
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
