package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.theme.*

@Composable
fun TextureInspectorView(
    bundle: FullAssetBundle,
    activeTextureKey: String,
    activeChannelMode: String,
    textureBitmaps: Map<String, Bitmap>,
    onSelectTextureKey: (String) -> Unit,
    onSelectChannelMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("texture_inspector_card")
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
                        imageVector = Icons.Default.Texture,
                        contentDescription = "Texture Maps",
                        tint = LaserOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MODULAR PBR TEXTURE MAPS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
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
                        text = bundle.textureManifest?.resolution ?: "128x128 PNG",
                        color = LaserOrange,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Texture Map Selector Pills (Albedo, Normal, Roughness, Emissive)
            Text(
                text = "SELECT TEXTURE CHANNEL MAP",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "albedo.png" to "Albedo",
                    "normal.png" to "Normal",
                    "roughness.png" to "Roughness",
                    "emissive.png" to "Emissive"
                ).forEach { (filename, label) ->
                    val isSelected = activeTextureKey == filename
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) LaserOrange.copy(alpha = 0.2f) else DarkSurfaceElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) LaserOrange else DarkSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectTextureKey(filename) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Channel Filter Modes (RGBA, Red, Green, Blue, Alpha)
            Text(
                text = "SOLO COLOR CHANNELS",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, letterSpacing = 0.8.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("RGBA (Full)", "Red", "Green", "Blue", "Alpha").forEach { channel ->
                    FilterChip(
                        selected = activeChannelMode == channel,
                        onClick = { onSelectChannelMode(channel) },
                        label = { Text(channel.split(" ")[0], fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bitmap Texture Display
            val bmp = textureBitmaps[activeTextureKey]
            val processedBmp = remember(bmp, activeChannelMode) {
                if (bmp == null || activeChannelMode == "RGBA (Full)") {
                    bmp
                } else {
                    filterBitmapChannel(bmp, activeChannelMode)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Texture Preview Box
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(1.dp, GlowingCyanBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (processedBmp != null) {
                        Image(
                            bitmap = processedBmp.asImageBitmap(),
                            contentDescription = "Texture $activeTextureKey",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        Text(
                            text = "Loading\nTexture...",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Metadata breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = activeTextureKey.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when (activeTextureKey) {
                            "albedo.png" -> "Base surface diffuse color. Dictates light absorption and reflectivity."
                            "normal.png" -> "Tangent space surface perturbations [R=X, G=Y, B=Z]."
                            "roughness.png" -> "Micro-surface roughness. Darker = Glossy, Lighter = Matte."
                            "emissive.png" -> "HDR self-illumination mask for engines and cockpit telemetry."
                            else -> "PBR Material Map"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Path: assets/${bundle.item.folder}/textures/$activeTextureKey",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun filterBitmapChannel(src: Bitmap, channel: String): Bitmap {
    val width = src.width
    val height = src.height
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    src.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val color = pixels[i]
        val a = (color shr 24) and 0xFF
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        val newColor = when (channel) {
            "Red" -> (0xFF shl 24) or (r shl 16) or (r shl 8) or r
            "Green" -> (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            "Blue" -> (0xFF shl 24) or (b shl 16) or (b shl 8) or b
            "Alpha" -> (0xFF shl 24) or (a shl 16) or (a shl 8) or a
            else -> color
        }
        pixels[i] = newColor
    }
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
}
