// ==============================================================================
// Modular Road Component: Bridge 3D Mesh Generator
// Path: native-assets/src/road/bridges/BridgeGenerator.h
// Vulkan Scene Architecture: Sascha Willems (SaschaWillems/Vulkan)
// Generates parametric meshes with dynamic curvature bending and size manipulation
// ==============================================================================
#pragma once

#include "BridgeParams.h"
#include <vector>
#include <memory>

namespace ApexAssets {
namespace Road {

struct BridgeVertex {
    float pos[3];        // Local space position (X, Y, Z) in meters
    float normal[3];     // Unit surface normal (Nx, Ny, Nz)
    float uv[2];         // Texture UV mapping
    float tangent[4];    // Tangent vector with handedness sign for PBR normal mapping
    float color[4];      // Linear RGBA architectural albedo
    float pbrParams[4];  // Roughness, Metallic, Ambient Occlusion, Emissive Intensity
    uint32_t materialId; // 0=Asphalt, 1=Concrete, 2=Steel/Cable, 3=Paint/Timber, 4=Warning Light
};

struct BridgeMeshBuffer {
    std::vector<BridgeVertex> vertices;
    std::vector<uint32_t> indices;
    
    // Bounding Box (AABB) in meters
    float minAABB[3];
    float maxAABB[3];
    
    // Telemetry & Engineering Metrics
    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float totalConcreteVolumeM3 = 0.0f;
    float totalStructuralSteelTons = 0.0f;
    float deadLoadKn = 0.0f;
};

class BridgeGenerator {
public:
    BridgeGenerator() = default;
    ~BridgeGenerator() = default;

    // Generates complete 3D bridge geometry according to parametric configuration
    static BridgeMeshBuffer GenerateBridgeMesh(const BridgeParams& params);

    // Evaluates 3D curved centerline position and tangent at normalized parameter t [0..1]
    static void SampleCenterline(
        float t,
        const BridgeParams& params,
        float& outX, float& outY, float& outZ,
        float& outTangentX, float& outTangentY, float& outTangentZ,
        float& outBankRad
    );
    // 3D Solid Geometry Primitives for Vulkan Mesh Buffer
    static void AddBox(
        BridgeMeshBuffer& buffer,
        float cx, float cy, float cz,
        float sx, float sy, float sz,
        uint32_t materialId,
        float r = 0.7f, float g = 0.7f, float b = 0.7f
    );

    static void AddOrientedBox(
        BridgeMeshBuffer& buffer,
        float cx, float cy, float cz,
        float dimNormal, float dimVertical, float dimTangent,
        float nx, float nz,
        float tx, float tz,
        uint32_t materialId,
        float r = 0.7f, float g = 0.7f, float b = 0.7f
    );

    static void AddBeam(
        BridgeMeshBuffer& buffer,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float width, float height,
        uint32_t materialId,
        float r = 0.7f, float g = 0.7f, float b = 0.7f
    );

private:
    static void ExtrudeHighwayDeckSegment(
        BridgeMeshBuffer& buffer,
        float t0, float t1,
        const BridgeParams& params
    );

    static void ExtrudePedestrianDeckSegment(
        BridgeMeshBuffer& buffer,
        float t0, float t1,
        const BridgeParams& params
    );

    static void ExtrudeRomanStoneDeckSegment(
        BridgeMeshBuffer& buffer,
        float t0, float t1,
        const BridgeParams& params
    );

    static void ExtrudeTrussDeckSegment(
        BridgeMeshBuffer& buffer,
        float t0, float t1,
        const BridgeParams& params
    );

    static void AddIntermediateSupportPier(
        BridgeMeshBuffer& buffer,
        float t,
        const BridgeParams& params,
        bool isPedestrian
    );

    static void BuildCableStayedPylonsAndCables(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildConcreteViaductPiers(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildSteelTrussMembers(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildParabolicArchRibs(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildSuspensionTowersAndCatenary(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildPedestrianFootbridgeStructure(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildExtradosedPylonsAndStays(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildBasculeDrawbridgeMachinery(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildCantileverTrussSuperstructure(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildCurvedCableStayedPylon(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildRomanMasonryArches(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildPostTensionedOverpassPiers(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildPedestrianStressRibbon(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildPedestrianCableStayed(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildPedestrianTimberArch(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildSteelPlateGirders(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildPedestrianCoveredSkywalk(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );

    static void BuildPedestrianSurfaceConnections(
        BridgeMeshBuffer& buffer,
        const BridgeParams& params
    );
};

} // namespace Road
} // namespace ApexAssets
