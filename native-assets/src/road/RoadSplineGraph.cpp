// ==============================================================================
// Modular Road Component: Hermite & Catmull-Rom Spline 3D Mesh Generator
// Path: native-assets/src/road/RoadSplineGraph.cpp
// ==============================================================================
#include "RoadSplineGraph.h"
#include "common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>
#include <vector>

namespace ApexAssets {
namespace Road {

void RoadSplineGraph::EvaluateCatmullRom(
    const float p0[3], const float p1[3], const float p2[3], const float p3[3],
    float t, float outPos[3]
) {
    float t2 = t * t;
    float t3 = t2 * t;
    for (int i = 0; i < 3; ++i) {
        outPos[i] = 0.5f * (
            (2.0f * p1[i]) +
            (-p0[i] + p2[i]) * t +
            (2.0f * p0[i] - 5.0f * p1[i] + 4.0f * p2[i] - p3[i]) * t2 +
            (-p0[i] + 3.0f * p1[i] - 3.0f * p2[i] + p3[i]) * t3
        );
    }
}

float RoadSplineGraph::ComputeSuperelevationRad(float designSpeedKmH, float radiusM) {
    float vMs = designSpeedKmH / 3.6f;
    float safeR = std::max(std::abs(radiusM), 18.0f);
    // Centrifugal banking law: tan(theta) = v^2 / (g * R), capped by AASHTO e_max (10 deg)
    float theta = std::atan((vMs * vMs) / (9.81f * safeR));
    float maxBankRad = 10.5f * Primitives::DEG2RAD;
    float sign = (radiusM >= 0.0f) ? 1.0f : -1.0f;
    return sign * std::min(theta * 0.45f, maxBankRad);
}

static void SampleSplineCurve(
    float globalT,
    const RoadSplineParams& params,
    float outPos[3]
) {
    float len = params.splineLengthM;
    float gain = params.elevationGainM;
    float amp = params.curvatureIntensity;

    switch (params.type) {
        case SplineCurveType::ClothoidSCurve: {
            float cp[6][3] = {
                { -18.0f * amp, 0.8f,               -len * 0.65f },
                { -15.0f * amp, 0.8f,               -len * 0.45f },
                { -14.0f * amp, 0.8f + gain * 0.3f, -len * 0.16f },
                {  14.0f * amp, 0.8f + gain * 0.7f,  len * 0.16f },
                {  15.0f * amp, 0.8f + gain,         len * 0.45f },
                {  18.0f * amp, 0.8f + gain,         len * 0.65f }
            };
            float scaled = std::clamp(globalT, 0.0f, 1.0f) * 3.0f;
            int seg = std::min(2, static_cast<int>(scaled));
            float lt = scaled - seg;
            RoadSplineGraph::EvaluateCatmullRom(cp[seg], cp[seg + 1], cp[seg + 2], cp[seg + 3], lt, outPos);
            break;
        }
        case SplineCurveType::HairpinSwitchback: {
            float R = (18.0f + 10.0f * amp);
            float theta = globalT * Primitives::PI; // 0 to 180 deg U-turn
            outPos[0] = (theta < 0.0f ? 0.0f : std::sin(theta) * R * 1.45f) - R * 0.7f;
            outPos[1] = 1.0f + globalT * gain;
            outPos[2] = std::cos(theta) * R;
            break;
        }
        case SplineCurveType::CatmullRomCorridor: {
            float cp[7][3] = {
                { -6.0f * amp,  1.2f,               -len * 0.62f },
                {  0.0f,        1.2f,               -len * 0.45f },
                {  16.0f * amp, 1.2f + gain * 0.65f,-len * 0.18f },
                { -16.0f * amp, 1.2f + gain * 0.35f, 0.0f },
                {  18.0f * amp, 1.2f + gain * 0.85f, len * 0.22f },
                {  2.0f * amp,  1.2f + gain,         len * 0.46f },
                { -6.0f * amp,  1.2f + gain,         len * 0.64f }
            };
            float scaled = std::clamp(globalT, 0.0f, 1.0f) * 4.0f;
            int seg = std::min(3, static_cast<int>(scaled));
            float lt = scaled - seg;
            RoadSplineGraph::EvaluateCatmullRom(cp[seg], cp[seg + 1], cp[seg + 2], cp[seg + 3], lt, outPos);
            break;
        }
        case SplineCurveType::BankedSpiralHelix: {
            float R = std::max(22.0f, len * 0.24f) / std::max(0.6f, std::sqrt(amp));
            float arc = 290.0f * Primitives::DEG2RAD;
            float angle = -arc * 0.5f + globalT * arc;
            outPos[0] = std::cos(angle) * R;
            outPos[1] = 1.4f + globalT * gain;
            outPos[2] = std::sin(angle) * R;
            break;
        }
    }
}

RoadSplineMeshBuffer RoadSplineGraph::GenerateSplineMesh(const RoadSplineParams& params) {
    RoadSplineMeshBuffer buf;

    RoadSplineParams p = params;
    p.splineLengthM = std::clamp(params.splineLengthM, 45.0f, 200.0f);
    p.roadWidthM = std::clamp(params.roadWidthM, 6.5f, 18.0f);
    p.designSpeedKmH = std::clamp(params.designSpeedKmH, 30.0f, 130.0f);
    p.elevationGainM = std::clamp(params.elevationGainM, 0.0f, 28.0f);
    p.curvatureIntensity = std::clamp(params.curvatureIntensity, 0.25f, 2.0f);

    const int slices = 72;
    float halfW = p.roadWidthM * 0.5f;
    float deckDepth = 0.72f;
    float maxBankObservedDeg = 0.0f;
    float minRadiusObservedM = 9999.0f;

    struct Frame {
        float pos[3];
        float T[3];
        float side[3];
        float up[3];
        float bankRad;
    };
    std::vector<Frame> frames(slices + 1);

    for (int i = 0; i <= slices; ++i) {
        float t = static_cast<float>(i) / slices;
        float dt = 0.004f;
        float pm[3], pp[3];
        SampleSplineCurve(t, p, frames[i].pos);
        SampleSplineCurve(std::max(0.0f, t - dt), p, pm);
        SampleSplineCurve(std::min(1.0f, t + dt), p, pp);

        float tx = pp[0] - pm[0], ty = pp[1] - pm[1], tz = pp[2] - pm[2];
        float tLen = std::sqrt(tx * tx + ty * ty + tz * tz);
        if (tLen > 1e-5f) { tx /= tLen; ty /= tLen; tz /= tLen; } else { tx = 0.0f; ty = 0.0f; tz = 1.0f; }
        frames[i].T[0] = tx; frames[i].T[1] = ty; frames[i].T[2] = tz;

        // Horizontal lateral right normal
        float hx = tz, hz = -tx;
        float hLen = std::sqrt(hx * hx + hz * hz);
        if (hLen > 1e-5f) { hx /= hLen; hz /= hLen; } else { hx = 1.0f; hz = 0.0f; }

        // Estimate planar signed curvature radius from second derivative
        float ddx = pp[0] - 2.0f * frames[i].pos[0] + pm[0];
        float ddz = pp[2] - 2.0f * frames[i].pos[2] + pm[2];
        float signedLatAccel = ddx * hx + ddz * hz;
        float ds = tLen * 0.5f;
        float kappa = (ds > 1e-4f) ? (signedLatAccel / (ds * ds)) : 0.0f;
        float radiusM = (std::abs(kappa) > 1e-4f) ? (1.0f / kappa) : 10000.0f;
        if (std::abs(radiusM) < minRadiusObservedM && std::abs(radiusM) > 1.0f) {
            minRadiusObservedM = std::abs(radiusM);
        }

        float bankRad = ComputeSuperelevationRad(p.designSpeedKmH, radiusM);
        // Blend smoothly near endpoints (t=0, t=1)
        float endFade = std::sin(t * Primitives::PI);
        bankRad *= std::min(1.0f, endFade * 2.5f);
        frames[i].bankRad = bankRad;
        maxBankObservedDeg = std::max(maxBankObservedDeg, std::abs(bankRad) / Primitives::DEG2RAD);

        float cb = std::cos(bankRad), sb = std::sin(bankRad);
        // Orthonormal side & banked surface normal
        frames[i].side[0] = hx * cb;
        frames[i].side[1] = sb;
        frames[i].side[2] = hz * cb;

        frames[i].up[0] = -hx * sb;
        frames[i].up[1] = cb;
        frames[i].up[2] = -hz * sb;
    }

    // Extrude banked cross-section segments
    for (int i = 0; i < slices; ++i) {
        const auto& f0 = frames[i];
        const auto& f1 = frames[i + 1];

        auto pt = [](const Frame& f, float lat, float vert, float out[3]) {
            out[0] = f.pos[0] + f.side[0] * lat + f.up[0] * vert;
            out[1] = f.pos[1] + f.side[1] * lat + f.up[1] * vert;
            out[2] = f.pos[2] + f.side[2] * lat + f.up[2] * vert;
        };

        float tl0[3], tr0[3], tl1[3], tr1[3];
        float bl0[3], br0[3], bl1[3], br1[3];
        pt(f0, -halfW, 0.0f, tl0); pt(f0, halfW, 0.0f, tr0);
        pt(f1, -halfW, 0.0f, tl1); pt(f1, halfW, 0.0f, tr1);
        pt(f0, -halfW * 0.88f, -deckDepth, bl0); pt(f0, halfW * 0.88f, -deckDepth, br0);
        pt(f1, -halfW * 0.88f, -deckDepth, bl1); pt(f1, halfW * 0.88f, -deckDepth, br1);

        float nUp[3] = {
            0.5f * (f0.up[0] + f1.up[0]),
            0.5f * (f0.up[1] + f1.up[1]),
            0.5f * (f0.up[2] + f1.up[2])
        };
        float nDown[3] = {-nUp[0], -nUp[1], -nUp[2]};
        float nL[3] = {-f0.side[0], -f0.side[1], -f0.side[2]};
        float nR[3] = { f0.side[0],  f0.side[1],  f0.side[2]};

        // Top SMA asphalt surface with parabolic camber bump at crown
        Primitives::AddQuad<RoadSplineMeshBuffer, RoadSplineVertex>(
            buf, tl0, tr0, tr1, tl1, nUp, 0, 0.19f, 0.21f, 0.24f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.84f, 0.02f
        );
        // Side facias + soffit
        Primitives::AddQuad<RoadSplineMeshBuffer, RoadSplineVertex>(
            buf, tl0, tl1, bl1, bl0, nL, 1, 0.62f, 0.65f, 0.70f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.68f, 0.04f
        );
        Primitives::AddQuad<RoadSplineMeshBuffer, RoadSplineVertex>(
            buf, tr0, br0, br1, tr1, nR, 1, 0.62f, 0.65f, 0.70f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.68f, 0.04f
        );
        Primitives::AddQuad<RoadSplineMeshBuffer, RoadSplineVertex>(
            buf, bl0, bl1, br1, br0, nDown, 1, 0.48f, 0.51f, 0.56f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.72f, 0.04f
        );

        // Edge safety barrier concrete kerbs
        for (int s : {-1, 1}) {
            float kOff = s * (halfW - 0.22f);
            float kb0[3], kt0[3], kb1[3], kt1[3];
            pt(f0, kOff - 0.18f, 0.0f, kb0); pt(f0, kOff + 0.18f, 0.65f, kt0);
            pt(f1, kOff - 0.18f, 0.0f, kb1); pt(f1, kOff + 0.18f, 0.65f, kt1);
            Primitives::AddBeam<RoadSplineMeshBuffer, RoadSplineVertex>(
                buf, kb0[0], 0.5f * (kb0[1] + kt0[1]), kb0[2],
                kb1[0], 0.5f * (kb1[1] + kt1[1]), kb1[2],
                0.38f, 0.68f, 1, 0.76f, 0.79f, 0.82f, 1.0f, 0.62f, 0.05f
            );
        }

        // Dashed yellow centerline stripe & white edge lines
        if (i % 3 != 2) {
            float c0L[3], c0R[3], c1L[3], c1R[3];
            pt(f0, -0.14f, 0.03f, c0L); pt(f0, 0.14f, 0.03f, c0R);
            pt(f1, -0.14f, 0.03f, c1L); pt(f1, 0.14f, 0.03f, c1R);
            Primitives::AddQuad<RoadSplineMeshBuffer, RoadSplineVertex>(
                buf, c0L, c0R, c1R, c1L, nUp, 3, 0.96f, 0.78f, 0.12f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.44f, 0.05f
            );
        }

        // Supporting RC cylindrical piers down to Y=0 whenever spline rises > 2.2m above grade
        if (i % 9 == 4 && f0.pos[1] > 2.2f) {
            float px = f0.pos[0];
            float py = f0.pos[1] - deckDepth;
            float pz = f0.pos[2];
            Primitives::AddCylinder<RoadSplineMeshBuffer, RoadSplineVertex>(
                buf, px, 0.0f, pz, px, py, pz, 0.95f, 0.80f, 10, 1, 0.66f, 0.69f, 0.74f, 1.0f, 0.68f, 0.04f
            );
            Primitives::AddBox<RoadSplineMeshBuffer, RoadSplineVertex>(
                buf, px, 0.25f, pz, 2.8f, 0.50f, 2.8f, 1, 0.52f, 0.55f, 0.60f, 1.0f, 0.74f, 0.04f
            );
            // Pier cap hammerhead
            Primitives::AddOrientedBox<RoadSplineMeshBuffer, RoadSplineVertex>(
                buf, px, py - 0.4f, pz, p.roadWidthM * 0.76f, 0.85f, 1.8f,
                f0.side[0], f0.side[2], f0.T[0], f0.T[2], 1, 0.60f, 0.64f, 0.68f, 1.0f, 0.68f, 0.04f
            );
        }
    }

    Primitives::ComputeMeshAABB(buf);
    buf.vertexCount = static_cast<uint32_t>(buf.vertices.size());
    buf.triangleCount = static_cast<uint32_t>(buf.indices.size() / 3);
    buf.maxSuperelevationDeg = maxBankObservedDeg;
    buf.minRadiusM = (minRadiusObservedM > 5000.0f) ? 0.0f : minRadiusObservedM;
    return buf;
}

} // namespace Road
} // namespace ApexAssets
