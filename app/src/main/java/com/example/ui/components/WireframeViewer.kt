package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FullAssetBundle
import com.example.ui.theme.*

@Composable
fun WireframeViewer(
    bundle: FullAssetBundle,
    lineWidth: Float,
    showVertices: Boolean,
    showBoundingBox: Boolean,
    showFloorGrid: Boolean,
    selectedPalette: String,
    onLineWidthChange: (Float) -> Unit,
    onToggleVertices: () -> Unit,
    onToggleBoundingBox: () -> Unit,
    onToggleFloorGrid: () -> Unit,
    onPaletteSelect: (String) -> Unit,
    onSelectCameraPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("wireframe_controls_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Grid4x4,
                        contentDescription = "Wireframe Mode",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HOLOGRAPHIC WIREFRAME ENGINE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder)
                ) {
                    Text(
                        text = "${bundle.wireframe.edgeCount} EDGES",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Camera Presets
            Text(
                text = "CAMERA PERSPECTIVES",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Isometric", "Front", "Top", "Side").forEach { preset ->
                    OutlinedButton(
                        onClick = { onSelectCameraPreset(preset) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_preset_${preset.lowercase()}")
                    ) {
                        Text(text = preset, fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Line Width Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "LINE THICKNESS: ${String.format("%.1f", lineWidth)} px",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
            Slider(
                value = lineWidth,
                onValueChange = onLineWidthChange,
                valueRange = 1.0f..6.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = DarkSurfaceHighlight
                ),
                modifier = Modifier.testTag("wireframe_line_width_slider")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Toggles
            Text(
                text = "OVERLAYS & GIZMOS",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = showVertices,
                    onClick = onToggleVertices,
                    label = { Text("Vertex Nodes", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(12.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = showBoundingBox,
                    onClick = onToggleBoundingBox,
                    label = { Text("Bound Cage", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = null, modifier = Modifier.size(12.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = showFloorGrid,
                    onClick = onToggleFloorGrid,
                    label = { Text("3D Grid", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.BorderAll, contentDescription = null, modifier = Modifier.size(12.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color Palettes
            Text(
                text = "HOLOGRAPHIC PALETTE",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "Neon Cyan" to CyberCyan,
                    "Electric Violet" to ElectricViolet,
                    "Cyber Green" to NeonLime,
                    "Laser Orange" to LaserOrange
                ).forEach { (name, color) ->
                    val isSelected = selectedPalette == name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) color.copy(alpha = 0.2f) else DarkSurfaceElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) color else DarkSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onPaletteSelect(name) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = name.split(" ")[0],
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
