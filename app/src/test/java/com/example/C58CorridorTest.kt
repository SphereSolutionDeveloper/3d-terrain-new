package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.repository.C58CorridorRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
class C58CorridorTest {

    @Test
    fun `route C58 summary and 10 sectors span full Magadi to Bomas corridor at 3km width`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = C58CorridorRepository(context)
        val summary = repo.getCorridorSummary()
        val ribbon = repo.getCorridorRibbon()
        val sectors = repo.buildSectorSpecs(summary)

        assertTrue("Corridor should be >= 75 km long", summary.totalLengthM >= 75_000.0)
        assertEquals(1500.0f, summary.halfWidthM, 0.01f)
        assertEquals(3.0f, summary.fullWidthKm, 0.01f)
        assertTrue(ribbon.size >= 40)
        assertEquals(10, sectors.size)
        assertEquals(0.0, sectors.first().startChainageM, 0.01)
        assertEquals(summary.totalLengthM, sectors.last().endChainageM, 0.5)

        // Every ribbon vertex must maintain 1.5 km Left and 1.5 km Right (3.0 km perpendicular width)
        for (v in ribbon) {
            val leftDist = hypot(v.leftE - v.centerE, v.leftN - v.centerN)
            val rightDist = hypot(v.rightE - v.centerE, v.rightN - v.centerN)
            assertEquals(1500.0, leftDist, 1.5)
            assertEquals(1500.0, rightDist, 1.5)
        }
    }

    @Test
    fun `non-box projection accepts centerline and +-1_2km offsets while rejecting 2_2km lateral offset`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = C58CorridorRepository(context)
        val ribbon = repo.getCorridorRibbon()
        val midVertex = ribbon[ribbon.size / 2]

        // 1. Centerline sample must project near 0m lateral offset and insideCorridor == true
        val (cLat, cLon) = repo.utmToLatLon(midVertex.centerE, midVertex.centerN)
        val cProj = repo.projectLatLon(cLat, cLon)
        assertTrue(cProj.insideCorridor)
        assertTrue(abs(cProj.lateralOffsetM) < 50f)

        // 2. Point 1.2 km LEFT of centerline must have negative offset and insideCorridor == true
        val dirLE = (midVertex.leftE - midVertex.centerE) / 1500.0
        val dirLN = (midVertex.leftN - midVertex.centerN) / 1500.0
        val (lLat, lLon) = repo.utmToLatLon(
            midVertex.centerE + dirLE * 1200.0,
            midVertex.centerN + dirLN * 1200.0
        )
        val lProj = repo.projectLatLon(lLat, lLon)
        assertTrue("1.2km left must stay inside non-box corridor", lProj.insideCorridor)
        assertTrue("Left side should yield negative lateral offset", lProj.lateralOffsetM < -800f)

        // 3. Point 1.2 km RIGHT of centerline must have positive offset and insideCorridor == true
        val (rLat, rLon) = repo.utmToLatLon(
            midVertex.centerE - dirLE * 1200.0,
            midVertex.centerN - dirLN * 1200.0
        )
        val rProj = repo.projectLatLon(rLat, rLon)
        assertTrue("1.2km right must stay inside non-box corridor", rProj.insideCorridor)
        assertTrue("Right side should yield positive lateral offset", rProj.lateralOffsetM > 800f)

        // 4. Point 2.2 km off centerline lies INSIDE bounding lat/lon rectangle of corridor
        //    yet MUST be rejected (insideCorridor == false) by the non-box ±1.5 km ribbon test!
        val (outLat, outLon) = repo.utmToLatLon(
            midVertex.centerE + dirLE * 2200.0,
            midVertex.centerN + dirLN * 2200.0
        )
        val outProj = repo.projectLatLon(outLat, outLon)
        assertFalse("2.2km perpendicular offset must be rejected outside ±1.5km ribbon", outProj.insideCorridor)
        assertTrue(abs(outProj.lateralOffsetM) > 1500f)
    }

    @Test
    fun `geojson export contains centerline, non-box corridor swath polygon, roads and houses`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = C58CorridorRepository(context)
        val base = repo.buildBaselineBundle()
        val mid = base.ribbon[base.ribbon.size / 2]

        val p0LL = repo.utmToLatLon(mid.centerE - 120.0, mid.centerN - 10.0)
        val p1LL = repo.utmToLatLon(mid.centerE - 105.0, mid.centerN - 10.0)
        val p2LL = repo.utmToLatLon(mid.centerE - 105.0, mid.centerN + 5.0)
        val c0 = repo.projectLatLon(p0LL.first, p0LL.second)
        val c1 = repo.projectLatLon(p1LL.first, p1LL.second)
        val c2 = repo.projectLatLon(p2LL.first, p2LL.second)

        val road = C58ExtractedRoad(
            osmId = 5001L,
            name = "Magadi Road (C58)",
            highwayType = "secondary",
            surface = "asphalt",
            lanes = 2,
            widthM = 7.5f,
            lengthM = 120.0f,
            sectorIndex = 5,
            midCoord = c0,
            nodes = listOf(c0, c1)
        )
        val house = C58ExtractedHouse(
            osmId = 6002L,
            name = "Olorgesailie Homestead",
            buildingType = "house",
            amenity = "",
            levels = 1,
            heightM = 3.4f,
            centroid = c1,
            elevM = 1042.5f,
            footprintAreaM2 = 112.0f,
            perimeterM = 44.0f,
            sectorIndex = 5,
            polygon = listOf(c0, c1, c2)
        )

        val bundle = base.copy(
            metadata = base.metadata.copy(
                extractedSectors = listOf(5),
                roadCount = 1,
                houseCount = 1,
                leftSideHouseCount = 1,
                rightSideHouseCount = 0
            ),
            roads = listOf(road),
            houses = listOf(house)
        )

        val geoJson = repo.exportGeoJson(bundle)
        assertEquals("FeatureCollection", geoJson.getString("type"))
        val meta = geoJson.getJSONObject("metadata")
        assertEquals("NON_BOX_CURVED_CORRIDOR_RIBBON", meta.getString("geometry_mode"))
        assertEquals(1500.0, meta.getDouble("half_width_left_m"), 0.01)
        assertEquals(1500.0, meta.getDouble("half_width_right_m"), 0.01)

        val features = geoJson.getJSONArray("features")
        assertEquals(4, features.length()) // centerline + non-box polygon + 1 road + 1 house

        assertEquals("corridor_centerline", features.getJSONObject(0).getJSONObject("properties").getString("feature_class"))
        assertEquals("LineString", features.getJSONObject(0).getJSONObject("geometry").getString("type"))
        assertEquals("corridor_boundary", features.getJSONObject(1).getJSONObject("properties").getString("feature_class"))
        assertEquals("Polygon", features.getJSONObject(1).getJSONObject("geometry").getString("type"))
        assertEquals("road", features.getJSONObject(2).getJSONObject("properties").getString("feature_class"))
        assertEquals("house", features.getJSONObject(3).getJSONObject("properties").getString("feature_class"))

        val ring = features.getJSONObject(3).getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0)
        assertEquals("House polygon ring must auto-close", 4, ring.length())
    }

    @Test
    fun `route c58 binary pack C58B serves real corridor houses, roads, symbols and curvilinear DEM offline`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val repo = C58CorridorRepository(context)
        val candidates = listOf(
            java.io.File("native-assets/assets/terrain_grid/c58_corridor_3km.bin"),
            java.io.File("../native-assets/assets/terrain_grid/c58_corridor_3km.bin")
        )
        val binFile = candidates.firstOrNull { it.exists() && it.length() > 100_000 }
        assertTrue("Expected c58_corridor_3km.bin to exist", binFile != null)

        val t0 = System.currentTimeMillis()
        val bundle = binFile!!.inputStream().use { repo.parseBinaryStream(it) }
        val elapsedMs = System.currentTimeMillis() - t0
        assertTrue("Binary parsing should complete promptly (was ${elapsedMs}ms)", elapsedMs < 4000)

        assertTrue("Bundle must report binary-served", bundle.metadata.isBinaryServed)
        assertEquals(10, bundle.sectors.size)
        assertTrue(
            "Centerline must start near Magadi Town (-1.901), was ${bundle.centerlineLatLon.firstOrNull()}",
            (bundle.centerlineLatLon.firstOrNull()?.first ?: 0.0) < -1.88
        )
        assertTrue(
            "Centerline must reach Bomas/Galleria (-1.343), was ${bundle.centerlineLatLon.lastOrNull()}",
            (bundle.centerlineLatLon.lastOrNull()?.first ?: -9.0) > -1.36
        )
        assertTrue(
            "Full corridor length must be >= 85 km, was ${bundle.summary.totalLengthM}",
            bundle.summary.totalLengthM >= 85_000.0
        )
        assertTrue("Corridor must contain real OSM roads", bundle.roads.size >= 500)
        assertTrue("Corridor must contain real OSM houses", bundle.houses.size >= 5000)
        assertTrue("Sector 1 (Magadi Town) must contain real houses", bundle.houses.any { it.sectorIndex == 1 })
        assertTrue("Sector 1 (Magadi Town) must contain real roads", bundle.roads.any { it.sectorIndex == 1 })
        assertTrue("Houses must stay strictly inside +-1.5 km width", bundle.houses.all { abs(it.centroid.lateralOffsetM) <= 1505.0f })
        assertTrue("Must classify Left and Right corridor houses", bundle.metadata.leftSideHouseCount > 0 && bundle.metadata.rightSideHouseCount > 0)
        val dem = bundle.demGrid
        assertTrue("Curvilinear Terrarium DEM grid must be bundled", dem != null && dem.uCols == 31 && dem.vRows == 220 && dem.elevationsM.size == 6820)

        // Verify median house in Sector 1 keeps houses & roads visible both at sector fit and when zoomed in
        val s1Houses = bundle.houses.filter { it.sectorIndex == 1 }.sortedBy { it.centroid.chainageM }
        val centerHouse = s1Houses[s1Houses.size / 2]
        val ce = centerHouse.centroid.utmEast
        val cn = centerHouse.centroid.utmNorth
        val zoomedHalfW = 400.0
        val housesInZoom = bundle.houses.count {
            abs(it.centroid.utmEast - ce) <= zoomedHalfW && abs(it.centroid.utmNorth - cn) <= zoomedHalfW
        }
        val roadsInZoom = bundle.roads.count { r ->
            r.nodes.any { abs(it.utmEast - ce) <= zoomedHalfW && abs(it.utmNorth - cn) <= zoomedHalfW }
        }
        assertTrue("Zoomed-in Sector 1 view must contain visible houses ($housesInZoom)", housesInZoom > 0)
        assertTrue("Zoomed-in Sector 1 view must contain visible roads ($roadsInZoom)", roadsInZoom > 0)

        // Verify C58 data extends well north of Ongata Rongai / Maasai Lodge (N >= 9,846,500) to Galleria & Bomas (N >= 9,852,000)
        val northOfMaasaiHouses = bundle.houses.count { it.centroid.utmNorth >= 9_846_500.0 }
        val northOfMaasaiRoads = bundle.roads.count { it.midCoord.utmNorth >= 9_846_500.0 }
        val bomasPois = bundle.pois.count {
            it.coord.utmNorth >= 9_850_000.0 &&
                (it.name.contains("Bomas", ignoreCase = true) || it.name.contains("Galleria", ignoreCase = true))
        }
        assertTrue("S10 must have houses north of Maasai Lodge ($northOfMaasaiHouses)", northOfMaasaiHouses >= 500)
        assertTrue("S10 must have roads north of Maasai Lodge ($northOfMaasaiRoads)", northOfMaasaiRoads >= 200)
        assertTrue("S10 must include Galleria/Bomas POIs ($bomasPois)", bomasPois >= 2)

        // Verify ViewModel camera framing for S10 and ALL C58 includes northern Bomas terminus on phone viewport aspect
        val vm = com.example.viewmodel.C58CorridorViewModel(context)
        val aspW = 1000f
        val aspH = 750f
        val asp = aspH / aspW
        vm.updateCanvasAspect(aspW, aspH)

        vm.fitSectorCamera(10, bundle)
        val s10CenterN = vm.viewCenterUtmN.value
        val s10HalfH = vm.viewWidthM.value * asp * 0.5
        val s10Start = bundle.sectors[9].startChainageM
        val s10StartN = bundle.ribbon.first { it.chainageM >= s10Start }.centerN
        val bomasEndN = bundle.ribbon.last().centerN
        assertTrue("S10 camera top (${s10CenterN + s10HalfH}) must reach Bomas terminus ($bomasEndN)", s10CenterN + s10HalfH >= bomasEndN)
        assertTrue("S10 camera bottom (${s10CenterN - s10HalfH}) must include Ongata Rongai start ($s10StartN)", s10CenterN - s10HalfH <= s10StartN)

        vm.fitSectorCamera(0, bundle)
        val allCenterN = vm.viewCenterUtmN.value
        val allHalfH = vm.viewWidthM.value * asp * 0.5
        val magadiStartN = bundle.ribbon.first().centerN
        assertTrue("ALL C58 camera top (${allCenterN + allHalfH}) must reach Bomas terminus ($bomasEndN)", allCenterN + allHalfH >= bomasEndN)
        assertTrue("ALL C58 camera bottom (${allCenterN - allHalfH}) must reach Magadi terminus ($magadiStartN)", allCenterN - allHalfH <= magadiStartN)

        // Verify unnamed side streets are normalized away from 'C58 <Class> #<id>'
        assertTrue(
            "Unnamed side roads without ref must not be labeled 'C58 ...'",
            bundle.roads.none { it.ref.isBlank() && it.name.startsWith("C58 ") && it.name.contains('#') }
        )
        assertTrue(
            "Corridor must contain named non-C58 roads as well as Magadi Road",
            bundle.roads.any { it.name.contains("Magadi Road", ignoreCase = true) } &&
                bundle.roads.any { !it.name.contains("C58", ignoreCase = true) && !it.name.startsWith("Unnamed ") && !it.name.contains('#') }
        )
    }
}
