package com.sd.arcuit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.sd.arcuit.logic.ConnectionBuilder
import com.sd.arcuit.logic.DetectedObject
import com.sd.arcuit.logic.GateType
import com.sd.arcuit.logic.ICComponent
import com.sd.arcuit.logic.ObjectType
import android.app.AlertDialog
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import com.sd.arcuit.detector.CircuitDetector
import com.sd.arcuit.util.toBitmap

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var circuitDetector: CircuitDetector

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ARCUIT_TEST", "onCreate reached")
        setContentView(R.layout.activity_main)

        circuitDetector = CircuitDetector(this)

        previewView = findViewById(R.id.previewView)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val overlayView = findViewById<OverlayView>(R.id.overlayView)

        val icComponents = listOf(
            ICComponent(
                id = "ic1",
                boundingBox = RectF(200f, 300f, 600f, 700f)
            )
        )

        overlayView.setICBodies(icComponents)

        val testObjects = listOf(
            DetectedObject(
                id = "w1",
                type = ObjectType.WIRE_ENDPOINT,
                left = 100f, top = 300f, right = 120f, bottom = 320f
            ),
            DetectedObject(
                id = "ic1",
                type = ObjectType.IC_BODY,
                left = 80f, top = 250f, right = 300f, bottom = 450f
            ),
            DetectedObject(
                id = "led1",
                type = ObjectType.LED,
                left = 100f, top = 500f, right = 120f, bottom = 520f
            )
        )

        val connections = ConnectionBuilder.build(testObjects)

        for (c in connections) {
            Log.d("ARCUIT_LOGIC", "${c.fromId} -> ${c.toId}")
        }

        overlayView.listener = object : OverlayView.ICClickListener {
            override fun onICClicked(ic: ICComponent) {
                showGateChooserDialog(ic)
            }
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this)
            ) { imageProxy ->

                val bitmap = imageProxy.toBitmap()
                val detections = circuitDetector.detect(bitmap)

                val overlayView = findViewById<OverlayView>(R.id.overlayView)

// Map model boxes (640x640) → PreviewView size
                val scaleX = overlayView.width / 640f
                val scaleY = overlayView.height / 640f

                val boxes = detections.map {
                    BoundingBox(
                        left = it.boundingBox.left * scaleX,
                        top = it.boundingBox.top * scaleY,
                        right = it.boundingBox.right * scaleX,
                        bottom = it.boundingBox.bottom * scaleY,
                        label = it.label
                    )
                }

                overlayView.setBoxes(boxes)

                Log.d("ARCUIT_ML", "Boxes drawn: ${boxes.size}")

                imageProxy.close()

            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun showGateChooserDialog(ic: ICComponent) {
        val gates = GateType.values().map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Gate Type")
            .setItems(gates) { _, which ->
                ic.gateType = GateType.values()[which]
                Log.d("ARCUIT_LOGIC", "IC ${ic.id} set to ${ic.gateType}")
                findViewById<OverlayView>(R.id.overlayView).invalidate()
            }
            .show()
    }

}
