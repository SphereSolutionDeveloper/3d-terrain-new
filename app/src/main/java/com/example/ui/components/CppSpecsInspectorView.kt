package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CppAssetType
import com.example.model.ProceduralModelMesh
import com.example.ui.theme.*

@Composable
fun CppSpecsInspectorView(
    mesh: ProceduralModelMesh,
    activeTab: String,
    onTabSelect: (String) -> Unit,
    // Vulkan 1.3 Pipeline Engine parameters
    renderingBackend: String = "Vulkan 1.3",
    vulkanPolygonMode: String = "VK_POLYGON_MODE_FILL",
    vulkanMsaa: String = "VK_SAMPLE_COUNT_4_BIT",
    vulkanValidationLayers: Boolean = true,
    vulkanDynamicRendering: Boolean = true,
    vulkanPipelineCache: Boolean = true,
    vulkanPushConstantSag: Boolean = true,
    onRenderingBackendChange: (String) -> Unit = {},
    onToggleRenderingBackend: () -> Unit = {},
    onVulkanPolygonModeChange: (String) -> Unit = {},
    onVulkanMsaaChange: (String) -> Unit = {},
    onToggleVulkanValidationLayers: () -> Unit = {},
    onToggleVulkanDynamicRendering: () -> Unit = {},
    onToggleVulkanPipelineCache: () -> Unit = {},
    onToggleVulkanPushConstantSag: () -> Unit = {},
    // Garage controls
    bay1OpenPct: Float,
    bay2OpenPct: Float,
    neonSignOn: Boolean,
    onBay1Change: (Float) -> Unit,
    onBay2Change: (Float) -> Unit,
    onNeonSignToggle: () -> Unit,
    // Vehicle controls
    headlightsOn: Boolean,
    taillightsOn: Boolean,
    onHeadlightsToggle: () -> Unit,
    onTaillightsToggle: () -> Unit,
    // Modular Electric Post controls
    postModularType: String = "CORNER",
    postHasTransformer: Boolean = true,
    postHasStreetlight: Boolean = true,
    postStreetlightOn: Boolean = true,
    postHasGuyWire: Boolean = true,
    onPostModularTypeChange: (String) -> Unit = {},
    onTogglePostTransformer: () -> Unit = {},
    onTogglePostStreetlight: () -> Unit = {},
    onTogglePostStreetlightOn: () -> Unit = {},
    onTogglePostGuyWire: () -> Unit = {},
    // Power Grid Network controls
    gridPostCount: Int = 4,
    gridCornerAngleDeg: Float = 85.0f,
    gridWireSagM: Float = 0.55f,
    gridEnergized: Boolean = true,
    gridStreetlightsOn: Boolean = true,
    onGridPostCountChange: (Int) -> Unit = {},
    onGridCornerAngleChange: (Float) -> Unit = {},
    onGridWireSagChange: (Float) -> Unit = {},
    onToggleGridEnergized: () -> Unit = {},
    onToggleGridStreetlightsOn: () -> Unit = {},
    // Cable Catenary Physics & Cylinder Controls
    cableGeometryMode: String = "CYLINDER",
    cableCylinderSides: Int = 8,
    cableConductorRadiusMm: Float = 35.0f,
    cableTensionKn: Float = 18.5f,
    cableLinearMassKgM: Float = 1.25f,
    onCableGeometryModeChange: (String) -> Unit = {},
    onCableCylinderSidesChange: (Int) -> Unit = {},
    onCableConductorRadiusMmChange: (Float) -> Unit = {},
    onCableTensionKnChange: (Float) -> Unit = {},
    onCableLinearMassKgMChange: (Float) -> Unit = {},
    // Shading & view controls
    shadingMode: String,
    explodeFactor: Float,
    lightAngle: Float,
    fovAngleDeg: Float = 60.0f,
    isPerspective: Boolean = true,
    onFovChange: (Float) -> Unit = {},
    onTogglePerspective: () -> Unit = {},
    onShadingModeSelect: (String) -> Unit,
    onExplodeFactorChange: (Float) -> Unit,
    onLightAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = if (mesh.wires.isNotEmpty() || mesh.assetType == CppAssetType.POWER_GRID_NETWORK) {
        listOf("CONTROLS", "SPECS", "CATENARY PHYSICS", "C++ CODE", "PRIMITIVES", "GLB EXPORT")
    } else {
        listOf("CONTROLS", "SPECS", "C++ CODE", "PRIMITIVES", "GLB EXPORT")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tab Selector Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tabs) { tab ->
                val isSelected = tab == activeTab || (activeTab.startsWith("INTERACTIVE") && tab == "CONTROLS") || (activeTab.startsWith("SPECS") && tab == "SPECS") || (activeTab.startsWith("CABLE") && tab == "CATENARY PHYSICS") || (activeTab.startsWith("C++") && tab == "C++ CODE") || (activeTab.startsWith("COMPONENTS") && tab == "PRIMITIVES") || (activeTab.startsWith("GLB") && tab == "GLB EXPORT")
                Surface(
                    color = if (isSelected) CyberCyan.copy(alpha = 0.22f) else DarkSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.2.dp else 1.dp,
                        color = if (isSelected) CyberCyan else DarkSurfaceHighlight
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelect(tab) }
                        .testTag("cpp_tab_${tab.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = tab,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CyberCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Area Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("cpp_specs_content_card")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                when {
                    activeTab == "CONTROLS" || activeTab == "INTERACTIVE CONTROLS" -> InteractiveControlsTab(
                        mesh = mesh,
                        renderingBackend = renderingBackend,
                        vulkanPolygonMode = vulkanPolygonMode,
                        vulkanMsaa = vulkanMsaa,
                        vulkanValidationLayers = vulkanValidationLayers,
                        vulkanDynamicRendering = vulkanDynamicRendering,
                        vulkanPipelineCache = vulkanPipelineCache,
                        vulkanPushConstantSag = vulkanPushConstantSag,
                        onRenderingBackendChange = onRenderingBackendChange,
                        onToggleRenderingBackend = onToggleRenderingBackend,
                        onVulkanPolygonModeChange = onVulkanPolygonModeChange,
                        onVulkanMsaaChange = onVulkanMsaaChange,
                        onToggleVulkanValidationLayers = onToggleVulkanValidationLayers,
                        onToggleVulkanDynamicRendering = onToggleVulkanDynamicRendering,
                        onToggleVulkanPipelineCache = onToggleVulkanPipelineCache,
                        onToggleVulkanPushConstantSag = onToggleVulkanPushConstantSag,
                        bay1OpenPct = bay1OpenPct,
                        bay2OpenPct = bay2OpenPct,
                        neonSignOn = neonSignOn,
                        onBay1Change = onBay1Change,
                        onBay2Change = onBay2Change,
                        onNeonSignToggle = onNeonSignToggle,
                        headlightsOn = headlightsOn,
                        taillightsOn = taillightsOn,
                        onHeadlightsToggle = onHeadlightsToggle,
                        onTaillightsToggle = onTaillightsToggle,
                        postModularType = postModularType,
                        postHasTransformer = postHasTransformer,
                        postHasStreetlight = postHasStreetlight,
                        postStreetlightOn = postStreetlightOn,
                        postHasGuyWire = postHasGuyWire,
                        onPostModularTypeChange = onPostModularTypeChange,
                        onTogglePostTransformer = onTogglePostTransformer,
                        onTogglePostStreetlight = onTogglePostStreetlight,
                        onTogglePostStreetlightOn = onTogglePostStreetlightOn,
                        onTogglePostGuyWire = onTogglePostGuyWire,
                        gridPostCount = gridPostCount,
                        gridCornerAngleDeg = gridCornerAngleDeg,
                        gridWireSagM = gridWireSagM,
                        gridEnergized = gridEnergized,
                        gridStreetlightsOn = gridStreetlightsOn,
                        onGridPostCountChange = onGridPostCountChange,
                        onGridCornerAngleChange = onGridCornerAngleChange,
                        onGridWireSagChange = onGridWireSagChange,
                        onToggleGridEnergized = onToggleGridEnergized,
                        onToggleGridStreetlightsOn = onToggleGridStreetlightsOn,
                        cableGeometryMode = cableGeometryMode,
                        cableCylinderSides = cableCylinderSides,
                        cableConductorRadiusMm = cableConductorRadiusMm,
                        cableTensionKn = cableTensionKn,
                        onCableGeometryModeChange = onCableGeometryModeChange,
                        onCableCylinderSidesChange = onCableCylinderSidesChange,
                        onCableConductorRadiusMmChange = onCableConductorRadiusMmChange,
                        onCableTensionKnChange = onCableTensionKnChange,
                        shadingMode = shadingMode,
                        explodeFactor = explodeFactor,
                        lightAngle = lightAngle,
                        fovAngleDeg = fovAngleDeg,
                        isPerspective = isPerspective,
                        onFovChange = onFovChange,
                        onTogglePerspective = onTogglePerspective,
                        onShadingModeSelect = onShadingModeSelect,
                        onExplodeFactorChange = onExplodeFactorChange,
                        onLightAngleChange = onLightAngleChange
                    )
                    activeTab == "SPECS" || activeTab == "SPECS & BLUEPRINT" -> SpecsBlueprintTab(
                        mesh = mesh,
                        renderingBackend = renderingBackend,
                        vulkanPolygonMode = vulkanPolygonMode,
                        vulkanMsaa = vulkanMsaa,
                        vulkanDynamicRendering = vulkanDynamicRendering,
                        vulkanValidationLayers = vulkanValidationLayers
                    )
                    activeTab == "CATENARY PHYSICS" || activeTab == "CABLE PHYSICS & CYLINDER" -> CablePhysicsCylinderTab(
                        mesh = mesh,
                        cableGeometryMode = cableGeometryMode,
                        cableCylinderSides = cableCylinderSides,
                        cableConductorRadiusMm = cableConductorRadiusMm,
                        cableTensionKn = cableTensionKn,
                        cableLinearMassKgM = cableLinearMassKgM,
                        gridWireSagM = gridWireSagM,
                        onCableGeometryModeChange = onCableGeometryModeChange,
                        onCableCylinderSidesChange = onCableCylinderSidesChange,
                        onCableConductorRadiusMmChange = onCableConductorRadiusMmChange,
                        onCableTensionKnChange = onCableTensionKnChange,
                        onCableLinearMassKgMChange = onCableLinearMassKgMChange
                    )
                    activeTab == "C++ CODE" || activeTab == "C++ SOURCE CODE" -> CppSourceCodeTab(
                        assetType = mesh.assetType,
                        renderingBackend = renderingBackend
                    )
                    activeTab == "PRIMITIVES" || activeTab == "COMPONENTS LIST" -> ComponentsListTab(mesh)
                    activeTab == "GLB EXPORT" || activeTab == "GLB EXPORTER" -> GlbExporterTab(mesh.assetType)
                    else -> SpecsBlueprintTab(
                        mesh = mesh,
                        renderingBackend = renderingBackend,
                        vulkanPolygonMode = vulkanPolygonMode,
                        vulkanMsaa = vulkanMsaa,
                        vulkanDynamicRendering = vulkanDynamicRendering,
                        vulkanValidationLayers = vulkanValidationLayers
                    )
                }
            }
        }
    }
}
