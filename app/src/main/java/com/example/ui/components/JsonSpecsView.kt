package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FullAssetBundle
import com.example.ui.theme.*

@Composable
fun JsonSpecsView(
    bundle: FullAssetBundle,
    activeJsonTab: String,
    searchQuery: String,
    onSelectJsonTab: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs = listOf("metadata.json", "specs.json", "physics.json", "materials.json")

    val rawContent = when (activeJsonTab) {
        "metadata.json" -> bundle.metadataJson
        "specs.json" -> bundle.specsJson
        "physics.json" -> bundle.physicsJson
        "materials.json" -> bundle.materialsJson
        else -> bundle.metadataJson
    }

    val filteredLines = remember(rawContent, searchQuery) {
        val lines = rawContent.lines()
        if (searchQuery.isBlank()) {
            lines
        } else {
            lines.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("json_specs_card")
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
                        imageVector = Icons.Default.DataObject,
                        contentDescription = "JSON Specifications",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MODULAR JSON TELEMETRY & SPECS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(activeJsonTab, rawContent))
                        Toast.makeText(context, "$activeJsonTab copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy JSON", tint = CyberCyan, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-tabs
            TabRow(
                selectedTabIndex = tabs.indexOf(activeJsonTab).coerceAtLeast(0),
                containerColor = DarkSurfaceElevated,
                contentColor = CyberCyan,
                divider = {}
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = activeJsonTab == tab,
                        onClick = { onSelectJsonTab(tab) },
                        text = {
                            Text(
                                tab.replace(".json", ""),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Filter JSON keys / values...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkSurfaceHighlight,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("json_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // JSON Code Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(DarkBackground, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                SelectionContainer {
                    val vScroll = rememberScrollState()
                    val hScroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                    ) {
                        filteredLines.forEachIndexed { idx, line ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%02d ".format(idx + 1),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                                Text(
                                    text = line,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (line.contains(":")) {
                                        if (line.contains("{") || line.contains("[")) TextPrimary else CyberCyan
                                    } else {
                                        TextSecondary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
