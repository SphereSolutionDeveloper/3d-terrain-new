package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MainAppView
import com.example.ui.theme.*

@Composable
fun ApexDrawerContent(
    currentView: MainAppView,
    onSelectView: (MainAppView) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = DarkSurface,
        drawerContentColor = TextPrimary,
        modifier = modifier
            .widthIn(max = 330.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = "Logo",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "APEX 3D ASSETS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Vulkan 1.3 Procedural Engine",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tech badge pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TechPill(text = "C++20", color = NeonLime)
                        TechPill(text = "ZERO GLBs", color = LaserOrange)
                        TechPill(text = "VULKAN", color = HologramBlue)
                    }
                }
            }

            // Section 1: Native Procedural Assets
            Text(
                text = "NATIVE PROCEDURAL ASSETS (src/)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberCyan,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            DrawerItemCard(
                title = "Magadi Town 3×5km (1m)",
                subtitle = "Real OSM Roads, Buildings & 1m DEM",
                emoji = "🏙️",
                badge = "1m Grid",
                badgeColor = NeonLime,
                isSelected = currentView == MainAppView.MAGADI_TOWN_3X5,
                testTag = "drawer_item_magadi_town_3x5",
                onClick = { onSelectView(MainAppView.MAGADI_TOWN_3X5) }
            )

            DrawerItemCard(
                title = "Kenya Road Corridors & Grid",
                subtitle = "10km Road Ribbon & 416 Sqtiles",
                emoji = "🛣️",
                badge = "10km Ribbon",
                badgeColor = NeonLime,
                isSelected = currentView == MainAppView.KENYA_GRID_VIEW,
                testTag = "drawer_item_kenya_grid",
                onClick = { onSelectView(MainAppView.KENYA_GRID_VIEW) }
            )

            DrawerItemCard(
                title = "Procedural Terrain",
                subtitle = "Heightmaps, Caves, Cliffs, CDLOD",
                emoji = "⛰️",
                badge = "71 C++ Files",
                badgeColor = CyberCyan,
                isSelected = currentView == MainAppView.TERRAIN_VIEW,
                testTag = "drawer_item_terrain",
                onClick = { onSelectView(MainAppView.TERRAIN_VIEW) }
            )

            DrawerItemCard(
                title = "Road Network System",
                subtitle = "Spline Graph, Lanes & Road Tiles",
                emoji = "🛣️",
                badge = "Spline 3D",
                badgeColor = NeonLime,
                isSelected = currentView == MainAppView.ROAD_VIEW,
                testTag = "drawer_item_road",
                onClick = { onSelectView(MainAppView.ROAD_VIEW) }
            )

            DrawerItemCard(
                title = "Native Audio Engine",
                subtitle = "3D Spatial Emitters & DSP Mixers",
                emoji = "🔊",
                badge = "Spatial DSP",
                badgeColor = HologramBlue,
                isSelected = currentView == MainAppView.AUDIO_VIEW,
                testTag = "drawer_item_audio",
                onClick = { onSelectView(MainAppView.AUDIO_VIEW) }
            )

            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // Section 2: Runtime 3D Environments
            Text(
                text = "RUNTIME 3D SIMULATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = ElectricViolet,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            DrawerItemCard(
                title = "3D Scene Landscape (10×10m)",
                subtitle = "Multi-Object Scatter, Grass & Rocks",
                emoji = "🌲",
                badge = "100m² Grid",
                badgeColor = NeonLime,
                isSelected = currentView == MainAppView.SCENE_3D_VIEW,
                testTag = "drawer_item_scene_3d",
                onClick = { onSelectView(MainAppView.SCENE_3D_VIEW) }
            )

            DrawerItemCard(
                title = "C++ Procedural Power Grid",
                subtitle = "Modular Posts, Catenary Cable Sag",
                emoji = "⚡",
                badge = "Physics Math",
                badgeColor = LaserOrange,
                isSelected = currentView == MainAppView.CPP_PROCEDURAL_VIEW,
                testTag = "drawer_item_cpp_procedural",
                onClick = { onSelectView(MainAppView.CPP_PROCEDURAL_VIEW) }
            )

            DrawerItemCard(
                title = "GLB / Asset Inspector",
                subtitle = "Wireframe, Textures, Volumes & Specs",
                emoji = "📦",
                badge = "Asset Catalog",
                badgeColor = CyberCyan,
                isSelected = currentView == MainAppView.SINGLE_ASSET_INSPECTOR,
                testTag = "drawer_item_asset_inspector",
                onClick = { onSelectView(MainAppView.SINGLE_ASSET_INSPECTOR) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vulkan SwapChain Active • 60 FPS",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItemCard(
    title: String,
    subtitle: String,
    emoji: String,
    badge: String,
    badgeColor: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) CyberCyan.copy(alpha = 0.12f) else DarkSurfaceElevated.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CyberCyan else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) CyberCyan else TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.18f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TechPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
