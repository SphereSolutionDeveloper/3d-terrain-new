// ==============================================================================
// Modular Road Component: Interchange & Ramp 3D Mesh Generator Implementation
// Path: native-assets/src/road/interchanges/InterchangeGen.cpp
// Standards: AASHTO Green Book Geometric Design & Box-Girder Viaduct Piers
// ==============================================================================
#include "InterchangeGen.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float DEG2RAD = PI / 180.0f;

static void AddIntQuad(
    InterchangeMeshBuffer& buffer,
    const float c0[3], const float c1[3], const float c2[3], const float c3[3],
    const float norm[3], uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddQuad<InterchangeMeshBuffer, InterchangeVertex>(
        buffer, c0, c1, c2, c3, norm, matId, r, g, b, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, rough, metal, emis
    );
}

static void AddIntBox(
    InterchangeMeshBuffer& buffer,
    float cx, float cy, float cz,
    float dimN, float dimY, float dimT,
    float nx, float nz, float tx, float tz,
    uint32_t matId,
    float r, float g, float b, float rough, float metal
) {
    Primitives::AddOrientedBox<InterchangeMeshBuffer, InterchangeVertex>(
        buffer, cx, cy, cz, dimN, dimY, dimT, nx, nz, tx, tz,
        matId, r, g, b, 1.0f, rough, metal, 0.0f
    );
}

static void AddInterchangePier(
    InterchangeMeshBuffer& buffer,
    float px, float py, float pz,
    float nx, float nz, float tx, float tz,
    float deckHalfW, float girderDepth
) {
    float seatY = py - girderDepth - 0.30f;
    if (seatY <= 1.8f) return;

    float footH = 1.4f;
    float capH = 1.15f;
    // Grounded concrete pile cap footing at Y=0
    AddIntBox(buffer, px, footH * 0.5f, pz,
              deckHalfW * 1.45f, footH, 4.4f, nx, nz, tx, tz,
              1, 0.48f, 0.51f, 0.55f, 0.72f, 0.04f);

    // Cylindrical concrete pier columns
    if (deckHalfW > 5.0f) {
        for (int side : {-1, 1}) {
            float sx = px + nx * (deckHalfW * 0.45f * side);
            float sz = pz + nz * (deckHalfW * 0.45f * side);
            Primitives::AddCylinder<InterchangeMeshBuffer, InterchangeVertex>(
                buffer, sx, footH, sz, sx, seatY - capH, sz,
                0.95f, 0.82f, 12, 1, 0.66f, 0.70f, 0.74f, 1.0f, 0.65f, 0.04f, 0.0f
            );
        }
    } else {
        Primitives::AddCylinder<InterchangeMeshBuffer, InterchangeVertex>(
            buffer, px, footH, pz, px, seatY - capH, pz,
            1.15f, 0.95f, 12, 1, 0.66f, 0.70f, 0.74f, 1.0f, 0.65f, 0.04f, 0.0f
        );
    }

    // Hammerhead pier cap + elastomer bearings
    AddIntBox(buffer, px, seatY - capH * 0.5f, pz,
              deckHalfW * 1.55f, capH, 2.8f, nx, nz, tx, tz,
              1, 0.62f, 0.65f, 0.69f, 0.68f, 0.04f);
    AddIntBox(buffer, px, seatY + 0.15f, pz,
              deckHalfW * 1.1f, 0.30f, 1.4f, nx, nz, tx, tz,
              4, 0.20f, 0.22f, 0.25f, 0.35f, 0.75f);
}

static void ExtrudeSolidRoadSegment(
    InterchangeMeshBuffer& buffer,
    float x0, float y0, float z0, float nx0, float nz0, float bank0,
    float x1, float y1, float z1, float nx1, float nz1, float bank1,
    float halfW, float depth, bool withBarriers, bool centerStripeYellow
) {
    float faciaH = 0.35f;
    float botW = halfW * 0.65f;

    float yL0 = y0 - std::sin(bank0) * halfW;
    float yR0 = y0 + std::sin(bank0) * halfW;
    float yL1 = y1 - std::sin(bank1) * halfW;
    float yR1 = y1 + std::sin(bank1) * halfW;

    float tl0[3] = {x0 - nx0 * halfW, yL0, z0 - nz0 * halfW};
    float tr0[3] = {x0 + nx0 * halfW, yR0, z0 + nz0 * halfW};
    float tl1[3] = {x1 - nx1 * halfW, yL1, z1 - nz1 * halfW};
    float tr1[3] = {x1 + nx1 * halfW, yR1, z1 + nz1 * halfW};

    float fl0[3] = {tl0[0], yL0 - faciaH, tl0[2]};
    float fr0[3] = {tr0[0], yR0 - faciaH, tr0[2]};
    float fl1[3] = {tl1[0], yL1 - faciaH, tl1[2]};
    float fr1[3] = {tr1[0], yR1 - faciaH, tr1[2]};

    float bl0[3] = {x0 - nx0 * botW, y0 - depth, z0 - nz0 * botW};
    float br0[3] = {x0 + nx0 * botW, y0 - depth, z0 + nz0 * botW};
    float bl1[3] = {x1 - nx1 * botW, y1 - depth, z1 - nz1 * botW};
    float br1[3] = {x1 + nx1 * botW, y1 - depth, z1 + nz1 * botW};

    float nUp[3] = {0.0f, 1.0f, 0.0f};
    // Asphalt top wearing course
    AddIntQuad(buffer, tl0, tr0, tr1, tl1, nUp, 0, 0.21f, 0.23f, 0.26f, 0.85f, 0.02f);

    // Center stripe & White Shoulder Edge Fog Lines
    if (centerStripeYellow) {
        float c0[3] = {x0 - nx0 * 0.14f, y0 + 0.025f, z0 - nz0 * 0.14f};
        float c1[3] = {x0 + nx0 * 0.14f, y0 + 0.025f, z0 + nz0 * 0.14f};
        float c2[3] = {x1 + nx1 * 0.14f, y1 + 0.025f, z1 + nz1 * 0.14f};
        float c3[3] = {x1 - nx1 * 0.14f, y1 + 0.025f, z1 - nz1 * 0.14f};
        AddIntQuad(buffer, c0, c1, c2, c3, nUp, 2, 0.96f, 0.78f, 0.14f, 0.45f, 0.04f);
    }

    // Concrete fascias, webs, and box-girder soffit
    float nL[3] = {-nx0, 0.0f, -nz0};
    float nR[3] = { nx0, 0.0f,  nz0};
    float nDown[3] = {0.0f, -1.0f, 0.0f};
    AddIntQuad(buffer, tl0, tl1, fl1, fl0, nL, 1, 0.68f, 0.71f, 0.75f, 0.68f, 0.04f);
    AddIntQuad(buffer, tr0, fr0, fr1, tr1, nR, 1, 0.68f, 0.71f, 0.75f, 0.68f, 0.04f);
    AddIntQuad(buffer, fl0, fl1, bl1, bl0, nL, 1, 0.56f, 0.59f, 0.64f, 0.70f, 0.04f);
    AddIntQuad(buffer, fr0, br0, br1, fr1, nR, 1, 0.56f, 0.59f, 0.64f, 0.70f, 0.04f);
    AddIntQuad(buffer, bl0, bl1, br1, br0, nDown, 1, 0.46f, 0.49f, 0.54f, 0.72f, 0.04f);

    if (withBarriers) {
        float bH = 0.88f, bW = 0.42f;
        for (int side : {-1, 1}) {
            float sign = static_cast<float>(side);
            float outOff = sign * (halfW - 0.05f);
            float inOff  = sign * (halfW - bW - 0.05f);
            float yB0 = (side < 0 ? yL0 : yR0);
            float yB1 = (side < 0 ? yL1 : yR1);

            float outBot0[3] = {x0 + nx0 * outOff, yB0,      z0 + nz0 * outOff};
            float outTop0[3] = {x0 + nx0 * outOff, yB0 + bH, z0 + nz0 * outOff};
            float outTop1[3] = {x1 + nx1 * outOff, yB1 + bH, z1 + nz1 * outOff};
            float outBot1[3] = {x1 + nx1 * outOff, yB1,      z1 + nz1 * outOff};

            float inBot0[3]  = {x0 + nx0 * inOff,  yB0,      z0 + nz0 * inOff};
            float inTop0[3]  = {x0 + nx0 * inOff,  yB0 + bH, z0 + nz0 * inOff};
            float inTop1[3]  = {x1 + nx1 * inOff,  yB1 + bH, z1 + nz1 * inOff};
            float inBot1[3]  = {x1 + nx1 * inOff,  yB1,      z1 + nz1 * inOff};

            if (side > 0) {
                AddIntQuad(buffer, outTop0, outBot0, outBot1, outTop1, nR, 1, 0.78f, 0.80f, 0.84f, 0.65f, 0.04f);
                AddIntQuad(buffer, inTop0, inTop1, outTop1, outTop0, nUp, 1, 0.86f, 0.88f, 0.90f, 0.62f, 0.04f);
                AddIntQuad(buffer, inTop0, inBot0, inBot1, inTop1, nL, 1, 0.82f, 0.84f, 0.87f, 0.64f, 0.04f);
            } else {
                AddIntQuad(buffer, outTop0, outTop1, outBot1, outBot0, nL, 1, 0.78f, 0.80f, 0.84f, 0.65f, 0.04f);
                AddIntQuad(buffer, outTop0, inTop0, inTop1, outTop1, nUp, 1, 0.86f, 0.88f, 0.90f, 0.62f, 0.04f);
                AddIntQuad(buffer, inTop0, inBot0, inBot1, inTop1, nR, 1, 0.82f, 0.84f, 0.87f, 0.64f, 0.04f);
            }
        }
    }
}

void InterchangeGen::SampleRampCenterline(
    float t,
    const InterchangeParams& params,
    float& outX, float& outY, float& outZ,
    float& outTx, float& outTy, float& outTz,
    float& outBankRad
) {
    if (params.type == InterchangeType::ModernRoundabout) {
        float angle = t * 2.0f * PI;
        float r = params.centralIslandRadiusM + params.rampWidthM * 0.55f;
        outX = r * std::cos(angle);
        outY = 0.45f;
        outZ = r * std::sin(angle);
        outTx = -std::sin(angle);
        outTy = 0.0f;
        outTz = std::cos(angle);
        outBankRad = -0.02f;
        return;
    }

    float totalAngleRad = params.totalArcAngleDeg * DEG2RAD;
    float r = params.loopRadiusM;
    float angle = t * totalAngleRad;

    outX = r * (1.0f - std::cos(angle)) - r;
    outZ = r * std::sin(angle);
    // Smooth vertical S-curve climbing from ground highway to elevated flyover grade
    outY = 0.5f + params.gradeElevationDeltaM * (3.0f * t * t - 2.0f * t * t * t);

    outTx = std::sin(angle);
    outTy = (params.gradeElevationDeltaM * 6.0f * t * (1.0f - t)) / std::max(1.0f, r * totalAngleRad);
    outTz = std::cos(angle);

    float tLen = std::sqrt(outTx * outTx + outTy * outTy + outTz * outTz);
    if (tLen > 1e-6f) {
        outTx /= tLen;
        outTy /= tLen;
        outTz /= tLen;
    }
    outBankRad = params.superelevationBankingDeg * DEG2RAD * std::sin(t * PI);
}

InterchangeMeshBuffer InterchangeGen::GenerateInterchangeMesh(const InterchangeParams& params) {
    InterchangeMeshBuffer buffer;

    float rLoop = (params.type == InterchangeType::ModernRoundabout)
        ? params.centralIslandRadiusM
        : params.loopRadiusM;
    float rampHalfW = params.rampWidthM * 0.5f;
    float deckDepth = 1.45f;
    float gradeH = params.gradeElevationDeltaM;

    if (params.type == InterchangeType::ModernRoundabout) {
        // 1. Continuous Circular Asphalt Carriageway Loop
        const int segs = 36;
        for (int i = 0; i < segs; ++i) {
            float t0 = static_cast<float>(i) / segs;
            float t1 = static_cast<float>(i + 1) / segs;
            float x0, y0, z0, tx0, ty0, tz0, b0;
            float x1, y1, z1, tx1, ty1, tz1, b1;
            SampleRampCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0, b0);
            SampleRampCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1, b1);
            float nx0 = tz0, nz0 = -tx0;
            float nx1 = tz1, nz1 = -tx1;
            ExtrudeSolidRoadSegment(buffer, x0, y0, z0, nx0, nz0, b0,
                                    x1, y1, z1, nx1, nz1, b1,
                                    rampHalfW, 0.55f, false, false);
        }

        // 2. Mountable Brick-Red Stamped Concrete Inner Truck Apron & Central Monument Island
        float innerEdge = params.centralIslandRadiusM + 0.05f;
        float apronInner = std::max(4.0f, innerEdge - params.truckApronWidthM);
        float nUp[3] = {0.0f, 1.0f, 0.0f};
        for (int i = 0; i < segs; ++i) {
            float a0 = (static_cast<float>(i) / segs) * 2.0f * PI;
            float a1 = (static_cast<float>(i + 1) / segs) * 2.0f * PI;
            float c0 = std::cos(a0), s0 = std::sin(a0);
            float c1 = std::cos(a1), s1 = std::sin(a1);

            float p00[3] = {apronInner * c0, 0.56f, apronInner * s0};
            float p01[3] = {innerEdge * c0,  0.52f, innerEdge * s0};
            float p11[3] = {innerEdge * c1,  0.52f, innerEdge * s1};
            float p10[3] = {apronInner * c1, 0.56f, apronInner * s1};
            AddIntQuad(buffer, p00, p01, p11, p10, nUp, 3, 0.72f, 0.28f, 0.22f, 0.74f, 0.02f);
        }

        // Central Concrete Planter Island Disc + Obelisk Feature
        Primitives::AddCylinder<InterchangeMeshBuffer, InterchangeVertex>(
            buffer, 0.0f, 0.0f, 0.0f, 0.0f, 0.95f, 0.0f,
            apronInner, apronInner * 0.96f, 24, 1, 0.34f, 0.56f, 0.32f, 1.0f, 0.85f, 0.02f, 0.0f
        );

        // 3. Radial Approach Carriageways + Triangular Channelized Splitter Islands
        int legs = std::clamp<int>(params.approachLegsCount, 3, 6);
        float legLen = params.centralIslandRadiusM + 36.0f;
        for (int leg = 0; leg < legs; ++leg) {
            float ang = (static_cast<float>(leg) / legs) * 2.0f * PI;
            float dx = std::cos(ang), dz = std::sin(ang);
            float lx = -dz, lz = dx;
            float rStart = params.centralIslandRadiusM + params.rampWidthM * 0.8f;

            ExtrudeSolidRoadSegment(
                buffer,
                dx * rStart, 0.45f, dz * rStart, lx, lz, 0.0f,
                dx * legLen, 0.45f, dz * legLen, lx, lz, 0.0f,
                rampHalfW * 1.15f, 0.55f, false, true
            );

            // Concrete triangular splitter island nose
            AddIntBox(buffer,
                      dx * (rStart + 6.0f), 0.70f, dz * (rStart + 6.0f),
                      1.8f, 0.45f, 9.5f, lx, lz, dx, dz,
                      3, 0.88f, 0.86f, 0.24f, 0.60f, 0.05f);
        }
    } else if (params.type == InterchangeType::DiamondSlipRamp ||
               params.type == InterchangeType::SinglePointUrbanInterchange) {
        // Mainline Overpass Viaduct along Z (Y = gradeH + 0.5f) with Intermediate Piers + Perpendicular Arterial (Y=0.5f)
        float mainLen = std::max(90.0f, rLoop * 2.2f);
        float crossLen = mainLen * 0.85f;
        const int segs = 20;
        for (int i = 0; i < segs; ++i) {
            float t0 = -0.5f + static_cast<float>(i) / segs;
            float t1 = -0.5f + static_cast<float>(i + 1) / segs;
            float z0 = t0 * mainLen, z1 = t1 * mainLen;
            // Elevated Overpass Deck along Z
            ExtrudeSolidRoadSegment(buffer, 0.0f, gradeH + 0.5f, z0, 1.0f, 0.0f, 0.0f,
                                    0.0f, gradeH + 0.5f, z1, 1.0f, 0.0f, 0.0f,
                                    rampHalfW * 1.25f, deckDepth, params.hasConcreteJerseyBarriers, true);
            if (i == 4 || i == 9 || i == 15) {
                AddInterchangePier(buffer, 0.0f, gradeH + 0.5f, z0, 1.0f, 0.0f, 0.0f, 1.0f,
                                   rampHalfW * 1.15f, deckDepth);
            }

            // Surface Crossroad Arterial along X at Y=0.5f
            float x0 = t0 * crossLen, x1 = t1 * crossLen;
            ExtrudeSolidRoadSegment(buffer, x0, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
                                    x1, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
                                    rampHalfW * 1.2f, 0.55f, false, true);
        }

        // 4 Sweeping Diamond / SPUI Slip Ramps connecting elevated Z mainline down to X arterial
        for (float sx : {-1.0f, 1.0f}) {
            for (float sz : {-1.0f, 1.0f}) {
                const int rSegs = 12;
                for (int i = 0; i < rSegs; ++i) {
                    auto sampleSlip = [&](float s, float& ox, float& oy, float& oz, float& onx, float& onz) {
                        float xStart = sx * (rampHalfW * 1.6f);
                        float zStart = sz * (mainLen * 0.44f);
                        float xEnd = (params.type == InterchangeType::SinglePointUrbanInterchange)
                            ? sx * (rampHalfW * 2.2f)
                            : sx * (crossLen * 0.36f);
                        float zEnd = sz * (rampHalfW * 1.5f);
                        float curvePush = (params.type == InterchangeType::SinglePointUrbanInterchange) ? -0.32f : 0.15f;
                        ox = xStart + (xEnd - xStart) * (s + curvePush * s * (1.0f - s));
                        oz = zStart + (zEnd - zStart) * s;
                        oy = 0.5f + gradeH * (1.0f - (3.0f * s * s - 2.0f * s * s * s));

                        float dx = (xEnd - xStart);
                        float dz = (zEnd - zStart);
                        float dLen = std::sqrt(dx * dx + dz * dz);
                        onx = dz / std::max(1.0f, dLen);
                        onz = -dx / std::max(1.0f, dLen);
                    };
                    float s0 = static_cast<float>(i) / rSegs;
                    float s1 = static_cast<float>(i + 1) / rSegs;
                    float x0, y0, z0, nx0, nz0;
                    float x1, y1, z1, nx1, nz1;
                    sampleSlip(s0, x0, y0, z0, nx0, nz0);
                    sampleSlip(s1, x1, y1, z1, nx1, nz1);
                    ExtrudeSolidRoadSegment(buffer, x0, y0, z0, nx0, nz0, 0.0f,
                                            x1, y1, z1, nx1, nz1, 0.0f,
                                            rampHalfW * 0.55f, 0.95f, params.hasConcreteJerseyBarriers, false);
                    if (i == 4 && y0 > 2.2f) {
                        AddInterchangePier(buffer, x0, y0, z0, nx0, nz0, -nz0, nx0, rampHalfW * 0.5f, 0.95f);
                    }
                }
            }
        }
    } else if (params.type == InterchangeType::DivergingDiamondInterchange) {
        // Diverging Diamond Criss-Cross Overpass Carriageways + Loop Connectors
        float mainLen = std::max(100.0f, rLoop * 2.2f);
        const int segs = 28;
        for (int laneSign : {-1, 1}) {
            for (int i = 0; i < segs; ++i) {
                auto sampleDDI = [&](float t, float& ox, float& oy, float& oz, float& onx, float& onz) {
                    float z = (t - 0.5f) * mainLen;
                    // Smooth braided crossover around z = +-18m switching side across center bridge
                    float crossWave = std::sin(t * 2.0f * PI - PI * 0.5f);
                    ox = laneSign * rampHalfW * (1.25f + 0.95f * crossWave);
                    oy = 0.5f + gradeH * (std::exp(-t * t * 0.0f) * 1.0f); // Bridge grade elevation
                    // Ramp down on extreme approaches t < 0.25 or t > 0.75
                    float elevFactor = 1.0f;
                    if (t < 0.22f) elevFactor = t / 0.22f;
                    else if (t > 0.78f) elevFactor = (1.0f - t) / 0.22f;
                    oy = 0.5f + gradeH * elevFactor;
                    oz = z;

                    float dxDt = laneSign * rampHalfW * 0.95f * 2.0f * PI * std::cos(t * 2.0f * PI - PI * 0.5f);
                    float dzDt = mainLen;
                    float l = std::sqrt(dxDt * dxDt + dzDt * dzDt);
                    onx = dzDt / l;
                    onz = -dxDt / l;
                };
                float t0 = static_cast<float>(i) / segs;
                float t1 = static_cast<float>(i + 1) / segs;
                float x0, y0, z0, nx0, nz0, x1, y1, z1, nx1, nz1;
                sampleDDI(t0, x0, y0, z0, nx0, nz0);
                sampleDDI(t1, x1, y1, z1, nx1, nz1);
                ExtrudeSolidRoadSegment(buffer, x0, y0, z0, nx0, nz0, 0.0f,
                                        x1, y1, z1, nx1, nz1, 0.0f,
                                        rampHalfW * 0.72f, 1.15f, params.hasConcreteJerseyBarriers, false);
                if (i % 6 == 3 && y0 > 2.2f) {
                    AddInterchangePier(buffer, x0, y0, z0, nx0, nz0, -nz0, nx0, rampHalfW * 0.65f, 1.15f);
                }
            }
        }
        // Lower perpendicular freeway underpass along X at Y=0.5f
        for (int i = 0; i < 16; ++i) {
            float x0 = -mainLen * 0.45f + (static_cast<float>(i) / 16.0f) * mainLen * 0.9f;
            float x1 = -mainLen * 0.45f + (static_cast<float>(i + 1) / 16.0f) * mainLen * 0.9f;
            ExtrudeSolidRoadSegment(buffer, x0, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
                                    x1, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
                                    rampHalfW * 1.4f, 0.55f, false, true);
        }
    } else {
        // CloverleafLoop (0) or DirectionalFlyover (1):
        // Sweeping elevated curve with grounded concrete piers + intersecting lower arterial highway!
        const int segments = 44;
        for (int i = 0; i < segments; ++i) {
            float t0 = static_cast<float>(i) / segments;
            float t1 = static_cast<float>(i + 1) / segments;

            float x0, y0, z0, tx0, ty0, tz0, b0;
            float x1, y1, z1, tx1, ty1, tz1, b1;
            SampleRampCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0, b0);
            SampleRampCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1, b1);

            float nx0 = tz0, nz0 = -tx0;
            float nx1 = tz1, nz1 = -tx1;
            ExtrudeSolidRoadSegment(buffer, x0, y0, z0, nx0, nz0, b0,
                                    x1, y1, z1, nx1, nz1, b1,
                                    rampHalfW, deckDepth, params.hasConcreteJerseyBarriers, true);

            if (i % 7 == 4 && y0 > 2.2f) {
                AddInterchangePier(buffer, x0, y0, z0, nx0, nz0, tx0, tz0, rampHalfW, deckDepth);
            }
        }

        // Ground-Level Mainline Highway at Y=0.5f underneath the loop / flyover
        float corLen = rLoop * 2.2f;
        for (int i = 0; i < 16; ++i) {
            float z0 = -corLen * 0.5f + (static_cast<float>(i) / 16.0f) * corLen;
            float z1 = -corLen * 0.5f + (static_cast<float>(i + 1) / 16.0f) * corLen;
            ExtrudeSolidRoadSegment(buffer, -rLoop * 0.6f, 0.5f, z0, 1.0f, 0.0f, 0.0f,
                                    -rLoop * 0.6f, 0.5f, z1, 1.0f, 0.0f, 0.0f,
                                    rampHalfW * 1.25f, 0.55f, false, true);
        }

        // Overhead Steel Guide Sign Gantry along approach
        if (params.hasOverheadGantrySignage) {
            float gx = -rLoop * 0.6f;
            float gz = -corLen * 0.32f;
            float gW = rampHalfW * 2.9f;
            Primitives::AddCylinder<InterchangeMeshBuffer, InterchangeVertex>(
                buffer, gx - gW * 0.5f, 0.0f, gz, gx - gW * 0.5f, 7.2f, gz,
                0.28f, 0.25f, 10, 4, 0.72f, 0.76f, 0.80f, 1.0f, 0.30f, 0.85f, 0.0f
            );
            Primitives::AddCylinder<InterchangeMeshBuffer, InterchangeVertex>(
                buffer, gx + gW * 0.5f, 0.0f, gz, gx + gW * 0.5f, 7.2f, gz,
                0.28f, 0.25f, 10, 4, 0.72f, 0.76f, 0.80f, 1.0f, 0.30f, 0.85f, 0.0f
            );
            Primitives::AddBeam<InterchangeMeshBuffer, InterchangeVertex>(
                buffer, gx - gW * 0.5f, 6.8f, gz, gx + gW * 0.5f, 6.8f, gz,
                0.35f, 0.45f, 4, 0.72f, 0.76f, 0.80f, 1.0f, 0.30f, 0.85f, 0.0f
            );
            // Highway green directional signboard
            AddIntBox(buffer, gx, 6.6f, gz + 0.28f, gW * 0.55f, 2.2f, 0.16f,
                      1.0f, 0.0f, 0.0f, 1.0f, 4, 0.08f, 0.48f, 0.26f, 0.35f, 0.20f);
        }
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);

    float vMps = params.designSpeedKmH / 3.6f;
    buffer.maxLateralCentrifugalG = (vMps * vMps) / (std::max(15.0f, params.loopRadiusM) * 9.81f);

    return buffer;
}

} // namespace Road
} // namespace ApexAssets
