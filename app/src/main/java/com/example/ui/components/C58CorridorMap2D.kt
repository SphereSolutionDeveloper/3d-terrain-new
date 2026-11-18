package com.example.ui.components

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.C58RasterOverlay
import kotlin.math.*

@Composable
fun C58CorridorMap2D(
    bundle: C58CorridorBundle,
    raster: C58RasterOverlay?,
    viewCenterUtmE: Double,
    viewCenterUtmN: Double,
    viewWidthM: Float,
    inspectedCell: C58InspectedCell,
    selectedHouse: C58ExtractedHouse?,
    selectedRoad: C58ExtractedRoad?,
    show1mGrid: Boolean,
    showElevationRaster: Boolean,
    showWater: Boolean,
    showCorridorBounds: Boolean,
    showContours: Boolean,
    showRoads: Boolean,
    showRoadSymbols: Boolean = true,
    showHouses: Boolean,
    showPois: Boolean,
    lakeLevelAslM: Float,
    demSourceLabel: String,
    onPan: (dxPx: Float, dyPx: Float, canvasW: Float) -> Unit,
    onZoom: (factor: Float) -> Unit,
    onTapInspect: (tapX: Float, tapY: Float, canvasW: Float, canvasH: Float) -> Unit,
    onResetView: () -> Unit,
    onSizeChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    hideAllText: Boolean = false,
    onToggleHideAllText: (() -> Unit)? = null
) {
    var canvasWidthPx by remember { mutableFloatStateOf(1f) }
    var canvasHeightPx by remember { mutableFloatStateOf(1f) }

    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E5E7EB")
            textSize = 23f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }
    val subLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#00F0FF")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val kmBadgePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FFD54F")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }
    val symbologyPaints = remember { GisSymbologyPaints() }
    val currentOnPan by rememberUpdatedState(onPan)
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnTapInspect by rememberUpdatedState(onTapInspect)

    // Precompute AABBs once per loaded binary pack for zero-allocation 60 FPS culling
    val houseBounds = remember(bundle.houses) {
        DoubleArray(bundle.houses.size * 4).also { arr ->
            for (i in bundle.houses.indices) {
                val poly = bundle.houses[i].polygon
                var minHe = Double.MAX_VALUE
                var maxHe = -Double.MAX_VALUE
                var minHn = Double.MAX_VALUE
                var maxHn = -Double.MAX_VALUE
                for (pt in poly) {
                    if (pt.utmEast < minHe) minHe = pt.utmEast
                    if (pt.utmEast > maxHe) maxHe = pt.utmEast
                    if (pt.utmNorth < minHn) minHn = pt.utmNorth
                    if (pt.utmNorth > maxHn) maxHn = pt.utmNorth
                }
                val b = i * 4
                arr[b] = minHe
                arr[b + 1] = maxHe
                arr[b + 2] = minHn
                arr[b + 3] = maxHn
            }
        }
    }
    val roadBounds = remember(bundle.roads) {
        DoubleArray(bundle.roads.size * 4).also { arr ->
            for (i in bundle.roads.indices) {
                val nodes = bundle.roads[i].nodes
                var minRe = Double.MAX_VALUE
                var maxRe = -Double.MAX_VALUE
                var minRn = Double.MAX_VALUE
                var maxRn = -Double.MAX_VALUE
                for (pt in nodes) {
                    if (pt.utmEast < minRe) minRe = pt.utmEast
                    if (pt.utmEast > maxRe) maxRe = pt.utmEast
                    if (pt.utmNorth < minRn) minRn = pt.utmNorth
                    if (pt.utmNorth > maxRn) maxRn = pt.utmNorth
                }
                val b = i * 4
                arr[b] = minRe
                arr[b + 1] = maxRe
                arr[b + 2] = minRn
                arr[b + 3] = maxRn
            }
        }
    }

    val (landmarkPois, infraPois) = remember(bundle.pois) {
        val infraCats = setOf(
            "gate", "bollard", "lift_gate", "toll_booth",
            "crossing", "bus_stop", "traffic_signals", "turning_circle", "mini_roundabout"
        )
        val placeCats = setOf(
            "city", "town", "suburb", "quarter", "village", "neighbourhood", "hamlet", "locality"
        )
        val majorCats = setOf(
            "fuel", "hospital", "clinic", "school", "college", "university",
            "mall", "supermarket", "marketplace", "police", "bank", "bus_station", "station"
        )
        val infra = ArrayList<C58ExtractedPoi>(bundle.pois.size / 2)
        val landmarks = ArrayList<C58ExtractedPoi>(bundle.pois.size)
        for (p in bundle.pois) {
            if (p.category in infraCats) {
                infra.add(p)
            } else {
                landmarks.add(p)
            }
        }
        fun landmarkRank(p: C58ExtractedPoi): Int {
            var score = when (p.category) {
                in placeCats -> 300
                in majorCats -> 200
                else -> 100
            }
            val n = p.name.lowercase()
            if (n.contains("bomas") || n.contains("galleria") || n.contains("maasai") ||
                n.contains("hardy") || n.contains("rongai") || n.contains("kiserian") ||
                n.contains("olorgesailie") || n.contains("magadi")
            ) {
                score += 60
            }
            if (p.name.isBlank()) score -= 80
            return score
        }
        landmarks.sortByDescending { landmarkRank(it) }
        landmarks to infra
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B12))
            .onSizeChanged { sz ->
                canvasWidthPx = sz.width.toFloat().coerceAtLeast(1f)
                canvasHeightPx = sz.height.toFloat().coerceAtLeast(1f)
                onSizeChanged(canvasWidthPx, canvasHeightPx)
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var movedTotal = 0f
                    var multiTouch = false

                    do {
                        val event = awaitPointerEvent()
                        val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollY != 0f) {
                            currentOnZoom(if (scrollY < 0f) 1.25f else 0.80f)
                            event.changes.forEach { it.consume() }
                            multiTouch = true
                        } else {
                            val active = event.changes.filter { it.pressed }
                            if (active.size == 1 && !multiTouch) {
                                val c = active[0]
                                val delta = c.position - c.previousPosition
                                movedTotal += delta.getDistance()
                                if (movedTotal > 6f && delta != Offset.Zero) {
                                    currentOnPan(delta.x, delta.y, size.width.toFloat())
                                    c.consume()
                                }
                            } else if (active.size >= 2) {
                                multiTouch = true
                                val p0 = active[0]
                                val p1 = active[1]
                                val curDist = (p0.position - p1.position).getDistance()
                                val prevDist = (p0.previousPosition - p1.previousPosition).getDistance()
                                if (prevDist > 2f && curDist > 2f) {
                                    val factor = curDist / prevDist
                                    if (abs(factor - 1f) > 0.001f) {
                                        currentOnZoom(factor)
                                    }
                                }
                                val curMid = (p0.position + p1.position) * 0.5f
                                val prevMid = (p0.previousPosition + p1.previousPosition) * 0.5f
                                val panDelta = curMid - prevMid
                                if (panDelta != Offset.Zero) {
                                    currentOnPan(panDelta.x, panDelta.y, size.width.toFloat())
                                }
                                active.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!multiTouch && movedTotal <= 8f) {
                        currentOnTapInspect(downPos.x, downPos.y, size.width.toFloat(), size.height.toFloat())
                    }
                }
            }
            .testTag("c58_2d_map_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            if (cw <= 1f || ch <= 1f) return@Canvas

            val pxPerMeter = cw / viewWidthM
            val viewHeightM = (ch / pxPerMeter).toDouble()
            val minE = viewCenterUtmE - viewWidthM * 0.5
            val maxE = viewCenterUtmE + viewWidthM * 0.5
            val minN = viewCenterUtmN - viewHeightM * 0.5
            val maxN = viewCenterUtmN + viewHeightM * 0.5

            fun toScreenX(utmE: Double): Float = ((utmE - minE) * pxPerMeter).toFloat()
            fun toScreenY(utmN: Double): Float = ((maxN - utmN) * pxPerMeter).toFloat()

            // 1. Non-Box Masked Hypsometric + Hillshade + Bathymetric Raster (4 floats per pixel)
            if (showElevationRaster && raster != null && raster.cols >= 2 && raster.rows >= 2) {
                val rCols = raster.cols
                val rRows = raster.rows
                val cellEM = (raster.maxUtmE - raster.minUtmE) / (rCols - 1)
                val cellNM = (raster.maxUtmN - raster.minUtmN) / (rRows - 1)
                val eSpan = (raster.maxElevM - raster.minElevM).coerceAtLeast(15f)

                for (r in 0 until rRows - 1) {
                    val nBot = raster.minUtmN + r * cellNM
                    val nTop = nBot + cellNM
                    val syTop = toScreenY(nTop)
                    val syBot = toScreenY(nBot)
                    for (c in 0 until rCols - 1) {
                        val eL = raster.minUtmE + c * cellEM
                        val eR = eL + cellEM
                        val sxL = toScreenX(eL)
                        val sxR = toScreenX(eR)

                        val base = (r * rCols + c) * 4
                        val elev = raster.data[base]
                        val hillshade = raster.data[base + 1]
                        val waterDepth = raster.data[base + 2]
                        val insideMask = raster.data[base + 3]

                        if (insideMask > 0.5f) {
                            val cellColor = if (showWater && waterDepth > 0f) {
                                c58BathymetricColor(waterDepth, hillshade)
                            } else {
                                val t = ((elev - raster.minElevM) / eSpan).coerceIn(0f, 1f)
                                c58HypsometricColor(elev, lakeLevelAslM, t, hillshade)
                            }
                            drawRect(
                                color = cellColor,
                                topLeft = Offset(sxL, syTop),
                                size = Size((sxR - sxL) + 0.9f, (syBot - syTop) + 0.9f)
                            )
                        }
                    }
                }
            }

            // 1b. Non-Box 3 km Ribbon Corridor Fill (always keeps swath visible even if DEM raster off)
            val ribbon = bundle.ribbon
            if (ribbon.size >= 2) {
                val swathPath = Path()
                swathPath.moveTo(toScreenX(ribbon[0].leftE), toScreenY(ribbon[0].leftN))
                for (i in 1 until ribbon.size) {
                    swathPath.lineTo(toScreenX(ribbon[i].leftE), toScreenY(ribbon[i].leftN))
                }
                for (i in ribbon.size - 1 downTo 0) {
                    swathPath.lineTo(toScreenX(ribbon[i].rightE), toScreenY(ribbon[i].rightN))
                }
                swathPath.close()
                if (!showElevationRaster) {
                    drawPath(path = swathPath, color = CyberCyan.copy(alpha = 0.08f))
                } else {
                    drawPath(path = swathPath, color = Color.White.copy(alpha = 0.03f))
                }
            }

            // 2. Extracted Water Polygons inside C58 Ribbon
            if (showWater && bundle.waterPolygons.isNotEmpty()) {
                val wPath = Path()
                for (wp in bundle.waterPolygons) {
                    if (wp.polygon.size < 3) continue
                    val minWe = wp.polygon.minOf { it.utmEast }
                    val maxWe = wp.polygon.maxOf { it.utmEast }
                    val minWn = wp.polygon.minOf { it.utmNorth }
                    val maxWn = wp.polygon.maxOf { it.utmNorth }
                    if (maxWe < minE || minWe > maxE || maxWn < minN || minWn > maxN) continue

                    wPath.reset()
                    wPath.moveTo(toScreenX(wp.polygon[0].utmEast), toScreenY(wp.polygon[0].utmNorth))
                    for (k in 1 until wp.polygon.size) {
                        wPath.lineTo(toScreenX(wp.polygon[k].utmEast), toScreenY(wp.polygon[k].utmNorth))
                    }
                    wPath.close()
                    val isSalt = wp.waterType.contains("salt") || wp.waterType.contains("mud")
                    drawPath(wPath, if (isSalt) Color(0xFF26C6DA).copy(alpha = 0.28f) else Color(0xFF0288D1).copy(alpha = 0.34f))
                    drawPath(wPath, Color(0xFF00E5FF).copy(alpha = 0.78f), style = Stroke(width = 1.8f))
                }
            }

            // 3. Topographic Contour Isolines (strictly clipped to insideCorridor == 1)
            if ((showContours || showWater) && raster != null && raster.cols >= 2 && raster.rows >= 2) {
                if (showContours) {
                    val contourStep = when {
                        viewWidthM <= 60f -> 0.5f
                        viewWidthM <= 200f -> 1.0f
                        viewWidthM <= 1000f -> 5.0f
                        viewWidthM <= 4500f -> 10.0f
                        else -> 25.0f
                    }
                    drawCorridorContours(raster, contourStep, ::toScreenX, ::toScreenY)
                }
                if (showWater) {
                    drawCorridorSingleLevel(raster, lakeLevelAslM, Color(0xFF00F0FF), 2.5f, ::toScreenX, ::toScreenY)
                }
            }

            // 4. Adaptive UTM Metric Grid (scales seamlessly down to true 1m x 1m cells!)
            if (show1mGrid) {
                val gridStepM: Double = when {
                    pxPerMeter >= 7.5f -> 1.0
                    pxPerMeter >= 2.2f -> 5.0
                    pxPerMeter >= 0.8f -> 25.0
                    pxPerMeter >= 0.22f -> 100.0
                    pxPerMeter >= 0.05f -> 500.0
                    else -> 2000.0
                }

                val startE = floor(minE / gridStepM) * gridStepM
                val endE = ceil(maxE / gridStepM) * gridStepM
                val startN = floor(minN / gridStepM) * gridStepM
                val endN = ceil(maxN / gridStepM) * gridStepM

                var ge = startE
                var lastXLabelPx = -999f
                while (ge <= endE + 0.01) {
                    val sx = toScreenX(ge)
                    val isMajor = gridStepM == 1.0 && (ge.roundToLong() % 10L == 0L)
                    val color = when {
                        gridStepM == 1.0 && isMajor -> CyberCyan.copy(alpha = 0.44f)
                        gridStepM == 1.0 -> Color.White.copy(alpha = 0.22f)
                        else -> Color.White.copy(alpha = 0.13f)
                    }
                    drawLine(color, Offset(sx, 0f), Offset(sx, ch), strokeWidth = if (isMajor) 1.5f else 0.8f)
                    val clearHudX = sx > 280f
                    if (!hideAllText && (isMajor || gridStepM >= 500.0) && clearHudX && sx in 12f..(cw - 92f) && sx - lastXLabelPx >= 120f) {
                        drawContext.canvas.nativeCanvas.drawText("${ge.roundToLong()}E", sx + 4f, 22f, subLabelPaint)
                        lastXLabelPx = sx
                    }
                    ge += gridStepM
                }

                var gn = startN
                var lastYLabelPx = -999f
                while (gn <= endN + 0.01) {
                    val sy = toScreenY(gn)
                    val isMajor = gridStepM == 1.0 && (gn.roundToLong() % 10L == 0L)
                    val color = when {
                        gridStepM == 1.0 && isMajor -> CyberCyan.copy(alpha = 0.44f)
                        gridStepM == 1.0 -> Color.White.copy(alpha = 0.22f)
                        else -> Color.White.copy(alpha = 0.13f)
                    }
                    drawLine(color, Offset(0f, sy), Offset(cw, sy), strokeWidth = if (isMajor) 1.5f else 0.8f)
                    val clearHudY = sy in 90f..(ch - 56f)
                    if (!hideAllText && (isMajor || gridStepM >= 500.0) && clearHudY && abs(sy - lastYLabelPx) >= 64f) {
                        drawContext.canvas.nativeCanvas.drawText("${gn.roundToLong()}N", 8f, sy - 4f, subLabelPaint)
                        lastYLabelPx = sy
                    }
                    gn += gridStepM
                }
            }

            // 5. Curved ±1.5 km Non-Box Corridor Boundaries + Centerline Guide (under roads & buildings)
            if (showCorridorBounds && ribbon.size >= 2) {
                val leftPath = Path()
                val rightPath = Path()
                val centerPath = Path()

                leftPath.moveTo(toScreenX(ribbon[0].leftE), toScreenY(ribbon[0].leftN))
                rightPath.moveTo(toScreenX(ribbon[0].rightE), toScreenY(ribbon[0].rightN))
                centerPath.moveTo(toScreenX(ribbon[0].centerE), toScreenY(ribbon[0].centerN))
                for (i in 1 until ribbon.size) {
                    leftPath.lineTo(toScreenX(ribbon[i].leftE), toScreenY(ribbon[i].leftN))
                    rightPath.lineTo(toScreenX(ribbon[i].rightE), toScreenY(ribbon[i].rightN))
                    centerPath.lineTo(toScreenX(ribbon[i].centerE), toScreenY(ribbon[i].centerN))
                }

                // Connect South & North end caps so non-box polygon closes cleanly
                drawLine(
                    color = CyberCyan.copy(alpha = 0.65f),
                    start = Offset(toScreenX(ribbon.first().leftE), toScreenY(ribbon.first().leftN)),
                    end = Offset(toScreenX(ribbon.first().rightE), toScreenY(ribbon.first().rightN)),
                    strokeWidth = 2.2f
                )
                drawLine(
                    color = CyberCyan.copy(alpha = 0.65f),
                    start = Offset(toScreenX(ribbon.last().leftE), toScreenY(ribbon.last().leftN)),
                    end = Offset(toScreenX(ribbon.last().rightE), toScreenY(ribbon.last().rightN)),
                    strokeWidth = 2.2f
                )

                // LEFT -1.5 km edge (Cyan) & RIGHT +1.5 km edge (Orange)
                drawPath(leftPath, CyberCyan.copy(alpha = 0.85f), style = Stroke(width = 2.6f, cap = StrokeCap.Round))
                drawPath(rightPath, LaserOrange.copy(alpha = 0.85f), style = Stroke(width = 2.6f, cap = StrokeCap.Round))

                // Subtle dashed C58 Centerline Reference Guide under OSM road geometry
                drawPath(
                    centerPath,
                    Color(0xFFFFD54F).copy(alpha = 0.45f),
                    style = Stroke(
                        width = 2.0f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
            }

            // 6. Extracted OSM Houses / Buildings inside ±1.5 km Non-Box Corridor (Scale-Adaptive & 60 FPS Culled)
            if (showHouses && bundle.houses.isNotEmpty()) {
                val hPath = Path()
                val visibleHouseIdx = ArrayList<Int>(512)
                for (i in bundle.houses.indices) {
                    val h = bundle.houses[i]
                    if (h.polygon.size < 3) continue
                    val b = i * 4
                    val minHe = houseBounds[b]
                    val maxHe = houseBounds[b + 1]
                    val minHn = houseBounds[b + 2]
                    val maxHn = houseBounds[b + 3]
                    if (maxHe < minE || minHe > maxE || maxHn < minN || minHn > maxN) continue
                    visibleHouseIdx.add(i)
                }

                // Stride only in ultra-dense wide urban overviews (>2200 visible houses above 5km); draw 100% below 5km!
                val houseStride = if (viewWidthM > 5000f && visibleHouseIdx.size > 2200) {
                    (visibleHouseIdx.size / 1800).coerceAtLeast(2)
                } else 1

                for (vi in visibleHouseIdx.indices) {
                    val i = visibleHouseIdx[vi]
                    val h = bundle.houses[i]
                    val isSel = selectedHouse?.osmId == h.osmId
                    if (houseStride > 1 && (vi % houseStride != 0) && !isSel) continue

                    val isLeft = h.centroid.lateralOffsetM < 0f
                    val fillColor = when {
                        isSel -> NeonLime.copy(alpha = 0.85f)
                        isLeft -> CyberCyan.copy(alpha = 0.64f)
                        else -> LaserOrange.copy(alpha = 0.66f)
                    }
                    val strokeColor = when {
                        isSel -> Color.White
                        isLeft -> Color(0xFF80F8FF)
                        else -> Color(0xFFFFD180)
                    }
                    val screenPoly = ArrayList<Offset>(h.polygon.size)
                    for (pt in h.polygon) {
                        screenPoly.add(Offset(toScreenX(pt.utmEast), toScreenY(pt.utmNorth)))
                    }
                    val centerScreen = Offset(toScreenX(h.centroid.utmEast), toScreenY(h.centroid.utmNorth))
                    drawScaleAdaptiveBuilding(
                        screenPoly = screenPoly,
                        centerScreen = centerScreen,
                        fillColor = fillColor,
                        strokeColor = strokeColor,
                        isSelected = isSel,
                        viewWidthM = viewWidthM,
                        pathBuffer = hPath
                    )

                    if (!hideAllText && viewWidthM <= 280f) {
                        val hLabel = h.name.take(18)
                        symbologyPaints.labelTextPaint.textSize = if (viewWidthM <= 90f) 23f else 18f
                        symbologyPaints.labelHaloPaint.textSize = symbologyPaints.labelTextPaint.textSize
                        drawContext.canvas.nativeCanvas.drawText(
                            hLabel,
                            centerScreen.x - 24f,
                            centerScreen.y,
                            symbologyPaints.labelHaloPaint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            hLabel,
                            centerScreen.x - 24f,
                            centerScreen.y,
                            symbologyPaints.labelTextPaint
                        )
                    }
                }
            }

            // 7. Extracted OSM Roads inside ±1.5 km Corridor + Shields, Labels, Bridges, Roundabouts & Arrows
            val placedLabelBoxes = ArrayList<RectF>()
            if (showRoads && bundle.roads.isNotEmpty()) {
                val rPath = Path()
                for (i in bundle.roads.indices) {
                    val road = bundle.roads[i]
                    if (road.nodes.size < 2) continue
                    val b = i * 4
                    val minRe = roadBounds[b]
                    val maxRe = roadBounds[b + 1]
                    val minRn = roadBounds[b + 2]
                    val maxRn = roadBounds[b + 3]
                    if (maxRe < minE || minRe > maxE || maxRn < minN || minRn > maxN) continue

                    val isSel = selectedRoad?.osmId == road.osmId
                    val isMajorRoad = road.highwayType in setOf(
                        "trunk", "trunk_link", "primary", "primary_link",
                        "secondary", "secondary_link", "tertiary", "tertiary_link"
                    )
                    // Keep all major arterials + bridges/refs across all scales; only filter tiny minor alleys at full 78km corridor view
                    if (viewWidthM > 36_000f && !isMajorRoad && !road.isBridge && !road.isRoundabout && road.ref.isBlank() && !isSel && road.lengthM < 180f) continue

                    val strokePx = max(2.0f, road.widthM * pxPerMeter * 0.78f).coerceAtMost(46f)
                    val screenPts = ArrayList<Offset>(road.nodes.size)
                    for (node in road.nodes) {
                        screenPts.add(Offset(toScreenX(node.utmEast), toScreenY(node.utmNorth)))
                    }
                    drawCartographicRoadWithSymbols(
                        pts = screenPts,
                        name = road.name,
                        ref = road.ref,
                        highwayType = road.highwayType,
                        lanes = road.lanes,
                        oneWay = road.oneWay,
                        isBridge = road.isBridge,
                        isRoundabout = road.isRoundabout,
                        isSelected = isSel,
                        strokePx = strokePx,
                        viewWidthM = viewWidthM,
                        showSymbols = showRoadSymbols,
                        paints = symbologyPaints,
                        placedBoxes = placedLabelBoxes,
                        pathBuffer = rPath,
                        showTextLabels = !hideAllText
                    )
                }
            }

            // 7b. KM Chainage Badges & Corridor Terminus Markers on Top so Always Legible
            if (!hideAllText && showCorridorBounds && ribbon.size >= 2) {
                fun tryDrawKmBadge(v: C58RibbonVertex, badge: String, dotColor: Color) {
                    val sx = toScreenX(v.centerE)
                    val sy = toScreenY(v.centerN)
                    if (sx !in 8f..(cw - 8f) || sy !in 14f..(ch - 8f)) return
                    val estW = badge.length * 13.5f
                    val tx = if (sx + estW + 14f > cw - 8f) (sx - estW - 10f).coerceAtLeast(8f) else sx + 8f
                    val ty = (sy - 6f).coerceIn(26f, ch - 10f)
                    val rect = RectF(tx - 6f, ty - 22f, tx + estW + 6f, ty + 6f)
                    if (placedLabelBoxes.none { RectF.intersects(it, rect) }) {
                        placedLabelBoxes.add(rect)
                        drawCircle(DarkBackground, radius = 6.5f, center = Offset(sx, sy))
                        drawCircle(dotColor, radius = 4.8f, center = Offset(sx, sy))
                        drawContext.canvas.nativeCanvas.drawText(badge, tx, ty, kmBadgePaint)
                    }
                }

                val totalKm = ribbon.last().chainageM / 1000.0
                tryDrawKmBadge(ribbon.first(), "KM 0 MAGADI", NeonLime)
                tryDrawKmBadge(ribbon.last(), "KM ${"%.1f".format(totalKm)} BOMAS", CyberCyan)

                val kmStep = when {
                    viewWidthM > 28_000f -> 20
                    viewWidthM > 12_000f -> 10
                    else -> 5
                }
                var nextMarkKm = kmStep
                for (v in ribbon) {
                    val kmD = v.chainageM / 1000.0
                    val km = kmD.toInt()
                    if (km >= nextMarkKm && kmD < totalKm - 1.2) {
                        tryDrawKmBadge(v, "KM $km", Color(0xFFFFD54F))
                        nextMarkKm += kmStep
                    }
                }
            }

            // 8. POIs & Road Infrastructure Nodes inside ±1.5 km corridor (priority-sorted + collision-free)
            if (showRoadSymbols && viewWidthM <= 4500f && infraPois.isNotEmpty()) {
                var infraDrawn = 0
                val maxInfraLabels = if (viewWidthM <= 1200f) 24 else 0
                var infraLabeled = 0
                for (poi in infraPois) {
                    if (infraDrawn >= 320) break
                    val px = toScreenX(poi.coord.utmEast)
                    val py = toScreenY(poi.coord.utmNorth)
                    if (px < 10f || px > cw - 10f || py < 20f || py > ch - 10f) continue
                    if (drawRoadInfraNodeSymbol(Offset(px, py), poi.category, symbologyPaints)) {
                        infraDrawn++
                        if (!hideAllText && showPois && infraLabeled < maxInfraLabels && poi.name.isNotBlank()) {
                            val cleanName = poi.name.take(20)
                            val estW = cleanName.length * 10.5f
                            val tx = (px + 9f).coerceAtMost((cw - estW - 8f).coerceAtLeast(8f))
                            val ty = (py + 5f).coerceIn(24f, ch - 8f)
                            val rect = RectF(tx - 6f, ty - 18f, tx + estW + 6f, ty + 6f)
                            if (placedLabelBoxes.none { RectF.intersects(it, rect) }) {
                                placedLabelBoxes.add(rect)
                                symbologyPaints.labelTextPaint.textSize = 18f
                                symbologyPaints.labelHaloPaint.textSize = 18f
                                drawContext.canvas.nativeCanvas.drawText(cleanName, tx, ty, symbologyPaints.labelHaloPaint)
                                drawContext.canvas.nativeCanvas.drawText(cleanName, tx, ty, symbologyPaints.labelTextPaint)
                                infraLabeled++
                            }
                        }
                    }
                }
            }

            if (showPois && landmarkPois.isNotEmpty()) {
                val maxLabels = if (viewWidthM > 4000f) 55 else 85
                var drawnLabels = 0
                labelPaint.textSize = 21f
                for (poi in landmarkPois) {
                    val px = toScreenX(poi.coord.utmEast)
                    val py = toScreenY(poi.coord.utmNorth)
                    if (px < 10f || px > cw - 10f || py < 20f || py > ch - 10f) continue

                    if (hideAllText) {
                        drawCircle(NeonLime.copy(alpha = 0.80f), radius = 3.6f, center = Offset(px, py))
                        continue
                    }

                    val cleanName = poi.name.take(22)
                    val estW = cleanName.length * 12.0f
                    val tx = if (px + estW + 12f > cw - 8f) (px - estW - 8f).coerceAtLeast(8f) else px + 8f
                    val ty = (py + 6f).coerceIn(26f, ch - 8f)
                    val rect = RectF(tx - 10f, ty - 22f, tx + estW + 10f, ty + 8f)
                    val canPlaceLabel = cleanName.isNotBlank() && drawnLabels < maxLabels && placedLabelBoxes.none { RectF.intersects(it, rect) }

                    if (canPlaceLabel) {
                        placedLabelBoxes.add(rect)
                        drawCircle(NeonLime, radius = 4.8f, center = Offset(px, py))
                        drawCircle(DarkBackground, radius = 2.0f, center = Offset(px, py))
                        symbologyPaints.labelTextPaint.textSize = 20f
                        symbologyPaints.labelHaloPaint.textSize = 20f
                        drawContext.canvas.nativeCanvas.drawText(cleanName, tx, ty, symbologyPaints.labelHaloPaint)
                        drawContext.canvas.nativeCanvas.drawText(cleanName, tx, ty, labelPaint)
                        drawnLabels++
                    } else if (viewWidthM <= 4500f) {
                        drawCircle(NeonLime.copy(alpha = 0.72f), radius = 3.2f, center = Offset(px, py))
                    }
                }
            }

            // 9. Inspected 1m x 1m UTM Cell Box & Crosshair
            val cellE = floor(inspectedCell.utmEast)
            val cellN = floor(inspectedCell.utmNorth)
            val cL = toScreenX(cellE)
            val cR = toScreenX(cellE + 1.0)
            val cT = toScreenY(cellN + 1.0)
            val cB = toScreenY(cellN)
            val ix = toScreenX(inspectedCell.utmEast)
            val iy = toScreenY(inspectedCell.utmNorth)

            if (cR - cL >= 3f) {
                drawRect(NeonLime.copy(alpha = 0.35f), Offset(cL, cT), Size(cR - cL, cB - cT))
                drawRect(NeonLime, Offset(cL, cT), Size(cR - cL, cB - cT), style = Stroke(2.2f))
            }
            drawLine(NeonLime, Offset(ix - 14f, iy), Offset(ix + 14f, iy), strokeWidth = 2f)
            drawLine(NeonLime, Offset(ix, iy - 14f), Offset(ix, iy + 14f), strokeWidth = 2f)
            drawCircle(NeonLime, radius = 4f, center = Offset(ix, iy))
        }

        // Top-Left Resolution & Scale HUD
        val pxPerM = canvasWidthPx / viewWidthM
        val isTrue1mMode = viewWidthM <= 115f
        val barMeters = when {
            viewWidthM <= 25f -> 2f
            viewWidthM <= 75f -> 10f
            viewWidthM <= 300f -> 50f
            viewWidthM <= 1200f -> 200f
            viewWidthM <= 6000f -> 1000f
            viewWidthM <= 20000f -> 3000f
            else -> 10000f
        }
        val barPx = (barMeters * pxPerM).coerceIn(28f, 180f)

        if (!hideAllText) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, if (isTrue1mMode) NeonLime else GlowingCyanBorder),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(
                        text = if (isTrue1mMode) {
                            "1m × 1m GRID (${viewWidthM.roundToInt()}m)"
                        } else {
                            "SPAN ${if (viewWidthM >= 1500f) "${"%.1f".format(viewWidthM / 1000f)}km" else "${viewWidthM.roundToInt()}m"}"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTrue1mMode) NeonLime else CyberCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width((barPx / 3f).coerceIn(28f, 90f).dp)
                                .height(3.dp)
                                .background(if (isTrue1mMode) NeonLime else CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (barMeters >= 1000f) "${(barMeters / 1000f).toInt()} km" else "${barMeters.roundToInt()} m"} (${"%.2f".format(pxPerM)} px/m)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Bottom-Left Compact Corridor Legend
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(9.dp, 7.dp).background(CyberCyan))
                    Text("L", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                    Box(modifier = Modifier.size(9.dp, 7.dp).background(Color(0xFFFFD54F)))
                    Text("C58", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFFD54F))
                    Box(modifier = Modifier.size(9.dp, 7.dp).background(LaserOrange))
                    Text("R", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = LaserOrange)
                    Text("• $demSourceLabel", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                }
            }
        }

        // Right-Side 48dp Accessible Zoom + Hide-Text / Fullscreen Strip
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, if (hideAllText) NeonLime else GlowingCyanBorder),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(if (hideAllText) WindowInsets.safeDrawing else WindowInsets(0))
                .padding(end = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                IconButton(
                    onClick = { onZoom(1.50f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("c58_map_zoom_in_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In C58 Map", tint = CyberCyan)
                }
                IconButton(
                    onClick = { onZoom(0.66f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("c58_map_zoom_out_btn")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out C58 Map", tint = CyberCyan)
                }
                IconButton(
                    onClick = onResetView,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("c58_map_reset_btn")
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit Active C58 Sector", tint = TextPrimary)
                }
                if (onToggleHideAllText != null) {
                    HorizontalDivider(
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.width(32.dp)
                    )
                    IconButton(
                        onClick = onToggleHideAllText,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("c58_map_hide_text_btn")
                    ) {
                        Icon(
                            imageVector = if (hideAllText) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (hideAllText) "Show Map Text & Controls" else "Hide All Text for Fullscreen Map",
                            tint = if (hideAllText) NeonLime else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun c58BathymetricColor(depthM: Float, hillshade: Float): Color {
    val dt = (depthM / 4.2f).coerceIn(0f, 1f)
    val mod = (0.86f + 0.14f * hillshade).coerceIn(0.78f, 1.08f)
    val r = ((0.14f * (1f - dt) + 0.02f * dt) * mod).coerceIn(0f, 1f)
    val g = ((0.76f * (1f - dt) + 0.24f * dt) * mod).coerceIn(0f, 1f)
    val b = ((0.88f * (1f - dt) + 0.56f * dt) * mod).coerceIn(0f, 1f)
    return Color(r, g, b)
}

private fun c58HypsometricColor(elevM: Float, lakeLevelAslM: Float, norm: Float, hillshade: Float): Color {
    val (r, g, b) = when {
        elevM <= lakeLevelAslM + 2.0f -> Triple(0.86f, 0.89f, 0.86f) // Trona crust
        elevM < 850f -> Triple(0.60f, 0.55f, 0.36f)                   // Rift floor alkali scrub
        elevM < 1250f -> Triple(0.45f, 0.49f, 0.27f)                  // Olorgesailie / Tinga savannah
        elevM < 1600f -> Triple(0.58f, 0.43f, 0.28f)                  // Kwenia / Escarpment slope
        else -> Triple(0.32f, 0.52f, 0.30f)                           // Highland forest & Bomas ridge
    }
    val mod = (0.72f + 0.32f * norm) * hillshade
    return Color(
        red = (r * mod).coerceIn(0f, 1f),
        green = (g * mod).coerceIn(0f, 1f),
        blue = (b * mod).coerceIn(0f, 1f)
    )
}

private fun DrawScope.drawCorridorSingleLevel(
    raster: C58RasterOverlay,
    targetLevelM: Float,
    lineColor: Color,
    strokeW: Float,
    toScreenX: (Double) -> Float,
    toScreenY: (Double) -> Float
) {
    val cols = raster.cols
    val rows = raster.rows
    val cellEM = (raster.maxUtmE - raster.minUtmE) / (cols - 1)
    val cellNM = (raster.maxUtmN - raster.minUtmN) / (rows - 1)

    for (r in 0 until rows - 1) {
        val n0 = raster.minUtmN + r * cellNM
        val n1 = n0 + cellNM
        for (c in 0 until cols - 1) {
            val m00 = raster.data[(r * cols + c) * 4 + 3]
            val m11 = raster.data[((r + 1) * cols + (c + 1)) * 4 + 3]
            if (m00 < 0.5f && m11 < 0.5f) continue

            val e0 = raster.minUtmE + c * cellEM
            val e1 = e0 + cellEM
            val h00 = raster.data[(r * cols + c) * 4]
            val h10 = raster.data[(r * cols + (c + 1)) * 4]
            val h01 = raster.data[((r + 1) * cols + c) * 4]
            val h11 = raster.data[((r + 1) * cols + (c + 1)) * 4]

            val pts = ArrayList<Offset>(2)
            fun checkEdge(ha: Float, ea: Double, na: Double, hb: Float, eb: Double, nb: Double) {
                if ((ha <= targetLevelM && targetLevelM < hb) || (hb <= targetLevelM && targetLevelM < ha)) {
                    val t = ((targetLevelM - ha) / (hb - ha)).toDouble()
                    pts.add(Offset(toScreenX(ea + t * (eb - ea)), toScreenY(na + t * (nb - na))))
                }
            }
            checkEdge(h00, e0, n0, h10, e1, n0)
            checkEdge(h10, e1, n0, h11, e1, n1)
            checkEdge(h11, e1, n1, h01, e0, n1)
            checkEdge(h01, e0, n1, h00, e0, n0)

            if (pts.size >= 2) {
                drawLine(lineColor, pts[0], pts[1], strokeWidth = strokeW)
            }
        }
    }
}

private fun DrawScope.drawCorridorContours(
    raster: C58RasterOverlay,
    contourStep: Float,
    toScreenX: (Double) -> Float,
    toScreenY: (Double) -> Float
) {
    val cols = raster.cols
    val rows = raster.rows
    val cellEM = (raster.maxUtmE - raster.minUtmE) / (cols - 1)
    val cellNM = (raster.maxUtmN - raster.minUtmN) / (rows - 1)

    val firstLevel = ceil(raster.minElevM / contourStep) * contourStep
    var level = firstLevel
    var guard = 0
    while (level <= raster.maxElevM && guard < 42) {
        guard++
        val isIndex = abs(level - round(level / (contourStep * 5f)) * (contourStep * 5f)) < 0.05f
        val lineColor = if (isIndex) Color(0xFFFFD54F).copy(alpha = 0.70f) else CyberCyan.copy(alpha = 0.34f)
        val strokeW = if (isIndex) 1.7f else 0.9f

        for (r in 0 until rows - 1) {
            val n0 = raster.minUtmN + r * cellNM
            val n1 = n0 + cellNM
            for (c in 0 until cols - 1) {
                val m00 = raster.data[(r * cols + c) * 4 + 3]
                val m11 = raster.data[((r + 1) * cols + (c + 1)) * 4 + 3]
                if (m00 < 0.5f && m11 < 0.5f) continue

                val e0 = raster.minUtmE + c * cellEM
                val e1 = e0 + cellEM
                val h00 = raster.data[(r * cols + c) * 4]
                val h10 = raster.data[(r * cols + (c + 1)) * 4]
                val h01 = raster.data[((r + 1) * cols + c) * 4]
                val h11 = raster.data[((r + 1) * cols + (c + 1)) * 4]

                val pts = ArrayList<Offset>(2)
                fun checkEdge(ha: Float, ea: Double, na: Double, hb: Float, eb: Double, nb: Double) {
                    if ((ha <= level && level < hb) || (hb <= level && level < ha)) {
                        val t = ((level - ha) / (hb - ha)).toDouble()
                        pts.add(Offset(toScreenX(ea + t * (eb - ea)), toScreenY(na + t * (nb - na))))
                    }
                }
                checkEdge(h00, e0, n0, h10, e1, n0)
                checkEdge(h10, e1, n0, h11, e1, n1)
                checkEdge(h11, e1, n1, h01, e0, n1)
                checkEdge(h01, e0, n1, h00, e0, n0)

                if (pts.size >= 2) {
                    drawLine(lineColor, pts[0], pts[1], strokeWidth = strokeW)
                }
            }
        }
        level += contourStep
    }
}
