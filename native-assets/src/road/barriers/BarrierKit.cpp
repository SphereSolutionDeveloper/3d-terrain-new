// ==============================================================================
// Modular Road Component: Safety Barrier & Guardrail 3D Mesh Generator
// Path: native-assets/src/road/barriers/BarrierKit.cpp
// Standards: MASH 2016 Crash Test Criteria & EN 1317 Road Restraint Systems
// ==============================================================================
#include "BarrierKit.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float DEG2RAD = PI / 180.0f;

static void AddBarQuad(
    BarrierMeshBuffer& buffer,
    const float c0[3], const float c1[3], const float c2[3], const float c3[3],
    const float norm[3], uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddQuad<BarrierMeshBuffer, BarrierVertex>(
        buffer, c0, c1, c2, c3, norm, matId, r, g, b, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, rough, metal, emis
    );
}

static void AddBarBox(
    BarrierMeshBuffer& buffer,
    float cx, float cy, float cz,
    float dimN, float dimY, float dimT,
    float nx, float nz, float tx, float tz,
    uint32_t matId,
    float r, float g, float b, float rough, float metal, float emis = 0.0f
) {
    Primitives::AddOrientedBox<BarrierMeshBuffer, BarrierVertex>(
        buffer, cx, cy, cz, dimN, dimY, dimT, nx, nz, tx, tz,
        matId, r, g, b, 1.0f, rough, metal, emis
    );
}

void BarrierKit::SampleBarrierCurve(
    float t,
    const BarrierParams& params,
    float& outX, float& outY, float& outZ,
    float& outTx, float& outTz
) {
    float length = params.barrierLengthM;
    if (std::abs(params.curveBendAngleDeg) > 0.01f) {
        float bendRad = params.curveBendAngleDeg * DEG2RAD;
        float radius = length / bendRad;
        float ang = (t - 0.5f) * bendRad;
        outX = radius * (1.0f - std::cos(ang));
        outZ = (t - 0.5f) * length;
        outTx = std::sin(ang);
        outTz = std::cos(ang);
        float l = std::sqrt(outTx * outTx + outTz * outTz);
        if (l > 1e-5f) { outTx /= l; outTz /= l; }
    } else {
        outX = 0.0f;
        outZ = (t - 0.5f) * length;
        outTx = 0.0f;
        outTz = 1.0f;
    }
    outY = 0.02f;
}

BarrierMeshBuffer BarrierKit::GenerateBarrierMesh(const BarrierParams& params) {
    BarrierMeshBuffer buffer;

    float L = params.barrierLengthM;
    float H = std::max(0.75f, params.barrierHeightM);
    int segs = std::max(12, static_cast<int>(std::round(L / std::max(1.2f, params.postSpacingM))));

    // 1. Extrude Grounded Highway Shoulder Asphalt & Concrete Mow Strip Base
    for (int i = 0; i < segs; ++i) {
        float t0 = static_cast<float>(i) / segs;
        float t1 = static_cast<float>(i + 1) / segs;
        float x0, y0, z0, tx0, tz0;
        float x1, y1, z1, tx1, tz1;
        SampleBarrierCurve(t0, params, x0, y0, z0, tx0, tz0);
        SampleBarrierCurve(t1, params, x1, y1, z1, tx1, tz1);

        float nx0 = tz0, nz0 = -tx0;
        float nx1 = tz1, nz1 = -tx1;

        float roadHalf = 2.6f;
        float nUp[3] = {0.0f, 1.0f, 0.0f};
        float a00[3] = {x0 - nx0 * roadHalf, 0.0f, z0 - nz0 * roadHalf};
        float a01[3] = {x0 + nx0 * roadHalf, 0.0f, z0 + nz0 * roadHalf};
        float a11[3] = {x1 + nx1 * roadHalf, 0.0f, z1 + nz1 * roadHalf};
        float a10[3] = {x1 - nx1 * roadHalf, 0.0f, z1 - nz1 * roadHalf};
        AddBarQuad(buffer, a00, a01, a11, a10, nUp, 0, 0.19f, 0.21f, 0.24f, 0.86f, 0.02f);

        // Edge lane shoulder line offset 1.4m from barrier center
        float sOff = -1.45f;
        float s00[3] = {x0 + nx0 * (sOff - 0.09f), 0.02f, z0 + nz0 * (sOff - 0.09f)};
        float s01[3] = {x0 + nx0 * (sOff + 0.09f), 0.02f, z0 + nz0 * (sOff + 0.09f)};
        float s11[3] = {x1 + nx1 * (sOff + 0.09f), 0.02f, z1 + nz1 * (sOff + 0.09f)};
        float s10[3] = {x1 + nx1 * (sOff - 0.09f), 0.02f, z1 + nz1 * (sOff - 0.09f)};
        AddBarQuad(buffer, s00, s01, s11, s10, nUp, 3, 0.94f, 0.95f, 0.97f, 0.45f, 0.05f);

        // 2. Render Specific Crash Barrier / Restraint Superstructure
        switch (params.type) {
            case BarrierType::NewJerseyFShapeConcrete: {
                // Multi-Slope F-Shape Concrete Jersey Barrier Profile (Wide Toe -> Sloped Base -> Upper Wall -> Flat Cap)
                float hKnee = H * 0.34f;
                float wBase = 0.36f, wKnee = 0.20f, wTop = 0.12f;

                float bl0[3] = {x0 - nx0 * wBase, 0.0f,   z0 - nz0 * wBase};
                float br0[3] = {x0 + nx0 * wBase, 0.0f,   z0 + nz0 * wBase};
                float bl1[3] = {x1 - nx1 * wBase, 0.0f,   z1 - nz1 * wBase};
                float br1[3] = {x1 + nx1 * wBase, 0.0f,   z1 + nz1 * wBase};

                float kl0[3] = {x0 - nx0 * wKnee, hKnee,  z0 - nz0 * wKnee};
                float kr0[3] = {x0 + nx0 * wKnee, hKnee,  z0 + nz0 * wKnee};
                float kl1[3] = {x1 - nx1 * wKnee, hKnee,  z1 - nz1 * wKnee};
                float kr1[3] = {x1 + nx1 * wKnee, hKnee,  z1 + nz1 * wKnee};

                float tl0[3] = {x0 - nx0 * wTop,  H,      z0 - nz0 * wTop};
                float tr0[3] = {x0 + nx0 * wTop,  H,      z0 + nz0 * wTop};
                float tl1[3] = {x1 - nx1 * wTop,  H,      z1 - nz1 * wTop};
                float tr1[3] = {x1 + nx1 * wTop,  H,      z1 + nz1 * wTop};

                float nL[3] = {-nx0, 0.35f, -nz0};
                float nR[3] = { nx0, 0.35f,  nz0};
                AddBarQuad(buffer, kl0, kl1, bl1, bl0, nL,  1, 0.70f, 0.73f, 0.77f, 0.68f, 0.04f);
                AddBarQuad(buffer, tl0, tl1, kl1, kl0, nL,  1, 0.76f, 0.79f, 0.83f, 0.65f, 0.04f);
                AddBarQuad(buffer, kr0, br0, br1, kr1, nR,  1, 0.70f, 0.73f, 0.77f, 0.68f, 0.04f);
                AddBarQuad(buffer, tr0, kr0, kr1, tr1, nR,  1, 0.76f, 0.79f, 0.83f, 0.65f, 0.04f);
                AddBarQuad(buffer, tl0, tr0, tr1, tl1, nUp, 1, 0.86f, 0.88f, 0.90f, 0.60f, 0.04f);

                if (params.hasReflectorDelineators && (i % 2 == 0)) {
                    AddBarBox(buffer, x0, H + 0.08f, z0, 0.16f, 0.16f, 0.14f,
                              nx0, nz0, tx0, tz0, 4, 0.98f, 0.74f, 0.08f, 0.18f, 0.10f, 3.8f);
                }
                break;
            }

            case BarrierType::GalvanizedWBeamGuardrail: {
                // Driven Steel I-Beam Post + Offset Blockout + Double-Wave W-Beam Steel Profile + SKT Terminal Head
                AddBarBox(buffer, x0, H * 0.45f, z0, 0.14f, H * 0.90f, 0.12f,
                          nx0, nz0, tx0, tz0, 2, 0.46f, 0.50f, 0.54f, 0.35f, 0.88f);
                AddBarBox(buffer, x0 - nx0 * 0.14f, H * 0.68f, z0 - nz0 * 0.14f,
                          0.16f, 0.34f, 0.14f, nx0, nz0, tx0, tz0,
                          2, 0.28f, 0.30f, 0.34f, 0.45f, 0.70f);
                // Double corrugated galvanized steel waves (upper ridge & lower ridge along -nx edge)
                float railOff = -0.24f;
                for (float ridgeY : {H * 0.58f, H * 0.78f}) {
                    Primitives::AddBeam<BarrierMeshBuffer, BarrierVertex>(
                        buffer,
                        x0 + nx0 * railOff, ridgeY, z0 + nz0 * railOff,
                        x1 + nx1 * railOff, ridgeY, z1 + nz1 * railOff,
                        0.09f, 0.13f, 2, 0.78f, 0.82f, 0.86f, 1.0f, 0.24f, 0.90f, 0.0f
                    );
                }
                // Yellow & Black MASH SKT Energy-Absorbing Impact Terminal Head at start nose
                if (i == 0) {
                    AddBarBox(buffer, x0 + nx0 * railOff, H * 0.68f, z0 + nz0 * railOff,
                              0.42f, 0.55f, 0.75f, nx0, nz0, tx0, tz0,
                              3, 0.98f, 0.74f, 0.08f, 0.38f, 0.20f, 0.3f);
                }
                break;
            }

            case BarrierType::HighTensionCableBarrier: {
                // Concrete Mow Strip + Driven Steel Posts + 4 Tensioned High-Strength Wire Cables
                AddBarBox(buffer, (x0 + x1) * 0.5f, 0.04f, (z0 + z1) * 0.5f,
                          0.75f, 0.08f, (L / segs) + 0.1f, nx0, nz0, tx0, tz0,
                          1, 0.62f, 0.65f, 0.69f, 0.70f, 0.04f);
                AddBarBox(buffer, x0, H * 0.48f, z0, 0.10f, H * 0.96f, 0.10f,
                          nx0, nz0, tx0, tz0, 2, 0.52f, 0.56f, 0.60f, 0.32f, 0.88f);
                for (float cableFrac : {0.32f, 0.52f, 0.70f, 0.88f}) {
                    float cy = H * cableFrac;
                    Primitives::AddBeam<BarrierMeshBuffer, BarrierVertex>(
                        buffer, x0, cy, z0, x1, cy, z1,
                        0.045f, 0.045f, 2, 0.84f, 0.88f, 0.92f, 1.0f, 0.22f, 0.92f, 0.0f
                    );
                }
                if (i == 0 || i == segs - 1) {
                    float ax = (i == 0) ? x0 : x1, az = (i == 0) ? z0 : z1;
                    AddBarBox(buffer, ax, 0.30f, az, 0.85f, 0.60f, 1.45f, nx0, nz0, tx0, tz0,
                              1, 0.54f, 0.57f, 0.61f, 0.72f, 0.05f);
                }
                break;
            }

            case BarrierType::AcousticNoiseBarrierWall: {
                // Tall Highway Sound Wall (3.2m - 4.8m) on Plinth with H-Piles + Ribbed Panels + Clear Aero Top
                float wallH = (H < 2.2f) ? std::max(3.2f, H * 2.8f) : H;
                float midX = (x0 + x1) * 0.5f, midZ = (z0 + z1) * 0.5f;
                float bayLen = std::sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));

                // Vertical Wide-Flange Steel H-Pile Column
                AddBarBox(buffer, x0, wallH * 0.5f, z0, 0.32f, wallH + 0.15f, 0.30f,
                          nx0, nz0, tx0, tz0, 2, 0.34f, 0.38f, 0.42f, 0.36f, 0.86f);
                // Concrete Plinth Base Sill
                AddBarBox(buffer, midX, 0.30f, midZ, 0.44f, 0.60f, bayLen + 0.05f,
                          nx0, nz0, tx0, tz0, 1, 0.58f, 0.61f, 0.65f, 0.70f, 0.04f);
                // Ribbed Sound-Absorbing Composite Lower Panels (Slate Teal/Grey)
                AddBarBox(buffer, midX, 0.60f + wallH * 0.32f, midZ, 0.22f, wallH * 0.64f, bayLen * 0.96f,
                          nx0, nz0, tx0, tz0, 1, 0.42f, 0.52f, 0.56f, 0.65f, 0.15f);
                // Upper Translucent Acoustic Acrylic Glazing Screen
                AddBarBox(buffer, midX, 0.60f + wallH * 0.78f, midZ, 0.10f, wallH * 0.26f, bayLen * 0.96f,
                          nx0, nz0, tx0, tz0, 4, 0.34f, 0.76f, 0.88f, 0.12f, 0.20f, 0.2f);
                break;
            }

            case BarrierType::RollerImpactCrashCushion: {
                // High-Visibility EVA Shock-Absorbing Yellow Rotating Roller Barrels on Twin Pipe Rails
                Primitives::AddCylinder<BarrierMeshBuffer, BarrierVertex>(
                    buffer, x0, 0.0f, z0, x0, H + 0.15f, z0,
                    0.11f, 0.11f, 10, 2, 0.74f, 0.78f, 0.82f, 1.0f, 0.28f, 0.88f, 0.0f
                );
                for (float railY : {H * 0.25f, H * 0.92f}) {
                    Primitives::AddBeam<BarrierMeshBuffer, BarrierVertex>(
                        buffer, x0, railY, z0, x1, railY, z1,
                        0.10f, 0.10f, 2, 0.74f, 0.78f, 0.82f, 1.0f, 0.28f, 0.88f, 0.0f
                    );
                }
                // Alternating Rotating Bright Yellow EVA Impact Roller Barrels along Span
                for (int k = 1; k <= 3; ++k) {
                    float f = static_cast<float>(k) / 4.0f;
                    float rx = x0 + (x1 - x0) * f;
                    float rz = z0 + (z1 - z0) * f;
                    Primitives::AddCylinder<BarrierMeshBuffer, BarrierVertex>(
                        buffer, rx, H * 0.30f, rz, rx, H * 0.86f, rz,
                        0.24f, 0.24f, 12, 3, 0.98f, 0.76f, 0.08f, 1.0f, 0.40f, 0.05f, 0.2f
                    );
                }
                break;
            }
        }
    }

    Primitives::ComputeMeshAABB(buffer);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.triangleCount = static_cast<uint32_t>(buffer.indices.size() / 3);
    buffer.containmentRatingKj = (params.type == BarrierType::HighTensionCableBarrier) ? 260.0f : 580.0f;

    return buffer;
}

} // namespace Road
} // namespace ApexAssets
