// ==============================================================================
// Modular Road Component: Road Block & Security Checkpoint 3D Mesh Generator
// Path: native-assets/src/road/roadblocks/RoadBlockGenerator.cpp
// Standards: FHWA MUTCD Part 6 Temporary Traffic Control & ASTM F2656 K12 Anti-Ram
// ==============================================================================
#include "RoadBlockGenerator.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static void AddRBBox(
    RoadBlockMeshBuffer& buffer,
    float cx, float cy, float cz,
    float sx, float sy, float sz,
    uint32_t matId,
    float r, float g, float b,
    float rough, float metal, float emis = 0.0f
) {
    Primitives::AddBox<RoadBlockMeshBuffer, RoadBlockVertex>(
        buffer, cx, cy, cz, sx, sy, sz, matId, r, g, b, 1.0f, rough, metal, emis
    );
}

static void AddTrafficCone(RoadBlockMeshBuffer& buffer, float x, float z) {
    // Weighted black rubber square base
    AddRBBox(buffer, x, 0.04f, z, 0.44f, 0.08f, 0.44f, 0, 0.12f, 0.13f, 0.15f, 0.80f, 0.02f);
    // Fluorescent safety orange cone frustum + white reflective collar
    Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
        buffer, x, 0.08f, z, x, 0.56f, z,
        0.18f, 0.09f, 10, 3, 0.98f, 0.36f, 0.06f, 1.0f, 0.45f, 0.05f, 0.0f
    );
    Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
        buffer, x, 0.56f, z, x, 0.74f, z,
        0.09f, 0.06f, 10, 3, 0.96f, 0.97f, 0.98f, 1.0f, 0.30f, 0.10f, 0.5f
    );
    Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
        buffer, x, 0.74f, z, x, 0.92f, z,
        0.06f, 0.03f, 10, 3, 0.98f, 0.36f, 0.06f, 1.0f, 0.45f, 0.05f, 0.0f
    );
}

static void AddJerseyBarricadeModule(
    RoadBlockMeshBuffer& buffer,
    float cx, float cz, float widthX, float lengthZ,
    bool horizontalAcrossX, bool isOrangeWhite
) {
    float r = isOrangeWhite ? 0.96f : 0.72f;
    float g = isOrangeWhite ? 0.38f : 0.74f;
    float b = isOrangeWhite ? 0.08f : 0.78f;
    float rough = isOrangeWhite ? 0.40f : 0.70f;

    if (horizontalAcrossX) {
        // Tapered Jersey cross-section running along X
        AddRBBox(buffer, cx, 0.14f, cz, widthX, 0.28f, 0.62f, isOrangeWhite ? 3 : 1, r, g, b, rough, 0.04f);
        AddRBBox(buffer, cx, 0.46f, cz, widthX, 0.38f, 0.38f, isOrangeWhite ? 3 : 1, r, g, b, rough, 0.04f);
        AddRBBox(buffer, cx, 0.80f, cz, widthX, 0.32f, 0.24f, isOrangeWhite ? 3 : 1, r, g, b, rough, 0.04f);
    } else {
        AddRBBox(buffer, cx, 0.14f, cz, 0.62f, 0.28f, lengthZ, isOrangeWhite ? 3 : 1, r, g, b, rough, 0.04f);
        AddRBBox(buffer, cx, 0.46f, cz, 0.38f, 0.38f, lengthZ, isOrangeWhite ? 3 : 1, r, g, b, rough, 0.04f);
        AddRBBox(buffer, cx, 0.80f, cz, 0.24f, 0.32f, lengthZ, isOrangeWhite ? 3 : 1, r, g, b, rough, 0.04f);
    }
}

RoadBlockMeshBuffer RoadBlockGenerator::GenerateRoadBlockMesh(const RoadBlockParams& params) {
    RoadBlockMeshBuffer buffer;

    BuildAsphaltBasePad(buffer, params);

    switch (params.type) {
        case RoadBlockType::HydraAntiRamBollardArray:
            BuildHydraulicBollardArray(buffer, params);
            break;
        case RoadBlockType::TacticalSpikeAndChicane:
            BuildTacticalSpikeChicane(buffer, params);
            break;
        case RoadBlockType::ConstructionLaneClosureZone:
            BuildConstructionClosureZone(buffer, params);
            break;
        case RoadBlockType::ConcreteDragonTeethChicane:
            BuildDragonTeethChicane(buffer, params);
            break;
        case RoadBlockType::JerseyAndWaterBarricadeLine:
        default:
            BuildWaterAndJerseyLine(buffer, params);
            break;
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);
    buffer.kineticStopRatingKj = (params.type == RoadBlockType::HydraAntiRamBollardArray ||
                                  params.type == RoadBlockType::ConcreteDragonTeethChicane) ? 1850.0f : 420.0f;

    return buffer;
}

void RoadBlockGenerator::BuildAsphaltBasePad(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params) {
    float W = params.roadWidthM;
    float L = params.zoneLengthM;
    // Heavy asphalt pavement slab + outer concrete curbs
    AddRBBox(buffer, 0.0f, -0.20f, 0.0f, W + 2.0f, 0.40f, L, 0, 0.19f, 0.21f, 0.24f, 0.86f, 0.02f);
    for (float sx : {-W * 0.5f - 0.55f, W * 0.5f + 0.55f}) {
        AddRBBox(buffer, sx, 0.14f, 0.0f, 0.90f, 0.28f, L, 1, 0.64f, 0.67f, 0.71f, 0.68f, 0.04f);
        AddRBBox(buffer, sx * 0.88f, 0.012f, 0.0f, 0.18f, 0.02f, L * 0.94f, 3, 0.95f, 0.95f, 0.96f, 0.48f, 0.04f);
    }
    // Dashed centerline down pavement
    int dashes = std::max(3, static_cast<int>(L / 5.5f));
    for (int d = 0; d < dashes; ++d) {
        float dz = -L * 0.42f + (static_cast<float>(d) / std::max(1, dashes - 1)) * L * 0.84f;
        AddRBBox(buffer, 0.0f, 0.012f, dz, 0.16f, 0.02f, 2.0f, 3, 0.96f, 0.78f, 0.14f, 0.48f, 0.04f);
    }
}

void RoadBlockGenerator::BuildWaterAndJerseyLine(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params) {
    int count = std::clamp<int>(params.unitCount, 3, 12);
    float totalSpan = params.roadWidthM * 0.92f;
    float unitW = totalSpan / count;

    // 1. Primary Interlocking Safety Orange & White Water-Filled Jersey Barricades across roadway
    for (int i = 0; i < count; ++i) {
        float cx = -totalSpan * 0.5f + (i + 0.5f) * unitW;
        bool isOrange = (i % 2 == 0);
        AddJerseyBarricadeModule(buffer, cx, -1.5f, unitW * 0.96f, 0.62f, true, isOrange);
        if (!isOrange) {
            // Paint crisp high-vis white block color override by adding top reflective cap
            AddRBBox(buffer, cx, 0.78f, -1.5f, unitW * 0.92f, 0.34f, 0.25f, 3, 0.95f, 0.96f, 0.98f, 0.32f, 0.08f);
        }
        if (params.hasWarningBeacons && (i == 0 || i == count - 1 || i == count / 2)) {
            AddRBBox(buffer, cx, 1.06f, -1.5f, 0.24f, 0.22f, 0.24f, 4, 1.0f, 0.65f, 0.08f, 0.15f, 0.10f, 4.8f);
        }
    }

    // 2. Upstream MUTCD Type III Reflective Striped Road-Closed Saw-Horse Barricades & Warning Cones
    for (float sx : {-totalSpan * 0.26f, totalSpan * 0.26f}) {
        // Steel A-frame galvanized base skids & uprights
        AddRBBox(buffer, sx - 1.1f, 0.04f, -6.5f, 0.12f, 0.08f, 1.25f, 2, 0.70f, 0.74f, 0.78f, 0.30f, 0.85f);
        AddRBBox(buffer, sx + 1.1f, 0.04f, -6.5f, 0.12f, 0.08f, 1.25f, 2, 0.70f, 0.74f, 0.78f, 0.30f, 0.85f);
        AddRBBox(buffer, sx - 1.1f, 0.80f, -6.5f, 0.09f, 1.52f, 0.09f, 2, 0.70f, 0.74f, 0.78f, 0.30f, 0.85f);
        AddRBBox(buffer, sx + 1.1f, 0.80f, -6.5f, 0.09f, 1.52f, 0.09f, 2, 0.70f, 0.74f, 0.78f, 0.30f, 0.85f);
        // 3 Stacked Retroreflective Striped Planks
        for (int rail = 0; rail < 3; ++rail) {
            float ry = 0.55f + rail * 0.42f;
            AddRBBox(buffer, sx, ry, -6.5f, 2.55f, 0.24f, 0.06f, 3,
                     (rail % 2 == 0) ? 0.96f : 0.95f,
                     (rail % 2 == 0) ? 0.40f : 0.95f,
                     (rail % 2 == 0) ? 0.08f : 0.96f,
                     0.35f, 0.10f);
        }
    }

    // Lead-in Taper of High-Vis Traffic Cones
    for (int c = 0; c < 6; ++c) {
        float frac = static_cast<float>(c) / 5.0f;
        AddTrafficCone(buffer, -totalSpan * 0.44f + frac * totalSpan * 0.88f, -11.5f + frac * 3.0f);
    }
}

void RoadBlockGenerator::BuildHydraulicBollardArray(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params) {
    int count = std::clamp<int>(params.unitCount, 3, 10);
    float span = params.roadWidthM * 0.88f;

    // 1. Sub-Surface Reinforced Concrete K12 Bollard Trench Pit Slab & Yellow Hazard Hatch Border
    AddRBBox(buffer, 0.0f, 0.03f, 0.0f, span + 1.2f, 0.08f, 2.2f, 1, 0.52f, 0.55f, 0.59f, 0.65f, 0.15f);
    AddRBBox(buffer, 0.0f, 0.05f, -1.25f, span + 1.2f, 0.06f, 0.32f, 3, 0.96f, 0.76f, 0.10f, 0.45f, 0.05f);
    AddRBBox(buffer, 0.0f, 0.05f,  1.25f, span + 1.2f, 0.06f, 0.32f, 3, 0.96f, 0.76f, 0.10f, 0.45f, 0.05f);

    // 2. Heavy Brushed-Steel K12 Anti-Ram Cylindrical Bollards with Recessed Night LED Collars
    for (int i = 0; i < count; ++i) {
        float bx = -span * 0.5f + (count == 1 ? 0.5f : static_cast<float>(i) / (count - 1)) * span;
        // Steel flush floor sleeve collar
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, bx, 0.05f, 0.0f, bx, 0.16f, 0.0f,
            0.24f, 0.22f, 12, 2, 0.25f, 0.28f, 0.32f, 1.0f, 0.35f, 0.85f, 0.0f
        );
        // Main High-Strength Stainless Steel Bollard Piston
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, bx, 0.16f, 0.0f, bx, 0.96f, 0.0f,
            0.16f, 0.16f, 14, 2, 0.80f, 0.84f, 0.88f, 1.0f, 0.22f, 0.92f, 0.0f
        );
        // Glowing Red LED Visibility Ring Band + Reflective Cap Dome
        if (params.hasWarningBeacons) {
            Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
                buffer, bx, 0.96f, 0.0f, bx, 1.06f, 0.0f,
                0.165f, 0.165f, 14, 4, 1.0f, 0.18f, 0.12f, 1.0f, 0.15f, 0.10f, 4.8f
            );
        }
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, bx, 1.06f, 0.0f, bx, 1.14f, 0.0f,
            0.16f, 0.10f, 14, 2, 0.82f, 0.86f, 0.90f, 1.0f, 0.22f, 0.92f, 0.0f
        );
    }

    // 3. Automated Hydraulic Arm Security Gate + Guard Control Pedestal on Right Shoulder
    float pedX = params.roadWidthM * 0.5f + 0.5f;
    AddRBBox(buffer, pedX, 0.65f, -3.8f, 0.65f, 1.30f, 0.65f, 3, 0.96f, 0.70f, 0.10f, 0.35f, 0.65f);
    Primitives::AddBeam<RoadBlockMeshBuffer, RoadBlockVertex>(
        buffer, pedX - 0.3f, 1.05f, -3.8f, -params.roadWidthM * 0.44f, 1.05f, -3.8f,
        0.10f, 0.14f, 3, 0.92f, 0.20f, 0.16f, 1.0f, 0.35f, 0.25f, 0.0f
    );
    // Upstream Rubber Rumble Speed Bump Threshold
    AddRBBox(buffer, 0.0f, 0.07f, -7.5f, params.roadWidthM * 0.92f, 0.14f, 0.65f,
             3, 0.96f, 0.78f, 0.12f, 0.60f, 0.05f);
}

void RoadBlockGenerator::BuildTacticalSpikeChicane(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params) {
    float W = params.roadWidthM;
    // 1. Staggered Concrete S-Curve Chicane Barriers forcing extreme serpentine deceleration
    AddJerseyBarricadeModule(buffer, -W * 0.16f, -7.5f, W * 0.60f, 0.65f, true, false);
    AddJerseyBarricadeModule(buffer,  W * 0.16f,  7.5f, W * 0.60f, 0.65f, true, false);

    // 2. Retractable Yellow/Black Roadway Tire-Deflation Spike Strip Tray at Midpoint (Z=0)
    AddRBBox(buffer, 0.0f, 0.04f, 0.0f, W * 0.85f, 0.08f, 0.58f, 3, 0.96f, 0.76f, 0.10f, 0.42f, 0.35f);
    int spikes = 16;
    for (int s = 0; s < spikes; ++s) {
        float sx = -W * 0.38f + (static_cast<float>(s) / (spikes - 1)) * (W * 0.76f);
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, sx, 0.08f, 0.0f, sx, 0.22f, 0.0f,
            0.045f, 0.01f, 6, 2, 0.82f, 0.86f, 0.90f, 1.0f, 0.22f, 0.92f, 0.0f
        );
    }

    // 3. Tactical Inspection Booth & Overhead Flashing Bar on Shoulder
    float boothX = -W * 0.5f - 1.2f;
    AddRBBox(buffer, boothX, 1.35f, 0.0f, 1.8f, 2.7f, 2.8f, 1, 0.36f, 0.42f, 0.48f, 0.48f, 0.45f);
    AddRBBox(buffer, boothX, 1.75f, 0.0f, 1.86f, 1.0f, 2.2f, 2, 0.18f, 0.48f, 0.68f, 0.12f, 0.25f);
    if (params.hasWarningBeacons) {
        AddRBBox(buffer, boothX, 2.85f, -0.5f, 0.4f, 0.28f, 0.4f, 4, 1.0f, 0.18f, 0.15f, 0.15f, 0.10f, 4.8f);
        AddRBBox(buffer, boothX, 2.85f,  0.5f, 0.4f, 0.28f, 0.4f, 4, 0.16f, 0.55f, 1.0f, 0.15f, 0.10f, 4.8f);
    }
}

void RoadBlockGenerator::BuildConstructionClosureZone(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params) {
    float W = params.roadWidthM;
    float L = params.zoneLengthM;
    int barrels = std::clamp<int>(params.unitCount + 4, 6, 14);

    // 1. Diagonal Lane-Drop Taper of Orange/White MUTCD Construction Channelizer Drums
    for (int i = 0; i < barrels; ++i) {
        float frac = static_cast<float>(i) / (barrels - 1);
        float bx = (frac < 0.5f)
            ? (-W * 0.42f + (frac * 2.0f) * (W * 0.42f))
            : 0.0f;
        float bz = -L * 0.44f + frac * L * 0.85f;

        AddRBBox(buffer, bx, 0.03f, bz, 0.72f, 0.06f, 0.72f, 0, 0.12f, 0.13f, 0.15f, 0.85f, 0.02f);
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, bx, 0.06f, bz, bx, 0.35f, bz,
            0.28f, 0.26f, 12, 3, 0.98f, 0.40f, 0.06f, 1.0f, 0.45f, 0.05f, 0.0f
        );
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, bx, 0.35f, bz, bx, 0.62f, bz,
            0.26f, 0.24f, 12, 3, 0.96f, 0.97f, 0.98f, 1.0f, 0.30f, 0.10f, 0.6f
        );
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, bx, 0.62f, bz, bx, 0.96f, bz,
            0.24f, 0.22f, 12, 3, 0.98f, 0.40f, 0.06f, 1.0f, 0.45f, 0.05f, 0.0f
        );
    }

    // 2. Concrete Workzone K-Rail Longitudinal Shield inside Closed Lane
    for (int k = 0; k < 3; ++k) {
        AddJerseyBarricadeModule(buffer, W * 0.05f, 3.0f + k * 4.2f, 0.60f, 3.9f, false, false);
    }

    // 3. Portable Trailer-Mounted Flashing LED Arrow Board inside Work Bay
    float tx = W * 0.25f, tz = 4.5f;
    AddRBBox(buffer, tx, 0.55f, tz, 1.85f, 0.22f, 2.65f, 3, 0.96f, 0.70f, 0.10f, 0.40f, 0.65f);
    for (float wx : {-1.02f, 1.02f}) {
        Primitives::AddCylinder<RoadBlockMeshBuffer, RoadBlockVertex>(
            buffer, tx + wx - 0.15f, 0.36f, tz, tx + wx + 0.15f, 0.36f, tz,
            0.36f, 0.36f, 12, 0, 0.14f, 0.15f, 0.17f, 1.0f, 0.85f, 0.05f, 0.0f
        );
    }
    // Mast & Large Dark Matrix Arrow Panel + Glowing Amber Chevron Bars
    AddRBBox(buffer, tx, 1.65f, tz, 0.18f, 2.1f, 0.18f, 2, 0.68f, 0.72f, 0.76f, 0.32f, 0.85f);
    AddRBBox(buffer, tx, 2.75f, tz, 2.45f, 1.35f, 0.18f, 2, 0.10f, 0.11f, 0.13f, 0.35f, 0.70f);
    if (params.hasWarningBeacons) {
        AddRBBox(buffer, tx, 2.75f, tz - 0.11f, 1.65f, 0.22f, 0.05f, 4, 1.0f, 0.72f, 0.08f, 0.15f, 0.10f, 5.0f);
        AddRBBox(buffer, tx + 0.45f, 2.95f, tz - 0.11f, 0.35f, 0.55f, 0.05f, 4, 1.0f, 0.72f, 0.08f, 0.15f, 0.10f, 5.0f);
        AddRBBox(buffer, tx + 0.45f, 2.55f, tz - 0.11f, 0.35f, 0.55f, 0.05f, 4, 1.0f, 0.72f, 0.08f, 0.15f, 0.10f, 5.0f);
    }
}

void RoadBlockGenerator::BuildDragonTeethChicane(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params) {
    float span = params.roadWidthM * 0.86f;
    int perRow = std::clamp<int>(params.unitCount, 3, 8);

    // 2 Staggered Rows of Monolithic Anti-Vehicle Precast Concrete Dragon-Teeth Blocks
    for (int row = 0; row < 2; ++row) {
        float rz = (row == 0) ? -3.2f : 3.2f;
        int nBlocks = (row == 0) ? perRow : std::max(2, perRow - 1);
        for (int b = 0; b < nBlocks; ++b) {
            float offset = (row == 0) ? 0.0f : (span / std::max(2, perRow) * 0.5f);
            float bx = -span * 0.44f + offset + (static_cast<float>(b) / std::max(1, nBlocks)) * span * 0.88f;
            // Massive stepped concrete base & tapered upper pyramid core
            AddRBBox(buffer, bx, 0.28f, rz, 1.35f, 0.56f, 1.35f, 1, 0.62f, 0.65f, 0.69f, 0.74f, 0.04f);
            AddRBBox(buffer, bx, 0.82f, rz, 0.92f, 0.54f, 0.92f, 1, 0.72f, 0.75f, 0.79f, 0.72f, 0.04f);
            // Hazard yellow reflector plates front & back
            AddRBBox(buffer, bx, 0.65f, rz - 0.52f, 0.75f, 0.35f, 0.08f, 3, 0.98f, 0.76f, 0.08f, 0.35f, 0.12f);
        }
    }
}

} // namespace Road
} // namespace ApexAssets
