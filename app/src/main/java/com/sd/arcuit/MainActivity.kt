package com.sd.arcuit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.sd.arcuit.detector.CircuitDetector
import com.sd.arcuit.logic.ICComponent
import com.sd.arcuit.util.toBitmap

class MainActivity : AppCompatActivity(), OverlayView.ICClickListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var detector: CircuitDetector
    private var camera: Camera? = null
    private var torchEnabled = false

    private val icMap = mutableMapOf<String, ICComponent>()
    private val icLabels = mutableMapOf<String, String>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

        overlayView = findViewById(R.id.overlayView)
        overlayView.listener = this

        detector = CircuitDetector(this)

        findViewById<Button>(R.id.btnTorch).setOnClickListener {
            toggleTorch()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->

                val fullBitmap = imageProxy.toBitmap()
                val crop = imageProxy.cropRect
                val bitmap = Bitmap.createBitmap(
                    fullBitmap,
                    crop.left,
                    crop.top,
                    crop.width(),
                    crop.height()
                )

                val detections = detector.detect(bitmap)

                val viewW = overlayView.width.toFloat()
                val viewH = overlayView.height.toFloat()
                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()

                val scale = minOf(viewW / imgW, viewH / imgH)
                val dx = (viewW - imgW * scale) / 2f
                val dy = (viewH - imgH * scale) / 2f

                val rawBoxes = detections.map {
                    BoundingBox(
                        it.boundingBox.left * scale + dx,
                        it.boundingBox.top * scale + dy,
                        it.boundingBox.right * scale + dx,
                        it.boundingBox.bottom * scale + dy,
                        it.label,
                        colorForLabel(it.label)
                    )
                }

                val finalBoxes = mutableListOf<BoundingBox>()
                val icBodies = mutableListOf<ICComponent>()

                rawBoxes.forEach { box ->

                    if (box.label != "ic_body") {
                        finalBoxes.add(box)
                        return@forEach
                    }

                    val boxRect = box.toRectF()

                    // 🔒 If overlaps frozen IC → ignore detector box
                    val frozenIC = icMap.values.firstOrNull { ic ->
                        ic.id in icLabels &&
                                RectF.intersects(ic.boundingBox, boxRect)
                    }

                    if (frozenIC != null) {
                        icBodies.add(frozenIC)
                        return@forEach
                    }

                    // 🔹 Match unfrozen IC
                    val matched = icMap.values.firstOrNull { ic ->
                        ic.id !in icLabels &&
                                RectF.intersects(ic.boundingBox, boxRect)
                    }

                    val ic = if (matched != null) {
                        matched.boundingBox.set(boxRect)
                        matched
                    } else {
                        val id = "IC_${System.nanoTime()}"
                        ICComponent(id, RectF(boxRect)).also {
                            icMap[id] = it
                        }
                    }

                    icBodies.add(ic)
                    finalBoxes.add(box)
                }

                // 🔒 Draw frozen IC boxes from stored geometry
                icMap.values
                    .filter { it.id in icLabels }
                    .forEach { frozen ->
                        finalBoxes.add(
                            BoundingBox(
                                frozen.boundingBox.left,
                                frozen.boundingBox.top,
                                frozen.boundingBox.right,
                                frozen.boundingBox.bottom,
                                "ic_body",
                                colorForLabel("ic_body")
                            )
                        )
                        if (!icBodies.contains(frozen)) {
                            icBodies.add(frozen)
                        }
                    }

                overlayView.setBoxes(finalBoxes)
                overlayView.setICBodies(icBodies)
                overlayView.setICLabels(icLabels)

                imageProxy.close()
            }

            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleTorch() {
        torchEnabled = !torchEnabled
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    // ❄️ Freeze IC EXACT SIZE at labeling time
    override fun onICClicked(ic: ICComponent) {
        val gates = arrayOf(
            "AND", "OR", "NOT",
            "NAND", "NOR", "XOR", "XNOR"
        )

        AlertDialog.Builder(this)
            .setTitle("Select IC Type")
            .setItems(gates) { _, which ->
                icLabels[ic.id] = gates[which]
                overlayView.invalidate()
            }
            .show()
    }
}
