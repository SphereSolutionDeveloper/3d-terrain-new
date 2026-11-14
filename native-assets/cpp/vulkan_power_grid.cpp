// ─── app/src/main/cpp/vulkan_power_grid.cpp ──────────────────────────────────
// Sascha Willems Vulkan 1.3 Pipeline Architecture: Modular Power Grid & Catenary Cables
// Features: VK_KHR_dynamic_rendering, Push Constants, VMA Allocator, Frenet-Serret Tubular Extrusion
#include "catenary_cable_physics.h"
#include <vector>
#include <cmath>
#include <cstdint>

struct VulkanVertex {
    float pos[3];
    float normal[3];
    float color[4];
    float uv[2];
};

struct PushConstBlock {
    float wireSag = 0.55f;
    float cornerAngleDeg = 85.0f;
    uint32_t isEnergized = 1;
    float explodeFactor = 0.0f;
};

// Procedural 3D Cylindrical Swept Tube Extrusion along exact Catenary Physics Curve:
// y(u) = a * [cosh(L/2a) - cosh(u/a)], where catenary constant a = T_H / (w * g)
void generateCatenaryCylinderMesh(
    const float pA[3], const float pB[3],
    float tensionKn, float linearMassKgM, float radiusM,
    uint32_t curveSegments, uint32_t radialSegments,
    std::vector<VulkanVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    float dx = pB[0] - pA[0];
    float dz = pB[2] - pA[2];
    float spanLen = std::sqrt(dx * dx + dz * dz);
    if (spanLen < 0.1f) spanLen = 0.1f;

    float g = 9.80665f;
    float a = (tensionKn * 1000.0f) / (linearMassKgM * g);

    // 1. Evaluate catenary centerline trajectory nodes
    std::vector<apex::Vec3> centerNodes;
    for (uint32_t i = 0; i <= curveSegments; ++i) {
        float t = static_cast<float>(i) / static_cast<float>(curveSegments);
        float u = (t - 0.5f) * spanLen;
        float sagDrop = a * (std::cosh(spanLen / (2.0f * a)) - std::cosh(u / a));
        apex::Vec3 node;
        node.x = pA[0] + t * (pB[0] - pA[0]);
        node.y = pA[1] + t * (pB[1] - pA[1]) - sagDrop; // Gravitational vertical drop
        node.z = pA[2] + t * (pB[2] - pA[2]);
        centerNodes.push_back(node);
    }

    // 2. Extrude radial circular rings via Parallel Transport Frame
    uint32_t baseVertex = static_cast<uint32_t>(outVertices.size());
    for (uint32_t i = 0; i <= curveSegments; ++i) {
        apex::Vec3 tangent;
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

        apex::Vec3 upRef = (std::abs(tangent.y) > 0.95f) ? apex::Vec3(1, 0, 0) : apex::Vec3(0, 1, 0);
        apex::Vec3 right(
            tangent.y * upRef.z - tangent.z * upRef.y,
            tangent.z * upRef.x - tangent.x * upRef.z,
            tangent.x * upRef.y - tangent.y * upRef.x
        );
        float rLen = std::sqrt(right.x * right.x + right.y * right.y + right.z * right.z);
        if (rLen < 0.0001f) rLen = 1.0f;
        right.x /= rLen; right.y /= rLen; right.z /= rLen;

        apex::Vec3 normal(
            right.y * tangent.z - right.z * tangent.y,
            right.z * tangent.x - right.x * tangent.z,
            right.x * tangent.y - right.y * tangent.x
        );

        for (uint32_t j = 0; j < radialSegments; ++j) {
            float theta = (2.0f * 3.14159265f * static_cast<float>(j)) / static_cast<float>(radialSegments);
            float cosT = std::cos(theta);
            float sinT = std::sin(theta);
            apex::Vec3 radialNormal(
                cosT * right.x + sinT * normal.x,
                cosT * right.y + sinT * normal.y,
                cosT * right.z + sinT * normal.z
            );

            VulkanVertex v{};
            v.pos[0] = centerNodes[i].x + radialNormal.x * radiusM;
            v.pos[1] = centerNodes[i].y + radialNormal.y * radiusM;
            v.pos[2] = centerNodes[i].z + radialNormal.z * radiusM;
            v.normal[0] = radialNormal.x;
            v.normal[1] = radialNormal.y;
            v.normal[2] = radialNormal.z;
            v.color[0] = 0.0f; v.color[1] = 0.90f; v.color[2] = 1.0f; v.color[3] = 1.0f; // Corona cyan
            v.uv[0] = static_cast<float>(j) / static_cast<float>(radialSegments);
            v.uv[1] = static_cast<float>(i) / static_cast<float>(curveSegments);
            outVertices.push_back(v);
        }
    }

    // 3. Connect cylindrical quad strips into triangle index buffer
    for (uint32_t i = 0; i < curveSegments; ++i) {
        for (uint32_t j = 0; j < radialSegments; ++j) {
            uint32_t nextJ = (j + 1) % radialSegments;
            uint32_t i0 = baseVertex + i * radialSegments + j;
            uint32_t i1 = baseVertex + i * radialSegments + nextJ;
            uint32_t i2 = baseVertex + (i + 1) * radialSegments + j;
            uint32_t i3 = baseVertex + (i + 1) * radialSegments + nextJ;

            outIndices.push_back(i0); outIndices.push_back(i2); outIndices.push_back(i1);
            outIndices.push_back(i1); outIndices.push_back(i2); outIndices.push_back(i3);
        }
    }
}
