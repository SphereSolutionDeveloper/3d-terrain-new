// ==============================================================================
// Modular Road Component: Interchange 3D Mesh Generator
// Path: native-assets/src/road/interchanges/InterchangeGen.h
// ==============================================================================
#pragma once

#include "InterchangeParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct InterchangeVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA architectural albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Asphalt, 1=Concrete Barrier/Pier, 2=Painted Gore/Arrow, 3=Truck Apron, 4=Signage/Steel
};

struct InterchangeMeshBuffer {
    std::vector<InterchangeVertex> vertices;
    std::vector<uint32_t> indices;
    
    float minAABB[3];
    float maxAABB[3];
    
    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float maxLateralCentrifugalG = 0.0f; // Centrifugal force verification (v^2 / R)
};

class InterchangeGen {
public:
    InterchangeGen() = default;
    ~InterchangeGen() = default;

    static InterchangeMeshBuffer GenerateInterchangeMesh(const InterchangeParams& params);

    static void SampleRampCenterline(
        float t,
        const InterchangeParams& params,
        float& outX, float& outY, float& outZ,
        float& outTx, float& outTy, float& outTz,
        float& outBankRad
    );
};

} // namespace Road
} // namespace ApexAssets
