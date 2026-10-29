package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.CppAssetType
import com.example.model.ProceduralModelMesh
import com.example.ui.theme.*

@Composable
fun InteractiveControlsTab(
    mesh: ProceduralModelMesh,
    renderingBackend: String = "Vulkan 1.3",
    vulkanPolygonMode: String = "VK_POLYGON_MODE_FILL",
    vulkanMsaa: String = "VK_SAMPLE_COUNT_4_BIT",
    vulkanValidationLayers: Boolean = true,
    vulkanDynamicRendering: Boolean = true,
    vulkanPipelineCache: Boolean = true,
    vulkanPushConstantSag: Boolean = true,
    onRenderingBackendChange: (String) -> Unit = {},
    onToggleRenderingBackend: () -> Unit = {},
    onVulkanPolygonModeChange: (String) -> Unit = {},
    onVulkanMsaaChange: (String) -> Unit = {},
    onToggleVulkanValidationLayers: () -> Unit = {},
    onToggleVulkanDynamicRendering: () -> Unit = {},
    onToggleVulkanPipelineCache: () -> Unit = {},
    onToggleVulkanPushConstantSag: () -> Unit = {},
    bay1OpenPct: Float,
    bay2OpenPct: Float,
    neonSignOn: Boolean,
    onBay1Change: (Float) -> Unit,
    onBay2Change: (Float) -> Unit,
    onNeonSignToggle: () -> Unit,
    headlightsOn: Boolean,
    taillightsOn: Boolean,
    onHeadlightsToggle: () -> Unit,
    onTaillightsToggle: () -> Unit,
    postModularType: String = "CORNER",
    postHasTransformer: Boolean = true,
    postHasStreetlight: Boolean = true,
    postStreetlightOn: Boolean = true,
    postHasGuyWire: Boolean = true,
    onPostModularTypeChange: (String) -> Unit = {},
    onTogglePostTransformer: () -> Unit = {},
    onTogglePostStreetlight: () -> Unit = {},
    onTogglePostStreetlightOn: () -> Unit = {},
    onTogglePostGuyWire: () -> Unit = {},
    gridPostCount: Int = 4,
    gridCornerAngleDeg: Float = 85.0f,
    gridWireSagM: Float = 0.55f,
    gridEnergized: Boolean = true,
    gridStreetlightsOn: Boolean = true,
    onGridPostCountChange: (Int) -> Unit = {},
    onGridCornerAngleChange: (Float) -> Unit = {},
    onGridWireSagChange: (Float) -> Unit = {},
    onToggleGridEnergized: () -> Unit = {},
    onToggleGridStreetlightsOn: () -> Unit = {},
    cableGeometryMode: String = "CYLINDER",
    cableCylinderSides: Int = 8,
    cableConductorRadiusMm: Float = 35.0f,
    cableTensionKn: Float = 18.5f,
    onCableGeometryModeChange: (String) -> Unit = {},
    onCableCylinderSidesChange: (Int) -> Unit = {},
    onCableConductorRadiusMmChange: (Float) -> Unit = {},
    onCableTensionKnChange: (Float) -> Unit = {},
    shadingMode: String,
    explodeFactor: Float,
    lightAngle: Float,
    fovAngleDeg: Float,
    isPerspective: Boolean,
    onFovChange: (Float) -> Unit,
    onTogglePerspective: () -> Unit,
    onShadingModeSelect: (String) -> Unit,
    onExplodeFactorChange: (Float) -> Unit,
    onLightAngleChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // 1. Pipeline Settings Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .border(1.dp, NeonLime.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(NeonLime, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VULKAN 1.3 PIPELINE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime
                    )
                }

                Surface(
                    color = NeonLime.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "NATIVE C++",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Polygon Mode Selection (Solid vs Wireframe)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "VK_POLYGON_MODE_FILL" to "SOLID FILL",
                    "VK_POLYGON_MODE_LINE" to "VK WIRE"
                ).forEach { (mode, label) ->
                    val isSel = vulkanPolygonMode == mode
                    Surface(
                        color = if (isSel) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else DarkSurfaceHighlight),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onVulkanPolygonModeChange(mode) }
                            .testTag("btn_polygon_mode_${label.lowercase().replace(" ", "_")}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) CyberCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            // MSAA Antialiasing Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "VK_SAMPLE_COUNT_1_BIT" to "1x OFF",
                    "VK_SAMPLE_COUNT_4_BIT" to "4x MSAA",
                    "VK_SAMPLE_COUNT_8_BIT" to "8x MSAA"
                ).forEach { (msaa, label) ->
                    val isSel = vulkanMsaa == msaa
                    Surface(
                        color = if (isSel) NeonLime.copy(alpha = 0.2f) else DarkSurface,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonLime else DarkSurfaceHighlight),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onVulkanMsaaChange(msaa) }
                            .testTag("btn_msaa_${label.lowercase().replace(" ", "_")}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) NeonLime else TextSecondary
                            )
                        }
                    }
                }
            }

            // Compact Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dynamic Rendering", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                Switch(
                    checked = vulkanDynamicRendering,
                    onCheckedChange = { onToggleVulkanDynamicRendering() },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonLime, checkedTrackColor = NeonLime.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("switch_vulkan_dynamic_rendering")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Validation Layers", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                Switch(
                    checked = vulkanValidationLayers,
                    onCheckedChange = { onToggleVulkanValidationLayers() },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("switch_vulkan_validation_layers")
                )
            }
        }

        // 2. Camera, Projection & Shading
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Projection Mode", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                Surface(
                    color = if (isPerspective) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPerspective) CyberCyan else DarkSurfaceHighlight),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTogglePerspective() }
                        .testTag("btn_specs_toggle_perspective")
                ) {
                    Text(
                        text = if (isPerspective) "PERSPECTIVE" else "ORTHOGRAPHIC",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isPerspective) CyberCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // FOV Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Field of View", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                Text("${fovAngleDeg.toInt()}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
            Slider(
                value = fovAngleDeg,
                onValueChange = onFovChange,
                valueRange = 30f..90f,
                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                modifier = Modifier.fillMaxWidth().height(26.dp).testTag("slider_camera_fov")
            )

            // Shading Modes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Lit Shaded", "Wire Solid", "Pure Wire", "Flat Unlit").forEach { mode ->
                    val isSel = shadingMode.startsWith(mode.take(3)) || shadingMode == mode
                    Surface(
                        color = if (isSel) CyberCyan.copy(alpha = 0.22f) else DarkSurface,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else DarkSurfaceHighlight),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onShadingModeSelect(mode) }
                            .testTag("shading_mode_${mode.lowercase().replace(" ", "_")}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                            Text(
                                text = mode,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) CyberCyan else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Explode factor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Explode", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                Text("${(explodeFactor * 100).toInt()}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ElectricViolet)
            }
            Slider(
                value = explodeFactor,
                onValueChange = onExplodeFactorChange,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(thumbColor = ElectricViolet, activeTrackColor = ElectricViolet),
                modifier = Modifier.fillMaxWidth().height(26.dp).testTag("slider_explode_factor")
            )

            // Sun azimuth
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sun Azimuth", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                Text("${Math.toDegrees(lightAngle.toDouble()).toInt()}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = LaserOrange)
            }
            Slider(
                value = lightAngle,
                onValueChange = onLightAngleChange,
                valueRange = 0.0f..6.28f,
                colors = SliderDefaults.colors(thumbColor = LaserOrange, activeTrackColor = LaserOrange),
                modifier = Modifier.fillMaxWidth().height(26.dp).testTag("slider_sun_angle")
            )
        }

        // 3. Asset-Specific Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (mesh.assetType) {
                CppAssetType.GARAGE_001 -> {
                    Text("GARAGE CONTROLS", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bay 1 Rollup Door", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        Text("${(bay1OpenPct * 100).toInt()}% Open", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    Slider(
                        value = bay1OpenPct,
                        onValueChange = onBay1Change,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth().height(26.dp).testTag("slider_bay1_door")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bay 2 Rollup Door", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        Text("${(bay2OpenPct * 100).toInt()}% Open", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    Slider(
                        value = bay2OpenPct,
                        onValueChange = onBay2Change,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth().height(26.dp).testTag("slider_bay2_door")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Workshop Neon Sign Glow", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = neonSignOn,
                            onCheckedChange = { onNeonSignToggle() },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_neon_sign")
                        )
                    }
                }
                CppAssetType.TEST_VEHICLE_LORRY -> {
                    Text("VEHICLE LIGHTING", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Front Amber Headlights", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = headlightsOn,
                            onCheckedChange = { onHeadlightsToggle() },
                            colors = SwitchDefaults.colors(checkedThumbColor = LaserOrange, checkedTrackColor = LaserOrange.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_headlights")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rear Crimson Taillights", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = taillightsOn,
                            onCheckedChange = { onTaillightsToggle() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD93838), checkedTrackColor = Color(0xFFD93838).copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_taillights")
                        )
                    }
                }
                CppAssetType.MODULAR_POWER_POST -> {
                    Text("MODULAR POST DECK", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))

                    // Post Types
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "TANGENT" to "Tangent",
                            "TRANSFORMER" to "Transf.",
                            "CORNER" to "Corner",
                            "DEAD_END" to "DeadEnd"
                        ).forEach { (typeKey, label) ->
                            val isSelected = postModularType == typeKey
                            Surface(
                                color = if (isSelected) Color(0xFFFFB300).copy(alpha = 0.22f) else DarkSurface,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else DarkSurfaceHighlight),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onPostModularTypeChange(typeKey) }
                                    .testTag("post_type_${typeKey.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFFFFB300) else TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("50kVA Transformer Can", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = postHasTransformer,
                            onCheckedChange = { onTogglePostTransformer() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB300), checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_post_transformer")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cobra-Head Streetlight", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = postHasStreetlight,
                            onCheckedChange = { onTogglePostStreetlight() },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_post_streetlight")
                        )
                    }

                    if (postHasStreetlight) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Streetlight Illumination", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Switch(
                                checked = postStreetlightOn,
                                onCheckedChange = { onTogglePostStreetlightOn() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFEE58), checkedTrackColor = Color(0xFFFFEE58).copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("switch_post_streetlight_on")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Guy Wire Stay & Anchor", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = postHasGuyWire,
                            onCheckedChange = { onTogglePostGuyWire() },
                            colors = SwitchDefaults.colors(checkedThumbColor = LaserOrange, checkedTrackColor = LaserOrange.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_post_guy_wire")
                        )
                    }
                }
                CppAssetType.POWER_GRID_NETWORK -> {
                    Text("POWER GRID NETWORK", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)

                    // Post Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3 to "3 POSTS (TURN)", 4 to "4 POSTS (CORRIDOR)").forEach { (cnt, label) ->
                            val isSelected = gridPostCount == cnt
                            Surface(
                                color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else DarkSurface,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberCyan else DarkSurfaceHighlight),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onGridPostCountChange(cnt) }
                                    .testTag("grid_post_count_$cnt")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CyberCyan else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Corner Turn Angle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Corner Angle", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        Text("${gridCornerAngleDeg.toInt()}°", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = LaserOrange)
                    }
                    Slider(
                        value = gridCornerAngleDeg,
                        onValueChange = onGridCornerAngleChange,
                        valueRange = 45f..120f,
                        colors = SliderDefaults.colors(thumbColor = LaserOrange, activeTrackColor = LaserOrange),
                        modifier = Modifier.fillMaxWidth().height(26.dp).testTag("slider_corner_angle")
                    )

                    // Cable Geometry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CYLINDER" to "3D CYLINDER", "BOX" to "BOX CHORD").forEach { (mode, label) ->
                            val isSel = cableGeometryMode == mode
                            Surface(
                                color = if (isSel) CyberCyan.copy(alpha = 0.22f) else DarkSurface,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else DarkSurfaceHighlight),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onCableGeometryModeChange(mode) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) CyberCyan else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (cableGeometryMode == "CYLINDER") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(6 to "6 Sides", 8 to "8 Sides", 12 to "12 Sides").forEach { (sides, label) ->
                                val isSel = cableCylinderSides == sides
                                Surface(
                                    color = if (isSel) NeonLime.copy(alpha = 0.22f) else DarkSurface,
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonLime else DarkSurfaceHighlight),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onCableCylinderSidesChange(sides) }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = label,
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) NeonLime else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Conductor Radius", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("${cableConductorRadiusMm.toInt()} mm", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                        Slider(
                            value = cableConductorRadiusMm,
                            onValueChange = onCableConductorRadiusMmChange,
                            valueRange = 10.0f..60.0f,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                            modifier = Modifier.fillMaxWidth().height(26.dp)
                        )
                    }

                    // Wire Sag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Overhead Catenary Sag", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        Text("%.2f m".format(gridWireSagM), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    Slider(
                        value = gridWireSagM,
                        onValueChange = onGridWireSagChange,
                        valueRange = 0.1f..1.2f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth().height(26.dp).testTag("slider_wire_sag")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3-Phase Power Energized", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = gridEnergized,
                            onCheckedChange = { onToggleGridEnergized() },
                            colors = SwitchDefaults.colors(checkedThumbColor = LaserOrange, checkedTrackColor = LaserOrange.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_grid_energized")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Route Streetlights Active", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Switch(
                            checked = gridStreetlightsOn,
                            onCheckedChange = { onToggleGridStreetlightsOn() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFEE58), checkedTrackColor = Color(0xFFFFEE58).copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("switch_grid_streetlights")
                        )
                    }
                }
            }
        }
    }
}
