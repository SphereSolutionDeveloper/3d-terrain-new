// ─── app/src/main/cpp/vulkan_asset_models.h ──────────────────────────────────
// Pure C++ 3D Asset Modeling Engine: Zero Kotlin for models or rendering
#pragma once

#include "vulkan_engine.h"
#include "continuous_cable_cylinder.h"
#include <vector>
#include <string>

namespace custom_vulkan {

class VulkanAssetModelFactory {
public:
    // 1. Modular Electric Utility Post (Wood/Steel Mast, Crossarms, Pin Insulators, Transformers)
    static GPUMeshBuffer buildModularPowerPost(
        const std::string& postType,
        bool hasTransformer,
        bool hasStreetlight,
        bool streetlightOn,
        bool hasGuyWire
    );

    // 2. Continuous Catenary Overhead Transmission Cable (Zero Blocks, Exact Cosh Bending)
    static GPUMeshBuffer buildContinuousCatenaryCables(
        const apex::Vec3D& poleA,
        const apex::Vec3D& poleB,
        float tensionKn,
        float linearMassKgM,
        float conductorRadiusM,
        uint32_t radialFacets,
        uint32_t curveSegments,
        bool isEnergized
    );

    // 3. Complete Power Grid Network Corridor (Multi-span posts + 3-Phase Continuous Cables)
    static std::vector<GPUMeshBuffer> buildPowerGridNetwork(
        uint32_t numPosts,
        float spanDistanceM,
        float cornerAngleDeg,
        float tensionKn,
        float linearMassKgM,
        float conductorRadiusM,
        uint32_t radialFacets,
        bool isEnergized
    );

    // 4. Industrial Garage 001 Warehouse (Bays, Structural Columns, Steel Trusses)
    static GPUMeshBuffer buildIndustrialGarage001(
        float bay1OpenPct,
        float bay2OpenPct,
        bool neonSignOn
    );

    // 5. Heavy Transport Vehicle (Chassis, Cab, Wheels, Headlights, Cargo Container)
    static GPUMeshBuffer buildHeavyTransportVehicle(
        bool headlightsOn,
        bool taillightsOn
    );

private:
    static void appendBoxMesh(
        std::vector<EngineVertex>& vertices,
        std::vector<uint32_t>& indices,
        float cx, float cy, float cz,
        float sx, float sy, float sz,
        float r, float g, float b, float a
    );
};

} // namespace custom_vulkan
