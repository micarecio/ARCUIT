package com.sd.arcuit

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.Button

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle

import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.sd.arcuit.detector.CircuitDetector
import com.sd.arcuit.logic.ConnectionPoint
import com.sd.arcuit.logic.DetectedObject
import com.sd.arcuit.logic.ICComponent
import com.sd.arcuit.logic.ICPin
import com.sd.arcuit.logic.ICPinMaps
import com.sd.arcuit.logic.Net
import com.sd.arcuit.logic.NetBuilder
import com.sd.arcuit.logic.ObjectType
import com.sd.arcuit.logic.PinConnectionDetector
import com.sd.arcuit.logic.PinRole
import com.sd.arcuit.logic.Node

import com.sd.arcuit.util.toBitmap

import java.util.concurrent.Executors
import kotlin.math.hypot

class MainActivity : AppCompatActivity(), OverlayView.ICClickListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var detector: CircuitDetector

    private lateinit var btnAR2D: FloatingActionButton
    private var isAR2D = false

    private var stableFrames = 0
    private val REQUIRED_STABLE_FRAMES = 8

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var torchEnabled = false

    private val icMap = mutableMapOf<String, ICComponent>()
    private val icLabels = mutableMapOf<String, String>()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var lastAnalyzedTime = 0L

    // 🔒 Persistent electrical points across frames
    private val persistentPoints = mutableListOf<ConnectionPoint>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && !isDestroyed) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

        overlayView = findViewById(R.id.overlayView)
        overlayView.listener = this

        detector = CircuitDetector(this)

        // Torch
        val btnTorch = findViewById<FloatingActionButton>(R.id.btnTorch)
        btnTorch.setOnClickListener { toggleTorch() }

        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)

        // Detection Button
        val btnDetection = findViewById<FloatingActionButton>(R.id.btnDetection)
        btnDetection.setOnClickListener {
            overlayView.setLayer(OverlayView.Layer.BOUNDING_BOX)
            // Optional: highlight active button
            highlightButton(btnDetection)
        }

        // 2D AR Button
        btnAR2D = findViewById(R.id.btnAR2D)
        btnAR2D.setOnClickListener {
            overlayView.setLayer(OverlayView.Layer.AR_2D)
            highlightButton(btnAR2D)
        }

        // Circuit Diagram Button
        val btnCircuit = findViewById<FloatingActionButton>(R.id.btnCircuit)
        btnCircuit.setOnClickListener {
            overlayView.setLayer(OverlayView.Layer.CIRCUIT_DIAGRAM)
            highlightButton(btnCircuit)
        }
    }

    // Optional helper to highlight which layer button is active
    private fun highlightButton(activeButton: FloatingActionButton) {
        val buttons = listOf(
            findViewById<FloatingActionButton>(R.id.btnDetection),
            btnAR2D,
            findViewById<FloatingActionButton>(R.id.btnCircuit)
        )

        buttons.forEach { btn ->
            if (btn == activeButton) {
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00FFCC"))
                btn.elevation = 24f
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1A1A1A"))
                btn.elevation = 12f
            }
        }
    }


    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            if (lifecycle.currentState == Lifecycle.State.DESTROYED) return@addListener

            cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analysisExecutor) { imageProxy ->

                val now = System.currentTimeMillis()
                if (now - lastAnalyzedTime < 100) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                lastAnalyzedTime = now

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

                if (detections.isEmpty()) {
                    stableFrames = 0
                } else {
                    stableFrames++
                }

                if (stableFrames < REQUIRED_STABLE_FRAMES) {
                    imageProxy.close()
                    return@setAnalyzer
                }

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

                // ---------------- IC BODY TRACKING ----------------

                rawBoxes.forEach { box ->
                    if (box.label != "ic_body") {
                        finalBoxes.add(box)
                        return@forEach
                    }

                    val rect = box.toRectF()

                    val frozen = icMap.values.firstOrNull {
                        it.id in icLabels && RectF.intersects(it.boundingBox, rect)
                    }

                    if (frozen != null) {
                        icBodies.add(frozen)
                        return@forEach
                    }

                    val matched = icMap.values.firstOrNull {
                        it.id !in icLabels && RectF.intersects(it.boundingBox, rect)
                    }

                    val ic = if (matched != null) {
                        matched.boundingBox.set(rect)
                        matched
                    } else {
                        val id = "IC_${rect.centerX().toInt()}_${rect.centerY().toInt()}"
                        ICComponent(id, RectF(rect)).also { icMap[id] = it }
                    }

                    icBodies.add(ic)
                    finalBoxes.add(box)
                }

                icMap.values.filter { it.id in icLabels }.forEach {
                    finalBoxes.add(
                        BoundingBox(
                            it.boundingBox.left,
                            it.boundingBox.top,
                            it.boundingBox.right,
                            it.boundingBox.bottom,
                            "ic_body",
                            colorForLabel("ic_body")
                        )
                    )
                    if (!icBodies.contains(it)) icBodies.add(it)
                }

                overlayView.update(finalBoxes, icBodies, icLabels)

                // ---------------- IC PIN GENERATION ----------------
                icBodies.forEach { ic ->

                    val pinCountPerSide = 7
                    val PIN_OFFSET = 6f

                    val width = ic.boundingBox.width()
                    val height = ic.boundingBox.height()

                    val isVertical = height >= width

                    ic.pins.clear()

                    val icType = icLabels[ic.id]
                    val roleMap = ICPinMaps.DIP14[icType]

                    Log.d("IC_ORIENTATION", "IC ${ic.id} vertical=$isVertical")

                    if (isVertical) {

                        val spacing = height / (pinCountPerSide + 1)

                        for (i in 1..pinCountPerSide) {

                            val y = ic.boundingBox.top + spacing * i

                            val leftPoint = ConnectionPoint(
                                "${ic.id}:$i",
                                ic.boundingBox.left - PIN_OFFSET,
                                y
                            )

                            ic.pins.add(
                                ICPin(
                                    index = i,
                                    role = roleMap?.get(i) ?: PinRole.UNKNOWN,
                                    point = leftPoint
                                )
                            )

                            val rightIndex = 15 - i

                            val rightPoint = ConnectionPoint(
                                "${ic.id}:$rightIndex",
                                ic.boundingBox.right + PIN_OFFSET,
                                y
                            )

                            ic.pins.add(
                                ICPin(
                                    index = rightIndex,
                                    role = roleMap?.get(rightIndex) ?: PinRole.UNKNOWN,
                                    point = rightPoint
                                )
                            )
                        }

                    } else {

                        val spacing = width / (pinCountPerSide + 1)

                        for (i in 1..pinCountPerSide) {

                            val x = ic.boundingBox.left + spacing * i

                            val topPoint = ConnectionPoint(
                                "${ic.id}:$i",
                                x,
                                ic.boundingBox.top - PIN_OFFSET
                            )

                            ic.pins.add(
                                ICPin(
                                    index = i,
                                    role = roleMap?.get(i) ?: PinRole.UNKNOWN,
                                    point = topPoint
                                )
                            )

                            val bottomIndex = 15 - i

                            val bottomPoint = ConnectionPoint(
                                "${ic.id}:$bottomIndex",
                                x,
                                ic.boundingBox.bottom + PIN_OFFSET
                            )

                            ic.pins.add(
                                ICPin(
                                    index = bottomIndex,
                                    role = roleMap?.get(bottomIndex) ?: PinRole.UNKNOWN,
                                    point = bottomPoint
                                )
                            )
                        }
                    }

                    Log.d(
                        "IC_PINS",
                        ic.pins.sortedBy { it.index }
                            .joinToString { "${it.index}:${it.role}" }
                    )
                }
                // ---------------- CONNECTION POINTS ----------------

                finalBoxes.forEach { box ->
                    when (box.label) {

                        "wire_endpoint",
                        "pos_rail",
                        "neg_rail",
                        "resistor",
                        "led",
                        "switch",
                        "push_button" -> {

                            val cx = (box.left + box.right) / 2f
                            val cy = (box.top + box.bottom) / 2f

                            findOrCreatePoint(cx, cy, box.label)
                        }
                    }
                }

                // ---------------- NETLIST ----------------

                // 1️⃣ Convert bounding boxes into DetectedObject list (for PinConnectionDetector)
                val detectedObjects = finalBoxes.mapIndexed { index, box ->
                    DetectedObject(
                        id = "OBJ_$index",
                        type = ObjectType.fromLabel(box.label),
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                }

                // 2️⃣ Convert bounding boxes into ConnectionPoints (for NetBuilder graph)
                // 2️⃣ Build connection points list for graph
                val connectionPoints = mutableListOf<ConnectionPoint>()

                // include detected electrical points
                connectionPoints.addAll(persistentPoints)

                // include IC pins (generated each frame)
                icBodies.forEach { ic ->
                    ic.pins.forEach { pin ->
                        connectionPoints.add(pin.point)
                    }
                }

                // 3️⃣ Detect IC pin connections (requires List<DetectedObject>)
                val pinConnections = PinConnectionDetector.detect(icBodies, detectedObjects)

                // 4️⃣ Build electrical nets using the new graph-based NetBuilder

                val nodes = connectionPoints.mapIndexed { index, pt ->
                    Node(
                        id = index,
                        x = pt.x,
                        y = pt.y,
                        type = pt.label
                    )
                }

                val nets = NetBuilder().buildNets(nodes)

                val overlayNets = nets.map { (id, nodeList) ->
                    Net(
                        id = "NET_$id",
                        points = nodeList.map { node ->
                            ConnectionPoint(
                                label = node.type,
                                x = node.x,
                                y = node.y
                            )
                        }.toMutableList()
                    )
                }

                overlayView.setNets(overlayNets)

                overlayNets.forEach { net ->
                    Log.d("NET_DEBUG", "${net.id} -> ${net.points.map { it.label }}")
                }

// ---------------- CIRCUIT ANALYSIS ----------------
                val logicDetections = detections.map {
                    com.sd.arcuit.logic.Detection(
                        it.label,
                        it.confidence,
                        it.boundingBox
                    )
                }

                com.sd.arcuit.logic.CircuitAnalyzer.analyze(logicDetections, icBodies)

                imageProxy.close()
            }

            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
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

        val btn = findViewById<FloatingActionButton>(R.id.btnTorch)

        if (torchEnabled) {
            btn.setImageResource(R.drawable.ic_flash_on)
            btn.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#00FFCC"))
            btn.elevation = 24f

            // Smooth neon scale animation
            btn.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction {
                    btn.animate().scaleX(1f).scaleY(1f).duration = 150
                }
        } else {
            btn.setImageResource(R.drawable.ic_flash_off)
            btn.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#1A1A1A"))
            btn.elevation = 12f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

    override fun onICClicked(ic: ICComponent) {
        if (ic.id in icLabels) {
            AlertDialog.Builder(this)
                .setTitle("Remove Gate?")
                .setPositiveButton("Remove") { _, _ ->
                    icLabels.remove(ic.id)
                    overlayView.postInvalidateOnAnimation()
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val gateTypes = arrayOf("NOT", "AND", "OR", "NAND", "NOR", "XOR", "XNOR")

        AlertDialog.Builder(this)
            .setTitle("Select Gate Type")
            .setItems(gateTypes) { _, which ->
                showICOptions(ic, gateTypes[which])
            }
            .show()

    }

    private fun showICOptions(ic: ICComponent, gate: String) {

        val options = when (gate) {

            "NOT" -> arrayOf("7404")

            "AND" -> arrayOf(
                "7408 – 2 Inputs",
                "7411 – 3 Inputs",
                "7421 – 4 Inputs"
            )

            "OR" -> arrayOf("7432 – 2 Inputs")

            "NAND" -> arrayOf(
                "7400 – 2 Inputs",
                "7410 – 3 Inputs",
                "7420 – 4 Inputs",
                "7430 – 8 Inputs"
            )

            "NOR" -> arrayOf(
                "7402 – 2 Inputs",
                "7427 – 3 Inputs"
            )

            "XOR" -> arrayOf("7486 – 2 Inputs")

            "XNOR" -> arrayOf("74266 – 2 Inputs")

            else -> emptyArray()
        }

        AlertDialog.Builder(this)
            .setTitle("Select IC Model")
            .setItems(options) { _, which ->
                val icCode = options[which].split(" ")[0]
                icLabels[ic.id] = icCode
                Log.d("IC_SELECTED", "IC ${ic.id} -> $icCode")
                overlayView.postInvalidateOnAnimation()
            }
            .show()
    }


    private fun findOrCreatePoint(
        x: Float,
        y: Float,
        label: String
    ): ConnectionPoint {

        if (label == "ic_pin") {
            return ConnectionPoint(label, x, y)
        }

        val existing = persistentPoints.firstOrNull {
            hypot(it.x - x, it.y - y) < 12f
        }

        return if (existing != null) {
            existing.x = x
            existing.y = y
            existing
        } else {
            ConnectionPoint(label, x, y).also {
                persistentPoints.add(it)
            }
        }
    }

}
