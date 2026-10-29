package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.FileNode
import com.example.ui.theme.*

@Composable
fun FolderArchitectureView(
    folderTree: FileNode?,
    previewNode: FileNode?,
    previewContent: String?,
    onSelectNode: (FileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("folder_architecture_card")
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
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = "Modular Folder Architecture",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MODULAR FOLDER ARCHITECTURE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder)
                ) {
                    Text(
                        text = "STANDARDS COMPLIANT",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modular Explanation Banner
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PROJECT DIRECTORY TOPOLOGY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• app/  (Jetpack Compose UI & Real-Time 3D Projection Engine)\n" +
                               "• assets/ & asserts/  (Modular 3D Packages: ship/, drone/, rover/, mech_core/)\n" +
                               "   ↳ textures/ (.png albedo, normal, roughness, emissive)\n" +
                               "   ↳ json/ (.json metadata, specs, physics, materials)\n" +
                               "   ↳ meshes/ (.obj 3D models, geometry, wireframe edges)\n" +
                               "   ↳ glb/ (.glb binary glTF 2.0 containers)\n" +
                               "• dev_scripts/  (Python asset compilers & validation suite)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "INTERACTIVE MODULAR ASSET TREE",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // File Tree List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(DarkBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, DarkSurfaceHighlight, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                if (folderTree != null) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(folderTree.children) { assetNode ->
                            FolderAssetItemView(
                                node = assetNode,
                                selectedNode = previewNode,
                                onSelectNode = onSelectNode
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Preview drawer if a file is clicked
            if (previewNode != null && !previewNode.isDirectory) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "FILE: ${previewNode.path}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = "${previewNode.sizeBytes} Bytes",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (previewContent != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewContent.take(300) + if (previewContent.length > 300) "\n... [continued]" else "",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 6
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderAssetItemView(
    node: FileNode,
    selectedNode: FileNode?,
    onSelectNode: (FileNode) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column {
        // Asset top folder row (e.g. ship/)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${node.name}/",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${node.children.size} subdirs",
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                node.children.forEach { subFolderNode ->
                    SubFolderItemView(
                        node = subFolderNode,
                        selectedNode = selectedNode,
                        onSelectNode = onSelectNode
                    )
                }
            }
        }
    }
}

@Composable
private fun SubFolderItemView(
    node: FileNode,
    selectedNode: FileNode?,
    onSelectNode: (FileNode) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    val folderColor = when (node.name) {
        "textures" -> LaserOrange
        "json" -> CyberCyan
        "meshes" -> NeonLime
        "glb" -> ElectricViolet
        else -> TextSecondary
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 3.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = folderColor,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = folderColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${node.name}/",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = folderColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${node.children.size} files",
                fontSize = 9.sp,
                color = TextMuted
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 22.dp)) {
                node.children.forEach { fileNode ->
                    val isSelected = selectedNode?.path == fileNode.path
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) folderColor.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onSelectNode(fileNode) }
                            .padding(vertical = 3.dp, horizontal = 4.dp)
                    ) {
                        val fileIcon = when {
                            fileNode.name.endsWith(".png") -> Icons.Default.Image
                            fileNode.name.endsWith(".json") -> Icons.Default.DataObject
                            fileNode.name.endsWith(".obj") -> Icons.Default.Polyline
                            fileNode.name.endsWith(".glb") -> Icons.Default.ViewInAr
                            else -> Icons.Default.InsertDriveFile
                        }
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = null,
                            tint = if (isSelected) folderColor else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = fileNode.name,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${fileNode.sizeBytes} B",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
