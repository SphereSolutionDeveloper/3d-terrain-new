// ─── app/src/main/cpp/catenary_cable_physics.h ────────────────────────────────
// Real C++ Header: Hyperbolic Cosine Catenary Physics & 3D Cylinder Swept Tube
#pragma once
#include <cmath>
#include <cstdint>
#include <vector>

namespace apex {

struct Vec3 {
    float x, y, z;
    Vec3(float _x = 0.0f, float _y = 0.0f, float _z = 0.0f) : x(_x), y(_y), z(_z) {}
};

struct CableVertex {
    Vec3 position;
    Vec3 normal;
    float u, v;
    float r, g, b, a;
};

class CatenaryCablePhysics {
public:
    static constexpr float GRAVITY = 9.80665f; // m/s^2

    // Calculates Catenary scaling parameter: a = T_H / (w * g)
    static float calculateCatenaryConstant(float tensionNewtons, float linearMassKgPerM) {
        if (linearMassKgPerM <= 0.0001f) return 2000.0f;
        return tensionNewtons / (linearMassKgPerM * GRAVITY);
    }

    // Exact mid-span maximum sag: S = a * [cosh(L / 2a) - 1]
    static float calculateExactSag(float spanLengthM, float a) {
        if (a <= 0.1f) return 0.0f;
        return a * (std::cosh(spanLengthM / (2.0f * a)) - 1.0f);
    }

    // Parabolic approximation sag: S ≈ (w * g * L^2) / (8 * T_H) = L^2 / (8 * a)
    static float calculateParabolicSag(float spanLengthM, float a) {
        if (a <= 0.1f) return 0.0f;
        return (spanLengthM * spanLengthM) / (8.0f * a);
    }

    // Conductor actual arc length: s = 2a * sinh(L / 2a)
    static float calculateArcLength(float spanLengthM, float a) {
        if (a <= 0.1f) return spanLengthM;
        return 2.0f * a * std::sinh(spanLengthM / (2.0f * a));
    }
};

void generateCatenaryCylinder(
    const Vec3& pA, const Vec3& pB,
    float tensionKn, float linearMassKgM, float radiusM,
    uint32_t curveSegments, uint32_t radialSegments,
    std::vector<CableVertex>& outVertices,
    std::vector<uint32_t>& outIndices
);

} // namespace apex
