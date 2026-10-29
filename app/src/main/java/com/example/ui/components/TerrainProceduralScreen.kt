package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ProceduralTerrainGenerator
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

@Composable
fun TerrainProceduralScreen(
    modifier: Modifier = Modifier
) {
    var terrainState by remember { mutableStateOf(TerrainState()) }
    var renderMode by remember { mutableStateOf(TerrainRenderMode.SHADED_TRIPLANAR) }
    var activeSubsystemTab by remember { mutableStateOf(TerrainSubsystemTab.GAME_TERRAIN_001) }

    // Camera state (True 3D Isometric perspective by default: pitch = 35.264° = 0.615 rad)
    var yaw by remember { mutableFloatStateOf(0.785f) }
    var pitch by remember { mutableFloatStateOf(0.615f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0.0f) }
    var panY by remember { mutableFloatStateOf(0.0f) }
    var isAutoRotate by remember { mutableStateOf(false) }
    var isWireframeOverlay by remember { mutableStateOf(false) }
    var lightAngle by remember { mutableFloatStateOf(0.85f) }

    var activeTerrainMap by remember { mutableStateOf("MAGADI_50KM") }
    var isMagadiOverview by remember { mutableStateOf(true) }
    var selectedMagadiCol by remember { mutableIntStateOf(0) }
    var selectedMagadiRow by remember { mutableIntStateOf(0) }
    var isTestGround001Active by remember { mutableStateOf(false) }
    var selectedNativeFile by remember { mutableStateOf(NativeSourceCatalog.terrainFiles.first()) }
    var fileSearchQuery by remember { mutableStateOf("") }
    var isSimulatingErosion by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Auto-rotate loop
    LaunchedEffect(isAutoRotate) {
        while (isAutoRotate) {
            delay(16)
            yaw = (yaw + 0.006f) % (2f * Math.PI.toFloat())
        }
    }

    // Generated 3D Mesh (Kenya Magadi 50km, Test Ground 001, or Procedural FBM)
    val mesh = remember(terrainState, activeTerrainMap, isMagadiOverview, selectedMagadiCol, selectedMagadiRow) {
        when (activeTerrainMap) {
            "MAGADI_50KM" -> {
                if (isMagadiOverview) {
                    ProceduralTerrainGenerator.generateMagadiOverviewMesh(gridSize = 45)
                } else {
                    ProceduralTerrainGenerator.generateMagadiChunkMesh(selectedMagadiCol, selectedMagadiRow, gridSize = 41)
                }
            }
            "TEST_GROUND_001" -> {
                ProceduralTerrainGenerator.generateTestGround001Mesh(gridSize = 49)
            }
            else -> {
                ProceduralTerrainGenerator.generateTerrainMesh(terrainState, gridSize = 33, worldSize = 100f)
            }
        }
    }

    var isControlsCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. 3D Terrain Viewport (Dominant screen space: weight 2.4f or fillMaxSize when collapsed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isControlsCollapsed) 1f else 2.3f)
                .padding(4.dp)
        ) {
            Terrain3DCanvas(
                mesh = mesh,
                renderMode = renderMode,
                yaw = yaw,
                pitch = pitch,
                zoom = zoom,
                panX = panX,
                panY = panY,
                lightAngleRad = lightAngle,
                isWireframeOverlay = isWireframeOverlay,
                onRotateDrag = { dx, dy ->
                    yaw += dx * 0.007f
                    pitch = (pitch - dy * 0.007f).coerceIn(0.20f, 1.48f)
                },
                onPanDrag = { dx, dy ->
                    panX += dx
                    panY += dy
                },
                onZoomChange = { dz ->
                    zoom = (zoom * (1f + dz)).coerceIn(0.4f, 3.5f)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Viewport Controls (Top-Right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Push Controls Down / Fullscreen Toggle
                IconButton(
                    onClick = { isControlsCollapsed = !isControlsCollapsed },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isControlsCollapsed) CyberCyan.copy(alpha = 0.35f) else DarkSurface.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("terrain_fullscreen_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isControlsCollapsed) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isControlsCollapsed) "Show Controls" else "Maximize Scene",
                        tint = if (isControlsCollapsed) CyberCyan else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Auto rotate toggle
                IconButton(
                    onClick = { isAutoRotate = !isAutoRotate },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isAutoRotate) CyberCyan.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("terrain_auto_rotate_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Auto Rotate",
                        tint = if (isAutoRotate) CyberCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Wireframe overlay toggle
                IconButton(
                    onClick = { isWireframeOverlay = !isWireframeOverlay },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isWireframeOverlay) NeonLime.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("terrain_wireframe_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Wireframe Overlay",
                        tint = if (isWireframeOverlay) NeonLime else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Random seed button
                IconButton(
                    onClick = {
                        terrainState = terrainState.copy(seed = Random.nextLong(1000, 999999))
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = DarkSurface.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("terrain_random_seed_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Randomize Seed",
                        tint = LaserOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset camera to true 3D Isometric Viewpoint (pitch = 35.264° = 0.615 rad)
                IconButton(
                    onClick = {
                        yaw = 0.785f
                        pitch = 0.615f
                        zoom = 1.0f
                        panX = 0f
                        panY = 0f
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = DarkSurface.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("terrain_reset_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Camera (3D Iso)",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Floating Bottom HUD: Active Map Mode Switcher, Camera Viewpoint Presets and Render Modes
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Map Source Selector Pills (Kenya Magadi 50km vs Test Ground 001 vs Procedural FBM)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMagadiActive = activeTerrainMap == "MAGADI_50KM"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMagadiActive) LaserOrange.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMagadiActive) LaserOrange else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .clickable {
                                activeTerrainMap = "MAGADI_50KM"
                                isTestGround001Active = false
                                activeSubsystemTab = TerrainSubsystemTab.KENYA_MAGADI_50KM
                            }
                            .testTag("map_mode_magadi_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isMagadiActive) LaserOrange else TextMuted,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "MAGADI RD AREA (~101 KM)",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isMagadiActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isMagadiActive) LaserOrange else TextSecondary
                            )
                        }
                    }

                    val isTestGroundActive = activeTerrainMap == "TEST_GROUND_001"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isTestGroundActive) NeonLime.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTestGroundActive) NeonLime else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .clickable {
                                activeTerrainMap = "TEST_GROUND_001"
                                isTestGround001Active = true
                                activeSubsystemTab = TerrainSubsystemTab.GAME_TERRAIN_001
                            }
                            .testTag("map_mode_test_ground_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isTestGroundActive) NeonLime else TextMuted,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "TEST GROUND 001",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isTestGroundActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTestGroundActive) NeonLime else TextSecondary
                            )
                        }
                    }

                    val isFbmActive = activeTerrainMap == "PROCEDURAL_FBM"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isFbmActive) CyberCyan.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isFbmActive) CyberCyan else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .clickable {
                                activeTerrainMap = "PROCEDURAL_FBM"
                                isTestGround001Active = false
                                activeSubsystemTab = TerrainSubsystemTab.HEIGHTMAP_NOISE
                            }
                            .testTag("map_mode_procedural_btn")
                    ) {
                        Text(
                            text = "PROCEDURAL FBM",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isFbmActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFbmActive) CyberCyan else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                        )
                    }
                }

                // Camera Viewpoint Presets (Correct mathematical perspective angles)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAM:",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    listOf(
                        Triple("3D Iso", 0.785f, 0.615f),
                        Triple("Orbit", 0.52f, 0.85f),
                        Triple("Driver", 0f, 0.28f),
                        Triple("Top-Down", 0f, 1.48f),
                        Triple("West Peak", 1.57f, 0.45f)
                    ).forEach { (label, yTarget, pTarget) ->
                        val isCurrent = abs(yaw - yTarget) < 0.18f && abs(pitch - pTarget) < 0.18f
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) CyberCyan.copy(alpha = 0.28f) else DarkSurface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) CyberCyan else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    yaw = yTarget
                                    pitch = pTarget
                                    zoom = 1.0f
                                    panX = 0f
                                    panY = 0f
                                }
                                .testTag("camera_preset_${label.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) CyberCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Render Modes Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(TerrainRenderMode.values()) { mode ->
                        val isSelected = renderMode == mode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) NeonLime.copy(alpha = 0.22f) else DarkSurface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonLime else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .clickable { renderMode = mode }
                                .testTag("render_mode_${mode.name}")
                        ) {
                            Text(
                                text = mode.title,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonLime else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
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
                .testTag("terrain_controls_dock_bar")
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
                    // Small drag pill indicator
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isControlsCollapsed) CyberCyan else TextMuted)
                    )
                    Text(
                        text = if (isControlsCollapsed) "▲ CONTROLS: ${activeSubsystemTab.label.uppercase()}" else "▼ PUSH DOWN CONTROLS",
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
                        text = "POLY: ${mesh.indices.size / 3} • SEED: ${terrainState.seed}",
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

        // 3. Subsystem Tabs Row (Only visible when controls are open)
        AnimatedVisibility(visible = !isControlsCollapsed) {
            ScrollableTabRow(
                selectedTabIndex = activeSubsystemTab.ordinal,
                containerColor = DarkSurface,
                contentColor = CyberCyan,
                edgePadding = 8.dp,
                divider = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                TerrainSubsystemTab.values().forEach { tab ->
                    val selected = activeSubsystemTab == tab
                    Tab(
                        selected = selected,
                        onClick = { activeSubsystemTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) CyberCyan else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("terrain_tab_${tab.name}")
                    )
                }
            }
        }

        // 4. Subsystem Controls & Inspectors (Pushed down into lower area)
        AnimatedVisibility(
            visible = !isControlsCollapsed,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
            when (activeSubsystemTab) {
                TerrainSubsystemTab.KENYA_MAGADI_50KM -> {
                    KenyaMagadiControls(
                        isOverview = isMagadiOverview,
                        onToggleOverview = {
                            isMagadiOverview = it
                            activeTerrainMap = "MAGADI_50KM"
                        },
                        selectedCol = selectedMagadiCol,
                        selectedRow = selectedMagadiRow,
                        onSelectChunk = { col, row ->
                            selectedMagadiCol = col
                            selectedMagadiRow = row
                            isMagadiOverview = false
                            activeTerrainMap = "MAGADI_50KM"
                        },
                        mesh = mesh,
                        onFocusPreset = { y, p ->
                            yaw = y
                            pitch = p
                            zoom = 1.0f
                            panX = 0f
                            panY = 0f
                        },
                        onSelectFile = { file ->
                            selectedNativeFile = file
                            activeSubsystemTab = TerrainSubsystemTab.CPP_SOURCE_TREE
                        }
                    )
                }
                TerrainSubsystemTab.GAME_TERRAIN_001 -> {
                    GameTerrain001Controls(
                        isTestGroundActive = isTestGround001Active,
                        onToggleTestGround = {
                            isTestGround001Active = it
                            if (it) activeTerrainMap = "TEST_GROUND_001"
                        },
                        mesh = mesh,
                        onFocusPreset = { y, p ->
                            yaw = y
                            pitch = p
                            zoom = 1.0f
                            panX = 0f
                            panY = 0f
                        },
                        onSelectFile = { file ->
                            selectedNativeFile = file
                            activeSubsystemTab = TerrainSubsystemTab.CPP_SOURCE_TREE
                        }
                    )
                }
                TerrainSubsystemTab.HEIGHTMAP_NOISE -> {
                    HeightmapControls(
                        state = terrainState,
                        onStateChange = { terrainState = it },
                        mesh = mesh
                    )
                }
                TerrainSubsystemTab.EROSION_SIM -> {
                    ErosionSimulationControls(
                        state = terrainState,
                        isSimulating = isSimulatingErosion,
                        onRunErosion = {
                            coroutineScope.launch {
                                isSimulatingErosion = true
                                for (step in 1..8) {
                                    delay(40)
                                    terrainState = ProceduralTerrainGenerator.simulateErosionCycle(terrainState)
                                }
                                isSimulatingErosion = false
                            }
                        },
                        onStateChange = { terrainState = it }
                    )
                }
                TerrainSubsystemTab.SPLAT_BIOMES -> {
                    SplatBiomesControls(
                        state = terrainState,
                        onStateChange = { terrainState = it }
                    )
                }
                TerrainSubsystemTab.CLIFFS_STRATA -> {
                    CliffsStrataControls(
                        state = terrainState,
                        onStateChange = { terrainState = it }
                    )
                }
                TerrainSubsystemTab.ROAD_CUTS -> {
                    RoadCutControls(
                        state = terrainState,
                        onStateChange = { terrainState = it }
                    )
                }
                TerrainSubsystemTab.CAVES_VOXELS -> {
                    CaveVoxelsControls(
                        state = terrainState,
                        onStateChange = { terrainState = it }
                    )
                }
                TerrainSubsystemTab.CDLOD_SHADERS -> {
                    CdlodShadersControls(
                        state = terrainState,
                        onStateChange = { terrainState = it }
                    )
                }
                TerrainSubsystemTab.CPP_SOURCE_TREE -> {
                    CppSourceTreeViewer(
                        files = NativeSourceCatalog.terrainFiles,
                        selectedFile = selectedNativeFile,
                        searchQuery = fileSearchQuery,
                        onSearchChange = { fileSearchQuery = it },
                        onSelectFile = { selectedNativeFile = it },
                        onCopyCode = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                        }
                    )
                }
            }
        }
    }
}
}

// ==================== SUBSYSTEM CONTROL COMPONENTS ====================

@Composable
private fun HeightmapControls(
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit,
    mesh: TerrainMesh3D
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Spec overview card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "FBM & RIDGE NOISE PARAMETERS (C++ FbmNoise.h)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Seed: ${state.seed} • Min Elev: ${mesh.minHeight.toInt()}m • Max Elev: ${mesh.maxHeight.toInt()}m • Avg Slope: ${mesh.avgSlope.toInt()}°",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Sliders
        ControlSlider(
            label = "Elevation Scale",
            value = state.heightScale,
            valueRange = 10f..80f,
            unit = "m",
            onValueChange = { onStateChange(state.copy(heightScale = it)) }
        )

        ControlSlider(
            label = "FBM Octaves",
            value = state.octaves.toFloat(),
            valueRange = 1f..8f,
            steps = 6,
            unit = "oct",
            onValueChange = { onStateChange(state.copy(octaves = it.toInt())) }
        )

        ControlSlider(
            label = "Persistence (Roughness Decay)",
            value = state.persistence,
            valueRange = 0.2f..0.8f,
            unit = "",
            onValueChange = { onStateChange(state.copy(persistence = it)) }
        )

        ControlSlider(
            label = "Lacunarity (Frequency Step)",
            value = state.lacunarity,
            valueRange = 1.5f..3.5f,
            unit = "x",
            onValueChange = { onStateChange(state.copy(lacunarity = it)) }
        )

        ControlSlider(
            label = "Domain Warping Strength",
            value = state.domainWarpStrength,
            valueRange = 0f..0.8f,
            unit = "",
            onValueChange = { onStateChange(state.copy(domainWarpStrength = it)) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ridged Multifractal Mode", color = TextPrimary, fontSize = 12.sp)
            Switch(
                checked = state.ridgedMultifractal,
                onCheckedChange = { onStateChange(state.copy(ridgedMultifractal = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun ErosionSimulationControls(
    state: TerrainState,
    isSimulating: Boolean,
    onRunErosion: () -> Unit,
    onStateChange: (TerrainState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "PARTICLE HYDRAULIC & THERMAL EROSION (Erosion.h)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = HologramBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Droplets carve river gullies, dissolve sediment, and deposit alluvial fans. Thermal relaxation smooths slopes above the angle of repose.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Button(
            onClick = onRunErosion,
            enabled = !isSimulating,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("run_erosion_sim_btn")
        ) {
            if (isSimulating) {
                CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulating Droplet Physics...", color = DarkBackground, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = DarkBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run 50 Erosion Cycles", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }

        ControlSlider(
            label = "Rain Droplet Budget",
            value = state.rainDropletCount.toFloat(),
            valueRange = 500f..10000f,
            unit = " drops",
            onValueChange = { onStateChange(state.copy(rainDropletCount = it.toInt())) }
        )

        ControlSlider(
            label = "Erosion Carve Rate",
            value = state.erosionRate,
            valueRange = 0.05f..0.8f,
            unit = "",
            onValueChange = { onStateChange(state.copy(erosionRate = it)) }
        )

        ControlSlider(
            label = "Talus Angle of Repose (Scree slope)",
            value = state.talusAngleDeg,
            valueRange = 25f..45f,
            unit = "°",
            onValueChange = { onStateChange(state.copy(talusAngleDeg = it)) }
        )
    }
}

@Composable
private fun SplatBiomesControls(
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "4-CHANNEL SPLAT WEIGHT DISTRIBUTOR (SplatGenerator.h)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonLime
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Weights: R=Grass, G=Dirt Soil, B=Basalt Cliff Rock, A=Alpine Snow.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        ControlSlider(
            label = "Grass Max Elevation",
            value = state.grassElevationMax,
            valueRange = 5f..35f,
            unit = "m",
            onValueChange = { onStateChange(state.copy(grassElevationMax = it)) }
        )

        ControlSlider(
            label = "Dirt Max Elevation",
            value = state.dirtElevationMax,
            valueRange = 15f..50f,
            unit = "m",
            onValueChange = { onStateChange(state.copy(dirtElevationMax = it)) }
        )

        ControlSlider(
            label = "Cliff Slope Cutoff (Rock replaces Soil)",
            value = state.cliffSlopeThresholdDeg,
            valueRange = 20f..60f,
            unit = "°",
            onValueChange = { onStateChange(state.copy(cliffSlopeThresholdDeg = it)) }
        )

        ControlSlider(
            label = "Snow Line Min Elevation",
            value = state.snowElevationMin,
            valueRange = 25f..65f,
            unit = "m",
            onValueChange = { onStateChange(state.copy(snowElevationMin = it)) }
        )
    }
}

@Composable
private fun CliffsStrataControls(
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "STEEP CLIFF STRATA & OVERHANGS (CliffGen.h)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LaserOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Synthesizes layered sedimentary bands and rock wall micro-relief on slopes > ${state.cliffSlopeThresholdDeg.toInt()}°.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        ControlSlider(
            label = "Cliff Steepness Cutoff",
            value = state.cliffSlopeThresholdDeg,
            valueRange = 25f..55f,
            unit = "°",
            onValueChange = { onStateChange(state.copy(cliffSlopeThresholdDeg = it)) }
        )
    }
}

@Composable
private fun RoadCutControls(
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ROAD CORRIDOR CUT & FILL (RoadCut.h)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Intersects spline roadway centerline through mountains. Generates cut slopes (1:1 ratio) and fill ramps.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        ControlSlider(
            label = "Road Corridor Width",
            value = state.roadWidth,
            valueRange = 4f..16f,
            unit = "m",
            onValueChange = { onStateChange(state.copy(roadWidth = it)) }
        )

        ControlSlider(
            label = "Cut Slope Angle",
            value = state.roadCutSlope,
            valueRange = 30f..65f,
            unit = "°",
            onValueChange = { onStateChange(state.copy(roadCutSlope = it)) }
        )
    }
}

@Composable
private fun CaveVoxelsControls(
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "MARCHING CUBES 3D CAVES (CaveGen.h)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFF0055)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subterranean 3D voxel density isosurface carving winding caverns and hollow tubes without GLBs.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        ControlSlider(
            label = "Cave Voxel Density Threshold",
            value = state.caveThreshold,
            valueRange = 0.4f..0.8f,
            unit = "",
            onValueChange = { onStateChange(state.copy(caveThreshold = it)) }
        )

        ControlSlider(
            label = "Tunnel Diameter",
            value = state.caveTunnelRadius,
            valueRange = 2f..8f,
            unit = "m",
            onValueChange = { onStateChange(state.copy(caveTunnelRadius = it)) }
        )
    }
}

@Composable
private fun CdlodShadersControls(
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "CONTINUOUS DISTANCE-DEPENDENT LOD (TileLOD.h)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CDLOD morph factor smoothly interpolates odd vertices between LOD n and n+1 to eliminate popping.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        ControlSlider(
            label = "CDLOD Morph Factor (Alpha)",
            value = state.cdlodMorphFactor,
            valueRange = 0f..1f,
            unit = "",
            onValueChange = { onStateChange(state.copy(cdlodMorphFactor = it)) }
        )

        ControlSlider(
            label = "Tile Patch Grid Resolution",
            value = state.cdlodTileResolution.toFloat(),
            valueRange = 17f..129f,
            steps = 3,
            unit = "x",
            onValueChange = { onStateChange(state.copy(cdlodTileResolution = it.toInt())) }
        )
    }
}

@Composable
private fun CppSourceTreeViewer(
    files: List<NativeFileTreeItem>,
    selectedFile: NativeFileTreeItem,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectFile: (NativeFileTreeItem) -> Unit,
    onCopyCode: (String) -> Unit
) {
    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Filter 71 native terrain files...", color = TextMuted, fontSize = 12.sp) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyberCyan
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )

        // File chips row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredFiles) { file ->
                val isSelected = file.path == selectedFile.path
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CyberCyan else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.clickable { onSelectFile(file) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.name.endsWith(".h")) Icons.Default.Description else Icons.Default.Code,
                            contentDescription = null,
                            tint = if (isSelected) CyberCyan else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = file.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CyberCyan else TextPrimary
                        )
                    }
                }
            }
        }

        // Selected file card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedFile.path,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                        Text(
                            text = "${selectedFile.category} • ${selectedFile.lines} lines",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { onCopyCode(selectedFile.codeSnippet) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = selectedFile.description,
                    fontSize = 11.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Code snippet view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF070B12))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = selectedFile.codeSnippet,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonLime,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = TextPrimary)
            Text(
                text = "${(value * 10).toInt() / 10f}$unit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun GameTerrain001Controls(
    isTestGroundActive: Boolean,
    onToggleTestGround: (Boolean) -> Unit,
    mesh: TerrainMesh3D,
    onFocusPreset: (Float, Float) -> Unit,
    onSelectFile: (NativeFileTreeItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Hero Overview & 3D Scene Activation Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isTestGroundActive) NeonLime.copy(alpha = 0.5f) else GlowingCyanBorder.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TEST GROUND 001 - MOBILE GAME TERRAIN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isTestGroundActive) NeonLime else CyberCyan
                        )
                        Text(
                            text = "Standardized JSON/C++ Game Engine Format (Unity, Unreal, Godot)",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isTestGroundActive) NeonLime.copy(alpha = 0.2f) else DarkSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isTestGroundActive) NeonLime else TextMuted)
                    ) {
                        Text(
                            text = if (isTestGroundActive) "3D ACTIVE" else "STANDBY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isTestGroundActive) NeonLime else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 3D Scene Mesh Button
                Button(
                    onClick = { onToggleTestGround(!isTestGroundActive) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestGroundActive) NeonLime.copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.35f),
                        contentColor = if (isTestGroundActive) NeonLime else CyberCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isTestGroundActive) NeonLime else CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("test_ground_toggle_render_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isTestGroundActive) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isTestGroundActive) "TEST GROUND 001 ACTIVE IN 3D SCENE" else "LOAD & RENDER TEST GROUND 001 IN 3D SCENE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick camera focus chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "FOCUS:",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    listOf(
                        Triple("West Hill (+40m)", 1.57f, 0.45f),
                        Triple("Central Lake (-15m)", 0f, 1.48f),
                        Triple("Bridge & River", 0.785f, 0.615f),
                        Triple("Start Grid & Garage", 0.20f, 0.35f)
                    ).forEach { (label, yTarget, pTarget) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.clickable { onFocusPreset(yTarget, pTarget) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Physical Specifications & Grid Dimensions
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "MAP SPECIFICATIONS (transport_sim_test_001.json)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dimensions: 320m × 320m", fontSize = 10.sp, color = TextPrimary)
                        Text("HD Grid: 160 × 160 (25,600 pts)", fontSize = 10.sp, color = TextPrimary)
                        Text("Resolution: 2.0m mobile mesh", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Scale Ratio: 1:1:1 Equal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Base Elevation: 1700.0m", fontSize = 10.sp, color = TextPrimary)
                        Text("Elevation Range: 1685.6m .. 1739.7m", fontSize = 10.sp, color = TextPrimary)
                        Text("Physics Cell Size: 5.0m", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Isotropic: X=1.0, Y=1.0, Z=1.0", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                    }
                }
            }
        }

        // 3. Physical Surface Zones & Friction Mapping
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SURFACE ZONES & TIRE FRICTION COEFFICIENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LaserOrange
                )
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Pair("Asphalt Road (μ = 0.85)", "Superellipse corridor (n=4, half_side=125m, width=5.0m)"),
                    Pair("Mud Hazard (μ = 0.22)", "South muddy trench hazard zone (25m < Y < 45m)"),
                    Pair("Gravel Ditches (μ = 0.65)", "East drainage ditches (dx=280..290m, depth=0.5..1.5m)"),
                    Pair("Water Surface (μ = 0.05)", "Central lake (R=20m, -15m depth) & North river trench (-8m)"),
                    Pair("Grass Meadow (μ = 0.85)", "Lush baseline ground and surrounding green space")
                ).forEach { (title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                title.contains("Asphalt") -> Color(0xFF212529)
                                title.contains("Mud") -> Color(0xFF5D4037)
                                title.contains("Gravel") -> Color(0xFF78909C)
                                title.contains("Water") -> Color(0xFF0288D1)
                                else -> Color(0xFF2E7D32)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(desc, fontSize = 9.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // 4. 3D GLB Asset Placements (43 Objects)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3D GLB ASSET PLACEMENTS (43 Objects)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonLime
                    )
                    Text(
                        text = "Auto-Placed",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                listOf(
                    Triple("garage_001 (Garage Hub)", "[300, 300, 1700m] rot: [0, 0, 0]", "assets/structures/garage_hub.glb"),
                    Triple("bridge_001 (Concrete Bridge)", "[160, 285, 1700m] rot: [0, 0, 0]", "assets/structures/concrete_bridge.glb"),
                    Triple("start_line (Spawn Grid)", "[300, 285, 1700m] rot: [0, -90, 0]", "assets/markers/start_grid.glb"),
                    Triple("30 Forest Trees", "East sector (X: 250..315, Y: 30..290)", "assets/nature/palm_tree.glb"),
                    Triple("10 Hill Boulders", "West hill slopes (X: 15..30, Y: 120..180)", "assets/nature/boulder_1.5m.glb")
                ).forEach { (id, coords, glb) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Text(id, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(coords, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                        Text(glb, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    }
                }
            }
        }

        // 5. C/C++ Engine Conversation & Native Architecture
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "C/C++ ENGINE INTEGRATION (Unity C#, Unreal, Godot)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All procedural terrain math has been implemented in standardized C/C++ files under native-assets/src/terrain/game_terrain/test_ground_001/cpp/",
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val loaderFile = NativeSourceCatalog.terrainFiles.find { it.name == "GameTerrainLoader.h" }
                    if (loaderFile != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(loaderFile) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("GameTerrainLoader.h", fontSize = 9.sp, color = CyberCyan)
                        }
                    }

                    val apiFile = NativeSourceCatalog.terrainFiles.find { it.name == "GameTerrainC_API.h" }
                    if (apiFile != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(apiFile) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("GameTerrainC_API.h", fontSize = 9.sp, color = NeonLime)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Fast binary storage vs JSON info
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceHighlight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "ZERO-COPY BINARY CACHE (.bin / .tg1)",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LaserOrange
                        )
                        Text(
                            text = "JSON: 790 KB • Binary: 125 KB (84% reduction, instant memmap loader)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ==================== KENYA MAGADI 50KM CONTROLS ====================

@Composable
private fun KenyaMagadiControls(
    isOverview: Boolean,
    onToggleOverview: (Boolean) -> Unit,
    selectedCol: Int,
    selectedRow: Int,
    onSelectChunk: (Int, Int) -> Unit,
    mesh: TerrainMesh3D,
    onFocusPreset: (Float, Float) -> Unit,
    onSelectFile: (NativeFileTreeItem) -> Unit
) {
    val roadChunks = setOf(
        Pair(4, 4), Pair(3, 4), Pair(3, 3), Pair(2, 2), Pair(1, 1), Pair(1, 0), Pair(0, 0)
    )

    val chunkId = "chunk_%02d_%02d".format(selectedCol, selectedRow)
    val latMin = -1.90 + selectedRow * 0.09
    val latMax = latMin + 0.09
    val lonMin = 36.25 + selectedCol * 0.09
    val lonMax = lonMin + 0.09

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Hero Overview & Mode Switcher Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MAGADI RD AREA (C58/B19) — REAL ROAD & 50KM TERRAIN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LaserOrange
                        )
                        Text(
                            text = "105.27 km Real OSM Route • 1,799 GPS Vertices • Copernicus COP30 DEM • 106 3D Chunks",
                            fontSize = 10.sp,
                            color = NeonLime
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LaserOrange.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange)
                    ) {
                        Text(
                            text = if (isOverview) "50KM OVERVIEW" else chunkId.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LaserOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode toggle buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onToggleOverview(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverview) LaserOrange.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                            contentColor = if (isOverview) LaserOrange else TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOverview) LaserOrange else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("50km Overview Mesh", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onToggleOverview(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isOverview) NeonLime.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                            contentColor = if (!isOverview) NeonLime else TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (!isOverview) NeonLime else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("10km Tile Inspection", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Interactive 5x5 Chunk Matrix (Col 0..4, Row 4..0)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5×5 CHUNK GRID (10KM² TILES • 2,500 KM² TOTAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Text(
                        text = "Tap to load chunk",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5x5 Grid (North at top: Row 4 down to 0)
                for (row in 4 downTo 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..4) {
                            val isSelected = !isOverview && selectedCol == col && selectedRow == row
                            val hasRoad = roadChunks.contains(Pair(col, row))
                            val isLake = (col == 0 && (row == 0 || row == 1))

                            val bg = when {
                                isSelected -> NeonLime.copy(alpha = 0.35f)
                                hasRoad -> LaserOrange.copy(alpha = 0.22f)
                                isLake -> CyberCyan.copy(alpha = 0.15f)
                                else -> DarkSurfaceHighlight.copy(alpha = 0.6f)
                            }
                            val borderCol = when {
                                isSelected -> NeonLime
                                hasRoad -> LaserOrange.copy(alpha = 0.7f)
                                isLake -> CyberCyan.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.08f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bg)
                                    .border(1.dp, borderCol, RoundedCornerShape(4.dp))
                                    .clickable { onSelectChunk(col, row) }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${col},${row}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonLime else if (hasRoad) LaserOrange else TextSecondary
                                    )
                                    if (hasRoad) {
                                        Text("ROAD", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                                    } else if (isLake) {
                                        Text("LAKE", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    }
                                }
                            }
                        }
                    }
                    if (row > 0) Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(2.dp), color = LaserOrange, modifier = Modifier.size(8.dp)) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("C58 Road", fontSize = 8.5.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(2.dp), color = CyberCyan, modifier = Modifier.size(8.dp)) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lake Magadi", fontSize = 8.5.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(2.dp), color = NeonLime, modifier = Modifier.size(8.dp)) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Selected", fontSize = 8.5.sp, color = TextSecondary)
                        }
                    }
                    Text("North ↑ (Kiserian to Lake)", fontSize = 8.5.sp, color = TextMuted)
                }
            }
        }

        // 3. Current Selected Chunk Specs & Coordinates Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isOverview) "50KM OVERVIEW TELEMETRY" else "TILE TELEMETRY: $chunkId",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("LOCAL ORIGIN (METERS)", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = if (isOverview) "[0 m, 0 m] (50km span)" else "[${selectedCol * 10000} m, ${selectedRow * 10000} m]",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("WGS84 LATITUDE BOUNDS", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = if (isOverview) "-1.9000° .. -1.4500° S" else "%.4f° .. %.4f° S".format(latMin, latMax),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = LaserOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ISOTROPIC SCALE RATIO", fontSize = 9.sp, color = TextMuted)
                        Text("1:1:1 (X=1m, Y=1m, Z=1m)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("WGS84 LONGITUDE BOUNDS", fontSize = 9.sp, color = TextMuted)
                        Text(
                            text = if (isOverview) "36.2500° .. 36.7000° E" else "%.4f° .. %.4f° E".format(lonMin, lonMax),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LaserOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceHighlight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ELEVATION RANGE", fontSize = 9.sp, color = TextMuted)
                            Text("595m (Lake) .. 1950m (Ngong)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("C58 CORRIDOR", fontSize = 9.sp, color = TextMuted)
                            Text("101.4 km Total Length", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("STORAGE", fontSize = 9.sp, color = TextMuted)
                            Text("JSON + Fast .bin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                    }
                }
            }
        }

        // 4. Architectural Deep-Dive (Answers User Question on Metric Grid vs Lat/Lon Decimals)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "WHY CHUNKS USE LOCAL METRIC NUMBERS INSTEAD OF LAT/LON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonLime
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your architecture question regarding saving 101km road data in 10km² chunks with Lat/Lon decimals is solved via a Multi-Tier Precision Pipeline:",
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val archPoints = listOf(
                    Triple(
                        "1. Eliminates GPU Float32 Jitter",
                        "Game engines (Unity, Unreal, Godot) render with 32-bit floats (6-7 digits precision). Raw GPS coordinates like -1.900000°, 36.250000° truncate and cause severe vertex jitter and collision tunneling. Local metric floats [0..10,000m] guarantee sub-millimeter precision.",
                        CyberCyan
                    ),
                    Triple(
                        "2. Isotropic 1:1:1 Scale Ratio",
                        "Latitude and Longitude are non-isotropic: 1° Lat ≈ 110.57 km, but 1° Lon varies with cos(Lat). Physics engines (vehicle suspension, friction, speed) cannot operate directly in degrees. A 1:1:1 metric grid (1m = 1m = 1m) ensures identical physics in all directions.",
                        LaserOrange
                    ),
                    Triple(
                        "3. Master Manifest (README & JSON)",
                        "Real-world square corners are documented in WGS84 GPS degrees in magadi_manifest.json and README.md. Each chunk file stores clean, local numbers [0..10,000m], perfectly decoupled for mobile game engines.",
                        NeonLime
                    ),
                    Triple(
                        "4. Seamless Floating Origin",
                        "When driving across chunk borders on the 101km road, the engine recenters the camera to (0,0,0) of the active chunk. This allows infinite scale without precision loss.",
                        Color(0xFFE040FB)
                    )
                )

                archPoints.forEach { (title, desc, color) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = color)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(desc, fontSize = 9.sp, color = TextSecondary, lineHeight = 13.sp)
                        }
                    }
                }
            }
        }

        // 5. Iconic C58 Road Waypoints / Landmarks Preset Jumps
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "C58 MAGADI ROAD WAYPOINT JUMPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                val landmarks = listOf(
                    Triple("Corner Baridi Hairpin Descent", "Lat -1.485°, Elev 1620m (Row 4, Col 4)", Pair(4, 4)),
                    Triple("Tinga Trading Centre", "Lat -1.615°, Elev 1380m (Row 3, Col 3)", Pair(3, 3)),
                    Triple("Mt. Olorgesailie Prehistoric Site", "Lat -1.685°, Elev 1760m (Row 2, Col 2)", Pair(2, 2)),
                    Triple("Kamukuru & Sogonoi Shelf", "Lat -1.785°, Elev 860m (Row 1, Col 1)", Pair(1, 1)),
                    Triple("Lake Magadi Soda Works & Brine", "Lat -1.900°, Elev 595m (Row 0, Col 0)", Pair(0, 0))
                )

                landmarks.forEach { (name, sub, colRow) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (!isOverview && selectedCol == colRow.first && selectedRow == colRow.second) NeonLime else Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.5.dp)
                            .clickable {
                                onSelectChunk(colRow.first, colRow.second)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(sub, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                            }
                            Text(
                                "JUMP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LaserOrange
                            )
                        }
                    }
                }
            }
        }

        // 6. Surface Materials & Physical Friction
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SURFACE ZONES & FRICTION (μ)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                val surfaces = listOf(
                    Pair("Asphalt Highway (C58)", "μ = 0.85 (High traction)"),
                    Pair("Gravel Shoulder / Runoff", "μ = 0.65 (Medium slip)"),
                    Pair("Trona Soda Crust (Magadi)", "μ = 0.40 (Crystalline brine)"),
                    Pair("Alkaline Mud Flats", "μ = 0.22 (Extreme slide hazard)"),
                    Pair("Escarpment Phonolite Rock", "μ = 0.78 (Steep basalt)"),
                    Pair("Volcanic Ash Scree (Olorgesailie)", "μ = 0.60 (Loose gravel)"),
                    Pair("Savannah Red Loam / Scrub", "μ = 0.82 (Natural plain)")
                )

                surfaces.forEach { (name, friction) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, fontSize = 9.5.sp, color = TextPrimary)
                        Text(friction, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = LaserOrange)
                    }
                }
            }
        }

        // 7. C++ Engine & Script Architecture Actions
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "C++ CHUNK MANAGER & DEV SCRIPTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inspect the native C++ streaming manager, C ABI header, and python chunking generators:",
                    fontSize = 9.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val mgrFile = NativeSourceCatalog.terrainFiles.find { it.name == "MagadiTerrainChunkManager.h" }
                    if (mgrFile != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(mgrFile) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("ChunkManager.h", fontSize = 8.5.sp, color = CyberCyan)
                        }
                    }

                    val apiFile = NativeSourceCatalog.terrainFiles.find { it.name == "MagadiTerrainC_API.h" }
                    if (apiFile != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(apiFile) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("MagadiC_API.h", fontSize = 8.5.sp, color = NeonLime)
                        }
                    }

                    val pyFile = NativeSourceCatalog.terrainFiles.find { it.name == "generate_magadi_chunks.py" }
                    if (pyFile != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(pyFile) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("generate.py", fontSize = 8.5.sp, color = LaserOrange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val logicDoc = NativeSourceCatalog.terrainFiles.find { it.name == "ROAD_BOUNDING_AREA_LOGIC.md" }
                if (logicDoc != null) {
                    OutlinedButton(
                        onClick = { onSelectFile(logicDoc) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text("Open ROAD_BOUNDING_AREA_LOGIC.md (Sample Math & Corners)", fontSize = 8.5.sp, color = NeonLime, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val magadiMd = NativeSourceCatalog.terrainFiles.find { it.name == "Magadi road.md" }
                    if (magadiMd != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(magadiMd) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("Open Magadi road.md", fontSize = 8.5.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    val pipelineScript = NativeSourceCatalog.terrainFiles.find { it.name == "pipeline.py" }
                    if (pipelineScript != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(pipelineScript) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("Open pipeline.py", fontSize = 8.5.sp, color = LaserOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val realPipe = NativeSourceCatalog.terrainFiles.find { it.name == "run_real_pipeline.py" }
                    if (realPipe != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(realPipe) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.8f)),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("⚡ run_real_pipeline.py", fontSize = 8.5.sp, color = NeonLime, fontWeight = FontWeight.Bold)
                        }
                    }

                    val metaDoc = NativeSourceCatalog.terrainFiles.find { it.name == "road_3d/meta.json" }
                    if (metaDoc != null) {
                        OutlinedButton(
                            onClick = { onSelectFile(metaDoc) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.8f)),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text("📄 road_3d/meta.json", fontSize = 8.5.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

