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
fun ShadedGlbViewer(
    bundle: FullAssetBundle,
    shadingMode: String,
    lightAngle: Float,
    onShadingModeSelect: (String) -> Unit,
    onLightAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("shaded_glb_controls_card")
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
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "GLB 3D Shaded",
                        tint = ElectricViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GLB SHADER & 3D SOLID RENDERER",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    color = if (bundle.glbHeader.isValid) NeonLime.copy(alpha = 0.15f) else LaserOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (bundle.glbHeader.isValid) NeonLime.copy(alpha = 0.5f) else LaserOrange.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (bundle.glbHeader.isValid) "GLTF 2.0 VALID" else "UNVERIFIED",
                        color = if (bundle.glbHeader.isValid) NeonLime else LaserOrange,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shading Models
            Text(
                text = "SHADER VISUALIZATION MODES",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "Solid Metallic",
                    "Lit PBR Shaded",
                    "Volume Shapes",
                    "Surface Normals",
                    "X-Ray Hologram",
                    "Wireframe Over Solid"
                ).forEach { mode ->
                    val isSelected = shadingMode == mode
                    val label = when (mode) {
                        "Solid Metallic" -> "Metallic"
                        "Lit PBR Shaded" -> "Lit PBR"
                        "Volume Shapes" -> "Volumes"
                        "Surface Normals" -> "Normals"
                        "X-Ray Hologram" -> "X-Ray"
                        "Wireframe Over Solid" -> "Wire+Solid"
                        else -> mode
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ElectricViolet.copy(alpha = 0.2f) else DarkSurfaceElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ElectricViolet else DarkSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onShadingModeSelect(mode) }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Light Angle Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SOLAR LIGHT AZIMUTH: ${(lightAngle * 180f / Math.PI.toFloat()).toInt()}°",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
            Slider(
                value = lightAngle,
                onValueChange = onLightAngleChange,
                valueRange = 0f..(2f * Math.PI.toFloat()),
                colors = SliderDefaults.colors(
                    thumbColor = ElectricViolet,
                    activeTrackColor = ElectricViolet,
                    inactiveTrackColor = DarkSurfaceHighlight
                ),
                modifier = Modifier.testTag("light_angle_slider")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // GLB Binary Container Spec Matrix
            Text(
                text = "BINARY GLTF 2.0 (GLB) CONTAINER SPECS",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpecDataPill("Header Magic", bundle.glbHeader.magic)
                        SpecDataPill("glTF Version", "${bundle.glbHeader.version}.0")
                        SpecDataPill("File Length", "${bundle.glbHeader.length} B")
                    }
                    Divider(color = DarkSurfaceHighlight, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpecDataPill("JSON Chunk", "${bundle.glbHeader.jsonLength} B")
                        SpecDataPill("BIN Chunk", "${bundle.glbHeader.binLength} B")
                        SpecDataPill("Primitives", "${bundle.geometry.faceCount} Tris")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vulkan 1.3 Pipeline Spec Matrix (Sascha Willems Architecture)
            Text(
                text = "VULKAN 1.3 GRAPHICS PIPELINE (SASCHA WILLEMS ARCHITECTURE)",
                style = MaterialTheme.typography.labelSmall.copy(color = NeonLime, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpecDataPill("Pipeline Type", "VkPipeline (Dynamic)")
                        SpecDataPill("Shader IR", "SPIR-V 1.5")
                        SpecDataPill("Memory Alloc", "VMA Device-Local")
                    }
                    Divider(color = DarkSurfaceHighlight, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpecDataPill("Descriptor Sets", "Set 0: UBO • Set 1: PBR")
                        SpecDataPill("Multisampling", "4x MSAA Subpixel")
                        SpecDataPill("Draw Dispatch", "vkCmdDrawIndexed")
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecDataPill(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
