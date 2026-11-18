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

class MagadiTownRepository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "magadi_town_3x5km_1m_v2_cache.json")

    fun getNativeBounds(): MagadiTownBounds {
        return try {
            if (NativeEngineBridge.isLoaded()) {
                val arr = NativeEngineBridge.nativeGetMagadiTownBounds()
                if (arr.size >= 12) {
                    MagadiTownBounds(
                        centerLat = arr[0],
                        centerLon = arr[1],
                        widthM = arr[2],
                        heightM = arr[3],
                        areaKm2 = (arr[2] * arr[3]) / 1_000_000.0,
                        gridResolutionM = 1.0,
                        gridCols = arr[2].toInt(),
                        gridRows = arr[3].toInt(),
                        totalGridCells = arr[2].toLong() * arr[3].toLong(),
                        minLat = arr[4],
                        maxLat = arr[5],
                        minLon = arr[6],
                        maxLon = arr[7],
                        originUtmEast = arr[10],
                        originUtmNorth = arr[11]
                    )
                } else MagadiTownBounds()
            } else {
                MagadiTownBounds()
            }
        } catch (_: Throwable) {
            MagadiTownBounds()
        }
    }

    fun convertLatLonToCoord(lat: Double, lon: Double, bounds: MagadiTownBounds): MagadiGeoCoord {
        if (NativeEngineBridge.isLoaded()) {
            try {
                val res = NativeEngineBridge.nativeMagadiTownLatLonToLocalAndUtm(lat, lon)
                if (res.size >= 4) {
                    val xM = res[0].toFloat()
                    val yM = res[1].toFloat()
                    return MagadiGeoCoord(
                        lat = lat,
                        lon = lon,
                        xM = xM,
                        yM = yM,
                        utmEast = res[2],
                        utmNorth = res[3],
                        cellCol = floor(xM).toInt().coerceIn(0, bounds.gridCols - 1),
                        cellRow = floor(yM).toInt().coerceIn(0, bounds.gridRows - 1)
                    )
                }
            } catch (_: Throwable) {
            }
        }
        val mPerLat = 110574.0
        val mPerLon = 111320.0 * cos(Math.toRadians(bounds.centerLat))
        val xM = ((lon - bounds.minLon) * mPerLon).toFloat()
        val yM = ((lat - bounds.minLat) * mPerLat).toFloat()
        return MagadiGeoCoord(
            lat = lat,
            lon = lon,
            xM = xM,
            yM = yM,
            utmEast = bounds.originUtmEast + xM,
            utmNorth = bounds.originUtmNorth + yM,
            cellCol = floor(xM).toInt().coerceIn(0, bounds.gridCols - 1),
            cellRow = floor(yM).toInt().coerceIn(0, bounds.gridRows - 1)
        )
    }

    fun inspect1mCell(xM: Float, yM: Float): Inspected1mCell {
        val clampedX = xM.coerceIn(0f, 3000f)
        val clampedY = yM.coerceIn(0f, 5000f)
        if (NativeEngineBridge.isLoaded()) {
            try {
                val d = NativeEngineBridge.nativeGetMagadiTownTelemetryAt(clampedX, clampedY)
                if (d.size >= 12) {
                    return Inspected1mCell(
                        xM = d[0].toFloat(),
                        yM = d[1].toFloat(),
                        cellCol = d[2].toInt(),
                        cellRow = d[3].toInt(),
                        lat = d[4],
                        lon = d[5],
                        utmEast = d[6],
                        utmNorth = d[7],
                        elevM = d[8].toFloat(),
                        slopeDeg = d[9].toFloat(),
                        waterDepthM = d[10].toFloat(),
                        isWater = d[11] > 0.5
                    )
                } else if (d.size >= 10) {
                    return Inspected1mCell(
                        xM = d[0].toFloat(),
                        yM = d[1].toFloat(),
                        cellCol = d[2].toInt(),
                        cellRow = d[3].toInt(),
                        lat = d[4],
                        lon = d[5],
                        utmEast = d[6],
                        utmNorth = d[7],
                        elevM = d[8].toFloat(),
                        slopeDeg = d[9].toFloat()
                    )
                }
            } catch (_: Throwable) {
            }
        }
        val b = getNativeBounds()
        val lat = b.minLat + clampedY / 110574.0
        val lon = b.minLon + clampedX / (111320.0 * cos(Math.toRadians(b.centerLat)))
        return Inspected1mCell(
            xM = clampedX,
            yM = clampedY,
            cellCol = floor(clampedX).toInt().coerceIn(0, 2999),
            cellRow = floor(clampedY).toInt().coerceIn(0, 4999),
            lat = lat,
            lon = lon,
            utmEast = b.originUtmEast + clampedX,
            utmNorth = b.originUtmNorth + clampedY,
            elevM = 602.0f,
            slopeDeg = 1.5f
        )
    }

    suspend fun loadBinaryAssetBundle(): MagadiTownBundle? = withContext(Dispatchers.IO) {
        return@withContext try {
            context.assets.open("terrain_grid/magadi_town_3x5km.bin").use { stream ->
                val bundle = parseBinaryStream(stream)
                syncBundleToCpp(bundle)
                bundle
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parseBinaryStream(stream: InputStream): MagadiTownBundle {
        val bytes = stream.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val magic = ByteArray(4)
        buf.get(magic)
        check(String(magic, Charsets.US_ASCII) == "MAGT") { "Invalid MAGT binary header" }
        buf.short // version
        buf.short // flags

        fun readStr(): String {
            val len = buf.short.toInt() and 0xFFFF
            if (len == 0) return ""
            val arr = ByteArray(len)
            buf.get(arr)
            return String(arr, Charsets.UTF_8)
        }

        val centerLat = buf.double
        val centerLon = buf.double
        val widthM = buf.double
        val heightM = buf.double
        val minLat = buf.double
        val maxLat = buf.double
        val minLon = buf.double
        val maxLon = buf.double
        val originUtmE = buf.double
        val originUtmN = buf.double

        val bounds = MagadiTownBounds(
            centerLat = centerLat,
            centerLon = centerLon,
            widthM = widthM,
            heightM = heightM,
            areaKm2 = (widthM * heightM) / 1_000_000.0,
            gridResolutionM = 1.0,
            gridCols = widthM.toInt(),
            gridRows = heightM.toInt(),
            totalGridCells = widthM.toLong() * heightM.toLong(),
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            originUtmEast = originUtmE,
            originUtmNorth = originUtmN
        )

        val mPerLat = 110574.0
        val mPerLon = 111320.0 * cos(Math.toRadians(centerLat))

        fun readLocalCoord(): MagadiGeoCoord {
            val xM = buf.float
            val yM = buf.float
            val lat = minLat + yM / mPerLat
            val lon = minLon + xM / mPerLon
            return MagadiGeoCoord(
                lat = lat,
                lon = lon,
                xM = xM,
                yM = yM,
                utmEast = originUtmE + xM,
                utmNorth = originUtmN + yM,
                cellCol = floor(xM).toInt().coerceIn(0, bounds.gridCols - 1),
                cellRow = floor(yM).toInt().coerceIn(0, bounds.gridRows - 1)
            )
        }

        val placeName = readStr()
        val extractedAtIso = readStr()
        val osmSourceUrl = readStr()
        val demSourceUrl = readStr()
        val demResolutionM = buf.float
        val lakeLevelAslM = buf.float
        val elevMinM = buf.float
        val elevMaxM = buf.float
        val elevMeanM = buf.float

        val demSource = readStr()
        val demCols = buf.int
        val demRows = buf.int
        val demNativeRes = buf.float
        val demMinE = buf.float
        val demMaxE = buf.float
        val demMeanE = buf.float
        val demCount = demCols * demRows
        val demElevs = FloatArray(demCount)
        for (i in 0 until demCount) {
            demElevs[i] = buf.float
        }
        val demGrid = if (demCount > 0) {
            DemControlGrid(
                source = demSource,
                nativeResolutionM = demNativeRes,
                cols = demCols,
                rows = demRows,
                elevationsM = demElevs,
                minElevM = demMinE,
                maxElevM = demMaxE,
                meanElevM = demMeanE
            )
        } else null

        val roadCount = buf.int
        val roads = ArrayList<ExtractedRoadWay>(roadCount)
        for (i in 0 until roadCount) {
            val osmId = buf.long
            val name = readStr()
            val ref = readStr()
            val hType = readStr()
            val surface = readStr()
            val lanes = buf.get().toInt() and 0xFF
            val rFlags = buf.get().toInt() and 0xFF
            val widthR = buf.float
            val lengthM = buf.float
            val nCount = buf.int
            val nodes = ArrayList<MagadiGeoCoord>(nCount)
            for (k in 0 until nCount) nodes.add(readLocalCoord())
            roads.add(
                ExtractedRoadWay(
                    osmId = osmId,
                    name = name,
                    highwayType = hType,
                    surface = surface,
                    lanes = lanes,
                    oneWay = (rFlags and 1) != 0,
                    widthM = widthR,
                    lengthM = lengthM,
                    nodes = nodes,
                    ref = ref,
                    isBridge = (rFlags and 2) != 0,
                    isRoundabout = (rFlags and 4) != 0
                )
            )
        }

        val bldgCount = buf.int
        val buildings = ArrayList<ExtractedBuilding>(bldgCount)
        for (i in 0 until bldgCount) {
            val osmId = buf.long
            val name = readStr()
            val bType = readStr()
            val amenity = readStr()
            val levels = buf.get().toInt() and 0xFF
            val heightM = buf.float
            val elevM = buf.float
            val areaM2 = buf.float
            val perimM = buf.float
            val centX = buf.float
            val centY = buf.float
            val polyCount = buf.int
            val poly = ArrayList<MagadiGeoCoord>(polyCount)
            for (k in 0 until polyCount) poly.add(readLocalCoord())
            val centroid = MagadiGeoCoord(
                lat = minLat + centY / mPerLat,
                lon = minLon + centX / mPerLon,
                xM = centX,
                yM = centY,
                utmEast = originUtmE + centX,
                utmNorth = originUtmN + centY,
                cellCol = floor(centX).toInt().coerceIn(0, bounds.gridCols - 1),
                cellRow = floor(centY).toInt().coerceIn(0, bounds.gridRows - 1)
            )
            buildings.add(
                ExtractedBuilding(
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
                    polygon = poly
                )
            )
        }

        val waterCount = buf.int
        val waters = ArrayList<ExtractedWaterPolygon>(waterCount)
        for (i in 0 until waterCount) {
            val osmId = buf.long
            val name = readStr()
            val wType = readStr()
            val meanE = buf.float
            val polyCount = buf.int
            val poly = ArrayList<MagadiGeoCoord>(polyCount)
            for (k in 0 until polyCount) poly.add(readLocalCoord())
            waters.add(
                ExtractedWaterPolygon(
                    osmId = osmId,
                    name = name,
                    waterType = wType,
                    polygon = poly,
                    meanElevM = meanE
                )
            )
        }

        val poiCount = buf.int
        val pois = ArrayList<ExtractedPoiMetadata>(poiCount)
        for (i in 0 until poiCount) {
            val osmId = buf.long
            val name = readStr()
            val cat = readStr()
            val coord = readLocalCoord()
            val elevM = buf.float
            pois.add(
                ExtractedPoiMetadata(
                    osmId = osmId,
                    name = name,
                    category = cat,
                    coord = coord,
                    elevM = elevM
                )
            )
        }

        val metadata = MagadiTownMetadata(
            placeName = placeName,
            extractedAtIso = extractedAtIso,
            osmSourceUrl = "Binary Pack ($osmSourceUrl)",
            demSourceUrl = demSourceUrl,
            demResolutionM = demResolutionM,
            lakeLevelAslM = lakeLevelAslM,
            isLiveExtracted = false,
            roadCount = roads.size,
            totalRoadLengthKm = roads.sumOf { it.lengthM.toDouble() }.toFloat() / 1000f,
            buildingCount = buildings.size,
            totalBuildingFootprintM2 = buildings.sumOf { it.footprintAreaM2.toDouble() }.toFloat(),
            waterCount = waters.size,
            poiCount = pois.size,
            elevMinM = elevMinM,
            elevMaxM = elevMaxM,
            elevMeanM = elevMeanM,
            isBinaryServed = true
        )

        return MagadiTownBundle(
            bounds = bounds,
            metadata = metadata,
            demGrid = demGrid,
            roads = roads,
            buildings = buildings,
            pois = pois,
            waterPolygons = waters
        )
    }

    suspend fun loadCachedBundle(): MagadiTownBundle? = withContext(Dispatchers.IO) {
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

    fun syncLakeLevelToCpp(lakeLevelAslM: Float, waterPolygons: List<ExtractedWaterPolygon>) {
        if (!NativeEngineBridge.isLoaded()) return
        try {
            val metaList = ArrayList<Int>(waterPolygons.size * 2)
            val coordList = ArrayList<Float>()
            for (wp in waterPolygons) {
                if (wp.polygon.size < 3) continue
                metaList.add(wp.polygon.size)
                val isSalt = if (wp.waterType.contains("salt") || wp.waterType.contains("mud")) 1 else 0
                metaList.add(isSalt)
                for (pt in wp.polygon) {
                    coordList.add(pt.xM)
                    coordList.add(pt.yM)
                }
            }
            NativeEngineBridge.nativeSetMagadiTownLakeAndWater(
                lakeLevelAslM,
                metaList.toIntArray(),
                coordList.toFloatArray()
            )
        } catch (_: Throwable) {
        }
    }

    fun syncBundleToCpp(bundle: MagadiTownBundle) {
        if (!NativeEngineBridge.isLoaded()) return
        try {
            bundle.demGrid?.let { dem ->
                NativeEngineBridge.nativeSetMagadiTownDemGrid(dem.cols, dem.rows, dem.elevationsM)
            }
            syncLakeLevelToCpp(bundle.metadata.lakeLevelAslM, bundle.waterPolygons)

            // Pack road segments: [x0, y0, x1, y1, halfWidthM] * N
            val segCount = bundle.roads.sumOf { max(0, it.nodes.size - 1) }
            val roadFloats = FloatArray(segCount * 5)
            var rIdx = 0
            for (road in bundle.roads) {
                val hw = (road.widthM * 0.5f).coerceAtLeast(2.0f)
                for (i in 0 until road.nodes.size - 1) {
                    val p0 = road.nodes[i]
                    val p1 = road.nodes[i + 1]
                    roadFloats[rIdx++] = p0.xM
                    roadFloats[rIdx++] = p0.yM
                    roadFloats[rIdx++] = p1.xM
                    roadFloats[rIdx++] = p1.yM
                    roadFloats[rIdx++] = hw
                }
            }
            // Pack building bounding rects: [minX, minY, maxX, maxY] * M
            val bldgFloats = FloatArray(bundle.buildings.size * 4)
            var bIdx = 0
            for (b in bundle.buildings) {
                if (b.polygon.isEmpty()) continue
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (v in b.polygon) {
                    if (v.xM < minX) minX = v.xM
                    if (v.yM < minY) minY = v.yM
                    if (v.xM > maxX) maxX = v.xM
                    if (v.yM > maxY) maxY = v.yM
                }
                bldgFloats[bIdx++] = minX
                bldgFloats[bIdx++] = minY
                bldgFloats[bIdx++] = maxX
                bldgFloats[bIdx++] = maxY
            }
            NativeEngineBridge.nativeSetMagadiTownFootprints(
                roadFloats.copyOf(rIdx),
                bldgFloats.copyOf(bIdx)
            )
        } catch (_: Throwable) {
        }
    }

    fun exportGeoJson(bundle: MagadiTownBundle): JSONObject {
        val features = JSONArray()
        for (road in bundle.roads) {
            val coords = JSONArray()
            road.nodes.forEach { pt ->
                coords.put(JSONArray().put(pt.lon).put(pt.lat))
            }
            features.put(
                JSONObject().apply {
                    put("type", "Feature")
                    put("id", "way/${road.osmId}")
                    put("properties", JSONObject().apply {
                        put("feature_class", "road")
                        put("name", road.name)
                        put("highway", road.highwayType)
                        put("surface", road.surface)
                        put("lanes", road.lanes)
                        put("length_m", road.lengthM)
                    })
                    put("geometry", JSONObject().apply {
                        put("type", "LineString")
                        put("coordinates", coords)
                    })
                }
            )
        }
        for (b in bundle.buildings) {
            val ring = JSONArray()
            b.polygon.forEach { pt ->
                ring.put(JSONArray().put(pt.lon).put(pt.lat))
            }
            if (b.polygon.isNotEmpty() && (b.polygon.first().lat != b.polygon.last().lat || b.polygon.first().lon != b.polygon.last().lon)) {
                ring.put(JSONArray().put(b.polygon.first().lon).put(b.polygon.first().lat))
            }
            features.put(
                JSONObject().apply {
                    put("type", "Feature")
                    put("id", "way/${b.osmId}")
                    put("properties", JSONObject().apply {
                        put("feature_class", "building")
                        put("name", b.name)
                        put("building", b.buildingType)
                        put("amenity", b.amenity)
                        put("height_m", b.heightM)
                        put("footprint_area_m2", b.footprintAreaM2)
                        put("elev_m", b.elevM)
                        put("grid_1m_col", b.centroid.cellCol)
                        put("grid_1m_row", b.centroid.cellRow)
                        put("utm37s_east", b.centroid.utmEast)
                        put("utm37s_north", b.centroid.utmNorth)
                    })
                    put("geometry", JSONObject().apply {
                        put("type", "Polygon")
                        put("coordinates", JSONArray().put(ring))
                    })
                }
            )
        }
        return JSONObject().apply {
            put("type", "FeatureCollection")
            put("name", "Magadi Town 3x5km 1m Grid Extract")
            put("metadata", JSONObject().apply {
                put("place", bundle.metadata.placeName)
                put("width_m", bundle.bounds.widthM)
                put("height_m", bundle.bounds.heightM)
                put("grid_resolution_m", bundle.bounds.gridResolutionM)
                put("total_1m_cells", bundle.bounds.totalGridCells)
                put("dem_resolution_m", bundle.metadata.demResolutionM)
                put("lake_level_asl_m", bundle.metadata.lakeLevelAslM)
                put("extracted_at", bundle.metadata.extractedAtIso)
                put("roads", bundle.roads.size)
                put("buildings", bundle.buildings.size)
                put("water_bodies", bundle.waterPolygons.size)
            })
            put("features", features)
        }
    }

    private fun serializeBundle(bundle: MagadiTownBundle): JSONObject {
        fun coordToJson(c: MagadiGeoCoord) = JSONObject().apply {
            put("lat", c.lat); put("lon", c.lon)
            put("xM", c.xM.toDouble()); put("yM", c.yM.toDouble())
            put("utmE", c.utmEast); put("utmN", c.utmNorth)
            put("col", c.cellCol); put("row", c.cellRow)
        }

        val root = JSONObject()
        root.put("bounds", JSONObject().apply {
            put("centerLat", bundle.bounds.centerLat); put("centerLon", bundle.bounds.centerLon)
            put("widthM", bundle.bounds.widthM); put("heightM", bundle.bounds.heightM)
            put("minLat", bundle.bounds.minLat); put("maxLat", bundle.bounds.maxLat)
            put("minLon", bundle.bounds.minLon); put("maxLon", bundle.bounds.maxLon)
            put("originUtmE", bundle.bounds.originUtmEast); put("originUtmN", bundle.bounds.originUtmNorth)
        })
        root.put("metadata", JSONObject().apply {
            put("placeName", bundle.metadata.placeName)
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
            val arr = JSONArray()
            dem.elevationsM.forEach { arr.put(it.toDouble()) }
            root.put("demGrid", JSONObject().apply {
                put("source", dem.source)
                put("resM", dem.nativeResolutionM.toDouble())
                put("cols", dem.cols); put("rows", dem.rows)
                put("minE", dem.minElevM.toDouble()); put("maxE", dem.maxElevM.toDouble())
                put("meanE", dem.meanElevM.toDouble())
                put("elevations", arr)
            })
        }
        val rArr = JSONArray()
        for (r in bundle.roads) {
            rArr.put(JSONObject().apply {
                put("osmId", r.osmId); put("name", r.name)
                put("highwayType", r.highwayType); put("surface", r.surface)
                put("lanes", r.lanes); put("oneWay", r.oneWay)
                put("widthM", r.widthM.toDouble()); put("lengthM", r.lengthM.toDouble())
                val nArr = JSONArray()
                r.nodes.forEach { nArr.put(coordToJson(it)) }
                put("nodes", nArr)
            })
        }
        root.put("roads", rArr)

        val bArr = JSONArray()
        for (b in bundle.buildings) {
            bArr.put(JSONObject().apply {
                put("osmId", b.osmId); put("name", b.name)
                put("buildingType", b.buildingType); put("amenity", b.amenity)
                put("levels", b.levels); put("heightM", b.heightM.toDouble())
                put("elevM", b.elevM.toDouble()); put("area", b.footprintAreaM2.toDouble())
                put("perim", b.perimeterM.toDouble())
                put("centroid", coordToJson(b.centroid))
                val pArr = JSONArray()
                b.polygon.forEach { pArr.put(coordToJson(it)) }
                put("polygon", pArr)
            })
        }
        root.put("buildings", bArr)

        val wArr = JSONArray()
        for (w in bundle.waterPolygons) {
            wArr.put(JSONObject().apply {
                put("osmId", w.osmId); put("name", w.name)
                put("waterType", w.waterType); put("meanElevM", w.meanElevM.toDouble())
                val pArr = JSONArray()
                w.polygon.forEach { pArr.put(coordToJson(it)) }
                put("polygon", pArr)
            })
        }
        root.put("waterPolygons", wArr)

        val pArr = JSONArray()
        for (p in bundle.pois) {
            pArr.put(JSONObject().apply {
                put("osmId", p.osmId); put("name", p.name)
                put("category", p.category); put("elevM", p.elevM.toDouble())
                put("coord", coordToJson(p.coord))
            })
        }
        root.put("pois", pArr)
        return root
    }

    private fun deserializeBundle(root: JSONObject): MagadiTownBundle {
        fun jsonToCoord(o: JSONObject) = MagadiGeoCoord(
            lat = o.getDouble("lat"), lon = o.getDouble("lon"),
            xM = o.getDouble("xM").toFloat(), yM = o.getDouble("yM").toFloat(),
            utmEast = o.getDouble("utmE"), utmNorth = o.getDouble("utmN"),
            cellCol = o.getInt("col"), cellRow = o.getInt("row")
        )

        val bObj = root.getJSONObject("bounds")
        val bounds = MagadiTownBounds(
            centerLat = bObj.getDouble("centerLat"), centerLon = bObj.getDouble("centerLon"),
            widthM = bObj.getDouble("widthM"), heightM = bObj.getDouble("heightM"),
            minLat = bObj.getDouble("minLat"), maxLat = bObj.getDouble("maxLat"),
            minLon = bObj.getDouble("minLon"), maxLon = bObj.getDouble("maxLon"),
            originUtmEast = bObj.getDouble("originUtmE"), originUtmNorth = bObj.getDouble("originUtmN")
        )
        val demObj = root.optJSONObject("demGrid")
        val demGrid = if (demObj != null) {
            val eArr = demObj.getJSONArray("elevations")
            val vals = FloatArray(eArr.length()) { idx -> eArr.getDouble(idx).toFloat() }
            DemControlGrid(
                source = demObj.getString("source"),
                nativeResolutionM = demObj.optDouble("resM", 9.5).toFloat(),
                cols = demObj.getInt("cols"),
                rows = demObj.getInt("rows"),
                elevationsM = vals,
                minElevM = demObj.getDouble("minE").toFloat(),
                maxElevM = demObj.getDouble("maxE").toFloat(),
                meanElevM = demObj.getDouble("meanE").toFloat()
            )
        } else null

        val rArr = root.getJSONArray("roads")
        val roads = (0 until rArr.length()).map { i ->
            val ro = rArr.getJSONObject(i)
            val nArr = ro.getJSONArray("nodes")
            ExtractedRoadWay(
                osmId = ro.getLong("osmId"), name = ro.getString("name"),
                highwayType = ro.getString("highwayType"), surface = ro.getString("surface"),
                lanes = ro.getInt("lanes"), oneWay = ro.getBoolean("oneWay"),
                widthM = ro.getDouble("widthM").toFloat(), lengthM = ro.getDouble("lengthM").toFloat(),
                nodes = (0 until nArr.length()).map { j -> jsonToCoord(nArr.getJSONObject(j)) }
            )
        }

        val bArr = root.getJSONArray("buildings")
        val buildings = (0 until bArr.length()).map { i ->
            val bo = bArr.getJSONObject(i)
            val pArr = bo.getJSONArray("polygon")
            ExtractedBuilding(
                osmId = bo.getLong("osmId"), name = bo.getString("name"),
                buildingType = bo.getString("buildingType"), amenity = bo.optString("amenity", ""),
                levels = bo.getInt("levels"), heightM = bo.getDouble("heightM").toFloat(),
                centroid = jsonToCoord(bo.getJSONObject("centroid")),
                elevM = bo.getDouble("elevM").toFloat(),
                footprintAreaM2 = bo.getDouble("area").toFloat(),
                perimeterM = bo.getDouble("perim").toFloat(),
                polygon = (0 until pArr.length()).map { j -> jsonToCoord(pArr.getJSONObject(j)) }
            )
        }

        val wArr = root.optJSONArray("waterPolygons") ?: JSONArray()
        val waterPolygons = (0 until wArr.length()).map { i ->
            val wo = wArr.getJSONObject(i)
            val pArr = wo.getJSONArray("polygon")
            ExtractedWaterPolygon(
                osmId = wo.getLong("osmId"),
                name = wo.getString("name"),
                waterType = wo.getString("waterType"),
                meanElevM = wo.getDouble("meanElevM").toFloat(),
                polygon = (0 until pArr.length()).map { j -> jsonToCoord(pArr.getJSONObject(j)) }
            )
        }

        val pArr = root.getJSONArray("pois")
        val pois = (0 until pArr.length()).map { i ->
            val po = pArr.getJSONObject(i)
            ExtractedPoiMetadata(
                osmId = po.getLong("osmId"), name = po.getString("name"),
                category = po.getString("category"),
                coord = jsonToCoord(po.getJSONObject("coord")),
                elevM = po.getDouble("elevM").toFloat()
            )
        }

        val mObj = root.getJSONObject("metadata")
        val metadata = MagadiTownMetadata(
            placeName = mObj.getString("placeName"),
            extractedAtIso = mObj.getString("extractedAtIso"),
            osmSourceUrl = mObj.optString("osmSourceUrl", ""),
            demSourceUrl = mObj.optString("demSourceUrl", ""),
            demResolutionM = mObj.optDouble("demResolutionM", 9.5).toFloat(),
            lakeLevelAslM = mObj.optDouble("lakeLevelAslM", 602.5).toFloat(),
            isLiveExtracted = true,
            roadCount = roads.size,
            totalRoadLengthKm = roads.sumOf { it.lengthM.toDouble() }.toFloat() / 1000f,
            buildingCount = buildings.size,
            totalBuildingFootprintM2 = buildings.sumOf { it.footprintAreaM2.toDouble() }.toFloat(),
            waterCount = waterPolygons.size,
            poiCount = pois.size,
            elevMinM = mObj.getDouble("elevMinM").toFloat(),
            elevMaxM = mObj.getDouble("elevMaxM").toFloat(),
            elevMeanM = mObj.getDouble("elevMeanM").toFloat()
        )
        return MagadiTownBundle(bounds, metadata, demGrid, roads, buildings, pois, waterPolygons)
    }
}
