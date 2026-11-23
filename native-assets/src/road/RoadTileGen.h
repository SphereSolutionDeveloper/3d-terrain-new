// ==============================================================================
// Modular Road Component: Streaming LOD Road Tile & Surface State Generator
// Path: native-assets/src/road/RoadTileGen.h
// Standards: Quadtree Terrain-Conforming Cut/Fill, Drainage & 10-State Surface Physics
// ==============================================================================
#pragma once

#include <cstdint>
#include <vector>

namespace ApexAssets {
namespace Road {

enum class RoadTileType : uint32_t {
    CutFillHillsideTile = 0, // Rock-cut hillside bench + sloped gravel embankment + V-drainage ditches
    QuadtreeUrbanTile,       // Modular city street block tile with granite kerbs, sidewalks & storm grates
    WetPuddleStateTile,      // Rain-slicked asphalt with depressed wheel tracks & reflective water puddles
    MuddyGravelRutTile,      // Unpaved rural haul road with washboard corrugations, mud ruts & rock debris
    PotholedCrackedTile,     // Distressed aged asphalt with sculpted potholes, exposed stone base & patch plates
    SnowyIcyCorridorTile     // Alpine corridor with plowed snow berms, black-ice wheel tracks & marker poles
};

struct RoadTileParams {
    RoadTileType type = RoadTileType::CutFillHillsideTile;
    float tileSizeM = 36.0f;          // Square/longitudinal tile span (24.0m to 72.0m)
    float roadWidthM = 11.0f;         // Active roadway width (8.0m to 16.0m)
    float cutFillHeightM = 4.5f;      // Hillside cut wall / fill embankment height (0.0m to 10.0m)
    float distressDepthM = 0.06f;     // Wheel rut / pothole / washboard depth (0.0m to 0.15m)
    bool hasDrainageAndCurbs = true;  // Concrete V-ditches, storm catch basins, kerbs or snow poles
};

struct RoadTileVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Road Surface, 1=Concrete/Cut Rock/Curb, 2=Grates/Steel, 3=Markings/Aggregate/Snow, 4=Water/Beacons
};

struct RoadTileMeshBuffer {
    std::vector<RoadTileVertex> vertices;
    std::vector<uint32_t> indices;

    float minAABB[3];
    float maxAABB[3];

    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float frictionMu = 0.82f;
    float rollingResistance = 0.015f;
};

class RoadTileGen {
public:
    RoadTileGen() = default;
    ~RoadTileGen() = default;

    static RoadTileMeshBuffer GenerateRoadTileMesh(const RoadTileParams& params);
};

} // namespace Road
} // namespace ApexAssets
