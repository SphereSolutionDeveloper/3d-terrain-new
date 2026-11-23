package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.NativeEngineBridge
import com.example.engine.NativeEngineBridge.toProceduralModelMesh
import com.example.model.*
import com.example.repository.AssetRepository
import kotlin.math.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AssetStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AssetRepository(application)

    private val _catalog = MutableStateFlow<List<AssetItem>>(emptyList())
    val catalog: StateFlow<List<AssetItem>> = _catalog.asStateFlow()

    private val _selectedAsset = MutableStateFlow<AssetItem?>(null)
    val selectedAsset: StateFlow<AssetItem?> = _selectedAsset.asStateFlow()

    private val _assetBundle = MutableStateFlow<FullAssetBundle?>(null)
    val assetBundle: StateFlow<FullAssetBundle?> = _assetBundle.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inspectionMode = MutableStateFlow(InspectionMode.WIREFRAME)
    val inspectionMode: StateFlow<InspectionMode> = _inspectionMode.asStateFlow()

    private val _folderTree = MutableStateFlow<FileNode?>(null)
    val folderTree: StateFlow<FileNode?> = _folderTree.asStateFlow()

    // 3D Viewport Navigation & Camera State
    val yaw = MutableStateFlow(0.45f)
    val pitch = MutableStateFlow(0.35f)
    val zoom = MutableStateFlow(1.0f)
    val panX = MutableStateFlow(0.0f)
    val panY = MutableStateFlow(0.0f)

    // Render & Inspection Settings
    val isAutoRotate = MutableStateFlow(true)
    val autoRotateSpeed = MutableStateFlow(1.0f)
    val wireframeLineWidth = MutableStateFlow(2.5f)
    val showVertexNodes = MutableStateFlow(true)
    val showBoundingBox = MutableStateFlow(true)
    val showCoordinateGizmo = MutableStateFlow(true)
    val showFloorGrid = MutableStateFlow(true)
    val wireframeColorPalette = MutableStateFlow("Neon Cyan")

    // 3D Shading
    val shadingMode = MutableStateFlow("Solid Metallic") // Solid Metallic, Lit PBR Shaded, Volume Shapes, Surface Normals, X-Ray Hologram, Wireframe Over Solid
    val lightAngleRad = MutableStateFlow(0.8f)

    // Volumetric Shape Decomposition
    val selectedVolumeShapeId = MutableStateFlow<String?>(null) // null = all volumes
    val explodeFactor = MutableStateFlow(0.0f) // 0.0 to 1.0 explosion expansion

    // Textures
    val activeTextureKey = MutableStateFlow("albedo.png")
    val activeChannelMode = MutableStateFlow("RGBA (Full)")
    private val _textureBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val textureBitmaps: StateFlow<Map<String, Bitmap>> = _textureBitmaps.asStateFlow()

    // JSON Specs
    val activeJsonTab = MutableStateFlow("metadata.json")
    val jsonSearchQuery = MutableStateFlow("")

    // File Explorer preview
    val previewFileNode = MutableStateFlow<FileNode?>(null)
    val previewFileContent = MutableStateFlow<String?>(null)

    // 10x10 Rock, Tree & Grass Grid (1m x 1m Cells) State
    val activeGridType = MutableStateFlow("ROCKS") // "ROCKS", "TREES", or "GRASS"
    private val _rockGridCatalog = MutableStateFlow<RockGridCatalog?>(null)
    val rockGridCatalog: StateFlow<RockGridCatalog?> = _rockGridCatalog.asStateFlow()
    private val _treeGridCatalog = MutableStateFlow<RockGridCatalog?>(null)
    val treeGridCatalog: StateFlow<RockGridCatalog?> = _treeGridCatalog.asStateFlow()
    private val _grassGridCatalog = MutableStateFlow<RockGridCatalog?>(null)
    val grassGridCatalog: StateFlow<RockGridCatalog?> = _grassGridCatalog.asStateFlow()
    val selectedGridCell = MutableStateFlow<GridCellData?>(null)
    val gridFilterArchetype = MutableStateFlow<String>("ALL")

    // Advanced Procedural Noise & Erosion Simulation State
    val erosionData = MutableStateFlow(ErosionSimulationData())
    val isErosionRunning = MutableStateFlow(false)
    val triplanarEnabled = MutableStateFlow(true)

    // --- 3D SCENE VIEW STATE (10x10 Flat Ground with Multi-Object Noisy Distribution) ---
    val mainAppView = MutableStateFlow(MainAppView.TERRAIN_VIEW)
    val sceneMode = MutableStateFlow("ROCKS") // "ROCKS", "TREES", "MIXED", "GRASS"
    val sceneDensity = MutableStateFlow(24) // 12, 24, 36, 48 instances (for rocks, trees, mixed)
    val grassViewMode = MutableStateFlow(GrassViewMode.VARIETY) // Grass presentation: SINGLE or VARIETY
    val singleGrassSpecies = MutableStateFlow(GrassSpecies.KENTUCKY_BLUEGRASS)
    val singleGrassBlend = MutableStateFlow(GrassBlendType.LUSH_CHLOROPHYLL)
    val sceneShowGridLines = MutableStateFlow(true)
    val sceneShadingMode = MutableStateFlow("Solid PBR") // "Solid PBR", "Wireframe", "Clay Shaded"
    val sceneLightAngle = MutableStateFlow(0.85f)
    val selectedSceneInstance = MutableStateFlow<Scene3DObjectInstance?>(null)

    // Scene View Camera
    val sceneYaw = MutableStateFlow(0.78f)
    val scenePitch = MutableStateFlow(0.55f)
    val sceneZoom = MutableStateFlow(1.0f)
    val scenePanX = MutableStateFlow(0.0f)
    val scenePanY = MutableStateFlow(0.0f)
    val isSceneAutoRotate = MutableStateFlow(true)
    val sceneAutoRotateSpeed = MutableStateFlow(0.8f)

    private val _sceneInstances = MutableStateFlow<List<Scene3DObjectInstance>>(emptyList())
    val sceneInstances: StateFlow<List<Scene3DObjectInstance>> = _sceneInstances.asStateFlow()
    val isSceneLoading = MutableStateFlow(false)

    // --- C++ PROCEDURAL SCREEN STATE (Electric Grid, Posts, Garage & Vehicle) ---
    val selectedCppAsset = MutableStateFlow(CppAssetType.POWER_GRID_NETWORK)
    val cppActiveTab = MutableStateFlow("SPECS & BLUEPRINT")
    val cppYaw = MutableStateFlow(0.72f)
    val cppPitch = MutableStateFlow(0.30f)
    val cppZoom = MutableStateFlow(1.0f)
    val cppPanX = MutableStateFlow(0.0f)
    val cppPanY = MutableStateFlow(0.0f)
    val cppIsAutoRotate = MutableStateFlow(true)
    val cppShowGrid = MutableStateFlow(true)
    val cppShadingMode = MutableStateFlow("Lit Shaded")
    val cppExplodeFactor = MutableStateFlow(0.0f)
    val cppLightAngle = MutableStateFlow(0.85f)

    // Garage rigging controls
    val cppBay1OpenPct = MutableStateFlow(0.0f)
    val cppBay2OpenPct = MutableStateFlow(0.0f)
    val cppNeonSignOn = MutableStateFlow(true)

    // Vehicle lighting controls
    val cppHeadlightsOn = MutableStateFlow(true)
    val cppTaillightsOn = MutableStateFlow(true)

    // Modular Electric Post controls
    val postModularType = MutableStateFlow("CORNER") // "TANGENT", "CORNER", "TRANSFORMER", "DEAD_END"
    val postHasTransformer = MutableStateFlow(true)
    val postHasStreetlight = MutableStateFlow(true)
    val postStreetlightOn = MutableStateFlow(true)
    val postHasGuyWire = MutableStateFlow(true)

    // Power Grid Network controls (3 to 4 posts with at least one corner turn)
    val gridPostCount = MutableStateFlow(4) // 3 or 4 posts
    val gridSpanDistance = MutableStateFlow(16.0f)
    val gridCornerAngleDeg = MutableStateFlow(85.0f)
    val gridWireSagM = MutableStateFlow(0.55f)
    val gridEnergized = MutableStateFlow(true)
    val gridStreetlightsOn = MutableStateFlow(true)

    // Camera view angle & perspective projection
    val cppFovAngleDeg = MutableStateFlow(60.0f)
    val cppIsPerspective = MutableStateFlow(true)

    // Vulkan 1.3 Pipeline Engine State (Sascha Willems Architecture: https://github.com/SaschaWillems/Vulkan)
    // Hardware-native Vulkan 1.3 pipeline with dynamic rendering, VMA device-local buffers, and SPIR-V
    val renderingBackend = MutableStateFlow("Vulkan 1.3 (Sascha Willems Architecture)")
    val vulkanPolygonMode = MutableStateFlow("VK_POLYGON_MODE_FILL") // "VK_POLYGON_MODE_FILL", "VK_POLYGON_MODE_LINE"
    val vulkanMsaa = MutableStateFlow("VK_SAMPLE_COUNT_4_BIT") // "VK_SAMPLE_COUNT_1_BIT", "VK_SAMPLE_COUNT_4_BIT", "VK_SAMPLE_COUNT_8_BIT"
    val vulkanValidationLayers = MutableStateFlow(true)
    val vulkanDynamicRendering = MutableStateFlow(true) // VK_KHR_dynamic_rendering
    val vulkanPipelineCache = MutableStateFlow(true)
    val vulkanPushConstantSag = MutableStateFlow(true)

    // Cable Catenary Physics & 3D Cylinder Swept Tube State
    val cableGeometryMode = MutableStateFlow("CYLINDER") // "CYLINDER" vs "BOX"
    val cableCylinderSides = MutableStateFlow(8) // 6, 8, 12 radial facets
    val cableConductorRadiusMm = MutableStateFlow(35.0f) // in mm (10-60mm)
    val cableTensionKn = MutableStateFlow(18.5f) // horizontal tension T_H in kN
    val cableLinearMassKgM = MutableStateFlow(1.25f) // conductor unit mass w in kg/m

    // --- ROAD PROCEDURAL STUDIO STATE (Bridges, Tolls, Tunnels, Interchanges, Walls, Connected Scene) ---
    val roadSelectedCategory = MutableStateFlow("CONNECTED_SCENE") // "CONNECTED_SCENE", "BRIDGE", "TOLL_PLAZA", "TUNNEL", "INTERCHANGE", "RETAINING_WALL"
    
    // Bridge state
    val bridgeType = MutableStateFlow("CABLE_STAYED") // "CABLE_STAYED", "CONCRETE_VIADUCT", "STEEL_TRUSS", "PARABOLIC_ARCH"
    val bridgeSpanLengthM = MutableStateFlow(160.0f)
    val bridgeDeckWidthM = MutableStateFlow(24.0f)
    val bridgePylonHeightM = MutableStateFlow(58.0f)
    val bridgeCurveBendDeg = MutableStateFlow(0.0f)
    val bridgeSuperelevationDeg = MutableStateFlow(3.5f)
    val bridgeCablePairs = MutableStateFlow(12)
    val bridgeHasBarriers = MutableStateFlow(true)

    // Toll Plaza state
    val tollPlazaType = MutableStateFlow("CANOPY_PLAZA") // "CANOPY_PLAZA", "ETC_GANTRY", "CHECKPOINT", "RAMP_GATE"
    val tollLaneCount = MutableStateFlow(6)
    val tollLaneWidthM = MutableStateFlow(3.8f)
    val tollPlazaLengthM = MutableStateFlow(64.0f)
    val tollCanopyClearanceM = MutableStateFlow(5.5f)
    val tollHasVMS = MutableStateFlow(true)

    // Tunnel state
    val tunnelBoreType = MutableStateFlow("MOUNTAIN_HORSESHOE") // "MOUNTAIN_HORSESHOE", "CIRCULAR_BORED", "CUT_AND_COVER", "PORTAL_TRANSITION"
    val tunnelInternalRadiusM = MutableStateFlow(5.8f)
    val tunnelLengthM = MutableStateFlow(120.0f)
    val tunnelCurveBendDeg = MutableStateFlow(0.0f)
    val tunnelJetFanSpacingM = MutableStateFlow(35.0f)
    val tunnelHasFlaredPortal = MutableStateFlow(true)
    val tunnelHasJetFans = MutableStateFlow(true)

    // Interchange state
    val interchangeType = MutableStateFlow("CLOVERLEAF_LOOP") // "CLOVERLEAF_LOOP", "DIRECTIONAL_FLYOVER", "ROUNDABOUT"
    val interchangeLoopRadiusM = MutableStateFlow(55.0f)
    val interchangeTotalArcDeg = MutableStateFlow(270.0f)
    val interchangeRampWidthM = MutableStateFlow(7.5f)
    val interchangeElevationDeltaM = MutableStateFlow(7.5f)
    val interchangeBankingDeg = MutableStateFlow(6.0f)

    // Retaining Wall state
    val retainingWallType = MutableStateFlow("MSE_MODULAR") // "MSE_MODULAR", "STEPPED_GABION", "CANTILEVER_RC"
    val retainingWallHeightM = MutableStateFlow(6.5f)
    val retainingWallLengthM = MutableStateFlow(60.0f)
    val retainingWallBatterDeg = MutableStateFlow(4.0f)

    // 3D Viewport State for Road Studio
    val roadYaw = MutableStateFlow(0.78f)
    val roadPitch = MutableStateFlow(0.38f)
    val roadZoom = MutableStateFlow(1.0f)
    val roadPanX = MutableStateFlow(0.0f)
    val roadPanY = MutableStateFlow(0.0f)
    val roadIsAutoRotate = MutableStateFlow(true)
    val roadShadingMode = MutableStateFlow("Lit Shaded") // "Lit Shaded", "Wireframe", "Normals", "Cross-Section"
    val roadActiveTab = MutableStateFlow("3D VIEWPORT") // "3D VIEWPORT", "SIZE & BENDING CONTROLS", "C++ SOURCE CODE", "CROSS-SECTION"

    private val _roadMesh = MutableStateFlow(
        NativeEngineBridge.generateConnectedHighwayScene().toProceduralModelMesh()
    )
    val roadMesh: StateFlow<ProceduralModelMesh> = _roadMesh.asStateFlow()

    private val _cppMesh = MutableStateFlow(
        NativeEngineBridge.generatePowerGridMesh(
            numPosts = 4,
            spanDistance = 16.0f,
            cornerAngleDeg = 85.0f
        ).toProceduralModelMesh()
    )
    val cppMesh: StateFlow<ProceduralModelMesh> = _cppMesh.asStateFlow()

    init {
        loadData()
        startAutoRotateLoop()
        startSceneAutoRotateLoop()
        startCppAutoRotateLoop()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val items = repository.loadCatalog()
            _catalog.value = items
            val tree = repository.buildFolderTree()
            _folderTree.value = tree

            val rockGrid = repository.loadRockGridCatalog()
            _rockGridCatalog.value = rockGrid
            val treeGrid = repository.loadTreeGridCatalog()
            _treeGridCatalog.value = treeGrid
            val grassGrid = repository.loadGrassGridCatalog()
            _grassGridCatalog.value = grassGrid

            if (rockGrid != null && rockGrid.cells.isNotEmpty()) {
                selectedGridCell.value = rockGrid.cells.first()
            }

            if (items.isNotEmpty()) {
                selectAsset(items.first())
            }

            rebuildSceneInstances()
            _isLoading.value = false
        }
    }

    fun setMainAppView(view: MainAppView) {
        mainAppView.value = view
    }

    fun setSceneMode(mode: String) {
        sceneMode.value = mode
        rebuildSceneInstances()
    }

    fun setSceneDensity(density: Int) {
        sceneDensity.value = density
        rebuildSceneInstances()
    }

    fun setGrassViewMode(mode: GrassViewMode) {
        grassViewMode.value = mode
        if (mode == GrassViewMode.SINGLE) {
            sceneZoom.value = 2.4f
            scenePitch.value = 0.38f
        } else {
            sceneZoom.value = 1.0f
            scenePitch.value = 0.55f
        }
        rebuildSceneInstances()
    }

    fun setSingleGrassSpecies(species: GrassSpecies) {
        singleGrassSpecies.value = species
        if (grassViewMode.value == GrassViewMode.SINGLE) {
            rebuildSceneInstances()
        }
    }

    fun setSingleGrassBlend(blend: GrassBlendType) {
        singleGrassBlend.value = blend
        if (grassViewMode.value == GrassViewMode.SINGLE) {
            rebuildSceneInstances()
        }
    }

    fun selectSceneInstance(instance: Scene3DObjectInstance?) {
        selectedSceneInstance.value = instance
    }

    fun focusSceneInstanceInSingleView(instance: Scene3DObjectInstance) {
        val found = _catalog.value.find { it.folder == instance.folder || it.id == instance.folder }
        if (found != null) {
            selectAsset(found)
            mainAppView.value = MainAppView.SINGLE_ASSET_INSPECTOR
            _inspectionMode.value = InspectionMode.GLB_SHADED
        }
    }

    fun rebuildSceneInstances() {
        viewModelScope.launch {
            isSceneLoading.value = true
            val mode = sceneMode.value
            val targetCount = sceneDensity.value
            val rockCells = _rockGridCatalog.value?.cells ?: emptyList()
            val treeCells = _treeGridCatalog.value?.cells ?: emptyList()
            val grassCells = _grassGridCatalog.value?.cells ?: emptyList()

            // Handle Grass Single Specimen inspection mode
            if (mode == "GRASS" && grassViewMode.value == GrassViewMode.SINGLE) {
                val geomBroad = repository.getMeshGeometry("grass")
                val geomFine = repository.getMeshGeometry("grass_fine") ?: geomBroad
                val chosenSpecies = singleGrassSpecies.value
                val chosenBlend = singleGrassBlend.value
                val useFineModel = chosenSpecies.meshFolderKey == "grass_fine"
                val subGeom = if (useFineModel) geomFine else geomBroad
                val albedoBmp = if (useFineModel) null else repository.loadTextureBitmap("grass", "albedo.png")
                val meshRawHeight = 0.1003f
                val targetHeight = ((chosenSpecies.minHeightMeters + chosenSpecies.maxHeightMeters) * 0.5f).coerceIn(0.065f, 0.110f)
                val finalScaleY = targetHeight / meshRawHeight

                val hexColor = String.format(
                    "#%02X%02X%02X",
                    (chosenBlend.tipColor.red * 255).toInt(),
                    (chosenBlend.tipColor.green * 255).toInt(),
                    (chosenBlend.tipColor.blue * 255).toInt()
                )
                val heightCm = (targetHeight * 100f).let { Math.round(it * 10f) / 10f }
                val subTitle = "${chosenSpecies.commonName} • ${chosenBlend.blendName} (${heightCm}cm)"

                if (subGeom != null) {
                    val singleInst = Scene3DObjectInstance(
                        id = "single_grass_specimen",
                        sectorId = "SPECIMEN",
                        folder = if (useFineModel) "grass_fine" else "grass",
                        title = subTitle,
                        posX = 0f,
                        posY = 0f,
                        posZ = 0f,
                        scaleX = 1.0f,
                        scaleY = finalScaleY,
                        scaleZ = 1.0f,
                        rotationYaw = 0f,
                        accentColorHex = hexColor,
                        isTree = false,
                        isGrass = true,
                        geometry = subGeom,
                        albedoBitmap = albedoBmp,
                        grassSpecies = chosenSpecies,
                        grassBlendType = chosenBlend
                    )
                    selectedSceneInstance.value = singleInst
                    _sceneInstances.value = listOf(singleInst)
                } else {
                    _sceneInstances.value = emptyList()
                }
                isSceneLoading.value = false
                return@launch
            }

            val isGrassMode = mode == "GRASS"

            // In GRASS Variety mode: grass covers all 100 cells of the 10x10 field densely
            val selectedCells = when (mode) {
                "GRASS" -> {
                    // Full coverage of all 100 grid cells for millions of uncountable blades in the scene grid
                    grassCells
                }
                "TREES" -> treeCells.shuffled(java.util.Random(42)).take(targetCount)
                "MIXED" -> {
                    val third = targetCount / 3
                    val r = rockCells.shuffled(java.util.Random(101)).take(third)
                    val t = treeCells.shuffled(java.util.Random(202)).take(third)
                    val gCount = ((targetCount - 2 * third) * 2).coerceAtLeast(16)
                    val g = grassCells.shuffled(java.util.Random(303)).take(gCount)
                    r + t + g
                }
                else -> rockCells.shuffled(java.util.Random(77)).take(targetCount)
            }

            val instances = mutableListOf<Scene3DObjectInstance>()
            var instIndex = 0
            for (c in selectedCells) {
                val geom = repository.getMeshGeometry(c.archetypeId)
                if (geom != null) {
                    val isTree = c.isTree || c.archetypeId.contains("tree")
                    val isGrass = c.archetypeId.contains("grass")
                    val albedoBmp = repository.loadTextureBitmap(c.archetypeId, "albedo.png")

                    if (isGrass) {
                        // 1. Two 3D Grass Models:
                        // - Model 1: Broad Tuft ("grass")
                        // - Model 2: Fine Arched Blade Clump ("grass_fine")
                        val geomBroad = geom
                        val geomFine = repository.getMeshGeometry("grass_fine") ?: geomBroad

                        // 2. 5 Grass Species & 6 Botanical Blend Types
                        val speciesList = GrassSpecies.values()
                        val blendTypes = GrassBlendType.values()

                        // Mathematical Perlin-like pseudo-random noise function for biome clustering
                        val row = c.gridRow.toFloat()
                        val col = c.gridCol.toFloat()

                        // Multi-octave continuous spatial noise across 10m x 10m grid
                        val noise1 = sin(row * 0.73f + col * 0.47f) * cos(col * 0.81f - row * 0.35f)
                        val noise2 = sin(row * 1.45f - col * 1.13f + 1.7f) * 0.5f
                        val moistureNoise = (noise1 + noise2 + 1.5f) / 3.0f // 0.0 to 1.0 moisture gradient

                        // Select grass species based on spatial moisture & elevation noise (Constraints Algorithm)
                        val speciesIndex = when {
                            moistureNoise > 0.72f -> 0 // Kentucky Bluegrass (Poa pratensis) - lush moisture
                            moistureNoise > 0.52f -> 1 // Perennial Ryegrass (Lolium perenne)
                            moistureNoise > 0.36f -> 2 // Fine Fescue (Festuca rubra) - shade/arch
                            moistureNoise > 0.20f -> 3 // Tall Fescue (Schedonorus arundinaceus) - coarse
                            else -> 4                  // Bermudagrass Hybrid (Cynodon dactylon) - drought tolerant
                        }
                        val chosenSpecies = speciesList[speciesIndex]

                        // Overlapping sub-clusters per cell for dense interlocking lawn
                        val subClumps = if (isGrassMode) 4 else 2

                        val offsets = listOf(
                            Pair(0f, 0f),
                            Pair(0.24f, 0.22f),
                            Pair(-0.22f, -0.24f),
                            Pair(0.18f, -0.20f)
                        )

                        for (subIdx in 0 until subClumps) {
                            val off = offsets[subIdx]
                            // Sub-clump micro noise
                            val clumpHash = (c.gridRow * 43 + c.gridCol * 67 + subIdx * 109) and 0xFFFF
                            val microNoise = (clumpHash % 1000) / 1000f // 0.0 to 1.0

                            // Select between Model 1 (Broad) and Model 2 (Fine Arched)
                            val useFineModel = if (chosenSpecies.meshFolderKey == "grass_fine") {
                                microNoise > 0.25f // 75% fine, 25% broad understory
                            } else {
                                microNoise > 0.65f // 35% fine accent, 65% broad
                            }
                            val subGeom = if (useFineModel) geomFine else geomBroad
                            val meshRawHeight = 0.1003f

                            // Height constrained strictly between species min and max, strictly <= 0.11m (11cm)
                            val targetHeight = (chosenSpecies.minHeightMeters +
                                (chosenSpecies.maxHeightMeters - chosenSpecies.minHeightMeters) * (0.3f + 0.7f * microNoise)
                            ).coerceIn(0.065f, 0.110f)

                            val finalScaleY = targetHeight / meshRawHeight

                            // Blend selection: 6 botanical blends including brown weathered thatch
                            // ~12% naturally weathered brown thatch / senescent gold scattered dynamically
                            val chosenBlend = when {
                                (clumpHash % 19) == 0 -> GrassBlendType.WEATHERED_BROWN_THATCH // brown weathered
                                (clumpHash % 13) == 0 -> GrassBlendType.SENESCENT_GOLD         // senescent ochre
                                moistureNoise > 0.68f -> GrassBlendType.EMERALD_SPRING         // spring emerald
                                moistureNoise > 0.45f -> GrassBlendType.LUSH_CHLOROPHYLL       // lush chlorophyll
                                moistureNoise > 0.28f -> GrassBlendType.WARM_LIME              // warm lime
                                else -> GrassBlendType.OLIVE_SHADOW                            // olive shadow
                            }

                            val subRot = ((c.gridRow * 37 + c.gridCol * 53 + subIdx * 113) % 360) * (Math.PI.toFloat() / 180f)

                            // Natural clump horizontal scale factor (1.0x keeps botanical blade width at exact 2mm - 4.5mm)
                            val spreadScale = 0.95f + (microNoise * 0.10f)

                            val hexColor = String.format(
                                "#%02X%02X%02X",
                                (chosenBlend.tipColor.red * 255).toInt(),
                                (chosenBlend.tipColor.green * 255).toInt(),
                                (chosenBlend.tipColor.blue * 255).toInt()
                            )

                            val heightCm = (targetHeight * 100f).let { Math.round(it * 10f) / 10f }
                            val subTitle = "${chosenSpecies.commonName} • ${chosenBlend.blendName} (${heightCm}cm)"

                            val inst = Scene3DObjectInstance(
                                id = "inst_${c.sectorId}_$subIdx",
                                sectorId = c.sectorId,
                                folder = if (useFineModel) "grass_fine" else c.archetypeId,
                                title = subTitle,
                                posX = (c.worldPositionMeters.getOrNull(0) ?: 0f) + off.first,
                                posY = c.worldPositionMeters.getOrNull(1) ?: 0f,
                                posZ = (c.worldPositionMeters.getOrNull(2) ?: 0f) + off.second,
                                scaleX = spreadScale,
                                scaleY = finalScaleY,
                                scaleZ = spreadScale,
                                rotationYaw = subRot,
                                accentColorHex = hexColor,
                                isTree = false,
                                isGrass = true,
                                geometry = subGeom,
                                albedoBitmap = if (useFineModel) null else albedoBmp,
                                grassSpecies = chosenSpecies,
                                grassBlendType = chosenBlend
                            )
                            instances.add(inst)
                        }
                    } else {
                        val rot = ((c.gridRow * 37 + c.gridCol * 53) % 360) * (Math.PI.toFloat() / 180f)
                        val inst = Scene3DObjectInstance(
                            id = "inst_${c.sectorId}",
                            sectorId = c.sectorId,
                            folder = c.archetypeId,
                            title = c.rockTitle,
                            posX = c.worldPositionMeters.getOrNull(0) ?: 0f,
                            posY = c.worldPositionMeters.getOrNull(1) ?: 0f,
                            posZ = c.worldPositionMeters.getOrNull(2) ?: 0f,
                            scaleX = c.dimensionsMeters.getOrNull(0) ?: 0.8f,
                            scaleY = c.dimensionsMeters.getOrNull(1) ?: 0.7f,
                            scaleZ = c.dimensionsMeters.getOrNull(2) ?: 0.8f,
                            rotationYaw = rot,
                            accentColorHex = c.accentColor,
                            isTree = isTree,
                            isGrass = false,
                            geometry = geom,
                            albedoBitmap = albedoBmp
                        )
                        instances.add(inst)
                    }
                    instIndex++
                }
            }
            _sceneInstances.value = instances
            selectedSceneInstance.value = instances.firstOrNull()
            isSceneLoading.value = false
        }
    }

    fun onSceneRotateDrag(deltaX: Float, deltaY: Float) {
        sceneYaw.value = (sceneYaw.value + deltaX * 0.008f) % (2f * Math.PI.toFloat())
        scenePitch.value = (scenePitch.value + deltaY * 0.008f).coerceIn(0.05f, 1.52f)
    }

    fun onScenePanDrag(deltaX: Float, deltaY: Float) {
        scenePanX.value += deltaX * 0.006f
        scenePanY.value -= deltaY * 0.006f
    }

    fun onSceneZoomChange(zoomDelta: Float) {
        sceneZoom.value = (sceneZoom.value * zoomDelta).coerceIn(0.05f, 50.0f)
    }

    fun resetSceneCamera(preset: String = "Isometric") {
        when (preset) {
            "Top-Down" -> { sceneYaw.value = 0f; scenePitch.value = 1.50f }
            "Ground-Level" -> { sceneYaw.value = 0.5f; scenePitch.value = 0.12f }
            "Front" -> { sceneYaw.value = 0f; scenePitch.value = 0.35f }
            else -> { sceneYaw.value = 0.78f; scenePitch.value = 0.55f } // Isometric
        }
        sceneZoom.value = 1.0f
        scenePanX.value = 0f
        scenePanY.value = 0f
    }

    private fun startSceneAutoRotateLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (isSceneAutoRotate.value && mainAppView.value == MainAppView.SCENE_3D_VIEW) {
                    sceneYaw.value = (sceneYaw.value + 0.008f * sceneAutoRotateSpeed.value) % (2f * Math.PI.toFloat())
                }
                delay(30)
            }
        }
    }

    fun setGridType(type: String) {
        activeGridType.value = type
        val currentCatalog = if (type == "TREES") _treeGridCatalog.value else _rockGridCatalog.value
        selectedGridCell.value = currentCatalog?.cells?.firstOrNull()
        gridFilterArchetype.value = "ALL"
    }

    fun selectGridCell(cell: GridCellData) {
        selectedGridCell.value = cell
    }

    fun setGridFilterArchetype(filter: String) {
        gridFilterArchetype.value = filter
    }

    fun focusRockArchetype(archetypeId: String) {
        val found = _catalog.value.find { it.folder == archetypeId || it.id == archetypeId }
        if (found != null) {
            selectAsset(found)
            _inspectionMode.value = InspectionMode.GLB_SHADED
        }
    }

    fun selectAsset(item: AssetItem) {
        viewModelScope.launch {
            _selectedAsset.value = item
            val bundle = repository.loadFullAssetBundle(item)
            _assetBundle.value = bundle

            // Load texture bitmaps for the asset
            val texMap = mutableMapOf<String, Bitmap>()
            listOf("albedo.png", "normal.png", "roughness.png", "emissive.png", "volume_diagram.png").forEach { texName ->
                repository.loadTextureBitmap(item.folder, texName)?.let { bmp ->
                    texMap[texName] = bmp
                }
            }
            _textureBitmaps.value = texMap
            selectedVolumeShapeId.value = null
            explodeFactor.value = 0.0f

            // Reset camera to standard 3/4 isometric preview
            yaw.value = 0.5f
            pitch.value = 0.3f
            zoom.value = 1.0f
            panX.value = 0.0f
            panY.value = 0.0f
        }
    }

    fun selectVolumeShape(id: String?) {
        selectedVolumeShapeId.value = id
    }

    fun setExplodeFactor(factor: Float) {
        explodeFactor.value = factor.coerceIn(0.0f, 1.0f)
    }

    fun setInspectionMode(mode: InspectionMode) {
        _inspectionMode.value = mode
        if (mode == InspectionMode.VOLUME_DIAGRAM) {
            shadingMode.value = "Volume Shapes"
        }
    }

    fun onRotateDrag(deltaX: Float, deltaY: Float) {
        // Drag to orbit yaw and pitch
        yaw.value = (yaw.value + deltaX * 0.008f) % (2f * Math.PI.toFloat())
        pitch.value = (pitch.value + deltaY * 0.008f).coerceIn(-1.5f, 1.5f)
    }

    fun onPanDrag(deltaX: Float, deltaY: Float) {
        panX.value += deltaX * 0.005f
        panY.value -= deltaY * 0.005f
    }

    fun onZoomChange(zoomDelta: Float) {
        zoom.value = (zoom.value * zoomDelta).coerceIn(0.05f, 50.0f)
    }

    fun resetCamera(viewName: String = "Isometric") {
        when (viewName) {
            "Front" -> { yaw.value = 0f; pitch.value = 0f }
            "Top" -> { yaw.value = 0f; pitch.value = 1.57f }
            "Side" -> { yaw.value = 1.57f; pitch.value = 0f }
            else -> { yaw.value = 0.5f; pitch.value = 0.35f } // Isometric
        }
        zoom.value = 1.0f
        panX.value = 0f
        panY.value = 0f
    }

    private fun startAutoRotateLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (isAutoRotate.value) {
                    yaw.value = (yaw.value + 0.015f * autoRotateSpeed.value) % (2f * Math.PI.toFloat())
                }
                delay(30)
            }
        }
    }

    fun updateThermalAngle(angle: Float) {
        erosionData.value = erosionData.value.copy(thermalAngleDeg = angle)
    }

    fun updateHydraulicDroplets(droplets: Int) {
        erosionData.value = erosionData.value.copy(hydraulicDroplets = droplets)
    }

    fun updateDomainWarp(warp: Float) {
        erosionData.value = erosionData.value.copy(domainWarpAmplitude = warp)
    }

    fun updateRidgedOctaves(octaves: Int) {
        erosionData.value = erosionData.value.copy(ridgedFractureOctaves = octaves)
    }

    fun setLodLevel(level: Int) {
        erosionData.value = erosionData.value.copy(activeLodLevel = level)
    }

    fun triggerErosionSimulation() {
        viewModelScope.launch {
            isErosionRunning.value = true
            delay(500)
            isErosionRunning.value = false
        }
    }

    fun selectFileNode(node: FileNode) {
        previewFileNode.value = node
        viewModelScope.launch {
            if (!node.isDirectory) {
                try {
                    val text = getApplication<Application>().assets.open(node.path).bufferedReader().use { it.readText() }
                    previewFileContent.value = text
                } catch (e: Exception) {
                    previewFileContent.value = "// Binary file (${node.sizeBytes} bytes) - Preview not text"
                }
            } else {
                previewFileContent.value = null
            }
        }
    }

    // --- C++ Procedural Asset Methods ---
    fun selectCppAsset(assetType: CppAssetType) {
        selectedCppAsset.value = assetType
        rebuildCppMesh()
        resetCppCamera("Isometric")
    }

    fun setCppActiveTab(tab: String) {
        cppActiveTab.value = tab
    }

    fun setCppBay1OpenPct(pct: Float) {
        cppBay1OpenPct.value = pct.coerceIn(0f, 1f)
        rebuildCppMesh()
    }

    fun setCppBay2OpenPct(pct: Float) {
        cppBay2OpenPct.value = pct.coerceIn(0f, 1f)
        rebuildCppMesh()
    }

    fun toggleCppNeonSign() {
        cppNeonSignOn.value = !cppNeonSignOn.value
        rebuildCppMesh()
    }

    fun toggleCppHeadlights() {
        cppHeadlightsOn.value = !cppHeadlightsOn.value
        rebuildCppMesh()
    }

    fun toggleCppTaillights() {
        cppTaillightsOn.value = !cppTaillightsOn.value
        rebuildCppMesh()
    }

    // --- Modular Electric Post & Power Grid Mutator Methods ---
    fun setPostModularType(type: String) {
        postModularType.value = type
        if (type == "TRANSFORMER") postHasTransformer.value = true
        if (type == "CORNER" || type == "DEAD_END") postHasGuyWire.value = true
        rebuildCppMesh()
    }

    fun togglePostTransformer() {
        postHasTransformer.value = !postHasTransformer.value
        rebuildCppMesh()
    }

    fun togglePostStreetlight() {
        postHasStreetlight.value = !postHasStreetlight.value
        rebuildCppMesh()
    }

    fun togglePostStreetlightOn() {
        postStreetlightOn.value = !postStreetlightOn.value
        rebuildCppMesh()
    }

    fun togglePostGuyWire() {
        postHasGuyWire.value = !postHasGuyWire.value
        rebuildCppMesh()
    }

    fun setGridPostCount(count: Int) {
        gridPostCount.value = count.coerceIn(3, 4)
        rebuildCppMesh()
    }

    fun setGridCornerAngle(angle: Float) {
        gridCornerAngleDeg.value = angle.coerceIn(45f, 120f)
        rebuildCppMesh()
    }

    fun setGridWireSag(sag: Float) {
        gridWireSagM.value = sag.coerceIn(0.1f, 1.2f)
        rebuildCppMesh()
    }

    fun toggleGridEnergized() {
        gridEnergized.value = !gridEnergized.value
        rebuildCppMesh()
    }

    fun toggleGridStreetlightsOn() {
        gridStreetlightsOn.value = !gridStreetlightsOn.value
        rebuildCppMesh()
    }

    // --- Vulkan 1.3 Pipeline Engine Methods (Sascha Willems Architecture) ---
    fun setRenderingBackend(backend: String) {
        renderingBackend.value = backend
    }

    fun toggleRenderingBackend() {
        // Pipeline strictly locked to hardware-native Vulkan 1.3 (Sascha Willems Architecture)
        renderingBackend.value = "Vulkan 1.3 (Sascha Willems Architecture)"
    }

    fun setVulkanPolygonMode(mode: String) {
        vulkanPolygonMode.value = mode
    }

    fun setVulkanMsaa(msaa: String) {
        vulkanMsaa.value = msaa
    }

    fun toggleVulkanValidationLayers() {
        vulkanValidationLayers.value = !vulkanValidationLayers.value
    }

    fun toggleVulkanDynamicRendering() {
        vulkanDynamicRendering.value = !vulkanDynamicRendering.value
    }

    fun toggleVulkanPipelineCache() {
        vulkanPipelineCache.value = !vulkanPipelineCache.value
    }

    fun toggleVulkanPushConstantSag() {
        vulkanPushConstantSag.value = !vulkanPushConstantSag.value
    }

    // --- Cable Catenary Physics & 3D Cylinder Mutators ---
    fun setCableGeometryMode(mode: String) {
        cableGeometryMode.value = mode
        rebuildCppMesh()
    }

    fun toggleCableGeometryMode() {
        cableGeometryMode.value = if (cableGeometryMode.value == "CYLINDER") "BOX" else "CYLINDER"
        rebuildCppMesh()
    }

    fun setCableCylinderSides(sides: Int) {
        cableCylinderSides.value = sides
        rebuildCppMesh()
    }

    fun setCableConductorRadiusMm(radiusMm: Float) {
        cableConductorRadiusMm.value = radiusMm.coerceIn(10.0f, 60.0f)
        rebuildCppMesh()
    }

    fun setCableTensionKn(tensionKn: Float) {
        cableTensionKn.value = tensionKn.coerceIn(5.0f, 40.0f)
        // Exact physical sag equilibrium equation: S = (w * g * L^2) / (8 * T_H)
        val w = cableLinearMassKgM.value
        val g = 9.81f
        val l = gridSpanDistance.value
        val tNewtons = cableTensionKn.value * 1000.0f
        val calculatedSag = ((w * g * l * l) / (8.0f * tNewtons)).coerceIn(0.12f, 1.80f)
        gridWireSagM.value = calculatedSag
        rebuildCppMesh()
    }

    fun setCableLinearMassKgM(massKgM: Float) {
        cableLinearMassKgM.value = massKgM.coerceIn(0.40f, 3.50f)
        val w = cableLinearMassKgM.value
        val g = 9.81f
        val l = gridSpanDistance.value
        val tNewtons = cableTensionKn.value * 1000.0f
        val calculatedSag = ((w * g * l * l) / (8.0f * tNewtons)).coerceIn(0.12f, 1.80f)
        gridWireSagM.value = calculatedSag
        rebuildCppMesh()
    }

    fun rebuildCppMesh() {
        when (selectedCppAsset.value) {
            CppAssetType.GARAGE_001,
            CppAssetType.TEST_VEHICLE_LORRY,
            CppAssetType.MODULAR_POWER_POST -> {
                _cppMesh.value = NativeEngineBridge.generatePowerGridMesh(
                    numPosts = 1,
                    spanDistance = 0f,
                    cornerAngleDeg = 0f
                ).toProceduralModelMesh()
            }
            CppAssetType.POWER_GRID_NETWORK -> {
                _cppMesh.value = NativeEngineBridge.generatePowerGridMesh(
                    numPosts = gridPostCount.value,
                    spanDistance = gridSpanDistance.value,
                    cornerAngleDeg = gridCornerAngleDeg.value
                ).toProceduralModelMesh()
            }
            CppAssetType.MODULAR_BRIDGE -> {
                _cppMesh.value = NativeEngineBridge.generateModularBridgeMesh(
                    bridgeType = bridgeType.value,
                    spanLengthM = bridgeSpanLengthM.value,
                    deckWidthM = bridgeDeckWidthM.value,
                    pylonHeightM = bridgePylonHeightM.value,
                    curveBendAngleDeg = bridgeCurveBendDeg.value,
                    superelevationDeg = bridgeSuperelevationDeg.value,
                    cablePairsCount = bridgeCablePairs.value,
                    hasBarriers = bridgeHasBarriers.value
                ).toProceduralModelMesh()
            }
            CppAssetType.MODULAR_TOLL_PLAZA -> {
                _cppMesh.value = NativeEngineBridge.generateModularTollPlazaMesh(
                    tollType = tollPlazaType.value,
                    laneCount = tollLaneCount.value,
                    laneWidthM = tollLaneWidthM.value,
                    plazaLengthM = tollPlazaLengthM.value,
                    canopyClearanceM = tollCanopyClearanceM.value,
                    hasDynamicVMS = tollHasVMS.value
                ).toProceduralModelMesh()
            }
            CppAssetType.MODULAR_TUNNEL -> {
                _cppMesh.value = NativeEngineBridge.generateModularTunnelMesh(
                    tunnelType = tunnelBoreType.value,
                    internalRadiusM = tunnelInternalRadiusM.value,
                    tunnelLengthM = tunnelLengthM.value,
                    curveBendAngleDeg = tunnelCurveBendDeg.value,
                    jetFanSpacingM = tunnelJetFanSpacingM.value,
                    hasFlaredPortal = tunnelHasFlaredPortal.value,
                    hasJetFans = tunnelHasJetFans.value
                ).toProceduralModelMesh()
            }
            CppAssetType.MODULAR_INTERCHANGE -> {
                _cppMesh.value = NativeEngineBridge.generateModularInterchangeMesh(
                    interchangeType = interchangeType.value,
                    loopRadiusM = interchangeLoopRadiusM.value,
                    totalArcAngleDeg = interchangeTotalArcDeg.value,
                    rampWidthM = interchangeRampWidthM.value,
                    elevationDeltaM = interchangeElevationDeltaM.value,
                    superelevationBankingDeg = interchangeBankingDeg.value
                ).toProceduralModelMesh()
            }
            CppAssetType.MODULAR_RETAINING_WALL -> {
                _cppMesh.value = NativeEngineBridge.generateModularRetainingWallMesh(
                    wallType = retainingWallType.value,
                    wallHeightM = retainingWallHeightM.value,
                    wallLengthM = retainingWallLengthM.value,
                    wallBatterAngleDeg = retainingWallBatterDeg.value
                ).toProceduralModelMesh()
            }
            CppAssetType.CONNECTED_HIGHWAY_SCENE -> {
                _cppMesh.value = NativeEngineBridge.generateConnectedHighwayScene().toProceduralModelMesh()
            }
            CppAssetType.MODULAR_FOOTBRIDGE -> {
                _cppMesh.value = NativeEngineBridge.generateModularBridgeMesh(
                    bridgeType = "PEDESTRIAN_FOOTBRIDGE",
                    spanLengthM = 75f,
                    deckWidthM = 4.8f,
                    pylonHeightM = 22f
                ).toProceduralModelMesh()
            }
            CppAssetType.MODULAR_EXPRESSWAY -> {
                _cppMesh.value = NativeEngineBridge.generateModularExpresswayMesh().toProceduralModelMesh()
            }
            CppAssetType.MODULAR_SIGNPOST -> {
                _cppMesh.value = NativeEngineBridge.generateModularSignpostMesh().toProceduralModelMesh()
            }
            CppAssetType.MODULAR_BARRIER -> {
                _cppMesh.value = NativeEngineBridge.generateModularBarrierMesh().toProceduralModelMesh()
            }
            CppAssetType.MODULAR_ROAD_BLOCK -> {
                _cppMesh.value = NativeEngineBridge.generateModularRoadBlockMesh().toProceduralModelMesh()
            }
            CppAssetType.MODULAR_ROAD_NETWORK -> {
                _cppMesh.value = NativeEngineBridge.generateModularRoadNetworkMesh().toProceduralModelMesh()
            }
            CppAssetType.MODULAR_SPLINE_GRAPH -> {
                _cppMesh.value = NativeEngineBridge.generateModularSplineGraphMesh().toProceduralModelMesh()
            }
            CppAssetType.MODULAR_ROAD_TILE -> {
                _cppMesh.value = NativeEngineBridge.generateModularRoadTileMesh().toProceduralModelMesh()
            }
        }
    }

    fun rebuildRoadMesh() {
        when (roadSelectedCategory.value) {
            "BRIDGE" -> {
                _roadMesh.value = NativeEngineBridge.generateModularBridgeMesh(
                    bridgeType = bridgeType.value,
                    spanLengthM = bridgeSpanLengthM.value,
                    deckWidthM = bridgeDeckWidthM.value,
                    pylonHeightM = bridgePylonHeightM.value,
                    curveBendAngleDeg = bridgeCurveBendDeg.value,
                    superelevationDeg = bridgeSuperelevationDeg.value,
                    cablePairsCount = bridgeCablePairs.value,
                    hasBarriers = bridgeHasBarriers.value
                ).toProceduralModelMesh()
            }
            "TOLL_PLAZA" -> {
                _roadMesh.value = NativeEngineBridge.generateModularTollPlazaMesh(
                    tollType = tollPlazaType.value,
                    laneCount = tollLaneCount.value,
                    laneWidthM = tollLaneWidthM.value,
                    plazaLengthM = tollPlazaLengthM.value,
                    canopyClearanceM = tollCanopyClearanceM.value,
                    hasDynamicVMS = tollHasVMS.value
                ).toProceduralModelMesh()
            }
            "TUNNEL" -> {
                _roadMesh.value = NativeEngineBridge.generateModularTunnelMesh(
                    tunnelType = tunnelBoreType.value,
                    internalRadiusM = tunnelInternalRadiusM.value,
                    tunnelLengthM = tunnelLengthM.value,
                    curveBendAngleDeg = tunnelCurveBendDeg.value,
                    jetFanSpacingM = tunnelJetFanSpacingM.value,
                    hasFlaredPortal = tunnelHasFlaredPortal.value,
                    hasJetFans = tunnelHasJetFans.value
                ).toProceduralModelMesh()
            }
            "INTERCHANGE" -> {
                _roadMesh.value = NativeEngineBridge.generateModularInterchangeMesh(
                    interchangeType = interchangeType.value,
                    loopRadiusM = interchangeLoopRadiusM.value,
                    totalArcAngleDeg = interchangeTotalArcDeg.value,
                    rampWidthM = interchangeRampWidthM.value,
                    elevationDeltaM = interchangeElevationDeltaM.value,
                    superelevationBankingDeg = interchangeBankingDeg.value
                ).toProceduralModelMesh()
            }
            "RETAINING_WALL" -> {
                _roadMesh.value = NativeEngineBridge.generateModularRetainingWallMesh(
                    wallType = retainingWallType.value,
                    wallHeightM = retainingWallHeightM.value,
                    wallLengthM = retainingWallLengthM.value,
                    wallBatterAngleDeg = retainingWallBatterDeg.value
                ).toProceduralModelMesh()
            }
            "FOOTBRIDGE" -> {
                _roadMesh.value = NativeEngineBridge.generateModularBridgeMesh(
                    bridgeType = "PEDESTRIAN_FOOTBRIDGE",
                    spanLengthM = 75f,
                    deckWidthM = 4.8f,
                    pylonHeightM = 22f
                ).toProceduralModelMesh()
            }
            "EXPRESSWAY" -> {
                _roadMesh.value = NativeEngineBridge.generateModularExpresswayMesh().toProceduralModelMesh()
            }
            "SIGNPOST" -> {
                _roadMesh.value = NativeEngineBridge.generateModularSignpostMesh().toProceduralModelMesh()
            }
            "BARRIER" -> {
                _roadMesh.value = NativeEngineBridge.generateModularBarrierMesh().toProceduralModelMesh()
            }
            "ROAD_BLOCK" -> {
                _roadMesh.value = NativeEngineBridge.generateModularRoadBlockMesh().toProceduralModelMesh()
            }
            "ROAD_NETWORK" -> {
                _roadMesh.value = NativeEngineBridge.generateModularRoadNetworkMesh().toProceduralModelMesh()
            }
            "SPLINE_GRAPH" -> {
                _roadMesh.value = NativeEngineBridge.generateModularSplineGraphMesh().toProceduralModelMesh()
            }
            "ROAD_TILE" -> {
                _roadMesh.value = NativeEngineBridge.generateModularRoadTileMesh().toProceduralModelMesh()
            }
            else -> {
                _roadMesh.value = NativeEngineBridge.generateConnectedHighwayScene().toProceduralModelMesh()
            }
        }
    }

    fun onRoadRotateDrag(deltaX: Float, deltaY: Float) {
        roadYaw.value = (roadYaw.value + deltaX * 0.008f) % (2f * Math.PI.toFloat())
        roadPitch.value = (roadPitch.value + deltaY * 0.008f).coerceIn(0.05f, 1.50f)
    }

    fun onRoadPanDrag(deltaX: Float, deltaY: Float) {
        roadPanX.value += deltaX * 0.006f
        roadPanY.value -= deltaY * 0.006f
    }

    fun onRoadZoomChange(zoomDelta: Float) {
        roadZoom.value = (roadZoom.value * zoomDelta).coerceIn(0.05f, 50.0f)
    }

    fun resetRoadCamera(preset: String = "Isometric") {
        when (preset) {
            "Front" -> { roadYaw.value = 0.0f; roadPitch.value = 0.20f }
            "Side" -> { roadYaw.value = 1.57f; roadPitch.value = 0.20f }
            "Top" -> { roadYaw.value = 0.0f; roadPitch.value = 1.48f }
            else -> { roadYaw.value = 0.78f; roadPitch.value = 0.38f } // Isometric
        }
        roadZoom.value = 1.0f
        roadPanX.value = 0f
        roadPanY.value = 0f
    }

    fun onCppRotateDrag(deltaX: Float, deltaY: Float) {
        cppYaw.value = (cppYaw.value + deltaX * 0.008f) % (2f * Math.PI.toFloat())
        cppPitch.value = (cppPitch.value + deltaY * 0.008f).coerceIn(0.05f, 1.50f)
    }

    fun onCppPanDrag(deltaX: Float, deltaY: Float) {
        cppPanX.value += deltaX * 0.006f
        cppPanY.value -= deltaY * 0.006f
    }

    fun onCppZoomChange(zoomDelta: Float) {
        cppZoom.value = (cppZoom.value * zoomDelta).coerceIn(0.05f, 50.0f)
    }

    fun onCppFovChange(fovDeg: Float) {
        cppFovAngleDeg.value = fovDeg.coerceIn(30f, 90f)
    }

    fun toggleCppPerspective() {
        cppIsPerspective.value = !cppIsPerspective.value
    }

    fun resetCppCamera(preset: String = "Isometric") {
        when (selectedCppAsset.value) {
            CppAssetType.GARAGE_001 -> {
                when (preset) {
                    "Front" -> { cppYaw.value = -1.57f; cppPitch.value = 0.20f }
                    "Side" -> { cppYaw.value = 0.0f; cppPitch.value = 0.20f }
                    "Top" -> { cppYaw.value = -1.57f; cppPitch.value = 1.48f }
                    else -> { cppYaw.value = -1.15f; cppPitch.value = 0.36f }
                }
            }
            CppAssetType.TEST_VEHICLE_LORRY -> {
                when (preset) {
                    "Front" -> { cppYaw.value = 1.57f; cppPitch.value = 0.20f }
                    "Side" -> { cppYaw.value = 0.0f; cppPitch.value = 0.20f }
                    "Top" -> { cppYaw.value = 1.57f; cppPitch.value = 1.48f }
                    else -> { cppYaw.value = 0.95f; cppPitch.value = 0.34f }
                }
            }
            CppAssetType.MODULAR_POWER_POST -> {
                when (preset) {
                    "Front" -> { cppYaw.value = 0.0f; cppPitch.value = 0.16f }
                    "Side" -> { cppYaw.value = 1.57f; cppPitch.value = 0.16f }
                    "Top" -> { cppYaw.value = 0.0f; cppPitch.value = 1.48f }
                    else -> { cppYaw.value = 0.65f; cppPitch.value = 0.28f }
                }
            }
            CppAssetType.POWER_GRID_NETWORK -> {
                when (preset) {
                    "Front" -> { cppYaw.value = 0.0f; cppPitch.value = 0.28f }
                    "Side" -> { cppYaw.value = 1.57f; cppPitch.value = 0.28f }
                    "Top" -> { cppYaw.value = 0.55f; cppPitch.value = 1.48f }
                    else -> { cppYaw.value = 0.72f; cppPitch.value = 0.30f }
                }
            }
            CppAssetType.MODULAR_BRIDGE,
            CppAssetType.MODULAR_TOLL_PLAZA,
            CppAssetType.MODULAR_TUNNEL,
            CppAssetType.MODULAR_INTERCHANGE,
            CppAssetType.MODULAR_RETAINING_WALL,
            CppAssetType.CONNECTED_HIGHWAY_SCENE,
            CppAssetType.MODULAR_FOOTBRIDGE,
            CppAssetType.MODULAR_EXPRESSWAY,
            CppAssetType.MODULAR_SIGNPOST,
            CppAssetType.MODULAR_BARRIER,
            CppAssetType.MODULAR_ROAD_BLOCK,
            CppAssetType.MODULAR_ROAD_NETWORK,
            CppAssetType.MODULAR_SPLINE_GRAPH,
            CppAssetType.MODULAR_ROAD_TILE -> {
                when (preset) {
                    "Front" -> { cppYaw.value = 0.0f; cppPitch.value = 0.20f }
                    "Side" -> { cppYaw.value = 1.57f; cppPitch.value = 0.20f }
                    "Top" -> { cppYaw.value = 0.0f; cppPitch.value = 1.48f }
                    else -> { cppYaw.value = 0.78f; cppPitch.value = 0.38f }
                }
            }
        }
        cppZoom.value = 1.0f
        cppPanX.value = 0f
        cppPanY.value = 0f
    }

    private fun startCppAutoRotateLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (cppIsAutoRotate.value && mainAppView.value == MainAppView.CPP_PROCEDURAL_VIEW) {
                    cppYaw.value = (cppYaw.value + 0.012f) % (2f * Math.PI.toFloat())
                }
                if (roadIsAutoRotate.value && mainAppView.value == MainAppView.ROAD_VIEW) {
                    roadYaw.value = (roadYaw.value + 0.008f) % (2f * Math.PI.toFloat())
                }
                delay(30)
            }
        }
    }
}
