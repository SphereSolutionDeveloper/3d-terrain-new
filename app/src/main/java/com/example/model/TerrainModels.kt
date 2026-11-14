package com.example.model

/**
 * Procedural Terrain System Models
 * Matches the C++ Vulkan 1.3 architecture in native-assets/src/terrain/
 */
enum class TerrainRenderMode(val title: String, val description: String) {
    SHADED_TRIPLANAR("Realistic Splat PBR", "Slope & elevation biomes: grass valley, dirt hills, rock cliffs, snow peaks"),
    CONTOURS_3D_ELEVATION("3D Contour Isolines", "Real-time topographic contour isolines (25m/50m/100m) directly on 3D terrain"),
    HYPSOMETRIC_CONTOURS("Hypsometric Elevation Tints", "Topographic elevation bands: Lake Magadi (590m) to Ngong Summit (1980m)"),
    HEIGHTMAP_HEATMAP("Elevation Heatmap", "Color gradients by altitude with topographic contour lines"),
    WIREFRAME_CDLOD("CDLOD Tessellation", "Continuous distance-dependent LOD quadtree wireframe with morphing"),
    SLOPE_ANALYSIS("Slope & Aspect Angle", "Visualizes steepness angles for cliff strata & talus rock deposits"),
    CAVE_VOXEL_SLICE("Marching Cubes 3D Caves", "Subterranean 3D voxel density isosurface & cavern tunnels"),
    ROAD_CUT_EMBANKMENT("Road Corridor Cut & Fill", "Spline road cut, embankment slopes, and retaining wall geometry")
}

enum class TerrainSubsystemTab(val label: String, val iconName: String) {
    KENYA_MAGADI_50KM("Magadi Rd Chunks", "explore"),
    VULKAN_CPP_PIPELINE("Vulkan C++ Terrain", "code"),
    GAME_TERRAIN_001("Test Ground 001", "sports_esports"),
    HEIGHTMAP_NOISE("Height & Noise", "terrain"),
    EROSION_SIM("Erosion Dynamics", "water_drop"),
    SPLAT_BIOMES("Splat Biomes", "palette"),
    CLIFFS_STRATA("Cliffs & Strata", "landscape"),
    ROAD_CUTS("Road Cuts", "alt_route"),
    CAVES_VOXELS("3D Caves", "cave"),
    CDLOD_SHADERS("CDLOD & Shaders", "memory"),
    CPP_SOURCE_TREE("C++ Source Tree", "terminal")
}

data class TerrainState(
    val seed: Long = 1337L,
    val octaves: Int = 5,
    val persistence: Float = 0.5f,
    val lacunarity: Float = 2.0f,
    val heightScale: Float = 42.0f,
    val domainWarpStrength: Float = 0.35f,
    val ridgedMultifractal: Boolean = false,
    val rainDropletCount: Int = 1000,
    val erosionRate: Float = 0.3f,
    val talusAngleDeg: Float = 33.0f,
    val grassElevationMax: Float = 15.0f,
    val dirtElevationMax: Float = 28.0f,
    val cliffSlopeThresholdDeg: Float = 38.0f,
    val snowElevationMin: Float = 34.0f,
    val caveThreshold: Float = 0.58f,
    val caveTunnelRadius: Float = 3.5f,
    val roadWidth: Float = 6.0f,
    val roadCutSlope: Float = 45.0f,
    val cdlodTileResolution: Int = 65,
    val cdlodMorphFactor: Float = 0.5f,
    val contourInterval: Float = 25.0f,
    val indexContourInterval: Float = 100.0f,
    val stepTerracingFactor: Float = 0.0f,
    val showContourLines: Boolean = true,
    val showGroundPiece: Boolean = true,
    val groundPieceDatumM: Float = 400.0f
)

/**
 * Solid Geological Ground Piece Foundation Metrics (400m ASL Baseline Datum)
 * Evaluated directly from C++ Vulkan 1.3 engine (MagadiSceneEngine.cpp)
 */
data class GroundPieceMetrics(
    val datumASLM: Float = 400.0f,
    val depthMinM: Float = 198.0f,
    val depthMaxM: Float = 1580.0f,
    val depthRoadStartM: Float = 198.0f,
    val depthRoadEndM: Float = 240.0f,
    val verticalReliefM: Float = 1382.0f
)

data class TerrainVertex(
    val x: Float,
    val y: Float,
    val z: Float,
    val nx: Float = 0f,
    val ny: Float = 1f,
    val nz: Float = 0f,
    val slopeDeg: Float = 0f,
    val grassWeight: Float = 1f,
    val dirtWeight: Float = 0f,
    val rockWeight: Float = 0f,
    val snowWeight: Float = 0f,
    val isCaveHollow: Boolean = false,
    val isRoadSurface: Boolean = false,
    val surfaceType: String = "grass",
    val isWaterSurface: Boolean = false,
    val isMudSurface: Boolean = false,
    val isGravelSurface: Boolean = false,
    val rawElevation: Float = 0f,
    val contourBand: Float = 0f,
    val isolineFactor: Float = 0f
)

data class GameTerrainAssetPlacement(
    val id: String,
    val type: String,
    val glbPath: String,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    val rotY: Float,
    val scale: Float
)

data class RoadWaypoint3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val rawElevation: Float,
    val forwardM: Float,
    val globalKmM: Float,
    val lateralM: Float,
    val gradePercent: Float,
    val headingDeg: Float,
    val naturalElevM: Float = 0f,
    val cutFillDepthM: Float = 0f,
    val nextContourElevM: Float = 0f,
    val distToNextContourM: Float = 0f
)

data class TerrainMesh3D(
    val resolutionX: Int,
    val resolutionZ: Int,
    val vertices: List<TerrainVertex>,
    val indices: List<Int>,
    val minHeight: Float,
    val maxHeight: Float,
    val avgSlope: Float,
    val assetPlacements: List<GameTerrainAssetPlacement> = emptyList(),
    val roadCenterline: List<RoadWaypoint3D> = emptyList()
)

data class NativeFileTreeItem(
    val path: String,
    val name: String,
    val category: String,
    val lines: Int,
    val description: String,
    val codeSnippet: String
)

data class TerrainBuilding3D(
    val id: Long,
    val label: String,
    val baseCornersXZ: List<Pair<Float, Float>>,
    val baseY: Float,
    val roofY: Float,
    val isSelected: Boolean = false
)

data class TerrainRoadSeg3D(
    val x0: Float,
    val y0: Float,
    val z0: Float,
    val x1: Float,
    val y1: Float,
    val z1: Float,
    val isPrimary: Boolean = false,
    val isSelected: Boolean = false
)

