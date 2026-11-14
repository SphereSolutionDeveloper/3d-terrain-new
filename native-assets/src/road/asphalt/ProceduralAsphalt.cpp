// ==============================================================================
// Modular Road Component: Expressway & Multi-Lane Highway 3D Mesh Generator
// Path: native-assets/src/road/asphalt/ProceduralAsphalt.cpp
// Standards: AASHTO Green Book / Eurocode EN 13108 Multilane Highway Geometry
// ==============================================================================
#include "ProceduralAsphalt.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float DEG2RAD = PI / 180.0f;

static void AddExpQuad(
    ExpresswayMeshBuffer& buffer,
    const float c0[3], const float c1[3], const float c2[3], const float c3[3],
    const float norm[3], uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddQuad<ExpresswayMeshBuffer, ExpresswayVertex>(
        buffer, c0, c1, c2, c3, norm, matId, r, g, b, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, rough, metal, emis
    );
}

static void AddExpBox(
    ExpresswayMeshBuffer& buffer,
    float cx, float cy, float cz,
    float dimN, float dimY, float dimT,
    float nx, float nz, float tx, float tz,
    uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddOrientedBox<ExpresswayMeshBuffer, ExpresswayVertex>(
        buffer, cx, cy, cz, dimN, dimY, dimT, nx, nz, tx, tz,
        matId, r, g, b, 1.0f, rough, metal, emis
    );
}

void ProceduralAsphalt::SampleExpresswayCenterline(
    float t,
    const AsphaltParams& params,
    float& outX, float& outY, float& outZ,
    float& outTx, float& outTy, float& outTz,
    float& outBankRad
) {
    float length = params.sectionLengthM;
    float baseElev = (params.type == ExpresswayType::ElevatedUrbanViaductDeck) ? 7.5f : 0.65f;

    if (std::abs(params.curveBendAngleDeg) > 0.01f) {
        float totalBendRad = params.curveBendAngleDeg * DEG2RAD;
        float radius = length / totalBendRad;
        float angle = (t - 0.5f) * totalBendRad;

        outX = radius * (1.0f - std::cos(angle));
        outZ = (t - 0.5f) * length;
        outTx = std::sin(angle);
        outTz = std::cos(angle);
        outTy = 0.0f;

        float bankSign = (params.curveBendAngleDeg > 0.0f) ? 1.0f : -1.0f;
        outBankRad = bankSign * params.superelevationDeg * DEG2RAD * std::sin(t * PI);
    } else {
        outX = 0.0f;
        outZ = (t - 0.5f) * length;
        outTx = 0.0f;
        outTy = 0.0f;
        outTz = 1.0f;
        outBankRad = 0.0f;
    }

    float arch = (params.type == ExpresswayType::ElevatedUrbanViaductDeck) ? 0.55f * std::sin(t * PI) : 0.0f;
    outY = baseElev + arch;

    float tLen = std::sqrt(outTx * outTx + outTy * outTy + outTz * outTz);
    if (tLen > 1e-6f) {
        outTx /= tLen;
        outTy /= tLen;
        outTz /= tLen;
    }
}

static void AddConcreteFBarrierSegment(
    ExpresswayMeshBuffer& buffer,
    float cx0, float cy0, float cz0, float nx0, float nz0,
    float cx1, float cy1, float cz1, float nx1, float nz1,
    float latOffset, float barrierH, float halfTopW, float halfBotW
) {
    float x0 = cx0 + nx0 * latOffset, z0 = cz0 + nz0 * latOffset;
    float x1 = cx1 + nx1 * latOffset, z1 = cz1 + nz1 * latOffset;

    float bl0[3] = {x0 - nx0 * halfBotW, cy0, z0 - nz0 * halfBotW};
    float br0[3] = {x0 + nx0 * halfBotW, cy0, z0 + nz0 * halfBotW};
    float bl1[3] = {x1 - nx1 * halfBotW, cy1, z1 - nz1 * halfBotW};
    float br1[3] = {x1 + nx1 * halfBotW, cy1, z1 + nz1 * halfBotW};

    float tl0[3] = {x0 - nx0 * halfTopW, cy0 + barrierH, z0 - nz0 * halfTopW};
    float tr0[3] = {x0 + nx0 * halfTopW, cy0 + barrierH, z0 + nz0 * halfTopW};
    float tl1[3] = {x1 - nx1 * halfTopW, cy1 + barrierH, z1 - nz1 * halfTopW};
    float tr1[3] = {x1 + nx1 * halfTopW, cy1 + barrierH, z1 + nz1 * halfTopW};

    float nUp[3] = {0.0f, 1.0f, 0.0f};
    float nL[3] = {-nx0, 0.25f, -nz0};
    float nR[3] = { nx0, 0.25f,  nz0};

    AddExpQuad(buffer, tl0, tr0, tr1, tl1, nUp, 1, 0.86f, 0.88f, 0.90f, 0.60f, 0.04f);
    AddExpQuad(buffer, tl0, tl1, bl1, bl0, nL,  1, 0.74f, 0.77f, 0.81f, 0.65f, 0.04f);
    AddExpQuad(buffer, tr0, br0, br1, tr1, nR,  1, 0.74f, 0.77f, 0.81f, 0.65f, 0.04f);
}

ExpresswayMeshBuffer ProceduralAsphalt::GenerateExpresswayMesh(const AsphaltParams& params) {
    ExpresswayMeshBuffer buffer;

    uint32_t lanesPerDir = std::clamp<uint32_t>(params.laneCountPerDir, 2u, 4u);
    float medianHalfW = 1.25f;
    float shoulderW = 2.4f;
    float carriageW = lanesPerDir * params.laneWidthM;
    float baseHalfW = medianHalfW + carriageW + shoulderW;
    bool isElevated = (params.type == ExpresswayType::ElevatedUrbanViaductDeck);
    float slabDepth = isElevated ? 1.85f : 0.65f;
    float botHalfW = isElevated ? baseHalfW * 0.68f : baseHalfW + 1.4f;

    const int segments = 32;
    for (int i = 0; i < segments; ++i) {
        float t0 = static_cast<float>(i) / segments;
        float t1 = static_cast<float>(i + 1) / segments;

        float x0, y0, z0, tx0, ty0, tz0, bank0;
        float x1, y1, z1, tx1, ty1, tz1, bank1;
        SampleExpresswayCenterline(t0, params, x0, y0, z0, tx0, ty0, tz0, bank0);
        SampleExpresswayCenterline(t1, params, x1, y1, z1, tx1, ty1, tz1, bank1);

        float nx0 = tz0, nz0 = -tx0;
        float nx1 = tz1, nz1 = -tx1;

        float yL0 = y0 - std::sin(bank0) * baseHalfW;
        float yR0 = y0 + std::sin(bank0) * baseHalfW;
        float yL1 = y1 - std::sin(bank1) * baseHalfW;
        float yR1 = y1 + std::sin(bank1) * baseHalfW;

        // 1. Solid Roadway Deck / Graded Compacted Embankment Section
        float tl0[3] = {x0 - nx0 * baseHalfW, yL0, z0 - nz0 * baseHalfW};
        float tr0[3] = {x0 + nx0 * baseHalfW, yR0, z0 + nz0 * baseHalfW};
        float tl1[3] = {x1 - nx1 * baseHalfW, yL1, z1 - nz1 * baseHalfW};
        float tr1[3] = {x1 + nx1 * baseHalfW, yR1, z1 + nz1 * baseHalfW};

        float bl0[3] = {x0 - nx0 * botHalfW, std::max(0.0f, y0 - slabDepth), z0 - nz0 * botHalfW};
        float br0[3] = {x0 + nx0 * botHalfW, std::max(0.0f, y0 - slabDepth), z0 + nz0 * botHalfW};
        float bl1[3] = {x1 - nx1 * botHalfW, std::max(0.0f, y1 - slabDepth), z1 - nz1 * botHalfW};
        float br1[3] = {x1 + nx1 * botHalfW, std::max(0.0f, y1 - slabDepth), z1 + nz1 * botHalfW};

        float nUp[3] = {0.0f, 1.0f, 0.0f};
        float nL[3] = {-nx0, 0.15f, -nz0};
        float nR[3] = { nx0, 0.15f,  nz0};
        float nDown[3] = {0.0f, -1.0f, 0.0f};

        // Main Wearing Course Asphalt
        AddExpQuad(buffer, tl0, tr0, tr1, tl1, nUp, 0, 0.20f, 0.22f, 0.25f, 0.85f, 0.02f);

        // Sloped Side Embankment / Box-Girder Facia & Soffit
        float sideR = isElevated ? 0.66f : 0.48f;
        float sideG = isElevated ? 0.69f : 0.44f;
        float sideB = isElevated ? 0.73f : 0.36f;
        AddExpQuad(buffer, tl0, tl1, bl1, bl0, nL, 1, sideR, sideG, sideB, 0.78f, 0.03f);
        AddExpQuad(buffer, tr0, br0, br1, tr1, nR, 1, sideR, sideG, sideB, 0.78f, 0.03f);
        AddExpQuad(buffer, bl0, bl1, br1, br0, nDown, 1, 0.46f, 0.49f, 0.54f, 0.74f, 0.03f);

        // 2. Lane Markings (Inner Yellow Median Edge Lines, White Outer Fog Lines & Dashed Lane Dividers)
        for (int dir : {-1, 1}) {
            float innerEdge = dir * medianHalfW;
            float outerEdge = dir * (medianHalfW + carriageW);

            // Inner yellow median line
            float yiL0 = y0 + std::sin(bank0) * innerEdge + 0.02f;
            float yiL1 = y1 + std::sin(bank1) * innerEdge + 0.02f;
            float yl0[3] = {x0 + nx0 * (innerEdge - 0.10f), yiL0, z0 + nz0 * (innerEdge - 0.10f)};
            float yr0[3] = {x0 + nx0 * (innerEdge + 0.10f), yiL0, z0 + nz0 * (innerEdge + 0.10f)};
            float yr1[3] = {x1 + nx1 * (innerEdge + 0.10f), yiL1, z1 + nz1 * (innerEdge + 0.10f)};
            float yl1[3] = {x1 + nx1 * (innerEdge - 0.10f), yiL1, z1 + nz1 * (innerEdge - 0.10f)};
            AddExpQuad(buffer, yl0, yr0, yr1, yl1, nUp, 3, 0.96f, 0.78f, 0.14f, 0.45f, 0.05f);

            // Outer white shoulder edge fog line
            float yo0 = y0 + std::sin(bank0) * outerEdge + 0.02f;
            float yo1 = y1 + std::sin(bank1) * outerEdge + 0.02f;
            float fl0[3] = {x0 + nx0 * (outerEdge - 0.11f), yo0, z0 + nz0 * (outerEdge - 0.11f)};
            float fr0[3] = {x0 + nx0 * (outerEdge + 0.11f), yo0, z0 + nz0 * (outerEdge + 0.11f)};
            float fr1[3] = {x1 + nx1 * (outerEdge + 0.11f), yo1, z1 + nz1 * (outerEdge + 0.11f)};
            float fl1[3] = {x1 + nx1 * (outerEdge - 0.11f), yo1, z1 + nz1 * (outerEdge - 0.11f)};
            AddExpQuad(buffer, fl0, fr0, fr1, fl1, nUp, 3, 0.94f, 0.95f, 0.97f, 0.45f, 0.05f);

            // Dashed lane separators on alternate segments
            if (i % 2 == 0) {
                for (uint32_t l = 1; l < lanesPerDir; ++l) {
                    float ld = dir * (medianHalfW + l * params.laneWidthM);
                    float yd0 = y0 + std::sin(bank0) * ld + 0.02f;
                    float yd1 = y1 + std::sin(bank1) * ld + 0.02f;
                    float dl0[3] = {x0 + nx0 * (ld - 0.08f), yd0, z0 + nz0 * (ld - 0.08f)};
                    float dr0[3] = {x0 + nx0 * (ld + 0.08f), yd0, z0 + nz0 * (ld + 0.08f)};
                    float dr1[3] = {x1 + nx1 * (ld + 0.08f), yd1, z1 + nz1 * (ld + 0.08f)};
                    float dl1[3] = {x1 + nx1 * (ld - 0.08f), yd1, z1 + nz1 * (ld - 0.08f)};
                    AddExpQuad(buffer, dl0, dr0, dr1, dl1, nUp, 3, 0.94f, 0.95f, 0.97f, 0.45f, 0.05f);
                }
            }

            // Dedicated Emerald EV/HOV Managed Lane Strip (SmartManagedEVCorridor)
            if (params.type == ExpresswayType::SmartManagedEVCorridor) {
                float evCenter = dir * (medianHalfW + params.laneWidthM * 0.5f);
                float ye0 = y0 + std::sin(bank0) * evCenter + 0.015f;
                float ye1 = y1 + std::sin(bank1) * evCenter + 0.015f;
                float evW = params.laneWidthM * 0.40f;
                float el0[3] = {x0 + nx0 * (evCenter - evW), ye0, z0 + nz0 * (evCenter - evW)};
                float er0[3] = {x0 + nx0 * (evCenter + evW), ye0, z0 + nz0 * (evCenter + evW)};
                float er1[3] = {x1 + nx1 * (evCenter + evW), ye1, z1 + nz1 * (evCenter + evW)};
                float el1[3] = {x1 + nx1 * (evCenter - evW), ye1, z1 + nz1 * (evCenter - evW)};
                AddExpQuad(buffer, el0, er0, er1, el1, nUp, 3, 0.08f, 0.46f, 0.34f, 0.72f, 0.05f);

                // Glowing Inductive Charging Coils
                if (i % 3 == 0) {
                    float ce0[3] = {x0 + nx0 * (evCenter - 0.35f), ye0 + 0.015f, z0 + nz0 * (evCenter - 0.35f)};
                    float ce1[3] = {x0 + nx0 * (evCenter + 0.35f), ye0 + 0.015f, z0 + nz0 * (evCenter + 0.35f)};
                    float ce2[3] = {x1 + nx1 * (evCenter + 0.35f), ye1 + 0.015f, z1 + nz1 * (evCenter + 0.35f)};
                    float ce3[3] = {x1 + nx1 * (evCenter - 0.35f), ye1 + 0.015f, z1 + nz1 * (evCenter - 0.35f)};
                    AddExpQuad(buffer, ce0, ce1, ce2, ce3, nUp, 4, 0.10f, 0.90f, 0.75f, 0.20f, 0.10f, 2.2f);
                }
            }
        }

        // 3. Diverging Deceleration Slip Lane for ExpresswayTollMergeTaper
        if (params.type == ExpresswayType::ExpresswayTollMergeTaper && t0 > 0.25f) {
            float flareFrac = (t0 - 0.25f) / 0.75f;
            float flareFrac1 = (t1 - 0.25f) / 0.75f;
            float slipW0 = flareFrac * (params.laneWidthM * 2.2f);
            float slipW1 = flareFrac1 * (params.laneWidthM * 2.2f);
            float sl0[3] = {tr0[0], yR0, tr0[2]};
            float sr0[3] = {tr0[0] + nx0 * slipW0, yR0, tr0[2] + nz0 * slipW0};
            float sr1[3] = {tr1[0] + nx1 * slipW1, yR1, tr1[2] + nz1 * slipW1};
            float sl1[3] = {tr1[0], yR1, tr1[2]};
            AddExpQuad(buffer, sl0, sr0, sr1, sl1, nUp, 0, 0.18f, 0.20f, 0.23f, 0.85f, 0.02f);
        }

        // 4. Central Double-Sided Concrete New Jersey Median Barrier
        if (params.hasMedianBarrier) {
            AddConcreteFBarrierSegment(buffer, x0, y0, z0, nx0, nz0, x1, y1, z1, nx1, nz1,
                                       0.0f, 1.05f, 0.14f, 0.34f);
        }

        // 5. Outer W-Beam Guardrails (or Parapet / Acoustic Barrier on Viaduct / Mountain Rock Cut)
        if (isElevated) {
            AddConcreteFBarrierSegment(buffer, x0, yL0, z0, nx0, nz0, x1, yL1, z1, nx1, nz1,
                                       -(baseHalfW - 0.25f), 1.05f, 0.18f, 0.24f);
            AddConcreteFBarrierSegment(buffer, x0, yR0, z0, nx0, nz0, x1, yR1, z1, nx1, nz1,
                                       (baseHalfW - 0.25f), 1.05f, 0.18f, 0.24f);
            // Grounded cylindrical viaduct piers every 6 segments
            if (i % 6 == 3) {
                float seatY = y0 - slabDepth;
                AddExpBox(buffer, x0, 0.65f, z0, baseHalfW * 1.15f, 1.3f, 4.2f, nx0, nz0, tx0, tz0,
                          1, 0.48f, 0.51f, 0.55f, 0.72f, 0.04f);
                for (float pSide : {-baseHalfW * 0.40f, baseHalfW * 0.40f}) {
                    float px = x0 + nx0 * pSide, pz = z0 + nz0 * pSide;
                    Primitives::AddCylinder<ExpresswayMeshBuffer, ExpresswayVertex>(
                        buffer, px, 1.25f, pz, px, seatY, pz,
                        0.95f, 0.82f, 12, 1, 0.66f, 0.70f, 0.74f, 1.0f, 0.65f, 0.04f, 0.0f
                    );
                }
                AddExpBox(buffer, x0, seatY - 0.6f, z0, baseHalfW * 1.30f, 1.2f, 2.8f, nx0, nz0, tx0, tz0,
                          1, 0.62f, 0.65f, 0.69f, 0.68f, 0.04f);
            }
        } else {
            // Galvanized steel W-beam rails & posts on outside shoulders
            for (int side : {-1, 1}) {
                float edgeOff = side * (baseHalfW - 0.35f);
                float yg0 = y0 + std::sin(bank0) * edgeOff;
                float yg1 = y1 + std::sin(bank1) * edgeOff;
                float p0x = x0 + nx0 * edgeOff, p0z = z0 + nz0 * edgeOff;
                float p1x = x1 + nx1 * edgeOff, p1z = z1 + nz1 * edgeOff;
                Primitives::AddBeam<ExpresswayMeshBuffer, ExpresswayVertex>(
                    buffer, p0x, yg0 + 0.68f, p0z, p1x, yg1 + 0.68f, p1z,
                    0.10f, 0.30f, 2, 0.76f, 0.80f, 0.85f, 1.0f, 0.28f, 0.88f, 0.0f
                );
                AddExpBox(buffer, p0x, yg0 + 0.42f, p0z, 0.12f, 0.84f, 0.12f, nx0, nz0, tx0, tz0,
                          2, 0.48f, 0.52f, 0.56f, 0.36f, 0.85f);
            }
        }

        // Battered Uphill Rock Slope Cut for MountainSwitchbackCut
        if (params.type == ExpresswayType::MountainSwitchbackCut) {
            float cutH = 6.2f;
            float c0x = x0 - nx0 * (baseHalfW + 0.4f), c0z = z0 - nz0 * (baseHalfW + 0.4f);
            float c1x = x1 - nx1 * (baseHalfW + 0.4f), c1z = z1 - nz1 * (baseHalfW + 0.4f);
            float t0x = x0 - nx0 * (baseHalfW + 3.2f), t0z = z0 - nz0 * (baseHalfW + 3.2f);
            float t1x = x1 - nx1 * (baseHalfW + 3.2f), t1z = z1 - nz1 * (baseHalfW + 3.2f);
            float rb0[3] = {c0x, yL0,        c0z};
            float rb1[3] = {c1x, yL1,        c1z};
            float rt0[3] = {t0x, yL0 + cutH, t0z};
            float rt1[3] = {t1x, yL1 + cutH, t1z};
            AddExpQuad(buffer, rt0, rt1, rb1, rb0, nR, 3, 0.44f, 0.41f, 0.36f, 0.92f, 0.02f);
        }

        // 6. Twin-Arm Highway Cobra-Head LED Street Lighting Poles on Median every 6 segments
        if (params.hasStreetLighting && (i % 6 == 3)) {
            float poleBaseY = y0 + (params.hasMedianBarrier ? 1.05f : 0.0f);
            float poleTopY = y0 + 9.2f;
            Primitives::AddCylinder<ExpresswayMeshBuffer, ExpresswayVertex>(
                buffer, x0, poleBaseY, z0, x0, poleTopY, z0,
                0.16f, 0.11f, 8, 2, 0.76f, 0.80f, 0.85f, 1.0f, 0.28f, 0.88f, 0.0f
            );
            for (float armDir : {-1.0f, 1.0f}) {
                float armEndX = x0 + nx0 * (armDir * 2.8f);
                float armEndZ = z0 + nz0 * (armDir * 2.8f);
                Primitives::AddBeam<ExpresswayMeshBuffer, ExpresswayVertex>(
                    buffer, x0, poleTopY - 0.1f, z0, armEndX, poleTopY + 0.45f, armEndZ,
                    0.10f, 0.10f, 2, 0.76f, 0.80f, 0.85f, 1.0f, 0.28f, 0.88f, 0.0f
                );
                AddExpBox(buffer, armEndX, poleTopY + 0.42f, armEndZ, 0.75f, 0.12f, 0.38f, nx0, nz0, tx0, tz0,
                          4, 0.96f, 0.98f, 1.0f, 0.15f, 0.10f, 4.2f);
            }
        }
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);
    buffer.designCapacityVph = lanesPerDir * 2u * 2100.0f;

    return buffer;
}

} // namespace Road
} // namespace ApexAssets
