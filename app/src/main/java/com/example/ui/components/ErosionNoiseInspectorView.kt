package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ErosionSimulationData
import com.example.model.FullAssetBundle
import com.example.ui.theme.*

@Composable
fun ErosionNoiseInspectorView(
    bundle: FullAssetBundle,
    erosionData: ErosionSimulationData,
    isSimulating: Boolean,
    triplanarEnabled: Boolean,
    onThermalAngleChange: (Float) -> Unit,
    onHydraulicDropletsChange: (Int) -> Unit,
    onDomainWarpChange: (Float) -> Unit,
    onRidgedOctavesChange: (Int) -> Unit,
    onLodSelect: (Int) -> Unit,
    onToggleTriplanar: () -> Unit,
    onRunSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("erosion_noise_inspector_container"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Banner: Procedural Pipeline Overview
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyberCyan.copy(alpha = 0.18f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Erosion",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PROCEDURAL ROCK SYNTHESIS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "NOISE DISPLACEMENT • EROSION • TRIPLANAR PBR",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }

                    // Simulation Action Button
                    Button(
                        onClick = onRunSimulation,
                        enabled = !isSimulating,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("run_erosion_sim_btn")
                    ) {
                        if (isSimulating) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SIMULATING...",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RUN EROSION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        }
                    }
                }
            }
        }

        // 2. Multi-Octave Noise Synthesis Section
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "1. NOISE DISPLACEMENT FUNCTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = NeonLime
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Domain Warping Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Domain Warping (Twisted Strata)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Feeds fBm noise into coordinates to create organic twisting",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${(erosionData.domainWarpAmplitude * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime
                    )
                }
                Slider(
                    value = erosionData.domainWarpAmplitude,
                    onValueChange = onDomainWarpChange,
                    valueRange = 0.1f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonLime,
                        activeTrackColor = NeonLime,
                        inactiveTrackColor = DarkSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("domain_warp_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Ridged Multifractal Noise Octaves
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Ridged Multifractal (1 - |Noise|)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Creates sharp tectonic fractures, cleavage facets and ridges",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${erosionData.ridgedFractureOctaves} Octaves",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = LaserOrange
                    )
                }
                Slider(
                    value = erosionData.ridgedFractureOctaves.toFloat(),
                    onValueChange = { onRidgedOctavesChange(it.toInt()) },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = LaserOrange,
                        activeTrackColor = LaserOrange,
                        inactiveTrackColor = DarkSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("ridged_octaves_slider")
                )
            }
        }

        // 3. Erosion Simulation Lab Section
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "2. EROSION SIMULATION (NATURAL WEATHERING)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Thermal Erosion: Talus Slope Angle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Thermal Erosion (Talus Repose Angle)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Material slides down slopes exceeding critical angle into talus apron",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${erosionData.thermalAngleDeg.toInt()}°",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Slider(
                    value = erosionData.thermalAngleDeg,
                    onValueChange = onThermalAngleChange,
                    valueRange = 25f..55f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("thermal_angle_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Hydraulic Erosion: Water Droplets
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Hydraulic Erosion (Droplet Sediment Transport)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Water droplets carve micro-gullies and deposit sediment in depressions",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${erosionData.hydraulicDroplets} Drops",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet
                    )
                }
                Slider(
                    value = erosionData.hydraulicDroplets.toFloat(),
                    onValueChange = { onHydraulicDropletsChange(it.toInt()) },
                    valueRange = 50f..500f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricViolet,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = DarkSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("hydraulic_droplets_slider")
                )
            }
        }

        // 4. Game Engine Optimization: LODs & Collision Meshes
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "3. LEVEL OF DETAIL (LOD) & COLLISION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = HologramBlue
                    )
                    Text(
                        text = "${bundle.geometry.vertexCount} Verts • ${bundle.geometry.faceCount} Tris",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LodLevelCard(
                        levelName = "LOD0 (High)",
                        subtitle = "100% Geometry",
                        badge = "${bundle.geometry.vertexCount}v",
                        isSelected = erosionData.activeLodLevel == 0,
                        accentColor = CyberCyan,
                        onClick = { onLodSelect(0) },
                        modifier = Modifier.weight(1f)
                    )

                    LodLevelCard(
                        levelName = "LOD1 (Game)",
                        subtitle = "50% Decimated",
                        badge = "${bundle.geometry.vertexCount / 2}v",
                        isSelected = erosionData.activeLodLevel == 1,
                        accentColor = NeonLime,
                        onClick = { onLodSelect(1) },
                        modifier = Modifier.weight(1f)
                    )

                    LodLevelCard(
                        levelName = "LOD2 (Physics)",
                        subtitle = "Convex Hull",
                        badge = "Collision",
                        isSelected = erosionData.activeLodLevel == 2,
                        accentColor = LaserOrange,
                        onClick = { onLodSelect(2) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. PBR Surface Texturing & Triplanar Mapping
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "4. TRIPLANAR SHADER MAPPING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = LaserOrange
                        )
                        Text(
                            text = "Blends X, Y, Z texture projections based on normal (eliminates UV seams)",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = triplanarEnabled,
                        onCheckedChange = { onToggleTriplanar() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LaserOrange,
                            checkedTrackColor = LaserOrange.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("triplanar_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Maps breakdown badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MapBadge("Albedo", "Strata Color", CyberCyan, Modifier.weight(1f))
                    MapBadge("Normal", "Sobel Gradients", NeonLime, Modifier.weight(1f))
                    MapBadge("Roughness", "Micro-Cavities", LaserOrange, Modifier.weight(1f))
                    MapBadge("AO", "Fissure Occlusion", ElectricViolet, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LodLevelCard(
    levelName: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) accentColor.copy(alpha = 0.20f) else DarkSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor else DarkSurfaceHighlight
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Text(
                text = levelName,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) accentColor else TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = accentColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun MapBadge(
    title: String,
    desc: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = desc,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}
