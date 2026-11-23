// ==============================================================================
// Modular Road Component Studio: Real-World Civil Engineering & Vulkan 1.3 Scene
// Path: app/src/main/java/com/example/ui/components/RoadProceduralScreen.kt
// Standards: AASHTO LRFD, FHWA, NFPA 502, Eurocode EN 1991-2
// Vulkan Scene Standard: Sascha Willems Framework & Khronos Vulkan Samples
// ==============================================================================
package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.Math3D
import com.example.engine.NativeEngineBridge
import com.example.model.NativeFileTreeItem
import com.example.model.NativeSourceCatalog
import com.example.model.NativeVulkanMesh
import com.example.ui.theme.*
import com.example.viewmodel.AssetStudioViewModel
import kotlin.math.*

private data class RoadProjectedTri(
    val path: Path,
    val color: Color,
    val avgDepth: Float,
    val isOutline: Boolean = false,
    val outlineColor: Color = Color.Transparent,
    val nx: Float = 0f,
    val ny: Float = 1f,
    val nz: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadProceduralScreen(
    viewModel: AssetStudioViewModel? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    // Category / Component selection
    var selectedCategory by remember { mutableStateOf("CONNECTED_SCENE") } // CONNECTED_SCENE, BRIDGE, TOLL_PLAZA, TUNNEL, INTERCHANGE, RETAINING_WALL
    var activeSubTab by remember { mutableStateOf("CONTROLS") } // CONTROLS, CODE, CROSS_SECTION

    // Bridge Parameters
    var bridgeType by remember { mutableStateOf("CABLE_STAYED") } // CABLE_STAYED, CONCRETE_VIADUCT, STEEL_TRUSS, PARABOLIC_ARCH
    var bridgeSpanLength by remember { mutableFloatStateOf(160f) }
    var bridgeDeckWidth by remember { mutableFloatStateOf(24f) }
    var bridgePylonHeight by remember { mutableFloatStateOf(58f) }
    var bridgeCurveBendDeg by remember { mutableFloatStateOf(0f) }
    var bridgeSuperelevationDeg by remember { mutableFloatStateOf(3.5f) }
    var bridgeCablePairs by remember { mutableIntStateOf(8) }
    var bridgeHasBarriers by remember { mutableStateOf(true) }

    // Toll Plaza Parameters
    var tollPlazaType by remember { mutableStateOf("CANOPY_PLAZA") } // CANOPY_PLAZA, ETC_GANTRY, CHECKPOINT, RAMP_GATE
    var tollLaneCount by remember { mutableIntStateOf(6) }
    var tollLaneWidth by remember { mutableFloatStateOf(3.8f) }
    var tollCanopyClearance by remember { mutableFloatStateOf(5.5f) }
    var tollPlazaLength by remember { mutableFloatStateOf(64f) }
    var tollHasVMS by remember { mutableStateOf(true) }

    // Tunnel Parameters
    var tunnelType by remember { mutableStateOf("MOUNTAIN_HORSESHOE") } // MOUNTAIN_HORSESHOE, CIRCULAR_BORED, CUT_AND_COVER, PORTAL_TRANSITION
    var tunnelInternalRadius by remember { mutableFloatStateOf(5.8f) }
    var tunnelLength by remember { mutableFloatStateOf(120f) }
    var tunnelCurveBendDeg by remember { mutableFloatStateOf(0f) }
    var tunnelJetFanSpacing by remember { mutableFloatStateOf(35f) }
    var tunnelHasPortal by remember { mutableStateOf(true) }
    var tunnelHasJetFans by remember { mutableStateOf(true) }

    // Interchange Parameters
    var interchangeType by remember { mutableStateOf("CLOVERLEAF_LOOP") } // CLOVERLEAF_LOOP, DIRECTIONAL_FLYOVER, ROUNDABOUT
    var interchangeLoopRadius by remember { mutableFloatStateOf(55f) }
    var interchangeArcDeg by remember { mutableFloatStateOf(270f) }
    var interchangeRampWidth by remember { mutableFloatStateOf(7.5f) }
    var interchangeElevationDelta by remember { mutableFloatStateOf(7.5f) }
    var interchangeBankingDeg by remember { mutableFloatStateOf(6.0f) }

    // Retaining Wall Parameters
    var wallType by remember { mutableStateOf("MSE_MODULAR") } // MSE_MODULAR, STEPPED_GABION, CANTILEVER_RC
    var wallHeight by remember { mutableFloatStateOf(6.5f) }
    var wallLength by remember { mutableFloatStateOf(60f) }
    var wallBatterDeg by remember { mutableFloatStateOf(4.0f) }

    // Footbridge Parameters
    var footbridgeType by remember { mutableStateOf("PEDESTRIAN_FOOTBRIDGE") }
    var footbridgeSpanLength by remember { mutableFloatStateOf(75f) }
    var footbridgeDeckWidth by remember { mutableFloatStateOf(4.8f) }
    var footbridgePylonHeight by remember { mutableFloatStateOf(22f) }
    var footbridgeCurveDeg by remember { mutableFloatStateOf(0f) }

    // Expressway Parameters
    var expresswayType by remember { mutableStateOf("SIX_LANE_FREEWAY") }
    var expLanesPerDir by remember { mutableIntStateOf(3) }
    var expSectionLength by remember { mutableFloatStateOf(120f) }
    var expCurveBendDeg by remember { mutableFloatStateOf(14f) }
    var expBankingDeg by remember { mutableFloatStateOf(4.5f) }
    var expHasLighting by remember { mutableStateOf(true) }

    // Signpost & Overhead Gantry Parameters
    var signpostType by remember { mutableStateOf("CANTILEVER_HIGHWAY") }
    var signSpanM by remember { mutableFloatStateOf(18f) }
    var signClearanceM by remember { mutableFloatStateOf(5.8f) }
    var signBoardWidthM by remember { mutableFloatStateOf(6.5f) }
    var signCount by remember { mutableIntStateOf(3) }
    var signHasDMS by remember { mutableStateOf(true) }

    // Safety Barrier & Guardrail Parameters
    var barrierType by remember { mutableStateOf("NEW_JERSEY_FSHAPE") }
    var barrierLengthM by remember { mutableFloatStateOf(48f) }
    var barrierHeightM by remember { mutableFloatStateOf(1.10f) }
    var barrierCurveDeg by remember { mutableFloatStateOf(12f) }
    var barrierPostSpacingM by remember { mutableFloatStateOf(2.0f) }
    var barrierHasReflectors by remember { mutableStateOf(true) }

    // Road Block & Workzone Parameters
    var roadBlockType by remember { mutableStateOf("WATER_JERSEY_LINE") }
    var rbRoadWidthM by remember { mutableFloatStateOf(12f) }
    var rbZoneLengthM by remember { mutableFloatStateOf(34f) }
    var rbUnitCount by remember { mutableIntStateOf(6) }
    var rbHasBeacons by remember { mutableStateOf(true) }

    // Road Network & Intersections Parameters
    var roadNetworkType by remember { mutableStateOf("SIGNALIZED_4WAY") }
    var rnRoadWidthM by remember { mutableFloatStateOf(12.0f) }
    var rnArmSpanM by remember { mutableFloatStateOf(56.0f) }
    var rnLaneCount by remember { mutableIntStateOf(3) }
    var rnCornerRadiusM by remember { mutableFloatStateOf(8.0f) }
    var rnHasSignalMasts by remember { mutableStateOf(true) }

    // Clothoid Spline & Superelevation Parameters
    var splineType by remember { mutableStateOf("CLOTHOID_S_CURVE") }
    var spLengthM by remember { mutableFloatStateOf(110.0f) }
    var spWidthM by remember { mutableFloatStateOf(10.5f) }
    var spDesignSpeedKmh by remember { mutableFloatStateOf(80.0f) }
    var spElevationGainM by remember { mutableFloatStateOf(8.0f) }
    var spCurvatureIntensity by remember { mutableFloatStateOf(1.0f) }

    // Terrain-Conforming Road Tile & Surface States Parameters
    var roadTileType by remember { mutableStateOf("CUT_FILL_HILLSIDE") }
    var rtTileSizeM by remember { mutableFloatStateOf(36.0f) }
    var rtRoadWidthM by remember { mutableFloatStateOf(11.0f) }
    var rtCutFillHeightM by remember { mutableFloatStateOf(4.5f) }
    var rtDistressDepthM by remember { mutableFloatStateOf(0.06f) }
    var rtHasDrainageAndCurbs by remember { mutableStateOf(true) }

    // 3D Camera & Viewport States (Stationary default for fixed inspection)
    var yaw by remember { mutableFloatStateOf(0.68f) }
    var pitch by remember { mutableFloatStateOf(0.36f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var isAutoRotate by remember { mutableStateOf(false) }
    var shadingMode by remember { mutableStateOf("Lit Shaded") } // Lit Shaded, Wireframe, Normals
    var isPerspective by remember { mutableStateOf(true) }

    // C++ / GLSL / Compute Shader Code Viewer
    var codeFilter by remember { mutableStateOf("ALL") } // ALL, CPP, GLSL, COMP
    var selectedFile by remember { mutableStateOf(NativeSourceCatalog.roadFiles.first()) }
    var copyNotice by remember { mutableStateOf("") }

    // Auto-rotation loop
    LaunchedEffect(isAutoRotate) {
        while (isAutoRotate) {
            yaw = (yaw + 0.008f) % (2f * Math.PI.toFloat())
            kotlinx.coroutines.delay(30)
        }
    }

    // Generate active 3D Model Mesh dynamically based on current configuration (C++ libapex_vulkan.so)
    val activeMesh: NativeVulkanMesh = remember(
        selectedCategory,
        bridgeType, bridgeSpanLength, bridgeDeckWidth, bridgePylonHeight, bridgeCurveBendDeg, bridgeSuperelevationDeg, bridgeCablePairs, bridgeHasBarriers,
        tollPlazaType, tollLaneCount, tollLaneWidth, tollCanopyClearance, tollPlazaLength, tollHasVMS,
        tunnelType, tunnelInternalRadius, tunnelLength, tunnelCurveBendDeg, tunnelJetFanSpacing, tunnelHasPortal, tunnelHasJetFans,
        interchangeType, interchangeLoopRadius, interchangeArcDeg, interchangeRampWidth, interchangeElevationDelta, interchangeBankingDeg,
        wallType, wallHeight, wallLength, wallBatterDeg,
        footbridgeType, footbridgeSpanLength, footbridgeDeckWidth, footbridgePylonHeight, footbridgeCurveDeg,
        expresswayType, expLanesPerDir, expSectionLength, expCurveBendDeg, expBankingDeg, expHasLighting,
        signpostType, signSpanM, signClearanceM, signBoardWidthM, signCount, signHasDMS,
        barrierType, barrierLengthM, barrierHeightM, barrierCurveDeg, barrierPostSpacingM, barrierHasReflectors,
        roadBlockType, rbRoadWidthM, rbZoneLengthM, rbUnitCount, rbHasBeacons,
        roadNetworkType, rnRoadWidthM, rnArmSpanM, rnLaneCount, rnCornerRadiusM, rnHasSignalMasts,
        splineType, spLengthM, spWidthM, spDesignSpeedKmh, spElevationGainM, spCurvatureIntensity,
        roadTileType, rtTileSizeM, rtRoadWidthM, rtCutFillHeightM, rtDistressDepthM, rtHasDrainageAndCurbs
    ) {
        when (selectedCategory) {
            "BRIDGE" -> NativeEngineBridge.generateModularBridgeMesh(
                bridgeType = bridgeType,
                spanLengthM = bridgeSpanLength,
                deckWidthM = bridgeDeckWidth,
                pylonHeightM = bridgePylonHeight,
                curveBendAngleDeg = bridgeCurveBendDeg,
                superelevationDeg = bridgeSuperelevationDeg,
                cablePairsCount = bridgeCablePairs,
                hasBarriers = bridgeHasBarriers
            )
            "FOOTBRIDGE" -> NativeEngineBridge.generateModularBridgeMesh(
                bridgeType = footbridgeType,
                spanLengthM = footbridgeSpanLength,
                deckWidthM = footbridgeDeckWidth,
                pylonHeightM = footbridgePylonHeight,
                curveBendAngleDeg = footbridgeCurveDeg,
                superelevationDeg = 0f,
                cablePairsCount = 8,
                hasBarriers = true
            )
            "EXPRESSWAY" -> NativeEngineBridge.generateModularExpresswayMesh(
                expresswayType = expresswayType,
                lanesPerDir = expLanesPerDir,
                sectionLengthM = expSectionLength,
                curveBendDeg = expCurveBendDeg,
                bankingDeg = expBankingDeg,
                hasLighting = expHasLighting
            )
            "SIGNPOST" -> NativeEngineBridge.generateModularSignpostMesh(
                signType = signpostType,
                gantrySpanM = signSpanM,
                clearanceH = signClearanceM,
                boardWidthM = signBoardWidthM,
                signCount = signCount,
                hasDMS = signHasDMS
            )
            "BARRIER" -> NativeEngineBridge.generateModularBarrierMesh(
                barrierType = barrierType,
                barrierLengthM = barrierLengthM,
                barrierHeightM = barrierHeightM,
                curveBendDeg = barrierCurveDeg,
                postSpacingM = barrierPostSpacingM,
                hasReflectors = barrierHasReflectors
            )
            "ROAD_BLOCK" -> NativeEngineBridge.generateModularRoadBlockMesh(
                roadBlockType = roadBlockType,
                roadWidthM = rbRoadWidthM,
                zoneLengthM = rbZoneLengthM,
                unitCount = rbUnitCount,
                hasBeacons = rbHasBeacons
            )
            "TOLL_PLAZA" -> NativeEngineBridge.generateModularTollPlazaMesh(
                tollType = tollPlazaType,
                laneCount = tollLaneCount,
                laneWidthM = tollLaneWidth,
                plazaLengthM = tollPlazaLength,
                canopyClearanceM = tollCanopyClearance,
                hasDynamicVMS = tollHasVMS
            )
            "TUNNEL" -> NativeEngineBridge.generateModularTunnelMesh(
                tunnelType = tunnelType,
                internalRadiusM = tunnelInternalRadius,
                tunnelLengthM = tunnelLength,
                curveBendAngleDeg = tunnelCurveBendDeg,
                jetFanSpacingM = tunnelJetFanSpacing,
                hasFlaredPortal = tunnelHasPortal,
                hasJetFans = tunnelHasJetFans
            )
            "INTERCHANGE" -> NativeEngineBridge.generateModularInterchangeMesh(
                interchangeType = interchangeType,
                loopRadiusM = interchangeLoopRadius,
                totalArcAngleDeg = interchangeArcDeg,
                rampWidthM = interchangeRampWidth,
                elevationDeltaM = interchangeElevationDelta,
                superelevationBankingDeg = interchangeBankingDeg
            )
            "RETAINING_WALL" -> NativeEngineBridge.generateModularRetainingWallMesh(
                wallType = wallType,
                wallHeightM = wallHeight,
                wallLengthM = wallLength,
                wallBatterAngleDeg = wallBatterDeg
            )
            "ROAD_NETWORK" -> NativeEngineBridge.generateModularRoadNetworkMesh(
                networkType = roadNetworkType,
                roadWidthM = rnRoadWidthM,
                armSpanM = rnArmSpanM,
                laneCount = rnLaneCount,
                cornerRadiusM = rnCornerRadiusM,
                hasSignals = rnHasSignalMasts
            )
            "SPLINE_GRAPH" -> NativeEngineBridge.generateModularSplineGraphMesh(
                splineType = splineType,
                splineLengthM = spLengthM,
                roadWidthM = spWidthM,
                designSpeedKmH = spDesignSpeedKmh,
                elevationGainM = spElevationGainM,
                curvatureIntensity = spCurvatureIntensity
            )
            "ROAD_TILE" -> NativeEngineBridge.generateModularRoadTileMesh(
                tileType = roadTileType,
                tileSizeM = rtTileSizeM,
                roadWidthM = rtRoadWidthM,
                cutFillHeightM = rtCutFillHeightM,
                distressDepthM = rtDistressDepthM,
                hasDrainageAndCurbs = rtHasDrainageAndCurbs
            )
            else -> NativeEngineBridge.generateConnectedHighwayScene()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ─── 1. TOP COMPONENT SELECTION BAR ──────────────────────────────────────
        val categories = listOf(
            "CONNECTED_SCENE" to "Highway Scene",
            "ROAD_NETWORK" to "Road Network",
            "SPLINE_GRAPH" to "Spline Graph",
            "ROAD_TILE" to "Surface & Tile",
            "BRIDGE" to "Bridge System",
            "FOOTBRIDGE" to "Footbridge",
            "EXPRESSWAY" to "Expressway",
            "SIGNPOST" to "Signposts",
            "BARRIER" to "Barriers",
            "ROAD_BLOCK" to "Road Blocks",
            "TOLL_PLAZA" to "Toll Plaza",
            "TUNNEL" to "Tunnel Bore",
            "INTERCHANGE" to "Interchange Ramp",
            "RETAINING_WALL" to "Retaining Wall"
        )

        Surface(
            color = Color(0xFF070B12),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { (catKey, catLabel) ->
                    val isSelected = selectedCategory == catKey
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) CyberCyan else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .clickable {
                                selectedCategory = catKey
                                // Reset camera appropriate for component envelope
                                when (catKey) {
                                    "CONNECTED_SCENE" -> { yaw = 0.78f; pitch = 0.38f; zoom = 0.95f }
                                    "ROAD_NETWORK" -> { yaw = 0.78f; pitch = 0.62f; zoom = 1.0f }
                                    "SPLINE_GRAPH" -> { yaw = 0.88f; pitch = 0.44f; zoom = 0.98f }
                                    "ROAD_TILE" -> { yaw = 0.74f; pitch = 0.48f; zoom = 1.05f }
                                    "BRIDGE" -> { yaw = 0.65f; pitch = 0.32f; zoom = 1.0f }
                                    "FOOTBRIDGE" -> { yaw = 0.68f; pitch = 0.30f; zoom = 1.05f }
                                    "EXPRESSWAY" -> { yaw = 0.72f; pitch = 0.36f; zoom = 1.0f }
                                    "SIGNPOST" -> { yaw = 0.52f; pitch = 0.24f; zoom = 1.05f }
                                    "BARRIER" -> { yaw = 0.75f; pitch = 0.30f; zoom = 1.05f }
                                    "ROAD_BLOCK" -> { yaw = 0.62f; pitch = 0.34f; zoom = 1.05f }
                                    "TOLL_PLAZA" -> { yaw = 0.85f; pitch = 0.35f; zoom = 1.05f }
                                    "TUNNEL" -> { yaw = 0.45f; pitch = 0.28f; zoom = 1.0f }
                                    "INTERCHANGE" -> { yaw = 1.05f; pitch = 0.52f; zoom = 0.9f }
                                    "RETAINING_WALL" -> { yaw = 0.55f; pitch = 0.25f; zoom = 1.1f }
                                }
                                panX = 0f
                                panY = 0f
                            }
                            .testTag("road_category_$catKey")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NeonLime else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = catLabel.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) CyberCyan else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ─── 2. 3D INTERACTIVE VIEWPORT (CANVAS) ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.35f)
                .background(Color(0xFF0A0E17))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        if (gestureZoom != 1f) {
                            zoom = (zoom * gestureZoom).coerceIn(0.15f, 5.0f)
                        } else if (pan != Offset.Zero) {
                            yaw = (yaw + pan.x * 0.008f) % (2f * Math.PI.toFloat())
                            pitch = (pitch + pan.y * 0.008f).coerceIn(0.05f, 1.50f)
                        }
                    }
                }
                .testTag("road_3d_viewport_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f + panX
                val cy = h * 0.5f + panY

                val extentX = (activeMesh.boundingBoxMax[0] - activeMesh.boundingBoxMin[0]).coerceAtLeast(1f)
                val extentY = (activeMesh.boundingBoxMax[1] - activeMesh.boundingBoxMin[1]).coerceAtLeast(1f)
                val extentZ = (activeMesh.boundingBoxMax[2] - activeMesh.boundingBoxMin[2]).coerceAtLeast(1f)
                val bridgeExtentM = maxOf(extentX, extentY * 1.4f, extentZ, 32f)

                // Scale factor based on physical component AABB envelope (1:1 world meters)
                val baseScale = when (selectedCategory) {
                    "CONNECTED_SCENE" -> (min(w, h) / 290f) * zoom
                    "ROAD_NETWORK" -> (min(w, h) / 74f) * zoom
                    "SPLINE_GRAPH" -> (min(w, h) / 118f) * zoom
                    "ROAD_TILE" -> (min(w, h) / 60f) * zoom
                    "BRIDGE", "FOOTBRIDGE" -> (min(w, h) / (bridgeExtentM * 0.92f)) * zoom
                    "EXPRESSWAY" -> (min(w, h) / 135f) * zoom
                    "SIGNPOST" -> (min(w, h) / 38f) * zoom
                    "BARRIER" -> (min(w, h) / 48f) * zoom
                    "ROAD_BLOCK" -> (min(w, h) / 42f) * zoom
                    "TOLL_PLAZA" -> (min(w, h) / 62f) * zoom
                    "TUNNEL" -> (min(w, h) / 95f) * zoom
                    "INTERCHANGE" -> (min(w, h) / 145f) * zoom
                    "RETAINING_WALL" -> (min(w, h) / 58f) * zoom
                    else -> (min(w, h) / 90f) * zoom
                }

                val centerModelX = (activeMesh.boundingBoxMin[0] + activeMesh.boundingBoxMax[0]) * 0.5f
                val centerModelY = if (selectedCategory == "BRIDGE" || selectedCategory == "FOOTBRIDGE") {
                    (activeMesh.boundingBoxMin[1] + activeMesh.boundingBoxMax[1]) * 0.42f
                } else {
                    (activeMesh.boundingBoxMin[1] + activeMesh.boundingBoxMax[1]) * 0.48f
                }
                val centerModelZ = (activeMesh.boundingBoxMin[2] + activeMesh.boundingBoxMax[2]) * 0.5f

                val cameraDist = when (selectedCategory) {
                    "CONNECTED_SCENE" -> 440.0f
                    "ROAD_NETWORK" -> 98.0f
                    "SPLINE_GRAPH" -> 162.0f
                    "ROAD_TILE" -> 84.0f
                    "BRIDGE", "FOOTBRIDGE" -> bridgeExtentM * 1.45f
                    "EXPRESSWAY" -> 200.0f
                    "SIGNPOST" -> 62.0f
                    "BARRIER" -> 76.0f
                    "ROAD_BLOCK" -> 66.0f
                    "TOLL_PLAZA" -> 95.0f
                    "TUNNEL" -> 125.0f
                    "INTERCHANGE" -> 185.0f
                    "RETAINING_WALL" -> 90.0f
                    else -> 120.0f
                }

                // 1. Draw 3D Ground Grid calibrated to exact 10m engineering survey squares for bridges
                val gridHalfSize = when (selectedCategory) {
                    "SIGNPOST", "ROAD_BLOCK" -> 36.0f
                    "BARRIER", "ROAD_TILE" -> 48.0f
                    "ROAD_NETWORK" -> 64.0f
                    "SPLINE_GRAPH" -> 110.0f
                    "BRIDGE", "FOOTBRIDGE" -> (kotlin.math.ceil((maxOf(extentX, extentZ) * 0.60f) / 10.0f) * 10.0f).coerceAtLeast(30.0f)
                    else -> 120.0f
                }
                val gridSteps = if (selectedCategory == "BRIDGE" || selectedCategory == "FOOTBRIDGE") {
                    ((gridHalfSize * 2.0f) / 10.0f).toInt().coerceIn(8, 36)
                } else {
                    16
                }
                val stepM = (gridHalfSize * 2.0f) / gridSteps
                for (g in 0..gridSteps) {
                    val coord = -gridHalfSize + g * stepM
                    val pA = Math3D.project3DTo2D(coord, 0.0f, -gridHalfSize, yaw, pitch, baseScale, cx, cy, centerModelX, 0f, centerModelZ, cameraDist, isPerspective)
                    val pB = Math3D.project3DTo2D(coord, 0.0f, gridHalfSize, yaw, pitch, baseScale, cx, cy, centerModelX, 0f, centerModelZ, cameraDist, isPerspective)
                    val isAxis = kotlin.math.abs(coord) < 0.1f
                    drawLine(
                        color = if (isAxis) CyberCyan.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f),
                        start = Offset(pA.screenX, pA.screenY),
                        end = Offset(pB.screenX, pB.screenY),
                        strokeWidth = if (isAxis) 1.5f else 0.8f
                    )

                    val pC = Math3D.project3DTo2D(-gridHalfSize, 0.0f, coord, yaw, pitch, baseScale, cx, cy, centerModelX, 0f, centerModelZ, cameraDist, isPerspective)
                    val pD = Math3D.project3DTo2D(gridHalfSize, 0.0f, coord, yaw, pitch, baseScale, cx, cy, centerModelX, 0f, centerModelZ, cameraDist, isPerspective)
                    drawLine(
                        color = if (isAxis) LaserOrange.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f),
                        start = Offset(pC.screenX, pC.screenY),
                        end = Offset(pD.screenX, pD.screenY),
                        strokeWidth = if (isAxis) 1.5f else 0.8f
                    )
                }

                // 2. Project and sort Native C++ Vulkan 3D Triangles (Painter's algorithm)
                val quads = mutableListOf<RoadProjectedTri>()
                val vCount = activeMesh.vertexCount
                val vertices = activeMesh.vertices
                val indices = activeMesh.indices
                val tCount = activeMesh.triangleCount

                val screenX = FloatArray(vCount)
                val screenY = FloatArray(vCount)
                val depths = FloatArray(vCount)

                for (i in 0 until vCount) {
                    val b = i * 12
                    val vx = vertices[b + 0]
                    val vy = vertices[b + 1]
                    val vz = vertices[b + 2]
                    val proj = Math3D.project3DTo2D(
                        vx, vy, vz, yaw, pitch, baseScale, cx, cy,
                        centerModelX, centerModelY, centerModelZ, cameraDist, isPerspective
                    )
                    screenX[i] = proj.screenX
                    screenY[i] = proj.screenY
                    depths[i] = proj.depth
                }

                for (t in 0 until tCount) {
                    val i0 = indices[t * 3 + 0]
                    val i1 = indices[t * 3 + 1]
                    val i2 = indices[t * 3 + 2]

                    if (i0 >= vCount || i1 >= vCount || i2 >= vCount) continue

                    val x0 = screenX[i0]; val y0 = screenY[i0]
                    val x1 = screenX[i1]; val y1 = screenY[i1]
                    val x2 = screenX[i2]; val y2 = screenY[i2]

                    // Screen-space backface culling via signed area
                    val crossZ = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)
                    val isCable = (vertices[i0 * 12 + 10] > 0.8f && vertices[i0 * 12 + 8] > 0.8f)
                    if (!isCable && shadingMode != "Wireframe" && crossZ <= 0.0f) {
                        continue
                    }

                    val avgDepth = (depths[i0] + depths[i1] + depths[i2]) * 0.3333f

                    val nx = vertices[i0 * 12 + 3]
                    val ny = vertices[i0 * 12 + 4]
                    val nz = vertices[i0 * 12 + 5]

                    val dotL = max(0f, nx * 0.40f + ny * 0.85f + nz * 0.35f)
                    val litFactor = (0.45f + 0.55f * dotL).coerceIn(0.2f, 1.0f)

                    val rC = (vertices[i0 * 12 + 8] * litFactor).coerceIn(0f, 1f)
                    val gC = (vertices[i0 * 12 + 9] * litFactor).coerceIn(0f, 1f)
                    val bC = (vertices[i0 * 12 + 10] * litFactor).coerceIn(0f, 1f)
                    val aC = vertices[i0 * 12 + 11].coerceIn(0f, 1f)

                    val polyPath = Path().apply {
                        moveTo(x0, y0)
                        lineTo(x1, y1)
                        lineTo(x2, y2)
                        close()
                    }

                    val faceColor = when (shadingMode) {
                        "Wireframe" -> Color.Transparent
                        "Normals" -> Color((nx + 1f) * 0.5f, (ny + 1f) * 0.5f, (nz + 1f) * 0.5f, 1f)
                        else -> Color(rC, gC, bC, aC)
                    }

                    quads.add(
                        RoadProjectedTri(
                            path = polyPath,
                            color = faceColor,
                            avgDepth = avgDepth,
                            isOutline = shadingMode == "Wireframe",
                            outlineColor = if (shadingMode == "Wireframe") CyberCyan else Color.Black.copy(alpha = 0.20f),
                            nx = nx, ny = ny, nz = nz
                        )
                    )
                }

                // Render sorted native triangles from back to front
                quads.sortBy { it.avgDepth }
                for (q in quads) {
                    if (shadingMode != "Wireframe") {
                        drawPath(q.path, color = q.color)
                    }
                    if (q.isOutline) {
                        drawPath(q.path, color = q.outlineColor, style = Stroke(width = 1.0f))
                    }
                }
            }

            // HUD Overlays: Vulkan 1.3 Telemetry & Architecture Badges
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface.copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlowingCyanBorder.copy(alpha = 0.3f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonLime)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VULKAN 1.3 REAL-WORLD ARCHITECTURE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                    }
                    Text(
                        text = when (selectedCategory) {
                            "ROAD_NETWORK" -> "MUTCD Ch. 4D Signalized Junctions & NACTO Urban Street Grids"
                            "SPLINE_GRAPH" -> "AASHTO Clothoid / Catmull-Rom Banking θ = arctan(v²/gR)"
                            "ROAD_TILE" -> "Quadtree Road Tiles, Hillside Cut/Fill & ASTM Surface States"
                            "BRIDGE" -> "AASHTO LRFD / Eurocode EN 1991-2 Bridge Specification"
                            "FOOTBRIDGE" -> "Pedestrian Overpass / AASHTO LRFD Guide for Pedestrian Bridges"
                            "EXPRESSWAY" -> "AASHTO Divided Multi-Lane Freeway & EV Smart Corridor"
                            "SIGNPOST" -> "FHWA MUTCD Overhead Gantries, Cantilevers & DMS Sign Supports"
                            "BARRIER" -> "MASH 2016 Crash Barriers, W-Beam Guardrails & Sound Walls"
                            "ROAD_BLOCK" -> "MUTCD Workzones & ASTM F2656 K12 Anti-Ram Checkpoints"
                            "TOLL_PLAZA" -> "FHWA Toll Facilities / ASECAP Electronic Tolling"
                            "TUNNEL" -> "PIARC Tunnels / NFPA 502 Subterranean Fire Safety"
                            "INTERCHANGE" -> "AASHTO Green Book Geometric Design & Clothoids"
                            "RETAINING_WALL" -> "FHWA GEC 011 MSE Walls / Eurocode 7 Geotechnical"
                            else -> "Combined Connected Highway System (Toll -> Bridge -> Tunnel)"
                        },
                        fontSize = 10.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "VRAM: ${(activeMesh.vertexCount * 48 + activeMesh.triangleCount * 12) / 1024} KB | Verts: ${activeMesh.vertexCount} | Triangles: ${activeMesh.triangleCount}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }

            // Viewport Floating Controls: Shading Mode & Presets
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface.copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { isAutoRotate = !isAutoRotate },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoRotate) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Auto Rotation",
                            tint = if (isAutoRotate) NeonLime else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            shadingMode = when (shadingMode) {
                                "Lit Shaded" -> "Wireframe"
                                else -> "Lit Shaded"
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Wireframe",
                            tint = if (shadingMode == "Wireframe") CyberCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            when (selectedCategory) {
                                "CONNECTED_SCENE" -> { yaw = 0.78f; pitch = 0.38f; zoom = 0.95f }
                                "ROAD_NETWORK" -> { yaw = 0.78f; pitch = 0.62f; zoom = 1.0f }
                                "SPLINE_GRAPH" -> { yaw = 0.88f; pitch = 0.44f; zoom = 0.98f }
                                "ROAD_TILE" -> { yaw = 0.74f; pitch = 0.48f; zoom = 1.05f }
                                "BRIDGE" -> { yaw = 0.65f; pitch = 0.32f; zoom = 1.0f }
                                "FOOTBRIDGE" -> { yaw = 0.68f; pitch = 0.30f; zoom = 1.05f }
                                "EXPRESSWAY" -> { yaw = 0.72f; pitch = 0.36f; zoom = 1.0f }
                                "SIGNPOST" -> { yaw = 0.52f; pitch = 0.24f; zoom = 1.05f }
                                "BARRIER" -> { yaw = 0.75f; pitch = 0.30f; zoom = 1.05f }
                                "ROAD_BLOCK" -> { yaw = 0.62f; pitch = 0.34f; zoom = 1.05f }
                                "TOLL_PLAZA" -> { yaw = 0.85f; pitch = 0.35f; zoom = 1.05f }
                                "TUNNEL" -> { yaw = 0.45f; pitch = 0.28f; zoom = 1.0f }
                                "INTERCHANGE" -> { yaw = 1.05f; pitch = 0.52f; zoom = 0.9f }
                                "RETAINING_WALL" -> { yaw = 0.55f; pitch = 0.25f; zoom = 1.1f }
                                else -> { yaw = 0.78f; pitch = 0.38f; zoom = 1.0f }
                            }
                            panX = 0f
                            panY = 0f
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Camera",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ─── 3. SUB-TABS: CONTROLS vs CODE vs CROSS-SECTION ─────────────────────
        TabRow(
            selectedTabIndex = when (activeSubTab) {
                "CONTROLS" -> 0
                "CODE" -> 1
                else -> 2
            },
            containerColor = Color(0xFF0F141C),
            contentColor = CyberCyan,
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Tab(
                selected = activeSubTab == "CONTROLS",
                onClick = { activeSubTab = "CONTROLS" },
                text = { Text("PARAMETRIC CONTROLS", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == "CODE",
                onClick = { activeSubTab = "CODE" },
                text = { Text("C++ / VULKAN SOURCE", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == "CROSS_SECTION",
                onClick = { activeSubTab = "CROSS_SECTION" },
                text = { Text("CROSS SECTION & SPECS", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // ─── 4. TAB CONTENT ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .background(DarkSurface)
        ) {
            when (activeSubTab) {
                "CONTROLS" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedCategory) {
                            "BRIDGE" -> {
                                item {
                                    Text("VEHICULAR BRIDGE SUPERSTRUCTURE (12 ARCHITECTURAL TYPES)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val bridgeOptions = listOf(
                                            "CABLE_STAYED" to "Cable-Stayed",
                                            "SUSPENSION" to "Suspension",
                                            "CONCRETE_VIADUCT" to "Box Girder",
                                            "STEEL_TRUSS" to "Steel Truss",
                                            "PARABOLIC_ARCH" to "Arch Rib",
                                            "EXTRADOSED" to "Extradosed",
                                            "BASCULE_DRAWBRIDGE" to "Bascule Drawbridge",
                                            "CANTILEVER_TRUSS" to "Cantilever Truss",
                                            "CURVED_CABLE_STAYED" to "Curved Cable-Stayed",
                                            "ROMAN_VIADUCT" to "Masonry Viaduct",
                                            "POST_TENSIONED_OVERPASS" to "Flyover Overpass",
                                            "STEEL_PLATE_GIRDER" to "Plate Girder"
                                        )
                                        items(bridgeOptions) { (typeKey, typeLabel) ->
                                            val isSel = bridgeType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = {
                                                    bridgeType = typeKey
                                                    if (typeKey == "CURVED_CABLE_STAYED" && bridgeCurveBendDeg == 0f) {
                                                        bridgeCurveBendDeg = 26f
                                                    }
                                                },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                    selectedLabelColor = CyberCyan
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Main Span Length", value = bridgeSpanLength, range = 60f..350f, unit = "m", onValueChange = { bridgeSpanLength = it })
                                }
                                item {
                                    ParamSlider(label = "Deck Width (Lanes + Shoulders)", value = bridgeDeckWidth, range = 12f..36f, unit = "m", onValueChange = { bridgeDeckWidth = it })
                                }
                                item {
                                    ParamSlider(label = "Pylon / Tower Height", value = bridgePylonHeight, range = 25f..95f, unit = "m", onValueChange = { bridgePylonHeight = it })
                                }
                                item {
                                    ParamSlider(label = "Horizontal Bending Curvature", value = bridgeCurveBendDeg, range = -45f..45f, unit = "°", onValueChange = { bridgeCurveBendDeg = it })
                                }
                                item {
                                    ParamSlider(label = "Superelevation Banking Angle", value = bridgeSuperelevationDeg, range = 0f..8f, unit = "°", onValueChange = { bridgeSuperelevationDeg = it })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Precast Concrete Jersey Barriers", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = bridgeHasBarriers, onCheckedChange = { bridgeHasBarriers = it })
                                    }
                                }
                            }
                            "FOOTBRIDGE" -> {
                                item {
                                    Text("PEDESTRIAN FOOTBRIDGE ARCHITECTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val footbridgeOptions = listOf(
                                            "PEDESTRIAN_FOOTBRIDGE" to "Arch Footbridge",
                                            "PEDESTRIAN_STRESS_RIBBON" to "Stress-Ribbon",
                                            "PEDESTRIAN_CABLE_STAYED" to "Cable Promenade",
                                            "PEDESTRIAN_TIMBER_ARCH" to "Timber Arch",
                                            "PEDESTRIAN_COVERED_SKYWALK" to "Covered Skywalk"
                                        )
                                        items(footbridgeOptions) { (typeKey, typeLabel) ->
                                            val isSel = footbridgeType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { footbridgeType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                    selectedLabelColor = CyberCyan
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Footbridge Clear Span", value = footbridgeSpanLength, range = 35f..160f, unit = "m", onValueChange = { footbridgeSpanLength = it })
                                }
                                item {
                                    ParamSlider(label = "Walkway Clear Width", value = footbridgeDeckWidth, range = 3.2f..8.0f, unit = "m", onValueChange = { footbridgeDeckWidth = it })
                                }
                                item {
                                    ParamSlider(label = "Mast / Arch Rise Height", value = footbridgePylonHeight, range = 12f..55f, unit = "m", onValueChange = { footbridgePylonHeight = it })
                                }
                                item {
                                    ParamSlider(label = "Plan Sweep Curvature", value = footbridgeCurveDeg, range = -35f..35f, unit = "°", onValueChange = { footbridgeCurveDeg = it })
                                }
                            }
                            "EXPRESSWAY" -> {
                                item {
                                    Text("EXPRESSWAY & CONTROLLED HIGHWAY TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val expOptions = listOf(
                                            "SIX_LANE_FREEWAY" to "Divided Freeway",
                                            "ELEVATED_VIADUCT" to "Elevated Viaduct",
                                            "GORE_MERGE_TAPER" to "Gore Merge Taper",
                                            "SMART_EV_CORRIDOR" to "Smart EV Corridor",
                                            "MOUNTAIN_SWITCHBACK_CUT" to "Mountain Cut"
                                        )
                                        items(expOptions) { (typeKey, typeLabel) ->
                                            val isSel = expresswayType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { expresswayType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = NeonLime.copy(alpha = 0.25f),
                                                    selectedLabelColor = NeonLime
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Lanes Per Direction", value = expLanesPerDir.toFloat(), range = 2f..4f, unit = " lanes", onValueChange = { expLanesPerDir = it.toInt() })
                                }
                                item {
                                    ParamSlider(label = "Expressway Section Length", value = expSectionLength, range = 50f..220f, unit = "m", onValueChange = { expSectionLength = it })
                                }
                                item {
                                    ParamSlider(label = "Horizontal Sweep Angle", value = expCurveBendDeg, range = -45f..45f, unit = "°", onValueChange = { expCurveBendDeg = it })
                                }
                                item {
                                    ParamSlider(label = "Superelevation Banking", value = expBankingDeg, range = 0f..8f, unit = "°", onValueChange = { expBankingDeg = it })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Cobra-Head LED Luminaire Poles", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = expHasLighting, onCheckedChange = { expHasLighting = it })
                                    }
                                }
                            }
                            "SIGNPOST" -> {
                                item {
                                    Text("SIGNPOSTS & OVERHEAD GANTRY ARCHITECTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val signOptions = listOf(
                                            "CANTILEVER_HIGHWAY" to "Cantilever Sign",
                                            "FULL_SPAN_GANTRY" to "Warren Gantry",
                                            "VARIABLE_MESSAGE_DMS" to "LED VMS Matrix",
                                            "REGULATORY_CHEVRON_SET" to "Chevron Series",
                                            "BUTTERFLY_SPLIT_GORE" to "Butterfly Gore"
                                        )
                                        items(signOptions) { (typeKey, typeLabel) ->
                                            val isSel = signpostType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { signpostType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                    selectedLabelColor = CyberCyan
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Gantry / Mast Clear Span", value = signSpanM, range = 8f..32f, unit = "m", onValueChange = { signSpanM = it })
                                }
                                item {
                                    ParamSlider(label = "Vertical Road Clearance", value = signClearanceM, range = 4.5f..8.5f, unit = "m", onValueChange = { signClearanceM = it })
                                }
                                item {
                                    ParamSlider(label = "Sign Face Board Width", value = signBoardWidthM, range = 3.0f..11.0f, unit = "m", onValueChange = { signBoardWidthM = it })
                                }
                                item {
                                    ParamSlider(label = "Panels / Chevrons Count", value = signCount.toFloat(), range = 1f..6f, unit = " units", onValueChange = { signCount = it.toInt() })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Luminaire & Emissive LED Matrix", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = signHasDMS, onCheckedChange = { signHasDMS = it })
                                    }
                                }
                            }
                            "BARRIER" -> {
                                item {
                                    Text("SAFETY BARRIER & GUARDRAIL SYSTEMS (MASH 2016)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val barrierOptions = listOf(
                                            "NEW_JERSEY_FSHAPE" to "Jersey F-Shape",
                                            "W_BEAM_GUARDRAIL" to "W-Beam Guardrail",
                                            "HIGH_TENSION_CABLE" to "Cable Barrier",
                                            "ACOUSTIC_NOISE_WALL" to "Acoustic Wall",
                                            "ROLLER_CRASH_CUSHION" to "EVA Roller Rail"
                                        )
                                        items(barrierOptions) { (typeKey, typeLabel) ->
                                            val isSel = barrierType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = {
                                                    barrierType = typeKey
                                                    if (typeKey == "ACOUSTIC_NOISE_WALL" && barrierHeightM < 2.2f) {
                                                        barrierHeightM = 3.6f
                                                    } else if (typeKey != "ACOUSTIC_NOISE_WALL" && barrierHeightM > 1.8f) {
                                                        barrierHeightM = 1.10f
                                                    }
                                                },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LaserOrange.copy(alpha = 0.25f),
                                                    selectedLabelColor = LaserOrange
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Barrier Run Length", value = barrierLengthM, range = 20f..100f, unit = "m", onValueChange = { barrierLengthM = it })
                                }
                                item {
                                    ParamSlider(label = "Barrier Wall / Rail Height", value = barrierHeightM, range = 0.75f..4.5f, unit = "m", onValueChange = { barrierHeightM = it })
                                }
                                item {
                                    ParamSlider(label = "Shoulder Sweep Curvature", value = barrierCurveDeg, range = -40f..40f, unit = "°", onValueChange = { barrierCurveDeg = it })
                                }
                                item {
                                    ParamSlider(label = "Driven Post Spacing", value = barrierPostSpacingM, range = 1.2f..4.0f, unit = "m", onValueChange = { barrierPostSpacingM = it })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Retroreflective Delineator Tabs", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = barrierHasReflectors, onCheckedChange = { barrierHasReflectors = it })
                                    }
                                }
                            }
                            "ROAD_BLOCK" -> {
                                item {
                                    Text("ROAD BLOCKS & WORKZONE SECURITY SYSTEMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val rbOptions = listOf(
                                            "WATER_JERSEY_LINE" to "Water Barricades",
                                            "HYDRAULIC_BOLLARDS" to "K12 Bollards",
                                            "TACTICAL_SPIKE_CHICANE" to "Tactical Chicane",
                                            "CONSTRUCTION_CLOSURE" to "Workzone Taper",
                                            "DRAGON_TEETH_CHICANE" to "Dragon Teeth"
                                        )
                                        items(rbOptions) { (typeKey, typeLabel) ->
                                            val isSel = roadBlockType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { roadBlockType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LaserOrange.copy(alpha = 0.25f),
                                                    selectedLabelColor = LaserOrange
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Checkpoint Width", value = rbRoadWidthM, range = 6f..22f, unit = "m", onValueChange = { rbRoadWidthM = it })
                                }
                                item {
                                    ParamSlider(label = "Control Zone Length", value = rbZoneLengthM, range = 18f..65f, unit = "m", onValueChange = { rbZoneLengthM = it })
                                }
                                item {
                                    ParamSlider(label = "Barricade / Bollard Modules", value = rbUnitCount.toFloat(), range = 3f..12f, unit = " units", onValueChange = { rbUnitCount = it.toInt() })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Amber Warning Beacons & Arrow Matrix", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = rbHasBeacons, onCheckedChange = { rbHasBeacons = it })
                                    }
                                }
                            }
                            "TOLL_PLAZA" -> {
                                item {
                                    Text("TOLL PLAZA CONFIGURATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val tollOptions = listOf(
                                            "CANOPY_PLAZA" to "Canopy Plaza",
                                            "ETC_GANTRY" to "ETC Gantry",
                                            "CHECKPOINT" to "Border Checkpoint",
                                            "RAMP_GATE" to "Ramp Gate",
                                            "SMART_SOLAR_CANOPY" to "Solar Canopy",
                                            "WEIGH_IN_MOTION" to "Weigh-in-Motion"
                                        )
                                        items(tollOptions) { (typeKey, typeLabel) ->
                                            val isSel = tollPlazaType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { tollPlazaType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LaserOrange.copy(alpha = 0.25f),
                                                    selectedLabelColor = LaserOrange
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Number of Active Toll Lanes", value = tollLaneCount.toFloat(), range = 2f..10f, unit = " lanes", onValueChange = { tollLaneCount = it.toInt() })
                                }
                                item {
                                    ParamSlider(label = "Lane Width", value = tollLaneWidth, range = 3.2f..4.5f, unit = "m", onValueChange = { tollLaneWidth = it })
                                }
                                item {
                                    ParamSlider(label = "Canopy Freight Height Clearance", value = tollCanopyClearance, range = 4.5f..7.0f, unit = "m", onValueChange = { tollCanopyClearance = it })
                                }
                                item {
                                    ParamSlider(label = "Plaza Concrete Apron Length", value = tollPlazaLength, range = 35f..90f, unit = "m", onValueChange = { tollPlazaLength = it })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Dynamic Overhead LED VMS Displays", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = tollHasVMS, onCheckedChange = { tollHasVMS = it })
                                    }
                                }
                            }
                            "TUNNEL" -> {
                                item {
                                    Text("TUNNEL BORE ARCHITECTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val tunnelOptions = listOf(
                                            "MOUNTAIN_HORSESHOE" to "Horseshoe NATM",
                                            "CIRCULAR_BORED" to "Circular TBM",
                                            "CUT_AND_COVER" to "Box Cut & Cover",
                                            "PORTAL_TRANSITION" to "Flared Portal",
                                            "SUBSEA_IMMERSED_TUBE" to "Immersed Tube",
                                            "BI_LEVEL_CIRCULAR" to "Bi-Level Circular"
                                        )
                                        items(tunnelOptions) { (typeKey, typeLabel) ->
                                            val isSel = tunnelType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { tunnelType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                                                    selectedLabelColor = ElectricViolet
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Internal Bore Radius", value = tunnelInternalRadius, range = 4.5f..8.0f, unit = "m", onValueChange = { tunnelInternalRadius = it })
                                }
                                item {
                                    ParamSlider(label = "Tunnel Bore Length", value = tunnelLength, range = 50f..250f, unit = "m", onValueChange = { tunnelLength = it })
                                }
                                item {
                                    ParamSlider(label = "Subterranean Bending Curve Angle", value = tunnelCurveBendDeg, range = -45f..45f, unit = "°", onValueChange = { tunnelCurveBendDeg = it })
                                }
                                item {
                                    ParamSlider(label = "Jet Fan Longitudinal Spacing", value = tunnelJetFanSpacing, range = 20f..55f, unit = "m", onValueChange = { tunnelJetFanSpacing = it })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Acoustic Flared Entrance Portal Hood", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = tunnelHasPortal, onCheckedChange = { tunnelHasPortal = it })
                                    }
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Ceiling Jet Ventilation Fans (NFPA 502)", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = tunnelHasJetFans, onCheckedChange = { tunnelHasJetFans = it })
                                    }
                                }
                            }
                            "INTERCHANGE" -> {
                                item {
                                    Text("HIGHWAY INTERCHANGE & RAMPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val interchangeOptions = listOf(
                                            "CLOVERLEAF_LOOP" to "270° Cloverleaf",
                                            "DIRECTIONAL_FLYOVER" to "Flyover Ramp",
                                            "DIAMOND_SLIP_RAMP" to "Diamond Ramp",
                                            "ROUNDABOUT" to "Roundabout",
                                            "SPUI_SINGLE_POINT" to "SPUI Urban",
                                            "DIVERGING_DIAMOND" to "Diverging Diamond"
                                        )
                                        items(interchangeOptions) { (typeKey, typeLabel) ->
                                            val isSel = interchangeType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { interchangeType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = NeonLime.copy(alpha = 0.25f),
                                                    selectedLabelColor = NeonLime
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Curved Loop Radius", value = interchangeLoopRadius, range = 35f..85f, unit = "m", onValueChange = { interchangeLoopRadius = it })
                                }
                                item {
                                    ParamSlider(label = "Total Arc Turn Angle", value = interchangeArcDeg, range = 90f..360f, unit = "°", onValueChange = { interchangeArcDeg = it })
                                }
                                item {
                                    ParamSlider(label = "Grade Separation Elevation Delta", value = interchangeElevationDelta, range = 4.0f..14.0f, unit = "m", onValueChange = { interchangeElevationDelta = it })
                                }
                                item {
                                    ParamSlider(label = "Superelevation Banking Angle", value = interchangeBankingDeg, range = 2.0f..9.0f, unit = "°", onValueChange = { interchangeBankingDeg = it })
                                }
                            }
                            "RETAINING_WALL" -> {
                                item {
                                    Text("EARTH RETENTION ARCHITECTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val wallOptions = listOf(
                                            "MSE_MODULAR" to "MSE Panels",
                                            "STEPPED_GABION" to "Rock Gabion",
                                            "CANTILEVER_RC" to "RC Cantilever",
                                            "SOIL_NAILED" to "Soil Nailed",
                                            "CRIB_WALL_CELLULAR" to "Cellular Crib",
                                            "CONTIGUOUS_BORED_PILE" to "Bored Pile Wall"
                                        )
                                        items(wallOptions) { (typeKey, typeLabel) ->
                                            val isSel = wallType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { wallType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                    selectedLabelColor = CyberCyan
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Retained Wall Height", value = wallHeight, range = 3.0f..14.0f, unit = "m", onValueChange = { wallHeight = it })
                                }
                                item {
                                    ParamSlider(label = "Longitudinal Wall Length", value = wallLength, range = 25f..120f, unit = "m", onValueChange = { wallLength = it })
                                }
                                item {
                                    ParamSlider(label = "Inward Batter Lean Angle", value = wallBatterDeg, range = 0f..10f, unit = "°", onValueChange = { wallBatterDeg = it })
                                }
                            }
                            "ROAD_NETWORK" -> {
                                item {
                                    Text("MODULAR ROAD NETWORK & INTERSECTION TOPOLOGY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val networkOptions = listOf(
                                            "SIGNALIZED_4WAY" to "4-Way Signalized",
                                            "T_JUNCTION_ARTERIAL" to "Arterial T-Junction",
                                            "Y_FORK_BIFURCATION" to "Y-Fork Gore",
                                            "DISTRICT_STREET_GRID" to "2×2 District Grid"
                                        )
                                        items(networkOptions) { (typeKey, typeLabel) ->
                                            val isSel = roadNetworkType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { roadNetworkType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                    selectedLabelColor = CyberCyan
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Carriageway Road Width", value = rnRoadWidthM, range = 8.0f..18.0f, unit = "m", onValueChange = { rnRoadWidthM = it })
                                }
                                item {
                                    ParamSlider(label = "Network Arm Span", value = rnArmSpanM, range = 36.0f..100.0f, unit = "m", onValueChange = { rnArmSpanM = it })
                                }
                                item {
                                    ParamSlider(label = "Corner Fillet Curb Radius", value = rnCornerRadiusM, range = 4.0f..16.0f, unit = "m", onValueChange = { rnCornerRadiusM = it })
                                }
                                item {
                                    ParamSlider(label = "Total Lanes Across Deck", value = rnLaneCount.toFloat(), range = 2f..4f, unit = " lanes", onValueChange = { rnLaneCount = it.toInt() })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Cantilever Traffic Signal Masts (R/Y/G LED)", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = rnHasSignalMasts, onCheckedChange = { rnHasSignalMasts = it })
                                    }
                                }
                            }
                            "SPLINE_GRAPH" -> {
                                item {
                                    Text("CLOTHOID SPLINE & SUPERELEVATION BANKING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val splineOptions = listOf(
                                            "CLOTHOID_S_CURVE" to "Clothoid S-Curve",
                                            "HAIRPIN_SWITCHBACK" to "180° Hairpin",
                                            "CATMULL_ROM_CORRIDOR" to "Catmull-Rom",
                                            "BANKED_SPIRAL_HELIX" to "Banked Helix"
                                        )
                                        items(splineOptions) { (typeKey, typeLabel) ->
                                            val isSel = splineType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { splineType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                                                    selectedLabelColor = ElectricViolet
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Corridor Alignment Length", value = spLengthM, range = 60f..180f, unit = "m", onValueChange = { spLengthM = it })
                                }
                                item {
                                    ParamSlider(label = "Carriageway Surface Width", value = spWidthM, range = 7.0f..16.0f, unit = "m", onValueChange = { spWidthM = it })
                                }
                                item {
                                    ParamSlider(label = "AASHTO Design Speed", value = spDesignSpeedKmh, range = 35f..130f, unit = " km/h", onValueChange = { spDesignSpeedKmh = it })
                                }
                                item {
                                    ParamSlider(label = "Vertical Elevation Climb", value = spElevationGainM, range = 0f..22f, unit = "m", onValueChange = { spElevationGainM = it })
                                }
                                item {
                                    ParamSlider(label = "Curvature Lateral Intensity", value = spCurvatureIntensity, range = 0.3f..1.8f, unit = "x", onValueChange = { spCurvatureIntensity = it })
                                }
                            }
                            "ROAD_TILE" -> {
                                item {
                                    Text("TERRAIN-CONFORMING TILE & SURFACE STATES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        val tileOptions = listOf(
                                            "CUT_FILL_HILLSIDE" to "Cut/Fill Hillside",
                                            "QUADTREE_URBAN_TILE" to "Quadtree Urban",
                                            "WET_PUDDLE_STATE" to "Wet Puddles",
                                            "MUDDY_GRAVEL_RUT" to "Muddy Gravel",
                                            "POTHOLED_CRACKED_TILE" to "Potholed/Cracked",
                                            "SNOWY_ICY_CORRIDOR" to "Snow/Black-Ice"
                                        )
                                        items(tileOptions) { (typeKey, typeLabel) ->
                                            val isSel = roadTileType == typeKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { roadTileType = typeKey },
                                                label = { Text(typeLabel, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LaserOrange.copy(alpha = 0.25f),
                                                    selectedLabelColor = LaserOrange
                                                )
                                            )
                                        }
                                    }
                                }
                                item {
                                    ParamSlider(label = "Tile Footprint Span", value = rtTileSizeM, range = 24f..72f, unit = "m", onValueChange = { rtTileSizeM = it })
                                }
                                item {
                                    ParamSlider(label = "Roadway Bench Width", value = rtRoadWidthM, range = 8.0f..16.0f, unit = "m", onValueChange = { rtRoadWidthM = it })
                                }
                                item {
                                    ParamSlider(label = "Cut/Fill Bench Relief Height", value = rtCutFillHeightM, range = 0.0f..10.0f, unit = "m", onValueChange = { rtCutFillHeightM = it })
                                }
                                item {
                                    ParamSlider(label = "Distress / Rut / Pothole Depth", value = rtDistressDepthM, range = 0.0f..0.15f, unit = "m", onValueChange = { rtDistressDepthM = it })
                                }
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Concrete V-Ditch, Kerbs & Storm Grates", fontSize = 11.sp, color = TextPrimary)
                                        Switch(checked = rtHasDrainageAndCurbs, onCheckedChange = { rtHasDrainageAndCurbs = it })
                                    }
                                }
                            }
                            else -> {
                                // CONNECTED_SCENE
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CyberCyan.copy(alpha = 0.1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("COMPLETE VULKAN HIGHWAY CORRIDOR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CyberCyan)
                                            Text(
                                                text = "Continuous 480m alignment seamlessly integrating: 4-lane approach with MSE retaining walls (Station -120m) -> 6-lane Canopy Toll Plaza (Station -40m) -> 160m Cable-Stayed Bridge across river chasm (Station +35m) -> Mountain Tunnel Portal & Subterranean Horseshoe Bore (Station +145m).",
                                                fontSize = 10.sp,
                                                color = TextPrimary,
                                                lineHeight = 15.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { selectedCategory = "BRIDGE" },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Isolate Bridge", fontSize = 11.sp, color = CyberCyan)
                                        }
                                        Button(
                                            onClick = { selectedCategory = "TOLL_PLAZA" },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, LaserOrange.copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Isolate Toll", fontSize = 11.sp, color = LaserOrange)
                                        }
                                        Button(
                                            onClick = { selectedCategory = "TUNNEL" },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Isolate Tunnel", fontSize = 11.sp, color = ElectricViolet)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "CODE" -> {
                    val filteredFiles = remember(codeFilter) {
                        when (codeFilter) {
                            "CPP" -> NativeSourceCatalog.roadFiles.filter { it.name.endsWith(".h") || it.name.endsWith(".cpp") }
                            "GLSL" -> NativeSourceCatalog.roadFiles.filter { it.name.endsWith(".glsl") || it.name.endsWith(".vert") || it.name.endsWith(".frag") }
                            "COMP" -> NativeSourceCatalog.roadFiles.filter { it.name.endsWith(".comp") }
                            else -> NativeSourceCatalog.roadFiles
                        }
                    }
                    LaunchedEffect(filteredFiles) {
                        if (filteredFiles.isNotEmpty() && filteredFiles.none { it.path == selectedFile.path }) {
                            selectedFile = filteredFiles.first()
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        // Source type filter row (ALL, C++, GLSL, .comp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val filterTabs = listOf("ALL" to "ALL (${NativeSourceCatalog.roadFiles.size})", "CPP" to "C++ (.h)", "GLSL" to "GLSL (.vert/.frag)", "COMP" to "Compute (.comp)")
                            items(filterTabs) { (fKey, fLabel) ->
                                val isFilterSel = codeFilter == fKey
                                FilterChip(
                                    selected = isFilterSel,
                                    onClick = { codeFilter = fKey },
                                    label = { Text(fLabel, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                        selectedLabelColor = CyberCyan
                                    ),
                                    modifier = Modifier.height(28.dp).testTag("code_filter_$fKey")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // File Selector
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredFiles) { file ->
                                val isSel = file.path == selectedFile.path
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) CyberCyan.copy(alpha = 0.25f) else Color(0xFF0F141C),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyberCyan else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.clickable { selectedFile = file }
                                ) {
                                    Text(
                                        text = file.name,
                                        fontSize = 10.sp,
                                        color = if (isSel) CyberCyan else TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Code Box with vertical + horizontal scroll
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF070B12),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(selectedFile.path, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                                        Text(selectedFile.description, fontSize = 9.sp, color = TextSecondary, maxLines = 2)
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(selectedFile.codeSnippet))
                                            copyNotice = "Copied to clipboard!"
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (copyNotice.isNotEmpty()) {
                                    Text(copyNotice, fontSize = 9.sp, color = NeonLime, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val vScroll = rememberScrollState()
                                val hScroll = rememberScrollState()
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF05080E))
                                        .verticalScroll(vScroll)
                                        .horizontalScroll(hScroll)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = selectedFile.codeSnippet,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonLime,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
                "CROSS_SECTION" -> {
                    // Civil Engineering Cross Section & Telemetry
                    val isPed = selectedCategory == "FOOTBRIDGE"
                    val isNetwork = selectedCategory == "ROAD_NETWORK"
                    val isSpline = selectedCategory == "SPLINE_GRAPH"
                    val isTile = selectedCategory == "ROAD_TILE"
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text(
                                text = when {
                                    isPed -> "AASHTO PEDESTRIAN FOOTBRIDGE DECK SECTION"
                                    isNetwork -> "MUTCD INTERSECTION CHANNELIZATION & SIGNAL LAYOUT"
                                    isSpline -> "CLOTHOID SPIRAL SUPERELEVATION CROSS SECTION"
                                    isTile -> "CUT/FILL HILLSIDE BENCH & SURFACE STATE PROFILE"
                                    else -> "AASHTO / EUROCODE HIGHWAY LAYER STRUCTURE"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF070B12),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().height(140.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                    val cw = size.width
                                    val ch = size.height

                                    if (isPed) {
                                        drawRect(Color(0xFF90A4AE), Offset(cw * 0.14f, ch * 0.08f), androidx.compose.ui.geometry.Size(10f, ch * 0.48f))
                                        drawRect(Color(0xFF90A4AE), Offset(cw * 0.86f - 10f, ch * 0.08f), androidx.compose.ui.geometry.Size(10f, ch * 0.48f))
                                        drawRect(Color(0xFF26C6DA).copy(alpha = 0.45f), Offset(cw * 0.15f, ch * 0.16f), androidx.compose.ui.geometry.Size(4f, ch * 0.38f))
                                        drawRect(Color(0xFF26C6DA).copy(alpha = 0.45f), Offset(cw * 0.85f - 4f, ch * 0.16f), androidx.compose.ui.geometry.Size(4f, ch * 0.38f))
                                        drawRect(Color(0xFF8D6E63), Offset(cw * 0.14f, ch * 0.56f), androidx.compose.ui.geometry.Size(cw * 0.72f, ch * 0.12f))
                                        drawRect(Color(0xFF607D8B), Offset(cw * 0.12f, ch * 0.68f), androidx.compose.ui.geometry.Size(cw * 0.76f, ch * 0.16f))
                                        drawRect(Color(0xFF455A64), Offset(cw * 0.38f, ch * 0.84f), androidx.compose.ui.geometry.Size(cw * 0.24f, ch * 0.14f))
                                    } else if (isSpline) {
                                        // Superelevated tilted roadway prism & outer guardrail
                                        val bankTilt = (spCurvatureIntensity / 1.8f) * ch * 0.22f
                                        val p = Path().apply {
                                            moveTo(cw * 0.14f, ch * 0.46f + bankTilt)
                                            lineTo(cw * 0.86f, ch * 0.46f - bankTilt)
                                            lineTo(cw * 0.88f, ch * 0.70f - bankTilt)
                                            lineTo(cw * 0.12f, ch * 0.70f + bankTilt)
                                            close()
                                        }
                                        drawPath(p, Color(0xFF37474F))
                                        drawLine(Color(0xFFFFD54F), Offset(cw * 0.49f, ch * 0.45f), Offset(cw * 0.51f, ch * 0.45f), strokeWidth = 5f)
                                        drawLine(Color(0xFF00E5FF), Offset(cw * 0.14f, ch * 0.46f + bankTilt), Offset(cw * 0.86f, ch * 0.46f - bankTilt), strokeWidth = 3f)
                                    } else {
                                        drawRect(Color(0xFF263238), Offset(cw * 0.1f, ch * 0.15f), androidx.compose.ui.geometry.Size(cw * 0.8f, ch * 0.16f))
                                        drawRect(Color(0xFF37474F), Offset(cw * 0.08f, ch * 0.31f), androidx.compose.ui.geometry.Size(cw * 0.84f, ch * 0.20f))
                                        drawRect(Color(0xFF607D8B), Offset(cw * 0.05f, ch * 0.51f), androidx.compose.ui.geometry.Size(cw * 0.90f, ch * 0.24f))
                                        drawRect(Color(0xFF795548), Offset(0f, ch * 0.75f), androidx.compose.ui.geometry.Size(cw, ch * 0.25f))
                                        drawRect(Color(0xFFB0BEC5), Offset(cw * 0.08f - 14f, ch * 0.05f), androidx.compose.ui.geometry.Size(14f, ch * 0.26f))
                                        drawRect(Color(0xFFB0BEC5), Offset(cw * 0.92f, ch * 0.05f), androidx.compose.ui.geometry.Size(14f, ch * 0.26f))
                                        drawLine(Color(0xFFFFD54F), Offset(cw * 0.5f, ch * 0.15f), Offset(cw * 0.5f, ch * 0.31f), strokeWidth = 3f)
                                    }
                                }
                            }
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    when {
                                        isPed -> {
                                            Text("PEDESTRIAN FOOTBRIDGE SPECS & SURFACE ACCESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                                            MetricRow("Design Live Load", "4.3 kPa (90 psf AASHTO Pedestrian)")
                                            MetricRow("Walkway Clear Width", "${(footbridgeDeckWidth * 10).roundToInt() / 10.0} m (Human-Scale Promenade)")
                                            MetricRow("Overpass Deck Elevation", "7.2 m (Min 5.8 m Underpass Clearance)")
                                            MetricRow("Balustrade Handrail Height", "1.15 m (Stainless Cable & Glass Infill)")
                                            MetricRow("Surface Stair Connections", "2-Flight Stepped Stairs + Mid Landing")
                                            MetricRow("Accessible Surface Ramp", "Flanking Switchback ADA Ramp -> Y=0")
                                        }
                                        isNetwork -> {
                                            Text("SIGNALIZED ROAD NETWORK & JUNCTION SPECS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                            MetricRow("Junction Topology", roadNetworkType)
                                            MetricRow("Carriageway Width", "${(rnRoadWidthM * 10).roundToInt() / 10.0} m (${rnLaneCount} lanes)")
                                            MetricRow("Corner Curb Radius", "${(rnCornerRadiusM * 10).roundToInt() / 10.0} m (WB-20 Truck Turning Envelope)")
                                            MetricRow("Network Arm Span", "${rnArmSpanM.roundToInt()} m")
                                            MetricRow("Traffic Signal Masts", if (rnHasSignalMasts) "MUTCD R/Y/G Cantilever + Luminaire" else "Unsignalized")
                                        }
                                        isSpline -> {
                                            val vMs = spDesignSpeedKmh / 3.6f
                                            val calcBankDeg = (atan((vMs * vMs) / (9.81f * (28f / spCurvatureIntensity.coerceAtLeast(0.3f)))) * 180f / PI.toFloat()).coerceAtMost(8.5f)
                                            Text("CLOTHOID SPLINE & BANKING PHYSICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
                                            MetricRow("Spline Formulation", splineType)
                                            MetricRow("Design Speed (V_d)", "${spDesignSpeedKmh.roundToInt()} km/h (${(vMs * 10).roundToInt() / 10.0} m/s)")
                                            MetricRow("Applied Banking (θ)", "${(calcBankDeg * 10).roundToInt() / 10.0}° (Climb ${spElevationGainM}m)")
                                            MetricRow("Corridor Length", "${spLengthM.roundToInt()} m / Width ${spWidthM} m")
                                            MetricRow("Compute Kernel", "road_spline.comp (VK_BUFFER_USAGE_STORAGE)")
                                        }
                                        isTile -> {
                                            val mu = when (roadTileType) {
                                                "WET_PUDDLE_STATE" -> 0.62f
                                                "MUDDY_GRAVEL_RUT" -> 0.34f
                                                "POTHOLED_CRACKED_TILE" -> 0.55f
                                                "SNOWY_ICY_CORRIDOR" -> 0.18f
                                                else -> 0.82f
                                            }
                                            Text("TERRAIN TILE & SURFACE STATE PHYSICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LaserOrange)
                                            MetricRow("Tile State Mode", roadTileType)
                                            MetricRow("Tire Friction Coefficient (μ)", "%.2f (Rolling Res: %.3f)".format(mu, if (roadTileType == "MUDDY_GRAVEL_RUT") 0.048f else 0.015f))
                                            MetricRow("Cut/Fill Bench Height", "${(rtCutFillHeightM * 10).roundToInt() / 10.0} m (Drainage: ${if (rtHasDrainageAndCurbs) "Active" else "Off"})")
                                            MetricRow("Distress Depth", "${(rtDistressDepthM * 100).roundToInt()} cm")
                                            MetricRow("Compute Texture Shader", "road_surface_state_tex.comp (RGBA8 Albedo/Normal)")
                                        }
                                        else -> {
                                            Text("CIVIL ENGINEERING SPECS & METRICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonLime)
                                            MetricRow("Design Speed Limit", "120 km/h (Expressway Tier 1)")
                                            MetricRow("Max Superelevation (e_max)", "${bridgeSuperelevationDeg}° (AASHTO Table 3-7)")
                                            MetricRow("Freight Height Clearance", "${tollCanopyClearance}m (Standard 5.2m + 0.3m buffer)")
                                            MetricRow("Tunnel Fire Safety Standard", "NFPA 502 / PIARC Category A")
                                            MetricRow("Longitudinal Ventilation", "${(tunnelLength / tunnelJetFanSpacing).toInt()} Jet Fans (reversible 35 m³/s)")
                                            MetricRow("Seismic Design Category", "SDC D (Soil Profile Type C)")
                                            MetricRow("Earth Retention", "MSE Precast Panels with Geogrid Straps")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = TextPrimary)
            Text(
                text = "${(value * 10).roundToInt() / 10.0}$unit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
