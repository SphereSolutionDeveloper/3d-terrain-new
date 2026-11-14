// ==============================================================================
// Modular Road Component: Retaining Wall 3D Mesh Generator Implementation
// Path: native-assets/src/road/retaining_walls/RetainingWallGen.cpp
// Standards: FHWA GEC 011 MSE Walls & Eurocode 7 Geotechnical Engineering
// ==============================================================================
#include "RetainingWallGen.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float DEG2RAD = PI / 180.0f;

static void AddWallBox(
    WallMeshBuffer& buffer,
    float cx, float cy, float cz,
    float dimN, float dimY, float dimT,
    float nx, float nz, float tx, float tz,
    uint32_t matId,
    float r, float g, float b, float rough, float metal
) {
    Primitives::AddOrientedBox<WallMeshBuffer, WallVertex>(
        buffer, cx, cy, cz, dimN, dimY, dimT, nx, nz, tx, tz,
        matId, r, g, b, 1.0f, rough, metal, 0.0f
    );
}

WallMeshBuffer RetainingWallGen::GenerateWallMesh(const RetainingWallParams& params) {
    WallMeshBuffer buffer;

    float H = params.wallHeightM;
    float L = params.wallLengthM;
    float halfL = L * 0.5f;
    float batterRad = params.wallBatterAngleDeg * DEG2RAD;
    float batterSin = std::sin(batterRad);
    float batterCos = std::cos(batterRad);
    float topXOffset = -std::tan(batterRad) * H;

    // 1. Grounded Reinforced Concrete Leveling Pad / Spread Footing at Y=0
    float footingW = (params.type == RetainingWallType::CantileverRCWall)
        ? std::max(3.6f, H * 0.65f)
        : 2.2f;
    float footingH = 0.65f;
    AddWallBox(buffer, -footingW * 0.25f + 0.35f, footingH * 0.5f, 0.0f,
               footingW, footingH, L + 0.4f,
               1.0f, 0.0f, 0.0f, 1.0f,
               2, 0.52f, 0.55f, 0.59f, 0.74f, 0.04f);

    float wallStartY = footingH;
    float activeH = std::max(1.5f, H - footingH);

    // 2. Build Specialized Geotechnical Structure by RetainingWallType
    switch (params.type) {
        case RetainingWallType::SteppedRockGabion: {
            int numTiers = std::max(2, static_cast<int>(std::ceil(activeH)));
            float tierH = activeH / numTiers;
            int lengthSegs = std::max(8, static_cast<int>(std::round(L / 2.2f)));
            float segLen = L / lengthSegs;

            for (int tier = 0; tier < numTiers; ++tier) {
                float yc = wallStartY + (tier + 0.5f) * tierH;
                float depthX = 1.45f;
                float xc = -tier * 0.42f - depthX * 0.5f;

                for (int seg = 0; seg < lengthSegs; ++seg) {
                    float zc = -halfL + (seg + 0.5f) * segLen;
                    float tone = ((tier + seg) % 2 == 0) ? 0.50f : 0.43f;
                    // Real 3D Double-Twisted Wire Mesh Stone-Filled Basket
                    AddWallBox(buffer, xc, yc, zc,
                               depthX, tierH * 0.94f, segLen * 0.96f,
                               1.0f, 0.0f, 0.0f, 1.0f,
                               1, tone + 0.02f, tone, tone - 0.03f, 0.85f, params.wireGabionMetallic * 0.5f);
                    // Metallic Wire Mesh Frame Trim on Front Face
                    AddWallBox(buffer, xc + depthX * 0.5f + 0.02f, yc + tierH * 0.44f, zc,
                               0.05f, 0.06f, segLen * 0.96f,
                               1.0f, 0.0f, 0.0f, 1.0f,
                               3, 0.68f, 0.72f, 0.76f, 0.35f, params.wireGabionMetallic);
                }
            }
            break;
        }

        case RetainingWallType::CantileverRCWall: {
            // Cast-in-Place Battered T-Stem Reinforced Concrete Wall
            int slices = std::max(8, static_cast<int>(std::round(H * 2.0f)));
            float sliceH = activeH / slices;
            for (int s = 0; s < slices; ++s) {
                float yMid = wallStartY + (s + 0.5f) * sliceH;
                float thickness = 0.85f - 0.35f * (static_cast<float>(s) / slices);
                float xBack = -std::tan(batterRad) * (yMid - wallStartY) - thickness * 0.5f;
                AddWallBox(buffer, xBack, yMid, 0.0f,
                           thickness, sliceH + 0.02f, L,
                           1.0f, 0.0f, 0.0f, 1.0f,
                           0, 0.66f, 0.69f, 0.73f, params.concretePanelRoughness, 0.04f);
            }
            // Counterfort Buttress Stiffener Ribs behind the RC stem every 6.0m
            int numRibs = std::max(3, static_cast<int>(L / 6.0f));
            for (int r = 0; r <= numRibs; ++r) {
                float rz = -halfL + 1.5f + (static_cast<float>(r) / numRibs) * (L - 3.0f);
                for (int step = 0; step < 4; ++step) {
                    float sy = wallStartY + (step + 0.5f) * (activeH * 0.24f);
                    float ribD = footingW * 0.70f * (1.0f - step * 0.20f);
                    AddWallBox(buffer, -ribD * 0.5f - 0.5f, sy, rz,
                               ribD, activeH * 0.24f, 0.45f,
                               1.0f, 0.0f, 0.0f, 1.0f,
                               0, 0.58f, 0.61f, 0.65f, 0.72f, 0.04f);
                }
            }
            break;
        }

        case RetainingWallType::SoilNailShotcrete: {
            // Continuous Textured Shotcrete Facing + Grid of Steel Soil-Nail Bearing Plates & Anchor Heads
            int rows = std::max(3, static_cast<int>(std::round(activeH / 1.4f)));
            int cols = std::max(6, static_cast<int>(std::round(L / 2.2f)));
            float rowH = activeH / rows;
            float colW = L / cols;

            for (int r = 0; r < rows; ++r) {
                float yc = wallStartY + (r + 0.5f) * rowH;
                float xc = -std::tan(batterRad) * (yc - wallStartY) - 0.18f;
                for (int c = 0; c < cols; ++c) {
                    float zc = -halfL + (c + 0.5f) * colW;
                    AddWallBox(buffer, xc, yc, zc,
                               0.36f, rowH * 0.98f, colW * 0.98f,
                               batterCos, batterSin, 0.0f, 1.0f,
                               0, 0.62f, 0.63f, 0.64f, 0.88f, 0.02f);

                    // Steel Soil-Nail Bearing Plate + Hex Anchor Head Stud
                    AddWallBox(buffer, xc + 0.20f, yc, zc,
                               0.08f, 0.34f, 0.34f,
                               batterCos, batterSin, 0.0f, 1.0f,
                               3, 0.30f, 0.34f, 0.38f, 0.35f, 0.85f);
                    // Grouted Soil Nail Rod angled down into embankment behind facing
                    Primitives::AddCylinder<WallMeshBuffer, WallVertex>(
                        buffer, xc + 0.24f, yc, zc,
                        xc - params.strapReinforcementLengthM * 0.85f, yc - 1.15f, zc,
                        0.06f, 0.05f, 6, 3, 0.45f, 0.48f, 0.52f, 1.0f, 0.40f, 0.82f, 0.0f
                    );
                }
            }
            break;
        }

        case RetainingWallType::CribWallCellular: {
            // Alternating Courses of Precast Concrete Stretchers (Longitudinal) & Headers (Transverse)
            int courses = std::max(4, static_cast<int>(std::round(activeH / 0.65f)));
            float cH = activeH / courses;
            int bays = std::max(6, static_cast<int>(std::round(L / 2.4f)));
            float bayL = L / bays;
            float cribDepth = 1.85f;

            for (int k = 0; k < courses; ++k) {
                float yc = wallStartY + (k + 0.5f) * cH;
                float xc = -std::tan(batterRad) * (yc - wallStartY) - cribDepth * 0.5f;
                // Front & Rear Longitudinal Stretcher Beams
                for (float bxOff : {cribDepth * 0.42f, -cribDepth * 0.42f}) {
                    AddWallBox(buffer, xc + bxOff, yc, 0.0f,
                               0.26f, cH * 0.48f, L,
                               1.0f, 0.0f, 0.0f, 1.0f,
                               0, 0.72f, 0.74f, 0.78f, params.concretePanelRoughness, 0.04f);
                }
                // Cross-Tie Headers framing cellular stone pockets
                for (int b = 0; b <= bays; ++b) {
                    float bz = -halfL + b * bayL;
                    AddWallBox(buffer, xc, yc + cH * 0.22f, bz,
                               cribDepth * 1.05f, cH * 0.44f, 0.28f,
                               1.0f, 0.0f, 0.0f, 1.0f,
                               0, 0.64f, 0.66f, 0.70f, params.concretePanelRoughness, 0.04f);
                }
            }
            // Crushed angular drainage stone infill core inside crib cells
            AddWallBox(buffer, -cribDepth * 0.5f, wallStartY + activeH * 0.5f, 0.0f,
                       cribDepth * 0.68f, activeH * 0.92f, L * 0.96f,
                       1.0f, 0.0f, 0.0f, 1.0f,
                       1, 0.36f, 0.35f, 0.34f, 0.90f, 0.02f);
            break;
        }

        case RetainingWallType::ContiguousBoredPileWall: {
            // Overlapping Cylindrical Cast-In-Situ Secant Concrete Piles + Capping Waler Beam
            float pileR = 0.62f;
            float spacing = pileR * 1.65f;
            int numPiles = std::max(8, static_cast<int>(L / spacing));
            for (int p = 0; p <= numPiles; ++p) {
                float pz = -halfL + 0.8f + (static_cast<float>(p) / numPiles) * (L - 1.6f);
                float tone = (p % 2 == 0) ? 0.68f : 0.60f;
                Primitives::AddCylinder<WallMeshBuffer, WallVertex>(
                    buffer, -pileR * 0.6f, footingH * 0.5f, pz,
                    -pileR * 0.6f, H, pz,
                    pileR, pileR, 12, 0,
                    tone, tone + 0.02f, tone + 0.05f, 1.0f, params.concretePanelRoughness, 0.04f, 0.0f
                );
            }
            // Heavy Capping Waler Beam locking all pile heads
            AddWallBox(buffer, -pileR * 0.6f, H - 0.45f, 0.0f,
                       pileR * 2.25f, 0.95f, L,
                       1.0f, 0.0f, 0.0f, 1.0f,
                       2, 0.78f, 0.80f, 0.84f, 0.60f, 0.05f);
            break;
        }

        case RetainingWallType::MSEModularPanel:
        default: {
            // Interlocking Beveled Precast Concrete Panels + Galvanized Steel Soil Reinforcement Straps
            int rows = std::max(2, static_cast<int>(std::ceil(activeH / params.panelHeightM)));
            int cols = std::max(4, static_cast<int>(std::ceil(L / params.panelWidthM)));
            float rH = activeH / rows;
            float cW = L / cols;

            for (int r = 0; r < rows; ++r) {
                float yc = wallStartY + (r + 0.5f) * rH;
                float xc = -std::tan(batterRad) * (yc - wallStartY) - 0.16f;
                float stagger = (r % 2 == 1) ? cW * 0.25f : 0.0f;

                for (int c = 0; c < cols; ++c) {
                    float zc = -halfL + (c + 0.5f) * cW + stagger;
                    if (zc > halfL - 0.3f) continue;
                    float effW = cW * 0.95f;
                    AddWallBox(buffer, xc, yc, zc,
                               0.32f, rH * 0.94f, effW,
                               1.0f, 0.0f, 0.0f, 1.0f,
                               0, 0.68f, 0.71f, 0.75f, params.concretePanelRoughness, 0.04f);

                    // Galvanized Steel Geogrid Straps extending backward into soil backfill
                    if (c % 2 == 0) {
                        AddWallBox(buffer, xc - params.strapReinforcementLengthM * 0.5f, yc, zc,
                                   params.strapReinforcementLengthM, 0.05f, 0.14f,
                                   1.0f, 0.0f, 0.0f, 1.0f,
                                   3, 0.72f, 0.76f, 0.82f, 0.32f, 0.88f);
                    }
                }
            }
            break;
        }
    }

    // 3. Hydrostatic PVC Weephole Drainage Pipe Protrusions along Toe
    if (params.hasWeepholes) {
        int numWeeps = std::max(4, static_cast<int>(L / 6.0f));
        for (int w = 0; w < numWeeps; ++w) {
            float wz = -halfL + 3.0f + (static_cast<float>(w) / std::max(1, numWeeps - 1)) * (L - 6.0f);
            Primitives::AddCylinder<WallMeshBuffer, WallVertex>(
                buffer, -0.30f, footingH + 0.45f, wz, 0.24f, footingH + 0.45f, wz,
                0.09f, 0.09f, 8, 3, 0.16f, 0.18f, 0.20f, 1.0f, 0.40f, 0.10f, 0.0f
            );
        }
    }

    // 4. Retained Earth / Gravel Backfill Terrace Prism Behind Wall
    float backfillW = std::max(3.5f, params.strapReinforcementLengthM * 0.92f);
    float backfillX = topXOffset - backfillW * 0.5f - 0.65f;
    AddWallBox(buffer, backfillX, wallStartY + activeH * 0.48f, 0.0f,
               backfillW, activeH * 0.96f, L - 0.2f,
               1.0f, 0.0f, 0.0f, 1.0f,
               4, 0.42f, 0.36f, 0.28f, 0.92f, 0.01f);

    // 5. Concrete Coping Cap Beam & Highway TL-4 Jersey Barrier on Top
    if (params.hasTopCopigSlab) {
        float capH = 0.42f;
        AddWallBox(buffer, topXOffset - 0.42f, H + capH * 0.5f, 0.0f,
                   0.95f, capH, L + 0.2f,
                   1.0f, 0.0f, 0.0f, 1.0f,
                   2, 0.80f, 0.82f, 0.86f, 0.60f, 0.04f);

        if (params.hasHighwayJerseyBarrier) {
            float jH = 0.85f;
            AddWallBox(buffer, topXOffset - 0.35f, H + capH + jH * 0.5f, 0.0f,
                       0.45f, jH, L,
                       1.0f, 0.0f, 0.0f, 1.0f,
                       2, 0.86f, 0.88f, 0.90f, 0.62f, 0.04f);
        }
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);

    float gamma = 18.5f;
    float phi = 32.0f * DEG2RAD;
    float Ka = std::pow(std::tan(PI * 0.25f - phi * 0.5f), 2.0f);
    buffer.activeLateralEarthPressureKnM = 0.5f * gamma * params.wallHeightM * params.wallHeightM * Ka;

    return buffer;
}

} // namespace Road
} // namespace ApexAssets
