package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GridCellData
import com.example.model.RockGridCatalog
import com.example.ui.theme.*

@Composable
fun RockGridInspectorView(
    catalog: RockGridCatalog?,
    selectedCell: GridCellData?,
    filterArchetype: String,
    gridType: String = "ROCKS",
    onGridTypeChange: (String) -> Unit = {},
    onCellSelect: (GridCellData) -> Unit,
    onFilterChange: (String) -> Unit,
    onFocusRockIn3D: (String) -> Unit,
    onOpenScene3D: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isTreeGrid = gridType == "TREES"
    val isGrassGrid = gridType == "GRASS"
    val activeColor = when (gridType) {
        "TREES" -> NeonLime
        "GRASS" -> Color(0xFF39FF14)
        else -> CyberCyan
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("rock_grid_inspector_container")
    ) {
        // Mode Selector: Rocks vs Trees vs Grass
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Rocks
                Surface(
                    color = if (gridType == "ROCKS") CyberCyan.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (gridType == "ROCKS") androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onGridTypeChange("ROCKS") }
                        .testTag("toggle_rocks_grid")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = "Rocks",
                            tint = if (gridType == "ROCKS") CyberCyan else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ROCKS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gridType == "ROCKS") CyberCyan else TextSecondary
                        )
                    }
                }

                // 2. Trees
                Surface(
                    color = if (gridType == "TREES") NeonLime.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (gridType == "TREES") androidx.compose.foundation.BorderStroke(1.dp, NeonLime) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onGridTypeChange("TREES") }
                        .testTag("toggle_trees_grid")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Park,
                            contentDescription = "Trees",
                            tint = if (gridType == "TREES") NeonLime else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TREES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gridType == "TREES") NeonLime else TextSecondary
                        )
                    }
                }

                // 3. Grass
                Surface(
                    color = if (gridType == "GRASS") Color(0xFF39FF14).copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (gridType == "GRASS") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF39FF14)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onGridTypeChange("GRASS") }
                        .testTag("toggle_grass_grid")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = "Grass",
                            tint = if (gridType == "GRASS") Color(0xFF39FF14) else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GRASS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gridType == "GRASS") Color(0xFF39FF14) else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Header Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                activeColor.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = activeColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val headerIcon = when {
                                    isGrassGrid -> Icons.Default.Grass
                                    isTreeGrid -> Icons.Default.Park
                                    else -> Icons.Default.GridOn
                                }
                                Icon(
                                    imageVector = headerIcon,
                                    contentDescription = "10x10 Grid",
                                    tint = activeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val headerTitle = when {
                                isGrassGrid -> "10x10 BÉZIER GRASS MEADOW"
                                isTreeGrid -> "10x10 FOREST TREE GROVE"
                                else -> "10x10 ROCK FIELD TERRAIN"
                            }
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = TextPrimary
                                )
                            )
                            val headerSub = when {
                                isGrassGrid -> "5 Botanical Species • 2 3D Models • 6 Color Blends & Thatch • Max 11cm"
                                isTreeGrid -> "Stochastic L-System • Leonardo Da Vinci (w_r=0.707) • 100 Cells"
                                else -> "100 Grid Cells • 1m x 1m Each • 100m² Footprint"
                            }
                            Text(
                                text = headerSub,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = activeColor
                                )
                            )
                        }
                    }

                    Surface(
                        color = NeonLime.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "1.0m CELL SIZE",
                            color = NeonLime,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricChip(
                        label = "TOTAL CELLS",
                        value = "${catalog?.totalCells ?: 100}",
                        icon = Icons.Default.Widgets,
                        tint = if (isTreeGrid) NeonLime else CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "GRID AREA",
                        value = "${catalog?.totalAreaM2?.toInt() ?: 100} m²",
                        icon = Icons.Default.CropFree,
                        tint = if (isTreeGrid) CyberCyan else NeonLime,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "CELL SCALE",
                        value = "1.0m × 1.0m",
                        icon = Icons.Default.Straighten,
                        tint = LaserOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenScene3D,
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("launch_3d_scene_from_grid_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXPLORE IN FULL 3D SCENE (10m × 10m FLAT GROUND)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = DarkBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Archetype Filter Bar
        Text(
            text = if (isTreeGrid) "BOTANICAL SPECIES FILTERS" else "GEOLOGICAL ARCHETYPE FILTERS",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 1.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        val filterOptions = when {
            isGrassGrid -> listOf(
                "ALL" to "All Grass Tufts (100)",
                "grass" to "Bézier Meadow Tufts"
            )
            isTreeGrid -> listOf(
                "ALL" to "All Trees (100)",
                "tree_oak" to "English Oak",
                "tree_pine" to "Scotch Pine",
                "tree_birch" to "Silver Birch",
                "tree_willow" to "Weeping Willow",
                "tree_acacia" to "Umbrella Acacia"
            )
            else -> listOf(
                "ALL" to "All Rocks (100)",
                "rock_granite" to "Granite Boulders",
                "rock_basalt" to "Basalt Columns",
                "rock_meteorite" to "Chondrite Meteorites",
                "rock_crystal" to "Quartz Geodes",
                "rock_strata" to "Sedimentary Strata"
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { (key, label) ->
                val isSelected = filterArchetype == key
                val activeTint = if (isTreeGrid) NeonLime else CyberCyan
                Surface(
                    color = if (isSelected) activeTint.copy(alpha = 0.2f) else DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) activeTint else DarkSurfaceHighlight
                    ),
                    modifier = Modifier
                        .clickable { onFilterChange(key) }
                        .testTag("filter_chip_$key")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) activeTint else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 10x10 Interactive Matrix Board
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("interactive_10x10_grid_matrix")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "10×10 SPATIAL SECTOR MATRIX (100 CELLS)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Text(
                        text = "Tap cell to inspect",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 10x10 Grid View (Horizontally Scrollable if screen is narrow)
                val gridCells = catalog?.cells ?: emptyList()
                val cellsByRow = gridCells.groupBy { it.gridRow }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until 10) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (c in 0 until 10) {
                                    val cell = cellsByRow[r]?.find { it.gridCol == c }
                                    val isSelected = selectedCell?.gridRow == r && selectedCell?.gridCol == c
                                    val isDimmed = filterArchetype != "ALL" && cell?.archetypeId != filterArchetype

                                    CellGridButton(
                                        row = r,
                                        col = c,
                                        cell = cell,
                                        isSelected = isSelected,
                                        isDimmed = isDimmed,
                                        onClick = {
                                            cell?.let { onCellSelect(it) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Cell Details Card
        selectedCell?.let { cell ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, parseHexColor(cell.accentColor).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_cell_telemetry_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = parseHexColor(cell.accentColor),
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cell.sectorId,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = parseHexColor(cell.accentColor)
                                    )
                                )
                            }
                            Text(
                                text = cell.rockTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Button(
                            onClick = { onFocusRockIn3D(cell.archetypeId) },
                            colors = ButtonDefaults.buttonColors(containerColor = parseHexColor(cell.accentColor)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("inspect_rock_3d_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "3D View",
                                tint = DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VIEW IN 3D",
                                color = DarkBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = DarkSurfaceHighlight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Spatial & Physical Metrics
                    Text(
                        text = "SPATIAL & GEOLOGICAL METRICS",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DetailChip(
                            label = "GRID COORDINATE",
                            value = "Row ${cell.gridRow}, Col ${cell.gridCol}",
                            modifier = Modifier.weight(1f)
                        )
                        DetailChip(
                            label = "CELL FOOTPRINT",
                            value = "1.0m × 1.0m",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val pos = cell.worldPositionMeters
                        DetailChip(
                            label = "WORLD (X, Y, Z)",
                            value = "${pos.getOrNull(0) ?: 0f}m, ${pos.getOrNull(1) ?: 0f}m, ${pos.getOrNull(2) ?: 0f}m",
                            modifier = Modifier.weight(1f)
                        )
                        val dim = cell.dimensionsMeters
                        DetailChip(
                            label = when {
                                isGrassGrid -> "GRASS CANOPY (W×H×D)"
                                cell.isTree -> "TREE BOUNDS (W×H×D)"
                                else -> "ROCK BOUNDS (W×H×D)"
                            },
                            value = "${dim.getOrNull(0) ?: 0f}m × ${dim.getOrNull(1) ?: 0f}m × ${dim.getOrNull(2) ?: 0f}m",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DetailChip(
                            label = when {
                                isGrassGrid -> "TURF BIOMASS VOL"
                                cell.isTree -> "TIMBER VOLUME"
                                else -> "ROCK VOLUME"
                            },
                            value = "${cell.volumeM3} m³",
                            modifier = Modifier.weight(1f)
                        )
                        DetailChip(
                            label = when {
                                isGrassGrid -> "FOLIAGE BIOMASS"
                                cell.isTree -> "WOOD BIOMASS"
                                else -> "ESTIMATED MASS"
                            },
                            value = "${cell.massKg} kg",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Parameters
                    Text(
                        text = if (cell.isTree) "L-SYSTEM & DA VINCI RULES (Stojanovic 2016)" else "PROCEDURAL NOISE PARAMETERS",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Seed: ${cell.noiseSeed}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = CyberCyan
                                )
                                Text(
                                    text = if (cell.isTree) "Iterations: ${cell.fbmOctaves}" else "FBM Octaves: ${cell.fbmOctaves}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = if (cell.isTree) "Angle: ${cell.frequency.toInt()}°" else "Frequency: ${cell.frequency}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (cell.isTree) "Leonardo: w_r = 0.707" else "Voronoi Weight: ${cell.voronoiFacets}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NeonLime
                                )
                                Text(
                                    text = if (cell.isTree) "Leaf Coverage: ${cell.surfaceRoughness}" else "Roughness: ${cell.surfaceRoughness}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = LaserOrange
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cell.geologicalNotes,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }
        } ?: run {
            // No cell selected prompt
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Select Cell",
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Select any of the 100 sectors in the 10x10 matrix above to inspect its 1m x 1m cell coordinates, procedural noise seed, volume, and mass.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}

@Composable
fun CellGridButton(
    row: Int,
    col: Int,
    cell: GridCellData?,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    val cellColor = if (cell != null) parseHexColor(cell.accentColor) else CyberCyan
    val alpha = if (isDimmed) 0.25f else 1.0f

    Surface(
        color = if (isSelected) cellColor.copy(alpha = 0.35f * alpha) else DarkSurfaceElevated.copy(alpha = alpha),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) cellColor else cellColor.copy(alpha = 0.3f * alpha)
        ),
        modifier = Modifier
            .size(32.dp)
            .clickable { onClick() }
            .testTag("cell_btn_${row}_${col}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$row,$col",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else cellColor.copy(alpha = 0.9f * alpha)
            )
        }
    }
}

@Composable
fun MetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = tint,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DetailChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                fontSize = 8.5.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        CyberCyan
    }
}
