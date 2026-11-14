// ─── app/src/main/java/com/example/engine/NativeEngineBridge.kt ───────────────
// Native JNI Bridge linking Android UI Shell directly to C/C++ Vulkan Procedural Engine (libapex_vulkan.so)
// All 3D algorithms, physics, terrain simulations, and mesh generations run exclusively in C/C++
package com.example.engine

import android.util.Log
import com.example.model.*
import kotlin.math.*

object NativeEngineBridge {
    private const val TAG = "NativeEngineBridge"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("apex_vulkan")
            isNativeLoaded = true
            try {
                Log.i(TAG, "Successfully loaded native C++ engine library: libapex_vulkan.so")
            } catch (_: Throwable) {
                // Ignore Log failure in headless JVM unit test runner
            }
        } catch (_: Throwable) {
            isNativeLoaded = false
        }
    }

    fun isLoaded(): Boolean = isNativeLoaded

    // ─── C++ NATIVE JNI DECLARATIONS (libapex_vulkan.so) ─────────────────────────

    external fun nativeGetElevation(wx: Float, wy: Float, interval: Float, terracing: Float): Float
    external fun nativeCalculateHumanMetrics(walkDistanceM: Float, eyeHeightOffsetM: Float): FloatArray

    external fun nativeGenerateHumanStartVertices(
        walkDistanceM: Float, gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): FloatArray

    external fun nativeGenerateHumanStartIndices(
        walkDistanceM: Float, gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): IntArray

    external fun nativeGenerateRoadSegmentVertices(
        segmentIndex: Int, gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): FloatArray

    external fun nativeGenerateRoadSegmentIndices(
        segmentIndex: Int, gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): IntArray

    external fun nativeGenerateRoadSubSegmentVertices(
        segmentIndex: Int, subSegmentIndex: Int, gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): FloatArray

    external fun nativeGenerateRoadSubSegmentIndices(
        segmentIndex: Int, subSegmentIndex: Int, gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): IntArray

    external fun nativeGenerate1mResolutionVertices(
        segmentIndex: Int, centerForwardM: Float, contourInterval: Float, stepTerracing: Float
    ): FloatArray

    external fun nativeGenerate1mResolutionIndices(
        segmentIndex: Int, centerForwardM: Float, contourInterval: Float, stepTerracing: Float
    ): IntArray

    external fun nativeGenerateWholeCombinedTerrainVertices(
        gridResX: Int, gridResZ: Int, contourInterval: Float, stepTerracing: Float
    ): FloatArray

    external fun nativeGenerateWholeCombinedTerrainIndices(
        gridResX: Int, gridResZ: Int, contourInterval: Float, stepTerracing: Float
    ): IntArray

    external fun nativeGetCombinedRoadCenterline(numPoints: Int): FloatArray
    external fun nativeGetRoadSegmentInfo(segmentIndex: Int): FloatArray
    external fun nativeGetGroundPieceMetrics(segmentIndex: Int, groundDatumM: Float): FloatArray
    external fun nativeGetRoadCenterline(segmentIndex: Int, numPoints: Int): FloatArray
    external fun nativeGetRoadPointAt(segmentIndex: Int, forwardM: Float): FloatArray

    external fun nativeGenerateFirst10kmSegmentVertices(
        gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): FloatArray

    external fun nativeGenerateFirst10kmSegmentIndices(
        gridSize: Int, contourInterval: Float, stepTerracing: Float
    ): IntArray

    external fun nativeBuildPowerGridVertices(postCount: Int, spanDistance: Float, cornerAngleDeg: Float): FloatArray
    external fun nativeBuildPowerGridIndices(postCount: Int, spanDistance: Float, cornerAngleDeg: Float): IntArray

    external fun nativeGenerateRoadBridgeVertices(
        type: Int, spanM: Float, deckWidthM: Float, pylonHeightM: Float,
        curveBendDeg: Float, bankingDeg: Float, cablePairs: Int
    ): FloatArray

    external fun nativeGenerateRoadBridgeIndices(
        type: Int, spanM: Float, deckWidthM: Float, pylonHeightM: Float,
        curveBendDeg: Float, bankingDeg: Float, cablePairs: Int
    ): IntArray

    external fun nativeGenerateTollPlazaVertices(
        type: Int, laneCount: Int, laneWidthM: Float, canopyClearanceM: Float,
        plazaLengthM: Float, hasDynamicVMS: Boolean
    ): FloatArray

    external fun nativeGenerateTollPlazaIndices(
        type: Int, laneCount: Int, laneWidthM: Float, canopyClearanceM: Float,
        plazaLengthM: Float, hasDynamicVMS: Boolean
    ): IntArray

    external fun nativeGenerateTunnelVertices(
        type: Int, radiusM: Float, lengthM: Float, curveBendDeg: Float,
        jetFanSpacingM: Float, hasFlaredPortal: Boolean
    ): FloatArray

    external fun nativeGenerateTunnelIndices(
        type: Int, radiusM: Float, lengthM: Float, curveBendDeg: Float,
        jetFanSpacingM: Float, hasFlaredPortal: Boolean
    ): IntArray

    external fun nativeGenerateConnectedHighwayVertices(scale: Float): FloatArray
    external fun nativeGenerateConnectedHighwayIndices(scale: Float): IntArray

    external fun nativeGenerateInterchangeVertices(
        type: Int, loopRadiusM: Float, totalArcAngleDeg: Float,
        rampWidthM: Float, elevationDeltaM: Float, bankingDeg: Float
    ): FloatArray

    external fun nativeGenerateInterchangeIndices(
        type: Int, loopRadiusM: Float, totalArcAngleDeg: Float,
        rampWidthM: Float, elevationDeltaM: Float, bankingDeg: Float
    ): IntArray

    external fun nativeGenerateRetainingWallVertices(
        type: Int, wallHeightM: Float, wallLengthM: Float, batterAngleDeg: Float
    ): FloatArray

    external fun nativeGenerateRetainingWallIndices(
        type: Int, wallHeightM: Float, wallLengthM: Float, batterAngleDeg: Float
    ): IntArray

    external fun nativeGenerateSignpostVertices(
        type: Int, gantrySpanM: Float, clearanceH: Float, boardWidthM: Float,
        signCount: Int, hasDMS: Boolean
    ): FloatArray

    external fun nativeGenerateSignpostIndices(
        type: Int, gantrySpanM: Float, clearanceH: Float, boardWidthM: Float,
        signCount: Int, hasDMS: Boolean
    ): IntArray

    external fun nativeGenerateExpresswayVertices(
        type: Int, lanesPerDir: Int, sectionLengthM: Float, curveBendDeg: Float,
        bankingDeg: Float, hasLighting: Boolean
    ): FloatArray

    external fun nativeGenerateExpresswayIndices(
        type: Int, lanesPerDir: Int, sectionLengthM: Float, curveBendDeg: Float,
        bankingDeg: Float, hasLighting: Boolean
    ): IntArray

    external fun nativeGenerateBarrierVertices(
        type: Int, barrierLengthM: Float, barrierHeightM: Float, curveBendDeg: Float,
        postSpacingM: Float, hasReflectors: Boolean
    ): FloatArray

    external fun nativeGenerateBarrierIndices(
        type: Int, barrierLengthM: Float, barrierHeightM: Float, curveBendDeg: Float,
        postSpacingM: Float, hasReflectors: Boolean
    ): IntArray

    external fun nativeGenerateRoadBlockVertices(
        type: Int, roadWidthM: Float, zoneLengthM: Float, unitCount: Int, hasBeacons: Boolean
    ): FloatArray

    external fun nativeGenerateRoadBlockIndices(
        type: Int, roadWidthM: Float, zoneLengthM: Float, unitCount: Int, hasBeacons: Boolean
    ): IntArray

    // ─── MAGADI TOWN 3 KM x 5 KM (1 M GRID RESOLUTION) JNI ───────────────────────
    external fun nativeGetMagadiTownBounds(): DoubleArray
    external fun nativeMagadiTownLatLonToLocalAndUtm(lat: Double, lon: Double): DoubleArray
    external fun nativeSetMagadiTownDemGrid(cols: Int, rows: Int, elevationsM: FloatArray)
    external fun nativeSetMagadiTownFootprints(roadSegsPacked: FloatArray, buildingRectsPacked: FloatArray)
    external fun nativeSetMagadiTownLakeAndWater(lakeLevelAslM: Float, polyMetaPacked: IntArray, coordsXY: FloatArray)
    external fun nativeSampleMagadiTownRaster(
        minXM: Float, maxXM: Float, minYM: Float, maxYM: Float, cols: Int, rows: Int
    ): FloatArray
    external fun nativeGetMagadiTownTelemetryAt(xM: Float, yM: Float): DoubleArray
    external fun nativeGenerateMagadiTownOverviewVertices(
        resX: Int, resZ: Int, contourInterval: Float, stepTerracing: Float, verticalExaggeration: Float
    ): FloatArray
    external fun nativeGenerateMagadiTownOverviewIndices(
        resX: Int, resZ: Int, contourInterval: Float, stepTerracing: Float, verticalExaggeration: Float
    ): IntArray
    external fun nativeGenerateMagadiTown1mWindowVertices(
        centerXM: Float, centerYM: Float, windowCells: Int, contourInterval: Float, stepTerracing: Float, verticalExaggeration: Float
    ): FloatArray
    external fun nativeGenerateMagadiTown1mWindowIndices(
        centerXM: Float, centerYM: Float, windowCells: Int, contourInterval: Float, stepTerracing: Float, verticalExaggeration: Float
    ): IntArray

    // ─── HIGH-SPEED NATIVE VULKAN MESH BUILDERS (C++ DRIVEN) ─────────────────────

    fun getBridgeTypeName(type: Int): String = when (type) {
        0 -> "Cable-Stayed Bridge (Double A-Frame)"
        1 -> "Long-Span Suspension Bridge (Catenary Main Cables)"
        2 -> "Pedestrian Foot Bridge (Arched Tubular Steel Spine)"
        3 -> "Segmental Concrete Box Girder Viaduct"
        4 -> "Warren Through-Truss Steel Bridge"
        5 -> "Fixed Parabolic Arch Highway Bridge"
        6 -> "Extradosed Hybrid Girder-Stay Bridge"
        7 -> "Double-Leaf Movable Bascule Drawbridge"
        8 -> "Balanced Cantilever Steel Truss Bridge"
        9 -> "Asymmetric Curved Cable-Stayed Bridge"
        10 -> "Multi-Span Roman Masonry Stone Arch Viaduct"
        11 -> "Post-Tensioned Voided Slab Highway Overpass"
        12 -> "Prestressed Catenary Stress-Ribbon Footbridge"
        13 -> "Asymmetric Cable-Stayed Pedestrian Promenade"
        14 -> "Glulam Timber Parabolic Tied-Arch Footbridge"
        15 -> "Weathering Steel Composite Multi-Plate Girder Bridge"
        16 -> "Glazed Tubular Steel Covered Pedestrian Skywalk"
        else -> "Modular Bridge Structure"
    }

    fun mapBridgeTypeKeyToIndex(key: String): Int = when (key) {
        "CABLE_STAYED" -> 0
        "SUSPENSION" -> 1
        "PEDESTRIAN_FOOTBRIDGE" -> 2
        "CONCRETE_VIADUCT" -> 3
        "STEEL_TRUSS" -> 4
        "PARABOLIC_ARCH" -> 5
        "EXTRADOSED" -> 6
        "BASCULE_DRAWBRIDGE" -> 7
        "CANTILEVER_TRUSS" -> 8
        "CURVED_CABLE_STAYED" -> 9
        "ROMAN_VIADUCT" -> 10
        "POST_TENSIONED_OVERPASS" -> 11
        "PEDESTRIAN_STRESS_RIBBON" -> 12
        "PEDESTRIAN_CABLE_STAYED" -> 13
        "PEDESTRIAN_TIMBER_ARCH" -> 14
        "STEEL_PLATE_GIRDER" -> 15
        "PEDESTRIAN_COVERED_SKYWALK" -> 16
        else -> 0
    }

    private fun computeVertexAABB(verts: FloatArray): Pair<List<Float>, List<Float>> {
        val count = verts.size / 12
        if (count <= 0) return listOf(0f, 0f, 0f) to listOf(0f, 0f, 0f)
        var minX = verts[0]; var minY = verts[1]; var minZ = verts[2]
        var maxX = verts[0]; var maxY = verts[1]; var maxZ = verts[2]
        for (i in 1 until count) {
            val b = i * 12
            val x = verts[b]; val y = verts[b + 1]; val z = verts[b + 2]
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
        }
        return listOf(minX, minY, minZ) to listOf(maxX, maxY, maxZ)
    }

    fun generateModularBridgeMesh(
        bridgeType: String,
        spanLengthM: Float = 160f,
        deckWidthM: Float = 24f,
        pylonHeightM: Float = 58f,
        curveBendAngleDeg: Float = 0f,
        superelevationDeg: Float = 0f,
        cablePairsCount: Int = 12,
        hasBarriers: Boolean = true
    ): NativeVulkanMesh {
        val typeInt = mapBridgeTypeKeyToIndex(bridgeType)
        val effCurve = if (typeInt == 9 && abs(curveBendAngleDeg) < 0.01f) 26.0f else curveBendAngleDeg

        val verts = nativeGenerateRoadBridgeVertices(
            typeInt, spanLengthM, deckWidthM, pylonHeightM, effCurve, superelevationDeg, cablePairsCount
        )
        val indices = nativeGenerateRoadBridgeIndices(
            typeInt, spanLengthM, deckWidthM, pylonHeightM, effCurve, superelevationDeg, cablePairsCount
        )
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_BRIDGE,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = spanLengthM * deckWidthM * 2.8f * 0.42f,
            structuralSteelTons = spanLengthM * (if (typeInt == 4) 18.5f else 6.2f),
            deadLoadKn = spanLengthM * deckWidthM * 2.8f * 0.42f * 25f * 9.81f,
            civilStandard = "AASHTO LRFD Bridge Design Specs (9th Edition) / Eurocode EN 1991-2",
            title = getBridgeTypeName(typeInt)
        )
    }

    fun generateModularTollPlazaMesh(
        tollType: String = "CANOPY_PLAZA",
        laneCount: Int = 6,
        laneWidthM: Float = 3.65f,
        plazaLengthM: Float = 64f,
        canopyClearanceM: Float = 5.5f,
        hasDynamicVMS: Boolean = true
    ): NativeVulkanMesh {
        val typeInt = when (tollType) {
            "CANOPY_PLAZA", "BARRIER_TERMINAL" -> 0
            "ETC_GANTRY", "ALL_ELECTRONIC_GANTRIES" -> 1
            "CHECKPOINT", "RAMP_PLAZA" -> 2
            "RAMP_GATE", "CURVED_HYBRID" -> 3
            "SMART_SOLAR_CANOPY" -> 4
            "WEIGH_IN_MOTION" -> 5
            else -> 0
        }
        val verts = nativeGenerateTollPlazaVertices(
            typeInt, laneCount, laneWidthM, canopyClearanceM, plazaLengthM, hasDynamicVMS
        )
        val indices = nativeGenerateTollPlazaIndices(
            typeInt, laneCount, laneWidthM, canopyClearanceM, plazaLengthM, hasDynamicVMS
        )
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_TOLL_PLAZA,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = 640f,
            structuralSteelTons = 120f,
            deadLoadKn = 15600f,
            civilStandard = "FHWA Toll Facilities Guidelines / ASECAP Electronic Tolling",
            title = when (typeInt) {
                1 -> "Free-Flow ETC Tolling Gantry ($laneCount Lanes)"
                2 -> "Border Inspection Toll Checkpoint ($laneCount Lanes)"
                3 -> "Interchange Ramp Automated Barrier Gate"
                4 -> "Smart Solar PV Canopy Toll Plaza ($laneCount Lanes)"
                5 -> "Dynamic Weigh-in-Motion Scale Station ($laneCount Lanes)"
                else -> "Monumental Canopy Toll Plaza ($laneCount Lanes)"
            }
        )
    }

    fun generateModularTunnelMesh(
        tunnelType: String = "MOUNTAIN_HORSESHOE",
        internalRadiusM: Float = 5.8f,
        tunnelLengthM: Float = 120f,
        curveBendAngleDeg: Float = 14f,
        jetFanSpacingM: Float = 28f,
        hasFlaredPortal: Boolean = true,
        hasJetFans: Boolean = true
    ): NativeVulkanMesh {
        val typeInt = when (tunnelType) {
            "MOUNTAIN_HORSESHOE", "NATM_HORSESHOE" -> 0
            "CIRCULAR_BORED", "CIRCULAR_TBM" -> 1
            "CUT_AND_COVER", "CUT_AND_COVER_BOX" -> 2
            "PORTAL_TRANSITION", "TWIN_TUBE_INTERSTATE" -> 3
            "SUBSEA_IMMERSED_TUBE" -> 4
            "BI_LEVEL_CIRCULAR" -> 5
            else -> 0
        }
        val verts = nativeGenerateTunnelVertices(
            typeInt, internalRadiusM, tunnelLengthM, curveBendAngleDeg, jetFanSpacingM, hasFlaredPortal
        )
        val indices = nativeGenerateTunnelIndices(
            typeInt, internalRadiusM, tunnelLengthM, curveBendAngleDeg, jetFanSpacingM, hasFlaredPortal
        )
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_TUNNEL,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = 2450f,
            structuralSteelTons = 310f,
            deadLoadKn = 60000f,
            civilStandard = "PIARC Road Tunnels / NFPA 502 Subterranean Fire Safety",
            title = when (typeInt) {
                1 -> "Circular TBM Twin-Bore Tunnel"
                2 -> "Cut-and-Cover Multi-Cell Box Tunnel"
                3 -> "Flared Acoustic Portal Transition"
                4 -> "Subsea Immersed Concrete Tube"
                5 -> "Bi-Level Double-Deck Shield Tunnel"
                else -> "NATM Mountain Horseshoe Arch Tunnel"
            }
        )
    }

    fun generateModularInterchangeMesh(
        interchangeType: String = "CLOVERLEAF_LOOP",
        loopRadiusM: Float = 55f,
        totalArcAngleDeg: Float = 270f,
        rampWidthM: Float = 7.5f,
        elevationDeltaM: Float = 7.8f,
        superelevationBankingDeg: Float = 5.5f
    ): NativeVulkanMesh {
        val typeInt = when (interchangeType) {
            "CLOVERLEAF_LOOP", "CLOVERLEAF_270" -> 0
            "DIRECTIONAL_FLYOVER" -> 1
            "DIAMOND_SLIP_RAMP", "DIAMOND_RAMP" -> 2
            "ROUNDABOUT", "ROUNDABOUT_INTERCHANGE" -> 3
            "SPUI_SINGLE_POINT" -> 4
            "DIVERGING_DIAMOND" -> 5
            else -> 0
        }
        val verts = nativeGenerateInterchangeVertices(
            typeInt, loopRadiusM, totalArcAngleDeg, rampWidthM, elevationDeltaM, superelevationBankingDeg
        )
        val indices = nativeGenerateInterchangeIndices(
            typeInt, loopRadiusM, totalArcAngleDeg, rampWidthM, elevationDeltaM, superelevationBankingDeg
        )
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_INTERCHANGE,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = 1800f,
            structuralSteelTons = 210f,
            deadLoadKn = 44000f,
            civilStandard = "AASHTO Green Book Geometric Design / Clothoid Splines",
            title = when (typeInt) {
                1 -> "Directional High-Speed Flyover Viaduct"
                2 -> "Diamond Slip Ramp Overpass Interchange"
                3 -> "Modern Multilane Roundabout with Apron"
                4 -> "Single-Point Urban Interchange (SPUI)"
                5 -> "Diverging Diamond Crossover Interchange (DDI)"
                else -> "270° Helical Cloverleaf Loop Interchange"
            }
        )
    }

    fun generateModularRetainingWallMesh(
        wallType: String = "MSE_MODULAR",
        wallHeightM: Float = 7.5f,
        wallLengthM: Float = 60f,
        wallBatterAngleDeg: Float = 4f
    ): NativeVulkanMesh {
        val typeInt = when (wallType) {
            "MSE_MODULAR", "MSE_PRECAST_PANELS" -> 0
            "STEPPED_GABION", "GABION_STEPPED_BASKETS" -> 1
            "CANTILEVER_RC", "CANTILEVER_RC_WALL" -> 2
            "SOIL_NAILED", "SOIL_NAIL_SHOTCRETE" -> 3
            "CRIB_WALL_CELLULAR" -> 4
            "CONTIGUOUS_BORED_PILE" -> 5
            else -> 0
        }
        val verts = nativeGenerateRetainingWallVertices(typeInt, wallHeightM, wallLengthM, wallBatterAngleDeg)
        val indices = nativeGenerateRetainingWallIndices(typeInt, wallHeightM, wallLengthM, wallBatterAngleDeg)
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_RETAINING_WALL,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = 850f,
            structuralSteelTons = 45f,
            deadLoadKn = 20800f,
            civilStandard = "FHWA GEC 011 MSE Walls / Eurocode 7 Geotechnical",
            title = when (typeInt) {
                1 -> "Stepped Wire-Mesh Rock Gabion Wall"
                2 -> "Cast-in-Place Cantilever RC Stem Wall"
                3 -> "Soil-Nailed Shotcrete Wall with Anchor Plates"
                4 -> "Interlocking Concrete Cellular Crib Wall"
                5 -> "Contiguous Secant Bored Pile Retaining Wall"
                else -> "MSE Cruciform Precast Panel Retaining Wall"
            }
        )
    }

    fun generateModularSignpostMesh(
        signType: String = "CANTILEVER_HIGHWAY",
        gantrySpanM: Float = 18f,
        clearanceH: Float = 5.8f,
        boardWidthM: Float = 6.5f,
        signCount: Int = 3,
        hasDMS: Boolean = true
    ): NativeVulkanMesh {
        val typeInt = when (signType) {
            "CANTILEVER_HIGHWAY" -> 0
            "FULL_SPAN_GANTRY" -> 1
            "VARIABLE_MESSAGE_DMS" -> 2
            "REGULATORY_CHEVRON_SET" -> 3
            "BUTTERFLY_SPLIT_GORE" -> 4
            else -> 0
        }
        val verts = nativeGenerateSignpostVertices(typeInt, gantrySpanM, clearanceH, boardWidthM, signCount, hasDMS)
        val indices = nativeGenerateSignpostIndices(typeInt, gantrySpanM, clearanceH, boardWidthM, signCount, hasDMS)
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_SIGNPOST,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = 42f,
            structuralSteelTons = 14.5f,
            deadLoadKn = 380f,
            civilStandard = "FHWA MUTCD 11th Edition / AASHTO LTS-6 Sign Supports",
            title = when (typeInt) {
                1 -> "Full-Span Overhead Warren Truss Sign Gantry"
                2 -> "Dynamic LED Variable Message Sign (VMS/DMS)"
                3 -> "MUTCD Curve Chevron & Regulatory Sign Series"
                4 -> "Dual-Wing Gore Split Butterfly Highway Sign"
                else -> "Cantilever Truss Highway Guide Sign"
            }
        )
    }

    fun generateModularExpresswayMesh(
        expresswayType: String = "SIX_LANE_FREEWAY",
        lanesPerDir: Int = 3,
        sectionLengthM: Float = 120f,
        curveBendDeg: Float = 14f,
        bankingDeg: Float = 4.5f,
        hasLighting: Boolean = true
    ): NativeVulkanMesh {
        val typeInt = when (expresswayType) {
            "SIX_LANE_FREEWAY" -> 0
            "ELEVATED_VIADUCT" -> 1
            "GORE_MERGE_TAPER" -> 2
            "SMART_EV_CORRIDOR" -> 3
            "MOUNTAIN_SWITCHBACK_CUT" -> 4
            else -> 0
        }
        val verts = nativeGenerateExpresswayVertices(typeInt, lanesPerDir, sectionLengthM, curveBendDeg, bankingDeg, hasLighting)
        val indices = nativeGenerateExpresswayIndices(typeInt, lanesPerDir, sectionLengthM, curveBendDeg, bankingDeg, hasLighting)
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_EXPRESSWAY,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = sectionLengthM * lanesPerDir * 14f,
            structuralSteelTons = 185f,
            deadLoadKn = 36000f,
            civilStandard = "AASHTO Geometric Design of Highways / Eurocode EN 13108",
            title = when (typeInt) {
                1 -> "Elevated Urban Expressway Concrete Viaduct"
                2 -> "Expressway Exit Gore & Deceleration Taper"
                3 -> "Smart Managed Inductive EV & BRT Express Corridor"
                4 -> "Superelevated Mountain Rock-Cut Bench Expressway"
                else -> "Divided Multi-Lane Controlled-Access Freeway"
            }
        )
    }

    fun generateModularBarrierMesh(
        barrierType: String = "NEW_JERSEY_FSHAPE",
        barrierLengthM: Float = 48f,
        barrierHeightM: Float = 1.10f,
        curveBendDeg: Float = 12f,
        postSpacingM: Float = 2.0f,
        hasReflectors: Boolean = true
    ): NativeVulkanMesh {
        val typeInt = when (barrierType) {
            "NEW_JERSEY_FSHAPE" -> 0
            "W_BEAM_GUARDRAIL" -> 1
            "HIGH_TENSION_CABLE" -> 2
            "ACOUSTIC_NOISE_WALL" -> 3
            "ROLLER_CRASH_CUSHION" -> 4
            else -> 0
        }
        val verts = nativeGenerateBarrierVertices(typeInt, barrierLengthM, barrierHeightM, curveBendDeg, postSpacingM, hasReflectors)
        val indices = nativeGenerateBarrierIndices(typeInt, barrierLengthM, barrierHeightM, curveBendDeg, postSpacingM, hasReflectors)
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_BARRIER,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = barrierLengthM * 0.65f,
            structuralSteelTons = 9.5f,
            deadLoadKn = 720f,
            civilStandard = "MASH 2016 Crash Criteria / EN 1317 Road Restraint",
            title = when (typeInt) {
                1 -> "Galvanized Corrugated W-Beam Guardrail & SKT"
                2 -> "4-Strand High-Tension Median Cable Barrier"
                3 -> "Highway Acoustic Noise Barrier Sound Wall"
                4 -> "Rotating Yellow EVA Roller Crash Cushion Rail"
                else -> "MASH F-Shape Precast Concrete Jersey Barrier"
            }
        )
    }

    fun generateModularRoadBlockMesh(
        roadBlockType: String = "WATER_JERSEY_LINE",
        roadWidthM: Float = 12f,
        zoneLengthM: Float = 34f,
        unitCount: Int = 6,
        hasBeacons: Boolean = true
    ): NativeVulkanMesh {
        val typeInt = when (roadBlockType) {
            "WATER_JERSEY_LINE" -> 0
            "HYDRAULIC_BOLLARDS" -> 1
            "TACTICAL_SPIKE_CHICANE" -> 2
            "CONSTRUCTION_CLOSURE" -> 3
            "DRAGON_TEETH_CHICANE" -> 4
            else -> 0
        }
        val verts = nativeGenerateRoadBlockVertices(typeInt, roadWidthM, zoneLengthM, unitCount, hasBeacons)
        val indices = nativeGenerateRoadBlockIndices(typeInt, roadWidthM, zoneLengthM, unitCount, hasBeacons)
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_ROAD_BLOCK,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = 68f,
            structuralSteelTons = 16.0f,
            deadLoadKn = 1480f,
            civilStandard = "FHWA MUTCD Part 6 Temporary Control / ASTM F2656 K12",
            title = when (typeInt) {
                1 -> "K12 Hydraulic Anti-Ram Stainless Bollard Array"
                2 -> "Tactical Checkpoint Slalom Chicane & Spike Strip"
                3 -> "MUTCD Workzone Drum Taper & LED Arrow Trailer"
                4 -> "Staggered Precast Concrete Dragon-Teeth Blocks"
                else -> "Water-Filled Jersey Barricade & Saw-Horse Line"
            }
        )
    }

    fun generateConnectedHighwayScene(scale: Float = 1.0f): NativeVulkanMesh {
        val verts = nativeGenerateConnectedHighwayVertices(scale)
        val indices = nativeGenerateConnectedHighwayIndices(scale)
        val (bbMin, bbMax) = computeVertexAABB(verts)

        return NativeVulkanMesh(
            assetType = CppAssetType.CONNECTED_HIGHWAY_SCENE,
            vertices = verts,
            indices = indices,
            boundingBoxMin = bbMin,
            boundingBoxMax = bbMax,
            totalVolumeM3 = 8200f,
            structuralSteelTons = 1200f,
            deadLoadKn = 201000f,
            civilStandard = "Vulkan 1.3 Full Highway Corridor (Toll -> Bridge -> Tunnel)",
            title = "Connected Highway Real-World Scene"
        )
    }

    fun generatePowerGridMesh(
        numPosts: Int = 4,
        spanDistance: Float = 16f,
        cornerAngleDeg: Float = 85f
    ): NativeVulkanMesh {
        val verts10 = nativeBuildPowerGridVertices(numPosts, spanDistance, cornerAngleDeg)
        val indices = nativeBuildPowerGridIndices(numPosts, spanDistance, cornerAngleDeg)
        // Convert 10-float to 12-float: pos(3), norm(3), uv(2), color(4)
        val vCount = verts10.size / 10
        val verts12 = FloatArray(vCount * 12)
        for (i in 0 until vCount) {
            val b10 = i * 10
            val b12 = i * 12
            verts12[b12 + 0] = verts10[b10 + 0]
            verts12[b12 + 1] = verts10[b10 + 1]
            verts12[b12 + 2] = verts10[b10 + 2]
            verts12[b12 + 3] = verts10[b10 + 3]
            verts12[b12 + 4] = verts10[b10 + 4]
            verts12[b12 + 5] = verts10[b10 + 5]
            verts12[b12 + 6] = verts10[b10 + 6]
            verts12[b12 + 7] = verts10[b10 + 7]
            verts12[b12 + 8] = 0.85f
            verts12[b12 + 9] = 0.88f
            verts12[b12 + 10] = 0.92f
            verts12[b12 + 11] = 1.0f
        }
        return NativeVulkanMesh(
            assetType = CppAssetType.POWER_GRID_NETWORK,
            vertices = verts12,
            indices = indices,
            boundingBoxMin = listOf(-spanDistance * 1.2f, 0f, -spanDistance * 1.2f),
            boundingBoxMax = listOf(spanDistance * 1.2f, 15f, spanDistance * 1.2f),
            totalVolumeM3 = 45f,
            structuralSteelTons = 18f,
            deadLoadKn = 450f,
            civilStandard = "IEEE 738 High-Voltage Transmission Line Clearance",
            title = "Power Grid Corner Route ($numPosts Units)"
        )
    }

    fun NativeVulkanMesh.toProceduralModelMesh(): ProceduralModelMesh {
        val representativeBoxes = listOf(
            ProceduralBoxPart("substructure", "Substructure Plinth / Pier", 0f, 6f, 0f, 6f, 12f, 8f, "#607D8B", 0.38f, 0.49f, 0.55f, 1f, "Foundation"),
            ProceduralBoxPart("superstructure", "Superstructure & Deck", 0f, 13f, 0f, 18f, 2.5f, 60f, "#37474F", 0.22f, 0.28f, 0.31f, 1f, "Deck"),
            ProceduralBoxPart("pylon_tower", "Tower / Architectural Feature", 0f, 32f, 0f, 3.5f, 38f, 4f, "#ECEFF1", 0.92f, 0.94f, 0.95f, 1f, "Pylon")
        )
        return ProceduralModelMesh(
            assetType = assetType,
            boxes = representativeBoxes,
            totalVertices = vertexCount,
            totalTriangles = triangleCount,
            boundingBoxMin = boundingBoxMin,
            boundingBoxMax = boundingBoxMax,
            estimatedVolumeM3 = totalVolumeM3,
            memoryFootprintBytes = vertexCount * 48 + triangleCount * 12,
            wires = emptyList(),
            nativeMesh = this
        )
    }

    // ─── TERRAIN UNPACKER METHODS (C++ NATIVE ENGINE) ────────────────────────────

    fun getMagadiElevation(
        worldXMeters: Float,
        worldYMeters: Float,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): Float = nativeGetElevation(worldXMeters, worldYMeters, contourInterval, stepTerracing)

    fun generateMagadiHumanStartMesh(
        walkDistanceM: Float = 0f,
        gridSize: Int = 45,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D {
        val packedVerts = nativeGenerateHumanStartVertices(walkDistanceM, gridSize, contourInterval, stepTerracing)
        val nativeIndices = nativeGenerateHumanStartIndices(walkDistanceM, gridSize, contourInterval, stepTerracing)
        return unpackTerrainMesh(packedVerts, nativeIndices, gridSize, gridSize, 1, 48)
    }

    fun generateRoadSegmentMesh(
        segmentIndex: Int,
        gridSize: Int = 49,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D {
        val clampedSegment = segmentIndex.coerceIn(1, 10)
        val packedVerts = nativeGenerateRoadSegmentVertices(clampedSegment, gridSize, contourInterval, stepTerracing)
        val nativeIndices = nativeGenerateRoadSegmentIndices(clampedSegment, gridSize, contourInterval, stepTerracing)
        return unpackTerrainMesh(packedVerts, nativeIndices, gridSize, gridSize, clampedSegment, 64)
    }

    fun generateRoadSubSegmentMesh(
        segmentIndex: Int,
        subSegmentIndex: Int,
        gridSize: Int = 49,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D {
        val clampedSegment = segmentIndex.coerceIn(1, 10)
        val clampedSub = subSegmentIndex.coerceIn(1, 10)
        val packedVerts = nativeGenerateRoadSubSegmentVertices(clampedSegment, clampedSub, gridSize, contourInterval, stepTerracing)
        val nativeIndices = nativeGenerateRoadSubSegmentIndices(clampedSegment, clampedSub, gridSize, contourInterval, stepTerracing)
        return unpackTerrainMesh(packedVerts, nativeIndices, gridSize, gridSize, clampedSegment, 48)
    }

    fun generate1mResolutionMesh(
        segmentIndex: Int,
        centerForwardM: Float,
        contourInterval: Float = 5f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D {
        val clampedSegment = segmentIndex.coerceIn(1, 10)
        val packedVerts = nativeGenerate1mResolutionVertices(clampedSegment, centerForwardM, contourInterval, stepTerracing)
        val nativeIndices = nativeGenerate1mResolutionIndices(clampedSegment, centerForwardM, contourInterval, stepTerracing)
        return unpackTerrainMesh(packedVerts, nativeIndices, 33, 33, clampedSegment, 32)
    }

    fun generateWholeCombinedTerrainMesh(
        gridResX: Int = 33,
        gridResZ: Int = 129,
        contourInterval: Float = 50f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D {
        val packedVerts = nativeGenerateWholeCombinedTerrainVertices(gridResX, gridResZ, contourInterval, stepTerracing)
        val nativeIndices = nativeGenerateWholeCombinedTerrainIndices(gridResX, gridResZ, contourInterval, stepTerracing)

        val vertCount = packedVerts.size / 11
        val vertices = ArrayList<TerrainVertex>(vertCount)
        var minH = Float.MAX_VALUE
        var maxH = -Float.MAX_VALUE
        var totalSlope = 0f

        for (i in 0 until vertCount) {
            val b = i * 11
            val x = packedVerts[b + 0]
            val y = packedVerts[b + 1]
            val z = packedVerts[b + 2]
            val nx = packedVerts[b + 3]
            val ny = packedVerts[b + 4]
            val nz = packedVerts[b + 5]
            val slopeDeg = packedVerts[b + 6]
            val rawElev = packedVerts[b + 7]
            val contourBand = packedVerts[b + 8]
            val isolineFactor = packedVerts[b + 9]
            val flag = packedVerts[b + 10]

            if (y < minH) minH = y
            if (y > maxH) maxH = y
            totalSlope += slopeDeg

            val surfType = when (flag.toInt()) {
                1 -> "asphalt"
                2 -> "water"
                3 -> "trona_soda_crust"
                4 -> "gravel"
                5 -> "escarpment_rock"
                6 -> "highland_grass"
                else -> "savannah_scrub"
            }

            vertices.add(
                TerrainVertex(
                    x = x, y = y, z = z, nx = nx, ny = ny, nz = nz,
                    slopeDeg = slopeDeg,
                    grassWeight = if (surfType == "savannah_scrub" || surfType == "highland_grass") 1f else 0f,
                    dirtWeight = if (surfType == "gravel" || flag == 4f) 1f else 0f,
                    rockWeight = if (surfType == "escarpment_rock" || slopeDeg > 20f) 0.90f else 0f,
                    snowWeight = if (surfType == "trona_soda_crust") 1f else 0f,
                    isRoadSurface = flag == 1.0f,
                    surfaceType = surfType,
                    isWaterSurface = flag == 2.0f,
                    isMudSurface = flag == 3.0f,
                    isGravelSurface = flag == 4.0f,
                    rawElevation = rawElev,
                    contourBand = contourBand,
                    isolineFactor = isolineFactor
                )
            )
        }

        val indices = ArrayList<Int>(nativeIndices.size)
        for (idx in nativeIndices) indices.add(idx)

        val rawWps = nativeGetCombinedRoadCenterline(128)
        val numWps = rawWps.size / 13
        val centerline = ArrayList<RoadWaypoint3D>(numWps)
        for (w in 0 until numWps) {
            val b = w * 13
            centerline.add(
                RoadWaypoint3D(
                    x = rawWps[b + 0], y = rawWps[b + 1], z = rawWps[b + 2],
                    rawElevation = rawWps[b + 3], forwardM = rawWps[b + 4], globalKmM = rawWps[b + 5],
                    lateralM = rawWps[b + 6], gradePercent = rawWps[b + 7], headingDeg = rawWps[b + 8],
                    naturalElevM = rawWps[b + 9], cutFillDepthM = rawWps[b + 10],
                    nextContourElevM = rawWps[b + 11], distToNextContourM = rawWps[b + 12]
                )
            )
        }

        val landmarks = listOf(
            GameTerrainAssetPlacement("marker_lake_magadi", "spawn", "", 0.0f, -50.0f, -110.0f, 0f, 1.2f),
            GameTerrainAssetPlacement("marker_olorgesailie", "garage", "", 2.2f, -15.0f, -44.0f, 0f, 1.0f),
            GameTerrainAssetPlacement("marker_kamukuru", "bridge", "", 3.0f, 0.0f, 0.0f, 0f, 1.0f),
            GameTerrainAssetPlacement("marker_corner_baridi", "spawn", "", 1.8f, 75.0f, 56.0f, 0f, 1.2f),
            GameTerrainAssetPlacement("marker_nairobi_bomas", "garage", "", 0.0f, 48.0f, 110.0f, 0f, 1.0f)
        )

        return TerrainMesh3D(
            resolutionX = gridResX,
            resolutionZ = gridResZ,
            vertices = vertices,
            indices = indices,
            minHeight = minH,
            maxHeight = maxH,
            avgSlope = if (vertCount > 0) totalSlope / vertCount else 5f,
            assetPlacements = landmarks,
            roadCenterline = centerline
        )
    }

    private fun unpackTerrainMesh(
        packedVerts: FloatArray,
        nativeIndices: IntArray,
        resX: Int,
        resZ: Int,
        segmentIndex: Int,
        centerlinePoints: Int
    ): TerrainMesh3D {
        val vertCount = packedVerts.size / 11
        val vertices = ArrayList<TerrainVertex>(vertCount)
        var minH = Float.MAX_VALUE
        var maxH = -Float.MAX_VALUE
        var totalSlope = 0f

        for (i in 0 until vertCount) {
            val b = i * 11
            val x = packedVerts[b + 0]
            val y = packedVerts[b + 1]
            val z = packedVerts[b + 2]
            val nx = packedVerts[b + 3]
            val ny = packedVerts[b + 4]
            val nz = packedVerts[b + 5]
            val slopeDeg = packedVerts[b + 6]
            val rawElev = packedVerts[b + 7]
            val contourBand = packedVerts[b + 8]
            val isolineFactor = packedVerts[b + 9]
            val flag = packedVerts[b + 10]

            if (y < minH) minH = y
            if (y > maxH) maxH = y
            totalSlope += slopeDeg

            val surfType = when (flag.toInt()) {
                1 -> "asphalt"
                2 -> "water"
                3 -> "trona_soda_crust"
                4 -> "gravel"
                5 -> "escarpment_rock"
                6 -> "highland_grass"
                else -> "savannah_scrub"
            }

            vertices.add(
                TerrainVertex(
                    x = x, y = y, z = z, nx = nx, ny = ny, nz = nz,
                    slopeDeg = slopeDeg,
                    grassWeight = if (surfType == "savannah_scrub" || surfType == "highland_grass") 1f else 0f,
                    dirtWeight = if (surfType == "gravel" || flag == 4f) 1f else 0f,
                    rockWeight = if (surfType == "escarpment_rock" || slopeDeg > 22f) 0.90f else 0f,
                    snowWeight = if (surfType == "trona_soda_crust") 1f else 0f,
                    isRoadSurface = flag == 1.0f,
                    surfaceType = surfType,
                    isWaterSurface = flag == 2.0f,
                    isMudSurface = flag == 3.0f,
                    isGravelSurface = flag == 4.0f,
                    rawElevation = rawElev,
                    contourBand = contourBand,
                    isolineFactor = isolineFactor
                )
            )
        }

        val indices = ArrayList<Int>(nativeIndices.size)
        for (idx in nativeIndices) indices.add(idx)

        val packedWps = nativeGetRoadCenterline(segmentIndex, centerlinePoints)
        val wpCount = packedWps.size / 13
        val centerline = ArrayList<RoadWaypoint3D>(wpCount)
        for (w in 0 until wpCount) {
            val base = w * 13
            centerline.add(
                RoadWaypoint3D(
                    x = packedWps[base + 0], y = packedWps[base + 1], z = packedWps[base + 2],
                    rawElevation = packedWps[base + 3], forwardM = packedWps[base + 4], globalKmM = packedWps[base + 5],
                    lateralM = packedWps[base + 6], gradePercent = packedWps[base + 7], headingDeg = packedWps[base + 8],
                    naturalElevM = packedWps[base + 9], cutFillDepthM = packedWps[base + 10],
                    nextContourElevM = packedWps[base + 11], distToNextContourM = packedWps[base + 12]
                )
            )
        }

        return TerrainMesh3D(
            resolutionX = resX,
            resolutionZ = resZ,
            vertices = vertices,
            indices = indices,
            minHeight = minH,
            maxHeight = maxH,
            avgSlope = if (vertCount > 0) totalSlope / vertCount else 5f,
            assetPlacements = emptyList(),
            roadCenterline = centerline
        )
    }

    fun getRoadPointAt(segmentIndex: Int, forwardM: Float): RoadWaypoint3D {
        val raw = nativeGetRoadPointAt(segmentIndex.coerceIn(1, 10), forwardM)
        if (raw.size >= 13) {
            return RoadWaypoint3D(
                x = raw[0], y = raw[1], z = raw[2],
                rawElevation = raw[3], forwardM = raw[4], globalKmM = raw[5],
                lateralM = raw[6], gradePercent = raw[7], headingDeg = raw[8],
                naturalElevM = raw[9], cutFillDepthM = raw[10],
                nextContourElevM = raw[11], distToNextContourM = raw[12]
            )
        }
        return RoadWaypoint3D(
            x = raw[0], y = raw[1], z = raw[2],
            rawElevation = raw[3], forwardM = raw[4], globalKmM = raw[5],
            lateralM = raw[6], gradePercent = raw[7], headingDeg = raw[8]
        )
    }

    fun getRoadSegmentInfo(segmentIndex: Int): FloatArray =
        nativeGetRoadSegmentInfo(segmentIndex.coerceIn(1, 10))

    fun getGroundPieceMetrics(segmentIndex: Int, groundDatumM: Float = 400.0f): GroundPieceMetrics {
        val raw = nativeGetGroundPieceMetrics(segmentIndex.coerceIn(1, 10), groundDatumM)
        return GroundPieceMetrics(
            datumASLM = raw[0], depthMinM = raw[1], depthMaxM = raw[2],
            depthRoadStartM = raw[3], depthRoadEndM = raw[4], verticalReliefM = raw[5]
        )
    }

    fun generateFirst10kmSegmentMesh(gridSize: Int = 49, contourInterval: Float = 25f, stepTerracing: Float = 0f): TerrainMesh3D =
        generateRoadSegmentMesh(1, gridSize, contourInterval, stepTerracing)

    fun generateMagadiOverviewMesh(gridSize: Int = 45, contourInterval: Float = 25f, stepTerracing: Float = 0f): TerrainMesh3D =
        generateRoadSegmentMesh(1, gridSize, contourInterval, stepTerracing)

    fun generateMagadiChunkMesh(col: Int, row: Int, gridSize: Int = 41, contourInterval: Float = 25f, stepTerracing: Float = 0f): TerrainMesh3D =
        generateFirst10kmSegmentMesh(gridSize, contourInterval, stepTerracing)

    fun generateTestGround001Mesh(gridSize: Int = 49): TerrainMesh3D =
        generateFirst10kmSegmentMesh(gridSize, 25f, 0f)

    fun generateTerrainMesh(state: TerrainState, gridSize: Int = 33, worldSize: Float = 100f): TerrainMesh3D =
        generateFirst10kmSegmentMesh(gridSize, state.contourInterval, state.stepTerracingFactor)

    fun simulateErosionCycle(state: TerrainState): TerrainState =
        state.copy(erosionRate = (state.erosionRate + 0.05f).coerceAtMost(1.0f))

    // ─── MAGADI TOWN 3 KM x 5 KM (1M GRID) C++ WRAPPERS ──────────────────────────

    fun generateMagadiTownOverviewMesh(
        resX: Int = 61,
        resZ: Int = 101,
        contourInterval: Float = 10f,
        stepTerracing: Float = 0f,
        verticalExaggeration: Float = 2.4f
    ): TerrainMesh3D {
        val verts = nativeGenerateMagadiTownOverviewVertices(
            resX, resZ, contourInterval, stepTerracing, verticalExaggeration
        )
        val indices = nativeGenerateMagadiTownOverviewIndices(
            resX, resZ, contourInterval, stepTerracing, verticalExaggeration
        )
        return unpackPlainTerrainMesh(verts, indices, resX, resZ)
    }

    fun generateMagadiTown1mWindowMesh(
        centerXM: Float,
        centerYM: Float,
        windowCells: Int = 64,
        contourInterval: Float = 2f,
        stepTerracing: Float = 0f,
        verticalExaggeration: Float = 1.6f
    ): TerrainMesh3D {
        val gridSize = windowCells.coerceIn(24, 80) + 1
        val verts = nativeGenerateMagadiTown1mWindowVertices(
            centerXM, centerYM, windowCells, contourInterval, stepTerracing, verticalExaggeration
        )
        val indices = nativeGenerateMagadiTown1mWindowIndices(
            centerXM, centerYM, windowCells, contourInterval, stepTerracing, verticalExaggeration
        )
        return unpackPlainTerrainMesh(verts, indices, gridSize, gridSize)
    }

    private fun unpackPlainTerrainMesh(
        packedVerts: FloatArray,
        nativeIndices: IntArray,
        resX: Int,
        resZ: Int
    ): TerrainMesh3D {
        val vertCount = packedVerts.size / 11
        val vertices = ArrayList<TerrainVertex>(vertCount)
        var minH = Float.MAX_VALUE
        var maxH = -Float.MAX_VALUE
        var totalSlope = 0f

        for (i in 0 until vertCount) {
            val b = i * 11
            val x = packedVerts[b + 0]
            val y = packedVerts[b + 1]
            val z = packedVerts[b + 2]
            val nx = packedVerts[b + 3]
            val ny = packedVerts[b + 4]
            val nz = packedVerts[b + 5]
            val slopeDeg = packedVerts[b + 6]
            val rawElev = packedVerts[b + 7]
            val contourBand = packedVerts[b + 8]
            val isolineFactor = packedVerts[b + 9]
            val flag = packedVerts[b + 10]

            if (y < minH) minH = y
            if (y > maxH) maxH = y
            totalSlope += slopeDeg

            val surfType = when (flag.toInt()) {
                1 -> "asphalt"
                2 -> "water"
                3 -> "trona_soda_crust"
                4 -> "gravel"
                5 -> "escarpment_rock"
                6 -> "highland_grass"
                else -> "savannah_scrub"
            }

            vertices.add(
                TerrainVertex(
                    x = x, y = y, z = z, nx = nx, ny = ny, nz = nz,
                    slopeDeg = slopeDeg,
                    grassWeight = if (surfType == "savannah_scrub" || surfType == "highland_grass") 1f else 0f,
                    dirtWeight = if (surfType == "gravel" || flag == 4f) 1f else 0f,
                    rockWeight = if (surfType == "escarpment_rock" || slopeDeg > 22f) 0.90f else 0f,
                    snowWeight = if (surfType == "trona_soda_crust") 1f else 0f,
                    isRoadSurface = flag == 1.0f,
                    surfaceType = surfType,
                    isWaterSurface = flag == 2.0f,
                    isMudSurface = flag == 3.0f,
                    isGravelSurface = flag == 4.0f,
                    rawElevation = rawElev,
                    contourBand = contourBand,
                    isolineFactor = isolineFactor
                )
            )
        }

        val indices = ArrayList<Int>(nativeIndices.size)
        for (idx in nativeIndices) indices.add(idx)

        return TerrainMesh3D(
            resolutionX = resX,
            resolutionZ = resZ,
            vertices = vertices,
            indices = indices,
            minHeight = minH,
            maxHeight = maxH,
            avgSlope = if (vertCount > 0) totalSlope / vertCount else 2f,
            assetPlacements = emptyList(),
            roadCenterline = emptyList()
        )
    }
}
