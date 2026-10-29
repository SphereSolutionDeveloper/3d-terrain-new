package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NativeSourceCatalog
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun AudioProceduralScreen(
    modifier: Modifier = Modifier
) {
    var masterVolume by remember { mutableFloatStateOf(0.85f) }
    var spatialOcclusion by remember { mutableFloatStateOf(0.4f) }
    var activeSurface by remember { mutableStateOf("GRASS") }
    var isRainActive by remember { mutableStateOf(true) }
    var isForestActive by remember { mutableStateOf(false) }

    var selectedFile by remember { mutableStateOf(NativeSourceCatalog.audioFiles.first()) }
    val clipboardManager = LocalClipboardManager.current

    var isControlsCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. 3D Spatial Audio Radar Viewport
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
                val cy = h * 0.5f

                // Draw radar circles
                val maxR = (w.coerceAtMost(h) * 0.42f)
                drawCircle(Color.White.copy(alpha = 0.08f), radius = maxR, center = Offset(cx, cy), style = Stroke(1.5f))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = maxR * 0.66f, center = Offset(cx, cy), style = Stroke(1.5f))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = maxR * 0.33f, center = Offset(cx, cy), style = Stroke(1.5f))

                // Crosshairs
                drawLine(Color.White.copy(alpha = 0.1f), Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.1f), Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 1f)

                // Listener (Center point)
                drawCircle(CyberCyan, radius = 7f, center = Offset(cx, cy))
                // Listener heading cone
                drawLine(CyberCyan, Offset(cx, cy), Offset(cx, cy - 24f), strokeWidth = 3f)

                // Sound Emitters around the scene
                val rainAngle = 0.8f
                val rainX = cx + cos(rainAngle) * maxR * 0.7f
                val rainY = cy + sin(rainAngle) * maxR * 0.7f
                drawCircle(HologramBlue, radius = 6f, center = Offset(rainX, rainY))
                drawCircle(HologramBlue.copy(alpha = 0.25f), radius = 22f, center = Offset(rainX, rainY), style = Stroke(1.5f))

                val footstepAngle = 2.4f
                val footX = cx + cos(footstepAngle) * maxR * 0.4f
                val footY = cy + sin(footstepAngle) * maxR * 0.4f
                drawCircle(NeonLime, radius = 6f, center = Offset(footX, footY))
            }

            // HUD
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("3D SPATIAL AUDIO RADAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Text("HRTF • Doppler • Distance Attenuation • DSP Synth", fontSize = 9.sp, color = TextSecondary)
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
                    .testTag("audio_fullscreen_toggle_btn")
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
                .testTag("audio_controls_dock_bar")
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
                        text = if (isControlsCollapsed) "▲ CONTROLS: AUDIO DSP SYNTH" else "▼ PUSH DOWN CONTROLS",
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
                        text = "VOL: ${(masterVolume * 100).toInt()}% • $activeSurface",
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

        // 3. Audio Parameters & Files (Pushed down into lower area)
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
                    Text("NATIVE AUDIO SUBSYSTEM (native-assets/src/audio/)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Text("Procedural DSP sound generation without prerecorded audio clips.", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Sliders
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Master Volume", fontSize = 11.sp, color = TextPrimary)
                    Text("${(masterVolume * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                Slider(
                    value = masterVolume,
                    onValueChange = { masterVolume = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                    modifier = Modifier.height(28.dp)
                )
            }

            // Footstep material selector
            Text("Footstep Surface Synthesis:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("GRASS", "CONCRETE", "GRAVEL", "WOOD").forEach { surface ->
                    val isSel = activeSurface == surface
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) NeonLime.copy(alpha = 0.2f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonLime else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.clickable { activeSurface = surface }
                    ) {
                        Text(surface, fontSize = 11.sp, color = if (isSel) NeonLime else TextSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            // Native files
            Text("C++ Audio Engine Architecture:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(NativeSourceCatalog.audioFiles) { file ->
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
