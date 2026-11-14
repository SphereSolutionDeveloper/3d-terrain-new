// ==============================================================================
// Modular Road Component: Bridge 3D Mesh Generator Implementation
// Path: native-assets/src/road/bridges/BridgeGenerator.cpp
// Standards: AASHTO LRFD Bridge Design & Eurocode EN 1991-2
// Follows Sascha Willems Vulkan mesh generation pipeline
// ==============================================================================
#include "BridgeGenerator.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>
#include <vector>

namespace ApexAssets {
namespace Road {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float DEG2RAD = PI / 180.0f;

void BridgeGenerator::SampleCenterline(
    float t,
    const BridgeParams& params,
    float& outX, float& outY, float& outZ,
    float& outTangentX, float& outTangentY, float& outTangentZ,
    float& outBankRad
) {
    float length = params.spanLengthM;
    
    float bendAngle = params.curveBendAngleDeg;
    if (params.type == BridgeType::CableStayedCurved && std::abs(bendAngle) < 0.01f) {
        bendAngle = 26.0f; // Authentic curve default for curved cable stayed
    }
    
    // Evaluate horizontal curve with bending manipulation
    if (std::abs(bendAngle) > 0.01f) {
        float totalBendRad = bendAngle * DEG2RAD;
        float radius = length / totalBendRad;
        float angle = (t - 0.5f) * totalBendRad;
        
        outX = radius * (1.0f - std::cos(angle));
        outZ = (t - 0.5f) * length;
        
        // Tangent unit vector
        outTangentX = std::sin(angle);
        outTangentZ = std::cos(angle);
        outTangentY = params.longitudinalGradePct * 0.01f;
        
        // Superelevation (banking) smoothly blended with curvature
        float bankSign = (bendAngle > 0.0f) ? 1.0f : -1.0f;
        outBankRad = bankSign * params.superelevationDeg * DEG2RAD * std::sin(t * PI);
    } else {
        // Straight axis along Z
        outX = 0.0f;
        outZ = (t - 0.5f) * length;
        outTangentX = 0.0f;
        outTangentY = params.longitudinalGradePct * 0.01f;
        outTangentZ = 1.0f;
        outBankRad = 0.0f;
    }
    
    // Vertical profile (deck clearance elevation + vertical parabola camber)
    float camber = 0.85f * std::sin(t * PI);
    outY = params.deckClearanceM + (t - 0.5f) * length * (params.longitudinalGradePct * 0.01f) + camber;
    
    // Normalize tangent
    float tLen = std::sqrt(outTangentX * outTangentX + outTangentY * outTangentY + outTangentZ * outTangentZ);
    if (tLen > 1e-6f) {
        outTangentX /= tLen;
        outTangentY /= tLen;
        outTangentZ /= tLen;
    }
}

static void FillMaterialPbr(uint32_t materialId, float& roughness, float& metallic, float& emissive) {
    switch (materialId) {
        case 0: roughness = 0.85f; metallic = 0.02f; emissive = 0.0f; break; // Asphalt
        case 1: roughness = 0.68f; metallic = 0.04f; emissive = 0.0f; break; // Concrete / Masonry
        case 2: roughness = 0.28f; metallic = 0.85f; emissive = 0.0f; break; // Steel / Cables / Bearings
        case 3: roughness = 0.55f; metallic = 0.05f; emissive = 0.0f; break; // Timber / Road Striping
        case 4: roughness = 0.20f; metallic = 0.10f; emissive = 5.0f; break; // Warning Beacons / LEDs
        default: roughness = 0.55f; metallic = 0.10f; emissive = 0.0f; break;
    }
}

void BridgeGenerator::AddOrientedBox(
    BridgeMeshBuffer& buffer,
    float cx, float cy, float cz,
    float dimNormal, float dimVertical, float dimTangent,
    float nx, float nz,
    float tx, float tz,
    uint32_t materialId,
    float r, float g, float b
) {
    float rough, metal, emis;
    FillMaterialPbr(materialId, rough, metal, emis);
    Primitives::AddOrientedBox<BridgeMeshBuffer, BridgeVertex>(
        buffer, cx, cy, cz, dimNormal, dimVertical, dimTangent,
        nx, nz, tx, tz, materialId, r, g, b, 1.0f, rough, metal, emis
    );
}

void BridgeGenerator::AddBox(
    BridgeMeshBuffer& buffer,
    float cx, float cy, float cz,
    float sx, float sy, float sz,
    uint32_t materialId,
    float r, float g, float b
) {
    AddOrientedBox(buffer, cx, cy, cz, sx, sy, sz, 1.0f, 0.0f, 0.0f, 1.0f, materialId, r, g, b);
}

void BridgeGenerator::AddBeam(
    BridgeMeshBuffer& buffer,
    float x1, float y1, float z1,
    float x2, float y2, float z2,
    float width, float height,
    uint32_t materialId,
    float r, float g, float b
) {
    float rough, metal, emis;
    FillMaterialPbr(materialId, rough, metal, emis);
    Primitives::AddBeam<BridgeMeshBuffer, BridgeVertex>(
        buffer, x1, y1, z1, x2, y2, z2, width, height,
        materialId, r, g, b, 1.0f, rough, metal, emis
    );
}

void BridgeGenerator::AddIntermediateSupportPier(
    BridgeMeshBuffer& buffer,
    float t,
    const BridgeParams& params,
    bool isPedestrian
) {
    float px, py, pz, tx, ty, tz, bank;
    SampleCenterline(t, params, px, py, pz, tx, ty, tz, bank);
    float nx = tz, nz = -tx;

    float halfW = isPedestrian ? std::clamp(params.deckWidthM * 0.5f, 1.6f, 4.2f) : params.deckWidthM * 0.5f;
    float depth = isPedestrian ? 0.55f : params.deckThicknessM;
    float footH = isPedestrian ? 1.2f : 2.2f;
    float seatY = py - depth - 0.35f;
    if (seatY <= footH + 1.0f) return;

    // 1. Grounded Foundation Plinth at Y=0
    AddOrientedBox(buffer, px, footH * 0.5f, pz, halfW * 1.9f, footH, isPedestrian ? 3.8f : 6.2f, nx, nz, tx, tz, 1, 0.48f, 0.51f, 0.55f);

    // 2. Pier Shafts (Cylindrical for pedestrian / sculptural bridges)
    float capH = isPedestrian ? 0.9f : 1.5f;
    float colH = std::max(0.8f, seatY - capH - footH);
    float shaftOffset = isPedestrian ? 0.0f : halfW * 0.45f;
    if (isPedestrian) {
        Primitives::AddCylinder<BridgeMeshBuffer, BridgeVertex>(
            buffer, px, footH, pz, px, seatY - capH, pz,
            0.95f, 0.78f, 12, 1, 0.68f, 0.72f, 0.76f, 1.0f, 0.65f, 0.05f, 0.0f
        );
    } else {
        for (int side : {-1, 1}) {
            float cx = px + nx * (shaftOffset * side);
            float cz = pz + nz * (shaftOffset * side);
            Primitives::AddCylinder<BridgeMeshBuffer, BridgeVertex>(
                buffer, cx, footH, cz, cx, seatY - capH, cz,
                1.35f, 1.15f, 12, 1, 0.68f, 0.72f, 0.76f, 1.0f, 0.65f, 0.05f, 0.0f
            );
        }
    }

    // 3. Hammerhead Pier Cap Beam
    AddOrientedBox(buffer, px, seatY - capH * 0.5f, pz, halfW * 1.85f, capH, isPedestrian ? 2.2f : 3.6f, nx, nz, tx, tz, 1, 0.62f, 0.66f, 0.70f);

    // 4. Pot Bearings beneath deck soffit
    for (int side : {-1, 1}) {
        float bx = px + nx * (halfW * 0.48f * side);
        float bz = pz + nz * (halfW * 0.48f * side);
        AddOrientedBox(buffer, bx, seatY + 0.18f, bz, isPedestrian ? 0.8f : 1.6f, 0.36f, isPedestrian ? 0.8f : 1.6f, nx, nz, tx, tz, 2, 0.20f, 0.22f, 0.25f);
    }
}

BridgeMeshBuffer BridgeGenerator::GenerateBridgeMesh(const BridgeParams& inputParams) {
    BridgeParams params = inputParams;
    BridgeMeshBuffer buffer;
    buffer.minAABB[0] = -params.deckWidthM * 0.9f;
    buffer.minAABB[1] = 0.0f;
    buffer.minAABB[2] = -params.spanLengthM * 0.65f;

    // 1. Extrude Segmental Superstructure / Deck according to Architectural Class
    bool isPedestrian = (params.type == BridgeType::PedestrianFootbridge ||
                         params.type == BridgeType::PedestrianStressRibbon ||
                         params.type == BridgeType::PedestrianCableStayed ||
                         params.type == BridgeType::PedestrianTimberArch ||
                         params.type == BridgeType::PedestrianCoveredSkywalk);

    if (isPedestrian && params.deckClearanceM > 12.0f) {
        // Real-world pedestrian overpass deck elevation (AASHTO LRFD Pedestrian Guide)
        params.deckClearanceM = 7.2f;
    }

    buffer.maxAABB[0] = params.deckWidthM * 0.9f;
    buffer.maxAABB[1] = params.deckClearanceM + params.pylonHeightM + 8.0f;
    buffer.maxAABB[2] = params.spanLengthM * 0.65f;

    bool isRoman = (params.type == BridgeType::MasonryViaductRoman);

    bool isTruss = (params.type == BridgeType::SteelTrussViaduct ||
                    params.type == BridgeType::CantileverTruss ||
                    params.type == BridgeType::SuspensionBridge);

    const int segments = 40;
    for (int i = 0; i < segments; ++i) {
        float t0 = static_cast<float>(i) / segments;
        float t1 = static_cast<float>(i + 1) / segments;
        if (params.type == BridgeType::PedestrianStressRibbon) {
            // Stress-ribbon extrudes its own catenary deck directly in BuildPedestrianStressRibbon
            continue;
        } else if (isPedestrian) {
            ExtrudePedestrianDeckSegment(buffer, t0, t1, params);
        } else if (isRoman) {
            ExtrudeRomanStoneDeckSegment(buffer, t0, t1, params);
        } else if (isTruss) {
            ExtrudeTrussDeckSegment(buffer, t0, t1, params);
        } else {
            ExtrudeHighwayDeckSegment(buffer, t0, t1, params);
        }
    }

    // 2. Build Substructure according to Bridge Type
    switch (params.type) {
        case BridgeType::CableStayed:
            BuildCableStayedPylonsAndCables(buffer, params);
            break;
        case BridgeType::SuspensionBridge:
            BuildSuspensionTowersAndCatenary(buffer, params);
            break;
        case BridgeType::PedestrianFootbridge:
            BuildPedestrianFootbridgeStructure(buffer, params);
            break;
        case BridgeType::ConcreteBoxGirder:
            BuildConcreteViaductPiers(buffer, params);
            break;
        case BridgeType::SteelTrussViaduct:
            BuildSteelTrussMembers(buffer, params);
            break;
        case BridgeType::ParabolicArch:
            BuildParabolicArchRibs(buffer, params);
            break;
        case BridgeType::ExtradosedBridge:
            BuildExtradosedPylonsAndStays(buffer, params);
            break;
        case BridgeType::BasculeDrawbridge:
            BuildBasculeDrawbridgeMachinery(buffer, params);
            break;
        case BridgeType::CantileverTruss:
            BuildCantileverTrussSuperstructure(buffer, params);
            break;
        case BridgeType::CableStayedCurved:
            BuildCurvedCableStayedPylon(buffer, params);
            break;
        case BridgeType::MasonryViaductRoman:
            BuildRomanMasonryArches(buffer, params);
            break;
        case BridgeType::PostTensionedOverpass:
            BuildPostTensionedOverpassPiers(buffer, params);
            break;
        case BridgeType::PedestrianStressRibbon:
            BuildPedestrianStressRibbon(buffer, params);
            break;
        case BridgeType::PedestrianCableStayed:
            BuildPedestrianCableStayed(buffer, params);
            break;
        case BridgeType::PedestrianTimberArch:
            BuildPedestrianTimberArch(buffer, params);
            break;
        case BridgeType::SteelPlateGirder:
            BuildSteelPlateGirders(buffer, params);
            break;
        case BridgeType::PedestrianCoveredSkywalk:
            BuildPedestrianCoveredSkywalk(buffer, params);
            break;
    }

    // Ground both ends of pedestrian footbridges cleanly to Y=0 (staircases, ADA switchback ramps, piers & entry plazas)
    if (isPedestrian) {
        BuildPedestrianSurfaceConnections(buffer, params);
    }

    // Cleanly cap highway bridge ends without bulky abutment blocks
    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);

    buffer.totalConcreteVolumeM3 = params.spanLengthM * params.deckWidthM * params.deckThicknessM * 0.46f;
    buffer.totalStructuralSteelTons = params.spanLengthM * (params.type == BridgeType::SteelTrussViaduct ? 18.5f : 6.2f);
    buffer.deadLoadKn = buffer.totalConcreteVolumeM3 * 25.0f * 9.81f;

    return buffer;
}

void BridgeGenerator::ExtrudeHighwayDeckSegment(
    BridgeMeshBuffer& buffer,
    float t0, float t1,
    const BridgeParams& params
) {
    float x0, y0, z0, tx0, ty0, tz0, bank0;
    float x1, y1, z1, tx1, ty1, tz1, bank1;
    SampleCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0, bank0);
    SampleCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1, bank1);

    float nx0 = tz0, nz0 = -tx0;
    float nLen0 = std::sqrt(nx0 * nx0 + nz0 * nz0);
    if (nLen0 > 1e-5f) { nx0 /= nLen0; nz0 /= nLen0; }

    float nx1 = tz1, nz1 = -tx1;
    float nLen1 = std::sqrt(nx1 * nx1 + nz1 * nz1);
    if (nLen1 > 1e-5f) { nx1 /= nLen1; nz1 /= nLen1; }

    float halfW = params.deckWidthM * 0.5f;
    float depth = params.deckThicknessM;
    float botW = halfW * 0.62f;
    float faciaH = 0.40f;

    float yL0 = y0 - std::sin(bank0) * halfW;
    float yR0 = y0 + std::sin(bank0) * halfW;
    float tl0[3] = { x0 - nx0 * halfW, yL0, z0 - nz0 * halfW };
    float tr0[3] = { x0 + nx0 * halfW, yR0, z0 + nz0 * halfW };
    float fl0[3] = { tl0[0], yL0 - faciaH, tl0[2] };
    float fr0[3] = { tr0[0], yR0 - faciaH, tr0[2] };
    float bl0[3] = { x0 - nx0 * botW, y0 - depth, z0 - nz0 * botW };
    float br0[3] = { x0 + nx0 * botW, y0 - depth, z0 + nz0 * botW };

    float yL1 = y1 - std::sin(bank1) * halfW;
    float yR1 = y1 + std::sin(bank1) * halfW;
    float tl1[3] = { x1 - nx1 * halfW, yL1, z1 - nz1 * halfW };
    float tr1[3] = { x1 + nx1 * halfW, yR1, z1 + nz1 * halfW };
    float fl1[3] = { tl1[0], yL1 - faciaH, tl1[2] };
    float fr1[3] = { tr1[0], yR1 - faciaH, tr1[2] };
    float bl1[3] = { x1 - nx1 * botW, y1 - depth, z1 - nz1 * botW };
    float br1[3] = { x1 + nx1 * botW, y1 - depth, z1 + nz1 * botW };

    auto addQuad = [&](
        const float c0[3], const float c1[3], const float c2[3], const float c3[3],
        const float norm[3], uint32_t matId, float r, float g, float b, float u0, float u1
    ) {
        float rough, metal, emis;
        FillMaterialPbr(matId, rough, metal, emis);
        Primitives::AddQuad<BridgeMeshBuffer, BridgeVertex>(
            buffer, c0, c1, c2, c3, norm, matId, r, g, b, 1.0f, u0, u1, 0.0f, 1.0f, rough, metal, emis
        );
    };

    // 1. Asphalt Roadway Wearing Course (Dark asphalt: 0.22, 0.24, 0.27)
    float nUp[3] = { 0.0f, 1.0f, 0.0f };
    addQuad(tl0, tr0, tr1, tl1, nUp, 0 /* Asphalt */, 0.22f, 0.24f, 0.27f, 0.0f, 1.0f);

    // 2. White Edge Fog Lines & Yellow Centerline
    float stripeY0 = y0 + 0.02f;
    float stripeY1 = y1 + 0.02f;
    // Yellow Center Stripe
    float cL0[3] = { x0 - nx0 * 0.15f, stripeY0, z0 - nz0 * 0.15f };
    float cR0[3] = { x0 + nx0 * 0.15f, stripeY0, z0 + nz0 * 0.15f };
    float cL1[3] = { x1 - nx1 * 0.15f, stripeY1, z1 - nz1 * 0.15f };
    float cR1[3] = { x1 + nx1 * 0.15f, stripeY1, z1 + nz1 * 0.15f };
    addQuad(cL0, cR0, cR1, cL1, nUp, 3, 0.95f, 0.82f, 0.20f, 0.0f, 1.0f);

    // 3. Concrete Cantilever Facias (Architectural concrete: 0.70, 0.72, 0.75)
    float nLeft[3] = { -nx0, 0.0f, -nz0 };
    addQuad(tl0, tl1, fl1, fl0, nLeft, 1 /* Concrete */, 0.70f, 0.72f, 0.75f, 0.0f, 1.0f);

    float nRight[3] = { nx0, 0.0f, nz0 };
    addQuad(tr0, fr0, fr1, tr1, nRight, 1, 0.70f, 0.72f, 0.75f, 0.0f, 1.0f);

    // 4. Inclined Box Girder Webs
    float nLeftWeb[3] = { -nx0 * 0.85f, -0.52f, -nz0 * 0.85f };
    addQuad(fl0, fl1, bl1, bl0, nLeftWeb, 1, 0.65f, 0.68f, 0.72f, 0.0f, 1.0f);

    float nRightWeb[3] = { nx0 * 0.85f, -0.52f, nz0 * 0.85f };
    addQuad(fr0, br0, br1, fr1, nRightWeb, 1, 0.65f, 0.68f, 0.72f, 0.0f, 1.0f);

    // 5. Bottom Soffit Slab
    float nDown[3] = { 0.0f, -1.0f, 0.0f };
    addQuad(bl0, bl1, br1, br0, nDown, 1, 0.58f, 0.60f, 0.64f, 0.0f, 1.0f);

    // 6. Solid 3D Concrete New Jersey Barriers (Realistic Concrete Gray: 0.72, 0.74, 0.76)
    if (params.hasConcreteNewJerseyBarrier) {
        float bH = 0.95f;
        float bW = 0.45f;
        float bTW = 0.30f;

        float lb_ob0[3] = { tl0[0], tl0[1], tl0[2] };
        float lb_ib0[3] = { tl0[0] + nx0 * bW, tl0[1], tl0[2] + nz0 * bW };
        float lb_ot0[3] = { tl0[0], tl0[1] + bH, tl0[2] };
        float lb_it0[3] = { tl0[0] + nx0 * bTW, tl0[1] + bH, tl0[2] + nz0 * bTW };

        float lb_ob1[3] = { tl1[0], tl1[1], tl1[2] };
        float lb_ib1[3] = { tl1[0] + nx1 * bW, tl1[1], tl1[2] + nz1 * bW };
        float lb_ot1[3] = { tl1[0], tl1[1] + bH, tl1[2] };
        float lb_it1[3] = { tl1[0] + nx1 * bTW, tl1[1] + bH, tl1[2] + nz1 * bTW };

        addQuad(lb_ib0, lb_it0, lb_it1, lb_ib1, nRight, 1, 0.72f, 0.74f, 0.76f, 0.0f, 1.0f);
        addQuad(lb_it0, lb_ot0, lb_ot1, lb_it1, nUp, 1, 0.76f, 0.78f, 0.80f, 0.0f, 1.0f);
        addQuad(lb_ot0, lb_ob0, lb_ob1, lb_ot1, nLeft, 1, 0.68f, 0.70f, 0.73f, 0.0f, 1.0f);

        float rb_ob0[3] = { tr0[0], tr0[1], tr0[2] };
        float rb_ib0[3] = { tr0[0] - nx0 * bW, tr0[1], tr0[2] - nz0 * bW };
        float rb_ot0[3] = { tr0[0], tr0[1] + bH, tr0[2] };
        float rb_it0[3] = { tr0[0] - nx0 * bTW, tr0[1] + bH, tr0[2] - nz0 * bTW };

        float rb_ob1[3] = { tr1[0], tr1[1], tr1[2] };
        float rb_ib1[3] = { tr1[0] - nx1 * bW, tr1[1], tr1[2] - nz1 * bW };
        float rb_ot1[3] = { tr1[0], tr1[1] + bH, tr1[2] };
        float rb_it1[3] = { tr1[0] - nx1 * bTW, tr1[1] + bH, tr1[2] - nz1 * bTW };

        addQuad(rb_ib0, rb_ib1, rb_it1, rb_it0, nLeft, 1, 0.72f, 0.74f, 0.76f, 0.0f, 1.0f);
        addQuad(rb_it0, rb_it1, rb_ot1, rb_ot0, nUp, 1, 0.76f, 0.78f, 0.80f, 0.0f, 1.0f);
        addQuad(rb_ot0, rb_ot1, rb_ob1, rb_ob0, nRight, 1, 0.68f, 0.70f, 0.73f, 0.0f, 1.0f);
    }
}

void BridgeGenerator::ExtrudePedestrianDeckSegment(
    BridgeMeshBuffer& buffer,
    float t0, float t1,
    const BridgeParams& params
) {
    float x0, y0, z0, tx0, ty0, tz0, bank0;
    float x1, y1, z1, tx1, ty1, tz1, bank1;
    SampleCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0, bank0);
    SampleCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1, bank1);

    float nx0 = tz0, nz0 = -tx0;
    float nLen0 = std::sqrt(nx0 * nx0 + nz0 * nz0);
    if (nLen0 > 1e-5f) { nx0 /= nLen0; nz0 /= nLen0; }

    float nx1 = tz1, nz1 = -tx1;
    float nLen1 = std::sqrt(nx1 * nx1 + nz1 * nz1);
    if (nLen1 > 1e-5f) { nx1 /= nLen1; nz1 /= nLen1; }

    float pedHalfW = std::clamp(params.deckWidthM * 0.5f, 1.6f, 4.2f); // Human-scaled pedestrian promenade

    float p0L[3] = { x0 - nx0 * pedHalfW, y0, z0 - nz0 * pedHalfW };
    float p0R[3] = { x0 + nx0 * pedHalfW, y0, z0 + nz0 * pedHalfW };
    float p1L[3] = { x1 - nx1 * pedHalfW, y1, z1 - nz1 * pedHalfW };
    float p1R[3] = { x1 + nx1 * pedHalfW, y1, z1 + nz1 * pedHalfW };

    auto addQuad = [&](
        const float c0[3], const float c1[3], const float c2[3], const float c3[3],
        const float norm[3], uint32_t matId, float r, float g, float b
    ) {
        float rough, metal, emis;
        FillMaterialPbr(matId, rough, metal, emis);
        Primitives::AddQuad<BridgeMeshBuffer, BridgeVertex>(
            buffer, c0, c1, c2, c3, norm, matId, r, g, b, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, rough, metal, emis
        );
    };

    // 1. Natural Teak/Oak Walking Planks (Warm timber: 0.52, 0.38, 0.26)
    float nUp[3] = { 0.0f, 1.0f, 0.0f };
    addQuad(p0L, p0R, p1R, p1L, nUp, 3 /* Timber */, 0.52f, 0.38f, 0.26f);

    // 2. Under-Deck Longitudinal Steel Box Edge Stringers
    for (int side : {-1, 1}) {
        float xS0 = x0 + nx0 * (pedHalfW * side);
        float zS0 = z0 + nz0 * (pedHalfW * side);
        float xS1 = x1 + nx1 * (pedHalfW * side);
        float zS1 = z1 + nz1 * (pedHalfW * side);
        AddBeam(buffer, xS0, y0 - 0.25f, zS0, xS1, y1 - 0.25f, zS1, 0.35f, 0.45f, 2 /* Steel */, 0.72f, 0.75f, 0.78f);
    }

    // 3. Transverse Floorbeam Outriggers underneath deck
    AddBeam(buffer, p0L[0] - nx0 * 0.4f, y0 - 0.45f, p0L[2] - nz0 * 0.4f, p0R[0] + nx0 * 0.4f, y0 - 0.45f, p0R[2] + nz0 * 0.4f, 0.25f, 0.40f, 2, 0.65f, 0.68f, 0.72f);

    // 4. Stainless Steel Architectural Balustrades (Brushed steel: 0.82, 0.86, 0.90)
    float railH = 1.15f;
    for (int side : {-1, 1}) {
        float railX0 = x0 + nx0 * (pedHalfW * side);
        float railZ0 = z0 + nz0 * (pedHalfW * side);
        float railX1 = x1 + nx1 * (pedHalfW * side);
        float railZ1 = z1 + nz1 * (pedHalfW * side);

        // Top Handrail
        AddBeam(buffer, railX0, y0 + railH, railZ0, railX1, y1 + railH, railZ1, 0.10f, 0.10f, 2, 0.85f, 0.88f, 0.92f);
        // Mid-height safety wire rail
        AddBeam(buffer, railX0, y0 + railH * 0.55f, railZ0, railX1, y1 + railH * 0.55f, railZ1, 0.05f, 0.05f, 2, 0.78f, 0.82f, 0.85f);
        // Lower curb kick rail
        AddBeam(buffer, railX0, y0 + 0.12f, railZ0, railX1, y1 + 0.12f, railZ1, 0.08f, 0.15f, 2, 0.70f, 0.74f, 0.78f);

        // Vertical Stanchions
        AddBeam(buffer, railX0, y0, railZ0, railX0, y0 + railH, railZ0, 0.08f, 0.08f, 2, 0.80f, 0.84f, 0.88f);
    }
}

void BridgeGenerator::ExtrudeRomanStoneDeckSegment(
    BridgeMeshBuffer& buffer,
    float t0, float t1,
    const BridgeParams& params
) {
    float x0, y0, z0, tx0, ty0, tz0, bank0;
    float x1, y1, z1, tx1, ty1, tz1, bank1;
    SampleCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0, bank0);
    SampleCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1, bank1);

    float nx0 = tz0, nz0 = -tx0;
    float nLen0 = std::sqrt(nx0 * nx0 + nz0 * nz0);
    if (nLen0 > 1e-5f) { nx0 /= nLen0; nz0 /= nLen0; }

    float nx1 = tz1, nz1 = -tx1;
    float nLen1 = std::sqrt(nx1 * nx1 + nz1 * nz1);
    if (nLen1 > 1e-5f) { nx1 /= nLen1; nz1 /= nLen1; }

    float halfW = params.deckWidthM * 0.42f;
    float parapetH = 1.1f;
    float parapetW = 0.65f;

    // Stone road paved surface (Limestone: 0.58, 0.55, 0.50)
    float p0L[3] = { x0 - nx0 * halfW, y0, z0 - nz0 * halfW };
    float p0R[3] = { x0 + nx0 * halfW, y0, z0 + nz0 * halfW };
    float p1L[3] = { x1 - nx1 * halfW, y1, z1 - nz1 * halfW };
    float p1R[3] = { x1 + nx1 * halfW, y1, z1 + nz1 * halfW };

    const float normUp[3] = { 0.0f, 1.0f, 0.0f };
    Primitives::AddQuad<BridgeMeshBuffer, BridgeVertex>(
        buffer, p0L, p0R, p1R, p1L, normUp, 1, 0.58f, 0.55f, 0.50f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.85f, 0.02f, 0.0f
    );

    // Continuous Classical Ashlar Stone Parapets
    for (int side : {-1, 1}) {
        float bX0 = x0 + nx0 * ((halfW + parapetW * 0.5f) * side);
        float bZ0 = z0 + nz0 * ((halfW + parapetW * 0.5f) * side);
        float bX1 = x1 + nx1 * ((halfW + parapetW * 0.5f) * side);
        float bZ1 = z1 + nz1 * ((halfW + parapetW * 0.5f) * side);

        AddBeam(buffer, bX0, y0 + parapetH * 0.5f, bZ0, bX1, y1 + parapetH * 0.5f, bZ1, parapetW, parapetH, 1, 0.62f, 0.58f, 0.52f);
        AddBeam(buffer, bX0, y0 + parapetH + 0.10f, bZ0, bX1, y1 + parapetH + 0.10f, bZ1, parapetW * 1.25f, 0.20f, 1, 0.66f, 0.62f, 0.56f);
    }

    AddBeam(buffer, x0 - nx0 * (halfW + parapetW), y0 - 0.35f, z0 - nz0 * (halfW + parapetW),
                   x1 - nx1 * (halfW + parapetW), y1 - 0.35f, z1 - nz1 * (halfW + parapetW),
                   0.5f, 0.6f, 1, 0.50f, 0.46f, 0.42f);
    AddBeam(buffer, x0 + nx0 * (halfW + parapetW), y0 - 0.35f, z0 + nz0 * (halfW + parapetW),
                   x1 + nx1 * (halfW + parapetW), y1 - 0.35f, z1 + nz1 * (halfW + parapetW),
                   0.5f, 0.6f, 1, 0.50f, 0.46f, 0.42f);
}

void BridgeGenerator::ExtrudeTrussDeckSegment(
    BridgeMeshBuffer& buffer,
    float t0, float t1,
    const BridgeParams& params
) {
    float x0, y0, z0, tx0, ty0, tz0, bank0;
    float x1, y1, z1, tx1, ty1, tz1, bank1;
    SampleCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0, bank0);
    SampleCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1, bank1);

    float nx0 = tz0, nz0 = -tx0;
    float nLen0 = std::sqrt(nx0 * nx0 + nz0 * nz0);
    if (nLen0 > 1e-5f) { nx0 /= nLen0; nz0 /= nLen0; }

    float nx1 = tz1, nz1 = -tx1;
    float nLen1 = std::sqrt(nx1 * nx1 + nz1 * nz1);
    if (nLen1 > 1e-5f) { nx1 /= nLen1; nz1 /= nLen1; }

    float halfW = params.deckWidthM * 0.5f;

    float tl0[3] = { x0 - nx0 * (halfW - 0.4f), y0, z0 - nz0 * (halfW - 0.4f) };
    float tr0[3] = { x0 + nx0 * (halfW - 0.4f), y0, z0 + nz0 * (halfW - 0.4f) };
    float tl1[3] = { x1 - nx1 * (halfW - 0.4f), y1, z1 - nz1 * (halfW - 0.4f) };
    float tr1[3] = { x1 + nx1 * (halfW - 0.4f), y1, z1 + nz1 * (halfW - 0.4f) };

    const float normUp[3] = { 0.0f, 1.0f, 0.0f };
    Primitives::AddQuad<BridgeMeshBuffer, BridgeVertex>(
        buffer, tl0, tr0, tr1, tl1, normUp, 0, 0.22f, 0.24f, 0.27f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.85f, 0.02f, 0.0f
    );

    // Structural steel floorbeams beneath deck
    AddBeam(buffer, x0 - nx0 * halfW, y0 - 0.6f, z0 - nz0 * halfW,
                   x0 + nx0 * halfW, y0 - 0.6f, z0 + nz0 * halfW,
                   0.65f, 0.95f, 2, 0.68f, 0.72f, 0.76f);

    // Longitudinal stringer beams
    for (float f : {-0.55f, 0.0f, 0.55f}) {
        float sx0 = x0 + nx0 * (halfW * f);
        float sz0 = z0 + nz0 * (halfW * f);
        float sx1 = x1 + nx1 * (halfW * f);
        float sz1 = z1 + nz1 * (halfW * f);
        AddBeam(buffer, sx0, y0 - 0.35f, sz0, sx1, y1 - 0.35f, sz1, 0.35f, 0.45f, 2, 0.62f, 0.66f, 0.70f);
    }

    // Steel W-beam guardrails
    for (int side : {-1, 1}) {
        float gX0 = x0 + nx0 * ((halfW - 0.25f) * side);
        float gZ0 = z0 + nz0 * ((halfW - 0.25f) * side);
        float gX1 = x1 + nx1 * ((halfW - 0.25f) * side);
        float gZ1 = z1 + nz1 * ((halfW - 0.25f) * side);
        AddBeam(buffer, gX0, y0 + 0.82f, gZ0, gX1, y1 + 0.82f, gZ1, 0.20f, 0.35f, 2, 0.85f, 0.88f, 0.92f);
        AddBeam(buffer, gX0, y0 + 0.35f, gZ0, gX1, y1 + 0.35f, gZ1, 0.15f, 0.25f, 2, 0.78f, 0.82f, 0.86f);
    }
}

void BridgeGenerator::BuildCableStayedPylonsAndCables(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    std::vector<float> pylonT = params.twinPylons ? std::vector<float>{0.28f, 0.72f} : std::vector<float>{0.5f};
    float halfW = params.deckWidthM * 0.5f;
    float depth = params.deckThicknessM;
    float footH = 3.2f;

    for (float pt : pylonT) {
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float mastH = params.pylonHeightM;

        // 1. Monolithic Ground Foundation Pile Cap / Plinth at Ground Level (Y = 0 to 3.2m)
        AddOrientedBox(buffer, px, footH * 0.5f, pz, params.deckWidthM * 1.55f, footH, 13.0f, nx, nz, tx, tz, 1, 0.50f, 0.53f, 0.57f);
        // Pedestal collar on top of footing
        AddOrientedBox(buffer, px, footH + 0.4f, pz, params.deckWidthM * 1.42f, 0.8f, 11.5f, nx, nz, tx, tz, 1, 0.58f, 0.62f, 0.66f);

        // 2. Transverse Lower Crosshead Beam (Transom) directly beneath deck
        float seatY = py - depth - 0.45f;
        AddOrientedBox(buffer, px, seatY - 1.2f, pz, params.deckWidthM * 1.20f, 2.4f, 4.8f, nx, nz, tx, tz, 1, 0.64f, 0.67f, 0.72f);

        // 3. Spherical Pot Bearings seated on crosshead beam directly beneath deck soffit
        for (int side : {-1, 1}) {
            AddOrientedBox(buffer, px + nx * (halfW * 0.55f * side), seatY + 0.22f, pz + nz * (halfW * 0.55f * side), 2.2f, 0.44f, 2.2f, nx, nz, tx, tz, 2, 0.18f, 0.20f, 0.24f);
        }

        // 4. Continuous Lower Pylon Legs (from foundation pedestal footH+0.8m up to deck py)
        float legBaseY = footH + 0.8f;
        float legBaseOffset = halfW + 3.2f;
        float legDeckOffset = halfW + 1.2f;
        AddBeam(buffer, px - nx * legBaseOffset, legBaseY, pz - nz * legBaseOffset, px - nx * legDeckOffset, py, pz - nz * legDeckOffset, 3.2f, 3.6f, 1, 0.76f, 0.79f, 0.84f);
        AddBeam(buffer, px + nx * legBaseOffset, legBaseY, pz + nz * legBaseOffset, px + nx * legDeckOffset, py, pz + nz * legDeckOffset, 3.2f, 3.6f, 1, 0.76f, 0.79f, 0.84f);

        // 5. Continuous Upper Pylon Legs (from deck py inward to apex junction)
        float apexY = py + 11.5f;
        AddBeam(buffer, px - nx * legDeckOffset, py, pz - nz * legDeckOffset, px - nx * 2.0f, apexY, pz - nz * 2.0f, 3.0f, 3.4f, 1, 0.80f, 0.83f, 0.88f);
        AddBeam(buffer, px + nx * legDeckOffset, py, pz + nz * legDeckOffset, px + nx * 2.0f, apexY, pz + nz * 2.0f, 3.0f, 3.4f, 1, 0.80f, 0.83f, 0.88f);

        // 6. Overhead Portal Sway Cross-Strut at 7.5m roadway clearance
        AddOrientedBox(buffer, px, py + 7.5f, pz, params.deckWidthM * 0.82f, 2.0f, 3.2f, nx, nz, tx, tz, 1, 0.68f, 0.72f, 0.76f);

        // 7. Monolithic Upper Cable Anchorage Mast
        float mastTopY = py + mastH;
        float mastMidY = (apexY + mastTopY) * 0.5f;
        float mastHeight = mastTopY - apexY;
        AddOrientedBox(buffer, px, mastMidY, pz, 4.4f, mastHeight, 5.4f, nx, nz, tx, tz, 1, 0.85f, 0.88f, 0.92f);

        // Crown saddle cap and aviation warning lights
        AddOrientedBox(buffer, px, mastTopY + 1.2f, pz, 5.2f, 2.4f, 6.2f, nx, nz, tx, tz, 1, 0.50f, 0.54f, 0.58f);
        AddOrientedBox(buffer, px - nx * 2.0f, mastTopY + 2.6f, pz - nz * 2.0f, 0.5f, 0.8f, 0.5f, nx, nz, tx, tz, 4, 1.0f, 0.15f, 0.1f);
        AddOrientedBox(buffer, px + nx * 2.0f, mastTopY + 2.6f, pz + nz * 2.0f, 0.5f, 0.8f, 0.5f, nx, nz, tx, tz, 4, 1.0f, 0.15f, 0.1f);

        // 8. 3D Stay Cables
        uint32_t pairs = (params.cablePairsCount > 0) ? params.cablePairsCount : 10;
        bool isFirstPylon = (pt < 0.5f);
        float maxInwardOffset = 0.165f;
        float maxOutwardOffset = 0.22f;

        for (uint32_t c = 1; c <= pairs; ++c) {
            float frac = static_cast<float>(c) / (pairs + 1);
            float cableAnchorY = apexY + frac * (mastTopY - apexY) * 0.85f;

            float tInward = isFirstPylon ? (pt + frac * maxInwardOffset) : (pt - frac * maxInwardOffset);
            float inX, inY, inZ, inTx, inTy, inTz, inBank;
            SampleCenterline(tInward, params, inX, inY, inZ, inTx, inTy, inTz, inBank);
            float inNx = inTz, inNz = -inTx;

            float inLeftX = inX - inNx * (halfW - 0.8f);
            float inLeftZ = inZ - inNz * (halfW - 0.8f);
            AddBeam(buffer, px - nx * 1.8f, cableAnchorY, pz - nz * 1.8f, inLeftX, inY + 0.6f, inLeftZ, 0.18f, 0.18f, 2, 0.92f, 0.94f, 0.96f);
            AddOrientedBox(buffer, inLeftX, inY + 0.3f, inLeftZ, 0.9f, 0.6f, 0.9f, inNx, inNz, inTx, inTz, 2, 0.30f, 0.34f, 0.38f);

            float inRightX = inX + inNx * (halfW - 0.8f);
            float inRightZ = inZ + inNz * (halfW - 0.8f);
            AddBeam(buffer, px + nx * 1.8f, cableAnchorY, pz + nz * 1.8f, inRightX, inY + 0.6f, inRightZ, 0.18f, 0.18f, 2, 0.92f, 0.94f, 0.96f);
            AddOrientedBox(buffer, inRightX, inY + 0.3f, inRightZ, 0.9f, 0.6f, 0.9f, inNx, inNz, inTx, inTz, 2, 0.30f, 0.34f, 0.38f);

            float tOutward = isFirstPylon ? (pt - frac * maxOutwardOffset) : (pt + frac * maxOutwardOffset);
            float outX, outY, outZ, outTx, outTy, outTz, outBank;
            SampleCenterline(tOutward, params, outX, outY, outZ, outTx, outTy, outTz, outBank);
            float outNx = outTz, outNz = -outTx;

            float outLeftX = outX - outNx * (halfW - 0.8f);
            float outLeftZ = outZ - outNz * (halfW - 0.8f);
            AddBeam(buffer, px - nx * 1.8f, cableAnchorY, pz - nz * 1.8f, outLeftX, outY + 0.6f, outLeftZ, 0.18f, 0.18f, 2, 0.92f, 0.94f, 0.96f);
            AddOrientedBox(buffer, outLeftX, outY + 0.3f, outLeftZ, 0.9f, 0.6f, 0.9f, outNx, outNz, outTx, outTz, 2, 0.30f, 0.34f, 0.38f);

            float outRightX = outX + outNx * (halfW - 0.8f);
            float outRightZ = outZ + outNz * (halfW - 0.8f);
            AddBeam(buffer, px + nx * 1.8f, cableAnchorY, pz + nz * 1.8f, outRightX, outY + 0.6f, outRightZ, 0.18f, 0.18f, 2, 0.92f, 0.94f, 0.96f);
            AddOrientedBox(buffer, outRightX, outY + 0.3f, outRightZ, 0.9f, 0.6f, 0.9f, outNx, outNz, outTx, outTz, 2, 0.30f, 0.34f, 0.38f);
        }
    }

    // 9. Side Span Anchor Piers (Tie-Down Bents) to support deck and resist backstay uplift
    for (float sideT : {0.14f, 0.86f}) {
        float sx, sy, sz, stx, sty, stz, sbank;
        SampleCenterline(sideT, params, sx, sy, sz, stx, sty, stz, sbank);
        float snx = stz, snz = -stx;

        // Ground footing plinth at Y=0
        AddOrientedBox(buffer, sx, 1.2f, sz, halfW * 1.8f, 2.4f, 6.0f, snx, snz, stx, stz, 1, 0.48f, 0.51f, 0.55f);

        // Twin column shafts
        float pierSeatY = sy - depth - 0.45f;
        float colH = std::max(1.0f, pierSeatY - 2.4f);
        for (int side : {-1, 1}) {
            float cx = sx + snx * (halfW * 0.45f * side);
            float cz = sz + snz * (halfW * 0.45f * side);
            AddOrientedBox(buffer, cx, 2.4f + colH * 0.5f, cz, 2.2f, colH, 2.8f, snx, snz, stx, stz, 1, 0.66f, 0.70f, 0.74f);
        }

        // Transverse pier cap crosshead
        AddOrientedBox(buffer, sx, pierSeatY - 0.55f, sz, halfW * 1.6f, 1.1f, 3.4f, snx, snz, stx, stz, 1, 0.62f, 0.66f, 0.70f);

        // Tie-down bearings beneath deck
        for (int side : {-1, 1}) {
            float bx = sx + snx * (halfW * 0.45f * side);
            float bz = sz + snz * (halfW * 0.45f * side);
            AddOrientedBox(buffer, bx, pierSeatY + 0.18f, bz, 1.4f, 0.36f, 1.4f, snx, snz, stx, stz, 2, 0.20f, 0.22f, 0.25f);
        }
    }
}

void BridgeGenerator::BuildConcreteViaductPiers(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    uint32_t piers = std::max(2u, params.pierCount);
    float halfW = params.deckWidthM * 0.5f;

    for (uint32_t p = 1; p <= piers; ++p) {
        float pt = static_cast<float>(p) / (piers + 1);
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float pierW = params.pierWidthM;
        float depth = params.deckThicknessM;
        float footH = 2.4f;
        float seatY = py - depth - 0.45f;

        // 1. Grounded Footing / Pile Cap at y=0 (0 to 2.4m)
        AddOrientedBox(buffer, px, footH * 0.5f, pz, params.deckWidthM * 1.1f, footH, 7.5f, nx, nz, tx, tz, 1, 0.48f, 0.51f, 0.55f);

        // 2. Monolithic Flared Pier Columns from footing to pier cap
        float capH = 1.6f;
        float capBottomY = seatY - capH;
        float colH = std::max(1.0f, capBottomY - footH);
        float colMidY = footH + colH * 0.5f;
        for (int side : {-1, 1}) {
            float colX = px + nx * (halfW * 0.42f * side);
            float colZ = pz + nz * (halfW * 0.42f * side);
            AddOrientedBox(buffer, colX, colMidY, colZ, pierW, colH, 3.4f, nx, nz, tx, tz, 1, 0.68f, 0.72f, 0.76f);
        }

        // 3. Monolithic Flared Hammerhead Pier Cap
        AddOrientedBox(buffer, px, seatY - capH * 0.5f, pz, params.deckWidthM * 0.92f, capH, 4.2f, nx, nz, tx, tz, 1, 0.62f, 0.66f, 0.70f);

        // 4. Heavy Pot Bearings beneath box girder webs
        for (int side : {-1, 1}) {
            float bX = px + nx * (halfW * 0.48f * side);
            float bZ = pz + nz * (halfW * 0.48f * side);
            AddOrientedBox(buffer, bX, seatY + 0.22f, bZ, 1.8f, 0.44f, 1.8f, nx, nz, tx, tz, 2, 0.18f, 0.20f, 0.24f);
        }
    }
}

void BridgeGenerator::BuildSteelTrussMembers(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    const int panels = 12;
    float trussH = 9.8f;
    float halfW = params.deckWidthM * 0.52f;

    struct Node {
        float botL[3], botR[3];
        float topL[3], topR[3];
    };
    std::vector<Node> nodes(panels + 1);

    for (int p = 0; p <= panels; ++p) {
        float t = static_cast<float>(p) / panels;
        float x, y, z, tx, ty, tz, bank;
        SampleCenterline(t, params, x, y, z, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        nodes[p].botL[0] = x - nx * halfW; nodes[p].botL[1] = y; nodes[p].botL[2] = z - nz * halfW;
        nodes[p].botR[0] = x + nx * halfW; nodes[p].botR[1] = y; nodes[p].botR[2] = z + nz * halfW;

        nodes[p].topL[0] = nodes[p].botL[0]; nodes[p].topL[1] = y + trussH; nodes[p].topL[2] = nodes[p].botL[2];
        nodes[p].topR[0] = nodes[p].botR[0]; nodes[p].topR[1] = y + trussH; nodes[p].topR[2] = nodes[p].botR[2];
    }

    for (int p = 0; p < panels; ++p) {
        const auto& nA = nodes[p];
        const auto& nB = nodes[p + 1];

        // Bottom Chords
        AddBeam(buffer, nA.botL[0], nA.botL[1], nA.botL[2], nB.botL[0], nB.botL[1], nB.botL[2], 0.65f, 0.65f, 2, 0.78f, 0.82f, 0.86f);
        AddBeam(buffer, nA.botR[0], nA.botR[1], nA.botR[2], nB.botR[0], nB.botR[1], nB.botR[2], 0.65f, 0.65f, 2, 0.78f, 0.82f, 0.86f);

        // Top Chords
        AddBeam(buffer, nA.topL[0], nA.topL[1], nA.topL[2], nB.topL[0], nB.topL[1], nB.topL[2], 0.75f, 0.75f, 2, 0.82f, 0.86f, 0.90f);
        AddBeam(buffer, nA.topR[0], nA.topR[1], nA.topR[2], nB.topR[0], nB.topR[1], nB.topR[2], 0.75f, 0.75f, 2, 0.82f, 0.86f, 0.90f);

        // Vertical Posts
        AddBeam(buffer, nA.botL[0], nA.botL[1], nA.botL[2], nA.topL[0], nA.topL[1], nA.topL[2], 0.55f, 0.55f, 2, 0.74f, 0.78f, 0.82f);
        AddBeam(buffer, nA.botR[0], nA.botR[1], nA.botR[2], nA.topR[0], nA.topR[1], nA.topR[2], 0.55f, 0.55f, 2, 0.74f, 0.78f, 0.82f);

        // Warren Diagonals
        if (p % 2 == 0) {
            AddBeam(buffer, nA.botL[0], nA.botL[1], nA.botL[2], nB.topL[0], nB.topL[1], nB.topL[2], 0.52f, 0.52f, 2, 0.72f, 0.76f, 0.80f);
            AddBeam(buffer, nA.botR[0], nA.botR[1], nA.botR[2], nB.topR[0], nB.topR[1], nB.topR[2], 0.52f, 0.52f, 2, 0.72f, 0.76f, 0.80f);
        } else {
            AddBeam(buffer, nA.topL[0], nA.topL[1], nA.topL[2], nB.botL[0], nB.botL[1], nB.botL[2], 0.52f, 0.52f, 2, 0.72f, 0.76f, 0.80f);
            AddBeam(buffer, nA.topR[0], nA.topR[1], nA.topR[2], nB.botR[0], nB.botR[1], nB.botR[2], 0.52f, 0.52f, 2, 0.72f, 0.76f, 0.80f);
        }

        // Transverse Overhead Portal Sway Struts
        AddBeam(buffer, nA.topL[0], nA.topL[1], nA.topL[2], nA.topR[0], nA.topR[1], nA.topR[2], 0.48f, 0.48f, 2, 0.70f, 0.74f, 0.78f);

        // Overhead X-Bracing
        AddBeam(buffer, nA.topL[0], nA.topL[1], nA.topL[2], nB.topR[0], nB.topR[1], nB.topR[2], 0.22f, 0.22f, 2, 0.88f, 0.90f, 0.94f);
        AddBeam(buffer, nA.topR[0], nA.topR[1], nA.topR[2], nB.topL[0], nB.topL[1], nB.topL[2], 0.22f, 0.22f, 2, 0.88f, 0.90f, 0.94f);
    }

    // Grounded Intermediate Truss Support Piers (inset from bridge ends)
    AddIntermediateSupportPier(buffer, 0.18f, params, false);
    AddIntermediateSupportPier(buffer, 0.82f, params, false);
}

void BridgeGenerator::BuildParabolicArchRibs(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    const int archSegs = 24;
    float archApexH = params.pylonHeightM * 0.85f;
    float halfW = params.deckWidthM * 0.52f;

    struct ArchNode {
        float left[3], right[3];
        float deckL[3], deckR[3];
        float y;
    };
    std::vector<ArchNode> nodes(archSegs + 1);

    for (int i = 0; i <= archSegs; ++i) {
        float t = static_cast<float>(i) / archSegs;
        float x, y, z, tx, ty, tz, bank;
        SampleCenterline(t, params, x, y, z, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float archY = y + 4.0f * archApexH * t * (1.0f - t) - params.deckThicknessM * 0.5f;
        nodes[i].y = archY;

        nodes[i].left[0] = x - nx * halfW; nodes[i].left[1] = archY; nodes[i].left[2] = z - nz * halfW;
        nodes[i].right[0] = x + nx * halfW; nodes[i].right[1] = archY; nodes[i].right[2] = z + nz * halfW;

        nodes[i].deckL[0] = x - nx * halfW; nodes[i].deckL[1] = y + 0.3f; nodes[i].deckL[2] = z - nz * halfW;
        nodes[i].deckR[0] = x + nx * halfW; nodes[i].deckR[1] = y + 0.3f; nodes[i].deckR[2] = z + nz * halfW;
    }

    for (int i = 0; i < archSegs; ++i) {
        const auto& nA = nodes[i];
        const auto& nB = nodes[i + 1];

        AddBeam(buffer, nA.left[0], nA.left[1], nA.left[2], nB.left[0], nB.left[1], nB.left[2], 0.95f, 1.4f, 1, 0.85f, 0.88f, 0.92f);
        AddBeam(buffer, nA.right[0], nA.right[1], nA.right[2], nB.right[0], nB.right[1], nB.right[2], 0.95f, 1.4f, 1, 0.85f, 0.88f, 0.92f);

        if (nA.y > nA.deckL[1] + 1.2f && i % 2 == 0) {
            AddBeam(buffer, nA.left[0], nA.left[1], nA.left[2], nA.deckL[0], nA.deckL[1], nA.deckL[2], 0.12f, 0.12f, 2, 0.92f, 0.94f, 0.96f);
            AddBeam(buffer, nA.right[0], nA.right[1], nA.right[2], nA.deckR[0], nA.deckR[1], nA.deckR[2], 0.12f, 0.12f, 2, 0.92f, 0.94f, 0.96f);
            AddBox(buffer, nA.deckL[0], nA.deckL[1], nA.deckL[2], 0.6f, 0.5f, 0.6f, 2, 0.25f, 0.28f, 0.32f);
            AddBox(buffer, nA.deckR[0], nA.deckR[1], nA.deckR[2], 0.6f, 0.5f, 0.6f, 2, 0.25f, 0.28f, 0.32f);
        }

        if (nA.y > nA.deckL[1] + 4.0f && i % 3 == 0) {
            AddBeam(buffer, nA.left[0], nA.left[1], nA.left[2], nA.right[0], nA.right[1], nA.right[2], 0.55f, 0.55f, 2, 0.74f, 0.78f, 0.82f);
        }
    }

    // Grounded intermediate arch piers inset from bridge termini
    AddIntermediateSupportPier(buffer, 0.15f, params, false);
    AddIntermediateSupportPier(buffer, 0.85f, params, false);
}

void BridgeGenerator::BuildSuspensionTowersAndCatenary(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    float towerH = params.pylonHeightM * 1.15f;
    float halfW = params.deckWidthM * 0.52f;
    float depth = params.deckThicknessM;
    float footH = 3.5f;
    float towerPositions[2] = { 0.22f, 0.78f };

    for (int tIdx = 0; tIdx < 2; ++tIdx) {
        float pt = towerPositions[tIdx];
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        // 1. Massive Ground Foundation Plinth (Y = 0 to 3.5m)
        AddOrientedBox(buffer, px, footH * 0.5f, pz, params.deckWidthM * 1.55f, footH, 14.0f, nx, nz, tx, tz, 1, 0.48f, 0.52f, 0.56f);

        // 2. Transverse Lower Crosshead Beam beneath deck
        float seatY = py - depth - 0.45f;
        AddOrientedBox(buffer, px, seatY - 1.2f, pz, params.deckWidthM * 1.18f, 2.4f, 4.6f, nx, nz, tx, tz, 1, 0.62f, 0.66f, 0.70f);

        // 3. Pot Bearings seating the deck
        for (int side : {-1, 1}) {
            AddOrientedBox(buffer, px + nx * (halfW * 0.55f * side), seatY + 0.22f, pz + nz * (halfW * 0.55f * side), 2.2f, 0.44f, 2.2f, nx, nz, tx, tz, 2, 0.18f, 0.20f, 0.24f);
        }

        // 4. Continuous Cellular Steel Tower Legs (from footing footH up to py + towerH)
        float totalLegH = (py + towerH) - footH;
        float legMidY = footH + totalLegH * 0.5f;
        for (int side = -1; side <= 1; side += 2) {
            float towerX = px + nx * halfW * side;
            float towerZ = pz + nz * halfW * side;
            AddOrientedBox(buffer, towerX, legMidY, towerZ, 3.8f, totalLegH, 4.4f, nx, nz, tx, tz, 2, 0.78f, 0.82f, 0.86f);
            // Saddle casting atop tower leg
            AddOrientedBox(buffer, towerX, py + towerH + 1.2f, towerZ, 4.2f, 2.4f, 4.8f, nx, nz, tx, tz, 2, 0.55f, 0.60f, 0.65f);
        }

        // 5. Portal Sway Cross-Struts
        for (float hFrac : {0.35f, 0.70f, 0.98f}) {
            AddOrientedBox(buffer, px, py + towerH * hFrac, pz, params.deckWidthM + 3.0f, 2.2f, 3.2f, nx, nz, tx, tz, 2, 0.68f, 0.72f, 0.76f);
        }
    }

    // 6. Smooth Continuous Main Suspension Catenary Cables (anchored to deck edge at t=0,1 without end blocks)
    const int catSegs = 40;
    for (int side = -1; side <= 1; side += 2) {
        float prevX = 0, prevY = 0, prevZ = 0;
        bool hasPrev = false;

        for (int i = 0; i <= catSegs; ++i) {
            float t = static_cast<float>(i) / catSegs;
            float x, y, z, tx, ty, tz, bank;
            SampleCenterline(t, params, x, y, z, tx, ty, tz, bank);
            float nx = tz, nz = -tx;

            float sagCurve;
            if (t <= 0.22f) {
                float frac = t / 0.22f;
                sagCurve = 0.03f + 0.97f * (frac * frac);
            } else if (t >= 0.78f) {
                float frac = (1.0f - t) / 0.22f;
                sagCurve = 0.03f + 0.97f * (frac * frac);
            } else {
                float midFrac = (t - 0.5f) / 0.28f;
                sagCurve = 0.12f + 0.88f * (midFrac * midFrac);
            }

            float catX = x + nx * halfW * side;
            float catY = y + towerH * sagCurve;
            float catZ = z + nz * halfW * side;

            if (hasPrev) {
                AddBeam(buffer, prevX, prevY, prevZ, catX, catY, catZ, 0.42f, 0.42f, 2, 0.94f, 0.96f, 0.98f);
            }
            prevX = catX; prevY = catY; prevZ = catZ;
            hasPrev = true;

            if (i > 1 && i < catSegs - 1 && i % 2 == 0) {
                AddBeam(buffer, catX, catY, catZ, catX, y + 0.6f, catZ, 0.10f, 0.10f, 2, 0.88f, 0.90f, 0.94f);
                AddOrientedBox(buffer, catX, y + 0.3f, catZ, 0.6f, 0.6f, 0.6f, nx, nz, tx, tz, 2, 0.25f, 0.28f, 0.32f);
            }
        }
    }
}

void BridgeGenerator::BuildPedestrianFootbridgeStructure(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    // Signature Pedestrian Basket-Handle Tubular Spine Arch:
    // Twin inclined steel arch tubes spring wide outside deck curbs and converge gracefully
    // inward toward the crown above the walkway, suspended by fanned stainless tie-rods,
    // translucent parapet panels, and illuminated promenade bollards.
    const int archDivs = 28;
    float archApexH = std::clamp(params.pylonHeightM * 0.55f, 9.5f, 22.0f);
    float pedHalfW = std::clamp(params.deckWidthM * 0.5f, 1.6f, 4.2f);
    float springHalfW = pedHalfW + 1.05f;
    float crownHalfW = 0.72f;

    struct ArchPt {
        float lX, lY, lZ;
        float rX, rY, rZ;
        float deckLX, deckLZ;
        float deckRX, deckRZ;
        float y;
        float nx, nz, tx, tz;
    };
    std::vector<ArchPt> pts(archDivs + 1);

    for (int i = 0; i <= archDivs; ++i) {
        float t = static_cast<float>(i) / archDivs;
        float x, y, z, tx, ty, tz, bank;
        SampleCenterline(t, params, x, y, z, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float sinArc = std::sin(t * PI);
        float curY = y + 4.0f * archApexH * t * (1.0f - t);
        // Basket-handle inward lean toward the crown
        float ribOffset = springHalfW - (springHalfW - crownHalfW) * sinArc;

        pts[i].lX = x - nx * ribOffset;
        pts[i].lY = curY;
        pts[i].lZ = z - nz * ribOffset;

        pts[i].rX = x + nx * ribOffset;
        pts[i].rY = curY;
        pts[i].rZ = z + nz * ribOffset;

        pts[i].deckLX = x - nx * pedHalfW;
        pts[i].deckLZ = z - nz * pedHalfW;
        pts[i].deckRX = x + nx * pedHalfW;
        pts[i].deckRZ = z + nz * pedHalfW;
        pts[i].y = y;
        pts[i].nx = nx; pts[i].nz = nz;
        pts[i].tx = tx; pts[i].tz = tz;
    }

    // 1. Inclined White/Cyan Tubular Steel Basket-Handle Arch Ribs + Vierendeel Crown Ties
    for (int i = 0; i < archDivs; ++i) {
        const auto& pA = pts[i];
        const auto& pB = pts[i + 1];

        AddBeam(buffer, pA.lX, pA.lY, pA.lZ, pB.lX, pB.lY, pB.lZ, 0.52f, 0.56f, 2, 0.24f, 0.74f, 0.88f);
        AddBeam(buffer, pA.rX, pA.rY, pA.rZ, pB.rX, pB.rY, pB.rZ, 0.52f, 0.56f, 2, 0.24f, 0.74f, 0.88f);

        if (pA.lY > pA.y + 3.0f && i % 2 == 0) {
            AddBeam(buffer, pA.lX, pA.lY, pA.lZ, pA.rX, pA.rY, pA.rZ, 0.26f, 0.26f, 2, 0.84f, 0.88f, 0.92f);
        }

        if (pA.lY > pA.y + 1.0f) {
            AddBeam(buffer, pA.lX, pA.lY, pA.lZ, pA.deckLX, pA.y + 0.1f, pA.deckLZ, 0.07f, 0.07f, 2, 0.90f, 0.94f, 0.97f);
            AddBeam(buffer, pA.rX, pA.rY, pA.rZ, pA.deckRX, pA.y + 0.1f, pA.deckRZ, 0.07f, 0.07f, 2, 0.90f, 0.94f, 0.97f);
            AddBox(buffer, pA.deckLX, pA.y + 0.1f, pA.deckLZ, 0.22f, 0.30f, 0.22f, 2, 0.28f, 0.32f, 0.36f);
            AddBox(buffer, pA.deckRX, pA.y + 0.1f, pA.deckRZ, 0.22f, 0.30f, 0.22f, 2, 0.28f, 0.32f, 0.36f);
        }

        // Translucent cyan architectural balustrade glass infill & illuminated promenade bollards
        if (i % 4 == 2) {
            AddOrientedBox(buffer, pA.deckLX, pA.y + 0.62f, pA.deckLZ, 0.05f, 0.78f, 1.85f, pA.nx, pA.nz, pA.tx, pA.tz, 2, 0.35f, 0.72f, 0.85f);
            AddOrientedBox(buffer, pA.deckRX, pA.y + 0.62f, pA.deckRZ, 0.05f, 0.78f, 1.85f, pA.nx, pA.nz, pA.tx, pA.tz, 2, 0.35f, 0.72f, 0.85f);
            AddBox(buffer, pA.deckLX, pA.y + 1.32f, pA.deckLZ, 0.16f, 0.12f, 0.16f, 4, 0.98f, 0.85f, 0.40f);
            AddBox(buffer, pA.deckRX, pA.y + 1.32f, pA.deckRZ, 0.16f, 0.12f, 0.16f, 4, 0.98f, 0.85f, 0.40f);
        }
    }

    AddIntermediateSupportPier(buffer, 0.16f, params, true);
    AddIntermediateSupportPier(buffer, 0.84f, params, true);
}

void BridgeGenerator::BuildExtradosedPylonsAndStays(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    float towerH = params.pylonHeightM * 0.42f;
    float towerPositions[2] = { 0.28f, 0.72f };
    float halfW = params.deckWidthM * 0.5f;

    for (int p = 0; p < 2; ++p) {
        float pt = towerPositions[p];
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float depth = params.deckThicknessM;
        float footH = 2.4f;
        float seatY = py - depth - 0.45f;

        // 1. Grounded Footing / Pile Cap at y=0 (0 to 2.4m)
        AddOrientedBox(buffer, px, footH * 0.5f, pz, params.deckWidthM * 1.30f, footH, 8.5f, nx, nz, tx, tz, 1, 0.48f, 0.52f, 0.56f);

        // 2. Twin Columns from footing to pier cap
        float capH = 1.6f;
        float capBottomY = seatY - capH;
        float colH = std::max(1.0f, capBottomY - footH);
        float colMidY = footH + colH * 0.5f;
        for (int side : {-1, 1}) {
            float colX = px + nx * (halfW * 0.46f * side);
            float colZ = pz + nz * (halfW * 0.46f * side);
            AddOrientedBox(buffer, colX, colMidY, colZ, 2.6f, colH, 3.6f, nx, nz, tx, tz, 1, 0.68f, 0.72f, 0.76f);
        }

        // 3. Pier Cap Beam beneath deck
        AddOrientedBox(buffer, px, seatY - capH * 0.5f, pz, params.deckWidthM * 1.05f, capH, 4.6f, nx, nz, tx, tz, 1, 0.62f, 0.66f, 0.70f);

        // 4. Pot Bearings seating the deck
        for (int side : {-1, 1}) {
            float bX = px + nx * (halfW * 0.50f * side);
            float bZ = pz + nz * (halfW * 0.50f * side);
            AddOrientedBox(buffer, bX, seatY + 0.22f, bZ, 2.0f, 0.44f, 2.0f, nx, nz, tx, tz, 2, 0.18f, 0.20f, 0.24f);
        }

        // 5. Extradosed Tower Mast above roadway
        AddOrientedBox(buffer, px, py + towerH * 0.5f, pz, 4.2f, towerH, 5.0f, nx, nz, tx, tz, 1, 0.80f, 0.84f, 0.88f);
        AddOrientedBox(buffer, px, py + towerH + 0.6f, pz, 4.6f, 1.2f, 5.4f, nx, nz, tx, tz, 2, 0.45f, 0.50f, 0.55f);

        for (int c = 1; c <= 6; ++c) {
            float deltaT = c * 0.028f;
            float stayAnchorY = py + towerH * (0.45f + c * 0.08f);

            for (int side : {-1, 1}) {
                float tFore = pt + deltaT;
                if (tFore <= 0.95f) {
                    float fx, fy, fz, ftx, fty, ftz, fbank;
                    SampleCenterline(tFore, params, fx, fy, fz, ftx, fty, ftz, fbank);
                    float fnx = ftz, fnz = -ftx;
                    float anchorX = fx + fnx * (halfW * 0.6f * side);
                    float anchorZ = fz + fnz * (halfW * 0.6f * side);
                    AddBeam(buffer, px + nx * (1.8f * side), stayAnchorY, pz + nz * (1.8f * side), anchorX, fy + 0.4f, anchorZ, 0.22f, 0.22f, 2, 0.92f, 0.94f, 0.96f);
                    AddOrientedBox(buffer, anchorX, fy + 0.2f, anchorZ, 0.8f, 0.5f, 0.8f, fnx, fnz, ftx, ftz, 2, 0.30f, 0.34f, 0.38f);
                }

                float tBack = pt - deltaT;
                if (tBack >= 0.05f) {
                    float bx, by, bz, btx, bty, btz, bbank;
                    SampleCenterline(tBack, params, bx, by, bz, btx, bty, btz, bbank);
                    float bnx = btz, bnz = -btx;
                    float anchorX = bx + bnx * (halfW * 0.6f * side);
                    float anchorZ = bz + bnz * (halfW * 0.6f * side);
                    AddBeam(buffer, px + nx * (1.8f * side), stayAnchorY, pz + nz * (1.8f * side), anchorX, by + 0.4f, anchorZ, 0.22f, 0.22f, 2, 0.92f, 0.94f, 0.96f);
                    AddOrientedBox(buffer, anchorX, by + 0.2f, anchorZ, 0.8f, 0.5f, 0.8f, bnx, bnz, btx, btz, 2, 0.30f, 0.34f, 0.38f);
                }
            }
        }
    }
}

void BridgeGenerator::BuildBasculeDrawbridgeMachinery(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    float halfW = params.deckWidthM * 0.5f;

    for (float pt : { 0.35f, 0.65f }) {
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        // Bascule trunnion pier continuous from Y=0 to deck py
        AddOrientedBox(buffer, px, py * 0.5f, pz, params.deckWidthM * 1.35f, py, 12.0f, nx, nz, tx, tz, 1, 0.50f, 0.53f, 0.58f);

        AddOrientedBox(buffer, px - nx * (halfW + 3.0f), py + 2.5f, pz, 3.6f, 5.0f, 7.0f, nx, nz, tx, tz, 1, 0.70f, 0.74f, 0.78f);
        AddOrientedBox(buffer, px + nx * (halfW + 3.0f), py + 2.5f, pz, 3.6f, 5.0f, 7.0f, nx, nz, tx, tz, 1, 0.70f, 0.74f, 0.78f);

        for (int side : {-1, 1}) {
            float hubX = px + nx * (halfW * 0.85f * side);
            float hubZ = pz + nz * (halfW * 0.85f * side);
            AddOrientedBox(buffer, hubX, py, hubZ, 1.6f, 1.6f, 3.0f, nx, nz, tx, tz, 2, 0.25f, 0.28f, 0.32f);
        }

        float targetT = (pt < 0.5f) ? 0.495f : 0.505f;
        float mx, my, mz, mtx, mty, mtz, mbank;
        SampleCenterline(targetT, params, mx, my, mz, mtx, mty, mtz, mbank);
        float mnx = mtz, mnz = -mtx;

        for (int side : {-1, 1}) {
            float startX = px + nx * (halfW * 0.82f * side);
            float startZ = pz + nz * (halfW * 0.82f * side);
            float endX = mx + mnx * (halfW * 0.82f * side);
            float endZ = mz + mnz * (halfW * 0.82f * side);
            AddBeam(buffer, startX, py - 0.5f, startZ, endX, my - 0.5f, endZ, 0.8f, 1.8f, 2, 0.75f, 0.78f, 0.82f);
        }
    }

    for (float pt : { 0.18f, 0.82f }) {
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;
        AddOrientedBox(buffer, px, py * 0.5f, pz, params.deckWidthM * 1.1f, py, 4.0f, nx, nz, tx, tz, 1, 0.58f, 0.62f, 0.66f);
    }
}

void BridgeGenerator::BuildCantileverTrussSuperstructure(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    float halfW = params.deckWidthM * 0.52f;
    float pierPositions[2] = { 0.30f, 0.70f };

    for (int p = 0; p < 2; ++p) {
        float pt = pierPositions[p];
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        // Cantilever truss pier continuous from ground Y=0 to deck py
        AddOrientedBox(buffer, px, py * 0.5f, pz, params.deckWidthM * 1.35f, py, 14.0f, nx, nz, tx, tz, 1, 0.48f, 0.52f, 0.56f);

        float cantileverTowerH = 20.0f;

        for (int side = -1; side <= 1; side += 2) {
            float sideX = px + nx * halfW * side;
            float sideZ = pz + nz * halfW * side;

            float apexX = sideX;
            float apexY = py + cantileverTowerH;
            float apexZ = sideZ;

            float tAnchor = (p == 0) ? (pt - 0.16f) : (pt + 0.16f);
            float ax, ay, az, atx, aty, atz, abank;
            SampleCenterline(tAnchor, params, ax, ay, az, atx, aty, atz, abank);
            float anx = atz, anz = -atx;
            float armShoreX = ax + anx * halfW * side;
            float armShoreZ = az + anz * halfW * side;

            float tCant = (p == 0) ? (pt + 0.12f) : (pt - 0.12f);
            float cx, cy, cz, ctx, cty, ctz, cbank;
            SampleCenterline(tCant, params, cx, cy, cz, ctx, cty, ctz, cbank);
            float cnx = ctz, cnz = -ctx;
            float armCentX = cx + cnx * halfW * side;
            float armCentZ = cz + cnz * halfW * side;

            AddBeam(buffer, sideX, py, sideZ, apexX, apexY, apexZ, 1.2f, 1.2f, 2, 0.78f, 0.82f, 0.86f);
            AddBeam(buffer, apexX, apexY, apexZ, armShoreX, ay, armShoreZ, 0.9f, 0.9f, 2, 0.72f, 0.76f, 0.80f);
            AddBeam(buffer, apexX, apexY, apexZ, armCentX, cy, armCentZ, 0.9f, 0.9f, 2, 0.72f, 0.76f, 0.80f);
            AddBeam(buffer, armShoreX, ay, armShoreZ, sideX, py, sideZ, 0.85f, 0.85f, 2, 0.70f, 0.74f, 0.78f);
            AddBeam(buffer, sideX, py, sideZ, armCentX, cy, armCentZ, 0.85f, 0.85f, 2, 0.70f, 0.74f, 0.78f);
        }

        AddBeam(buffer, px - nx * halfW, py + cantileverTowerH, pz - nz * halfW, px + nx * halfW, py + cantileverTowerH, pz + nz * halfW, 0.75f, 0.75f, 2, 0.70f, 0.74f, 0.78f);
    }

    for (int side = -1; side <= 1; side += 2) {
        float x1, y1, z1, tx1, ty1, tz1, b1;
        float x2, y2, z2, tx2, ty2, tz2, b2;
        SampleCenterline(0.42f, params, x1, y1, z1, tx1, ty1, tz1, b1);
        SampleCenterline(0.58f, params, x2, y2, z2, tx2, ty2, tz2, b2);
        float nx1 = tz1, nz1 = -tx1;
        float nx2 = tz2, nz2 = -tx2;

        float p1X = x1 + nx1 * halfW * side, p1Z = z1 + nz1 * halfW * side;
        float p2X = x2 + nx2 * halfW * side, p2Z = z2 + nz2 * halfW * side;
        AddBeam(buffer, p1X, y1, p1Z, p2X, y2, p2Z, 0.85f, 0.85f, 2, 0.78f, 0.82f, 0.86f);
        AddBeam(buffer, p1X, y1 + 5.0f, p1Z, p2X, y2 + 5.0f, p2Z, 0.70f, 0.70f, 2, 0.82f, 0.86f, 0.90f);
    }
}

void BridgeGenerator::BuildCurvedCableStayedPylon(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    // Iconic Santiago Calatrava Landmark: Alamillo / Sundial Style
    float halfW = params.deckWidthM * 0.52f;
    float towerH = params.pylonHeightM * 1.15f;
    float leanBack = 22.0f;

    float midT = 0.5f;
    float mx, my, mz, mtx, mty, mtz, mbank;
    SampleCenterline(midT, params, mx, my, mz, mtx, mty, mtz, mbank);
    float nx = mtz, nz = -mtx;

    float pylonX = mx - nx * (halfW + 6.0f);
    float pylonZ = mz - nz * (halfW + 6.0f);

    // 1. Massive Sculptural Riverbank Caisson Plinth grounded into terrain + Deck Cross-Girder at mid-span
    AddOrientedBox(buffer, pylonX, my * 0.45f, pylonZ, 11.5f, my * 0.90f, 16.0f, nx, nz, mtx, mtz, 1, 0.50f, 0.54f, 0.58f);
    float seatY = my - params.deckThicknessM - 0.45f;
    AddBeam(buffer, pylonX, seatY, pylonZ, mx + nx * (halfW * 0.5f), seatY, mz + nz * (halfW * 0.5f), 4.0f, 2.2f, 1, 0.62f, 0.66f, 0.70f);

    // 2. Sculptural Leaning Needle Pylon Mast (72° incline leaning gracefully backward away from curve)
    float apexX = pylonX - nx * leanBack;
    float apexY = my + towerH;
    float apexZ = pylonZ - nz * leanBack;
    AddBeam(buffer, pylonX, my, pylonZ, apexX, apexY, apexZ, 4.4f, 5.0f, 2, 0.90f, 0.92f, 0.96f);

    // Spire finial and aviation warning beacon
    AddOrientedBox(buffer, apexX, apexY + 1.8f, apexZ, 1.2f, 3.6f, 1.2f, nx, nz, mtx, mtz, 4, 1.0f, 0.2f, 0.15f);

    // 3. High-Capacity Bedrock Locked-Coil Steel Backstays (3 heavy tension stays)
    float anchorX = pylonX - nx * (leanBack + 18.0f);
    float anchorZ = pylonZ - nz * (leanBack + 18.0f);
    AddBeam(buffer, apexX, apexY - 1.0f, apexZ, anchorX, 1.5f, anchorZ - 18.0f * mtz, 0.28f, 0.28f, 2, 0.92f, 0.94f, 0.98f);
    AddBeam(buffer, apexX, apexY - 1.0f, apexZ, anchorX, 1.5f, anchorZ, 0.28f, 0.28f, 2, 0.92f, 0.94f, 0.98f);
    AddBeam(buffer, apexX, apexY - 1.0f, apexZ, anchorX, 1.5f, anchorZ + 18.0f * mtz, 0.28f, 0.28f, 2, 0.92f, 0.94f, 0.98f);

    // Subterranean Bedrock Anchor Block
    AddOrientedBox(buffer, anchorX, 1.5f, anchorZ, 12.0f, 3.0f, 44.0f, nx, nz, mtx, mtz, 1, 0.45f, 0.48f, 0.52f);

    // 4. Radiating Harp Stay Cables fanning from leaning mast to outer curve edge of deck (14 pairs)
    uint32_t pairs = (params.cablePairsCount > 0) ? params.cablePairsCount : 14;
    for (uint32_t c = 1; c <= pairs; ++c) {
        float frac = static_cast<float>(c) / (pairs + 1);
        float tStay = 0.10f + frac * 0.80f;
        float dx, dy, dz, dtx, dty, dtz, dbank;
        SampleCenterline(tStay, params, dx, dy, dz, dtx, dty, dtz, dbank);

        float lateralNx = dtz, lateralNz = -dtx;
        float deckAnchorX = dx + lateralNx * halfW;
        float deckAnchorY = dy + 0.6f;
        float deckAnchorZ = dz + lateralNz * halfW;

        float mastAnchorX = pylonX - nx * leanBack * (0.32f + frac * 0.68f);
        float mastAnchorY = my + towerH * (0.32f + frac * 0.68f);
        float mastAnchorZ = pylonZ - nz * leanBack * (0.32f + frac * 0.68f);

        AddBeam(buffer, mastAnchorX, mastAnchorY, mastAnchorZ, deckAnchorX, deckAnchorY, deckAnchorZ, 0.18f, 0.18f, 2, 0.94f, 0.96f, 0.98f);
        AddOrientedBox(buffer, deckAnchorX, deckAnchorY - 0.3f, deckAnchorZ, 1.1f, 0.8f, 1.1f, lateralNx, lateralNz, dtx, dtz, 2, 0.30f, 0.34f, 0.38f);
    }

    // 5. Curved Deck River Support Piers along the radius (Twin-column sculptural pier bents with bullnoses)
    for (float pt : {0.22f, 0.78f}) {
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float pnx = tz, pnz = -tx;

        float depth = params.deckThicknessM;
        float footH = 2.4f;
        float seatY = py - depth - 0.45f;

        // Grounded riverbed pile cap plinth (Y = 0 to 2.4m)
        AddOrientedBox(buffer, px, footH * 0.5f, pz, halfW * 1.8f, footH, 8.0f, pnx, pnz, tx, tz, 1, 0.48f, 0.52f, 0.56f);

        // Twin flared sculptural columns
        float capH = 1.6f;
        float capBottomY = seatY - capH;
        float colH = std::max(1.0f, capBottomY - footH);
        float colMidY = footH + colH * 0.5f;
        for (int side : {-1, 1}) {
            float cX = px + pnx * (halfW * 0.45f * side);
            float cZ = pz + pnz * (halfW * 0.45f * side);
            AddOrientedBox(buffer, cX, colMidY, cZ, 2.6f, colH, 3.4f, pnx, pnz, tx, tz, 1, 0.68f, 0.72f, 0.76f);
        }

        // Curved pier cap beam beneath deck
        AddOrientedBox(buffer, px, seatY - capH * 0.5f, pz, params.deckWidthM * 0.90f, capH, 4.2f, pnx, pnz, tx, tz, 1, 0.62f, 0.66f, 0.70f);

        // Spherical pot bearings
        for (int side : {-1, 1}) {
            float bX = px + pnx * (halfW * 0.50f * side);
            float bZ = pz + pnz * (halfW * 0.50f * side);
            AddOrientedBox(buffer, bX, seatY + 0.22f, bZ, 1.8f, 0.44f, 1.8f, pnx, pnz, tx, tz, 2, 0.20f, 0.22f, 0.25f);
        }
    }
}

void BridgeGenerator::BuildRomanMasonryArches(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    const int numArches = 6;
    float halfW = params.deckWidthM * 0.42f;

    for (int p = 1; p < numArches; ++p) {
        float pt = static_cast<float>(p) / numArches;
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        AddOrientedBox(buffer, px, (py - 1.5f) * 0.5f, pz, params.deckWidthM * 1.05f, py - 1.5f, 5.2f, nx, nz, tx, tz, 1, 0.58f, 0.54f, 0.48f);

        for (int side : {-1, 1}) {
            float cwX = px + nx * (halfW * 1.15f * side);
            float cwZ = pz + nz * (halfW * 1.15f * side);
            AddOrientedBox(buffer, cwX, (py - 1.5f) * 0.5f, cwZ, 2.8f, py - 1.5f, 3.2f, nx, nz, tx, tz, 1, 0.52f, 0.48f, 0.44f);
        }
    }

    for (int a = 0; a < numArches; ++a) {
        float t0 = static_cast<float>(a) / numArches;
        float t1 = static_cast<float>(a + 1) / numArches;
        const int archSegs = 10;

        for (int side : {-1, 1}) {
            float prevX = 0, prevY = 0, prevZ = 0;
            bool hasPrev = false;

            for (int s = 0; s <= archSegs; ++s) {
                float frac = static_cast<float>(s) / archSegs;
                float t = t0 + frac * (t1 - t0);
                float x, y, z, tx, ty, tz, bank;
                SampleCenterline(t, params, x, y, z, tx, ty, tz, bank);
                float nx = tz, nz = -tx;

                float archRise = std::sin(frac * PI) * (params.deckClearanceM * 0.68f);
                float curX = x + nx * (halfW * 0.95f * side);
                float curY = (y - params.deckThicknessM) - (params.deckClearanceM * 0.68f - archRise);
                float curZ = z + nz * (halfW * 0.95f * side);

                if (hasPrev) {
                    AddBeam(buffer, prevX, prevY, prevZ, curX, curY, curZ, 1.1f, 1.4f, 1, 0.62f, 0.58f, 0.52f);
                }
                prevX = curX; prevY = curY; prevZ = curZ;
                hasPrev = true;
            }
        }
    }
}

void BridgeGenerator::BuildPostTensionedOverpassPiers(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    float halfW = params.deckWidthM * 0.5f;

    for (float pt : { 0.38f, 0.62f }) {
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float depth = params.deckThicknessM;
        float footH = 2.0f;
        float seatY = py - depth - 0.45f;

        // 1. Grounded Footing / Pile Cap at y=0 (0 to 2.0m)
        AddOrientedBox(buffer, px, footH * 0.5f, pz, params.deckWidthM * 1.05f, footH, 6.0f, nx, nz, tx, tz, 1, 0.48f, 0.52f, 0.56f);

        // 2. Pier Columns from footing to pier cap
        float capH = 1.5f;
        float capBottomY = seatY - capH;
        float colH = std::max(1.0f, capBottomY - footH);
        float colMidY = footH + colH * 0.5f;
        for (int side : {-1, 1}) {
            float colX = px + nx * (halfW * 0.44f * side);
            float colZ = pz + nz * (halfW * 0.44f * side);
            AddOrientedBox(buffer, colX, colMidY, colZ, 2.4f, colH, 3.2f, nx, nz, tx, tz, 1, 0.68f, 0.72f, 0.76f);
        }

        // 3. Pier Cap Beam
        AddOrientedBox(buffer, px, seatY - capH * 0.5f, pz, params.deckWidthM * 0.90f, capH, 4.0f, nx, nz, tx, tz, 1, 0.62f, 0.66f, 0.70f);

        // 4. Pot Bearings
        for (int side : {-1, 1}) {
            float bX = px + nx * (halfW * 0.50f * side);
            float bZ = pz + nz * (halfW * 0.50f * side);
            AddOrientedBox(buffer, bX, seatY + 0.22f, bZ, 1.8f, 0.44f, 1.8f, nx, nz, tx, tz, 2, 0.20f, 0.22f, 0.25f);
        }
    }
}

void BridgeGenerator::BuildPedestrianStressRibbon(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    // True ultra-slender prestressed concrete catenary ribbon deck (single sagging deck, no flat overlap)
    const int ribbonSegs = 32;
    float maxSagM = 2.1f;
    float ribbonW = std::clamp(params.deckWidthM, 3.2f, 8.0f);
    float halfW = ribbonW * 0.5f;

    float prevX = 0.0f, prevY = 0.0f, prevZ = 0.0f;
    float prevLx = 0.0f, prevLz = 0.0f, prevRx = 0.0f, prevRz = 0.0f;
    bool hasPrev = false;

    for (int i = 0; i <= ribbonSegs; ++i) {
        float t = static_cast<float>(i) / ribbonSegs;
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(t, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float catenarySag = 4.0f * maxSagM * t * (1.0f - t);
        float rY = py - catenarySag;

        float lX = px - nx * halfW, lZ = pz - nz * halfW;
        float rX = px + nx * halfW, rZ = pz + nz * halfW;

        if (hasPrev) {
            // Sloped prestressed concrete catenary ribbon segment
            AddBeam(buffer, prevX, prevY - 0.14f, prevZ, px, rY - 0.14f, pz, ribbonW, 0.28f, 1, 0.82f, 0.84f, 0.88f);
            // Underside post-tensioned stainless steel tensioning bands
            for (float f : {-0.38f, 0.38f}) {
                AddBeam(buffer, prevX + nx * (ribbonW * f), prevY - 0.30f, prevZ + nz * (ribbonW * f),
                                px + nx * (ribbonW * f), rY - 0.30f, pz + nz * (ribbonW * f),
                                0.32f, 0.09f, 2, 0.42f, 0.46f, 0.52f);
            }
            // Continuous warm oak handrails & dual stainless safety cables
            AddBeam(buffer, prevLx, prevY + 1.15f, prevLz, lX, rY + 1.15f, lZ, 0.12f, 0.09f, 3, 0.56f, 0.41f, 0.28f);
            AddBeam(buffer, prevRx, prevY + 1.15f, prevRz, rX, rY + 1.15f, rZ, 0.12f, 0.09f, 3, 0.56f, 0.41f, 0.28f);
            AddBeam(buffer, prevLx, prevY + 0.60f, prevLz, lX, rY + 0.60f, lZ, 0.04f, 0.04f, 2, 0.84f, 0.88f, 0.92f);
            AddBeam(buffer, prevRx, prevY + 0.60f, prevRz, rX, rY + 0.60f, rZ, 0.04f, 0.04f, 2, 0.84f, 0.88f, 0.92f);
        }

        // Sloped vertical stanchions along ribbon profile
        AddBeam(buffer, lX, rY, lZ, lX, rY + 1.15f, lZ, 0.07f, 0.07f, 2, 0.82f, 0.86f, 0.90f);
        AddBeam(buffer, rX, rY, rZ, rX, rY + 1.15f, rZ, 0.07f, 0.07f, 2, 0.82f, 0.86f, 0.90f);

        prevX = px; prevY = rY; prevZ = pz;
        prevLx = lX; prevLz = lZ; prevRx = rX; prevRz = rZ;
        hasPrev = true;
    }

    // Grounded saddle piers aligned directly under the deflected catenary ribbon soffit
    AddIntermediateSupportPier(buffer, 0.12f, params, true);
    AddIntermediateSupportPier(buffer, 0.88f, params, true);
}

void BridgeGenerator::BuildPedestrianCableStayed(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    // Signature Sculptural Pedestrian Cable-Stayed Promenade:
    // Twin-fork inverted-Y tubular steel mast straddling a cantilevered scenic lookout balcony
    // with fine radial fan stay cables, timber promenade benches, and pedestrian lantern posts.
    float pylonT = 0.30f;
    float px, py, pz, tx, ty, tz, bank;
    SampleCenterline(pylonT, params, px, py, pz, tx, ty, tz, bank);
    float nx = tz, nz = -tx;

    float mastH = std::clamp(params.pylonHeightM * 0.72f, 12.0f, 28.0f);
    float halfW = std::clamp(params.deckWidthM * 0.5f, 1.6f, 4.2f);
    float forkSpread = halfW + 1.45f;
    float waistY = py + mastH * 0.42f;
    float crownY = py + mastH;

    // 1. Grounded Twin-Fork Pier Pile Cap + Inverted-Y Sculptural Pedestrian Steel Mast
    AddOrientedBox(buffer, px, 0.9f, pz, forkSpread * 2.4f, 1.8f, 4.6f, nx, nz, tx, tz, 1, 0.50f, 0.54f, 0.58f);
    AddOrientedBox(buffer, px, py - 0.55f, pz, forkSpread * 2.15f, 0.90f, 2.2f, nx, nz, tx, tz, 1, 0.62f, 0.66f, 0.70f);
    for (int side : {-1, 1}) {
        float lx = px + nx * (forkSpread * side);
        float lz = pz + nz * (forkSpread * side);
        // Lower pier legs straddling outside promenade handrails
        AddBeam(buffer, lx, 1.6f, lz, lx, waistY, lz, 0.85f, 0.95f, 2, 0.84f, 0.88f, 0.92f);
        // Converging upper Y-arms tilting back slightly along span
        AddBeam(buffer, lx, waistY, lz, px - nx * (0.25f * side) - tx * 2.2f, crownY, pz - nz * (0.25f * side) - tz * 2.2f, 0.75f, 0.85f, 2, 0.90f, 0.93f, 0.96f);
    }
    // Crown collar & warm architectural beacon spire
    float spireX = px - tx * 2.2f, spireZ = pz - tz * 2.2f;
    AddOrientedBox(buffer, spireX, crownY + 0.5f, spireZ, 1.2f, 1.0f, 1.2f, nx, nz, tx, tz, 2, 0.25f, 0.72f, 0.85f);
    AddOrientedBox(buffer, spireX, crownY + 1.7f, spireZ, 0.35f, 1.4f, 0.35f, nx, nz, tx, tz, 4, 1.0f, 0.86f, 0.38f);

    // 2. Twin Fine Harp Backstays anchored cleanly to outer deck edges behind pylon (stay clear of surface stairs)
    for (float tBack : {0.06f, 0.14f}) {
        float bx, by, bz, btx, bty, btz, bbank;
        SampleCenterline(tBack, params, bx, by, bz, btx, bty, btz, bbank);
        float bnx = btz, bnz = -btx;
        float mastY = waistY + (crownY - waistY) * (tBack < 0.10f ? 0.92f : 0.68f);
        for (int side : {-1, 1}) {
            AddBeam(buffer, spireX, mastY, spireZ, bx + bnx * (halfW * side), by + 0.2f, bz + bnz * (halfW * side), 0.10f, 0.10f, 2, 0.92f, 0.94f, 0.96f);
        }
    }

    // 3. Radiating Forward Pedestrian Fan Stay Cables
    const int stayCount = 9;
    for (int s = 1; s <= stayCount; ++s) {
        float frac = static_cast<float>(s) / (stayCount + 1);
        float t = pylonT + frac * 0.60f;
        if (t > 0.93f) break;
        float dx, dy, dz, dtx, dty, dtz, dbank;
        SampleCenterline(t, params, dx, dy, dz, dtx, dty, dtz, dbank);
        float dnx = dtz, dnz = -dtx;

        float stayAnchorY = waistY + (crownY - waistY) * (0.20f + frac * 0.78f);
        float mastAnchorX = px - tx * (2.2f * (0.20f + frac * 0.78f));
        float mastAnchorZ = pz - tz * (2.2f * (0.20f + frac * 0.78f));

        AddBeam(buffer, mastAnchorX, stayAnchorY, mastAnchorZ, dx - dnx * halfW, dy + 0.35f, dz - dnz * halfW, 0.08f, 0.08f, 2, 0.92f, 0.94f, 0.96f);
        AddBeam(buffer, mastAnchorX, stayAnchorY, mastAnchorZ, dx + dnx * halfW, dy + 0.35f, dz + dnz * halfW, 0.08f, 0.08f, 2, 0.92f, 0.94f, 0.96f);
    }

    // 4. Cantilevered Scenic Observation Balcony & Timber Bench at mid-span (+nx outlook)
    {
        float ox, oy, oz, otx, oty, otz, obank;
        SampleCenterline(0.56f, params, ox, oy, oz, otx, oty, otz, obank);
        float onx = otz, onz = -otx;
        float balX = ox + onx * (halfW + 1.4f), balZ = oz + onz * (halfW + 1.4f);
        AddOrientedBox(buffer, balX, oy - 0.05f, balZ, 2.8f, 0.22f, 7.2f, onx, onz, otx, otz, 3, 0.58f, 0.42f, 0.28f);
        AddOrientedBox(buffer, balX, oy - 0.26f, balZ, 2.6f, 0.22f, 6.8f, onx, onz, otx, otz, 2, 0.70f, 0.74f, 0.78f);
        // Perimeter observation rail + timber bench
        float edgeX = balX + onx * 1.35f, edgeZ = balZ + onz * 1.35f;
        AddOrientedBox(buffer, edgeX, oy + 0.58f, edgeZ, 0.08f, 1.15f, 7.1f, onx, onz, otx, otz, 2, 0.84f, 0.88f, 0.92f);
        AddOrientedBox(buffer, balX + onx * 0.45f, oy + 0.42f, balZ + onz * 0.45f, 0.55f, 0.24f, 4.2f, onx, onz, otx, otz, 3, 0.46f, 0.33f, 0.22f);
    }

    // 5. Promenade Pedestrian Lantern Posts every ~15% of span + Far Span Pier
    for (float tl : {0.18f, 0.44f, 0.72f, 0.86f}) {
        float lx, ly, lz, ltx, lty, ltz, lbank;
        SampleCenterline(tl, params, lx, ly, lz, ltx, lty, ltz, lbank);
        float lnx = ltz, lnz = -ltx;
        float pxL = lx - lnx * (halfW - 0.1f), pzL = lz - lnz * (halfW - 0.1f);
        AddBeam(buffer, pxL, ly, pzL, pxL, ly + 3.2f, pzL, 0.10f, 0.10f, 2, 0.26f, 0.30f, 0.36f);
        AddBox(buffer, pxL, ly + 3.35f, pzL, 0.32f, 0.28f, 0.32f, 4, 0.98f, 0.86f, 0.40f);
    }

    AddIntermediateSupportPier(buffer, 0.84f, params, true);
}

void BridgeGenerator::BuildPedestrianTimberArch(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    const int archDivs = 20;
    float archApexH = 12.0f;
    float walkHalfW = std::clamp(params.deckWidthM * 0.5f, 1.6f, 4.2f);
    float archSpacing = walkHalfW * 2.0f + 1.2f;

    for (int side : {-1, 1}) {
        float ribXOffset = side * (archSpacing * 0.5f);
        float prevX = 0, prevY = 0, prevZ = 0;
        bool hasPrev = false;

        for (int i = 0; i <= archDivs; ++i) {
            float t = static_cast<float>(i) / archDivs;
            float px, py, pz, tx, ty, tz, bank;
            SampleCenterline(t, params, px, py, pz, tx, ty, tz, bank);
            float nx = tz, nz = -tx;

            float archY = py + 4.0f * archApexH * t * (1.0f - t);
            float curX = px + nx * ribXOffset;
            float curZ = pz + nz * ribXOffset;

            if (hasPrev) {
                AddBeam(buffer, prevX, prevY, prevZ, curX, archY, curZ, 0.75f, 1.4f, 3 /* Timber */, 0.55f, 0.40f, 0.28f);
            }
            prevX = curX; prevY = archY; prevZ = curZ;
            hasPrev = true;

            if (archY > py + 2.0f && i % 2 == 0) {
                AddBeam(buffer, curX, archY - 0.7f, curZ, curX, py + 0.4f, curZ, 0.10f, 0.10f, 2, 0.88f, 0.90f, 0.94f);
            }
        }
    }

    for (int i = 4; i < archDivs - 4; i += 3) {
        float t = static_cast<float>(i) / archDivs;
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(t, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;
        float archY = py + 4.0f * archApexH * t * (1.0f - t);

        if (archY > py + 3.5f) {
            float xL = px - nx * (archSpacing * 0.5f);
            float zL = pz - nz * (archSpacing * 0.5f);
            float xR = px + nx * (archSpacing * 0.5f);
            float zR = pz + nz * (archSpacing * 0.5f);
            AddBeam(buffer, xL, archY, zL, xR, archY, zR, 0.55f, 0.55f, 3, 0.48f, 0.35f, 0.24f);
        }
    }

    // Intermediate timber-arch support piers inset from bridge termini
    AddIntermediateSupportPier(buffer, 0.16f, params, true);
    AddIntermediateSupportPier(buffer, 0.84f, params, true);
}

void BridgeGenerator::BuildSteelPlateGirders(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    const int numGirders = 4;
    float girderSpacing = params.deckWidthM / (numGirders + 1);
    float halfW = params.deckWidthM * 0.5f;

    const int pierCount = 3;
    for (int p = 1; p <= pierCount; ++p) {
        float pt = static_cast<float>(p) / (pierCount + 1);
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(pt, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        float depth = params.deckThicknessM;
        float footH = 2.2f;
        float seatY = py - depth - 0.45f;

        // 1. Grounded Footing / Pile Cap at y=0 (0 to 2.2m)
        AddOrientedBox(buffer, px, footH * 0.5f, pz, params.deckWidthM * 1.05f, footH, 6.2f, nx, nz, tx, tz, 1, 0.48f, 0.52f, 0.56f);

        // 2. Twin Columns from footing to pier cap
        float capH = 1.5f;
        float capBottomY = seatY - capH;
        float colH = std::max(1.0f, capBottomY - footH);
        float colMidY = footH + colH * 0.5f;
        for (int side : {-1, 1}) {
            float colX = px + nx * (halfW * 0.48f * side);
            float colZ = pz + nz * (halfW * 0.48f * side);
            AddOrientedBox(buffer, colX, colMidY, colZ, 2.4f, colH, 2.8f, nx, nz, tx, tz, 1, 0.65f, 0.68f, 0.72f);
        }

        // 3. Pier Cap Beam
        AddOrientedBox(buffer, px, seatY - capH * 0.5f, pz, params.deckWidthM * 0.94f, capH, 3.8f, nx, nz, tx, tz, 1, 0.58f, 0.62f, 0.66f);

        // 4. Pot Bearings seating each plate girder
        for (int g = 1; g <= numGirders; ++g) {
            float gOff = -halfW + g * girderSpacing;
            AddOrientedBox(buffer, px + nx * gOff, seatY + 0.18f, pz + nz * gOff, 1.1f, 0.36f, 1.1f, nx, nz, tx, tz, 2, 0.20f, 0.22f, 0.25f);
        }
    }

    const int segs = 30;
    for (int g = 1; g <= numGirders; ++g) {
        float gOff = -halfW + g * girderSpacing;
        for (int s = 0; s < segs; ++s) {
            float t = (s + 0.5f) / segs;
            float px, py, pz, tx, ty, tz, bank;
            SampleCenterline(t, params, px, py, pz, tx, ty, tz, bank);
            float nx = tz, nz = -tx;
            float segL = params.spanLengthM / segs;

            float gx = px + nx * gOff;
            float gz = pz + nz * gOff;

            AddOrientedBox(buffer, gx, py - 0.12f, gz, 0.95f, 0.12f, segL * 1.02f, nx, nz, tx, tz, 2, 0.52f, 0.40f, 0.34f);
            AddOrientedBox(buffer, gx, py - 0.90f, gz, 0.14f, 1.44f, segL * 1.02f, nx, nz, tx, tz, 2, 0.45f, 0.32f, 0.26f);
            AddOrientedBox(buffer, gx, py - 1.68f, gz, 1.10f, 0.14f, segL * 1.02f, nx, nz, tx, tz, 2, 0.40f, 0.28f, 0.22f);

            if (s % 4 == 0 && g < numGirders) {
                float nextGOff = -halfW + (g + 1) * girderSpacing;
                float midX = px + nx * (gOff + nextGOff) * 0.5f;
                float midZ = pz + nz * (gOff + nextGOff) * 0.5f;
                AddOrientedBox(buffer, midX, py - 0.90f, midZ, girderSpacing - 0.95f, 1.2f, 0.22f, nx, nz, tx, tz, 2, 0.38f, 0.26f, 0.22f);
            }
        }
    }
}

void BridgeGenerator::BuildPedestrianCoveredSkywalk(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    // Sleek urban covered glazed skywalk: helical steel arch rings, longitudinal purlins,
    // skylight glazing panes, amber deck handrail strips, and grounded intermediate piers (open ends).
    const int ringCount = 24;
    const float skyHalfW = std::clamp(params.deckWidthM * 0.5f, 1.6f, 4.2f) + 0.55f;
    const float archApexH = 4.6f;

    float prevTopX = 0.0f, prevTopY = 0.0f, prevTopZ = 0.0f;
    float prevEaveLX = 0.0f, prevEaveLY = 0.0f, prevEaveLZ = 0.0f;
    float prevEaveRX = 0.0f, prevEaveRY = 0.0f, prevEaveRZ = 0.0f;
    bool hasPrevSpine = false;

    for (int r = 0; r <= ringCount; ++r) {
        float t = static_cast<float>(r) / ringCount;
        float px, py, pz, tx, ty, tz, bank;
        SampleCenterline(t, params, px, py, pz, tx, ty, tz, bank);
        float nx = tz, nz = -tx;

        // Taper profile gracefully into sleek canopy portal
        float archSteps = 8;
        float prevArcX = 0.0f, prevArcY = 0.0f, prevArcZ = 0.0f;
        for (int a = 0; a <= archSteps; ++a) {
            float theta = (static_cast<float>(a) / archSteps) * PI;
            float lat = -std::cos(theta) * skyHalfW;
            float hgt = std::sin(theta) * archApexH;
            float cx = px + nx * lat;
            float cy = py + 0.35f + hgt;
            float cz = pz + nz * lat;
            if (a > 0) {
                AddBeam(buffer, prevArcX, prevArcY, prevArcZ, cx, cy, cz, 0.16f, 0.22f, 2, 0.82f, 0.86f, 0.90f);
            }
            prevArcX = cx; prevArcY = cy; prevArcZ = cz;
        }

        // Glazed semi-translucent canopy fascia ribs (cyan-tinted architectural glass)
        float crownX = px, crownY = py + 0.35f + archApexH, crownZ = pz;
        float eaveY = py + 0.35f + archApexH * 0.58f;
        float eaveLX = px - nx * (skyHalfW * 0.86f);
        float eaveLZ = pz - nz * (skyHalfW * 0.86f);
        float eaveRX = px + nx * (skyHalfW * 0.86f);
        float eaveRZ = pz + nz * (skyHalfW * 0.86f);

        if (hasPrevSpine) {
            // Ridge spine beam
            AddBeam(buffer, prevTopX, prevTopY, prevTopZ, crownX, crownY, crownZ, 0.24f, 0.26f, 2, 0.22f, 0.28f, 0.35f);
            // Left and Right skylight purlins
            AddBeam(buffer, prevEaveLX, prevEaveLY, prevEaveLZ, eaveLX, eaveY, eaveLZ, 0.16f, 0.18f, 2, 0.32f, 0.72f, 0.86f);
            AddBeam(buffer, prevEaveRX, prevEaveRY, prevEaveRZ, eaveRX, eaveY, eaveRZ, 0.16f, 0.18f, 2, 0.32f, 0.72f, 0.86f);

            // Warm LED cove illumination strips under eave purlins
            AddBeam(buffer, prevEaveLX, prevEaveLY - 0.15f, prevEaveLZ, eaveLX, eaveY - 0.15f, eaveLZ, 0.08f, 0.08f, 4, 0.98f, 0.82f, 0.38f);
            AddBeam(buffer, prevEaveRX, prevEaveRY - 0.15f, prevEaveRZ, eaveRX, eaveY - 0.15f, eaveRZ, 0.08f, 0.08f, 4, 0.98f, 0.82f, 0.38f);
        }

        prevTopX = crownX; prevTopY = crownY; prevTopZ = crownZ;
        prevEaveLX = eaveLX; prevEaveLY = eaveY; prevEaveLZ = eaveLZ;
        prevEaveRX = eaveRX; prevEaveRY = eaveY; prevEaveRZ = eaveRZ;
        hasPrevSpine = true;
    }

    // Two intermediate sculptural cylindrical piers inset cleanly from open ends
    AddIntermediateSupportPier(buffer, 0.20f, params, true);
    AddIntermediateSupportPier(buffer, 0.80f, params, true);
}

void BridgeGenerator::BuildPedestrianSurfaceConnections(
    BridgeMeshBuffer& buffer,
    const BridgeParams& params
) {
    const bool isRibbon = (params.type == BridgeType::PedestrianStressRibbon);
    const bool isTimber = (params.type == BridgeType::PedestrianTimberArch);

    const uint32_t walkMat = isRibbon ? 1u : 3u;
    const float walkR = isRibbon ? 0.78f : (isTimber ? 0.56f : 0.52f);
    const float walkG = isRibbon ? 0.80f : (isTimber ? 0.41f : 0.38f);
    const float walkB = isRibbon ? 0.84f : (isTimber ? 0.28f : 0.26f);

    const uint32_t frameMat = isTimber ? 3u : 2u;
    const float frameR = isTimber ? 0.44f : 0.72f;
    const float frameG = isTimber ? 0.32f : 0.76f;
    const float frameB = isTimber ? 0.22f : 0.80f;

    const float pedHalfW = std::clamp(params.deckWidthM * 0.5f, 1.6f, 4.2f);
    const float stairHalfW = 1.65f;
    const float railH = 1.12f;

    for (float tEnd : { 0.0f, 1.0f }) {
        float ex, ey, ez, etx, ety, etz, ebank;
        SampleCenterline(tEnd, params, ex, ey, ez, etx, ety, etz, ebank);

        float hLen = std::sqrt(etx * etx + etz * etz);
        float htx = (hLen > 1e-5f) ? (etx / hLen) : 0.0f;
        float htz = (hLen > 1e-5f) ? (etz / hLen) : 1.0f;

        // Consistent right-handed horizontal frame (nx,nz) and (htx,htz) for oriented primitives
        float nx = htz;
        float nz = -htx;

        // Outward direction extending away from the bridge span
        float signOut = (tEnd < 0.5f) ? -1.0f : 1.0f;
        float outX = htx * signOut;
        float outZ = htz * signOut;

        auto worldX = [&](float distOut, float latOff) {
            return ex + outX * distOut + nx * latOff;
        };
        auto worldZ = [&](float distOut, float latOff) {
            return ez + outZ * distOut + nz * latOff;
        };
        auto addOrientedPart = [&](float distOut, float latOff, float cy,
                                   float dimLat, float dimV, float dimLong,
                                   uint32_t matId, float r, float g, float b) {
            AddOrientedBox(
                buffer,
                worldX(distOut, latOff), cy, worldZ(distOut, latOff),
                dimLat, dimV, dimLong,
                nx, nz, htx, htz,
                matId, r, g, b
            );
        };
        auto addColumnWithFoot = [&](float distOut, float latOff, float topY, float rad) {
            float cx = worldX(distOut, latOff);
            float cz = worldZ(distOut, latOff);
            float footH = 0.55f;
            if (topY <= footH + 0.15f) return;
            addOrientedPart(distOut, latOff, footH * 0.5f, rad * 3.8f, footH, rad * 3.8f, 1, 0.48f, 0.51f, 0.56f);
            Primitives::AddCylinder<BridgeMeshBuffer, BridgeVertex>(
                buffer,
                cx, footH, cz,
                cx, topY, cz,
                rad * 1.15f, rad, 10,
                2, 0.68f, 0.72f, 0.76f, 1.0f, 0.40f, 0.55f, 0.0f
            );
        };

        // 1. Upper Portal Landing Platform flush with main deck end
        float upperLandLen = 2.6f;
        float upperLandMid = upperLandLen * 0.5f;
        addOrientedPart(upperLandMid, 0.0f, ey - 0.12f, pedHalfW * 2.0f, 0.24f, upperLandLen, walkMat, walkR, walkG, walkB);
        addOrientedPart(upperLandMid, 0.0f, ey - 0.36f, pedHalfW * 1.92f, 0.24f, upperLandLen * 0.92f, frameMat, frameR, frameG, frameB);

        // Twin grounded pier columns supporting upper landing near bridge approach edge
        addColumnWithFoot(2.15f, -pedHalfW * 0.70f, ey - 0.36f, 0.36f);
        addColumnWithFoot(2.15f,  pedHalfW * 0.70f, ey - 0.36f, 0.36f);
        addOrientedPart(2.15f, 0.0f, ey - 0.44f, pedHalfW * 1.75f, 0.32f, 0.95f, 1, 0.60f, 0.64f, 0.68f);

        // Side guardrails along -nx outer edge of Upper Landing (and closing shoulder between stair edge and deck edge)
        {
            float rX0 = worldX(0.0f, -pedHalfW), rZ0 = worldZ(0.0f, -pedHalfW);
            float rX1 = worldX(upperLandLen, -pedHalfW), rZ1 = worldZ(upperLandLen, -pedHalfW);
            AddBeam(buffer, rX0, ey + railH, rZ0, rX1, ey + railH, rZ1, 0.09f, 0.09f, 2, 0.85f, 0.88f, 0.92f);
            AddBeam(buffer, rX0, ey + railH * 0.52f, rZ0, rX1, ey + railH * 0.52f, rZ1, 0.05f, 0.05f, 2, 0.78f, 0.82f, 0.86f);
            AddBeam(buffer, rX1, ey, rZ1, rX1, ey + railH, rZ1, 0.09f, 0.09f, 2, 0.80f, 0.84f, 0.88f);
            // Short shoulder rail returning to stair opening on -nx side
            float sX1 = worldX(upperLandLen, -stairHalfW), sZ1 = worldZ(upperLandLen, -stairHalfW);
            AddBeam(buffer, rX1, ey + railH, rZ1, sX1, ey + railH, sZ1, 0.09f, 0.09f, 2, 0.85f, 0.88f, 0.92f);
            AddBeam(buffer, sX1, ey, sZ1, sX1, ey + railH, sZ1, 0.09f, 0.09f, 2, 0.80f, 0.84f, 0.88f);
        }

        // 2. Central Architectural Two-Flight Staircase descending to Ground Entry Plaza (Y = 0)
        float midY = ey * 0.50f;
        float flightRun = std::clamp(ey * 0.82f, 4.8f, 9.2f);
        float d1Start = upperLandLen;
        float d1End = d1Start + flightRun;
        float midLandLen = 2.4f;
        float d2Start = d1End + midLandLen;
        float d2End = d2Start + flightRun;
        float groundTopY = 0.14f;

        auto buildStairFlight = [&](float dA, float yA, float dB, float yB) {
            // Twin side steel box stringers + center spine beam
            for (int side : {-1, 1}) {
                float sx0 = worldX(dA, stairHalfW * side), sz0 = worldZ(dA, stairHalfW * side);
                float sx1 = worldX(dB, stairHalfW * side), sz1 = worldZ(dB, stairHalfW * side);
                AddBeam(buffer, sx0, yA - 0.18f, sz0, sx1, yB - 0.18f, sz1, 0.22f, 0.34f, frameMat, frameR, frameG, frameB);
                // Continuous sloped handrail + mid-rail
                AddBeam(buffer, sx0, yA + railH, sz0, sx1, yB + railH, sz1, 0.08f, 0.08f, 2, 0.86f, 0.90f, 0.94f);
                AddBeam(buffer, sx0, yA + railH * 0.54f, sz0, sx1, yB + railH * 0.54f, sz1, 0.05f, 0.05f, 2, 0.78f, 0.82f, 0.86f);
            }
            AddBeam(
                buffer,
                worldX(dA, 0.0f), yA - 0.24f, worldZ(dA, 0.0f),
                worldX(dB, 0.0f), yB - 0.24f, worldZ(dB, 0.0f),
                0.36f, 0.36f, frameMat, 0.45f, 0.48f, 0.52f
            );

            // Individual 3D treads & balusters
            const int steps = 12;
            float stepRun = (dB - dA) / steps;
            float stepDrop = (yA - yB) / steps;
            for (int s = 0; s < steps; ++s) {
                float dStep = dA + (s + 0.5f) * stepRun;
                float yStepTop = yA - (s + 1) * stepDrop;
                addOrientedPart(dStep, 0.0f, yStepTop - stepDrop * 0.42f, stairHalfW * 1.92f, std::max(0.14f, stepDrop * 0.84f), stepRun * 1.08f, walkMat, walkR, walkG, walkB);
                if (s % 3 == 0) {
                    for (int side : {-1, 1}) {
                        float bx = worldX(dStep, stairHalfW * side), bz = worldZ(dStep, stairHalfW * side);
                        AddBeam(buffer, bx, yStepTop, bz, bx, yStepTop + railH + 0.05f, bz, 0.07f, 0.07f, 2, 0.80f, 0.84f, 0.88f);
                    }
                }
            }
        };

        // Upper Stair Flight (Upper Landing -> Mid-Landing)
        buildStairFlight(d1Start, ey, d1End, midY);

        // Mid-Flight Landing Platform + Support Columns
        float dMidCenter = 0.5f * (d1End + d2Start);
        addOrientedPart(dMidCenter, 0.0f, midY - 0.12f, stairHalfW * 2.15f, 0.24f, midLandLen, walkMat, walkR, walkG, walkB);
        addOrientedPart(dMidCenter, 0.0f, midY - 0.32f, stairHalfW * 2.08f, 0.20f, midLandLen * 0.92f, frameMat, frameR, frameG, frameB);
        addColumnWithFoot(dMidCenter, -stairHalfW * 0.80f, midY - 0.32f, 0.28f);
        addColumnWithFoot(dMidCenter,  stairHalfW * 0.80f, midY - 0.32f, 0.28f);
        for (int side : {-1, 1}) {
            float lEdge = stairHalfW * 1.05f * side;
            float rx0 = worldX(d1End, lEdge), rz0 = worldZ(d1End, lEdge);
            float rx1 = worldX(d2Start, lEdge), rz1 = worldZ(d2Start, lEdge);
            AddBeam(buffer, rx0, midY + railH, rz0, rx1, midY + railH, rz1, 0.08f, 0.08f, 2, 0.86f, 0.90f, 0.94f);
            AddBeam(buffer, rx0, midY + railH * 0.54f, rz0, rx1, midY + railH * 0.54f, rz1, 0.05f, 0.05f, 2, 0.78f, 0.82f, 0.86f);
        }

        // Lower Stair Flight (Mid-Landing -> Ground Entry Plaza at Y=0.14m)
        buildStairFlight(d2Start, midY, d2End, groundTopY);

        // Ground-Level Concrete Entry Plaza & Tactile Step Apron at Y=0
        float plazaLen = 4.2f;
        float plazaCenterD = d2End + plazaLen * 0.5f;
        addOrientedPart(plazaCenterD, 0.0f, 0.08f, stairHalfW * 3.4f, 0.16f, plazaLen, 1, 0.56f, 0.59f, 0.63f);
        // Yellow tactile warning strip at bottom step
        addOrientedPart(d2End + 0.32f, 0.0f, 0.165f, stairHalfW * 2.05f, 0.03f, 0.45f, 3, 0.94f, 0.78f, 0.18f);
        // Flanked illuminated pedestrian entry bollards on surface
        for (int side : {-1, 1}) {
            float bLat = stairHalfW * 1.25f * side;
            addOrientedPart(d2End + 1.4f, bLat, 0.52f, 0.22f, 0.88f, 0.22f, 2, 0.24f, 0.27f, 0.32f);
            addOrientedPart(d2End + 1.4f, bLat, 0.99f, 0.24f, 0.10f, 0.24f, 4, 0.98f, 0.82f, 0.34f);
        }

        // 3. Integrated ADA Switchback Access Ramp on +nx side -> Ground Surface (Y=0)
        // Kept strictly on +nx side so it never interferes with asymmetrical -nx cable-stayed anchorages
        float rampW = 2.2f;
        float halfRW = rampW * 0.5f;
        float rampGap = 0.55f;
        float latInner = pedHalfW + halfRW;              // flush against Upper Landing +nx edge (2.7 + 1.1 = 3.8m)
        float latOuter = latInner + rampW + rampGap;     // outer return lane (3.8 + 2.2 + 0.55 = 6.55m)

        // Flush Upper Ramp Entry Threshold Deck adjoining Upper Landing (+nx side)
        addOrientedPart(upperLandMid, latInner, ey - 0.12f, rampW, 0.24f, upperLandLen, walkMat, walkR, walkG, walkB);
        addOrientedPart(upperLandMid, latInner, ey - 0.34f, rampW * 0.94f, 0.20f, upperLandLen * 0.92f, frameMat, frameR, frameG, frameB);
        addColumnWithFoot(upperLandMid, latInner + halfRW * 0.78f, ey - 0.34f, 0.26f);
        // Back guardrail on threshold bridge (d=0.1) & outer rail
        {
            float txA = worldX(0.1f, pedHalfW), tzA = worldZ(0.1f, pedHalfW);
            float txB = worldX(0.1f, latInner + halfRW), tzB = worldZ(0.1f, latInner + halfRW);
            float txC = worldX(upperLandLen, latInner + halfRW), tzC = worldZ(upperLandLen, latInner + halfRW);
            AddBeam(buffer, txA, ey + railH, tzA, txB, ey + railH, tzB, 0.08f, 0.08f, 2, 0.86f, 0.90f, 0.94f);
            AddBeam(buffer, txB, ey + railH, tzB, txC, ey + railH, tzC, 0.08f, 0.08f, 2, 0.86f, 0.90f, 0.94f);
            AddBeam(buffer, txB, ey, tzB, txB, ey + railH, tzB, 0.08f, 0.08f, 2, 0.80f, 0.84f, 0.88f);
        }

        float rampRun = std::clamp(ey * 1.55f, 10.5f, 16.5f);
        float dTurnStart = upperLandLen + rampRun;
        float turnLen = 2.6f;
        float dTurnEnd = dTurnStart + turnLen;

        auto buildRampRun = [&](float dA, float yA, float dB, float yB, float latCenter) {
            const int rSegs = 8;
            for (int r = 0; r < rSegs; ++r) {
                float u0 = static_cast<float>(r) / rSegs;
                float u1 = static_cast<float>(r + 1) / rSegs;
                float da = dA + u0 * (dB - dA), db = dA + u1 * (dB - dA);
                float ya = yA + u0 * (yB - yA), yb = yA + u1 * (yB - yA);
                // Continuous sloped walking slab
                AddBeam(
                    buffer,
                    worldX(da, latCenter), ya - 0.11f, worldZ(da, latCenter),
                    worldX(db, latCenter), yb - 0.11f, worldZ(db, latCenter),
                    rampW, 0.22f, walkMat, walkR, walkG, walkB
                );
                // Side edge steel stringers + dual handrails
                for (int side : {-1, 1}) {
                    float lPos = latCenter + halfRW * side;
                    float ax = worldX(da, lPos), az = worldZ(da, lPos);
                    float bx = worldX(db, lPos), bz = worldZ(db, lPos);
                    AddBeam(buffer, ax, ya - 0.26f, az, bx, yb - 0.26f, bz, 0.14f, 0.22f, frameMat, frameR, frameG, frameB);
                    AddBeam(buffer, ax, ya + railH, az, bx, yb + railH, bz, 0.07f, 0.07f, 2, 0.86f, 0.90f, 0.94f);
                    AddBeam(buffer, ax, ya + railH * 0.54f, az, bx, yb + railH * 0.54f, bz, 0.04f, 0.04f, 2, 0.78f, 0.82f, 0.86f);
                    if (r % 2 == 0) {
                        AddBeam(buffer, ax, ya, az, ax, ya + railH, az, 0.06f, 0.06f, 2, 0.80f, 0.84f, 0.88f);
                    }
                }
            }
            // Mid-run cylindrical column support down to Y=0
            float midD = 0.5f * (dA + dB);
            float midRunY = 0.5f * (yA + yB);
            addColumnWithFoot(midD, latCenter, midRunY - 0.22f, 0.24f);
        };

        // Upper Sloped Ramp Run: (upperLandLen, ey) -> (dTurnStart, midY)
        buildRampRun(upperLandLen, ey, dTurnStart, midY, latInner);

        // Switchback Intermediate Turn Landing Deck + Grounded Piers
        float turnMidD = 0.5f * (dTurnStart + dTurnEnd);
        float turnMidLat = 0.5f * (latInner + latOuter);
        float turnSpanW = (latOuter - latInner) + rampW;
        addOrientedPart(turnMidD, turnMidLat, midY - 0.12f, turnSpanW, 0.24f, turnLen, walkMat, walkR, walkG, walkB);
        addOrientedPart(turnMidD, turnMidLat, midY - 0.32f, turnSpanW * 0.96f, 0.20f, turnLen * 0.92f, frameMat, frameR, frameG, frameB);
        addColumnWithFoot(turnMidD, latInner, midY - 0.32f, 0.26f);
        addColumnWithFoot(turnMidD, latOuter, midY - 0.32f, 0.26f);
        // Back guardrail on switchback landing
        {
            float lxA = worldX(dTurnEnd, latInner - halfRW), lzA = worldZ(dTurnEnd, latInner - halfRW);
            float lxB = worldX(dTurnEnd, latOuter + halfRW), lzB = worldZ(dTurnEnd, latOuter + halfRW);
            float lxC = worldX(dTurnStart, latOuter + halfRW), lzC = worldZ(dTurnStart, latOuter + halfRW);
            AddBeam(buffer, lxA, midY + railH, lzA, lxB, midY + railH, lzB, 0.08f, 0.08f, 2, 0.86f, 0.90f, 0.94f);
            AddBeam(buffer, lxB, midY + railH, lzB, lxC, midY + railH, lzC, 0.08f, 0.08f, 2, 0.86f, 0.90f, 0.94f);
            AddBeam(buffer, lxA, midY + railH * 0.54f, lzA, lxB, midY + railH * 0.54f, lzB, 0.05f, 0.05f, 2, 0.78f, 0.82f, 0.86f);
        }

        // Lower Sloped Ramp Run returning toward surface touchdown: (dTurnStart, midY) -> (upperLandLen, groundTopY)
        buildRampRun(dTurnStart, midY, upperLandLen, groundTopY, latOuter);

        // Ground-Level ADA Ramp Touchdown Apron Plaza at Y = 0
        float rampPlazaCenterD = upperLandLen - 1.5f;
        addOrientedPart(rampPlazaCenterD, latOuter, 0.08f, rampW * 1.45f, 0.16f, 3.4f, 1, 0.56f, 0.59f, 0.63f);
        // Tactile warning mat at bottom of ramp + surface entry bollards
        addOrientedPart(upperLandLen - 0.25f, latOuter, 0.165f, rampW * 0.95f, 0.03f, 0.45f, 3, 0.94f, 0.78f, 0.18f);
        for (int side : {-1, 1}) {
            float bLat = latOuter + (halfRW + 0.18f) * side;
            addOrientedPart(upperLandLen - 2.2f, bLat, 0.52f, 0.22f, 0.88f, 0.22f, 2, 0.24f, 0.27f, 0.32f);
            addOrientedPart(upperLandLen - 2.2f, bLat, 0.99f, 0.24f, 0.10f, 0.24f, 4, 0.98f, 0.82f, 0.34f);
        }
    }
}

} // namespace Road
} // namespace ApexAssets
