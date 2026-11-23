package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.model.CppAssetType
import com.example.model.ProceduralBoxPart
import com.example.model.ProceduralModelMesh
import com.example.ui.theme.*
import kotlin.math.*

private data class ProjectedQuad(
    val path: Path,
    val color: Color,
    val avgDepth: Float,
    val isOutline: Boolean = false,
    val outlineColor: Color = Color.Transparent
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun CppViewport3DCanvas(
    mesh: ProceduralModelMesh,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isAutoRotate: Boolean,
    showGrid: Boolean,
    shadingMode: String,
    explodeFactor: Float,
    lightAngle: Float,
    fovAngleDeg: Float = 60.0f,
    isPerspective: Boolean = true,
    renderingBackend: String = "Vulkan 1.3 (Sascha Willems)",
    vulkanPolygonMode: String = "VK_POLYGON_MODE_FILL",
    vulkanMsaa: String = "VK_SAMPLE_COUNT_4_BIT",
    vulkanValidationLayers: Boolean = true,
    vulkanDynamicRendering: Boolean = true,
    onRotateDrag: (Float, Float) -> Unit,
    onPanDrag: (Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onToggleAutoRotate: () -> Unit,
    onTogglePerspective: () -> Unit = {},
    onResetCamera: (String) -> Unit,
    onToggleBackend: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(Color(0xFF0F141C))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    if (gestureZoom != 1f) {
                        onZoomChange(gestureZoom)
                    } else if (pan != Offset.Zero) {
                        // Horizontal & Vertical touch drag rotates camera
                        onRotateDrag(pan.x, pan.y)
                    }
                }
            }
            .testTag("cpp_viewport_canvas_container")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("cpp_3d_canvas")) {
            val width = size.width
            val height = size.height
            val cx = width / 2f + panX
            val cy = height / 2f + panY

            val extentX = (mesh.boundingBoxMax[0] - mesh.boundingBoxMin[0]).coerceAtLeast(1f)
            val extentY = (mesh.boundingBoxMax[1] - mesh.boundingBoxMin[1]).coerceAtLeast(1f)
            val extentZ = (mesh.boundingBoxMax[2] - mesh.boundingBoxMin[2]).coerceAtLeast(1f)
            val bridgeExtentM = maxOf(extentX, extentY * 1.4f, extentZ, 32f)
            val bridgeGridHalfM = (kotlin.math.ceil((maxOf(extentX, extentZ) * 0.60f) / 10f) * 10f).coerceAtLeast(30f)

            // Responsive scale based on physical asset AABB envelope
            val baseScale = when (mesh.assetType) {
                CppAssetType.GARAGE_001 -> (min(width, height) / 38f) * zoom
                CppAssetType.TEST_VEHICLE_LORRY -> (min(width, height) / 8.5f) * zoom
                CppAssetType.MODULAR_POWER_POST -> (min(width, height) / 14.5f) * zoom
                CppAssetType.POWER_GRID_NETWORK -> (min(width, height) / 44f) * zoom
                CppAssetType.MODULAR_BRIDGE, CppAssetType.MODULAR_FOOTBRIDGE -> (min(width, height) / (bridgeExtentM * 0.92f)) * zoom
                CppAssetType.MODULAR_TOLL_PLAZA -> (min(width, height) / 46f) * zoom
                CppAssetType.MODULAR_TUNNEL -> (min(width, height) / 70f) * zoom
                CppAssetType.MODULAR_INTERCHANGE -> (min(width, height) / 110f) * zoom
                CppAssetType.MODULAR_RETAINING_WALL -> (min(width, height) / 50f) * zoom
                CppAssetType.CONNECTED_HIGHWAY_SCENE -> (min(width, height) / 160f) * zoom
                CppAssetType.MODULAR_EXPRESSWAY -> (min(width, height) / 110f) * zoom
                CppAssetType.MODULAR_SIGNPOST -> (min(width, height) / 36f) * zoom
                CppAssetType.MODULAR_BARRIER -> (min(width, height) / 44f) * zoom
                CppAssetType.MODULAR_ROAD_BLOCK -> (min(width, height) / 38f) * zoom
                CppAssetType.MODULAR_ROAD_NETWORK -> (min(width, height) / 72f) * zoom
                CppAssetType.MODULAR_SPLINE_GRAPH -> (min(width, height) / 110f) * zoom
                CppAssetType.MODULAR_ROAD_TILE -> (min(width, height) / 58f) * zoom
            }

            // Center of entire model for camera centering, rotation, and explosion disassembly
            val centerModelX = (mesh.boundingBoxMin[0] + mesh.boundingBoxMax[0]) * 0.5f
            val centerModelY = (mesh.boundingBoxMin[1] + mesh.boundingBoxMax[1]) * 0.5f
            val centerModelZ = (mesh.boundingBoxMin[2] + mesh.boundingBoxMax[2]) * 0.5f

            // Virtual camera distance in meters for perspective projection
            val cameraDist = when (mesh.assetType) {
                CppAssetType.GARAGE_001 -> 70.0f
                CppAssetType.TEST_VEHICLE_LORRY -> 13.0f
                CppAssetType.MODULAR_POWER_POST -> 20.0f
                CppAssetType.POWER_GRID_NETWORK -> 56.0f
                CppAssetType.MODULAR_BRIDGE, CppAssetType.MODULAR_FOOTBRIDGE -> bridgeExtentM * 1.45f
                CppAssetType.MODULAR_TOLL_PLAZA -> 75.0f
                CppAssetType.MODULAR_TUNNEL -> 95.0f
                CppAssetType.MODULAR_INTERCHANGE -> 150.0f
                CppAssetType.MODULAR_RETAINING_WALL -> 70.0f
                CppAssetType.CONNECTED_HIGHWAY_SCENE -> 260.0f
                CppAssetType.MODULAR_EXPRESSWAY -> 160.0f
                CppAssetType.MODULAR_SIGNPOST -> 58.0f
                CppAssetType.MODULAR_BARRIER -> 66.0f
                CppAssetType.MODULAR_ROAD_BLOCK -> 58.0f
                CppAssetType.MODULAR_ROAD_NETWORK -> 95.0f
                CppAssetType.MODULAR_SPLINE_GRAPH -> 155.0f
                CppAssetType.MODULAR_ROAD_TILE -> 78.0f
            }

            // 1. Draw 3D Ground Grid with matching perspective convergence
            if (showGrid) {
                drawGroundGrid(
                    cx = cx, cy = cy,
                    scale = baseScale,
                    yaw = yaw, pitch = pitch,
                    assetType = mesh.assetType,
                    centerX = centerModelX,
                    centerY = 0f,
                    centerZ = centerModelZ,
                    cameraDist = cameraDist,
                    isPerspective = isPerspective,
                    fovDeg = fovAngleDeg,
                    bridgeGridHalfM = bridgeGridHalfM
                )
            }

            // 2. Project boxes and render solid / wireframe faces sorted by depth (Painter's algorithm)
            val quadsToDraw = mutableListOf<ProjectedQuad>()
            val wireframeLines = mutableListOf<Pair<Offset, Offset>>()

            // Light vector direction from spherical light angle
            val lightDirX = cos(lightAngle) * 0.7f
            val lightDirY = 0.85f
            val lightDirZ = sin(lightAngle) * 0.7f
            val lightLen = sqrt(lightDirX * lightDirX + lightDirY * lightDirY + lightDirZ * lightDirZ)
            val lx = lightDirX / lightLen
            val ly = lightDirY / lightLen
            val lz = lightDirZ / lightLen

            mesh.boxes.forEach { box ->
                // Explode translation vector
                val dirX = box.centerX - centerModelX
                val dirY = box.centerY - centerModelY
                val dirZ = box.centerZ - centerModelZ
                val offsetX = dirX * explodeFactor * 1.5f
                val offsetY = dirY * explodeFactor * 1.5f
                val offsetZ = dirZ * explodeFactor * 1.5f

                val curCx = box.centerX + offsetX
                val curCy = box.centerY + offsetY
                val curCz = box.centerZ + offsetZ

                val hx = box.sizeX * 0.5f
                val hy = box.sizeY * 0.5f
                val hz = box.sizeZ * 0.5f

                // 8 Box corners in local/world coordinates with perspective projection
                val v000 = project(curCx - hx, curCy - hy, curCz - hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)
                val v100 = project(curCx + hx, curCy - hy, curCz - hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)
                val v110 = project(curCx + hx, curCy + hy, curCz - hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)
                val v010 = project(curCx - hx, curCy + hy, curCz - hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)

                val v001 = project(curCx - hx, curCy - hy, curCz + hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)
                val v101 = project(curCx + hx, curCy - hy, curCz + hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)
                val v111 = project(curCx + hx, curCy + hy, curCz + hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)
                val v011 = project(curCx - hx, curCy + hy, curCz + hz, centerModelX, centerModelY, centerModelZ, yaw, pitch, baseScale, cx, cy, cameraDist, isPerspective, fovAngleDeg)

                // Face definitions: {Corner indices, World Normal (nx, ny, nz)}
                val faces = listOf(
                    // Top (+Y)
                    listOf(v010, v110, v111, v011) to Triple(0f, 1f, 0f),
                    // Front (-X facing West in depot, or front)
                    listOf(v000, v001, v011, v010) to Triple(-1f, 0f, 0f),
                    // Back (+X)
                    listOf(v100, v110, v111, v101) to Triple(1f, 0f, 0f),
                    // Side 1 (-Z)
                    listOf(v000, v100, v110, v010) to Triple(0f, 0f, -1f),
                    // Side 2 (+Z)
                    listOf(v001, v101, v111, v011) to Triple(0f, 0f, 1f),
                    // Bottom (-Y)
                    listOf(v000, v100, v101, v001) to Triple(0f, -1f, 0f)
                )

                val baseColor = Color(box.r, box.g, box.b, box.a)

                faces.forEach { (quadPts, normal) ->
                    // Rotate normal by yaw to view space
                    val cosY = cos(yaw)
                    val sinY = sin(yaw)
                    val cosP = cos(pitch)
                    val sinP = sin(pitch)

                    val nxRot = normal.first * cosY - normal.third * sinY
                    val nzRot = normal.first * sinY + normal.third * cosY
                    val nyRot = normal.second * cosP - nzRot * sinP

                    // Backface culling: in orthographic projection, normal pointing towards viewer has -nz in view space
                    val viewNz = normal.second * sinP + nzRot * cosP
                    val isWireframeOnly = shadingMode == "Pure Wireframe" || (renderingBackend.contains("Vulkan") && vulkanPolygonMode == "VK_POLYGON_MODE_LINE")

                    // Include if facing somewhat towards screen or in wireframe
                    if (viewNz >= -0.2f || isWireframeOnly) {
                        val path = Path().apply {
                            moveTo(quadPts[0].first.x, quadPts[0].first.y)
                            lineTo(quadPts[1].first.x, quadPts[1].first.y)
                            lineTo(quadPts[2].first.x, quadPts[2].first.y)
                            lineTo(quadPts[3].first.x, quadPts[3].first.y)
                            close()
                        }

                        val avgDepth = (quadPts[0].second + quadPts[1].second + quadPts[2].second + quadPts[3].second) / 4f

                        // Lighting computation matching Sascha Willems Vulkan pipeline / Renderer:
                        // diff = max(dot(norm, lightDir), 0.22)
                        // ambient = (0.38, 0.42, 0.48), lit = (ambient + diff * (0.85, 0.82, 0.76)) * vColor
                        val dotL = max(0f, normal.first * lx + normal.second * ly + normal.third * lz)
                        val diff = max(dotL, 0.22f)

                        val litR = ((0.38f + diff * 0.85f) * box.r).coerceIn(0f, 1f)
                        val litG = ((0.42f + diff * 0.82f) * box.g).coerceIn(0f, 1f)
                        val litB = ((0.48f + diff * 0.76f) * box.b).coerceIn(0f, 1f)

                        val faceColor = when {
                            isWireframeOnly -> Color.Transparent
                            shadingMode == "Flat Unlit" -> baseColor
                            else -> Color(litR, litG, litB, box.a)
                        }

                        val outlineColor = when {
                            isWireframeOnly -> Color(0xFF00E5FF)
                            shadingMode == "Wireframe Over Solid" -> Color.White.copy(alpha = 0.5f)
                            else -> Color.Black.copy(alpha = 0.25f)
                        }

                        quadsToDraw.add(
                            ProjectedQuad(
                                path = path,
                                color = faceColor,
                                avgDepth = avgDepth,
                                isOutline = true,
                                outlineColor = outlineColor
                            )
                        )
                    }
                }
            }

            // Depth sort: furthest quads drawn first (lowest depth in view space)
            quadsToDraw.sortBy { it.avgDepth }
            quadsToDraw.forEach { quad ->
                if (quad.color != Color.Transparent) {
                    drawPath(quad.path, color = quad.color)
                }
                if (quad.isOutline) {
                    drawPath(
                        quad.path,
                        color = quad.outlineColor,
                        style = Stroke(width = if (shadingMode == "Pure Wireframe") 1.8f else 1.0f)
                    )
                }
            }

            // 3. Render High-Voltage Catenary Overhead Conductors (3D Swept Cylinder vs Box Chord)
            if (mesh.wires.isNotEmpty()) {
                mesh.wires.forEach { wire ->
                    val path = Path()
                    val numPts = 36
                    val projectedNodes = mutableListOf<Pair<Offset, Float>>()

                    val spanLen = sqrt((wire.x2 - wire.x1) * (wire.x2 - wire.x1) + (wire.z2 - wire.z1) * (wire.z2 - wire.z1)).coerceAtLeast(0.1f)
                    val catA = if (wire.catenaryConstantA > 10.0f) wire.catenaryConstantA else (spanLen * spanLen) / (8.0f * wire.sag.coerceAtLeast(0.01f))

                    for (seg in 0..numPts) {
                        val t = seg.toFloat() / numPts.toFloat()
                        val wx = wire.x1 + t * (wire.x2 - wire.x1)
                        val wz = wire.z1 + t * (wire.z2 - wire.z1)

                        // Exact hyperbolic cosine catenary physics: y(u) = a * [cosh(L/2a) - cosh(u/a)]
                        val u = (t - 0.5f) * spanLen
                        val sagDrop = if (catA > 10.0f && catA < 50000.0f) {
                            val coshHalfSpan = kotlin.math.cosh((spanLen / (2.0f * catA)).toDouble()).toFloat()
                            val coshU = kotlin.math.cosh((u / catA).toDouble()).toFloat()
                            (catA * (coshHalfSpan - coshU)).coerceAtLeast(0.0f)
                        } else {
                            4.0f * wire.sag * t * (1.0f - t)
                        }
                        val wy = wire.y1 + t * (wire.y2 - wire.y1) - sagDrop

                        val proj = project(
                            wx, wy, wz,
                            centerModelX, centerModelY, centerModelZ,
                            yaw, pitch, baseScale, cx, cy,
                            cameraDist, isPerspective, fovAngleDeg
                        )

                        projectedNodes.add(proj.first to proj.second)
                        if (seg == 0) {
                            path.moveTo(proj.first.x, proj.first.y)
                        } else {
                            path.lineTo(proj.first.x, proj.first.y)
                        }
                    }

                    if (shadingMode == "Pure Wireframe") {
                        drawPath(
                            path,
                            color = if (wire.isHotPhase) Color(0xFFFFB74D) else Color(0xFFCFD8DC),
                            style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        return@forEach
                    }

                    val wireColor = if (wire.isHotPhase) Color(0xFFFFB74D) else Color(0xFF78909C)
                    val avgDepth = projectedNodes.map { it.second }.average().toFloat()
                    val depthScale = if (isPerspective) (cameraDist / (cameraDist + avgDepth * 0.1f)).coerceIn(0.6f, 1.6f) else 1.0f
                    val baseThickPx = (wire.radiusM * baseScale * 0.55f * depthScale).coerceIn(2.5f, 10.0f)

                    if (wire.useCylinder) {
                        // --- 3D CYLINDRICAL SWEPT TUBE RENDERING ---
                        // 1. Warm electrical energization halo (replaces previous blue/cyan corona)
                        if (wire.isHotPhase) {
                            drawPath(
                                path,
                                color = Color(0xFFFFB300).copy(alpha = 0.25f),
                                style = Stroke(width = baseThickPx * 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // 2. Main cylindrical conductor metallic body (realistic dark aluminum/charcoal graphite)
                        drawPath(
                            path,
                            color = if (wire.isHotPhase) Color(0xFF37474F) else Color(0xFF546E7A),
                            style = Stroke(width = baseThickPx * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 3. Specular crest highlight along cylinder ridge (metallic silver/warm golden reflection)
                        drawPath(
                            path,
                            color = if (wire.isHotPhase) Color(0xFFFFE082) else Color(0xFFECEFF1),
                            style = Stroke(width = (baseThickPx * 0.65f).coerceAtLeast(1.2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 4. Transverse circular rib rings along catenary curve showing circular cross-section
                        for (idx in 3 until projectedNodes.size - 2 step 3) {
                            val curr = projectedNodes[idx].first
                            val next = projectedNodes[idx + 1].first
                            val dx = next.x - curr.x
                            val dy = next.y - curr.y
                            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f)
                            val nx = -dy / len
                            val ny = dx / len
                            val ringR = baseThickPx * 0.9f

                            drawLine(
                                color = if (wire.isHotPhase) Color(0xFFFFCA28).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.75f),
                                start = Offset(curr.x - nx * ringR, curr.y - ny * ringR),
                                end = Offset(curr.x + nx * ringR, curr.y + ny * ringR),
                                strokeWidth = 1.6f,
                                cap = StrokeCap.Round
                            )
                        }
                    } else {
                        // --- RECTANGULAR BOX CHORD RENDERING ---
                        // Coarse 4-sided flat box ribbon with angular edge contours
                        drawPath(
                            path,
                            color = wireColor.copy(alpha = 0.5f),
                            style = Stroke(width = baseThickPx * 2.0f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
                        )
                        // Sharp parallel boundary edges
                        drawPath(
                            path,
                            color = wireColor,
                            style = Stroke(width = 1.4f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
                        )
                        // Transverse square crossbar joints
                        for (idx in 2 until projectedNodes.size - 1 step 2) {
                            val curr = projectedNodes[idx].first
                            val next = projectedNodes[idx + 1].first
                            val dx = next.x - curr.x
                            val dy = next.y - curr.y
                            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f)
                            val nx = -dy / len
                            val ny = dx / len
                            val boxR = baseThickPx * 1.1f

                            drawLine(
                                color = Color.White.copy(alpha = 0.9f),
                                start = Offset(curr.x - nx * boxR, curr.y - ny * boxR),
                                end = Offset(curr.x + nx * boxR, curr.y + ny * boxR),
                                strokeWidth = 1.8f,
                                cap = StrokeCap.Square
                            )
                        }
                    }
                }
            }

            // 4. Coordinate Gizmo in Top-Left
            drawGizmo(cx = 50f, cy = 60f, size = 30f, yaw = yaw, pitch = pitch)

            // 5. Compact on-canvas diagnostic watermark
            val infoText = buildAnnotatedString {
                withStyle(SpanStyle(color = NeonLime, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 9.sp)) {
                    append("VK 1.3 // ")
                }
                withStyle(SpanStyle(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.sp)) {
                    append("${mesh.totalVertices} V • ${mesh.totalTriangles} T • ${mesh.boxes.size} Parts")
                }
            }
            drawText(
                textMeasurer = textMeasurer,
                text = infoText,
                topLeft = Offset(16f, height - 24f)
            )
        }

        // Overlay Viewport Controls (Top Right Camera HUD)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Preset Camera Buttons & Perspective Toggle
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Vulkan 1.3 Hardware Native Badge (Permanent)
                Surface(
                    color = NeonLime.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime),
                    modifier = Modifier.testTag("vulkan_pipeline_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeonLime, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VK 1.3",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime
                        )
                    }
                }

                Surface(
                    color = if (isPerspective) CyberCyan.copy(alpha = 0.22f) else DarkSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPerspective) CyberCyan else DarkSurfaceHighlight),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onTogglePerspective() }
                        .testTag("btn_toggle_perspective")
                ) {
                    Text(
                        text = if (isPerspective) "PERSP" else "ORTHO",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isPerspective) CyberCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                listOf("Front", "Side", "Top", "Isometric").forEach { preset ->
                    Surface(
                        color = DarkSurface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onResetCamera(preset) }
                            .testTag("camera_preset_${preset.lowercase()}")
                    ) {
                        Text(
                            text = preset.take(3).uppercase(),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Zoom & Rotate HUD
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = if (isAutoRotate) CyberCyan.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoRotate) CyberCyan else DarkSurfaceHighlight),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onToggleAutoRotate() }
                        .testTag("btn_toggle_autorotate")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Auto Rotate",
                            tint = if (isAutoRotate) CyberCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    color = DarkSurface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onZoomChange(1.2f) }
                        .testTag("btn_zoom_in")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    color = DarkSurface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onZoomChange(0.83f) }
                        .testTag("btn_zoom_out")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── 3D Projection & Coordinate Mathematics ────────────────────────────────────

private fun project(
    x: Float, y: Float, z: Float,
    centerX: Float, centerY: Float, centerZ: Float,
    yaw: Float, pitch: Float,
    scale: Float,
    cx: Float, cy: Float,
    cameraDist: Float,
    isPerspective: Boolean,
    fovDeg: Float
): Pair<Offset, Float> {
    // 1. Center coordinates relative to model geometric center
    val rx0 = x - centerX
    val ry0 = y - centerY
    val rz0 = z - centerZ

    // 2. Yaw rotation (horizontal azimuth)
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val x1 = rx0 * cosY - rz0 * sinY
    val z1 = rx0 * sinY + rz0 * cosY

    // 3. Pitch rotation (vertical elevation)
    val cosP = cos(pitch)
    val sinP = sin(pitch)
    val y2 = ry0 * cosP - z1 * sinP
    val z2 = ry0 * sinP + z1 * cosP

    // 4. Perspective Projection & View Angle Scaling
    // In camera view space, the viewer looks down the line of sight toward the object center.
    // Points closer to the viewer (front view point / foreground) have z2 > 0 -> distance (cameraDist - z2) is smaller!
    // Points further away (back / end) have z2 < 0 -> distance (cameraDist - z2) is larger!
    val perspectiveFactor = if (isPerspective) {
        val fovRad = Math.toRadians(fovDeg.toDouble()).toFloat()
        val tanHalfFov = tan(fovRad * 0.5f).coerceAtLeast(0.18f)
        val distToCamera = (cameraDist - z2).coerceAtLeast(cameraDist * 0.20f)
        // Magnifies foreground (enlarged at front view point) and tapers background (smaller at end)
        (cameraDist / distToCamera) * (0.577f / tanHalfFov)
    } else {
        1.0f
    }

    val screenX = cx + (x1 * scale * perspectiveFactor)
    val screenY = cy - (y2 * scale * perspectiveFactor)

    return Pair(
        Offset(screenX, screenY),
        z2 // Depth for painter's algorithm sorting: larger z2 is closer to camera
    )
}

private fun DrawScope.drawGroundGrid(
    cx: Float, cy: Float,
    scale: Float,
    yaw: Float, pitch: Float,
    assetType: CppAssetType,
    centerX: Float, centerY: Float, centerZ: Float,
    cameraDist: Float,
    isPerspective: Boolean,
    fovDeg: Float,
    bridgeGridHalfM: Float = 100f
) {
    val halfSize = when (assetType) {
        CppAssetType.GARAGE_001 -> 36f
        CppAssetType.POWER_GRID_NETWORK -> 42f
        CppAssetType.MODULAR_POWER_POST -> 9f
        CppAssetType.TEST_VEHICLE_LORRY -> 7f
        CppAssetType.MODULAR_BRIDGE, CppAssetType.MODULAR_FOOTBRIDGE -> bridgeGridHalfM
        CppAssetType.MODULAR_TOLL_PLAZA -> 45f
        CppAssetType.MODULAR_TUNNEL -> 60f
        CppAssetType.MODULAR_INTERCHANGE -> 120f
        CppAssetType.MODULAR_RETAINING_WALL -> 50f
        CppAssetType.CONNECTED_HIGHWAY_SCENE -> 180f
        CppAssetType.MODULAR_EXPRESSWAY -> 110f
        CppAssetType.MODULAR_SIGNPOST -> 28f
        CppAssetType.MODULAR_BARRIER -> 45f
        CppAssetType.MODULAR_ROAD_BLOCK -> 35f
        CppAssetType.MODULAR_ROAD_NETWORK -> 60f
        CppAssetType.MODULAR_SPLINE_GRAPH -> 100f
        CppAssetType.MODULAR_ROAD_TILE -> 42f
    }
    val step = when (assetType) {
        CppAssetType.GARAGE_001, CppAssetType.POWER_GRID_NETWORK -> 4f
        CppAssetType.MODULAR_BRIDGE, CppAssetType.MODULAR_FOOTBRIDGE, CppAssetType.MODULAR_INTERCHANGE,
        CppAssetType.CONNECTED_HIGHWAY_SCENE, CppAssetType.MODULAR_EXPRESSWAY,
        CppAssetType.MODULAR_ROAD_NETWORK, CppAssetType.MODULAR_SPLINE_GRAPH -> 10f
        else -> 2f
    }

    var z = -halfSize
    while (z <= halfSize) {
        val p1 = project(centerX - halfSize, 0f, centerZ + z, centerX, centerY, centerZ, yaw, pitch, scale, cx, cy, cameraDist, isPerspective, fovDeg).first
        val p2 = project(centerX + halfSize, 0f, centerZ + z, centerX, centerY, centerZ, yaw, pitch, scale, cx, cy, cameraDist, isPerspective, fovDeg).first
        val isMajor = abs(z) < 0.01f
        drawLine(
            color = if (isMajor) CyberCyan.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.07f),
            start = p1,
            end = p2,
            strokeWidth = if (isMajor) 1.5f else 1.0f
        )
        z += step
    }

    var x = -halfSize
    while (x <= halfSize) {
        val p1 = project(centerX + x, 0f, centerZ - halfSize, centerX, centerY, centerZ, yaw, pitch, scale, cx, cy, cameraDist, isPerspective, fovDeg).first
        val p2 = project(centerX + x, 0f, centerZ + halfSize, centerX, centerY, centerZ, yaw, pitch, scale, cx, cy, cameraDist, isPerspective, fovDeg).first
        val isMajor = abs(x) < 0.01f
        drawLine(
            color = if (isMajor) LaserOrange.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.07f),
            start = p1,
            end = p2,
            strokeWidth = if (isMajor) 1.5f else 1.0f
        )
        x += step
    }
}

private fun DrawScope.drawGizmo(cx: Float, cy: Float, size: Float, yaw: Float, pitch: Float) {
    val origin = Offset(cx, cy)
    val cosY = cos(yaw); val sinY = sin(yaw)
    val cosP = cos(pitch); val sinP = sin(pitch)
    fun projAxis(ax: Float, ay: Float, az: Float): Offset {
        val x1 = ax * cosY - az * sinY
        val z1 = ax * sinY + az * cosY
        val y2 = ay * cosP - z1 * sinP
        return Offset(cx + x1 * size, cy - y2 * size)
    }

    // X Axis (Red)
    drawLine(color = LaserOrange, start = origin, end = projAxis(1f, 0f, 0f), strokeWidth = 2.5f)
    // Y Axis (Green / Lime)
    drawLine(color = NeonLime, start = origin, end = projAxis(0f, 1f, 0f), strokeWidth = 2.5f)
    // Z Axis (Blue / Cyan)
    drawLine(color = CyberCyan, start = origin, end = projAxis(0f, 0f, 1f), strokeWidth = 2.5f)
}
