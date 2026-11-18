package com.example.repository

import android.content.Context
import com.example.engine.NativeEngineBridge
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class C58CorridorRepository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "route_c58_3km_corridor_v1_cache.json")

    // Pure-Kotlin UTM Zone 37S + centerline mirror state for headless JVM tests when .so isn't loaded
    private var fallbackCenterlineLatLon: List<Pair<Double, Double>> = buildDefaultAnchorsLatLon()
    private var fallbackRibbon: List<C58RibbonVertex> = emptyList()
    private var fallbackSummary: C58CorridorSummary = C58CorridorSummary()

    init {
        rebuildFallbackRibbon()
    }

    fun getCorridorSummary(): C58CorridorSummary {
        return try {
            if (NativeEngineBridge.isLoaded()) {
                val arr = NativeEngineBridge.nativeGetC58Summary()
                if (arr.size >= 11) {
                    C58CorridorSummary(
                        totalLengthM = arr[0],
                        halfWidthM = arr[1].toFloat(),
                        centerlinePoints = arr[2].toInt(),
                        minUtmEast = arr[3],
                        maxUtmEast = arr[4],
                        minUtmNorth = arr[5],
                        maxUtmNorth = arr[6],
                        minLat = arr[7],
                        maxLat = arr[8],
                        minLon = arr[9],
                        maxLon = arr[10]
                    )
                } else fallbackSummary
            } else fallbackSummary
        } catch (_: Throwable) {
            fallbackSummary
        }
    }

    fun getCorridorRibbon(): List<C58RibbonVertex> {
        return try {
            if (NativeEngineBridge.isLoaded()) {
                val raw = NativeEngineBridge.nativeGetC58CorridorRibbonUtm()
                val out = ArrayList<C58RibbonVertex>(raw.size / 7)
                var i = 0
                while (i + 6 < raw.size) {
                    out.add(
                        C58RibbonVertex(
                            centerE = raw[i],
                            centerN = raw[i + 1],
                            leftE = raw[i + 2],
                            leftN = raw[i + 3],
                            rightE = raw[i + 4],
                            rightN = raw[i + 5],
                            chainageM = raw[i + 6]
                        )
                    )
                    i += 7
                }
                if (out.isNotEmpty()) out else fallbackRibbon
            } else fallbackRibbon
        } catch (_: Throwable) {
            fallbackRibbon
        }
    }

    fun buildSectorSpecs(summary: C58CorridorSummary): List<C58SectorSpec> {
        val total = summary.totalLengthM.coerceAtLeast(10_000.0)
        val step = total / 10.0
        val names = listOf(
            "Magadi Causeway",
            "Soda Works North",
            "Ol Keju Nero",
            "Kwenia Gorge",
            "Olorgesailie",
            "Tinga Center",
            "Kamukuru Climb",
            "Corner Baridi",
            "Kiserian Town",
            "Rongai - Bomas"
        )
        return (1..10).map { idx ->
            C58SectorSpec(
                index = idx,
                shortLabel = "S$idx",
                title = "S$idx: ${names[idx - 1]}",
                startChainageM = (idx - 1) * step,
                endChainageM = idx * step
            )
        }
    }

    fun projectLatLon(lat: Double, lon: Double): C58Coord {
        if (NativeEngineBridge.isLoaded()) {
            try {
                val res = NativeEngineBridge.nativeProjectLatLonToC58(lat, lon)
                if (res.size >= 5) {
                    return C58Coord(
                        lat = lat,
                        lon = lon,
                        utmEast = res[0],
                        utmNorth = res[1],
                        chainageM = res[2],
                        lateralOffsetM = res[3].toFloat(),
                        insideCorridor = res[4] > 0.5
                    )
                }
            } catch (_: Throwable) {
            }
        }
        val (e, n) = latLonToUtm37SKotlin(lat, lon)
        val (chainM, offM, inside) = projectUtmKotlin(e, n)
        return C58Coord(
            lat = lat,
            lon = lon,
            utmEast = e,
            utmNorth = n,
            chainageM = chainM,
            lateralOffsetM = offM,
            insideCorridor = inside
        )
    }

    fun inspectAtUtm(utmE: Double, utmN: Double): C58InspectedCell {
        if (NativeEngineBridge.isLoaded()) {
            try {
                val d = NativeEngineBridge.nativeGetC58TelemetryAtUtm(utmE, utmN)
                if (d.size >= 11) {
                    return C58InspectedCell(
                        lat = d[0],
                        lon = d[1],
                        utmEast = d[2],
                        utmNorth = d[3],
                        chainageM = d[4],
                        lateralOffsetM = d[5].toFloat(),
                        insideCorridor = d[6] > 0.5,
                        elevM = d[7].toFloat(),
                        slopeDeg = d[8].toFloat(),
                        waterDepthM = d[9].toFloat(),
                        isWater = d[10] > 0.5
                    )
                }
            } catch (_: Throwable) {
            }
        }
        val (lat, lon) = utm37SToLatLonKotlin(utmE, utmN)
        val (chainM, offM, inside) = projectUtmKotlin(utmE, utmN)
        return C58InspectedCell(
            lat = lat,
            lon = lon,
            utmEast = utmE,
            utmNorth = utmN,
            chainageM = chainM,
            lateralOffsetM = offM,
            insideCorridor = inside,
            elevM = 1120f,
            slopeDeg = 3.2f
        )
    }

    fun utmToLatLon(utmE: Double, utmN: Double): Pair<Double, Double> {
        if (NativeEngineBridge.isLoaded()) {
            try {
                val ll = NativeEngineBridge.nativeC58UtmToLatLon(utmE, utmN)
                if (ll.size >= 2) return ll[0] to ll[1]
            } catch (_: Throwable) {
            }
        }
        return utm37SToLatLonKotlin(utmE, utmN)
    }

    fun buildBaselineBundle(): C58CorridorBundle {
        val summary = getCorridorSummary()
        val ribbon = getCorridorRibbon()
        val sectors = buildSectorSpecs(summary)
        return C58CorridorBundle(
            summary = summary,
            metadata = C58CorridorMetadata(
                routeName = "Route C58 (Magadi Road • ±1.5 km Non-Box Corridor)",
                isLiveOsmCenterline = false,
                extractedSectors = emptyList()
            ),
            centerlineLatLon = fallbackCenterlineLatLon,
            ribbon = ribbon,
            sectors = sectors,
            demGrid = null,
            roads = emptyList(),
            houses = emptyList(),
            pois = emptyList(),
            waterPolygons = emptyList()
        )
    }

    suspend fun loadBinaryAssetBundle(): C58CorridorBundle? = withContext(Dispatchers.IO) {
        return@withContext try {
            context.assets.open("terrain_grid/c58_corridor_3km.bin").use { stream ->
                val bundle = parseBinaryStream(stream)
                syncBundleToCpp(bundle)
                bundle
            }
        } catch (e: Exception) {
            System.err.println("C58CorridorRepository.loadBinaryAssetBundle failed: ${e.message}")
            null
        }
    }

    fun parseBinaryStream(stream: InputStream): C58CorridorBundle {
        val bytes = stream.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val magic = ByteArray(4)
        buf.get(magic)
        check(String(magic, Charsets.US_ASCII) == "C58B") { "Invalid C58B binary header" }
        buf.short // version
        val isLiveCl = buf.short.toInt() != 0

        fun readStr(): String {
            val len = buf.short.toInt() and 0xFFFF
            if (len == 0) return ""
            val arr = ByteArray(len)
            buf.get(arr)
            return String(arr, Charsets.UTF_8)
        }

        val refE = buf.double
        val refN = buf.double

        // Exact inverse for points; nearby house vertices use local scale+rotation around centroid
        fun readC58Coord(): C58Coord {
            val ue = refE + buf.float.toDouble()
            val un = refN + buf.float.toDouble()
            val ch = buf.float.toDouble()
            val off = buf.float
            val (lat, lon) = utm37SToLatLonKotlin(ue, un)
            return C58Coord(
                lat = lat,
                lon = lon,
                utmEast = ue,
                utmNorth = un,
                chainageM = ch,
                lateralOffsetM = off,
                insideCorridor = abs(off) <= 1500.5f
            )
        }

        fun readC58CoordNear(anchor: C58Coord, dLatPerE: Double, dLatPerN: Double, dLonPerE: Double, dLonPerN: Double): C58Coord {
            val ue = refE + buf.float.toDouble()
            val un = refN + buf.float.toDouble()
            val ch = buf.float.toDouble()
            val off = buf.float
            val de = ue - anchor.utmEast
            val dn = un - anchor.utmNorth
            return C58Coord(
                lat = anchor.lat + de * dLatPerE + dn * dLatPerN,
                lon = anchor.lon + de * dLonPerE + dn * dLonPerN,
                utmEast = ue,
                utmNorth = un,
                chainageM = ch,
                lateralOffsetM = off,
                insideCorridor = abs(off) <= 1500.5f
            )
        }

        val routeName = readStr()
        val extractedAtIso = readStr()
        val osmSourceUrl = readStr()
        val demSourceUrl = readStr()
        val demResolutionM = buf.float
        val lakeLevelAslM = buf.float
        val elevMinM = buf.float
        val elevMaxM = buf.float
        val elevMeanM = buf.float

        val secCount = buf.get().toInt() and 0xFF
        val extractedSectors = ArrayList<Int>(secCount)
        for (i in 0 until secCount) {
            extractedSectors.add(buf.get().toInt() and 0xFF)
        }

        val clCount = buf.int
        val clPairs = ArrayList<Pair<Double, Double>>(clCount)
        for (i in 0 until clCount) {
            val lat = buf.double
            val lon = buf.double
            clPairs.add(lat to lon)
        }
        if (clPairs.size >= 2) {
            setCenterlinePairs(clPairs)
        }
        val summary = getCorridorSummary()
        val ribbon = getCorridorRibbon()
        val sectors = buildSectorSpecs(summary)

        val demSource = readStr()
        val uCols = buf.int
        val vRows = buf.int
        val demNativeRes = buf.float
        val demMinE = buf.float
        val demMaxE = buf.float
        val demMeanE = buf.float
        val demCount = uCols * vRows
        val demVals = FloatArray(demCount)
        for (i in 0 until demCount) {
            demVals[i] = buf.float
        }
        val demGrid = if (demCount > 0) {
            C58DemGrid(
                source = demSource,
                nativeResolutionM = demNativeRes,
                uCols = uCols,
                vRows = vRows,
                elevationsM = demVals,
                minElevM = demMinE,
                maxElevM = demMaxE,
                meanElevM = demMeanE
            )
        } else null

        val roadCount = buf.int
        val roads = ArrayList<C58ExtractedRoad>(roadCount)
        for (i in 0 until roadCount) {
            val osmId = buf.long
            val rawName = readStr()
            val ref = readStr()
            val hType = readStr()
            val surface = readStr()
            val lanes = buf.get().toInt() and 0xFF
            val rFlags = buf.get().toInt() and 0xFF
            val secIdx = buf.get().toInt() and 0xFF
            val widthM = buf.float
            val lengthM = buf.float
            val mid = readC58Coord()
            val nCount = buf.short.toInt() and 0xFFFF
            val nodes = ArrayList<C58Coord>(nCount)
            for (k in 0 until nCount) nodes.add(readC58Coord())
            val normalizedName = if (
                ref.isBlank() &&
                rawName.startsWith("C58 ") &&
                rawName.endsWith("#$osmId") &&
                !rawName.contains("Magadi", ignoreCase = true)
            ) {
                "Unnamed " + rawName.removePrefix("C58 ")
            } else {
                rawName
            }
            roads.add(
                C58ExtractedRoad(
                    osmId = osmId,
                    name = normalizedName,
                    highwayType = hType,
                    surface = surface,
                    lanes = lanes,
                    widthM = widthM,
                    lengthM = lengthM,
                    sectorIndex = secIdx,
                    midCoord = mid,
                    nodes = nodes,
                    ref = ref,
                    oneWay = (rFlags and 1) != 0,
                    isBridge = (rFlags and 2) != 0,
                    isRoundabout = (rFlags and 4) != 0
                )
            )
        }

        val houseCount = buf.int
        val houses = ArrayList<C58ExtractedHouse>(houseCount)
        val degToRad = Math.PI / 180.0
        for (i in 0 until houseCount) {
            val osmId = buf.long
            val name = readStr()
            val bType = readStr()
            val amenity = readStr()
            val levels = buf.get().toInt() and 0xFF
            val secIdx = buf.get().toInt() and 0xFF
            val heightM = buf.float
            val elevM = buf.float
            val areaM2 = buf.float
            val perimM = buf.float
            buf.float // reserved lateral offset
            val centroid = readC58Coord()
            val polyCount = buf.short.toInt() and 0xFFFF
            val poly = ArrayList<C58Coord>(polyCount)
            val cosLat = cos(centroid.lat * degToRad).coerceAtLeast(0.5)
            val gamma = ((centroid.lon - 39.0) * degToRad) * sin(centroid.lat * degToRad)
            val cosG = cos(gamma)
            val sinG = sin(gamma)
            val dLatPerN = cosG / (0.9996 * 110574.0)
            val dLatPerE = -sinG / (0.9996 * 110574.0)
            val dLonPerE = cosG / (0.9996 * 111320.0 * cosLat)
            val dLonPerN = sinG / (0.9996 * 111320.0 * cosLat)
            for (k in 0 until polyCount) {
                poly.add(readC58CoordNear(centroid, dLatPerE, dLatPerN, dLonPerE, dLonPerN))
            }
            houses.add(
                C58ExtractedHouse(
                    osmId = osmId,
                    name = name,
                    buildingType = bType,
                    amenity = amenity,
                    levels = levels,
                    heightM = heightM,
                    centroid = centroid,
                    elevM = elevM,
                    footprintAreaM2 = areaM2,
                    perimeterM = perimM,
                    sectorIndex = secIdx,
                    polygon = poly
                )
            )
        }

        val waterCount = buf.int
        val waterPolys = ArrayList<C58WaterPolygon>(waterCount)
        for (i in 0 until waterCount) {
            val osmId = buf.long
            val name = readStr()
            val wType = readStr()
            val meanE = buf.float
            val polyCount = buf.short.toInt() and 0xFFFF
            val poly = ArrayList<C58Coord>(polyCount)
            for (k in 0 until polyCount) poly.add(readC58Coord())
            waterPolys.add(
                C58WaterPolygon(
                    osmId = osmId,
                    name = name,
                    waterType = wType,
                    polygon = poly,
                    meanElevM = meanE
                )
            )
        }

        val poiCount = buf.int
        val pois = ArrayList<C58ExtractedPoi>(poiCount)
        for (i in 0 until poiCount) {
            val osmId = buf.long
            val name = readStr()
            val cat = readStr()
            val secIdx = buf.get().toInt() and 0xFF
            val elevM = buf.float
            val coord = readC58Coord()
            pois.add(
                C58ExtractedPoi(
                    osmId = osmId,
                    name = name,
                    category = cat,
                    coord = coord,
                    elevM = elevM,
                    sectorIndex = secIdx
                )
            )
        }

        val leftHouses = houses.count { it.centroid.lateralOffsetM < 0f }
        val metadata = C58CorridorMetadata(
            routeName = routeName,
            isLiveOsmCenterline = isLiveCl,
            extractedSectors = extractedSectors.ifEmpty { (1..10).toList() },
            extractedAtIso = extractedAtIso,
            osmSourceUrl = "Binary Pack ($osmSourceUrl)",
            demSourceUrl = demSourceUrl,
            demResolutionM = demResolutionM,
            lakeLevelAslM = lakeLevelAslM,
            roadCount = roads.size,
            totalRoadLengthKm = roads.sumOf { it.lengthM.toDouble() }.toFloat() / 1000f,
            houseCount = houses.size,
            leftSideHouseCount = leftHouses,
            rightSideHouseCount = houses.size - leftHouses,
            totalHouseAreaM2 = houses.sumOf { it.footprintAreaM2.toDouble() }.toFloat(),
            waterCount = waterPolys.size,
            poiCount = pois.size,
            elevMinM = elevMinM,
            elevMaxM = elevMaxM,
            elevMeanM = elevMeanM,
            isBinaryServed = true
        )

        return C58CorridorBundle(
            summary = summary,
            metadata = metadata,
            centerlineLatLon = clPairs.ifEmpty { fallbackCenterlineLatLon },
            ribbon = ribbon,
            sectors = sectors,
            demGrid = demGrid,
            roads = roads,
            houses = houses,
            pois = pois,
            waterPolygons = waterPolys
        )
    }

    suspend fun loadCachedBundle(): C58CorridorBundle? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        return@withContext try {
            val json = JSONObject(cacheFile.readText())
            val bundle = deserializeBundle(json)
            syncBundleToCpp(bundle)
            bundle
        } catch (_: Exception) {
            null
        }
    }

    fun setCenterlinePairs(pairs: List<Pair<Double, Double>>) {
        if (pairs.size < 2) return
        if (fallbackCenterlineLatLon == pairs && fallbackRibbon.isNotEmpty()) {
            if (NativeEngineBridge.isLoaded()) {
                try {
                    val flat = DoubleArray(pairs.size * 2)
                    for (i in pairs.indices) {
                        flat[i * 2] = pairs[i].first
                        flat[i * 2 + 1] = pairs[i].second
                    }
                    NativeEngineBridge.nativeSetC58Centerline(flat)
                } catch (_: Throwable) {
                }
            }
            return
        }
        fallbackCenterlineLatLon = pairs
        rebuildFallbackRibbon()
        if (NativeEngineBridge.isLoaded()) {
            try {
                val flat = DoubleArray(pairs.size * 2)
                for (i in pairs.indices) {
                    flat[i * 2] = pairs[i].first
                    flat[i * 2 + 1] = pairs[i].second
                }
                NativeEngineBridge.nativeSetC58Centerline(flat)
            } catch (_: Throwable) {
            }
        }
    }

    fun syncLakeLevelToCpp(lakeLevelAslM: Float, waterPolygons: List<C58WaterPolygon>) {
        if (!NativeEngineBridge.isLoaded()) return
        try {
            val metaList = ArrayList<Int>(waterPolygons.size * 2)
            val coordList = ArrayList<Double>()
            for (wp in waterPolygons) {
                if (wp.polygon.size < 3) continue
                metaList.add(wp.polygon.size)
                metaList.add(if (wp.waterType.contains("salt") || wp.waterType.contains("mud")) 1 else 0)
                for (pt in wp.polygon) {
                    coordList.add(pt.utmEast)
                    coordList.add(pt.utmNorth)
                }
            }
            NativeEngineBridge.nativeSetC58LakeAndWater(
                lakeLevelAslM,
                metaList.toIntArray(),
                coordList.toDoubleArray()
            )
        } catch (_: Throwable) {
        }
    }

    fun syncBundleToCpp(bundle: C58CorridorBundle) {
        setCenterlinePairs(bundle.centerlineLatLon)
        if (!NativeEngineBridge.isLoaded()) return
        try {
            bundle.demGrid?.let { dem ->
                NativeEngineBridge.nativeSetC58DemGrid(dem.uCols, dem.vRows, dem.elevationsM)
            }
            syncLakeLevelToCpp(bundle.metadata.lakeLevelAslM, bundle.waterPolygons)

            val segCount = bundle.roads.sumOf { max(0, it.nodes.size - 1) }
            val roadDoubles = DoubleArray(segCount * 5)
            var rIdx = 0
            for (road in bundle.roads) {
                val hw = (road.widthM * 0.5).coerceAtLeast(2.0)
                for (i in 0 until road.nodes.size - 1) {
                    val p0 = road.nodes[i]
                    val p1 = road.nodes[i + 1]
                    roadDoubles[rIdx++] = p0.utmEast
                    roadDoubles[rIdx++] = p0.utmNorth
                    roadDoubles[rIdx++] = p1.utmEast
                    roadDoubles[rIdx++] = p1.utmNorth
                    roadDoubles[rIdx++] = hw
                }
            }
            val bldgDoubles = DoubleArray(bundle.houses.size * 4)
            var bIdx = 0
            for (h in bundle.houses) {
                if (h.polygon.isEmpty()) continue
                var minE = Double.MAX_VALUE
                var minN = Double.MAX_VALUE
                var maxE = -Double.MAX_VALUE
                var maxN = -Double.MAX_VALUE
                for (v in h.polygon) {
                    if (v.utmEast < minE) minE = v.utmEast
                    if (v.utmNorth < minN) minN = v.utmNorth
                    if (v.utmEast > maxE) maxE = v.utmEast
                    if (v.utmNorth > maxN) maxN = v.utmNorth
                }
                bldgDoubles[bIdx++] = minE
                bldgDoubles[bIdx++] = minN
                bldgDoubles[bIdx++] = maxE
                bldgDoubles[bIdx++] = maxN
            }
            NativeEngineBridge.nativeSetC58Footprints(
                roadDoubles.copyOf(rIdx),
                bldgDoubles.copyOf(bIdx)
            )
        } catch (_: Throwable) {
        }
    }

    private data class RibbonInterp(
        val ce: Double, val cn: Double, val le: Double, val ln: Double, val re: Double, val rn: Double
    )

    private fun interpolateRibbonAtChainage(ribbon: List<C58RibbonVertex>, s: Double): RibbonInterp {
        if (ribbon.isEmpty()) return RibbonInterp(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        if (ribbon.size == 1 || s <= ribbon.first().chainageM) {
            val v = ribbon.first()
            return RibbonInterp(v.centerE, v.centerN, v.leftE, v.leftN, v.rightE, v.rightN)
        }
        for (i in 0 until ribbon.size - 1) {
            val a = ribbon[i]
            val b = ribbon[i + 1]
            if (s <= b.chainageM || i == ribbon.size - 2) {
                val span = (b.chainageM - a.chainageM).coerceAtLeast(1e-3)
                val t = ((s - a.chainageM) / span).coerceIn(0.0, 1.0)
                return RibbonInterp(
                    ce = a.centerE + (b.centerE - a.centerE) * t,
                    cn = a.centerN + (b.centerN - a.centerN) * t,
                    le = a.leftE + (b.leftE - a.leftE) * t,
                    ln = a.leftN + (b.leftN - a.leftN) * t,
                    re = a.rightE + (b.rightE - a.rightE) * t,
                    rn = a.rightN + (b.rightN - a.rightN) * t
                )
            }
        }
        val v = ribbon.last()
        return RibbonInterp(v.centerE, v.centerN, v.leftE, v.leftN, v.rightE, v.rightN)
    }

    private fun sectorFromChainage(chainageM: Double, summary: C58CorridorSummary): Int {
        val total = summary.totalLengthM.coerceAtLeast(10_000.0)
        val frac = (chainageM / total).coerceIn(0.0, 0.999999)
        return (frac * 10.0).toInt().coerceIn(0, 9) + 1
    }

    fun exportGeoJson(bundle: C58CorridorBundle): JSONObject {
        val features = JSONArray()

        // 1. Feature: Route C58 Centerline
        val centerCoords = JSONArray()
        for (v in bundle.ribbon) {
            val (lat, lon) = utmToLatLon(v.centerE, v.centerN)
            centerCoords.put(JSONArray().put(lon).put(lat))
        }
        features.put(JSONObject().apply {
            put("type", "Feature")
            put("id", "route_c58_centerline")
            put("properties", JSONObject().apply {
                put("feature_class", "corridor_centerline")
                put("name", "Route C58 Centerline")
                put("length_km", bundle.summary.totalLengthKm)
            })
            put("geometry", JSONObject().apply {
                put("type", "LineString")
                put("coordinates", centerCoords)
            })
        })

        // 2. Feature: Non-Box 3.0 km Corridor Ribbon Boundary Polygon (-1.5km Left -> +1.5km Right)
        if (bundle.ribbon.size >= 2) {
            val ribbonRing = JSONArray()
            for (v in bundle.ribbon) {
                val (lat, lon) = utmToLatLon(v.leftE, v.leftN)
                ribbonRing.put(JSONArray().put(lon).put(lat))
            }
            for (i in bundle.ribbon.indices.reversed()) {
                val v = bundle.ribbon[i]
                val (lat, lon) = utmToLatLon(v.rightE, v.rightN)
                ribbonRing.put(JSONArray().put(lon).put(lat))
            }
            val (firstLat, firstLon) = utmToLatLon(bundle.ribbon[0].leftE, bundle.ribbon[0].leftN)
            ribbonRing.put(JSONArray().put(firstLon).put(firstLat))
            features.put(JSONObject().apply {
                put("type", "Feature")
                put("id", "route_c58_3km_swath")
                put("properties", JSONObject().apply {
                    put("feature_class", "corridor_boundary")
                    put("name", "Route C58 ±1.5 km Non-Box Swath (3 km Width)")
                    put("half_width_left_m", 1500)
                    put("half_width_right_m", 1500)
                })
                put("geometry", JSONObject().apply {
                    put("type", "Polygon")
                    put("coordinates", JSONArray().put(ribbonRing))
                })
            })
        }

        // 3. Extracted Corridor Roads
        for (road in bundle.roads) {
            val coords = JSONArray()
            road.nodes.forEach { pt -> coords.put(JSONArray().put(pt.lon).put(pt.lat)) }
            features.put(JSONObject().apply {
                put("type", "Feature")
                put("id", "way/${road.osmId}")
                put("properties", JSONObject().apply {
                    put("feature_class", "road")
                    put("name", road.name)
                    put("highway", road.highwayType)
                    put("surface", road.surface)
                    put("sector", road.sectorIndex)
                    put("chainage_km", road.midCoord.chainageKm)
                    put("lateral_offset_m", road.midCoord.lateralOffsetM)
                    put("corridor_side", road.midCoord.corridorSide)
                    put("length_m", road.lengthM)
                })
                put("geometry", JSONObject().apply {
                    put("type", "LineString")
                    put("coordinates", coords)
                })
            })
        }

        // 4. Extracted Corridor Houses/Buildings (Closed Polygon Rings)
        for (h in bundle.houses) {
            val ring = JSONArray()
            h.polygon.forEach { pt -> ring.put(JSONArray().put(pt.lon).put(pt.lat)) }
            if (h.polygon.isNotEmpty() && (h.polygon.first().lat != h.polygon.last().lat || h.polygon.first().lon != h.polygon.last().lon)) {
                ring.put(JSONArray().put(h.polygon.first().lon).put(h.polygon.first().lat))
            }
            features.put(JSONObject().apply {
                put("type", "Feature")
                put("id", "way/${h.osmId}")
                put("properties", JSONObject().apply {
                    put("feature_class", "house")
                    put("name", h.name)
                    put("building", h.buildingType)
                    put("sector", h.sectorIndex)
                    put("chainage_km", h.centroid.chainageKm)
                    put("lateral_offset_m", h.centroid.lateralOffsetM)
                    put("corridor_side", h.centroid.corridorSide)
                    put("elev_m", h.elevM)
                    put("footprint_area_m2", h.footprintAreaM2)
                    put("utm37s_east", h.centroid.utmEast)
                    put("utm37s_north", h.centroid.utmNorth)
                })
                put("geometry", JSONObject().apply {
                    put("type", "Polygon")
                    put("coordinates", JSONArray().put(ring))
                })
            })
        }

        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("name", "Route C58 ±1.5km Non-Box Corridor Extract")
            put("metadata", JSONObject().apply {
                put("route", bundle.metadata.routeName)
                put("geometry_mode", "NON_BOX_CURVED_CORRIDOR_RIBBON")
                put("corridor_width_km", bundle.metadata.corridorWidthKm)
                put("half_width_left_m", bundle.metadata.halfWidthLeftM)
                put("half_width_right_m", bundle.metadata.halfWidthRightM)
                put("length_km", bundle.summary.totalLengthKm)
                put("houses", bundle.houses.size)
                put("roads", bundle.roads.size)
                put("dem_resolution_m", bundle.metadata.demResolutionM)
                put("extracted_at", bundle.metadata.extractedAtIso)
            })
            put("features", features)
        }
    }

    // ─── Pure-Kotlin Geometry Helpers (Mirrors C++ C58CorridorEngine when .so unavailable on JVM test host) ───

    private fun buildDefaultAnchorsLatLon(): List<Pair<Double, Double>> {
        val anchors = listOf(
            -1.9012 to 36.2870,
            -1.8620 to 36.3060,
            -1.7890 to 36.3420,
            -1.7180 to 36.3780,
            -1.6480 to 36.4125,
            -1.5785 to 36.4472,
            -1.5250 to 36.4950,
            -1.4820 to 36.5480,
            -1.4480 to 36.6050,
            -1.4295 to 36.6820,
            -1.4110 to 36.7160,
            -1.3945 to 36.7438,
            -1.3640 to 36.7565,
            -1.3340 to 36.7660
        )
        val out = ArrayList<Pair<Double, Double>>()
        for (i in 0 until anchors.size - 1) {
            val a = anchors[i]
            val b = anchors[i + 1]
            for (s in 0 until 5) {
                val t = s / 5.0
                out.add((a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t))
            }
        }
        out.add(anchors.last())
        return out
    }

    private fun rebuildFallbackRibbon() {
        val utmPts = fallbackCenterlineLatLon.map { latLonToUtm37SKotlin(it.first, it.second) }
        if (utmPts.size < 2) return
        val ribbon = ArrayList<C58RibbonVertex>(utmPts.size)
        var cumS = 0.0
        var minE = Double.MAX_VALUE; var maxE = -Double.MAX_VALUE
        var minN = Double.MAX_VALUE; var maxN = -Double.MAX_VALUE
        for (i in utmPts.indices) {
            if (i > 0) cumS += hypot(utmPts[i].first - utmPts[i - 1].first, utmPts[i].second - utmPts[i - 1].second)
            val prev = utmPts[(i - 1).coerceAtLeast(0)]
            val next = utmPts[(i + 1).coerceAtMost(utmPts.lastIndex)]
            var te = next.first - prev.first
            var tn = next.second - prev.second
            val tl = hypot(te, tn).coerceAtLeast(1e-6)
            te /= tl; tn /= tl
            val re = tn; val rn = -te
            val ce = utmPts[i].first; val cn = utmPts[i].second
            val le = ce - re * 1500.0; val ln = cn - rn * 1500.0
            val rre = ce + re * 1500.0; val rrn = cn + rn * 1500.0
            ribbon.add(C58RibbonVertex(ce, cn, le, ln, rre, rrn, cumS))
            minE = minOf(minE, ce, le, rre); maxE = maxOf(maxE, ce, le, rre)
            minN = minOf(minN, cn, ln, rrn); maxN = maxOf(maxN, cn, ln, rrn)
        }
        fallbackRibbon = ribbon
        fallbackSummary = C58CorridorSummary(
            totalLengthM = cumS,
            halfWidthM = 1500f,
            centerlinePoints = ribbon.size,
            minUtmEast = minE,
            maxUtmEast = maxE,
            minUtmNorth = minN,
            maxUtmNorth = maxN,
            minLat = fallbackCenterlineLatLon.minOf { it.first } - 0.015,
            maxLat = fallbackCenterlineLatLon.maxOf { it.first } + 0.015,
            minLon = fallbackCenterlineLatLon.minOf { it.second } - 0.015,
            maxLon = fallbackCenterlineLatLon.maxOf { it.second } + 0.015
        )
    }

    private fun projectUtmKotlin(utmE: Double, utmN: Double): Triple<Double, Float, Boolean> {
        if (fallbackRibbon.size < 2) return Triple(0.0, 9999f, false)
        var bestDistSq = Double.MAX_VALUE
        var bestChain = 0.0
        var bestOffset = 9999.0
        var insideEnds = false
        for (i in 0 until fallbackRibbon.size - 1) {
            val a = fallbackRibbon[i]; val b = fallbackRibbon[i + 1]
            val dx = b.centerE - a.centerE
            val dy = b.centerN - a.centerN
            val segLenSq = dx * dx + dy * dy
            val segLen = sqrt(segLenSq)
            if (segLen < 1e-6) continue
            val tRaw = ((utmE - a.centerE) * dx + (utmN - a.centerN) * dy) / segLenSq
            val tClamped = tRaw.coerceIn(0.0, 1.0)
            val pe = a.centerE + tClamped * dx
            val pn = a.centerN + tClamped * dy
            val errE = utmE - pe
            val errN = utmN - pn
            val dSq = errE * errE + errN * errN
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                bestChain = a.chainageM + tClamped * segLen
                val rE = dy / segLen
                val rN = -dx / segLen
                val sign = if (errE * rE + errN * rN >= 0.0) 1.0 else -1.0
                bestOffset = sign * sqrt(dSq)
                val beforeStart = (i == 0 && tRaw < -1500.0 / segLen)
                val afterEnd = (i == fallbackRibbon.size - 2 && tRaw > 1.0 + 1500.0 / segLen)
                insideEnds = !(beforeStart || afterEnd)
            }
        }
        val inside = insideEnds && abs(bestOffset) <= 1500.5
        return Triple(bestChain, bestOffset.toFloat(), inside)
    }

    private fun latLonToUtm37SKotlin(lat: Double, lon: Double): Pair<Double, Double> {
        val a = 6378137.0
        val f = 1.0 / 298.257223563
        val e2 = f * (2.0 - f)
        val ep2 = e2 / (1.0 - e2)
        val k0 = 0.9996
        val lon0 = Math.toRadians(39.0)
        val phi = Math.toRadians(lat)
        val lam = Math.toRadians(lon)
        val sinP = sin(phi); val cosP = cos(phi); val tanP = tan(phi)
        val nVal = a / sqrt(1.0 - e2 * sinP * sinP)
        val tVal = tanP * tanP
        val cVal = ep2 * cosP * cosP
        val aVal = cosP * (lam - lon0)
        val mVal = a * ((1.0 - e2 / 4.0 - 3.0 * e2 * e2 / 64.0 - 5.0 * e2 * e2 * e2 / 256.0) * phi -
            (3.0 * e2 / 8.0 + 3.0 * e2 * e2 / 32.0 + 45.0 * e2 * e2 * e2 / 1024.0) * sin(2.0 * phi) +
            (15.0 * e2 * e2 / 256.0 + 45.0 * e2 * e2 * e2 / 1024.0) * sin(4.0 * phi) -
            (35.0 * e2 * e2 * e2 / 3072.0) * sin(6.0 * phi))
        val easting = 500000.0 + k0 * nVal * (aVal + (1.0 - tVal + cVal) * aVal.pow(3) / 6.0 +
            (5.0 - 18.0 * tVal + tVal * tVal + 72.0 * cVal - 58.0 * ep2) * aVal.pow(5) / 120.0)
        val northingRaw = k0 * (mVal + nVal * tanP * (aVal * aVal / 2.0 +
            (5.0 - tVal + 9.0 * cVal + 4.0 * cVal * cVal) * aVal.pow(4) / 24.0 +
            (61.0 - 58.0 * tVal + tVal * tVal + 600.0 * cVal - 330.0 * ep2) * aVal.pow(6) / 720.0))
        return easting to (if (lat < 0.0) 10000000.0 + northingRaw else northingRaw)
    }

    private fun utm37SToLatLonKotlin(easting: Double, northing: Double): Pair<Double, Double> {
        val x = easting - 500000.0
        val y = northing - 10000000.0
        val mVal = y / UTM_K0
        val mu = mVal / UTM_M_SCALE
        val sin2mu = sin(2.0 * mu)
        val cos2mu = cos(2.0 * mu)
        val sin4mu = 2.0 * sin2mu * cos2mu
        val sin6mu = sin(6.0 * mu)
        val sin8mu = sin(8.0 * mu)
        val phi1 = mu + UTM_P1 * sin2mu + UTM_P2 * sin4mu + UTM_P3 * sin6mu + UTM_P4 * sin8mu
        val sinP1 = sin(phi1)
        val cosP1 = cos(phi1)
        val tanP1 = sinP1 / cosP1
        val sin2P1 = sinP1 * sinP1
        val denomSq = 1.0 - UTM_E2 * sin2P1
        val sqrtDenom = sqrt(denomSq)
        val n1 = UTM_A / sqrtDenom
        val t1 = tanP1 * tanP1
        val c1 = UTM_EP2 * cosP1 * cosP1
        val r1 = UTM_A_ONE_M_E2 / (denomSq * sqrtDenom)
        val dVal = x / (n1 * UTM_K0)
        val d2 = dVal * dVal
        val d3 = d2 * dVal
        val d4 = d2 * d2
        val d5 = d4 * dVal
        val d6 = d4 * d2
        val t1sq = t1 * t1
        val c1sq = c1 * c1
        val latRad = phi1 - (n1 * tanP1 / r1) * (
            d2 * 0.5 -
                (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1sq - 9.0 * UTM_EP2) * d4 / 24.0 +
                (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1sq - 252.0 * UTM_EP2 - 3.0 * c1sq) * d6 / 720.0
            )
        val lonRad = UTM_LON0_RAD + (
            dVal - (1.0 + 2.0 * t1 + c1) * d3 / 6.0 +
                (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1sq + 8.0 * UTM_EP2 + 24.0 * t1sq) * d5 / 120.0
            ) / cosP1
        return Math.toDegrees(latRad) to Math.toDegrees(lonRad)
    }

    private companion object {
        const val UTM_A = 6378137.0
        const val UTM_F = 1.0 / 298.257223563
        const val UTM_E2 = UTM_F * (2.0 - UTM_F)
        const val UTM_E4 = UTM_E2 * UTM_E2
        const val UTM_E6 = UTM_E4 * UTM_E2
        const val UTM_EP2 = UTM_E2 / (1.0 - UTM_E2)
        const val UTM_K0 = 0.9996
        val UTM_LON0_RAD = Math.toRadians(39.0)
        const val UTM_A_ONE_M_E2 = UTM_A * (1.0 - UTM_E2)
        const val UTM_M_SCALE = UTM_A * (1.0 - UTM_E2 / 4.0 - 3.0 * UTM_E4 / 64.0 - 5.0 * UTM_E6 / 256.0)
        val UTM_E1 = (1.0 - sqrt(1.0 - UTM_E2)) / (1.0 + sqrt(1.0 - UTM_E2))
        val UTM_E1_2 = UTM_E1 * UTM_E1
        val UTM_E1_3 = UTM_E1_2 * UTM_E1
        val UTM_E1_4 = UTM_E1_2 * UTM_E1_2
        val UTM_P1 = 3.0 * UTM_E1 / 2.0 - 27.0 * UTM_E1_3 / 32.0
        val UTM_P2 = 21.0 * UTM_E1_2 / 16.0 - 55.0 * UTM_E1_4 / 32.0
        val UTM_P3 = 151.0 * UTM_E1_3 / 96.0
        val UTM_P4 = 1097.0 * UTM_E1_4 / 512.0
    }

    // ─── JSON Cache Serialization ───────────────────────────────────────────────

    private fun serializeBundle(bundle: C58CorridorBundle): JSONObject {
        fun coordJson(c: C58Coord) = JSONObject().apply {
            put("lat", c.lat); put("lon", c.lon)
            put("utmE", c.utmEast); put("utmN", c.utmNorth)
            put("ch", c.chainageM); put("off", c.lateralOffsetM.toDouble())
            put("in", c.insideCorridor)
        }

        val root = JSONObject()
        val clArr = JSONArray()
        bundle.centerlineLatLon.forEach { clArr.put(JSONArray().put(it.first).put(it.second)) }
        root.put("centerline", clArr)

        root.put("metadata", JSONObject().apply {
            put("routeName", bundle.metadata.routeName)
            put("isLiveOsmCenterline", bundle.metadata.isLiveOsmCenterline)
            put("extractedSectors", JSONArray(bundle.metadata.extractedSectors))
            put("extractedAtIso", bundle.metadata.extractedAtIso)
            put("osmSourceUrl", bundle.metadata.osmSourceUrl)
            put("demSourceUrl", bundle.metadata.demSourceUrl)
            put("demResolutionM", bundle.metadata.demResolutionM.toDouble())
            put("lakeLevelAslM", bundle.metadata.lakeLevelAslM.toDouble())
            put("elevMinM", bundle.metadata.elevMinM.toDouble())
            put("elevMaxM", bundle.metadata.elevMaxM.toDouble())
            put("elevMeanM", bundle.metadata.elevMeanM.toDouble())
        })

        bundle.demGrid?.let { dem ->
            val eArr = JSONArray()
            dem.elevationsM.forEach { eArr.put(it.toDouble()) }
            root.put("demGrid", JSONObject().apply {
                put("source", dem.source)
                put("resM", dem.nativeResolutionM.toDouble())
                put("uCols", dem.uCols); put("vRows", dem.vRows)
                put("minE", dem.minElevM.toDouble()); put("maxE", dem.maxElevM.toDouble())
                put("meanE", dem.meanElevM.toDouble())
                put("elevations", eArr)
            })
        }

        val rArr = JSONArray()
        bundle.roads.forEach { r ->
            rArr.put(JSONObject().apply {
                put("osmId", r.osmId); put("name", r.name)
                put("highwayType", r.highwayType); put("surface", r.surface)
                put("lanes", r.lanes); put("widthM", r.widthM.toDouble())
                put("lengthM", r.lengthM.toDouble()); put("sec", r.sectorIndex)
                put("mid", coordJson(r.midCoord))
                val nArr = JSONArray()
                r.nodes.forEach { nArr.put(coordJson(it)) }
                put("nodes", nArr)
            })
        }
        root.put("roads", rArr)

        val hArr = JSONArray()
        bundle.houses.forEach { h ->
            hArr.put(JSONObject().apply {
                put("osmId", h.osmId); put("name", h.name)
                put("bType", h.buildingType); put("amenity", h.amenity)
                put("levels", h.levels); put("heightM", h.heightM.toDouble())
                put("elevM", h.elevM.toDouble()); put("area", h.footprintAreaM2.toDouble())
                put("perim", h.perimeterM.toDouble()); put("sec", h.sectorIndex)
                put("centroid", coordJson(h.centroid))
                val pArr = JSONArray()
                h.polygon.forEach { pArr.put(coordJson(it)) }
                put("polygon", pArr)
            })
        }
        root.put("houses", hArr)

        val pArr = JSONArray()
        bundle.pois.forEach { p ->
            pArr.put(JSONObject().apply {
                put("osmId", p.osmId); put("name", p.name)
                put("cat", p.category); put("elevM", p.elevM.toDouble()); put("sec", p.sectorIndex)
                put("coord", coordJson(p.coord))
            })
        }
        root.put("pois", pArr)

        val wArr = JSONArray()
        bundle.waterPolygons.forEach { w ->
            wArr.put(JSONObject().apply {
                put("osmId", w.osmId); put("name", w.name)
                put("wType", w.waterType); put("meanE", w.meanElevM.toDouble())
                val pArr2 = JSONArray()
                w.polygon.forEach { pArr2.put(coordJson(it)) }
                put("polygon", pArr2)
            })
        }
        root.put("waterPolygons", wArr)

        return root
    }

    private fun deserializeBundle(root: JSONObject): C58CorridorBundle {
        fun jsonCoord(o: JSONObject) = C58Coord(
            lat = o.getDouble("lat"), lon = o.getDouble("lon"),
            utmEast = o.getDouble("utmE"), utmNorth = o.getDouble("utmN"),
            chainageM = o.getDouble("ch"), lateralOffsetM = o.getDouble("off").toFloat(),
            insideCorridor = o.optBoolean("in", true)
        )

        val clArr = root.optJSONArray("centerline") ?: JSONArray()
        val clPairs = ArrayList<Pair<Double, Double>>(clArr.length())
        for (i in 0 until clArr.length()) {
            val pair = clArr.getJSONArray(i)
            clPairs.add(pair.getDouble(0) to pair.getDouble(1))
        }
        if (clPairs.size >= 2) setCenterlinePairs(clPairs)

        val summary = getCorridorSummary()
        val ribbon = getCorridorRibbon()
        val sectors = buildSectorSpecs(summary)

        val demObj = root.optJSONObject("demGrid")
        val demGrid = if (demObj != null) {
            val eArr = demObj.getJSONArray("elevations")
            val vals = FloatArray(eArr.length()) { eArr.getDouble(it).toFloat() }
            C58DemGrid(
                source = demObj.getString("source"),
                nativeResolutionM = demObj.optDouble("resM", 19.0).toFloat(),
                uCols = demObj.getInt("uCols"),
                vRows = demObj.getInt("vRows"),
                elevationsM = vals,
                minElevM = demObj.getDouble("minE").toFloat(),
                maxElevM = demObj.getDouble("maxE").toFloat(),
                meanElevM = demObj.getDouble("meanE").toFloat()
            )
        } else null

        val roads = ArrayList<C58ExtractedRoad>()
        val rArr = root.optJSONArray("roads") ?: JSONArray()
        for (i in 0 until rArr.length()) {
            val o = rArr.getJSONObject(i)
            val nArr = o.getJSONArray("nodes")
            val nodes = ArrayList<C58Coord>(nArr.length())
            for (j in 0 until nArr.length()) nodes.add(jsonCoord(nArr.getJSONObject(j)))
            roads.add(
                C58ExtractedRoad(
                    osmId = o.getLong("osmId"), name = o.getString("name"),
                    highwayType = o.getString("highwayType"), surface = o.getString("surface"),
                    lanes = o.getInt("lanes"), widthM = o.getDouble("widthM").toFloat(),
                    lengthM = o.getDouble("lengthM").toFloat(), sectorIndex = o.optInt("sec", 1),
                    midCoord = jsonCoord(o.getJSONObject("mid")), nodes = nodes
                )
            )
        }

        val houses = ArrayList<C58ExtractedHouse>()
        val hArr = root.optJSONArray("houses") ?: JSONArray()
        for (i in 0 until hArr.length()) {
            val o = hArr.getJSONObject(i)
            val pArr = o.getJSONArray("polygon")
            val poly = ArrayList<C58Coord>(pArr.length())
            for (j in 0 until pArr.length()) poly.add(jsonCoord(pArr.getJSONObject(j)))
            houses.add(
                C58ExtractedHouse(
                    osmId = o.getLong("osmId"), name = o.getString("name"),
                    buildingType = o.getString("bType"), amenity = o.optString("amenity", ""),
                    levels = o.getInt("levels"), heightM = o.getDouble("heightM").toFloat(),
                    centroid = jsonCoord(o.getJSONObject("centroid")),
                    elevM = o.getDouble("elevM").toFloat(), footprintAreaM2 = o.getDouble("area").toFloat(),
                    perimeterM = o.getDouble("perim").toFloat(), sectorIndex = o.optInt("sec", 1),
                    polygon = poly
                )
            )
        }

        val pois = ArrayList<C58ExtractedPoi>()
        val pArr = root.optJSONArray("pois") ?: JSONArray()
        for (i in 0 until pArr.length()) {
            val o = pArr.getJSONObject(i)
            pois.add(
                C58ExtractedPoi(
                    osmId = o.getLong("osmId"), name = o.getString("name"),
                    category = o.getString("cat"), coord = jsonCoord(o.getJSONObject("coord")),
                    elevM = o.getDouble("elevM").toFloat(), sectorIndex = o.optInt("sec", 1)
                )
            )
        }

        val waterPolys = ArrayList<C58WaterPolygon>()
        val wArr = root.optJSONArray("waterPolygons") ?: JSONArray()
        for (i in 0 until wArr.length()) {
            val o = wArr.getJSONObject(i)
            val pArr2 = o.getJSONArray("polygon")
            val poly = ArrayList<C58Coord>(pArr2.length())
            for (j in 0 until pArr2.length()) poly.add(jsonCoord(pArr2.getJSONObject(j)))
            waterPolys.add(
                C58WaterPolygon(
                    osmId = o.getLong("osmId"), name = o.getString("name"),
                    waterType = o.getString("wType"), polygon = poly,
                    meanElevM = o.getDouble("meanE").toFloat()
                )
            )
        }

        val mObj = root.getJSONObject("metadata")
        val secJson = mObj.optJSONArray("extractedSectors") ?: JSONArray()
        val secList = (0 until secJson.length()).map { secJson.getInt(it) }
        val leftCount = houses.count { it.centroid.lateralOffsetM < 0f }

        val metadata = C58CorridorMetadata(
            routeName = mObj.optString("routeName", "Route C58 (±1.5km Non-Box Corridor)"),
            isLiveOsmCenterline = mObj.optBoolean("isLiveOsmCenterline", false),
            extractedSectors = secList,
            extractedAtIso = mObj.optString("extractedAtIso", ""),
            osmSourceUrl = mObj.optString("osmSourceUrl", ""),
            demSourceUrl = mObj.optString("demSourceUrl", ""),
            demResolutionM = mObj.optDouble("demResolutionM", 19.0).toFloat(),
            lakeLevelAslM = mObj.optDouble("lakeLevelAslM", 602.5).toFloat(),
            roadCount = roads.size,
            totalRoadLengthKm = roads.sumOf { it.lengthM.toDouble() }.toFloat() / 1000f,
            houseCount = houses.size,
            leftSideHouseCount = leftCount,
            rightSideHouseCount = houses.size - leftCount,
            totalHouseAreaM2 = houses.sumOf { it.footprintAreaM2.toDouble() }.toFloat(),
            waterCount = waterPolys.size,
            poiCount = pois.size,
            elevMinM = mObj.optDouble("elevMinM", 596.0).toFloat(),
            elevMaxM = mObj.optDouble("elevMaxM", 1820.0).toFloat(),
            elevMeanM = mObj.optDouble("elevMeanM", 1240.0).toFloat()
        )

        return C58CorridorBundle(
            summary = summary,
            metadata = metadata,
            centerlineLatLon = clPairs.ifEmpty { fallbackCenterlineLatLon },
            ribbon = ribbon,
            sectors = sectors,
            demGrid = demGrid,
            roads = roads,
            houses = houses,
            pois = pois,
            waterPolygons = waterPolys
        )
    }
}
