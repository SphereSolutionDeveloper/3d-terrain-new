package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.CppAssetType
import com.example.model.ProceduralBoxPart
import com.example.model.ProceduralModelMesh
import com.example.model.ProceduralWireCable
import kotlin.math.max
import kotlin.math.min

object Procedural3DBuilder {

    /**
     * Builds the procedural Garage 001 model matching native C++ ProceduralGarage::buildGarage001()
     * and Technical Reference dimensions.
     */
    /**
     * Builds the procedural Standalone Garage House model matching native C++ Vulkan 1.3 architecture:
     * - Standalone garage house alone (hollow architectural room with floor, walls, and roof)
     * - Modular articulated sectional doors that roll along overhead ceiling tracks
     * - Fully detailed workshop interior visible inside through the open door:
     *   hydraulic 2-post vehicle hoist, mechanic workbench with vise & tool pegboard,
     *   mobile rolling tool cabinet, heavy pallet storage racks, oil barrels, tire rack,
     *   high-bay LED shop light fixtures, and safety zone floor markings.
     */
    fun buildGarage001(
        bay1OpenPct: Float = 0.70f,
        bay2OpenPct: Float = 0.0f,
        neonSignOn: Boolean = true,
        showApron: Boolean = false,
        showGuidelines: Boolean = false,
        showDriveway: Boolean = false
    ): ProceduralModelMesh {
        val boxes = mutableListOf<ProceduralBoxPart>()

        // 1. Concrete Foundation Floor Slab & Safety Painted Bay Zones
        boxes.add(
            ProceduralBoxPart(
                id = "floor_slab",
                name = "Reinforced Concrete Foundation Slab",
                centerX = 0.0f, centerY = 0.10f, centerZ = 0.0f,
                sizeX = 12.4f, sizeY = 0.20f, sizeZ = 16.4f,
                colorHex = "#3A414D",
                r = 0.23f, g = 0.26f, b = 0.30f,
                category = "Foundation"
            )
        )
        // Interior floor painted safety parking stalls
        boxes.add(
            ProceduralBoxPart(
                id = "floor_marking_bay1",
                name = "Bay 1 Vehicle Service Zone Boundary",
                centerX = -0.5f, centerY = 0.21f, centerZ = -2.85f,
                sizeX = 6.0f, sizeY = 0.02f, sizeZ = 3.6f,
                colorHex = "#FFD54F",
                r = 1.0f, g = 0.85f, b = 0.2f,
                category = "Floor"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "floor_marking_bay2",
                name = "Bay 2 Vehicle Parking Zone Boundary",
                centerX = -0.5f, centerY = 0.21f, centerZ = 2.85f,
                sizeX = 6.0f, sizeY = 0.02f, sizeZ = 3.6f,
                colorHex = "#FFD54F",
                r = 1.0f, g = 0.85f, b = 0.2f,
                category = "Floor"
            )
        )

        // 2. Standalone Garage House Hollow Exterior Walls
        // Rear Wall (East, X = +5.9m) - also retains id main_warehouse for test compatibility
        boxes.add(
            ProceduralBoxPart(
                id = "main_warehouse",
                name = "Garage House Rear Structural Wall",
                centerX = 5.9f, centerY = 2.9f, centerZ = 0.0f,
                sizeX = 0.35f, sizeY = 5.4f, sizeZ = 16.0f,
                colorHex = "#384252",
                r = 0.22f, g = 0.26f, b = 0.32f,
                category = "Structure"
            )
        )
        // Left Side Wall (North, Z = +7.9m)
        boxes.add(
            ProceduralBoxPart(
                id = "left_wall",
                name = "Garage House North Wall",
                centerX = 0.0f, centerY = 2.9f, centerZ = 7.9f,
                sizeX = 12.0f, sizeY = 5.4f, sizeZ = 0.35f,
                colorHex = "#384252",
                r = 0.22f, g = 0.26f, b = 0.32f,
                category = "Structure"
            )
        )
        // Right Side Wall (South, Z = -7.9m)
        boxes.add(
            ProceduralBoxPart(
                id = "right_wall",
                name = "Garage House South Wall",
                centerX = 0.0f, centerY = 2.9f, centerZ = -7.9f,
                sizeX = 12.0f, sizeY = 5.4f, sizeZ = 0.35f,
                colorHex = "#384252",
                r = 0.22f, g = 0.26f, b = 0.32f,
                category = "Structure"
            )
        )

        // Front Entrance Portal (West, X = -5.9m) with doorway openings
        boxes.add(
            ProceduralBoxPart(
                id = "front_pillar_left",
                name = "Entrance Portal North Corner Pillar",
                centerX = -5.9f, centerY = 2.9f, centerZ = 6.4f,
                sizeX = 0.35f, sizeY = 5.4f, sizeZ = 3.0f,
                colorHex = "#384252",
                r = 0.22f, g = 0.26f, b = 0.32f,
                category = "Structure"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "front_pillar_right",
                name = "Entrance Portal South Corner Pillar",
                centerX = -5.9f, centerY = 2.9f, centerZ = -6.4f,
                sizeX = 0.35f, sizeY = 5.4f, sizeZ = 3.0f,
                colorHex = "#384252",
                r = 0.22f, g = 0.26f, b = 0.32f,
                category = "Structure"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "front_pillar_center",
                name = "Central Portal Dividing Mullion",
                centerX = -5.9f, centerY = 2.9f, centerZ = 0.0f,
                sizeX = 0.35f, sizeY = 5.4f, sizeZ = 0.8f,
                colorHex = "#455A64",
                r = 0.27f, g = 0.35f, b = 0.39f,
                category = "Structure"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "front_lintel_header",
                name = "Overhead Transom Lintel Wall",
                centerX = -5.9f, centerY = 4.85f, centerZ = 0.0f,
                sizeX = 0.35f, sizeY = 1.5f, sizeZ = 16.0f,
                colorHex = "#384252",
                r = 0.22f, g = 0.26f, b = 0.32f,
                category = "Structure"
            )
        )

        // 3. Roof & Overhead Structural Ceiling Steel Trusses
        boxes.add(
            ProceduralBoxPart(
                id = "roof_slab",
                name = "Overhead Weather Roof Slab",
                centerX = 0.0f, centerY = 5.65f, centerZ = 0.0f,
                sizeX = 12.6f, sizeY = 0.25f, sizeZ = 16.6f,
                colorHex = "#1E242C",
                r = 0.12f, g = 0.14f, b = 0.17f,
                category = "Roof"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "roof_trim",
                name = "Crimson Architectural Roof Trim",
                centerX = 0.0f, centerY = 5.85f, centerZ = 0.0f,
                sizeX = 13.0f, sizeY = 0.20f, sizeZ = 17.0f,
                colorHex = "#D93838",
                r = 0.85f, g = 0.22f, b = 0.22f,
                category = "Roof"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "roof_truss_1",
                name = "Structural Steel Ceiling I-Beam West",
                centerX = -2.0f, centerY = 5.35f, centerZ = 0.0f,
                sizeX = 0.25f, sizeY = 0.35f, sizeZ = 15.4f,
                colorHex = "#455A64",
                r = 0.27f, g = 0.35f, b = 0.39f,
                category = "Roof"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "roof_truss_2",
                name = "Structural Steel Ceiling I-Beam East",
                centerX = 2.0f, centerY = 5.35f, centerZ = 0.0f,
                sizeX = 0.25f, sizeY = 0.35f, sizeZ = 15.4f,
                colorHex = "#455A64",
                r = 0.27f, g = 0.35f, b = 0.39f,
                category = "Roof"
            )
        )

        // Workshop Facade Signboard
        val bannerHex = if (neonSignOn) "#FFA726" else "#455A64"
        val bannerR = if (neonSignOn) 0.95f else 0.27f
        val bannerG = if (neonSignOn) 0.65f else 0.35f
        val bannerB = if (neonSignOn) 0.18f else 0.39f
        boxes.add(
            ProceduralBoxPart(
                id = "workshop_banner",
                name = "Workshop Facade Signboard",
                centerX = -6.15f, centerY = 4.95f, centerZ = 0.0f,
                sizeX = 0.12f, sizeY = 0.85f, sizeZ = 8.0f,
                colorHex = bannerHex,
                r = bannerR, g = bannerG, b = bannerB,
                category = "Signage"
            )
        )

        // 4. Modular Articulated Sectional Doors & Hardware Tracks
        // Bay 1 Hardware (Z = -2.85m)
        boxes.add(
            ProceduralBoxPart(
                id = "door_track_left_bay1",
                name = "Bay 1 Vertical Guide Track (Left)",
                centerX = -5.75f, centerY = 2.05f, centerZ = -4.95f,
                sizeX = 0.08f, sizeY = 3.9f, sizeZ = 0.08f,
                colorHex = "#78909C",
                r = 0.47f, g = 0.56f, b = 0.61f,
                category = "Doors"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "door_track_right_bay1",
                name = "Bay 1 Vertical Guide Track (Right)",
                centerX = -5.75f, centerY = 2.05f, centerZ = -0.75f,
                sizeX = 0.08f, sizeY = 3.9f, sizeZ = 0.08f,
                colorHex = "#78909C",
                r = 0.47f, g = 0.56f, b = 0.61f,
                category = "Doors"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "door_track_overhead_left_bay1",
                name = "Bay 1 Overhead Ceiling Track (Left)",
                centerX = -3.2f, centerY = 4.05f, centerZ = -4.95f,
                sizeX = 5.1f, sizeY = 0.08f, sizeZ = 0.08f,
                colorHex = "#78909C",
                r = 0.47f, g = 0.56f, b = 0.61f,
                category = "Doors"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "door_track_overhead_right_bay1",
                name = "Bay 1 Overhead Ceiling Track (Right)",
                centerX = -3.2f, centerY = 4.05f, centerZ = -0.75f,
                sizeX = 5.1f, sizeY = 0.08f, sizeZ = 0.08f,
                colorHex = "#78909C",
                r = 0.47f, g = 0.56f, b = 0.61f,
                category = "Doors"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "door_torsion_spring_bay1",
                name = "Bay 1 Torsion Spring Counterbalance Shaft",
                centerX = -5.7f, centerY = 4.2f, centerZ = -2.85f,
                sizeX = 0.14f, sizeY = 0.14f, sizeZ = 4.1f,
                colorHex = "#37474F",
                r = 0.22f, g = 0.28f, b = 0.31f,
                category = "Doors"
            )
        )

        // Bay 1 Modular Articulated Door Panels (Sliding up into ceiling tracks when open)
        val travel1 = bay1OpenPct.coerceIn(0.0f, 1.0f) * 3.8f
        val panelNames = listOf(
            "Threshold Weather-Seal Module",
            "Lower Structural Lock Module",
            "Glazed Vision Lite Window Module",
            "Top Articulated Header Module"
        )
        val panelColors = listOf(
            "#90A4AE", // Base bottom
            "#B0BEC5", // Lower
            "#B0BEC5", // Vision
            "#B0BEC5"  // Header
        )

        for (k in 1..4) {
            val baseY = 0.65f + (k - 1) * 0.95f
            val curPos = baseY + travel1
            val panelId = if (k == 1) "bay_door_1" else "bay_door_1_panel_$k"

            if (curPos <= 3.8f) {
                // Vertical orientation in doorway
                boxes.add(
                    ProceduralBoxPart(
                        id = panelId,
                        name = "Bay 1 Modular Door - ${panelNames[k - 1]}",
                        centerX = -5.85f, centerY = curPos, centerZ = -2.85f,
                        sizeX = 0.10f, sizeY = 0.88f, sizeZ = 4.1f,
                        colorHex = panelColors[k - 1],
                        r = 0.70f, g = 0.74f, b = 0.78f,
                        category = "Doors"
                    )
                )
            } else {
                // Articulated onto overhead ceiling tracks inside the garage
                val overshoot = curPos - 3.8f
                boxes.add(
                    ProceduralBoxPart(
                        id = panelId,
                        name = "Bay 1 Modular Door (Overhead) - ${panelNames[k - 1]}",
                        centerX = -5.85f + overshoot, centerY = 4.12f, centerZ = -2.85f,
                        sizeX = 0.88f, sizeY = 0.10f, sizeZ = 4.1f,
                        colorHex = panelColors[k - 1],
                        r = 0.65f, g = 0.70f, b = 0.75f,
                        category = "Doors"
                    )
                )
            }
        }

        // Bay 2 Modular Door Assembly (Z = +2.85m)
        val travel2 = bay2OpenPct.coerceIn(0.0f, 1.0f) * 3.8f
        for (k in 1..4) {
            val baseY = 0.65f + (k - 1) * 0.95f
            val curPos = baseY + travel2
            val panelId = if (k == 1) "bay_door_2" else "bay_door_2_panel_$k"

            if (curPos <= 3.8f) {
                boxes.add(
                    ProceduralBoxPart(
                        id = panelId,
                        name = "Bay 2 Modular Door - ${panelNames[k - 1]}",
                        centerX = -5.85f, centerY = curPos, centerZ = 2.85f,
                        sizeX = 0.10f, sizeY = 0.88f, sizeZ = 4.1f,
                        colorHex = panelColors[k - 1],
                        r = 0.70f, g = 0.74f, b = 0.78f,
                        category = "Doors"
                    )
                )
            } else {
                val overshoot = curPos - 3.8f
                boxes.add(
                    ProceduralBoxPart(
                        id = panelId,
                        name = "Bay 2 Modular Door (Overhead) - ${panelNames[k - 1]}",
                        centerX = -5.85f + overshoot, centerY = 4.12f, centerZ = 2.85f,
                        sizeX = 0.88f, sizeY = 0.10f, sizeZ = 4.1f,
                        colorHex = panelColors[k - 1],
                        r = 0.65f, g = 0.70f, b = 0.75f,
                        category = "Doors"
                    )
                )
            }
        }

        // 5. DETAILED INTERIOR WORKSHOP (VISIBLE INSIDE THROUGH OPEN DOOR!)
        // A. Hydraulic 2-Post Vehicle Hoist / Lift in Bay 1 (Z = -2.85m)
        boxes.add(
            ProceduralBoxPart(
                id = "lift_post_left",
                name = "Hydraulic 2-Post Hoist Left Column",
                centerX = -0.5f, centerY = 2.1f, centerZ = -4.3f,
                sizeX = 0.35f, sizeY = 4.0f, sizeZ = 0.35f,
                colorHex = "#1565C0",
                r = 0.08f, g = 0.40f, b = 0.75f,
                category = "Interior - Vehicle Lift"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "lift_post_right",
                name = "Hydraulic 2-Post Hoist Right Column",
                centerX = -0.5f, centerY = 2.1f, centerZ = -1.4f,
                sizeX = 0.35f, sizeY = 4.0f, sizeZ = 0.35f,
                colorHex = "#1565C0",
                r = 0.08f, g = 0.40f, b = 0.75f,
                category = "Interior - Vehicle Lift"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "lift_crossbeam",
                name = "Overhead Hydraulic Cable Equalizer Beam",
                centerX = -0.5f, centerY = 4.15f, centerZ = -2.85f,
                sizeX = 0.25f, sizeY = 0.20f, sizeZ = 3.1f,
                colorHex = "#0D47A1",
                r = 0.05f, g = 0.28f, b = 0.63f,
                category = "Interior - Vehicle Lift"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "lift_carriages_and_arms",
                name = "Dual Swing Lifting Arms & Hoist Carriage",
                centerX = -0.5f, centerY = 0.95f, centerZ = -2.85f,
                sizeX = 2.4f, sizeY = 0.16f, sizeZ = 1.8f,
                colorHex = "#FFB300",
                r = 1.0f, g = 0.70f, b = 0.0f,
                category = "Interior - Vehicle Lift"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "lift_power_unit",
                name = "Electro-Hydraulic Pump & Fluid Reservoir",
                centerX = -0.5f, centerY = 2.0f, centerZ = -4.55f,
                sizeX = 0.22f, sizeY = 0.75f, sizeZ = 0.22f,
                colorHex = "#212121",
                r = 0.13f, g = 0.13f, b = 0.13f,
                category = "Interior - Vehicle Lift"
            )
        )

        // B. Mechanic's Heavy Industrial Workbench & Tool Pegboard (Rear Wall, X = +4.8m, Z = -2.85m)
        boxes.add(
            ProceduralBoxPart(
                id = "workbench_base",
                name = "Heavy Industrial Workbench Steel Cabinet",
                centerX = 4.8f, centerY = 0.55f, centerZ = -2.85f,
                sizeX = 1.2f, sizeY = 0.9f, sizeZ = 3.6f,
                colorHex = "#263238",
                r = 0.15f, g = 0.20f, b = 0.22f,
                category = "Interior - Workbench"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "workbench_top",
                name = "Solid Hardwood Work Surface Top",
                centerX = 4.8f, centerY = 1.05f, centerZ = -2.85f,
                sizeX = 1.35f, sizeY = 0.10f, sizeZ = 3.8f,
                colorHex = "#8D6E63",
                r = 0.55f, g = 0.43f, b = 0.39f,
                category = "Interior - Workbench"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "tool_pegboard",
                name = "Perforated Tool Mounting Pegboard Back",
                centerX = 5.6f, centerY = 1.95f, centerZ = -2.85f,
                sizeX = 0.08f, sizeY = 1.6f, sizeZ = 3.6f,
                colorHex = "#607D8B",
                r = 0.38f, g = 0.49f, b = 0.55f,
                category = "Interior - Workbench"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "bench_vise",
                name = "Heavy Cast Iron Mechanic Bench Vise",
                centerX = 4.2f, centerY = 1.22f, centerZ = -4.2f,
                sizeX = 0.25f, sizeY = 0.25f, sizeZ = 0.25f,
                colorHex = "#C62828",
                r = 0.78f, g = 0.16f, b = 0.16f,
                category = "Interior - Workbench"
            )
        )

        // C. Rolling Multi-Drawer Tool Cabinet (Red Tool Chest, X = +4.5m, Z = 0.0m)
        boxes.add(
            ProceduralBoxPart(
                id = "tool_chest_body",
                name = "Mobile 8-Drawer Industrial Tool Cabinet",
                centerX = 4.5f, centerY = 0.75f, centerZ = 0.0f,
                sizeX = 0.9f, sizeY = 1.3f, sizeZ = 1.1f,
                colorHex = "#D32F2F",
                r = 0.83f, g = 0.18f, b = 0.18f,
                category = "Interior - Tool Storage"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "tool_chest_handle_trim",
                name = "Chrome Drawer Handles & Edge Bumpers",
                centerX = 4.0f, centerY = 0.75f, centerZ = 0.0f,
                sizeX = 0.06f, sizeY = 1.2f, sizeZ = 0.9f,
                colorHex = "#CFD8DC",
                r = 0.81f, g = 0.85f, b = 0.86f,
                category = "Interior - Tool Storage"
            )
        )

        // D. Heavy Pallet Storage Racking & Oil Drums (North Wall, Z = +7.1m)
        boxes.add(
            ProceduralBoxPart(
                id = "storage_rack_frame",
                name = "Heavy-Duty Steel Pallet Racking Frame",
                centerX = 1.5f, centerY = 1.8f, centerZ = 7.1f,
                sizeX = 4.5f, sizeY = 3.4f, sizeZ = 0.75f,
                colorHex = "#455A64",
                r = 0.27f, g = 0.35f, b = 0.39f,
                category = "Interior - Storage"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "storage_rack_shelves",
                name = "Orange High-Load Deck Shelves",
                centerX = 1.5f, centerY = 1.3f, centerZ = 7.1f,
                sizeX = 4.4f, sizeY = 0.08f, sizeZ = 0.7f,
                colorHex = "#EF6C00",
                r = 0.94f, g = 0.42f, b = 0.0f,
                category = "Interior - Storage"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "storage_battery_crates",
                name = "Heavy Industrial Battery Storage Crates",
                centerX = 2.0f, centerY = 1.6f, centerZ = 7.1f,
                sizeX = 1.4f, sizeY = 0.5f, sizeZ = 0.6f,
                colorHex = "#37474F",
                r = 0.22f, g = 0.28f, b = 0.31f,
                category = "Interior - Storage"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "oil_drums_stack",
                name = "55-Gallon Hydraulic Lubricant Drums",
                centerX = -2.5f, centerY = 0.65f, centerZ = 7.1f,
                sizeX = 0.75f, sizeY = 1.1f, sizeZ = 0.75f,
                colorHex = "#0D47A1",
                r = 0.05f, g = 0.28f, b = 0.63f,
                category = "Interior - Storage"
            )
        )

        // E. Wall-Mounted Tire Storage Rack (South Wall, Z = -7.2m)
        boxes.add(
            ProceduralBoxPart(
                id = "tire_storage_rack",
                name = "Heavy Automotive Wheel & Tire Rack",
                centerX = 2.0f, centerY = 2.4f, centerZ = -7.2f,
                sizeX = 3.0f, sizeY = 0.5f, sizeZ = 0.65f,
                colorHex = "#37474F",
                r = 0.22f, g = 0.28f, b = 0.31f,
                category = "Interior - Tire Storage"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "tire_set_rubber",
                name = "High-Performance Testing Tire Stack",
                centerX = 2.0f, centerY = 2.5f, centerZ = -7.2f,
                sizeX = 2.8f, sizeY = 0.75f, sizeZ = 0.6f,
                colorHex = "#212121",
                r = 0.13f, g = 0.13f, b = 0.13f,
                category = "Interior - Tire Storage"
            )
        )

        // F. Overhead High-Bay LED Shop Light Fixtures (Ceiling)
        boxes.add(
            ProceduralBoxPart(
                id = "shop_light_bay1",
                name = "High-Bay LED Shop Light Luminaire (Bay 1)",
                centerX = -0.5f, centerY = 5.25f, centerZ = -2.85f,
                sizeX = 0.5f, sizeY = 0.12f, sizeZ = 2.6f,
                colorHex = "#FFF9C4",
                r = 1.0f, g = 0.98f, b = 0.77f,
                category = "Interior - Shop Lighting"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "shop_light_bay2",
                name = "High-Bay LED Shop Light Luminaire (Bay 2)",
                centerX = -0.5f, centerY = 5.25f, centerZ = 2.85f,
                sizeX = 0.5f, sizeY = 0.12f, sizeZ = 2.6f,
                colorHex = "#FFF9C4",
                r = 1.0f, g = 0.98f, b = 0.77f,
                category = "Interior - Shop Lighting"
            )
        )

        // 6. Optional External Apron & Driveway (Only included when explicitly requested)
        if (showApron) {
            boxes.add(
                ProceduralBoxPart(
                    id = "service_apron",
                    name = "Paved Service Apron",
                    centerX = -13.0f, centerY = 0.05f, centerZ = 0.0f,
                    sizeX = 14.0f, sizeY = 0.10f, sizeZ = 16.4f,
                    colorHex = "#2C3238",
                    r = 0.20f, g = 0.22f, b = 0.25f,
                    category = "Ground"
                )
            )
        }
        if (showGuidelines) {
            boxes.add(
                ProceduralBoxPart(
                    id = "guideline_bay_1",
                    name = "Bay 1 Safety Guideline",
                    centerX = -10.0f, centerY = 0.11f, centerZ = -2.85f,
                    sizeX = 8.0f, sizeY = 0.02f, sizeZ = 0.22f,
                    colorHex = "#FFD54F",
                    r = 1.0f, g = 0.85f, b = 0.2f,
                    category = "Ground"
                )
            )
            boxes.add(
                ProceduralBoxPart(
                    id = "guideline_bay_2",
                    name = "Bay 2 Safety Guideline",
                    centerX = -10.0f, centerY = 0.11f, centerZ = 2.85f,
                    sizeX = 8.0f, sizeY = 0.02f, sizeZ = 0.22f,
                    colorHex = "#FFD54F",
                    r = 1.0f, g = 0.85f, b = 0.2f,
                    category = "Ground"
                )
            )
        }
        if (showDriveway) {
            boxes.add(
                ProceduralBoxPart(
                    id = "connecting_driveway",
                    name = "Connecting Driveway",
                    centerX = -26.0f, centerY = 0.06f, centerZ = -6.0f,
                    sizeX = 14.0f, sizeY = 0.10f, sizeZ = 10.0f,
                    colorHex = "#262C33",
                    r = 0.18f, g = 0.20f, b = 0.24f,
                    category = "Ground"
                )
            )
        }

        return createMeshModel(CppAssetType.GARAGE_001, boxes)
    }

    /**
     * Builds the procedural Proving Ground Box Lorry model matching native C++
     * ProceduralVehicle::buildTestLorry() and Technical Reference dimensions.
     */
    fun buildTestVehicle(
        headlightsOn: Boolean = true,
        taillightsOn: Boolean = true,
        steerAngleDeg: Float = 0.0f
    ): ProceduralModelMesh {
        val boxes = mutableListOf<ProceduralBoxPart>()

        // 1. Heavy Chassis Frame: 5.8m x 0.4m x 2.2m centered at Y = 0.65m
        boxes.add(
            ProceduralBoxPart(
                id = "chassis_frame",
                name = "Heavy Dual-Rail Chassis Frame",
                centerX = 0.0f, centerY = 0.65f, centerZ = 0.0f,
                sizeX = 5.8f, sizeY = 0.4f, sizeZ = 2.2f,
                colorHex = "#1F242E",
                r = 0.12f, g = 0.14f, b = 0.18f,
                category = "Chassis"
            )
        )

        // 2. 4 Heavy-Duty Wheels: Dia 0.9m, width 0.45m at (+-1.8m, +-1.15m)
        // Rear Left
        boxes.add(
            ProceduralBoxPart(
                id = "wheel_rear_left",
                name = "Rear Left Heavy Wheel",
                centerX = -1.8f, centerY = 0.45f, centerZ = -1.15f,
                sizeX = 0.9f, sizeY = 0.9f, sizeZ = 0.45f,
                colorHex = "#141414",
                r = 0.08f, g = 0.08f, b = 0.08f,
                category = "Wheels"
            )
        )
        // Rear Right
        boxes.add(
            ProceduralBoxPart(
                id = "wheel_rear_right",
                name = "Rear Right Heavy Wheel",
                centerX = -1.8f, centerY = 0.45f, centerZ = 1.15f,
                sizeX = 0.9f, sizeY = 0.9f, sizeZ = 0.45f,
                colorHex = "#141414",
                r = 0.08f, g = 0.08f, b = 0.08f,
                category = "Wheels"
            )
        )
        // Front Left
        boxes.add(
            ProceduralBoxPart(
                id = "wheel_front_left",
                name = "Front Left Steering Wheel",
                centerX = 1.8f, centerY = 0.45f, centerZ = -1.15f,
                sizeX = 0.9f, sizeY = 0.9f, sizeZ = 0.45f,
                colorHex = "#141414",
                r = 0.08f, g = 0.08f, b = 0.08f,
                category = "Wheels"
            )
        )
        // Front Right
        boxes.add(
            ProceduralBoxPart(
                id = "wheel_front_right",
                name = "Front Right Steering Wheel",
                centerX = 1.8f, centerY = 0.45f, centerZ = 1.15f,
                sizeX = 0.9f, sizeY = 0.9f, sizeZ = 0.45f,
                colorHex = "#141414",
                r = 0.08f, g = 0.08f, b = 0.08f,
                category = "Wheels"
            )
        )

        // 3. Driver's Cab: 1.8m x 1.8m x 2.1m at front X = 1.8m, Electric Cyan #00BCD4
        boxes.add(
            ProceduralBoxPart(
                id = "driver_cab",
                name = "Driver Cab (Electric Cyan)",
                centerX = 1.8f, centerY = 1.75f, centerZ = 0.0f,
                sizeX = 1.8f, sizeY = 1.8f, sizeZ = 2.1f,
                colorHex = "#00BCD4",
                r = 0.0f, g = 0.74f, b = 0.83f,
                category = "Cab"
            )
        )

        // 4. Dark Tint Glass Windshield & Side Windows
        // Front Windshield
        boxes.add(
            ProceduralBoxPart(
                id = "windshield",
                name = "Tint Glass Windshield",
                centerX = 2.72f, centerY = 1.95f, centerZ = 0.0f,
                sizeX = 0.05f, sizeY = 0.9f, sizeZ = 1.8f,
                colorHex = "#1F2E47",
                r = 0.12f, g = 0.18f, b = 0.28f,
                category = "Cab"
            )
        )
        // Left Side Window
        boxes.add(
            ProceduralBoxPart(
                id = "window_left",
                name = "Left Cab Window",
                centerX = 1.8f, centerY = 1.95f, centerZ = -1.06f,
                sizeX = 1.0f, sizeY = 0.8f, sizeZ = 0.05f,
                colorHex = "#1F2E47",
                r = 0.12f, g = 0.18f, b = 0.28f,
                category = "Cab"
            )
        )
        // Right Side Window
        boxes.add(
            ProceduralBoxPart(
                id = "window_right",
                name = "Right Cab Window",
                centerX = 1.8f, centerY = 1.95f, centerZ = 1.06f,
                sizeX = 1.0f, sizeY = 0.8f, sizeZ = 0.05f,
                colorHex = "#1F2E47",
                r = 0.12f, g = 0.18f, b = 0.28f,
                category = "Cab"
            )
        )

        // 5. Freight Cargo Box: 3.8m x 2.4m x 2.3m centered at X = -0.9m, Clean Silver White #EBF0F5
        boxes.add(
            ProceduralBoxPart(
                id = "cargo_box",
                name = "Freight Cargo Container",
                centerX = -0.9f, centerY = 2.1f, centerZ = 0.0f,
                sizeX = 3.8f, sizeY = 2.4f, sizeZ = 2.3f,
                colorHex = "#EBF0F5",
                r = 0.92f, g = 0.94f, b = 0.96f,
                category = "Cargo"
            )
        )

        // 6. Livery Flanking Racing Stripes: Left and Right Cyan Stripes
        boxes.add(
            ProceduralBoxPart(
                id = "stripe_left",
                name = "Livery Cyan Accent Stripe (Left)",
                centerX = -0.9f, centerY = 1.8f, centerZ = -1.16f,
                sizeX = 3.82f, sizeY = 0.4f, sizeZ = 0.02f,
                colorHex = "#00BCD4",
                r = 0.0f, g = 0.74f, b = 0.83f,
                category = "Cargo"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "stripe_right",
                name = "Livery Cyan Accent Stripe (Right)",
                centerX = -0.9f, centerY = 1.8f, centerZ = 1.16f,
                sizeX = 3.82f, sizeY = 0.4f, sizeZ = 0.02f,
                colorHex = "#00BCD4",
                r = 0.0f, g = 0.74f, b = 0.83f,
                category = "Cargo"
            )
        )

        // 7. Dual Headlights: Amber #FFE033 at front bumper
        val hlColor = if (headlightsOn) "#FFE033" else "#554411"
        val hlR = if (headlightsOn) 1.0f else 0.4f
        val hlG = if (headlightsOn) 0.88f else 0.35f
        val hlB = if (headlightsOn) 0.2f else 0.1f
        boxes.add(
            ProceduralBoxPart(
                id = "headlight_left",
                name = "Amber Front Headlight (Left)",
                centerX = 2.72f, centerY = 1.1f, centerZ = -0.75f,
                sizeX = 0.05f, sizeY = 0.3f, sizeZ = 0.4f,
                colorHex = hlColor,
                r = hlR, g = hlG, b = hlB,
                category = "Lighting"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "headlight_right",
                name = "Amber Front Headlight (Right)",
                centerX = 2.72f, centerY = 1.1f, centerZ = 0.75f,
                sizeX = 0.05f, sizeY = 0.3f, sizeZ = 0.4f,
                colorHex = hlColor,
                r = hlR, g = hlG, b = hlB,
                category = "Lighting"
            )
        )

        // 8. Dual Taillights: Crimson #E61A1A at rear bumper
        val tlColor = if (taillightsOn) "#E61A1A" else "#441111"
        val tlR = if (taillightsOn) 0.9f else 0.3f
        val tlG = if (taillightsOn) 0.1f else 0.05f
        val tlB = if (taillightsOn) 0.1f else 0.05f
        boxes.add(
            ProceduralBoxPart(
                id = "taillight_left",
                name = "Crimson Rear Taillight (Left)",
                centerX = -2.82f, centerY = 1.0f, centerZ = -0.8f,
                sizeX = 0.05f, sizeY = 0.25f, sizeZ = 0.35f,
                colorHex = tlColor,
                r = tlR, g = tlG, b = tlB,
                category = "Lighting"
            )
        )
        boxes.add(
            ProceduralBoxPart(
                id = "taillight_right",
                name = "Crimson Rear Taillight (Right)",
                centerX = -2.82f, centerY = 1.0f, centerZ = 0.8f,
                sizeX = 0.05f, sizeY = 0.25f, sizeZ = 0.35f,
                colorHex = tlColor,
                r = tlR, g = tlG, b = tlB,
                category = "Lighting"
            )
        )

        return createMeshModel(CppAssetType.TEST_VEHICLE_LORRY, boxes)
    }

    /**
     * Helper to extrude a multi-segment catenary sagging cable between two 3D attachment points
     * using the exact hyperbolic cosine catenary physics equilibrium equation:
     * y(u) = a * [cosh(L/2a) - cosh(u/a)] where a = T_H / (w * g)
     */
    private fun addCatenaryWireBoxes(
        boxes: MutableList<ProceduralBoxPart>,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        sag: Float,
        segments: Int = 10,
        thickness: Float = 0.035f,
        colorHex: String = "#37474F",
        r: Float = 0.22f, g: Float = 0.25f, b: Float = 0.30f,
        idPrefix: String = "wire",
        useCylinder: Boolean = true,
        cylinderSides: Int = 8
    ) {
        val spanLen = kotlin.math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1)).coerceAtLeast(0.1f)
        val a = if (sag > 0.005f) (spanLen * spanLen) / (8.0f * sag) else 2000.0f
        var prevX = x1
        var prevY = y1
        var prevZ = z1

        val radius = thickness * 0.5f

        for (i in 1..segments) {
            val t = i.toFloat() / segments.toFloat()
            val curX = x1 + t * (x2 - x1)
            val curZ = z1 + t * (z2 - z1)

            // Exact hyperbolic cosine catenary calculation: a * (cosh(L/2a) - cosh(u/a))
            val u = (t - 0.5f) * spanLen
            val sagOffset = if (a > 10.0f && a < 50000.0f) {
                val coshHalfSpan = kotlin.math.cosh((spanLen / (2.0f * a)).toDouble()).toFloat()
                val coshU = kotlin.math.cosh((u / a).toDouble()).toFloat()
                (a * (coshHalfSpan - coshU)).coerceAtLeast(0.0f)
            } else {
                4.0f * sag * t * (1.0f - t)
            }
            val curY = y1 + t * (y2 - y1) - sagOffset

            val midX = (prevX + curX) * 0.5f
            val midY = (prevY + curY) * 0.5f
            val midZ = (prevZ + curZ) * 0.5f

            val dx = kotlin.math.abs(curX - prevX)
            val dy = kotlin.math.abs(curY - prevY)
            val dz = kotlin.math.abs(curZ - prevZ)

            val sx = if (dx > 0.02f) dx else thickness
            val sy = if (dy > 0.02f) dy else thickness
            val sz = if (dz > 0.02f) dz else thickness

            if (useCylinder) {
                // Cylindrical tube segment: central tubular core prism
                boxes.add(
                    ProceduralBoxPart(
                        id = "${idPrefix}_cyl_core_$i",
                        name = "Catenary Cylinder Tube $i Core (r=${(radius * 1000).toInt()}mm)",
                        centerX = midX, centerY = midY, centerZ = midZ,
                        sizeX = sx, sizeY = sy, sizeZ = sz,
                        colorHex = colorHex,
                        r = r, g = g, b = b,
                        category = "Cylindrical Swept Tube"
                    )
                )

                // Radial envelope facets for 6, 8, or 12 facet cylinder circumference
                val numFacets = if (cylinderSides >= 12) 6 else if (cylinderSides >= 8) 4 else 2
                val facetSize = radius * 0.65f
                for (f in 0 until numFacets) {
                    val angle = (Math.PI * f / numFacets).toFloat()
                    val cosF = kotlin.math.cos(angle)
                    val sinF = kotlin.math.sin(angle)
                    val offY = sinF * radius * 0.40f
                    val offZ = cosF * radius * 0.40f

                    boxes.add(
                        ProceduralBoxPart(
                            id = "${idPrefix}_cyl_facet_${i}_$f",
                            name = "Cylinder Radial Facet $f (Segment $i)",
                            centerX = midX, centerY = midY + offY, centerZ = midZ + offZ,
                            sizeX = if (dx > 0.02f) dx else facetSize,
                            sizeY = facetSize,
                            sizeZ = facetSize,
                            colorHex = colorHex,
                            r = (r * 1.12f).coerceAtMost(1f),
                            g = (g * 1.12f).coerceAtMost(1f),
                            b = (b * 1.12f).coerceAtMost(1f),
                            category = "Cylinder Facet Surface"
                        )
                    )
                }
            } else {
                // Box Mode: coarse single rectangular cuboid chord
                boxes.add(
                    ProceduralBoxPart(
                        id = "${idPrefix}_seg_$i",
                        name = "Catenary Box Segment $i (Cuboid Chord)",
                        centerX = midX, centerY = midY, centerZ = midZ,
                        sizeX = sx, sizeY = sy, sizeZ = sz,
                        colorHex = colorHex,
                        r = r, g = g, b = b,
                        category = "Box Cable"
                    )
                )
            }

            prevX = curX
            prevY = curY
            prevZ = curZ
        }
    }

    /**
     * Adds procedural components for an individual modular electric post at a given 3D origin and yaw angle.
     */
    private fun addSinglePostBoxes(
        boxes: MutableList<ProceduralBoxPart>,
        originX: Float = 0.0f,
        originY: Float = 0.0f,
        originZ: Float = 0.0f,
        yawRad: Float = 0.0f,
        postType: String = "CORNER", // "TANGENT", "CORNER", "TRANSFORMER", "DEAD_END"
        hasTransformer: Boolean = false,
        hasStreetlight: Boolean = true,
        isStreetlightOn: Boolean = true,
        hasGuyWire: Boolean = true,
        crossarmWidth: Float = 2.4f,
        idPrefix: String = "post",
        wires: MutableList<ProceduralWireCable>? = null,
        useCylinder: Boolean = true,
        cylinderSides: Int = 8
    ) {
        val cosA = kotlin.math.cos(yawRad)
        val sinA = kotlin.math.sin(yawRad)

        fun rotatePoint(lx: Float, lz: Float): Pair<Float, Float> {
            val rx = originX + (lx * cosA - lz * sinA)
            val rz = originZ + (lx * sinA + lz * cosA)
            return Pair(rx, rz)
        }

        // 1. Concrete Ground Pier Foundation (0.65m x 0.45m x 0.65m)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_foundation",
                name = "Reinforced Concrete Foundation Pier",
                centerX = originX, centerY = originY + 0.22f, centerZ = originZ,
                sizeX = 0.65f, sizeY = 0.45f, sizeZ = 0.65f,
                colorHex = "#8A929A",
                r = 0.54f, g = 0.57f, b = 0.60f,
                category = "Foundation"
            )
        )

        // 2. Vertical Tapered Mast (Treated Timber / Pre-stressed Concrete, 10.5m total height)
        // Lower base mast section (Ground to 4.0m)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_mast_lower",
                name = "Treated Utility Mast Base",
                centerX = originX, centerY = originY + 2.2f, centerZ = originZ,
                sizeX = 0.36f, sizeY = 4.0f, sizeZ = 0.36f,
                colorHex = "#6D4C41",
                r = 0.44f, g = 0.35f, b = 0.28f,
                category = "Mast"
            )
        )
        // Mid mast section (4.0m to 7.5m)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_mast_mid",
                name = "Treated Utility Mast Middle",
                centerX = originX, centerY = originY + 5.75f, centerZ = originZ,
                sizeX = 0.30f, sizeY = 3.5f, sizeZ = 0.30f,
                colorHex = "#795548",
                r = 0.48f, g = 0.38f, b = 0.30f,
                category = "Mast"
            )
        )
        // Upper mast section (7.5m to 10.5m)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_mast_top",
                name = "Treated Utility Mast Top",
                centerX = originX, centerY = originY + 9.0f, centerZ = originZ,
                sizeX = 0.24f, sizeY = 3.0f, sizeZ = 0.24f,
                colorHex = "#8D6E63",
                r = 0.52f, g = 0.42f, b = 0.34f,
                category = "Mast"
            )
        )
        // Steel Weather Cap
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_mast_cap",
                name = "Galvanized Steel Weather Cap",
                centerX = originX, centerY = originY + 10.55f, centerZ = originZ,
                sizeX = 0.28f, sizeY = 0.10f, sizeZ = 0.28f,
                colorHex = "#546E7A",
                r = 0.33f, g = 0.43f, b = 0.48f,
                category = "Mast"
            )
        )

        // Grounding Conductor Cable running down mast
        val (gndX, gndZ) = rotatePoint(0.14f, 0.0f)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_ground_wire",
                name = "Earth Grounding Conductor",
                centerX = gndX, centerY = originY + 5.25f, centerZ = gndZ,
                sizeX = 0.02f, sizeY = 10.2f, sizeZ = 0.02f,
                colorHex = "#CFD8DC",
                r = 0.70f, g = 0.72f, b = 0.75f,
                category = "Grounding"
            )
        )

        // Lineman Climbing Foot Rungs
        for (step in 0..5) {
            val stepY = originY + 3.0f + step * 1.1f
            val side = if (step % 2 == 0) 0.18f else -0.18f
            val (rx, rz) = rotatePoint(side, 0.0f)
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_step_$step",
                    name = "Lineman Climbing Step $step",
                    centerX = rx, centerY = stepY, centerZ = rz,
                    sizeX = 0.14f, sizeY = 0.03f, sizeZ = 0.03f,
                    colorHex = "#B0BEC5",
                    r = 0.75f, g = 0.75f, b = 0.78f,
                    category = "Fittings"
                )
            )
        }

        // 3. Primary High-Voltage Crossarm & Steel Angle Braces
        val armY = originY + 9.8f
        val isCorner = postType == "CORNER" || postType == "DEAD_END"
        val armRotW = kotlin.math.max(kotlin.math.abs(crossarmWidth * cosA), 0.12f)
        val armRotD = kotlin.math.max(kotlin.math.abs(crossarmWidth * sinA), 0.12f)

        // Main Crossarm
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_crossarm",
                name = "Primary High-Voltage Crossarm",
                centerX = originX, centerY = armY, centerZ = originZ,
                sizeX = armRotW, sizeY = 0.12f, sizeZ = armRotD,
                colorHex = "#455A64",
                r = 0.38f, g = 0.40f, b = 0.44f,
                category = "Crossarm"
            )
        )

        // Diagonal Galvanized Bracing
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_crossarm_brace",
                name = "Crossarm Angle Bracing",
                centerX = originX, centerY = armY - 0.42f, centerZ = originZ,
                sizeX = armRotW * 0.55f, sizeY = 0.05f, sizeZ = armRotD * 0.55f,
                colorHex = "#78909C",
                r = 0.50f, g = 0.52f, b = 0.55f,
                category = "Crossarm"
            )
        )

        // Double Crossarm for Corner & Dead-End
        if (isCorner) {
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_crossarm_double",
                    name = "Secondary Double Crossarm (Strain Angle)",
                    centerX = originX, centerY = armY - 0.35f, centerZ = originZ,
                    sizeX = armRotW, sizeY = 0.12f, sizeZ = armRotD,
                    colorHex = "#37474F",
                    r = 0.34f, g = 0.36f, b = 0.40f,
                    category = "Crossarm"
                )
            )
        }

        // Top Ridge Pin Bracket (Phase B)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_ridge_pin",
                name = "Top Ridge Conductor Bracket",
                centerX = originX, centerY = originY + 10.75f, centerZ = originZ,
                sizeX = 0.08f, sizeY = 0.35f, sizeZ = 0.08f,
                colorHex = "#546E7A",
                r = 0.40f, g = 0.42f, b = 0.45f,
                category = "Fittings"
            )
        )

        // 4. Porcelain Pin Insulators (3 High-Voltage Phases)
        val insOff = (crossarmWidth * 0.5f) - 0.2f
        val (ins1X, ins1Z) = rotatePoint(-insOff, 0.0f)
        val (ins3X, ins3Z) = rotatePoint(insOff, 0.0f)

        // Phase A Insulator (Left)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_insulator_a",
                name = "Glazed Porcelain Pin Insulator (Phase A)",
                centerX = ins1X, centerY = armY + 0.26f, centerZ = ins1Z,
                sizeX = 0.16f, sizeY = 0.22f, sizeZ = 0.16f,
                colorHex = "#8D6E63",
                r = 0.58f, g = 0.22f, b = 0.18f,
                category = "Insulators"
            )
        )
        // Phase B Insulator (Center Ridge)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_insulator_b",
                name = "Glazed Porcelain Ridge Insulator (Phase B)",
                centerX = originX, centerY = originY + 11.0f, centerZ = originZ,
                sizeX = 0.16f, sizeY = 0.22f, sizeZ = 0.16f,
                colorHex = "#8D6E63",
                r = 0.58f, g = 0.22f, b = 0.18f,
                category = "Insulators"
            )
        )
        // Phase C Insulator (Right)
        boxes.add(
            ProceduralBoxPart(
                id = "${idPrefix}_insulator_c",
                name = "Glazed Porcelain Pin Insulator (Phase C)",
                centerX = ins3X, centerY = armY + 0.26f, centerZ = ins3Z,
                sizeX = 0.16f, sizeY = 0.22f, sizeZ = 0.16f,
                colorHex = "#8D6E63",
                r = 0.58f, g = 0.22f, b = 0.18f,
                category = "Insulators"
            )
        )

        // 5. Mounted Step-down Distribution Transformer (If TRANSFORMER or hasTransformer)
        if (postType == "TRANSFORMER" || hasTransformer) {
            val (transOffX, transOffZ) = rotatePoint(0.0f, 0.52f)

            // Heavy Hanger Bracket
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_trans_bracket",
                    name = "Transformer Mounting Hanger Bracket",
                    centerX = transOffX, centerY = originY + 7.2f, centerZ = transOffZ,
                    sizeX = 0.40f, sizeY = 0.08f, sizeZ = 0.20f,
                    colorHex = "#263238",
                    r = 0.25f, g = 0.28f, b = 0.32f,
                    category = "Transformer"
                )
            )
            // 50kVA Cylindrical Transformer Can Body
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_trans_tank",
                    name = "50kVA Step-Down Transformer Tank",
                    centerX = transOffX, centerY = originY + 6.6f, centerZ = transOffZ,
                    sizeX = 0.65f, sizeY = 1.15f, sizeZ = 0.65f,
                    colorHex = "#37474F",
                    r = 0.22f, g = 0.28f, b = 0.31f,
                    category = "Transformer"
                )
            )
            // Radiator Heat Fins
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_trans_fins",
                    name = "Cooling Radiator Fin Flanges",
                    centerX = transOffX, centerY = originY + 6.6f, centerZ = transOffZ,
                    sizeX = 0.76f, sizeY = 0.85f, sizeZ = 0.56f,
                    colorHex = "#263238",
                    r = 0.18f, g = 0.22f, b = 0.25f,
                    category = "Transformer"
                )
            )
            // Top Bushing Terminals
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_trans_bushing_1",
                    name = "HV Primary Bushing Terminal (Left)",
                    centerX = transOffX - 0.14f, centerY = originY + 7.38f, centerZ = transOffZ,
                    sizeX = 0.08f, sizeY = 0.22f, sizeZ = 0.08f,
                    colorHex = "#D7CCC8",
                    r = 0.85f, g = 0.55f, b = 0.20f,
                    category = "Transformer"
                )
            )
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_trans_bushing_2",
                    name = "HV Primary Bushing Terminal (Right)",
                    centerX = transOffX + 0.14f, centerY = originY + 7.38f, centerZ = transOffZ,
                    sizeX = 0.08f, sizeY = 0.22f, sizeZ = 0.08f,
                    colorHex = "#D7CCC8",
                    r = 0.85f, g = 0.55f, b = 0.20f,
                    category = "Transformer"
                )
            )

            // Secondary Low-Voltage Spool Rack
            val (rackX, rackZ) = rotatePoint(0.0f, -0.22f)
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_lv_rack",
                    name = "Secondary 120/240V Spool Rack",
                    centerX = rackX, centerY = originY + 5.2f, centerZ = rackZ,
                    sizeX = 0.12f, sizeY = 0.75f, sizeZ = 0.12f,
                    colorHex = "#546E7A",
                    r = 0.35f, g = 0.38f, b = 0.42f,
                    category = "Transformer"
                )
            )
            for (lv in 0..2) {
                boxes.add(
                    ProceduralBoxPart(
                        id = "${idPrefix}_lv_spool_$lv",
                        name = "Low Voltage Spool Insulator $lv",
                        centerX = rackX, centerY = originY + 4.95f + lv * 0.25f, centerZ = rackZ,
                        sizeX = 0.16f, sizeY = 0.12f, sizeZ = 0.16f,
                        colorHex = "#ECEFF1",
                        r = 0.90f, g = 0.90f, b = 0.92f,
                        category = "Transformer"
                    )
                )
            }
        }

        // 6. Guy Wire Stay & Counterbalance Anchor Stake (for Corner & Dead-End)
        if (isCorner || hasGuyWire) {
            val clampY = originY + 9.5f
            // Guy clamp on mast
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_guy_clamp",
                    name = "Guy Wire Mast Clamping Collar",
                    centerX = originX, centerY = clampY, centerZ = originZ,
                    sizeX = 0.32f, sizeY = 0.12f, sizeZ = 0.32f,
                    colorHex = "#37474F",
                    r = 0.25f, g = 0.27f, b = 0.30f,
                    category = "Guy Wire"
                )
            )

            // Ground Anchor Stake (counterbalancing transverse pull)
            val anchorDist = 4.4f
            val anchorAngle = yawRad + 3.14159f
            val anchorX = originX + anchorDist * kotlin.math.cos(anchorAngle)
            val anchorZ = originZ + anchorDist * kotlin.math.sin(anchorAngle)

            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_guy_anchor_stake",
                    name = "Steel Earth Anchor Stake Rod",
                    centerX = anchorX, centerY = originY + 0.35f, centerZ = anchorZ,
                    sizeX = 0.14f, sizeY = 0.70f, sizeZ = 0.14f,
                    colorHex = "#455A64",
                    r = 0.30f, g = 0.32f, b = 0.35f,
                    category = "Guy Wire"
                )
            )

            // High-visibility yellow safety sleeve (#FFEB3B) on lower 1.8m
            val sleeveMidX = anchorX * 0.75f + originX * 0.25f
            val sleeveMidZ = anchorZ * 0.75f + originZ * 0.25f
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_guy_sleeve",
                    name = "High-Visibility Guy Wire Guard Sleeve",
                    centerX = sleeveMidX, centerY = originY + 0.95f, centerZ = sleeveMidZ,
                    sizeX = 0.12f, sizeY = 1.80f, sizeZ = 0.12f,
                    colorHex = "#FFEB3B",
                    r = 1.0f, g = 0.92f, b = 0.23f,
                    category = "Guy Wire"
                )
            )

            // Extrude 3D guy wire stay cable:
            // When in Continuous Cylinder mode, the support cable going from post to ground
            // is generated as a smooth continuous 3D cylinder swept tube (in wires) with zero block cuboids.
            if (wires != null) {
                wires.add(
                    ProceduralWireCable(
                        id = "${idPrefix}_guy_cable",
                        name = "Post-to-Ground Support Stay Cable",
                        x1 = originX, y1 = clampY, z1 = originZ,
                        x2 = anchorX, y2 = originY + 0.38f, z2 = anchorZ,
                        sag = 0.035f,
                        colorHex = "#78909C",
                        r = 0.50f, g = 0.53f, b = 0.56f,
                        isHotPhase = false,
                        radiusM = 0.016f,
                        tensionKn = 15.0f,
                        linearMassKgM = 0.65f,
                        catenaryConstantA = 2300.0f,
                        useCylinder = useCylinder,
                        cylinderSides = cylinderSides
                    )
                )
            }
            if (!useCylinder || wires == null) {
                // In box chord mode (!useCylinder), generate coarse cuboid chord boxes.
                addCatenaryWireBoxes(
                    boxes = boxes,
                    x1 = originX, y1 = clampY, z1 = originZ,
                    x2 = anchorX, y2 = originY + 0.38f, z2 = anchorZ,
                    sag = 0.035f,
                    segments = 6,
                    thickness = 0.032f,
                    colorHex = "#78909C",
                    r = 0.50f, g = 0.53f, b = 0.56f,
                    idPrefix = "${idPrefix}_guy_cable",
                    useCylinder = false,
                    cylinderSides = 4
                )
            }
        }

        // 7. Cantilever Streetlight Luminaire Arm (Amenity fixture)
        if (hasStreetlight) {
            val armBaseY = originY + 8.2f
            val (lampEndX, lampEndZ) = rotatePoint(1.8f, 0.0f)
            val armMidX = (originX + lampEndX) * 0.5f
            val armMidZ = (originZ + lampEndZ) * 0.5f

            // Galvanized Curved Pipe Arm
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_light_arm",
                    name = "Curved Streetlight Cantilever Arm",
                    centerX = armMidX, centerY = armBaseY + 0.2f, centerZ = armMidZ,
                    sizeX = kotlin.math.abs(lampEndX - originX) + 0.08f, sizeY = 0.07f, sizeZ = kotlin.math.abs(lampEndZ - originZ) + 0.08f,
                    colorHex = "#90A4AE",
                    r = 0.60f, g = 0.63f, b = 0.67f,
                    category = "Streetlight"
                )
            )

            // Cobra-head Fixture
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_light_head",
                    name = "Cobra-Head Streetlight Luminaire",
                    centerX = lampEndX, centerY = armBaseY + 0.35f, centerZ = lampEndZ,
                    sizeX = 0.35f, sizeY = 0.12f, sizeZ = 0.65f,
                    colorHex = "#CFD8DC",
                    r = 0.75f, g = 0.78f, b = 0.82f,
                    category = "Streetlight"
                )
            )

            // Lamp Lens (Glowing warm yellow if on)
            val lampColor = if (isStreetlightOn) "#FFEE58" else "#546E7A"
            val lampR = if (isStreetlightOn) 1.0f else 0.40f
            val lampG = if (isStreetlightOn) 0.94f else 0.40f
            val lampB = if (isStreetlightOn) 0.35f else 0.35f
            boxes.add(
                ProceduralBoxPart(
                    id = "${idPrefix}_light_lens",
                    name = "LED Optical Refractor Lens",
                    centerX = lampEndX, centerY = armBaseY + 0.26f, centerZ = lampEndZ,
                    sizeX = 0.28f, sizeY = 0.06f, sizeZ = 0.52f,
                    colorHex = lampColor,
                    r = lampR, g = lampG, b = lampB,
                    category = "Streetlight"
                )
            )
        }
    }

    /**
     * Builds a single modular electric post inspection model.
     */
    fun buildModularPowerPost(
        postType: String = "CORNER", // "TANGENT", "CORNER", "TRANSFORMER", "DEAD_END"
        hasTransformer: Boolean = true,
        hasStreetlight: Boolean = true,
        isStreetlightOn: Boolean = true,
        hasGuyWire: Boolean = true,
        crossarmWidth: Float = 2.4f,
        useCylinder: Boolean = true,
        cableRadiusM: Float = 0.035f,
        cylinderSides: Int = 8
    ): ProceduralModelMesh {
        val boxes = mutableListOf<ProceduralBoxPart>()
        val wires = mutableListOf<ProceduralWireCable>()

        addSinglePostBoxes(
            boxes = boxes,
            originX = 0.0f, originY = 0.0f, originZ = 0.0f,
            yawRad = 0.0f,
            postType = postType,
            hasTransformer = hasTransformer,
            hasStreetlight = hasStreetlight,
            isStreetlightOn = isStreetlightOn,
            hasGuyWire = hasGuyWire,
            crossarmWidth = crossarmWidth,
            idPrefix = "single_post",
            wires = wires,
            useCylinder = useCylinder,
            cylinderSides = cylinderSides
        )

        return createMeshModel(CppAssetType.MODULAR_POWER_POST, boxes, wires)
    }

    /**
     * Builds a complete connected 3-4 post electric power grid network with overhead sagging catenary wires
     * and at least ONE corner turn!
     */
    fun buildPowerGridNetwork(
        numPosts: Int = 4,
        spanDistance: Float = 16.0f,
        cornerAngleDeg: Float = 85.0f,
        wireSag: Float = 0.55f,
        energized: Boolean = true,
        hasStreetlights: Boolean = true,
        streetlightOn: Boolean = true,
        useCylinder: Boolean = true,
        cableRadiusM: Float = 0.035f,
        tensionKn: Float = 18.5f,
        linearMassKgM: Float = 1.25f,
        cylinderSides: Int = 8
    ): ProceduralModelMesh {
        val boxes = mutableListOf<ProceduralBoxPart>()
        val wires = mutableListOf<ProceduralWireCable>()

        val cornerAngleRad = Math.toRadians(cornerAngleDeg.toDouble()).toFloat()

        data class GridPoleNode(
            val x: Float, val y: Float, val z: Float,
            val postType: String,
            val yawRad: Float,
            val hasTransformer: Boolean,
            val hasGuyWire: Boolean
        )

        val nodes = mutableListOf<GridPoleNode>()

        if (numPosts <= 3) {
            // 3-Post Grid: Start Pole -> Corner Angle Turn Pole -> Dead-End Terminal Pole
            nodes.add(
                GridPoleNode(
                    x = -spanDistance * 1.1f, y = 0.0f, z = 0.0f,
                    postType = "TRANSFORMER",
                    yawRad = 0.0f,
                    hasTransformer = true,
                    hasGuyWire = false
                )
            )

            nodes.add(
                GridPoleNode(
                    x = 0.0f, y = 0.0f, z = 0.0f,
                    postType = "CORNER",
                    yawRad = cornerAngleRad * 0.5f,
                    hasTransformer = false,
                    hasGuyWire = true
                )
            )

            val dirX = kotlin.math.cos(cornerAngleRad)
            val dirZ = kotlin.math.sin(cornerAngleRad)
            nodes.add(
                GridPoleNode(
                    x = dirX * spanDistance * 1.1f, y = 0.0f, z = dirZ * spanDistance * 1.1f,
                    postType = "DEAD_END",
                    yawRad = cornerAngleRad,
                    hasTransformer = false,
                    hasGuyWire = true
                )
            )
        } else {
            // 4-Post Grid: Tangent Straight Run -> Transformer Pole -> Corner Strain Pole -> Dead-End Terminal Pole
            nodes.add(
                GridPoleNode(
                    x = -spanDistance * 1.35f, y = 0.0f, z = 0.0f,
                    postType = "TANGENT",
                    yawRad = 0.0f,
                    hasTransformer = false,
                    hasGuyWire = false
                )
            )

            nodes.add(
                GridPoleNode(
                    x = 0.0f, y = 0.0f, z = 0.0f,
                    postType = "TRANSFORMER",
                    yawRad = 0.0f,
                    hasTransformer = true,
                    hasGuyWire = false
                )
            )

            nodes.add(
                GridPoleNode(
                    x = spanDistance * 1.15f, y = 0.0f, z = 0.0f,
                    postType = "CORNER",
                    yawRad = cornerAngleRad * 0.5f,
                    hasTransformer = false,
                    hasGuyWire = true
                )
            )

            val dirX = kotlin.math.cos(cornerAngleRad)
            val dirZ = kotlin.math.sin(cornerAngleRad)
            val p4X = (spanDistance * 1.15f) + dirX * (spanDistance * 1.05f)
            val p4Z = 0.0f + dirZ * (spanDistance * 1.05f)

            nodes.add(
                GridPoleNode(
                    x = p4X, y = 0.0f, z = p4Z,
                    postType = "DEAD_END",
                    yawRad = cornerAngleRad,
                    hasTransformer = false,
                    hasGuyWire = true
                )
            )
        }

        // 1. Build all posts
        nodes.forEachIndexed { idx, node ->
            addSinglePostBoxes(
                boxes = boxes,
                originX = node.x, originY = node.y, originZ = node.z,
                yawRad = node.yawRad,
                postType = node.postType,
                hasTransformer = node.hasTransformer,
                hasStreetlight = hasStreetlights,
                isStreetlightOn = streetlightOn,
                hasGuyWire = node.hasGuyWire,
                crossarmWidth = 2.4f,
                idPrefix = "grid_pole_$idx",
                wires = wires,
                useCylinder = useCylinder,
                cylinderSides = cylinderSides
            )
        }

        // 2. Extrude 3-Phase Conductors and Neutral Ground Wire between each consecutive post
        for (i in 0 until (nodes.size - 1)) {
            val nA = nodes[i]
            val nB = nodes[i + 1]

            val cosA = kotlin.math.cos(nA.yawRad); val sinA = kotlin.math.sin(nA.yawRad)
            val cosB = kotlin.math.cos(nB.yawRad); val sinB = kotlin.math.sin(nB.yawRad)

            val offW = 1.0f
            val armYA = nA.y + 10.06f
            val armYB = nB.y + 10.06f

            // Phase A Conductor (Left)
            val pA1_X = nA.x - offW * cosA; val pA1_Z = nA.z - offW * sinA
            val pB1_X = nB.x - offW * cosB; val pB1_Z = nB.z - offW * sinB

            // Phase B Conductor (Center Ridge)
            val pA2_X = nA.x; val pA2_Z = nA.z; val pA2_Y = nA.y + 11.22f
            val pB2_X = nB.x; val pB2_Z = nB.z; val pB2_Y = nB.y + 11.22f

            // Phase C Conductor (Right)
            val pA3_X = nA.x + offW * cosA; val pA3_Z = nA.z + offW * sinA
            val pB3_X = nB.x + offW * cosB; val pB3_Z = nB.z + offW * sinB

            // Neutral/Ground Wire
            val pAgnd_Y = nA.y + 8.8f
            val pBgnd_Y = nB.y + 8.8f

            // Realistic non-blue power line colors: metallic charcoal/aluminum, with warm energized amber when hot
            val wireHex = if (energized) "#FFA726" else "#455A64"
            val wireR = if (energized) 0.95f else 0.30f
            val wireG = if (energized) 0.65f else 0.34f
            val wireB = if (energized) 0.18f else 0.38f

            val spanDist = kotlin.math.sqrt((pB1_X - pA1_X) * (pB1_X - pA1_X) + (pB1_Z - pA1_Z) * (pB1_Z - pA1_Z)).coerceAtLeast(0.1f)
            val catenaryA = if (wireSag > 0.005f) (spanDist * spanDist) / (8.0f * wireSag) else 2000.0f

            // Store high-level catenary cables with physics parameters
            wires.add(ProceduralWireCable("span_${i}_phase_a", "Phase A Conductor", pA1_X, armYA, pA1_Z, pB1_X, armYB, pB1_Z, wireSag, wireHex, wireR, wireG, wireB, true, cableRadiusM, tensionKn, linearMassKgM, catenaryA, useCylinder, cylinderSides))
            wires.add(ProceduralWireCable("span_${i}_phase_b", "Phase B Conductor", pA2_X, pA2_Y, pA2_Z, pB2_X, pB2_Y, pB2_Z, wireSag * 0.9f, wireHex, wireR, wireG, wireB, true, cableRadiusM, tensionKn, linearMassKgM, catenaryA * 1.1f, useCylinder, cylinderSides))
            wires.add(ProceduralWireCable("span_${i}_phase_c", "Phase C Conductor", pA3_X, armYA, pA3_Z, pB3_X, armYB, pB3_Z, wireSag, wireHex, wireR, wireG, wireB, true, cableRadiusM, tensionKn, linearMassKgM, catenaryA, useCylinder, cylinderSides))
            wires.add(ProceduralWireCable("span_${i}_neutral", "Neutral Messenger", nA.x, pAgnd_Y, nA.z, nB.x, pBgnd_Y, nB.z, wireSag * 0.6f, "#78909C", 0.50f, 0.53f, 0.56f, false, cableRadiusM * 0.8f, tensionKn * 0.75f, linearMassKgM * 0.7f, catenaryA * 1.35f, useCylinder, cylinderSides))

            // When in Block mode (!useCylinder), generate coarse cuboid chord boxes.
            // When in Continuous Cylinder mode (useCylinder), zero box blocks are added:
            // the conductor is a pure continuous tubular swept cylinder along the catenary curve.
            if (!useCylinder) {
                val wireThick = cableRadiusM * 2.0f
                addCatenaryWireBoxes(boxes, pA1_X, armYA, pA1_Z, pB1_X, armYB, pB1_Z, wireSag, 10, wireThick, wireHex, wireR, wireG, wireB, "wire_span_${i}_phase_a", false, 4)
                addCatenaryWireBoxes(boxes, pA2_X, pA2_Y, pA2_Z, pB2_X, pB2_Y, pB2_Z, wireSag * 0.9f, 10, wireThick, wireHex, wireR, wireG, wireB, "wire_span_${i}_phase_b", false, 4)
                addCatenaryWireBoxes(boxes, pA3_X, armYA, pA3_Z, pB3_X, armYB, pB3_Z, wireSag, 10, wireThick, wireHex, wireR, wireG, wireB, "wire_span_${i}_phase_c", false, 4)
                addCatenaryWireBoxes(boxes, nA.x, pAgnd_Y, nA.z, nB.x, pBgnd_Y, nB.z, wireSag * 0.6f, 8, wireThick * 0.8f, "#78909C", 0.50f, 0.53f, 0.56f, "wire_span_${i}_neutral", false, 4)
            }
        }

        return createMeshModel(CppAssetType.POWER_GRID_NETWORK, boxes, wires)
    }

    private fun createMeshModel(
        assetType: CppAssetType,
        boxes: List<ProceduralBoxPart>,
        wires: List<ProceduralWireCable> = emptyList()
    ): ProceduralModelMesh {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        var volumeSum = 0.0f

        for (box in boxes) {
            val hx = box.sizeX * 0.5f
            val hy = box.sizeY * 0.5f
            val hz = box.sizeZ * 0.5f

            minX = min(minX, box.centerX - hx)
            maxX = max(maxX, box.centerX + hx)
            minY = min(minY, box.centerY - hy)
            maxY = max(maxY, box.centerY + hy)
            minZ = min(minZ, box.centerZ - hz)
            maxZ = max(maxZ, box.centerZ + hz)

            volumeSum += (box.sizeX * box.sizeY * box.sizeZ)
        }

        for (wire in wires) {
            minX = min(minX, min(wire.x1, wire.x2))
            maxX = max(maxX, max(wire.x1, wire.x2))
            minY = min(minY, min(wire.y1 - wire.sag, wire.y2 - wire.sag))
            maxY = max(maxY, max(wire.y1, wire.y2))
            minZ = min(minZ, min(wire.z1, wire.z2))
            maxZ = max(maxZ, max(wire.z1, wire.z2))
        }

        val totalVertices = boxes.size * 36
        val totalTriangles = boxes.size * 12
        val memoryFootprintBytes = totalVertices * 36

        return ProceduralModelMesh(
            assetType = assetType,
            boxes = boxes,
            totalVertices = totalVertices,
            totalTriangles = totalTriangles,
            boundingBoxMin = listOf(minX, minY, minZ),
            boundingBoxMax = listOf(maxX, maxY, maxZ),
            estimatedVolumeM3 = volumeSum,
            memoryFootprintBytes = memoryFootprintBytes,
            wires = wires
        )
    }
}
