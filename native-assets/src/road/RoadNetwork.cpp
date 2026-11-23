// ==============================================================================
// Modular Road Component: Road Network & Signalized Intersection 3D Implementation
// Path: native-assets/src/road/RoadNetwork.cpp
// ==============================================================================
#include "RoadNetwork.h"
#include "common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static void AddNetQuad(
    RoadNetworkMeshBuffer& buf,
    const float c0[3], const float c1[3], const float c2[3], const float c3[3],
    const float n[3], uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddQuad<RoadNetworkMeshBuffer, RoadNetworkVertex>(
        buf, c0, c1, c2, c3, n, matId, r, g, b, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, rough, metal, emis
    );
}

static void AddNetBox(
    RoadNetworkMeshBuffer& buf,
    float cx, float cy, float cz,
    float sx, float sy, float sz,
    uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddBox<RoadNetworkMeshBuffer, RoadNetworkVertex>(
        buf, cx, cy, cz, sx, sy, sz, matId, r, g, b, 1.0f, rough, metal, emis
    );
}

static void AddNetCylinder(
    RoadNetworkMeshBuffer& buf,
    float x1, float y1, float z1,
    float x2, float y2, float z2,
    float rb, float rt, int slices, uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddCylinder<RoadNetworkMeshBuffer, RoadNetworkVertex>(
        buf, x1, y1, z1, x2, y2, z2, rb, rt, slices, matId, r, g, b, 1.0f, rough, metal, emis
    );
}

static void AddTrafficSignalMast(
    RoadNetworkMeshBuffer& buf,
    float baseX, float baseZ,
    float armDirX, float armDirZ,
    float armLen, float mastH,
    bool greenPhase
) {
    // Concrete pedestal
    AddNetBox(buf, baseX, 0.25f, baseZ, 0.72f, 0.50f, 0.72f, 1, 0.62f, 0.65f, 0.69f, 0.7f, 0.05f);
    // Galvanized vertical pole
    AddNetCylinder(buf, baseX, 0.45f, baseZ, baseX, mastH + 0.6f, baseZ, 0.16f, 0.12f, 10, 2, 0.76f, 0.80f, 0.84f, 0.30f, 0.88f);
    // Horizontal cantilever mast arm
    float tipX = baseX + armDirX * armLen;
    float tipZ = baseZ + armDirZ * armLen;
    Primitives::AddBeam<RoadNetworkMeshBuffer, RoadNetworkVertex>(
        buf, baseX, mastH, baseZ, tipX, mastH + 0.18f, tipZ,
        0.18f, 0.22f, 2, 0.74f, 0.78f, 0.82f, 1.0f, 0.30f, 0.88f
    );
    // Diagonal tension stay gusset
    Primitives::AddBeam<RoadNetworkMeshBuffer, RoadNetworkVertex>(
        buf, baseX, mastH - 1.1f, baseZ, baseX + armDirX * (armLen * 0.48f), mastH + 0.08f, baseZ + armDirZ * (armLen * 0.48f),
        0.09f, 0.09f, 2, 0.70f, 0.74f, 0.78f, 1.0f, 0.32f, 0.85f
    );

    // 2 overhead signal heads along the mast arm
    for (int h = 0; h < 2; ++h) {
        float frac = (h == 0) ? 0.55f : 0.88f;
        float hx = baseX + armDirX * (armLen * frac);
        float hz = baseZ + armDirZ * (armLen * frac);
        float hy = mastH - 0.58f;
        // Signal cabinet housing (black/dark anodized aluminum)
        AddNetBox(buf, hx, hy, hz, 0.44f, 0.96f, 0.40f, 2, 0.14f, 0.15f, 0.17f, 0.45f, 0.65f);
        // R/Y/G LED aspects
        float rEmis = greenPhase ? 0.15f : 6.0f;
        float gEmis = greenPhase ? 6.0f : 0.15f;
        AddNetBox(buf, hx, hy + 0.28f, hz, 0.47f, 0.20f, 0.43f, 4, 0.95f, 0.12f, 0.10f, 0.2f, 0.0f, rEmis);
        AddNetBox(buf, hx, hy, hz,         0.47f, 0.20f, 0.43f, 4, 0.98f, 0.68f, 0.08f, 0.2f, 0.0f, 0.4f);
        AddNetBox(buf, hx, hy - 0.28f, hz, 0.47f, 0.20f, 0.43f, 4, 0.10f, 0.95f, 0.45f, 0.2f, 0.0f, gEmis);
    }

    // Luminaire extension pole + warm LED cobra-head
    float lampH = mastH + 2.2f;
    AddNetCylinder(buf, baseX, mastH, baseZ, baseX, lampH, baseZ, 0.11f, 0.08f, 8, 2, 0.78f, 0.82f, 0.86f, 0.3f, 0.88f);
    float lx = baseX + armDirX * 1.6f;
    float lz = baseZ + armDirZ * 1.6f;
    Primitives::AddBeam<RoadNetworkMeshBuffer, RoadNetworkVertex>(
        buf, baseX, lampH, baseZ, lx, lampH + 0.22f, lz, 0.10f, 0.10f, 2, 0.78f, 0.82f, 0.86f, 1.0f, 0.3f, 0.85f
    );
    AddNetBox(buf, lx, lampH + 0.12f, lz, 0.45f, 0.10f, 0.30f, 4, 0.98f, 0.94f, 0.78f, 0.2f, 0.1f, 3.8f);
}

static void AddZebraCrosswalk(
    RoadNetworkMeshBuffer& buf,
    float centerX, float centerZ,
    float widthAlongRoad, float spanAcrossRoad,
    bool orientAlongX
) {
    int stripes = std::max(4, static_cast<int>(spanAcrossRoad / 1.15f));
    float stripePitch = spanAcrossRoad / stripes;
    for (int i = 0; i < stripes; ++i) {
        float offset = -spanAcrossRoad * 0.5f + (i + 0.5f) * stripePitch;
        float cx = orientAlongX ? centerX : (centerX + offset);
        float cz = orientAlongX ? (centerZ + offset) : centerZ;
        float sx = orientAlongX ? widthAlongRoad : (stripePitch * 0.48f);
        float sz = orientAlongX ? (stripePitch * 0.48f) : widthAlongRoad;
        AddNetBox(buf, cx, 0.365f, cz, sx, 0.025f, sz, 3, 0.94f, 0.95f, 0.97f, 0.45f, 0.04f);
    }
}

static void BuildSingleIntersectionNode(
    RoadNetworkMeshBuffer& buf,
    float originX, float originZ,
    float roadW, float armSpan, float cornerR,
    uint32_t laneCount, bool hasSignals, bool isTJunction
) {
    float halfR = roadW * 0.5f;
    float halfArm = armSpan * 0.5f;

    // 1. Sub-base & continuous asphalt crossing pad
    // East-West corridor asphalt
    AddNetBox(buf, originX, 0.16f, originZ, armSpan, 0.32f, roadW, 0, 0.19f, 0.21f, 0.24f, 0.85f, 0.02f);
    // North-South corridor asphalt (for T-junction only half +Z arm extends from junction)
    if (isTJunction) {
        float nsLen = halfArm + halfR;
        float nsCenterZ = originZ + (nsLen * 0.5f) - halfR;
        AddNetBox(buf, originX, 0.162f, nsCenterZ, roadW, 0.32f, nsLen, 0, 0.19f, 0.21f, 0.24f, 0.85f, 0.02f);
        // Southern continuous concrete sidewalk apron closing T-junction top
        AddNetBox(buf, originX, 0.25f, originZ - halfR - 2.5f, armSpan, 0.50f, 5.0f, 1, 0.66f, 0.69f, 0.72f, 0.72f, 0.04f);
        // Raised granite curb along bottom edge
        AddNetBox(buf, originX, 0.39f, originZ - halfR - 0.18f, armSpan, 0.18f, 0.36f, 1, 0.78f, 0.80f, 0.83f, 0.65f, 0.04f);
    } else {
        AddNetBox(buf, originX, 0.162f, originZ, roadW, 0.32f, armSpan, 0, 0.19f, 0.21f, 0.24f, 0.85f, 0.02f);
    }

    // 2. Central intersection box asphalt apron + yellow box junction diagonal accent
    AddNetBox(buf, originX, 0.17f, originZ, roadW + 1.2f, 0.33f, roadW + 1.2f, 0, 0.18f, 0.20f, 0.23f, 0.84f, 0.02f);

    // 3. Quadrant raised concrete sidewalks with curved/chamfered corner fillets
    float quadSpan = std::max(4.0f, halfArm - halfR);
    float qCenterDist = halfR + quadSpan * 0.5f;
    for (int sx : {-1, 1}) {
        for (int sz : {-1, 1}) {
            if (isTJunction && sz < 0) continue; // T-junction uses full southern sidewalk above
            float qcx = originX + sx * qCenterDist;
            float qcz = originZ + sz * qCenterDist;
            AddNetBox(buf, qcx, 0.24f, qcz, quadSpan, 0.48f, quadSpan, 1, 0.62f, 0.65f, 0.69f, 0.74f, 0.04f);

            // Inner chamfered corner curb bevel ring stepping around cornerR
            float cornerX = originX + sx * (halfR + cornerR);
            float cornerZ = originZ + sz * (halfR + cornerR);
            int arcSegs = 6;
            for (int a = 0; a < arcSegs; ++a) {
                float th0 = (static_cast<float>(a) / arcSegs) * (Primitives::PI * 0.5f);
                float th1 = (static_cast<float>(a + 1) / arcSegs) * (Primitives::PI * 0.5f);
                float p0x = cornerX - sx * std::cos(th0) * cornerR;
                float p0z = cornerZ - sz * std::sin(th0) * cornerR;
                float p1x = cornerX - sx * std::cos(th1) * cornerR;
                float p1z = cornerZ - sz * std::sin(th1) * cornerR;
                Primitives::AddBeam<RoadNetworkMeshBuffer, RoadNetworkVertex>(
                    buf, p0x, 0.41f, p0z, p1x, 0.41f, p1z,
                    0.35f, 0.18f, 1, 0.80f, 0.82f, 0.85f, 1.0f, 0.62f, 0.05f
                );
            }
        }
    }

    // 4. Zebra Pedestrian Crosswalks & Stop Bars on approaches
    float cwDist = halfR + 2.4f;
    float cwSpan = roadW * 0.84f;
    AddZebraCrosswalk(buf, originX - cwDist, originZ, 2.6f, cwSpan, true);
    AddZebraCrosswalk(buf, originX + cwDist, originZ, 2.6f, cwSpan, true);
    AddZebraCrosswalk(buf, originX, originZ + cwDist, 2.6f, cwSpan, false);
    if (!isTJunction) {
        AddZebraCrosswalk(buf, originX, originZ - cwDist, 2.6f, cwSpan, false);
    }

    // Solid white stop bars & yellow centerlines on every arm outside crosswalk
    float stopDist = cwDist + 2.2f;
    float lineLen = std::max(2.0f, halfArm - stopDist - 0.5f);
    float lineMid = stopDist + lineLen * 0.5f;
    for (int s : {-1, 1}) {
        // Stop bars on E-W
        AddNetBox(buf, originX + s * stopDist, 0.34f, originZ, 0.42f, 0.025f, roadW * 0.90f, 3, 0.95f, 0.96f, 0.97f, 0.42f, 0.04f);
        // Double yellow median lines E-W
        for (float yo : {-0.18f, 0.18f}) {
            AddNetBox(buf, originX + s * lineMid, 0.34f, originZ + yo, lineLen, 0.025f, 0.14f, 3, 0.96f, 0.76f, 0.12f, 0.45f, 0.05f);
        }
        // Dashed lane dividers when >= 3 lanes
        if (laneCount >= 3) {
            int dashes = std::max(1, static_cast<int>(lineLen / 4.2f));
            for (int d = 0; d < dashes; ++d) {
                float dx = originX + s * (stopDist + 2.2f + d * 4.2f);
                for (float lo : {-roadW * 0.25f, roadW * 0.25f}) {
                    AddNetBox(buf, dx, 0.34f, originZ + lo, 1.8f, 0.024f, 0.12f, 3, 0.93f, 0.94f, 0.96f, 0.45f, 0.04f);
                }
            }
        }
    }
    for (int s : {-1, 1}) {
        if (isTJunction && s < 0) continue;
        AddNetBox(buf, originX, 0.34f, originZ + s * stopDist, roadW * 0.90f, 0.025f, 0.42f, 3, 0.95f, 0.96f, 0.97f, 0.42f, 0.04f);
        for (float xo : {-0.18f, 0.18f}) {
            AddNetBox(buf, originX + xo, 0.34f, originZ + s * lineMid, 0.14f, 0.025f, lineLen, 3, 0.96f, 0.76f, 0.12f, 0.45f, 0.05f);
        }
    }

    // Channelized raised median splitter island on T-junction stem
    if (isTJunction) {
        AddNetBox(buf, originX, 0.41f, originZ + stopDist + lineLen * 0.45f, 1.2f, 0.18f, lineLen * 0.65f, 1, 0.76f, 0.78f, 0.80f, 0.65f, 0.04f);
    }

    // 5. Overhead Traffic Signal Cantilever Masts on intersection corners
    if (hasSignals) {
        float mOff = halfR + 1.6f;
        AddTrafficSignalMast(buf, originX + mOff, originZ + mOff, -1.0f, 0.0f, halfR + 1.2f, 6.2f, true);
        AddTrafficSignalMast(buf, originX - mOff, originZ + mOff, 0.0f, -1.0f, halfR + 1.2f, 6.2f, false);
        if (!isTJunction) {
            AddTrafficSignalMast(buf, originX - mOff, originZ - mOff, 1.0f, 0.0f, halfR + 1.2f, 6.2f, true);
            AddTrafficSignalMast(buf, originX + mOff, originZ - mOff, 0.0f, 1.0f, halfR + 1.2f, 6.2f, false);
        } else {
            AddTrafficSignalMast(buf, originX, originZ - halfR - 1.4f, 0.0f, 1.0f, halfR + 0.8f, 6.2f, false);
        }
    }
}

static void BuildYForkBifurcation(
    RoadNetworkMeshBuffer& buf,
    const RoadNetworkParams& params
) {
    float roadW = params.roadWidthM;
    float halfR = roadW * 0.5f;
    float totalLen = params.armSpanM;
    float splitZ = -totalLen * 0.10f;
    float halfLen = totalLen * 0.5f;

    // 1. Incoming Main Trunk Road (-Z -> splitZ)
    float trunkLen = splitZ - (-halfLen);
    float trunkCenterZ = -halfLen + trunkLen * 0.5f;
    AddNetBox(buf, 0.0f, 0.16f, trunkCenterZ, roadW, 0.32f, trunkLen, 0, 0.19f, 0.21f, 0.24f, 0.85f, 0.02f);
    // Double yellow edge & white dashed lane stripes
    for (float xo : {-halfR + 0.45f, halfR - 0.45f}) {
        AddNetBox(buf, xo, 0.33f, trunkCenterZ, 0.18f, 0.02f, trunkLen, 3, 0.96f, 0.96f, 0.97f, 0.45f, 0.04f);
    }

    // 2. Diverging Left & Right Fork Branches from splitZ -> +halfLen
    float branchLen = halfLen - splitZ;
    float branchAngleRad = 22.0f * Primitives::DEG2RAD;
    float branchW = roadW * 0.72f;
    int segs = 14;
    for (int side : {-1, 1}) {
        float dirX = side * std::sin(branchAngleRad);
        float dirZ = std::cos(branchAngleRad);
        float nX = dirZ, nZ = -dirX;
        float halfBW = branchW * 0.5f;

        for (int s = 0; s < segs; ++s) {
            float t0 = static_cast<float>(s) / segs;
            float t1 = static_cast<float>(s + 1) / segs;
            float d0 = t0 * branchLen / dirZ;
            float d1 = t1 * branchLen / dirZ;

            float cx0 = dirX * d0;
            float cz0 = splitZ + dirZ * d0;
            float cx1 = dirX * d1;
            float cz1 = splitZ + dirZ * d1;

            float pL0[3] = {cx0 - nX * halfBW, 0.32f, cz0 - nZ * halfBW};
            float pR0[3] = {cx0 + nX * halfBW, 0.32f, cz0 + nZ * halfBW};
            float pL1[3] = {cx1 - nX * halfBW, 0.32f, cz1 - nZ * halfBW};
            float pR1[3] = {cx1 + nX * halfBW, 0.32f, cz1 + nZ * halfBW};
            float nUp[3] = {0.0f, 1.0f, 0.0f};
            AddNetQuad(buf, pL0, pR0, pR1, pL1, nUp, 0, 0.19f, 0.21f, 0.24f, 0.85f, 0.02f);

            // Outer shoulder curb apron
            float midX = 0.5f * (cx0 + cx1) + side * nX * (halfBW + 0.35f);
            float midZ = 0.5f * (cz0 + cz1) + side * nZ * (halfBW + 0.35f);
            Primitives::AddOrientedBox<RoadNetworkMeshBuffer, RoadNetworkVertex>(
                buf, midX, 0.26f, midZ, 0.70f, 0.52f, (d1 - d0) + 0.15f,
                nX, nZ, dirX, dirZ, 1, 0.65f, 0.68f, 0.72f, 1.0f, 0.70f, 0.04f
            );

            // White edge striping
            float sX = 0.5f * (cx0 + cx1);
            float sZ = 0.5f * (cz0 + cz1);
            if (s % 2 == 0) {
                Primitives::AddOrientedBox<RoadNetworkMeshBuffer, RoadNetworkVertex>(
                    buf, sX, 0.34f, sZ, 0.16f, 0.025f, (d1 - d0) * 0.65f,
                    nX, nZ, dirX, dirZ, 3, 0.96f, 0.96f, 0.97f, 1.0f, 0.45f, 0.04f
                );
            }
        }
    }

    // 3. Gore Wedge Concrete Island & Chevron Stripes + Yellow Impact Attenuator Nose
    int chevrons = 7;
    for (int c = 0; c < chevrons; ++c) {
        float frac = (static_cast<float>(c) + 0.5f) / chevrons;
        float zPos = splitZ + 3.0f + frac * (branchLen * 0.45f);
        float w = 0.6f + frac * (roadW * 0.48f);
        AddNetBox(buf, 0.0f, 0.35f, zPos, w, 0.05f, 0.48f, 3, 0.98f, 0.82f, 0.12f, 0.42f, 0.05f);
    }
    // QuadGuard Crash Attenuator Barrels at Gore Nose
    for (int b = 0; b < 4; ++b) {
        float bz = splitZ + 4.5f + b * 1.15f;
        AddNetCylinder(buf, 0.0f, 0.34f, bz, 0.0f, 1.32f, bz, 0.48f, 0.46f, 10, 3, 0.98f, 0.78f, 0.08f, 0.38f, 0.05f);
        AddNetBox(buf, 0.0f, 1.10f, bz, 0.96f, 0.16f, 0.96f, 3, 0.12f, 0.12f, 0.14f, 0.50f, 0.05f);
    }

    // Overhead guide bifurcation gantry if signals enabled
    if (params.hasTrafficSignals) {
        float gz = splitZ + 2.0f;
        float span = roadW + 3.0f;
        AddNetCylinder(buf, -span * 0.5f, 0.0f, gz, -span * 0.5f, 6.8f, gz, 0.24f, 0.20f, 10, 2, 0.76f, 0.80f, 0.84f, 0.30f, 0.88f);
        AddNetCylinder(buf,  span * 0.5f, 0.0f, gz,  span * 0.5f, 6.8f, gz, 0.24f, 0.20f, 10, 2, 0.76f, 0.80f, 0.84f, 0.30f, 0.88f);
        AddNetBox(buf, 0.0f, 6.6f, gz, span + 0.5f, 0.55f, 0.55f, 2, 0.74f, 0.78f, 0.82f, 0.32f, 0.85f);
        // Dual directional MUTCD green guide boards
        AddNetBox(buf, -span * 0.22f, 6.8f, gz + 0.35f, span * 0.38f, 2.2f, 0.12f, 1, 0.04f, 0.42f, 0.24f, 0.35f, 0.15f);
        AddNetBox(buf,  span * 0.22f, 6.8f, gz + 0.35f, span * 0.38f, 2.2f, 0.12f, 1, 0.04f, 0.42f, 0.24f, 0.35f, 0.15f);
    }
}

RoadNetworkMeshBuffer RoadNetworkGenerator::GenerateNetworkMesh(const RoadNetworkParams& params) {
    RoadNetworkMeshBuffer buf;

    RoadNetworkParams clamped = params;
    clamped.roadWidthM = std::clamp(params.roadWidthM, 7.5f, 20.0f);
    clamped.armSpanM = std::clamp(params.armSpanM, 30.0f, 110.0f);
    clamped.laneCount = std::clamp<uint32_t>(params.laneCount, 2u, 4u);
    clamped.cornerRadiusM = std::clamp(params.cornerRadiusM, 3.0f, 16.0f);

    switch (clamped.type) {
        case RoadNetworkType::Signalized4WayIntersection:
            BuildSingleIntersectionNode(buf, 0.0f, 0.0f, clamped.roadWidthM, clamped.armSpanM, clamped.cornerRadiusM, clamped.laneCount, clamped.hasTrafficSignals, false);
            buf.nodeCount = 1;
            buf.signalPhaseCount = 4;
            break;

        case RoadNetworkType::TJunctionArterial:
            BuildSingleIntersectionNode(buf, 0.0f, 0.0f, clamped.roadWidthM, clamped.armSpanM, clamped.cornerRadiusM, clamped.laneCount, clamped.hasTrafficSignals, true);
            buf.nodeCount = 1;
            buf.signalPhaseCount = 3;
            break;

        case RoadNetworkType::YForkBifurcation:
            BuildYForkBifurcation(buf, clamped);
            buf.nodeCount = 2;
            buf.signalPhaseCount = 2;
            break;

        case RoadNetworkType::DistrictStreetGrid: {
            float nodePitch = std::max(clamped.roadWidthM * 2.2f, clamped.armSpanM * 0.56f);
            float localArm = nodePitch * 0.98f;
            float halfP = nodePitch * 0.5f;
            for (int gx : {-1, 1}) {
                for (int gz : {-1, 1}) {
                    BuildSingleIntersectionNode(
                        buf,
                        gx * halfP,
                        gz * halfP,
                        clamped.roadWidthM * 0.82f,
                        localArm,
                        std::min(clamped.cornerRadiusM, 6.5f),
                        clamped.laneCount,
                        clamped.hasTrafficSignals,
                        false
                    );
                }
            }
            // Central urban civic courtyard park pad at grid center
            float innerSpan = std::max(4.0f, nodePitch - clamped.roadWidthM * 0.85f);
            AddNetBox(buf, 0.0f, 0.25f, 0.0f, innerSpan, 0.50f, innerSpan, 1, 0.55f, 0.59f, 0.64f, 0.72f, 0.04f);
            AddNetBox(buf, 0.0f, 0.54f, 0.0f, innerSpan * 0.72f, 0.12f, innerSpan * 0.72f, 3, 0.22f, 0.46f, 0.26f, 0.85f, 0.02f);
            buf.nodeCount = 4;
            buf.signalPhaseCount = 8;
            break;
        }
    }

    Primitives::ComputeMeshAABB(buf);
    buf.vertexCount = static_cast<uint32_t>(buf.vertices.size());
    buf.triangleCount = static_cast<uint32_t>(buf.indices.size() / 3);
    return buf;
}

} // namespace Road
} // namespace ApexAssets
