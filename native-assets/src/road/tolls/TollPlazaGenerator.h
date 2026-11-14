// ==============================================================================
// Modular Road Component: Toll Plaza 3D Mesh Generator
// Path: native-assets/src/road/tolls/TollPlazaGenerator.h
// Vulkan Scene Architecture: Sascha Willems (SaschaWillems/Vulkan)
// ==============================================================================
#pragma once

#include "TollParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct TollVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA architectural albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Concrete/Asphalt Apron, 1=Steel Canopy, 2=Glass/Booth, 3=Safety/Striping, 4=LED Sign
};

struct TollMeshBuffer {
    std::vector<TollVertex> vertices;
    std::vector<uint32_t> indices;
    
    float minAABB[3];
    float maxAABB[3];
    
    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float peakTrafficThroughputVph = 0.0f; // Vehicles per hour theoretical capacity
    float totalPlazaFootprintM2 = 0.0f;
};

class TollPlazaGenerator {
public:
    TollPlazaGenerator() = default;
    ~TollPlazaGenerator() = default;

    static TollMeshBuffer GenerateTollMesh(const TollParams& params);

private:
    static void BuildConcreteApronAndIslands(TollMeshBuffer& buffer, const TollParams& params);
    static void BuildCanopyRoofStructure(TollMeshBuffer& buffer, const TollParams& params);
    static void BuildBoothsAndBarriers(TollMeshBuffer& buffer, const TollParams& params);
    static void BuildOverheadETCGantry(TollMeshBuffer& buffer, const TollParams& params);
    static void BuildVMSLaneSignals(TollMeshBuffer& buffer, const TollParams& params);
    static void BuildWIMScalesAndBypass(TollMeshBuffer& buffer, const TollParams& params);
    static void BuildBorderInspectionBays(TollMeshBuffer& buffer, const TollParams& params);
};

} // namespace Road
} // namespace ApexAssets
