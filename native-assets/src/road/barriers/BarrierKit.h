// ==============================================================================
// Modular Road Component: Safety Barrier & Guardrail 3D Mesh Generator
// Path: native-assets/src/road/barriers/BarrierKit.h
// Vulkan Architecture: Sascha Willems / Khronos glTF 2.0 PBR Primitives
// ==============================================================================
#pragma once

#include "BarrierParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct BarrierVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Shoulder Asphalt, 1=Concrete Base/Jersey, 2=Galvanized Steel, 3=Safety/Roller Yellow, 4=Reflector LED
};

struct BarrierMeshBuffer {
    std::vector<BarrierVertex> vertices;
    std::vector<uint32_t> indices;

    float minAABB[3];
    float maxAABB[3];

    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float containmentRatingKj = 0.0f;
};

class BarrierKit {
public:
    BarrierKit() = default;
    ~BarrierKit() = default;

    static BarrierMeshBuffer GenerateBarrierMesh(const BarrierParams& params);

    static void SampleBarrierCurve(
        float t,
        const BarrierParams& params,
        float& outX, float& outY, float& outZ,
        float& outTx, float& outTz
    );
};

} // namespace Road
} // namespace ApexAssets
