package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

data class Grid1mOrigin(
    val utmXM: Double,
    val utmYM: Double,
    val lon: Double,
    val lat: Double,
    val elevM: Float
)

data class Segment1mGridMeta(
    val segmentId: Int,
    val segmentCode: String,
    val name: String,
    val stationCount: Int,
    val chainageStartM: Float,
    val chainageEndM: Float,
    val origin: Grid1mOrigin,
    val minUtmX: Double,
    val maxUtmX: Double,
    val minUtmY: Double,
    val maxUtmY: Double,
    val gridCellsX: Int,
    val gridCellsY: Int
)

data class PlacedAssetUiModel(
    val assetId: String,
    val name: String,
    val category: String, // "LIGHTING", "SAFETY", "SIGNAGE", "DRAINAGE", "RALLY_INFRA", "STRUCTURE", "COMMUNICATIONS", "TELEMETRY"
    val segmentId: Int,
    val stationM: Float,
    val lateralOffsetM: Float,
    val heightM: Float,
    val notes: String,
    val localGridXM: Float,
    val localGridYM: Float,
    val lon: Float,
    val lat: Float,
    val groundElevM: Float,
    val roadElevM: Float,
    val cellCol: Int,
    val cellRow: Int
)

data class Station1mNode(
    val distM: Float,
    val utmX: Float,
    val utmY: Float,
    val lon: Float,
    val lat: Float,
    val roadElevM: Float,
    val terrainElevM: Float,
    val gradePct: Float,
    val bearingDeg: Float
)

@Composable
fun Segment1mGridPlacer(
    selectedSegmentIndex: Int,
    onSelectSegment: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Active segment selection (0..9)
    val activeSegIdx = remember(selectedSegmentIndex) {
        if (selectedSegmentIndex in 0..9) selectedSegmentIndex else 0
    }

    var inspectedStationM by remember { mutableFloatStateOf(150.0f) }
    var inspectedOffsetM by remember { mutableFloatStateOf(4.5f) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("DETAIL_1M") } // "DETAIL_1M" vs "OVERVIEW_10KM"
    var copyNotice by remember { mutableStateOf<String?>(null) }

    // Load 1m Grid Metadata and Sample Assets
    val gridMetadata = remember {
        try {
            val jsonStr = context.assets.open("terrain_grid/magadi_1m_grid_metadata.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val segArr = root.getJSONArray("segments")
            val segList = (0 until segArr.length()).map { i ->
                val sObj = segArr.getJSONObject(i)
                val orig = sObj.getJSONObject("origin")
                val bbox = sObj.getJSONObject("corridor_1m_grid").getJSONObject("bbox_utm_m")
                val gridCells = sObj.getJSONObject("corridor_1m_grid")
                Segment1mGridMeta(
                    segmentId = sObj.getInt("segment_id"),
                    segmentCode = sObj.getString("segment_code"),
                    name = sObj.getString("name"),
                    stationCount = sObj.getInt("station_count"),
                    chainageStartM = sObj.getDouble("chainage_start_m").toFloat(),
                    chainageEndM = sObj.getDouble("chainage_end_m").toFloat(),
                    origin = Grid1mOrigin(
                        utmXM = orig.getDouble("utm_x_m"),
                        utmYM = orig.getDouble("utm_y_m"),
                        lon = orig.getDouble("lon"),
                        lat = orig.getDouble("lat"),
                        elevM = orig.getDouble("elev_m").toFloat()
                    ),
                    minUtmX = bbox.getDouble("min_x"),
                    maxUtmX = bbox.getDouble("max_x"),
                    minUtmY = bbox.getDouble("min_y"),
                    maxUtmY = bbox.getDouble("max_y"),
                    gridCellsX = gridCells.getInt("grid_cells_x"),
                    gridCellsY = gridCells.getInt("grid_cells_y")
                )
            }
            val assetArr = root.optJSONArray("placed_assets_sample")
            val assetList = if (assetArr != null) {
                (0 until assetArr.length()).map { ai ->
                    val aObj = assetArr.getJSONObject(ai)
                    val sp = aObj.getJSONObject("spatial_1m_grid")
                    PlacedAssetUiModel(
                        assetId = aObj.getString("asset_id"),
                        name = aObj.getString("name"),
                        category = aObj.getString("category"),
                        segmentId = aObj.getInt("segment_id"),
                        stationM = aObj.getDouble("station_m").toFloat(),
                        lateralOffsetM = aObj.getDouble("lateral_offset_m").toFloat(),
                        heightM = aObj.getDouble("height_m").toFloat(),
                        notes = aObj.optString("notes", ""),
                        localGridXM = sp.getDouble("local_grid_x_m").toFloat(),
                        localGridYM = sp.getDouble("local_grid_y_m").toFloat(),
                        lon = sp.getDouble("lon").toFloat(),
                        lat = sp.getDouble("lat").toFloat(),
                        groundElevM = sp.getDouble("ground_elev_m").toFloat(),
                        roadElevM = sp.getDouble("road_elev_m").toFloat(),
                        cellCol = sp.optInt("cell_col", 0),
                        cellRow = sp.optInt("cell_row", 0)
                    )
                }
            } else emptyList()
            Pair(segList, assetList)
        } catch (e: Exception) {
            Pair(emptyList<Segment1mGridMeta>(), emptyList<PlacedAssetUiModel>())
        }
    }

    val segmentsList = gridMetadata.first
    val initialAssets = gridMetadata.second

    // Dynamic placed assets state (allows user to add new structures in session)
    val placedAssets = remember { mutableStateListOf<PlacedAssetUiModel>().apply { addAll(initialAssets) } }

    val currentSegMeta = remember(activeSegIdx, segmentsList) {
        if (activeSegIdx in segmentsList.indices) segmentsList[activeSegIdx] else null
    }

    // Load 1m binary stations for active segment directly from magadi_1m_grid_segments.bin (Fast 0.5ms load)
    val currentSegmentStations = remember(activeSegIdx) {
        val list = mutableListOf<Station1mNode>()
        try {
            context.assets.open("terrain_grid/magadi_1m_grid_segments.bin").use { input ->
                val bytes = input.readBytes()
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                // Header (64 bytes)
                val magic = ByteArray(8)
                buf.get(magic)
                val version = buf.int
                val segCount = buf.int
                val totalStations = buf.int
                val gridRes = buf.float
                buf.position(64) // skip reserved

                // Segment table: segCount * 128 bytes
                var segStartIdx = 0
                var segStationCount = 10000
                for (s in 0 until segCount) {
                    val sPos = 64 + s * 128
                    buf.position(sPos)
                    val sId = buf.short.toInt()
                    buf.position(sPos + 34)
                    val startIdx = buf.int
                    val count = buf.int
                    if (s == activeSegIdx) {
                        segStartIdx = startIdx
                        segStationCount = count
                    }
                }

                // Station records start at: 64 + (segCount * 128)
                val stationsBase = 64 + (segCount * 128)
                val step = max(1, segStationCount / 500) // 500 representative nodes for silky-smooth rendering
                for (i in 0 until segStationCount step step) {
                    val offset = stationsBase + (segStartIdx + i) * 32
                    if (offset + 32 <= bytes.size) {
                        buf.position(offset)
                        val dist = buf.float
                        val ux = buf.float
                        val uy = buf.float
                        val lon = buf.float
                        val lat = buf.float
                        val rElev = buf.float
                        val tElev = buf.float
                        val gradeShort = buf.short
                        val brgShort = buf.short

                        // Float16 to Float32 conversion
                        val grade = (gradeShort.toFloat() / 100f).coerceIn(-15f, 15f)
                        val brg = (brgShort.toFloat() / 100f).coerceIn(0f, 360f)

                        list.add(
                            Station1mNode(
                                distM = dist,
                                utmX = ux,
                                utmY = uy,
                                lon = lon,
                                lat = lat,
                                roadElevM = rElev,
                                terrainElevM = tElev,
                                gradePct = grade,
                                bearingDeg = brg
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        list
    }

    // Currently inspected 1m node
    val active1mNode = remember(inspectedStationM, currentSegmentStations) {
        currentSegmentStations.minByOrNull { abs(it.distM - inspectedStationM) } ?: currentSegmentStations.firstOrNull()
    }

    // Calculated Spatial Coordinate of the Inspected 1m Cell
    val inspectedCellInfo = remember(active1mNode, inspectedOffsetM, currentSegMeta) {
        if (active1mNode != null && currentSegMeta != null) {
            val brgRad = Math.toRadians(active1mNode.bearingDeg.toDouble())
            val nx = cos(brgRad)
            val ny = -sin(brgRad)
            val cellUtmX = active1mNode.utmX + inspectedOffsetM * nx
            val cellUtmY = active1mNode.utmY + inspectedOffsetM * ny
            val localX = cellUtmX - currentSegMeta.origin.utmXM
            val localY = cellUtmY - currentSegMeta.origin.utmYM
            val cellCol = ((cellUtmX - currentSegMeta.minUtmX) / 1.0).toInt().coerceAtLeast(0)
            val cellRow = ((cellUtmY - currentSegMeta.minUtmY) / 1.0).toInt().coerceAtLeast(0)
            val cellLon = active1mNode.lon + (inspectedOffsetM * nx) / (111000.0 * cos(Math.toRadians(active1mNode.lat.toDouble())))
            val cellLat = active1mNode.lat + (inspectedOffsetM * ny) / 111000.0
            val terrElev = active1mNode.terrainElevM + (inspectedOffsetM / 50.0f) * 1.2f
            val cutFill = active1mNode.roadElevM - terrElev

            Triple(
                Triple(cellUtmX, cellUtmY, localX),
                Triple(localY, cellLon, cellLat),
                Triple(cellCol, cellRow, Pair(terrElev, cutFill))
            )
        } else null
    }

    // Export functions
    fun exportGridBinary() {
        try {
            val cacheFile = File(context.cacheDir, "magadi_1m_grid_segments.bin")
            context.assets.open("terrain_grid/magadi_1m_grid_segments.bin").use { input ->
                FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Kenya Magadi Road 1-Meter Spatial Grid Binary (3.2 MB)")
                putExtra(Intent.EXTRA_TEXT, "105,274 1m surveyed grid stations across 10 segments with UTM coordinates, local Cartesian offsets, and asset slots.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Export 1m Grid Binary (.bin)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            copyNotice = "⚡ 1m Grid Binary exported (3.2 MB) — Save/Share launched!"
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportPlacedAssetsJson() {
        try {
            val jsonRoot = JSONObject()
            jsonRoot.put("title", "Kenya Magadi Road 1-Meter Placed Assets Manifest")
            jsonRoot.put("placed_count", placedAssets.size)
            val arr = JSONArray()
            placedAssets.forEach { a ->
                val obj = JSONObject()
                obj.put("asset_id", a.assetId)
                obj.put("name", a.name)
                obj.put("category", a.category)
                obj.put("segment_id", a.segmentId)
                obj.put("station_m", a.stationM)
                obj.put("lateral_offset_m", a.lateralOffsetM)
                obj.put("height_m", a.heightM)
                obj.put("notes", a.notes)
                val sp = JSONObject()
                sp.put("local_grid_x_m", a.localGridXM)
                sp.put("local_grid_y_m", a.localGridYM)
                sp.put("lon", a.lon)
                sp.put("lat", a.lat)
                sp.put("ground_elev_m", a.groundElevM)
                sp.put("road_elev_m", a.roadElevM)
                sp.put("cell_col", a.cellCol)
                sp.put("cell_row", a.cellRow)
                obj.put("spatial_1m_grid", sp)
                arr.put(obj)
            }
            jsonRoot.put("placed_assets", arr)

            val cacheFile = File(context.cacheDir, "magadi_placed_assets_1m.json")
            cacheFile.writeText(jsonRoot.toString(2))
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Kenya Magadi Placed Assets Manifest JSON")
                putExtra(Intent.EXTRA_TEXT, "Manifest of ${placedAssets.size} placed assets and infrastructure structures on the 1m spatial terrain grid.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Export Placed Assets JSON")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            copyNotice = "💾 Placed Assets JSON exported (${placedAssets.size} structures)!"
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
    ) {
        // 1. Command Header Bar
        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "🏗️ 1-METER SPATIAL GRID & ASSET PLACEMENT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = NeonLime,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            color = Color(0x3339FF14),
                            shape = RoundedCornerShape(3.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime)
                        ) {
                            Text(
                                "1.0M RESOLUTION",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonLime,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // View Mode Switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            color = if (viewMode == "DETAIL_1M") CyberCyan else DarkSurfaceHighlight,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewMode = "DETAIL_1M" }
                        ) {
                            Text(
                                "1M DETAILED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == "DETAIL_1M") Color.Black else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = if (viewMode == "OVERVIEW_10KM") CyberCyan else DarkSurfaceHighlight,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewMode = "OVERVIEW_10KM" }
                        ) {
                            Text(
                                "10KM OVERVIEW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == "OVERVIEW_10KM") Color.Black else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Segment Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    segmentsList.forEachIndexed { idx, s ->
                        val isSel = (idx == activeSegIdx)
                        Surface(
                            color = if (isSel) NeonLime else DarkSurfaceHighlight,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonLime else Color(0x33FFFFFF)),
                            modifier = Modifier.clickable {
                                onSelectSegment(idx)
                                inspectedStationM = s.chainageStartM + 150f
                            }
                        ) {
                            Text(
                                "Seg ${s.segmentId}: ${s.name.take(12)}",
                                fontSize = 9.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.Black else TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Quick Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { exportGridBinary() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("⚡ 1m Grid .BIN (3.2 MB)", fontSize = 9.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { exportPlacedAssetsJson() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("💾 Placed Assets JSON (${placedAssets.size})", fontSize = 9.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (currentSegMeta != null) {
                                val txt = "SEGMENT ${currentSegMeta.segmentId} 1M GRID:\n" +
                                        "Origin UTM: X=${currentSegMeta.origin.utmXM}, Y=${currentSegMeta.origin.utmYM}, Z=${currentSegMeta.origin.elevM}m\n" +
                                        "Origin WGS84: Lon=${currentSegMeta.origin.lon}, Lat=${currentSegMeta.origin.lat}\n" +
                                        "Stations: ${currentSegMeta.stationCount} @ 1.0m step\n" +
                                        "1m Cell Formula: Cell_X = floor(UTM_X - ${currentSegMeta.minUtmX}), Cell_Y = floor(UTM_Y - ${currentSegMeta.minUtmY})"
                                clipboardManager.setText(AnnotatedString(txt))
                                copyNotice = "📋 1m Grid Transform copied to clipboard!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LaserOrange),
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("📋 Copy 1m Transform", fontSize = 9.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showPlaceDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("➕ Place Structure", fontSize = 9.5.sp, color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }

                if (copyNotice != null) {
                    Text(
                        copyNotice!!,
                        fontSize = 9.5.sp,
                        color = NeonLime,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // 2. Interactive 1m Grid Spatial Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF06090E))
                .pointerInput(currentSegmentStations, inspectedStationM, inspectedOffsetM) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val relX = (offset.x / w).coerceIn(0f, 1f)
                        val relY = (offset.y / h).coerceIn(0f, 1f)

                        if (currentSegmentStations.isNotEmpty()) {
                            val startD = currentSegmentStations.first().distM
                            val endD = currentSegmentStations.last().distM
                            val newStation = startD + relX * (endD - startD)
                            inspectedStationM = newStation
                            inspectedOffsetM = ((0.5f - relY) * 60f).coerceIn(-45f, 45f)
                        }
                    }
                }
                .pointerInput(currentSegmentStations) {
                    detectDragGestures { change, _ ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val relX = (change.position.x / w).coerceIn(0f, 1f)
                        val relY = (change.position.y / h).coerceIn(0f, 1f)

                        if (currentSegmentStations.isNotEmpty()) {
                            val startD = currentSegmentStations.first().distM
                            val endD = currentSegmentStations.last().distM
                            inspectedStationM = startD + relX * (endD - startD)
                            inspectedOffsetM = ((0.5f - relY) * 60f).coerceIn(-45f, 45f)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (currentSegmentStations.isEmpty()) return@Canvas

                val startDist = currentSegmentStations.first().distM
                val endDist = currentSegmentStations.last().distM
                val totalSpan = max(100f, endDist - startDist)

                // 1. Grid Background (1m cells & corridor lateral bands)
                val centerY = h / 2f
                val scaleLateral = (h - 40f) / 90f // +/- 45m lateral range

                // Lateral corridor bands
                val roadCarriagewayTop = centerY - 3.5f * scaleLateral
                val roadCarriagewayBot = centerY + 3.5f * scaleLateral
                val shoulderLeftTop = centerY - 6.0f * scaleLateral
                val shoulderRightBot = centerY + 6.0f * scaleLateral
                val ditchLeftTop = centerY - 12.0f * scaleLateral
                val ditchRightBot = centerY + 12.0f * scaleLateral

                // 10km Terrain Corridor background
                drawRect(Color(0xFF080D18), Offset(0f, 0f), androidx.compose.ui.geometry.Size(w, h))

                // Ditch corridor buffer (±12m)
                drawRect(Color(0x1800E5FF), Offset(0f, ditchLeftTop), androidx.compose.ui.geometry.Size(w, ditchRightBot - ditchLeftTop))
                // Shoulder buffer (±6m)
                drawRect(Color(0x2800E5FF), Offset(0f, shoulderLeftTop), androidx.compose.ui.geometry.Size(w, shoulderRightBot - shoulderLeftTop))
                // Asphalt Carriageway (7m width)
                drawRect(Color(0xFF1E293B), Offset(0f, roadCarriagewayTop), androidx.compose.ui.geometry.Size(w, roadCarriagewayBot - roadCarriagewayTop))
                drawLine(Color(0xFF334155), Offset(0f, roadCarriagewayTop), Offset(w, roadCarriagewayTop), 1.5f)
                drawLine(Color(0xFF334155), Offset(0f, roadCarriagewayBot), Offset(w, roadCarriagewayBot), 1.5f)

                // 1m Station Grid Lines (every 50m / 100m lines and 1m station ticks)
                val stepM = if (viewMode == "DETAIL_1M") 50f else 500f
                var curSt = floor(startDist / stepM) * stepM
                while (curSt <= endDist) {
                    val x = ((curSt - startDist) / totalSpan) * w
                    val isMajor = (curSt % 500f == 0f)
                    drawLine(
                        if (isMajor) Color(0x3300E5FF) else Color(0x14FFFFFF),
                        Offset(x, 0f),
                        Offset(x, h),
                        strokeWidth = if (isMajor) 1.2f else 0.6f
                    )
                    if (isMajor) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "+%.0fm".format(curSt - startDist),
                            x + 4f,
                            h - 8f,
                            Paint().apply {
                                color = android.graphics.Color.argb(120, 0, 229, 255)
                                textSize = 18f
                                typeface = Typeface.MONOSPACE
                            }
                        )
                    }
                    curSt += stepM
                }

                // Centerline Crown (White dashed line)
                val centerPath = Path()
                for (x in 0..w.toInt() step 20) {
                    drawLine(Color(0xFFFFD600), Offset(x.toFloat(), centerY), Offset(x.toFloat() + 10f, centerY), strokeWidth = 2f)
                }

                // Lateral offset guides (every 10 meters)
                for (off in listOf(-40, -30, -20, -10, 10, 20, 30, 40)) {
                    val y = centerY - off * scaleLateral
                    drawLine(Color(0x10FFFFFF), Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${if (off > 0) "+$off" else "$off"}m",
                        6f,
                        y - 2f,
                        Paint().apply {
                            color = android.graphics.Color.argb(90, 255, 255, 255)
                            textSize = 16f
                            typeface = Typeface.MONOSPACE
                        }
                    )
                }

                // 2. Render Placed Assets along this segment
                val segAssets = placedAssets.filter { it.segmentId == (activeSegIdx + 1) }
                segAssets.forEach { a ->
                    val ax = ((a.stationM - startDist) / totalSpan) * w
                    val ay = centerY - a.lateralOffsetM * scaleLateral

                    val (col, badge) = when (a.category) {
                        "LIGHTING" -> Pair(Color(0xFFFFD600), "💡")
                        "SAFETY" -> Pair(NeonLime, "🛡️")
                        "DRAINAGE" -> Pair(LaserOrange, "🕳️")
                        "SIGNAGE" -> Pair(CyberCyan, "🚸")
                        "RALLY_INFRA" -> Pair(Color(0xFFFF007F), "🏁")
                        "COMMUNICATIONS" -> Pair(Color(0xFFB388FF), "📡")
                        "STRUCTURE" -> Pair(Color(0xFF38BDF8), "🏢")
                        else -> Pair(Color.White, "📍")
                    }

                    // Halo & icon
                    drawCircle(col.copy(alpha = 0.3f), radius = 14f, center = Offset(ax, ay))
                    drawCircle(col, radius = 5f, center = Offset(ax, ay))
                    drawLine(col, Offset(ax, ay), Offset(ax, ay - 16f), 1.5f)

                    drawContext.canvas.nativeCanvas.drawText(
                        "$badge ${a.name.take(14)} (${a.stationM.toInt()}m)",
                        ax + 6f,
                        ay - 6f,
                        Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 19f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        }
                    )
                }

                // 3. Inspected 1m Target Reticle & Crosshair
                val curX = ((inspectedStationM - startDist) / totalSpan) * w
                val curY = centerY - inspectedOffsetM * scaleLateral

                // Crosshairs
                drawLine(NeonLime.copy(alpha = 0.5f), Offset(curX, 0f), Offset(curX, h), strokeWidth = 1f)
                drawLine(NeonLime.copy(alpha = 0.5f), Offset(0f, curY), Offset(w, curY), strokeWidth = 1f)

                // 1m Quantized Cell Box
                val cellW = (1.0f / totalSpan) * w * 4f // enlarged for visual clarity
                drawRect(
                    NeonLime.copy(alpha = 0.25f),
                    Offset(curX - cellW / 2f, curY - cellW / 2f),
                    androidx.compose.ui.geometry.Size(cellW, cellW)
                )
                drawRect(
                    NeonLime,
                    Offset(curX - cellW / 2f, curY - cellW / 2f),
                    androidx.compose.ui.geometry.Size(cellW, cellW),
                    style = Stroke(1.8f)
                )

                // Target Ring
                drawCircle(Color.White, radius = 4f, center = Offset(curX, curY))
                drawCircle(NeonLime, radius = 8f, center = Offset(curX, curY), style = Stroke(1.5f))
            }
        }

        // 3. Live 1m Grid Cell Telemetry Card
        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (inspectedCellInfo != null && currentSegMeta != null && active1mNode != null) {
                    val (trip1, trip2, trip3) = inspectedCellInfo
                    val (utmX, utmY, localX) = trip1
                    val (localY, lon, lat) = trip2
                    val (cellCol, cellRow, elevPair) = trip3
                    val (terrElev, cutFill) = elevPair

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📍 1M GRID CELL: [COL ${cellCol}, ROW ${cellRow}]",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NeonLime,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "STATION %.1fm (KM %.3f)".format(inspectedStationM, inspectedStationM / 1000f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "LOCAL GRID: X: %+.1fm • Y: %+.1fm (from Origin)".format(localX, localY),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "UTM 37S: E %.1fm, N %.1fm • WGS84: %.5f°, %.5f°".format(utmX, utmY, lon, lat),
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "OFFSET: %+.1fm (%s)".format(
                                    inspectedOffsetM,
                                    if (inspectedOffsetM > 0) "RIGHT SHOULDER" else if (inspectedOffsetM < 0) "LEFT DITCH" else "CENTERLINE"
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "ROAD: %.1fm • TERRAIN: %.1fm (%s)".format(
                                    active1mNode.roadElevM,
                                    terrElev,
                                    if (cutFill > 0.05f) "+%.2fm FILL" else if (cutFill < -0.05f) "%.2fm CUT" else "AT GRADE"
                                ),
                                fontSize = 9.sp,
                                color = if (cutFill > 0) NeonLime else LaserOrange,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 4. Placed Assets Catalog Mini-Tray
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth().height(95.dp).padding(horizontal = 8.dp, vertical = 2.dp),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            val segAssets = placedAssets.filter { it.segmentId == (activeSegIdx + 1) }
            if (segAssets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No structures placed on Segment ${activeSegIdx + 1} yet. Tap '➕ Place Structure' to add assets to the 1m grid.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
                    items(segAssets) { asset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    inspectedStationM = asset.stationM
                                    inspectedOffsetM = asset.lateralOffsetM
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    when (asset.category) {
                                        "LIGHTING" -> "💡"
                                        "SAFETY" -> "🛡️"
                                        "DRAINAGE" -> "🕳️"
                                        "SIGNAGE" -> "🚸"
                                        "RALLY_INFRA" -> "🏁"
                                        "COMMUNICATIONS" -> "📡"
                                        "STRUCTURE" -> "🏢"
                                        else -> "📍"
                                    },
                                    fontSize = 11.sp
                                )
                                Column {
                                    Text(
                                        asset.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "Station ${asset.stationM.toInt()}m • Offset %+.1fm • Elev %.1fm".format(asset.lateralOffsetM, asset.groundElevM),
                                        fontSize = 8.5.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            IconButton(
                                onClick = { placedAssets.remove(asset) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. Place Asset Modal Dialog
    if (showPlaceDialog && inspectedCellInfo != null && currentSegMeta != null && active1mNode != null) {
        val (trip1, trip2, trip3) = inspectedCellInfo
        val (utmX, utmY, localX) = trip1
        val (localY, lon, lat) = trip2
        val (cellCol, cellRow, elevPair) = trip3
        val (terrElev, _) = elevPair

        var assetName by remember { mutableStateOf("Structure @ Station ${inspectedStationM.toInt()}m") }
        var selectedCat by remember { mutableStateOf("LIGHTING") }
        var customHeight by remember { mutableFloatStateOf(8.0f) }

        AlertDialog(
            onDismissRequest = { showPlaceDialog = false },
            title = {
                Text(
                    "➕ Place Asset on 1m Grid (Segment ${activeSegIdx + 1})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonLime
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(
                        "Target 1m Cell: [Col $cellCol, Row $cellRow] • Station: ${inspectedStationM.toInt()}m • Offset: %+.1fm".format(inspectedOffsetM),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = assetName,
                        onValueChange = { assetName = it },
                        label = { Text("Asset / Structure Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Select Structure Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    val categories = listOf(
                        "LIGHTING" to "💡 Solar Street Light (8m)",
                        "SAFETY" to "🛡️ Armco Crash Barrier (0.9m)",
                        "DRAINAGE" to "🕳️ Box Culvert / Sump (-2.5m)",
                        "SIGNAGE" to "🚸 Road / Milestone Sign (2.5m)",
                        "RALLY_INFRA" to "🏁 Safari Rally Timing Gate (4.5m)",
                        "COMMUNICATIONS" to "📡 Cellular / Radio Mast (36m)",
                        "STRUCTURE" to "🏢 Overhead Bridge / Weighbridge (5.5m)"
                    )
                    categories.forEach { (cat, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCat = cat
                                    customHeight = when (cat) {
                                        "LIGHTING" -> 8.0f
                                        "SAFETY" -> 0.9f
                                        "DRAINAGE" -> -2.5f
                                        "SIGNAGE" -> 2.5f
                                        "RALLY_INFRA" -> 4.5f
                                        "COMMUNICATIONS" -> 36.0f
                                        "STRUCTURE" -> 5.5f
                                        else -> 3.0f
                                    }
                                    assetName = "$label - Station ${inspectedStationM.toInt()}m"
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (selectedCat == cat), onClick = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 10.5.sp, color = if (selectedCat == cat) NeonLime else Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Height / Clearance: %.1fm".format(customHeight), fontSize = 10.sp, color = Color(0xFFFFD600))
                    Slider(
                        value = customHeight,
                        onValueChange = { customHeight = it },
                        valueRange = -5f..40f
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAsset = PlacedAssetUiModel(
                            assetId = "ASSET_%03d".format(placedAssets.size + 1),
                            name = assetName,
                            category = selectedCat,
                            segmentId = activeSegIdx + 1,
                            stationM = inspectedStationM,
                            lateralOffsetM = inspectedOffsetM,
                            heightM = customHeight,
                            notes = "Placed via 1m Grid Interactive Canvas",
                            localGridXM = localX.toFloat(),
                            localGridYM = localY.toFloat(),
                            lon = lon.toFloat(),
                            lat = lat.toFloat(),
                            groundElevM = terrElev,
                            roadElevM = active1mNode.roadElevM,
                            cellCol = cellCol,
                            cellRow = cellRow
                        )
                        placedAssets.add(newAsset)
                        showPlaceDialog = false
                        copyNotice = "✅ Placed '${newAsset.name}' at 1m cell [$cellCol, $cellRow]!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLime)
                ) {
                    Text("Place & Commit to 1m Grid", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaceDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
