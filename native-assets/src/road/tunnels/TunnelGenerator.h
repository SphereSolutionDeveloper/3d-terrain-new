// ==============================================================================
// Modular Road Component: Tunnel 3D Mesh Generator
// Path: native-assets/src/road/tunnels/TunnelGenerator.h
// Vulkan Scene Architecture: Sascha Willems (SaschaWillems/Vulkan)
// ==============================================================================
#pragma once

#include "TunnelParams.h"
#include <vector>

namespace ApexAssets {
namespace Road {

struct TunnelVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA architectural albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Asphalt Invert, 1=Concrete Arch Lining, 2=Walkway Curb, 3=Jet Fan Steel, 4=LED Strip
};

struct TunnelMeshBuffer {
    std::vector<TunnelVertex> vertices;
    std::vector<uint32_t> indices;
    
    float minAABB[3];
    float maxAABB[3];
    
    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float excavationVolumeM3 = 0.0f;
    float totalVentilationAirflowM3s = 0.0f;
};

class TunnelGenerator {
public:
    TunnelGenerator() = default;
    ~TunnelGenerator() = default;

    static TunnelMeshBuffer GenerateTunnelMesh(const TunnelParams& params);

    static void SampleTunnelCenterline(
        float t,
        const TunnelParams& params,
        float& outX, float& outY, float& outZ,
        float& outTx, float& outTy, float& outTz
    );

private:
    static void ExtrudeTunnelBoreRing(
        TunnelMeshBuffer& buffer,
        float t0, float t1,
        int ringIdx,
        const TunnelParams& params
    );

    static void BuildPortalEntranceHeadwall(
        TunnelMeshBuffer& buffer,
        bool isExit,
        const TunnelParams& params
    );

    static void BuildJetFans(
        TunnelMeshBuffer& buffer,
        const TunnelParams& params
    );

    static void BuildEmergencyNiches(
        TunnelMeshBuffer& buffer,
        const TunnelParams& params
    );
};

} // namespace Road
} // namespace ApexAssets
