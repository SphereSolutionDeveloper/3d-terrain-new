package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.CppAssetType
import com.example.model.ProceduralModelMesh
import com.example.ui.theme.*

@Composable
fun CppProceduralScreen(
    currentAssetType: CppAssetType,
    mesh: ProceduralModelMesh,
    activeTab: String,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isAutoRotate: Boolean,
    showGrid: Boolean,
    shadingMode: String,
    explodeFactor: Float,
    lightAngle: Float,
    bay1OpenPct: Float,
    bay2OpenPct: Float,
    neonSignOn: Boolean,
    headlightsOn: Boolean,
    taillightsOn: Boolean,
    fovAngleDeg: Float = 60.0f,
    isPerspective: Boolean = true,
    // Vulkan 1.3 Pipeline Engine parameters (Sascha Willems Architecture)
    renderingBackend: String = "Vulkan 1.3 (Sascha Willems)",
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
    onSelectAsset: (CppAssetType) -> Unit,
    onTabSelect: (String) -> Unit,
    onRotateDrag: (Float, Float) -> Unit,
    onPanDrag: (Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onToggleAutoRotate: () -> Unit,
    onToggleGrid: () -> Unit,
    onTogglePerspective: () -> Unit = {},
    onFovChange: (Float) -> Unit = {},
    onResetCamera: (String) -> Unit,
    onShadingModeSelect: (String) -> Unit,
    onExplodeFactorChange: (Float) -> Unit,
    onLightAngleChange: (Float) -> Unit,
    onBay1Change: (Float) -> Unit,
    onBay2Change: (Float) -> Unit,
    onNeonSignToggle: () -> Unit,
    onHeadlightsToggle: () -> Unit,
    onTaillightsToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .testTag("cpp_procedural_screen_container")
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. Procedural Asset Switcher (Garage 001, Modular Post, Power Grid, Heavy Lorry)
        CppAssetSelectorRow(
            currentAsset = currentAssetType,
            onSelectAsset = onSelectAsset
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Interactive 3D Viewport Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GlowingCyanBorder, RoundedCornerShape(12.dp))
        ) {
            CppViewport3DCanvas(
                mesh = mesh,
                yaw = yaw,
                pitch = pitch,
                zoom = zoom,
                panX = panX,
                panY = panY,
                isAutoRotate = isAutoRotate,
                showGrid = showGrid,
                shadingMode = shadingMode,
                explodeFactor = explodeFactor,
                lightAngle = lightAngle,
                fovAngleDeg = fovAngleDeg,
                isPerspective = isPerspective,
                renderingBackend = renderingBackend,
                vulkanPolygonMode = vulkanPolygonMode,
                vulkanMsaa = vulkanMsaa,
                vulkanValidationLayers = vulkanValidationLayers,
                vulkanDynamicRendering = vulkanDynamicRendering,
                onRotateDrag = onRotateDrag,
                onPanDrag = onPanDrag,
                onZoomChange = onZoomChange,
                onToggleAutoRotate = onToggleAutoRotate,
                onTogglePerspective = onTogglePerspective,
                onResetCamera = onResetCamera,
                onToggleBackend = onToggleRenderingBackend,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Specs, C++ Code, Interactive Rigging and Controls Tabs
        CppSpecsInspectorView(
            mesh = mesh,
            activeTab = activeTab,
            onTabSelect = onTabSelect,
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
            cableLinearMassKgM = cableLinearMassKgM,
            onCableGeometryModeChange = onCableGeometryModeChange,
            onCableCylinderSidesChange = onCableCylinderSidesChange,
            onCableConductorRadiusMmChange = onCableConductorRadiusMmChange,
            onCableTensionKnChange = onCableTensionKnChange,
            onCableLinearMassKgMChange = onCableLinearMassKgMChange,
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
    }
}

@Composable
private fun CppAssetSelectorRow(
    currentAsset: CppAssetType,
    onSelectAsset: (CppAssetType) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(CppAssetType.values()) { asset ->
            val isSelected = asset == currentAsset
            val accentColor = when (asset) {
                CppAssetType.GARAGE_001 -> CyberCyan
                CppAssetType.TEST_VEHICLE_LORRY -> LaserOrange
                CppAssetType.MODULAR_POWER_POST -> Color(0xFFFFB300)
                CppAssetType.POWER_GRID_NETWORK -> Color(0xFF00E5FF)
                CppAssetType.MODULAR_BRIDGE -> Color(0xFF64B5F6)
                CppAssetType.MODULAR_TOLL_PLAZA -> Color(0xFFFFB74D)
                CppAssetType.MODULAR_TUNNEL -> Color(0xFF81C784)
                CppAssetType.MODULAR_INTERCHANGE -> Color(0xFFBA68C8)
                CppAssetType.MODULAR_RETAINING_WALL -> Color(0xFFE57373)
                CppAssetType.CONNECTED_HIGHWAY_SCENE -> Color(0xFF4DD0E1)
                CppAssetType.MODULAR_FOOTBRIDGE -> Color(0xFF00E5FF)
                CppAssetType.MODULAR_EXPRESSWAY -> Color(0xFF26A69A)
                CppAssetType.MODULAR_SIGNPOST -> Color(0xFF66BB6A)
                CppAssetType.MODULAR_BARRIER -> Color(0xFFFFCA28)
                CppAssetType.MODULAR_ROAD_BLOCK -> Color(0xFFFF7043)
            }

            Surface(
                color = if (isSelected) accentColor.copy(alpha = 0.2f) else DarkSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) accentColor else DarkSurfaceHighlight
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectAsset(asset) }
                    .testTag("cpp_asset_card_${asset.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val iconVector = when (asset) {
                        CppAssetType.GARAGE_001 -> Icons.Default.Garage
                        CppAssetType.TEST_VEHICLE_LORRY -> Icons.Default.LocalShipping
                        CppAssetType.MODULAR_POWER_POST -> Icons.Default.ElectricBolt
                        CppAssetType.POWER_GRID_NETWORK -> Icons.Default.LinearScale
                        CppAssetType.MODULAR_BRIDGE -> Icons.Default.Directions
                        CppAssetType.MODULAR_TOLL_PLAZA -> Icons.Default.AccountBalance
                        CppAssetType.MODULAR_TUNNEL -> Icons.Default.Tune
                        CppAssetType.MODULAR_INTERCHANGE -> Icons.Default.AltRoute
                        CppAssetType.MODULAR_RETAINING_WALL -> Icons.Default.ViewAgenda
                        CppAssetType.CONNECTED_HIGHWAY_SCENE -> Icons.Default.Map
                        CppAssetType.MODULAR_FOOTBRIDGE -> Icons.Default.DirectionsWalk
                        CppAssetType.MODULAR_EXPRESSWAY -> Icons.Default.AddRoad
                        CppAssetType.MODULAR_SIGNPOST -> Icons.Default.Signpost
                        CppAssetType.MODULAR_BARRIER -> Icons.Default.Fence
                        CppAssetType.MODULAR_ROAD_BLOCK -> Icons.Default.Block
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = asset.title,
                        tint = if (isSelected) accentColor else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = asset.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                        Text(
                            text = asset.dimensionsLabel,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) accentColor else TextMuted
                        )
                    }
                }
            }
        }
    }
}
