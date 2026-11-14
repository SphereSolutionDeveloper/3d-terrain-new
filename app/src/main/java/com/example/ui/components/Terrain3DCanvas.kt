package com.example.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TerrainBuilding3D
import com.example.model.TerrainMesh3D
import com.example.model.TerrainRenderMode
import com.example.model.TerrainRoadSeg3D
import com.example.ui.theme.*
import kotlin.math.*

/**
 * High-Precision 3D Procedural Terrain Canvas
 * Uses true Orbit Camera perspective projection with painter's algorithm depth sorting
 * and solid geological bedrock cross-section foundation.
 */
@Composable
fun Terrain3DCanvas(
    mesh: TerrainMesh3D,
    renderMode: TerrainRenderMode,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    lightAngleRad: Float,
    isWireframeOverlay: Boolean = false,
    showGridContours: Boolean = true,
    contourInterval: Float = 25f,
    isHumanView: Boolean = false,
    is1mResolution: Boolean = false,
    walkDistanceM: Float = 0f,
    selectedRoadDistanceM: Float = 0f,
    showGroundPiece: Boolean = true,
    groundPieceDatumASL: Float = 400.0f,
    groundPieceUnitsPerMeter: Float = 0.12f,
    lakeLevelAslM: Float? = null,
    overlayBuildings3D: List<TerrainBuilding3D> = emptyList(),
    overlayRoadSegments3D: List<TerrainRoadSeg3D> = emptyList(),
    onRotateDrag: (Float, Float) -> Unit,
    onPanDrag: (Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollY != 0f) {
                            val scrollFactor = if (scrollY < 0f) 1.22f else 0.82f
                            onZoomChange(scrollFactor)
                            event.changes.forEach { it.consume() }
                        } else {
                            val activePointers = event.changes.filter { it.pressed }
                            if (activePointers.size == 1) {
                                val change = activePointers[0]
                                val dragDelta = change.position - change.previousPosition
                                if (dragDelta != Offset.Zero) {
                                    onRotateDrag(dragDelta.x, dragDelta.y)
                                    change.consume()
                                }
                            } else if (activePointers.size >= 2) {
                                val p0 = activePointers[0]
                                val p1 = activePointers[1]
                                val currentDist = (p0.position - p1.position).getDistance()
                                val prevDist = (p0.previousPosition - p1.previousPosition).getDistance()
                                if (prevDist > 1f && currentDist > 1f) {
                                    val scaleFactor = currentDist / prevDist
                                    if (abs(scaleFactor - 1f) > 0.0005f) {
                                        onZoomChange(scaleFactor)
                                    }
                                }
                                val currentCentroid = (p0.position + p1.position) / 2f
                                val prevCentroid = (p0.previousPosition + p1.previousPosition) / 2f
                                val panDelta = currentCentroid - prevCentroid
                                if (panDelta != Offset.Zero) {
                                    onPanDrag(panDelta.x, panDelta.y)
                                }
                                activePointers.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag("terrain_3d_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            if (canvasW <= 0f || canvasH <= 0f) return@Canvas

            val centerX = canvasW * 0.5f + panX
            val centerY = canvasH * 0.52f + panY

            // In Human Viewpoint mode: Draw atmospheric East African Rift Valley sky and distant escarpment silhouette
            if (isHumanView) {
                // 1. Deep atmospheric sky gradient
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3C72), // Zenith deep blue
                            Color(0xFF2A5298), // Mid sky
                            Color(0xFF87CEEB), // Atmospheric cyan haze
                            Color(0xFFE8D7C3), // Dust haze layer
                            Color(0xFFD49B6A)  // Magadi soda flat horizon glow
                        ),
                        startY = 0f,
                        endY = canvasH * 0.60f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(canvasW, canvasH * 0.60f)
                )

                // 2. Distant Ngong Hills / Corner Baridi mountain escarpment silhouette (+1,382m above Magadi basin)
                val horizonY = centerY - 28f
                val mountainPath = Path().apply {
                    moveTo(0f, horizonY + 18f)
                    lineTo(canvasW * 0.12f, horizonY - 14f)
                    lineTo(canvasW * 0.28f, horizonY - 26f) // Olorgesailie volcanic shoulder
                    lineTo(canvasW * 0.44f, horizonY - 48f) // Corner Baridi pass (1,780m)
                    lineTo(canvasW * 0.58f, horizonY - 65f) // Ngong Hills crest (1,980m - 2,460m)
                    lineTo(canvasW * 0.72f, horizonY - 52f) // Ngong second peak
                    lineTo(canvasW * 0.86f, horizonY - 30f) // Rift western wall step
                    lineTo(canvasW, horizonY - 12f)
                    lineTo(canvasW, horizonY + 30f)
                    lineTo(0f, horizonY + 30f)
                    close()
                }
                drawPath(
                    path = mountainPath,
                    color = Color(0xFF282338).copy(alpha = 0.88f) // Deep indigo-purple atmospheric haze
                )

                // 3. Desert heat shimmer band across the horizon
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFFF3E0).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = horizonY - 8f,
                        endY = horizonY + 12f
                    ),
                    topLeft = Offset(0f, horizonY - 8f),
                    size = androidx.compose.ui.geometry.Size(canvasW, 20f)
                )
            }

            val cosY = cos(yaw)
            val sinY = sin(yaw)
            val cosP = cos(pitch)
            val sinP = sin(pitch)

            // Directional Light Vector
            val lx = cos(lightAngleRad)
            val ly = 1.15f
            val lz = sin(lightAngleRad)
            val lLen = sqrt(lx * lx + ly * ly + lz * lz).coerceAtLeast(0.001f)
            val nlx = lx / lLen
            val nly = ly / lLen
            val nlz = lz / lLen

            val verts = mesh.vertices
            val count = verts.size
            if (count == 0) return@Canvas

            val resX = mesh.resolutionX.coerceAtLeast(2)
            val resZ = mesh.resolutionZ.coerceAtLeast(2)

            val minY = verts.minOfOrNull { it.y } ?: 0f
            val maxY = verts.maxOfOrNull { it.y } ?: 0f
            val midH = (minY + maxY) * 0.5f
            val baseLevelH = minY - (maxY - minY).coerceAtLeast(4f) * 0.15f

            // Unrestricted camera perspective: stable orbit distance prevents near-plane clipping
            val cameraDist = 260f
            val focal = canvasW.coerceAtMost(canvasH) * 2.1f
            // Dynamic scale factor directly drives effective focal length without artificial clamps
            val effectiveFocal = focal * zoom

            // 1. Project all terrain vertices
            val projX = FloatArray(count)
            val projY = FloatArray(count)
            val eyeZ = FloatArray(count)

            // Bedrock base projected vertices (directly below each terrain vertex)
            val baseProjX = FloatArray(count)
            val baseProjY = FloatArray(count)
            val baseEyeZ = FloatArray(count)

            // Geological Foundation Datum in Scene Y Units
            val hasRealElevation = verts.any { it.rawElevation > 0f }
            val safeUnitsPerM = groundPieceUnitsPerMeter.coerceAtLeast(0.01f)
            val baseRelY: Float = if (hasRealElevation) {
                val sample = verts.first { it.rawElevation > 0f }
                (sample.y - midH) - (sample.rawElevation - groundPieceDatumASL) * safeUnitsPerM
            } else {
                minY - midH - 18.0f
            }

            // Horizontal Lake Water Level in Scene Y Units (when enabled)
            val lakeRelY: Float? = if (lakeLevelAslM != null && hasRealElevation) {
                val sample = verts.first { it.rawElevation > 0f }
                (sample.y - midH) + (lakeLevelAslM - sample.rawElevation) * safeUnitsPerM
            } else null

            val hasWaterPlane = lakeRelY != null
            val waterProjX = if (hasWaterPlane) FloatArray(count) else FloatArray(0)
            val waterProjY = if (hasWaterPlane) FloatArray(count) else FloatArray(0)
            val waterEyeZ = if (hasWaterPlane) FloatArray(count) else FloatArray(0)

            for (i in 0 until count) {
                val v = verts[i]
                val x0 = v.x
                val y0 = v.y - midH
                val z0 = v.z

                // Terrain surface vertex: Y-rotation (yaw)
                val rx = x0 * cosY - z0 * sinY
                val ry = y0
                val rz = x0 * sinY + z0 * cosY

                // Pitch rotation around X (viewing down at landscape)
                val cy = ry * cosP + rz * sinP
                val cz = -ry * sinP + rz * cosP
                val ez = (cameraDist + cz).coerceAtLeast(8f)

                projX[i] = centerX + (rx / ez) * effectiveFocal
                projY[i] = centerY - (cy / ez) * effectiveFocal
                eyeZ[i] = ez

                // Bedrock foundation vertex at datum
                val brx = rx
                val bry = baseRelY
                val brz = rz

                val bcy = bry * cosP + brz * sinP
                val bcz = -bry * sinP + brz * cosP
                val bez = (cameraDist + bcz).coerceAtLeast(8f)

                baseProjX[i] = centerX + (brx / bez) * effectiveFocal
                baseProjY[i] = centerY - (bcy / bez) * effectiveFocal
                baseEyeZ[i] = bez

                // Lake water level vertex
                if (lakeRelY != null) {
                    val wcy = lakeRelY * cosP + rz * sinP
                    val wcz = -lakeRelY * sinP + rz * cosP
                    val wez = (cameraDist + wcz).coerceAtLeast(8f)
                    waterProjX[i] = centerX + (rx / wez) * effectiveFocal
                    waterProjY[i] = centerY - (wcy / wez) * effectiveFocal
                    waterEyeZ[i] = wez
                }
            }

            // 2. Build list of all renderable faces: terrain surface triangles + water quads + solid ground piece skirts
            val indices = mesh.indices
            val triCount = indices.size / 3
            val skirtFaceCount = if (showGroundPiece) (resX + resZ) * 2 + 1 else 0
            val renderList = ArrayList<RenderFace>(triCount + skirtFaceCount + if (hasWaterPlane) triCount / 2 else 0)

            // Add all terrain surface triangles
            for (t in 0 until triCount) {
                val i0 = indices[t * 3]
                val i1 = indices[t * 3 + 1]
                val i2 = indices[t * 3 + 2]
                val avgDist = (eyeZ[i0] + eyeZ[i1] + eyeZ[i2]) / 3f
                renderList.add(RenderFace.TerrainTri(t, i0, i1, i2, avgDist))
            }

            // Add translucent Lake Magadi Water Sheet quads where basin lies at or below lakeLevelAslM
            if (lakeLevelAslM != null && hasWaterPlane) {
                for (iz in 0 until (resZ - 1)) {
                    for (ix in 0 until (resX - 1)) {
                        val tl = iz * resX + ix
                        val tr = tl + 1
                        val bl = (iz + 1) * resX + ix
                        val br = bl + 1
                        val vTL = verts[tl]; val vTR = verts[tr]
                        val vBL = verts[bl]; val vBR = verts[br]

                        val tlSub = vTL.isWaterSurface || vTL.rawElevation < lakeLevelAslM
                        val trSub = vTR.isWaterSurface || vTR.rawElevation < lakeLevelAslM
                        val blSub = vBL.isWaterSurface || vBL.rawElevation < lakeLevelAslM
                        val brSub = vBR.isWaterSurface || vBR.rawElevation < lakeLevelAslM

                        if (tlSub || trSub || blSub || brSub) {
                            val dSum = (lakeLevelAslM - vTL.rawElevation).coerceAtLeast(0f) +
                                (lakeLevelAslM - vTR.rawElevation).coerceAtLeast(0f) +
                                (lakeLevelAslM - vBL.rawElevation).coerceAtLeast(0f) +
                                (lakeLevelAslM - vBR.rawElevation).coerceAtLeast(0f)
                            val avgDepth = (dSum * 0.25f).coerceAtLeast(0.15f)
                            val wDist = (waterEyeZ[tl] + waterEyeZ[tr] + waterEyeZ[br] + waterEyeZ[bl]) * 0.25f - 0.08f
                            val isShoreEdge = !(tlSub && trSub && blSub && brSub)
                            renderList.add(RenderFace.WaterQuad(tl, tr, br, bl, avgDepth, isShoreEdge, wDist))
                        }
                    }
                }
            }

            // Add 4 vertical perimeter walls & bottom slab down to geological datum
            if (showGroundPiece) {
                // South Skirt Wall (KM Start / iz = 0)
                for (ix in 0 until (resX - 1)) {
                    val iA = 0 * resX + ix
                    val iB = 0 * resX + (ix + 1)
                    val avgDist = (eyeZ[iA] + eyeZ[iB] + baseEyeZ[iA] + baseEyeZ[iB]) * 0.25f
                    renderList.add(RenderFace.SkirtQuad(iA, iB, 0f, 0f, -1f, WallType.SOUTH, avgDist))
                }

                // North Skirt Wall (KM End / iz = resZ - 1)
                for (ix in 0 until (resX - 1)) {
                    val iA = (resZ - 1) * resX + ix
                    val iB = (resZ - 1) * resX + (ix + 1)
                    val avgDist = (eyeZ[iA] + eyeZ[iB] + baseEyeZ[iA] + baseEyeZ[iB]) * 0.25f
                    renderList.add(RenderFace.SkirtQuad(iA, iB, 0f, 0f, 1f, WallType.NORTH, avgDist))
                }

                // West Skirt Wall (Left Boundary / ix = 0)
                for (iz in 0 until (resZ - 1)) {
                    val iA = iz * resX + 0
                    val iB = (iz + 1) * resX + 0
                    val avgDist = (eyeZ[iA] + eyeZ[iB] + baseEyeZ[iA] + baseEyeZ[iB]) * 0.25f
                    renderList.add(RenderFace.SkirtQuad(iA, iB, -1f, 0f, 0f, WallType.WEST, avgDist))
                }

                // East Skirt Wall (Right Boundary / ix = resX - 1)
                for (iz in 0 until (resZ - 1)) {
                    val iA = iz * resX + (resX - 1)
                    val iB = (iz + 1) * resX + (resX - 1)
                    val avgDist = (eyeZ[iA] + eyeZ[iB] + baseEyeZ[iA] + baseEyeZ[iB]) * 0.25f
                    renderList.add(RenderFace.SkirtQuad(iA, iB, 1f, 0f, 0f, WallType.EAST, avgDist))
                }

                // Basal Foundation Plate (Flat horizontal plane at datum)
                val cSW = 0
                val cSE = resX - 1
                val cNE = (resZ - 1) * resX + (resX - 1)
                val cNW = (resZ - 1) * resX
                val slabDist = (baseEyeZ[cSW] + baseEyeZ[cSE] + baseEyeZ[cNE] + baseEyeZ[cNW]) * 0.25f
                renderList.add(RenderFace.BottomSlab(cSW, cSE, cNE, cNW, slabDist))
            }

            // 3. Painter's Algorithm: Sort faces from farthest to nearest (DESCENDING DISTANCE)
            renderList.sortByDescending { it.sortDist }

            val path = Path()

            for (face in renderList) {
                when (face) {
                    is RenderFace.TerrainTri -> {
                        val i0 = face.i0
                        val i1 = face.i1
                        val i2 = face.i2

                        val v0 = verts[i0]
                        val v1 = verts[i1]
                        val v2 = verts[i2]

                        // Lighting calculation (bold valley/scarp contrast)
                        val avgNx = (v0.nx + v1.nx + v2.nx) / 3f
                        val avgNy = (v0.ny + v1.ny + v2.ny) / 3f
                        val avgNz = (v0.nz + v1.nz + v2.nz) / 3f
                        val dot = (avgNx * nlx + avgNy * nly + avgNz * nlz).coerceIn(0.08f, 1.0f)
                        val diffuse = 0.30f + 0.70f * dot

                        val avgY = (v0.y + v1.y + v2.y) / 3f
                        val avgSlope = (v0.slopeDeg + v1.slopeDeg + v2.slopeDeg) / 3f
                        val avgGrass = (v0.grassWeight + v1.grassWeight + v2.grassWeight) / 3f
                        val avgDirt = (v0.dirtWeight + v1.dirtWeight + v2.dirtWeight) / 3f
                        val avgRock = (v0.rockWeight + v1.rockWeight + v2.rockWeight) / 3f
                        val avgSnow = (v0.snowWeight + v1.snowWeight + v2.snowWeight) / 3f

                        val isCave = v0.isCaveHollow || v1.isCaveHollow || v2.isCaveHollow
                        val isPureRoad = v0.isRoadSurface && v1.isRoadSurface && v2.isRoadSurface
                        val isRoadTransition = (v0.isRoadSurface || v1.isRoadSurface || v2.isRoadSurface) && !isPureRoad
                        val isWater = v0.isWaterSurface || v1.isWaterSurface || v2.isWaterSurface
                        val isMud = v0.isMudSurface || v1.isMudSurface || v2.isMudSurface
                        val isGravel = v0.isGravelSurface || v1.isGravelSurface || v2.isGravelSurface
                        val bedDepthT = if (lakeLevelAslM != null) {
                            val avgRaw = (v0.rawElevation + v1.rawElevation + v2.rawElevation) / 3f
                            ((lakeLevelAslM - avgRaw) / 4.5f).coerceIn(0f, 1f)
                        } else 0.35f

                        val faceColor: Color = when (renderMode) {
                            TerrainRenderMode.SHADED_TRIPLANAR -> {
                                if (isWater) {
                                    Color(
                                        red = (0.14f - 0.10f * bedDepthT) * diffuse,
                                        green = (0.48f - 0.24f * bedDepthT) * diffuse + 0.06f,
                                        blue = (0.64f - 0.20f * bedDepthT) * diffuse
                                    )
                                } else if (isPureRoad) {
                                    // Engineered asphalt pavement deck
                                    val r = (0.24f + 0.12f * diffuse).coerceIn(0f, 1f)
                                    val g = (0.26f + 0.12f * diffuse).coerceIn(0f, 1f)
                                    val b = (0.30f + 0.12f * diffuse).coerceIn(0f, 1f)
                                    Color(r, g, b)
                                } else if (isRoadTransition) {
                                    // Engineered roadbed cut/fill gravel verge and embankment
                                    val r = (0.52f * diffuse + 0.08f).coerceIn(0f, 1f)
                                    val g = (0.44f * diffuse + 0.06f).coerceIn(0f, 1f)
                                    val b = (0.35f * diffuse + 0.04f).coerceIn(0f, 1f)
                                    Color(r, g, b)
                                } else if (isMud) {
                                    Color(
                                        red = (0.78f * diffuse).coerceIn(0f, 1f),
                                        green = (0.72f * diffuse).coerceIn(0f, 1f),
                                        blue = (0.65f * diffuse).coerceIn(0f, 1f)
                                    )
                                } else if (isGravel) {
                                    Color(
                                        red = (0.55f * diffuse).coerceIn(0f, 1f),
                                        green = (0.48f * diffuse).coerceIn(0f, 1f),
                                        blue = (0.40f * diffuse).coerceIn(0f, 1f)
                                    )
                                } else {
                                    // East African Rift Valley biome palette
                                    val r = (0.80f * avgGrass + 0.65f * avgDirt + 0.32f * avgRock + 0.95f * avgSnow) * diffuse
                                    val g = (0.70f * avgGrass + 0.46f * avgDirt + 0.30f * avgRock + 0.97f * avgSnow) * diffuse
                                    val b = (0.42f * avgGrass + 0.32f * avgDirt + 0.32f * avgRock + 1.00f * avgSnow) * diffuse
                                    Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
                                }
                            }
                            TerrainRenderMode.CONTOURS_3D_ELEVATION,
                            TerrainRenderMode.HYPSOMETRIC_CONTOURS -> {
                                if (isWater) {
                                    Color(
                                        red = (0.10f - 0.08f * bedDepthT) * diffuse,
                                        green = (0.42f - 0.22f * bedDepthT) * diffuse + 0.06f,
                                        blue = (0.62f - 0.18f * bedDepthT) * diffuse
                                    )
                                } else if (isPureRoad) {
                                    val r = (0.24f + 0.10f * diffuse).coerceIn(0f, 1f)
                                    val g = (0.26f + 0.10f * diffuse).coerceIn(0f, 1f)
                                    val b = (0.30f + 0.10f * diffuse).coerceIn(0f, 1f)
                                    Color(r, g, b)
                                } else if (isRoadTransition) {
                                    val r = (0.48f * diffuse + 0.08f).coerceIn(0f, 1f)
                                    val g = (0.42f * diffuse + 0.06f).coerceIn(0f, 1f)
                                    val b = (0.34f * diffuse + 0.04f).coerceIn(0f, 1f)
                                    Color(r, g, b)
                                } else if (isGravel) {
                                    Color(0xFF607D8B).copy(
                                        red = 0.40f * diffuse,
                                        green = 0.44f * diffuse,
                                        blue = 0.48f * diffuse
                                    )
                                } else {
                                    val avgRawE = if (v0.rawElevation > 0f) {
                                        (v0.rawElevation + v1.rawElevation + v2.rawElevation) / 3f
                                    } else {
                                        590f + ((avgY - mesh.minHeight) / (mesh.maxHeight - mesh.minHeight).coerceAtLeast(1f)) * 1390f
                                    }
                                    val t = ((avgRawE - 590f) / 1390f).coerceIn(0f, 1f)
                                    val br: Float
                                    val bg: Float
                                    val bb: Float
                                    if (t < 0.12f) {
                                        // Lake Magadi Trona soda flats
                                        br = 0.92f; bg = 0.96f; bb = 0.98f
                                    } else if (t < 0.35f) {
                                        // Savannah scrub
                                        br = 0.82f; bg = 0.74f; bb = 0.45f
                                    } else if (t < 0.65f) {
                                        // Volcanic ash & Tinga
                                        br = 0.70f; bg = 0.46f; bb = 0.32f
                                    } else if (t < 0.88f) {
                                        // Highland green
                                        br = 0.30f; bg = 0.52f; bb = 0.26f
                                    } else {
                                        // Escarpment rock summit
                                        br = 0.42f; bg = 0.38f; bb = 0.36f
                                    }
                                    Color((br * diffuse).coerceIn(0f, 1f), (bg * diffuse).coerceIn(0f, 1f), (bb * diffuse).coerceIn(0f, 1f))
                                }
                            }
                            TerrainRenderMode.HEIGHTMAP_HEATMAP -> {
                                val hNorm = ((avgY - mesh.minHeight) / (mesh.maxHeight - mesh.minHeight).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                val r = (sin(hNorm * 3.14159f) * 0.95f).coerceIn(0f, 1f)
                                val g = (sin((hNorm + 0.25f) * 3.14159f) * 0.85f).coerceIn(0f, 1f)
                                val b = (cos(hNorm * 3.14159f * 0.5f) * 0.95f).coerceIn(0f, 1f)
                                Color(r * diffuse, g * diffuse, b * diffuse)
                            }
                            TerrainRenderMode.WIREFRAME_CDLOD -> {
                                Color(0xFF0F1E2E).copy(alpha = 0.88f)
                            }
                            TerrainRenderMode.SLOPE_ANALYSIS -> {
                                val sNorm = (avgSlope / 55f).coerceIn(0f, 1f)
                                val r = (sNorm * 1.4f).coerceIn(0f, 1f)
                                val g = ((1f - sNorm) * 1.3f).coerceIn(0f, 1f)
                                Color(r * diffuse, g * diffuse, 0.18f)
                            }
                            TerrainRenderMode.CAVE_VOXEL_SLICE -> {
                                if (isCave) Color(0xFFFF0055).copy(alpha = 0.92f) else Color(0xFF1E293B).copy(alpha = 0.75f)
                            }
                            TerrainRenderMode.ROAD_CUT_EMBANKMENT -> {
                                if (isPureRoad) Color(0xFFFFB300)
                                else if (isRoadTransition) Color(0xFFFF7043).copy(alpha = 0.85f)
                                else Color(0xFF263238).copy(alpha = 0.8f)
                            }
                        }

                        path.reset()
                        path.moveTo(projX[i0], projY[i0])
                        path.lineTo(projX[i1], projY[i1])
                        path.lineTo(projX[i2], projY[i2])
                        path.close()

                        drawPath(path, color = faceColor)

                        // 3D Topographic Contour Isolines
                        if (showGridContours || renderMode == TerrainRenderMode.CONTOURS_3D_ELEVATION || renderMode == TerrainRenderMode.HYPSOMETRIC_CONTOURS) {
                            val e0 = if (v0.rawElevation > 0f) v0.rawElevation else 590f + ((v0.y - mesh.minHeight) / (mesh.maxHeight - mesh.minHeight).coerceAtLeast(1f)) * 1390f
                            val e1 = if (v1.rawElevation > 0f) v1.rawElevation else 590f + ((v1.y - mesh.minHeight) / (mesh.maxHeight - mesh.minHeight).coerceAtLeast(1f)) * 1390f
                            val e2 = if (v2.rawElevation > 0f) v2.rawElevation else 590f + ((v2.y - mesh.minHeight) / (mesh.maxHeight - mesh.minHeight).coerceAtLeast(1f)) * 1390f

                            val minE = minOf(e0, minOf(e1, e2))
                            val maxE = maxOf(e0, maxOf(e1, e2))

                            val cStep = contourInterval.coerceAtLeast(5f)
                            var hc = ceil(minE / cStep) * cStep
                            while (hc <= maxE) {
                                var pCount = 0
                                var xA = 0f; var yA = 0f
                                var xB = 0f; var yB = 0f

                                if ((e0 <= hc && hc < e1) || (e1 <= hc && hc < e0)) {
                                    val tEdge = (hc - e0) / (e1 - e0)
                                    xA = projX[i0] + tEdge * (projX[i1] - projX[i0])
                                    yA = projY[i0] + tEdge * (projY[i1] - projY[i0])
                                    pCount++
                                }
                                if ((e1 <= hc && hc < e2) || (e2 <= hc && hc < e1)) {
                                    val tEdge = (hc - e1) / (e2 - e1)
                                    val px = projX[i1] + tEdge * (projX[i2] - projX[i1])
                                    val py = projY[i1] + tEdge * (projY[i2] - projY[i1])
                                    if (pCount == 0) {
                                        xA = px; yA = py; pCount++
                                    } else {
                                        xB = px; yB = py; pCount++
                                    }
                                }
                                if (pCount < 2 && ((e2 <= hc && hc < e0) || (e0 <= hc && hc < e2))) {
                                    val tEdge = (hc - e2) / (e0 - e2)
                                    val px = projX[i2] + tEdge * (projX[i0] - projX[i2])
                                    val py = projY[i2] + tEdge * (projY[i0] - projY[i2])
                                    if (pCount == 0) {
                                        xA = px; yA = py; pCount++
                                    } else {
                                        xB = px; yB = py; pCount++
                                    }
                                }

                                if (pCount == 2) {
                                    val isIndexContour = (abs(round(hc / 100f) * 100f - hc) < 1.0f)
                                    val lineColor = if (isIndexContour) Color(0xFFFFD700) else Color(0xFF00E5FF).copy(alpha = 0.88f)
                                    val strokeW = if (isIndexContour) 2.2f else 1.1f
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(xA, yA),
                                        end = Offset(xB, yB),
                                        strokeWidth = strokeW
                                    )
                                }

                                hc += cStep
                            }
                        }

                        if (isWireframeOverlay || renderMode == TerrainRenderMode.WIREFRAME_CDLOD) {
                            val wireColor = when (renderMode) {
                                TerrainRenderMode.WIREFRAME_CDLOD -> CyberCyan.copy(alpha = 0.70f)
                                TerrainRenderMode.CAVE_VOXEL_SLICE -> if (isCave) Color(0xFFFF4081) else Color(0xFF334155)
                                else -> Color(0xFF00E5FF).copy(alpha = 0.28f)
                            }
                            drawPath(path, color = wireColor, style = Stroke(width = 1f))
                        }
                    }
                    is RenderFace.SkirtQuad -> {
                        val iA = face.iA
                        val iB = face.iB

                        // Normal vector dot light vector
                        val dot = (face.nx * nlx + face.ny * nly + face.nz * nlz).coerceIn(-1f, 1f)
                        val diffuse = (dot * 0.38f + 0.62f).coerceIn(0.24f, 1.0f)

                        val baseR = (0.15f * diffuse).coerceIn(0f, 1f)
                        val baseG = (0.17f * diffuse).coerceIn(0f, 1f)
                        val baseB = (0.23f * diffuse).coerceIn(0f, 1f)
                        val wallColor = Color(baseR, baseG, baseB, 1f)

                        path.reset()
                        path.moveTo(projX[iA], projY[iA])
                        path.lineTo(projX[iB], projY[iB])
                        path.lineTo(baseProjX[iB], baseProjY[iB])
                        path.lineTo(baseProjX[iA], baseProjY[iA])
                        path.close()

                        drawPath(path, color = wallColor)

                        // Geological horizontal strata bands across vertical cut (Phonolite & Basalt layers)
                        val strataAlpha = (0.32f * diffuse).coerceIn(0.10f, 0.55f)
                        val strataColor = Color(0xFF4A5568).copy(alpha = strataAlpha)

                        for (step in 1..3) {
                            val t = step * 0.25f
                            val sxA = projX[iA] + (baseProjX[iA] - projX[iA]) * t
                            val syA = projY[iA] + (baseProjY[iA] - projY[iA]) * t
                            val sxB = projX[iB] + (baseProjX[iB] - projX[iB]) * t
                            val syB = projY[iB] + (baseProjY[iB] - projY[iB]) * t
                            drawLine(
                                color = strataColor,
                                start = Offset(sxA, syA),
                                end = Offset(sxB, syB),
                                strokeWidth = 1.0f
                            )
                        }

                        // Vertical columnar jointing seam
                        drawLine(
                            color = Color(0xFF1E2430).copy(alpha = 0.55f),
                            start = Offset(projX[iA], projY[iA]),
                            end = Offset(baseProjX[iA], baseProjY[iA]),
                            strokeWidth = 1.0f
                        )

                        // 400m ASL Baseline Datum Highlight Line
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                            start = Offset(baseProjX[iA], baseProjY[iA]),
                            end = Offset(baseProjX[iB], baseProjY[iB]),
                            strokeWidth = 1.6f
                        )
                    }
                    is RenderFace.BottomSlab -> {
                        path.reset()
                        path.moveTo(baseProjX[face.cSW], baseProjY[face.cSW])
                        path.lineTo(baseProjX[face.cSE], baseProjY[face.cSE])
                        path.lineTo(baseProjX[face.cNE], baseProjY[face.cNE])
                        path.lineTo(baseProjX[face.cNW], baseProjY[face.cNW])
                        path.close()

                        drawPath(path, color = Color(0xFF0D1117))
                        drawPath(
                            path,
                            color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                            style = Stroke(width = 1.5f)
                        )
                    }
                    is RenderFace.WaterQuad -> {
                        if (hasWaterPlane) {
                            path.reset()
                            path.moveTo(waterProjX[face.i0], waterProjY[face.i0])
                            path.lineTo(waterProjX[face.i1], waterProjY[face.i1])
                            path.lineTo(waterProjX[face.i2], waterProjY[face.i2])
                            path.lineTo(waterProjX[face.i3], waterProjY[face.i3])
                            path.close()

                            val dt = (face.avgDepthM / 4.2f).coerceIn(0f, 1f)
                            val wColor = Color(
                                red = (0.04f * (1f - dt) + 0.01f * dt),
                                green = (0.78f * (1f - dt) + 0.32f * dt),
                                blue = (0.94f * (1f - dt) + 0.72f * dt),
                                alpha = 0.56f + 0.22f * dt
                            )
                            drawPath(path, color = wColor)
                            if (face.isShoreEdge) {
                                drawPath(
                                    path,
                                    color = Color(0xFF80F3FF).copy(alpha = 0.82f),
                                    style = Stroke(width = 1.4f)
                                )
                            }
                        }
                    }
                }
            }

            // Draw 3D GLB Asset Placements (Garage, Bridge, Start Line, Trees, Rocks)
            for (asset in mesh.assetPlacements) {
                val arx = asset.posX * cosY - asset.posZ * sinY
                val ary = asset.posY - midH
                val arz = asset.posX * sinY + asset.posZ * cosY

                val acy = ary * cosP + arz * sinP
                val acz = -ary * sinP + arz * cosP
                val aez = (cameraDist + acz).coerceAtLeast(8f)

                val asx = centerX + (arx / aez) * effectiveFocal
                val asy = centerY - (acy / aez) * effectiveFocal

                if (asx in 15f..(canvasW - 15f) && asy in 15f..(canvasH - 15f)) {
                    when (asset.type) {
                        "garage" -> {
                            // Orange hub marker
                            drawCircle(Color(0xFFFF9100), radius = 6.5f, center = Offset(asx, asy))
                            drawCircle(Color.White, radius = 2.5f, center = Offset(asx, asy))
                        }
                        "bridge" -> {
                            // Cyan concrete bridge deck indicator
                            drawRect(
                                Color(0xFF00E5FF),
                                topLeft = Offset(asx - 8f, asy - 3.5f),
                                size = androidx.compose.ui.geometry.Size(16f, 7f)
                            )
                        }
                        "spawn" -> {
                            // Bright lime spawn / start line grid
                            drawCircle(Color(0xFF00E676), radius = 5.5f, center = Offset(asx, asy))
                            drawCircle(Color.Black, radius = 2.0f, center = Offset(asx, asy))
                        }
                        "tree" -> {
                            // Forest palm tree dot
                            drawCircle(Color(0xFF2E7D32), radius = 3.0f, center = Offset(asx, asy))
                        }
                        "rock" -> {
                            // Boulder marker
                            drawCircle(Color(0xFF9E9E9E), radius = 2.6f, center = Offset(asx, asy))
                        }
                    }
                }
            }

            // 4. Render 3D Continuous Road Trajectory Ribbon & C58 Markings
            val waypoints = mesh.roadCenterline
            if (waypoints.size >= 2) {
                val wpProjX = FloatArray(waypoints.size)
                val wpProjY = FloatArray(waypoints.size)
                val wpEyeZ = FloatArray(waypoints.size)

                for (w in waypoints.indices) {
                    val wp = waypoints[w]
                    val wrx = wp.x * cosY - wp.z * sinY
                    val wry = wp.y - midH
                    val wrz = wp.x * sinY + wp.z * cosY

                    val wcy = wry * cosP + wrz * sinP
                    val wcz = -wry * sinP + wrz * cosP
                    val wez = (cameraDist + wcz).coerceAtLeast(8f)

                    wpProjX[w] = centerX + (wrx / wez) * effectiveFocal
                    wpProjY[w] = centerY - (wcy / wez) * effectiveFocal
                    wpEyeZ[w] = wez
                }

                // Draw asphalt highway ribbon and transverse topographic contour crossings
                val cStep = contourInterval.coerceAtLeast(5f)
                for (w in 0 until waypoints.size - 1) {
                    val p1x = wpProjX[w]
                    val p1y = wpProjY[w]
                    val p2x = wpProjX[w + 1]
                    val p2y = wpProjY[w + 1]
                    val avgZ = (wpEyeZ[w] + wpEyeZ[w + 1]) * 0.5f
                    val strokeW = (effectiveFocal / avgZ * 0.082f).coerceIn(1.5f, 400.0f)

                    // Road tangent & perpendicular normal for edge lines and contour crossings
                    val rdx = p2x - p1x
                    val rdy = p2y - p1y
                    val rLen = sqrt(rdx * rdx + rdy * rdy).coerceAtLeast(0.001f)
                    val pnx = -rdy / rLen
                    val pny = rdx / rLen

                    // 1. Gravel shoulder embankment base
                    drawLine(
                        color = Color(0xFF6B5844).copy(alpha = 0.85f),
                        start = Offset(p1x, p1y),
                        end = Offset(p2x, p2y),
                        strokeWidth = strokeW * 1.55f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // 2. Dark asphalt highway surface
                    drawLine(
                        color = Color(0xFF1E2126),
                        start = Offset(p1x, p1y),
                        end = Offset(p2x, p2y),
                        strokeWidth = strokeW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // 3. Asphalt pavement wearing course
                    drawLine(
                        color = Color(0xFF2C313A),
                        start = Offset(p1x, p1y),
                        end = Offset(p2x, p2y),
                        strokeWidth = strokeW * 0.78f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // 4. White road edge fog lines (Left & Right)
                    val edgeDist = strokeW * 0.38f
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(p1x + pnx * edgeDist, p1y + pny * edgeDist),
                        end = Offset(p2x + pnx * edgeDist, p2y + pny * edgeDist),
                        strokeWidth = (strokeW * 0.09f).coerceAtLeast(1.0f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(p1x - pnx * edgeDist, p1y - pny * edgeDist),
                        end = Offset(p2x - pnx * edgeDist, p2y - pny * edgeDist),
                        strokeWidth = (strokeW * 0.09f).coerceAtLeast(1.0f)
                    )

                    // 5. Alternating yellow dashed centerline
                    if (w % 2 == 0) {
                        drawLine(
                            color = Color(0xFFFFD54F),
                            start = Offset(p1x, p1y),
                            end = Offset(p2x, p2y),
                            strokeWidth = (strokeW * 0.16f).coerceAtLeast(1.5f)
                        )
                    }

                    // 6. Transverse Topographic Contour Isolines Crossing Road Deck
                    val eA = waypoints[w].rawElevation
                    val eB = waypoints[w + 1].rawElevation
                    val minE = minOf(eA, eB)
                    val maxE = maxOf(eA, eB)
                    var hc = ceil(minE / cStep) * cStep
                    while (hc <= maxE) {
                        if (abs(eB - eA) > 0.001f) {
                            val tCrossing = ((hc - eA) / (eB - eA)).coerceIn(0f, 1f)
                            val crossX = p1x + tCrossing * (p2x - p1x)
                            val crossY = p1y + tCrossing * (p2y - p1y)
                            val tickHalfW = strokeW * 0.90f

                            val isIndexContour = (abs(round(hc / 100f) * 100f - hc) < 1.0f)
                            val contourColor = if (isIndexContour) Color(0xFFFFD700) else Color(0xFF00E5FF)
                            val tickStroke = if (isIndexContour) 2.6f else 1.6f

                            // Transverse contour line crossing road deck
                            drawLine(
                                color = contourColor,
                                start = Offset(crossX - pnx * tickHalfW, crossY - pny * tickHalfW),
                                end = Offset(crossX + pnx * tickHalfW, crossY + pny * tickHalfW),
                                strokeWidth = tickStroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                        hc += cStep
                    }
                }

                // Draw Milestone Posts every ~2 km
                val stepWp = (waypoints.size / 5).coerceAtLeast(1)
                for (w in 0 until waypoints.size step stepWp) {
                    val mx = wpProjX[w]
                    val my = wpProjY[w]
                    if (mx in 20f..(canvasW - 20f) && my in 20f..(canvasH - 20f)) {
                        drawCircle(Color(0xFF263238), radius = 5.0f, center = Offset(mx, my))
                        drawCircle(Color(0xFFFFB300), radius = 3.0f, center = Offset(mx, my))
                    }
                }

                // 5. Draw 3D Illuminated Road Position Beacon at selectedRoadDistanceM
                val clampedDist = selectedRoadDistanceM.coerceIn(0f, 10100f)
                val tNorm = clampedDist / 10100f
                val idxFloat = tNorm * (waypoints.size - 1)
                val idxLow = idxFloat.toInt().coerceIn(0, waypoints.size - 1)
                val idxHigh = (idxLow + 1).coerceIn(0, waypoints.size - 1)
                val frac = idxFloat - idxLow

                val bx = wpProjX[idxLow] + (wpProjX[idxHigh] - wpProjX[idxLow]) * frac
                val by = wpProjY[idxLow] + (wpProjY[idxHigh] - wpProjY[idxLow]) * frac

                if (bx in 20f..(canvasW - 20f) && by in 20f..(canvasH - 20f)) {
                    // Pulsating glowing aura
                    drawCircle(Color(0xFF00E5FF).copy(alpha = 0.28f), radius = 18f, center = Offset(bx, by))
                    drawCircle(Color(0xFFFF9100).copy(alpha = 0.40f), radius = 11f, center = Offset(bx, by))

                    // Center beacon core
                    drawCircle(Color(0xFF00E5FF), radius = 6.0f, center = Offset(bx, by))
                    drawCircle(Color.White, radius = 2.8f, center = Offset(bx, by))

                    // Antenna flagpole pointing up
                    val flagTopY = by - 26f
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(bx, by),
                        end = Offset(bx, flagTopY),
                        strokeWidth = 2.0f
                    )
                    // Diamond tip
                    drawCircle(Color(0xFFFF9100), radius = 4.2f, center = Offset(bx, flagTopY))
                    drawCircle(Color.White, radius = 1.8f, center = Offset(bx, flagTopY))
                }
            }

            // 6. Render Extracted 3D Vector Road Overlays (e.g., OSM Roads in Magadi Town)
            if (overlayRoadSegments3D.isNotEmpty()) {
                for (seg in overlayRoadSegments3D) {
                    val r0x = seg.x0 * cosY - seg.z0 * sinY
                    val r0y = seg.y0 - midH
                    val r0z = seg.x0 * sinY + seg.z0 * cosY
                    val c0y = r0y * cosP + r0z * sinP
                    val c0z = -r0y * sinP + r0z * cosP
                    val e0z = (cameraDist + c0z).coerceAtLeast(8f)
                    val s0x = centerX + (r0x / e0z) * effectiveFocal
                    val s0y = centerY - (c0y / e0z) * effectiveFocal

                    val r1x = seg.x1 * cosY - seg.z1 * sinY
                    val r1y = seg.y1 - midH
                    val r1z = seg.x1 * sinY + seg.z1 * cosY
                    val c1y = r1y * cosP + r1z * sinP
                    val c1z = -r1y * sinP + r1z * cosP
                    val e1z = (cameraDist + c1z).coerceAtLeast(8f)
                    val s1x = centerX + (r1x / e1z) * effectiveFocal
                    val s1y = centerY - (c1y / e1z) * effectiveFocal

                    val avgEz = (e0z + e1z) * 0.5f
                    val baseW = (effectiveFocal / avgEz * (if (is1mResolution) 0.35f else 0.09f))
                        .coerceIn(1.5f, if (is1mResolution) 18f else 5.5f)
                    val roadColor = when {
                        seg.isSelected -> Color(0xFF00E5FF)
                        seg.isPrimary -> Color(0xFFFFB300)
                        else -> Color(0xFFCFD8DC).copy(alpha = 0.85f)
                    }
                    drawLine(
                        color = Color(0xFF1A1F26),
                        start = Offset(s0x, s0y),
                        end = Offset(s1x, s1y),
                        strokeWidth = baseW * 1.45f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = roadColor,
                        start = Offset(s0x, s0y),
                        end = Offset(s1x, s1y),
                        strokeWidth = baseW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // 7. Render Depth-Sorted 3D Extruded Building Volumes
            if (overlayBuildings3D.isNotEmpty()) {
                data class BuildingDrawFace(
                    val pts: List<Offset>,
                    val fillColor: Color,
                    val strokeColor: Color,
                    val strokeWidth: Float,
                    val dist: Float
                )

                fun project3D(wx: Float, wy: Float, wz: Float): Triple<Float, Float, Float> {
                    val rx = wx * cosY - wz * sinY
                    val ry = wy - midH
                    val rz = wx * sinY + wz * cosY
                    val cy = ry * cosP + rz * sinP
                    val cz = -ry * sinP + rz * cosP
                    val ez = (cameraDist + cz).coerceAtLeast(8f)
                    return Triple(
                        centerX + (rx / ez) * effectiveFocal,
                        centerY - (cy / ez) * effectiveFocal,
                        ez
                    )
                }

                val bldgFaces = ArrayList<BuildingDrawFace>(overlayBuildings3D.size * 5)
                for (bldg in overlayBuildings3D) {
                    val corners = bldg.baseCornersXZ
                    if (corners.size < 3) continue
                    val n = corners.size
                    val baseProj = ArrayList<Triple<Float, Float, Float>>(n)
                    val roofProj = ArrayList<Triple<Float, Float, Float>>(n)
                    for ((bx, bz) in corners) {
                        baseProj.add(project3D(bx, bldg.baseY, bz))
                        roofProj.add(project3D(bx, bldg.roofY, bz))
                    }

                    val wallColor = if (bldg.isSelected) {
                        Color(0xFF00E5FF).copy(alpha = 0.88f)
                    } else {
                        Color(0xFFD97736).copy(alpha = 0.86f)
                    }
                    val roofColor = if (bldg.isSelected) {
                        Color(0xFF80F3FF)
                    } else {
                        Color(0xFFFFB74D)
                    }
                    val edgeColor = if (bldg.isSelected) Color.White else Color(0xFF2B1B12)
                    val edgeWidth = if (bldg.isSelected) 2.0f else 1.0f

                    for (k in 0 until n) {
                        val next = (k + 1) % n
                        val p0 = baseProj[k]
                        val p1 = baseProj[next]
                        val p2 = roofProj[next]
                        val p3 = roofProj[k]
                        val dist = (p0.third + p1.third + p2.third + p3.third) * 0.25f
                        bldgFaces.add(
                            BuildingDrawFace(
                                pts = listOf(
                                    Offset(p0.first, p0.second),
                                    Offset(p1.first, p1.second),
                                    Offset(p2.first, p2.second),
                                    Offset(p3.first, p3.second)
                                ),
                                fillColor = wallColor,
                                strokeColor = edgeColor,
                                strokeWidth = edgeWidth,
                                dist = dist
                            )
                        )
                    }

                    var roofDistSum = 0f
                    val roofPts = ArrayList<Offset>(n)
                    for (rp in roofProj) {
                        roofPts.add(Offset(rp.first, rp.second))
                        roofDistSum += rp.third
                    }
                    bldgFaces.add(
                        BuildingDrawFace(
                            pts = roofPts,
                            fillColor = roofColor,
                            strokeColor = edgeColor,
                            strokeWidth = edgeWidth,
                            dist = (roofDistSum / n) - 0.05f
                        )
                    )
                }

                bldgFaces.sortByDescending { it.dist }
                val bPath = Path()
                for (bf in bldgFaces) {
                    if (bf.pts.size < 3) continue
                    bPath.reset()
                    bPath.moveTo(bf.pts[0].x, bf.pts[0].y)
                    for (pi in 1 until bf.pts.size) {
                        bPath.lineTo(bf.pts[pi].x, bf.pts[pi].y)
                    }
                    bPath.close()
                    drawPath(bPath, color = bf.fillColor)
                    drawPath(bPath, color = bf.strokeColor, style = Stroke(width = bf.strokeWidth))
                }
            }

            // Draw 1-Meter Engineering Scale Bar at bottom-center when in 1m resolution mode
            if (is1mResolution) {
                val barW = 90f
                val barX = canvasW * 0.5f - barW * 0.5f
                val barY = canvasH - 18f
                // Main bar
                drawLine(Color(0xFF00E5FF), Offset(barX, barY), Offset(barX + barW, barY), strokeWidth = 3.0f)
                // Left end tick (0.0m)
                drawLine(Color(0xFF00E5FF), Offset(barX, barY - 7f), Offset(barX, barY + 7f), strokeWidth = 3.0f)
                // Right end tick (1.0m)
                drawLine(Color(0xFF00E5FF), Offset(barX + barW, barY - 7f), Offset(barX + barW, barY + 7f), strokeWidth = 3.0f)
                // Mid tick (0.5m)
                drawLine(Color(0xFFFFD54F), Offset(barX + barW * 0.5f, barY - 5f), Offset(barX + barW * 0.5f, barY + 5f), strokeWidth = 2.0f)
            }

            // Draw coordinate axis gizmo in bottom-left (Equal 1:1:1 lengths)
            drawCoordinateGizmo(centerX - canvasW * 0.38f, centerY + canvasH * 0.35f, yaw, pitch)
        }

        // 1m Resolution Floating Scale Badge
        if (is1mResolution) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LaserOrange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "1.0m RESOLUTION GRID • 1 VERTEX/METER (C++ DEM)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Compact HUD Status Pill (Non-overlapping, top-left)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (is1mResolution) LaserOrange else if (isHumanView) LaserOrange else NeonLime,
                    modifier = Modifier.size(6.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        is1mResolution -> {
                            val lakeStr = lakeLevelAslM?.let { " • LAKE %.1fm".format(it) } ?: ""
                            "1.0m DEM • ${mesh.resolutionX}×${mesh.resolutionZ}$lakeStr • ZOOM: ${(zoom * 100).toInt()}%"
                        }
                        isHumanView -> {
                            val currentElev = 598.0f + (walkDistanceM / 1000f) * 18.5f
                            "EYE LEVEL 1.7m • ROAD: %.1fm • ESCARPMENT +1382m • ZOOM: ${(zoom * 100).toInt()}%".format(currentElev)
                        }
                        mesh.resolutionZ >= 100 && lakeLevelAslM != null ->
                            "MAGADI 3×5KM • ${mesh.resolutionX}×${mesh.resolutionZ} • LAKE %.1fm ASL • ZOOM: ${(zoom * 100).toInt()}%".format(lakeLevelAslM)
                        mesh.resolutionZ >= 100 -> "COMBINED 101KM • ${mesh.resolutionX}×${mesh.resolutionZ} • 598m-1980m ASL • ZOOM: ${(zoom * 100).toInt()}%"
                        else -> "VK 1.3 • ${mesh.resolutionX}×${mesh.resolutionZ} • ${mesh.indices.size / 3}Δ • 400m DATUM • ZOOM: ${(zoom * 100).toInt()}%"
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (is1mResolution || isHumanView) LaserOrange else if (mesh.resolutionZ >= 100) NeonLime else CyberCyan
                )
            }
        }

        // 400m ASL Ground Piece Foundation Badge
        if (showGroundPiece && !isHumanView && !is1mResolution) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface.copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, (if (mesh.resolutionZ >= 100) NeonLime else CyberCyan).copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = if (mesh.resolutionZ >= 100) NeonLime else CyberCyan,
                        modifier = Modifier.size(5.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (mesh.resolutionZ >= 100) "WHOLE COMBINED 101KM BLOCK • 400m ASL DATUM" else "400m ASL DATUM • BASALT GROUND BLOCK",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (mesh.resolutionZ >= 100) NeonLime else CyberCyan
                    )
                }
            }
        }
        // Floating Vertical Zoom Control Strip (Right Edge) - Zero restrictions on zoom
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.40f)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                // Zoom In Button (+)
                IconButton(
                    onClick = { onZoomChange(1.30f) },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("terrain_zoom_in_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom Level Readout Badge (Tap resets to 1.0x)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBackground.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, CyberCyan.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clickable { onZoomChange(1.0f / zoom) }
                        .testTag("terrain_zoom_badge")
                ) {
                    val zoomPct = (zoom * 100).roundToInt()
                    val zoomLabel = if (zoom >= 10f) "${zoom.roundToInt()}x"
                                    else if (zoom < 0.2f) "%.2fx".format(zoom)
                                    else "$zoomPct%"
                    Text(
                        text = zoomLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (zoom > 3f) LaserOrange else if (zoom < 0.8f) NeonLime else CyberCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Zoom Out Button (-)
                IconButton(
                    onClick = { onZoomChange(0.77f) },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("terrain_zoom_out_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Reset Zoom to 1.0x (Center Focus)
                IconButton(
                    onClick = { onZoomChange(1.0f / zoom) },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("terrain_zoom_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterCenterFocus,
                        contentDescription = "Reset Zoom (1x)",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCoordinateGizmo(cx: Float, cy: Float, yaw: Float, pitch: Float) {
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val cosP = cos(pitch)
    val sinP = sin(pitch)
    val len = 28f

    // X Axis (East - Red)
    val xx = cosY * len
    val xy = -sinY * sinP * len
    drawLine(Color(0xFFFF5252), Offset(cx, cy), Offset(cx + xx, cy + xy), strokeWidth = 2.2f)

    // Y Axis (Elevation - Green)
    val yy = -cosP * len
    drawLine(Color(0xFF69F0AE), Offset(cx, cy), Offset(cx, cy + yy), strokeWidth = 2.2f)

    // Z Axis (North - Blue)
    val zx = sinY * len
    val zy = cosY * sinP * len
    drawLine(Color(0xFF40C4FF), Offset(cx, cy), Offset(cx + zx, cy + zy), strokeWidth = 2.2f)
}

private enum class WallType { SOUTH, NORTH, WEST, EAST }

private sealed class RenderFace(val sortDist: Float) {
    class TerrainTri(
        val tIdx: Int,
        val i0: Int,
        val i1: Int,
        val i2: Int,
        dist: Float
    ) : RenderFace(dist)

    class SkirtQuad(
        val iA: Int,
        val iB: Int,
        val nx: Float,
        val ny: Float,
        val nz: Float,
        val wallType: WallType,
        dist: Float
    ) : RenderFace(dist)

    class BottomSlab(
        val cSW: Int,
        val cSE: Int,
        val cNE: Int,
        val cNW: Int,
        dist: Float
    ) : RenderFace(dist)

    class WaterQuad(
        val i0: Int,
        val i1: Int,
        val i2: Int,
        val i3: Int,
        val avgDepthM: Float,
        val isShoreEdge: Boolean,
        dist: Float
    ) : RenderFace(dist)
}
