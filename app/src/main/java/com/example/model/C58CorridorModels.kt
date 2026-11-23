package com.example.model

import kotlin.math.abs

/**
 * Coordinate projected into WGS84, UTM Zone 37S (EPSG:32737),
 * and Route C58 curvilinear corridor space (chainage along road, signed lateral offset L/R).
 */
data class C58Coord(
    val lat: Double,
    val lon: Double,
    val utmEast: Double,
    val utmNorth: Double,
    val chainageM: Double,
    val lateralOffsetM: Float, // negative = LEFT (-1500..0m), positive = RIGHT (0..+1500m)
    val insideCorridor: Boolean = abs(lateralOffsetM) <= 1500f
) {
    val chainageKm: Float get() = (chainageM / 1000.0).toFloat()
    val corridorSide: String get() = if (lateralOffsetM < 0f) "LEFT (-1.5km)" else "RIGHT (+1.5km)"
    val cellCol1m: Int get() = kotlin.math.floor(utmEast).toInt()
    val cellRow1m: Int get() = kotlin.math.floor(utmNorth).toInt()
}

data class C58CorridorSummary(
    val totalLengthM: Double = 101_000.0,
    val halfWidthM: Float = 1500.0f,
    val centerlinePoints: Int = 66,
    val minUtmEast: Double = 195_000.0,
    val maxUtmEast: Double = 256_000.0,
    val minUtmNorth: Double = 9_787_000.0,
    val maxUtmNorth: Double = 9_853_000.0,
    val minLat: Double = -1.916,
    val maxLat: Double = -1.320,
    val minLon: Double = 36.270,
    val maxLon: Double = 36.782
) {
    val totalLengthKm: Float get() = (totalLengthM / 1000.0).toFloat()
    val fullWidthKm: Float get() = (halfWidthM * 2f) / 1000f
    val corridorAreaKm2: Float get() = totalLengthKm * fullWidthKm
}

data class C58RibbonVertex(
    val centerE: Double,
    val centerN: Double,
    val leftE: Double,
    val leftN: Double,
    val rightE: Double,
    val rightN: Double,
    val chainageM: Double
)

data class C58SectorSpec(
    val index: Int, // 1..10 (0 means FULL C58 CORRIDOR)
    val shortLabel: String,
    val title: String,
    val startChainageM: Double,
    val endChainageM: Double
)

data class C58ExtractedRoad(
    val osmId: Long,
    val name: String,
    val highwayType: String,
    val surface: String,
    val lanes: Int,
    val widthM: Float,
    val lengthM: Float,
    val sectorIndex: Int,
    val midCoord: C58Coord,
    val nodes: List<C58Coord>,
    val tags: Map<String, String> = emptyMap(),
    val ref: String = "",
    val oneWay: Boolean = false,
    val isBridge: Boolean = false,
    val isRoundabout: Boolean = false
)

data class C58ExtractedHouse(
    val osmId: Long,
    val name: String,
    val buildingType: String,
    val amenity: String,
    val levels: Int,
    val heightM: Float,
    val centroid: C58Coord,
    val elevM: Float,
    val footprintAreaM2: Float,
    val perimeterM: Float,
    val sectorIndex: Int,
    val polygon: List<C58Coord>,
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
    val fencePolygon: List<C58Coord> = emptyList(),
    val hasGate: Boolean = false,
    val gateType: String = "NONE",
    val gateWidthM: Float = 0f,
    val gateAngleRad: Float = 0f,
    val gateCoord: C58Coord? = null
)

data class C58ExtractedPoi(
    val osmId: Long,
    val name: String,
    val category: String,
    val coord: C58Coord,
    val elevM: Float,
    val sectorIndex: Int,
    val tags: Map<String, String> = emptyMap()
)

data class C58WaterPolygon(
    val osmId: Long,
    val name: String,
    val waterType: String,
    val polygon: List<C58Coord>,
    val meanElevM: Float
)

data class C58DemGrid(
    val source: String,
    val nativeResolutionM: Float = 19.0f,
    val uCols: Int, // Lateral across -1500m .. +1500m
    val vRows: Int, // Longitudinal along 0 .. totalLengthM
    val elevationsM: FloatArray,
    val minElevM: Float,
    val maxElevM: Float,
    val meanElevM: Float
)

data class C58CorridorMetadata(
    val routeName: String = "Route C58 (Magadi Road • ±1.5 km Non-Box Corridor)",
    val crsWgs84: String = "EPSG:4326 (WGS84)",
    val crsProjected: String = "EPSG:32737 (UTM Zone 37S)",
    val corridorWidthKm: Float = 3.0f,
    val halfWidthLeftM: Float = 1500f,
    val halfWidthRightM: Float = 1500f,
    val isLiveOsmCenterline: Boolean = false,
    val extractedSectors: List<Int> = emptyList(),
    val extractedAtIso: String = "",
    val osmSourceUrl: String = "",
    val demSourceUrl: String = "",
    val demResolutionM: Float = 19.0f,
    val lakeLevelAslM: Float = 602.5f,
    val roadCount: Int = 0,
    val totalRoadLengthKm: Float = 0f,
    val houseCount: Int = 0,
    val leftSideHouseCount: Int = 0,
    val rightSideHouseCount: Int = 0,
    val totalHouseAreaM2: Float = 0f,
    val waterCount: Int = 0,
    val poiCount: Int = 0,
    val elevMinM: Float = 596f,
    val elevMaxM: Float = 1820f,
    val elevMeanM: Float = 1240f,
    val isBinaryServed: Boolean = false
)

data class C58CorridorBundle(
    val summary: C58CorridorSummary,
    val metadata: C58CorridorMetadata,
    val centerlineLatLon: List<Pair<Double, Double>>,
    val ribbon: List<C58RibbonVertex>,
    val sectors: List<C58SectorSpec>,
    val demGrid: C58DemGrid?,
    val roads: List<C58ExtractedRoad>,
    val houses: List<C58ExtractedHouse>,
    val pois: List<C58ExtractedPoi>,
    val waterPolygons: List<C58WaterPolygon> = emptyList()
)

sealed interface C58ExtractionUiState {
    data object Idle : C58ExtractionUiState
    data class Extracting(val stage: String, val progress: Float) : C58ExtractionUiState
    data class Ready(val bundle: C58CorridorBundle, val isFromCache: Boolean) : C58ExtractionUiState
    data class Error(val message: String, val cachedBundle: C58CorridorBundle? = null) : C58ExtractionUiState
}

data class C58InspectedCell(
    val lat: Double,
    val lon: Double,
    val utmEast: Double,
    val utmNorth: Double,
    val chainageM: Double,
    val lateralOffsetM: Float,
    val insideCorridor: Boolean,
    val elevM: Float,
    val slopeDeg: Float,
    val waterDepthM: Float = 0f,
    val isWater: Boolean = false
) {
    val chainageKm: Float get() = (chainageM / 1000.0).toFloat()
    val sideLabel: String
        get() = when {
            !insideCorridor -> "OUTSIDE ±1.5km RIBBON"
            lateralOffsetM < 0f -> "${kotlin.math.abs(lateralOffsetM).toInt()}m LEFT (-1.5km)"
            else -> "${lateralOffsetM.toInt()}m RIGHT (+1.5km)"
        }
}
