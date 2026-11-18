package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun C58CorridorDataPanel(
    bundle: C58CorridorBundle,
    selectedSector: Int,
    activeTab: Int,
    searchQuery: String,
    sideFilter: String,
    onSelectTab: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onSideFilterChange: (String) -> Unit,
    onSelectRoad: (C58ExtractedRoad) -> Unit,
    onSelectHouseMap: (C58ExtractedHouse) -> Unit,
    onSelectHouse3d: (C58ExtractedHouse) -> Unit,
    onSelectPoi: (C58ExtractedPoi) -> Unit,
    onExportGeoJson: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Export Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportGeoJson,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.20f)),
                border = BorderStroke(1.dp, CyberCyan),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("c58_export_geojson_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share C58 GeoJSON", tint = CyberCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("GEOJSON (SWATH)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
            Button(
                onClick = onExportCsv,
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime.copy(alpha = 0.18f)),
                border = BorderStroke(1.dp, NeonLime),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("c58_export_csv_btn")
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "Export Houses CSV", tint = NeonLime, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("HOUSES CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonLime)
            }
        }

        val scopedRoads = if (selectedSector == 0) bundle.roads else bundle.roads.filter { it.sectorIndex == selectedSector }
        val scopedHouses = if (selectedSector == 0) bundle.houses else bundle.houses.filter { it.sectorIndex == selectedSector }
        val scopedPois = if (selectedSector == 0) bundle.pois else bundle.pois.filter { it.sectorIndex == selectedSector }

        val tabs = listOf(
            "CORRIDOR META",
            "ROADS (${scopedRoads.size})",
            "HOUSES (${scopedHouses.size})",
            "POIS (${scopedPois.size})"
        )
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = DarkSurface,
            contentColor = CyberCyan,
            edgePadding = 4.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeTab == idx,
                    onClick = { onSelectTab(idx) },
                    modifier = Modifier.testTag("c58_data_tab_$idx"),
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == idx) CyberCyan else TextSecondary
                        )
                    }
                )
            }
        }

        if (activeTab > 0) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                placeholder = { Text("Search name, type, KM, OSM ID...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search C58 Features", tint = CyberCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkSurfaceHighlight,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("c58_data_search_input")
            )
        }

        if (activeTab == 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "ALL ±1.5km",
                    "LEFT" to "LEFT (-1.5km)",
                    "RIGHT" to "RIGHT (+1.5km)"
                ).forEach { (key, label) ->
                    val sel = sideFilter == key
                    val c = when (key) {
                        "LEFT" -> CyberCyan
                        "RIGHT" -> LaserOrange
                        else -> NeonLime
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) c.copy(alpha = 0.22f) else DarkSurface,
                        border = BorderStroke(1.dp, if (sel) c else DarkSurfaceHighlight),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onSideFilterChange(key) }
                            .testTag("c58_house_side_filter_${key.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = if (sel) c else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        val q = searchQuery.trim().lowercase()
        when (activeTab) {
            0 -> C58MetadataSummaryList(bundle, selectedSector, Modifier.weight(1f))
            1 -> {
                val filtered = scopedRoads
                    .filter {
                        q.isEmpty() || it.name.lowercase().contains(q) ||
                            it.ref.lowercase().contains(q) ||
                            it.highwayType.lowercase().contains(q) ||
                            it.osmId.toString().contains(q)
                    }
                    .sortedWith(
                        compareByDescending<C58ExtractedRoad> {
                            it.ref.equals("C58", ignoreCase = true) ||
                                it.ref.equals("B19", ignoreCase = true) ||
                                it.name.contains("Magadi Road", ignoreCase = true)
                        }
                            .thenByDescending {
                                !it.name.startsWith("Unnamed ") &&
                                    !it.name.contains('#')
                            }
                            .thenByDescending { it.highwayType in setOf("trunk", "primary", "secondary", "tertiary") }
                            .thenBy { it.midCoord.chainageM }
                    )
                if (filtered.isEmpty()) {
                    C58EmptyNotice("No extracted Route C58 roads matched. Tap SYNC SECTOR to extract.", Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.osmId }) { road ->
                            C58RoadCard(road = road, onClick = { onSelectRoad(road) })
                        }
                    }
                }
            }
            2 -> {
                val filtered = scopedHouses.filter { h ->
                    val sideOk = when (sideFilter) {
                        "LEFT" -> h.centroid.lateralOffsetM < 0f
                        "RIGHT" -> h.centroid.lateralOffsetM >= 0f
                        else -> true
                    }
                    sideOk && (q.isEmpty() || h.name.lowercase().contains(q) ||
                        h.buildingType.lowercase().contains(q) ||
                        h.amenity.lowercase().contains(q) ||
                        h.osmId.toString().contains(q))
                }
                if (filtered.isEmpty()) {
                    C58EmptyNotice("No extracted Route C58 houses matched. Tap SYNC SECTOR to extract.", Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.osmId }) { house ->
                            C58HouseCard(
                                house = house,
                                onMapFocus = { onSelectHouseMap(house) },
                                on3dFocus = { onSelectHouse3d(house) }
                            )
                        }
                    }
                }
            }
            else -> {
                val filtered = scopedPois.filter {
                    q.isEmpty() || it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
                }
                if (filtered.isEmpty()) {
                    C58EmptyNotice("No extracted Route C58 POIs matched.", Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.osmId }) { poi ->
                            C58PoiCard(poi = poi, onClick = { onSelectPoi(poi) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun C58MetadataSummaryList(
    bundle: C58CorridorBundle,
    selectedSector: Int,
    modifier: Modifier = Modifier
) {
    val sum = bundle.summary
    val meta = bundle.metadata
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            C58InfoCard("NON-BOX 3.0 KM CORRIDOR RIBBON (±1.5 KM CENTERLINE OFFSET)") {
                C58KeyVal("Geometry", "Curved Swath Ribbon (NOT a rectangular box)")
                C58KeyVal("Corridor Span", "${"%.2f".format(sum.totalLengthKm)} km (Magadi -> Bomas)")
                C58KeyVal("Ribbon Width", "1.5 km Left + 1.5 km Right = ${"%.1f".format(sum.fullWidthKm)} km")
                C58KeyVal("Corridor Area", "${"%.1f".format(sum.corridorAreaKm2)} km² (1m zoomable UTM grid)")
                C58KeyVal("Centerline Source", if (meta.isLiveOsmCenterline) "Live OSM C58 (${sum.centerlinePoints} pts)" else "C58 Spline (${sum.centerlinePoints} pts)")
                C58KeyVal("Projection", meta.crsProjected)
                C58KeyVal("Sectors Synced", if (meta.extractedSectors.isEmpty()) "None yet (tap SYNC SECTOR)" else meta.extractedSectors.joinToString(", ") { "S$it" })
            }
        }
        item {
            val secTitle = if (selectedSector == 0) "ALL C58 SECTORS" else bundle.sectors.getOrNull(selectedSector - 1)?.title ?: "S$selectedSector"
            C58InfoCard("EXTRACTED INVENTORY ($secTitle)") {
                C58KeyVal("Corridor Houses", "${meta.houseCount} total (L: ${meta.leftSideHouseCount} | R: ${meta.rightSideHouseCount})")
                C58KeyVal("House Footprint", "${"%.0f".format(meta.totalHouseAreaM2)} m²")
                C58KeyVal("Corridor Roads", "${meta.roadCount} ways (${"%.2f".format(meta.totalRoadLengthKm)} km)")
                C58KeyVal("Water Polygons", "${meta.waterCount} basins / salt pans")
                C58KeyVal("Corridor POIs", "${meta.poiCount} named landmarks")
                C58KeyVal("Elevation ASL", "${"%.1f".format(meta.elevMinM)}m .. ${"%.1f".format(meta.elevMaxM)}m (Mean ${"%.1f".format(meta.elevMeanM)}m)")
                C58KeyVal("DEM Native Res", "${"%.1f".format(meta.demResolutionM)} m/px -> 1m C++ Bicubic")
                C58KeyVal("Extracted At", meta.extractedAtIso.ifBlank { "Pending live fetch" })
            }
        }
    }
}

@Composable
private fun C58RoadCard(road: C58ExtractedRoad, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("c58_road_card_${road.osmId}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val displayTitle = if (road.ref.isNotBlank() && !road.name.contains(road.ref, ignoreCase = true)) {
                    "${road.name} [${road.ref}]"
                } else {
                    road.name
                }
                Text(displayTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LaserOrange, modifier = Modifier.weight(1f))
                Text("S${road.sectorIndex} • #${road.osmId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
            }
            Text(
                "Class: ${road.highwayType} • Surface: ${road.surface} • ${road.lanes} lane(s) • ${"%.1f".format(road.lengthM)}m",
                fontSize = 11.sp,
                color = TextPrimary
            )
            Text(
                "KM ${"%.2f".format(road.midCoord.chainageKm)} • ${abs(road.midCoord.lateralOffsetM).toInt()}m ${if (road.midCoord.lateralOffsetM < 0f) "LEFT" else "RIGHT"} • WGS84: ${"%.5f".format(road.midCoord.lat)}, ${"%.5f".format(road.midCoord.lon)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberCyan
            )
        }
    }
}

@Composable
private fun C58HouseCard(
    house: C58ExtractedHouse,
    onMapFocus: () -> Unit,
    on3dFocus: () -> Unit
) {
    val c = house.centroid
    val isLeft = c.lateralOffsetM < 0f
    val sideColor = if (isLeft) CyberCyan else LaserOrange
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("c58_house_card_${house.osmId}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(house.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = sideColor, modifier = Modifier.weight(1f))
                Text("S${house.sectorIndex} • #${house.osmId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
            }
            Text(
                "Type: ${house.buildingType}${if (house.amenity.isNotBlank()) " (${house.amenity})" else ""} • ${house.levels} lvl (${"%.1f".format(house.heightM)}m) • ${"%.1f".format(house.footprintAreaM2)} m²",
                fontSize = 11.sp,
                color = TextPrimary
            )
            Text(
                "KM ${"%.2f".format(c.chainageKm)} • ${abs(c.lateralOffsetM).toInt()}m ${if (isLeft) "LEFT (-1.5km)" else "RIGHT (+1.5km)"} • ASL ${"%.1f".format(house.elevM)}m",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonLime
            )
            Text(
                "WGS84: ${"%.6f".format(c.lat)}, ${"%.6f".format(c.lon)} | UTM37S: ${c.utmEast.toInt()}E, ${c.utmNorth.toInt()}N",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(
                    onClick = onMapFocus,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("ZOOM 1M MAP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
                Button(
                    onClick = on3dFocus,
                    colors = ButtonDefaults.buttonColors(containerColor = LaserOrange.copy(alpha = 0.22f)),
                    border = BorderStroke(1.dp, LaserOrange),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("VIEW IN 3D", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                }
            }
        }
    }
}

@Composable
private fun C58PoiCard(poi: C58ExtractedPoi, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(poi.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonLime)
            Text("S${poi.sectorIndex} • Category: ${poi.category} • Elev: ${"%.1f".format(poi.elevM)}m ASL", fontSize = 11.sp, color = TextPrimary)
            Text(
                "KM ${"%.2f".format(poi.coord.chainageKm)} • ${abs(poi.coord.lateralOffsetM).toInt()}m ${if (poi.coord.lateralOffsetM < 0f) "LEFT" else "RIGHT"} • ${"%.5f".format(poi.coord.lat)}, ${"%.5f".format(poi.coord.lon)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberCyan
            )
        }
    }
}

@Composable
private fun C58InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, GlowingCyanBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyberCyan)
            HorizontalDivider(color = DarkSurfaceHighlight)
            content()
        }
    }
}

@Composable
private fun C58KeyVal(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(0.38f))
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary, modifier = Modifier.weight(0.62f))
    }
}

@Composable
private fun C58EmptyNotice(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, fontSize = 12.sp, color = TextSecondary)
    }
}
