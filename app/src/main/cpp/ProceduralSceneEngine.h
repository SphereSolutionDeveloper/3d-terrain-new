// ─── app/src/main/cpp/ProceduralSceneEngine.h ──────────────────────────────────
// Native C++ Procedural 3D Asset & Catenary Scene Engine
#pragma once

#include "Math3D.h"
#include <vector>
#include <string>
#include <cstdint>

namespace custom_vulkan {

struct NativeModelVertex {
    Vec3 position;
    Vec3 normal;
    Vec2 uv;
    Vec4 color = {0.8f, 0.8f, 0.8f, 1.0f};
};

struct NativeModelMesh {
    std::string name;
    std::vector<NativeModelVertex> vertices;
    std::vector<uint32_t> indices;
    NativeAABB aabb;
    uint32_t triangleCount = 0;
};

class ProceduralSceneEngine {
public:
    static ProceduralSceneEngine& getInstance();

    // 1. Modular Electric Power Post with crossarms & insulators
    NativeModelMesh buildModularPowerPost(float heightM = 12.0f,
                                          float poleRadius = 0.22f,
                                          int crossarmLevels = 2);

    // 2. Continuous Catenary Cable with realistic hyperbolic cosine sagging physics
    NativeModelMesh buildCatenaryCableSpan(const Vec3& startPos,
                                          const Vec3& endPos,
                                          float sagM = 1.25f,
                                          float cableRadius = 0.022f,
                                          int segments = 24);

    // 3. Multi-Post Transmission Grid Route with corner angle turn
    NativeModelMesh buildPowerGridNetwork(int postCount = 4,
                                          float spanDistance = 35.0f,
                                          float cornerAngleDeg = 45.0f);

    // 4. Industrial Garage / Hub Facility (Magadi soda storage)
    NativeModelMesh buildGarage001(float width = 16.0f, float length = 24.0f, float height = 8.0f);

    // 5. Desert 4x4 Survey Vehicle (Toyota Land Cruiser chassis)
    NativeModelMesh buildTestVehicle(float scale = 1.0f);

    // 6. Basalt Boulder / Cinder Rock
    NativeModelMesh buildBasaltRock(float radius = 1.5f, int subdivisions = 2);

    // 7. Acacia Tortilis Tree with flat-topped canopy
    NativeModelMesh buildAcaciaTree(float height = 6.0f, float crownRadius = 4.5f);

private:
    ProceduralSceneEngine() = default;

    void addCylinder(NativeModelMesh& mesh, const Vec3& base, const Vec3& top,
                     float radiusBottom, float radiusTop, int radialSegments, const Vec4& color);

    void addBox(NativeModelMesh& mesh, const Vec3& center, const Vec3& size, const Vec4& color);
};

} // namespace custom_vulkan
