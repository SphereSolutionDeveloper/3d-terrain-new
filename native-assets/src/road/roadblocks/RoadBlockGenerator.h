// ==============================================================================
// Modular Road Component: Road Block 3D Mesh Generator
// Path: native-assets/src/road/roadblocks/RoadBlockGenerator.h
// Vulkan Architecture: Sascha Willems / Khronos glTF 2.0 PBR Primitives
// ==============================================================================
#pragma once

#include "RoadBlockParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct RoadBlockVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Asphalt, 1=Concrete/Heavy Block, 2=Steel/Hardware, 3=Safety Orange/White/Yellow, 4=Emissive Beacon
};

struct RoadBlockMeshBuffer {
    std::vector<RoadBlockVertex> vertices;
    std::vector<uint32_t> indices;

    float minAABB[3];
    float maxAABB[3];

    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float kineticStopRatingKj = 0.0f;
};

class RoadBlockGenerator {
public:
    RoadBlockGenerator() = default;
    ~RoadBlockGenerator() = default;

    static RoadBlockMeshBuffer GenerateRoadBlockMesh(const RoadBlockParams& params);

private:
    static void BuildAsphaltBasePad(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params);
    static void BuildWaterAndJerseyLine(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params);
    static void BuildHydraulicBollardArray(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params);
    static void BuildTacticalSpikeChicane(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params);
    static void BuildConstructionClosureZone(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params);
    static void BuildDragonTeethChicane(RoadBlockMeshBuffer& buffer, const RoadBlockParams& params);
};

} // namespace Road
} // namespace ApexAssets
