package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.Math3D
import com.example.engine.ProjectedPoint
import com.example.engine.ProjectedTriangle
import com.example.engine.Vec3
import com.example.model.FullAssetBundle
import com.example.model.InspectionMode
import com.example.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun Viewport3DCanvas(
    bundle: FullAssetBundle,
    mode: InspectionMode,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isAutoRotate: Boolean,
    wireframeLineWidth: Float,
    showVertexNodes: Boolean,
    showBoundingBox: Boolean,
    showCoordinateGizmo: Boolean,
    showFloorGrid: Boolean,
    paletteName: String,
    shadingMode: String,
    lightAngle: Float,
    selectedVolumeShapeId: String? = null,
    explodeFactor: Float = 0.0f,
    textureBitmaps: Map<String, Bitmap> = emptyMap(),
    onRotateDrag: (Float, Float) -> Unit,
    onPanDrag: (Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onToggleAutoRotate: () -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Determine primary line color based on palette
    val wireframeColor = when (paletteName) {
        "Electric Violet" -> ElectricViolet
        "Cyber Green" -> NeonLime
        "Laser Orange" -> LaserOrange
        else -> CyberCyan
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomFactor, rotation ->
                    if (zoomFactor != 1.0f) {
                        onZoomChange(zoomFactor)
                    }
                    if (pan != Offset.Zero) {
                        // If one finger / simple drag, orbit; if panning with shift or gesture, pan
                        onRotateDrag(pan.x, pan.y)
                    }
                }
            }
            .testTag("viewport_3d_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw futuristic holographic background grid & concentric scanner rings
            drawHolographicBackground(width, height)

            // 2. Draw 3D Ground plane / grid if enabled
            if (showFloorGrid) {
                drawFloorGrid3D(width, height, yaw, pitch, zoom, panX, panY)
            }

            // 3. Prepare vertices
            val rawVertices = bundle.geometry.vertices
            if (rawVertices.isNotEmpty()) {
                val vecList = rawVertices.map { Vec3(it[0], it[1], it[2]) }

                // Find bounding dimensions for normalizing
                var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
                var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
                for (v in vecList) {
                    minX = min(minX, v.x); maxX = max(maxX, v.x)
                    minY = min(minY, v.y); maxY = max(maxY, v.y)
                    minZ = min(minZ, v.z); maxZ = max(maxZ, v.z)
                }
                val center = Vec3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
                val maxExtent = max(maxX - minX, max(maxY - minY, maxZ - minZ)).coerceAtLeast(0.1f)
                val normalizeScale = (2.2f / maxExtent) * zoom

                // Map vertex to volume center if exploding
                val vertToVolCenter = Array<Vec3?>(vecList.size) { null }
                if (explodeFactor > 0f) {
                    bundle.geometry.volumeShapes.forEach { vs ->
                        if (vs.center.size >= 3) {
                            val c = Vec3(vs.center[0], vs.center[1], vs.center[2])
                            vs.vertexIndices.forEach { vIdx ->
                                if (vIdx in vertToVolCenter.indices) {
                                    vertToVolCenter[vIdx] = c
                                }
                            }
                        }
                    }
                }

                // Transform and project all vertices
                val cameraDistance = 3.8f
                val transformedVertices = vecList.mapIndexed { vIdx, rawV ->
                    val explodeOffset = if (explodeFactor > 0f) {
                        val vc = vertToVolCenter[vIdx]
                        if (vc != null) {
                            val dir = vc - center
                            val len = dir.length()
                            if (len > 0.001f) {
                                Vec3(dir.x / len, dir.y / len, dir.z / len) * (explodeFactor * 1.2f)
                            } else Vec3(0f, explodeFactor * 0.6f, 0f)
                        } else Vec3(0f, 0f, 0f)
                    } else Vec3(0f, 0f, 0f)

                    val centered = (rawV + explodeOffset) - center
                    Math3D.transformVertex(
                        vertex = centered,
                        yawRad = yaw,
                        pitchRad = pitch,
                        scale = normalizeScale,
                        panX = panX,
                        panY = panY,
                        cameraDistance = cameraDistance
                    )
                }

                val projectedPoints = transformedVertices.map { tv ->
                    Math3D.projectPerspective(tv, width, height)
                }

                // 4. Render Bounding Box if enabled
                if (showBoundingBox) {
                    drawBoundingBox3D(
                        width, height, minX, maxX, minY, maxY, minZ, maxZ,
                        center, normalizeScale, yaw, pitch, panX, panY, cameraDistance
                    )
                }

                // Render selected volume bounding box if focused
                val activeVol = bundle.geometry.volumeShapes.firstOrNull { it.id == selectedVolumeShapeId }
                if (activeVol != null && activeVol.dimensions.size >= 3 && activeVol.center.size >= 3) {
                    val hx = activeVol.dimensions[0] / 2f
                    val hy = activeVol.dimensions[1] / 2f
                    val hz = activeVol.dimensions[2] / 2f
                    val cx = activeVol.center[0]
                    val cy = activeVol.center[1]
                    val cz = activeVol.center[2]
                    val volColor = try {
                        Color(android.graphics.Color.parseColor(activeVol.colorHex))
                    } catch (e: Exception) { CyberCyan }
                    drawBoundingBox3D(
                        width, height, cx - hx, cx + hx, cy - hy, cy + hy, cz - hz, cz + hz,
                        center, normalizeScale, yaw, pitch, panX, panY, cameraDistance,
                        customColor = volColor
                    )
                }

                // Light direction vector based on lightAngle
                val lightDir = Vec3(
                    cos(lightAngle),
                    0.8f,
                    sin(lightAngle)
                ).normalized()

                // Render based on active inspection mode
                val effectiveShadingMode = if (mode == InspectionMode.VOLUME_DIAGRAM && shadingMode != "Volume Shapes") {
                    "Volume Shapes"
                } else shadingMode

                val isShaded = mode == InspectionMode.GLB_SHADED || mode == InspectionMode.VOLUME_DIAGRAM || effectiveShadingMode == "Volume Shapes" || effectiveShadingMode == "Wireframe Over Solid"
                val isWireframe = mode == InspectionMode.WIREFRAME || effectiveShadingMode == "Wireframe Over Solid" || mode == InspectionMode.MESH_DATA

                if (isShaded && bundle.geometry.faces.isNotEmpty()) {
                    drawShadedFaces(
                        faces = bundle.geometry.faces,
                        vecList = vecList,
                        transformedVertices = transformedVertices,
                        projectedPoints = projectedPoints,
                        lightDir = lightDir,
                        shadingMode = effectiveShadingMode,
                        primaryColor = wireframeColor,
                        uvs = bundle.geometry.uvs,
                        albedoBitmap = textureBitmaps["albedo.png"],
                        roughnessBitmap = textureBitmaps["roughness.png"],
                        emissiveBitmap = textureBitmaps["emissive.png"],
                        volumeShapes = bundle.geometry.volumeShapes,
                        selectedVolumeShapeId = selectedVolumeShapeId
                    )
                }

                if (isWireframe) {
                    // Draw edges
                    drawWireframeEdges(
                        edges = bundle.wireframe.edges,
                        projected = projectedPoints,
                        transformed = transformedVertices,
                        color = wireframeColor,
                        lineWidth = wireframeLineWidth
                    )

                    // Draw vertices
                    if (showVertexNodes) {
                        drawVertexDots(
                            projected = projectedPoints,
                            color = wireframeColor
                        )
                    }
                }
            }

            // 5. Draw 3D Orientation Gizmo (top-right)
            if (showCoordinateGizmo) {
                drawOrientationGizmo(width, height, yaw, pitch, textMeasurer)
            }

            // 6. Draw HUD Telemetry in top-left and bottom-left
            drawHudOverlays(width, height, bundle, yaw, pitch, zoom, textMeasurer)
        }

        // Viewport Interactive Floating Toolbar
        ViewportFloatingControls(
            isAutoRotate = isAutoRotate,
            onToggleAutoRotate = onToggleAutoRotate,
            onResetCamera = onResetCamera,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        )
    }
}

private fun DrawScope.drawHolographicBackground(width: Float, height: Float) {
    // Holographic vignette gradient
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                DarkSurfaceElevated.copy(alpha = 0.5f),
                DarkBackground.copy(alpha = 0.95f),
                DarkBackground
            ),
            center = Offset(width / 2f, height / 2f),
            radius = (width.coerceAtLeast(height)) * 0.8f
        )
    )

    // Corner targeting brackets (Sci-Fi HUD)
    val bracketColor = CyberCyan.copy(alpha = 0.25f)
    val bracketLen = 28f
    val pad = 16f

    // Top-Left
    drawLine(bracketColor, Offset(pad, pad), Offset(pad + bracketLen, pad), strokeWidth = 2f)
    drawLine(bracketColor, Offset(pad, pad), Offset(pad, pad + bracketLen), strokeWidth = 2f)
    // Top-Right
    drawLine(bracketColor, Offset(width - pad, pad), Offset(width - pad - bracketLen, pad), strokeWidth = 2f)
    drawLine(bracketColor, Offset(width - pad, pad), Offset(width - pad, pad + bracketLen), strokeWidth = 2f)
    // Bottom-Left
    drawLine(bracketColor, Offset(pad, height - pad), Offset(pad + bracketLen, height - pad), strokeWidth = 2f)
    drawLine(bracketColor, Offset(pad, height - pad), Offset(pad, height - pad - bracketLen), strokeWidth = 2f)
    // Bottom-Right
    drawLine(bracketColor, Offset(width - pad, height - pad), Offset(width - pad - bracketLen, height - pad), strokeWidth = 2f)
    drawLine(bracketColor, Offset(width - pad, height - pad), Offset(width - pad, height - pad - bracketLen), strokeWidth = 2f)
}

private fun DrawScope.drawFloorGrid3D(
    width: Float,
    height: Float,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    panX: Float,
    panY: Float
) {
    val floorY = -1.4f
    val gridSize = 2.4f
    val steps = 6
    val step = (gridSize * 2f) / steps
    val gridColor = CyberCyan.copy(alpha = 0.12f)
    val cameraDistance = 3.8f

    // Grid lines along X
    for (i in 0..steps) {
        val z = -gridSize + i * step
        val p1 = FloorProjectionHelper.transformFloorPoint(-gridSize, floorY, z, yaw, pitch, zoom, panX, panY, cameraDistance, width, height)
        val p2 = FloorProjectionHelper.transformFloorPoint(gridSize, floorY, z, yaw, pitch, zoom, panX, panY, cameraDistance, width, height)
        if (p1.isVisible && p2.isVisible) {
            drawLine(gridColor, Offset(p1.screenX, p1.screenY), Offset(p2.screenX, p2.screenY), strokeWidth = 1f)
        }
    }
    // Grid lines along Z
    for (i in 0..steps) {
        val x = -gridSize + i * step
        val p1 = FloorProjectionHelper.transformFloorPoint(x, floorY, -gridSize, yaw, pitch, zoom, panX, panY, cameraDistance, width, height)
        val p2 = FloorProjectionHelper.transformFloorPoint(x, floorY, gridSize, yaw, pitch, zoom, panX, panY, cameraDistance, width, height)
        if (p1.isVisible && p2.isVisible) {
            drawLine(gridColor, Offset(p1.screenX, p1.screenY), Offset(p2.screenX, p2.screenY), strokeWidth = 1f)
        }
    }
}

private fun DrawScope.drawBoundingBox3D(
    width: Float,
    height: Float,
    minX: Float, maxX: Float,
    minY: Float, maxY: Float,
    minZ: Float, maxZ: Float,
    center: Vec3,
    scale: Float,
    yaw: Float, pitch: Float,
    panX: Float, panY: Float,
    cameraDistance: Float,
    customColor: Color? = null
) {
    val corners = listOf(
        Vec3(minX, minY, minZ), Vec3(maxX, minY, minZ),
        Vec3(maxX, maxY, minZ), Vec3(minX, maxY, minZ),
        Vec3(minX, minY, maxZ), Vec3(maxX, minY, maxZ),
        Vec3(maxX, maxY, maxZ), Vec3(minX, maxY, maxZ)
    )

    val proj = corners.map { c ->
        val centered = c - center
        val tv = Math3D.transformVertex(centered, yaw, pitch, scale, panX, panY, cameraDistance)
        Math3D.projectPerspective(tv, width, height)
    }

    val boxColor = customColor ?: CyberAccent.copy(alpha = 0.20f)
    val edges = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0,
        4 to 5, 5 to 6, 6 to 7, 7 to 4,
        0 to 4, 1 to 5, 2 to 6, 3 to 7
    )

    edges.forEach { (i1, i2) ->
        val p1 = proj[i1]
        val p2 = proj[i2]
        if (p1.isVisible && p2.isVisible) {
            drawLine(boxColor, Offset(p1.screenX, p1.screenY), Offset(p2.screenX, p2.screenY), strokeWidth = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }
    }
}

private fun DrawScope.drawWireframeEdges(
    edges: List<List<Int>>,
    projected: List<ProjectedPoint>,
    transformed: List<Vec3>,
    color: Color,
    lineWidth: Float
) {
    for (edge in edges) {
        val i1 = edge[0]
        val i2 = edge[1]
        if (i1 < projected.size && i2 < projected.size) {
            val p1 = projected[i1]
            val p2 = projected[i2]
            if (p1.isVisible && p2.isVisible) {
                // Depth fog fade
                val avgZ = (transformed[i1].z + transformed[i2].z) / 2f
                val fogAlpha = ((-avgZ - 2.0f) / 4.0f).coerceIn(0.25f, 0.95f)

                // Glow under-line
                drawLine(
                    color = color.copy(alpha = fogAlpha * 0.35f),
                    start = Offset(p1.screenX, p1.screenY),
                    end = Offset(p2.screenX, p2.screenY),
                    strokeWidth = lineWidth + 2.5f,
                    cap = StrokeCap.Round
                )
                // Crisp core line
                drawLine(
                    color = color.copy(alpha = fogAlpha),
                    start = Offset(p1.screenX, p1.screenY),
                    end = Offset(p2.screenX, p2.screenY),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawVertexDots(
    projected: List<ProjectedPoint>,
    color: Color
) {
    for (p in projected) {
        if (p.isVisible) {
            drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = 4.5f,
                center = Offset(p.screenX, p.screenY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.2f,
                center = Offset(p.screenX, p.screenY)
            )
        }
    }
}

private fun DrawScope.drawShadedFaces(
    faces: List<List<Int>>,
    vecList: List<Vec3>,
    transformedVertices: List<Vec3>,
    projectedPoints: List<ProjectedPoint>,
    lightDir: Vec3,
    shadingMode: String,
    primaryColor: Color,
    uvs: List<List<Float>>,
    albedoBitmap: Bitmap?,
    roughnessBitmap: Bitmap? = null,
    emissiveBitmap: Bitmap?,
    volumeShapes: List<com.example.model.VolumeShape> = emptyList(),
    selectedVolumeShapeId: String? = null
) {
    // Project and collect triangles
    val triangles = mutableListOf<ProjectedTriangle>()

    val faceToVolumeMap = IntArray(faces.size) { -1 }
    volumeShapes.forEachIndexed { sIdx, vs ->
        vs.faceIndices.forEach { fIdx ->
            if (fIdx in faceToVolumeMap.indices) faceToVolumeMap[fIdx] = sIdx
        }
    }

    for (faceIdx in faces.indices) {
        val face = faces[faceIdx]
        if (face.size >= 3) {
            val i1 = face[0]
            val i2 = face[1]
            val i3 = face[2]

            val p1 = projectedPoints[i1]
            val p2 = projectedPoints[i2]
            val p3 = projectedPoints[i3]

            if (p1.isVisible && p2.isVisible && p3.isVisible) {
                val t1 = transformedVertices[i1]
                val t2 = transformedVertices[i2]
                val t3 = transformedVertices[i3]

                val normal = Math3D.calculateFaceNormal(t1, t2, t3)
                val avgDepth = (t1.z + t2.z + t3.z) / 3f

                val diffuse = normal.dot(lightDir).coerceAtLeast(0.06f)

                val triU = if (uvs.size > maxOf(i1, i2, i3) && uvs[i1].isNotEmpty()) {
                    (uvs[i1][0] + uvs[i2][0] + uvs[i3][0]) / 3f
                } else 0.5f

                val triV = if (uvs.size > maxOf(i1, i2, i3) && uvs[i1].size > 1) {
                    (uvs[i1][1] + uvs[i2][1] + uvs[i3][1]) / 3f
                } else 0.5f

                val centerPos = Vec3(
                    (t1.x + t2.x + t3.x) / 3f,
                    (t1.y + t2.y + t3.y) / 3f,
                    (t1.z + t2.z + t3.z) / 3f
                )

                triangles.add(ProjectedTriangle(p1, p2, p3, normal, avgDepth, diffuse, triU, triV, centerPos, faceIndex = faceIdx))
            }
        }
    }

    // Sort by depth (Painter's algorithm: draw farthest triangles first)
    triangles.sortBy { it.averageDepth }

    for (tri in triangles) {
        val path = Path().apply {
            moveTo(tri.p1.screenX, tri.p1.screenY)
            lineTo(tri.p2.screenX, tri.p2.screenY)
            lineTo(tri.p3.screenX, tri.p3.screenY)
            close()
        }

        val sIdx = if (tri.faceIndex in faceToVolumeMap.indices) faceToVolumeMap[tri.faceIndex] else -1
        val vs = if (sIdx in volumeShapes.indices) volumeShapes[sIdx] else null

        val faceColor = when (shadingMode) {
            "Volume Shapes" -> {
                if (selectedVolumeShapeId != null) {
                    if (vs != null && vs.id == selectedVolumeShapeId) {
                        val baseColor = try {
                            Color(android.graphics.Color.parseColor(vs.colorHex))
                        } catch (e: Exception) { CyberCyan }
                        val light = (0.35f + tri.lightingFactor * 0.65f).coerceIn(0f, 1f)
                        baseColor.copy(
                            red = (baseColor.red * light).coerceIn(0f, 1f),
                            green = (baseColor.green * light).coerceIn(0f, 1f),
                            blue = (baseColor.blue * light).coerceIn(0f, 1f),
                            alpha = 0.98f
                        )
                    } else {
                        // Ghosted inactive volume
                        Color(0xFF141926).copy(alpha = 0.20f)
                    }
                } else {
                    val baseColor = if (vs != null) {
                        try {
                            Color(android.graphics.Color.parseColor(vs.colorHex))
                        } catch (e: Exception) { CyberCyan }
                    } else primaryColor
                    val light = (0.32f + tri.lightingFactor * 0.68f).coerceIn(0f, 1f)
                    baseColor.copy(
                        red = (baseColor.red * light).coerceIn(0f, 1f),
                        green = (baseColor.green * light).coerceIn(0f, 1f),
                        blue = (baseColor.blue * light).coerceIn(0f, 1f),
                        alpha = 0.95f
                    )
                }
            }
            "Surface Normals" -> {
                // Map normal [-1, 1] to RGB [0, 1]
                Color(
                    red = (tri.normal.x * 0.5f + 0.5f).coerceIn(0f, 1f),
                    green = (tri.normal.y * 0.5f + 0.5f).coerceIn(0f, 1f),
                    blue = (tri.normal.z * 0.5f + 0.5f).coerceIn(0f, 1f),
                    alpha = 0.95f
                )
            }
            "X-Ray Hologram" -> {
                primaryColor.copy(alpha = 0.28f + (1f - abs(tri.normal.z)) * 0.45f)
            }
            else -> {
                // Real Colored Solid PBR Shading sampled from genuine albedo and roughness maps
                var baseR = primaryColor.red
                var baseG = primaryColor.green
                var baseB = primaryColor.blue

                // 1. Sample real colored texture from albedoBitmap
                if (albedoBitmap != null) {
                    val px = (tri.u * (albedoBitmap.width - 1)).toInt().coerceIn(0, albedoBitmap.width - 1)
                    val py = ((1f - tri.v) * (albedoBitmap.height - 1)).toInt().coerceIn(0, albedoBitmap.height - 1)
                    val pixel = albedoBitmap.getPixel(px, py)
                    val texR = ((pixel shr 16) and 0xFF) / 255f
                    val texG = ((pixel shr 8) and 0xFF) / 255f
                    val texB = (pixel and 0xFF) / 255f
                    // Use genuine texture albedo colors at full opacity
                    baseR = texR
                    baseG = texG
                    baseB = texB
                }

                // Sample roughness if available
                var roughnessVal = 0.65f
                if (roughnessBitmap != null) {
                    val rx = (tri.u * (roughnessBitmap.width - 1)).toInt().coerceIn(0, roughnessBitmap.width - 1)
                    val ry = ((1f - tri.v) * (roughnessBitmap.height - 1)).toInt().coerceIn(0, roughnessBitmap.height - 1)
                    val rPixel = roughnessBitmap.getPixel(rx, ry)
                    roughnessVal = ((rPixel and 0xFF) / 255f).coerceIn(0.15f, 0.95f)
                }

                // 2. View direction (camera at origin 0,0,0 looking down -Z)
                val vLen = tri.centerViewPos.length()
                val viewDir = if (vLen > 0.001f) {
                    Vec3(-tri.centerViewPos.x / vLen, -tri.centerViewPos.y / vLen, -tri.centerViewPos.z / vLen)
                } else Vec3(0f, 0f, 1f)

                // 3. Half-vector for Blinn-Phong specular glint
                val hVec = (lightDir + viewDir).normalized()
                val nDotH = max(0f, tri.normal.dot(hVec))
                val specPow = (1f - roughnessVal) * 48f + 8f
                val spec = nDotH.pow(specPow) * (1f - roughnessVal)

                // 4. Fresnel rim glint at grazing angles
                val nDotV = max(0f, tri.normal.dot(viewDir))
                val rim = (1f - nDotV).pow(3.0f) * 0.15f

                // 5. Rich Ambient & Diffuse lighting preserving deep, saturated base colors
                val ambient = 0.38f
                val diffuse = tri.lightingFactor * 0.62f
                val totalLight = ambient + diffuse

                var finalR = baseR * totalLight + spec * 0.45f + rim * 0.15f
                var finalG = baseG * totalLight + spec * 0.45f + rim * 0.15f
                var finalB = baseB * totalLight + spec * 0.45f + rim * 0.15f

                // 6. Emissive Glow overlay
                if (emissiveBitmap != null) {
                    val ePx = (tri.u * (emissiveBitmap.width - 1)).toInt().coerceIn(0, emissiveBitmap.width - 1)
                    val ePy = ((1f - tri.v) * (emissiveBitmap.height - 1)).toInt().coerceIn(0, emissiveBitmap.height - 1)
                    val ePixel = emissiveBitmap.getPixel(ePx, ePy)
                    val eR = ((ePixel shr 16) and 0xFF) / 255f
                    val eG = ((ePixel shr 8) and 0xFF) / 255f
                    val eB = (ePixel and 0xFF) / 255f
                    if (eR + eG + eB > 0.05f) {
                        finalR = min(1f, finalR + eR * 1.4f)
                        finalG = min(1f, finalG + eG * 1.4f)
                        finalB = min(1f, finalB + eB * 1.4f)
                    }
                }

                Color(
                    red = finalR.coerceIn(0f, 1f),
                    green = finalG.coerceIn(0f, 1f),
                    blue = finalB.coerceIn(0f, 1f),
                    alpha = 1.0f
                )
            }
        }

        drawPath(path, faceColor)
        // Edge crease or volume highlight
        val isVolActive = selectedVolumeShapeId != null && vs != null && vs.id == selectedVolumeShapeId
        val creaseColor = if (isVolActive) {
            Color.White.copy(alpha = 0.85f)
        } else if (shadingMode == "Volume Shapes") {
            Color.Black.copy(alpha = 0.35f)
        } else if (shadingMode == "Wireframe Over Solid") {
            primaryColor.copy(alpha = 0.55f)
        } else {
            Color.Black.copy(alpha = 0.16f)
        }
        drawPath(path, creaseColor, style = Stroke(width = if (isVolActive) 1.6f else 0.8f))
    }
}

private fun DrawScope.drawOrientationGizmo(
    width: Float,
    height: Float,
    yaw: Float,
    pitch: Float,
    textMeasurer: TextMeasurer
) {
    val gizmoCenterX = width - 48f
    val gizmoCenterY = 48f
    val gizmoLen = 28f

    // Origin circle
    drawCircle(DarkSurfaceElevated.copy(alpha = 0.85f), radius = 36f, center = Offset(gizmoCenterX, gizmoCenterY))
    drawCircle(GlowingCyanBorder, radius = 36f, center = Offset(gizmoCenterX, gizmoCenterY), style = Stroke(width = 1.2f))

    val axes = listOf(
        Triple(Vec3(1f, 0f, 0f), Color(0xFFFF4D4D), "X"), // Red = X
        Triple(Vec3(0f, 1f, 0f), Color(0xFF39FF14), "Y"), // Green = Y
        Triple(Vec3(0f, 0f, 1f), Color(0xFF00F0FF), "Z")  // Cyan/Blue = Z
    )

    for ((axisVec, color, label) in axes) {
        var p = Math3D.rotateY(axisVec, yaw)
        p = Math3D.rotateX(p, pitch)

        val endX = gizmoCenterX + p.x * gizmoLen
        val endY = gizmoCenterY - p.y * gizmoLen // Invert Y for screen

        drawLine(color, Offset(gizmoCenterX, gizmoCenterY), Offset(endX, endY), strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawCircle(color, radius = 3.5f, center = Offset(endX, endY))

        // Draw small label
        val textLayout = textMeasurer.measure(
            text = label,
            style = TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
        drawText(textLayout, topLeft = Offset(endX + 3f, endY - 8f))
    }
}

private fun DrawScope.drawHudOverlays(
    width: Float,
    height: Float,
    bundle: FullAssetBundle,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    // Top-Left Telemetry: Vulkan 1.3 Pipeline Active
    val hudText = buildAnnotatedString {
        withStyle(SpanStyle(color = NeonLime, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)) {
            append("VULKAN 1.3 // SASCHA WILLEMS PIPELINE\n")
        }
        withStyle(SpanStyle(color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)) {
            append("ASSET: ${bundle.item.folder.uppercase()}/ • VK_DEVICE_LOCAL_MEMORY\n")
            append("VERTS: ${bundle.geometry.vertexCount}  POLYS: ${bundle.geometry.faceCount}  EDGES: ${bundle.wireframe.edgeCount}\n")
            append("YAW: ${(yaw * 180f / kotlin.math.PI.toFloat()).toInt()}°  PITCH: ${(pitch * 180f / kotlin.math.PI.toFloat()).toInt()}°  ZOOM: ${(zoom * 100).toInt()}%\n")
            append("SPIR-V: ${if (bundle.glbHeader.isValid) "glTF 2.0 / SPIR-V 1.5 READY" else "UNVERIFIED"}")
        }
    }

    val measured = textMeasurer.measure(hudText)
    drawRect(
        color = DarkSurface.copy(alpha = 0.85f),
        topLeft = Offset(12f, 12f),
        size = androidx.compose.ui.geometry.Size(measured.size.width + 16f, measured.size.height + 12f)
    )
    drawRect(
        color = NeonLime.copy(alpha = 0.5f),
        topLeft = Offset(12f, 12f),
        size = androidx.compose.ui.geometry.Size(measured.size.width + 16f, measured.size.height + 12f),
        style = Stroke(width = 1f)
    )
    drawText(measured, topLeft = Offset(20f, 18f))
}

private object FloorProjectionHelper {
    fun transformFloorPoint(
        x: Float, y: Float, z: Float,
        yaw: Float, pitch: Float, zoom: Float,
        panX: Float, panY: Float, cameraDistance: Float,
        width: Float, height: Float
    ): ProjectedPoint {
        val tv = Math3D.transformVertex(Vec3(x, y, z), yaw, pitch, zoom, panX, panY, cameraDistance)
        return Math3D.projectPerspective(tv, width, height)
    }
}

@Composable
private fun ViewportFloatingControls(
    isAutoRotate: Boolean,
    onToggleAutoRotate: () -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceElevated.copy(alpha = 0.90f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onToggleAutoRotate,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("toggle_autorotate_button")
            ) {
                Icon(
                    imageVector = if (isAutoRotate) Icons.Default.RotateRight else Icons.Default.Pause,
                    contentDescription = "Toggle Turntable Rotation",
                    tint = if (isAutoRotate) CyberCyan else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onResetCamera,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("reset_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Reset Camera View",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
