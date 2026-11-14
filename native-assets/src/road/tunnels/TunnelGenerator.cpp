// ==============================================================================
// Modular Road Component: Tunnel 3D Mesh Generator Implementation
// Path: native-assets/src/road/tunnels/TunnelGenerator.cpp
// Standards: NFPA 502 & PIARC Road Tunnel Guidelines
// Vulkan Architecture: Sascha Willems / Khronos glTF 2.0 Double-Skinned Vaults
// ==============================================================================
#include "TunnelGenerator.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>
#include <vector>

namespace ApexAssets {
namespace Road {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float DEG2RAD = PI / 180.0f;

static void AddTunnelQuad(
    TunnelMeshBuffer& buffer,
    const float c0[3], const float c1[3], const float c2[3], const float c3[3],
    const float norm[3], uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddQuad<TunnelMeshBuffer, TunnelVertex>(
        buffer, c0, c1, c2, c3, norm, matId, r, g, b, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, rough, metal, emis
    );
}

static void AddTunnelOrientedBox(
    TunnelMeshBuffer& buffer,
    float cx, float cy, float cz,
    float dimN, float dimY, float dimT,
    float nx, float nz, float tx, float tz,
    uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddOrientedBox<TunnelMeshBuffer, TunnelVertex>(
        buffer, cx, cy, cz, dimN, dimY, dimT, nx, nz, tx, tz,
        matId, r, g, b, 1.0f, rough, metal, emis
    );
}

void TunnelGenerator::SampleTunnelCenterline(
    float t,
    const TunnelParams& params,
    float& outX, float& outY, float& outZ,
    float& outTx, float& outTy, float& outTz
) {
    float length = params.tunnelLengthM;

    if (std::abs(params.curveBendAngleDeg) > 0.01f) {
        float totalBendRad = params.curveBendAngleDeg * DEG2RAD;
        float radius = length / totalBendRad;
        float angle = (t - 0.5f) * totalBendRad;

        outX = radius * (1.0f - std::cos(angle));
        outZ = (t - 0.5f) * length;
        outTx = std::sin(angle);
        outTz = std::cos(angle);
        outTy = params.gradientSlopePct * 0.01f;
    } else {
        outX = 0.0f;
        outZ = (t - 0.5f) * length;
        outTx = 0.0f;
        outTy = params.gradientSlopePct * 0.01f;
        outTz = 1.0f;
    }

    outY = t * length * (params.gradientSlopePct * 0.01f);

    float tLen = std::sqrt(outTx * outTx + outTy * outTy + outTz * outTz);
    if (tLen > 1e-6f) {
        outTx /= tLen;
        outTy /= tLen;
        outTz /= tLen;
    }
}

struct BoreProfilePt {
    float lx;
    float ly;
    float nLx;
    float nLy;
};

static std::vector<BoreProfilePt> BuildBoreCrossSection(
    const TunnelParams& params,
    float flareScale,
    float wallThickness
) {
    std::vector<BoreProfilePt> pts;
    float r = params.internalRadiusM * flareScale;

    if (params.boreType == TunnelBoreType::CutAndCoverBox ||
        params.boreType == TunnelBoreType::SubseaImmersedTube) {
        float halfW = (params.boreType == TunnelBoreType::SubseaImmersedTube) ? r * 1.45f : r * 1.25f;
        float clearH = r * 1.35f;
        float chamfer = 0.9f;
        float raw[][2] = {
            {-halfW, 0.0f},
            {-halfW, clearH - chamfer},
            {-halfW + chamfer, clearH},
            { halfW - chamfer, clearH},
            { halfW, clearH - chamfer},
            { halfW, 0.0f}
        };
        int count = 6;
        for (int i = 0; i < count; ++i) {
            float x = raw[i][0] + (raw[i][0] < 0 ? -wallThickness : wallThickness);
            float y = raw[i][1] + (raw[i][1] > clearH * 0.4f ? wallThickness : 0.0f);
            float nx = (raw[i][0] < -0.1f) ? -1.0f : (raw[i][0] > 0.1f ? 1.0f : 0.0f);
            float ny = (raw[i][1] > clearH * 0.5f) ? 1.0f : 0.0f;
            float nL = std::sqrt(nx * nx + ny * ny);
            if (nL > 1e-4f) { nx /= nL; ny /= nL; }
            pts.push_back({x, y, nx, ny});
        }
        return pts;
    }

    // MountainHorseshoe, TwinBoreCircularTBM, FlaredPortalTransition, BiLevelCircularTunnel
    const int slices = 18;
    float haunchH = (params.boreType == TunnelBoreType::MountainHorseshoe ||
                     params.boreType == TunnelBoreType::FlaredPortalTransition)
        ? r * 0.42f
        : (params.boreType == TunnelBoreType::BiLevelCircularTunnel ? r * 0.65f : r * 0.25f);

    float archRadius = (params.boreType == TunnelBoreType::BiLevelCircularTunnel) ? r * 1.18f : r;
    float effectiveR = archRadius + wallThickness;

    for (int s = 0; s <= slices; ++s) {
        // Angle sweeps left footing (-PI/2) through crown (0) to right footing (+PI/2)
        float theta = -PI * 0.5f + (static_cast<float>(s) / slices) * PI;
        float sinT = std::sin(theta);
        float cosT = std::cos(theta);
        float lx = effectiveR * sinT;
        float ly = haunchH + effectiveR * cosT;
        pts.push_back({lx, ly, sinT, cosT});
    }
    return pts;
}

TunnelMeshBuffer TunnelGenerator::GenerateTunnelMesh(const TunnelParams& params) {
    TunnelMeshBuffer buffer;

    const int rings = 32;
    for (int i = 0; i < rings; ++i) {
        float t0 = static_cast<float>(i) / rings;
        float t1 = static_cast<float>(i + 1) / rings;
        ExtrudeTunnelBoreRing(buffer, t0, t1, i, params);
    }

    if (params.hasFlaredPortalHood || params.boreType == TunnelBoreType::FlaredPortalTransition) {
        BuildPortalEntranceHeadwall(buffer, false, params);
        BuildPortalEntranceHeadwall(buffer, true, params);
    }

    if (params.hasJetVentilationFans) {
        BuildJetFans(buffer, params);
    }

    if (params.hasEmergencyEgressNiches) {
        BuildEmergencyNiches(buffer, params);
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);

    float r = params.internalRadiusM;
    float crossSectionArea = PI * r * r * 0.85f;
    if (params.boreType == TunnelBoreType::TwinBoreCircularTBM) crossSectionArea *= 2.0f;
    buffer.excavationVolumeM3 = crossSectionArea * params.tunnelLengthM;
    buffer.totalVentilationAirflowM3s = std::max(1.0f, params.tunnelLengthM / params.jetFanSpacingM) * 35.0f;

    return buffer;
}

void TunnelGenerator::ExtrudeTunnelBoreRing(
    TunnelMeshBuffer& buffer,
    float t0, float t1,
    int ringIdx,
    const TunnelParams& params
) {
    float x0, y0, z0, tx0, ty0, tz0;
    float x1, y1, z1, tx1, ty1, tz1;
    SampleTunnelCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0);
    SampleTunnelCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1);

    float nx0 = tz0, nz0 = -tx0;
    float nx1 = tz1, nz1 = -tx1;

    float flare0 = 1.0f, flare1 = 1.0f;
    if (params.boreType == TunnelBoreType::FlaredPortalTransition) {
        auto calcFlare = [](float t) {
            if (t < 0.16f) return 1.0f + 0.35f * (1.0f - t / 0.16f);
            if (t > 0.84f) return 1.0f + 0.35f * ((t - 0.84f) / 0.16f);
            return 1.0f;
        };
        flare0 = calcFlare(t0);
        flare1 = calcFlare(t1);
    }

    float shellThickness = (params.boreType == TunnelBoreType::TwinBoreCircularTBM && (ringIdx % 3 == 0))
        ? 0.72f
        : 0.55f;

    float tubeOffsets[2] = {0.0f, 0.0f};
    int numBores = (params.boreType == TunnelBoreType::TwinBoreCircularTBM) ? 2 : 1;
    if (numBores == 2) {
        float pitch = params.internalRadiusM * 2.45f;
        tubeOffsets[0] = -pitch * 0.5f;
        tubeOffsets[1] =  pitch * 0.5f;
    }

    for (int bore = 0; bore < numBores; ++bore) {
        float xOff = tubeOffsets[bore];
        auto inner0 = BuildBoreCrossSection(params, flare0, 0.0f);
        auto inner1 = BuildBoreCrossSection(params, flare1, 0.0f);
        auto outer0 = BuildBoreCrossSection(params, flare0, shellThickness);
        auto outer1 = BuildBoreCrossSection(params, flare1, shellThickness);

        int nPts = static_cast<int>(inner0.size());
        float liningColor = (params.boreType == TunnelBoreType::TwinBoreCircularTBM && (ringIdx % 3 == 0)) ? 0.68f : 0.76f;

        for (int s = 0; s < nPts - 1; ++s) {
            // Outer extrados concrete rock-lining skin (visible from exterior camera)
            float o00[3] = { x0 + nx0 * (xOff + outer0[s].lx),     y0 + outer0[s].ly,     z0 + nz0 * (xOff + outer0[s].lx) };
            float o01[3] = { x0 + nx0 * (xOff + outer0[s + 1].lx), y0 + outer0[s + 1].ly, z0 + nz0 * (xOff + outer0[s + 1].lx) };
            float o11[3] = { x1 + nx1 * (xOff + outer1[s + 1].lx), y1 + outer1[s + 1].ly, z1 + nz1 * (xOff + outer1[s + 1].lx) };
            float o10[3] = { x1 + nx1 * (xOff + outer1[s].lx),     y1 + outer1[s].ly,     z1 + nz1 * (xOff + outer1[s].lx) };

            float nOut[3] = {
                nx0 * outer0[s].nLx,
                outer0[s].nLy,
                nz0 * outer0[s].nLx
            };
            AddTunnelQuad(buffer, o00, o01, o11, o10, nOut, 1,
                          liningColor * 0.86f, liningColor * 0.88f, liningColor * 0.92f,
                          params.wallRoughness + 0.15f, 0.04f);

            // Inner intrados tunnel lining (visible when looking through portal mouth)
            float i00[3] = { x0 + nx0 * (xOff + inner0[s].lx),     y0 + inner0[s].ly,     z0 + nz0 * (xOff + inner0[s].lx) };
            float i01[3] = { x0 + nx0 * (xOff + inner0[s + 1].lx), y0 + inner0[s + 1].ly, z0 + nz0 * (xOff + inner0[s + 1].lx) };
            float i11[3] = { x1 + nx1 * (xOff + inner1[s + 1].lx), y1 + inner1[s + 1].ly, z1 + nz1 * (xOff + inner1[s + 1].lx) };
            float i10[3] = { x1 + nx1 * (xOff + inner1[s].lx),     y1 + inner1[s].ly,     z1 + nz1 * (xOff + inner1[s].lx) };

            float nIn[3] = {
                -nx0 * inner0[s].nLx,
                -inner0[s].nLy,
                -nz0 * inner0[s].nLx
            };
            bool isSpringlineLED = params.hasLinearLEDLightingStrips &&
                (s == 3 || s == nPts - 5);
            if (isSpringlineLED) {
                AddTunnelQuad(buffer, i00, i10, i11, i01, nIn, 4,
                              0.95f, 0.98f, 1.00f, 0.15f, 0.10f, params.ledLuminaireIntensity);
            } else {
                AddTunnelQuad(buffer, i00, i10, i11, i01, nIn, 1,
                              liningColor, liningColor + 0.02f, liningColor + 0.04f,
                              params.wallRoughness, 0.04f);
            }

            // Cap thick portal rings at Entrance (ringIdx == 0) and Exit (t1 >= 0.999)
            if (ringIdx == 0) {
                float nEnt[3] = {-tx0, 0.0f, -tz0};
                AddTunnelQuad(buffer, i00, i01, o01, o00, nEnt, 1, 0.72f, 0.74f, 0.78f, 0.55f, 0.05f);
            }
            if (t1 > 0.99f) {
                float nExt[3] = {tx1, 0.0f, tz1};
                AddTunnelQuad(buffer, i10, o10, o11, i11, nExt, 1, 0.72f, 0.74f, 0.78f, 0.55f, 0.05f);
            }
        }

        // Solid Invert Foundation Slab & Dark Asphalt Roadway + Raised Emergency Egress Walkways
        float nUp[3] = {0.0f, 1.0f, 0.0f};
        float halfInnerW = (inner0.back().lx - inner0.front().lx) * 0.5f;
        float slabBotY0 = y0 - 0.55f, slabBotY1 = y1 - 0.55f;

        // Concrete base slab underside / left / right
        float nDown[3] = {0.0f, -1.0f, 0.0f};
        float b00[3] = {x0 + nx0 * (xOff - halfInnerW - shellThickness), slabBotY0, z0 + nz0 * (xOff - halfInnerW - shellThickness)};
        float b01[3] = {x0 + nx0 * (xOff + halfInnerW + shellThickness), slabBotY0, z0 + nz0 * (xOff + halfInnerW + shellThickness)};
        float b11[3] = {x1 + nx1 * (xOff + halfInnerW + shellThickness), slabBotY1, z1 + nz1 * (xOff + halfInnerW + shellThickness)};
        float b10[3] = {x1 + nx1 * (xOff - halfInnerW - shellThickness), slabBotY1, z1 + nz1 * (xOff - halfInnerW - shellThickness)};
        AddTunnelQuad(buffer, b00, b10, b11, b01, nDown, 1, 0.52f, 0.54f, 0.58f, 0.75f, 0.03f);

        // Roadway Asphalt Top
        float roadHalfW = std::min(halfInnerW - params.walkwayWidthM * 0.8f, params.roadwayWidthM * 0.5f);
        float a00[3] = {x0 + nx0 * (xOff - roadHalfW), y0, z0 + nz0 * (xOff - roadHalfW)};
        float a01[3] = {x0 + nx0 * (xOff + roadHalfW), y0, z0 + nz0 * (xOff + roadHalfW)};
        float a11[3] = {x1 + nx1 * (xOff + roadHalfW), y1, z1 + nz1 * (xOff + roadHalfW)};
        float a10[3] = {x1 + nx1 * (xOff - roadHalfW), y1, z1 + nz1 * (xOff - roadHalfW)};
        AddTunnelQuad(buffer, a00, a01, a11, a10, nUp, 0, 0.19f, 0.21f, 0.24f, 0.85f, 0.02f);

        // Centerline yellow stripe
        float yL0[3] = {x0 + nx0 * (xOff - 0.12f), y0 + 0.02f, z0 + nz0 * (xOff - 0.12f)};
        float yR0[3] = {x0 + nx0 * (xOff + 0.12f), y0 + 0.02f, z0 + nz0 * (xOff + 0.12f)};
        float yR1[3] = {x1 + nx1 * (xOff + 0.12f), y1 + 0.02f, z1 + nz1 * (xOff + 0.12f)};
        float yL1[3] = {x1 + nx1 * (xOff - 0.12f), y1 + 0.02f, z1 + nz1 * (xOff - 0.12f)};
        AddTunnelQuad(buffer, yL0, yR0, yR1, yL1, nUp, 4, 0.96f, 0.78f, 0.14f, 0.45f, 0.05f);

        // Raised Left & Right Pedestrian Emergency Egress Walkways
        for (int side : {-1, 1}) {
            float wInner = xOff + side * roadHalfW;
            float wOuter = xOff + side * (halfInnerW + shellThickness * 0.1f);
            float wMin = std::min(wInner, wOuter);
            float wMax = std::max(wInner, wOuter);
            float wy0 = y0 + params.walkwayHeightM;
            float wy1 = y1 + params.walkwayHeightM;

            float wt00[3] = {x0 + nx0 * wMin, wy0, z0 + nz0 * wMin};
            float wt01[3] = {x0 + nx0 * wMax, wy0, z0 + nz0 * wMax};
            float wt11[3] = {x1 + nx1 * wMax, wy1, z1 + nz1 * wMax};
            float wt10[3] = {x1 + nx1 * wMin, wy1, z1 + nz1 * wMin};
            AddTunnelQuad(buffer, wt00, wt01, wt11, wt10, nUp, 2, 0.80f, 0.82f, 0.86f, 0.60f, 0.04f);

            // Inner Curb Face facing roadway
            float curbX0 = x0 + nx0 * wInner, curbZ0 = z0 + nz0 * wInner;
            float curbX1 = x1 + nx1 * wInner, curbZ1 = z1 + nz1 * wInner;
            float cTop0[3] = {curbX0, wy0, curbZ0};
            float cBot0[3] = {curbX0, y0,  curbZ0};
            float cBot1[3] = {curbX1, y1,  curbZ1};
            float cTop1[3] = {curbX1, wy1, curbZ1};
            float nCurb[3] = {-side * nx0, 0.0f, -side * nz0};
            if (side < 0) {
                AddTunnelQuad(buffer, cTop0, cBot0, cBot1, cTop1, nCurb, 2, 0.88f, 0.88f, 0.90f, 0.60f, 0.05f);
            } else {
                AddTunnelQuad(buffer, cTop0, cTop1, cBot1, cBot0, nCurb, 2, 0.88f, 0.88f, 0.90f, 0.60f, 0.05f);
            }
        }

        // Upper Stacked Roadway Deck for BiLevelCircularTunnel
        if (params.boreType == TunnelBoreType::BiLevelCircularTunnel) {
            float deckY0 = y0 + params.internalRadiusM * 0.98f;
            float deckY1 = y1 + params.internalRadiusM * 0.98f;
            float uHalf = params.internalRadiusM * 0.95f;
            float u00[3] = {x0 + nx0 * (-uHalf), deckY0, z0 + nz0 * (-uHalf)};
            float u01[3] = {x0 + nx0 * ( uHalf), deckY0, z0 + nz0 * ( uHalf)};
            float u11[3] = {x1 + nx1 * ( uHalf), deckY1, z1 + nz1 * ( uHalf)};
            float u10[3] = {x1 + nx1 * (-uHalf), deckY1, z1 + nz1 * (-uHalf)};
            AddTunnelQuad(buffer, u00, u01, u11, u10, nUp, 0, 0.22f, 0.24f, 0.28f, 0.82f, 0.04f);
        }

        // Central Structural Wall for CutAndCoverBox & SubseaImmersedTube
        if ((params.boreType == TunnelBoreType::CutAndCoverBox ||
             params.boreType == TunnelBoreType::SubseaImmersedTube) && (ringIdx % 2 == 0)) {
            float segLen = (params.tunnelLengthM / 32.0f) * 1.4f;
            float colH = params.internalRadiusM * 1.32f;
            AddTunnelOrientedBox(buffer, (x0 + x1) * 0.5f, (y0 + y1) * 0.5f + colH * 0.5f, (z0 + z1) * 0.5f,
                                 0.65f, colH, segLen, nx0, nz0, tx0, tz0,
                                 1, 0.72f, 0.74f, 0.78f, 0.65f, 0.04f);
        }
    }
}

void TunnelGenerator::BuildPortalEntranceHeadwall(
    TunnelMeshBuffer& buffer,
    bool isExit,
    const TunnelParams& params
) {
    float t = isExit ? 1.0f : 0.0f;
    float px, py, pz, tx, ty, tz;
    SampleTunnelCenterline(t, params, px, py, pz, tx, ty, tz);

    float nx = tz, nz = -tx;
    float r = params.internalRadiusM;
    float boreOuterHalfW = (params.boreType == TunnelBoreType::TwinBoreCircularTBM)
        ? (r * 2.45f)
        : (r * 1.35f + 0.55f);
    float wallH = std::max(params.portalRetainingWallHeightM, r * 1.9f);
    float crownY = (params.boreType == TunnelBoreType::BiLevelCircularTunnel) ? r * 1.85f : r * 1.45f;
    float dirSign = isExit ? 1.0f : -1.0f;

    // 1. Open-Mouth Retaining Portal: Flanked Wingwalls LEFT and RIGHT + Spandrel Header ABOVE bore
    float wingW = r * 1.1f;
    for (int side : {-1, 1}) {
        float wx = px + nx * (side * (boreOuterHalfW + wingW * 0.45f)) + tx * (dirSign * 2.2f);
        float wz = pz + nz * (side * (boreOuterHalfW + wingW * 0.45f)) + tz * (dirSign * 2.2f);
        AddTunnelOrientedBox(buffer, wx, py + wallH * 0.42f, wz,
                             wingW, wallH * 0.85f, 5.2f,
                             nx, nz, tx, tz,
                             1, 0.64f, 0.67f, 0.71f, 0.72f, 0.04f);
    }

    // Spandrel Concrete Header Wall strictly ABOVE tunnel crown (keeps portal opening clear!)
    float headerH = std::max(1.8f, wallH - crownY);
    float headerSpan = (boreOuterHalfW + wingW) * 2.0f;
    float hx = px + tx * (dirSign * 0.8f);
    float hz = pz + tz * (dirSign * 0.8f);
    AddTunnelOrientedBox(buffer, hx, py + crownY + headerH * 0.5f, hz,
                         headerSpan, headerH, 2.2f,
                         nx, nz, tx, tz,
                         1, 0.68f, 0.71f, 0.75f, 0.68f, 0.05f);

    // Portal Coping Top Parapet Trim
    AddTunnelOrientedBox(buffer, hx, py + crownY + headerH + 0.25f, hz,
                         headerSpan + 0.6f, 0.50f, 2.5f,
                         nx, nz, tx, tz,
                         2, 0.82f, 0.84f, 0.88f, 0.55f, 0.06f);

    // 2. Daylight Adaptation Louver Canopy Beams extending outward from Portal Mouth
    float louverLen = std::min(params.daylightAdaptationLouverLengthM, 14.0f);
    int numSlats = 8;
    for (int s = 1; s <= numSlats; ++s) {
        float dist = (static_cast<float>(s) / numSlats) * louverLen * dirSign;
        float sx = px + tx * dist;
        float sz = pz + tz * dist;
        AddTunnelOrientedBox(buffer, sx, py + crownY - 0.1f, sz,
                             boreOuterHalfW * 2.05f, 0.28f, 0.42f,
                             nx, nz, tx, tz,
                             1, 0.76f, 0.78f, 0.82f, 0.45f, 0.35f);
    }
}

void TunnelGenerator::BuildJetFans(
    TunnelMeshBuffer& buffer,
    const TunnelParams& params
) {
    float spacing = std::max(15.0f, params.jetFanSpacingM);
    int numFanStations = static_cast<int>(params.tunnelLengthM / spacing);

    for (int f = 1; f <= numFanStations; ++f) {
        float t = (f * spacing) / params.tunnelLengthM;
        if (t <= 0.06f || t >= 0.94f) continue;

        float px, py, pz, tx, ty, tz;
        SampleTunnelCenterline(t, params, px, py, pz, tx, ty, tz);
        float nx = tz, nz = -tx;

        float fanCrownY = py + params.internalRadiusM * 1.18f;
        float fanL = 2.8f;

        for (float side : {-1.45f, 1.45f}) {
            float fx = px + nx * side;
            float fz = pz + nz * side;
            float p1x = fx - tx * (fanL * 0.5f), p1z = fz - tz * (fanL * 0.5f);
            float p2x = fx + tx * (fanL * 0.5f), p2z = fz + tz * (fanL * 0.5f);

            // Jet fan silencer turbine drum (Industrial safety orange steel)
            Primitives::AddCylinder<TunnelMeshBuffer, TunnelVertex>(
                buffer, p1x, fanCrownY, p1z, p2x, fanCrownY, p2z,
                0.48f, 0.48f, 12, 3, 0.88f, 0.44f, 0.16f, 1.0f, 0.32f, 0.85f, 0.0f
            );
            // Crown hanger struts
            Primitives::AddBeam<TunnelMeshBuffer, TunnelVertex>(
                buffer, fx, fanCrownY + 0.4f, fz, fx, fanCrownY + 1.0f, fz,
                0.14f, 0.14f, 3, 0.65f, 0.68f, 0.72f, 1.0f, 0.35f, 0.85f, 0.0f
            );
        }
    }
}

void TunnelGenerator::BuildEmergencyNiches(
    TunnelMeshBuffer& buffer,
    const TunnelParams& params
) {
    const float nicheSpacing = 45.0f;
    int numNiches = std::max(1, static_cast<int>(params.tunnelLengthM / nicheSpacing));

    for (int n = 1; n <= numNiches; ++n) {
        float t = (static_cast<float>(n) / (numNiches + 1));
        float px, py, pz, tx, ty, tz;
        SampleTunnelCenterline(t, params, px, py, pz, tx, ty, tz);
        float nx = tz, nz = -tx;

        float wallOff = -params.internalRadiusM * 0.88f;
        float sx = px + nx * wallOff;
        float sz = pz + nz * wallOff;

        // Emergency Cabinet Frame + Luminous Green SOS Exit Sign
        AddTunnelOrientedBox(buffer, sx, py + 1.45f, sz, 0.35f, 1.9f, 1.15f,
                             nx, nz, tx, tz, 2, 0.86f, 0.88f, 0.90f, 0.45f, 0.30f);
        AddTunnelOrientedBox(buffer, sx + nx * 0.22f, py + 2.55f, sz + nz * 0.22f,
                             0.14f, 0.52f, 0.95f,
                             nx, nz, tx, tz, 4, 0.12f, 0.95f, 0.35f, 0.15f, 0.10f, 4.5f);
    }
}

} // namespace Road
} // namespace ApexAssets
