package com.example.model

/**
 * Geographic, 1m-grid metric, and UTM Zone 37S coordinate point.
 */
data class MagadiGeoCoord(
    val lat: Double,
    val lon: Double,
    val xM: Float,      // Local metric Easting from West boundary (0 .. 3000m)
    val yM: Float,      // Local metric Northing from South boundary (0 .. 5000m)
    val utmEast: Double,
    val utmNorth: Double,
    val cellCol: Int = xM.toInt().coerceIn(0, 2999),
    val cellRow: Int = yM.toInt().coerceIn(0, 4999)
)

data class MagadiTownBounds(
    val centerLat: Double = -1.9012,
    val centerLon: Double = 36.2870,
    val widthM: Double = 3000.0,
    val heightM: Double = 5000.0,
    val areaKm2: Double = 15.0,
    val gridResolutionM: Double = 1.0,
    val gridCols: Int = 3000,
    val gridRows: Int = 5000,
    val totalGridCells: Long = 15_000_000L,
    val minLat: Double = -1.92381,
    val maxLat: Double = -1.87859,
    val minLon: Double = 36.27352,
    val maxLon: Double = 36.30048,
    val originUtmEast: Double = 196600.0,
    val originUtmNorth: Double = 9787000.0
)

data class ExtractedRoadWay(
    val osmId: Long,
    val name: String,
    val highwayType: String,
    val surface: String,
    val lanes: Int,
    val oneWay: Boolean,
    val widthM: Float,
    val lengthM: Float,
    val nodes: List<MagadiGeoCoord>,
    val tags: Map<String, String> = emptyMap(),
    val ref: String = "",
    val isBridge: Boolean = false,
    val isRoundabout: Boolean = false
)

data class ExtractedBuilding(
    val osmId: Long,
    val name: String,
    val buildingType: String,
    val amenity: String,
    val levels: Int,
    val heightM: Float,
    val centroid: MagadiGeoCoord,
    val elevM: Float,
    val footprintAreaM2: Float,
    val perimeterM: Float,
    val polygon: List<MagadiGeoCoord>,
    val tags: Map<String, String> = emptyMap(),
    // v2 Architectural Envelope & Compound Containment
    val floorHeightM: Float = if (levels > 0) heightM / levels else heightM,
    val shapeType: String = "RECTANGULAR",
    val roofType: String = "GABLED",
    val roofPitchDeg: Float = 20f,
    val roofMaterial: String = "CORRUGATED_IRON_MABATI",
    val wallType: String = "CONCRETE_BLOCK_PLASTER",
    val windowType: String = "SLIDING_ALUMINUM",
    val windowToWallRatio: Float = 0.20f,
    val parentCompoundId: Long = 0L,
    val parentCompoundName: String = "",
    // v2 Road-Collision-Free Perimeter Fence & Gate Coordinates
    val hasFence: Boolean = false,
    val fenceType: String = "NONE",
    val fenceHeightM: Float = 0f,
    val fencePolygon: List<MagadiGeoCoord> = emptyList(),
    val hasGate: Boolean = false,
    val gateType: String = "NONE",
    val gateWidthM: Float = 0f,
    val gateAngleRad: Float = 0f,
    val gateCoord: MagadiGeoCoord? = null
)

object BuildingV2Catalog {
    val SHAPE_TYPES = arrayOf(
        "SQUARE", "RECTANGULAR", "L_SHAPE", "U_SHAPE",
        "T_SHAPE", "STAR_CROSS", "CIRCULAR_SILO", "POLYGON_IRREGULAR"
    )
    val ROOF_TYPES = arrayOf(
        "GABLED", "HIPPED", "FLAT_PARAPET", "SAW_TOOTH_INDUSTRIAL",
        "MONO_PITCH_SHED", "ARCHED_TRUSS", "CONICAL_DOME"
    )
    val ROOF_MATERIALS = arrayOf(
        "CORRUGATED_IRON_MABATI", "STONE_COATED_TILE", "REINFORCED_CONCRETE",
        "GALVANIZED_TRUSS_SHEET", "CLAY_TILE", "THATCH_MAASAI"
    )
    val WALL_TYPES = arrayOf(
        "CONCRETE_BLOCK_PLASTER", "CORRUGATED_IRON_CLADDING", "DRESSED_MASONRY_STONE",
        "PRECAST_INDUSTRIAL_PANEL", "TIMBER_PLANK", "BRICK_MASONRY"
    )
    val WINDOW_TYPES = arrayOf(
        "SLIDING_ALUMINUM", "STEEL_CASEMENT", "INDUSTRIAL_CLERESTORY",
        "LOUVERED_VENT", "CURTAIN_WALL", "NARROW_SLIT"
    )
    val FENCE_TYPES = arrayOf(
        "NONE", "CHAIN_LINK_POSTS", "MASONRY_BLOCK_WALL",
        "TIMBER_POST_RAIL", "LIVE_THORN_HEDGE", "PALISADE_SECURITY_STEEL"
    )
    val GATE_TYPES = arrayOf(
        "NONE", "SLIDING_STEEL_GATE", "DOUBLE_SWING_TIMBER",
        "SECURITY_BOOM_BARRIER", "PEDESTRIAN_WICKET"
    )

    fun shapeAt(idx: Int): String = SHAPE_TYPES[idx.coerceIn(0, SHAPE_TYPES.lastIndex)]
    fun roofAt(idx: Int): String = ROOF_TYPES[idx.coerceIn(0, ROOF_TYPES.lastIndex)]
    fun roofMatAt(idx: Int): String = ROOF_MATERIALS[idx.coerceIn(0, ROOF_MATERIALS.lastIndex)]
    fun wallAt(idx: Int): String = WALL_TYPES[idx.coerceIn(0, WALL_TYPES.lastIndex)]
    fun windowAt(idx: Int): String = WINDOW_TYPES[idx.coerceIn(0, WINDOW_TYPES.lastIndex)]
    fun fenceAt(idx: Int): String = FENCE_TYPES[idx.coerceIn(0, FENCE_TYPES.lastIndex)]
    fun gateAt(idx: Int): String = GATE_TYPES[idx.coerceIn(0, GATE_TYPES.lastIndex)]
}

data class ExtractedPoiMetadata(
    val osmId: Long,
    val name: String,
    val category: String,
    val coord: MagadiGeoCoord,
    val elevM: Float,
    val tags: Map<String, String> = emptyMap()
)

data class ExtractedWaterPolygon(
    val osmId: Long,
    val name: String,
    val waterType: String,
    val polygon: List<MagadiGeoCoord>,
    val meanElevM: Float
)

data class DemControlGrid(
    val source: String,
    val nativeResolutionM: Float = 9.5f,
    val cols: Int,
    val rows: Int,
    val elevationsM: FloatArray,
    val minElevM: Float,
    val maxElevM: Float,
    val meanElevM: Float
)

data class MagadiTownMetadata(
    val placeName: String = "Magadi Town, Kajiado County, Kenya",
    val crsWgs84: String = "EPSG:4326 (WGS84)",
    val crsProjected: String = "EPSG:32737 (UTM Zone 37S)",
    val extractedAtIso: String = "",
    val osmSourceUrl: String = "",
    val demSourceUrl: String = "",
    val demResolutionM: Float = 9.5f,
    val lakeLevelAslM: Float = 602.5f,
    val isLiveExtracted: Boolean = false,
    val roadCount: Int = 0,
    val totalRoadLengthKm: Float = 0f,
    val buildingCount: Int = 0,
    val totalBuildingFootprintM2: Float = 0f,
    val waterCount: Int = 0,
    val poiCount: Int = 0,
    val elevMinM: Float = 595f,
    val elevMaxM: Float = 720f,
    val elevMeanM: Float = 625f,
    val isBinaryServed: Boolean = false
)

data class MagadiTownBundle(
    val bounds: MagadiTownBounds,
    val metadata: MagadiTownMetadata,
    val demGrid: DemControlGrid?,
    val roads: List<ExtractedRoadWay>,
    val buildings: List<ExtractedBuilding>,
    val pois: List<ExtractedPoiMetadata>,
    val waterPolygons: List<ExtractedWaterPolygon> = emptyList()
)

sealed interface MagadiExtractionUiState {
    data object Idle : MagadiExtractionUiState
    data class Extracting(val stage: String, val progress: Float) : MagadiExtractionUiState
    data class Ready(val bundle: MagadiTownBundle, val isFromCache: Boolean) : MagadiExtractionUiState
    data class Error(val message: String, val cachedBundle: MagadiTownBundle? = null) : MagadiExtractionUiState
}

data class Inspected1mCell(
    val xM: Float,
    val yM: Float,
    val cellCol: Int,
    val cellRow: Int,
    val lat: Double,
    val lon: Double,
    val utmEast: Double,
    val utmNorth: Double,
    val elevM: Float,
    val slopeDeg: Float,
    val waterDepthM: Float = 0f,
    val isWater: Boolean = false
)

