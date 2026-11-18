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
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.C58CorridorViewModel
import com.example.viewmodel.C58SubView
import kotlin.math.roundToInt

@Composable
fun C58CorridorScreen(
    modifier: Modifier = Modifier,
    c58ViewModel: C58CorridorViewModel = viewModel(),
    isFullScreenMap: Boolean = false,
    onToggleFullScreenMap: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val extractionState by c58ViewModel.extractionState.collectAsStateWithLifecycle()
    val selectedSector by c58ViewModel.selectedSector.collectAsStateWithLifecycle()
    val activeSubView by c58ViewModel.activeSubView.collectAsStateWithLifecycle()

    val viewCenterUtmE by c58ViewModel.viewCenterUtmE.collectAsStateWithLifecycle()
    val viewCenterUtmN by c58ViewModel.viewCenterUtmN.collectAsStateWithLifecycle()
    val viewWidthM by c58ViewModel.viewWidthM.collectAsStateWithLifecycle()
    val rasterOverlay by c58ViewModel.rasterOverlay.collectAsStateWithLifecycle()
    val inspectedCell by c58ViewModel.inspectedCell.collectAsStateWithLifecycle()
    val selectedHouse by c58ViewModel.selectedHouse.collectAsStateWithLifecycle()
    val selectedRoad by c58ViewModel.selectedRoad.collectAsStateWithLifecycle()

    val show1mGrid by c58ViewModel.show1mGrid.collectAsStateWithLifecycle()
    val showElevationRaster by c58ViewModel.showElevationRaster.collectAsStateWithLifecycle()
    val showWater by c58ViewModel.showWater.collectAsStateWithLifecycle()
    val showCorridorBounds by c58ViewModel.showCorridorBounds.collectAsStateWithLifecycle()
    val showContours by c58ViewModel.showContours.collectAsStateWithLifecycle()
    val showRoads by c58ViewModel.showRoads.collectAsStateWithLifecycle()
    val showRoadSymbols by c58ViewModel.showRoadSymbols.collectAsStateWithLifecycle()
    val showHouses by c58ViewModel.showHouses.collectAsStateWithLifecycle()
    val showPois by c58ViewModel.showPois.collectAsStateWithLifecycle()

    val lakeLevelAslM by c58ViewModel.lakeLevelAslM.collectAsStateWithLifecycle()
    val verticalExaggeration by c58ViewModel.verticalExaggeration.collectAsStateWithLifecycle()
    val groundDatumAslM by c58ViewModel.groundDatumAslM.collectAsStateWithLifecycle()
    val sceneUnitsPerMeter3D by c58ViewModel.sceneUnitsPerMeter3D.collectAsStateWithLifecycle()

    val terrain3dMode by c58ViewModel.terrain3dMode.collectAsStateWithLifecycle()
    val terrainRenderMode by c58ViewModel.terrainRenderMode.collectAsStateWithLifecycle()
    val is3dWireframe by c58ViewModel.is3dWireframe.collectAsStateWithLifecycle()
    val show3dContours by c58ViewModel.show3dContours.collectAsStateWithLifecycle()
    val yaw3d by c58ViewModel.yaw3d.collectAsStateWithLifecycle()
    val pitch3d by c58ViewModel.pitch3d.collectAsStateWithLifecycle()
    val zoom3d by c58ViewModel.zoom3d.collectAsStateWithLifecycle()
    val panX3d by c58ViewModel.panX3d.collectAsStateWithLifecycle()
    val panY3d by c58ViewModel.panY3d.collectAsStateWithLifecycle()
    val lightAngle3d by c58ViewModel.lightAngle3d.collectAsStateWithLifecycle()
    val mesh3d by c58ViewModel.terrainMesh3D.collectAsStateWithLifecycle()
    val houses3d by c58ViewModel.overlayHouses3D.collectAsStateWithLifecycle()
    val roads3d by c58ViewModel.overlayRoads3D.collectAsStateWithLifecycle()
    val symbols3d by c58ViewModel.overlayRoadSymbols3D.collectAsStateWithLifecycle()

    val dataTabIndex by c58ViewModel.dataTabIndex.collectAsStateWithLifecycle()
    val searchQuery by c58ViewModel.searchQuery.collectAsStateWithLifecycle()
    val sideFilter by c58ViewModel.sideFilter.collectAsStateWithLifecycle()

    var localFullScreen by remember { mutableStateOf(false) }
    val fullScreen = if (onToggleFullScreenMap != null) isFullScreenMap else localFullScreen
    val setFullScreen: (Boolean) -> Unit = { v ->
        if (onToggleFullScreenMap != null) onToggleFullScreenMap(v) else localFullScreen = v
    }
    val effectiveFullScreen = fullScreen && activeSubView != C58SubView.DATA_COORDS
    var showEnvSliders by remember { mutableStateOf(false) }

    BackHandler(enabled = effectiveFullScreen) {
        setFullScreen(false)
    }

    val bundle = c58ViewModel.activeBundle()
    val demResM = bundle.metadata.demResolutionM
    val demSourceLabel = when {
        demResM <= 24f -> "Terrarium ~19m DEM"
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
                // 1. Compact Single-Row Status & Quick Actions
                C58StatusHeader(
                    extractionState = extractionState,
                    selectedSector = selectedSector,
                    bundle = bundle,
                    showEnvSliders = showEnvSliders,
                    canHideText = activeSubView != C58SubView.DATA_COORDS,
                    onFitSector = { c58ViewModel.fitSectorCamera() },
                    onReloadBinary = { c58ViewModel.reloadBinaryPack() },
                    onToggleEnvSliders = { showEnvSliders = !showEnvSliders },
                    onEnterFullScreen = { setFullScreen(true) }
                )

                // 2. Sector Selector Strip (ALL C58 or S1..S10)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val syncedSectors = bundle.metadata.extractedSectors.toSet()
                    C58SectorChip(
                        label = "ALL (${bundle.summary.totalLengthKm.roundToInt()}km)",
                        selected = selectedSector == 0,
                        isSynced = syncedSectors.size == 10,
                        activeColor = CyberCyan,
                        testTag = "c58_sector_all",
                        onClick = { c58ViewModel.selectSector(0) }
                    )
                    bundle.sectors.forEach { sec ->
                        val isSel = selectedSector == sec.index
                        val isSynced = sec.index in syncedSectors
                        C58SectorChip(
                            label = "${sec.shortLabel}${if (isSynced) " ✓" else ""}",
                            selected = isSel,
                            isSynced = isSynced,
                            activeColor = if (isSynced) NeonLime else LaserOrange,
                            testTag = "c58_sector_${sec.index}",
                            onClick = { c58ViewModel.selectSector(sec.index) }
                        )
                    }
                }

                // 3. Sub-View Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    C58SubView.values().forEach { sub ->
                        val isSel = activeSubView == sub
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) CyberCyan.copy(alpha = 0.22f) else DarkSurface,
                            border = BorderStroke(1.dp, if (isSel) CyberCyan else DarkSurfaceHighlight),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { c58ViewModel.setSubView(sub) }
                                .testTag("c58_subview_${sub.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = sub.label,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) CyberCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Main Content Area
        when (activeSubView) {
            C58SubView.MAP_2D -> {
                AnimatedVisibility(visible = !effectiveFullScreen) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        C58MapToolbar(
                            viewWidthM = viewWidthM,
                            show1mGrid = show1mGrid,
                            showElevationRaster = showElevationRaster,
                            showWater = showWater,
                            showCorridorBounds = showCorridorBounds,
                            showContours = showContours,
                            showRoads = showRoads,
                            showRoadSymbols = showRoadSymbols,
                            showHouses = showHouses,
                            showPois = showPois,
                            showEnvSliders = showEnvSliders,
                            onZoomPreset = { c58ViewModel.setZoomPreset(it) },
                            onToggleGrid = { c58ViewModel.show1mGrid.value = !show1mGrid },
                            onToggleRaster = { c58ViewModel.showElevationRaster.value = !showElevationRaster },
                            onToggleWater = { c58ViewModel.showWater.value = !showWater },
                            onToggleBounds = { c58ViewModel.showCorridorBounds.value = !showCorridorBounds },
                            onToggleContours = { c58ViewModel.showContours.value = !showContours },
                            onToggleRoads = { c58ViewModel.showRoads.value = !showRoads },
                            onToggleRoadSymbols = { c58ViewModel.showRoadSymbols.value = !showRoadSymbols },
                            onToggleHouses = { c58ViewModel.showHouses.value = !showHouses },
                            onTogglePois = { c58ViewModel.showPois.value = !showPois },
                            onToggleEnvSliders = { showEnvSliders = !showEnvSliders }
                        )

                        AnimatedVisibility(visible = showEnvSliders) {
                            C58LakeAndDepthBar(
                                lakeLevelAslM = lakeLevelAslM,
                                verticalExaggeration = verticalExaggeration,
                                onLakeLevelChange = { c58ViewModel.setLakeLevel(it) },
                                onVerticalExagChange = { c58ViewModel.setVerticalExaggeration(it) }
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
                    C58CorridorMap2D(
                        bundle = bundle,
                        raster = rasterOverlay,
                        viewCenterUtmE = viewCenterUtmE,
                        viewCenterUtmN = viewCenterUtmN,
                        viewWidthM = viewWidthM,
                        inspectedCell = inspectedCell,
                        selectedHouse = selectedHouse,
                        selectedRoad = selectedRoad,
                        show1mGrid = show1mGrid,
                        showElevationRaster = showElevationRaster,
                        showWater = showWater,
                        showCorridorBounds = showCorridorBounds,
                        showContours = showContours,
                        showRoads = showRoads,
                        showRoadSymbols = showRoadSymbols,
                        showHouses = showHouses,
                        showPois = showPois,
                        lakeLevelAslM = lakeLevelAslM,
                        demSourceLabel = demSourceLabel,
                        hideAllText = effectiveFullScreen,
                        onToggleHideAllText = { setFullScreen(!fullScreen) },
                        onPan = { dx, dy, w -> c58ViewModel.pan2D(dx, dy, w) },
                        onZoom = { f -> c58ViewModel.zoom2D(f) },
                        onTapInspect = { x, y, w, h -> c58ViewModel.inspectUtmAtScreen(x, y, w, h) },
                        onResetView = { c58ViewModel.fitSectorCamera() },
                        onSizeChanged = { w, h -> c58ViewModel.updateCanvasAspect(w, h) }
                    )
                }

                AnimatedVisibility(visible = !effectiveFullScreen) {
                    C58CellTelemetryCard(
                        inspectedCell = inspectedCell,
                        lakeLevelAslM = lakeLevelAslM,
                        selectedHouse = selectedHouse,
                        selectedRoad = selectedRoad,
                        onOpen3d1m = { c58ViewModel.viewInspectedIn3d1m() }
                    )
                }
            }

            C58SubView.SPLIT_VIEW -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (effectiveFullScreen) 0.dp else 6.dp,
                            vertical = if (effectiveFullScreen) 0.dp else 2.dp
                        )
                ) {
                    C58CorridorMap2D(
                        bundle = bundle,
                        raster = rasterOverlay,
                        viewCenterUtmE = viewCenterUtmE,
                        viewCenterUtmN = viewCenterUtmN,
                        viewWidthM = viewWidthM,
                        inspectedCell = inspectedCell,
                        selectedHouse = selectedHouse,
                        selectedRoad = selectedRoad,
                        show1mGrid = show1mGrid,
                        showElevationRaster = showElevationRaster,
                        showWater = showWater,
                        showCorridorBounds = showCorridorBounds,
                        showContours = showContours,
                        showRoads = showRoads,
                        showRoadSymbols = showRoadSymbols,
                        showHouses = showHouses,
                        showPois = showPois,
                        lakeLevelAslM = lakeLevelAslM,
                        demSourceLabel = demSourceLabel,
                        hideAllText = effectiveFullScreen,
                        onToggleHideAllText = { setFullScreen(!fullScreen) },
                        onPan = { dx, dy, w -> c58ViewModel.pan2D(dx, dy, w) },
                        onZoom = { f -> c58ViewModel.zoom2D(f) },
                        onTapInspect = { x, y, w, h -> c58ViewModel.inspectUtmAtScreen(x, y, w, h) },
                        onResetView = { c58ViewModel.fitSectorCamera() },
                        onSizeChanged = { w, h -> c58ViewModel.updateCanvasAspect(w, h) }
                    )
                }

                AnimatedVisibility(visible = !effectiveFullScreen) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        C583dControlsBar(
                            terrain3dMode = terrain3dMode,
                            terrainRenderMode = terrainRenderMode,
                            is3dWireframe = is3dWireframe,
                            showWater = showWater,
                            selectedSector = selectedSector,
                            onSelect3dMode = { c58ViewModel.set3dMode(it) },
                            onSelectRenderMode = { c58ViewModel.terrainRenderMode.value = it },
                            onToggleWireframe = { c58ViewModel.is3dWireframe.value = !is3dWireframe },
                            onToggleWater = { c58ViewModel.showWater.value = !showWater },
                            onResetCamera = { c58ViewModel.reset3dCamera() }
                        )

                        AnimatedVisibility(visible = showEnvSliders) {
                            C58LakeAndDepthBar(
                                lakeLevelAslM = lakeLevelAslM,
                                verticalExaggeration = verticalExaggeration,
                                onLakeLevelChange = { c58ViewModel.setLakeLevel(it) },
                                onVerticalExagChange = { c58ViewModel.setVerticalExaggeration(it) }
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
                        overlayBuildings3D = houses3d,
                        overlayRoadSegments3D = roads3d,
                        overlayRoadSymbols3D = symbols3d,
                        onRotateDrag = { dx, dy ->
                            c58ViewModel.yaw3d.value += dx * 0.007f
                            c58ViewModel.pitch3d.value = (c58ViewModel.pitch3d.value - dy * 0.007f).coerceIn(0.06f, 1.46f)
                        },
                        onPanDrag = { dx, dy ->
                            c58ViewModel.panX3d.value += dx
                            c58ViewModel.panY3d.value += dy
                        },
                        onZoomChange = { f ->
                            c58ViewModel.zoom3d.value = (c58ViewModel.zoom3d.value * f).coerceIn(0.15f, 35f)
                        }
                    )
                }
            }

            C58SubView.TERRAIN_3D -> {
                AnimatedVisibility(visible = !effectiveFullScreen) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        C583dControlsBar(
                            terrain3dMode = terrain3dMode,
                            terrainRenderMode = terrainRenderMode,
                            is3dWireframe = is3dWireframe,
                            showWater = showWater,
                            selectedSector = selectedSector,
                            onSelect3dMode = { c58ViewModel.set3dMode(it) },
                            onSelectRenderMode = { c58ViewModel.terrainRenderMode.value = it },
                            onToggleWireframe = { c58ViewModel.is3dWireframe.value = !is3dWireframe },
                            onToggleWater = { c58ViewModel.showWater.value = !showWater },
                            onResetCamera = { c58ViewModel.reset3dCamera() }
                        )

                        AnimatedVisibility(visible = showEnvSliders) {
                            C58LakeAndDepthBar(
                                lakeLevelAslM = lakeLevelAslM,
                                verticalExaggeration = verticalExaggeration,
                                onLakeLevelChange = { c58ViewModel.setLakeLevel(it) },
                                onVerticalExagChange = { c58ViewModel.setVerticalExaggeration(it) }
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
                        overlayBuildings3D = houses3d,
                        overlayRoadSegments3D = roads3d,
                        overlayRoadSymbols3D = symbols3d,
                        onRotateDrag = { dx, dy ->
                            c58ViewModel.yaw3d.value += dx * 0.007f
                            c58ViewModel.pitch3d.value = (c58ViewModel.pitch3d.value - dy * 0.007f).coerceIn(0.06f, 1.46f)
                        },
                        onPanDrag = { dx, dy ->
                            c58ViewModel.panX3d.value += dx
                            c58ViewModel.panY3d.value += dy
                        },
                        onZoomChange = { f ->
                            c58ViewModel.zoom3d.value = (c58ViewModel.zoom3d.value * f).coerceIn(0.15f, 35f)
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
                                    .testTag("c58_3d_restore_text_btn")
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
                    C58CellTelemetryCard(
                        inspectedCell = inspectedCell,
                        lakeLevelAslM = lakeLevelAslM,
                        selectedHouse = selectedHouse,
                        selectedRoad = selectedRoad,
                        onOpen3d1m = { c58ViewModel.viewInspectedIn3d1m() }
                    )
                }
            }

            C58SubView.DATA_COORDS -> {
                C58CorridorDataPanel(
                    bundle = bundle,
                    selectedSector = selectedSector,
                    activeTab = dataTabIndex,
                    searchQuery = searchQuery,
                    sideFilter = sideFilter,
                    onSelectTab = { c58ViewModel.dataTabIndex.value = it },
                    onSearchChange = { c58ViewModel.searchQuery.value = it },
                    onSideFilterChange = { c58ViewModel.sideFilter.value = it },
                    onSelectRoad = { c58ViewModel.selectRoad(it) },
                    onSelectHouseMap = { c58ViewModel.selectHouse(it, openIn3d = false) },
                    onSelectHouse3d = { c58ViewModel.selectHouse(it, openIn3d = true) },
                    onSelectPoi = { poi -> c58ViewModel.focusOnUtm(poi.coord.utmEast, poi.coord.utmNorth, targetWidthM = 140f) },
                    onExportGeoJson = { c58ViewModel.exportAndShareGeoJson(context) },
                    onExportCsv = { c58ViewModel.exportAndShareHousesCsv(context) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun C58StatusHeader(
    extractionState: C58ExtractionUiState,
    selectedSector: Int,
    bundle: C58CorridorBundle,
    showEnvSliders: Boolean,
    canHideText: Boolean,
    onFitSector: () -> Unit,
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
                    is C58ExtractionUiState.Extracting -> extractionState.stage
                    is C58ExtractionUiState.Ready -> {
                        "H:${bundle.houses.size} (L:${bundle.metadata.leftSideHouseCount}/R:${bundle.metadata.rightSideHouseCount}) • R:${bundle.roads.size}"
                    }
                    is C58ExtractionUiState.Error -> "ERROR: ${extractionState.message}"
                    C58ExtractionUiState.Idle -> "Loading ±1.5km binary pack..."
                }
                Text(
                    text = statusLine,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (extractionState is C58ExtractionUiState.Error) LaserOrange else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )

                Button(
                    onClick = onFitSector,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.20f)),
                    border = BorderStroke(1.dp, CyberCyan),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("c58_extract_sector_btn")
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit Active Sector", tint = CyberCyan, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (selectedSector == 0) "FIT" else "S$selectedSector", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }

                IconButton(
                    onClick = onReloadBinary,
                    enabled = extractionState !is C58ExtractionUiState.Extracting,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("c58_extract_all_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload Binary Corridor Pack", tint = NeonLime)
                }

                IconButton(
                    onClick = onToggleEnvSliders,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("c58_toggle_sliders_btn")
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
                            .testTag("c58_header_hide_text_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Hide All Text for Fullscreen Map",
                            tint = NeonLime
                        )
                    }
                }
            }

            if (extractionState is C58ExtractionUiState.Extracting) {
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
private fun C58SectorChip(
    label: String,
    selected: Boolean,
    isSynced: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) activeColor.copy(alpha = 0.24f) else DarkSurface,
        border = BorderStroke(
            1.dp,
            when {
                selected -> activeColor
                isSynced -> NeonLime.copy(alpha = 0.45f)
                else -> DarkSurfaceHighlight
            }
        ),
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
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeColor else if (isSynced) NeonLime else TextSecondary
            )
        }
    }
}

@Composable
private fun C58LakeAndDepthBar(
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
                        .testTag("c58_lake_level_slider")
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
                        .testTag("c58_terrain_depth_slider")
                )
            }
        }
    }
}

@Composable
private fun C58MapToolbar(
    viewWidthM: Float,
    show1mGrid: Boolean,
    showElevationRaster: Boolean,
    showWater: Boolean,
    showCorridorBounds: Boolean,
    showContours: Boolean,
    showRoads: Boolean,
    showRoadSymbols: Boolean,
    showHouses: Boolean,
    showPois: Boolean,
    showEnvSliders: Boolean,
    onZoomPreset: (Float) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleRaster: () -> Unit,
    onToggleWater: () -> Unit,
    onToggleBounds: () -> Unit,
    onToggleContours: () -> Unit,
    onToggleRoads: () -> Unit,
    onToggleRoadSymbols: () -> Unit,
    onToggleHouses: () -> Unit,
    onTogglePois: () -> Unit,
    onToggleEnvSliders: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val presets = listOf(
            68_000f to "Full 101km",
            10_000f to "10km",
            3_200f to "3km Width",
            400f to "400m",
            40f to "1m GRID"
        )
        presets.forEach { (wM, lbl) ->
            val active = kotlin.math.abs(viewWidthM - wM) < wM * 0.28f
            C58TogglePill(lbl, active, NeonLime, "c58_zoom_${lbl.lowercase().replace(" ", "_")}") {
                onZoomPreset(wM)
            }
        }
        C58TogglePill("Lake/Depth", showEnvSliders, CyberCyan, "c58_layer_env_sliders", onToggleEnvSliders)
        C58TogglePill("±1.5km Ribbon", showCorridorBounds, CyberCyan, "c58_layer_ribbon", onToggleBounds)
        C58TogglePill("Houses", showHouses, LaserOrange, "c58_layer_houses", onToggleHouses)
        C58TogglePill("Roads", showRoads, LaserOrange, "c58_layer_roads", onToggleRoads)
        C58TogglePill("Symbols", showRoadSymbols, Color(0xFFFFD54F), "c58_layer_symbols", onToggleRoadSymbols)
        C58TogglePill("DEM", showElevationRaster, CyberCyan, "c58_layer_dem", onToggleRaster)
        C58TogglePill("Water", showWater, CyberCyan, "c58_layer_water", onToggleWater)
        C58TogglePill("1m Grid", show1mGrid, CyberCyan, "c58_layer_grid", onToggleGrid)
        C58TogglePill("Contours", showContours, CyberCyan, "c58_layer_contours", onToggleContours)
        C58TogglePill("POIs", showPois, NeonLime, "c58_layer_pois", onTogglePois)
    }
}

@Composable
private fun C583dControlsBar(
    terrain3dMode: String,
    terrainRenderMode: TerrainRenderMode,
    is3dWireframe: Boolean,
    showWater: Boolean,
    selectedSector: Int,
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
        C58TogglePill(
            label = if (selectedSector == 0) "3KM SWATH (ALL)" else "3KM SWATH (S$selectedSector)",
            selected = terrain3dMode == "SECTOR_RIBBON",
            activeColor = CyberCyan,
            testTag = "c58_3d_swath_btn",
            onClick = { onSelect3dMode("SECTOR_RIBBON") }
        )
        C58TogglePill(
            label = "1M WINDOW (64×64m)",
            selected = terrain3dMode == "WINDOW_1M",
            activeColor = LaserOrange,
            testTag = "c58_3d_1m_window_btn",
            onClick = { onSelect3dMode("WINDOW_1M") }
        )
        C58TogglePill("WATER", showWater, CyberCyan, "c58_3d_water_btn", onToggleWater)
        C58TogglePill("WIREFRAME", is3dWireframe, NeonLime, "c58_3d_wireframe_btn", onToggleWireframe)
        listOf(
            TerrainRenderMode.HYPSOMETRIC_CONTOURS to "Hypsometric",
            TerrainRenderMode.SHADED_TRIPLANAR to "Biome PBR",
            TerrainRenderMode.SLOPE_ANALYSIS to "Slope",
            TerrainRenderMode.WIREFRAME_CDLOD to "CDLOD"
        ).forEach { (rm, title) ->
            C58TogglePill(title, terrainRenderMode == rm, HologramBlue, "c58_3d_mode_${rm.name.lowercase()}") {
                onSelectRenderMode(rm)
            }
        }
        C58TogglePill("RESET CAM", false, CyberCyan, "c58_3d_reset_cam_btn", onResetCamera)
    }
}

@Composable
private fun C58CellTelemetryCard(
    inspectedCell: C58InspectedCell,
    lakeLevelAslM: Float,
    selectedHouse: C58ExtractedHouse?,
    selectedRoad: C58ExtractedRoad?,
    onOpen3d1m: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(
            1.dp,
            when {
                !inspectedCell.insideCorridor -> LaserOrange
                inspectedCell.isWater -> CyberCyan
                else -> GlowingCyanBorder
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("c58_cell_telemetry_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "KM ${"%.2f".format(inspectedCell.chainageKm)} • ${inspectedCell.sideLabel} • ${"%.1f".format(inspectedCell.elevM)}m (${"%.1f".format(inspectedCell.slopeDeg)}°)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (inspectedCell.insideCorridor) NeonLime else LaserOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val secondLine = when {
                    selectedHouse != null -> "HOUSE: ${selectedHouse.name} (${"%.0f".format(selectedHouse.footprintAreaM2)}m²)"
                    selectedRoad != null -> "ROAD: ${selectedRoad.name} (${selectedRoad.highwayType})"
                    inspectedCell.isWater -> "WATER • Depth -${"%.2f".format(inspectedCell.waterDepthM)}m (${"%.1f".format(lakeLevelAslM)}m)"
                    else -> "${inspectedCell.utmEast.roundToInt()}E, ${inspectedCell.utmNorth.roundToInt()}N • ${"%.4f".format(inspectedCell.lat)}, ${"%.4f".format(inspectedCell.lon)}"
                }
                Text(
                    text = secondLine,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (selectedHouse != null || selectedRoad != null) LaserOrange else CyberCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = onOpen3d1m,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.22f)),
                border = BorderStroke(1.dp, CyberCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("c58_view_in_3d_1m_btn")
            ) {
                Text("3D 1M", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
        }
    }
}

@Composable
private fun C58TogglePill(
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
