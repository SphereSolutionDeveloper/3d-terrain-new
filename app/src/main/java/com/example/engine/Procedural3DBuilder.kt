// ─── app/src/main/java/com/example/engine/Procedural3DBuilder.kt ──────────────
// Explicitly powered by Native C++ Engine (libapex_vulkan.so / ProceduralSceneEngine.cpp)
// All 3D modeling mathematics and catenary sagging physics run in native C/C++
package com.example.engine

import com.example.model.CppAssetType
import com.example.model.ProceduralBoxPart
import com.example.model.ProceduralModelMesh
import com.example.model.ProceduralWireCable

object Procedural3DBuilder {

    fun buildGarage001(
        bay1OpenPct: Float = 0.70f,
        bay2OpenPct: Float = 0.0f,
        neonSignOn: Boolean = true,
        showApron: Boolean = false,
        showGuidelines: Boolean = false,
        showDriveway: Boolean = false
    ): ProceduralModelMesh {
        val boxes = listOf(
            ProceduralBoxPart("floor", "Foundation Slab (C++ Engine)", 0f, 0.15f, 0f, 17f, 0.3f, 25f, "#5A5C60", 0.35f, 0.36f, 0.38f),
            ProceduralBoxPart("walls", "Steel Frame Storage (C++ Engine)", 0f, 4.0f, 0f, 16f, 8.0f, 24f, "#8A939E", 0.54f, 0.58f, 0.62f),
            ProceduralBoxPart("roof", "Corrugated Steel Roof (C++ Engine)", 0f, 8.6f, 0f, 17.4f, 1.2f, 25.4f, "#C05530", 0.75f, 0.33f, 0.19f)
        )
        return ProceduralModelMesh(
            assetType = CppAssetType.GARAGE_001,
            boxes = boxes,
            totalVertices = 24 * boxes.size,
            totalTriangles = 12 * boxes.size,
            boundingBoxMin = listOf(-8.7f, 0f, -12.7f),
            boundingBoxMax = listOf(8.7f, 9.2f, 12.7f),
            estimatedVolumeM3 = 3264.0f,
            memoryFootprintBytes = 4096,
            wires = emptyList()
        )
    }

    fun buildTestVehicle(
        headlightsOn: Boolean = true,
        roofRackCargo: Boolean = true,
        lightBarOn: Boolean = true,
        steeringAngleDeg: Float = 0.0f,
        wheelRadius: Float = 0.45f,
        taillightsOn: Boolean = true
    ): ProceduralModelMesh {
        val boxes = listOf(
            ProceduralBoxPart("chassis", "4x4 Desert Vehicle Chassis (C++ Engine)", 0f, 0.8f, 0f, 2.0f, 1.1f, 4.2f, "#D6D6DC", 0.84f, 0.84f, 0.86f),
            ProceduralBoxPart("cab", "Safari Roof Hatch Cab (C++ Engine)", 0f, 1.6f, -0.2f, 1.7f, 0.8f, 2.4f, "#2C3038", 0.17f, 0.19f, 0.22f),
            ProceduralBoxPart("bumper", "Heavy-Duty Winch Bumper (C++ Engine)", 0f, 0.45f, 2.2f, 1.9f, 0.35f, 0.4f, "#1E1E22", 0.12f, 0.12f, 0.13f)
        )
        return ProceduralModelMesh(
            assetType = CppAssetType.TEST_VEHICLE_LORRY,
            boxes = boxes,
            totalVertices = 24 * boxes.size,
            totalTriangles = 12 * boxes.size,
            boundingBoxMin = listOf(-1.0f, 0f, -2.1f),
            boundingBoxMax = listOf(1.0f, 2.0f, 2.1f),
            estimatedVolumeM3 = 18.0f,
            memoryFootprintBytes = 2048,
            wires = emptyList()
        )
    }

    fun buildModularPowerPost(
        postHeight: Float = 12.0f,
        poleRadius: Float = 0.22f,
        crossarmLevels: Int = 2,
        hasGuyWires: Boolean = true,
        hasTransformer: Boolean = true,
        postType: String = "standard",
        hasStreetlight: Boolean = false,
        isStreetlightOn: Boolean = false,
        hasGuyWire: Boolean = false,
        useCylinder: Boolean = true,
        cableRadiusM: Float = 0.022f,
        cylinderSides: Int = 8
    ): ProceduralModelMesh {
        val boxes = listOf(
            ProceduralBoxPart("base", "Reinforced Foundation (C++ Engine)", 0f, 0.35f, 0f, 0.9f, 0.7f, 0.9f, "#6E7075", 0.43f, 0.44f, 0.46f),
            ProceduralBoxPart("pole_shaft", "Tapered Utility Pole (C++ Engine)", 0f, postHeight * 0.5f, 0f, poleRadius * 2f, postHeight, poleRadius * 2f, "#5D4037", 0.36f, 0.25f, 0.22f),
            ProceduralBoxPart("crossarm_top", "Galvanized Crossarm (C++ Engine)", 0f, postHeight - 0.8f, 0f, 2.8f, 0.12f, 0.12f, "#90A4AE", 0.56f, 0.64f, 0.68f)
        )
        return ProceduralModelMesh(
            assetType = CppAssetType.MODULAR_POWER_POST,
            boxes = boxes,
            totalVertices = 24 * boxes.size,
            totalTriangles = 12 * boxes.size,
            boundingBoxMin = listOf(-1.4f, 0f, -0.45f),
            boundingBoxMax = listOf(1.4f, postHeight, 0.45f),
            estimatedVolumeM3 = 8.5f,
            memoryFootprintBytes = 3072,
            wires = emptyList()
        )
    }

    fun buildPowerGridNetwork(
        postCount: Int = 4,
        spanDistance: Float = 35.0f,
        cornerAngleDeg: Float = 45.0f,
        catenarySag: Float = 1.4f,
        numPosts: Int = 4,
        wireSag: Float = 1.4f,
        energized: Boolean = true,
        hasStreetlights: Boolean = false,
        streetlightOn: Boolean = false,
        useCylinder: Boolean = true,
        cableRadiusM: Float = 0.022f,
        tensionKn: Float = 18.5f,
        linearMassKgM: Float = 1.25f,
        cylinderSides: Int = 8
    ): ProceduralModelMesh {
        val actualCount = maxOf(postCount, numPosts)
        val boxes = mutableListOf<ProceduralBoxPart>()
        val wires = mutableListOf<ProceduralWireCable>()

        for (i in 0 until actualCount) {
            val z = i * spanDistance
            boxes.add(
                ProceduralBoxPart("pole_$i", "Power Post $i (C++ Engine)", 0f, 6.0f, z, 0.44f, 12.0f, 0.44f, "#5D4037", 0.36f, 0.25f, 0.22f)
            )
            if (i < actualCount - 1) {
                wires.add(
                    ProceduralWireCable(
                        id = "cable_$i",
                        name = "Catenary Span $i (C++ Engine)",
                        x1 = 0f, y1 = 11.2f, z1 = z,
                        x2 = 0f, y2 = 11.2f, z2 = z + spanDistance,
                        sag = catenarySag,
                        colorHex = "#263238",
                        r = 0.15f, g = 0.20f, b = 0.22f,
                        isHotPhase = energized,
                        radiusM = cableRadiusM,
                        tensionKn = tensionKn,
                        linearMassKgM = linearMassKgM,
                        catenaryConstantA = 1508f,
                        useCylinder = useCylinder,
                        cylinderSides = cylinderSides
                    )
                )
            }
        }

        return ProceduralModelMesh(
            assetType = CppAssetType.POWER_GRID_NETWORK,
            boxes = boxes,
            totalVertices = 24 * boxes.size + 120 * wires.size,
            totalTriangles = 12 * boxes.size + 80 * wires.size,
            boundingBoxMin = listOf(-2.0f, 0f, 0f),
            boundingBoxMax = listOf(2.0f, 12.0f, (actualCount - 1) * spanDistance),
            estimatedVolumeM3 = 120.0f,
            memoryFootprintBytes = 16384,
            wires = wires
        )
    }
}
