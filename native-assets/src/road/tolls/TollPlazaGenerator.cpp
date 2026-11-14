// ==============================================================================
// Modular Road Component: Toll Plaza 3D Mesh Generator Implementation
// Path: native-assets/src/road/tolls/TollPlazaGenerator.cpp
// Vulkan Architecture: Sascha Willems / Khronos glTF 2.0 PBR Solid Primitives
// ==============================================================================
#include "TollPlazaGenerator.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static void AddPlazaQuad(
    TollMeshBuffer& buffer,
    const float c0[3], const float c1[3], const float c2[3], const float c3[3],
    const float norm[3], uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddQuad<TollMeshBuffer, TollVertex>(
        buffer, c0, c1, c2, c3, norm, matId, r, g, b, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, rough, metal, emis
    );
}

static void AddPlazaBox(
    TollMeshBuffer& buffer,
    float cx, float cy, float cz,
    float sx, float sy, float sz,
    uint32_t matId,
    float r, float g, float b,
    float rough, float metal, float emis = 0.0f
) {
    Primitives::AddBox<TollMeshBuffer, TollVertex>(
        buffer, cx, cy, cz, sx, sy, sz, matId, r, g, b, 1.0f, rough, metal, emis
    );
}

TollMeshBuffer TollPlazaGenerator::GenerateTollMesh(const TollParams& params) {
    TollMeshBuffer buffer;

    // 1. Thick concrete apron, asphalt toll lanes, dashed lane markings & protective islands
    BuildConcreteApronAndIslands(buffer, params);

    // 2. Superstructure according to TollPlazaType
    switch (params.type) {
        case TollPlazaType::OverheadETCGantry:
            BuildOverheadETCGantry(buffer, params);
            break;
        case TollPlazaType::ExpressBorderCheckpoint:
            BuildCanopyRoofStructure(buffer, params);
            BuildBoothsAndBarriers(buffer, params);
            BuildBorderInspectionBays(buffer, params);
            break;
        case TollPlazaType::WeighInMotionPlaza:
            BuildOverheadETCGantry(buffer, params);
            BuildWIMScalesAndBypass(buffer, params);
            break;
        case TollPlazaType::CanopyHighwayPlaza:
        case TollPlazaType::RampBarrierGate:
        case TollPlazaType::SmartSolarCanopyPlaza:
        default:
            BuildCanopyRoofStructure(buffer, params);
            BuildBoothsAndBarriers(buffer, params);
            break;
    }

    // 3. Overhead dynamic LED Variable Message Signs (Green Arrow / Amber Express)
    if (params.hasDynamicVMSDisplay) {
        BuildVMSLaneSignals(buffer, params);
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);

    float perLaneCap = (params.type == TollPlazaType::OverheadETCGantry ||
                        params.type == TollPlazaType::WeighInMotionPlaza) ? 1800.0f : 750.0f;
    buffer.peakTrafficThroughputVph = params.laneCount * perLaneCap;

    float totalWidth = (params.type == TollPlazaType::OverheadETCGantry || params.type == TollPlazaType::WeighInMotionPlaza)
        ? params.laneCount * params.laneWidthM + 4.0f
        : params.laneCount * params.laneWidthM + (params.laneCount + 1) * params.boothIslandWidthM;
    buffer.totalPlazaFootprintM2 = totalWidth * params.plazaLengthM;

    return buffer;
}

void TollPlazaGenerator::BuildConcreteApronAndIslands(
    TollMeshBuffer& buffer,
    const TollParams& params
) {
    bool isFreeFlow = (params.type == TollPlazaType::OverheadETCGantry ||
                       params.type == TollPlazaType::WeighInMotionPlaza);
    uint32_t effectiveLanes = (params.type == TollPlazaType::RampBarrierGate)
        ? std::min<uint32_t>(params.laneCount, 4u)
        : params.laneCount;

    float totalWidth = isFreeFlow
        ? (effectiveLanes * params.laneWidthM + 4.5f)
        : (effectiveLanes * params.laneWidthM + (effectiveLanes + 1) * params.boothIslandWidthM);
    float halfW = totalWidth * 0.5f;
    float halfL = params.plazaLengthM * 0.5f;

    // 1. Thick Reinforced Concrete Apron Foundation Slab (0.45m structural depth)
    float slabH = 0.45f;
    AddPlazaBox(buffer, 0.0f, -slabH * 0.5f, 0.0f, totalWidth + 2.4f, slabH, params.plazaLengthM,
                0, 0.44f, 0.47f, 0.51f, params.pavementConcreteRoughness, 0.03f);

    // Concrete perimeter outer curbs
    AddPlazaBox(buffer, -halfW - 0.65f, 0.16f, 0.0f, 1.1f, 0.32f, params.plazaLengthM,
                0, 0.66f, 0.69f, 0.72f, 0.65f, 0.04f);
    AddPlazaBox(buffer,  halfW + 0.65f, 0.16f, 0.0f, 1.1f, 0.32f, params.plazaLengthM,
                0, 0.66f, 0.69f, 0.72f, 0.65f, 0.04f);

    // 2. Dark Asphalt Carriageway Wearing Course + Approach/Departure Taper Shoulders
    float nUp[3] = {0.0f, 1.0f, 0.0f};
    float roadY = 0.02f;
    float r0[3] = {-halfW, roadY, -halfL};
    float r1[3] = { halfW, roadY, -halfL};
    float r2[3] = { halfW, roadY,  halfL};
    float r3[3] = {-halfW, roadY,  halfL};
    AddPlazaQuad(buffer, r0, r1, r2, r3, nUp, 0, 0.18f, 0.20f, 0.23f, 0.86f, 0.02f);

    // 3. Lane Center Stripes & Dashed Stop Zone Markings
    if (isFreeFlow) {
        float startX = -(effectiveLanes * params.laneWidthM) * 0.5f;
        for (uint32_t l = 1; l < effectiveLanes; ++l) {
            float lx = startX + l * params.laneWidthM;
            for (int d = -4; d <= 4; ++d) {
                float dz0 = d * 6.5f - 1.8f;
                float dz1 = d * 6.5f + 1.8f;
                float s0[3] = {lx - 0.09f, roadY + 0.015f, dz0};
                float s1[3] = {lx + 0.09f, roadY + 0.015f, dz0};
                float s2[3] = {lx + 0.09f, roadY + 0.015f, dz1};
                float s3[3] = {lx - 0.09f, roadY + 0.015f, dz1};
                AddPlazaQuad(buffer, s0, s1, s2, s3, nUp, 3, 0.94f, 0.95f, 0.96f, 0.50f, 0.05f);
            }
        }
        // Yellow outer shoulder edge lines
        for (float ex : {-halfW + 1.5f, halfW - 1.5f}) {
            float e0[3] = {ex - 0.11f, roadY + 0.015f, -halfL + 1.0f};
            float e1[3] = {ex + 0.11f, roadY + 0.015f, -halfL + 1.0f};
            float e2[3] = {ex + 0.11f, roadY + 0.015f,  halfL - 1.0f};
            float e3[3] = {ex - 0.11f, roadY + 0.015f,  halfL - 1.0f};
            AddPlazaQuad(buffer, e0, e1, e2, e3, nUp, 3, 0.96f, 0.78f, 0.14f, 0.45f, 0.05f);
        }
        return;
    }

    // 4. Concrete Booth Islands + Tapered Bullnose Crash Attenuators (Manual/Hybrid/Border Plazas)
    float islandPitch = params.laneWidthM + params.boothIslandWidthM;
    float startX = -halfW + params.boothIslandWidthM * 0.5f;
    float islandH = 0.40f;
    float islandLen = (params.type == TollPlazaType::RampBarrierGate)
        ? std::min(params.boothIslandLengthM, 12.0f)
        : params.boothIslandLengthM;

    for (uint32_t i = 0; i <= effectiveLanes; ++i) {
        float cx = startX + i * islandPitch;

        // Solid 3D concrete booth island block
        AddPlazaBox(buffer, cx, islandH * 0.5f, 0.0f,
                    params.boothIslandWidthM, islandH, islandLen,
                    0, 0.68f, 0.71f, 0.75f, 0.62f, 0.05f);

        // Safety yellow-painted curb top cap trim along island edge
        AddPlazaBox(buffer, cx, islandH + 0.03f, 0.0f,
                    params.boothIslandWidthM * 0.96f, 0.06f, islandLen * 0.98f,
                    3, 0.95f, 0.74f, 0.12f, 0.48f, 0.08f);

        // Stepped QuadGuard Crash Attenuator Cushions on upstream & downstream bullnoses
        if (params.hasCrashAttenuators) {
            for (float dir : {-1.0f, 1.0f}) {
                for (int step = 0; step < 3; ++step) {
                    float wScale = 0.78f - step * 0.18f;
                    float zCenter = dir * (islandLen * 0.5f + 0.65f + step * 1.15f);
                    float cR = (step % 2 == 0) ? 0.98f : 0.14f;
                    float cG = (step % 2 == 0) ? 0.74f : 0.15f;
                    float cB = (step % 2 == 0) ? 0.08f : 0.17f;
                    AddPlazaBox(buffer, cx, 0.48f, zCenter,
                                params.boothIslandWidthM * wScale, 0.96f, 1.05f,
                                3, cR, cG, cB, 0.42f, 0.12f);
                }
            }
        }
    }

    // Stop bars & lane channel arrows in front of booths
    float boothStartX = -halfW + params.boothIslandWidthM + params.laneWidthM * 0.5f;
    for (uint32_t l = 0; l < effectiveLanes; ++l) {
        float lx = boothStartX + l * islandPitch;
        float barZ = -islandLen * 0.5f + 2.5f;
        AddPlazaBox(buffer, lx, roadY + 0.015f, barZ, params.laneWidthM * 0.78f, 0.02f, 0.45f,
                    3, 0.95f, 0.96f, 0.96f, 0.50f, 0.05f);

        // Cyan EV Fast-Charge lane marking for SmartSolarCanopyPlaza
        if (params.type == TollPlazaType::SmartSolarCanopyPlaza && (l == 0 || l == effectiveLanes - 1)) {
            AddPlazaBox(buffer, lx, roadY + 0.016f, -islandLen * 0.25f, params.laneWidthM * 0.55f, 0.02f, islandLen * 0.7f,
                        4, 0.12f, 0.85f, 0.65f, 0.40f, 0.10f, 1.2f);
        }
    }
}

void TollPlazaGenerator::BuildCanopyRoofStructure(
    TollMeshBuffer& buffer,
    const TollParams& params
) {
    uint32_t effectiveLanes = (params.type == TollPlazaType::RampBarrierGate)
        ? std::min<uint32_t>(params.laneCount, 4u)
        : params.laneCount;
    float totalWidth = effectiveLanes * params.laneWidthM + (effectiveLanes + 1) * params.boothIslandWidthM;
    float canopyW = totalWidth + 5.0f;
    float halfCW = canopyW * 0.5f;
    float islandLen = (params.type == TollPlazaType::RampBarrierGate)
        ? std::min(params.boothIslandLengthM, 12.0f)
        : params.boothIslandLengthM;
    float canopyL = islandLen + params.canopyOverhangM * 2.0f;
    float halfCL = canopyL * 0.5f;
    float roofBaseY = params.canopyClearanceM;

    // 1. Thick Sculpted Airfoil Canopy Roof Deck (Top Cladding, Soffit Ceiling & Fascia Rim)
    const int numRibs = 12;
    float roofMatR = (params.type == TollPlazaType::SmartSolarCanopyPlaza) ? 0.82f : 0.78f;
    float roofMatG = (params.type == TollPlazaType::SmartSolarCanopyPlaza) ? 0.86f : 0.82f;
    float roofMatB = (params.type == TollPlazaType::SmartSolarCanopyPlaza) ? 0.90f : 0.87f;

    auto wingProfileTop = [&](float t) {
        return roofBaseY + 1.35f + 1.15f * std::sin(t * Primitives::PI);
    };
    auto wingProfileBot = [&](float t) {
        return roofBaseY + 0.15f + 0.45f * std::sin(t * Primitives::PI);
    };

    for (int r = 0; r < numRibs; ++r) {
        float t0 = static_cast<float>(r) / numRibs;
        float t1 = static_cast<float>(r + 1) / numRibs;
        float z0 = -halfCL + t0 * canopyL;
        float z1 = -halfCL + t1 * canopyL;

        float yt0 = wingProfileTop(t0), yt1 = wingProfileTop(t1);
        float yb0 = wingProfileBot(t0), yb1 = wingProfileBot(t1);

        // Top weathered steel / composite cladding (upward normal, CW outer)
        float nUp[3] = {0.0f, 1.0f, 0.0f};
        float top0[3] = {-halfCW, yt0, z0}, top1[3] = { halfCW, yt0, z0};
        float top2[3] = { halfCW, yt1, z1}, top3[3] = {-halfCW, yt1, z1};
        AddPlazaQuad(buffer, top0, top1, top2, top3, nUp, 1,
                     roofMatR, roofMatG, roofMatB, params.canopyRoughness, params.canopyMetallic);

        // Soffit underside ceiling (visible when looking up into lanes!)
        float nDown[3] = {0.0f, -1.0f, 0.0f};
        float bot0[3] = {-halfCW, yb0, z0}, bot1[3] = { halfCW, yb0, z0};
        float bot2[3] = { halfCW, yb1, z1}, bot3[3] = {-halfCW, yb1, z1};
        AddPlazaQuad(buffer, bot0, bot3, bot2, bot1, nDown, 1,
                     0.62f, 0.66f, 0.72f, 0.42f, 0.55f);

        // Left & Right Cantilever Fascia Sidewalls
        float nLeft[3] = {-1.0f, 0.0f, 0.0f};
        AddPlazaQuad(buffer, top0, top3, bot3, bot0, nLeft, 1, 0.24f, 0.30f, 0.38f, 0.32f, 0.72f);

        float nRight[3] = {1.0f, 0.0f, 0.0f};
        AddPlazaQuad(buffer, top1, bot1, bot2, top2, nRight, 1, 0.24f, 0.30f, 0.38f, 0.32f, 0.72f);
    }

    // Front & Back Bullnose Fascia Caps + Highway Authority Emblem Header Band
    float ytStart = wingProfileTop(0.0f), ybStart = wingProfileBot(0.0f);
    float ytEnd   = wingProfileTop(1.0f), ybEnd   = wingProfileBot(1.0f);
    float fCapH = std::max(0.75f, ytStart - ybStart);
    AddPlazaBox(buffer, 0.0f, (ytStart + ybStart) * 0.5f, -halfCL, canopyW, fCapH, 0.65f,
                1, 0.16f, 0.34f, 0.56f, 0.30f, 0.78f);
    AddPlazaBox(buffer, 0.0f, (ytEnd + ybEnd) * 0.5f,  halfCL, canopyW, fCapH, 0.65f,
                1, 0.16f, 0.34f, 0.56f, 0.30f, 0.78f);

    // 2. Solar Photovoltaic Arrays mounted atop roof for SmartSolarCanopyPlaza
    if (params.type == TollPlazaType::SmartSolarCanopyPlaza) {
        int rows = 4;
        int cols = std::max(4, static_cast<int>(effectiveLanes));
        float panelW = (canopyW - 3.0f) / cols * 0.85f;
        float panelL = (canopyL - 3.0f) / rows * 0.72f;
        for (int r = 0; r < rows; ++r) {
            float tz = -halfCL + 2.2f + (r + 0.5f) * ((canopyL - 4.4f) / rows);
            float tNorm = (tz + halfCL) / canopyL;
            float baseY = wingProfileTop(tNorm) + 0.32f;
            for (int c = 0; c < cols; ++c) {
                float tx = -halfCW + 2.0f + (c + 0.5f) * ((canopyW - 4.0f) / cols);
                // Solar frame pedestal + deep blue silicon PV panel
                AddPlazaBox(buffer, tx, baseY - 0.16f, tz, panelW * 0.7f, 0.28f, panelL * 0.4f,
                            1, 0.70f, 0.74f, 0.78f, 0.32f, 0.85f);
                AddPlazaBox(buffer, tx, baseY + 0.06f, tz, panelW, 0.10f, panelL,
                            2, 0.08f, 0.16f, 0.36f, 0.14f, 0.75f);
            }
        }
    }

    // 3. Space-Frame Under-Roof Transverse Lattice Trusses & Longitudinal Chords
    float trussBottomY = roofBaseY - 0.35f;
    for (int f = -2; f <= 2; ++f) {
        float fz = f * (canopyL * 0.20f);
        Primitives::AddBeam<TollMeshBuffer, TollVertex>(
            buffer, -halfCW + 0.6f, trussBottomY, fz, halfCW - 0.6f, trussBottomY, fz,
            0.35f, 0.45f, 1, 0.66f, 0.72f, 0.78f, 1.0f, 0.30f, 0.85f, 0.0f
        );
    }

    // 4. Heavy Grounded Steel Support Columns anchored into concrete booth islands
    float islandPitch = params.laneWidthM + params.boothIslandWidthM;
    float startX = -totalWidth * 0.5f + params.boothIslandWidthM * 0.5f;
    float columnTopY = roofBaseY + 0.25f;

    for (uint32_t i = 0; i <= effectiveLanes; ++i) {
        // Place outer columns on island ends and alternate middle islands
        if (i == 0 || i == effectiveLanes || i % 2 == 0) {
            float cx = startX + i * islandPitch;
            for (float cz : {-islandLen * 0.32f, islandLen * 0.32f}) {
                // Concrete base plinth collar
                AddPlazaBox(buffer, cx, 0.55f, cz, 1.05f, 0.45f, 1.05f,
                            0, 0.58f, 0.61f, 0.65f, 0.65f, 0.05f);
                // Tapered cylindrical structural steel column
                Primitives::AddCylinder<TollMeshBuffer, TollVertex>(
                    buffer, cx, 0.75f, cz, cx, columnTopY, cz,
                    0.36f, 0.30f, 12, 1, 0.74f, 0.78f, 0.83f, 1.0f, 0.28f, 0.86f, 0.0f
                );
                // Diagonal V-strut braces up to canopy space frame
                Primitives::AddBeam<TollMeshBuffer, TollVertex>(
                    buffer, cx, columnTopY - 1.2f, cz,
                    cx, columnTopY + 0.1f, cz + (cz < 0 ? -1.4f : 1.4f),
                    0.18f, 0.18f, 1, 0.68f, 0.72f, 0.78f, 1.0f, 0.30f, 0.85f, 0.0f
                );
            }
        }
    }

    // Recessed Under-Canopy LED Downlight Strips over every lane
    float laneStartX = -totalWidth * 0.5f + params.boothIslandWidthM + params.laneWidthM * 0.5f;
    for (uint32_t l = 0; l < effectiveLanes; ++l) {
        float lx = laneStartX + l * islandPitch;
        AddPlazaBox(buffer, lx, roofBaseY + 0.10f, 0.0f, 0.55f, 0.10f, islandLen * 0.7f,
                    4, 0.96f, 0.98f, 1.0f, 0.15f, 0.05f, 3.2f);
    }
}

void TollPlazaGenerator::BuildBoothsAndBarriers(
    TollMeshBuffer& buffer,
    const TollParams& params
) {
    uint32_t effectiveLanes = (params.type == TollPlazaType::RampBarrierGate)
        ? std::min<uint32_t>(params.laneCount, 4u)
        : params.laneCount;
    float totalWidth = effectiveLanes * params.laneWidthM + (effectiveLanes + 1) * params.boothIslandWidthM;
    float halfW = totalWidth * 0.5f;
    float islandPitch = params.laneWidthM + params.boothIslandWidthM;
    float startX = -halfW + params.boothIslandWidthM * 0.5f;

    float bW = params.boothIslandWidthM * 0.76f;
    float bL = 3.4f;
    float bH = params.boothHeightM;
    float baseY = 0.40f;

    for (uint32_t i = 0; i <= effectiveLanes; ++i) {
        float cx = startX + i * islandPitch;

        // 1. Lower Composite Cladding Skirt (Waist height)
        float lowerH = 1.05f;
        AddPlazaBox(buffer, cx, baseY + lowerH * 0.5f, 0.0f,
                    bW, lowerH, bL, 1, 0.34f, 0.42f, 0.50f, 0.38f, 0.65f);

        // 2. Panoramic Tinted Security Glass Glazing Core
        float glassH = bH - lowerH - 0.30f;
        float glassCy = baseY + lowerH + glassH * 0.5f;
        AddPlazaBox(buffer, cx, glassCy, 0.0f,
                    bW * 0.90f, glassH, bL * 0.90f,
                    2, 0.18f, 0.48f, 0.68f, 0.10f, 0.25f);

        // 4 Aluminum Corner Mullions
        for (float sx : {-bW * 0.47f, bW * 0.47f}) {
            for (float sz : {-bL * 0.47f, bL * 0.47f}) {
                AddPlazaBox(buffer, cx + sx, glassCy, sz,
                            0.12f, glassH + 0.05f, 0.12f,
                            1, 0.78f, 0.80f, 0.84f, 0.28f, 0.85f);
            }
        }

        // 3. Cantilevered Overhang Booth Weather Visor Cap + Roof HVAC Box
        float capY = baseY + bH - 0.12f;
        AddPlazaBox(buffer, cx, capY, 0.0f, bW + 0.45f, 0.24f, bL + 0.45f,
                    1, 0.72f, 0.76f, 0.80f, 0.32f, 0.78f);
        AddPlazaBox(buffer, cx, capY + 0.24f, -0.35f, bW * 0.55f, 0.26f, 0.75f,
                    1, 0.52f, 0.56f, 0.60f, 0.45f, 0.65f);

        // 4. Automated Barrier Actuator Pedestal & Articulated Striped Boom Arm
        if (params.hasAutomatedBarrierArms && i < effectiveLanes) {
            float pedX = cx + bW * 0.5f + 0.22f;
            float pedZ = bL * 0.5f + 1.1f;
            AddPlazaBox(buffer, pedX, baseY + 0.55f, pedZ, 0.42f, 1.10f, 0.42f,
                        3, 0.95f, 0.65f, 0.10f, 0.35f, 0.60f);

            // Boom arm alternating red and white segments across active toll lane
            float armStart = pedX + 0.2f;
            float armEnd = cx + islandPitch - bW * 0.5f - 0.25f;
            int segs = 6;
            for (int s = 0; s < segs; ++s) {
                float u0 = static_cast<float>(s) / segs;
                float u1 = static_cast<float>(s + 1) / segs;
                float sx0 = armStart + u0 * (armEnd - armStart);
                float sx1 = armStart + u1 * (armEnd - armStart);
                bool isRed = (s % 2 == 0);
                Primitives::AddBeam<TollMeshBuffer, TollVertex>(
                    buffer, sx0, baseY + 0.96f, pedZ, sx1, baseY + 0.96f, pedZ,
                    0.09f, 0.14f, 3,
                    isRed ? 0.90f : 0.96f,
                    isRed ? 0.18f : 0.96f,
                    isRed ? 0.16f : 0.96f,
                    1.0f, 0.38f, 0.25f, 0.0f
                );
            }
        }
    }
}

void TollPlazaGenerator::BuildOverheadETCGantry(
    TollMeshBuffer& buffer,
    const TollParams& params
) {
    float totalWidth = params.laneCount * params.laneWidthM + 4.5f;
    float halfW = totalWidth * 0.5f;
    float gantryH = params.canopyClearanceM + 1.6f;

    // Twin Steel Pipe Lattice Gantries (Entry RFID Trigger Gantry + Exit Verification Gantry)
    for (float gz : {-5.5f, 5.5f}) {
        float colXLeft = -halfW - 0.6f;
        float colXRight = halfW + 0.6f;

        for (float colX : {colXLeft, colXRight}) {
            // Grounded concrete foundation pedestal
            AddPlazaBox(buffer, colX, 0.55f, gz, 1.35f, 1.1f, 1.6f,
                        0, 0.58f, 0.61f, 0.65f, 0.68f, 0.04f);

            // Twin vertical tubular steel masts + horizontal rungs
            for (float dz : {-0.45f, 0.45f}) {
                Primitives::AddCylinder<TollMeshBuffer, TollVertex>(
                    buffer, colX, 1.0f, gz + dz, colX, gantryH + 0.9f, gz + dz,
                    0.22f, 0.20f, 10, 1, 0.72f, 0.76f, 0.80f, 1.0f, 0.28f, 0.88f, 0.0f
                );
            }
            for (float hy = 2.0f; hy < gantryH; hy += 1.5f) {
                Primitives::AddBeam<TollMeshBuffer, TollVertex>(
                    buffer, colX, hy, gz - 0.45f, colX, hy, gz + 0.45f,
                    0.12f, 0.12f, 1, 0.65f, 0.70f, 0.76f, 1.0f, 0.30f, 0.85f, 0.0f
                );
            }
        }

        // Dual-Chord Overhead Box Truss Bridge (Upper & Lower chords + vertical & diagonal web diagonals)
        for (float cy : {gantryH - 0.35f, gantryH + 0.65f}) {
            for (float cz : {gz - 0.42f, gz + 0.42f}) {
                Primitives::AddBeam<TollMeshBuffer, TollVertex>(
                    buffer, colXLeft, cy, cz, colXRight, cy, cz,
                    0.18f, 0.18f, 1, 0.76f, 0.80f, 0.85f, 1.0f, 0.26f, 0.88f, 0.0f
                );
            }
        }
        int trussBays = std::max(8, static_cast<int>(totalWidth / 2.5f));
        for (int b = 0; b <= trussBays; ++b) {
            float bx = colXLeft + (static_cast<float>(b) / trussBays) * (colXRight - colXLeft);
            for (float cz : {gz - 0.42f, gz + 0.42f}) {
                Primitives::AddBeam<TollMeshBuffer, TollVertex>(
                    buffer, bx, gantryH - 0.35f, cz, bx, gantryH + 0.65f, cz,
                    0.10f, 0.10f, 1, 0.68f, 0.72f, 0.78f, 1.0f, 0.30f, 0.85f, 0.0f
                );
                if (b < trussBays) {
                    float bxNext = colXLeft + (static_cast<float>(b + 1) / trussBays) * (colXRight - colXLeft);
                    Primitives::AddBeam<TollMeshBuffer, TollVertex>(
                        buffer, bx, gantryH - 0.35f, cz, bxNext, gantryH + 0.65f, cz,
                        0.09f, 0.09f, 1, 0.64f, 0.68f, 0.74f, 1.0f, 0.30f, 0.85f, 0.0f
                    );
                }
            }
        }

        // RFID DSRC Transceiver Pods + ALPR Dual IR Cameras + LiDAR Scanners per lane
        float laneStartX = -(params.laneCount * params.laneWidthM) * 0.5f + params.laneWidthM * 0.5f;
        for (uint32_t lane = 0; lane < params.laneCount; ++lane) {
            float lx = laneStartX + lane * params.laneWidthM;
            // Transceiver Panel
            AddPlazaBox(buffer, lx, gantryH - 0.62f, gz, 0.95f, 0.38f, 0.35f,
                        1, 0.88f, 0.90f, 0.93f, 0.25f, 0.45f);
            // Dual ALPR IR Camera Housings
            if (params.hasALPRCameras) {
                AddPlazaBox(buffer, lx - 0.70f, gantryH - 0.55f, gz - 0.45f, 0.24f, 0.24f, 0.48f,
                            2, 0.16f, 0.18f, 0.22f, 0.20f, 0.75f);
                AddPlazaBox(buffer, lx + 0.70f, gantryH - 0.55f, gz - 0.45f, 0.24f, 0.24f, 0.48f,
                            2, 0.16f, 0.18f, 0.22f, 0.20f, 0.75f);
            }
            // Overhead Cyan LED Status Beacon
            AddPlazaBox(buffer, lx, gantryH + 0.15f, gz - 0.55f, 1.35f, 0.65f, 0.14f,
                        4, 0.10f, 0.92f, 0.45f, 0.15f, 0.10f, 4.5f);
        }
    }
}

void TollPlazaGenerator::BuildBorderInspectionBays(
    TollMeshBuffer& buffer,
    const TollParams& params
) {
    float totalWidth = params.laneCount * params.laneWidthM + (params.laneCount + 1) * params.boothIslandWidthM;
    float halfW = totalWidth * 0.5f;
    // Secondary Customs X-Ray Portal Arch flanking outside edge lane
    float bayX = -halfW - 2.6f;
    AddPlazaBox(buffer, bayX, 2.75f, 0.0f, 1.2f, 5.5f, 8.5f,
                1, 0.86f, 0.88f, 0.90f, 0.34f, 0.70f);
    AddPlazaBox(buffer, bayX + 1.8f, 5.2f, 0.0f, 3.8f, 0.85f, 8.5f,
                1, 0.86f, 0.88f, 0.90f, 0.34f, 0.70f);
    // Amber strobe inspection beacon
    AddPlazaBox(buffer, bayX + 1.8f, 5.75f, 0.0f, 0.75f, 0.35f, 0.75f,
                4, 1.0f, 0.55f, 0.08f, 0.15f, 0.10f, 4.5f);
}

void TollPlazaGenerator::BuildWIMScalesAndBypass(
    TollMeshBuffer& buffer,
    const TollParams& params
) {
    float laneStartX = -(params.laneCount * params.laneWidthM) * 0.5f + params.laneWidthM * 0.5f;
    for (uint32_t lane = 0; lane < params.laneCount; ++lane) {
        float lx = laneStartX + lane * params.laneWidthM;
        // Flush Brushed-Steel Dynamic Bending Plate Weighbridge Pads
        for (float wz : {-1.8f, 1.8f}) {
            AddPlazaBox(buffer, lx, 0.04f, wz, params.laneWidthM * 0.78f, 0.06f, 1.25f,
                        1, 0.52f, 0.56f, 0.62f, 0.28f, 0.90f);
            // Yellow rumble guide bars
            AddPlazaBox(buffer, lx - params.laneWidthM * 0.42f, 0.06f, wz, 0.18f, 0.10f, 2.2f,
                        3, 0.95f, 0.76f, 0.10f, 0.45f, 0.10f);
        }
    }
}

void TollPlazaGenerator::BuildVMSLaneSignals(
    TollMeshBuffer& buffer,
    const TollParams& params
) {
    bool isFreeFlow = (params.type == TollPlazaType::OverheadETCGantry ||
                       params.type == TollPlazaType::WeighInMotionPlaza);
    uint32_t effectiveLanes = (params.type == TollPlazaType::RampBarrierGate)
        ? std::min<uint32_t>(params.laneCount, 4u)
        : params.laneCount;

    float totalWidth = isFreeFlow
        ? (effectiveLanes * params.laneWidthM + 4.5f)
        : (effectiveLanes * params.laneWidthM + (effectiveLanes + 1) * params.boothIslandWidthM);
    float halfW = totalWidth * 0.5f;
    float islandPitch = isFreeFlow ? params.laneWidthM : (params.laneWidthM + params.boothIslandWidthM);
    float startX = isFreeFlow
        ? (-(effectiveLanes * params.laneWidthM) * 0.5f + params.laneWidthM * 0.5f)
        : (-halfW + params.boothIslandWidthM + params.laneWidthM * 0.5f);

    float sy = params.canopyClearanceM + 0.55f;
    float islandLen = (params.type == TollPlazaType::RampBarrierGate)
        ? std::min(params.boothIslandLengthM, 12.0f)
        : params.boothIslandLengthM;
    float sz = isFreeFlow ? 0.0f : -(islandLen * 0.5f + params.canopyOverhangM + 0.15f);

    for (uint32_t lane = 0; lane < effectiveLanes; ++lane) {
        float lx = startX + lane * islandPitch;
        // Signal Matrix Housing Cabinet
        AddPlazaBox(buffer, lx, sy, sz, 1.35f, 0.95f, 0.42f,
                    1, 0.12f, 0.14f, 0.17f, 0.35f, 0.70f);
        // Emissive LED Panel (Alternating ETC Express Cyan vs Open Green Arrow)
        bool isExpress = (lane % 2 == 0);
        AddPlazaBox(buffer, lx, sy, sz - 0.22f, 1.15f, 0.72f, 0.06f,
                    4,
                    isExpress ? 0.10f : 0.18f,
                    isExpress ? 0.92f : 0.96f,
                    isExpress ? 0.95f : 0.32f,
                    0.12f, 0.10f, params.ledEmissiveIntensity);
    }
}

} // namespace Road
} // namespace ApexAssets
