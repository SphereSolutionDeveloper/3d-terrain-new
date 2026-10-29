package com.example

import com.example.engine.Procedural3DBuilder
import com.example.model.CppAssetType
import org.junit.Assert.*
import org.junit.Test

class Procedural3DBuilderTest {

    @Test
    fun testBuildGarage001DimensionsAndPrimitives() {
        val mesh = Procedural3DBuilder.buildGarage001(
            bay1OpenPct = 0.0f,
            bay2OpenPct = 0.5f,
            neonSignOn = true
        )

        assertEquals(CppAssetType.GARAGE_001, mesh.assetType)
        assertTrue(mesh.boxes.isNotEmpty())
        // Total vertices is boxes.size * 36
        assertEquals(mesh.boxes.size * 36, mesh.totalVertices)
        assertEquals(mesh.boxes.size * 12, mesh.totalTriangles)

        // Check essential boxes exist for standalone garage house
        val boxNames = mesh.boxes.map { it.id }
        assertTrue(boxNames.contains("floor_slab"))
        assertTrue(boxNames.contains("main_warehouse"))
        assertTrue(boxNames.contains("roof_trim"))
        assertTrue(boxNames.contains("workshop_banner"))
    }

    @Test
    fun testBuildTestVehicleDimensionsAndPrimitives() {
        val mesh = Procedural3DBuilder.buildTestVehicle(
            headlightsOn = true,
            taillightsOn = true
        )

        assertEquals(CppAssetType.TEST_VEHICLE_LORRY, mesh.assetType)
        assertTrue(mesh.boxes.isNotEmpty())
        assertEquals(mesh.boxes.size * 36, mesh.totalVertices)

        // Check key components
        val boxNames = mesh.boxes.map { it.id }
        assertTrue(boxNames.contains("chassis_frame"))
        assertTrue(boxNames.contains("driver_cab"))
        assertTrue(boxNames.contains("cargo_box"))
        assertTrue(boxNames.contains("headlight_left"))
        assertTrue(boxNames.contains("taillight_left"))
        assertTrue(boxNames.contains("wheel_front_left"))
        assertTrue(boxNames.contains("wheel_rear_right"))
    }

    @Test
    fun testBuildPowerGridNetworkCylinderVsBox() {
        val cylinderMesh = Procedural3DBuilder.buildPowerGridNetwork(
            numPosts = 4,
            spanDistance = 16.0f,
            useCylinder = true,
            cylinderSides = 8,
            cableRadiusM = 0.035f,
            tensionKn = 18.5f,
            linearMassKgM = 1.25f
        )

        assertEquals(CppAssetType.POWER_GRID_NETWORK, cylinderMesh.assetType)
        assertTrue(cylinderMesh.wires.isNotEmpty())
        assertTrue(cylinderMesh.wires.first().useCylinder)
        assertEquals(8, cylinderMesh.wires.first().cylinderSides)

        val boxMesh = Procedural3DBuilder.buildPowerGridNetwork(
            numPosts = 4,
            spanDistance = 16.0f,
            useCylinder = false,
            cylinderSides = 8,
            cableRadiusM = 0.035f,
            tensionKn = 18.5f,
            linearMassKgM = 1.25f
        )

        assertFalse(boxMesh.wires.first().useCylinder)
        // In continuous cylinder mode, cables are pure continuous tubular geometry without box blocks
        assertTrue(cylinderMesh.wires.all { it.useCylinder })
        assertTrue(boxMesh.boxes.size > cylinderMesh.boxes.size)

        // Verify post-to-ground guy support cables are present as continuous cylinder wires
        val guyWires = cylinderMesh.wires.filter { it.id.contains("guy_cable") }
        assertTrue(guyWires.isNotEmpty())
        assertTrue(guyWires.all { it.useCylinder })
        assertFalse(guyWires.any { it.isHotPhase }) // Support cables are structural grounding stays, not hot

        // Verify no cables have cyan / blue color (#00E5FF or high blue channel)
        assertTrue(cylinderMesh.wires.none { it.colorHex == "#00E5FF" })
        assertTrue(cylinderMesh.wires.none { it.b > 0.8f && it.r < 0.2f }) // Not cyan/blue
    }

    @Test
    fun testBuildModularPowerPostSupportCableCylinder() {
        val postCylinder = Procedural3DBuilder.buildModularPowerPost(
            postType = "CORNER",
            hasGuyWire = true,
            useCylinder = true,
            cylinderSides = 8
        )

        assertEquals(CppAssetType.MODULAR_POWER_POST, postCylinder.assetType)
        assertTrue(postCylinder.wires.isNotEmpty())
        val guyWire = postCylinder.wires.first()
        assertTrue(guyWire.useCylinder)
        assertEquals("Post-to-Ground Support Stay Cable", guyWire.name)
        // Zero cylinder core or facet blocks in boxes
        assertTrue(postCylinder.boxes.none { it.id.contains("cyl_core") || it.id.contains("cyl_facet") })
    }
}
