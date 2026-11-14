// ==============================================================================
// Vulkan 1.3 Road Scene Engine & Modular Component Assembler
// Path: native-assets/src/road/vulkan/VulkanRoadScene.h
// Architecture: Sascha Willems Vulkan Framework & Khronos Vulkan Samples
// ==============================================================================
#pragma once

#include <cstdint>
#include <string>
#include <vector>
#include <memory>

#include "../bridges/BridgeGenerator.h"
#include "../tolls/TollPlazaGenerator.h"
#include "../tunnels/TunnelGenerator.h"
#include "../interchanges/InterchangeGen.h"
#include "../retaining_walls/RetainingWallGen.h"
#include "../signage/SignpostGenerator.h"
#include "../barriers/BarrierKit.h"

namespace ApexAssets {
namespace Road {

struct VulkanPipelineSpecs {
    std::string apiVersion = "Vulkan 1.3.268";
    std::string shaderModel = "GLSL 4.60 SPIR-V";
    std::string polygonMode = "VK_POLYGON_MODE_FILL";
    std::string msaaSamples = "VK_SAMPLE_COUNT_4_BIT";
    bool dynamicRenderingEnabled = true;
    bool pipelineCaching = true;
    bool validationLayers = true;
    uint32_t descriptorSetsCount = 4;
    uint32_t pushConstantBytes = 128;
};

// Unified Vertex format for the entire Vulkan scene buffer
struct VulkanSceneVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA albedo
    float pbrParams[4];  // Roughness, Metallic, Ambient Occlusion, Emissive
    uint32_t materialId; // 0=Asphalt, 1=Concrete, 2=Steel, 3=Jersey Barrier, 4=LED/Emissive
};

// Uniform Buffer Object (UBO) matching Sascha Willems Vulkan scene standard
struct VulkanSceneUBO {
    float projection[16];
    float view[16];
    float model[16];
    float cameraPos[4];
    float lightDirection[4];
    float lightColor[4];
    float timeSeconds;
    float wetnessFrictionFactor;
    float tunnelLuminanceBoost;
    float padding;
};

// Push Constants layout (128 bytes limit)
struct VulkanRoadPushConstants {
    float transformMatrix[16]; // 64 bytes
    float bendCurvature;       // 4 bytes
    float superelevationBank;  // 4 bytes
    float componentScale;      // 4 bytes
    uint32_t componentType;    // 4 bytes: 0=Road, 1=Bridge, 2=Toll, 3=Tunnel, 4=Interchange, 5=Wall
    float pbrOverride[4];      // 16 bytes
    float customParams[8];     // 32 bytes
};

struct VulkanAssembledScene {
    std::vector<VulkanSceneVertex> vertices;
    std::vector<uint32_t> indices;
    
    // Total Scene Telemetry
    uint32_t totalVertices = 0;
    uint32_t totalTriangles = 0;
    float totalRoadLengthKm = 0.0f;
    float estimatedVramMegabytes = 0.0f;
    
    // Sub-component offsets in the unified vertex buffer
    uint32_t bridgeVertexOffset = 0;
    uint32_t bridgeIndexOffset = 0;
    uint32_t tollVertexOffset = 0;
    uint32_t tollIndexOffset = 0;
    uint32_t tunnelVertexOffset = 0;
    uint32_t tunnelIndexOffset = 0;
    uint32_t interchangeVertexOffset = 0;
    uint32_t interchangeIndexOffset = 0;
    uint32_t wallVertexOffset = 0;
    uint32_t wallIndexOffset = 0;
};

class VulkanRoadScene {
public:
    VulkanRoadScene();
    ~VulkanRoadScene() = default;

    // Assembles complete connected real-world roadway scene:
    // Highway corridor -> Toll Plaza -> Sweeping curve ramp -> Cable-stayed bridge -> Mountain Tunnel Portal
    VulkanAssembledScene AssembleFullConnectedScene(
        const BridgeParams& bridgeParams,
        const TollParams& tollParams,
        const TunnelParams& tunnelParams,
        const InterchangeParams& interchangeParams,
        const RetainingWallParams& wallParams
    );

    // Queries Vulkan pipeline architecture and capabilities
    VulkanPipelineSpecs GetPipelineSpecs() const;

private:
    VulkanPipelineSpecs m_specs;
};

} // namespace Road
} // namespace ApexAssets
