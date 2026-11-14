// ==============================================================================
// Modular Road Component: Signpost & Overhead Gantry 3D Mesh Generator
// Path: native-assets/src/road/signage/SignpostGenerator.h
// Vulkan Architecture: Sascha Willems / Khronos glTF 2.0 PBR Primitives
// ==============================================================================
#pragma once

#include "SignpostParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct SignpostVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA architectural albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Concrete Footing/Roadbed, 1=Galvanized Steel, 2=Sign Face, 3=Warning/Safety, 4=LED Emissive
};

struct SignpostMeshBuffer {
    std::vector<SignpostVertex> vertices;
    std::vector<uint32_t> indices;

    float minAABB[3];
    float maxAABB[3];

    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float windLoadDesignKn = 0.0f;
};

class SignpostGenerator {
public:
    SignpostGenerator() = default;
    ~SignpostGenerator() = default;

    static SignpostMeshBuffer GenerateSignpostMesh(const SignpostParams& params);

private:
    static void BuildCantileverSign(SignpostMeshBuffer& buffer, const SignpostParams& params);
    static void BuildFullSpanTrussGantry(SignpostMeshBuffer& buffer, const SignpostParams& params);
    static void BuildVariableMessageMatrix(SignpostMeshBuffer& buffer, const SignpostParams& params);
    static void BuildRegulatoryChevronSet(SignpostMeshBuffer& buffer, const SignpostParams& params);
    static void BuildButterflySplitSign(SignpostMeshBuffer& buffer, const SignpostParams& params);
};

} // namespace Road
} // namespace ApexAssets
