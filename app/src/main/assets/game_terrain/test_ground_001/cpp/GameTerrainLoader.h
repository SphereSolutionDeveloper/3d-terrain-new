#pragma once

#include <string>
#include <vector>
#include <cstdint>
#include <memory>

namespace GameEngine {
namespace Terrain {

/**
 * Surface Material Types matching transport_sim_test_001.json
 */
enum class SurfaceType : uint8_t {
    Grass = 0,
    Asphalt = 1,
    Water = 2,
    Mud = 3,
    Gravel = 4,
    Rock = 5,
    Count = 6
};

inline const char* SurfaceTypeToString(SurfaceType type) {
    switch (type) {
        case SurfaceType::Grass: return "grass";
        case SurfaceType::Asphalt: return "asphalt";
        case SurfaceType::Water: return "water";
        case SurfaceType::Mud: return "mud";
        case SurfaceType::Gravel: return "gravel";
        case SurfaceType::Rock: return "rock";
        default: return "unknown";
    }
}

inline SurfaceType StringToSurfaceType(const std::string& str) {
    if (str == "asphalt") return SurfaceType::Asphalt;
    if (str == "water") return SurfaceType::Water;
    if (str == "mud") return SurfaceType::Mud;
    if (str == "gravel") return SurfaceType::Gravel;
    if (str == "rock") return SurfaceType::Rock;
    return SurfaceType::Grass;
}

/**
 * 3D Vector utility
 */
struct Vec3 {
    float x{0.0f};
    float y{0.0f};
    float z{0.0f};

    Vec3() = default;
    Vec3(float x_, float y_, float z_) : x(x_), y(y_), z(z_) {}
};

/**
 * Standard Mesh Vertex for GPU Buffers (Vulkan / Metal / DirectX / OpenGL)
 */
struct TerrainVertex {
    Vec3 position;
    Vec3 normal;
    float uv[2];
    uint8_t surfaceId;
    float splatWeights[4]; // Grass, Asphalt, Mud, Gravel
};

/**
 * 3D Asset Instance placed across the terrain
 */
struct AssetInstance {
    std::string id;
    std::string type;
    std::string glbPath;
    Vec3 position;
    Vec3 rotation;
    Vec3 scale;
};

/**
 * Physics Configuration
 */
struct TerrainPhysics {
    float gravity{-9.81f};
    float defaultFriction{0.85f};
    float mudFriction{0.22f};
    float asphaltFriction{0.85f};
    float gravelFriction{0.65f};
    float waterFriction{0.05f};
};

/**
 * Metadata Block
 */
struct TerrainMetadata {
    std::string mapName{"TEST-001"};
    std::string version{"1.0.0"};
    float cellSizeM{5.0f};
    float heightmapResolutionM{2.0f};
    float mapWidthM{320.0f};
    float mapDepthM{320.0f};
    float baseElevationM{1700.0f};
    float minElevationM{1685.0f};
    float maxElevationM{1740.0f};
    TerrainPhysics physics;
};

/**
 * Complete Game Terrain Dataset
 */
class GameTerrain {
public:
    GameTerrain() = default;
    ~GameTerrain() = default;

    // Load from JSON file (transport_sim_test_001.json)
    bool LoadFromJSON(const std::string& filepath);
    bool LoadFromJSONString(const std::string& jsonContent);

    // High-performance binary format (.bin / .tg1) for instant mobile loading
    bool LoadFromBinary(const std::string& filepath);
    bool SaveToBinary(const std::string& filepath) const;

    // Queries
    float GetElevationAt(float worldX, float worldZ) const;
    SurfaceType GetSurfaceTypeAt(float worldX, float worldZ) const;
    float GetFrictionAt(float worldX, float worldZ) const;
    Vec3 GetNormalAt(float worldX, float worldZ) const;

    // Raycast against terrain mesh (origin + dir), returns distance and hit position
    bool Raycast(const Vec3& rayOrigin, const Vec3& rayDir, float maxDist, Vec3& outHitPoint, Vec3& outNormal) const;

    // Mesh generation for Vulkan / Unreal / Unity / Godot
    void GenerateMeshData(std::vector<TerrainVertex>& outVertices, std::vector<uint32_t>& outIndices, int stepLOD = 1) const;

    // Getters
    const TerrainMetadata& GetMeta() const { return m_meta; }
    int GetGridWidth() const { return m_width; }
    int GetGridDepth() const { return m_depth; }
    const std::vector<float>& GetHeights() const { return m_heights; }
    const std::vector<SurfaceType>& GetSurfaceZones() const { return m_surfaceZones; }
    const std::vector<AssetInstance>& GetAssets() const { return m_assets; }

private:
    TerrainMetadata m_meta;
    int m_width{0};
    int m_depth{0};
    std::vector<float> m_heights;
    std::vector<SurfaceType> m_surfaceZones;
    std::vector<AssetInstance> m_assets;

    void RecomputeBounds();
};

} // namespace Terrain
} // namespace GameEngine
