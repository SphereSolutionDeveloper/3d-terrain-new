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
import com.example.engine.Vec3
import com.example.model.GrassBlendType
import com.example.model.GrassSpecies
import com.example.model.GrassViewMode
import com.example.model.Scene3DObjectInstance
import com.example.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun Scene3DCanvas(
    instances: List<Scene3DObjectInstance>,
    selectedInstance: Scene3DObjectInstance?,
    sceneMode: String,
    grassViewMode: GrassViewMode = GrassViewMode.VARIETY,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isAutoRotate: Boolean,
    showGridLines: Boolean,
    shadingMode: String,
    lightAngle: Float,
    onRotateDrag: (Float, Float) -> Unit,
    onPanDrag: (Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onToggleAutoRotate: () -> Unit,
    onResetCamera: (String) -> Unit,
    onSelectInstance: (Scene3DObjectInstance?) -> Unit,
    onOpenInspector: (Scene3DObjectInstance) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomFactor, _ ->
                    if (zoomFactor != 1.0f) {
                        onZoomChange(zoomFactor)
                    }
                    if (pan != Offset.Zero) {
                        onRotateDrag(pan.x, pan.y)
                    }
                }
            }
            .testTag("scene_3d_viewport_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val isGrassMode = sceneMode == "GRASS"
            val isGreenBiome = sceneMode == "TREES" || sceneMode == "GRASS" || sceneMode == "MIXED"

            // 1. Draw Space / Atmospheric backdrop
            drawSceneBackdrop(width, height, isForest = isGreenBiome)

            // Camera framing distance for 10m x 10m field
            val cameraDist = if (isGrassMode && grassViewMode == GrassViewMode.SINGLE) 7.5f else 14.5f
            val baseScale = (1.55f * zoom)

            // 2. Draw 10m x 10m Flat Ground or Single Specimen Pedestal
            drawFlatTerrain10x10(
                width = width,
                height = height,
                yaw = yaw,
                pitch = pitch,
                scale = baseScale,
                panX = panX,
                panY = panY,
                cameraDistance = cameraDist,
                showGridLines = showGridLines,
                isForest = isGreenBiome,
                isGrass = isGrassMode,
                grassViewMode = grassViewMode,
                textMeasurer = textMeasurer
            )

            // 3. Render 3D Objects (Rocks and/or Trees) across the 10x10 grid
            val lightDir = Vec3(cos(lightAngle), 0.85f, sin(lightAngle)).normalized()

            // Sort instances by camera depth (Z-buffer / Painter's algorithm)
            val sortedInstances = instances.sortedByDescending { inst ->
                val v = Vec3(inst.posX, inst.posY, inst.posZ)
                val cosY = cos(yaw); val sinY = sin(yaw)
                val cosP = cos(pitch); val sinP = sin(pitch)
                val rx = v.x * cosY + v.z * sinY
                val rz = -v.x * sinY + v.z * cosY
                -rz * cosP + v.y * sinP
            }

            for (inst in sortedInstances) {
                val isSelected = selectedInstance?.id == inst.id
                draw3DObjectInstance(
                    inst = inst,
                    width = width,
                    height = height,
                    yaw = yaw,
                    pitch = pitch,
                    scale = baseScale,
                    panX = panX,
                    panY = panY,
                    cameraDistance = cameraDist,
                    lightDir = lightDir,
                    shadingMode = shadingMode,
                    isSelected = isSelected
                )
            }
        }

        // --- HUD OVERLAY: Top Floating Bar ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Status Pill
                Surface(
                    color = DarkSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        val badgeColor = when (sceneMode) {
                            "GRASS" -> Color(0xFF39FF14)
                            "TREES" -> NeonLime
                            "MIXED" -> LaserOrange
                            else -> CyberCyan
                        }
                        Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(7.dp)) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        val badgeLabel = when (sceneMode) {
                            "GRASS" -> "10×10m • Dense Turf Meadow (Max 11cm)"
                            "MIXED" -> "10×10m • Woodland Meadow"
                            else -> "10×10m • ${instances.size} Objects"
                        }
                        Text(
                            text = badgeLabel,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                // Vulkan 1.3 Instanced Pipeline Pill
                Surface(
                    color = NeonLime.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "VK 1.3 INSTANCED",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Right Quick Controls (Auto-Rotate & Camera Reset)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = if (isAutoRotate) CyberCyan.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAutoRotate) CyberCyan else DarkSurfaceHighlight
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onToggleAutoRotate() }
                        .testTag("scene_auto_rotate_btn")
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
                        .clickable { onResetCamera("Isometric") }
                        .testTag("scene_reset_camera_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Reset Camera",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- HUD OVERLAY: Bottom Selected Object Card ---
        if (selectedInstance != null) {
            val inst = selectedInstance
            val accent = parseColor(inst.accentColorHex)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("scene_selected_instance_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Object Icon Avatar
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.2f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val icon = when {
                                inst.folder.contains("grass") -> Icons.Default.Grass
                                inst.isTree -> Icons.Default.Park
                                else -> Icons.Default.Terrain
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = inst.title,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Object Info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = inst.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        val subText = if (inst.isGrass) {
                            val species = inst.grassSpecies
                            val blend = inst.grassBlendType
                            val rawH = 0.1003f
                            val heightCm = round(inst.scaleY * rawH * 1000f) / 10f
                            val blendInfo = blend?.blendName ?: "Botanical"
                            if (inst.id == "single_grass_specimen") {
                                "Single Specimen • ${blendInfo} • ${heightCm}cm (Max 11cm, ~2.5mm width)"
                            } else {
                                "${inst.sectorId} • ${blendInfo} • ${heightCm}cm (Max 11cm, ~2.5mm width)"
                            }
                        } else {
                            "${inst.sectorId} • (${round(inst.posX * 10f) / 10f}m, ${round(inst.posZ * 10f) / 10f}m) • ${round(inst.scaleX * 10f) / 10f}m scale"
                        }
                        Text(
                            text = subText,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Inspect Button
                    Button(
                        onClick = { onOpenInspector(inst) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("inspect_single_asset_from_scene_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Inspect",
                            tint = DarkBackground,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "INSPECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBackground
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Deselect 'X' button
                    IconButton(
                        onClick = { onSelectInstance(null) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Deselect",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else if (sceneMode == "GRASS" && grassViewMode == GrassViewMode.VARIETY) {
            // Uncountable Grass Field HUD Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF39FF14).copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF39FF14).copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Grass,
                                contentDescription = "Meadow",
                                tint = Color(0xFF39FF14),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "100m² Scene Grid • Millions Uncountable Grass Blades",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "5 Botanical Species • 6 Blend Types • Max 11cm Height • 2.0-4.5mm Width",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF39FF14).copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3D RENDERING PIPELINE FUNCTIONS
// ---------------------------------------------------------------------------

private fun DrawScope.drawSceneBackdrop(width: Float, height: Float, isForest: Boolean) {
    val horizonY = height * 0.45f
    // Atmospheric sky gradient with natural horizon haze
    val skyBrush = Brush.verticalGradient(
        colors = if (isForest) {
            listOf(Color(0xFF071714), Color(0xFF102824), Color(0xFF1A3830))
        } else {
            listOf(Color(0xFF09121E), Color(0xFF142436), Color(0xFF1F3248))
        },
        startY = 0f,
        endY = horizonY
    )
    drawRect(brush = skyBrush, size = androidx.compose.ui.geometry.Size(width, horizonY))

    // Ground haze
    val groundBrush = Brush.verticalGradient(
        colors = if (isForest) {
            listOf(Color(0xFF1A3830), Color(0xFF12241E), Color(0xFF0A1410))
        } else {
            listOf(Color(0xFF1F3248), Color(0xFF14202C), Color(0xFF0B1218))
        },
        startY = horizonY,
        endY = height
    )
    drawRect(
        brush = groundBrush,
        topLeft = Offset(0f, horizonY),
        size = androidx.compose.ui.geometry.Size(width, height - horizonY)
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawFlatTerrain10x10(
    width: Float,
    height: Float,
    yaw: Float,
    pitch: Float,
    scale: Float,
    panX: Float,
    panY: Float,
    cameraDistance: Float,
    showGridLines: Boolean,
    isForest: Boolean,
    isGrass: Boolean = false,
    grassViewMode: GrassViewMode = GrassViewMode.VARIETY,
    textMeasurer: TextMeasurer
) {
    // -------------------------------------------------------------
    // Single Grass Specimen Pedestal View
    // -------------------------------------------------------------
    if (isGrass && grassViewMode == GrassViewMode.SINGLE) {
        val numSegments = 32
        val radius = 0.85f
        val pedestalPoly = (0 until numSegments).map { i ->
            val angle = i * 2f * Math.PI.toFloat() / numSegments
            Vec3(cos(angle) * radius, 0f, sin(angle) * radius)
        }
        val projPedestal = pedestalPoly.map { pt ->
            val tv = Math3D.transformVertex(pt, yaw, pitch, scale, panX, panY, cameraDistance)
            Math3D.projectPerspective(tv, width, height)
        }
        if (projPedestal.all { it.isVisible }) {
            val path = Path().apply {
                moveTo(projPedestal[0].screenX, projPedestal[0].screenY)
                for (p in projPedestal.drop(1)) {
                    lineTo(p.screenX, p.screenY)
                }
                close()
            }
            // Rich dark loam soil
            drawPath(path = path, color = Color(0xFF1B2C1C))
            // Neon lime glowing pedestal rim
            drawPath(path = path, color = Color(0xFF39FF14).copy(alpha = 0.7f), style = Stroke(width = 2.0f))
        }

        // Inner circular measurement ring (0.45m radius)
        val ringPoly = (0 until 24).map { i ->
            val angle = i * 2f * Math.PI.toFloat() / 24
            Vec3(cos(angle) * 0.45f, 0f, sin(angle) * 0.45f)
        }
        val projRing = ringPoly.map { pt ->
            val tv = Math3D.transformVertex(pt, yaw, pitch, scale, panX, panY, cameraDistance)
            Math3D.projectPerspective(tv, width, height)
        }
        if (projRing.all { it.isVisible }) {
            val ringPath = Path().apply {
                moveTo(projRing[0].screenX, projRing[0].screenY)
                for (p in projRing.drop(1)) {
                    lineTo(p.screenX, p.screenY)
                }
                close()
            }
            drawPath(path = ringPath, color = Color(0xFF39FF14).copy(alpha = 0.35f), style = Stroke(width = 1.0f))
        }
        return
    }

    // -------------------------------------------------------------
    // Full 10x10m Terrain Field View
    // -------------------------------------------------------------
    val halfSize = 5.0f // 10m total (-5m to +5m)
    val gridColor = when {
        isGrass -> Color(0xFF39FF14)
        isForest -> Color(0xFF55C77A)
        else -> Color(0xFF38B0DE)
    }
    // Rich, solid earthen ground terrain: lush dense meadow topsoil vs forest vs craggy regolith
    val tileColor = when {
        isGrass -> Color(0xFF163820)
        isForest -> Color(0xFF1B3B2B)
        else -> Color(0xFF222B36)
    }

    // 1. Draw flat terrain slab polygon
    val corners = listOf(
        Vec3(-halfSize, 0.0f, -halfSize),
        Vec3(halfSize, 0.0f, -halfSize),
        Vec3(halfSize, 0.0f, halfSize),
        Vec3(-halfSize, 0.0f, halfSize)
    )

    val projCorners = corners.map { pt ->
        val tv = Math3D.transformVertex(pt, yaw, pitch, scale, panX, panY, cameraDistance)
        Math3D.projectPerspective(tv, width, height)
    }

    if (projCorners.all { it.isVisible }) {
        val path = Path().apply {
            moveTo(projCorners[0].screenX, projCorners[0].screenY)
            lineTo(projCorners[1].screenX, projCorners[1].screenY)
            lineTo(projCorners[2].screenX, projCorners[2].screenY)
            lineTo(projCorners[3].screenX, projCorners[3].screenY)
            close()
        }
        drawPath(path = path, color = tileColor)
        drawPath(path = path, color = gridColor.copy(alpha = 0.85f), style = Stroke(width = 2.0f))
    }

    // Dense organic turf micro-underlayer across grass field:
    // Seamless uncountable sod blades covering the ground with 6 blend types and weathered brown thatch
    if (isGrass) {
        val blendPalette = listOf(
            Color(0xFF35A848), // 0: Lush Chlorophyll
            Color(0xFF4EE060), // 1: Emerald Spring
            Color(0xFF2E6B37), // 2: Olive Understory
            Color(0xFF7CB832), // 3: Golden Lime
            Color(0xFFB5A642), // 4: Senescent Gold
            Color(0xFF8A6A40)  // 5: Weathered Brown Thatch
        )

        // Millions uncountable grass representation:
        // High-density stochastic matrix across 10m x 10m ground with interlocking dual-blade tufts
        for (gx in -26..26) {
            val wx = gx * 0.185f
            for (gz in -26..26) {
                val wz = gz * 0.185f
                val hash = ((gx * 73 + gz * 127 + 59) and 0xFFFF)
                val jitterX = ((hash % 19) - 9) * 0.009f
                val jitterZ = (((hash / 19) % 19) - 9) * 0.009f

                val cx = wx + jitterX
                val cz = wz + jitterZ

                // Dynamic blend selection with brown weathered thatch and golden senescence
                val colorIdx = when {
                    (hash % 17) == 0 -> 5 // Weathered Brown Thatch (~6%)
                    (hash % 11) == 0 -> 4 // Senescent Gold (~9%)
                    (hash % 5) == 0  -> 3 // Golden Lime
                    (hash % 3) == 0  -> 1 // Emerald Spring
                    (hash % 2) == 0  -> 2 // Olive Understory
                    else             -> 0 // Lush Chlorophyll
                }
                val color = blendPalette[colorIdx]

                // Blade 1: Main upright blade (6.5cm - 10.8cm, strictly <= 11cm max)
                val stemHeight = 0.065f + ((hash % 19) * 0.0022f)
                val leanX1 = ((hash % 13) - 6) * 0.004f
                val leanZ1 = (((hash / 13) % 13) - 6) * 0.004f

                val b1 = Math3D.transformVertex(Vec3(cx, 0f, cz), yaw, pitch, scale, panX, panY, cameraDistance)
                val t1 = Math3D.transformVertex(Vec3(cx + leanX1, stemHeight, cz + leanZ1), yaw, pitch, scale, panX, panY, cameraDistance)
                val pb1 = Math3D.projectPerspective(b1, width, height)
                val pt1 = Math3D.projectPerspective(t1, width, height)

                if (pb1.isVisible && pt1.isVisible) {
                    drawLine(
                        color = color.copy(alpha = 0.88f),
                        start = Offset(pb1.screenX, pb1.screenY),
                        end = Offset(pt1.screenX, pt1.screenY),
                        strokeWidth = if (colorIdx == 5) 1.2f else 1.5f
                    )
                }

                // Blade 2: Interlocking arching side blade (creates unbroken continuous weave)
                val stemHeight2 = stemHeight * 0.82f
                val leanX2 = -leanX1 * 1.3f + (((hash % 7) - 3) * 0.003f)
                val leanZ2 = -leanZ1 * 1.3f + ((((hash / 7) % 7) - 3) * 0.003f)

                val t2 = Math3D.transformVertex(Vec3(cx + leanX2, stemHeight2, cz + leanZ2), yaw, pitch, scale, panX, panY, cameraDistance)
                val pt2 = Math3D.projectPerspective(t2, width, height)

                if (pb1.isVisible && pt2.isVisible) {
                    val sideColor = if (colorIdx == 5) blendPalette[4] else blendPalette[(colorIdx + 1) % 4]
                    drawLine(
                        color = sideColor.copy(alpha = 0.75f),
                        start = Offset(pb1.screenX, pb1.screenY),
                        end = Offset(pt2.screenX, pt2.screenY),
                        strokeWidth = 1.2f
                    )
                }
            }
        }
    }

    // 2. Draw 1m x 1m subdivisions lines (10 by 10)
    if (showGridLines) {
        val step = 1.0f
        // X lines (parallel to Z)
        for (i in -5..5) {
            val x = i * step
            val p1 = Math3D.transformVertex(Vec3(x, 0f, -halfSize), yaw, pitch, scale, panX, panY, cameraDistance)
            val p2 = Math3D.transformVertex(Vec3(x, 0f, halfSize), yaw, pitch, scale, panX, panY, cameraDistance)
            val pr1 = Math3D.projectPerspective(p1, width, height)
            val pr2 = Math3D.projectPerspective(p2, width, height)
            if (pr1.isVisible && pr2.isVisible) {
                val isAxis = (i == 0)
                drawLine(
                    color = if (isAxis) gridColor.copy(alpha = 0.95f) else gridColor.copy(alpha = 0.40f),
                    start = Offset(pr1.screenX, pr1.screenY),
                    end = Offset(pr2.screenX, pr2.screenY),
                    strokeWidth = if (isAxis) 2.0f else 1.0f
                )
            }
        }

        // Z lines (parallel to X)
        for (j in -5..5) {
            val z = j * step
            val p1 = Math3D.transformVertex(Vec3(-halfSize, 0f, z), yaw, pitch, scale, panX, panY, cameraDistance)
            val p2 = Math3D.transformVertex(Vec3(halfSize, 0f, z), yaw, pitch, scale, panX, panY, cameraDistance)
            val pr1 = Math3D.projectPerspective(p1, width, height)
            val pr2 = Math3D.projectPerspective(p2, width, height)
            if (pr1.isVisible && pr2.isVisible) {
                val isAxis = (j == 0)
                drawLine(
                    color = if (isAxis) gridColor.copy(alpha = 0.95f) else gridColor.copy(alpha = 0.40f),
                    start = Offset(pr1.screenX, pr1.screenY),
                    end = Offset(pr2.screenX, pr2.screenY),
                    strokeWidth = if (isAxis) 2.0f else 1.0f
                )
            }
        }
    }
}

private fun DrawScope.draw3DObjectInstance(
    inst: Scene3DObjectInstance,
    width: Float,
    height: Float,
    yaw: Float,
    pitch: Float,
    scale: Float,
    panX: Float,
    panY: Float,
    cameraDistance: Float,
    lightDir: Vec3,
    shadingMode: String,
    isSelected: Boolean
) {
    val geom = inst.geometry ?: return
    val verts = geom.vertices
    if (verts.isEmpty()) return

    val cosRot = cos(inst.rotationYaw)
    val sinRot = sin(inst.rotationYaw)

    val transformedVerts = ArrayList<ProjectedPoint>(verts.size)

    for (v in verts) {
        // 1. Scale
        val sx = v[0] * inst.scaleX
        val sy = v[1] * inst.scaleY
        val sz = v[2] * inst.scaleZ

        // 2. Rotate around Y (noisy procedural orientation)
        val rx = sx * cosRot + sz * sinRot
        val ry = sy
        val rz = -sx * sinRot + sz * cosRot

        // 3. Translate to sector world position
        val wx = rx + inst.posX
        val wy = ry + inst.posY
        val wz = rz + inst.posZ

        // 4. Transform through camera matrix
        val camV = Math3D.transformVertex(
            vertex = Vec3(wx, wy, wz),
            yawRad = yaw,
            pitchRad = pitch,
            scale = scale,
            panX = panX,
            panY = panY,
            cameraDistance = cameraDistance
        )

        transformedVerts.add(Math3D.projectPerspective(camV, width, height))
    }

    val accent = parseColor(inst.accentColorHex)

    // Draw Ground Selection Marker
    if (isSelected) {
        val groundPt = Math3D.transformVertex(
            Vec3(inst.posX, inst.posY, inst.posZ),
            yaw, pitch, scale, panX, panY, cameraDistance
        )
        val projGround = Math3D.projectPerspective(groundPt, width, height)
        if (projGround.isVisible) {
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = 16f,
                center = Offset(projGround.screenX, projGround.screenY)
            )
            drawCircle(
                color = accent,
                radius = 16f,
                center = Offset(projGround.screenX, projGround.screenY),
                style = Stroke(width = 2.0f)
            )
        }
    }

    // Render Triangles
    val faces = geom.faces
    val uvs = geom.uvs
    val albedoBmp = inst.albedoBitmap

    for (f in faces) {
        if (f.size < 3) continue
        val i0 = f[0]; val i1 = f[1]; val i2 = f[2]
        val p0 = transformedVerts.getOrNull(i0) ?: continue
        val p1 = transformedVerts.getOrNull(i1) ?: continue
        val p2 = transformedVerts.getOrNull(i2) ?: continue

        if (!p0.isVisible || !p1.isVisible || !p2.isVisible) continue

        // 2D Screen-space cross product for back-face culling
        val edge1x = p1.screenX - p0.screenX; val edge1y = p1.screenY - p0.screenY
        val edge2x = p2.screenX - p0.screenX; val edge2y = p2.screenY - p0.screenY
        val crossZ = edge1x * edge2y - edge1y * edge2x

        // Grass blades are double-sided ribbon polygons; non-vegetation solid meshes use front-face culling (crossZ > 0)
        val shouldRenderFace = if (inst.isGrass) {
            abs(crossZ) > 0.001f
        } else {
            crossZ > 0.0f
        }

        if (shadingMode != "Wireframe" && shouldRenderFace) {
            // Facing front in screen space or double-sided grass blade
            val path = Path().apply {
                moveTo(p0.screenX, p0.screenY)
                lineTo(p1.screenX, p1.screenY)
                lineTo(p2.screenX, p2.screenY)
                close()
            }

            if (shadingMode == "Solid PBR") {
                // Compute average UV for this triangle
                val triU = if (uvs.size > maxOf(i0, i1, i2) && uvs[i0].isNotEmpty()) {
                    (uvs[i0][0] + uvs[i1][0] + uvs[i2][0]) / 3f
                } else 0.5f

                val triV = if (uvs.size > maxOf(i0, i1, i2) && uvs[i0].size > 1) {
                    (uvs[i0][1] + uvs[i1][1] + uvs[i2][1]) / 3f
                } else 0.5f

                // Sample genuine texture albedo bitmap or botanical blend colors
                var r = accent.red
                var g = accent.green
                var b = accent.blue

                if (inst.isGrass && inst.grassBlendType != null) {
                    val blend = inst.grassBlendType
                    // Vertical gradient along blade from base to tip (triV)
                    val t = triV.coerceIn(0f, 1f)
                    val baseC = blend.baseColor
                    val tipC = blend.tipColor
                    val grassR = baseC.red * (1f - t) + tipC.red * t
                    val grassG = baseC.green * (1f - t) + tipC.green * t
                    val grassB = baseC.blue * (1f - t) + tipC.blue * t

                    if (albedoBmp != null) {
                        val px = (triU * (albedoBmp.width - 1)).toInt().coerceIn(0, albedoBmp.width - 1)
                        val py = ((1f - triV) * (albedoBmp.height - 1)).toInt().coerceIn(0, albedoBmp.height - 1)
                        val pixel = albedoBmp.getPixel(px, py)
                        val bmpR = ((pixel shr 16) and 0xFF) / 255f
                        val bmpG = ((pixel shr 8) and 0xFF) / 255f
                        val bmpB = (pixel and 0xFF) / 255f
                        // Blend texture details with botanical blend type
                        r = grassR * 0.7f + bmpR * 0.3f
                        g = grassG * 0.7f + bmpG * 0.3f
                        b = grassB * 0.7f + bmpB * 0.3f
                    } else {
                        r = grassR
                        g = grassG
                        b = grassB
                    }
                } else if (albedoBmp != null) {
                    val px = (triU * (albedoBmp.width - 1)).toInt().coerceIn(0, albedoBmp.width - 1)
                    val py = ((1f - triV) * (albedoBmp.height - 1)).toInt().coerceIn(0, albedoBmp.height - 1)
                    val pixel = albedoBmp.getPixel(px, py)
                    r = ((pixel shr 16) and 0xFF) / 255f
                    g = ((pixel shr 8) and 0xFF) / 255f
                    b = (pixel and 0xFF) / 255f
                } else if (inst.isGrass) {
                    // Vivid lush green meadow blade
                    r = 0.22f
                    g = 0.82f
                    b = 0.28f
                } else if (inst.isTree) {
                    r = if (inst.folder.contains("pine")) 0.18f else 0.24f
                    g = if (inst.folder.contains("pine")) 0.44f else 0.63f
                    b = if (inst.folder.contains("pine")) 0.25f else 0.33f
                }

                // Surface normal estimation from screen edges for realistic solar diffuse
                val invLen = 1f / (sqrt(edge1x * edge1x + edge1y * edge1y).coerceAtLeast(1f))
                val sign = if (crossZ < 0f && inst.isGrass) -1f else 1f
                val nx = -edge1y * invLen * sign
                val ny = edge1x * invLen * sign
                val dotL = abs(nx * lightDir.x + ny * lightDir.y)
                val diffuse = if (inst.isGrass) {
                    // Translucent subsurface scattering effect for organic grass foliage
                    0.55f + 0.45f * dotL
                } else {
                    0.42f + 0.58f * dotL.coerceAtLeast(0f)
                }

                val finalColor = Color(
                    red = (r * diffuse).coerceIn(0f, 1f),
                    green = (g * diffuse).coerceIn(0f, 1f),
                    blue = (b * diffuse).coerceIn(0f, 1f),
                    alpha = 1.0f
                )

                drawPath(path = path, color = finalColor)
                drawPath(
                    path = path,
                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else if (inst.isGrass) Color(0xFF1B4D20).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.20f),
                    style = Stroke(width = if (isSelected) 1.5f else if (inst.isGrass) 0.4f else 0.6f)
                )
            } else {
                drawPath(path = path, color = accent)
                drawPath(path = path, color = Color.Black.copy(alpha = 0.3f), style = Stroke(width = 0.8f))
            }
        } else if (shadingMode == "Wireframe") {
            drawLine(accent, Offset(p0.screenX, p0.screenY), Offset(p1.screenX, p1.screenY), strokeWidth = 1.0f)
            drawLine(accent, Offset(p1.screenX, p1.screenY), Offset(p2.screenX, p2.screenY), strokeWidth = 1.0f)
            drawLine(accent, Offset(p2.screenX, p2.screenY), Offset(p0.screenX, p0.screenY), strokeWidth = 1.0f)
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        CyberCyan
    }
}
