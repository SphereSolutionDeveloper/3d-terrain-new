// Kenya Magadi Road (C58) 50km x 50km Terrain System - Vulkan Chunk Architecture
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunk.h
#pragma once

#include <vector>
#include <string>
#include <cstdint>
#include <cmath>

namespace Magadi {

/**
 * Surface types matching the geological zones of Kenya Magadi corridor
 */
enum class MagadiSurfaceType : uint8_t {
    SavannahScrub = 0,     // 650m - 1200m: Sandy clay & dry savannah
    Asphalt = 1,           // C58 B19 Road Corridor: 2-lane bitumen surface
    Gravel = 2,            // Road shoulders & service tracks
    TronaSodaCrust = 3,    // 590m - 620m: Lake Magadi alkaline evaporite crust
    AlkalineMud = 4,       // Lake shores and thermal spring deposits
    EscarpmentRock = 5,    // > 1400m: Basaltic fault scarps & craggy cliffs
    VolcanicAshTalus = 6   // Olorgesailie volcanic cones & loose gravel
};

/**
 * Vertex data structure for Vulkan vertex buffer binding
 * Following Sascha Willems Vulkan vertex layout specifications
 */
struct TerrainVertex {
    float pos[3];          // Position: X, Y (elevation), Z (local meters)
    float normal[3];       // Normal vector (Nx, Ny, Nz) via central differencing
    float uv[2];           // Texture coordinates
    float rawElevation;    // Real-world absolute elevation in meters (590m - 1980m)
    float contourBand;     // Normalized elevation band [0.0 - 1.0]
    float isolineFactor;   // Screen-space contour line factor for fragment shader
    float slopeDeg;        // Surface slope in degrees [0 - 90]
    uint32_t surfaceType;  // MagadiSurfaceType cast to uint32
    float splatWeights[4]; // Savannah, Escarpment, Trona, Road
};

/**
 * Topographic Contour Configuration
 * Contours define how the terrain elevation is shaped and terraced
 */
struct ContourConfig {
    float contourInterval = 25.0f;       // Distance between contour lines (meters)
    float indexContourInterval = 100.0f; // Distance between major/bold contour lines
    float stepTerracingFactor = 0.0f;    // 0.0 = continuous smooth slope, 1.0 = sharp contour terraces
    float scarpSteepness = 1.35f;        // Rift Valley tectonic fault steepening
    bool enableRoadCutEmbankment = true; // Flattens contour slopes for the C58 road
};

/**
 * 3D Axis-Aligned Bounding Box for Frustum Culling
 */
struct ChunkBoundingBox {
    float minX, minY, minZ;
    float maxX, maxY, maxZ;
};

/**
 * Terrain LOD levels
 */
enum class ChunkLOD : uint8_t {
    LOD0_High = 0,   // 65x65 grid (4,225 vertices, 8,192 triangles)
    LOD1_Medium = 1, // 33x33 grid (1,089 vertices, 2,048 triangles)
    LOD2_Low = 2     // 17x17 grid (289 vertices, 512 triangles)
};

/**
 * Magadi Terrain Chunk
 * Represents a single 10km x 10km terrain tile in the 5x5 50km grid
 */
class MagadiTerrainChunk {
public:
    MagadiTerrainChunk(int col, int row, float originX, float originZ, float sizeM = 10000.0f);
    ~MagadiTerrainChunk() = default;

    // Mesh Generation using Contour-guided elevation math
    void GenerateMesh(const ContourConfig& contourConfig, ChunkLOD lod = ChunkLOD::LOD0_High);

    // Queries
    float SampleContourElevation(float worldX, float worldZ, const ContourConfig& cfg) const;
    void CalculateNormal(float worldX, float worldZ, const ContourConfig& cfg, float step, float outNormal[3]) const;
    MagadiSurfaceType DetermineSurfaceType(float worldX, float worldZ, float elev, float distRoad) const;

    // Getters for Vulkan buffer uploading
    const std::vector<TerrainVertex>& GetVertices() const { return m_vertices; }
    const std::vector<uint32_t>& GetIndices() const { return m_indices; }
    uint32_t GetVertexCount() const { return static_cast<uint32_t>(m_vertices.size()); }
    uint32_t GetIndexCount() const { return static_cast<uint32_t>(m_indices.size()); }
    const ChunkBoundingBox& GetBoundingBox() const { return m_bounds; }

    int GetCol() const { return m_col; }
    int GetRow() const { return m_row; }
    float GetMinElevation() const { return m_minElevation; }
    float GetMaxElevation() const { return m_maxElevation; }
    bool HasRoad() const { return m_hasRoad; }

    // Distance to C58 Road Centerline in local meters
    float DistanceToRoadCenterline(float worldX, float worldZ, float& outRoadElev) const;

private:
    int m_col;
    int m_row;
    float m_originX;
    float m_originZ;
    float m_sizeM;
    float m_minElevation = 590.0f;
    float m_maxElevation = 1980.0f;
    bool m_hasRoad = false;

    ChunkBoundingBox m_bounds{};
    std::vector<TerrainVertex> m_vertices;
    std::vector<uint32_t> m_indices;
};

} // namespace Magadi
