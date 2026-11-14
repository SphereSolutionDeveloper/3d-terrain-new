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
import com.example.engine.NativeEngineBridge
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
    var activeSubsystemTab by remember { mutableStateOf(TerrainSubsystemTab.KENYA_MAGADI_50KM) }

    // Camera parameters: defaults to 3D Isometric Viewpoint matching Sascha Willems' Vulkan terrain repository
    var yaw by remember { mutableFloatStateOf(0.785f) }
    var pitch by remember { mutableFloatStateOf(0.615f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0.0f) }
    var panY by remember { mutableFloatStateOf(0.0f) }
    var isAutoRotate by remember { mutableStateOf(false) }
    var isWireframeOverlay by remember { mutableStateOf(false) }
    var lightAngle by remember { mutableFloatStateOf(0.85f) }

    var activeTerrainMap by remember { mutableStateOf("MAGADI_50KM") }
    var magadiViewMode by remember { mutableStateOf("SEGMENT_1") } // "SEGMENT_1".."SEGMENT_10", "HUMAN_START", "OVERVIEW_50KM", "CHUNKS_10KM"
    var selectedRoadSegment by remember { mutableIntStateOf(1) } // 1 to 10 Road Segments across 101km corridor
    var selectedSubSegment by remember { mutableIntStateOf(0) } // 0 = Whole 10.1km segment, 1..10 = 1.01km sub-segments
    var is1mResolutionMode by remember { mutableStateOf(false) } // 1.0m resolution inspection mode
    var isGroundPieceActive by remember { mutableStateOf(true) } // Solid 3D ground piece with 400m ASL datum
    var groundPieceDatumM by remember { mutableFloatStateOf(400.0f) } // 400m Above Sea Level baseline
    var roadPositionOnSegmentM by remember { mutableFloatStateOf(0f) } // 0 to 10100m on selected segment
    var humanWalkDistanceM by remember { mutableFloatStateOf(0f) }
    var humanEyeHeightType by remember { mutableStateOf("STANDING_1_7M") } // "STANDING_1_7M", "DRIVER_1_2M", "SAFARI_2_8M", "DRONE_25M"
    var humanLookDirection by remember { mutableStateOf("AHEAD_NORTH") } // "AHEAD_NORTH", "LEFT_TRONA", "RIGHT_BASALT", "BEHIND_MAGADI"

    var isMagadiOverview by remember { mutableStateOf(false) }
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

    // Generated 3D Mesh (Kenya Magadi Segments 1-10 in 100% C++, Sub-segments 1-10, 1m DEM, or Test Ground 001)
    val mesh = remember(
        terrainState.contourInterval,
        terrainState.stepTerracingFactor,
        terrainState.heightScale,
        activeTerrainMap,
        magadiViewMode,
        selectedRoadSegment,
        selectedSubSegment,
        is1mResolutionMode,
        roadPositionOnSegmentM,
        humanWalkDistanceM,
        isMagadiOverview,
        selectedMagadiCol,
        selectedMagadiRow
    ) {
        when (activeTerrainMap) {
            "MAGADI_50KM" -> {
                if (is1mResolutionMode) {
                    NativeEngineBridge.generate1mResolutionMesh(
                        segmentIndex = selectedRoadSegment,
                        centerForwardM = roadPositionOnSegmentM,
                        contourInterval = 5f,
                        stepTerracing = terrainState.stepTerracingFactor
                    )
                } else if (selectedSubSegment > 0) {
                    NativeEngineBridge.generateRoadSubSegmentMesh(
                        segmentIndex = selectedRoadSegment,
                        subSegmentIndex = selectedSubSegment,
                        gridSize = 49,
                        contourInterval = terrainState.contourInterval,
                        stepTerracing = terrainState.stepTerracingFactor
                    )
                } else {
                    when (magadiViewMode) {
                        "HUMAN_START" -> {
                            NativeEngineBridge.generateMagadiHumanStartMesh(
                                walkDistanceM = humanWalkDistanceM,
                                gridSize = 45,
                                contourInterval = terrainState.contourInterval,
                                stepTerracing = terrainState.stepTerracingFactor
                            )
                        }
                        "COMBINED_101KM", "OVERVIEW_50KM" -> {
                            NativeEngineBridge.generateWholeCombinedTerrainMesh(
                                gridResX = 33,
                                gridResZ = 129,
                                contourInterval = terrainState.contourInterval,
                                stepTerracing = terrainState.stepTerracingFactor
                            )
                        }
                        "CHUNKS_10KM" -> {
                            NativeEngineBridge.generateMagadiChunkMesh(
                                col = selectedMagadiCol,
                                row = selectedMagadiRow,
                                gridSize = 41,
                                contourInterval = terrainState.contourInterval,
                                stepTerracing = terrainState.stepTerracingFactor
                            )
                        }
                        else -> {
                            // Road Segments 1 to 10 (100% Pure C++ libapex_vulkan.so Engine)
                            NativeEngineBridge.generateRoadSegmentMesh(
                                segmentIndex = selectedRoadSegment,
                                gridSize = 49,
                                contourInterval = terrainState.contourInterval,
                                stepTerracing = terrainState.stepTerracingFactor
                            )
                        }
                    }
                }
            }
            "TEST_GROUND_001" -> {
                NativeEngineBridge.generateTestGround001Mesh(gridSize = 49)
            }
            else -> {
                NativeEngineBridge.generateTerrainMesh(terrainState, gridSize = 33, worldSize = 100f)
            }
        }
    }

    val isHumanActive = (activeTerrainMap == "MAGADI_50KM" && magadiViewMode == "HUMAN_START")
    var isControlsCollapsed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Quick Top Terrain Dataset Switcher (Always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isTownActive = activeTerrainMap == "MAGADI_TOWN_3X5"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isTownActive) NeonLime.copy(alpha = 0.24f) else DarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isTownActive) NeonLime else GlowingCyanBorder
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clickable { activeTerrainMap = "MAGADI_TOWN_3X5" }
                    .testTag("terrain_switch_magadi_town_3x5")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isTownActive) NeonLime else TextMuted,
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MAGADI TOWN 3×5KM (1M)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isTownActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isTownActive) NeonLime else CyberCyan
                    )
                }
            }

            val isCorridorActive = activeTerrainMap == "MAGADI_50KM"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isCorridorActive) LaserOrange.copy(alpha = 0.22f) else DarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCorridorActive) LaserOrange else DarkSurfaceHighlight
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clickable { activeTerrainMap = "MAGADI_50KM" }
                    .testTag("terrain_switch_magadi_corridor")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "C58 101KM CORRIDOR",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isCorridorActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCorridorActive) LaserOrange else TextSecondary
                    )
                }
            }

            val isProceduralActive = activeTerrainMap == "PROCEDURAL_FBM" || activeTerrainMap == "TEST_GROUND_001"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isProceduralActive) CyberCyan.copy(alpha = 0.22f) else DarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isProceduralActive) CyberCyan else DarkSurfaceHighlight
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clickable { activeTerrainMap = "PROCEDURAL_FBM" }
                    .testTag("terrain_switch_procedural_fbm")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "PROCEDURAL / TEST",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isProceduralActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isProceduralActive) CyberCyan else TextSecondary
                    )
                }
            }
        }

        if (activeTerrainMap == "MAGADI_TOWN_3X5") {
            MagadiTownScreen(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
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
                showGridContours = terrainState.showContourLines,
                contourInterval = terrainState.contourInterval,
                isHumanView = isHumanActive,
                is1mResolution = is1mResolutionMode,
                walkDistanceM = humanWalkDistanceM,
                selectedRoadDistanceM = if (isHumanActive) humanWalkDistanceM else roadPositionOnSegmentM,
                showGroundPiece = isGroundPieceActive,
                groundPieceDatumASL = groundPieceDatumM,
                onRotateDrag = { dx, dy ->
                    yaw += dx * 0.007f
                    pitch = (pitch - dy * 0.007f).coerceIn(0.04f, 1.48f)
                },
                onPanDrag = { dx, dy ->
                    panX += dx
                    panY += dy
                },
                onZoomChange = { factor ->
                    zoom = (zoom * factor).coerceIn(0.05f, 50.0f)
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
                // Zoom In Button (+)
                IconButton(
                    onClick = { zoom = (zoom * 1.30f).coerceIn(0.05f, 50.0f) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_toolbar_zoom_in_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In (Unrestricted)",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zoom Out Button (-)
                IconButton(
                    onClick = { zoom = (zoom * 0.77f).coerceIn(0.05f, 50.0f) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_toolbar_zoom_out_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out (Unrestricted)",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 1-Meter Resolution View Toggle Button
                IconButton(
                    onClick = {
                        is1mResolutionMode = !is1mResolutionMode
                        if (is1mResolutionMode) {
                            yaw = 0.0f
                            pitch = 0.28f
                            zoom = 3.5f
                            panX = 0f
                            panY = 0f
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (is1mResolutionMode) LaserOrange.copy(alpha = 0.40f) else DarkSurface.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("terrain_1m_resolution_btn")
                ) {
                    Text(
                        text = "1M",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (is1mResolutionMode) LaserOrange else TextSecondary
                    )
                }

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

                // Quick Road Segment 1 to 10 Selector (Pure C++ Engine)
                if (activeTerrainMap == "MAGADI_50KM") {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROAD (C58):",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = LaserOrange
                        )

                        // Option to see the Whole Combined 101km Terrain
                        val isCombined = (magadiViewMode == "COMBINED_101KM" || magadiViewMode == "OVERVIEW_50KM" || isMagadiOverview) && !magadiViewMode.startsWith("SEGMENT_") && magadiViewMode != "HUMAN_START"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCombined) NeonLime.copy(alpha = 0.35f) else DarkSurface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCombined) NeonLime else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    magadiViewMode = "COMBINED_101KM"
                                    activeTerrainMap = "MAGADI_50KM"
                                    isMagadiOverview = true
                                    selectedSubSegment = 0
                                    is1mResolutionMode = false
                                    yaw = 0.85f
                                    pitch = 0.55f
                                    zoom = 0.60f
                                    panX = 0f
                                    panY = 0f
                                }
                                .testTag("floating_road_seg_combined")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = if (isCombined) NeonLime else TextMuted,
                                    modifier = Modifier.size(5.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "COMBINED 101KM",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isCombined) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCombined) NeonLime else TextSecondary
                                )
                            }
                        }

                        for (seg in 1..10) {
                            val isSel = (selectedRoadSegment == seg) && !isCombined && (magadiViewMode.startsWith("SEGMENT_") || magadiViewMode == "FIRST_10KM" || magadiViewMode == "SEGMENT_10KM")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) CyberCyan.copy(alpha = 0.35f) else DarkSurface.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) CyberCyan else Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        selectedRoadSegment = seg
                                        magadiViewMode = "SEGMENT_$seg"
                                        activeTerrainMap = "MAGADI_50KM"
                                        isMagadiOverview = false
                                    }
                                    .testTag("floating_road_seg_$seg")
                            ) {
                                Text(
                                    text = "$seg",
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) CyberCyan else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
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
                        Triple("101km Pan", 0.85f, 0.55f),
                        Triple("3D Iso", 0.785f, 0.615f),
                        Triple("Orbit", 0.52f, 0.85f),
                        Triple("Driver", 0f, 0.28f),
                        Triple("Top-Down", 0f, 1.48f),
                        Triple("1m Zoom", 0f, 0.28f)
                    ).forEach { (label, yTarget, pTarget) ->
                        val is1mPreset = label == "1m Zoom"
                        val is101kmPreset = label == "101km Pan"
                        val isCurrent = if (is1mPreset) is1mResolutionMode
                                        else if (is101kmPreset) (magadiViewMode == "COMBINED_101KM" || isMagadiOverview)
                                        else (!is1mResolutionMode && !isMagadiOverview && abs(yaw - yTarget) < 0.18f && abs(pitch - pTarget) < 0.18f)
                        val presetColor = if (is1mPreset) LaserOrange else if (is101kmPreset) NeonLime else CyberCyan
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) presetColor.copy(alpha = 0.32f) else DarkSurface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) presetColor else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    yaw = yTarget
                                    pitch = pTarget
                                    if (is101kmPreset) {
                                        magadiViewMode = "COMBINED_101KM"
                                        activeTerrainMap = "MAGADI_50KM"
                                        isMagadiOverview = true
                                        is1mResolutionMode = false
                                        zoom = 0.60f
                                    } else if (is1mPreset) {
                                        is1mResolutionMode = true
                                        zoom = 2.4f
                                    } else {
                                        is1mResolutionMode = false
                                        zoom = 1.0f
                                    }
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
                                color = if (isCurrent) presetColor else TextSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Dedicated Quick Zoom Presets Row (Zero artificial restrictions)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ZOOM:",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    listOf(
                        Pair("0.25x", 0.25f),
                        Pair("0.60x", 0.60f),
                        Pair("1.0x", 1.0f),
                        Pair("2.5x", 2.5f),
                        Pair("5.0x", 5.0f),
                        Pair("15x", 15.0f)
                    ).forEach { (label, zTarget) ->
                        val isCurrent = abs(zoom - zTarget) < 0.08f * zTarget
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) LaserOrange.copy(alpha = 0.35f) else DarkSurface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) LaserOrange else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    zoom = zTarget
                                }
                                .testTag("zoom_preset_${label.replace(".", "_")}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) LaserOrange else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
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
                        viewMode = magadiViewMode,
                        onViewModeChange = { mode ->
                            magadiViewMode = mode
                            activeTerrainMap = "MAGADI_50KM"
                            if (mode.startsWith("SEGMENT_") || mode == "FIRST_10KM" || mode == "SEGMENT_10KM") {
                                isMagadiOverview = false
                                yaw = 0.65f
                                pitch = 0.52f
                                zoom = 1.05f
                                panX = 0f
                                panY = 0f
                            } else if (mode == "HUMAN_START") {
                                isMagadiOverview = false
                                yaw = 0.0f
                                pitch = 0.18f
                                zoom = 1.0f
                                panX = 0f
                                panY = 40f
                            } else if (mode == "OVERVIEW_50KM") {
                                isMagadiOverview = true
                                yaw = 0.785f
                                pitch = 0.615f
                                zoom = 1.0f
                                panX = 0f
                                panY = 0f
                            } else {
                                isMagadiOverview = false
                                yaw = 0.785f
                                pitch = 0.615f
                                zoom = 1.0f
                                panX = 0f
                                panY = 0f
                            }
                        },
                        selectedRoadSegment = selectedRoadSegment,
                        onSelectRoadSegment = { seg ->
                            selectedRoadSegment = seg
                            selectedSubSegment = 0
                            magadiViewMode = "SEGMENT_$seg"
                            activeTerrainMap = "MAGADI_50KM"
                            isMagadiOverview = false
                            yaw = 0.785f
                            pitch = 0.615f
                            zoom = 1.0f
                            panX = 0f
                            panY = 0f
                        },
                        selectedSubSegment = selectedSubSegment,
                        onSelectSubSegment = { sub ->
                            selectedSubSegment = sub
                        },
                        is1mResolution = is1mResolutionMode,
                        onToggle1mResolution = { is1mResolutionMode = it },
                        onZoomTo1mScale = {
                            is1mResolutionMode = true
                            yaw = 0.0f
                            pitch = 0.28f
                            zoom = 2.4f
                            panX = 0f
                            panY = 0f
                        },
                        roadPositionM = roadPositionOnSegmentM,
                        onRoadPositionChange = { roadPositionOnSegmentM = it },
                        onSnapCameraToRoad = {
                            yaw = 0.0f
                            pitch = 0.16f
                            zoom = 1.35f
                            panX = 0f
                            panY = -25f
                        },
                        walkDistanceM = humanWalkDistanceM,
                        onWalkDistanceChange = { humanWalkDistanceM = it },
                        eyeHeightType = humanEyeHeightType,
                        onEyeHeightTypeChange = { type ->
                            humanEyeHeightType = type
                            when (type) {
                                "STANDING_1_7M" -> {
                                    pitch = 0.10f
                                    panY = -30f
                                    zoom = 1.35f
                                }
                                "DRIVER_1_2M" -> {
                                    pitch = 0.06f
                                    panY = -38f
                                    zoom = 1.45f
                                }
                                "SAFARI_2_8M" -> {
                                    pitch = 0.16f
                                    panY = -22f
                                    zoom = 1.25f
                                }
                                "DRONE_25M" -> {
                                    pitch = 0.45f
                                    panY = 0f
                                    zoom = 1.05f
                                }
                            }
                        },
                        lookDirection = humanLookDirection,
                        onLookDirectionChange = { dir ->
                            humanLookDirection = dir
                            when (dir) {
                                "AHEAD_NORTH" -> { yaw = 0.0f; pitch = 0.10f }
                                "LEFT_TRONA" -> { yaw = -1.57f; pitch = 0.08f }
                                "RIGHT_BASALT" -> { yaw = 1.57f; pitch = 0.12f }
                                "BEHIND_MAGADI" -> { yaw = 3.14f; pitch = 0.10f }
                            }
                        },
                        isOverview = isMagadiOverview,
                        onToggleOverview = {
                            isMagadiOverview = it
                            magadiViewMode = if (it) "OVERVIEW_50KM" else "CHUNKS_10KM"
                            activeTerrainMap = "MAGADI_50KM"
                        },
                        selectedCol = selectedMagadiCol,
                        selectedRow = selectedMagadiRow,
                        onSelectChunk = { col, row ->
                            selectedMagadiCol = col
                            selectedMagadiRow = row
                            isMagadiOverview = false
                            magadiViewMode = "CHUNKS_10KM"
                            activeTerrainMap = "MAGADI_50KM"
                        },
                        state = terrainState,
                        onStateChange = { terrainState = it },
                        renderMode = renderMode,
                        onRenderModeChange = { renderMode = it },
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
                        },
                        isGroundPiece = isGroundPieceActive,
                        onToggleGroundPiece = { isGroundPieceActive = it },
                        groundPieceDatumM = groundPieceDatumM,
                        onSetCameraPose = { y, p, z ->
                            yaw = y
                            pitch = p
                            zoom = z
                            panX = 0f
                            panY = 0f
                        }
                    )
                }
                TerrainSubsystemTab.VULKAN_CPP_PIPELINE -> {
                    VulkanCppPipelineControls(
                        selectedCol = selectedMagadiCol,
                        selectedRow = selectedMagadiRow,
                        state = terrainState,
                        onStateChange = { terrainState = it },
                        onCopyCode = { code ->
                            clipboardManager.setText(AnnotatedString(code))
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
                                    terrainState = NativeEngineBridge.simulateErosionCycle(terrainState)
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

// Data model for human-friendly road segment telemetry
private data class RoadSegmentMeta(
    val index: Int,
    val name: String,
    val km: String,
    val elev: String,
    val climb: String,
    val grade: String,
    val landmark: String,
    val biome: String
)

private val ROAD_SEGMENTS_CATALOG = listOf(
    RoadSegmentMeta(1, "Lake Magadi Causeway & Trona Flats", "KM 0.0 - 10.1", "598m - 642m", "+44m", "+0.44%", "Magadi Soda Works & Causeway Bridge", "Trona Evaporite & Alkaline Lagoon"),
    RoadSegmentMeta(2, "Ol Keju Nero Basin & South Rift Margin", "KM 10.1 - 20.2", "642m - 718m", "+76m", "+0.75%", "Ol Keju Nero River Lugga", "Alluvial Fan & Semi-Arid Acacia Scrub"),
    RoadSegmentMeta(3, "Olorgesailie Prehistoric Basin & Fossil Beds", "KM 20.2 - 30.3", "718m - 845m", "+127m", "+1.26%", "Acheulean Site & Prehistoric Museum", "Lacustrine Silt & Volcanic Ash Beds"),
    RoadSegmentMeta(4, "Mount Olorgesailie Volcanic Slopes", "KM 30.3 - 40.4", "845m - 1025m", "+180m", "+1.78%", "Mount Olorgesailie Shield Lava Shoulder", "Basalt Lava Benches & Volcanic Talus"),
    RoadSegmentMeta(5, "Tinga Trading Center & Mid-Rift Plateau", "KM 40.4 - 50.5", "1025m - 1245m", "+220m", "+2.18%", "Tinga Market & Borehole Oasis", "Semi-Arid Pastoralist Plateau"),
    RoadSegmentMeta(6, "Kamukuru Step Faults & Tectonic Terraces", "KM 50.5 - 60.6", "1245m - 1515m", "+270m", "+2.67%", "Gregory Rift Step Fault Scarps", "Acacia Woodland & Tectonic Fault Steps"),
    RoadSegmentMeta(7, "Corner Baridi Foothills & Canyon Hairpins", "KM 60.6 - 70.7", "1515m - 1820m", "+305m", "+3.02%", "Deep Canyon Overlook & Hairpins", "Steep Basalt Bluffs & Talus Slopes"),
    RoadSegmentMeta(8, "Corner Baridi Summit Crest & Ngong Saddle", "KM 70.7 - 80.8", "1820m - 1980m (Peak)", "+160m", "+1.15%", "Corner Baridi (Cold Corner) 1980m Summit", "Highland Mist Grassland & Heath"),
    RoadSegmentMeta(9, "Kiserian Ridge & Oloolua Foothills", "KM 80.8 - 90.9", "1865m - 1795m", "-70m", "-0.69%", "Kiserian Town & Oloolua Forest Ridge", "Sub-Humid Highland Ridge & Farmland"),
    RoadSegmentMeta(10, "Ongata Rongai to Nairobi Bomas / Galleria", "KM 90.9 - 101.0", "1795m - 1750m", "-45m", "-0.45%", "Bomas of Kenya / Galleria Mall Junction", "Nairobi Volcanic Plateau & Open Plains")
)

@Composable
private fun KenyaMagadiControls(
    viewMode: String = "HUMAN_START",
    onViewModeChange: (String) -> Unit = {},
    selectedRoadSegment: Int = 1,
    onSelectRoadSegment: (Int) -> Unit = {},
    selectedSubSegment: Int = 0,
    onSelectSubSegment: (Int) -> Unit = {},
    is1mResolution: Boolean = false,
    onToggle1mResolution: (Boolean) -> Unit = {},
    onZoomTo1mScale: () -> Unit = {},
    roadPositionM: Float = 0f,
    onRoadPositionChange: (Float) -> Unit = {},
    onSnapCameraToRoad: () -> Unit = {},
    walkDistanceM: Float = 0f,
    onWalkDistanceChange: (Float) -> Unit = {},
    eyeHeightType: String = "STANDING_1_7M",
    onEyeHeightTypeChange: (String) -> Unit = {},
    lookDirection: String = "AHEAD_NORTH",
    onLookDirectionChange: (String) -> Unit = {},
    isOverview: Boolean,
    onToggleOverview: (Boolean) -> Unit,
    selectedCol: Int,
    selectedRow: Int,
    onSelectChunk: (Int, Int) -> Unit,
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit,
    renderMode: TerrainRenderMode,
    onRenderModeChange: (TerrainRenderMode) -> Unit,
    mesh: TerrainMesh3D,
    onFocusPreset: (Float, Float) -> Unit,
    onSelectFile: (NativeFileTreeItem) -> Unit,
    isGroundPiece: Boolean = true,
    onToggleGroundPiece: (Boolean) -> Unit = {},
    groundPieceDatumM: Float = 400.0f,
    onSetCameraPose: (Float, Float, Float) -> Unit = { _, _, _ -> }
) {
    val roadChunks = setOf(
        Pair(4, 4), Pair(3, 4), Pair(3, 3), Pair(2, 2), Pair(1, 1), Pair(1, 0), Pair(0, 0)
    )

    val chunkId = "chunk_%02d_%02d".format(selectedCol, selectedRow)
    val latMin = -1.90 + selectedRow * 0.09
    val latMax = latMin + 0.09
    val lonMin = 36.25 + selectedCol * 0.09
    val lonMax = lonMin + 0.09
    val isCombinedMode = viewMode == "COMBINED_101KM" || viewMode == "OVERVIEW_50KM" || (isOverview && !viewMode.startsWith("SEGMENT_") && viewMode != "HUMAN_START")
    val isSegmentMode = (viewMode.startsWith("SEGMENT_") || viewMode == "FIRST_10KM" || viewMode == "SEGMENT_10KM") && !isCombinedMode
    val isHumanMode = viewMode == "HUMAN_START"
    val isOverviewMode = isCombinedMode
    val isTilesMode = viewMode == "CHUNKS_10KM"
    val currentElevAtWalk = 598.0f + (walkDistanceM / 1000f) * 18.5f
    val climbToEscarpment = 1980.0f - currentElevAtWalk
    val currentSegMeta = ROAD_SEGMENTS_CATALOG.getOrElse(selectedRoadSegment - 1) { ROAD_SEGMENTS_CATALOG.first() }

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
                            text = "MAGADI RD (C58/B19) — 101 KM CORRIDOR ELEVATION",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LaserOrange
                        )
                        Text(
                            text = "Lake Magadi (598m) ↔ Corner Baridi Summit (1,980m) ↔ Nairobi Bomas (1,750m) • 100% C++ Engine",
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
                            text = when {
                                isCombinedMode -> "WHOLE COMBINED 101KM"
                                isSegmentMode -> "SEGMENT $selectedRoadSegment / 10"
                                isHumanMode -> "HUMAN 1.7m VIEW"
                                else -> chunkId.uppercase()
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LaserOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode toggle buttons: Road Segments 1-10 vs Whole Combined 101km vs Human View vs 10km Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            onToggleOverview(true)
                            onViewModeChange("COMBINED_101KM")
                            onSetCameraPose(0.85f, 0.55f, 0.60f)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCombinedMode) NeonLime.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                            contentColor = if (isCombinedMode) NeonLime else TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCombinedMode) NeonLime else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(1.2f).height(34.dp).testTag("mode_combined_101km_btn")
                    ) {
                        Text("Whole 101km", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onToggleOverview(false)
                            onViewModeChange("SEGMENT_$selectedRoadSegment")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSegmentMode) CyberCyan.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                            contentColor = if (isSegmentMode) CyberCyan else TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSegmentMode) CyberCyan else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(1.05f).height(34.dp)
                    ) {
                        Text("Segments 1-10", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onViewModeChange("HUMAN_START") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHumanMode) LaserOrange.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                            contentColor = if (isHumanMode) LaserOrange else TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHumanMode) LaserOrange else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(0.95f).height(34.dp)
                    ) {
                        Text("Human 1.7m", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onToggleOverview(false)
                            onViewModeChange("CHUNKS_10KM")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTilesMode) TextPrimary.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            contentColor = if (isTilesMode) TextPrimary else TextSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isTilesMode) TextPrimary else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(0.85f).height(34.dp)
                    ) {
                        Text("10km Tiles", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==================== WHOLE COMBINED TERRAIN (101 KM CORRIDOR) CARD ====================
        if (isCombinedMode) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth().testTag("whole_combined_terrain_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = NeonLime,
                                    modifier = Modifier.size(7.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WHOLE COMBINED TERRAIN (101 KM)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonLime
                                )
                            }
                            Text(
                                text = "All 10 Segments Unified in Pure C++ • 101,000m Continuous Landscape",
                                fontSize = 9.sp,
                                color = CyberCyan
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonLime.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime)
                        ) {
                            Text(
                                text = "4,257 VERTICES • 8,192 Δ",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonLime,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Viewing the whole combined 3D terrain corridor from Lake Magadi basin (598m ASL) up across the Gregory Rift fault scarps, cresting over Corner Baridi escarpment pass (1,980m ASL summit), and descending through Kiserian into Nairobi Bomas/Galleria (1,750m ASL). Supported by the 400m ASL basalt bedrock ground piece.",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        lineHeight = 13.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 101km Corridor Stationing & Elevation Key Milestones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("KM 0.0 START", fontSize = 7.5.sp, color = TextMuted)
                                Text("598 m", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = NeonLime)
                                Text("Lake Magadi", fontSize = 7.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("KM 30.3 VOLC", fontSize = 7.5.sp, color = TextMuted)
                                Text("980 m", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberCyan)
                                Text("Olorgesailie", fontSize = 7.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("KM 50.5 FAULT", fontSize = 7.5.sp, color = TextMuted)
                                Text("1,280 m", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = LaserOrange)
                                Text("Kamukuru", fontSize = 7.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("KM 76.5 SUMMIT", fontSize = 7.5.sp, color = TextMuted)
                                Text("1,980 m", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFFFD700))
                                Text("Corner Baridi", fontSize = 7.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text("KM 101.0 END", fontSize = 7.5.sp, color = TextMuted)
                                Text("1,750 m", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberCyan)
                                Text("Nairobi Bomas", fontSize = 7.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "FOCUS CAMERA ON CORRIDOR SECTION:",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { onSetCameraPose(0.35f, 0.40f, 1.25f) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = NeonLime),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("Magadi KM 0", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSetCameraPose(0.65f, 0.50f, 1.15f) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = CyberCyan),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("Volcano KM 30", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSetCameraPose(0.95f, 0.55f, 1.20f) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("Summit KM 76", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSetCameraPose(0.85f, 0.55f, 0.58f) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = LaserOrange),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("Full 101km", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==================== ROAD SEGMENTS 1 TO 10 SELECTOR CARD ====================
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROAD SEGMENT SELECTOR (1 TO 10)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeonLime.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime)
                    ) {
                        Text(
                            text = "100% C++ ENGINE (libapex_vulkan.so)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonLime,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select any road segment from 1 to 10 along the complete 101km C58 corridor (Nairobi Bomas to Lake Magadi). All 3D terrain elevation, fault scarps, road grading, and contours are generated strictly in C++.",
                    fontSize = 9.5.sp,
                    color = TextSecondary,
                    lineHeight = 13.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Segment Selector Buttons 1 to 10 (Just like contours chips)
                Text(
                    text = "SELECT ROAD SEGMENT (1 TO 10):",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (seg in 1..10) {
                        val isSel = (selectedRoadSegment == seg) && isSegmentMode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) CyberCyan.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) CyberCyan else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectRoadSegment(seg)
                                    onViewModeChange("SEGMENT_$seg")
                                }
                                .testTag("road_segment_selector_chip_$seg")
                        ) {
                            Text(
                                text = "$seg",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSel) CyberCyan else TextSecondary,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 1.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-Segment Selector (Each 10.1km segment further rendered into 10 sub-segments of 1.01km)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUB-SEGMENT (10 SUB-SEGMENTS, 1.01 KM EACH):",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime
                    )
                    if (selectedSubSegment > 0) {
                        val subStartKm = (selectedRoadSegment - 1) * 10.1f + (selectedSubSegment - 1) * 1.01f
                        val subEndKm = subStartKm + 1.01f
                        Text(
                            text = "KM %.2f - %.2f (1,010m)".format(subStartKm, subEndKm),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = LaserOrange
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val isAll = selectedSubSegment == 0
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAll) NeonLime.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAll) NeonLime else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .weight(1.35f)
                            .clickable { onSelectSubSegment(0) }
                            .testTag("sub_segment_selector_all")
                    ) {
                        Text(
                            text = "ALL",
                            fontSize = 9.5.sp,
                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isAll) NeonLime else TextSecondary,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 1.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    for (sub in 1..10) {
                        val isSubSel = selectedSubSegment == sub
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSubSel) LaserOrange.copy(alpha = 0.35f) else DarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSubSel) LaserOrange else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectSubSegment(sub)
                                    onRoadPositionChange((sub - 1) * 1010f)
                                }
                                .testTag("sub_segment_selector_chip_$sub")
                        ) {
                            Text(
                                text = "$sub",
                                fontSize = 10.sp,
                                fontWeight = if (isSubSel) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSubSel) LaserOrange else TextSecondary,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 1.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detailed Human-Friendly Telemetry Card for Selected Segment
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceHighlight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SEGMENT ${currentSegMeta.index}: ${currentSegMeta.km.uppercase()}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LaserOrange
                            )
                            Text(
                                text = currentSegMeta.elev,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonLime
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = currentSegMeta.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vertical Climb: ${currentSegMeta.climb}", fontSize = 9.5.sp, color = TextPrimary)
                                Text("Average Grade: ${currentSegMeta.grade}", fontSize = 9.5.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text("Landmark: ${currentSegMeta.landmark}", fontSize = 9.sp, color = TextSecondary)
                                Text("Biome: ${currentSegMeta.biome}", fontSize = 9.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• 3D Contours: 25m cyan isolines & 100m gold index contours directly embedded in C++ mesh vertices.",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // ==================== 1M RESOLUTION INSPECTION MODE CARD ====================
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = if (is1mResolution) 0.85f else 0.35f)),
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
                            text = "1-METER RESOLUTION VIEW (C++ DEM)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = LaserOrange
                        )
                        Text(
                            text = "64m × 64m Patch • 1.0m × 1.0m Vertex Grid • Ultra-HD Pavement & Verge",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = is1mResolution,
                        onCheckedChange = { onToggle1mResolution(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LaserOrange,
                            checkedTrackColor = LaserOrange.copy(alpha = 0.35f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onZoomTo1mScale,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (is1mResolution) LaserOrange else DarkSurfaceHighlight,
                            contentColor = if (is1mResolution) Color.Black else LaserOrange
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange),
                        modifier = Modifier.weight(1f).height(36.dp).testTag("zoom_1m_resolution_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom to 1m Scale",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zoom to 1m Scale", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grid Step:", fontSize = 9.sp, color = TextMuted)
                            Text("1.00 m / Cell", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonLime)
                        }
                    }
                }
            }
        }

        // ==================== EXACT ROAD POSITION ON TERRAIN CARD ====================
        val activeRoadPoint = remember(selectedRoadSegment, roadPositionM) {
            NativeEngineBridge.getRoadPointAt(selectedRoadSegment, roadPositionM)
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f)),
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
                            text = "ROAD POSITION ON TERRAIN (SEGMENT $selectedRoadSegment / 10)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                        Text(
                            text = "2.0 km Corridor Width (1.0 km Left • 1.0 km Right from Centerline)",
                            fontSize = 9.sp,
                            color = NeonLime
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyberCyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                    ) {
                        Text(
                            text = "KM %.1f / 101.0 KM".format(activeRoadPoint.globalKmM),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Road Position Slider along this 10.1 km segment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "POSITION ALONG SEGMENT:",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "%.2f km / 10.10 km (%.0f m)".format(roadPositionM / 1000f, roadPositionM),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = LaserOrange
                    )
                }

                Slider(
                    value = roadPositionM,
                    onValueChange = { onRoadPositionChange(it) },
                    valueRange = 0f..10100f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkSurfaceHighlight
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("road_position_slider")
                )

                // Quick Jump Position Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "Start 0km" to 0f,
                        "2.5 km" to 2500f,
                        "Mid 5.0km" to 5050f,
                        "7.5 km" to 7575f,
                        "End 10.1km" to 10100f
                    ).forEach { (label, distM) ->
                        val isCurrent = abs(roadPositionM - distM) < 250f
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) LaserOrange.copy(alpha = 0.3f) else DarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) LaserOrange else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onRoadPositionChange(distM) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) LaserOrange else TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detailed 4-metric telemetry grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("ROAD ELEVATION", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "%.1f m".format(activeRoadPoint.rawElevation),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonLime
                            )
                            Text("ASL Datum", fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("LATERAL OFFSET", fontSize = 8.sp, color = TextMuted)
                            val curveDir = if (activeRoadPoint.lateralM > 5f) "East" else if (activeRoadPoint.lateralM < -5f) "West" else "Center"
                            Text(
                                text = "%+.1f m".format(activeRoadPoint.lateralM),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LaserOrange
                            )
                            Text(curveDir, fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("ROAD GRADE", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "%+.2f%%".format(activeRoadPoint.gradePercent),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                            Text(if (activeRoadPoint.gradePercent >= 0) "Climb" else "Descent", fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("HEADING", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "%.0f°".format(activeRoadPoint.headingDeg),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F)
                            )
                            Text("NNE Azimuth", fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Civil Engineering Cut & Fill Prism & Topographic Contour Coupling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1B2A), RoundedCornerShape(6.dp))
                        .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "CIVIL CUT/FILL PRISM & CONTOURS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                        val cutFillLabel = if (activeRoadPoint.cutFillDepthM > 0.25f) {
                            "Embankment Fill (+%.1fm above ground)".format(activeRoadPoint.cutFillDepthM)
                        } else if (activeRoadPoint.cutFillDepthM < -0.25f) {
                            "Hillside Cut (%.1fm excavation)".format(activeRoadPoint.cutFillDepthM)
                        } else {
                            "At Grade (Balanced Cut/Fill ±0.2m)"
                        }
                        Text(
                            text = cutFillLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activeRoadPoint.cutFillDepthM >= 0f) NeonLime else LaserOrange
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "NEXT CONTOUR LINE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                        Text(
                            text = "%.0fm in %.0fm".format(activeRoadPoint.nextContourElevM, activeRoadPoint.distToNextContourM),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // GPS & Landmark readout bar
                val estLat = -1.90f + (activeRoadPoint.globalKmM / 101.0f) * 0.60f
                val estLon = 36.28f + (activeRoadPoint.globalKmM / 101.0f) * 0.45f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1520), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GPS: %.4f° S, %.4f° E • C58 Corridor".format(abs(estLat), estLon),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = "Landmark: ${currentSegMeta.landmark}",
                            fontSize = 8.5.sp,
                            color = TextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { onSnapCameraToRoad() },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("snap_camera_to_road_btn")
                    ) {
                        Text("View On Road", fontSize = 8.5.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==================== 3D SOLID GROUND PIECE (400M ASL DATUM) CARD ====================
        val groundMetrics = remember(selectedRoadSegment, groundPieceDatumM) {
            NativeEngineBridge.getGroundPieceMetrics(selectedRoadSegment, groundPieceDatumM)
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = if (isGroundPiece) 0.65f else 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = CyberCyan,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3D SOLID GROUND PIECE (400M ASL)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                        Text(
                            text = "Solid Geological Block • Basalt Bedrock Foundation",
                            fontSize = 9.sp,
                            color = NeonLime
                        )
                    }
                    Switch(
                        checked = isGroundPiece,
                        onCheckedChange = { onToggleGroundPiece(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("ground_piece_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Replaces the floating 2D paper-sheet surface with a solid 3D geological landscape block down to 400m Above Sea Level (ASL). 4 vertical perimeter basalt rock faces show authentic horizontal phonolite/trachyte strata banding and a luminous cyan 400m baseline datum.",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    lineHeight = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Ground Piece Telemetry Metric Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("DATUM BASELINE", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "400.0 m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                            Text("Above Sea Level", fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("MAGADI BASALT", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "+198 m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonLime
                            )
                            Text("Foundation at Lake", fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("SEGMENT RELIEF", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "+%.0fm".format(groundMetrics.depthMaxM),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = LaserOrange
                            )
                            Text("Max Rock Depth", fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceHighlight,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Text("SUMMIT HEIGHT", fontSize = 8.sp, color = TextMuted)
                            Text(
                                text = "+1,580 m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD700)
                            )
                            Text("Corner Baridi Peak", fontSize = 7.5.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Camera Angle Presets for Ground Piece Inspection
                Text(
                    text = "INSPECT 3D GROUND BLOCK ELEVATION:",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onSetCameraPose(0.785f, 0.615f, 1.0f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceHighlight,
                            contentColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("ground_piece_iso_btn")
                    ) {
                        Text("3D Iso Cut", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSetCameraPose(0.0f, 0.22f, 1.15f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceHighlight,
                            contentColor = NeonLime
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.45f)),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("ground_piece_front_btn")
                    ) {
                        Text("Front Slab", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSetCameraPose(-1.35f, 0.45f, 1.05f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceHighlight,
                            contentColor = LaserOrange
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.45f)),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("ground_piece_side_btn")
                    ) {
                        Text("Side Strata", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSetCameraPose(0.52f, 0.85f, 0.95f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceHighlight,
                            contentColor = Color(0xFFFFD700)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.45f)),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("ground_piece_orbit_btn")
                    ) {
                        Text("Oblique", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==================== HUMAN VIEWPOINT DEDICATED CARDS ====================
        if (isHumanMode) {
            // A. Human Eye-Level Telemetry & Horizon Target Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HUMAN EYE-LEVEL PERSPECTIVE METRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonLime
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonLime.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime)
                        ) {
                            Text(
                                text = "HEADING: 028° NNE",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonLime,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("GROUND DATUM", fontSize = 8.5.sp, color = TextMuted)
                                Text(
                                    text = "%.1f m".format(currentElevAtWalk),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = LaserOrange
                                )
                                Text("Lake Magadi Basin", fontSize = 8.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("EYE ELEVATION", fontSize = 8.5.sp, color = TextMuted)
                                val eyeOffset = when (eyeHeightType) {
                                    "DRIVER_1_2M" -> 1.20f
                                    "SAFARI_2_8M" -> 2.80f
                                    "DRONE_25M" -> 25.0f
                                    else -> 1.70f
                                }
                                Text(
                                    text = "%.2f m".format(currentElevAtWalk + eyeOffset),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberCyan
                                )
                                Text(
                                    text = when (eyeHeightType) {
                                        "DRIVER_1_2M" -> "Cockpit (1.2m)"
                                        "SAFARI_2_8M" -> "Roof (2.8m)"
                                        "DRONE_25M" -> "Drone (25m)"
                                        else -> "Standing (1.7m)"
                                    },
                                    fontSize = 8.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceHighlight,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("HORIZON CLIMB", fontSize = 8.5.sp, color = TextMuted)
                                Text(
                                    text = "+%.0f m".format(climbToEscarpment),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonLime
                                )
                                Text("Ngong Escarpment 1980m", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // B. Walk / Drive Progression Along C58 Road (0 to 3,000m)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WALK ALONG C58 ROAD (Lake Magadi -> Nairobi)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                        Text(
                            text = "%.0f m / 3,000 m".format(walkDistanceM),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = walkDistanceM,
                        onValueChange = { onWalkDistanceChange(it) },
                        valueRange = 0f..3000f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = DarkSurfaceHighlight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Current location status description
                    val currentFeature = when {
                        walkDistanceM < 200f -> "Origin: Lake Magadi Causeway • 8m asphalt raised 0.8m over white trona soda flats."
                        walkDistanceM < 600f -> "Causeway Bridge (500m): Crossing alkaline brine channel connecting southern & northern lagoons."
                        walkDistanceM < 1400f -> "Lake Margin (1.2km): Transition from salt crust to volcanic gravel verge & dwarf acacia bushes."
                        walkDistanceM < 2200f -> "Fault Approach (1.8km): Gentle +18.5m/km climb towards first basalt step terrace."
                        else -> "First Basalt Step-Fault Scarp (2.5-3.0km): Road enters dark volcanic lava cliff cutting at 653m ASL."
                    }
                    Text(
                        text = currentFeature,
                        fontSize = 9.5.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Jump Waypoint Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("0m Origin", 0f),
                            Pair("500m Bridge", 500f),
                            Pair("1.2km Margin", 1200f),
                            Pair("2.5km Scarp", 2500f)
                        ).forEach { (label, dist) ->
                            val isSel = abs(walkDistanceM - dist) < 50f
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) CyberCyan.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onWalkDistanceChange(dist) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) CyberCyan else TextSecondary,
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // C. Eye-Level Perspective Height & Look Direction Buttons
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "OBSERVER EYE HEIGHT & PERSPECTIVE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = LaserOrange
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Standing 1.7m", "STANDING_1_7M", "Surveyor walking"),
                            Triple("Driver 1.2m", "DRIVER_1_2M", "4x4 Cockpit"),
                            Triple("Safari 2.8m", "SAFARI_2_8M", "Roof Hatch"),
                            Triple("Drone 25m", "DRONE_25M", "Low Survey")
                        ).forEach { (label, key, desc) ->
                            val isSel = eyeHeightType == key
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) LaserOrange.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) LaserOrange else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onEyeHeightTypeChange(key) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 8.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) LaserOrange else TextPrimary
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 7.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "360° GLANCE & OBSERVATION DIRECTION",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("Ahead (NNE)", "AHEAD_NORTH"),
                            Pair("Left (Trona)", "LEFT_TRONA"),
                            Pair("Right (Basalt)", "RIGHT_BASALT"),
                            Pair("Behind (Plant)", "BEHIND_MAGADI")
                        ).forEach { (label, dir) ->
                            val isSel = lookDirection == dir
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) CyberCyan.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onLookDirectionChange(dir) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) CyberCyan else TextSecondary,
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // D. Comprehensive "What The Human Eye Sees" Physical Elevation Breakdown Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "WHAT YOU SEE FROM MAGADI ROAD START (HUMAN VIEWPOINT)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Immediate Foreground
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1. FOREGROUND (0 - 150m)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                                Text("ELEV: 598.0 m ASL", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = LaserOrange)
                            }
                            Text(
                                text = "• Standing on 8m dark asphalt causeway elevated 0.8m above blinding white crystallized trona (sodium sesquicarbonate) salt beds.\n" +
                                       "• Sharp visual contrast between black bitumen roadbed and gleaming snow-white alkali crust with cracked polygonal fissures.\n" +
                                       "• Distinct road verge: red volcanic gravel shoulders dropping to alkaline brine pools.",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Lateral Views
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("2. LATERAL PANORAMA (LEFT / RIGHT)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                                Text("ELEV: 590m - 640m ASL", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = NeonLime)
                            }
                            Text(
                                text = "• WEST (Left): Expansive Lake Magadi soda pan stretching 30km south; alkaline thermal brine pools tinted vivid magenta/pink by halophilic spirulina algae; steam plumes drifting from 45°C–86°C hot springs.\n" +
                                       "• EAST (Right): Dark vesicular basalt boulders, alluvial fans of grey volcanic ash, sparse arid commiphora and dwarf acacia tortilis shrubs.\n" +
                                       "• SOUTH (Behind): Magadi Soda Company calcining chimneys, rail sidings, and factory township nestled on basalt ridge.",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Midground Terraces
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("3. MIDGROUND STEP FAULTS (500m - 15km)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Text("CLIMB: +50m to +350m", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                            }
                            Text(
                                text = "• Classic Gregory Rift horst-and-graben fault architecture: successive dark basalt fault scarps stepping upward like gigantic terraces toward the north.\n" +
                                       "• The C58 asphalt road gently climbs along natural structural contour benches (+18.5m/km) to bypass vertical volcanic rock bluffs toward Ol Keju Nero.",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4. Horizon Escarpment
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("4. DISTANT HORIZON (25km - 45km)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8))
                                Text("SUMMIT: 1,980m (+1,382m RISE)", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFCE93D8))
                            }
                            Text(
                                text = "• Towering directly ahead on the horizon: The colossal dark indigo/purple wall of the Eastern Rift Valley Escarpment and Ngong Hills rising 1,382 meters above your eye level.\n" +
                                       "• Corner Baridi mountain pass clearly visible as a notch in the mountain barrier where the road ascends to Nairobi (1,795m).\n" +
                                       "• Atmospheric heat mirage: In midday heat (38°C–42°C), intense convection over the black asphalt creates a reflective silvery 'water mirage' hovering 300m–600m ahead.",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Contour-Defined Elevation Architecture Card (Sascha Willems Architecture)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONTOUR-DEFINED ELEVATION (Sascha Willems C++ Spec)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyberCyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                    ) {
                        Text(
                            text = "${state.contourInterval.toInt()}m ISOLINES",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Contours define how elevation is shaped: geological fault terraces in the Great Rift Valley, stepped scarp cliffs, and road cuts.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Contour Interval Selector Chips
                Text("CONTOUR INTERVAL (ELEVATION DELTA PER ISOLINE)", fontSize = 9.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val intervals = listOf(10f to "10m Fine", 25f to "25m Std", 50f to "50m Mid", 100f to "100m Bold")
                    for ((cVal, label) in intervals) {
                        val isSel = state.contourInterval == cVal
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) CyberCyan.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onStateChange(state.copy(contourInterval = cVal)) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) CyberCyan else TextSecondary,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stepped Terracing Slider (shows how contours shape elevation into tectonic fault steps)
                ControlSlider(
                    label = "Stepped Fault Terracing (Contours Define Steps)",
                    value = state.stepTerracingFactor,
                    valueRange = 0f..1f,
                    unit = "%",
                    onValueChange = { onStateChange(state.copy(stepTerracingFactor = it)) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Show 3D Contour Lines Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("3D Topographic Contour Lines", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Draws isoline ribbons on 3D mesh (Gold = 100m index)", fontSize = 9.sp, color = TextMuted)
                    }
                    Switch(
                        checked = state.showContourLines,
                        onCheckedChange = { onStateChange(state.copy(showContourLines = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3D Render Mode Quick Toggles
                Text("3D SHADING & CONTOUR VISUALIZATION MODES", fontSize = 9.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        TerrainRenderMode.CONTOURS_3D_ELEVATION to "3D Isolines",
                        TerrainRenderMode.HYPSOMETRIC_CONTOURS to "Hypsometric",
                        TerrainRenderMode.SHADED_TRIPLANAR to "PBR Splat",
                        TerrainRenderMode.WIREFRAME_CDLOD to "Vulkan Mesh"
                    )
                    for ((m, label) in modes) {
                        val isModeSel = renderMode == m
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isModeSel) LaserOrange.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isModeSel) LaserOrange else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onRenderModeChange(m) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                fontWeight = if (isModeSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isModeSel) LaserOrange else TextSecondary,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
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
                    text = if (isOverview) "101KM CORRIDOR OVERVIEW TELEMETRY" else "TILE TELEMETRY: $chunkId",
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
                            text = if (isOverview) "[0 m, 0 m] (101km span)" else "[${selectedCol * 10000} m, ${selectedRow * 10000} m]",
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

// ==================== VULKAN C++ TERRAIN PIPELINE CONTROLS ====================

@Composable
private fun VulkanCppPipelineControls(
    selectedCol: Int,
    selectedRow: Int,
    state: TerrainState,
    onStateChange: (TerrainState) -> Unit,
    onCopyCode: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("CHUNK_H") } // "CHUNK_H", "CHUNK_CPP", "MANAGER_CPP", "SHADER_FRAG", "SHADER_VERT"
    val chunkId = "chunk_%02d_%02d".format(selectedCol, selectedRow)

    val codeMap = remember {
        mapOf(
            "CHUNK_H" to """// Kenya Magadi Road (C58) 50km x 50km Terrain System - Vulkan Chunk Architecture
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunk.h
#pragma once
#include <vector>
#include <cstdint>

namespace Magadi {

struct TerrainVertex {
    float pos[3];          // Position: X, Y (elevation), Z (local meters)
    float normal[3];       // Normal vector (Nx, Ny, Nz) via central differencing
    float uv[2];           // Texture coordinates
    float rawElevation;    // Real-world elevation (590m - 1980m)
    float contourBand;     // Normalized elevation band [0.0 - 1.0]
    float isolineFactor;   // Screen-space contour line factor for fragment shader
    float slopeDeg;        // Surface slope in degrees [0 - 90]
    uint32_t surfaceType;  // MagadiSurfaceType
    float splatWeights[4]; // Savannah, Escarpment, Trona, Road
};

struct ContourConfig {
    float contourInterval = 25.0f;       // Distance between contour lines (meters)
    float indexContourInterval = 100.0f; // Bold index contour lines
    float stepTerracingFactor = 0.0f;    // Stepped tectonic fault terraces
    bool enableRoadCutEmbankment = true; // C58 road corridor grading
};

class MagadiTerrainChunk {
public:
    MagadiTerrainChunk(int col, int row, float originX, float originZ, float sizeM = 10000.0f);
    void GenerateMesh(const ContourConfig& contourConfig, ChunkLOD lod = ChunkLOD::LOD0_High);
    float SampleContourElevation(float worldX, float worldZ, const ContourConfig& cfg) const;
    void CalculateNormal(float worldX, float worldZ, const ContourConfig& cfg, float step, float outNormal[3]) const;
    MagadiSurfaceType DetermineSurfaceType(float worldX, float worldZ, float elev, float distRoad) const;

    const std::vector<TerrainVertex>& GetVertices() const { return m_vertices; }
    const std::vector<uint32_t>& GetIndices() const { return m_indices; }
};
}""",
            "CHUNK_CPP" to """// Kenya Magadi Road (C58) 50km x 50km Terrain System - Vulkan Chunk Implementation
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
#include "MagadiTerrainChunk.h"
#include <cmath>
#include <algorithm>

namespace Magadi {

float MagadiTerrainChunk::SampleContourElevation(float worldX, float worldZ, const ContourConfig& cfg) const {
    // 1. Regional Elevation Slope: East African Rift Valley floor (590m) to Eastern Escarpment (1980m)
    float u = std::max(0.0f, std::min(1.0f, (worldX + worldZ * 0.95f) / 95000.0f));
    float regionalElev = 590.0f + u * 1390.0f;

    // 2. Rift Valley Fault Scarps: North-South tectonic stepped contour terraces
    float fault1 = std::sin(worldX * 0.00035f + 0.8f) * 65.0f;
    float fault2 = std::sin(worldX * 0.00095f + worldZ * 0.00025f) * 35.0f;
    float continuousElev = regionalElev + fault1 + fault2;

    // 3. Lake Magadi Trona basin clamp (lake bed flat at ~590m)
    if (worldX < 11000.0f && worldZ < 11000.0f) {
        float lakeDist = std::sqrt(worldX * worldX + worldZ * worldZ);
        if (lakeDist < 9500.0f) {
            float lt = std::min(1.0f, lakeDist / 9500.0f);
            continuousElev = 590.0f + (continuousElev - 590.0f) * (lt * lt);
        }
    }

    // 4. Topographic Contour Shaping & Terracing
    // Contours explicitly define elevation steps when terracing is enabled
    if (cfg.stepTerracingFactor > 0.001f) {
        float stepH = cfg.contourInterval;
        float baseStep = std::floor(continuousElev / stepH) * stepH;
        float frac = (continuousElev - baseStep) / stepH;
        float steppedFrac = frac * frac * (3.0f - 2.0f * frac);
        float terracedElev = baseStep + steppedFrac * stepH;
        continuousElev = continuousElev * (1.0f - cfg.stepTerracingFactor) + terracedElev * cfg.stepTerracingFactor;
    }

    // 5. C58 Road Cut & Fill Corridor blending
    if (cfg.enableRoadCutEmbankment) {
        float roadElev = 0.0f;
        float distToRoad = DistanceToRoadCenterline(worldX, worldZ, roadElev);
        if (distToRoad < 8.0f) {
            continuousElev = roadElev;
        } else if (distToRoad < 35.0f) {
            float t = (distToRoad - 8.0f) / 27.0f;
            float smoothT = t * t * (3.0f - 2.0f * t);
            continuousElev = roadElev * (1.0f - smoothT) + continuousElev * smoothT;
        }
    }

    return std::max(590.0f, std::min(1980.0f, continuousElev));
}
}""",
            "MANAGER_CPP" to """// Kenya Magadi Road 50km Terrain System - Chunk Manager Implementation
#include "MagadiTerrainChunkManager.h"

namespace Magadi {

MagadiTerrainChunkManager::MagadiTerrainChunkManager() {
    m_contourConfig.contourInterval = 25.0f;
    m_contourConfig.indexContourInterval = 100.0f;
    m_contourConfig.stepTerracingFactor = 0.0f;
}

bool MagadiTerrainChunkManager::Initialize(const std::string& manifestPath) {
    m_chunks.clear();
    for (int row = 0; row < m_gridDim; ++row) {
        float originZ = row * m_chunkSpanM;
        for (int col = 0; col < m_gridDim; ++col) {
            float originX = col * m_chunkSpanM;
            auto chunk = std::make_unique<MagadiTerrainChunk>(col, row, originX, originZ, m_chunkSpanM);
            chunk->GenerateMesh(m_contourConfig, ChunkLOD::LOD0_High);
            m_chunks.push_back(std::move(chunk));
        }
    }
    return true;
}

void MagadiTerrainChunkManager::UpdateActiveArea(float vehicleX, float vehicleZ, int radius) {
    int cCol = static_cast<int>(std::floor(vehicleX / m_chunkSpanM));
    int cRow = static_cast<int>(std::floor(vehicleZ / m_chunkSpanM));
    for (int r = 0; r < m_gridDim; ++r) {
        for (int c = 0; c < m_gridDim; ++c) {
            int d = std::max(std::abs(c - cCol), std::abs(r - cRow));
            if (auto* ch = GetChunk(c, r)) {
                ch->GenerateMesh(m_contourConfig, (d <= radius) ? ChunkLOD::LOD0_High : ChunkLOD::LOD1_Medium);
            }
        }
    }
}
}""",
            "SHADER_FRAG" to """// Vulkan 1.3 GLSL Fragment Shader - 3D Contour Isolines & Hypsometric Tinting
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
#version 450
layout (location = 0) in vec3 inWorldPos;
layout (location = 1) in vec3 inNormal;
layout (location = 3) in float inElevation;
layout (location = 5) flat in uint inSurfaceType;

layout (push_constant) uniform PushConstants {
    mat4 mvpMatrix;
    vec4 lightDir;
    float contourInterval;       // 25.0m
    float indexContourInterval;  // 100.0m
    uint showContourIsolines;
} pushConstants;

layout (location = 0) out vec4 outFragColor;

void main() {
    vec3 N = normalize(inNormal);
    vec3 L = normalize(pushConstants.lightDir.xyz);
    float diffuse = 0.35 + 0.65 * max(dot(N, L), 0.15);

    // Hypsometric elevation coloring
    float t = clamp((inElevation - 590.0) / 1390.0, 0.0, 1.0);
    vec3 color = (t < 0.15) ? vec3(0.92, 0.96, 0.98) :
                 (t < 0.55) ? vec3(0.82, 0.74, 0.45) :
                 (t < 0.85) ? vec3(0.35, 0.52, 0.28) : vec3(0.42, 0.38, 0.36);

    // 3D Topographic Contour Isolines
    if (pushConstants.showContourIsolines == 1 && inSurfaceType != 1) {
        float dElev = max(fwidth(inElevation), 0.001);
        float cCoord = inElevation / pushConstants.contourInterval;
        float cDist = abs(fract(cCoord - 0.5) - 0.5) * pushConstants.contourInterval;
        float cLine = clamp(1.0 - cDist / (dElev * 1.2), 0.0, 1.0);

        // Golden Index contours every 100m
        float idxCoord = inElevation / pushConstants.indexContourInterval;
        float idxDist = abs(fract(idxCoord - 0.5) - 0.5) * pushConstants.indexContourInterval;
        float idxLine = clamp(1.0 - idxDist / (dElev * 2.2), 0.0, 1.0);

        color = mix(color, vec3(0.95, 0.95, 0.98), cLine * 0.55);
        color = mix(color, vec3(1.0, 0.78, 0.22), idxLine * 0.85);
    }

    outFragColor = vec4(color * diffuse, 1.0);
}""",
            "SHADER_VERT" to """// Vulkan 1.3 GLSL Vertex Shader - Magadi Terrain Chunk Pipeline
#version 450
layout (location = 0) in vec3 inPos;
layout (location = 1) in vec3 inNormal;
layout (location = 2) in vec2 inUV;
layout (location = 3) in float inRawElevation;
layout (location = 5) in uint inSurfaceType;

layout (push_constant) uniform PushConstants {
    mat4 mvpMatrix;
    vec4 lightDir;
    vec4 cameraPos;
    float contourInterval;
    float indexContourInterval;
} pushConstants;

layout (location = 0) out vec3 outWorldPos;
layout (location = 1) out vec3 outNormal;
layout (location = 2) out vec2 outUV;
layout (location = 3) out float outElevation;
layout (location = 5) flat out uint outSurfaceType;

void main() {
    outWorldPos = inPos;
    outNormal = inNormal;
    outUV = inUV;
    outElevation = inRawElevation;
    outSurfaceType = inSurfaceType;
    gl_Position = pushConstants.mvpMatrix * vec4(inPos, 1.0);
}"""
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Architecture Hero Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.5f)),
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
                            text = "SASCHA WILLEMS VULKAN PROCEDURAL TERRAIN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonLime
                        )
                        Text(
                            text = "github.com/SaschaWillems/Vulkan • VulkanInfiniteProceduralTerrain",
                            fontSize = 9.5.sp,
                            color = CyberCyan
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeonLime.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime)
                    ) {
                        Text(
                            text = "C++ 17 / VULKAN 1.3",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonLime,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Terrain chunks for Kenya Magadi C58 road corridor implemented explicitly in C/C++ with contour-guided elevation math, screen-space isoline shaders, and Vulkan GPU push constants.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        // 2. Active Chunk Telemetry Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ACTIVE CHUNK C++ TELEMETRY: $chunkId",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LaserOrange
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("GRID COORDINATES", fontSize = 9.sp, color = TextMuted)
                        Text("Col: $selectedCol, Row: $selectedRow (Tile 10km²)", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LOD0 VERTEX COUNT", fontSize = 9.sp, color = TextMuted)
                        Text("4,225 Vertices (65×65)", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonLime)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("VULKAN PRIMITIVE TOPOLOGY", fontSize = 9.sp, color = TextMuted)
                        Text("8,192 Triangles (Triangle List)", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("VMA GPU ALLOCATION", fontSize = 9.sp, color = TextMuted)
                        Text("168.4 KB VkBuffer", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = LaserOrange)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("CONTOUR ISOLINES", fontSize = 9.sp, color = TextMuted)
                        Text("${state.contourInterval.toInt()}m Intervals (100m Index)", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("FAULT SCARP TERRACING", fontSize = 9.sp, color = TextMuted)
                        Text("${(state.stepTerracingFactor * 100).toInt()}% Stepped", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = NeonLime)
                    }
                }
            }
        }

        // 3. C++ Source Code & Vulkan Shader Viewer Card
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
                        text = "C++ NATIVE SOURCE & GLSL SHADERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )

                    Button(
                        onClick = { onCopyCode(codeMap[selectedTab] ?: "") },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f), contentColor = CyberCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Copy Code", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Pills
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "CHUNK_H" to "MagadiTerrainChunk.h",
                        "CHUNK_CPP" to "MagadiTerrainChunk.cpp",
                        "MANAGER_CPP" to "ChunkManager.cpp",
                        "SHADER_FRAG" to "terrain.frag (Contour)",
                        "SHADER_VERT" to "terrain.vert"
                    )
                    for ((tKey, label) in tabs) {
                        val isSel = selectedTab == tKey
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) NeonLime.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonLime else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { selectedTab = tKey }
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) NeonLime else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Code block
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF090D14),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = codeMap[selectedTab] ?: "",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        modifier = Modifier
                            .padding(10.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

