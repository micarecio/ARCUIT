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
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle

import android.view.animation.DecelerateInterpolator
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
import com.sd.arcuit.logic.PinRole
import com.sd.arcuit.logic.Node

import android.graphics.Typeface
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

import android.view.ScaleGestureDetector
import android.view.MotionEvent
import com.sd.arcuit.logic.TruthTables

import com.sd.arcuit.util.toBitmap

import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), OverlayView.ICClickListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var detector: CircuitDetector

    private var debugFrameCounter = 0

    private var currentZoomRatio = 1.0f
    private var minZoomRatio = 1.0f
    private var maxZoomRatio = 1.0f

    private lateinit var btnTorch: FloatingActionButton

    private lateinit var btnDetection: FloatingActionButton
    private lateinit var btnCircuit: FloatingActionButton

    private lateinit var icSelectorOverlay: FrameLayout
    private lateinit var icSelectorCard: MaterialCardView
    private lateinit var txtIcSelectorTitle: TextView
    private lateinit var gateButtonContainer: LinearLayout
    private lateinit var icModelButtonContainer: LinearLayout

    private lateinit var btnTruthTable: FloatingActionButton
    private lateinit var truthTableOverlay: FrameLayout
    private lateinit var txtTruthTitle: TextView
    private lateinit var txtTruthContent: TextView
    private lateinit var truthTablePanelScroll: View

    private var activeIc: ICComponent? = null

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var torchEnabled = false

    private val icMap = mutableMapOf<String, ICComponent>()
    private val icLabels = mutableMapOf<String, String>()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var lastAnalyzedTime = 0L
    private val ANALYSIS_INTERVAL_MS = 40L

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && !isDestroyed) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        setupPinchToZoom()

        overlayView = findViewById(R.id.overlayView)
        overlayView.listener = this

        detector = CircuitDetector(this)

        // Torch
        btnTorch = findViewById(R.id.btnTorch)
        setTorchButtonActive(false)

        btnTorch.setOnClickListener {
            torchEnabled = !torchEnabled
            camera?.cameraControl?.enableTorch(torchEnabled)

            setTorchButtonActive(torchEnabled)
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)

        // Detection Button
        btnDetection = findViewById(R.id.btnDetection)
        btnDetection.setOnClickListener {
            overlayView.setLayer(OverlayView.Layer.BOUNDING_BOX)
            highlightButton(btnDetection)
        }

        // Circuit Diagram Button
        btnCircuit = findViewById(R.id.btnCircuit)
        btnCircuit.setOnClickListener {
            overlayView.setLayer(OverlayView.Layer.CIRCUIT_DIAGRAM)
            highlightButton(btnCircuit)
        }

        icSelectorOverlay = findViewById(R.id.icSelectorOverlay)
        icSelectorCard = findViewById(R.id.icSelectorCard)
        txtIcSelectorTitle = findViewById(R.id.txtIcSelectorTitle)
        gateButtonContainer = findViewById(R.id.gateButtonContainer)
        icModelButtonContainer = findViewById(R.id.icModelButtonContainer)

        icSelectorCard.setOnClickListener {
            // prevents closing when clicking inside the card
        }

        icSelectorOverlay.setOnClickListener {
            hideIcSelectorWithAnimation()
        }

        btnTruthTable = findViewById(R.id.btnTruthTable)

        btnTruthTable.backgroundTintList = null
        btnTruthTable.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")))
        btnTruthTable.imageTintList = ColorStateList.valueOf(Color.WHITE)

        truthTableOverlay = findViewById(R.id.truthTableOverlay)
        truthTablePanelScroll = findViewById(R.id.truthTablePanelScroll)
        txtTruthTitle = findViewById(R.id.txtTruthTitle)
        txtTruthContent = findViewById(R.id.txtTruthContent)

        btnTruthTable.setOnClickListener {
            val didShow = showTruthTableOverlay()
            setTruthButtonActive(didShow)
        }

        truthTableOverlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideTruthTableWithAnimation()
            }
            true
        }

        val truthTablePanel = findViewById<View>(R.id.truthTablePanel)
        truthTablePanel?.setOnClickListener {
            // prevent closing when clicking panel itself
        }

        overlayView.setLayer(OverlayView.Layer.CIRCUIT_DIAGRAM)
        highlightButton(btnCircuit)
    }


    private fun setupPinchToZoom() {
        val scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val cam = camera ?: return false

                    currentZoomRatio *= detector.scaleFactor
                    currentZoomRatio = currentZoomRatio.coerceIn(minZoomRatio, maxZoomRatio)

                    cam.cameraControl.setZoomRatio(currentZoomRatio)
                    return true
                }
            }
        )

        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun highlightButton(activeButton: FloatingActionButton) {
        val buttons = listOf(btnDetection, btnCircuit, btnTruthTable)

        buttons.forEach { btn ->
            val isActive = btn == activeButton

            btn.backgroundTintList = null
            btn.setBackgroundTintList(
                ColorStateList.valueOf(
                    if (isActive) Color.CYAN else Color.parseColor("#1A1A1A")
                )
            )

            btn.imageTintList = ColorStateList.valueOf(
                if (isActive) Color.BLACK else Color.WHITE
            )

            btn.alpha = if (isActive) 1f else 0.6f

            applyGlow(btn, isActive)
        }
    }

    private fun setTorchButtonActive(isActive: Boolean) {

        btnTorch.backgroundTintList = null
        btnTorch.backgroundTintList = ColorStateList.valueOf(
            if (isActive) Color.CYAN else Color.parseColor("#1A1A1A")
        )

        btnTorch.imageTintList = ColorStateList.valueOf(
            if (isActive) Color.BLACK else Color.WHITE
        )

        btnTorch.alpha = if (isActive) 1f else 0.6f

        btnTorch.setImageResource(
            if (isActive) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )

        applyGlow(btnTorch, isActive)
    }

    private fun setTruthButtonActive(isActive: Boolean) {

        btnTruthTable.backgroundTintList = null
        btnTruthTable.setBackgroundTintList(
            ColorStateList.valueOf(
                if (isActive) Color.CYAN else Color.parseColor("#1A1A1A")
            )
        )

        btnTruthTable.imageTintList = ColorStateList.valueOf(
            if (isActive) Color.BLACK else Color.WHITE
        )

        btnTruthTable.alpha = if (isActive) 1f else 0.6f

        applyGlow(btnTruthTable, isActive)
    }

    private fun applyGlow(btn: FloatingActionButton, isActive: Boolean) {
        if (isActive) {
            btn.elevation = 30f
            btn.translationZ = 30f

            btn.scaleX = 1.18f
            btn.scaleY = 1.18f

            // Optional stronger glow (API 28+)
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                btn.outlineSpotShadowColor = Color.CYAN
                btn.outlineAmbientShadowColor = Color.CYAN
            }

        } else {
            btn.elevation = 12f
            btn.translationZ = 12f

            btn.scaleX = 1f
            btn.scaleY = 1f
        }
    }

    private fun assignWireIdsToEndpoints(boxes: List<BoundingBox>): Map<Int, String> {

        val endpointIndices = boxes.mapIndexedNotNull { index, box ->
            if (box.label == "wire_endpoint") index else null
        }.toMutableList()

        val result = mutableMapOf<Int, String>()
        var wireCounter = 1

        while (endpointIndices.size >= 2) {

            val i = endpointIndices.removeAt(0)
            val a = boxes[i]

            val ax = (a.left + a.right) / 2f
            val ay = (a.top + a.bottom) / 2f

            var bestIndex = -1
            var bestDist = Float.MAX_VALUE
            var bestListIndex = -1

            for ((listIdx, j) in endpointIndices.withIndex()) {

                val b = boxes[j]

                val bx = (b.left + b.right) / 2f
                val by = (b.top + b.bottom) / 2f

                val dx = ax - bx
                val dy = ay - by
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                if (dist < bestDist) {
                    bestDist = dist
                    bestIndex = j
                    bestListIndex = listIdx
                }
            }

            if (bestIndex != -1) {

                val wireId = "wire_$wireCounter"

                result[i] = wireId
                result[bestIndex] = wireId

                endpointIndices.removeAt(bestListIndex)

                wireCounter++
            }
        }

        // 🔥 IMPORTANT: NO endpoint should remain unpaired
        if (endpointIndices.isNotEmpty()) {
            Log.e("WIRE_DEBUG", "UNPAIRED ENDPOINTS: $endpointIndices")
        }

        return result
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            if (lifecycle.currentState == Lifecycle.State.DESTROYED) return@addListener

            cameraProvider = providerFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(previewView.display.rotation)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analysisExecutor) { imageProxy ->

                val now = System.currentTimeMillis()
                if (now - lastAnalyzedTime < ANALYSIS_INTERVAL_MS) {
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

                val viewW = overlayView.width.toFloat()
                val viewH = overlayView.height.toFloat()
                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()

                val scale = maxOf(viewW / imgW, viewH / imgH)
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

                    val matched = icMap.values.firstOrNull {
                        RectF.intersects(it.boundingBox, rect)
                    }

                    val ic = if (matched != null) {

                        // ALWAYS update bounding box
                        matched.boundingBox.set(rect)
                        matched

                    } else {

                        val id = "IC_${rect.centerX().toInt()}_${rect.centerY().toInt()}"

                        ICComponent(id, RectF(rect)).also {
                            icMap[id] = it
                        }
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
                    val PIN_OFFSET = ic.boundingBox.height() * 0.15f

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

                            // LEFT SIDE → 1..7
                            val leftIndex = i
                            val leftPoint = ConnectionPoint(
                                "${ic.id}:$leftIndex",
                                ic.boundingBox.left - PIN_OFFSET,
                                y
                            )

                            ic.pins.add(
                                ICPin(
                                    index = leftIndex,
                                    role = roleMap?.get(leftIndex) ?: PinRole.UNKNOWN,
                                    point = leftPoint
                                )
                            )

                            // RIGHT SIDE → 14..8
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

                        val insetX = width * 0.12f
                        val usableWidth = width - insetX * 2
                        val spacing = usableWidth / (pinCountPerSide - 1)

                        for (i in 0 until pinCountPerSide) {

                            val x = ic.boundingBox.left + insetX + spacing * i

                            // BOTTOM → 1..7
                            val bottomIndex = i + 1

                            val bottomPoint = ConnectionPoint(
                                "${ic.id}:$bottomIndex",
                                x,
                                ic.boundingBox.bottom + height * 0.10f
                            )

                            ic.pins.add(
                                ICPin(
                                    index = bottomIndex,
                                    role = roleMap?.get(bottomIndex) ?: PinRole.UNKNOWN,
                                    point = bottomPoint
                                )
                            )

                            // TOP → 14..8
                            val topIndex = 14 - i

                            val topPoint = ConnectionPoint(
                                "${ic.id}:$topIndex",
                                x,
                                ic.boundingBox.top - height * 0.10f
                            )

                            ic.pins.add(
                                ICPin(
                                    index = topIndex,
                                    role = roleMap?.get(topIndex) ?: PinRole.UNKNOWN,
                                    point = topPoint
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

                val connectionPoints = mutableListOf<ConnectionPoint>()

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

                            connectionPoints.add(
                                ConnectionPoint(
                                    label = box.label,
                                    x = cx,
                                    y = cy
                                )
                            )
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

                // include IC pins for current frame only
                icBodies.forEach { ic ->
                    ic.pins.forEach { pin ->
                        connectionPoints.add(pin.point)
                    }
                }

                // Detect IC pin connections
//                val pinConnections = PinConnectionDetector.detect(icBodies, detectedObjects)

                val endpointWireIds = assignWireIdsToEndpoints(finalBoxes)

// Build electrical nets
                val nodes = mutableListOf<Node>()

// 1️⃣ Add detected components / rails / endpoints only once
                finalBoxes.forEachIndexed { boxIndex, box ->

                    if (box.label == "ic_body") return@forEachIndexed

                    val wireId = if (box.label == "wire_endpoint") {
                        endpointWireIds[boxIndex]
                    } else {
                        null
                    }

                    nodes.add(
                        Node(
                            id = nodes.size,
                            x = (box.left + box.right) / 2f,
                            y = (box.top + box.bottom) / 2f,
                            type = box.label,
                            left = box.left,
                            top = box.top,
                            right = box.right,
                            bottom = box.bottom,
                            wireId = wireId
                        )
                    )
                }

// 2️⃣ Assign nearest rail metadata only after all nodes exist
                val railNodes = nodes.filter { it.type == "pos_rail" || it.type == "neg_rail" }

                nodes.forEach { node ->
                    if (node.type == "wire_endpoint" && railNodes.isNotEmpty()) {
                        val nearestRail = railNodes.minByOrNull { rail ->
                            val dx = node.x - rail.x
                            val dy = node.y - rail.y
                            kotlin.math.sqrt(dx * dx + dy * dy)
                        }

                        if (nearestRail != null) {
                            node.metaRailType = nearestRail.type
                        }
                    }
                }

// 2️⃣ Add IC pins (THIS WAS MISSING)
                icBodies.forEach { ic ->

                    ic.pins.forEach { pin ->

                        nodes.add(
                            Node(
                                id = nodes.size,
                                x = pin.point.x,
                                y = pin.point.y,
                                type = "${ic.id}:${pin.index}",
                                left = pin.point.x - 3f,
                                top = pin.point.y - 3f,
                                right = pin.point.x + 3f,
                                bottom = pin.point.y + 3f
                            )
                        )
                    }
                    ic.pins.sortedBy { it.index }.forEach { pin ->
                        Log.d(
                            "PIN_POS",
                            "IC=${ic.id} pin=${pin.index} x=${pin.point.x} y=${pin.point.y}"
                        )
                    }
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

                debugFrameCounter++
                if (debugFrameCounter % 15 == 0) {
                    overlayNets.forEach { net ->
                        Log.d("NET_DEBUG", "${net.id} -> ${net.points.map { it.label }}")
                    }
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

            camera?.cameraInfo?.zoomState?.value?.let { zoomState ->
                minZoomRatio = zoomState.minZoomRatio
                maxZoomRatio = zoomState.maxZoomRatio
                currentZoomRatio = minZoomRatio
                camera?.cameraControl?.setZoomRatio(currentZoomRatio)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun showTruthTableOverlay(): Boolean {
        val selectedCodes = icLabels.values.distinct()

        if (selectedCodes.isEmpty()) {
            truthTableOverlay.visibility = View.GONE
            return false
        }

        val tables = selectedCodes.mapNotNull { TruthTables.get(it) }

        if (tables.isEmpty()) {
            truthTableOverlay.visibility = View.GONE
            return false
        }

        txtTruthTitle.text = if (tables.size == 1) {
            tables.first().title
        } else {
            "Truth Tables"
        }

        val content = buildString {
            tables.forEachIndexed { tableIndex, table ->
                append(table.title).append("\n\n")

                append(table.headers.joinToString("   "))
                append("\n")
                append("-".repeat(table.headers.size * 4 + 4))
                append("\n")

                table.rows.forEach { row ->
                    val values = row.inputs + row.output
                    append(values.joinToString("   "))
                    append("\n")
                }

                if (tableIndex != tables.lastIndex) {
                    append("\n\n")
                }
            }
        }

        txtTruthContent.text = content
        showTruthTableWithAnimation()
        return true
    }

    private fun showTruthTableWithAnimation() {
        truthTableOverlay.visibility = View.VISIBLE

        truthTablePanelScroll.post {
            truthTablePanelScroll.translationX = -truthTablePanelScroll.width.toFloat()
            truthTablePanelScroll.alpha = 0f

            truthTablePanelScroll.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(260)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun hideTruthTableWithAnimation() {
        truthTablePanelScroll.animate()
            .translationX(-truthTablePanelScroll.width.toFloat())
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                truthTableOverlay.visibility = View.GONE
                truthTablePanelScroll.translationX = 0f
                truthTablePanelScroll.alpha = 1f
                setTruthButtonActive(false)
            }
            .start()
    }

    private fun positionIcSelectorNearIc(ic: ICComponent) {
        icSelectorCard.post {
            val cardWidth = icSelectorCard.width.toFloat()
            val cardHeight = icSelectorCard.height.toFloat()

            val screenWidth = icSelectorOverlay.width.toFloat()
            val screenHeight = icSelectorOverlay.height.toFloat()

            val margin = 20f
            val buttonPanelReservedWidth = 110f

            val icBox = ic.boundingBox

            var targetX = icBox.right + 16f
            var targetY = icBox.top

            // If too close to right edge, place it to the left of the IC
            if (targetX + cardWidth > screenWidth - buttonPanelReservedWidth - margin) {
                targetX = icBox.left - cardWidth - 16f
            }

            // Clamp horizontally
            if (targetX < margin) targetX = margin
            if (targetX + cardWidth > screenWidth - margin) {
                targetX = screenWidth - cardWidth - margin
            }

            // Clamp vertically
            if (targetY < margin) targetY = margin
            if (targetY + cardHeight > screenHeight - margin) {
                targetY = screenHeight - cardHeight - margin
            }

            icSelectorCard.x = targetX
            icSelectorCard.y = targetY
        }
    }

    private fun showIcSelectorWithAnimation(ic: ICComponent) {
        icSelectorOverlay.visibility = View.VISIBLE
        positionIcSelectorNearIc(ic)

        icSelectorCard.post {
            icSelectorCard.translationX = -80f
            icSelectorCard.alpha = 0f

            icSelectorCard.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(260)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun hideIcSelectorWithAnimation() {
        icSelectorCard.animate()
            .translationX(-icSelectorCard.width.toFloat())
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                icSelectorOverlay.visibility = View.GONE
                icSelectorCard.translationX = 0f
                icSelectorCard.alpha = 1f
            }
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

    override fun onICClicked(ic: ICComponent) {
        if (ic.id in icLabels) {
            icLabels.remove(ic.id)
            activeIc = null
            icSelectorOverlay.visibility = View.GONE
            overlayView.postInvalidateOnAnimation()
            return
        }

        activeIc = ic
        truthTableOverlay.visibility = View.GONE
        showGateButtons()
        showIcSelectorWithAnimation(ic)
    }

    private fun showGateButtons() {
        val gates = listOf("NOT", "AND", "OR", "NAND", "NOR", "XOR", "XNOR")

        gateButtonContainer.removeAllViews()
        icModelButtonContainer.removeAllViews()

        gates.forEach { gate ->
            val btn = Button(this)
            btn.text = gate
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#222222"))

            btn.setOnClickListener {
                showICModels(gate)
            }

            gateButtonContainer.addView(btn)
        }
    }

    private fun showICModels(gate: String) {
        val options = when (gate) {
            "NOT" -> listOf("7404", "7405", "7406", "7414")
            "AND" -> listOf("7408", "7411", "7421")
            "OR" -> listOf("7432")
            "NAND" -> listOf("7400", "7410", "7420", "7430")
            "NOR" -> listOf("7402", "7427")
            "XOR" -> listOf("7486")
            "XNOR" -> listOf("74266")
            else -> emptyList()
        }

        icModelButtonContainer.removeAllViews()

        options.forEach { icCode ->
            val btn = Button(this)
            btn.text = icCode
            btn.setTextColor(Color.CYAN)

            btn.setOnClickListener {
                activeIc?.let {
                    icLabels[it.id] = icCode
                    overlayView.postInvalidateOnAnimation()
                    hideIcSelectorWithAnimation()
                }
            }

            icModelButtonContainer.addView(btn)
        }
    }
}