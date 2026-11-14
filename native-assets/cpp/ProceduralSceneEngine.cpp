// ─── app/src/main/cpp/ProceduralSceneEngine.cpp ────────────────────────────────
// Native C++ Procedural 3D Asset & Catenary Scene Engine Implementation
#include "ProceduralSceneEngine.h"
#include <cmath>

namespace custom_vulkan {

ProceduralSceneEngine& ProceduralSceneEngine::getInstance() {
    static ProceduralSceneEngine instance;
    return instance;
}

void ProceduralSceneEngine::addBox(NativeModelMesh& mesh, const Vec3& center, const Vec3& size, const Vec4& color) {
    Vec3 h = size * 0.5f;
    uint32_t startIdx = static_cast<uint32_t>(mesh.vertices.size());

    // 8 box vertices
    Vec3 corners[8] = {
        center + Vec3(-h.x, -h.y, -h.z),
        center + Vec3( h.x, -h.y, -h.z),
        center + Vec3( h.x,  h.y, -h.z),
        center + Vec3(-h.x,  h.y, -h.z),
        center + Vec3(-h.x, -h.y,  h.z),
        center + Vec3( h.x, -h.y,  h.z),
        center + Vec3( h.x,  h.y,  h.z),
        center + Vec3(-h.x,  h.y,  h.z)
    };

    // 6 faces: 24 vertices for sharp normals
    const int faceIndices[6][4] = {
        {0, 3, 2, 1}, // Front (-Z)
        {5, 6, 7, 4}, // Back (+Z)
        {4, 7, 3, 0}, // Left (-X)
        {1, 2, 6, 5}, // Right (+X)
        {3, 7, 6, 2}, // Top (+Y)
        {4, 0, 1, 5}  // Bottom (-Y)
    };

    const Vec3 faceNormals[6] = {
        { 0,  0, -1},
        { 0,  0,  1},
        {-1,  0,  0},
        { 1,  0,  0},
        { 0,  1,  0},
        { 0, -1,  0}
    };

    for (int f = 0; f < 6; ++f) {
        uint32_t fStart = static_cast<uint32_t>(mesh.vertices.size());
        for (int i = 0; i < 4; ++i) {
            NativeModelVertex v;
            v.position = corners[faceIndices[f][i]];
            v.normal = faceNormals[f];
            v.color = color;
            v.uv = (i == 0) ? Vec2(0, 0) : (i == 1) ? Vec2(1, 0) : (i == 2) ? Vec2(1, 1) : Vec2(0, 1);
            mesh.vertices.push_back(v);
            mesh.aabb.expand(v.position);
        }
        mesh.indices.push_back(fStart);
        mesh.indices.push_back(fStart + 1);
        mesh.indices.push_back(fStart + 2);

        mesh.indices.push_back(fStart);
        mesh.indices.push_back(fStart + 2);
        mesh.indices.push_back(fStart + 3);
        mesh.triangleCount += 2;
    }
}

void ProceduralSceneEngine::addCylinder(NativeModelMesh& mesh, const Vec3& base, const Vec3& top,
                                       float radiusBottom, float radiusTop, int radialSegments, const Vec4& color) {
    Vec3 axis = top - base;
    float height = axis.length();
    if (height < 0.0001f) return;
    Vec3 up = axis / height;

    Vec3 arbitrary = (std::abs(up.y) < 0.99f) ? Vec3(0, 1, 0) : Vec3(1, 0, 0);
    Vec3 sideX = up.cross(arbitrary).normalized();
    Vec3 sideZ = up.cross(sideX).normalized();

    uint32_t baseCenterIdx = static_cast<uint32_t>(mesh.vertices.size());

    // Generate ring vertices
    for (int i = 0; i < radialSegments; ++i) {
        float angle = (static_cast<float>(i) / radialSegments) * 2.0f * PI;
        float cosA = std::cos(angle);
        float sinA = std::sin(angle);
        Vec3 dir = sideX * cosA + sideZ * sinA;

        // Bottom vertex
        NativeModelVertex vBot;
        vBot.position = base + dir * radiusBottom;
        vBot.normal = dir;
        vBot.uv = Vec2(static_cast<float>(i) / radialSegments, 0.0f);
        vBot.color = color;
        mesh.vertices.push_back(vBot);
        mesh.aabb.expand(vBot.position);

        // Top vertex
        NativeModelVertex vTop;
        vTop.position = top + dir * radiusTop;
        vTop.normal = dir;
        vTop.uv = Vec2(static_cast<float>(i) / radialSegments, 1.0f);
        vTop.color = color;
        mesh.vertices.push_back(vTop);
        mesh.aabb.expand(vTop.position);
    }

    // Connect cylinder quads
    for (int i = 0; i < radialSegments; ++i) {
        int next = (i + 1) % radialSegments;
        uint32_t b0 = baseCenterIdx + i * 2;
        uint32_t t0 = b0 + 1;
        uint32_t b1 = baseCenterIdx + next * 2;
        uint32_t t1 = b1 + 1;

        mesh.indices.push_back(b0);
        mesh.indices.push_back(t0);
        mesh.indices.push_back(b1);

        mesh.indices.push_back(b1);
        mesh.indices.push_back(t0);
        mesh.indices.push_back(t1);
        mesh.triangleCount += 2;
    }
}

NativeModelMesh ProceduralSceneEngine::buildModularPowerPost(float heightM, float poleRadius, int crossarmLevels) {
    NativeModelMesh mesh;
    mesh.name = "modular_power_post";

    // 1. Concrete foundation base block
    addBox(mesh, Vec3(0, 0.35f, 0), Vec3(0.9f, 0.7f, 0.9f), Vec4(0.45f, 0.45f, 0.47f, 1.0f));

    // 2. Tapered wooden/composite utility pole shaft
    addCylinder(mesh, Vec3(0, 0.7f, 0), Vec3(0, heightM, 0), poleRadius, poleRadius * 0.7f, 12, Vec4(0.38f, 0.28f, 0.20f, 1.0f));

    // 3. Horizontal steel crossarms
    for (int lvl = 0; lvl < crossarmLevels; ++lvl) {
        float armHeight = heightM - 0.8f - lvl * 1.6f;
        float armLength = 2.8f - lvl * 0.4f;
        addBox(mesh, Vec3(0, armHeight, 0), Vec3(armLength, 0.12f, 0.12f), Vec4(0.30f, 0.32f, 0.35f, 1.0f));

        // Ceramic insulators on crossarms
        for (float insX : {-armLength * 0.45f, 0.0f, armLength * 0.45f}) {
            addCylinder(mesh, Vec3(insX, armHeight + 0.06f, 0), Vec3(insX, armHeight + 0.32f, 0),
                        0.07f, 0.045f, 8, Vec4(0.85f, 0.90f, 0.95f, 1.0f));
        }
    }

    return mesh;
}

NativeModelMesh ProceduralSceneEngine::buildCatenaryCableSpan(const Vec3& startPos,
                                                             const Vec3& endPos,
                                                             float sagM,
                                                             float cableRadius,
                                                             int segments) {
    NativeModelMesh mesh;
    mesh.name = "catenary_cable_span";

    std::vector<Vec3> spine(segments + 1);
    for (int i = 0; i <= segments; ++i) {
        float t = static_cast<float>(i) / segments;
        Vec3 straight = startPos * (1.0f - t) + endPos * t;

        // Catenary parabolic sag profile: 4 * sag * t * (1 - t)
        float sagFactor = 4.0f * sagM * t * (1.0f - t);
        straight.y -= sagFactor;
        spine[i] = straight;
    }

    for (int i = 0; i < segments; ++i) {
        addCylinder(mesh, spine[i], spine[i + 1], cableRadius, cableRadius, 6, Vec4(0.15f, 0.15f, 0.17f, 1.0f));
    }

    return mesh;
}

NativeModelMesh ProceduralSceneEngine::buildPowerGridNetwork(int postCount, float spanDistance, float cornerAngleDeg) {
    NativeModelMesh combined;
    combined.name = "power_grid_network";

    std::vector<Vec3> polePositions;
    float currentAngle = 0.0f;
    Vec3 curPos(0, 0, 0);
    polePositions.push_back(curPos);

    for (int i = 1; i < postCount; ++i) {
        if (i == 2) currentAngle += cornerAngleDeg * DEG2RAD;
        curPos.x += std::sin(currentAngle) * spanDistance;
        curPos.z += std::cos(currentAngle) * spanDistance;
        polePositions.push_back(curPos);
    }

    for (size_t p = 0; p < polePositions.size(); ++p) {
        NativeModelMesh pole = buildModularPowerPost(12.0f, 0.22f, 2);
        for (const auto& v : pole.vertices) {
            NativeModelVertex shifted = v;
            shifted.position += polePositions[p];
            combined.vertices.push_back(shifted);
            combined.aabb.expand(shifted.position);
        }
        uint32_t offset = static_cast<uint32_t>(combined.indices.size());
        for (uint32_t idx : pole.indices) {
            combined.indices.push_back(offset + idx);
        }
        combined.triangleCount += pole.triangleCount;

        // Span cables to next pole
        if (p + 1 < polePositions.size()) {
            Vec3 p0 = polePositions[p] + Vec3(0, 11.2f, 0);
            Vec3 p1 = polePositions[p + 1] + Vec3(0, 11.2f, 0);
            NativeModelMesh cable = buildCatenaryCableSpan(p0, p1, 1.4f, 0.02f, 20);
            uint32_t cOffset = static_cast<uint32_t>(combined.vertices.size());
            for (const auto& cv : cable.vertices) {
                combined.vertices.push_back(cv);
                combined.aabb.expand(cv.position);
            }
            for (uint32_t cIdx : cable.indices) {
                combined.indices.push_back(cOffset + cIdx);
            }
            combined.triangleCount += cable.triangleCount;
        }
    }

    return combined;
}

NativeModelMesh ProceduralSceneEngine::buildGarage001(float width, float length, float height) {
    NativeModelMesh mesh;
    mesh.name = "magadi_storage_garage";
    // Concrete floor pad
    addBox(mesh, Vec3(0, 0.15f, 0), Vec3(width + 1.0f, 0.3f, length + 1.0f), Vec4(0.35f, 0.35f, 0.36f, 1.0f));
    // Steel warehouse frame walls
    addBox(mesh, Vec3(0, height * 0.5f, 0), Vec3(width, height, length), Vec4(0.55f, 0.58f, 0.62f, 1.0f));
    // Pitched corrugated roof
    addBox(mesh, Vec3(0, height + 0.6f, 0), Vec3(width + 1.4f, 1.2f, length + 1.4f), Vec4(0.70f, 0.32f, 0.18f, 1.0f));
    return mesh;
}

NativeModelMesh ProceduralSceneEngine::buildTestVehicle(float scale) {
    NativeModelMesh mesh;
    mesh.name = "desert_survey_4x4";
    // Main vehicle body chassis
    addBox(mesh, Vec3(0, 0.8f * scale, 0), Vec3(2.0f * scale, 1.1f * scale, 4.2f * scale), Vec4(0.85f, 0.85f, 0.88f, 1.0f));
    // Safari pop-up roof cab
    addBox(mesh, Vec3(0, 1.6f * scale, -0.2f * scale), Vec3(1.7f * scale, 0.8f * scale, 2.4f * scale), Vec4(0.20f, 0.22f, 0.25f, 1.0f));
    // 4 Heavy-duty offroad tires
    for (float tx : {-1.0f * scale, 1.0f * scale}) {
        for (float tz : {-1.4f * scale, 1.4f * scale}) {
            addCylinder(mesh, Vec3(tx, 0.45f * scale, tz), Vec3(tx + (tx > 0 ? 0.3f : -0.3f), 0.45f * scale, tz),
                        0.45f * scale, 0.45f * scale, 10, Vec4(0.10f, 0.10f, 0.10f, 1.0f));
        }
    }
    return mesh;
}

NativeModelMesh ProceduralSceneEngine::buildBasaltRock(float radius, int subdivisions) {
    NativeModelMesh mesh;
    mesh.name = "magadi_basalt_boulder";
    addBox(mesh, Vec3(0, radius * 0.7f, 0), Vec3(radius * 1.8f, radius * 1.4f, radius * 1.6f), Vec4(0.25f, 0.23f, 0.22f, 1.0f));
    return mesh;
}

NativeModelMesh ProceduralSceneEngine::buildAcaciaTree(float height, float crownRadius) {
    NativeModelMesh mesh;
    mesh.name = "dwarf_acacia_tortilis";
    // Trunk
    addCylinder(mesh, Vec3(0, 0, 0), Vec3(0, height * 0.7f, 0), 0.35f, 0.18f, 8, Vec4(0.35f, 0.25f, 0.18f, 1.0f));
    // Flat-topped umbrella foliage canopy
    addCylinder(mesh, Vec3(0, height * 0.7f, 0), Vec3(0, height, 0), crownRadius, crownRadius * 0.7f, 10, Vec4(0.22f, 0.38f, 0.18f, 1.0f));
    return mesh;
}

} // namespace custom_vulkan
