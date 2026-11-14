// ==============================================================================
// Modular Road Component: Signpost & Overhead Gantry 3D Mesh Generator
// Path: native-assets/src/road/signage/SignpostGenerator.cpp
// Standards: MUTCD (Manual on Uniform Traffic Control Devices) & AASHTO LTS-6
// ==============================================================================
#include "SignpostGenerator.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static void AddSignBox(
    SignpostMeshBuffer& buffer,
    float cx, float cy, float cz,
    float sx, float sy, float sz,
    uint32_t matId,
    float r, float g, float b,
    float rough, float metal, float emis = 0.0f
) {
    Primitives::AddBox<SignpostMeshBuffer, SignpostVertex>(
        buffer, cx, cy, cz, sx, sy, sz, matId, r, g, b, 1.0f, rough, metal, emis
    );
}

static void AddBaseRoadwayContext(SignpostMeshBuffer& buffer, float widthM, float lengthM) {
    // Grounded asphalt roadway + shoulder curb so overhead signs/masts show lane scale cleanly
    AddSignBox(buffer, 0.0f, -0.18f, 0.0f, widthM, 0.36f, lengthM,
               0, 0.20f, 0.22f, 0.25f, 0.86f, 0.02f);
    // White outer fog stripes
    for (float sx : {-widthM * 0.44f, widthM * 0.44f}) {
        AddSignBox(buffer, sx, 0.015f, 0.0f, 0.20f, 0.03f, lengthM * 0.94f,
                   3, 0.94f, 0.95f, 0.96f, 0.48f, 0.04f);
    }
    // Yellow median/dashed dividers
    int numDashes = std::max(2, static_cast<int>(lengthM / 5.0f));
    for (int d = 0; d < numDashes; ++d) {
        float dz = -lengthM * 0.42f + d * (lengthM * 0.84f / std::max(1, numDashes - 1));
        AddSignBox(buffer, 0.0f, 0.015f, dz, 0.18f, 0.03f, 2.1f,
                   3, 0.96f, 0.78f, 0.14f, 0.48f, 0.04f);
    }
}

static void AddGuideSignPanel(
    SignpostMeshBuffer& buffer,
    float cx, float cy, float cz,
    float w, float h,
    bool hasLightFixture,
    bool isExitOrangeYellow = false
) {
    // Aluminum Z-channel vertical mounting rails on back
    for (float ox : {-w * 0.35f, w * 0.35f}) {
        AddSignBox(buffer, cx + ox, cy, cz + 0.14f, 0.12f, h * 0.95f, 0.12f,
                   1, 0.70f, 0.74f, 0.78f, 0.32f, 0.85f);
    }
    // Primary retroreflective highway green / MUTCD panel backing + front face
    float bgR = isExitOrangeYellow ? 0.92f : 0.06f;
    float bgG = isExitOrangeYellow ? 0.72f : 0.42f;
    float bgB = isExitOrangeYellow ? 0.10f : 0.23f;
    AddSignBox(buffer, cx, cy, cz, w, h, 0.14f, 2, bgR, bgG, bgB, 0.32f, 0.25f);

    // Crisp White Border Trim Frame on front face (-Z facing approaching drivers)
    float bz = cz - 0.08f;
    float borderT = 0.09f;
    AddSignBox(buffer, cx, cy + h * 0.46f, bz, w * 0.96f, borderT, 0.03f, 3, 0.95f, 0.96f, 0.98f, 0.30f, 0.10f);
    AddSignBox(buffer, cx, cy - h * 0.46f, bz, w * 0.96f, borderT, 0.03f, 3, 0.95f, 0.96f, 0.98f, 0.30f, 0.10f);
    AddSignBox(buffer, cx - w * 0.47f, cy, bz, borderT, h * 0.92f, 0.03f, 3, 0.95f, 0.96f, 0.98f, 0.30f, 0.10f);
    AddSignBox(buffer, cx + w * 0.47f, cy, bz, borderT, h * 0.92f, 0.03f, 3, 0.95f, 0.96f, 0.98f, 0.30f, 0.10f);

    // Simulated white legend bars + Interstate Shield emblem + downward lane arrow
    AddSignBox(buffer, cx - w * 0.22f, cy + h * 0.20f, bz, w * 0.18f, h * 0.24f, 0.03f, 3, 0.12f, 0.34f, 0.78f, 0.28f, 0.20f);
    AddSignBox(buffer, cx + w * 0.10f, cy + h * 0.20f, bz, w * 0.36f, h * 0.12f, 0.03f, 3, 0.95f, 0.96f, 0.98f, 0.30f, 0.10f);
    AddSignBox(buffer, cx + w * 0.05f, cy - h * 0.02f, bz, w * 0.56f, h * 0.11f, 0.03f, 3, 0.95f, 0.96f, 0.98f, 0.30f, 0.10f);
    // Arrow stem down
    AddSignBox(buffer, cx, cy - h * 0.26f, bz, w * 0.08f, h * 0.24f, 0.03f, 4, 0.96f, 0.96f, 0.98f, 0.25f, 0.10f, 0.8f);

    // Bottom Cantilever Floodlight Catwalk & LED Fixtures
    if (hasLightFixture) {
        AddSignBox(buffer, cx, cy - h * 0.55f, cz - 0.65f, w * 0.86f, 0.12f, 0.85f,
                   1, 0.68f, 0.72f, 0.76f, 0.35f, 0.85f);
        for (float lx : {-w * 0.28f, w * 0.28f}) {
            AddSignBox(buffer, cx + lx, cy - h * 0.45f, cz - 0.95f, 0.42f, 0.16f, 0.28f,
                       4, 0.95f, 0.98f, 1.0f, 0.15f, 0.15f, 4.2f);
        }
    }
}

SignpostMeshBuffer SignpostGenerator::GenerateSignpostMesh(const SignpostParams& params) {
    SignpostMeshBuffer buffer;

    switch (params.type) {
        case SignpostType::FullSpanTrussGantry:
            BuildFullSpanTrussGantry(buffer, params);
            break;
        case SignpostType::VariableMessageMatrix:
            BuildVariableMessageMatrix(buffer, params);
            break;
        case SignpostType::RegulatoryChevronCurveSet:
            BuildRegulatoryChevronSet(buffer, params);
            break;
        case SignpostType::ButterflySplitSign:
            BuildButterflySplitSign(buffer, params);
            break;
        case SignpostType::CantileverHighwaySign:
        default:
            BuildCantileverSign(buffer, params);
            break;
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);
    buffer.windLoadDesignKn = params.signBoardWidthM * params.signBoardHeightM * 1.45f; // AASHTO 145 km/h wind load

    return buffer;
}

void SignpostGenerator::BuildCantileverSign(SignpostMeshBuffer& buffer, const SignpostParams& params) {
    float roadW = std::max(12.0f, params.gantrySpanWidthM * 0.85f);
    AddBaseRoadwayContext(buffer, roadW, 18.0f);

    float mastX = roadW * 0.48f + 0.9f;
    float clearH = params.clearanceHeightM;
    float boomYBottom = clearH + 0.35f;
    float boomYTop = boomYBottom + 1.05f;
    float armReachX = mastX - std::max(3.5f, params.gantrySpanWidthM * 0.55f);

    // 1. Concrete Foundation Caisson Plinth + Anchor Flange
    AddSignBox(buffer, mastX, 0.45f, 0.0f, 1.45f, 0.90f, 1.45f, 0, 0.58f, 0.61f, 0.65f, 0.70f, 0.04f);
    AddSignBox(buffer, mastX, 0.96f, 0.0f, 0.95f, 0.12f, 0.95f, 1, 0.68f, 0.72f, 0.76f, 0.32f, 0.85f);

    // 2. Vertical Tapered Galvanized Steel Monotube Mast
    Primitives::AddCylinder<SignpostMeshBuffer, SignpostVertex>(
        buffer, mastX, 0.96f, 0.0f, mastX, boomYTop + 0.35f, 0.0f,
        0.34f, 0.28f, 14, 1, 0.74f, 0.78f, 0.82f, 1.0f, 0.28f, 0.88f, 0.0f
    );

    // 3. Cantilever Twin-Chord Box Truss Boom Arm (Upper + Lower Chords + Diagonal Braces)
    for (float by : {boomYBottom, boomYTop}) {
        for (float bz : {-0.28f, 0.28f}) {
            Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
                buffer, mastX, by, bz, armReachX, by, bz,
                0.15f, 0.15f, 1, 0.76f, 0.80f, 0.85f, 1.0f, 0.28f, 0.88f, 0.0f
            );
        }
    }
    int bays = 7;
    for (int b = 0; b < bays; ++b) {
        float x0 = mastX + (armReachX - mastX) * (static_cast<float>(b) / bays);
        float x1 = mastX + (armReachX - mastX) * (static_cast<float>(b + 1) / bays);
        for (float bz : {-0.28f, 0.28f}) {
            Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
                buffer, x0, boomYBottom, bz, x0, boomYTop, bz,
                0.08f, 0.08f, 1, 0.68f, 0.72f, 0.77f, 1.0f, 0.30f, 0.85f, 0.0f
            );
            Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
                buffer, x0, boomYBottom, bz, x1, boomYTop, bz,
                0.08f, 0.08f, 1, 0.64f, 0.68f, 0.74f, 1.0f, 0.30f, 0.85f, 0.0f
            );
        }
    }

    // 4. Primary Overhead Highway Guide Sign Board suspended cleanly on arm
    float signCenterX = 0.5f * (mastX * 0.4f + armReachX);
    float signCenterY = 0.5f * (boomYBottom + boomYTop);
    AddGuideSignPanel(buffer, signCenterX, signCenterY, -0.35f,
                      params.signBoardWidthM, params.signBoardHeightM, params.hasEmissiveDMS, false);
}

void SignpostGenerator::BuildFullSpanTrussGantry(SignpostMeshBuffer& buffer, const SignpostParams& params) {
    float spanW = std::max(14.0f, params.gantrySpanWidthM);
    float halfSpan = spanW * 0.5f;
    AddBaseRoadwayContext(buffer, spanW - 2.0f, 20.0f);

    float clearH = params.clearanceHeightM;
    float trussBot = clearH + 0.25f;
    float trussTop = trussBot + 1.45f;

    // 1. Left & Right Double-Column Steel Box Towers grounded on Concrete Footings
    for (float side : {-1.0f, 1.0f}) {
        float px = side * halfSpan;
        AddSignBox(buffer, px, 0.50f, 0.0f, 1.35f, 1.0f, 1.85f, 0, 0.56f, 0.59f, 0.63f, 0.70f, 0.04f);
        for (float pz : {-0.45f, 0.45f}) {
            Primitives::AddCylinder<SignpostMeshBuffer, SignpostVertex>(
                buffer, px, 0.95f, pz, px, trussTop + 0.15f, pz,
                0.24f, 0.22f, 10, 1, 0.74f, 0.78f, 0.82f, 1.0f, 0.28f, 0.88f, 0.0f
            );
        }
        // Tower horizontal lacing bars
        for (float hy = 2.0f; hy < trussTop; hy += 1.4f) {
            Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
                buffer, px, hy, -0.45f, px, hy, 0.45f,
                0.11f, 0.11f, 1, 0.66f, 0.70f, 0.75f, 1.0f, 0.30f, 0.85f, 0.0f
            );
        }
    }

    // 2. Full-Span 4-Chord Warren Space Truss Overhead Bridge
    for (float cy : {trussBot, trussTop}) {
        for (float cz : {-0.45f, 0.45f}) {
            Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
                buffer, -halfSpan, cy, cz, halfSpan, cy, cz,
                0.17f, 0.17f, 1, 0.78f, 0.82f, 0.86f, 1.0f, 0.26f, 0.88f, 0.0f
            );
        }
    }
    int bays = std::max(8, static_cast<int>(spanW / 2.2f));
    for (int b = 0; b <= bays; ++b) {
        float bx = -halfSpan + (static_cast<float>(b) / bays) * spanW;
        for (float cz : {-0.45f, 0.45f}) {
            Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
                buffer, bx, trussBot, cz, bx, trussTop, cz,
                0.10f, 0.10f, 1, 0.68f, 0.72f, 0.76f, 1.0f, 0.30f, 0.85f, 0.0f
            );
            if (b < bays) {
                float bxNext = -halfSpan + (static_cast<float>(b + 1) / bays) * spanW;
                Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
                    buffer, bx, trussBot, cz, bxNext, trussTop, cz,
                    0.09f, 0.09f, 1, 0.64f, 0.68f, 0.74f, 1.0f, 0.30f, 0.85f, 0.0f
                );
            }
        }
        // Top & bottom lateral plan struts
        Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
            buffer, bx, trussTop, -0.45f, bx, trussTop, 0.45f,
            0.09f, 0.09f, 1, 0.66f, 0.70f, 0.75f, 1.0f, 0.30f, 0.85f, 0.0f
        );
    }

    // 3. Overhead Backlit Directional Sign Panels (1 to signCount)
    uint32_t count = std::clamp<uint32_t>(params.signCount, 1u, 4u);
    float totalPanelsW = spanW * 0.78f;
    float eachW = std::min(params.signBoardWidthM, (totalPanelsW / count) * 0.86f);
    for (uint32_t s = 0; s < count; ++s) {
        float frac = (count == 1) ? 0.5f : static_cast<float>(s) / (count - 1);
        float sx = -totalPanelsW * 0.35f + frac * totalPanelsW * 0.70f;
        AddGuideSignPanel(buffer, sx, 0.5f * (trussBot + trussTop), -0.52f,
                          eachW, params.signBoardHeightM, params.hasServiceCatwalk, (s == count - 1 && count > 1));
    }
}

void SignpostGenerator::BuildVariableMessageMatrix(SignpostMeshBuffer& buffer, const SignpostParams& params) {
    float roadW = std::max(12.0f, params.gantrySpanWidthM * 0.85f);
    AddBaseRoadwayContext(buffer, roadW, 18.0f);

    float mastX = roadW * 0.48f + 0.85f;
    float clearH = params.clearanceHeightM;
    float armCenterY = clearH + 0.85f;

    // Grounded mast pedestal & large steel T-column + horizontal box beam
    AddSignBox(buffer, mastX, 0.55f, 0.0f, 1.55f, 1.1f, 1.55f, 0, 0.58f, 0.61f, 0.65f, 0.70f, 0.04f);
    Primitives::AddCylinder<SignpostMeshBuffer, SignpostVertex>(
        buffer, mastX, 1.0f, 0.0f, mastX, armCenterY + 0.55f, 0.0f,
        0.38f, 0.32f, 12, 1, 0.70f, 0.74f, 0.78f, 1.0f, 0.28f, 0.88f, 0.0f
    );
    Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
        buffer, mastX, armCenterY, 0.0f, -roadW * 0.15f, armCenterY, 0.0f,
        0.38f, 0.48f, 1, 0.72f, 0.76f, 0.80f, 1.0f, 0.28f, 0.88f, 0.0f
    );

    // Ventilated Aluminum Dynamic Message Cabinet (DMS Housing)
    float cabX = roadW * 0.14f;
    float cabW = std::max(5.5f, params.signBoardWidthM);
    float cabH = std::max(2.6f, params.signBoardHeightM * 0.85f);
    AddSignBox(buffer, cabX, armCenterY, -0.58f, cabW, cabH, 0.92f,
               1, 0.14f, 0.16f, 0.19f, 0.38f, 0.72f);

    // Outer recessed display bezel + dark LED matrix screen
    AddSignBox(buffer, cabX, armCenterY, -1.06f, cabW * 0.94f, cabH * 0.88f, 0.08f,
               2, 0.06f, 0.07f, 0.09f, 0.25f, 0.40f);

    // Emissive Amber Dot-Matrix Message Rows + Cyan Lane Speed Header
    if (params.hasEmissiveDMS) {
        AddSignBox(buffer, cabX - cabW * 0.24f, armCenterY + cabH * 0.20f, -1.11f, cabW * 0.36f, cabH * 0.18f, 0.04f,
                   4, 0.12f, 0.92f, 0.95f, 0.15f, 0.10f, 4.8f);
        AddSignBox(buffer, cabX, armCenterY - cabH * 0.02f, -1.11f, cabW * 0.78f, cabH * 0.14f, 0.04f,
                   4, 1.0f, 0.68f, 0.10f, 0.15f, 0.10f, 5.0f);
        AddSignBox(buffer, cabX, armCenterY - cabH * 0.24f, -1.11f, cabW * 0.64f, cabH * 0.14f, 0.04f,
                   4, 1.0f, 0.68f, 0.10f, 0.15f, 0.10f, 5.0f);

        // Twin Top Amber Corner Strobe Beacons
        for (float sx : {-cabW * 0.40f, cabW * 0.40f}) {
            AddSignBox(buffer, cabX + sx, armCenterY + cabH * 0.58f, -0.75f, 0.35f, 0.32f, 0.35f,
                       4, 1.0f, 0.56f, 0.08f, 0.15f, 0.10f, 4.5f);
        }
    }
}

void SignpostGenerator::BuildRegulatoryChevronSet(SignpostMeshBuffer& buffer, const SignpostParams& params) {
    // Curved Roadside Shoulder Verge with Grounded Breakaway Chevrons + Speed Limit Sign + Milepost
    int numChevrons = std::clamp<int>(params.signCount + 2, 3, 8);
    float curveRadius = 32.0f;
    float arcSpan = 0.72f;

    // Concrete Shoulder Berm & Dark Asphalt Strip underneath chevrons
    for (int i = 0; i < 12; ++i) {
        float a0 = -arcSpan * 0.5f + (static_cast<float>(i) / 12.0f) * arcSpan;
        float a1 = -arcSpan * 0.5f + (static_cast<float>(i + 1) / 12.0f) * arcSpan;
        float rIn = curveRadius - 6.0f, rOut = curveRadius + 1.8f;
        float p00[3] = {rIn * std::cos(a0) - curveRadius, 0.01f, rIn * std::sin(a0)};
        float p01[3] = {rOut * std::cos(a0) - curveRadius, 0.01f, rOut * std::sin(a0)};
        float p11[3] = {rOut * std::cos(a1) - curveRadius, 0.01f, rOut * std::sin(a1)};
        float p10[3] = {rIn * std::cos(a1) - curveRadius, 0.01f, rIn * std::sin(a1)};
        float nUp[3] = {0.0f, 1.0f, 0.0f};
        Primitives::AddQuad<SignpostMeshBuffer, SignpostVertex>(
            buffer, p00, p01, p11, p10, nUp, 0, 0.21f, 0.23f, 0.26f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.85f, 0.02f, 0.0f
        );
    }

    // Spaced MUTCD W1-8 Yellow/Black Chevron Alignment Signs along Outside Shoulder Curve
    for (int c = 0; c < numChevrons; ++c) {
        float frac = (numChevrons == 1) ? 0.5f : static_cast<float>(c) / (numChevrons - 1);
        float ang = -arcSpan * 0.44f + frac * (arcSpan * 0.88f);
        float px = (curveRadius + 0.65f) * std::cos(ang) - curveRadius;
        float pz = (curveRadius + 0.65f) * std::sin(ang);
        float nx = std::cos(ang), nz = std::sin(ang);
        float tx = -nz, tz = nx;

        // Concrete post socket + Twin U-channel steel breakaway legs
        Primitives::AddOrientedBox<SignpostMeshBuffer, SignpostVertex>(
            buffer, px, 0.20f, pz, 0.65f, 0.40f, 0.65f, nx, nz, tx, tz,
            0, 0.58f, 0.61f, 0.65f, 1.0f, 0.72f, 0.04f, 0.0f
        );
        Primitives::AddCylinder<SignpostMeshBuffer, SignpostVertex>(
            buffer, px, 0.35f, pz, px, 2.75f, pz,
            0.09f, 0.08f, 8, 1, 0.74f, 0.78f, 0.82f, 1.0f, 0.30f, 0.85f, 0.0f
        );
        // Bright Yellow W1-8 Diamond/Rectangular Chevron Panel facing oncoming lane
        Primitives::AddOrientedBox<SignpostMeshBuffer, SignpostVertex>(
            buffer, px - tx * 0.08f, 2.45f, pz - tz * 0.08f,
            1.15f, 1.45f, 0.08f, nx, nz, tx, tz,
            3, 0.98f, 0.78f, 0.08f, 1.0f, 0.28f, 0.15f, 0.2f
        );
        // Diagonal Black Chevron Bars facing approaching driver
        Primitives::AddOrientedBox<SignpostMeshBuffer, SignpostVertex>(
            buffer, px - tx * 0.13f, 2.45f, pz - tz * 0.13f,
            0.34f, 1.15f, 0.04f, nx, nz, tx, tz,
            2, 0.10f, 0.11f, 0.13f, 1.0f, 0.45f, 0.10f, 0.0f
        );
    }

    // White Regulatory Speed Limit Sign Stand on First Station
    AddSignBox(buffer, 1.2f, 0.20f, -arcSpan * curveRadius * 0.44f, 0.6f, 0.40f, 0.6f,
               0, 0.58f, 0.61f, 0.65f, 0.72f, 0.04f);
    Primitives::AddCylinder<SignpostMeshBuffer, SignpostVertex>(
        buffer, 1.2f, 0.35f, -arcSpan * curveRadius * 0.44f,
        1.2f, 3.35f, -arcSpan * curveRadius * 0.44f,
        0.10f, 0.09f, 8, 1, 0.76f, 0.80f, 0.84f, 1.0f, 0.30f, 0.86f, 0.0f
    );
    AddSignBox(buffer, 1.2f, 2.95f, -arcSpan * curveRadius * 0.44f - 0.09f, 1.25f, 1.55f, 0.08f,
               3, 0.95f, 0.96f, 0.98f, 0.30f, 0.10f);
}

void SignpostGenerator::BuildButterflySplitSign(SignpostMeshBuffer& buffer, const SignpostParams& params) {
    float roadW = std::max(15.0f, params.gantrySpanWidthM);
    AddBaseRoadwayContext(buffer, roadW, 20.0f);

    float clearH = params.clearanceHeightM;
    float armY = clearH + 0.55f;

    // Central Gore Island Concrete Plinth at Median Split (X=0)
    AddSignBox(buffer, 0.0f, 0.55f, 0.0f, 1.85f, 1.1f, 3.6f, 0, 0.60f, 0.63f, 0.67f, 0.68f, 0.04f);
    // Yellow attenuator nose on gore pedestal
    AddSignBox(buffer, 0.0f, 0.52f, -2.15f, 1.35f, 0.95f, 0.95f, 3, 0.98f, 0.74f, 0.08f, 0.42f, 0.10f);

    // Tall Heavy Steel Center T-Column + Dual Cantilever Butterfly Crossarms
    Primitives::AddCylinder<SignpostMeshBuffer, SignpostVertex>(
        buffer, 0.0f, 1.0f, 0.0f, 0.0f, armY + 1.15f, 0.0f,
        0.40f, 0.34f, 14, 1, 0.74f, 0.78f, 0.82f, 1.0f, 0.28f, 0.88f, 0.0f
    );
    Primitives::AddBeam<SignpostMeshBuffer, SignpostVertex>(
        buffer, -params.signBoardWidthM * 0.92f, armY, 0.0f,
        params.signBoardWidthM * 0.92f, armY, 0.0f,
        0.34f, 0.44f, 1, 0.74f, 0.78f, 0.82f, 1.0f, 0.28f, 0.88f, 0.0f
    );

    // Left (Main Route Green) & Right (Exit Only Yellow Banner + Green) Guide Sign Boards
    float eachW = std::max(4.5f, params.signBoardWidthM * 0.82f);
    AddGuideSignPanel(buffer, -eachW * 0.58f, armY + 0.25f, -0.35f, eachW, params.signBoardHeightM, params.hasEmissiveDMS, false);
    AddGuideSignPanel(buffer,  eachW * 0.58f, armY + 0.25f, -0.35f, eachW, params.signBoardHeightM, params.hasEmissiveDMS, true);
}

} // namespace Road
} // namespace ApexAssets
