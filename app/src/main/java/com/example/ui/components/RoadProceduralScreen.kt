package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NativeFileTreeItem
import com.example.model.NativeSourceCatalog
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun RoadProceduralScreen(
    modifier: Modifier = Modifier
) {
    var laneCount by remember { mutableFloatStateOf(2f) }
    var laneWidthMeters by remember { mutableFloatStateOf(3.5f) }
    var curveBankingDeg by remember { mutableFloatStateOf(4.5f) }
    var asphaltWearPct by remember { mutableFloatStateOf(20f) }
    var hasGuardrails by remember { mutableStateOf(true) }
    var hasCenterMarkings by remember { mutableStateOf(true) }

    var selectedFile by remember { mutableStateOf(NativeSourceCatalog.roadFiles.first()) }
    val clipboardManager = LocalClipboardManager.current

    var isControlsCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Road Cross Section & Spline Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isControlsCollapsed) 1f else 2.2f)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, GlowingCyanBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f
                val cy = h * 0.55f

                // Draw 2D Spline Perspective Highway View
                val roadBottomWidth = (laneCount * laneWidthMeters * 26f).coerceIn(120f, w * 0.85f)
                val roadTopWidth = roadBottomWidth * 0.25f
                val topY = cy - h * 0.32f
                val bottomY = cy + h * 0.35f

                // Subgrade base layer (Gravel)
                val baseLeftBottom = cx - roadBottomWidth * 0.58f
                val baseRightBottom = cx + roadBottomWidth * 0.58f
                val baseLeftTop = cx - roadTopWidth * 0.58f
                val baseRightTop = cx + roadTopWidth * 0.58f

                val basePath = Path().apply {
                    moveTo(baseLeftTop, topY)
                    lineTo(baseRightTop, topY)
                    lineTo(baseRightBottom, bottomY)
                    lineTo(baseLeftBottom, bottomY)
                    close()
                }
                drawPath(basePath, color = Color(0xFF424242))

                // Asphalt Wearing Course
                val roadLeftBottom = cx - roadBottomWidth * 0.5f
                val roadRightBottom = cx + roadBottomWidth * 0.5f
                val roadLeftTop = cx - roadTopWidth * 0.5f
                val roadRightTop = cx + roadTopWidth * 0.5f

                val roadPath = Path().apply {
                    moveTo(roadLeftTop, topY)
                    lineTo(roadRightTop, topY)
                    lineTo(roadRightBottom, bottomY)
                    lineTo(roadLeftBottom, bottomY)
                    close()
                }
                val wearColor = Color(0xFF1E2129)
                drawPath(roadPath, color = wearColor)

                // Road Center and Lane Markings
                if (hasCenterMarkings) {
                    val segments = 10
                    for (i in 0 until segments) {
                        val t0 = i.toFloat() / segments
                        val t1 = (i + 0.5f) / segments
                        val y0 = topY + (bottomY - topY) * t0
                        val y1 = topY + (bottomY - topY) * t1
                        val markWidth = 2f + 4f * t0
                        drawLine(
                            color = Color(0xFFFFD54F),
                            start = Offset(cx, y0),
                            end = Offset(cx, y1),
                            strokeWidth = markWidth
                        )
                    }
                }

                // White Edge Lines
                drawLine(Color(0xFFEEEEEE), Offset(roadLeftTop, topY), Offset(roadLeftBottom, bottomY), strokeWidth = 3f)
                drawLine(Color(0xFFEEEEEE), Offset(roadRightTop, topY), Offset(roadRightBottom, bottomY), strokeWidth = 3f)

                // Guardrails if enabled
                if (hasGuardrails) {
                    drawLine(Color(0xFF90A4AE), Offset(roadLeftTop - 6f, topY), Offset(roadLeftBottom - 14f, bottomY), strokeWidth = 4f)
                    drawLine(Color(0xFF90A4AE), Offset(roadRightTop + 6f, topY), Offset(roadRightBottom + 14f, bottomY), strokeWidth = 4f)
                }
            }

            // HUD Info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(
                        text = "VULKAN PROCEDURAL ROAD PROFILE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Text(
                        text = "${laneCount.toInt()} Lanes • Width ${(laneCount * laneWidthMeters).toInt()}m • Banking: ${curveBankingDeg}°",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }

            // Floating Fullscreen / Push-Down toggle button in top-right
            IconButton(
                onClick = { isControlsCollapsed = !isControlsCollapsed },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isControlsCollapsed) CyberCyan.copy(alpha = 0.35f) else DarkSurface.copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .testTag("road_fullscreen_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isControlsCollapsed) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isControlsCollapsed) "Show Controls" else "Maximize Scene",
                    tint = if (isControlsCollapsed) CyberCyan else TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. Control Panel Header / Pull Dock (Pushes controls down or expands them)
        Surface(
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isControlsCollapsed = !isControlsCollapsed }
                .testTag("road_controls_dock_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isControlsCollapsed) CyberCyan else TextMuted)
                    )
                    Text(
                        text = if (isControlsCollapsed) "▲ CONTROLS: ROAD PARAMETERS" else "▼ PUSH DOWN CONTROLS",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isControlsCollapsed) CyberCyan else TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${laneCount.toInt()} LANES • ${curveBankingDeg.toInt()}° BANK",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    Icon(
                        imageVector = if (isControlsCollapsed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isControlsCollapsed) "Expand Controls" else "Push Controls Down",
                        tint = if (isControlsCollapsed) CyberCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3. Road Parameters & Native File Tree (Pushed down into lower area)
        AnimatedVisibility(
            visible = !isControlsCollapsed,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("ROAD SUBSYSTEM (native-assets/src/road/)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Text("Generates continuous spline highways, intersections, cross-section extrusions, and pavement shaders.", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Sliders
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Number of Lanes", fontSize = 11.sp, color = TextPrimary)
                    Text("${laneCount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                Slider(
                    value = laneCount,
                    onValueChange = { laneCount = it },
                    valueRange = 1f..6f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                    modifier = Modifier.height(28.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lane Width (m)", fontSize = 11.sp, color = TextPrimary)
                    Text("${laneWidthMeters}m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                Slider(
                    value = laneWidthMeters,
                    onValueChange = { laneWidthMeters = it },
                    valueRange = 2.5f..4.5f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                    modifier = Modifier.height(28.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Extruded Steel Guardrails", fontSize = 11.sp, color = TextPrimary)
                Switch(checked = hasGuardrails, onCheckedChange = { hasGuardrails = it })
            }

            // Native files
            Text("C++ Road Architecture:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(NativeSourceCatalog.roadFiles) { file ->
                    val isSelected = file.path == selectedFile.path
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.clickable { selectedFile = file }
                    ) {
                        Text(file.name, fontSize = 11.sp, color = if (isSelected) CyberCyan else TextPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }

            // Code viewer
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF070B12),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(selectedFile.path, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(selectedFile.codeSnippet)) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(selectedFile.codeSnippet, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = NeonLime, lineHeight = 13.sp)
                }
            }
        }
    }
}
}
