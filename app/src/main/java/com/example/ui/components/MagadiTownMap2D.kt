package com.example.ui.components

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.viewmodel.MagadiRasterOverlay
import kotlin.math.*

@Composable
fun MagadiTownMap2D(
    bundle: MagadiTownBundle?,
    raster: MagadiRasterOverlay?,
    viewCenterXM: Float,
    viewCenterYM: Float,
    viewWidthM: Float,
    inspectedCell: Inspected1mCell,
    selectedBuilding: ExtractedBuilding?,
    selectedRoad: ExtractedRoadWay?,
    show1mGrid: Boolean,
    showElevationRaster: Boolean,
    showWater: Boolean,
    showContours: Boolean,
    showRoads: Boolean,
    showRoadSymbols: Boolean = true,
    showBuildings: Boolean,
    showPois: Boolean,
    lakeLevelAslM: Float = 602.5f,
    demSourceLabel: String = "~9.5m DEM",
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

    val subLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#00F0FF")
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val symbologyPaints = remember { GisSymbologyPaints() }
    val waterLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#80F8FF")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
        }
    }

    val currentOnPan by rememberUpdatedState(onPan)
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnTapInspect by rememberUpdatedState(onTapInspect)

    data class MagadiBuildingRenderItem(
        val bldg: ExtractedBuilding,
        val style: GisLandmarkStyle,
        val isCompound: Boolean
    )

    data class MagadiLandmarkCandidate(
        val xM: Float,
        val yM: Float,
        val label: String,
        val style: GisLandmarkStyle,
        val isFromPoi: Boolean
    )

    val orderedBuildings = remember(bundle?.buildings) {
        val list = bundle?.buildings ?: emptyList()
        list.map { b ->
            val style = classifyGisLandmark(b.buildingType, b.amenity, b.name)
            val isCompound = b.footprintAreaM2 > 18_000f
            MagadiBuildingRenderItem(b, style, isCompound)
        }.sortedWith(
            compareByDescending<MagadiBuildingRenderItem> { it.isCompound }
                .thenBy { it.style.kind != GisLandmarkKind.GENERIC }
                .thenBy { it.bldg.footprintAreaM2 }
        )
    }

    val (landmarkCandidates, infraPois) = remember(bundle?.pois, orderedBuildings) {
        val infraCats = setOf(
            "gate", "bollard", "lift_gate", "toll_booth",
            "crossing", "bus_stop", "traffic_signals", "turning_circle", "mini_roundabout"
        )
        val pois = bundle?.pois ?: emptyList()
        val infra = ArrayList<ExtractedPoiMetadata>()
        val candidates = ArrayList<MagadiLandmarkCandidate>(pois.size + 64)
        val seenNames = ArrayList<Triple<String, Float, Float>>()

        fun isDupNameNearby(name: String, x: Float, y: Float): Boolean {
            val nl = name.trim().lowercase()
            if (nl.isEmpty()) return false
            return seenNames.any { (sn, sx, sy) -> sn == nl && hypot(sx - x, sy - y) <= 180f }
        }

        for (p in pois) {
            if (p.category in infraCats) {
                infra.add(p)
            } else {
                val style = classifyGisLandmark(p.category, "", p.name)
                candidates.add(
                    MagadiLandmarkCandidate(
                        xM = p.coord.xM,
                        yM = p.coord.yM,
                        label = p.name,
                        style = style,
                        isFromPoi = true
                    )
                )
                if (p.name.isNotBlank()) {
                    seenNames.add(Triple(p.name.trim().lowercase(), p.coord.xM, p.coord.yM))
                }
            }
        }

        for (item in orderedBuildings) {
            val b = item.bldg
            val hasMeaningfulName = b.name.isNotBlank() && !b.name.contains('#') && !b.name.startsWith("Unnamed ")
            if (item.style.kind != GisLandmarkKind.GENERIC || hasMeaningfulName) {
                val label = b.name.trim()
                if (!isDupNameNearby(label, b.centroid.xM, b.centroid.yM)) {
                    candidates.add(
                        MagadiLandmarkCandidate(
                            xM = b.centroid.xM,
                            yM = b.centroid.yM,
                            label = label,
                            style = item.style,
                            isFromPoi = false
                        )
                    )
                    if (label.isNotEmpty()) {
                        seenNames.add(Triple(label.lowercase(), b.centroid.xM, b.centroid.yM))
                    }
                }
            }
        }

        candidates.sortWith(
            compareByDescending<MagadiLandmarkCandidate> { it.style.priority }
                .thenByDescending { it.isFromPoi }
                .thenByDescending { it.label.isNotBlank() && !it.label.contains('#') }
        )
        candidates to infra
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground)
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
            .testTag("magadi_2d_map_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            if (cw <= 1f || ch <= 1f) return@Canvas

            val pxPerMeter = cw / viewWidthM
            val viewHeightM = ch / pxPerMeter
            val minX = viewCenterXM - viewWidthM * 0.5f
            val maxX = viewCenterXM + viewWidthM * 0.5f
            val minY = viewCenterYM - viewHeightM * 0.5f
            val maxY = viewCenterYM + viewHeightM * 0.5f

            fun toScreenX(xM: Float): Float = (xM - minX) * pxPerMeter
            fun toScreenY(yM: Float): Float = (maxY - yM) * pxPerMeter

            val placedLabelBoxes = ArrayList<RectF>()

            // 1. Hypsometric Elevation & Bathymetric Lake Depth Raster Shading (from C++ 1m DEM + NNW Hillshade)
            if (showElevationRaster && raster != null && raster.cols >= 2 && raster.rows >= 2) {
                val rCols = raster.cols
                val rRows = raster.rows
                val cellWM = (raster.maxXM - raster.minXM) / (rCols - 1)
                val cellHM = (raster.maxYM - raster.minYM) / (rRows - 1)
                val eSpan = (raster.maxElevM - raster.minElevM).coerceAtLeast(8f)

                for (r in 0 until rRows - 1) {
                    val yBottom = raster.minYM + r * cellHM
                    val yTop = yBottom + cellHM
                    val syTop = toScreenY(yTop)
                    val syBot = toScreenY(yBottom)
                    for (c in 0 until rCols - 1) {
                        val xLeft = raster.minXM + c * cellWM
                        val xRight = xLeft + cellWM
                        val sxL = toScreenX(xLeft)
                        val sxR = toScreenX(xRight)

                        val idx = (r * rCols + c) * 3
                        val elev = raster.data[idx]
                        val hillshade = raster.data[idx + 1]
                        val waterDepth = raster.data[idx + 2]

                        val color = if (showWater && waterDepth > 0f) {
                            bathymetricWaterColor(waterDepth, hillshade)
                        } else {
                            val t = ((elev - raster.minElevM) / eSpan).coerceIn(0f, 1f)
                            hypsometricColor(elev, lakeLevelAslM, t, hillshade)
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(sxL, syTop),
                            size = Size((sxR - sxL) + 0.8f, (syBot - syTop) + 0.8f)
                        )
                    }
                }
            }

            // 1b. Extracted OSM Lake Magadi Water Polygons & Salt Pans
            val waterLabelTargets = ArrayList<Triple<String, Float, Float>>()
            if (showWater && bundle != null && bundle.waterPolygons.isNotEmpty()) {
                val wPath = Path()
                for (wp in bundle.waterPolygons) {
                    if (wp.polygon.size < 3) continue
                    val minWx = wp.polygon.minOf { it.xM }
                    val maxWx = wp.polygon.maxOf { it.xM }
                    val minWy = wp.polygon.minOf { it.yM }
                    val maxWy = wp.polygon.maxOf { it.yM }
                    if (maxWx < minX || minWx > maxX || maxWy < minY || minWy > maxY) continue

                    wPath.reset()
                    wPath.moveTo(toScreenX(wp.polygon[0].xM), toScreenY(wp.polygon[0].yM))
                    for (k in 1 until wp.polygon.size) {
                        wPath.lineTo(toScreenX(wp.polygon[k].xM), toScreenY(wp.polygon[k].yM))
                    }
                    wPath.close()

                    val isSalt = wp.waterType.contains("salt") || wp.waterType.contains("mud")
                    drawPath(
                        path = wPath,
                        color = if (isSalt) Color(0xFF26C6DA).copy(alpha = 0.24f) else Color(0xFF0288D1).copy(alpha = 0.30f)
                    )
                    drawPath(
                        path = wPath,
                        color = Color(0xFF00E5FF).copy(alpha = 0.78f),
                        style = Stroke(width = 2.0f)
                    )

                    if (!hideAllText && wp.name.isNotBlank() && !wp.name.startsWith("Unnamed") && !wp.name.contains('#')) {
                        val visCx = ((max(minWx, minX) + min(maxWx, maxX)) * 0.5f)
                        val visCy = ((max(minWy, minY) + min(maxWy, maxY)) * 0.5f)
                        waterLabelTargets.add(Triple(wp.name, visCx, visCy))
                    }
                }
            }

            // 2. Topographic Contour Isolines + Exact Lake Level Shoreline Contour
            if ((showContours || showWater) && raster != null && raster.cols >= 2 && raster.rows >= 2) {
                if (showContours) {
                    val contourStep = when {
                        viewWidthM <= 45f -> 0.5f
                        viewWidthM <= 140f -> 1.0f
                        viewWidthM <= 500f -> 2.0f
                        viewWidthM <= 1500f -> 5.0f
                        else -> 10.0f
                    }
                    drawRasterContours(
                        raster = raster,
                        contourStep = contourStep,
                        toScreenX = ::toScreenX,
                        toScreenY = ::toScreenY
                    )
                }
                if (showWater) {
                    drawSingleContourLevel(
                        raster = raster,
                        targetLevelM = lakeLevelAslM,
                        lineColor = Color(0xFF00F0FF),
                        strokeW = 2.8f,
                        toScreenX = ::toScreenX,
                        toScreenY = ::toScreenY
                    )
                }
            }

            // 3. Domain Frame (Magadi Town 3000m x 5000m = 15 km2)
            val domL = toScreenX(0f)
            val domR = toScreenX(3000f)
            val domT = toScreenY(5000f)
            val domB = toScreenY(0f)
            drawRect(
                color = CyberCyan.copy(alpha = 0.70f),
                topLeft = Offset(domL, domT),
                size = Size(domR - domL, domB - domT),
                style = Stroke(width = 2.5f)
            )

            // 4. Adaptive Metric Grid (scales all the way down to true 1m x 1m cells!)
            if (show1mGrid) {
                val gridStepM = when {
                    pxPerMeter >= 7.5f -> 1f      // True 1m x 1m grid!
                    pxPerMeter >= 2.2f -> 5f
                    pxPerMeter >= 0.85f -> 20f
                    pxPerMeter >= 0.30f -> 100f
                    else -> 500f
                }

                val startGX = floor(max(0f, minX) / gridStepM) * gridStepM
                val endGX = ceil(min(3000f, maxX) / gridStepM) * gridStepM
                val startGY = floor(max(0f, minY) / gridStepM) * gridStepM
                val endGY = ceil(min(5000f, maxY) / gridStepM) * gridStepM

                var gx = startGX
                var lastXLabelPx = -999f
                while (gx <= endGX + 0.001f) {
                    val sx = toScreenX(gx)
                    val isMajor = gridStepM == 1f && (gx.roundToInt() % 10 == 0)
                    val lineColor = when {
                        gridStepM == 1f && isMajor -> CyberCyan.copy(alpha = 0.42f)
                        gridStepM == 1f -> Color.White.copy(alpha = 0.22f)
                        else -> Color.White.copy(alpha = 0.16f)
                    }
                    drawLine(
                        color = lineColor,
                        start = Offset(sx, max(0f, domT)),
                        end = Offset(sx, min(ch, domB)),
                        strokeWidth = if (isMajor) 1.5f else 0.9f
                    )
                    if (!hideAllText && (isMajor || gridStepM >= 100f) && sx > 250f && sx in 12f..(cw - 80f) && sx - lastXLabelPx >= 110f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "${gx.roundToInt()}m",
                            sx + 4f,
                            22f,
                            subLabelPaint
                        )
                        lastXLabelPx = sx
                    }
                    gx += gridStepM
                }

                var gy = startGY
                var lastYLabelPx = -999f
                while (gy <= endGY + 0.001f) {
                    val sy = toScreenY(gy)
                    val isMajor = gridStepM == 1f && (gy.roundToInt() % 10 == 0)
                    val lineColor = when {
                        gridStepM == 1f && isMajor -> CyberCyan.copy(alpha = 0.42f)
                        gridStepM == 1f -> Color.White.copy(alpha = 0.22f)
                        else -> Color.White.copy(alpha = 0.16f)
                    }
                    drawLine(
                        color = lineColor,
                        start = Offset(max(0f, domL), sy),
                        end = Offset(min(cw, domR), sy),
                        strokeWidth = if (isMajor) 1.5f else 0.9f
                    )
                    if (!hideAllText && (isMajor || gridStepM >= 100f) && sy in 90f..(ch - 56f) && abs(sy - lastYLabelPx) >= 62f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "${gy.roundToInt()}m",
                            8f,
                            sy - 4f,
                            subLabelPaint
                        )
                        lastYLabelPx = sy
                    }
                    gy += gridStepM
                }
            }

            // 5. Extracted OSM Compounds & Buildings (Compounds drawn first, landmarks tinted by .bin classification)
            if (showBuildings && orderedBuildings.isNotEmpty()) {
                val bPath = Path()
                for (item in orderedBuildings) {
                    val b = item.bldg
                    if (b.polygon.size < 3) continue
                    val minBx = b.polygon.minOf { it.xM }
                    val maxBx = b.polygon.maxOf { it.xM }
                    val minBy = b.polygon.minOf { it.yM }
                    val maxBy = b.polygon.maxOf { it.yM }
                    if (maxBx < minX || minBx > maxX || maxBy < minY || minBy > maxY) continue

                    val isSel = selectedBuilding?.osmId == b.osmId
                    val screenPoly = ArrayList<Offset>(b.polygon.size)
                    for (pt in b.polygon) {
                        screenPoly.add(Offset(toScreenX(pt.xM), toScreenY(pt.yM)))
                    }
                    val centerScreen = Offset(toScreenX(b.centroid.xM), toScreenY(b.centroid.yM))
                    val fillColor = if (isSel) {
                        CyberCyan.copy(alpha = 0.78f)
                    } else if (item.style.kind != GisLandmarkKind.GENERIC) {
                        item.style.fillColor
                    } else {
                        LaserOrange.copy(alpha = 0.62f)
                    }
                    val strokeColor = if (isSel) {
                        Color.White
                    } else if (item.style.kind != GisLandmarkKind.GENERIC) {
                        item.style.strokeColor
                    } else {
                        Color(0xFFFFD180)
                    }
                    drawScaleAdaptiveBuilding(
                        screenPoly = screenPoly,
                        centerScreen = centerScreen,
                        fillColor = fillColor,
                        strokeColor = strokeColor,
                        isSelected = isSel,
                        viewWidthM = viewWidthM,
                        pathBuffer = bPath,
                        isCompoundPerimeter = item.isCompound,
                        roofType = b.roofType,
                        levels = b.levels
                    )

                    if (viewWidthM <= 700f && b.hasFence && b.fencePolygon.size >= 3) {
                        val fPoly = ArrayList<Offset>(b.fencePolygon.size)
                        for (fpt in b.fencePolygon) {
                            fPoly.add(Offset(toScreenX(fpt.xM), toScreenY(fpt.yM)))
                        }
                        val gScreen = if (b.hasGate && b.gateCoord != null) {
                            Offset(toScreenX(b.gateCoord.xM), toScreenY(b.gateCoord.yM))
                        } else null
                        val gWidthPx = (b.gateWidthM * pxPerMeter).coerceAtLeast(5f)
                        drawPerimeterFence2D(
                            fenceScreenPoly = fPoly,
                            fenceType = b.fenceType,
                            isSelected = isSel,
                            gateScreenPos = gScreen,
                            gateWidthPx = gWidthPx,
                            showPosts = (viewWidthM <= 260f)
                        )
                        if (gScreen != null) {
                            drawPropertyGate2D(
                                gateCenter = gScreen,
                                gateAngleRad = b.gateAngleRad,
                                gateWidthPx = gWidthPx,
                                gateType = b.gateType,
                                isSelected = isSel
                            )
                        }
                    }
                }
            }

            // 6. Extracted OSM Roads + Cartographic Symbols (Shields, Labels, Centerlines, Bridges, Roundabouts, Arrows)
            if (showRoads && bundle != null) {
                val rPath = Path()
                for (road in bundle.roads) {
                    if (road.nodes.size < 2) continue
                    val minRx = road.nodes.minOf { it.xM }
                    val maxRx = road.nodes.maxOf { it.xM }
                    val minRy = road.nodes.minOf { it.yM }
                    val maxRy = road.nodes.maxOf { it.yM }
                    if (maxRx < minX || minRx > maxX || maxRy < minY || minRy > maxY) continue

                    val isSel = selectedRoad?.osmId == road.osmId
                    val strokePx = max(1.8f, road.widthM * pxPerMeter * 0.72f).coerceAtMost(46f)
                    val screenPts = ArrayList<Offset>(road.nodes.size)
                    for (node in road.nodes) {
                        screenPts.add(Offset(toScreenX(node.xM), toScreenY(node.yM)))
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
                        showTextLabels = !hideAllText,
                        pxPerMeter = pxPerMeter,
                        roadWidthM = road.widthM,
                        roadLengthM = road.lengthM
                    )
                }
            }

            // 7. Extracted POI Labels, Landmark Compounds, Water Labels & Road Infrastructure Nodes (Collision-Free)
            if (showRoadSymbols && infraPois.isNotEmpty()) {
                val maxInfraLabels = if (viewWidthM <= 700f) 24 else 0
                var infraDrawn = 0
                var infraLabeled = 0
                for (poi in infraPois) {
                    if (infraDrawn >= 140) break
                    val px = toScreenX(poi.coord.xM)
                    val py = toScreenY(poi.coord.yM)
                    if (px < 10f || px > cw - 10f || py < 20f || py > ch - 10f) continue
                    if (drawRoadInfraNodeSymbol(Offset(px, py), poi.category, symbologyPaints, showTextLabels = !hideAllText)) {
                        infraDrawn++
                        if (!hideAllText && showPois && infraLabeled < maxInfraLabels && poi.name.isNotBlank()) {
                            val cleanName = poi.name.take(20)
                            symbologyPaints.labelTextPaint.textSize = 18f
                            symbologyPaints.labelHaloPaint.textSize = 18f
                            val estW = symbologyPaints.labelTextPaint.measureText(cleanName)
                            val tx = (px + 9f).coerceAtMost((cw - estW - 8f).coerceAtLeast(8f))
                            val ty = (py + 5f).coerceIn(22f, ch - 8f)
                            val rect = RectF(tx - 6f, ty - 18f, tx + estW + 6f, ty + 6f)
                            if (placedLabelBoxes.none { RectF.intersects(it, rect) }) {
                                placedLabelBoxes.add(rect)
                                drawContext.canvas.nativeCanvas.drawText(cleanName, tx, ty, symbologyPaints.labelHaloPaint)
                                drawContext.canvas.nativeCanvas.drawText(cleanName, tx, ty, symbologyPaints.labelTextPaint)
                                infraLabeled++
                            }
                        }
                    }
                }
            }

            if ((showPois || showBuildings) && landmarkCandidates.isNotEmpty()) {
                val maxLabels = if (viewWidthM > 1400f) 32 else 65
                var drawnLabels = 0
                val textSz = if (viewWidthM <= 450f) 21f else 19.5f
                for (cand in landmarkCandidates) {
                    if (!cand.isFromPoi && !showBuildings) continue
                    if (cand.isFromPoi && !showPois && cand.style.kind == GisLandmarkKind.GENERIC) continue
                    val px = toScreenX(cand.xM)
                    val py = toScreenY(cand.yM)
                    if (px < 8f || px > cw - 8f || py < 14f || py > ch - 8f) continue

                    val isMajorLandmark = cand.style.kind != GisLandmarkKind.GENERIC
                    val canTryLabel = !hideAllText && drawnLabels < maxLabels &&
                        (isMajorLandmark || cand.isFromPoi || viewWidthM <= 450f)

                    if (canTryLabel) {
                        val placed = drawLandmarkPinAndLabel(
                            center = Offset(px, py),
                            label = cand.label,
                            style = cand.style,
                            paints = symbologyPaints,
                            placedBoxes = placedLabelBoxes,
                            canvasW = cw,
                            canvasH = ch,
                            showTextLabels = true,
                            textSizePx = textSz,
                            drawPinAlways = cand.isFromPoi || isMajorLandmark
                        )
                        if (placed) drawnLabels++
                    } else if (cand.isFromPoi || isMajorLandmark) {
                        drawLandmarkPinAndLabel(
                            center = Offset(px, py),
                            label = cand.label,
                            style = cand.style,
                            paints = symbologyPaints,
                            placedBoxes = placedLabelBoxes,
                            canvasW = cw,
                            canvasH = ch,
                            showTextLabels = false,
                            drawPinAlways = true
                        )
                    }
                }
            }

            // Zoomed-in inner-compound structure roles & individual building labels (<= 340m, respecting hideAllText)
            if (!hideAllText && showBuildings && viewWidthM <= 340f) {
                var genericLabeled = 0
                for (item in orderedBuildings) {
                    if (genericLabeled >= 32) break
                    val b = item.bldg
                    val isInnerChild = b.parentCompoundName.isNotBlank() && !item.isCompound
                    if (!isInnerChild && item.style.kind != GisLandmarkKind.GENERIC) continue
                    val roleLabel = when {
                        isInnerChild -> {
                            val afterBullet = b.name.substringAfter("• ", "").substringBefore(" #").trim()
                            val role = afterBullet.ifBlank { b.buildingType.replace('_', ' ') }
                            if (b.levels > 1) "$role (${b.levels}F)" else role
                        }
                        !b.name.contains('#') && !b.name.startsWith("Unnamed ") -> b.name
                        viewWidthM <= 140f -> {
                            val tLabel = b.buildingType.replace('_', ' ').replaceFirstChar { it.uppercase() }
                            if (b.levels > 1) "$tLabel ${b.levels}F" else tLabel
                        }
                        else -> continue
                    }
                    val cx = toScreenX(b.centroid.xM)
                    val cy = toScreenY(b.centroid.yM)
                    if (drawLandmarkPinAndLabel(
                            center = Offset(cx, cy),
                            label = roleLabel,
                            style = item.style,
                            paints = symbologyPaints,
                            placedBoxes = placedLabelBoxes,
                            canvasW = cw,
                            canvasH = ch,
                            showTextLabels = true,
                            textSizePx = 17.5f,
                            drawPinAlways = isInnerChild
                        )
                    ) {
                        genericLabeled++
                    }
                }
            }

            // Named Water Polygons & Salt Pans from .bin (e.g. Lake Magadi)
            if (!hideAllText && showWater && waterLabelTargets.isNotEmpty()) {
                val drawnWaterNames = HashSet<String>()
                for ((wName, wx, wy) in waterLabelTargets) {
                    if (!drawnWaterNames.add(wName.lowercase())) continue
                    val sx = toScreenX(wx)
                    val sy = toScreenY(wy)
                    if (sx !in 24f..(cw - 24f) || sy !in 24f..(ch - 24f)) continue
                    val clean = wName.take(24)
                    val estW = waterLabelPaint.measureText(clean)
                    val tx = (sx - estW * 0.5f).coerceIn(10f, (cw - estW - 10f).coerceAtLeast(10f))
                    val ty = sy.coerceIn(26f, ch - 12f)
                    val rect = RectF(tx - 8f, ty - 22f, tx + estW + 8f, ty + 6f)
                    if (placedLabelBoxes.none { RectF.intersects(it, rect) }) {
                        placedLabelBoxes.add(rect)
                        symbologyPaints.labelHaloPaint.textSize = 20f
                        drawContext.canvas.nativeCanvas.drawText(clean, tx, ty, symbologyPaints.labelHaloPaint)
                        drawContext.canvas.nativeCanvas.drawText(clean, tx, ty, waterLabelPaint)
                    }
                }
            }

            // 8. Inspected 1m x 1m Cell Highlight Box & Crosshair
            val cellLeftX = toScreenX(inspectedCell.cellCol.toFloat())
            val cellRightX = toScreenX(inspectedCell.cellCol + 1f)
            val cellTopY = toScreenY(inspectedCell.cellRow + 1f)
            val cellBotY = toScreenY(inspectedCell.cellRow.toFloat())
            val ix = toScreenX(inspectedCell.xM)
            val iy = toScreenY(inspectedCell.yM)

            if (cellRightX - cellLeftX >= 3f) {
                drawRect(
                    color = NeonLime.copy(alpha = 0.35f),
                    topLeft = Offset(cellLeftX, cellTopY),
                    size = Size(cellRightX - cellLeftX, cellBotY - cellTopY)
                )
                drawRect(
                    color = NeonLime,
                    topLeft = Offset(cellLeftX, cellTopY),
                    size = Size(cellRightX - cellLeftX, cellBotY - cellTopY),
                    style = Stroke(width = 2.2f)
                )
            }
            drawLine(NeonLime, Offset(ix - 14f, iy), Offset(ix + 14f, iy), strokeWidth = 2f)
            drawLine(NeonLime, Offset(ix, iy - 14f), Offset(ix, iy + 14f), strokeWidth = 2f)
            drawCircle(NeonLime, radius = 4f, center = Offset(ix, iy))
        }

        // Top-Left Resolution & Scale HUD
        val pxPerM = canvasWidthPx / viewWidthM
        val isTrue1mMode = viewWidthM <= 95f
        val barMeters = when {
            viewWidthM <= 20f -> 2f
            viewWidthM <= 65f -> 10f
            viewWidthM <= 250f -> 50f
            viewWidthM <= 800f -> 100f
            else -> 500f
        }
        val barPx = (barMeters * pxPerM).coerceIn(30f, 180f)

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
                            "SPAN ${viewWidthM.roundToInt()}m"
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
                            text = "${barMeters.roundToInt()} m (${"%.1f".format(pxPerM)} px/m)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Bottom-Left Bathymetric Lake Level & Elevation Scale Pill
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
                    Box(modifier = Modifier.size(9.dp, 7.dp).background(Color(0xFF053D8F)))
                    Box(modifier = Modifier.size(9.dp, 7.dp).background(Color(0xFF00B4D8)))
                    Box(modifier = Modifier.size(9.dp, 7.dp).background(Color(0xFFE5EAE6)))
                    Box(modifier = Modifier.size(9.dp, 7.dp).background(Color(0xFF9E6B47)))
                    Text(
                        text = "LAKE ${"%.1f".format(lakeLevelAslM)}m • $demSourceLabel",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
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
                    onClick = { onZoom(1.45f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("magadi_map_zoom_in_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = CyberCyan)
                }
                IconButton(
                    onClick = { onZoom(0.68f) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("magadi_map_zoom_out_btn")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = CyberCyan)
                }
                IconButton(
                    onClick = onResetView,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("magadi_map_reset_btn")
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit Full 3x5 km Town", tint = TextPrimary)
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
                            .testTag("magadi_map_hide_text_btn")
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

private fun bathymetricWaterColor(depthM: Float, hillshade: Float): Color {
    val dt = (depthM / 4.2f).coerceIn(0f, 1f)
    val mod = (0.86f + 0.14f * hillshade).coerceIn(0.78f, 1.08f)
    val r = ((0.14f * (1f - dt) + 0.02f * dt) * mod).coerceIn(0f, 1f)
    val g = ((0.76f * (1f - dt) + 0.24f * dt) * mod).coerceIn(0f, 1f)
    val b = ((0.88f * (1f - dt) + 0.56f * dt) * mod).coerceIn(0f, 1f)
    return Color(r, g, b)
}

private fun hypsometricColor(elevM: Float, lakeLevelAslM: Float, norm: Float, hillshade: Float): Color {
    val (r, g, b) = when {
        elevM <= lakeLevelAslM + 1.5f -> Triple(0.88f, 0.91f, 0.89f) // Shoreline Trona soda crust
        elevM <= lakeLevelAslM + 8.0f -> Triple(0.66f, 0.62f, 0.42f) // Alkali basin terrace
        norm < 0.45f -> Triple(0.44f, 0.49f, 0.28f)                  // Lower Savannah Rift floor
        norm < 0.75f -> Triple(0.62f, 0.43f, 0.28f)                  // Volcanic ash & town bench
        else -> Triple(0.48f, 0.43f, 0.42f)                          // Rift basalt scarp ridge
    }
    return Color(
        red = (r * hillshade).coerceIn(0f, 1f),
        green = (g * hillshade).coerceIn(0f, 1f),
        blue = (b * hillshade).coerceIn(0f, 1f)
    )
}

private fun DrawScope.drawSingleContourLevel(
    raster: MagadiRasterOverlay,
    targetLevelM: Float,
    lineColor: Color,
    strokeW: Float,
    toScreenX: (Float) -> Float,
    toScreenY: (Float) -> Float
) {
    val cols = raster.cols
    val rows = raster.rows
    val cellWM = (raster.maxXM - raster.minXM) / (cols - 1)
    val cellHM = (raster.maxYM - raster.minYM) / (rows - 1)

    for (r in 0 until rows - 1) {
        val y0 = raster.minYM + r * cellHM
        val y1 = y0 + cellHM
        for (c in 0 until cols - 1) {
            val x0 = raster.minXM + c * cellWM
            val x1 = x0 + cellWM

            val e00 = raster.data[(r * cols + c) * 3]
            val e10 = raster.data[(r * cols + (c + 1)) * 3]
            val e01 = raster.data[((r + 1) * cols + c) * 3]
            val e11 = raster.data[((r + 1) * cols + (c + 1)) * 3]

            val pts = ArrayList<Offset>(2)
            fun checkEdge(ea: Float, xa: Float, ya: Float, eb: Float, xb: Float, yb: Float) {
                if ((ea <= targetLevelM && targetLevelM < eb) || (eb <= targetLevelM && targetLevelM < ea)) {
                    val t = (targetLevelM - ea) / (eb - ea)
                    pts.add(Offset(toScreenX(xa + t * (xb - xa)), toScreenY(ya + t * (yb - ya))))
                }
            }
            checkEdge(e00, x0, y0, e10, x1, y0)
            checkEdge(e10, x1, y0, e11, x1, y1)
            checkEdge(e11, x1, y1, e01, x0, y1)
            checkEdge(e01, x0, y1, e00, x0, y0)

            if (pts.size >= 2) {
                drawLine(lineColor, pts[0], pts[1], strokeWidth = strokeW)
            }
        }
    }
}

private fun DrawScope.drawRasterContours(
    raster: MagadiRasterOverlay,
    contourStep: Float,
    toScreenX: (Float) -> Float,
    toScreenY: (Float) -> Float
) {
    val cols = raster.cols
    val rows = raster.rows
    val cellWM = (raster.maxXM - raster.minXM) / (cols - 1)
    val cellHM = (raster.maxYM - raster.minYM) / (rows - 1)

    val firstLevel = ceil(raster.minElevM / contourStep) * contourStep
    var h = firstLevel
    var guard = 0
    while (h <= raster.maxElevM && guard < 40) {
        guard++
        val isIndex = abs(h - round(h / (contourStep * 5f)) * (contourStep * 5f)) < 0.05f
        val lineColor = if (isIndex) Color(0xFFFFD54F).copy(alpha = 0.72f) else CyberCyan.copy(alpha = 0.36f)
        val strokeW = if (isIndex) 1.8f else 0.9f

        for (r in 0 until rows - 1) {
            val y0 = raster.minYM + r * cellHM
            val y1 = y0 + cellHM
            for (c in 0 until cols - 1) {
                val x0 = raster.minXM + c * cellWM
                val x1 = x0 + cellWM

                val e00 = raster.data[(r * cols + c) * 3]
                val e10 = raster.data[(r * cols + (c + 1)) * 3]
                val e01 = raster.data[((r + 1) * cols + c) * 3]
                val e11 = raster.data[((r + 1) * cols + (c + 1)) * 3]

                val pts = ArrayList<Offset>(2)
                fun checkEdge(ea: Float, xa: Float, ya: Float, eb: Float, xb: Float, yb: Float) {
                    if ((ea <= h && h < eb) || (eb <= h && h < ea)) {
                        val t = (h - ea) / (eb - ea)
                        pts.add(Offset(toScreenX(xa + t * (xb - xa)), toScreenY(ya + t * (yb - ya))))
                    }
                }
                checkEdge(e00, x0, y0, e10, x1, y0)
                checkEdge(e10, x1, y0, e11, x1, y1)
                checkEdge(e11, x1, y1, e01, x0, y1)
                checkEdge(e01, x0, y1, e00, x0, y0)

                if (pts.size >= 2) {
                    drawLine(lineColor, pts[0], pts[1], strokeWidth = strokeW)
                }
            }
        }
        h += contourStep
    }
}
