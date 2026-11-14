// ==============================================================================
// Modular Road Component: Retaining Wall 3D Mesh Generator
// Path: native-assets/src/road/retaining_walls/RetainingWallGen.h
// ==============================================================================
#pragma once

#include "RetainingWallParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct WallVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA architectural albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Concrete Panel, 1=Rock Gabion, 2=Coping Cap, 3=Weep Hole/Steel, 4=Soil/Backfill
};

struct WallMeshBuffer {
    std::vector<WallVertex> vertices;
    std::vector<uint32_t> indices;
    
    float minAABB[3];
    float maxAABB[3];
    
    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float activeLateralEarthPressureKnM = 0.0f; // Coulomb lateral earth force
};

class RetainingWallGen {
public:
    RetainingWallGen() = default;
    ~RetainingWallGen() = default;

    static WallMeshBuffer GenerateWallMesh(const RetainingWallParams& params);
};

} // namespace Road
} // namespace ApexAssets
