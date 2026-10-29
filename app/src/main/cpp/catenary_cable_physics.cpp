// ─── app/src/main/cpp/catenary_cable_physics.cpp ──────────────────────────────
#include "catenary_cable_physics.h"
#include <cmath>
#include <cstdint>

namespace apex {

// Generates 3D swept cylindrical tube vertices along exact catenary physics curve
void generateCatenaryCylinder(
    const Vec3& pA, const Vec3& pB,
    float tensionKn, float linearMassKgM, float radiusM,
    uint32_t curveSegments, uint32_t radialSegments,
    std::vector<CableVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    float dx = pB.x - pA.x;
    float dz = pB.z - pA.z;
    float spanLen = std::sqrt(dx * dx + dz * dz);
    if (spanLen < 0.1f) spanLen = 0.1f;

    float tNewtons = tensionKn * 1000.0f;
    float a = CatenaryCablePhysics::calculateCatenaryConstant(tNewtons, linearMassKgM);

    // 1. Generate trajectory nodes along hyperbolic cosine curve:
    // y(u) = a * [cosh(L/2a) - cosh(u/a)]
    std::vector<Vec3> centerNodes;
    centerNodes.reserve(curveSegments + 1);

    for (uint32_t i = 0; i <= curveSegments; ++i) {
        float t = static_cast<float>(i) / static_cast<float>(curveSegments);
        float u = (t - 0.5f) * spanLen;
        float sagDrop = a * (std::cosh(spanLen / (2.0f * a)) - std::cosh(u / a));

        Vec3 pt;
        pt.x = pA.x + t * (pB.x - pA.x);
        pt.y = pA.y + t * (pB.y - pA.y) - sagDrop;
        pt.z = pA.z + t * (pB.z - pA.z);
        centerNodes.push_back(pt);
    }

    // 2. Extrude radial rings using Parallel Transport Frame
    uint32_t baseVertex = static_cast<uint32_t>(outVertices.size());

    for (uint32_t i = 0; i <= curveSegments; ++i) {
        Vec3 tangent;
        if (i < curveSegments) {
            tangent.x = centerNodes[i + 1].x - centerNodes[i].x;
            tangent.y = centerNodes[i + 1].y - centerNodes[i].y;
            tangent.z = centerNodes[i + 1].z - centerNodes[i].z;
        } else {
            tangent.x = centerNodes[i].x - centerNodes[i - 1].x;
            tangent.y = centerNodes[i].y - centerNodes[i - 1].y;
            tangent.z = centerNodes[i].z - centerNodes[i - 1].z;
        }
        float tLen = std::sqrt(tangent.x * tangent.x + tangent.y * tangent.y + tangent.z * tangent.z);
        if (tLen < 0.0001f) tLen = 1.0f;
        tangent.x /= tLen; tangent.y /= tLen; tangent.z /= tLen;

        // Up reference vector
        Vec3 upRef = (std::abs(tangent.y) > 0.95f) ? Vec3(1, 0, 0) : Vec3(0, 1, 0);

        // Right = Tangent x UpRef
        Vec3 right(
            tangent.y * upRef.z - tangent.z * upRef.y,
            tangent.z * upRef.x - tangent.x * upRef.z,
            tangent.x * upRef.y - tangent.y * upRef.x
        );
        float rLen = std::sqrt(right.x * right.x + right.y * right.y + right.z * right.z);
        if (rLen < 0.0001f) rLen = 1.0f;
        right.x /= rLen; right.y /= rLen; right.z /= rLen;

        // Normal = Right x Tangent
        Vec3 normal(
            right.y * tangent.z - right.z * tangent.y,
            right.z * tangent.x - right.x * tangent.z,
            right.x * tangent.y - right.y * tangent.x
        );

        // Generate circular ring vertices
        for (uint32_t j = 0; j < radialSegments; ++j) {
            float theta = (2.0f * 3.14159265f * static_cast<float>(j)) / static_cast<float>(radialSegments);
            float cosT = std::cos(theta);
            float sinT = std::sin(theta);

            Vec3 radNorm(
                cosT * right.x + sinT * normal.x,
                cosT * right.y + sinT * normal.y,
                cosT * right.z + sinT * normal.z
            );

            CableVertex v;
            v.position = Vec3(
                centerNodes[i].x + radNorm.x * radiusM,
                centerNodes[i].y + radNorm.y * radiusM,
                centerNodes[i].z + radNorm.z * radiusM
            );
            v.normal = radNorm;
            v.u = static_cast<float>(j) / static_cast<float>(radialSegments);
            v.v = static_cast<float>(i) / static_cast<float>(curveSegments);
            v.r = 0.0f; v.g = 0.90f; v.b = 1.0f; v.a = 1.0f; // Glowing Corona Cyan
            outVertices.push_back(v);
        }
    }

    // 3. Connect circular rings into triangle strip indices
    for (uint32_t i = 0; i < curveSegments; ++i) {
        for (uint32_t j = 0; j < radialSegments; ++j) {
            uint32_t nextJ = (j + 1) % radialSegments;
            uint32_t i0 = baseVertex + i * radialSegments + j;
            uint32_t i1 = baseVertex + i * radialSegments + nextJ;
            uint32_t i2 = baseVertex + (i + 1) * radialSegments + j;
            uint32_t i3 = baseVertex + (i + 1) * radialSegments + nextJ;

            // Two triangles per quad
            outIndices.push_back(i0);
            outIndices.push_back(i2);
            outIndices.push_back(i1);

            outIndices.push_back(i1);
            outIndices.push_back(i2);
            outIndices.push_back(i3);
        }
    }
}

} // namespace apex
