// ==============================================================================
// Modular Road Component: Road Network & Signalized Intersection 3D Generator
// Path: native-assets/src/road/RoadNetwork.h
// Standards: MUTCD Chapter 4 (Signalized Junctions) & AASHTO Urban Street Design
// ==============================================================================
#pragma once

#include <cstdint>
#include <vector>
#include <string>

namespace ApexAssets {
namespace Road {

enum class RoadNetworkType : uint32_t {
    Signalized4WayIntersection = 0, // 4-way urban arterial crossing, crosswalks & traffic signal cantilevers
    TJunctionArterial,              // 3-way channelized T-junction with splitter island & acceleration taper
    YForkBifurcation,               // Express highway Y-bifurcation split with chevron gore & crash nose
    DistrictStreetGrid              // 2x2 multi-node connected city block network with avenues & plazas
};

struct RoadNetworkParams {
    RoadNetworkType type = RoadNetworkType::Signalized4WayIntersection;
    float roadWidthM = 12.0f;       // Carriageway width curb-to-curb (8.0m to 18.0m)
    float armSpanM = 56.0f;         // Total network arm footprint width/length (36.0m to 100.0m)
    uint32_t laneCount = 3;         // Lanes across main roadway (2 to 4)
    float cornerRadiusM = 8.0f;     // Fillet curb radius at corners (4.0m to 16.0m)
    bool hasTrafficSignals = true;  // Cantilever traffic signal masts, LED heads & street lamps
};

struct RoadNetworkVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Asphalt, 1=Concrete/Curb/Plaza, 2=Galvanized Steel, 3=Markings/Chevrons, 4=Signal LED
};

struct RoadNetworkMeshBuffer {
    std::vector<RoadNetworkVertex> vertices;
    std::vector<uint32_t> indices;

    float minAABB[3];
    float maxAABB[3];

    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    uint32_t nodeCount = 1;
    uint32_t signalPhaseCount = 4;
};

class RoadNetworkGenerator {
public:
    RoadNetworkGenerator() = default;
    ~RoadNetworkGenerator() = default;

    static RoadNetworkMeshBuffer GenerateNetworkMesh(const RoadNetworkParams& params);
};

} // namespace Road

// Preserved top-level module interface
class RoadNetwork {
public:
    RoadNetwork() = default;
    virtual ~RoadNetwork() = default;
};

} // namespace ApexAssets
