package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun MagadiTownDataPanel(
    bounds: MagadiTownBounds,
    bundle: MagadiTownBundle?,
    activeTab: Int,
    searchQuery: String,
    onSelectTab: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectRoad: (ExtractedRoadWay) -> Unit,
    onSelectBuildingMap: (ExtractedBuilding) -> Unit,
    onSelectBuilding3d: (ExtractedBuilding) -> Unit,
    onSelectPoi: (ExtractedPoiMetadata) -> Unit,
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
                    .testTag("magadi_export_geojson_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share GeoJSON", tint = CyberCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("GEOJSON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            }
            Button(
                onClick = onExportCsv,
                colors = ButtonDefaults.buttonColors(containerColor = NeonLime.copy(alpha = 0.18f)),
                border = BorderStroke(1.dp, NeonLime),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("magadi_export_csv_btn")
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "Export Buildings CSV", tint = NeonLime, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("BUILDINGS CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonLime)
            }
        }

        // Sub-Tabs: Metadata | Roads | Buildings | POIs
        val tabs = listOf(
            "METADATA",
            "ROADS (${bundle?.roads?.size ?: 0})",
            "BUILDINGS (${bundle?.buildings?.size ?: 0})",
            "POIS (${bundle?.pois?.size ?: 0})"
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
                    modifier = Modifier.testTag("magadi_data_tab_$idx"),
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
                placeholder = { Text("Search name, type, OSM ID...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkSurfaceHighlight,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("magadi_data_search_input")
            )
        }

        val q = searchQuery.trim().lowercase()
        when (activeTab) {
            0 -> MetadataSummaryList(bounds, bundle, Modifier.weight(1f))
            1 -> {
                val roads = (bundle?.roads ?: emptyList()).filter {
                    q.isEmpty() || it.name.lowercase().contains(q) ||
                        it.highwayType.lowercase().contains(q) ||
                        it.osmId.toString().contains(q)
                }
                if (roads.isEmpty()) {
                    EmptyExtractionNotice("No extracted OSM roads matched.", Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(roads, key = { it.osmId }) { road ->
                            RoadMetadataCard(road, onClick = { onSelectRoad(road) })
                        }
                    }
                }
            }
            2 -> {
                val buildings = (bundle?.buildings ?: emptyList()).filter {
                    q.isEmpty() || it.name.lowercase().contains(q) ||
                        it.buildingType.lowercase().contains(q) ||
                        it.amenity.lowercase().contains(q) ||
                        it.shapeType.lowercase().contains(q) ||
                        it.roofType.lowercase().contains(q) ||
                        it.wallType.lowercase().contains(q) ||
                        it.parentCompoundName.lowercase().contains(q) ||
                        it.fenceType.lowercase().contains(q) ||
                        it.gateType.lowercase().contains(q) ||
                        it.osmId.toString().contains(q)
                }
                if (buildings.isEmpty()) {
                    EmptyExtractionNotice("No extracted OSM buildings matched.", Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(buildings, key = { it.osmId }) { bldg ->
                            BuildingMetadataCard(
                                building = bldg,
                                onMapFocus = { onSelectBuildingMap(bldg) },
                                on3dFocus = { onSelectBuilding3d(bldg) }
                            )
                        }
                    }
                }
            }
            else -> {
                val pois = (bundle?.pois ?: emptyList()).filter {
                    q.isEmpty() || it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
                }
                if (pois.isEmpty()) {
                    EmptyExtractionNotice("No extracted POIs matched.", Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pois, key = { it.osmId }) { poi ->
                            PoiMetadataCard(poi, onClick = { onSelectPoi(poi) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataSummaryList(
    bounds: MagadiTownBounds,
    bundle: MagadiTownBundle?,
    modifier: Modifier = Modifier
) {
    val meta = bundle?.metadata
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            InfoSectionCard("1M GRID & EXTENT (3 KM × 5 KM = 15 KM²)") {
                KeyValLine("Grid Matrix", "${bounds.gridCols} × ${bounds.gridRows} cells (${bounds.gridResolutionM} m/cell)")
                KeyValLine("Total 1m Cells", "15,000,000 indexed cells")
                KeyValLine("Center WGS84", "${"%.5f".format(bounds.centerLat)}, ${"%.5f".format(bounds.centerLon)}")
                KeyValLine("Lat Span", "${"%.5f".format(bounds.minLat)} to ${"%.5f".format(bounds.maxLat)}")
                KeyValLine("Lon Span", "${"%.5f".format(bounds.minLon)} to ${"%.5f".format(bounds.maxLon)}")
                KeyValLine("UTM 37S Origin", "E ${"%.1f".format(bounds.originUtmEast)} | N ${"%.1f".format(bounds.originUtmNorth)}")
            }
        }
        item {
            InfoSectionCard("EXTRACTED TOWN INVENTORY & DEM RELIEF") {
                KeyValLine("Place", meta?.placeName ?: "Magadi Town, Kajiado County, Kenya")
                KeyValLine("OSM Roads", "${meta?.roadCount ?: 0} ways (${"%.2f".format(meta?.totalRoadLengthKm ?: 0f)} km)")
                KeyValLine("OSM Buildings", "${meta?.buildingCount ?: 0} footprints (${"%.0f".format(meta?.totalBuildingFootprintM2 ?: 0f)} m²)")
                KeyValLine("Water Polygons", "${bundle?.waterPolygons?.size ?: meta?.waterCount ?: 0} OSM lakes / salt pans")
                KeyValLine("Town POIs", "${meta?.poiCount ?: 0} named places & facilities")
                KeyValLine(
                    "Elevation ASL",
                    "Min ${"%.1f".format(meta?.elevMinM ?: 595f)}m | Mean ${"%.1f".format(meta?.elevMeanM ?: 625f)}m | Max ${"%.1f".format(meta?.elevMaxM ?: 720f)}m"
                )
                KeyValLine("Lake Water Level", "${"%.2f".format(meta?.lakeLevelAslM ?: 602.5f)} m ASL")
                KeyValLine("DEM Native Res", "${"%.1f".format(meta?.demResolutionM ?: 9.5f)} m/px")
                KeyValLine("DEM Grid", bundle?.demGrid?.let { "${it.cols}×${it.rows} ${it.source}" } ?: "C++ Basin Baseline")
                val v2Bldgs = bundle?.buildings ?: emptyList()
                val fencedCount = v2Bldgs.count { it.hasFence }
                val gatedCount = v2Bldgs.count { it.hasGate }
                val compoundChildren = v2Bldgs.count { it.parentCompoundId != 0L }
                val multiStorey = v2Bldgs.count { it.levels > 1 }
                KeyValLine("v2 Arch & Fences", "$compoundChildren compound inner • $multiStorey multi-storey • $fencedCount fenced / $gatedCount gated")
                KeyValLine("Extracted At", meta?.extractedAtIso?.ifBlank { "Pending live fetch" } ?: "Pending")
            }
        }
    }
}

@Composable
private fun RoadMetadataCard(road: ExtractedRoadWay, onClick: () -> Unit) {
    val start = road.nodes.firstOrNull()
    val end = road.nodes.lastOrNull()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("magadi_road_card_${road.osmId}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(road.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                Text("OSM #${road.osmId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
            }
            Text(
                "Class: ${road.highwayType} • Surface: ${road.surface} • ${road.lanes} lane(s) • Length: ${"%.1f".format(road.lengthM)}m",
                fontSize = 11.sp,
                color = TextPrimary
            )
            if (start != null && end != null) {
                Text(
                    "Start [${start.cellCol},${start.cellRow}] (${"%.5f".format(start.lat)}, ${"%.5f".format(start.lon)}) → End [${end.cellCol},${end.cellRow}]",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
            }
        }
    }
}

@Composable
private fun BuildingMetadataCard(
    building: ExtractedBuilding,
    onMapFocus: () -> Unit,
    on3dFocus: () -> Unit
) {
    val c = building.centroid
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("magadi_building_card_${building.osmId}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(building.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan, modifier = Modifier.weight(1f))
                Text("OSM #${building.osmId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
            }
            if (building.parentCompoundName.isNotBlank()) {
                val sharedNote = if (!building.hasFence) " • Shares compound fence & gate" else ""
                Text(
                    "COMPOUND: ${building.parentCompoundName}$sharedNote",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = LaserOrange
                )
            }
            Text(
                "Type: ${building.buildingType}${if (building.amenity.isNotBlank()) " (${building.amenity})" else ""} • Shape: ${building.shapeType} • ${building.levels}F × ${"%.2f".format(building.floorHeightM)}m = ${"%.1f".format(building.heightM)}m • Area: ${"%.1f".format(building.footprintAreaM2)} m²",
                fontSize = 11.sp,
                color = TextPrimary
            )
            Text(
                "Envelope: Roof ${building.roofType} (${building.roofPitchDeg.toInt()}°, ${building.roofMaterial}) • Wall: ${building.wallType} • Win: ${building.windowType} (${(building.windowToWallRatio * 100).toInt()}%)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            if (building.hasFence || building.hasGate) {
                val gc = building.gateCoord
                val gatePosTxt = if (gc != null) {
                    " @ ${"%.5f".format(gc.lat)}, ${"%.5f".format(gc.lon)} (${gc.utmEast.toInt()}E, ${gc.utmNorth.toInt()}N)"
                } else ""
                Text(
                    "Perimeter: ${building.fenceType} (${"%.1f".format(building.fenceHeightM)}m) • Gate: ${building.gateType} (${"%.1f".format(building.gateWidthM)}m)$gatePosTxt",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
            }
            Text(
                "WGS84: ${"%.6f".format(c.lat)}, ${"%.6f".format(c.lon)} | 1m Cell: [${c.cellCol}, ${c.cellRow}] | ASL: ${"%.1f".format(building.elevM)}m",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonLime
            )
            Text(
                "UTM 37S: E ${"%.1f".format(c.utmEast)}, N ${"%.1f".format(c.utmNorth)} • ${building.polygon.size} vertices",
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
private fun PoiMetadataCard(poi: ExtractedPoiMetadata, onClick: () -> Unit) {
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
            Text("Category: ${poi.category} • Elev: ${"%.1f".format(poi.elevM)}m ASL", fontSize = 11.sp, color = TextPrimary)
            Text(
                "WGS84: ${"%.6f".format(poi.coord.lat)}, ${"%.6f".format(poi.coord.lon)} • 1m Cell [${poi.coord.cellCol}, ${poi.coord.cellRow}]",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberCyan
            )
        }
    }
}

@Composable
private fun InfoSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
private fun KeyValLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(0.36f))
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary, modifier = Modifier.weight(0.64f))
    }
}

@Composable
private fun EmptyExtractionNotice(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(message, fontSize = 12.sp, color = TextSecondary)
    }
}
