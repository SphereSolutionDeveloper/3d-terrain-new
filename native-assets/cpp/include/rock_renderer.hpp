#pragma once
#include <vector>
#include <string>
#include <array>
#include <memory>
#include "noise.hpp"

namespace apex {
namespace rendering {

struct Vec3 {
    float x = 0.0f, y = 0.0f, z = 0.0f;
    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator*(float s) const { return {x * s, y * s, z * s}; }
};

struct Vec2 {
    float u = 0.0f, v = 0.0f;
};

struct RockVertex {
    Vec3 position;
    Vec3 normal;
    Vec2 uv;
    float ao = 1.0f; // Ambient Occlusion baked factor
};

struct RockLodMesh {
    int lodLevel = 0; // 0 = High (100%), 1 = Mid (50%), 2 = Low / Collision (25%)
    std::vector<RockVertex> vertices;
    std::vector<uint16_t> indices;
};

struct PbrMaterial {
    std::string name;
    std::array<float, 4> baseColorFactor = {1.0f, 1.0f, 1.0f, 1.0f};
    float metallic = 0.04f;
    float roughness = 0.82f;
    float aoStrength = 1.0f;
    std::array<float, 3> emissiveColor = {0.0f, 0.0f, 0.0f};
    float emissiveStrength = 0.0f;
    float normalScale = 1.0f;
    bool useTriplanar = true;
};

struct GridCell1M {
    int row = 0;
    int col = 0;
    Vec3 worldPosition;       // Center in world meters (-4.5m to +4.5m)
    Vec3 cellSize = {1.0f, 0.0f, 1.0f}; // Exactly 1m x 1m cell
    Vec3 rockScale = {0.8f, 0.7f, 0.8f};
    std::string archetype;
    float massKg = 0.0f;
    float volumeM3 = 0.0f;
    float thermalErosionAmount = 0.15f;
    float hydraulicErosionAmount = 0.20f;
};

class RockRenderer {
public:
    RockRenderer();
    ~RockRenderer() = default;

    // Initializes 10x10 Grid (100 sectors, each 1.0m x 1.0m)
    void initGrid10x10(uint32_t seed = 777);

    // Procedurally generates realistic rock mesh with noise displacement,
    // domain warping, ridged fractures, and thermal/hydraulic erosion.
    void generateRockMesh(
        const std::string& archetype,
        float baseRadius,
        const Vec3& scale,
        std::vector<RockVertex>& outVertices,
        std::vector<uint16_t>& outIndices,
        bool applyErosion = true
    );

    // Generates 3 Levels of Detail (LOD0, LOD1, LOD2)
    std::array<RockLodMesh, 3> generateRockLODs(
        const std::string& archetype,
        float baseRadius,
        const Vec3& scale
    );

    // Triplanar texture coordinate blending based on surface normal
    Vec3 sampleTriplanarColor(
        const Vec3& worldPos,
        const Vec3& normal,
        float textureScale = 1.0f
    ) const;

    // Evaluates PBR Cook-Torrance Microfacet lighting with AO
    Vec3 shadePbrPixel(
        const Vec3& fragPos,
        const Vec3& normal,
        const Vec3& viewDir,
        const Vec3& lightDir,
        const Vec3& lightColor,
        const PbrMaterial& mat,
        float ao = 1.0f
    ) const;

    const std::vector<GridCell1M>& getGridCells() const { return gridCells; }

private:
    procedural::NoiseGenerator noise;
    std::vector<GridCell1M> gridCells;
};

} // namespace rendering
} // namespace apex
