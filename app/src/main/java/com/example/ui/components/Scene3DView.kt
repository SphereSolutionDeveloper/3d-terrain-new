package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.model.GrassBlendType
import com.example.model.GrassSpecies
import com.example.model.GrassViewMode
import com.example.model.Scene3DObjectInstance
import com.example.ui.theme.*

@Composable
fun Scene3DView(
    instances: List<Scene3DObjectInstance>,
    selectedInstance: Scene3DObjectInstance?,
    sceneMode: String,
    sceneDensity: Int,
    grassViewMode: GrassViewMode = GrassViewMode.VARIETY,
    singleGrassSpecies: GrassSpecies = GrassSpecies.KENTUCKY_BLUEGRASS,
    singleGrassBlend: GrassBlendType = GrassBlendType.LUSH_CHLOROPHYLL,
    shadingMode: String,
    showGridLines: Boolean,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isAutoRotate: Boolean,
    lightAngle: Float,
    isLoading: Boolean,
    onSceneModeChange: (String) -> Unit,
    onSceneDensityChange: (Int) -> Unit,
    onGrassViewModeChange: (GrassViewMode) -> Unit = {},
    onSingleGrassSpeciesChange: (GrassSpecies) -> Unit = {},
    onSingleGrassBlendChange: (GrassBlendType) -> Unit = {},
    onShadingModeChange: (String) -> Unit,
    onToggleGridLines: () -> Unit,
    onRotateDrag: (Float, Float) -> Unit,
    onPanDrag: (Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onToggleAutoRotate: () -> Unit,
    onResetCamera: (String) -> Unit,
    onSelectInstance: (Scene3DObjectInstance?) -> Unit,
    onOpenInspector: (Scene3DObjectInstance) -> Unit,
    modifier: Modifier = Modifier
) {
    var isControlsCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("scene_3d_view_container")
    ) {
        // 1. Primary 3D Viewport Canvas (At the very top, full available screen)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyberCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "POPULATING 10x10 NOISY TERRAIN...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }
                }
            } else {
                Scene3DCanvas(
                    instances = instances,
                    selectedInstance = selectedInstance,
                    sceneMode = sceneMode,
                    grassViewMode = grassViewMode,
                    yaw = yaw,
                    pitch = pitch,
                    zoom = zoom,
                    panX = panX,
                    panY = panY,
                    isAutoRotate = isAutoRotate,
                    showGridLines = showGridLines,
                    shadingMode = shadingMode,
                    lightAngle = lightAngle,
                    onRotateDrag = onRotateDrag,
                    onPanDrag = onPanDrag,
                    onZoomChange = onZoomChange,
                    onToggleAutoRotate = onToggleAutoRotate,
                    onResetCamera = onResetCamera,
                    onSelectInstance = onSelectInstance,
                    onOpenInspector = onOpenInspector,
                    modifier = Modifier.fillMaxSize()
                )

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
                        .testTag("scene_fullscreen_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isControlsCollapsed) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isControlsCollapsed) "Show Controls" else "Maximize Scene",
                        tint = if (isControlsCollapsed) CyberCyan else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Control Panel Header / Pull Dock (Pushes controls down or expands them)
        Surface(
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isControlsCollapsed = !isControlsCollapsed }
                .testTag("scene_controls_dock_bar")
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
                        text = if (isControlsCollapsed) "▲ CONTROLS: $sceneMode" else "▼ PUSH DOWN CONTROLS",
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
                        text = "10×10 GRID • ${instances.size} OBJS",
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

        // 3. Compact Bottom Controls Ribbon (Pushed Down to Bottom)
        AnimatedVisibility(visible = !isControlsCollapsed) {
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sleek Terrain Mode Switcher Bar (Pushed down into ribbon)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TerrainPill(
                            title = "🪨 Rocks",
                            isSelected = sceneMode == "ROCKS",
                            activeColor = CyberCyan,
                            onClick = { onSceneModeChange("ROCKS") },
                            modifier = Modifier.weight(1f)
                        )
                        TerrainPill(
                            title = "🌲 Trees",
                            isSelected = sceneMode == "TREES",
                            activeColor = NeonLime,
                            onClick = { onSceneModeChange("TREES") },
                            modifier = Modifier.weight(1f)
                        )
                        TerrainPill(
                            title = "🌾 Grass",
                            isSelected = sceneMode == "GRASS",
                            activeColor = Color(0xFF39FF14),
                            onClick = { onSceneModeChange("GRASS") },
                            modifier = Modifier.weight(1f)
                        )
                        TerrainPill(
                            title = "🌿 Mixed",
                            isSelected = sceneMode == "MIXED",
                            activeColor = LaserOrange,
                            onClick = { onSceneModeChange("MIXED") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                // Row 1: Camera Presets & Shading Modes & Grid
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Camera Presets
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CAM:",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        CameraPresetChip("Iso", onClick = { onResetCamera("Isometric") })
                        CameraPresetChip("Top", onClick = { onResetCamera("Top-Down") })
                        CameraPresetChip("Ground", onClick = { onResetCamera("Ground-Level") })
                    }

                    // Shading & Grid Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = if (shadingMode == "Solid PBR") CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (shadingMode == "Solid PBR") CyberCyan else DarkSurfaceHighlight
                            ),
                            modifier = Modifier
                                .clickable { onShadingModeChange("Solid PBR") }
                                .testTag("shading_solid_pbr_btn")
                        ) {
                            Text(
                                text = "Solid",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (shadingMode == "Solid PBR") CyberCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = if (shadingMode == "Wireframe") CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (shadingMode == "Wireframe") CyberCyan else DarkSurfaceHighlight
                            ),
                            modifier = Modifier
                                .clickable { onShadingModeChange("Wireframe") }
                                .testTag("shading_wireframe_btn")
                        ) {
                            Text(
                                text = "Wire",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (shadingMode == "Wireframe") CyberCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = if (showGridLines) CyberCyan.copy(alpha = 0.18f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (showGridLines) CyberCyan else DarkSurfaceHighlight
                            ),
                            modifier = Modifier
                                .clickable { onToggleGridLines() }
                                .testTag("toggle_grid_lines_btn")
                        ) {
                            Text(
                                text = if (showGridLines) "Grid [ON]" else "Grid [OFF]",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (showGridLines) CyberCyan else TextMuted,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Row 2: Grass View Switcher (Single vs Variety) OR Density Selector (Rocks/Trees/Mixed)
                if (sceneMode == "GRASS") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GRASS VIEW:",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                                // Single Pill
                                val isSingle = grassViewMode == GrassViewMode.SINGLE
                                Surface(
                                    color = if (isSingle) NeonLime.copy(alpha = 0.22f) else DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSingle) NeonLime else DarkSurfaceHighlight
                                    ),
                                    modifier = Modifier
                                        .clickable { onGrassViewModeChange(GrassViewMode.SINGLE) }
                                        .testTag("grass_view_single_btn")
                                ) {
                                    Text(
                                        text = "🌱 Single",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSingle) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isSingle) NeonLime else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                // Variety (Scene Grid) Pill
                                val isVariety = grassViewMode == GrassViewMode.VARIETY
                                Surface(
                                    color = if (isVariety) Color(0xFF39FF14).copy(alpha = 0.22f) else DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isVariety) Color(0xFF39FF14) else DarkSurfaceHighlight
                                    ),
                                    modifier = Modifier
                                        .clickable { onGrassViewModeChange(GrassViewMode.VARIETY) }
                                        .testTag("grass_view_variety_btn")
                                ) {
                                    Text(
                                        text = "🌾 Variety (Scene Grid)",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isVariety) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isVariety) Color(0xFF39FF14) else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (grassViewMode == GrassViewMode.SINGLE) "1 specimen • max 11cm" else "100m² field • Uncountable turf",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (grassViewMode == GrassViewMode.VARIETY) Color(0xFF39FF14) else TextMuted
                            )
                        }

                        // When in Single mode, provide quick species selector
                        if (grassViewMode == GrassViewMode.SINGLE) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "SPECIES:",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                                GrassSpecies.values().forEach { sp ->
                                    val isSpSel = singleGrassSpecies == sp
                                    Surface(
                                        color = if (isSpSel) CyberCyan.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSpSel) CyberCyan else DarkSurfaceHighlight
                                        ),
                                        modifier = Modifier
                                            .clickable { onSingleGrassSpeciesChange(sp) }
                                            .testTag("single_species_${sp.name}")
                                    ) {
                                        Text(
                                            text = when (sp) {
                                                GrassSpecies.KENTUCKY_BLUEGRASS -> "Kentucky"
                                                GrassSpecies.PERENNIAL_RYEGRASS -> "Ryegrass"
                                                GrassSpecies.FINE_FESCUE -> "Fine Fescue"
                                                GrassSpecies.TALL_FESCUE -> "Tall Fescue"
                                                GrassSpecies.BERMUDA_GRASS -> "Bermuda"
                                            },
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSpSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSpSel) CyberCyan else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Density Selector for ROCKS, TREES, MIXED
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "DENSITY:",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            listOf(12, 24, 36, 48, 64).forEach { count ->
                                val isSel = sceneDensity == count
                                Surface(
                                    color = if (isSel) NeonLime.copy(alpha = 0.22f) else DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) NeonLime else DarkSurfaceHighlight
                                    ),
                                    modifier = Modifier
                                        .clickable { onSceneDensityChange(count) }
                                        .testTag("density_btn_$count")
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSel) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSel) NeonLime else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "100m² field • 1m² cells",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun TerrainPill(
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) activeColor.copy(alpha = 0.22f) else DarkSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else DarkSurfaceHighlight
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) activeColor else TextPrimary
            )
        }
    }
}

@Composable
private fun CameraPresetChip(
    name: String,
    onClick: () -> Unit
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
