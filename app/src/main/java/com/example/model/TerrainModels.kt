package com.example.model

/**
 * Procedural Terrain System Models
 * Matches the C++ Vulkan 1.3 architecture in native-assets/src/terrain/
 */
enum class TerrainRenderMode(val title: String, val description: String) {
    SHADED_TRIPLANAR("Realistic Splat PBR", "Slope & elevation biomes: grass valley, dirt hills, rock cliffs, snow peaks"),
    HEIGHTMAP_HEATMAP("Elevation Heatmap", "Color gradients by altitude with topographic contour lines"),
    WIREFRAME_CDLOD("CDLOD Tessellation", "Continuous distance-dependent LOD quadtree wireframe with morphing"),
    SLOPE_ANALYSIS("Slope & Aspect Angle", "Visualizes steepness angles for cliff strata & talus rock deposits"),
    CAVE_VOXEL_SLICE("Marching Cubes 3D Caves", "Subterranean 3D voxel density isosurface & cavern tunnels"),
    ROAD_CUT_EMBANKMENT("Road Corridor Cut & Fill", "Spline road cut, embankment slopes, and retaining wall geometry")
}

enum class TerrainSubsystemTab(val label: String, val iconName: String) {
    KENYA_MAGADI_50KM("Magadi Rd Area", "explore"),
    GAME_TERRAIN_001("Test Ground 001", "sports_esports"),
    HEIGHTMAP_NOISE("Height & Noise", "terrain"),
    EROSION_SIM("Erosion Dynamics", "water_drop"),
    SPLAT_BIOMES("Splat Biomes", "palette"),
    CLIFFS_STRATA("Cliffs & Strata", "landscape"),
    ROAD_CUTS("Road Cuts", "alt_route"),
    CAVES_VOXELS("3D Caves", "cave"),
    CDLOD_SHADERS("CDLOD & Shaders", "memory"),
    CPP_SOURCE_TREE("C++ Source Tree", "code")
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
    val cdlodMorphFactor: Float = 0.5f
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
    val isGravelSurface: Boolean = false
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

data class TerrainMesh3D(
    val resolutionX: Int,
    val resolutionZ: Int,
    val vertices: List<TerrainVertex>,
    val indices: List<Int>,
    val minHeight: Float,
    val maxHeight: Float,
    val avgSlope: Float,
    val assetPlacements: List<GameTerrainAssetPlacement> = emptyList()
)

data class NativeFileTreeItem(
    val path: String,
    val name: String,
    val category: String,
    val lines: Int,
    val description: String,
    val codeSnippet: String
)
