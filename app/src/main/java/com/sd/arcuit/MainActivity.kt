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

import org.opencv.android.OpenCVLoader

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

import android.view.ScaleGestureDetector
import android.view.MotionEvent

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

    private lateinit var btnDetection: FloatingActionButton
    private lateinit var btnCircuit: FloatingActionButton

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var torchEnabled = false

    private val icMap = mutableMapOf<String, ICComponent>()
    private val icLabels = mutableMapOf<String, String>()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var lastAnalyzedTime = 0L
    private val ANALYSIS_INTERVAL_MS = 200L

    private var stableWirePairs = mutableMapOf<String, Int>()
    private val STABLE_WIRE_REQUIRED_FRAMES = 3

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && !isDestroyed) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
        setupPinchToZoom()

        overlayView = findViewById(R.id.overlayView)
        overlayView.listener = this

        detector = CircuitDetector(this)

        if (OpenCVLoader.initLocal()) {
            Log.d("OpenCV", "OpenCV loaded successfully")
        } else {
            Log.e("OpenCV", "OpenCV initialization failed")
        }

        // Torch
        val btnTorch = findViewById<FloatingActionButton>(R.id.btnTorch)
        btnTorch.setOnClickListener { toggleTorch() }

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
        val buttons = listOf(btnDetection, btnCircuit)

        buttons.forEach { btn ->
            if (btn == activeButton) {
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00FFCC"))
                btn.imageTintList = ColorStateList.valueOf(Color.BLACK)
                btn.alpha = 1f
                btn.scaleX = 1.15f
                btn.scaleY = 1.15f
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1A1A1A"))
                btn.imageTintList = ColorStateList.valueOf(Color.WHITE)
                btn.alpha = 0.6f
                btn.scaleX = 1f
                btn.scaleY = 1f
            }
        }
    }

    private fun assignWireIdsToEndpoints(
        boxes: List<BoundingBox>,
        bitmap: Bitmap
    ): Map<Int, String> {

        val endpointPairs = boxes.mapIndexedNotNull { index, box ->
            if (box.label == "wire_endpoint") {
                index to com.sd.arcuit.logic.WirePathTracer.Endpoint(
                    id = index,
                    box = RectF(box.left, box.top, box.right, box.bottom)
                )
            } else {
                null
            }
        }

        return try {
            if (endpointPairs.size < 2) return emptyMap()

            val tracedPairs = com.sd.arcuit.logic.WirePathTracer.findConnectedEndpointPairs(
                bitmap = bitmap,
                endpoints = endpointPairs.map { it.second }

            )

            fun findNearestFinalIndex(
                endpoint: com.sd.arcuit.logic.WirePathTracer.Endpoint,
                excludeIndex: Int? = null
            ): Int? {
                val cx = endpoint.box.centerX()
                val cy = endpoint.box.centerY()

                var bestIndex: Int? = null
                var bestDist = Float.MAX_VALUE

                boxes.forEachIndexed { index, box ->
                    if (box.label != "wire_endpoint") return@forEachIndexed
                    if (excludeIndex != null && index == excludeIndex) return@forEachIndexed

                    val bx = (box.left + box.right) / 2f
                    val by = (box.top + box.bottom) / 2f

                    val dx = cx - bx
                    val dy = cy - by
                    val dist = dx * dx + dy * dy

                    if (dist < bestDist) {
                        bestDist = dist
                        bestIndex = index
                    }
                }

                return bestIndex
            }

            val endpointById = endpointPairs.associate { it.first to it.second }

            val result = mutableMapOf<Int, String>()
            var wireCounter = 1
            val currentFramePairs = mutableSetOf<String>()

            val filteredPairs = tracedPairs
                .filter { it.confidence >= 0.80f }
                .sortedByDescending { it.confidence }

            filteredPairs.forEach { pair ->
                val endpointA = endpointById[pair.endpointAId]
                val endpointB = endpointById[pair.endpointBId]

                if (endpointA != null && endpointB != null) {
                    val finalA = findNearestFinalIndex(endpointA)
                    val finalB = findNearestFinalIndex(endpointB, excludeIndex = finalA)

                    if (finalA != null && finalB != null && finalA != finalB) {
                        val smaller = minOf(finalA!!, finalB!!)
                        val bigger = maxOf(finalA, finalB)
                        val pairKey = "$smaller-$bigger"

                        currentFramePairs.add(pairKey)

                        val previousCount = stableWirePairs[pairKey] ?: 0
                        stableWirePairs[pairKey] = previousCount + 1

                        if ((stableWirePairs[pairKey] ?: 0) >= STABLE_WIRE_REQUIRED_FRAMES) {
                            val wireId = "wire_$wireCounter"
                            result[smaller] = wireId
                            result[bigger] = wireId

                            Log.d(
                                "WIRE_TRACE",
                                "STABLE final[$smaller] <-> final[$bigger], color=${pair.colorName}, conf=${pair.confidence}, area=${pair.componentArea}, frames=${stableWirePairs[pairKey]}"
                            )

                            wireCounter++
                        } else {
                            Log.d(
                                "WIRE_TRACE",
                                "WAITING final[$smaller] <-> final[$bigger], frames=${stableWirePairs[pairKey]}"
                            )
                        }
                    } else {
                        Log.d(
                            "WIRE_TRACE",
                            "Skipped pair because mapped endpoints are same or null: A=$finalA B=$finalB"
                        )
                    }
                }
            }

            val keysToUpdate = stableWirePairs.keys.toList()
            keysToUpdate.forEach { key ->
                if (!currentFramePairs.contains(key)) {
                    val newCount = (stableWirePairs[key] ?: 0) - 1
                    if (newCount <= 0) {
                        stableWirePairs.remove(key)
                    } else {
                        stableWirePairs[key] = newCount
                    }
                }
            }

            result
        } catch (e: Exception) {
            Log.e("WIRE_TRACE", "assignWireIdsToEndpoints failed", e)
            emptyMap()
        }
    }

    private fun assignWireIdsToEndpointsFromBitmap(
        endpointPairs: List<Pair<Int, com.sd.arcuit.logic.WirePathTracer.Endpoint>>,
        finalBoxes: List<BoundingBox>,
        bitmap: Bitmap
    ): Map<Int, String> {

        return try {
            if (endpointPairs.size < 2) return emptyMap()

            val tracedPairs = com.sd.arcuit.logic.WirePathTracer.findConnectedEndpointPairs(
                bitmap = bitmap,
                endpoints = endpointPairs.map { it.second }
            )

            Log.d("WIRE_TRACE", "endpointPairs=${endpointPairs.size}, tracedPairs=${tracedPairs.size}")

            data class CandidatePair(
                val a: Int,
                val b: Int,
                val score: Float,
                val confidence: Float,
                val distance: Float,
                val colorName: String
            )

            val candidateMap = mutableMapOf<String, CandidatePair>()

            tracedPairs.forEach { pair ->

                val a = pair.endpointAId
                val b = pair.endpointBId

                if (a == b) return@forEach
                if (a !in finalBoxes.indices || b !in finalBoxes.indices) return@forEach

                val boxA = finalBoxes[a]
                val boxB = finalBoxes[b]

                if (boxA.label != "wire_endpoint" || boxB.label != "wire_endpoint") return@forEach

                val centerAx = (boxA.left + boxA.right) / 2f
                val centerAy = (boxA.top + boxA.bottom) / 2f
                val centerBx = (boxB.left + boxB.right) / 2f
                val centerBy = (boxB.top + boxB.bottom) / 2f

                val dx = kotlin.math.abs(centerAx - centerBx)
                val dy = kotlin.math.abs(centerAy - centerBy)
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                if (distance < 5f || distance > 220f) return@forEach

                val vertical = dx < 35f && dy in 12f..140f
                val horizontal = dy < 20f && dx in 8f..85f
                val diagonal = dx in 8f..60f && dy in 8f..110f

                if (!vertical && !horizontal && !diagonal) return@forEach

                val ratio = if (dx > dy) dy / dx else dx / dy

                if (horizontal && ratio > 0.28f) return@forEach
                if (vertical && ratio > 0.75f) return@forEach
                if (diagonal && (ratio < 0.28f || ratio > 0.85f)) return@forEach

                val alignmentPenalty = if (vertical) dx else dy

                val score =
                    pair.confidence * 1000f -
                            distance * 1.5f -
                            alignmentPenalty * 10f

                val smaller = minOf(a, b)
                val bigger = maxOf(a, b)
                val key = "$smaller-$bigger"

                val candidate = CandidatePair(
                    a = smaller,
                    b = bigger,
                    score = score,
                    confidence = pair.confidence,
                    distance = distance,
                    colorName = pair.colorName
                )

                val existing = candidateMap[key]
                if (existing == null || candidate.score > existing.score) {
                    candidateMap[key] = candidate
                }
            }

            val result = mutableMapOf<Int, String>()
            val usedEndpoints = mutableSetOf<Int>()
            var wireCounter = 1

            candidateMap.values
                .sortedByDescending { it.score }
                .forEach { candidate ->

                    if (candidate.a in usedEndpoints || candidate.b in usedEndpoints) return@forEach

                    val wireId = "wire_$wireCounter"
                    result[candidate.a] = wireId
                    result[candidate.b] = wireId

                    usedEndpoints.add(candidate.a)
                    usedEndpoints.add(candidate.b)

                    Log.d(
                        "WIRE_TRACE",
                        "TRACE final[${candidate.a}] <-> final[${candidate.b}] " +
                                "id=$wireId conf=${candidate.confidence} dist=${candidate.distance} color=${candidate.colorName}"
                    )

                    wireCounter++
                }

            data class EndpointInfo(
                val finalIndex: Int,
                val x: Float,
                val y: Float
            )

            val remaining = endpointPairs
                .map { (finalIndex, endpoint) ->
                    EndpointInfo(
                        finalIndex = finalIndex,
                        x = endpoint.box.centerX(),
                        y = endpoint.box.centerY()
                    )
                }
                .filter { it.finalIndex !in usedEndpoints }

            val fallbackUsed = mutableSetOf<Int>()

            for (i in remaining.indices) {
                if (i in fallbackUsed) continue

                val a = remaining[i]

                var bestJ = -1
                var bestScore = Float.MAX_VALUE

                for (j in remaining.indices) {
                    if (i == j) continue
                    if (j in fallbackUsed) continue

                    val b = remaining[j]

                    val dx = kotlin.math.abs(a.x - b.x)
                    val dy = kotlin.math.abs(a.y - b.y)
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    // vertical fallback only
                    if (dx >= 18f) continue
                    if (dy < 18f || dy > 120f) continue
                    if (distance > 130f) continue

                    val score = distance + dx * 8f

                    if (score < bestScore) {
                        bestScore = score
                        bestJ = j
                    }
                }

                if (bestJ == -1) continue
                if (bestJ in fallbackUsed) continue

                val b = remaining[bestJ]

                // mutual best check
                var reverseBest = -1
                var reverseScore = Float.MAX_VALUE

                for (k in remaining.indices) {
                    if (k == bestJ) continue
                    if (k in fallbackUsed && k != i) continue

                    val c = remaining[k]

                    val dx = kotlin.math.abs(b.x - c.x)
                    val dy = kotlin.math.abs(b.y - c.y)
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (dx >= 18f) continue
                    if (dy < 18f || dy > 120f) continue
                    if (distance > 130f) continue

                    val score = distance + dx * 8f

                    if (score < reverseScore) {
                        reverseScore = score
                        reverseBest = k
                    }
                }

                if (reverseBest != i) continue

                val wireId = "wire_$wireCounter"
                result[a.finalIndex] = wireId
                result[b.finalIndex] = wireId

                usedEndpoints.add(a.finalIndex)
                usedEndpoints.add(b.finalIndex)
                fallbackUsed.add(i)
                fallbackUsed.add(bestJ)

                Log.d(
                    "WIRE_TRACE",
                    "VERTICAL_FALLBACK final[${a.finalIndex}] <-> final[${b.finalIndex}] id=$wireId"
                )

                wireCounter++
            }

            result

        } catch (e: Exception) {
            Log.e("WIRE_TRACE", "assignWireIdsToEndpointsFromBitmap failed", e)
            emptyMap()
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

                val endpointPairs =
                    detections.mapIndexedNotNull { index, det ->
                        if (det.label == "wire_endpoint") {
                            index to com.sd.arcuit.logic.WirePathTracer.Endpoint(
                                id = index,
                                box = RectF(
                                    det.boundingBox.left,
                                    det.boundingBox.top,
                                    det.boundingBox.right,
                                    det.boundingBox.bottom
                                )
                            )
                        } else {
                            null
                        }
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

                val endpointWireIds = assignWireIdsToEndpointsFromBitmap(
                    endpointPairs = endpointPairs,
                    finalBoxes = finalBoxes,
                    bitmap = bitmap
                )

                val wireEndpointSegments = mutableListOf<OverlayView.WireEndpointSegment>()

                val endpointsByWireId = mutableMapOf<String, MutableList<BoundingBox>>()

                finalBoxes.forEachIndexed { index, box ->
                    if (box.label == "wire_endpoint") {
                        val wireId = endpointWireIds[index]
                        if (wireId != null) {
                            endpointsByWireId.getOrPut(wireId) { mutableListOf() }.add(box)
                        }
                    }
                }

                endpointsByWireId.forEach { (_, endpointBoxes) ->

                    val spreadX = endpointBoxes.maxOf { it.left } - endpointBoxes.minOf { it.left }
                    val spreadY = endpointBoxes.maxOf { it.top } - endpointBoxes.minOf { it.top }

                    val sorted = if (spreadX > spreadY) {
                        endpointBoxes.sortedBy { it.left }
                    } else {
                        endpointBoxes.sortedBy { it.top }
                    }

                    for (i in 0 until sorted.size - 1) {

                        val a = sorted[i]
                        val b = sorted[i + 1]

                        val ax = (a.left + a.right) / 2f
                        val ay = (a.top + a.bottom) / 2f
                        val bx = (b.left + b.right) / 2f
                        val by = (b.top + b.bottom) / 2f

                        wireEndpointSegments.add(
                            OverlayView.WireEndpointSegment(
                                startX = ax,
                                startY = ay,
                                endX = bx,
                                endY = by,
                                color = Color.CYAN
                            )
                        )
                    }
                }

                overlayView.setWireEndpointSegments(wireEndpointSegments)

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
                currentZoomRatio = 2.0f.coerceIn(minZoomRatio, maxZoomRatio)
                camera?.cameraControl?.setZoomRatio(currentZoomRatio)
            }

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

            "NOT" -> arrayOf(
                "7404 – NOT",
                "7405 – NOT (OC)",
                "7406 – NOT (OC)",
                "7414 – Schmitt NOT"
            )

            "AND" -> arrayOf(
                "7408 – 2 Inputs",
                "7409 – 2 Inputs (OC)",
                "7411 – 3 Inputs",
                "7421 – 4 Inputs",
                "7422 – 4 Inputs",
                "7440 – 4 Inputs",
                "747001 – Schmitt AND"
            )

            "OR" -> arrayOf(
                "7432 – 2 Inputs",
                "747032 – OR"
            )

            "NAND" -> arrayOf(
                "7400 – 2 Inputs",
                "7401 – 2 Inputs (OC)",
                "7403 – 2 Inputs (OC)",
                "7410 – 3 Inputs",
                "7412 – 3 Inputs (OC)",
                "7413 – 4 Inputs Schmitt",
                "7420 – 4 Inputs",
                "7424 – Schmitt NAND",
                "7426 – OC NAND",
                "7430 – 8 Inputs",
                "7437 – NAND",
                "7438 – NAND (OC)",
                "74132 – Schmitt NAND",
                "74136 – Schmitt NAND"
            )

            "NOR" -> arrayOf(
                "7402 – 2 Inputs",
                "7425 – 4 Inputs",
                "7427 – 3 Inputs",
                "7428 – 2 Inputs",
                "7433 – NOR (OC)",
                "747002 – Schmitt NOR"
            )

            "XOR" -> arrayOf(
                "7486 – 2 Inputs"
            )

            "XNOR" -> arrayOf(
                "74266 – 2 Inputs",
                "747266 – XNOR"
            )

            else -> emptyArray()
        }

        AlertDialog.Builder(this)
            .setTitle("Select IC Model")
            .setItems(options) { _, which ->

                // 🔥 safer extraction (no crash)
                val icCode = options[which].substringBefore(" ")

                icLabels[ic.id] = icCode

                Log.d("IC_SELECTED", "IC ${ic.id} -> $icCode")

                // 🔥 (optional but recommended)
                // refreshCircuitAnalysis()

                overlayView.postInvalidateOnAnimation()
            }
            .show()
    }
}