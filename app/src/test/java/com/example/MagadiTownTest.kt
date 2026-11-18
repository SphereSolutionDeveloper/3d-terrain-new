package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.repository.MagadiTownRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MagadiTownTest {

    @Test
    fun `magadi town bounds match exact 3x5 km domain at 1m grid resolution`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = MagadiTownRepository(context)
        val bounds = repo.getNativeBounds()

        assertEquals(3000.0, bounds.widthM, 0.01)
        assertEquals(5000.0, bounds.heightM, 0.01)
        assertEquals(15.0, bounds.areaKm2, 0.01)
        assertEquals(1.0, bounds.gridResolutionM, 0.001)
        assertEquals(3000, bounds.gridCols)
        assertEquals(5000, bounds.gridRows)
        assertEquals(15_000_000L, bounds.totalGridCells)
        assertTrue(bounds.minLat < bounds.centerLat && bounds.centerLat < bounds.maxLat)
        assertTrue(bounds.minLon < bounds.centerLon && bounds.centerLon < bounds.maxLon)
    }

    @Test
    fun `1m cell inspection maps and clamps coordinates into 3000x5000 index`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = MagadiTownRepository(context)

        val centerCell = repo.inspect1mCell(1500.4f, 2500.7f)
        assertEquals(1500, centerCell.cellCol)
        assertEquals(2500, centerCell.cellRow)
        assertTrue(centerCell.lat in -2.0..-1.8)
        assertTrue(centerCell.lon in 36.2..36.4)

        val clampedMax = repo.inspect1mCell(4000f, 6000f)
        assertEquals(2999, clampedMax.cellCol)
        assertEquals(4999, clampedMax.cellRow)
    }

    @Test
    fun `geojson export serializes roads and closed building polygons with 1m cell metadata`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = MagadiTownRepository(context)
        val bounds = repo.getNativeBounds()

        val p0 = repo.convertLatLonToCoord(-1.9010, 36.2868, bounds)
        val p1 = repo.convertLatLonToCoord(-1.9008, 36.2872, bounds)
        val p2 = repo.convertLatLonToCoord(-1.9012, 36.2874, bounds)

        val road = ExtractedRoadWay(
            osmId = 1001L,
            name = "Magadi Access Road",
            highwayType = "secondary",
            surface = "asphalt",
            lanes = 2,
            oneWay = false,
            widthM = 7.0f,
            lengthM = 50.0f,
            nodes = listOf(p0, p1)
        )
        val bldg = ExtractedBuilding(
            osmId = 2002L,
            name = "Magadi Soda Office",
            buildingType = "commercial",
            amenity = "office",
            levels = 2,
            heightM = 6.8f,
            centroid = p1,
            elevM = 612.4f,
            footprintAreaM2 = 180.0f,
            perimeterM = 54.0f,
            polygon = listOf(p0, p1, p2)
        )

        val waterPoly = ExtractedWaterPolygon(
            osmId = 3003L,
            name = "Lake Magadi North Basin",
            waterType = "salt_pond",
            polygon = listOf(p0, p1, p2),
            meanElevM = 601.5f
        )

        val bundle = MagadiTownBundle(
            bounds = bounds,
            metadata = MagadiTownMetadata(
                extractedAtIso = "2026-01-01T00:00:00Z",
                demResolutionM = 9.5f,
                lakeLevelAslM = 602.5f,
                isLiveExtracted = true,
                roadCount = 1,
                totalRoadLengthKm = 0.05f,
                buildingCount = 1,
                totalBuildingFootprintM2 = 180.0f,
                waterCount = 1
            ),
            demGrid = null,
            roads = listOf(road),
            buildings = listOf(bldg),
            pois = emptyList(),
            waterPolygons = listOf(waterPoly)
        )

        val geoJson = repo.exportGeoJson(bundle)
        assertEquals("FeatureCollection", geoJson.getString("type"))
        val meta = geoJson.getJSONObject("metadata")
        assertEquals(9.5, meta.getDouble("dem_resolution_m"), 0.01)
        assertEquals(602.5, meta.getDouble("lake_level_asl_m"), 0.01)
        assertEquals(1, meta.getInt("water_bodies"))

        val features = geoJson.getJSONArray("features")
        assertEquals(2, features.length())

        val roadFeature = features.getJSONObject(0)
        assertEquals("LineString", roadFeature.getJSONObject("geometry").getString("type"))
        assertEquals("road", roadFeature.getJSONObject("properties").getString("feature_class"))

        val bldgFeature = features.getJSONObject(1)
        assertEquals("Polygon", bldgFeature.getJSONObject("geometry").getString("type"))
        val ring = bldgFeature.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0)
        assertEquals("Polygon ring must auto-close", 4, ring.length())
        assertEquals(p1.cellCol, bldgFeature.getJSONObject("properties").getInt("grid_1m_col"))
        assertEquals(p1.cellRow, bldgFeature.getJSONObject("properties").getInt("grid_1m_row"))
    }

    @Test
    fun `magadi town binary pack MAGT serves real roads, buildings, symbols and Terrarium DEM without live network`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = MagadiTownRepository(context)
        val candidates = listOf(
            java.io.File("native-assets/assets/terrain_grid/magadi_town_3x5km.bin"),
            java.io.File("../native-assets/assets/terrain_grid/magadi_town_3x5km.bin")
        )
        val binFile = candidates.firstOrNull { it.exists() && it.length() > 100_000 }
        assertTrue("Expected magadi_town_3x5km.bin to exist", binFile != null)

        val bundle = binFile!!.inputStream().use { repo.parseBinaryStream(it) }
        assertTrue("Bundle must report binary-served", bundle.metadata.isBinaryServed)
        assertTrue("Bundle must contain real OSM roads", bundle.roads.isNotEmpty())
        assertTrue("Bundle must contain real OSM buildings", bundle.buildings.isNotEmpty())
        assertTrue("Roads must include route/highway metadata", bundle.roads.any { it.name.isNotBlank() && it.nodes.size >= 2 })
        assertTrue("Buildings must fall inside 3x5 km bounds", bundle.buildings.all { it.centroid.xM in 0f..3000f && it.centroid.yM in 0f..5000f })
        val dem = bundle.demGrid
        assertTrue("Terrarium DEM grid must be bundled", dem != null && dem.cols == 120 && dem.rows == 200 && dem.elevationsM.size == 24_000)
        assertTrue("DEM resolution must be ~9.5m Terrarium", dem!!.nativeResolutionM <= 12.0f)
    }
}
