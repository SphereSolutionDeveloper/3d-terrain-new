// ==============================================================================
// Vulkan Road Components C/C++ Engine Module
// Path: native-assets/cpp/vulkan_road_components.h
// Real-world road architecture: Bridge, Toll Plaza, Tunnel, Interchange, Retaining Walls
// Vulkan Scene Standard: Sascha Willems (SaschaWillems/Vulkan) & Khronos Vulkan Samples
// ==============================================================================
#pragma once

#include <vector>
#include <string>
#include <cstdint>
#include <memory>

#include "Math3D.h"
#include "../src/road/bridges/BridgeParams.h"
#include "../src/road/bridges/BridgeGenerator.h"
#include "../src/road/tolls/TollParams.h"
#include "../src/road/tolls/TollPlazaGenerator.h"
#include "../src/road/tunnels/TunnelParams.h"
#include "../src/road/tunnels/TunnelGenerator.h"
#include "../src/road/interchanges/InterchangeParams.h"
#include "../src/road/interchanges/InterchangeGen.h"
#include "../src/road/retaining_walls/RetainingWallParams.h"
#include "../src/road/retaining_walls/RetainingWallGen.h"
#include "../src/road/signage/SignpostParams.h"
#include "../src/road/signage/SignpostGenerator.h"
#include "../src/road/asphalt/AsphaltParams.h"
#include "../src/road/asphalt/ProceduralAsphalt.h"
#include "../src/road/barriers/BarrierParams.h"
#include "../src/road/barriers/BarrierKit.h"
#include "../src/road/roadblocks/RoadBlockParams.h"
#include "../src/road/roadblocks/RoadBlockGenerator.h"
#include "../src/road/vulkan/VulkanRoadScene.h"

namespace custom_vulkan {

struct RoadEngineVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float color[4];
};

struct RoadComponentMeshSummary {
    std::string componentId;
    std::string name;
    std::string subType;
    uint32_t vertexCount;
    uint32_t triangleCount;
    float boundingBoxMin[3];
    float boundingBoxMax[3];
    float lengthMeters;
    float widthMeters;
    float heightMeters;
    float curvatureRadiusM;
    float bankingAngleDeg;
    std::string civilStandard;
    std::string vulkanPipelineConfig;
};

class VulkanRoadComponentEngine {
public:
    VulkanRoadComponentEngine();
    ~VulkanRoadComponentEngine() = default;

    // Bridge Generation with Bending & Size Manipulation
    RoadComponentMeshSummary GenerateBridge(
        ApexAssets::Road::BridgeType type,
        float spanLengthM,
        float deckWidthM,
        float pylonHeightM,
        float curvatureBendDeg,
        float bankingDeg,
        uint32_t cablePairs,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Toll Plaza Generation with Lane & Size Manipulation
    RoadComponentMeshSummary GenerateTollPlaza(
        ApexAssets::Road::TollPlazaType type,
        uint32_t laneCount,
        float laneWidthM,
        float canopyClearanceM,
        float plazaLengthM,
        bool hasDynamicVMS,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Tunnel Generation with Radius, Length, Bending & Jet Fans
    RoadComponentMeshSummary GenerateTunnel(
        ApexAssets::Road::TunnelBoreType type,
        float internalRadiusM,
        float tunnelLengthM,
        float curveBendDeg,
        float jetFanSpacingM,
        bool hasFlaredPortal,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Interchange & Curved Ramp Generation
    RoadComponentMeshSummary GenerateInterchange(
        ApexAssets::Road::InterchangeType type,
        float loopRadiusM,
        float totalArcAngleDeg,
        float rampWidthM,
        float elevationDeltaM,
        float bankingDeg,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Retaining Wall & MSE Panel Generation
    RoadComponentMeshSummary GenerateRetainingWall(
        ApexAssets::Road::RetainingWallType type,
        float wallHeightM,
        float wallLengthM,
        float batterAngleDeg,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Signpost & Overhead Gantry Generation
    RoadComponentMeshSummary GenerateSignpost(
        ApexAssets::Road::SignpostType type,
        float gantrySpanM,
        float clearanceH,
        float boardWidthM,
        uint32_t signCount,
        bool hasDMS,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Expressway Corridor Generation
    RoadComponentMeshSummary GenerateExpressway(
        ApexAssets::Road::ExpresswayType type,
        uint32_t lanesPerDir,
        float sectionLengthM,
        float curveBendDeg,
        float bankingDeg,
        bool hasLighting,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Safety Barrier & Guardrail Generation
    RoadComponentMeshSummary GenerateBarrier(
        ApexAssets::Road::BarrierType type,
        float barrierLengthM,
        float barrierHeightM,
        float curveBendDeg,
        float postSpacingM,
        bool hasReflectors,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Road Block & Security Checkpoint Generation
    RoadComponentMeshSummary GenerateRoadBlock(
        ApexAssets::Road::RoadBlockType type,
        float roadWidthM,
        float zoneLengthM,
        uint32_t unitCount,
        bool hasBeacons,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

    // Full Connected Roadway Scene Assembler
    RoadComponentMeshSummary AssembleConnectedHighwayScene(
        float sceneScale,
        std::vector<RoadEngineVertex>& outVertices,
        std::vector<uint32_t>& outIndices
    );

private:
    ApexAssets::Road::VulkanRoadScene m_sceneOrchestrator;
};

} // namespace custom_vulkan
