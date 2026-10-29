package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FullAssetBundle
import com.example.model.VolumeShape
import com.example.ui.theme.*

@Composable
fun VolumeDiagramView(
    bundle: FullAssetBundle,
    selectedVolumeShapeId: String?,
    explodeFactor: Float,
    textureBitmaps: Map<String, Bitmap>,
    onSelectVolumeShape: (String?) -> Unit,
    onExplodeFactorChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val volumeShapes = bundle.geometry.volumeShapes
    val totalVolumeM3 = volumeShapes.sumOf { it.volumeM3 }.coerceAtLeast(1.0)
    val selectedShape = volumeShapes.firstOrNull { it.id == selectedVolumeShapeId }
    val volumeDiagramBitmap = textureBitmaps["volume_diagram.png"]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("volume_diagram_view")
    ) {
        // 1. Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Volumetric Decomposition",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOLUMETRIC DECOMPOSITION ARCHITECTURE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        )
                    }

                    Surface(
                        color = NeonLime.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${volumeShapes.size} VOLUMES",
                            color = NeonLime,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Deconstructed 3D volume primitives (cylinders, domes, airfoil slabs) and marked schematic blueprint digest.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Volume Shape Selector Chips
        Text(
            text = "INSPECT VOLUME PRIMITIVE",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All Volumes" chip
            val isAllSelected = selectedVolumeShapeId == null
            FilterChip(
                selected = isAllSelected,
                onClick = { onSelectVolumeShape(null) },
                label = { Text("All Volumes (Composite)", fontSize = 11.sp) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAllSelected) CyberCyan else TextMuted)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                    selectedLabelColor = TextPrimary,
                    containerColor = DarkSurfaceElevated,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isAllSelected,
                    borderColor = DarkSurfaceHighlight,
                    selectedBorderColor = CyberCyan
                )
            )

            // Individual Volume Shape Chips
            volumeShapes.forEach { vs ->
                val isSelected = vs.id == selectedVolumeShapeId
                val shapeColor = try {
                    Color(android.graphics.Color.parseColor(vs.colorHex))
                } catch (e: Exception) { CyberCyan }

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectVolumeShape(if (isSelected) null else vs.id) },
                    label = { Text(vs.name, fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(shapeColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = shapeColor.copy(alpha = 0.25f),
                        selectedLabelColor = TextPrimary,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = DarkSurfaceHighlight,
                        selectedBorderColor = shapeColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Exploded Assembly View Slider
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Explode Volumes",
                            tint = ElectricViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXPLODED ASSEMBLY SEPARATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    Text(
                        text = "${(explodeFactor * 100).toInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (explodeFactor > 0f) NeonLime else TextSecondary
                    )
                }

                Slider(
                    value = explodeFactor,
                    onValueChange = onExplodeFactorChange,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Assembled (0%)", fontSize = 10.sp, color = TextMuted)
                    Text("Full Radial Separation (100%)", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Selected Volume Telemetry Card (or Summary if none selected)
        if (selectedShape != null) {
            val shapeColor = try {
                Color(android.graphics.Color.parseColor(selectedShape.colorHex))
            } catch (e: Exception) { CyberCyan }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, shapeColor.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = selectedShape.name.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = shapeColor
                                )
                            )
                            if (selectedShape.callout.isNotEmpty()) {
                                Text(
                                    text = selectedShape.callout,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            color = shapeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, shapeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = selectedShape.role.uppercase(),
                                color = shapeColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Grid (Primitive Type, Volume, Dimensions, Faces)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VolumeMetricBox(
                            title = "PRIMITIVE TYPE",
                            value = selectedShape.shapeType,
                            color = shapeColor,
                            modifier = Modifier.weight(1f)
                        )
                        VolumeMetricBox(
                            title = "DISPLACEMENT",
                            value = "%.1f m³ (%.1f%%)".format(
                                selectedShape.volumeM3,
                                (selectedShape.volumeM3 / totalVolumeM3) * 100.0
                            ),
                            color = NeonLime,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val dimsStr = if (selectedShape.dimensions.size >= 3) {
                            "%.1fm × %.1fm × %.1fm".format(
                                selectedShape.dimensions[0],
                                selectedShape.dimensions[1],
                                selectedShape.dimensions[2]
                            )
                        } else "N/A"
                        VolumeMetricBox(
                            title = "BOUNDS (W×H×D)",
                            value = dimsStr,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        VolumeMetricBox(
                            title = "MESH FACETS",
                            value = "${selectedShape.faceIndices.size} Polys (${selectedShape.vertexIndices.size} Verts)",
                            color = CyberCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Engineering description
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedShape.description,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        } else {
            // General Assembly Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COMPOSITE 3D VOLUMETRIC ASSEMBLY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The Arrow Interceptor is decomposed into ${volumeShapes.size} discrete volume primitives with a total structural displacement of %.1f m³. Each volume shape maintains distinct collision boundaries, mass distributions, and manufacturing tolerances.".format(totalVolumeM3),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mini summary list
                    volumeShapes.forEach { vs ->
                        val shapeColor = try {
                            Color(android.graphics.Color.parseColor(vs.colorHex))
                        } catch (e: Exception) { CyberCyan }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVolumeShape(vs.id) }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(shapeColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = vs.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "%.1f m³".format(vs.volumeM3),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Marked Technical Blueprint Schematic Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Technical Schematic Blueprint",
                            tint = LaserOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MARKED SCHEMATIC BLUEPRINT DIGEST",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Surface(
                        color = LaserOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "TOP-DOWN DIGEST",
                            color = LaserOrange,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display volume_diagram.png if available
                if (volumeDiagramBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .background(Color(0xFF0B132B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = volumeDiagramBitmap.asImageBitmap(),
                            contentDescription = "Marked Volume Decomposition Schematic Blueprint",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Generating Blueprint Schematic...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Architectural explanation card
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WHY VOLUMETRIC DECOMPOSITION MATTERS:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rendering complex spacecraft solely with unorganized vertex triangles produces flat, ambiguous geometry. By decomposing the schematic into distinct 3D volume diagrams (cylindrical engine nacelles, teardrop canopy capsule, chiseled lifting-body fuselage, and crenellated airfoil slabs), the 3D model maintains accurate mass distribution, true aerodynamic cross-sections, and precise collision hulls.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeMetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1
            )
        }
    }
}
