package com.example.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.engine.NativeEngineBridge
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.*

class MagadiTownRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

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

    suspend fun extractLiveMagadiTownData(
        onProgress: (stage: String, progress: Float) -> Unit
    ): Result<MagadiTownBundle> = withContext(Dispatchers.IO) {
        try {
            val bounds = getNativeBounds()

            onProgress("Querying OpenStreetMap Overpass (Roads, Buildings, Lake & Waterbodies)...", 0.16f)
            val (osmJson, osmUrl) = fetchOverpassJson(bounds)

            onProgress("Fetching high-resolution Terrarium ~9.5m / SRTM 30m DEM tiles...", 0.48f)
            val (demGrid, demUrl) = fetchBestHighResDemGrid(bounds)

            onProgress("Calibrating C++ 1m DEM, Lake Magadi shoreline & UTM 37S projection...", 0.80f)
            if (demGrid != null && NativeEngineBridge.isLoaded()) {
                NativeEngineBridge.nativeSetMagadiTownDemGrid(
                    demGrid.cols,
                    demGrid.rows,
                    demGrid.elevationsM
                )
            }

            val parsed = parseOverpassIntoBundle(
                osmJson = osmJson,
                osmUrl = osmUrl,
                demGrid = demGrid,
                demUrl = demUrl,
                bounds = bounds
            )

            syncBundleToCpp(parsed)

            onProgress("Saving high-res spatial cache...", 0.96f)
            cacheFile.writeText(serializeBundle(parsed).toString())

            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun syncLakeLevelToCpp(lakeLevelAslM: Float, waterPolygons: List<ExtractedWaterPolygon>) {
        if (!NativeEngineBridge.isLoaded()) return
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
    }

    fun syncBundleToCpp(bundle: MagadiTownBundle) {
        if (!NativeEngineBridge.isLoaded()) return
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
    }

    private fun fetchOverpassJson(bounds: MagadiTownBounds): Pair<JSONObject, String> {
        val bboxStr = String.format(
            Locale.US,
            "%.6f,%.6f,%.6f,%.6f",
            bounds.minLat,
            bounds.minLon,
            bounds.maxLat,
            bounds.maxLon
        )
        val query = """
            [out:json][timeout:30];
            (
              way["highway"]($bboxStr);
              way["building"]($bboxStr);
              way["natural"="water"]($bboxStr);
              relation["natural"="water"]($bboxStr);
              way["natural"="wetland"]($bboxStr);
              way["natural"="mud"]($bboxStr);
              way["landuse"="salt_pond"]($bboxStr);
              relation["landuse"="salt_pond"]($bboxStr);
              way["landuse"="reservoir"]($bboxStr);
              node["place"]($bboxStr);
              node["amenity"]($bboxStr);
              node["man_made"]($bboxStr);
              node["shop"]($bboxStr);
              node["aeroway"]($bboxStr);
              node["tourism"]($bboxStr);
            );
            (._;>;);
            out body;
        """.trimIndent()

        val endpoints = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
        )

        var lastError: Exception? = null
        for (endpoint in endpoints) {
            try {
                val formBody = FormBody.Builder()
                    .add("data", query)
                    .build()
                val req = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "Apex3DAssets-MagadiGIS/1.0")
                    .post(formBody)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Overpass HTTP ${resp.code}")
                    }
                    val body = resp.body?.string() ?: throw IllegalStateException("Empty Overpass body")
                    return JSONObject(body) to endpoint
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException("OSM Overpass extraction failed: ${lastError?.message ?: "Network unavailable"}")
    }

    private fun fetchBestHighResDemGrid(bounds: MagadiTownBounds): Pair<DemControlGrid?, String> {
        // Tier 1: Mapzen / AWS Terrarium Elevation PNG Tiles @ Zoom 14 (~9.5 m/px real pixel resolution!)
        fetchTerrariumZoom14Grid(bounds)?.let { return it }

        // Tier 2: OpenTopoData SRTM GL1 30m DEM (batched 12 x 16 = 192 samples)
        fetchOpenTopoDataSrtm30Grid(bounds)?.let { return it }

        // Tier 3: Batched Open-Meteo Copernicus DEM 90m fallback (20 x 28 = 560 samples)
        return fetchOpenMeteoDemGrid(bounds)
    }

    private fun fetchTerrariumZoom14Grid(bounds: MagadiTownBounds): Pair<DemControlGrid, String>? {
        return try {
            val z = 14
            val n = 1 shl z

            fun lonToTileX(lon: Double): Double = ((lon + 180.0) / 360.0) * n
            fun latToTileY(lat: Double): Double {
                val rad = Math.toRadians(lat)
                return ((1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0) * n
            }

            val minTX = floor(lonToTileX(bounds.minLon)).toInt()
            val maxTX = floor(lonToTileX(bounds.maxLon)).toInt()
            val minTY = floor(latToTileY(bounds.maxLat)).toInt()
            val maxTY = floor(latToTileY(bounds.minLat)).toInt()

            val tiles = HashMap<Pair<Int, Int>, Bitmap>()
            for (ty in minTY..maxTY) {
                for (tx in minTX..maxTX) {
                    val url = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/$z/$tx/$ty.png"
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Apex3DAssets-MagadiGIS/1.0")
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return null
                        val bmp = resp.body?.byteStream()?.use { BitmapFactory.decodeStream(it) } ?: return null
                        tiles[tx to ty] = bmp
                    }
                }
            }
            if (tiles.isEmpty()) return null

            // Sample high-density 120 x 200 DEM control matrix (~25m spacing directly from ~9.5m/px tiles)
            val cols = 120
            val rows = 200
            val vals = FloatArray(cols * rows)
            var minE = Float.MAX_VALUE
            var maxE = -Float.MAX_VALUE
            var sumE = 0.0

            for (r in 0 until rows) {
                val lat = bounds.minLat + (r.toDouble() / (rows - 1)) * (bounds.maxLat - bounds.minLat)
                val worldTy = latToTileY(lat)
                val ty = floor(worldTy).toInt().coerceIn(minTY, maxTY)
                val py = ((worldTy - ty) * 256.0).toInt().coerceIn(0, 255)
                for (c in 0 until cols) {
                    val lon = bounds.minLon + (c.toDouble() / (cols - 1)) * (bounds.maxLon - bounds.minLon)
                    val worldTx = lonToTileX(lon)
                    val tx = floor(worldTx).toInt().coerceIn(minTX, maxTX)
                    val px = ((worldTx - tx) * 256.0).toInt().coerceIn(0, 255)
                    val bmp = tiles[tx to ty] ?: continue
                    val argb = bmp.getPixel(px, py)
                    val red = (argb shr 16) and 0xFF
                    val green = (argb shr 8) and 0xFF
                    val blue = argb and 0xFF
                    val elev = ((red * 256.0) + green + (blue / 256.0) - 32768.0).toFloat()
                    vals[r * cols + c] = elev
                    if (elev < minE) minE = elev
                    if (elev > maxE) maxE = elev
                    sumE += elev
                }
            }
            tiles.values.forEach { it.recycle() }

            if (minE < 300f || maxE > 3000f || maxE - minE < 0.5f) return null
            val meanE = (sumE / vals.size).toFloat()
            val srcUrl = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/14/{x}/{y}.png"
            DemControlGrid(
                source = "Terrarium ~9.5m/px DEM (120×200 -> 1m C++ Bicubic)",
                nativeResolutionM = 9.5f,
                cols = cols,
                rows = rows,
                elevationsM = vals,
                minElevM = minE,
                maxElevM = maxE,
                meanElevM = meanE
            ) to srcUrl
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchOpenTopoDataSrtm30Grid(bounds: MagadiTownBounds): Pair<DemControlGrid, String>? {
        return try {
            val cols = 12
            val rows = 16 // 192 points across 2 requests of <= 100 points
            val points = ArrayList<Pair<Double, Double>>(cols * rows)
            for (r in 0 until rows) {
                val lat = bounds.minLat + (r.toDouble() / (rows - 1)) * (bounds.maxLat - bounds.minLat)
                for (c in 0 until cols) {
                    val lon = bounds.minLon + (c.toDouble() / (cols - 1)) * (bounds.maxLon - bounds.minLon)
                    points.add(lat to lon)
                }
            }
            val vals = FloatArray(points.size)
            var idx = 0
            for (chunk in points.chunked(96)) {
                val locs = chunk.joinToString("|") { String.format(Locale.US, "%.6f,%.6f", it.first, it.second) }
                val url = "https://api.opentopodata.org/v1/srtm30m?locations=$locs"
                val req = Request.Builder().url(url).header("User-Agent", "Apex3DAssets-MagadiGIS/1.0").get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    val obj = JSONObject(resp.body?.string() ?: return null)
                    val results = obj.getJSONArray("results")
                    for (k in 0 until results.length()) {
                        vals[idx++] = results.getJSONObject(k).optDouble("elevation", 605.0).toFloat()
                    }
                }
            }
            if (idx < vals.size) return null
            val minE = vals.minOrNull() ?: 595f
            val maxE = vals.maxOrNull() ?: 720f
            val meanE = vals.average().toFloat()
            DemControlGrid(
                source = "SRTM GL1 30m DEM (12×16 -> 1m C++ Bicubic)",
                nativeResolutionM = 30.0f,
                cols = cols,
                rows = rows,
                elevationsM = vals,
                minElevM = minE,
                maxElevM = maxE,
                meanElevM = meanE
            ) to "https://api.opentopodata.org/v1/srtm30m"
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchOpenMeteoDemGrid(bounds: MagadiTownBounds): Pair<DemControlGrid?, String> {
        val cols = 16
        val rows = 24 // 384 points batched in chunks of 96
        val points = ArrayList<Pair<Double, Double>>(cols * rows)
        for (r in 0 until rows) {
            val lat = bounds.minLat + (r.toDouble() / (rows - 1)) * (bounds.maxLat - bounds.minLat)
            for (c in 0 until cols) {
                val lon = bounds.minLon + (c.toDouble() / (cols - 1)) * (bounds.maxLon - bounds.minLon)
                points.add(lat to lon)
            }
        }
        return try {
            val vals = FloatArray(points.size)
            var idx = 0
            for (chunk in points.chunked(96)) {
                val lats = chunk.joinToString(",") { String.format(Locale.US, "%.6f", it.first) }
                val lons = chunk.joinToString(",") { String.format(Locale.US, "%.6f", it.second) }
                val url = "https://api.open-meteo.com/v1/elevation?latitude=$lats&longitude=$lons"
                val req = Request.Builder().url(url).header("User-Agent", "Apex3DAssets-MagadiGIS/1.0").get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return null to "Open-Meteo HTTP ${resp.code}"
                    val obj = JSONObject(resp.body?.string() ?: return null to "Empty DEM response")
                    val elevArr = obj.getJSONArray("elevation")
                    for (i in 0 until elevArr.length()) {
                        if (idx < vals.size) vals[idx++] = elevArr.getDouble(i).toFloat()
                    }
                }
            }
            val minE = vals.minOrNull() ?: 595f
            val maxE = vals.maxOrNull() ?: 720f
            val meanE = vals.average().toFloat()
            DemControlGrid(
                source = "Open-Meteo Copernicus DEM 90m (16×24 fallback)",
                nativeResolutionM = 90.0f,
                cols = cols,
                rows = rows,
                elevationsM = vals,
                minElevM = minE,
                maxElevM = maxE,
                meanElevM = meanE
            ) to "https://api.open-meteo.com/v1/elevation"
        } catch (e: Exception) {
            null to "DEM fallback (${e.message})"
        }
    }

    private fun parseOverpassIntoBundle(
        osmJson: JSONObject,
        osmUrl: String,
        demGrid: DemControlGrid?,
        demUrl: String,
        bounds: MagadiTownBounds
    ): MagadiTownBundle {
        val elements = osmJson.optJSONArray("elements") ?: JSONArray()
        val nodeMap = HashMap<Long, Pair<Double, Double>>(elements.length())
        val wayNodeCoordsMap = HashMap<Long, List<MagadiGeoCoord>>()
        val pois = ArrayList<ExtractedPoiMetadata>()
        val roads = ArrayList<ExtractedRoadWay>()
        val buildings = ArrayList<ExtractedBuilding>()
        val waterPolygons = ArrayList<ExtractedWaterPolygon>()

        // Pass 1: Collect all node coordinates & named POIs
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            if (el.optString("type") == "node") {
                val id = el.optLong("id")
                val lat = el.optDouble("lat", Double.NaN)
                val lon = el.optDouble("lon", Double.NaN)
                if (!lat.isNaN() && !lon.isNaN()) {
                    nodeMap[id] = lat to lon
                    val tagsObj = el.optJSONObject("tags")
                    if (tagsObj != null && (tagsObj.has("place") || tagsObj.has("amenity") ||
                                tagsObj.has("man_made") || tagsObj.has("aeroway") ||
                                tagsObj.has("tourism") || tagsObj.has("shop") || tagsObj.has("name"))
                    ) {
                        val tags = jsonToMap(tagsObj)
                        val name = tags["name"] ?: tags["amenity"] ?: tags["man_made"] ?: tags["place"] ?: "POI #$id"
                        val cat = tags["place"] ?: tags["amenity"] ?: tags["man_made"]
                            ?: tags["aeroway"] ?: tags["tourism"] ?: tags["shop"] ?: "landmark"
                        val coord = convertLatLonToCoord(lat, lon, bounds)
                        val elev = inspect1mCell(coord.xM, coord.yM).elevM
                        pois.add(
                            ExtractedPoiMetadata(
                                osmId = id,
                                name = name,
                                category = cat,
                                coord = coord,
                                elevM = elev,
                                tags = tags
                            )
                        )
                    }
                }
            }
        }

        // Pass 2: Extract ways (roads, buildings, water polygons) and save wayMap for multipolygon relations
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            if (el.optString("type") != "way") continue
            val id = el.optLong("id")
            val nds = el.optJSONArray("nodes") ?: continue
            val coords = ArrayList<MagadiGeoCoord>(nds.length())
            for (j in 0 until nds.length()) {
                val nid = nds.optLong(j)
                val ll = nodeMap[nid] ?: continue
                coords.add(convertLatLonToCoord(ll.first, ll.second, bounds))
            }
            if (coords.size < 2) continue
            wayNodeCoordsMap[id] = coords

            val tagsObj = el.optJSONObject("tags") ?: continue
            val tags = jsonToMap(tagsObj)

            if (tags.containsKey("highway")) {
                val hType = tags["highway"] ?: "unclassified"
                val roadName = tags["name"] ?: tags["ref"] ?: "Magadi ${hType.replace('_', ' ').replaceFirstChar { it.uppercase() }} #$id"
                val surface = tags["surface"] ?: when (hType) {
                    "primary", "secondary", "trunk", "tertiary" -> "asphalt"
                    "residential", "service" -> "compacted / paved"
                    else -> "unpaved / murram"
                }
                val lanes = tags["lanes"]?.toIntOrNull() ?: when (hType) {
                    "primary", "trunk", "secondary" -> 2
                    else -> 1
                }
                val width = when (hType) {
                    "trunk", "primary" -> 8.0f
                    "secondary", "tertiary" -> 7.0f
                    "residential", "unclassified" -> 5.5f
                    else -> 3.5f
                }
                var lengthM = 0f
                for (k in 0 until coords.size - 1) {
                    val dx = coords[k + 1].xM - coords[k].xM
                    val dy = coords[k + 1].yM - coords[k].yM
                    lengthM += sqrt(dx * dx + dy * dy)
                }
                roads.add(
                    ExtractedRoadWay(
                        osmId = id,
                        name = roadName,
                        highwayType = hType,
                        surface = surface,
                        lanes = lanes,
                        oneWay = tags["oneway"] == "yes",
                        widthM = width,
                        lengthM = lengthM,
                        nodes = coords,
                        tags = tags
                    )
                )
            } else if (tags.containsKey("building") && coords.size >= 3) {
                val bType = tags["building"] ?: "yes"
                val amenity = tags["amenity"] ?: tags["shop"] ?: tags["industrial"] ?: ""
                val bName = tags["name"] ?: when {
                    amenity.isNotBlank() -> amenity.replace('_', ' ').replaceFirstChar { it.uppercase() }
                    bType != "yes" -> "${bType.replace('_', ' ').replaceFirstChar { it.uppercase() }} #$id"
                    else -> "Building #$id"
                }
                val levels = tags["building:levels"]?.toIntOrNull() ?: 1
                val height = tags["height"]?.replace("m", "")?.trim()?.toFloatOrNull() ?: (levels * 3.4f)

                val polyNoDup = if (coords.first().xM == coords.last().xM && coords.first().yM == coords.last().yM && coords.size > 3) {
                    coords.dropLast(1)
                } else coords

                var areaSum = 0.0
                var cx = 0.0
                var cy = 0.0
                var perim = 0.0
                for (k in polyNoDup.indices) {
                    val p0 = polyNoDup[k]
                    val p1 = polyNoDup[(k + 1) % polyNoDup.size]
                    val cross = p0.xM.toDouble() * p1.yM.toDouble() - p1.xM.toDouble() * p0.yM.toDouble()
                    areaSum += cross
                    cx += (p0.xM + p1.xM) * cross
                    cy += (p0.yM + p1.yM) * cross
                    perim += hypot((p1.xM - p0.xM).toDouble(), (p1.yM - p0.yM).toDouble())
                }
                val area = abs(areaSum * 0.5).toFloat().coerceAtLeast(4f)
                val centLat: Double
                val centLon: Double
                if (abs(areaSum) > 1e-3) {
                    val mx = (cx / (3.0 * areaSum)).toFloat()
                    val my = (cy / (3.0 * areaSum)).toFloat()
                    val cSample = inspect1mCell(mx, my)
                    centLat = if (mx in -200f..3200f && my in -200f..5200f) cSample.lat else polyNoDup.map { it.lat }.average()
                    centLon = if (mx in -200f..3200f && my in -200f..5200f) cSample.lon else polyNoDup.map { it.lon }.average()
                } else {
                    centLat = polyNoDup.map { it.lat }.average()
                    centLon = polyNoDup.map { it.lon }.average()
                }
                val centroid = convertLatLonToCoord(centLat, centLon, bounds)
                val elev = inspect1mCell(centroid.xM, centroid.yM).elevM

                buildings.add(
                    ExtractedBuilding(
                        osmId = id,
                        name = bName,
                        buildingType = bType,
                        amenity = amenity,
                        levels = levels,
                        heightM = height,
                        centroid = centroid,
                        elevM = elev,
                        footprintAreaM2 = area,
                        perimeterM = perim.toFloat(),
                        polygon = coords,
                        tags = tags
                    )
                )
            } else if ((tags["natural"] in setOf("water", "wetland", "mud") ||
                        tags["landuse"] in setOf("salt_pond", "reservoir")) && coords.size >= 3
            ) {
                val wType = tags["water"] ?: tags["landuse"] ?: tags["natural"] ?: "water"
                val wName = tags["name"] ?: if (wType == "salt_pond") "Magadi Salt Pan #$id" else "Lake Magadi Basin #$id"
                val meanE = coords.map { inspect1mCell(it.xM, it.yM).elevM }.average().toFloat()
                waterPolygons.add(
                    ExtractedWaterPolygon(
                        osmId = id,
                        name = wName,
                        waterType = wType,
                        polygon = coords,
                        meanElevM = meanE
                    )
                )
            }
        }

        // Pass 3: Reconstruct multipolygon water relations (e.g., Lake Magadi main shore rings)
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            if (el.optString("type") != "relation") continue
            val tagsObj = el.optJSONObject("tags") ?: continue
            val tags = jsonToMap(tagsObj)
            if (tags["natural"] !in setOf("water", "wetland") && tags["landuse"] !in setOf("salt_pond", "reservoir")) {
                continue
            }
            val relId = el.optLong("id")
            val wName = tags["name"] ?: "Lake Magadi #$relId"
            val wType = tags["water"] ?: tags["landuse"] ?: tags["natural"] ?: "lake"
            val members = el.optJSONArray("members") ?: continue
            for (m in 0 until members.length()) {
                val mem = members.optJSONObject(m) ?: continue
                if (mem.optString("type") == "way" && mem.optString("role") != "inner") {
                    val wayCoords = wayNodeCoordsMap[mem.optLong("ref")] ?: continue
                    if (wayCoords.size >= 3) {
                        val meanE = wayCoords.map { inspect1mCell(it.xM, it.yM).elevM }.average().toFloat()
                        waterPolygons.add(
                            ExtractedWaterPolygon(
                                osmId = relId * 1000L + m,
                                name = wName,
                                waterType = wType,
                                polygon = wayCoords,
                                meanElevM = meanE
                            )
                        )
                    }
                }
            }
        }

        // Auto-calibrate Lake Level ASL from extracted Lake Magadi shore elevations or basin lowest percentile
        val demMin = demGrid?.minElevM ?: 595.5f
        val demMax = demGrid?.maxElevM ?: 720.0f
        val sortedDem = demGrid?.elevationsM?.sortedArray()
        val p15Elev = sortedDem?.getOrNull((sortedDem.size * 0.14f).toInt().coerceIn(0, sortedDem.lastIndex))
        val autoLakeLevel = when {
            waterPolygons.isNotEmpty() -> {
                val shoreAvg = waterPolygons.map { it.meanElevM }.average().toFloat()
                (shoreAvg + 0.8f).coerceIn(demMin + 0.5f, demMax - 4.0f)
            }
            p15Elev != null -> (p15Elev + 1.2f).coerceIn(demMin + 0.8f, demMax - 4.0f)
            else -> 602.5f
        }

        val totalRoadKm = roads.sumOf { it.lengthM.toDouble() }.toFloat() / 1000f
        val totalBldgArea = buildings.sumOf { it.footprintAreaM2.toDouble() }.toFloat()
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val metadata = MagadiTownMetadata(
            placeName = "Magadi Town, Kajiado County, Kenya",
            extractedAtIso = isoFormatter.format(Date()),
            osmSourceUrl = osmUrl,
            demSourceUrl = demUrl,
            demResolutionM = demGrid?.nativeResolutionM ?: 9.5f,
            lakeLevelAslM = autoLakeLevel,
            isLiveExtracted = true,
            roadCount = roads.size,
            totalRoadLengthKm = totalRoadKm,
            buildingCount = buildings.size,
            totalBuildingFootprintM2 = totalBldgArea,
            waterCount = waterPolygons.size,
            poiCount = pois.size,
            elevMinM = demMin,
            elevMaxM = demMax,
            elevMeanM = demGrid?.meanElevM ?: 625f
        )

        return MagadiTownBundle(
            bounds = bounds,
            metadata = metadata,
            demGrid = demGrid,
            roads = roads.sortedByDescending { it.lengthM },
            buildings = buildings.sortedByDescending { it.footprintAreaM2 },
            pois = pois,
            waterPolygons = waterPolygons
        )
    }

    private fun jsonToMap(obj: JSONObject): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = obj.optString(k, "")
        }
        return map
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
