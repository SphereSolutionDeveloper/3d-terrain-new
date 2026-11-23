package com.example

import com.example.engine.Math3D
import com.example.engine.NativeEngineBridge
import com.example.model.CppAssetType
import com.example.model.NativeSourceCatalog
import com.example.model.NativeVulkanMesh
import org.junit.Assert.*
import org.junit.Test
import java.io.File

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
        assertEquals(16, NativeEngineBridge.mapBridgeTypeKeyToIndex("PEDESTRIAN_COVERED_SKYWALK"))

        for (i in 0..16) {
            val name = NativeEngineBridge.getBridgeTypeName(i)
            assertTrue("Bridge type $i should have non-empty name", name.isNotEmpty())
        }
    }

    @Test
    fun testRoadSubsystemTypeKeyMappings() {
        assertEquals(0, NativeEngineBridge.mapRoadNetworkTypeKeyToIndex("SIGNALIZED_4WAY"))
        assertEquals(1, NativeEngineBridge.mapRoadNetworkTypeKeyToIndex("T_JUNCTION_ARTERIAL"))
        assertEquals(2, NativeEngineBridge.mapRoadNetworkTypeKeyToIndex("Y_FORK_BIFURCATION"))
        assertEquals(3, NativeEngineBridge.mapRoadNetworkTypeKeyToIndex("DISTRICT_STREET_GRID"))

        assertEquals(0, NativeEngineBridge.mapSplineCurveTypeKeyToIndex("CLOTHOID_S_CURVE"))
        assertEquals(1, NativeEngineBridge.mapSplineCurveTypeKeyToIndex("HAIRPIN_SWITCHBACK"))
        assertEquals(2, NativeEngineBridge.mapSplineCurveTypeKeyToIndex("CATMULL_ROM_CORRIDOR"))
        assertEquals(3, NativeEngineBridge.mapSplineCurveTypeKeyToIndex("BANKED_SPIRAL_HELIX"))

        assertEquals(0, NativeEngineBridge.mapRoadTileTypeKeyToIndex("CUT_FILL_HILLSIDE"))
        assertEquals(1, NativeEngineBridge.mapRoadTileTypeKeyToIndex("QUADTREE_URBAN_TILE"))
        assertEquals(2, NativeEngineBridge.mapRoadTileTypeKeyToIndex("WET_PUDDLE_STATE"))
        assertEquals(3, NativeEngineBridge.mapRoadTileTypeKeyToIndex("MUDDY_GRAVEL_RUT"))
        assertEquals(4, NativeEngineBridge.mapRoadTileTypeKeyToIndex("POTHOLED_CRACKED_TILE"))
        assertEquals(5, NativeEngineBridge.mapRoadTileTypeKeyToIndex("SNOWY_ICY_CORRIDOR"))
    }

    @Test
    fun testRoadCatalogAndShaderFilesPresentOnDisk() {
        val roadFiles = NativeSourceCatalog.roadFiles
        assertTrue(roadFiles.any { it.name == "RoadNetwork.h" })
        assertTrue(roadFiles.any { it.name == "RoadSplineGraph.h" })
        assertTrue(roadFiles.any { it.name == "RoadTileGen.h" })
        assertTrue(roadFiles.any { it.name.endsWith(".glsl") })
        assertTrue(roadFiles.any { it.name.endsWith(".vert") })
        assertTrue(roadFiles.any { it.name.endsWith(".frag") })
        assertTrue(roadFiles.any { it.name.endsWith(".comp") })

        // Locate repository root dynamically from Gradle CWD (either workspace root or app/)
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val repoRoot = if (File(cwd, "native-assets").exists()) cwd else cwd.parentFile
        val expectedShaders = listOf(
            "native-assets/src/road/shaders/include/road_common.glsl",
            "native-assets/src/road/shaders/include/road_types.glsl",
            "native-assets/src/road/shaders/road_network.vert",
            "native-assets/src/road/shaders/road_network.frag",
            "native-assets/src/road/shaders/road_network.comp",
            "native-assets/src/road/shaders/road_spline.comp",
            "native-assets/src/road/shaders/road_tile.comp",
            "native-assets/src/road/shaders/road_markings_tex.comp",
            "native-assets/src/road/shaders/road_surface_state_tex.comp",
            "native-assets/src/road/asphalt/shaders/asphalt_albedo.comp",
            "native-assets/src/road/asphalt/shaders/asphalt_height.comp",
            "native-assets/src/road/asphalt/shaders/asphalt_normal.comp",
            "native-assets/src/road/asphalt/shaders/asphalt_orm.comp",
            "native-assets/src/road/barriers/shaders/jersey_barrier.comp",
            "native-assets/src/road/barriers/shaders/guardrail.comp",
            "native-assets/src/road/barriers/shaders/bollard.comp",
            "native-assets/src/road/bridges/shaders/bridge_pbr.vert",
            "native-assets/src/road/bridges/shaders/bridge_pbr.frag",
            "native-assets/src/road/bridges/shaders/bridge_steel_tex.comp",
            "native-assets/src/road/bridges/shaders/bridge_concrete_tex.comp",
            "native-assets/src/road/tunnels/shaders/tunnel_lighting.frag",
            "native-assets/src/road/tunnels/shaders/tunnel_lining_tex.comp",
            "native-assets/src/road/tolls/shaders/toll_vms_led.frag",
            "native-assets/src/road/tolls/shaders/toll_canopy_tex.comp",
            "native-assets/src/road/interchanges/shaders/interchange_gore_tex.comp",
            "native-assets/src/road/retaining_walls/shaders/retaining_wall_tex.comp",
            "native-assets/src/road/signage/shaders/retroreflective_sign.frag",
            "native-assets/src/road/signage/shaders/signage_atlas_tex.comp",
            "native-assets/src/road/roadblocks/shaders/hazard_stripe_tex.comp"
        )
        for (relPath in expectedShaders) {
            val f = File(repoRoot, relPath)
            assertTrue("Expected shader file to exist: $relPath", f.exists())
            val lines = f.readLines()
            assertTrue("Shader $relPath should be populated (got ${lines.size} lines)", lines.size >= 25)
            if (!relPath.endsWith(".glsl")) {
                assertTrue("Shader $relPath must declare #version 460", lines.any { it.contains("#version 460") })
            }
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
