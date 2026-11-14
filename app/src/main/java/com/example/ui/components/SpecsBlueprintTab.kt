package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CppAssetType
import com.example.model.ProceduralModelMesh
import com.example.ui.theme.*

@Composable
fun SpecsBlueprintTab(
    mesh: ProceduralModelMesh,
    renderingBackend: String = "Vulkan 1.3",
    vulkanPolygonMode: String = "VK_POLYGON_MODE_FILL",
    vulkanMsaa: String = "VK_SAMPLE_COUNT_4_BIT",
    vulkanDynamicRendering: Boolean = true,
    vulkanValidationLayers: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Compact Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MetricCard(
                title = "VERTICES",
                value = "${mesh.totalVertices}",
                subtitle = "Primitives",
                color = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "TRIANGLES",
                value = "${mesh.totalTriangles}",
                subtitle = "Indices",
                color = ElectricViolet,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "COMMAND",
                value = "DrawIndexed",
                subtitle = "Dynamic",
                color = NeonLime,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "VMA MEM",
                value = "${(mesh.memoryFootprintBytes / 1024f).toInt()} KB",
                subtitle = "Device-Local",
                color = LaserOrange,
                modifier = Modifier.weight(1f)
            )
        }

        // Compact Specs Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecRow("Primary Pipeline", "Vulkan 1.3 C++20 Core")
            SpecRow("Dynamic Rendering", if (vulkanDynamicRendering) "VK_KHR_dynamic_rendering" else "VkRenderPass")
            SpecRow("Envelope Size", mesh.assetType.dimensionsLabel)
            SpecRow("Vertex Buffer", "${mesh.memoryFootprintBytes} B (VMA Device-Local)")
            SpecRow("Index Buffer", "${mesh.totalTriangles * 12} B (uint32_t)")
            SpecRow("Rasterization", vulkanPolygonMode.replace("VK_POLYGON_MODE_", ""))
            SpecRow("Multisampling", vulkanMsaa.replace("VK_SAMPLE_COUNT_", "").replace("_BIT", "x MSAA"))
            SpecRow("Validation", if (vulkanValidationLayers) "Khronos Active (0 err)" else "Disabled")
            SpecRow("Normals", "Per-Vertex Unit Normals")
            SpecRow("Material Preset", when (mesh.assetType) {
                CppAssetType.GARAGE_001 -> "Steel Slate & Crimson Trim"
                CppAssetType.TEST_VEHICLE_LORRY -> "Cyan Cab, Silver Cargo & Amber Lights"
                CppAssetType.MODULAR_POWER_POST -> "Timber Mast, Porcelain & Crossarm"
                CppAssetType.POWER_GRID_NETWORK -> "3-Phase ACSR & Guy Cables"
                CppAssetType.MODULAR_BRIDGE -> "Asphalt, Concrete Box & High-Tensile Steel Cables"
                CppAssetType.MODULAR_TOLL_PLAZA -> "Polycarbonate Canopy, Concrete Islands & LED VMS"
                CppAssetType.MODULAR_TUNNEL -> "Shotcrete NATM Lining, Jet Fans & Reflective Niches"
                CppAssetType.MODULAR_INTERCHANGE -> "Multi-Level Flyovers, Spiral Ramps & NJ Barriers"
                CppAssetType.MODULAR_RETAINING_WALL -> "MSE Precast Panels & Wire Rock Gabions"
                CppAssetType.CONNECTED_HIGHWAY_SCENE -> "Integrated Vulkan 1.3 AASHTO Highway Corridor"
                CppAssetType.MODULAR_FOOTBRIDGE -> "Tubular Steel Spines, Glulam Timber & Architectural Glass"
                CppAssetType.MODULAR_EXPRESSWAY -> "SMA Wearing Course, Jersey Median & Cobra-Head LED Poles"
                CppAssetType.MODULAR_SIGNPOST -> "Galvanized Warren Trusses, Retroreflective Signs & LED Matrix"
                CppAssetType.MODULAR_BARRIER -> "MASH Concrete F-Shape, W-Beam Steel & EVA Safety Rollers"
                CppAssetType.MODULAR_ROAD_BLOCK -> "Safety Orange Barricades, K12 Stainless Bollards & Beacons"
            })
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = title,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
