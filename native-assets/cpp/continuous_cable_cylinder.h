// ─── app/src/main/cpp/continuous_cable_cylinder.h ────────────────────────────
// Real C++ Native Engine: Continuous Curved Cylinder Swept Cable
// Mathematically bent via exact hyperbolic catenary & corner spline equations
#pragma once
#include <cmath>
#include <cstdint>
#include <vector>
#include <string>

namespace apex {

struct Vec3D {
    float x, y, z;
    Vec3D(float _x = 0.0f, float _y = 0.0f, float _z = 0.0f) : x(_x), y(_y), z(_z) {}

    Vec3D operator+(const Vec3D& o) const { return Vec3D(x + o.x, y + o.y, z + o.z); }
    Vec3D operator-(const Vec3D& o) const { return Vec3D(x - o.x, y - o.y, z - o.z); }
    Vec3D operator*(float s) const { return Vec3D(x * s, y * s, z * s); }

    float length() const { return std::sqrt(x * x + y * y + z * z); }
    Vec3D normalized() const {
        float l = length();
        return (l > 0.00001f) ? Vec3D(x / l, y / l, z / l) : Vec3D(0, 1, 0);
    }
    static Vec3D cross(const Vec3D& a, const Vec3D& b) {
        return Vec3D(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        );
    }
    static float dot(const Vec3D& a, const Vec3D& b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }
};

struct CylinderVertex {
    Vec3D pos;       // 3D world position (meters)
    Vec3D normal;    // Radial unit normal vector for specular reflection
    float u, v;      // Cylindrical UV texture coordinates
    float r, g, b, a;// Vertex color (Corona Cyan or Galvanized Steel)
};

// Continuous Mathematical Bending Function for Overhead Cables
// Bends smoothly in 3D:
// 1. Vertical plane: exact Catenary cosh gravity drop: y(t) = a * [cosh(L/2a) - cosh((t - 0.5)L / a)]
// 2. Horizontal plane: continuous angle turn spline at corner post
class ContinuousCableBendingFunction {
public:
    Vec3D pStart;
    Vec3D pEnd;
    float spanLength;
    float tensionNewtons;
    float linearMassKgM;
    float catenaryA; // Scaling constant a = T_H / (w * g)
    float cornerAngleRad;

    ContinuousCableBendingFunction(
        const Vec3D& start,
        const Vec3D& end,
        float tensionKn = 18.5f,
        float linearMass = 1.25f,
        float cornerAngle = 0.0f
    ) : pStart(start), pEnd(end), cornerAngleRad(cornerAngle) {
        float dx = pEnd.x - pStart.x;
        float dz = pEnd.z - pStart.z;
        spanLength = std::sqrt(dx * dx + dz * dz);
        if (spanLength < 0.1f) spanLength = 0.1f;

        tensionNewtons = tensionKn * 1000.0f;
        linearMassKgM = linearMass;
        float g = 9.80665f;
        catenaryA = (tensionNewtons) / (linearMassKgM * g);
        if (catenaryA < 1.0f) catenaryA = 1000.0f;
    }

    // Exact continuous 3D position at normalized curve parameter t in [0.0, 1.0]
    Vec3D evaluatePosition(float t) const {
        // Linear baseline between support clamps
        Vec3D baseline = pStart + (pEnd - pStart) * t;

        // Vertical gravitational bending: exact catenary hyperbolic cosine
        float u = (t - 0.5f) * spanLength;
        float halfSpanRatio = spanLength / (2.0f * catenaryA);
        float sagDrop = catenaryA * (std::cosh(halfSpanRatio) - std::cosh(u / catenaryA));

        // Horizontal corner transition bending (if at turn pole)
        float horizontalSway = 0.0f;
        if (std::abs(cornerAngleRad) > 0.01f) {
            // Smooth sinusoidal transition across angle bisector
            horizontalSway = std::sin(t * 3.14159265f) * 0.15f * std::sin(cornerAngleRad * 0.5f);
        }

        return Vec3D(
            baseline.x + horizontalSway,
            baseline.y - sagDrop, // Continuous downward gravity sag
            baseline.z
        );
    }

    // Exact continuous tangent vector C'(t)
    Vec3D evaluateTangent(float t) const {
        // Analytical derivative of catenary sag:
        // d(sag)/dt = -spanLength * sinh((t - 0.5)L / a)
        float u = (t - 0.5f) * spanLength;
        float dy_dt = -spanLength * std::sinh(u / catenaryA);

        Vec3D dLinear = pEnd - pStart;
        Vec3D tangent(dLinear.x, dLinear.y - dy_dt, dLinear.z);
        return tangent.normalized();
    }
};

// Continuous 3D Cylinder Tubular Swept Mesh Generator
// Connects continuous circular rings seamlessly into a single unbroken tubular surface
class ContinuousCylinderMesh {
public:
    std::vector<CylinderVertex> vertices;
    std::vector<uint32_t> indices;
    float radiusMeters;
    uint32_t radialSides;
    uint32_t longitudinalSegments;

    ContinuousCylinderMesh(
        float radius = 0.035f,
        uint32_t sides = 8,
        uint32_t segments = 24
    ) : radiusMeters(radius), radialSides(sides), longitudinalSegments(segments) {}

    // Generates continuous, seamless cylindrical swept tube along the mathematical bending curve
    void generate(
        const ContinuousCableBendingFunction& curve,
        float colorR = 0.95f, float colorG = 0.65f, float colorB = 0.18f
    ) {
        vertices.clear();
        indices.clear();

        vertices.reserve((longitudinalSegments + 1) * radialSides);
        indices.reserve(longitudinalSegments * radialSides * 6);

        // Initial reference vector for Bishop Parallel Transport Frame
        Vec3D initialTangent = curve.evaluateTangent(0.0f);
        Vec3D upRef = (std::abs(initialTangent.y) > 0.95f) ? Vec3D(1, 0, 0) : Vec3D(0, 1, 0);
        Vec3D prevNormal = Vec3D::cross(Vec3D::cross(initialTangent, upRef), initialTangent).normalized();

        // 1. Generate continuous circular cross-section rings along the curved path
        for (uint32_t i = 0; i <= longitudinalSegments; ++i) {
            float t = static_cast<float>(i) / static_cast<float>(longitudinalSegments);
            Vec3D center = curve.evaluatePosition(t);
            Vec3D tangent = curve.evaluateTangent(t);

            // Parallel transport normal vector (minimizes twist along 3D catenary)
            Vec3D right = Vec3D::cross(tangent, prevNormal).normalized();
            Vec3D normal = Vec3D::cross(right, tangent).normalized();
            prevNormal = normal;

            // Generate circular ring vertices
            for (uint32_t j = 0; j < radialSides; ++j) {
                float phi = (2.0f * 3.14159265f * static_cast<float>(j)) / static_cast<float>(radialSides);
                float cosPhi = std::cos(phi);
                float sinPhi = std::sin(phi);

                // Radial normal pointing outwards from cylinder centerline
                Vec3D radNorm = (right * cosPhi + normal * sinPhi).normalized();
                Vec3D vertPos = center + radNorm * radiusMeters;

                CylinderVertex v;
                v.pos = vertPos;
                v.normal = radNorm;
                v.u = static_cast<float>(j) / static_cast<float>(radialSides);
                v.v = t * curve.spanLength;
                v.r = colorR; v.g = colorG; v.b = colorB; v.a = 1.0f;

                vertices.push_back(v);
            }
        }

        // 2. Stitch continuous quad strips into seamless triangle mesh
        for (uint32_t i = 0; i < longitudinalSegments; ++i) {
            for (uint32_t j = 0; j < radialSides; ++j) {
                uint32_t nextJ = (j + 1) % radialSides;

                uint32_t r0_curr = i * radialSides + j;
                uint32_t r0_next = i * radialSides + nextJ;
                uint32_t r1_curr = (i + 1) * radialSides + j;
                uint32_t r1_next = (i + 1) * radialSides + nextJ;

                // Triangle 1
                indices.push_back(r0_curr);
                indices.push_back(r1_curr);
                indices.push_back(r0_next);

                // Triangle 2
                indices.push_back(r0_next);
                indices.push_back(r1_curr);
                indices.push_back(r1_next);
            }
        }
    }
};

} // namespace apex
