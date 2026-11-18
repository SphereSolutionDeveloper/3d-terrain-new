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
import com.example.repository.C58CorridorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.*

data class C58RasterOverlay(
    val minUtmE: Double,
    val maxUtmE: Double,
    val minUtmN: Double,
    val maxUtmN: Double,
    val cols: Int,
    val rows: Int,
    val data: FloatArray, // interleaved 4 floats: [elevM, hillshade, waterDepthM, insideCorridorMask]
    val minElevM: Float,
    val maxElevM: Float
)

enum class C58SubView(val label: String) {
    MAP_2D("2D C58 MAP"),
    SPLIT_VIEW("SPLIT 2D+3D"),
    TERRAIN_3D("3D CORRIDOR"),
    DATA_COORDS("HOUSES & ROADS")
}

class C58CorridorViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = C58CorridorRepository(application.applicationContext)

    private val _baselineBundle = MutableStateFlow(repo.buildBaselineBundle())

    private val _extractionState = MutableStateFlow<C58ExtractionUiState>(C58ExtractionUiState.Idle)
    val extractionState: StateFlow<C58ExtractionUiState> = _extractionState.asStateFlow()

    // 0 = ALL (S1..S10), 1..10 = specific 10km corridor sector
    private val _selectedSector = MutableStateFlow(1)
    val selectedSector: StateFlow<Int> = _selectedSector.asStateFlow()

    private val _activeSubView = MutableStateFlow(C58SubView.MAP_2D)
    val activeSubView: StateFlow<C58SubView> = _activeSubView.asStateFlow()

    // 2D UTM Zone 37S Map Camera
    private val _viewCenterUtmE = MutableStateFlow(200_500.0)
    val viewCenterUtmE: StateFlow<Double> = _viewCenterUtmE.asStateFlow()

    private val _viewCenterUtmN = MutableStateFlow(9_794_500.0)
    val viewCenterUtmN: StateFlow<Double> = _viewCenterUtmN.asStateFlow()

    private val _viewWidthM = MutableStateFlow(9500f) // Clamped 12m (1m grid!) .. 78,000m (full 101km C58 corridor)
    val viewWidthM: StateFlow<Float> = _viewWidthM.asStateFlow()

    private val _canvasAspect = MutableStateFlow(1.0f)

    // Layer Toggles
    val show1mGrid = MutableStateFlow(true)
    val showElevationRaster = MutableStateFlow(true)
    val showWater = MutableStateFlow(true)
    val showCorridorBounds = MutableStateFlow(true)
    val showContours = MutableStateFlow(true)
    val showRoads = MutableStateFlow(true)
    val showRoadSymbols = MutableStateFlow(true)
    val showHouses = MutableStateFlow(true)
    val showPois = MutableStateFlow(true)

    // Lake Level & Relief Controls
    private val _lakeLevelAslM = MutableStateFlow(602.5f)
    val lakeLevelAslM: StateFlow<Float> = _lakeLevelAslM.asStateFlow()

    private val _verticalExaggeration = MutableStateFlow(2.2f)
    val verticalExaggeration: StateFlow<Float> = _verticalExaggeration.asStateFlow()

    private val _groundDatumAslM = MutableStateFlow(450f)
    val groundDatumAslM: StateFlow<Float> = _groundDatumAslM.asStateFlow()

    private val _sceneUnitsPerMeter3D = MutableStateFlow(0.02f)
    val sceneUnitsPerMeter3D: StateFlow<Float> = _sceneUnitsPerMeter3D.asStateFlow()

    // 4-float native C++ corridor raster
    private val _rasterOverlay = MutableStateFlow<C58RasterOverlay?>(null)
    val rasterOverlay: StateFlow<C58RasterOverlay?> = _rasterOverlay.asStateFlow()

    // Inspected Cell & Feature Selection
    private val _inspectedCell = MutableStateFlow(
        repo.inspectAtUtm(198_200.0, 9_789_600.0)
    )
    val inspectedCell: StateFlow<C58InspectedCell> = _inspectedCell.asStateFlow()

    private val _selectedHouse = MutableStateFlow<C58ExtractedHouse?>(null)
    val selectedHouse: StateFlow<C58ExtractedHouse?> = _selectedHouse.asStateFlow()

    private val _selectedRoad = MutableStateFlow<C58ExtractedRoad?>(null)
    val selectedRoad: StateFlow<C58ExtractedRoad?> = _selectedRoad.asStateFlow()

    // 3D Terrain Mode & Camera State
    private val _terrain3dMode = MutableStateFlow("SECTOR_RIBBON") // "SECTOR_RIBBON" or "WINDOW_1M"
    val terrain3dMode: StateFlow<String> = _terrain3dMode.asStateFlow()

    val terrainRenderMode = MutableStateFlow(TerrainRenderMode.HYPSOMETRIC_CONTOURS)
    val is3dWireframe = MutableStateFlow(false)
    val show3dContours = MutableStateFlow(true)
    val yaw3d = MutableStateFlow(0.74f)
    val pitch3d = MutableStateFlow(0.60f)
    val zoom3d = MutableStateFlow(1.05f)
    val panX3d = MutableStateFlow(0f)
    val panY3d = MutableStateFlow(0f)
    val lightAngle3d = MutableStateFlow(0.85f)

    private val _terrainMesh3D = MutableStateFlow(
        TerrainMesh3D(2, 2, emptyList(), emptyList(), 0f, 1f, 0f)
    )
    val terrainMesh3D: StateFlow<TerrainMesh3D> = _terrainMesh3D.asStateFlow()

    private val _overlayHouses3D = MutableStateFlow<List<TerrainBuilding3D>>(emptyList())
    val overlayHouses3D: StateFlow<List<TerrainBuilding3D>> = _overlayHouses3D.asStateFlow()

    private val _overlayRoads3D = MutableStateFlow<List<TerrainRoadSeg3D>>(emptyList())
    val overlayRoads3D: StateFlow<List<TerrainRoadSeg3D>> = _overlayRoads3D.asStateFlow()

    private val _overlayRoadSymbols3D = MutableStateFlow<List<TerrainRoadSymbol3D>>(emptyList())
    val overlayRoadSymbols3D: StateFlow<List<TerrainRoadSymbol3D>> = _overlayRoadSymbols3D.asStateFlow()

    // Data Explorer State
    val dataTabIndex = MutableStateFlow(0) // 0=Metadata, 1=Roads, 2=Houses, 3=POIs
    val searchQuery = MutableStateFlow("")
    val sideFilter = MutableStateFlow("ALL") // "ALL", "LEFT", "RIGHT"

    private var rasterJob: Job? = null
    private var mesh3dJob: Job? = null
    private var pinnedFitSector: Int? = 1

    init {
        val base = _baselineBundle.value
        fitSectorCamera(1, base)
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            _extractionState.value = C58ExtractionUiState.Extracting(
                stage = "Loading binary C58 ±1.5km corridor & Terrarium DEM pack...",
                progress = 0.25f
            )
            val binaryBundle = try {
                repo.loadBinaryAssetBundle()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                null
            }
            if (binaryBundle != null) {
                _baselineBundle.value = binaryBundle
                _lakeLevelAslM.value = binaryBundle.metadata.lakeLevelAslM
                _extractionState.value = C58ExtractionUiState.Ready(binaryBundle, isFromCache = true)
                val defaultSec = if (binaryBundle.houses.any { it.sectorIndex == 1 }) 1 else (binaryBundle.metadata.extractedSectors.firstOrNull() ?: 1)
                _selectedSector.value = defaultSec
                fitSectorCamera(defaultSec, binaryBundle)
                _inspectedCell.value = repo.inspectAtUtm(_viewCenterUtmE.value, _viewCenterUtmN.value)
            } else {
                _extractionState.value = C58ExtractionUiState.Error(
                    message = "Binary corridor pack missing (assets/terrain_grid/c58_corridor_3km.bin)",
                    cachedBundle = _baselineBundle.value
                )
            }
            refreshRasterOverlay()
            rebuild3dScene()
        }
    }

    fun reloadBinaryPack() {
        initializeData()
    }

    fun selectSector(sectorIndex: Int) {
        _selectedSector.value = sectorIndex.coerceIn(0, 10)
        val bundle = activeBundle()
        fitSectorCamera(_selectedSector.value, bundle)
        _inspectedCell.value = repo.inspectAtUtm(_viewCenterUtmE.value, _viewCenterUtmN.value)
        refreshRasterOverlay()
        if (_terrain3dMode.value == "SECTOR_RIBBON" || _activeSubView.value != C58SubView.MAP_2D) {
            rebuild3dScene()
        }
    }

    fun fitSectorCamera(sectorIndex: Int = _selectedSector.value, bundle: C58CorridorBundle = activeBundle()) {
        val ribbon = bundle.ribbon
        if (ribbon.isEmpty()) return
        pinnedFitSector = sectorIndex
        val aspect = _canvasAspect.value.coerceAtLeast(0.4f)
        if (sectorIndex == 0) {
            val minE = ribbon.minOf { min(it.leftE, it.rightE) }
            val maxE = ribbon.maxOf { max(it.leftE, it.rightE) }
            val minN = ribbon.minOf { min(it.leftN, it.rightN) }
            val maxN = ribbon.maxOf { max(it.leftN, it.rightN) }
            _viewCenterUtmE.value = (minE + maxE) * 0.5
            _viewCenterUtmN.value = (minN + maxN) * 0.5
            val spanNeeded = max(maxE - minE, (maxN - minN) / aspect) * 1.08
            _viewWidthM.value = spanNeeded.toFloat().coerceIn(5000f, 165_000f)
        } else {
            val sec = bundle.sectors.getOrNull(sectorIndex - 1) ?: return
            val s0 = sec.startChainageM
            val s1 = sec.endChainageM

            fun interpRibbon(targetCh: Double): C58RibbonVertex {
                if (ribbon.size == 1 || targetCh <= ribbon.first().chainageM) return ribbon.first()
                for (i in 0 until ribbon.size - 1) {
                    val a = ribbon[i]
                    val b = ribbon[i + 1]
                    if (targetCh <= b.chainageM || i == ribbon.size - 2) {
                        val span = (b.chainageM - a.chainageM).coerceAtLeast(1e-3)
                        val t = ((targetCh - a.chainageM) / span).coerceIn(0.0, 1.0)
                        return C58RibbonVertex(
                            centerE = a.centerE + (b.centerE - a.centerE) * t,
                            centerN = a.centerN + (b.centerN - a.centerN) * t,
                            leftE = a.leftE + (b.leftE - a.leftE) * t,
                            leftN = a.leftN + (b.leftN - a.leftN) * t,
                            rightE = a.rightE + (b.rightE - a.rightE) * t,
                            rightN = a.rightN + (b.rightN - a.rightN) * t,
                            chainageM = targetCh
                        )
                    }
                }
                return ribbon.last()
            }

            val interior = ribbon.filter { it.chainageM in s0..s1 }
            val verts = ArrayList<C58RibbonVertex>(interior.size + 2).apply {
                add(interpRibbon(s0))
                addAll(interior)
                add(interpRibbon(s1))
            }
            val minE = verts.minOf { min(it.leftE, it.rightE) }
            val maxE = verts.maxOf { max(it.leftE, it.rightE) }
            val minN = verts.minOf { min(it.leftN, it.rightN) }
            val maxN = verts.maxOf { max(it.leftN, it.rightN) }
            _viewCenterUtmE.value = (minE + maxE) * 0.5
            _viewCenterUtmN.value = (minN + maxN) * 0.5
            val spanNeeded = max(maxE - minE, (maxN - minN) / aspect) * 1.10
            _viewWidthM.value = spanNeeded.toFloat().coerceIn(3200f, 36_000f)
        }
    }

    fun setLakeLevel(newLevelM: Float) {
        val clamped = newLevelM.coerceIn(590f, 645f)
        _lakeLevelAslM.value = clamped
        repo.syncLakeLevelToCpp(clamped, activeBundle().waterPolygons)
        _inspectedCell.value = repo.inspectAtUtm(_inspectedCell.value.utmEast, _inspectedCell.value.utmNorth)
        refreshRasterOverlay()
        rebuild3dScene()
    }

    fun setVerticalExaggeration(v: Float) {
        _verticalExaggeration.value = v.coerceIn(0.8f, 5.0f)
        rebuild3dScene()
    }

    fun setSubView(view: C58SubView) {
        _activeSubView.value = view
        if (view == C58SubView.TERRAIN_3D || view == C58SubView.SPLIT_VIEW) {
            rebuild3dScene()
        }
    }

    fun updateCanvasAspect(widthPx: Float, heightPx: Float) {
        if (widthPx > 1f && heightPx > 1f) {
            val asp = (heightPx / widthPx).coerceIn(0.4f, 3.0f)
            if (abs(asp - _canvasAspect.value) > 0.02f) {
                _canvasAspect.value = asp
                pinnedFitSector?.let { sec ->
                    fitSectorCamera(sec)
                }
                refreshRasterOverlay()
            }
        }
    }

    fun pan2D(dxPx: Float, dyPx: Float, canvasWidthPx: Float) {
        if (canvasWidthPx <= 1f) return
        pinnedFitSector = null
        val sum = activeBundle().summary
        val metersPerPx = _viewWidthM.value / canvasWidthPx
        val newE = (_viewCenterUtmE.value - dxPx * metersPerPx)
            .coerceIn(sum.minUtmEast - 3000.0, sum.maxUtmEast + 3000.0)
        val newN = (_viewCenterUtmN.value + dyPx * metersPerPx)
            .coerceIn(sum.minUtmNorth - 3000.0, sum.maxUtmNorth + 3000.0)
        _viewCenterUtmE.value = newE
        _viewCenterUtmN.value = newN
        refreshRasterOverlay()
    }

    fun zoom2D(factor: Float) {
        if (factor <= 0f) return
        pinnedFitSector = null
        _viewWidthM.value = (_viewWidthM.value / factor).coerceIn(12f, 165_000f)
        refreshRasterOverlay()
    }

    fun setZoomPreset(widthMeters: Float) {
        if (widthMeters >= 55_000f) {
            selectSector(0)
        } else {
            pinnedFitSector = null
            _viewWidthM.value = widthMeters.coerceIn(12f, 165_000f)
            refreshRasterOverlay()
        }
    }

    fun focusOnUtm(utmE: Double, utmN: Double, targetWidthM: Float = 80f, switchToMap: Boolean = true) {
        pinnedFitSector = null
        _viewCenterUtmE.value = utmE
        _viewCenterUtmN.value = utmN
        _viewWidthM.value = targetWidthM.coerceIn(12f, 165_000f)
        _inspectedCell.value = repo.inspectAtUtm(utmE, utmN)
        if (switchToMap && _activeSubView.value == C58SubView.DATA_COORDS) {
            _activeSubView.value = C58SubView.MAP_2D
        }
        refreshRasterOverlay()
        if (_terrain3dMode.value == "WINDOW_1M" || _activeSubView.value != C58SubView.MAP_2D) {
            rebuild3dScene()
        }
    }

    fun inspectUtmAtScreen(tapX: Float, tapY: Float, canvasW: Float, canvasH: Float) {
        if (canvasW <= 1f || canvasH <= 1f) return
        val wM = _viewWidthM.value.toDouble()
        val hM = wM * (canvasH / canvasW)
        val minE = _viewCenterUtmE.value - wM * 0.5
        val maxN = _viewCenterUtmN.value + hM * 0.5
        val utmE = minE + (tapX / canvasW) * wM
        val utmN = maxN - (tapY / canvasH) * hM

        _inspectedCell.value = repo.inspectAtUtm(utmE, utmN)

        val bundle = activeBundle()
        val hitRadiusM = max(6.0, wM * 0.025)
        val prePad = hitRadiusM + 120.0
        val hitHouse = bundle.houses.firstOrNull { h ->
            if (abs(h.centroid.utmEast - utmE) > prePad || abs(h.centroid.utmNorth - utmN) > prePad) {
                false
            } else {
                val minHe = h.polygon.minOfOrNull { it.utmEast } ?: h.centroid.utmEast
                val maxHe = h.polygon.maxOfOrNull { it.utmEast } ?: h.centroid.utmEast
                val minHn = h.polygon.minOfOrNull { it.utmNorth } ?: h.centroid.utmNorth
                val maxHn = h.polygon.maxOfOrNull { it.utmNorth } ?: h.centroid.utmNorth
                utmE in (minHe - hitRadiusM * 0.4)..(maxHe + hitRadiusM * 0.4) &&
                    utmN in (minHn - hitRadiusM * 0.4)..(maxHn + hitRadiusM * 0.4)
            }
        }
        if (hitHouse != null) {
            _selectedHouse.value = hitHouse
            _selectedRoad.value = null
        } else {
            _selectedHouse.value = null
            _selectedRoad.value = findNearestRoad(bundle.roads, utmE, utmN, hitRadiusM)
        }

        if (_terrain3dMode.value == "WINDOW_1M") {
            rebuild3dScene()
        }
    }

    fun selectHouse(h: C58ExtractedHouse, openIn3d: Boolean = false) {
        _selectedHouse.value = h
        _selectedRoad.value = null
        focusOnUtm(h.centroid.utmEast, h.centroid.utmNorth, targetWidthM = 56f, switchToMap = !openIn3d)
        if (openIn3d) {
            _terrain3dMode.value = "WINDOW_1M"
            yaw3d.value = 0.65f
            pitch3d.value = 0.52f
            zoom3d.value = 1.15f
            panX3d.value = 0f
            panY3d.value = 0f
            _activeSubView.value = C58SubView.TERRAIN_3D
            rebuild3dScene(h.centroid.utmEast, h.centroid.utmNorth)
        }
    }

    fun selectRoad(r: C58ExtractedRoad) {
        _selectedRoad.value = r
        _selectedHouse.value = null
        focusOnUtm(
            r.midCoord.utmEast,
            r.midCoord.utmNorth,
            targetWidthM = max(180f, min(2800f, r.lengthM * 0.85f)),
            switchToMap = true
        )
    }

    fun viewInspectedIn3d1m() {
        val cell = _inspectedCell.value
        _terrain3dMode.value = "WINDOW_1M"
        yaw3d.value = 0.65f
        pitch3d.value = 0.52f
        zoom3d.value = 1.15f
        panX3d.value = 0f
        panY3d.value = 0f
        if (_activeSubView.value == C58SubView.MAP_2D || _activeSubView.value == C58SubView.DATA_COORDS) {
            _activeSubView.value = C58SubView.TERRAIN_3D
        }
        rebuild3dScene(cell.utmEast, cell.utmNorth)
    }

    fun set3dMode(mode: String) {
        _terrain3dMode.value = mode
        if (mode == "SECTOR_RIBBON") {
            yaw3d.value = 0.74f
            pitch3d.value = 0.60f
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
        yaw3d.value = if (_terrain3dMode.value == "WINDOW_1M") 0.65f else 0.74f
        pitch3d.value = if (_terrain3dMode.value == "WINDOW_1M") 0.52f else 0.60f
        zoom3d.value = if (_terrain3dMode.value == "WINDOW_1M") 1.15f else 1.05f
        panX3d.value = 0f
        panY3d.value = 0f
    }

    fun activeBundleOrNull(): C58CorridorBundle? = when (val st = _extractionState.value) {
        is C58ExtractionUiState.Ready -> st.bundle
        is C58ExtractionUiState.Error -> st.cachedBundle
        else -> null
    }

    fun activeBundle(): C58CorridorBundle = activeBundleOrNull() ?: _baselineBundle.value

    private fun findNearestRoad(
        roads: List<C58ExtractedRoad>,
        utmE: Double,
        utmN: Double,
        maxDistM: Double
    ): C58ExtractedRoad? {
        var best: C58ExtractedRoad? = null
        var bestDistSq = maxDistM * maxDistM
        for (road in roads) {
            for (i in 0 until road.nodes.size - 1) {
                val p0 = road.nodes[i]
                val p1 = road.nodes[i + 1]
                val dx = p1.utmEast - p0.utmEast
                val dy = p1.utmNorth - p0.utmNorth
                val lenSq = dx * dx + dy * dy
                val t = if (lenSq > 1e-4) (((utmE - p0.utmEast) * dx + (utmN - p0.utmNorth) * dy) / lenSq).coerceIn(0.0, 1.0) else 0.0
                val px = p0.utmEast + t * dx
                val py = p0.utmNorth + t * dy
                val dSq = (utmE - px) * (utmE - px) + (utmN - py) * (utmN - py)
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
            try {
                val wM = _viewWidthM.value.toDouble()
                val hM = wM * _canvasAspect.value
                val minE = _viewCenterUtmE.value - wM * 0.5
                val maxE = _viewCenterUtmE.value + wM * 0.5
                val minN = _viewCenterUtmN.value - hM * 0.5
                val maxN = _viewCenterUtmN.value + hM * 0.5

                val cols = 96
                val rows = (cols * _canvasAspect.value).roundToInt().coerceIn(48, 136)
                val data = NativeEngineBridge.nativeSampleC58UtmRaster(minE, maxE, minN, maxN, cols, rows)
                if (data.size >= cols * rows * 4) {
                    var minEl = Float.MAX_VALUE
                    var maxEl = -Float.MAX_VALUE
                    var hasInside = false
                    for (i in 0 until cols * rows) {
                        val base = i * 4
                        val mask = data[base + 3]
                        if (mask > 0.5f) {
                            val e = data[base]
                            if (e.isFinite()) {
                                if (e < minEl) minEl = e
                                if (e > maxEl) maxEl = e
                                hasInside = true
                            }
                        }
                    }
                    if (!hasInside || minEl > maxEl) {
                        minEl = 600f
                        maxEl = 1800f
                    }
                    _rasterOverlay.value = C58RasterOverlay(
                        minUtmE = minE,
                        maxUtmE = maxE,
                        minUtmN = minN,
                        maxUtmN = maxN,
                        cols = cols,
                        rows = rows,
                        data = data,
                        minElevM = minEl,
                        maxElevM = maxEl
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
            }
        }
    }

    private fun rebuild3dScene(
        overrideCenterE: Double? = null,
        overrideCenterN: Double? = null
    ) {
        mesh3dJob?.cancel()
        mesh3dJob = viewModelScope.launch(Dispatchers.Default) {
            if (!NativeEngineBridge.isLoaded()) return@launch
            try {
                val bundle = activeBundle()
                val mode = _terrain3dMode.value
                val cell = _inspectedCell.value
                val cE = overrideCenterE ?: cell.utmEast
                val cN = overrideCenterN ?: cell.utmNorth
                val vExag = _verticalExaggeration.value
                val windowExag = (vExag * 0.70f).coerceIn(0.6f, 4.2f)
                val sectorExag = vExag.coerceIn(0.8f, 5.0f)

                val secIdx = _selectedSector.value
                val totalLen = bundle.summary.totalLengthM.coerceAtLeast(10_000.0)
                val sStart = if (secIdx == 0) 0.0 else bundle.sectors.getOrNull(secIdx - 1)?.startChainageM ?: 0.0
                val sEnd = if (secIdx == 0) totalLen else bundle.sectors.getOrNull(secIdx - 1)?.endChainageM ?: totalLen

                val ru = if (secIdx == 0) 27 else 43
                val rv = if (secIdx == 0) 115 else 81

                val mesh = if (mode == "WINDOW_1M") {
                    NativeEngineBridge.generateC581mWindowMesh(
                        centerUtmE = cE,
                        centerUtmN = cN,
                        windowCells = 64,
                        contourInterval = 2.0f,
                        stepTerracing = 0f,
                        verticalExaggeration = windowExag
                    )
                } else {
                    NativeEngineBridge.generateC58Sector3dMesh(
                        sectorStartM = sStart,
                        sectorEndM = sEnd,
                        resU = ru,
                        resV = rv,
                        contourInterval = if (secIdx == 0) 25f else 10f,
                        stepTerracing = 0f,
                        verticalExaggeration = sectorExag
                    )
                }

                val tr = if (mode == "SECTOR_RIBBON") {
                    NativeEngineBridge.nativeGetC58Sector3dTransform(sStart, sEnd, ru, rv, sectorExag)
                } else null

                val trCenterE = tr?.getOrNull(0)?.takeIf { it.isFinite() } ?: cE
                val trCenterN = tr?.getOrNull(1)?.takeIf { it.isFinite() } ?: cN
                val trScaleXZ = tr?.getOrNull(2)?.toFloat()?.takeIf { it.isFinite() && it > 0f } ?: (56.0f / 64.0f)
                val trYScale = (tr?.getOrNull(3)?.toFloat()?.takeIf { it.isFinite() } ?: ((56.0f / 64.0f) * windowExag))
                    .coerceAtLeast(0.001f)

                val v0 = mesh.vertices.firstOrNull()
                val minRawElev = mesh.vertices.minOfOrNull { it.rawElevation }?.takeIf { it.isFinite() } ?: 595f
                val baseDatum = if (mode == "WINDOW_1M" && v0 != null) {
                    v0.rawElevation - (v0.y / trYScale)
                } else {
                    tr?.getOrNull(4)?.toFloat()?.takeIf { it.isFinite() } ?: floor(minRawElev - 18f)
                }

                val (houses3d, roads3d, symbols3d) = build3dFeatureOverlays(
                    mode = mode,
                    centerE = trCenterE,
                    centerN = trCenterN,
                    scaleXZ = trScaleXZ,
                    scaleYPerM = trYScale,
                    baseDatumElev = baseDatum,
                    sectorFilter = if (mode == "SECTOR_RIBBON" && secIdx > 0) secIdx else null,
                    sStartM = sStart,
                    sEndM = sEnd,
                    bundle = bundle,
                    selectedHouseId = _selectedHouse.value?.osmId,
                    selectedRoadId = _selectedRoad.value?.osmId
                )

                _sceneUnitsPerMeter3D.value = trYScale
                _groundDatumAslM.value = baseDatum
                _terrainMesh3D.value = mesh
                _overlayHouses3D.value = houses3d
                _overlayRoads3D.value = roads3d
                _overlayRoadSymbols3D.value = symbols3d
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
            }
        }
    }

    private fun sampleDemElevFast(bundle: C58CorridorBundle, coord: C58Coord): Float {
        val dem = bundle.demGrid
        if (dem == null || dem.uCols < 2 || dem.vRows < 2) return bundle.metadata.elevMeanM
        val totalLen = bundle.summary.totalLengthM.coerceAtLeast(1.0)
        val fu = ((coord.lateralOffsetM + 1500f) / 3000f).coerceIn(0f, 1f) * (dem.uCols - 1)
        val fv = (coord.chainageM / totalLen).toFloat().coerceIn(0f, 1f) * (dem.vRows - 1)
        val c0 = floor(fu).toInt().coerceIn(0, dem.uCols - 2)
        val r0 = floor(fv).toInt().coerceIn(0, dem.vRows - 2)
        val tu = fu - c0
        val tv = fv - r0
        val cols = dem.uCols
        val v00 = dem.elevationsM[r0 * cols + c0]
        val v10 = dem.elevationsM[r0 * cols + c0 + 1]
        val v01 = dem.elevationsM[(r0 + 1) * cols + c0]
        val v11 = dem.elevationsM[(r0 + 1) * cols + c0 + 1]
        val a = v00 + (v10 - v00) * tu
        val b = v01 + (v11 - v01) * tu
        return a + (b - a) * tv
    }

    private fun build3dFeatureOverlays(
        mode: String,
        centerE: Double,
        centerN: Double,
        scaleXZ: Float,
        scaleYPerM: Float,
        baseDatumElev: Float,
        sectorFilter: Int?,
        sStartM: Double,
        sEndM: Double,
        bundle: C58CorridorBundle,
        selectedHouseId: Long?,
        selectedRoadId: Long?
    ): Triple<List<TerrainBuilding3D>, List<TerrainRoadSeg3D>, List<TerrainRoadSymbol3D>> {
        val isWindow = mode == "WINDOW_1M"
        fun toSceneXZ(utmE: Double, utmN: Double): Pair<Float, Float> {
            return ((utmE - centerE) * scaleXZ).toFloat() to ((utmN - centerN) * scaleXZ).toFloat()
        }
        fun elevToY(rawElevM: Float): Float = (rawElevM - baseDatumElev) * scaleYPerM

        val roadsOut = ArrayList<TerrainRoadSeg3D>()
        val symbolsOut = ArrayList<TerrainRoadSymbol3D>()
        val halfWin = 33.5
        val candidateRoads = bundle.roads
            .filter { sectorFilter == null || it.midCoord.chainageM in (sStartM - 400.0)..(sEndM + 400.0) }
            .let { list ->
                val roadComparator = compareByDescending<C58ExtractedRoad> { it.osmId == selectedRoadId }
                    .thenByDescending { it.highwayType in setOf("trunk", "primary", "secondary", "tertiary") || it.ref.isNotBlank() || it.isBridge }
                    .thenByDescending { it.lengthM }
                if (!isWindow && list.size > 450) {
                    if (sectorFilter == null) {
                        (1..10).flatMap { sec ->
                            list.filter { it.sectorIndex == sec }.sortedWith(roadComparator).take(45)
                        }
                    } else {
                        list.sortedWith(roadComparator).take(450)
                    }
                } else list
            }
        for (road in candidateRoads) {
            val isPrimary = road.highwayType in setOf("trunk", "primary", "secondary", "tertiary")
            val isSel = road.osmId == selectedRoadId
            var midAdded = false
            for (k in 0 until road.nodes.size - 1) {
                val n0 = road.nodes[k]
                val n1 = road.nodes[k + 1]
                if (isWindow) {
                    val minE = min(n0.utmEast, n1.utmEast); val maxE = max(n0.utmEast, n1.utmEast)
                    val minN = min(n0.utmNorth, n1.utmNorth); val maxN = max(n0.utmNorth, n1.utmNorth)
                    if (maxE < centerE - halfWin || minE > centerE + halfWin ||
                        maxN < centerN - halfWin || minN > centerN + halfWin
                    ) continue
                }
                val e0 = if (isWindow) repo.inspectAtUtm(n0.utmEast, n0.utmNorth).elevM else sampleDemElevFast(bundle, n0)
                val e1 = if (isWindow) repo.inspectAtUtm(n1.utmEast, n1.utmNorth).elevM else sampleDemElevFast(bundle, n1)
                val (sx0, sz0) = toSceneXZ(n0.utmEast, n0.utmNorth)
                val (sx1, sz1) = toSceneXZ(n1.utmEast, n1.utmNorth)
                val sy0 = elevToY(e0) + if (isWindow) 0.14f else 0.16f
                val sy1 = elevToY(e1) + if (isWindow) 0.14f else 0.16f
                roadsOut.add(
                    TerrainRoadSeg3D(
                        x0 = sx0, y0 = sy0, z0 = sz0,
                        x1 = sx1, y1 = sy1, z1 = sz1,
                        isPrimary = isPrimary,
                        isSelected = isSel
                    )
                )
                if (!midAdded && k == (road.nodes.size - 1) / 2) {
                    midAdded = true
                    val mx = (sx0 + sx1) * 0.5f
                    val my = (sy0 + sy1) * 0.5f
                    val mz = (sz0 + sz1) * 0.5f
                    val dx = sx1 - sx0
                    val dz = sz1 - sz0
                    val dLen = hypot(dx, dz).coerceAtLeast(0.001f)
                    when {
                        road.isBridge -> symbolsOut.add(
                            TerrainRoadSymbol3D(mx, my + 0.12f, mz, dx / dLen, dz / dLen, "BRG", RoadSymbolType3D.BRIDGE_ABUTMENT)
                        )
                        road.isRoundabout -> symbolsOut.add(
                            TerrainRoadSymbol3D(mx, my + 0.12f, mz, dx / dLen, dz / dLen, "RND", RoadSymbolType3D.ROUNDABOUT_RING)
                        )
                        road.ref.isNotBlank() || road.name.contains("Magadi Road", ignoreCase = true) || road.name.equals("C58", ignoreCase = true) -> symbolsOut.add(
                            TerrainRoadSymbol3D(
                                mx, my + 0.15f, mz, dx / dLen, dz / dLen,
                                road.ref.ifBlank { "C58" },
                                RoadSymbolType3D.ROUTE_SHIELD
                            )
                        )
                    }
                    if (road.oneWay) {
                        symbolsOut.add(
                            TerrainRoadSymbol3D(mx, my + 0.10f, mz, dx / dLen, dz / dLen, "1W", RoadSymbolType3D.DIRECTION_ARROW)
                        )
                    }
                }
            }
        }

        for (poi in bundle.pois) {
            if (sectorFilter != null && poi.coord.chainageM !in (sStartM - 300.0)..(sEndM + 300.0)) continue
            if (isWindow) {
                if (abs(poi.coord.utmEast - centerE) > 35.0 || abs(poi.coord.utmNorth - centerN) > 35.0) continue
            }
            val symType = when {
                poi.category in setOf("gate", "bollard", "lift_gate", "toll_booth") -> RoadSymbolType3D.BARRIER_GATE
                poi.category in setOf("crossing", "bus_stop", "traffic_signals", "turning_circle", "mini_roundabout") -> RoadSymbolType3D.CROSSING_SIGNAL
                else -> null
            } ?: continue
            val (sx, sz) = toSceneXZ(poi.coord.utmEast, poi.coord.utmNorth)
            symbolsOut.add(TerrainRoadSymbol3D(sx, elevToY(poi.elevM) + 0.25f, sz, label = poi.category.take(6).uppercase(), type = symType))
        }

        val housesOut = ArrayList<TerrainBuilding3D>()
        val candidateHouses = if (isWindow) {
            bundle.houses.filter {
                abs(it.centroid.utmEast - centerE) <= 36.0 && abs(it.centroid.utmNorth - centerN) <= 36.0
            }
        } else {
            val houseComparator = compareByDescending<C58ExtractedHouse> { it.osmId == selectedHouseId }
                .thenByDescending { it.footprintAreaM2 }
            val filtered = bundle.houses
                .filter { sectorFilter == null || it.centroid.chainageM in (sStartM - 300.0)..(sEndM + 300.0) }
            if (sectorFilter == null) {
                (1..10).flatMap { sec ->
                    filtered.filter { it.sectorIndex == sec }.sortedWith(houseComparator).take(40)
                }
            } else {
                filtered.sortedWith(houseComparator).take(400)
            }
        }

        for (h in candidateHouses) {
            val poly = if (h.polygon.size > 3 && abs(h.polygon.first().utmEast - h.polygon.last().utmEast) < 0.01) {
                h.polygon.dropLast(1)
            } else h.polygon
            if (poly.size < 3) continue
            val cornersXZ = poly.map { pt -> toSceneXZ(pt.utmEast, pt.utmNorth) }
            val baseY = elevToY(h.elevM)
            val heightUnits = if (isWindow) {
                h.heightM.coerceIn(2.6f, 24f) * scaleYPerM
            } else {
                max(0.75f, h.heightM * scaleYPerM * 1.55f)
            }
            housesOut.add(
                TerrainBuilding3D(
                    id = h.osmId,
                    label = h.name,
                    baseCornersXZ = cornersXZ,
                    baseY = baseY,
                    roofY = baseY + heightUnits,
                    isSelected = (h.osmId == selectedHouseId)
                )
            )
        }
        return Triple(housesOut, roadsOut, symbolsOut.take(120))
    }

    fun exportAndShareGeoJson(context: Context) {
        val bundle = activeBundleOrNull() ?: run {
            Toast.makeText(context, "Extract Route C58 corridor data first", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val out = File(context.cacheDir, "route_c58_3km_corridor.geojson")
                out.writeText(repo.exportGeoJson(bundle).toString(2))
                out
            }
            shareFile(context, file, "application/geo+json", "Route C58 ±1.5km Corridor GeoJSON")
        }
    }

    fun exportAndShareHousesCsv(context: Context) {
        val bundle = activeBundleOrNull() ?: run {
            Toast.makeText(context, "Extract Route C58 corridor data first", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val out = File(context.cacheDir, "route_c58_houses_coords.csv")
                val sb = StringBuilder()
                sb.appendLine("osm_id,name,building_type,amenity,levels,height_m,area_m2,elev_asl_m,sector,chainage_km,lateral_offset_m,corridor_side,lat,lon,utm37s_east,utm37s_north,grid_1m_easting_col,grid_1m_northing_row")
                for (h in bundle.houses) {
                    val safeName = h.name.replace("\"", "\"\"")
                    val c = h.centroid
                    sb.appendLine(
                        "${h.osmId},\"$safeName\",${h.buildingType},${h.amenity},${h.levels},${"%.1f".format(h.heightM)},${"%.1f".format(h.footprintAreaM2)},${"%.2f".format(h.elevM)},S${h.sectorIndex},${"%.3f".format(c.chainageKm)},${"%.1f".format(c.lateralOffsetM)},\"${c.corridorSide}\",${"%.7f".format(c.lat)},${"%.7f".format(c.lon)},${"%.2f".format(c.utmEast)},${"%.2f".format(c.utmNorth)},${c.cellCol1m},${c.cellRow1m}"
                    )
                }
                out.writeText(sb.toString())
                out
            }
            shareFile(context, file, "text/csv", "Route C58 Corridor House Coordinates CSV")
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
