package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.NativeEngineBridge
import com.example.model.*
import com.example.repository.MagadiTownRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.*

data class MagadiRasterOverlay(
    val minXM: Float,
    val maxXM: Float,
    val minYM: Float,
    val maxYM: Float,
    val cols: Int,
    val rows: Int,
    val data: FloatArray, // interleaved [elevM, hillshade, waterDepthM]
    val minElevM: Float,
    val maxElevM: Float
)

enum class MagadiSubView(val label: String) {
    MAP_2D("2D 1M MAP"),
    SPLIT_VIEW("SPLIT 2D+3D"),
    TERRAIN_3D("3D TERRAIN"),
    DATA_COORDS("DATA & COORDS")
}

class MagadiTownViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MagadiTownRepository(application.applicationContext)

    private val _bounds = MutableStateFlow(repo.getNativeBounds())
    val bounds: StateFlow<MagadiTownBounds> = _bounds.asStateFlow()

    private val _extractionState = MutableStateFlow<MagadiExtractionUiState>(MagadiExtractionUiState.Idle)
    val extractionState: StateFlow<MagadiExtractionUiState> = _extractionState.asStateFlow()

    private val _activeSubView = MutableStateFlow(MagadiSubView.MAP_2D)
    val activeSubView: StateFlow<MagadiSubView> = _activeSubView.asStateFlow()

    // 2D Map Camera State (center & horizontal span in meters inside 3000m x 5000m domain)
    private val _viewCenterXM = MutableStateFlow(1500f)
    val viewCenterXM: StateFlow<Float> = _viewCenterXM.asStateFlow()

    private val _viewCenterYM = MutableStateFlow(2500f)
    val viewCenterYM: StateFlow<Float> = _viewCenterYM.asStateFlow()

    private val _viewWidthM = MutableStateFlow(3000f) // Clamped 12m (sub-1m zoom) .. 3600m (full 3x5km)
    val viewWidthM: StateFlow<Float> = _viewWidthM.asStateFlow()

    private val _canvasAspect = MutableStateFlow(1.0f) // height / width

    // Map Layer Toggles
    val show1mGrid = MutableStateFlow(true)
    val showElevationRaster = MutableStateFlow(true)
    val showWater = MutableStateFlow(true)
    val showContours = MutableStateFlow(true)
    val showRoads = MutableStateFlow(true)
    val showBuildings = MutableStateFlow(true)
    val showPois = MutableStateFlow(true)

    // Lake Level & Terrain Relief Controls
    private val _lakeLevelAslM = MutableStateFlow(602.5f)
    val lakeLevelAslM: StateFlow<Float> = _lakeLevelAslM.asStateFlow()

    private val _verticalExaggeration = MutableStateFlow(2.4f)
    val verticalExaggeration: StateFlow<Float> = _verticalExaggeration.asStateFlow()

    private val _groundDatumAslM = MutableStateFlow(400.0f)
    val groundDatumAslM: StateFlow<Float> = _groundDatumAslM.asStateFlow()

    private val _sceneUnitsPerMeter3D = MutableStateFlow(0.12f * 2.4f)
    val sceneUnitsPerMeter3D: StateFlow<Float> = _sceneUnitsPerMeter3D.asStateFlow()

    // Elevation Raster Overlay sampled directly from C++ 1m DEM engine
    private val _rasterOverlay = MutableStateFlow<MagadiRasterOverlay?>(null)
    val rasterOverlay: StateFlow<MagadiRasterOverlay?> = _rasterOverlay.asStateFlow()

    // Inspected 1m Cell + Selected Features
    private val _inspectedCell = MutableStateFlow(repo.inspect1mCell(1500f, 2500f))
    val inspectedCell: StateFlow<Inspected1mCell> = _inspectedCell.asStateFlow()

    private val _selectedBuilding = MutableStateFlow<ExtractedBuilding?>(null)
    val selectedBuilding: StateFlow<ExtractedBuilding?> = _selectedBuilding.asStateFlow()

    private val _selectedRoad = MutableStateFlow<ExtractedRoadWay?>(null)
    val selectedRoad: StateFlow<ExtractedRoadWay?> = _selectedRoad.asStateFlow()

    // 3D Terrain Mode & Camera State
    private val _terrain3dMode = MutableStateFlow("OVERVIEW_3X5KM") // "OVERVIEW_3X5KM" or "WINDOW_1M"
    val terrain3dMode: StateFlow<String> = _terrain3dMode.asStateFlow()

    val terrainRenderMode = MutableStateFlow(TerrainRenderMode.HYPSOMETRIC_CONTOURS)
    val is3dWireframe = MutableStateFlow(false)
    val show3dContours = MutableStateFlow(true)
    val yaw3d = MutableStateFlow(0.72f)
    val pitch3d = MutableStateFlow(0.62f)
    val zoom3d = MutableStateFlow(1.05f)
    val panX3d = MutableStateFlow(0f)
    val panY3d = MutableStateFlow(0f)
    val lightAngle3d = MutableStateFlow(0.85f)

    private val _terrainMesh3D = MutableStateFlow(
        TerrainMesh3D(2, 2, emptyList(), emptyList(), 0f, 1f, 0f)
    )
    val terrainMesh3D: StateFlow<TerrainMesh3D> = _terrainMesh3D.asStateFlow()

    private val _overlayBuildings3D = MutableStateFlow<List<TerrainBuilding3D>>(emptyList())
    val overlayBuildings3D: StateFlow<List<TerrainBuilding3D>> = _overlayBuildings3D.asStateFlow()

    private val _overlayRoads3D = MutableStateFlow<List<TerrainRoadSeg3D>>(emptyList())
    val overlayRoads3D: StateFlow<List<TerrainRoadSeg3D>> = _overlayRoads3D.asStateFlow()

    // Data Explorer filter state
    val dataTabIndex = MutableStateFlow(0) // 0=Metadata, 1=Roads, 2=Buildings, 3=POIs
    val searchQuery = MutableStateFlow("")

    private var rasterJob: Job? = null
    private var mesh3dJob: Job? = null

    init {
        refreshRasterOverlay()
        rebuild3dScene()
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            val cached = repo.loadCachedBundle()
            if (cached != null) {
                _bounds.value = cached.bounds
                _lakeLevelAslM.value = cached.metadata.lakeLevelAslM
                _extractionState.value = MagadiExtractionUiState.Ready(cached, isFromCache = true)
                _inspectedCell.value = repo.inspect1mCell(_viewCenterXM.value, _viewCenterYM.value)
                refreshRasterOverlay()
                rebuild3dScene()
            } else {
                extractLiveData()
            }
        }
    }

    fun extractLiveData() {
        viewModelScope.launch {
            val priorBundle = when (val st = _extractionState.value) {
                is MagadiExtractionUiState.Ready -> st.bundle
                is MagadiExtractionUiState.Error -> st.cachedBundle
                else -> null
            } ?: repo.loadCachedBundle()

            _extractionState.value = MagadiExtractionUiState.Extracting(
                stage = "Connecting to OpenStreetMap & Terrarium ~9.5m DEM...",
                progress = 0.08f
            )

            val result = repo.extractLiveMagadiTownData { stage, progress ->
                _extractionState.value = MagadiExtractionUiState.Extracting(stage, progress)
            }

            result.onSuccess { bundle ->
                _bounds.value = bundle.bounds
                _lakeLevelAslM.value = bundle.metadata.lakeLevelAslM
                _extractionState.value = MagadiExtractionUiState.Ready(bundle, isFromCache = false)
                _inspectedCell.value = repo.inspect1mCell(_viewCenterXM.value, _viewCenterYM.value)
                refreshRasterOverlay()
                rebuild3dScene()
            }.onFailure { err ->
                _extractionState.value = MagadiExtractionUiState.Error(
                    message = err.message ?: "Network error extracting OSM/DEM data",
                    cachedBundle = priorBundle
                )
            }
        }
    }

    fun setLakeLevel(newLevelM: Float) {
        val clamped = newLevelM.coerceIn(590f, 645f)
        _lakeLevelAslM.value = clamped
        val waterPolys = activeBundleOrNull()?.waterPolygons ?: emptyList()
        repo.syncLakeLevelToCpp(clamped, waterPolys)
        _inspectedCell.value = repo.inspect1mCell(_inspectedCell.value.xM, _inspectedCell.value.yM)
        refreshRasterOverlay()
        rebuild3dScene()
    }

    fun setVerticalExaggeration(v: Float) {
        _verticalExaggeration.value = v.coerceIn(0.8f, 5.0f)
        rebuild3dScene()
    }

    fun setSubView(view: MagadiSubView) {
        _activeSubView.value = view
        if (view == MagadiSubView.TERRAIN_3D || view == MagadiSubView.SPLIT_VIEW) {
            rebuild3dScene()
        }
    }

    fun updateCanvasAspect(widthPx: Float, heightPx: Float) {
        if (widthPx > 1f && heightPx > 1f) {
            val asp = (heightPx / widthPx).coerceIn(0.4f, 3.0f)
            if (abs(asp - _canvasAspect.value) > 0.02f) {
                _canvasAspect.value = asp
                refreshRasterOverlay()
            }
        }
    }

    fun pan2D(dxPx: Float, dyPx: Float, canvasWidthPx: Float) {
        if (canvasWidthPx <= 1f) return
        val metersPerPx = _viewWidthM.value / canvasWidthPx
        val newX = (_viewCenterXM.value - dxPx * metersPerPx).coerceIn(0f, 3000f)
        // Screen Y grows downward; metric Northing (yM) grows upward
        val newY = (_viewCenterYM.value + dyPx * metersPerPx).coerceIn(0f, 5000f)
        _viewCenterXM.value = newX
        _viewCenterYM.value = newY
        refreshRasterOverlay()
    }

    fun zoom2D(factor: Float) {
        if (factor <= 0f) return
        val targetW = (_viewWidthM.value / factor).coerceIn(12f, 3600f)
        _viewWidthM.value = targetW
        refreshRasterOverlay()
    }

    fun setZoomPreset(widthMeters: Float) {
        _viewWidthM.value = widthMeters.coerceIn(12f, 3600f)
        if (widthMeters >= 2800f) {
            _viewCenterXM.value = 1500f
            _viewCenterYM.value = 2500f
        }
        refreshRasterOverlay()
    }

    fun focusOnCoordinate(xM: Float, yM: Float, targetWidthM: Float = 64f, switchToMap: Boolean = true) {
        _viewCenterXM.value = xM.coerceIn(0f, 3000f)
        _viewCenterYM.value = yM.coerceIn(0f, 5000f)
        _viewWidthM.value = targetWidthM.coerceIn(12f, 3600f)
        _inspectedCell.value = repo.inspect1mCell(_viewCenterXM.value, _viewCenterYM.value)
        if (switchToMap && _activeSubView.value == MagadiSubView.DATA_COORDS) {
            _activeSubView.value = MagadiSubView.MAP_2D
        }
        refreshRasterOverlay()
        if (_terrain3dMode.value == "WINDOW_1M" || _activeSubView.value != MagadiSubView.MAP_2D) {
            rebuild3dScene()
        }
    }

    fun inspectPointAtScreen(
        tapX: Float,
        tapY: Float,
        canvasW: Float,
        canvasH: Float
    ) {
        if (canvasW <= 1f || canvasH <= 1f) return
        val wM = _viewWidthM.value
        val hM = wM * (canvasH / canvasW)
        val minX = _viewCenterXM.value - wM * 0.5f
        val maxY = _viewCenterYM.value + hM * 0.5f
        val xM = (minX + (tapX / canvasW) * wM).coerceIn(0f, 3000f)
        val yM = (maxY - (tapY / canvasH) * hM).coerceIn(0f, 5000f)

        _inspectedCell.value = repo.inspect1mCell(xM, yM)

        val bundle = activeBundleOrNull()
        if (bundle != null) {
            val hitRadiusM = max(4f, wM * 0.025f)
            // Check buildings first (centroid or bbox inside polygon)
            val hitBldg = bundle.buildings.firstOrNull { b ->
                val minBx = b.polygon.minOfOrNull { it.xM } ?: b.centroid.xM
                val maxBx = b.polygon.maxOfOrNull { it.xM } ?: b.centroid.xM
                val minBy = b.polygon.minOfOrNull { it.yM } ?: b.centroid.yM
                val maxBy = b.polygon.maxOfOrNull { it.yM } ?: b.centroid.yM
                xM in (minBx - hitRadiusM * 0.4f)..(maxBx + hitRadiusM * 0.4f) &&
                    yM in (minBy - hitRadiusM * 0.4f)..(maxBy + hitRadiusM * 0.4f)
            }
            if (hitBldg != null) {
                _selectedBuilding.value = hitBldg
                _selectedRoad.value = null
            } else {
                _selectedBuilding.value = null
                val hitRoad = findNearestRoad(bundle.roads, xM, yM, hitRadiusM)
                _selectedRoad.value = hitRoad
            }
        }

        if (_terrain3dMode.value == "WINDOW_1M") {
            rebuild3dScene()
        }
    }

    fun selectBuilding(b: ExtractedBuilding, openIn3d: Boolean = false) {
        _selectedBuilding.value = b
        _selectedRoad.value = null
        focusOnCoordinate(b.centroid.xM, b.centroid.yM, targetWidthM = 48f, switchToMap = !openIn3d)
        if (openIn3d) {
            set3dMode("WINDOW_1M")
            _activeSubView.value = MagadiSubView.TERRAIN_3D
        }
    }

    fun selectRoad(r: ExtractedRoadWay) {
        _selectedRoad.value = r
        _selectedBuilding.value = null
        val mid = r.nodes.getOrNull(r.nodes.size / 2) ?: return
        focusOnCoordinate(mid.xM, mid.yM, targetWidthM = max(140f, min(1400f, r.lengthM * 0.7f)), switchToMap = true)
    }

    fun viewInspectedCellIn3d1m() {
        val cell = _inspectedCell.value
        _terrain3dMode.value = "WINDOW_1M"
        yaw3d.value = 0.65f
        pitch3d.value = 0.52f
        zoom3d.value = 1.15f
        panX3d.value = 0f
        panY3d.value = 0f
        if (_activeSubView.value == MagadiSubView.MAP_2D || _activeSubView.value == MagadiSubView.DATA_COORDS) {
            _activeSubView.value = MagadiSubView.TERRAIN_3D
        }
        rebuild3dScene(cell.xM, cell.yM)
    }

    fun set3dMode(mode: String) {
        _terrain3dMode.value = mode
        if (mode == "OVERVIEW_3X5KM") {
            yaw3d.value = 0.72f
            pitch3d.value = 0.62f
            zoom3d.value = 1.05f
        } else {
            yaw3d.value = 0.65f
            pitch3d.value = 0.52f
            zoom3d.value = 1.15f
        }
        panX3d.value = 0f
        panY3d.value = 0f
        rebuild3dScene()
    }

    fun reset3dCamera() {
        yaw3d.value = if (_terrain3dMode.value == "WINDOW_1M") 0.65f else 0.72f
        pitch3d.value = if (_terrain3dMode.value == "WINDOW_1M") 0.52f else 0.62f
        zoom3d.value = if (_terrain3dMode.value == "WINDOW_1M") 1.15f else 1.05f
        panX3d.value = 0f
        panY3d.value = 0f
    }

    fun activeBundleOrNull(): MagadiTownBundle? = when (val st = _extractionState.value) {
        is MagadiExtractionUiState.Ready -> st.bundle
        is MagadiExtractionUiState.Error -> st.cachedBundle
        else -> null
    }

    private fun findNearestRoad(roads: List<ExtractedRoadWay>, xM: Float, yM: Float, maxDistM: Float): ExtractedRoadWay? {
        var best: ExtractedRoadWay? = null
        var bestDistSq = maxDistM * maxDistM
        for (road in roads) {
            for (i in 0 until road.nodes.size - 1) {
                val p0 = road.nodes[i]
                val p1 = road.nodes[i + 1]
                val dx = p1.xM - p0.xM
                val dy = p1.yM - p0.yM
                val lenSq = dx * dx + dy * dy
                val t = if (lenSq > 1e-4f) (((xM - p0.xM) * dx + (yM - p0.yM) * dy) / lenSq).coerceIn(0f, 1f) else 0f
                val px = p0.xM + t * dx
                val py = p0.yM + t * dy
                val dSq = (xM - px) * (xM - px) + (yM - py) * (yM - py)
                if (dSq < bestDistSq) {
                    bestDistSq = dSq
                    best = road
                }
            }
        }
        return best
    }

    private fun refreshRasterOverlay() {
        rasterJob?.cancel()
        rasterJob = viewModelScope.launch(Dispatchers.Default) {
            if (!NativeEngineBridge.isLoaded()) return@launch
            val wM = _viewWidthM.value
            val hM = wM * _canvasAspect.value
            val minX = (_viewCenterXM.value - wM * 0.5f).coerceIn(0f, 2999f)
            val maxX = (_viewCenterXM.value + wM * 0.5f).coerceIn(minX + 1f, 3000f)
            val minY = (_viewCenterYM.value - hM * 0.5f).coerceIn(0f, 4999f)
            val maxY = (_viewCenterYM.value + hM * 0.5f).coerceIn(minY + 1f, 5000f)

            val cols = 64
            val rows = (cols * ((maxY - minY) / (maxX - minX))).roundToInt().coerceIn(32, 88)
            val data = NativeEngineBridge.nativeSampleMagadiTownRaster(minX, maxX, minY, maxY, cols, rows)
            if (data.size >= cols * rows * 3) {
                var minE = Float.MAX_VALUE
                var maxE = -Float.MAX_VALUE
                for (i in 0 until cols * rows) {
                    val e = data[i * 3]
                    if (e < minE) minE = e
                    if (e > maxE) maxE = e
                }
                _rasterOverlay.value = MagadiRasterOverlay(
                    minXM = minX,
                    maxXM = maxX,
                    minYM = minY,
                    maxYM = maxY,
                    cols = cols,
                    rows = rows,
                    data = data,
                    minElevM = minE,
                    maxElevM = maxE
                )
            }
        }
    }

    private fun rebuild3dScene(
        overrideCenterX: Float? = null,
        overrideCenterY: Float? = null
    ) {
        mesh3dJob?.cancel()
        mesh3dJob = viewModelScope.launch(Dispatchers.Default) {
            if (!NativeEngineBridge.isLoaded()) return@launch
            val mode = _terrain3dMode.value
            val cell = _inspectedCell.value
            val cX = (overrideCenterX ?: cell.xM).coerceIn(32f, 2968f)
            val cY = (overrideCenterY ?: cell.yM).coerceIn(32f, 4968f)
            val vExag = _verticalExaggeration.value
            val windowExag = (vExag * 0.70f).coerceIn(0.6f, 4.2f)
            val overviewExag = vExag.coerceIn(0.8f, 5.5f)

            val mesh = if (mode == "WINDOW_1M") {
                NativeEngineBridge.generateMagadiTown1mWindowMesh(
                    centerXM = cX,
                    centerYM = cY,
                    windowCells = 64,
                    contourInterval = 2.0f,
                    stepTerracing = 0f,
                    verticalExaggeration = windowExag
                )
            } else {
                NativeEngineBridge.generateMagadiTownOverviewMesh(
                    resX = 61,
                    resZ = 101,
                    contourInterval = 10.0f,
                    stepTerracing = 0f,
                    verticalExaggeration = overviewExag
                )
            }

            val unitsPerM = if (mode == "WINDOW_1M") (56.0f / 64.0f) * windowExag else 0.12f * overviewExag
            val minRawElev = mesh.vertices.minOfOrNull { it.rawElevation } ?: 595f
            val datumAsl = if (mode == "WINDOW_1M") {
                floor(minRawElev - 10f)
            } else {
                min(420f, floor((minRawElev - 30f) / 10f) * 10f)
            }

            val v0 = mesh.vertices.firstOrNull()
            val (buildings3d, roads3d) = if (v0 != null) {
                build3dFeatureOverlays(
                    mode = mode,
                    centerXM = cX,
                    centerYM = cY,
                    anchorRawElev = v0.rawElevation,
                    anchorSceneY = v0.y,
                    scaleYPerM = unitsPerM,
                    overviewExag = overviewExag,
                    bundle = activeBundleOrNull(),
                    selectedBldgId = _selectedBuilding.value?.osmId,
                    selectedRoadId = _selectedRoad.value?.osmId
                )
            } else {
                emptyList<TerrainBuilding3D>() to emptyList()
            }

            _sceneUnitsPerMeter3D.value = unitsPerM
            _groundDatumAslM.value = datumAsl
            _terrainMesh3D.value = mesh
            _overlayBuildings3D.value = buildings3d
            _overlayRoads3D.value = roads3d
        }
    }

    private fun build3dFeatureOverlays(
        mode: String,
        centerXM: Float,
        centerYM: Float,
        anchorRawElev: Float,
        anchorSceneY: Float,
        scaleYPerM: Float,
        overviewExag: Float,
        bundle: MagadiTownBundle?,
        selectedBldgId: Long?,
        selectedRoadId: Long?
    ): Pair<List<TerrainBuilding3D>, List<TerrainRoadSeg3D>> {
        if (bundle == null) return emptyList<TerrainBuilding3D>() to emptyList()

        val isWindow = mode == "WINDOW_1M"
        val baseDatumElev = anchorRawElev - (anchorSceneY / scaleYPerM)

        fun toSceneXZ(xM: Float, yM: Float): Pair<Float, Float> {
            return if (isWindow) {
                ((xM - centerXM) / 64.0f) * 56.0f to ((yM - centerYM) / 64.0f) * 56.0f
            } else {
                ((xM - 1500.0f) / 3000.0f) * 48.0f to ((yM - 2500.0f) / 5000.0f) * 80.0f
            }
        }

        fun elevToSceneY(rawElevM: Float): Float = (rawElevM - baseDatumElev) * scaleYPerM

        val roadsOut = ArrayList<TerrainRoadSeg3D>()
        val halfWin = 33.5f
        for (road in bundle.roads) {
            val isPrimary = road.highwayType in setOf("trunk", "primary", "secondary", "tertiary")
            val isSel = road.osmId == selectedRoadId
            for (k in 0 until road.nodes.size - 1) {
                val n0 = road.nodes[k]
                val n1 = road.nodes[k + 1]
                if (isWindow) {
                    val minX = min(n0.xM, n1.xM); val maxX = max(n0.xM, n1.xM)
                    val minY = min(n0.yM, n1.yM); val maxY = max(n0.yM, n1.yM)
                    if (maxX < centerXM - halfWin || minX > centerXM + halfWin ||
                        maxY < centerYM - halfWin || minY > centerYM + halfWin
                    ) continue
                } else {
                    if ((n0.xM < -50f && n1.xM < -50f) || (n0.xM > 3050f && n1.xM > 3050f) ||
                        (n0.yM < -50f && n1.yM < -50f) || (n0.yM > 5050f && n1.yM > 5050f)
                    ) continue
                }
                val p0x = n0.xM.coerceIn(0f, 3000f)
                val p0y = n0.yM.coerceIn(0f, 5000f)
                val p1x = n1.xM.coerceIn(0f, 3000f)
                val p1y = n1.yM.coerceIn(0f, 5000f)
                val e0 = repo.inspect1mCell(p0x, p0y).elevM
                val e1 = repo.inspect1mCell(p1x, p1y).elevM
                val (sx0, sz0) = toSceneXZ(p0x, p0y)
                val (sx1, sz1) = toSceneXZ(p1x, p1y)
                val syLift = if (isWindow) 0.14f else 0.18f
                roadsOut.add(
                    TerrainRoadSeg3D(
                        x0 = sx0, y0 = elevToSceneY(e0) + syLift, z0 = sz0,
                        x1 = sx1, y1 = elevToSceneY(e1) + syLift, z1 = sz1,
                        isPrimary = isPrimary,
                        isSelected = isSel
                    )
                )
            }
        }

        val bldgOut = ArrayList<TerrainBuilding3D>()
        val candidateBldgs = if (isWindow) {
            bundle.buildings.filter {
                abs(it.centroid.xM - centerXM) <= 36f && abs(it.centroid.yM - centerYM) <= 36f
            }
        } else {
            bundle.buildings
                .filter { it.centroid.xM in 0f..3000f && it.centroid.yM in 0f..5000f }
                .sortedWith(compareByDescending<ExtractedBuilding> { it.osmId == selectedBldgId }
                    .thenByDescending { it.footprintAreaM2 })
                .take(240)
        }

        for (b in candidateBldgs) {
            val poly = if (b.polygon.size > 3 && b.polygon.first().xM == b.polygon.last().xM) {
                b.polygon.dropLast(1)
            } else b.polygon
            if (poly.size < 3) continue
            val cornersXZ = poly.map { pt ->
                toSceneXZ(pt.xM.coerceIn(0f, 3000f), pt.yM.coerceIn(0f, 5000f))
            }
            val baseY = elevToSceneY(b.elevM)
            val heightUnits = if (isWindow) {
                b.heightM.coerceIn(2.6f, 25f) * scaleYPerM
            } else {
                max(0.85f, b.heightM * 0.18f * min(2.0f, overviewExag * 0.65f))
            }
            bldgOut.add(
                TerrainBuilding3D(
                    id = b.osmId,
                    label = b.name,
                    baseCornersXZ = cornersXZ,
                    baseY = baseY,
                    roofY = baseY + heightUnits,
                    isSelected = (b.osmId == selectedBldgId)
                )
            )
        }

        return bldgOut to roadsOut
    }

    fun exportAndShareGeoJson(context: Context) {
        val bundle = activeBundleOrNull() ?: run {
            Toast.makeText(context, "Extract Magadi Town data before exporting", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val out = File(context.cacheDir, "magadi_town_3x5km_1m.geojson")
                out.writeText(repo.exportGeoJson(bundle).toString(2))
                out
            }
            shareFile(context, file, "application/geo+json", "Magadi Town 3x5km 1m GeoJSON")
        }
    }

    fun exportAndShareBuildingsCsv(context: Context) {
        val bundle = activeBundleOrNull() ?: run {
            Toast.makeText(context, "Extract Magadi Town data before exporting", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val out = File(context.cacheDir, "magadi_town_buildings_coords.csv")
                val sb = StringBuilder()
                sb.appendLine("osm_id,name,building_type,amenity,levels,height_m,area_m2,elev_asl_m,lat,lon,utm37s_easting,utm37s_northing,grid_1m_col,grid_1m_row")
                for (b in bundle.buildings) {
                    val safeName = b.name.replace("\"", "\"\"")
                    sb.appendLine(
                        "${b.osmId},\"$safeName\",${b.buildingType},${b.amenity},${b.levels},${"%.1f".format(b.heightM)},${"%.1f".format(b.footprintAreaM2)},${"%.2f".format(b.elevM)},${"%.7f".format(b.centroid.lat)},${"%.7f".format(b.centroid.lon)},${"%.2f".format(b.centroid.utmEast)},${"%.2f".format(b.centroid.utmNorth)},${b.centroid.cellCol},${b.centroid.cellRow}"
                    )
                }
                out.writeText(sb.toString())
                out
            }
            shareFile(context, file, "text/csv", "Magadi Town Building Coordinates CSV")
        }
    }

    private fun shareFile(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
