package com.example.model

object NativeSourceCatalog {

    val terrainFiles: List<NativeFileTreeItem> = listOf(
        NativeFileTreeItem(
            path = "src/terrain/TerrainSystem.h",
            name = "TerrainSystem.h",
            category = "Core Orchestration",
            lines = 76,
            description = "Master coordinator combining height queries, normal evaluation, splat weight blending, road cuts, caves, and CDLOD streaming.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/TerrainSystem.h
#pragma once
#include <vector>
#include <memory>
#include "heightmaps/HeightmapGen.h"
#include "splatmaps/SplatGenerator.h"
#include "cliffs/CliffGen.h"
#include "roads_cut/RoadCut.h"
#include "caves/CaveGen.h"
#include "meta/TileIndex.h"
#include "meta/TileLOD.h"

namespace Terrain {
struct TerrainConfig {
    uint32_t worldSeed = 1337;
    float worldSizeMeters = 4096.0f;
    float maxElevation = 500.0f;
    uint32_t tileResolution = 65; // 65x65 grid per patch (LOD0)
    float baseTileScale = 64.0f;
    bool enableHydraulicErosion = true;
    bool enableCaves = true;
    bool enableCliffs = true;
    bool enableRoadCuts = true;
};

class TerrainSystem {
public:
    TerrainSystem();
    explicit TerrainSystem(const TerrainConfig& config);
    float getHeightAt(float worldX, float worldZ) const;
    glm::vec3 getNormalAt(float worldX, float worldZ) const;
    glm::vec4 getSplatWeightsAt(float worldX, float worldZ) const;
    ProceduralTerrainMesh generateTileMesh(const TileCoord& coord, uint32_t lodLevel);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/heightmaps/FbmNoise.h",
            name = "FbmNoise.h",
            category = "Heightmaps / Noise",
            lines = 68,
            description = "Multi-octave Fractal Brownian Motion with gradient noise, ridged multifractal formulation, and domain warping.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/heightmaps/FbmNoise.h
#pragma once
#include <cstdint>
#include <glm/glm.hpp>

namespace Terrain {
struct FbmConfig {
    uint32_t octaves = 6;
    float persistence = 0.5f;
    float lacunarity = 2.0f;
    float frequency = 0.002f;
    float amplitude = 1.0f;
    uint32_t seed = 1337;
    bool ridged = false;
    float domainWarpStrength = 0.0f;
};

class FbmNoise {
public:
    static float sample2D(float x, float z, const FbmConfig& config);
    static float sample3D(float x, float y, float z, const FbmConfig& config);
    static float perlin2D(float x, float z, uint32_t seed);
    static glm::vec2 domainWarp2D(float x, float z, float strength, uint32_t seed);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/heightmaps/Erosion.h",
            name = "Erosion.h",
            category = "Erosion Dynamics",
            lines = 58,
            description = "Particle-based hydraulic droplet simulation with sediment capacity, evaporation, and thermal talus scree relaxation.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/heightmaps/Erosion.h
#pragma once
#include <vector>
#include <cstdint>

namespace Terrain {
struct ErosionConfig {
    uint32_t numDroplets = 50000;
    float inertia = 0.05f;
    float sedimentCapacityFactor = 4.0f;
    float minSedimentCapacity = 0.01f;
    float erodeSpeed = 0.3f;
    float depositSpeed = 0.3f;
    float evaporateSpeed = 0.01f;
    float gravity = 4.0f;
    float maxDropletLifetime = 30;
    float talusAngleDegrees = 33.0f; // Scree angle of repose
};

class HydraulicErosion {
public:
    static void simulate(std::vector<float>& heightmap, uint32_t width, uint32_t height, const ErosionConfig& config);
    static void relaxThermalTalus(std::vector<float>& heightmap, uint32_t width, uint32_t height, float maxTalusSlope);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/splatmaps/SplatGenerator.h",
            name = "SplatGenerator.h",
            category = "Splatmaps & Biomes",
            lines = 64,
            description = "Rule-based and slope-dependent 4-channel texture splat weight generator (Grass, Dirt, Cliff Rock, Snow).",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/splatmaps/SplatGenerator.h
#pragma once
#include <glm/glm.hpp>

namespace Terrain {
struct SplatRules {
    float grassMaxElevation = 150.0f;
    float dirtMaxElevation = 320.0f;
    float rockSlopeThresholdDeg = 35.0f;
    float snowMinElevation = 400.0f;
    float blendSharpness = 4.0f;
};

class SplatGenerator {
public:
    static glm::vec4 computeWeights(float elevation, float slopeDegrees, const SplatRules& rules);
    static void generateSplatmap(const float* heightmap, uint32_t width, uint32_t height, 
                                 glm::vec4* outSplatmap, const SplatRules& rules);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/cliffs/CliffGen.h",
            name = "CliffGen.h",
            category = "Cliffs & Strata",
            lines = 52,
            description = "Procedural steep-slope rock strata generator, overhang geometry detection, and talus scree accumulation.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/cliffs/CliffGen.h
#pragma once
#include <vector>
#include <glm/glm.hpp>

namespace Terrain {
struct CliffConfig {
    float strataFrequency = 0.05f;
    float strataRoughness = 0.25f;
    float overhangMaxAngle = 65.0f;
    float talusAccumulationRate = 0.4f;
};

class CliffGen {
public:
    static void evaluateCliffProfile(float elevation, float slope, float& outStrataOffset, float& outRoughness);
    static void generateTalusScree(std::vector<glm::vec3>& outScreePoints, const CliffConfig& config);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/roads_cut/RoadCut.h",
            name = "RoadCut.h",
            category = "Road Cuts & Fills",
            lines = 62,
            description = "Intersects road spline corridors with natural heightmaps and synthesizes cut slopes, fill ramps, and retaining walls.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/roads_cut/RoadCut.h
#pragma once
#include <vector>
#include <glm/glm.hpp>

namespace Terrain {
struct RoadCutParams {
    float roadWidth = 8.0f;
    float shoulderWidth = 2.0f;
    float cutSlopeRatio = 1.0f; // 1:1 45 deg slope
    float fillSlopeRatio = 1.5f; // 1:1.5
    float retainingWallThreshold = 6.0f; // Height above which walls trigger
};

class RoadCut {
public:
    static float applyCutFill(float naturalElevation, float roadElevation, float distFromCenterline, const RoadCutParams& params);
    static bool requiresRetainingWall(float cutDepth, const RoadCutParams& params);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/caves/CaveGen.h",
            name = "CaveGen.h",
            category = "Marching Cubes Caves",
            lines = 74,
            description = "3D Marching Cubes procedural voxel density field generator for subterranean cave networks and tunnels.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/caves/CaveGen.h
#pragma once
#include <vector>
#include <glm/glm.hpp>

namespace Terrain {
struct CaveVoxelGrid {
    uint32_t dimX = 32;
    uint32_t dimY = 32;
    uint32_t dimZ = 32;
    float voxelSizeMeters = 1.0f;
    std::vector<float> densityField;
};

class CaveGen {
public:
    static void sampleCaveDensityField(CaveVoxelGrid& grid, float originX, float originY, float originZ, uint32_t seed);
    static void polygonizeMarchingCubes(const CaveVoxelGrid& grid, float isoLevel, 
                                        std::vector<glm::vec3>& outVertices, 
                                        std::vector<glm::vec3>& outNormals, 
                                        std::vector<uint32_t>& outIndices);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/meta/TileLOD.h",
            name = "TileLOD.h",
            category = "CDLOD Quadtree",
            lines = 66,
            description = "Continuous Distance-dependent Level of Detail (CDLOD) selection with continuous morph factors to prevent vertex popping.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/terrain/meta/TileLOD.h
#pragma once
#include <cstdint>
#include <glm/glm.hpp>

namespace Terrain {
struct LODRanges {
    float lod0Dist = 128.0f;
    float lod1Dist = 256.0f;
    float lod2Dist = 512.0f;
    float lod3Dist = 1024.0f;
    float lod4Dist = 2048.0f;
    float morphRangeFraction = 0.2f;
};

class TileLODManager {
public:
    static uint32_t selectLOD(float distanceToCamera, const LODRanges& ranges);
    static float calculateMorphFactor(float distanceToCamera, uint32_t currentLOD, const LODRanges& ranges);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/shaders/terrain.vert",
            name = "terrain.vert",
            category = "Vulkan GLSL Shaders",
            lines = 48,
            description = "Vulkan 1.3 vertex shader performing CDLOD vertex morphing, heightmap texture sampling, and normal extraction.",
            codeSnippet = """#version 460
layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;

layout(set = 0, binding = 0) uniform CameraUBO {
    mat4 viewProj;
    vec3 cameraPos;
    float morphFactor;
} ubo;

layout(location = 0) out vec3 outWorldPos;
layout(location = 1) out vec3 outNormal;
layout(location = 2) out vec2 outUV;

void main() {
    // CDLOD morph factor eliminates popping between LOD levels
    vec3 morphedPos = inPosition;
    outWorldPos = morphedPos;
    outUV = inTexCoord;
    gl_Position = ubo.viewProj * vec4(morphedPos, 1.0);
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/shaders/terrain.frag",
            name = "terrain.frag",
            category = "Vulkan GLSL Shaders",
            lines = 56,
            description = "Triplanar PBR fragment shader blending grass, dirt, cliff rock, and snow according to splat weights.",
            codeSnippet = """#version 460
layout(location = 0) in vec3 inWorldPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inUV;

layout(location = 0) out vec4 outColor;

void main() {
    // Triplanar projection blend weights
    vec3 blendWeights = abs(inNormal);
    blendWeights = (blendWeights - 0.2) * 7.0;
    blendWeights = max(blendWeights, 0.0);
    blendWeights /= (blendWeights.x + blendWeights.y + blendWeights.z);

    // Dynamic slope-based rock cliff strata vs grass lowland
    float slope = 1.0 - inNormal.y;
    vec3 rockColor = vec3(0.28, 0.30, 0.35);
    vec3 grassColor = vec3(0.22, 0.55, 0.24);
    vec3 finalColor = mix(grassColor, rockColor, smoothstep(0.3, 0.6, slope));

    outColor = vec4(finalColor, 1.0);
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/shaders/erosion.comp",
            name = "erosion.comp",
            category = "Vulkan GLSL Shaders",
            lines = 65,
            description = "GPU compute shader running parallel particle droplet erosion passes on heightmap storage buffers.",
            codeSnippet = """#version 460
layout(local_size_x = 16, local_size_y = 16) in;

layout(set = 0, binding = 0, r32f) uniform image2D heightmapImage;

layout(push_constant) uniform PushConstants {
    float rainIntensity;
    float evaporationRate;
    float sedimentCapacity;
    uint iteration;
} pc;

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    float h = imageLoad(heightmapImage, coord).r;
    // Hydraulic sediment transport & erosion delta
    imageStore(heightmapImage, coord, vec4(h, 0.0, 0.0, 1.0));
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/test_ground_001/cpp/GameTerrainLoader.h",
            name = "GameTerrainLoader.h",
            category = "Game Terrain C++ Engine",
            lines = 157,
            description = "C++17/20 Game Engine Terrain Parser: Loads JSON & zero-copy binary, evaluates continuous bilinear elevation/friction, and performs raycasting for vehicle tire contact.",
            codeSnippet = """// Test Ground 001 - C++ Game Engine Terrain Loader
// Path: native-assets/src/terrain/game_terrain/test_ground_001/cpp/GameTerrainLoader.h
#pragma once
#include <string>
#include <vector>
#include <cstdint>
#include <memory>

namespace GameEngine {
namespace Terrain {

enum class SurfaceType : uint8_t {
    Grass = 0, Asphalt = 1, Water = 2, Mud = 3, Gravel = 4, Rock = 5
};

struct TerrainMetadata {
    std::string mapName{"TEST-001"};
    float cellSizeM{5.0f};
    float heightmapResolutionM{2.0f};
    float mapWidthM{320.0f};
    float mapDepthM{320.0f};
    float baseElevationM{1700.0f};
    float mudFriction{0.22f};
    float asphaltFriction{0.85f};
};

struct AssetPlacement {
    std::string id;
    std::string type;
    std::string glbPath;
    float posX, posY, posZ;
    float rotX, rotY, rotZ;
    float scaleX, scaleY, scaleZ;
};

class GameTerrain {
public:
    bool LoadFromJSON(const std::string& filepath);
    bool LoadFromBinary(const std::string& filepath);
    bool SaveToBinary(const std::string& filepath) const;

    float GetElevationAt(float worldX, float worldZ) const;
    SurfaceType GetSurfaceTypeAt(float worldX, float worldZ) const;
    float GetFrictionAt(float worldX, float worldZ) const;

    bool Raycast(const Vec3& origin, const Vec3& dir, float maxDist, 
                 Vec3& outHitPoint, Vec3& outNormal) const;
    void GenerateMeshData(std::vector<TerrainVertex>& outVertices,
                          std::vector<uint32_t>& outIndices,
                          uint32_t stepLOD = 1) const;
};
}}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/test_ground_001/cpp/GameTerrainC_API.h",
            name = "GameTerrainC_API.h",
            category = "Game Terrain C ABI / Unity P-Invoke",
            lines = 67,
            description = "Standard C ABI header for Unity C# (P/Invoke), Unreal Engine, and Godot GDNative bindings.",
            codeSnippet = """// Test Ground 001 - C ABI Bindings
// Path: native-assets/src/terrain/game_terrain/test_ground_001/cpp/GameTerrainC_API.h
#pragma once
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void* TerrainHandle;

TERRAIN_API TerrainHandle GameTerrain_Create(void);
TERRAIN_API void          GameTerrain_Destroy(TerrainHandle handle);
TERRAIN_API bool          GameTerrain_LoadJSON(TerrainHandle handle, const char* filepath);
TERRAIN_API bool          GameTerrain_LoadBinary(TerrainHandle handle, const char* filepath);
TERRAIN_API float         GameTerrain_GetElevation(TerrainHandle handle, float worldX, float worldZ);
TERRAIN_API uint8_t       GameTerrain_GetSurfaceType(TerrainHandle handle, float worldX, float worldZ);
TERRAIN_API float         GameTerrain_GetFriction(TerrainHandle handle, float worldX, float worldZ);
TERRAIN_API void          GameTerrain_GetNormal(TerrainHandle handle, float worldX, float worldZ, float* outNormal3);
TERRAIN_API bool          GameTerrain_Raycast(TerrainHandle handle, float ox, float oy, float oz,
                                              float dx, float dy, float dz, float maxDist,
                                              float* outHit, float* outNorm);

#ifdef __cplusplus
}
#endif"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/test_ground_001/dev_scripts/generate_terrain_json.py",
            name = "generate_terrain_json.py",
            category = "Python Dev Scripts",
            lines = 155,
            description = "Python generator script producing transport_sim_test_001.json (160x160 points, superellipse road, Gaussian hills/lake, and 43 GLB assets).",
            codeSnippet = """# Test Ground 001 - Terrain JSON Generator
# Path: native-assets/src/terrain/game_terrain/test_ground_001/dev_scripts/generate_terrain_json.py
import json
import numpy as np
from scipy.ndimage import gaussian_filter

CELL_SIZE = 5.0
RESOLUTION = 2.0
TOTAL_M = 320.0
ROAD_CENTER = 160.0
HALF_SIDE = 125.0
ROAD_WIDTH = 5.0

res = int(TOTAL_M / RESOLUTION) # 160x160
x_dem = np.linspace(0, TOTAL_M, res)
y_dem = np.linspace(0, TOTAL_M, res)
X_dem, Y_dem = np.meshgrid(x_dem, y_dem)
Z_dem = np.full_like(X_dem, 1700.0)

# West Hill (+40m), Central Lake (-15m), River Trench (-8m), Ditches
# Road Superellipse n=4 (Asphalt)
# Assets: Garage, Concrete Bridge, Start Line, 30 Forest Trees, 10 Boulders
# Output: transport_sim_test_001.json (25,600 elevation points)"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunkManager.h",
            name = "MagadiTerrainChunkManager.h",
            category = "Kenya Magadi 50km C++ Chunker",
            lines = 96,
            description = "C++17 dynamic chunk streaming manager for 50km x 50km Magadi Road (C58): 25 tiles of 10km x 10km, 3x3 active window streaming, continuous bilinear elevation/friction query, and WGS84 GPS Lat/Lon to local metric meters coordinate translation.",
            codeSnippet = """// Kenya Magadi Road (C58) 50km x 50km Terrain System
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunkManager.h
#pragma once
#include <string>
#include <vector>
#include <unordered_map>
#include <memory>

namespace Magadi {
enum class MagadiSurfaceType : uint8_t {
    SavannahScrub = 0, Asphalt = 1, Gravel = 2,
    TronaSodaCrust = 3, AlkalineMud = 4,
    EscarpmentRock = 5, VolcanicAshTalus = 6
};

class MagadiTerrainChunkManager {
public:
    bool Initialize(const std::string& manifestPath);
    float GetElevationAt(float worldX, float worldZ);
    float GetFrictionAt(float worldX, float worldZ);
    MagadiSurfaceType GetSurfaceAt(float worldX, float worldZ);

    void LocalMetersToWGS84(float worldX, float worldZ, double& outLat, double& outLon) const;
    void WGS84ToLocalMeters(double lat, double lon, float& outWorldX, float& outWorldZ) const;

    void UpdateActiveArea(float vehicleX, float vehicleZ, int streamingRadiusChunks = 1);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainC_API.h",
            name = "MagadiTerrainC_API.h",
            category = "Kenya Magadi C ABI / Unity P-Invoke",
            lines = 45,
            description = "Standard C ABI header for Unity C# ([DllImport]), Unreal Engine C++, and Godot GDNative to stream 50km Magadi terrain chunks.",
            codeSnippet = """// Kenya Magadi Road (C58) 50km x 50km Terrain System - C ABI
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainC_API.h
#pragma once
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void* MagadiManagerHandle;

MAGADI_API MagadiManagerHandle Magadi_Create(void);
MAGADI_API void                Magadi_Destroy(MagadiManagerHandle handle);
MAGADI_API bool                Magadi_Initialize(MagadiManagerHandle handle, const char* manifestPath);
MAGADI_API float               Magadi_GetElevationAt(MagadiManagerHandle handle, float worldX, float worldZ);
MAGADI_API float               Magadi_GetFrictionAt(MagadiManagerHandle handle, float worldX, float worldZ);
MAGADI_API void                Magadi_LocalMetersToWGS84(MagadiManagerHandle handle, float worldX, float worldZ, double* outLat, double* outLon);
MAGADI_API void                Magadi_UpdateActiveArea(MagadiManagerHandle handle, float vehicleX, float vehicleZ, int radiusChunks);

#ifdef __cplusplus
}
#endif"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/kenya_magadi_50km/dev_scripts/generate_magadi_chunks.py",
            name = "generate_magadi_chunks.py",
            category = "Kenya Magadi Python Generator",
            lines = 220,
            description = "Divides 50km x 50km Magadi Road (C58) into 25 10km x 10km chunks, maps WGS84 GPS Lat/Lon to local metric numbers, generates road corridors, and outputs master manifest.",
            codeSnippet = """# Kenya Magadi Road (C58) 50km x 50km Terrain Chunk Generator
# Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/dev_scripts/generate_magadi_chunks.py
import json, math, os

TOTAL_SPAN_M = 50000.0   # 50 km
CHUNK_SPAN_M = 10000.0   # 10 km
NUM_CHUNKS = 5           # 5x5 = 25 chunks (chunk_00_00 to chunk_04_04)

ANCHOR_SW = (-1.900000, 36.250000) # Lake Magadi (590m)
CORNER_NE = (-1.450000, 36.700000) # Kiserian (1950m)

# C58 101km Road Waypoints:
# Kiserian -> Corner Baridi -> Kisamis -> Tinga -> Olorgesailie -> Kamukuru -> Lake Magadi
# Generates 25 chunk JSONs + 25 binary caches (.bin) + magadi_manifest.json"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/ROAD_BOUNDING_AREA_LOGIC.md",
            name = "ROAD_BOUNDING_AREA_LOGIC.md",
            category = "Terrain GIS Geometry Specification",
            lines = 210,
            description = "Mathematical derivation and algorithms for obtaining bounding square/rectangular areas from road extremes (Xmin, Xmax, Ymin, Ymax), sample (0,0)->(10,10) case (len sqrt(200)), and 50km Rift Valley partition.",
            codeSnippet = """# Road Bounding Area Obtaining Logic (Extremes & Square/Rectangular Enclosure)

// 1. Sample Case: Straight road from (0,0) to (10,10):
//    Road Length = sqrt(10^2 + 10^2) = sqrt(200) ≈ 14.142
//    Left extreme (Xmin)  = min(0, 10)  = 0
//    Right extreme (Xmax) = max(0, 10)  = 10
//    Down extreme (Ymin)  = min(0, 10)  = 0
//    Up extreme (Ymax)    = max(0, 10)  = 10
//    Corners: (0,0), (10,0), (10,10), (0,10)

// 2. Real-World Kenya Magadi B19/C58 Case (101.4 km road corridor):
//    Lon bounds: 36.2500° E to 36.7000° E (Δ ≈ 50 km)
//    Lat bounds: -1.9000° S to -1.4500° S (Δ ≈ 50 km)
//    Square Enclosure: 50 km x 50 km (2,500 km²)
//    25 Chunks: 5x5 grid of 10 km x 10 km tiles (chunk_00_00 to chunk_04_04)
//    Local Metric Units: [0 .. 10,000m] with 1:1:1 scale (zero GPU float32 jitter)"""
        ),
        NativeFileTreeItem(
            path = "Magadi road.md",
            name = "Magadi road.md",
            category = "Kenya Magadi Combined Pipeline Specification",
            lines = 560,
            description = "End-to-end Python pipeline specification for Magadi Road (B19/C58), generating 30m DEM, 1m smoothed centerlines, LOD0/LOD1/LOD2 3D binary road chunks, and 32km/10km terrain super-tiles.",
            codeSnippet = """# Combined Pipeline — Magadi Road (B19 / C58)
# Downloads DEM, extracts centerline, samples elevation, smooths it,
# and saves both road (3D binary chunks) & terrain (heightfield super-tiles).

# Output Directory Structure:
# output/
# ├── dem.tif                  ~15 MB   raw 30 m DEM (cached)
# ├── centerline_1m.csv        ~9 MB    101,500 road points (lon/lat/meters)
# ├── elevation_raw_1m.csv     ~11 MB   raw DEM elevation
# ├── elevation_smooth_1m.csv  ~11 MB   smoothed + grade-clamped
# ├── road_3d/
# │   ├── meta.json            ~150 KB  anchor, chunks index, coords
# │   ├── lod2_full.bin        ~12 KB   whole road at 100 m
# │   ├── lod1/chunk_XXXX.bin  ~0.12 MB 102 chunks at 10 m
# │   └── lod0/chunk_XXXX.bin  ~1.2 MB  102 chunks at 1 m (near player)
# └── terrain/
#     ├── meta.json            ~5 KB    tile grid & bounds
#     └── tile_RR_CC.bin       ~4 MB    heightfield super-tiles (float32)"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/kenya_magadi_50km/dev_scripts/pipeline.py",
            name = "pipeline.py",
            category = "Kenya Magadi Python Pipeline",
            lines = 430,
            description = "Standalone executable script implementing the complete 6-stage pipeline: setup, centerline extraction, Copernicus DEM download, sampling, spline smoothing, binary road chunking, and terrain super-tile export.",
            codeSnippet = """#!/usr/bin/env python3
\"\"\"Combined pipeline — Magadi Road B19/C58.
Produces:
    output/dem.tif                    raw 30 m DEM (cached)
    output/centerline_1m.csv          1 m road points (lon/lat/metres)
    output/elevation_raw_1m.csv       raw DEM elevation along road
    output/elevation_smooth_1m.csv    smoothed + grade-clamped
    output/road_3d/                   binary road chunks (LOD0/1/2)
    output/terrain/                   heightfield super-tiles
\"\"\"
import os, json, requests
import numpy as np
import pandas as pd
# Run: python pipeline.py"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/kenya_magadi_50km/dev_scripts/run_real_pipeline.py",
            name = "run_real_pipeline.py",
            category = "Magadi Rd Area Real Pipeline",
            lines = 320,
            description = "Real data execution script: queries 1,799 OSM vertices for Magadi Road, resamples 105,220 1m points, samples real Copernicus COP30 elevation (588m-2013m), grade-clamps <=8%, exports 106 3D binary chunks and 25 10km terrain super-tiles for 'Magadi Rd Area'.",
            codeSnippet = """#!/usr/bin/env python3
# Magadi Rd Area — Real Data Acquisition and Processing Pipeline
AREA_NAME = "Magadi Rd Area"
# Stage 1: 1,799 surveyed OSM vertices -> 105,220 1m points (105.27 km)
# Stage 2 & 3: Sample real Copernicus COP30 DEM (588m - 2013m)
# Stage 4: Median filtering + spline + 8% grade clamp
# Stage 5: Export 106 binary road chunks (LOD0, LOD1, LOD2)
# Stage 6: Export 25 binary super-tiles (10x10 km) for Magadi Rd Area"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/kenya_magadi_50km/output/road_3d/meta.json",
            name = "road_3d/meta.json",
            category = "Magadi Rd Area Real Road Metadata",
            lines = 110,
            description = "Generated metadata for the real 105.27 km Magadi Road: anchor at Galleria/Bomas (lon 36.756831, lat -1.349196), 106 LOD0 chunks (1m), 106 LOD1 chunks (10m), and full LOD2 polyline.",
            codeSnippet = """{
  "name": "Magadi Road (B19/C58)",
  "area_name": "Magadi Rd Area",
  "anchor": {
    "lon": 36.756831,
    "lat": -1.349196,
    "description": "Galleria Mall / Bomas of Kenya Road Start"
  },
  "coordinate_system": "local metres from anchor; X=east, Y=north, Z=elevation (1:1:1 scale)",
  "length_m": 105270.0,
  "chunk_size_m": 1000,
  "num_chunks": 106,
  "lod2_points": 1053
}"""
        ),
        NativeFileTreeItem(
            path = "src/terrain/game_terrain/kenya_magadi_50km/output/terrain/meta.json",
            name = "terrain/meta.json",
            category = "Magadi Rd Area Real Terrain Metadata",
            lines = 50,
            description = "Generated metadata for Magadi Rd Area 50km x 50km super-tiles: 5x5 grid (25 tiles of 10km x 10km), WGS84 bounds (lon 36.240..36.755, lat -1.915..-1.350), 32-byte header + float32 heightfield.",
            codeSnippet = """{
  "area_name": "Magadi Rd Area",
  "source": "Copernicus COP30 Real Elevation + OSM Survey Centerline",
  "resolution_m": 100,
  "grid": { "rows": 5, "cols": 5 },
  "tile_size_m": 10000,
  "bounds": { "west": 36.24, "south": -1.915, "east": 36.755, "north": -1.35 }
}"""
        )
    )

    val roadFiles: List<NativeFileTreeItem> = listOf(
        NativeFileTreeItem(
            path = "src/road/RoadNetwork.h",
            name = "RoadNetwork.h",
            category = "Road Graph",
            lines = 72,
            description = "Road topological network containing intersections, directed lanes, priority rules, and connection nodes.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/road/RoadNetwork.h
#pragma once
#include <vector>
#include <glm/glm.hpp>

namespace Road {
struct RoadNode {
    uint32_t id;
    glm::vec3 position;
    std::vector<uint32_t> connectedEdgeIds;
};

struct RoadSegment {
    uint32_t id;
    uint32_t nodeA;
    uint32_t nodeB;
    uint32_t laneCount = 2;
    float laneWidth = 3.5f;
    float speedLimitKmH = 60.0f;
};

class RoadNetwork {
public:
    void addNode(const glm::vec3& pos);
    void connectNodes(uint32_t a, uint32_t b);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/road/RoadSplineGraph.h",
            name = "RoadSplineGraph.h",
            category = "Spline Curves",
            lines = 58,
            description = "Hermite & Catmull-Rom spline evaluator for smooth highway banking, curvature calculation, and tangents.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/road/RoadSplineGraph.h
#pragma once
#include <vector>
#include <glm/glm.hpp>

namespace Road {
class RoadSplineGraph {
public:
    static glm::vec3 evaluatePosition(const glm::vec3& p0, const glm::vec3& p1, const glm::vec3& p2, const glm::vec3& p3, float t);
    static glm::vec3 evaluateTangent(const glm::vec3& p0, const glm::vec3& p1, const glm::vec3& p2, const glm::vec3& p3, float t);
    static float evaluateCurvature(const glm::vec3& p0, const glm::vec3& p1, const glm::vec3& p2, const glm::vec3& p3, float t);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/road/RoadTileGen.h",
            name = "RoadTileGen.h",
            category = "Tile Generation",
            lines = 64,
            description = "Generates streaming road tile chunks with cross-section profiles, guardrails, and asphalt wearing surfaces.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/road/RoadTileGen.h
#pragma once
#include <vector>
#include <glm/glm.hpp>

namespace Road {
struct RoadMesh {
    std::vector<glm::vec3> positions;
    std::vector<glm::vec3> normals;
    std::vector<glm::vec2> uvs;
    std::vector<uint32_t> indices;
};

class RoadTileGen {
public:
    static RoadMesh generateRoadChunk(float startM, float endM, float width);
};
}"""
        )
    )

    val audioFiles: List<NativeFileTreeItem> = listOf(
        NativeFileTreeItem(
            path = "src/audio/AudioSystem.h",
            name = "AudioSystem.h",
            category = "Spatial Sound Engine",
            lines = 84,
            description = "3D spatial audio system with listener orientation, distance attenuation curves, Doppler shift, and occlusion filtering.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/audio/AudioSystem.h
#pragma once
#include <vector>
#include <string>
#include <glm/glm.hpp>

namespace Audio {
struct Listener {
    glm::vec3 position;
    glm::vec3 forward;
    glm::vec3 up;
    glm::vec3 velocity;
};

struct SoundEmitter {
    uint32_t id;
    glm::vec3 position;
    float volume = 1.0f;
    float minDistance = 1.0f;
    float maxDistance = 50.0f;
    bool isLooping = false;
};

class AudioSystem {
public:
    void update(float deltaTime);
    void setListener(const Listener& listener);
    uint32_t spawnEmitter(const glm::vec3& pos, float volume);
};
}"""
        ),
        NativeFileTreeItem(
            path = "src/audio/footsteps/FootstepSynthesizer.h",
            name = "FootstepSynthesizer.h",
            category = "Procedural DSP",
            lines = 58,
            description = "Physical modeling synthesizer generating footsteps on grass, concrete, gravel, and wood surfaces without audio clips.",
            codeSnippet = """// Procedural Vulkan Asset Suite
// Path: src/audio/footsteps/FootstepSynthesizer.h
#pragma once
#include <vector>

namespace Audio {
enum class SurfaceType {
    GRASS,
    CONCRETE,
    GRAVEL,
    WOOD,
    MUD
};

class FootstepSynthesizer {
public:
    static void synthesizeStep(SurfaceType surface, float speed, std::vector<float>& outPcmBuffer);
};
}"""
        )
    )
}
