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
    val tags: Map<String, String> = emptyMap()
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
    val tags: Map<String, String> = emptyMap()
)

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
    val elevMeanM: Float = 625f
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

