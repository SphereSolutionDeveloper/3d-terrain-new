package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.TerrainMesh3D
import com.example.model.TerrainRenderMode
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
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    if (gestureZoom != 1f) {
                        onZoomChange(gestureZoom - 1f)
                    }
                    if (pan != Offset.Zero) {
                        onPanDrag(pan.x, pan.y)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onRotateDrag(dragAmount.x, dragAmount.y)
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

            val midH = (mesh.minHeight + mesh.maxHeight) * 0.5f
            val baseLevelH = mesh.minHeight - 12f

            // Calibrated camera perspective (natural 45-degree field-of-view, zero fish-eye distortion)
            val cameraDist = (260f / zoom.coerceIn(0.35f, 4.0f))
            val focal = canvasW.coerceAtMost(canvasH) * 2.1f

            // 1. Project all terrain vertices
            val projX = FloatArray(count)
            val projY = FloatArray(count)
            val eyeZ = FloatArray(count)

            // Bedrock base projected vertices (directly below each terrain vertex)
            val baseProjX = FloatArray(count)
            val baseProjY = FloatArray(count)
            val baseEyeZ = FloatArray(count)

            // 1:1:1 Isometric scaling: equal relative scale across X, Y, and Z
            val baseRelY = baseLevelH - midH

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

                projX[i] = centerX + (rx / ez) * focal
                projY[i] = centerY - (cy / ez) * focal
                eyeZ[i] = ez

                // Bedrock foundation vertex
                val brx = rx
                val bry = baseRelY
                val brz = rz

                val bcy = bry * cosP + brz * sinP
                val bcz = -bry * sinP + brz * cosP
                val bez = (cameraDist + bcz).coerceAtLeast(8f)

                baseProjX[i] = centerX + (brx / bez) * focal
                baseProjY[i] = centerY - (bcy / bez) * focal
                baseEyeZ[i] = bez
            }

            // 2. Build list of all renderable faces: terrain triangles + bedrock skirt quads

            val indices = mesh.indices
            val triCount = indices.size / 3
            val renderList = ArrayList<RenderFace>(triCount + (resX + resZ) * 2)

            // Add all terrain surface triangles
            for (t in 0 until triCount) {
                val i0 = indices[t * 3]
                val i1 = indices[t * 3 + 1]
                val i2 = indices[t * 3 + 2]
                val avgDist = (eyeZ[i0] + eyeZ[i1] + eyeZ[i2]) / 3f
                renderList.add(RenderFace.TerrainTri(t, i0, i1, i2, avgDist))
            }

            // Add geological bedrock skirt walls along the four borders
            // North border (iz = 0)
            for (ix in 0 until resX - 1) {
                val a = ix
                val b = ix + 1
                val dist = (eyeZ[a] + eyeZ[b] + baseEyeZ[a] + baseEyeZ[b]) * 0.25f
                renderList.add(RenderFace.SkirtQuad(a, b, dist, -1f))
            }
            // South border (iz = resZ - 1)
            val southRowStart = (resZ - 1) * resX
            for (ix in 0 until resX - 1) {
                val a = southRowStart + ix
                val b = southRowStart + ix + 1
                val dist = (eyeZ[a] + eyeZ[b] + baseEyeZ[a] + baseEyeZ[b]) * 0.25f
                renderList.add(RenderFace.SkirtQuad(a, b, dist, 1f))
            }
            // West border (ix = 0)
            for (iz in 0 until resZ - 1) {
                val a = iz * resX
                val b = (iz + 1) * resX
                val dist = (eyeZ[a] + eyeZ[b] + baseEyeZ[a] + baseEyeZ[b]) * 0.25f
                renderList.add(RenderFace.SkirtQuad(a, b, dist, -1f))
            }
            // East border (ix = resX - 1)
            for (iz in 0 until resZ - 1) {
                val a = iz * resX + (resX - 1)
                val b = (iz + 1) * resX + (resX - 1)
                val dist = (eyeZ[a] + eyeZ[b] + baseEyeZ[a] + baseEyeZ[b]) * 0.25f
                renderList.add(RenderFace.SkirtQuad(a, b, dist, 1f))
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

                        // Lighting calculation
                        val avgNx = (v0.nx + v1.nx + v2.nx) / 3f
                        val avgNy = (v0.ny + v1.ny + v2.ny) / 3f
                        val avgNz = (v0.nz + v1.nz + v2.nz) / 3f
                        val dot = (avgNx * nlx + avgNy * nly + avgNz * nlz).coerceIn(0.12f, 1.0f)
                        val diffuse = 0.38f + 0.62f * dot

                        val avgY = (v0.y + v1.y + v2.y) / 3f
                        val avgSlope = (v0.slopeDeg + v1.slopeDeg + v2.slopeDeg) / 3f
                        val avgGrass = (v0.grassWeight + v1.grassWeight + v2.grassWeight) / 3f
                        val avgDirt = (v0.dirtWeight + v1.dirtWeight + v2.dirtWeight) / 3f
                        val avgRock = (v0.rockWeight + v1.rockWeight + v2.rockWeight) / 3f
                        val avgSnow = (v0.snowWeight + v1.snowWeight + v2.snowWeight) / 3f

                        val isCave = v0.isCaveHollow || v1.isCaveHollow || v2.isCaveHollow
                        val isRoad = (v0.isRoadSurface && v1.isRoadSurface) || (v1.isRoadSurface && v2.isRoadSurface)
                        val isWater = v0.isWaterSurface || v1.isWaterSurface || v2.isWaterSurface
                        val isMud = v0.isMudSurface || v1.isMudSurface || v2.isMudSurface
                        val isGravel = v0.isGravelSurface || v1.isGravelSurface || v2.isGravelSurface

                        val faceColor: Color = when (renderMode) {
                            TerrainRenderMode.SHADED_TRIPLANAR -> {
                                if (isWater) {
                                    Color(0xFF0091EA).copy(
                                        red = 0.05f * diffuse,
                                        green = 0.52f * diffuse + 0.12f,
                                        blue = 0.88f * diffuse
                                    )
                                } else if (isRoad) {
                                    Color(0xFF212529).copy(
                                        red = 0.16f * diffuse,
                                        green = 0.17f * diffuse,
                                        blue = 0.19f * diffuse
                                    )
                                } else if (isMud) {
                                    Color(0xFF4E342E).copy(
                                        red = 0.32f * diffuse,
                                        green = 0.22f * diffuse,
                                        blue = 0.16f * diffuse
                                    )
                                } else if (isGravel) {
                                    Color(0xFF607D8B).copy(
                                        red = 0.40f * diffuse,
                                        green = 0.44f * diffuse,
                                        blue = 0.48f * diffuse
                                    )
                                } else {
                                    val r = (0.24f * avgGrass + 0.48f * avgDirt + 0.30f * avgRock + 0.94f * avgSnow) * diffuse
                                    val g = (0.58f * avgGrass + 0.35f * avgDirt + 0.36f * avgRock + 0.95f * avgSnow) * diffuse
                                    val b = (0.26f * avgGrass + 0.28f * avgDirt + 0.42f * avgRock + 0.98f * avgSnow) * diffuse
                                    Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
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
                                if (isRoad) Color(0xFFFFB300)
                                else if (abs(v0.z) <= 9f) Color(0xFFFF7043).copy(alpha = 0.85f)
                                else Color(0xFF263238).copy(alpha = 0.8f)
                            }
                        }

                        path.reset()
                        path.moveTo(projX[i0], projY[i0])
                        path.lineTo(projX[i1], projY[i1])
                        path.lineTo(projX[i2], projY[i2])
                        path.close()

                        drawPath(path, color = faceColor)

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
                        // Draw solid bedrock skirt wall below border
                        val ta = face.topA
                        val tb = face.topB

                        path.reset()
                        path.moveTo(projX[ta], projY[ta])
                        path.lineTo(projX[tb], projY[tb])
                        path.lineTo(baseProjX[tb], baseProjY[tb])
                        path.lineTo(baseProjX[ta], baseProjY[ta])
                        path.close()

                        // Dark subterranean bedrock strata color
                        val bedrockColor = Color(0xFF13171F)
                        drawPath(path, color = bedrockColor)

                        // Subtle geological layer seam
                        val midXa = (projX[ta] + baseProjX[ta]) * 0.5f
                        val midYa = (projY[ta] + baseProjY[ta]) * 0.5f
                        val midXb = (projX[tb] + baseProjX[tb]) * 0.5f
                        val midYb = (projY[tb] + baseProjY[tb]) * 0.5f
                        drawLine(
                            color = Color(0xFF1F2937),
                            start = Offset(midXa, midYa),
                            end = Offset(midXb, midYb),
                            strokeWidth = 1.2f
                        )

                        // Outline along skirt edge
                        drawPath(path, color = Color(0xFF0F172A), style = Stroke(width = 1.0f))
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

                val asx = centerX + (arx / aez) * focal
                val asy = centerY - (acy / aez) * focal

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

            // Draw coordinate axis gizmo in bottom-left (Equal 1:1:1 lengths)
            drawCoordinateGizmo(centerX - canvasW * 0.38f, centerY + canvasH * 0.35f, yaw, pitch)
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
                    color = NeonLime,
                    modifier = Modifier.size(6.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VK 1.3 • ${mesh.resolutionX}×${mesh.resolutionZ} • ${mesh.indices.size / 3}Δ • 1:1:1 RATIO",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
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

private sealed class RenderFace(val sortDist: Float) {
    class TerrainTri(
        val tIdx: Int,
        val i0: Int,
        val i1: Int,
        val i2: Int,
        dist: Float
    ) : RenderFace(dist)

    class SkirtQuad(
        val topA: Int,
        val topB: Int,
        dist: Float,
        val sideNormal: Float
    ) : RenderFace(dist)
}
