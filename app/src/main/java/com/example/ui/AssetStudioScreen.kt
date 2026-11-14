package com.example.ui

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AssetItem
import com.example.model.FullAssetBundle
import com.example.model.InspectionMode
import com.example.model.MainAppView
import com.example.model.Scene3DObjectInstance
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AssetStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetStudioScreen(
    viewModel: AssetStudioViewModel,
    modifier: Modifier = Modifier
) {
    val mainAppView by viewModel.mainAppView.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val bundle by viewModel.assetBundle.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val inspectionMode by viewModel.inspectionMode.collectAsStateWithLifecycle()
    val folderTree by viewModel.folderTree.collectAsStateWithLifecycle()

    // 3D Scene Landscape state
    val sceneMode by viewModel.sceneMode.collectAsStateWithLifecycle()
    val sceneDensity by viewModel.sceneDensity.collectAsStateWithLifecycle()
    val grassViewMode by viewModel.grassViewMode.collectAsStateWithLifecycle()
    val singleGrassSpecies by viewModel.singleGrassSpecies.collectAsStateWithLifecycle()
    val singleGrassBlend by viewModel.singleGrassBlend.collectAsStateWithLifecycle()
    val sceneShowGridLines by viewModel.sceneShowGridLines.collectAsStateWithLifecycle()
    val sceneShadingMode by viewModel.sceneShadingMode.collectAsStateWithLifecycle()
    val sceneLightAngle by viewModel.sceneLightAngle.collectAsStateWithLifecycle()
    val selectedSceneInstance by viewModel.selectedSceneInstance.collectAsStateWithLifecycle()
    val sceneInstances by viewModel.sceneInstances.collectAsStateWithLifecycle()
    val isSceneLoading by viewModel.isSceneLoading.collectAsStateWithLifecycle()

    val sceneYaw by viewModel.sceneYaw.collectAsStateWithLifecycle()
    val scenePitch by viewModel.scenePitch.collectAsStateWithLifecycle()
    val sceneZoom by viewModel.sceneZoom.collectAsStateWithLifecycle()
    val scenePanX by viewModel.scenePanX.collectAsStateWithLifecycle()
    val scenePanY by viewModel.scenePanY.collectAsStateWithLifecycle()
    val isSceneAutoRotate by viewModel.isSceneAutoRotate.collectAsStateWithLifecycle()

    // Single Asset Viewport state
    val yaw by viewModel.yaw.collectAsStateWithLifecycle()
    val pitch by viewModel.pitch.collectAsStateWithLifecycle()
    val zoom by viewModel.zoom.collectAsStateWithLifecycle()
    val panX by viewModel.panX.collectAsStateWithLifecycle()
    val panY by viewModel.panY.collectAsStateWithLifecycle()

    val isAutoRotate by viewModel.isAutoRotate.collectAsStateWithLifecycle()
    val wireframeLineWidth by viewModel.wireframeLineWidth.collectAsStateWithLifecycle()
    val showVertexNodes by viewModel.showVertexNodes.collectAsStateWithLifecycle()
    val showBoundingBox by viewModel.showBoundingBox.collectAsStateWithLifecycle()
    val showCoordinateGizmo by viewModel.showCoordinateGizmo.collectAsStateWithLifecycle()
    val showFloorGrid by viewModel.showFloorGrid.collectAsStateWithLifecycle()
    val wireframePalette by viewModel.wireframeColorPalette.collectAsStateWithLifecycle()

    val shadingMode by viewModel.shadingMode.collectAsStateWithLifecycle()
    val lightAngle by viewModel.lightAngleRad.collectAsStateWithLifecycle()
    val selectedVolumeShapeId by viewModel.selectedVolumeShapeId.collectAsStateWithLifecycle()
    val explodeFactor by viewModel.explodeFactor.collectAsStateWithLifecycle()

    val activeTextureKey by viewModel.activeTextureKey.collectAsStateWithLifecycle()
    val activeChannelMode by viewModel.activeChannelMode.collectAsStateWithLifecycle()
    val textureBitmaps by viewModel.textureBitmaps.collectAsStateWithLifecycle()

    val activeJsonTab by viewModel.activeJsonTab.collectAsStateWithLifecycle()
    val jsonSearchQuery by viewModel.jsonSearchQuery.collectAsStateWithLifecycle()

    val previewFileNode by viewModel.previewFileNode.collectAsStateWithLifecycle()
    val previewFileContent by viewModel.previewFileContent.collectAsStateWithLifecycle()

    // Erosion & Procedural Noise state
    val erosionData by viewModel.erosionData.collectAsStateWithLifecycle()
    val isErosionRunning by viewModel.isErosionRunning.collectAsStateWithLifecycle()
    val triplanarEnabled by viewModel.triplanarEnabled.collectAsStateWithLifecycle()

    // C++ Procedural state
    val selectedCppAsset by viewModel.selectedCppAsset.collectAsStateWithLifecycle()
    val cppActiveTab by viewModel.cppActiveTab.collectAsStateWithLifecycle()
    val cppMesh by viewModel.cppMesh.collectAsStateWithLifecycle()
    val cppYaw by viewModel.cppYaw.collectAsStateWithLifecycle()
    val cppPitch by viewModel.cppPitch.collectAsStateWithLifecycle()
    val cppZoom by viewModel.cppZoom.collectAsStateWithLifecycle()
    val cppPanX by viewModel.cppPanX.collectAsStateWithLifecycle()
    val cppPanY by viewModel.cppPanY.collectAsStateWithLifecycle()
    val cppIsAutoRotate by viewModel.cppIsAutoRotate.collectAsStateWithLifecycle()
    val cppShowGrid by viewModel.cppShowGrid.collectAsStateWithLifecycle()
    val cppShadingMode by viewModel.cppShadingMode.collectAsStateWithLifecycle()
    val cppExplodeFactor by viewModel.cppExplodeFactor.collectAsStateWithLifecycle()
    val cppLightAngle by viewModel.cppLightAngle.collectAsStateWithLifecycle()
    val cppBay1OpenPct by viewModel.cppBay1OpenPct.collectAsStateWithLifecycle()
    val cppBay2OpenPct by viewModel.cppBay2OpenPct.collectAsStateWithLifecycle()
    val cppNeonSignOn by viewModel.cppNeonSignOn.collectAsStateWithLifecycle()
    val cppHeadlightsOn by viewModel.cppHeadlightsOn.collectAsStateWithLifecycle()
    val cppTaillightsOn by viewModel.cppTaillightsOn.collectAsStateWithLifecycle()
    val cppFovAngleDeg by viewModel.cppFovAngleDeg.collectAsStateWithLifecycle()
    val cppIsPerspective by viewModel.cppIsPerspective.collectAsStateWithLifecycle()

    // Modular Electric Post & Power Grid states
    val postModularType by viewModel.postModularType.collectAsStateWithLifecycle()
    val postHasTransformer by viewModel.postHasTransformer.collectAsStateWithLifecycle()
    val postHasStreetlight by viewModel.postHasStreetlight.collectAsStateWithLifecycle()
    val postStreetlightOn by viewModel.postStreetlightOn.collectAsStateWithLifecycle()
    val postHasGuyWire by viewModel.postHasGuyWire.collectAsStateWithLifecycle()

    val gridPostCount by viewModel.gridPostCount.collectAsStateWithLifecycle()
    val gridCornerAngleDeg by viewModel.gridCornerAngleDeg.collectAsStateWithLifecycle()
    val gridWireSagM by viewModel.gridWireSagM.collectAsStateWithLifecycle()
    val gridEnergized by viewModel.gridEnergized.collectAsStateWithLifecycle()
    val gridStreetlightsOn by viewModel.gridStreetlightsOn.collectAsStateWithLifecycle()

    // Cable Catenary Physics & 3D Cylinder states
    val cableGeometryMode by viewModel.cableGeometryMode.collectAsStateWithLifecycle()
    val cableCylinderSides by viewModel.cableCylinderSides.collectAsStateWithLifecycle()
    val cableConductorRadiusMm by viewModel.cableConductorRadiusMm.collectAsStateWithLifecycle()
    val cableTensionKn by viewModel.cableTensionKn.collectAsStateWithLifecycle()
    val cableLinearMassKgM by viewModel.cableLinearMassKgM.collectAsStateWithLifecycle()

    // Vulkan 1.3 Pipeline Engine states
    val renderingBackend by viewModel.renderingBackend.collectAsStateWithLifecycle()
    val vulkanPolygonMode by viewModel.vulkanPolygonMode.collectAsStateWithLifecycle()
    val vulkanMsaa by viewModel.vulkanMsaa.collectAsStateWithLifecycle()
    val vulkanValidationLayers by viewModel.vulkanValidationLayers.collectAsStateWithLifecycle()
    val vulkanDynamicRendering by viewModel.vulkanDynamicRendering.collectAsStateWithLifecycle()
    val vulkanPipelineCache by viewModel.vulkanPipelineCache.collectAsStateWithLifecycle()
    val vulkanPushConstantSag by viewModel.vulkanPushConstantSag.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen || mainAppView != MainAppView.TERRAIN_VIEW) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.setMainAppView(MainAppView.TERRAIN_VIEW)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ApexDrawerContent(
                currentView = mainAppView,
                onSelectView = { selected ->
                    coroutineScope.launch { drawerState.close() }
                    viewModel.setMainAppView(selected)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("top_bar_hamburger_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberCyan.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(mainAppView.emoji, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = mainAppView.label.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = mainAppView.subtitle.ifEmpty { "Vulkan 1.3 Procedural Suite" },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when (mainAppView) {
                                        MainAppView.MAGADI_TOWN_3X5 -> NeonLime
                                        MainAppView.KENYA_GRID_VIEW -> CyberCyan
                                        MainAppView.TERRAIN_VIEW -> CyberCyan
                                        MainAppView.ROAD_VIEW -> NeonLime
                                        MainAppView.AUDIO_VIEW -> HologramBlue
                                        MainAppView.SCENE_3D_VIEW -> LaserOrange
                                        MainAppView.CPP_PROCEDURAL_VIEW -> NeonLime
                                        MainAppView.SINGLE_ASSET_INSPECTOR -> ElectricViolet
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground,
                        titleContentColor = TextPrimary
                    ),
                    actions = {
                        IconButton(
                            onClick = {
                                val allViews = MainAppView.values()
                                val nextIndex = (allViews.indexOf(mainAppView) + 1) % allViews.size
                                viewModel.setMainAppView(allViews[nextIndex])
                            },
                            modifier = Modifier.testTag("top_bar_switch_view_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "Cycle Screen",
                                tint = CyberCyan
                            )
                        }

                        IconButton(
                            onClick = {
                                when (mainAppView) {
                                    MainAppView.SCENE_3D_VIEW -> viewModel.resetSceneCamera("Isometric")
                                    MainAppView.SINGLE_ASSET_INSPECTOR -> viewModel.resetCamera("Isometric")
                                    MainAppView.CPP_PROCEDURAL_VIEW -> viewModel.resetCppCamera("Isometric")
                                    else -> {}
                                }
                            },
                            modifier = Modifier.testTag("app_bar_reset_camera")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Camera", tint = TextSecondary)
                        }
                    }
                )
            },
            containerColor = DarkBackground,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            when (mainAppView) {
                MainAppView.MAGADI_TOWN_3X5 -> {
                    MagadiTownScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainAppView.KENYA_GRID_VIEW -> {
                    KenyaTerrainGridScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainAppView.TERRAIN_VIEW -> {
                    TerrainProceduralScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainAppView.ROAD_VIEW -> {
                    RoadProceduralScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainAppView.AUDIO_VIEW -> {
                    AudioProceduralScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainAppView.SCENE_3D_VIEW -> {
                    Scene3DView(
                        instances = sceneInstances,
                        selectedInstance = selectedSceneInstance,
                        sceneMode = sceneMode,
                        sceneDensity = sceneDensity,
                        grassViewMode = grassViewMode,
                        singleGrassSpecies = singleGrassSpecies,
                        singleGrassBlend = singleGrassBlend,
                        shadingMode = sceneShadingMode,
                        showGridLines = sceneShowGridLines,
                        yaw = sceneYaw,
                        pitch = scenePitch,
                        zoom = sceneZoom,
                        panX = scenePanX,
                        panY = scenePanY,
                        isAutoRotate = isSceneAutoRotate,
                        lightAngle = sceneLightAngle,
                        isLoading = isSceneLoading,
                        onSceneModeChange = { viewModel.setSceneMode(it) },
                        onSceneDensityChange = { viewModel.setSceneDensity(it) },
                        onGrassViewModeChange = { viewModel.setGrassViewMode(it) },
                        onSingleGrassSpeciesChange = { viewModel.setSingleGrassSpecies(it) },
                        onSingleGrassBlendChange = { viewModel.setSingleGrassBlend(it) },
                        onShadingModeChange = { viewModel.sceneShadingMode.value = it },
                        onToggleGridLines = { viewModel.sceneShowGridLines.value = !sceneShowGridLines },
                        onRotateDrag = { dx, dy -> viewModel.onSceneRotateDrag(dx, dy) },
                        onPanDrag = { dx, dy -> viewModel.onScenePanDrag(dx, dy) },
                        onZoomChange = { dz -> viewModel.onSceneZoomChange(dz) },
                        onToggleAutoRotate = { viewModel.isSceneAutoRotate.value = !isSceneAutoRotate },
                        onResetCamera = { viewModel.resetSceneCamera(it) },
                        onSelectInstance = { viewModel.selectSceneInstance(it) },
                        onOpenInspector = { viewModel.focusSceneInstanceInSingleView(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainAppView.SINGLE_ASSET_INSPECTOR -> {
                    if (isLoading || bundle == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyberCyan)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "INITIALIZING 3D PIPELINE...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = CyberCyan
                                )
                            }
                        }
                    } else {
                        val currentBundle = bundle!!
                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            // Quick Action Banner to return to 3D Scene View
                            Surface(
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ViewInAr,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Inspecting: ${selectedAsset?.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.setMainAppView(MainAppView.SCENE_3D_VIEW) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("nav_back_to_scene_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Landscape,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "10×10 SCENE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                    }
                                }
                            }

                            // 1. Asset Switcher Carousel
                            AssetSelectorRow(
                                catalog = catalog,
                                selectedAsset = selectedAsset,
                                onSelect = { viewModel.selectAsset(it) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Interactive 3D Viewport Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, GlowingCyanBorder, RoundedCornerShape(16.dp))
                            ) {
                                Viewport3DCanvas(
                                    bundle = currentBundle,
                                    mode = inspectionMode,
                                    yaw = yaw,
                                    pitch = pitch,
                                    zoom = zoom,
                                    panX = panX,
                                    panY = panY,
                                    isAutoRotate = isAutoRotate,
                                    wireframeLineWidth = wireframeLineWidth,
                                    showVertexNodes = showVertexNodes,
                                    showBoundingBox = showBoundingBox,
                                    showCoordinateGizmo = showCoordinateGizmo,
                                    showFloorGrid = showFloorGrid,
                                    paletteName = wireframePalette,
                                    shadingMode = shadingMode,
                                    lightAngle = lightAngle,
                                    selectedVolumeShapeId = selectedVolumeShapeId,
                                    explodeFactor = explodeFactor,
                                    textureBitmaps = textureBitmaps,
                                    onRotateDrag = { dx, dy -> viewModel.onRotateDrag(dx, dy) },
                                    onPanDrag = { dx, dy -> viewModel.onPanDrag(dx, dy) },
                                    onZoomChange = { dz -> viewModel.onZoomChange(dz) },
                                    onToggleAutoRotate = { viewModel.isAutoRotate.value = !isAutoRotate },
                                    onResetCamera = { viewModel.resetCamera("Isometric") }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. Inspection Modes Tab Bar
                            ModeSelectorBar(
                                currentMode = inspectionMode,
                                onSelectMode = { viewModel.setInspectionMode(it) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. Inspection Detail Content Panels
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                when (inspectionMode) {
                                    InspectionMode.WIREFRAME -> {
                                        WireframeViewer(
                                            bundle = currentBundle,
                                            lineWidth = wireframeLineWidth,
                                            showVertices = showVertexNodes,
                                            showBoundingBox = showBoundingBox,
                                            showFloorGrid = showFloorGrid,
                                            selectedPalette = wireframePalette,
                                            onLineWidthChange = { viewModel.wireframeLineWidth.value = it },
                                            onToggleVertices = { viewModel.showVertexNodes.value = !showVertexNodes },
                                            onToggleBoundingBox = { viewModel.showBoundingBox.value = !showBoundingBox },
                                            onToggleFloorGrid = { viewModel.showFloorGrid.value = !showFloorGrid },
                                            onPaletteSelect = { viewModel.wireframeColorPalette.value = it },
                                            onSelectCameraPreset = { viewModel.resetCamera(it) }
                                        )
                                    }
                                    InspectionMode.GLB_SHADED -> {
                                        ShadedGlbViewer(
                                            bundle = currentBundle,
                                            shadingMode = shadingMode,
                                            lightAngle = lightAngle,
                                            onShadingModeSelect = { viewModel.shadingMode.value = it },
                                            onLightAngleChange = { viewModel.lightAngleRad.value = it }
                                        )
                                    }
                                    InspectionMode.EROSION_SIM -> {
                                        ErosionNoiseInspectorView(
                                            bundle = currentBundle,
                                            erosionData = erosionData,
                                            isSimulating = isErosionRunning,
                                            triplanarEnabled = triplanarEnabled,
                                            onThermalAngleChange = { viewModel.updateThermalAngle(it) },
                                            onHydraulicDropletsChange = { viewModel.updateHydraulicDroplets(it) },
                                            onDomainWarpChange = { viewModel.updateDomainWarp(it) },
                                            onRidgedOctavesChange = { viewModel.updateRidgedOctaves(it) },
                                            onLodSelect = { viewModel.setLodLevel(it) },
                                            onToggleTriplanar = { viewModel.triplanarEnabled.value = !triplanarEnabled },
                                            onRunSimulation = { viewModel.triggerErosionSimulation() }
                                        )
                                    }
                                    InspectionMode.GRID_10X10 -> {
                                        val activeGridType by viewModel.activeGridType.collectAsStateWithLifecycle()
                                        val rockGridCatalog by viewModel.rockGridCatalog.collectAsStateWithLifecycle()
                                        val treeGridCatalog by viewModel.treeGridCatalog.collectAsStateWithLifecycle()
                                        val grassGridCatalog by viewModel.grassGridCatalog.collectAsStateWithLifecycle()
                                        val currentCatalog = when (activeGridType) {
                                            "TREES" -> treeGridCatalog
                                            "GRASS" -> grassGridCatalog
                                            else -> rockGridCatalog
                                        }
                                        val selectedCell by viewModel.selectedGridCell.collectAsStateWithLifecycle()
                                        val filterArchetype by viewModel.gridFilterArchetype.collectAsStateWithLifecycle()

                                        RockGridInspectorView(
                                            catalog = currentCatalog,
                                            selectedCell = selectedCell,
                                            filterArchetype = filterArchetype,
                                            gridType = activeGridType,
                                            onGridTypeChange = { viewModel.setGridType(it) },
                                            onCellSelect = { viewModel.selectGridCell(it) },
                                            onFilterChange = { viewModel.setGridFilterArchetype(it) },
                                            onFocusRockIn3D = { viewModel.focusRockArchetype(it) },
                                            onOpenScene3D = { viewModel.setMainAppView(MainAppView.SCENE_3D_VIEW) }
                                        )
                                    }
                                    InspectionMode.VOLUME_DIAGRAM -> {
                                        VolumeDiagramView(
                                            bundle = currentBundle,
                                            selectedVolumeShapeId = selectedVolumeShapeId,
                                            explodeFactor = explodeFactor,
                                            textureBitmaps = textureBitmaps,
                                            onSelectVolumeShape = { viewModel.selectVolumeShape(it) },
                                            onExplodeFactorChange = { viewModel.setExplodeFactor(it) }
                                        )
                                    }
                                    InspectionMode.MESH_DATA -> {
                                        MeshInspectorView(bundle = currentBundle)
                                    }
                                    InspectionMode.TEXTURES -> {
                                        TextureInspectorView(
                                            bundle = currentBundle,
                                            activeTextureKey = activeTextureKey,
                                            activeChannelMode = activeChannelMode,
                                            textureBitmaps = textureBitmaps,
                                            onSelectTextureKey = { viewModel.activeTextureKey.value = it },
                                            onSelectChannelMode = { viewModel.activeChannelMode.value = it }
                                        )
                                    }
                                    InspectionMode.JSON_SPECS -> {
                                        JsonSpecsView(
                                            bundle = currentBundle,
                                            activeJsonTab = activeJsonTab,
                                            searchQuery = jsonSearchQuery,
                                            onSelectJsonTab = { viewModel.activeJsonTab.value = it },
                                            onSearchQueryChange = { viewModel.jsonSearchQuery.value = it }
                                        )
                                    }
                                    InspectionMode.FOLDER_ARCH -> {
                                        FolderArchitectureView(
                                            folderTree = folderTree,
                                            previewNode = previewFileNode,
                                            previewContent = previewFileContent,
                                            onSelectNode = { viewModel.selectFileNode(it) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 5. Hero Banner / Showcase Card
                            ShowcaseBanner(bundle = currentBundle)

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                MainAppView.CPP_PROCEDURAL_VIEW -> {
                    CppProceduralScreen(
                        currentAssetType = selectedCppAsset,
                        mesh = cppMesh,
                        activeTab = cppActiveTab,
                        yaw = cppYaw,
                        pitch = cppPitch,
                        zoom = cppZoom,
                        panX = cppPanX,
                        panY = cppPanY,
                        isAutoRotate = cppIsAutoRotate,
                        showGrid = cppShowGrid,
                        shadingMode = cppShadingMode,
                        explodeFactor = cppExplodeFactor,
                        lightAngle = cppLightAngle,
                        bay1OpenPct = cppBay1OpenPct,
                        bay2OpenPct = cppBay2OpenPct,
                        neonSignOn = cppNeonSignOn,
                        headlightsOn = cppHeadlightsOn,
                        taillightsOn = cppTaillightsOn,
                        fovAngleDeg = cppFovAngleDeg,
                        isPerspective = cppIsPerspective,
                        renderingBackend = renderingBackend,
                        vulkanPolygonMode = vulkanPolygonMode,
                        vulkanMsaa = vulkanMsaa,
                        vulkanValidationLayers = vulkanValidationLayers,
                        vulkanDynamicRendering = vulkanDynamicRendering,
                        vulkanPipelineCache = vulkanPipelineCache,
                        vulkanPushConstantSag = vulkanPushConstantSag,
                        onRenderingBackendChange = { viewModel.setRenderingBackend(it) },
                        onToggleRenderingBackend = { viewModel.toggleRenderingBackend() },
                        onVulkanPolygonModeChange = { viewModel.setVulkanPolygonMode(it) },
                        onVulkanMsaaChange = { viewModel.setVulkanMsaa(it) },
                        onToggleVulkanValidationLayers = { viewModel.toggleVulkanValidationLayers() },
                        onToggleVulkanDynamicRendering = { viewModel.toggleVulkanDynamicRendering() },
                        onToggleVulkanPipelineCache = { viewModel.toggleVulkanPipelineCache() },
                        onToggleVulkanPushConstantSag = { viewModel.toggleVulkanPushConstantSag() },
                        postModularType = postModularType,
                        postHasTransformer = postHasTransformer,
                        postHasStreetlight = postHasStreetlight,
                        postStreetlightOn = postStreetlightOn,
                        postHasGuyWire = postHasGuyWire,
                        onPostModularTypeChange = { viewModel.setPostModularType(it) },
                        onTogglePostTransformer = { viewModel.togglePostTransformer() },
                        onTogglePostStreetlight = { viewModel.togglePostStreetlight() },
                        onTogglePostStreetlightOn = { viewModel.togglePostStreetlightOn() },
                        onTogglePostGuyWire = { viewModel.togglePostGuyWire() },
                        gridPostCount = gridPostCount,
                        gridCornerAngleDeg = gridCornerAngleDeg,
                        gridWireSagM = gridWireSagM,
                        gridEnergized = gridEnergized,
                        gridStreetlightsOn = gridStreetlightsOn,
                        onGridPostCountChange = { viewModel.setGridPostCount(it) },
                        onGridCornerAngleChange = { viewModel.setGridCornerAngle(it) },
                        onGridWireSagChange = { viewModel.setGridWireSag(it) },
                        onToggleGridEnergized = { viewModel.toggleGridEnergized() },
                        onToggleGridStreetlightsOn = { viewModel.toggleGridStreetlightsOn() },
                        cableGeometryMode = cableGeometryMode,
                        cableCylinderSides = cableCylinderSides,
                        cableConductorRadiusMm = cableConductorRadiusMm,
                        cableTensionKn = cableTensionKn,
                        cableLinearMassKgM = cableLinearMassKgM,
                        onCableGeometryModeChange = { viewModel.setCableGeometryMode(it) },
                        onCableCylinderSidesChange = { viewModel.setCableCylinderSides(it) },
                        onCableConductorRadiusMmChange = { viewModel.setCableConductorRadiusMm(it) },
                        onCableTensionKnChange = { viewModel.setCableTensionKn(it) },
                        onCableLinearMassKgMChange = { viewModel.setCableLinearMassKgM(it) },
                        onSelectAsset = { viewModel.selectCppAsset(it) },
                        onTabSelect = { viewModel.setCppActiveTab(it) },
                        onRotateDrag = { dx, dy -> viewModel.onCppRotateDrag(dx, dy) },
                        onPanDrag = { dx, dy -> viewModel.onCppPanDrag(dx, dy) },
                        onZoomChange = { dz -> viewModel.onCppZoomChange(dz) },
                        onToggleAutoRotate = { viewModel.cppIsAutoRotate.value = !cppIsAutoRotate },
                        onToggleGrid = { viewModel.cppShowGrid.value = !cppShowGrid },
                        onTogglePerspective = { viewModel.toggleCppPerspective() },
                        onFovChange = { viewModel.onCppFovChange(it) },
                        onResetCamera = { viewModel.resetCppCamera(it) },
                        onShadingModeSelect = { viewModel.cppShadingMode.value = it },
                        onExplodeFactorChange = { viewModel.cppExplodeFactor.value = it },
                        onLightAngleChange = { viewModel.cppLightAngle.value = it },
                        onBay1Change = { viewModel.setCppBay1OpenPct(it) },
                        onBay2Change = { viewModel.setCppBay2OpenPct(it) },
                        onNeonSignToggle = { viewModel.toggleCppNeonSign() },
                        onHeadlightsToggle = { viewModel.toggleCppHeadlights() },
                        onTaillightsToggle = { viewModel.toggleCppTaillights() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
}

@Composable
private fun AssetSelectorRow(
    catalog: List<AssetItem>,
    selectedAsset: AssetItem?,
    onSelect: (AssetItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(catalog) { asset ->
            val isSelected = asset.id == selectedAsset?.id
            val accentColor = Color(android.graphics.Color.parseColor(asset.primaryColorHex))

            Surface(
                color = if (isSelected) accentColor.copy(alpha = 0.2f) else DarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.8.dp else 1.dp,
                    color = if (isSelected) accentColor else DarkSurfaceHighlight
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(asset) }
                    .testTag("asset_card_${asset.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val iconVector = when {
                        asset.folder.contains("tree") -> Icons.Default.Park
                        asset.folder.contains("rock") -> Icons.Default.Terrain
                        asset.folder == "ship" -> Icons.Default.RocketLaunch
                        asset.folder == "drone" -> Icons.Default.FlightTakeoff
                        asset.folder == "rover" -> Icons.Default.DirectionsCar
                        asset.folder == "mech_core" -> Icons.Default.ElectricBolt
                        else -> Icons.Default.Landscape
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = asset.name,
                        tint = if (isSelected) accentColor else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = asset.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                        Text(
                            text = "${asset.category} • ${asset.folder}/",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) accentColor else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelectorBar(
    currentMode: InspectionMode,
    onSelectMode: (InspectionMode) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(InspectionMode.values()) { mode ->
            val isSelected = currentMode == mode
            val modeColor = when (mode) {
                InspectionMode.WIREFRAME -> CyberCyan
                InspectionMode.GLB_SHADED -> ElectricViolet
                InspectionMode.EROSION_SIM -> CyberCyan
                InspectionMode.GRID_10X10 -> NeonLime
                InspectionMode.VOLUME_DIAGRAM -> NeonLime
                InspectionMode.MESH_DATA -> CyberAccent
                InspectionMode.TEXTURES -> LaserOrange
                InspectionMode.JSON_SPECS -> ElectricViolet
                InspectionMode.FOLDER_ARCH -> HologramBlue
            }

            Surface(
                color = if (isSelected) modeColor.copy(alpha = 0.22f) else DarkSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) modeColor else DarkSurfaceHighlight
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(mode) }
                    .testTag("mode_tab_${mode.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val icon = when (mode) {
                        InspectionMode.WIREFRAME -> Icons.Default.Grid4x4
                        InspectionMode.GLB_SHADED -> Icons.Default.ViewInAr
                        InspectionMode.EROSION_SIM -> Icons.Default.WaterDrop
                        InspectionMode.GRID_10X10 -> Icons.Default.GridOn
                        InspectionMode.VOLUME_DIAGRAM -> Icons.Default.Category
                        InspectionMode.MESH_DATA -> Icons.Default.Polyline
                        InspectionMode.TEXTURES -> Icons.Default.Texture
                        InspectionMode.JSON_SPECS -> Icons.Default.DataObject
                        InspectionMode.FOLDER_ARCH -> Icons.Default.FolderSpecial
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.label,
                        tint = if (isSelected) modeColor else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mode.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseBanner(bundle: FullAssetBundle) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("showcase_hero_card")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_showcase_1791001198996),
                    contentDescription = "Futuristic 3D Hangar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DarkSurface)
                            )
                        )
                )
                Text(
                    text = "APEX 3D WORKFLOW ENGINE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = CyberCyan,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                if (bundle.item.folder in listOf("arrow_interceptor", "interceptor_x")) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AETHELRED SPACE INDUSTRIES // ARROW SCHEMATIC",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "SPEED: 1,200 Mhm/h",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = LaserOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "CREW SIZE: 3 • RANGE: 36 - 10,000 LY • HYPER-WAVE DRIVE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Modular Structure Overview for ${bundle.item.name}:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assets are partitioned into self-contained subdirectories under assets/${bundle.item.folder}/ allowing independent iteration on wireframe meshes, PBR texture maps, glTF binaries, and telemetry schemas.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun MainAppNavBar(
    currentView: MainAppView,
    onSelectView: (MainAppView) -> Unit
) {
    Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MainAppView.values()) { viewItem ->
                val isSelected = currentView == viewItem
                val itemColor = when (viewItem) {
                    MainAppView.MAGADI_TOWN_3X5 -> NeonLime
                    MainAppView.KENYA_GRID_VIEW -> CyberCyan
                    MainAppView.TERRAIN_VIEW -> CyberCyan
                    MainAppView.ROAD_VIEW -> NeonLime
                    MainAppView.AUDIO_VIEW -> HologramBlue
                    MainAppView.SCENE_3D_VIEW -> LaserOrange
                    MainAppView.CPP_PROCEDURAL_VIEW -> NeonLime
                    MainAppView.SINGLE_ASSET_INSPECTOR -> ElectricViolet
                }
                Surface(
                    color = if (isSelected) itemColor.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) itemColor else DarkSurfaceHighlight
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectView(viewItem) }
                        .testTag("nav_btn_${viewItem.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(viewItem.emoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewItem.label.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) itemColor else TextPrimary,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
