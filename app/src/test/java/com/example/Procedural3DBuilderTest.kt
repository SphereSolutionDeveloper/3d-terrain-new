package com.example

import com.example.engine.Math3D
import com.example.engine.NativeEngineBridge
import com.example.model.CppAssetType
import com.example.model.NativeVulkanMesh
import org.junit.Assert.*
import org.junit.Test

class Procedural3DBuilderTest {

    @Test
    fun testNativeVulkanMeshDataModel() {
        val dummyVerts = FloatArray(12 * 4) // 4 vertices
        val dummyIndices = intArrayOf(0, 1, 2, 0, 2, 3)
        val mesh = NativeVulkanMesh(
            assetType = CppAssetType.MODULAR_BRIDGE,
            vertices = dummyVerts,
            indices = dummyIndices,
            vertexCount = 4,
            triangleCount = 2,
            civilStandard = "AASHTO LRFD / Eurocode EN 1991-2"
        )

        assertEquals(CppAssetType.MODULAR_BRIDGE, mesh.assetType)
        assertEquals(4, mesh.vertexCount)
        assertEquals(2, mesh.triangleCount)
        assertEquals(48, mesh.vertices.size)
        assertEquals(6, mesh.indices.size)
        assertTrue(mesh.civilStandard.contains("AASHTO"))
    }

    @Test
    fun testMath3DProjectionForVulkanMesh() {
        val proj = Math3D.project3DTo2D(
            x = 10f, y = 5f, z = 20f,
            yaw = 0.5f, pitch = 0.3f,
            scale = 10f,
            cx = 400f, cy = 300f,
            centerX = 0f, centerY = 0f, centerZ = 0f,
            cameraDist = 100f,
            isPerspective = true
        )
        assertNotNull(proj)
        assertTrue(proj.screenX > 0f)
        assertTrue(proj.screenY > 0f)
    }

    @Test
    fun testBridgeTypeKeyMapping() {
        assertEquals(0, NativeEngineBridge.mapBridgeTypeKeyToIndex("CABLE_STAYED"))
        assertEquals(1, NativeEngineBridge.mapBridgeTypeKeyToIndex("SUSPENSION"))
        assertEquals(2, NativeEngineBridge.mapBridgeTypeKeyToIndex("PEDESTRIAN_FOOTBRIDGE"))
        assertEquals(3, NativeEngineBridge.mapBridgeTypeKeyToIndex("CONCRETE_VIADUCT"))
        assertEquals(4, NativeEngineBridge.mapBridgeTypeKeyToIndex("STEEL_TRUSS"))
        assertEquals(5, NativeEngineBridge.mapBridgeTypeKeyToIndex("PARABOLIC_ARCH"))
        assertEquals(6, NativeEngineBridge.mapBridgeTypeKeyToIndex("EXTRADOSED"))
        assertEquals(7, NativeEngineBridge.mapBridgeTypeKeyToIndex("BASCULE_DRAWBRIDGE"))
        assertEquals(8, NativeEngineBridge.mapBridgeTypeKeyToIndex("CANTILEVER_TRUSS"))
        assertEquals(9, NativeEngineBridge.mapBridgeTypeKeyToIndex("CURVED_CABLE_STAYED"))
        assertEquals(10, NativeEngineBridge.mapBridgeTypeKeyToIndex("ROMAN_VIADUCT"))
        assertEquals(11, NativeEngineBridge.mapBridgeTypeKeyToIndex("POST_TENSIONED_OVERPASS"))
        assertEquals(12, NativeEngineBridge.mapBridgeTypeKeyToIndex("PEDESTRIAN_STRESS_RIBBON"))
        assertEquals(13, NativeEngineBridge.mapBridgeTypeKeyToIndex("PEDESTRIAN_CABLE_STAYED"))
        assertEquals(14, NativeEngineBridge.mapBridgeTypeKeyToIndex("PEDESTRIAN_TIMBER_ARCH"))
        assertEquals(15, NativeEngineBridge.mapBridgeTypeKeyToIndex("STEEL_PLATE_GIRDER"))

        for (i in 0..15) {
            val name = NativeEngineBridge.getBridgeTypeName(i)
            assertTrue("Bridge type $i should have non-empty name", name.isNotEmpty())
        }
    }

    @Test
    fun testNativeBridgeGenerationIfLibraryLoaded() {
        if (!NativeEngineBridge.isLoaded()) {
            // Under JVM test runner without libapex_vulkan.so in java.library.path, graceful skip
            return
        }

        val bridge = NativeEngineBridge.generateModularBridgeMesh(
            bridgeType = "CURVED_CABLE_STAYED",
            spanLengthM = 160f,
            deckWidthM = 24f,
            pylonHeightM = 58f,
            curveBendAngleDeg = 26f,
            superelevationDeg = 4f,
            cablePairsCount = 12,
            hasBarriers = true
        )
        assertTrue(bridge.vertexCount > 0)
        assertTrue(bridge.triangleCount > 0)
        assertEquals(bridge.vertexCount * 12, bridge.vertices.size)
        assertEquals(bridge.triangleCount * 3, bridge.indices.size)
    }
}
