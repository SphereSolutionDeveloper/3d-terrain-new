// ==============================================================================
// Modular Road Component: Expressway 3D Mesh Generator
// Path: native-assets/src/road/asphalt/ProceduralAsphalt.h
// Vulkan Architecture: Sascha Willems / Khronos glTF 2.0 PBR Primitives
// ==============================================================================
#pragma once

#include "AsphaltParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct ExpresswayVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Asphalt, 1=Concrete Barrier/Curb/Pier, 2=Galvanized Steel, 3=Lane Paint/Rock, 4=LED Luminaire
};

struct ExpresswayMeshBuffer {
    std::vector<ExpresswayVertex> vertices;
    std::vector<uint32_t> indices;

    float minAABB[3];
    float maxAABB[3];

    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float designCapacityVph = 0.0f;
};

class ProceduralAsphalt {
public:
    ProceduralAsphalt() = default;
    ~ProceduralAsphalt() = default;

    static ExpresswayMeshBuffer GenerateExpresswayMesh(const AsphaltParams& params);

    static void SampleExpresswayCenterline(
        float t,
        const AsphaltParams& params,
        float& outX, float& outY, float& outZ,
        float& outTx, float& outTy, float& outTz,
        float& outBankRad
    );
};

} // namespace Road
} // namespace ApexAssets
