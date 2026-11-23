package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MagadiExtractionUiState
import com.example.model.TerrainRenderMode
import com.example.ui.theme.*
import com.example.viewmodel.MagadiSubView
import com.example.viewmodel.MagadiTownViewModel
import kotlin.math.roundToInt

@Composable
fun MagadiTownScreen(
    modifier: Modifier = Modifier,
    magadiViewModel: MagadiTownViewModel = viewModel(),
    isFullScreenMap: Boolean = false,
    onToggleFullScreenMap: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val bounds by magadiViewModel.bounds.collectAsStateWithLifecycle()
    val extractionState by magadiViewModel.extractionState.collectAsStateWithLifecycle()
    val activeSubView by magadiViewModel.activeSubView.collectAsStateWithLifecycle()

    val viewCenterXM by magadiViewModel.viewCenterXM.collectAsStateWithLifecycle()
    val viewCenterYM by magadiViewModel.viewCenterYM.collectAsStateWithLifecycle()
    val viewWidthM by magadiViewModel.viewWidthM.collectAsStateWithLifecycle()
    val rasterOverlay by magadiViewModel.rasterOverlay.collectAsStateWithLifecycle()
    val inspectedCell by magadiViewModel.inspectedCell.collectAsStateWithLifecycle()
    val selectedBuilding by magadiViewModel.selectedBuilding.collectAsStateWithLifecycle()
    val selectedRoad by magadiViewModel.selectedRoad.collectAsStateWithLifecycle()

    val show1mGrid by magadiViewModel.show1mGrid.collectAsStateWithLifecycle()
    val showElevationRaster by magadiViewModel.showElevationRaster.collectAsStateWithLifecycle()
    val showWater by magadiViewModel.showWater.collectAsStateWithLifecycle()
    val showContours by magadiViewModel.showContours.collectAsStateWithLifecycle()
    val showRoads by magadiViewModel.showRoads.collectAsStateWithLifecycle()
    val showRoadSymbols by magadiViewModel.showRoadSymbols.collectAsStateWithLifecycle()
    val showBuildings by magadiViewModel.showBuildings.collectAsStateWithLifecycle()
    val showPois by magadiViewModel.showPois.collectAsStateWithLifecycle()

    val lakeLevelAslM by magadiViewModel.lakeLevelAslM.collectAsStateWithLifecycle()
    val verticalExaggeration by magadiViewModel.verticalExaggeration.collectAsStateWithLifecycle()
    val groundDatumAslM by magadiViewModel.groundDatumAslM.collectAsStateWithLifecycle()
    val sceneUnitsPerMeter3D by magadiViewModel.sceneUnitsPerMeter3D.collectAsStateWithLifecycle()

    val terrain3dMode by magadiViewModel.terrain3dMode.collectAsStateWithLifecycle()
    val terrainRenderMode by magadiViewModel.terrainRenderMode.collectAsStateWithLifecycle()
    val is3dWireframe by magadiViewModel.is3dWireframe.collectAsStateWithLifecycle()
    val show3dContours by magadiViewModel.show3dContours.collectAsStateWithLifecycle()
    val yaw3d by magadiViewModel.yaw3d.collectAsStateWithLifecycle()
    val pitch3d by magadiViewModel.pitch3d.collectAsStateWithLifecycle()
    val zoom3d by magadiViewModel.zoom3d.collectAsStateWithLifecycle()
    val panX3d by magadiViewModel.panX3d.collectAsStateWithLifecycle()
    val panY3d by magadiViewModel.panY3d.collectAsStateWithLifecycle()
    val lightAngle3d by magadiViewModel.lightAngle3d.collectAsStateWithLifecycle()
    val mesh3d by magadiViewModel.terrainMesh3D.collectAsStateWithLifecycle()
    val buildings3d by magadiViewModel.overlayBuildings3D.collectAsStateWithLifecycle()
    val roads3d by magadiViewModel.overlayRoads3D.collectAsStateWithLifecycle()
    val symbols3d by magadiViewModel.overlayRoadSymbols3D.collectAsStateWithLifecycle()
    val bridges3d by magadiViewModel.overlayBridges3D.collectAsStateWithLifecycle()

    val dataTabIndex by magadiViewModel.dataTabIndex.collectAsStateWithLifecycle()
    val searchQuery by magadiViewModel.searchQuery.collectAsStateWithLifecycle()

    var localFullScreen by remember { mutableStateOf(false) }
    val fullScreen = if (onToggleFullScreenMap != null) isFullScreenMap else localFullScreen
    val setFullScreen: (Boolean) -> Unit = { v ->
        if (onToggleFullScreenMap != null) onToggleFullScreenMap(v) else localFullScreen = v
    }
    val effectiveFullScreen = fullScreen && activeSubView != MagadiSubView.DATA_COORDS
    var showEnvSliders by remember { mutableStateOf(false) }

    BackHandler(enabled = effectiveFullScreen) {
        setFullScreen(false)
    }

    val bundle = magadiViewModel.activeBundleOrNull()
    val demResM = bundle?.metadata?.demResolutionM ?: 9.5f
    val demSourceLabel = when {
        demResM <= 15f -> "Terrarium ~9.5m DEM"
        demResM <= 40f -> "SRTM 30m DEM"
        else -> "DEM ${demResM.roundToInt()}m"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AnimatedVisibility(visible = !effectiveFullScreen) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Compact Single-Row Status Banner
                MagadiStatusHeader(
                    extractionState = extractionState,
                    roadCount = bundle?.roads?.size ?: 0,
                    buildingCount = bundle?.buildings?.size ?: 0,
                    waterCount = bundle?.waterPolygons?.size ?: 0,
                    lakeLevelAslM = lakeLevelAslM,
                    showEnvSliders = showEnvSliders,
                    canHideText = activeSubView != MagadiSubView.DATA_COORDS,
                    onReloadBinary = { magadiViewModel.reloadBinaryPack() },
                    onToggleEnvSliders = { showEnvSliders = !showEnvSliders },
                    onEnterFullScreen = { setFullScreen(true) }
                )

                // 2. Mode Switcher Row: 2D 1M MAP | SPLIT 2D+3D | 3D TERRAIN | DATA & COORDS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MagadiSubView.values().forEach { sub ->
                        val isSelected = activeSubView == sub
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else DarkSurface,
                            border = BorderStroke(1.dp, if (isSelected) CyberCyan else DarkSurfaceHighlight),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { magadiViewModel.setSubView(sub) }
                                .testTag("magadi_subview_${sub.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = sub.label,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CyberCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Main Viewport Area
        when (activeSubView) {
            MagadiSubView.MAP_2D -> {
                AnimatedVisibility(visible = !effectiveFullScreen) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MagadiMapToolbar(
                            viewWidthM = viewWidthM,
                            show1mGrid = show1mGrid,
                            showElevationRaster = showElevationRaster,
                            showWater = showWater,
                            showContours = showContours,
                            showRoads = showRoads,
                            showRoadSymbols = showRoadSymbols,
                            showBuildings = showBuildings,
                            showPois = showPois,
                            showEnvSliders = showEnvSliders,
                            onZoomPreset = { magadiViewModel.setZoomPreset(it) },
                            onToggleGrid = { magadiViewModel.show1mGrid.value = !show1mGrid },
                            onToggleRaster = { magadiViewModel.showElevationRaster.value = !showElevationRaster },
                            onToggleWater = { magadiViewModel.showWater.value = !showWater },
                            onToggleContours = { magadiViewModel.showContours.value = !showContours },
                            onToggleRoads = { magadiViewModel.showRoads.value = !showRoads },
                            onToggleRoadSymbols = { magadiViewModel.showRoadSymbols.value = !showRoadSymbols },
                            onToggleBuildings = { magadiViewModel.showBuildings.value = !showBuildings },
                            onTogglePois = { magadiViewModel.showPois.value = !showPois },
                            onToggleEnvSliders = { showEnvSliders = !showEnvSliders }
                        )

                        AnimatedVisibility(visible = showEnvSliders) {
                            MagadiLakeAndDepthBar(
                                lakeLevelAslM = lakeLevelAslM,
                                verticalExaggeration = verticalExaggeration,
                                onLakeLevelChange = { magadiViewModel.setLakeLevel(it) },
                                onVerticalExagChange = { magadiViewModel.setVerticalExaggeration(it) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (effectiveFullScreen) 0.dp else 6.dp,
                            vertical = if (effectiveFullScreen) 0.dp else 2.dp
                        )
                ) {
                    MagadiTownMap2D(
                        bundle = bundle,
                        raster = rasterOverlay,
                        viewCenterXM = viewCenterXM,
                        viewCenterYM = viewCenterYM,
                        viewWidthM = viewWidthM,
                        inspectedCell = inspectedCell,
                        selectedBuilding = selectedBuilding,
                        selectedRoad = selectedRoad,
                        show1mGrid = show1mGrid,
                        showElevationRaster = showElevationRaster,
                        showWater = showWater,
                        showContours = showContours,
                        showRoads = showRoads,
                        showRoadSymbols = showRoadSymbols,
                        showBuildings = showBuildings,
                        showPois = showPois,
                        lakeLevelAslM = lakeLevelAslM,
                        demSourceLabel = demSourceLabel,
                        hideAllText = effectiveFullScreen,
                        onToggleHideAllText = { setFullScreen(!fullScreen) },
                        onPan = { dx, dy, w -> magadiViewModel.pan2D(dx, dy, w) },
                        onZoom = { f -> magadiViewModel.zoom2D(f) },
                        onTapInspect = { x, y, w, h -> magadiViewModel.inspectPointAtScreen(x, y, w, h) },
                        onResetView = { magadiViewModel.setZoomPreset(3000f) },
                        onSizeChanged = { w, h -> magadiViewModel.updateCanvasAspect(w, h) }
                    )
                }

                AnimatedVisibility(visible = !effectiveFullScreen) {
                    MagadiCellTelemetryCard(
                        inspectedCell = inspectedCell,
                        lakeLevelAslM = lakeLevelAslM,
                        selectedBuilding = selectedBuilding,
                        selectedRoad = selectedRoad,
                        onOpen3d1m = { magadiViewModel.viewInspectedCellIn3d1m() }
                    )
                }
            }

            MagadiSubView.SPLIT_VIEW -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (effectiveFullScreen) 0.dp else 6.dp,
                            vertical = if (effectiveFullScreen) 0.dp else 2.dp
                        )
                ) {
                    MagadiTownMap2D(
                        bundle = bundle,
                        raster = rasterOverlay,
                        viewCenterXM = viewCenterXM,
                        viewCenterYM = viewCenterYM,
                        viewWidthM = viewWidthM,
                        inspectedCell = inspectedCell,
                        selectedBuilding = selectedBuilding,
                        selectedRoad = selectedRoad,
                        show1mGrid = show1mGrid,
                        showElevationRaster = showElevationRaster,
                        showWater = showWater,
                        showContours = showContours,
                        showRoads = showRoads,
                        showRoadSymbols = showRoadSymbols,
                        showBuildings = showBuildings,
                        showPois = showPois,
                        lakeLevelAslM = lakeLevelAslM,
                        demSourceLabel = demSourceLabel,
                        hideAllText = effectiveFullScreen,
                        onToggleHideAllText = { setFullScreen(!fullScreen) },
                        onPan = { dx, dy, w -> magadiViewModel.pan2D(dx, dy, w) },
                        onZoom = { f -> magadiViewModel.zoom2D(f) },
                        onTapInspect = { x, y, w, h -> magadiViewModel.inspectPointAtScreen(x, y, w, h) },
                        onResetView = { magadiViewModel.setZoomPreset(3000f) },
                        onSizeChanged = { w, h -> magadiViewModel.updateCanvasAspect(w, h) }
                    )
                }

                AnimatedVisibility(visible = !effectiveFullScreen) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Magadi3dControlsBar(
                            terrain3dMode = terrain3dMode,
                            terrainRenderMode = terrainRenderMode,
                            is3dWireframe = is3dWireframe,
                            showWater = showWater,
                            onSelect3dMode = { magadiViewModel.set3dMode(it) },
                            onSelectRenderMode = { magadiViewModel.terrainRenderMode.value = it },
                            onToggleWireframe = { magadiViewModel.is3dWireframe.value = !is3dWireframe },
                            onToggleWater = { magadiViewModel.showWater.value = !showWater },
                            onResetCamera = { magadiViewModel.reset3dCamera() }
                        )

                        AnimatedVisibility(visible = showEnvSliders) {
                            MagadiLakeAndDepthBar(
                                lakeLevelAslM = lakeLevelAslM,
                                verticalExaggeration = verticalExaggeration,
                                onLakeLevelChange = { magadiViewModel.setLakeLevel(it) },
                                onVerticalExagChange = { magadiViewModel.setVerticalExaggeration(it) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (effectiveFullScreen) 0.dp else 6.dp,
                            vertical = if (effectiveFullScreen) 0.dp else 2.dp
                        )
                ) {
                    Terrain3DCanvas(
                        mesh = mesh3d,
                        renderMode = terrainRenderMode,
                        yaw = yaw3d,
                        pitch = pitch3d,
                        zoom = zoom3d,
                        panX = panX3d,
                        panY = panY3d,
                        lightAngleRad = lightAngle3d,
                        isWireframeOverlay = is3dWireframe,
                        showGridContours = show3dContours,
                        contourInterval = if (terrain3dMode == "WINDOW_1M") 2f else 10f,
                        is1mResolution = (terrain3dMode == "WINDOW_1M"),
                        showGroundPiece = true,
                        groundPieceDatumASL = groundDatumAslM,
                        groundPieceUnitsPerMeter = sceneUnitsPerMeter3D,
                        lakeLevelAslM = if (showWater) lakeLevelAslM else null,
                        overlayBuildings3D = buildings3d,
                        overlayRoadSegments3D = roads3d,
                        overlayRoadSymbols3D = symbols3d,
                        overlayBridges3D = bridges3d,
                        onRotateDrag = { dx, dy ->
                            magadiViewModel.yaw3d.value += dx * 0.007f
                            magadiViewModel.pitch3d.value = (magadiViewModel.pitch3d.value - dy * 0.007f).coerceIn(0.06f, 1.46f)
                        },
                        onPanDrag = { dx, dy ->
                            magadiViewModel.panX3d.value += dx
                            magadiViewModel.panY3d.value += dy
                        },
                        onZoomChange = { f ->
                            magadiViewModel.zoom3d.value = (magadiViewModel.zoom3d.value * f).coerceIn(0.15f, 35f)
                        }
                    )
                }
            }

            MagadiSubView.TERRAIN_3D -> {
                AnimatedVisibility(visible = !effectiveFullScreen) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Magadi3dControlsBar(
                            terrain3dMode = terrain3dMode,
                            terrainRenderMode = terrainRenderMode,
                            is3dWireframe = is3dWireframe,
                            showWater = showWater,
                            onSelect3dMode = { magadiViewModel.set3dMode(it) },
                            onSelectRenderMode = { magadiViewModel.terrainRenderMode.value = it },
                            onToggleWireframe = { magadiViewModel.is3dWireframe.value = !is3dWireframe },
                            onToggleWater = { magadiViewModel.showWater.value = !showWater },
                            onResetCamera = { magadiViewModel.reset3dCamera() }
                        )

                        AnimatedVisibility(visible = showEnvSliders) {
                            MagadiLakeAndDepthBar(
                                lakeLevelAslM = lakeLevelAslM,
                                verticalExaggeration = verticalExaggeration,
                                onLakeLevelChange = { magadiViewModel.setLakeLevel(it) },
                                onVerticalExagChange = { magadiViewModel.setVerticalExaggeration(it) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (effectiveFullScreen) 0.dp else 6.dp,
                            vertical = if (effectiveFullScreen) 0.dp else 2.dp
                        )
                ) {
                    Terrain3DCanvas(
                        mesh = mesh3d,
                        renderMode = terrainRenderMode,
                        yaw = yaw3d,
                        pitch = pitch3d,
                        zoom = zoom3d,
                        panX = panX3d,
                        panY = panY3d,
                        lightAngleRad = lightAngle3d,
                        isWireframeOverlay = is3dWireframe,
                        showGridContours = show3dContours,
                        contourInterval = if (terrain3dMode == "WINDOW_1M") 2f else 10f,
                        is1mResolution = (terrain3dMode == "WINDOW_1M"),
                        showGroundPiece = true,
                        groundPieceDatumASL = groundDatumAslM,
                        groundPieceUnitsPerMeter = sceneUnitsPerMeter3D,
                        lakeLevelAslM = if (showWater) lakeLevelAslM else null,
                        overlayBuildings3D = buildings3d,
                        overlayRoadSegments3D = roads3d,
                        overlayRoadSymbols3D = symbols3d,
                        overlayBridges3D = bridges3d,
                        onRotateDrag = { dx, dy ->
                            magadiViewModel.yaw3d.value += dx * 0.007f
                            magadiViewModel.pitch3d.value = (magadiViewModel.pitch3d.value - dy * 0.007f).coerceIn(0.06f, 1.46f)
                        },
                        onPanDrag = { dx, dy ->
                            magadiViewModel.panX3d.value += dx
                            magadiViewModel.panY3d.value += dy
                        },
                        onZoomChange = { f ->
                            magadiViewModel.zoom3d.value = (magadiViewModel.zoom3d.value * f).coerceIn(0.15f, 35f)
                        }
                    )

                    if (effectiveFullScreen) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, NeonLime),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(10.dp)
                        ) {
                            IconButton(
                                onClick = { setFullScreen(false) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("magadi_3d_restore_text_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Show Text & Controls",
                                    tint = NeonLime
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = !effectiveFullScreen) {
                    MagadiCellTelemetryCard(
                        inspectedCell = inspectedCell,
                        lakeLevelAslM = lakeLevelAslM,
                        selectedBuilding = selectedBuilding,
                        selectedRoad = selectedRoad,
                        onOpen3d1m = { magadiViewModel.viewInspectedCellIn3d1m() }
                    )
                }
            }

            MagadiSubView.DATA_COORDS -> {
                MagadiTownDataPanel(
                    bounds = bounds,
                    bundle = bundle,
                    activeTab = dataTabIndex,
                    searchQuery = searchQuery,
                    onSelectTab = { magadiViewModel.dataTabIndex.value = it },
                    onSearchChange = { magadiViewModel.searchQuery.value = it },
                    onSelectRoad = { magadiViewModel.selectRoad(it) },
                    onSelectBuildingMap = { magadiViewModel.selectBuilding(it, openIn3d = false) },
                    onSelectBuilding3d = { magadiViewModel.selectBuilding(it, openIn3d = true) },
                    onSelectPoi = { poi ->
                        magadiViewModel.focusOnCoordinate(poi.coord.xM, poi.coord.yM, targetWidthM = 120f)
                    },
                    onExportGeoJson = { magadiViewModel.exportAndShareGeoJson(context) },
                    onExportCsv = { magadiViewModel.exportAndShareBuildingsCsv(context) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MagadiStatusHeader(
    extractionState: MagadiExtractionUiState,
    roadCount: Int,
    buildingCount: Int,
    waterCount: Int,
    lakeLevelAslM: Float,
    showEnvSliders: Boolean,
    canHideText: Boolean,
    onReloadBinary: () -> Unit,
    onToggleEnvSliders: () -> Unit,
    onEnterFullScreen: () -> Unit
) {
    Surface(
        color = DarkSurface,
        border = BorderStroke(1.dp, GlowingCyanBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusLine = when (extractionState) {
                    is MagadiExtractionUiState.Extracting -> extractionState.stage
                    is MagadiExtractionUiState.Ready -> {
                        "R:$roadCount • B:$buildingCount • W:$waterCount • ${"%.1f".format(lakeLevelAslM)}m"
                    }
                    is MagadiExtractionUiState.Error -> "ERROR: ${extractionState.message}"
                    MagadiExtractionUiState.Idle -> "Loading 3×5km binary GIS pack..."
                }
                Text(
                    text = statusLine,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (extractionState is MagadiExtractionUiState.Error) LaserOrange else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )

                IconButton(
                    onClick = onReloadBinary,
                    enabled = extractionState !is MagadiExtractionUiState.Extracting,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("magadi_extract_live_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload Binary GIS Pack", tint = NeonLime)
                }

                IconButton(
                    onClick = onToggleEnvSliders,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("magadi_toggle_sliders_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Toggle Lake & Terrain Sliders",
                        tint = if (showEnvSliders) CyberCyan else TextSecondary
                    )
                }

                if (canHideText) {
                    IconButton(
                        onClick = onEnterFullScreen,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("magadi_header_hide_text_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hide All Text for Fullscreen Map",
                            tint = NeonLime
                        )
                    }
                }
            }

            if (extractionState is MagadiExtractionUiState.Extracting) {
                LinearProgressIndicator(
                    progress = { extractionState.progress },
                    color = CyberCyan,
                    trackColor = DarkSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MagadiLakeAndDepthBar(
    lakeLevelAslM: Float,
    verticalExaggeration: Float,
    onLakeLevelChange: (Float) -> Unit,
    onVerticalExagChange: (Float) -> Unit
) {
    Surface(
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceHighlight),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LAKE LEVEL: ${"%.1f".format(lakeLevelAslM)}m ASL",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
                Slider(
                    value = lakeLevelAslM.coerceIn(594f, 620f),
                    onValueChange = onLakeLevelChange,
                    valueRange = 594f..620f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("magadi_lake_level_slider")
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TERRAIN DEPTH: ${"%.1f".format(verticalExaggeration)}×",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LaserOrange
                )
                Slider(
                    value = verticalExaggeration.coerceIn(1.0f, 4.5f),
                    onValueChange = onVerticalExagChange,
                    valueRange = 1.0f..4.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = LaserOrange,
                        activeTrackColor = LaserOrange,
                        inactiveTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("magadi_terrain_depth_slider")
                )
            }
        }
    }
}

@Composable
private fun MagadiMapToolbar(
    viewWidthM: Float,
    show1mGrid: Boolean,
    showElevationRaster: Boolean,
    showWater: Boolean,
    showContours: Boolean,
    showRoads: Boolean,
    showRoadSymbols: Boolean,
    showBuildings: Boolean,
    showPois: Boolean,
    showEnvSliders: Boolean,
    onZoomPreset: (Float) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleRaster: () -> Unit,
    onToggleWater: () -> Unit,
    onToggleContours: () -> Unit,
    onToggleRoads: () -> Unit,
    onToggleRoadSymbols: () -> Unit,
    onToggleBuildings: () -> Unit,
    onTogglePois: () -> Unit,
    onToggleEnvSliders: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                3000f to "3×5km",
                1000f to "1km",
                250f to "250m",
                60f to "60m",
                20f to "1m GRID"
            )
            presets.forEach { (wM, lbl) ->
                val active = kotlin.math.abs(viewWidthM - wM) < wM * 0.28f
                ToggleChipPill(
                    label = lbl,
                    selected = active,
                    activeColor = NeonLime,
                    testTag = "magadi_zoom_preset_${lbl.lowercase().replace(" ", "_")}",
                    onClick = { onZoomPreset(wM) }
                )
            }
            ToggleChipPill("Lake/Depth", showEnvSliders, CyberCyan, "magadi_layer_env_sliders", onToggleEnvSliders)
            ToggleChipPill("Water", showWater, CyberCyan, "magadi_layer_water", onToggleWater)
            ToggleChipPill("1m Grid", show1mGrid, CyberCyan, "magadi_layer_grid", onToggleGrid)
            ToggleChipPill("DEM", showElevationRaster, CyberCyan, "magadi_layer_dem", onToggleRaster)
            ToggleChipPill("Contours", showContours, CyberCyan, "magadi_layer_contours", onToggleContours)
            ToggleChipPill("Roads", showRoads, LaserOrange, "magadi_layer_roads", onToggleRoads)
            ToggleChipPill("Symbols", showRoadSymbols, Color(0xFFFFD54F), "magadi_layer_symbols", onToggleRoadSymbols)
            ToggleChipPill("Bldgs", showBuildings, LaserOrange, "magadi_layer_buildings", onToggleBuildings)
            ToggleChipPill("POIs", showPois, NeonLime, "magadi_layer_pois", onTogglePois)
        }
    }
}

@Composable
private fun Magadi3dControlsBar(
    terrain3dMode: String,
    terrainRenderMode: TerrainRenderMode,
    is3dWireframe: Boolean,
    showWater: Boolean,
    onSelect3dMode: (String) -> Unit,
    onSelectRenderMode: (TerrainRenderMode) -> Unit,
    onToggleWireframe: () -> Unit,
    onToggleWater: () -> Unit,
    onResetCamera: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToggleChipPill(
            label = "3×5KM BLOCK",
            selected = terrain3dMode == "OVERVIEW_3X5KM",
            activeColor = CyberCyan,
            testTag = "magadi_3d_overview_btn",
            onClick = { onSelect3dMode("OVERVIEW_3X5KM") }
        )
        ToggleChipPill(
            label = "1M WINDOW (64×64m)",
            selected = terrain3dMode == "WINDOW_1M",
            activeColor = LaserOrange,
            testTag = "magadi_3d_1m_window_btn",
            onClick = { onSelect3dMode("WINDOW_1M") }
        )
        ToggleChipPill(
            label = "LAKE WATER",
            selected = showWater,
            activeColor = CyberCyan,
            testTag = "magadi_3d_water_btn",
            onClick = onToggleWater
        )
        ToggleChipPill(
            label = "WIREFRAME",
            selected = is3dWireframe,
            activeColor = NeonLime,
            testTag = "magadi_3d_wireframe_btn",
            onClick = onToggleWireframe
        )
        listOf(
            TerrainRenderMode.HYPSOMETRIC_CONTOURS to "Hypsometric",
            TerrainRenderMode.SHADED_TRIPLANAR to "Biome PBR",
            TerrainRenderMode.SLOPE_ANALYSIS to "Slope",
            TerrainRenderMode.WIREFRAME_CDLOD to "CDLOD"
        ).forEach { (rm, title) ->
            ToggleChipPill(
                label = title,
                selected = terrainRenderMode == rm,
                activeColor = HologramBlue,
                testTag = "magadi_3d_mode_${rm.name.lowercase()}",
                onClick = { onSelectRenderMode(rm) }
            )
        }
        ToggleChipPill(
            label = "RESET CAM",
            selected = false,
            activeColor = CyberCyan,
            testTag = "magadi_3d_reset_cam_btn",
            onClick = onResetCamera
        )
    }
}

@Composable
private fun MagadiCellTelemetryCard(
    inspectedCell: com.example.model.Inspected1mCell,
    lakeLevelAslM: Float,
    selectedBuilding: com.example.model.ExtractedBuilding?,
    selectedRoad: com.example.model.ExtractedRoadWay?,
    onOpen3d1m: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, if (inspectedCell.isWater) CyberCyan else GlowingCyanBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("magadi_cell_telemetry_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val primaryLine = if (inspectedCell.isWater) {
                    "CELL [${inspectedCell.cellCol},${inspectedCell.cellRow}] • BED ${"%.2f".format(inspectedCell.elevM)}m (-${"%.2f".format(inspectedCell.waterDepthM)}m)"
                } else {
                    "CELL [${inspectedCell.cellCol},${inspectedCell.cellRow}] • ${"%.1f".format(inspectedCell.elevM)}m (+${"%.1f".format(inspectedCell.elevM - lakeLevelAslM)}m) • ${"%.1f".format(inspectedCell.slopeDeg)}°"
                }
                Text(
                    text = primaryLine,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (inspectedCell.isWater) CyberCyan else NeonLime,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val secondLine = when {
                    selectedBuilding != null -> {
                        val b = selectedBuilding
                        val comp = if (b.parentCompoundName.isNotBlank()) " [${b.parentCompoundName.take(18)}]" else ""
                        "${b.name}$comp • ${b.shapeType} • ${b.levels}F (${"%.1f".format(b.heightM)}m) • ${b.roofType}/${b.wallType}"
                    }
                    selectedRoad != null -> "ROAD: ${selectedRoad.name} (${selectedRoad.highwayType})"
                    else -> "${"%.5f".format(inspectedCell.lat)}, ${"%.5f".format(inspectedCell.lon)} • ${inspectedCell.utmEast.roundToInt()}E, ${inspectedCell.utmNorth.roundToInt()}N"
                }
                Text(
                    text = secondLine,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (selectedBuilding != null || selectedRoad != null) LaserOrange else CyberCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedBuilding != null && (selectedBuilding.hasFence || selectedBuilding.hasGate)) {
                    val gc = selectedBuilding.gateCoord
                    val gTxt = if (gc != null) " @ ${"%.5f".format(gc.lat)}, ${"%.5f".format(gc.lon)}" else ""
                    Text(
                        text = "FENCE: ${selectedBuilding.fenceType} (${"%.1f".format(selectedBuilding.fenceHeightM)}m) • GATE: ${selectedBuilding.gateType}$gTxt",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = onOpen3d1m,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.22f)),
                border = BorderStroke(1.dp, CyberCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("magadi_view_in_3d_1m_btn")
            ) {
                Text("3D 1M", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }
    }
}

@Composable
private fun ToggleChipPill(
    label: String,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeColor.copy(alpha = 0.22f) else DarkSurface,
        border = BorderStroke(1.dp, if (selected) activeColor else DarkSurfaceHighlight),
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) activeColor else TextSecondary
            )
        }
    }
}
