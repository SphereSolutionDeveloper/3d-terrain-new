package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FullAssetBundle
import com.example.ui.theme.*

@Composable
fun MeshInspectorView(
    bundle: FullAssetBundle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf("Stats") } // Stats, Vertices, Faces, OBJ Source

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mesh_inspector_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Polyline,
                        contentDescription = "Mesh Geometry",
                        tint = NeonLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MESH TOPOLOGY & GEOMETRY",
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
                        text = "${bundle.geometry.vertexCount} VERTS / ${bundle.geometry.faceCount} TRIS",
                        color = NeonLime,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-tabs
            val subTabs = listOf("Stats", "Volumes", "Vertices", "Faces", "OBJ Source")
            TabRow(
                selectedTabIndex = subTabs.indexOf(selectedSubTab).coerceAtLeast(0),
                containerColor = DarkSurfaceElevated,
                contentColor = NeonLime,
                divider = {}
            ) {
                subTabs.forEach { tab ->
                    Tab(
                        selected = selectedSubTab == tab,
                        onClick = { selectedSubTab = tab },
                        text = { Text(tab, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedSubTab) {
                "Stats" -> GeometryStatsPanel(bundle)
                "Volumes" -> VolumeShapesPanel(bundle)
                "Vertices" -> VerticesTablePanel(bundle, context)
                "Faces" -> FacesTablePanel(bundle)
                "OBJ Source" -> ObjSourcePanel(bundle, context)
            }
        }
    }
}

@Composable
private fun GeometryStatsPanel(bundle: FullAssetBundle) {
    val verts = bundle.geometry.vertices
    var minX = 0f; var maxX = 0f; var minY = 0f; var maxY = 0f; var minZ = 0f; var maxZ = 0f
    if (verts.isNotEmpty()) {
        minX = verts.minOf { it[0] }; maxX = verts.maxOf { it[0] }
        minY = verts.minOf { it[1] }; maxY = verts.maxOf { it[1] }
        minZ = verts.minOf { it[2] }; maxZ = verts.maxOf { it[2] }
    }
    val dx = maxX - minX
    val dy = maxY - minY
    val dz = maxZ - minZ

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BOUNDING DIMENSIONS (METERS)", fontSize = 10.sp, color = TextMuted, letterSpacing = 0.8.sp)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    DimensionPill("Width (X)", String.format("%.2f m", dx))
                    DimensionPill("Height (Y)", String.format("%.2f m", dy))
                    DimensionPill("Depth (Z)", String.format("%.2f m", dz))
                }
            }
        }

        Surface(
            color = DarkSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SURFACE METRICS & COMPLEXITY", fontSize = 10.sp, color = TextMuted, letterSpacing = 0.8.sp)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    DimensionPill("Total Polygons", "${bundle.geometry.faceCount} Tris")
                    DimensionPill("Unique Edges", "${bundle.wireframe.edgeCount} Lines")
                    DimensionPill("Draw Calls", "1 Sub-mesh")
                }
                Divider(color = DarkSurfaceHighlight, modifier = Modifier.padding(vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    DimensionPill("Vertex Buffer", "${bundle.geometry.vertexCount * 12} Bytes")
                    DimensionPill("Index Buffer", "${bundle.geometry.indices.size * 2} Bytes")
                    DimensionPill("LOD Level", "LOD0 High-Res")
                }
            }
        }
    }
}

@Composable
private fun DimensionPill(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun VerticesTablePanel(bundle: FullAssetBundle, context: Context) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("VERTEX STREAM [X, Y, Z]", fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            TextButton(
                onClick = {
                    val sb = StringBuilder()
                    bundle.geometry.vertices.forEachIndexed { i, v ->
                        sb.append("v[$i]: (${v[0]}, ${v[1]}, ${v[2]})\n")
                    }
                    copyToClipboard(context, "Vertices", sb.toString())
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonLime)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy All", fontSize = 11.sp, color = NeonLime)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(bundle.geometry.vertices) { index, v ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "#%03d".format(index),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NeonLime
                        )
                        Text(
                            text = "X: %+.3f   Y: %+.3f   Z: %+.3f".format(v[0], v[1], v[2]),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FacesTablePanel(bundle: FullAssetBundle) {
    Column {
        Text("FACE INDICES [V1, V2, V3]", fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(bundle.geometry.faces) { index, face ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Triangle #%03d".format(index),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "(${face[0]}, ${face[1]}, ${face[2]})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeShapesPanel(bundle: FullAssetBundle) {
    val volumeShapes = bundle.geometry.volumeShapes
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DISCRETE VOLUMETRIC PRIMITIVES (${volumeShapes.size})",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = CyberCyan
        )
        if (volumeShapes.isEmpty()) {
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No volume shapes defined for this mesh.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            volumeShapes.forEach { vs ->
                val shapeColor = try {
                    Color(android.graphics.Color.parseColor(vs.colorHex))
                } catch (e: Exception) { CyberCyan }
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(shapeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = vs.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "%.1f m³".format(vs.volumeM3),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonLime
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${vs.shapeType} • ${vs.faceIndices.size} Polys / ${vs.vertexIndices.size} Verts",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = shapeColor
                        )
                        if (vs.callout.isNotEmpty()) {
                            Text(
                                text = vs.callout,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjSourcePanel(bundle: FullAssetBundle, context: Context) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${bundle.item.folder.uppercase()}.OBJ", fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            TextButton(
                onClick = { copyToClipboard(context, "Wavefront OBJ", bundle.objContent) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonLime)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy OBJ", fontSize = 11.sp, color = NeonLime)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = bundle.objContent.take(1200) + if (bundle.objContent.length > 1200) "\n... [truncated]" else "",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}
