// ─── app/src/main/cpp/procedural_models.cpp ───────────────────────────────────
#include "procedural_models.h"
#include "catenary_cable_physics.h"
#include <cmath>

namespace apex {

ModelMesh ProceduralModelGenerator::buildModularPowerPost(
    const std::string& postType,
    bool hasTransformer,
    bool hasStreetlight,
    bool streetlightOn,
    bool hasGuyWire
) {
    ModelMesh mesh;
    mesh.assetType = "MODULAR_POWER_POST";

    // 1. Foundation Pier (Concrete Ground Block)
    mesh.boxes.push_back({
        "pier_foundation", "Concrete Ground Pier Foundation",
        0.0f, 0.225f, 0.0f,
        0.65f, 0.45f, 0.65f,
        0.42f, 0.44f, 0.48f, 1.0f,
        "Foundation"
    });

    // 2. Tapered Octagonal Utility Pole Shaft (11.5m tall)
    mesh.boxes.push_back({
        "pole_shaft_lower", "Class-1 Douglas Fir / Steel Mast Lower",
        0.0f, 3.25f, 0.0f,
        0.42f, 6.0f, 0.42f,
        0.36f, 0.28f, 0.22f, 1.0f,
        "Structural Mast"
    });
    mesh.boxes.push_back({
        "pole_shaft_upper", "Class-1 Douglas Fir / Steel Mast Upper",
        0.0f, 8.75f, 0.0f,
        0.34f, 5.5f, 0.34f,
        0.39f, 0.30f, 0.24f, 1.0f,
        "Structural Mast"
    });

    // 3. Galvanized Steel Crossarm (2.4m width, Phase A, B, C)
    mesh.boxes.push_back({
        "crossarm_beam", "Galvanized Channel Steel Crossarm",
        0.0f, 10.05f, 0.0f,
        2.40f, 0.14f, 0.16f,
        0.58f, 0.62f, 0.66f, 1.0f,
        "Crossarm"
    });

    // 4. Pin Insulators (Porcelain / Polymer Sheds)
    mesh.boxes.push_back({
        "insulator_left", "Phase A Porcelain Pin Insulator",
        -1.0f, 10.35f, 0.0f,
        0.16f, 0.45f, 0.16f,
        0.48f, 0.24f, 0.18f, 1.0f,
        "Insulator"
    });
    mesh.boxes.push_back({
        "insulator_ridge", "Phase B Ridge Pin Insulator",
        0.0f, 11.55f, 0.0f,
        0.16f, 0.45f, 0.16f,
        0.48f, 0.24f, 0.18f, 1.0f,
        "Insulator"
    });
    mesh.boxes.push_back({
        "insulator_right", "Phase C Porcelain Pin Insulator",
        1.0f, 10.35f, 0.0f,
        0.16f, 0.45f, 0.16f,
        0.48f, 0.24f, 0.18f, 1.0f,
        "Insulator"
    });

    // 5. Transformer Tank (if equipped)
    if (hasTransformer) {
        mesh.boxes.push_back({
            "transformer_tank", "Distribution Step-Down Transformer Tank",
            0.0f, 8.0f, 0.45f,
            0.55f, 0.90f, 0.55f,
            0.28f, 0.32f, 0.36f, 1.0f,
            "Transformer Equipment"
        });
    }

    // 6. Streetlight Arm & Luminaire (if equipped)
    if (hasStreetlight) {
        mesh.boxes.push_back({
            "luminaire_housing", "LED Cobra-Head Luminaire",
            0.0f, 7.8f, -1.8f,
            0.32f, 0.14f, 0.55f,
            0.20f, 0.22f, 0.25f, 1.0f,
            "Lighting"
        });
        if (streetlightOn) {
            mesh.boxes.push_back({
                "luminaire_lens", "Illuminated High-Pressure Sodium Lens",
                0.0f, 7.72f, -1.8f,
                0.28f, 0.04f, 0.48f,
                1.0f, 0.94f, 0.45f, 1.0f,
                "Lighting"
            });
        }
    }

    mesh.totalVertices = static_cast<uint32_t>(mesh.boxes.size() * 36);
    mesh.totalTriangles = static_cast<uint32_t>(mesh.boxes.size() * 12);
    return mesh;
}

ModelMesh ProceduralModelGenerator::buildGarage001(
    float bay1OpenPct,
    float bay2OpenPct,
    bool neonSignOn
) {
    ModelMesh mesh;
    mesh.assetType = "GARAGE_001";

    // 1. Concrete Foundation Floor Slab
    mesh.boxes.push_back({"floor_slab", "Reinforced Concrete Foundation Slab", 0.0f, 0.10f, 0.0f, 12.4f, 0.20f, 16.4f, 0.23f, 0.26f, 0.30f, 1.0f, "Foundation"});
    mesh.boxes.push_back({"floor_marking_bay1", "Bay 1 Vehicle Service Zone Boundary", -0.5f, 0.21f, -2.85f, 6.0f, 0.02f, 3.6f, 1.0f, 0.85f, 0.2f, 1.0f, "Floor"});
    mesh.boxes.push_back({"floor_marking_bay2", "Bay 2 Vehicle Parking Zone Boundary", -0.5f, 0.21f, 2.85f, 6.0f, 0.02f, 3.6f, 1.0f, 0.85f, 0.2f, 1.0f, "Floor"});

    // 2. Standalone Garage House Hollow Exterior Walls
    mesh.boxes.push_back({"main_warehouse", "Garage House Rear Structural Wall", 5.9f, 2.9f, 0.0f, 0.35f, 5.4f, 16.0f, 0.22f, 0.26f, 0.32f, 1.0f, "Structure"});
    mesh.boxes.push_back({"left_wall", "Garage House North Wall", 0.0f, 2.9f, 7.9f, 12.0f, 5.4f, 0.35f, 0.22f, 0.26f, 0.32f, 1.0f, "Structure"});
    mesh.boxes.push_back({"right_wall", "Garage House South Wall", 0.0f, 2.9f, -7.9f, 12.0f, 5.4f, 0.35f, 0.22f, 0.26f, 0.32f, 1.0f, "Structure"});

    // Front Entrance Portal (West, X = -5.9m) with doorway openings
    mesh.boxes.push_back({"front_pillar_left", "Entrance Portal North Corner Pillar", -5.9f, 2.9f, 6.4f, 0.35f, 5.4f, 3.0f, 0.22f, 0.26f, 0.32f, 1.0f, "Structure"});
    mesh.boxes.push_back({"front_pillar_right", "Entrance Portal South Corner Pillar", -5.9f, 2.9f, -6.4f, 0.35f, 5.4f, 3.0f, 0.22f, 0.26f, 0.32f, 1.0f, "Structure"});
    mesh.boxes.push_back({"front_pillar_center", "Central Portal Dividing Mullion", -5.9f, 2.9f, 0.0f, 0.35f, 5.4f, 0.8f, 0.27f, 0.35f, 0.39f, 1.0f, "Structure"});
    mesh.boxes.push_back({"front_lintel_header", "Overhead Transom Lintel Wall", -5.9f, 4.85f, 0.0f, 0.35f, 1.5f, 16.0f, 0.22f, 0.26f, 0.32f, 1.0f, "Structure"});

    // 3. Roof & Overhead Structural Ceiling Steel Trusses
    mesh.boxes.push_back({"roof_slab", "Overhead Weather Roof Slab", 0.0f, 5.65f, 0.0f, 12.6f, 0.25f, 16.6f, 0.12f, 0.14f, 0.17f, 1.0f, "Roof"});
    mesh.boxes.push_back({"roof_trim", "Crimson Architectural Roof Trim", 0.0f, 5.85f, 0.0f, 13.0f, 0.20f, 17.0f, 0.85f, 0.22f, 0.22f, 1.0f, "Roof"});
    mesh.boxes.push_back({"roof_truss_1", "Structural Steel Ceiling I-Beam West", -2.0f, 5.35f, 0.0f, 0.25f, 0.35f, 15.4f, 0.27f, 0.35f, 0.39f, 1.0f, "Roof"});
    mesh.boxes.push_back({"roof_truss_2", "Structural Steel Ceiling I-Beam East", 2.0f, 5.35f, 0.0f, 0.25f, 0.35f, 15.4f, 0.27f, 0.35f, 0.39f, 1.0f, "Roof"});

    float bannerR = neonSignOn ? 0.95f : 0.27f;
    float bannerG = neonSignOn ? 0.65f : 0.35f;
    float bannerB = neonSignOn ? 0.18f : 0.39f;
    mesh.boxes.push_back({"workshop_banner", "Workshop Facade Signboard", -6.15f, 4.95f, 0.0f, 0.12f, 0.85f, 8.0f, bannerR, bannerG, bannerB, 1.0f, "Signage"});

    // 4. Modular Articulated Sectional Doors & Hardware Tracks
    mesh.boxes.push_back({"door_track_left_bay1", "Bay 1 Vertical Guide Track (Left)", -5.75f, 2.05f, -4.95f, 0.08f, 3.9f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f, "Doors"});
    mesh.boxes.push_back({"door_track_right_bay1", "Bay 1 Vertical Guide Track (Right)", -5.75f, 2.05f, -0.75f, 0.08f, 3.9f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f, "Doors"});
    mesh.boxes.push_back({"door_track_overhead_left_bay1", "Bay 1 Overhead Ceiling Track (Left)", -3.2f, 4.05f, -4.95f, 5.1f, 0.08f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f, "Doors"});
    mesh.boxes.push_back({"door_track_overhead_right_bay1", "Bay 1 Overhead Ceiling Track (Right)", -3.2f, 4.05f, -0.75f, 5.1f, 0.08f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f, "Doors"});
    mesh.boxes.push_back({"door_torsion_spring_bay1", "Bay 1 Torsion Spring Counterbalance Shaft", -5.7f, 4.2f, -2.85f, 0.14f, 0.14f, 4.1f, 0.22f, 0.28f, 0.31f, 1.0f, "Doors"});

    // Bay 1 Modular Door Panels (Overhead retraction along track)
    float travel1 = (bay1OpenPct < 0.0f ? 0.0f : (bay1OpenPct > 1.0f ? 1.0f : bay1OpenPct)) * 3.8f;
    for (int k = 1; k <= 4; ++k) {
        float baseY = 0.65f + static_cast<float>(k - 1) * 0.95f;
        float curPos = baseY + travel1;
        std::string pid = (k == 1) ? "bay_door_1" : ("bay_door_1_panel_" + std::to_string(k));
        if (curPos <= 3.8f) {
            mesh.boxes.push_back({pid, "Bay 1 Modular Door Panel " + std::to_string(k), -5.85f, curPos, -2.85f, 0.10f, 0.88f, 4.1f, 0.70f, 0.74f, 0.78f, 1.0f, "Doors"});
        } else {
            float overshoot = curPos - 3.8f;
            mesh.boxes.push_back({pid, "Bay 1 Modular Door (Overhead) " + std::to_string(k), -5.85f + overshoot, 4.12f, -2.85f, 0.88f, 0.10f, 4.1f, 0.65f, 0.70f, 0.75f, 1.0f, "Doors"});
        }
    }

    // Bay 2 Modular Door Panels
    float travel2 = (bay2OpenPct < 0.0f ? 0.0f : (bay2OpenPct > 1.0f ? 1.0f : bay2OpenPct)) * 3.8f;
    for (int k = 1; k <= 4; ++k) {
        float baseY = 0.65f + static_cast<float>(k - 1) * 0.95f;
        float curPos = baseY + travel2;
        std::string pid = (k == 1) ? "bay_door_2" : ("bay_door_2_panel_" + std::to_string(k));
        if (curPos <= 3.8f) {
            mesh.boxes.push_back({pid, "Bay 2 Modular Door Panel " + std::to_string(k), -5.85f, curPos, 2.85f, 0.10f, 0.88f, 4.1f, 0.70f, 0.74f, 0.78f, 1.0f, "Doors"});
        } else {
            float overshoot = curPos - 3.8f;
            mesh.boxes.push_back({pid, "Bay 2 Modular Door (Overhead) " + std::to_string(k), -5.85f + overshoot, 4.12f, 2.85f, 0.88f, 0.10f, 4.1f, 0.65f, 0.70f, 0.75f, 1.0f, "Doors"});
        }
    }

    // 5. DETAILED WORKSHOP INTERIOR (VISIBLE INSIDE THROUGH OPEN DOOR!)
    // Hydraulic 2-Post Vehicle Hoist in Bay 1
    mesh.boxes.push_back({"lift_post_left", "Hydraulic 2-Post Hoist Left Column", -0.5f, 2.1f, -4.3f, 0.35f, 4.0f, 0.35f, 0.08f, 0.40f, 0.75f, 1.0f, "Interior - Vehicle Lift"});
    mesh.boxes.push_back({"lift_post_right", "Hydraulic 2-Post Hoist Right Column", -0.5f, 2.1f, -1.4f, 0.35f, 4.0f, 0.35f, 0.08f, 0.40f, 0.75f, 1.0f, "Interior - Vehicle Lift"});
    mesh.boxes.push_back({"lift_crossbeam", "Overhead Hydraulic Cable Equalizer Beam", -0.5f, 4.15f, -2.85f, 0.25f, 0.20f, 3.1f, 0.05f, 0.28f, 0.63f, 1.0f, "Interior - Vehicle Lift"});
    mesh.boxes.push_back({"lift_carriages_and_arms", "Dual Swing Lifting Arms & Hoist Carriage", -0.5f, 0.95f, -2.85f, 2.4f, 0.16f, 1.8f, 1.0f, 0.70f, 0.0f, 1.0f, "Interior - Vehicle Lift"});
    mesh.boxes.push_back({"lift_power_unit", "Electro-Hydraulic Pump & Fluid Reservoir", -0.5f, 2.0f, -4.55f, 0.22f, 0.75f, 0.22f, 0.13f, 0.13f, 0.13f, 1.0f, "Interior - Vehicle Lift"});

    // Mechanic Workbench & Tool Pegboard
    mesh.boxes.push_back({"workbench_base", "Heavy Industrial Workbench Steel Cabinet", 4.8f, 0.55f, -2.85f, 1.2f, 0.9f, 3.6f, 0.15f, 0.20f, 0.22f, 1.0f, "Interior - Workbench"});
    mesh.boxes.push_back({"workbench_top", "Solid Hardwood Work Surface Top", 4.8f, 1.05f, -2.85f, 1.35f, 0.10f, 3.8f, 0.55f, 0.43f, 0.39f, 1.0f, "Interior - Workbench"});
    mesh.boxes.push_back({"tool_pegboard", "Perforated Tool Mounting Pegboard Back", 5.6f, 1.95f, -2.85f, 0.08f, 1.6f, 3.6f, 0.38f, 0.49f, 0.55f, 1.0f, "Interior - Workbench"});
    mesh.boxes.push_back({"bench_vise", "Heavy Cast Iron Mechanic Bench Vise", 4.2f, 1.22f, -4.2f, 0.25f, 0.25f, 0.25f, 0.78f, 0.16f, 0.16f, 1.0f, "Interior - Workbench"});

    // Rolling Multi-Drawer Tool Cabinet
    mesh.boxes.push_back({"tool_chest_body", "Mobile 8-Drawer Industrial Tool Cabinet", 4.5f, 0.75f, 0.0f, 0.9f, 1.3f, 1.1f, 0.83f, 0.18f, 0.18f, 1.0f, "Interior - Tool Storage"});
    mesh.boxes.push_back({"tool_chest_handle_trim", "Chrome Drawer Handles & Edge Bumpers", 4.0f, 0.75f, 0.0f, 0.06f, 1.2f, 0.9f, 0.81f, 0.85f, 0.86f, 1.0f, "Interior - Tool Storage"});

    // Heavy Industrial Pallet Racking & Shelves & Oil Drums
    mesh.boxes.push_back({"storage_rack_frame", "Heavy-Duty Steel Pallet Racking Frame", 1.5f, 1.8f, 7.1f, 4.5f, 3.4f, 0.75f, 0.27f, 0.35f, 0.39f, 1.0f, "Interior - Storage"});
    mesh.boxes.push_back({"storage_rack_shelves", "Orange High-Load Deck Shelves", 1.5f, 1.3f, 7.1f, 4.4f, 0.08f, 0.7f, 0.94f, 0.42f, 0.0f, 1.0f, "Interior - Storage"});
    mesh.boxes.push_back({"storage_battery_crates", "Heavy Industrial Battery Storage Crates", 2.0f, 1.6f, 7.1f, 1.4f, 0.5f, 0.6f, 0.22f, 0.28f, 0.31f, 1.0f, "Interior - Storage"});
    mesh.boxes.push_back({"oil_drums_stack", "55-Gallon Hydraulic Lubricant Drums", -2.5f, 0.65f, 7.1f, 0.75f, 1.1f, 0.75f, 0.05f, 0.28f, 0.63f, 1.0f, "Interior - Storage"});

    // Wall-Mounted Tire Storage Rack
    mesh.boxes.push_back({"tire_storage_rack", "Heavy Automotive Wheel & Tire Rack", 2.0f, 2.4f, -7.2f, 3.0f, 0.5f, 0.65f, 0.22f, 0.28f, 0.31f, 1.0f, "Interior - Tire Storage"});
    mesh.boxes.push_back({"tire_set_rubber", "High-Performance Testing Tire Stack", 2.0f, 2.5f, -7.2f, 2.8f, 0.75f, 0.6f, 0.13f, 0.13f, 0.13f, 1.0f, "Interior - Tire Storage"});

    // Overhead High-Bay LED Shop Light Fixtures
    mesh.boxes.push_back({"shop_light_bay1", "High-Bay LED Shop Light Luminaire (Bay 1)", -0.5f, 5.25f, -2.85f, 0.5f, 0.12f, 2.6f, 1.0f, 0.98f, 0.77f, 1.0f, "Interior - Shop Lighting"});
    mesh.boxes.push_back({"shop_light_bay2", "High-Bay LED Shop Light Luminaire (Bay 2)", -0.5f, 5.25f, 2.85f, 0.5f, 0.12f, 2.6f, 1.0f, 0.98f, 0.77f, 1.0f, "Interior - Shop Lighting"});

    mesh.totalVertices = static_cast<uint32_t>(mesh.boxes.size() * 36);
    mesh.totalTriangles = static_cast<uint32_t>(mesh.boxes.size() * 12);
    return mesh;
}

} // namespace apex
