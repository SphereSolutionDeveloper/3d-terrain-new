// ==============================================================================
// Vulkan Road Components C/C++ Engine Implementation
// Path: native-assets/cpp/vulkan_road_components.cpp
// ==============================================================================
#include "vulkan_road_components.h"
#include <cmath>
#include <algorithm>

namespace custom_vulkan {

VulkanRoadComponentEngine::VulkanRoadComponentEngine() {
}

template <typename SrcVertex>
static void CopyColorOrFallback(const SrcVertex& src, RoadEngineVertex& dst) {
    if (src.color[3] > 0.01f) {
        dst.color[0] = src.color[0];
        dst.color[1] = src.color[1];
        dst.color[2] = src.color[2];
        dst.color[3] = src.color[3];
        return;
    }
    switch (src.materialId) {
        case 0: dst.color[0] = 0.22f; dst.color[1] = 0.24f; dst.color[2] = 0.27f; dst.color[3] = 1.0f; break;
        case 1: dst.color[0] = 0.65f; dst.color[1] = 0.68f; dst.color[2] = 0.72f; dst.color[3] = 1.0f; break;
        case 2: dst.color[0] = 0.85f; dst.color[1] = 0.88f; dst.color[2] = 0.92f; dst.color[3] = 1.0f; break;
        case 3: dst.color[0] = 0.85f; dst.color[1] = 0.72f; dst.color[2] = 0.16f; dst.color[3] = 1.0f; break;
        case 4: dst.color[0] = 0.10f; dst.color[1] = 0.95f; dst.color[2] = 0.40f; dst.color[3] = 1.0f; break;
        default: dst.color[0] = 0.50f; dst.color[1] = 0.50f; dst.color[2] = 0.50f; dst.color[3] = 1.0f; break;
    }
}

static void ComputeEngineAABB(const std::vector<RoadEngineVertex>& verts, float minAABB[3], float maxAABB[3]) {
    if (verts.empty()) {
        for (int i = 0; i < 3; ++i) { minAABB[i] = 0.0f; maxAABB[i] = 0.0f; }
        return;
    }
    for (int i = 0; i < 3; ++i) { minAABB[i] = maxAABB[i] = verts[0].pos[i]; }
    for (const auto& v : verts) {
        for (int i = 0; i < 3; ++i) {
            minAABB[i] = std::min(minAABB[i], v.pos[i]);
            maxAABB[i] = std::max(maxAABB[i], v.pos[i]);
        }
    }
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateBridge(
    ApexAssets::Road::BridgeType type,
    float spanLengthM,
    float deckWidthM,
    float pylonHeightM,
    float curvatureBendDeg,
    float bankingDeg,
    uint32_t cablePairs,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::BridgeParams p;
    p.type = type;
    p.spanLengthM = spanLengthM;
    p.deckWidthM = deckWidthM;
    p.pylonHeightM = pylonHeightM;
    p.curveBendAngleDeg = curvatureBendDeg;
    p.superelevationDeg = bankingDeg;
    p.cablePairsCount = cablePairs;
    bool isPed = (type == ApexAssets::Road::BridgeType::PedestrianFootbridge ||
                  type == ApexAssets::Road::BridgeType::PedestrianStressRibbon ||
                  type == ApexAssets::Road::BridgeType::PedestrianCableStayed ||
                  type == ApexAssets::Road::BridgeType::PedestrianTimberArch ||
                  type == ApexAssets::Road::BridgeType::PedestrianCoveredSkywalk);
    if (isPed && p.deckClearanceM > 12.0f) {
        p.deckClearanceM = 7.2f;
    }

    auto mesh = ApexAssets::Road::BridgeGenerator::GenerateBridgeMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& bv : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = bv.pos[0]; v.pos[1] = bv.pos[1]; v.pos[2] = bv.pos[2];
        v.normal[0] = bv.normal[0]; v.normal[1] = bv.normal[1]; v.normal[2] = bv.normal[2];
        v.uv[0] = bv.uv[0]; v.uv[1] = bv.uv[1];
        CopyColorOrFallback(bv, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_bridge";
    switch (type) {
        case ApexAssets::Road::BridgeType::CableStayed:
            s.name = "Cable-Stayed Bridge (Double A-Frame)";
            s.subType = "Cable-Stayed";
            break;
        case ApexAssets::Road::BridgeType::SuspensionBridge:
            s.name = "Long-Span Suspension Bridge (Catenary Main Cables)";
            s.subType = "Suspension";
            break;
        case ApexAssets::Road::BridgeType::PedestrianFootbridge:
            s.name = "Pedestrian Foot Bridge (Arched Tubular Steel Spine)";
            s.subType = "Foot Bridge";
            break;
        case ApexAssets::Road::BridgeType::ConcreteBoxGirder:
            s.name = "Segmental Concrete Box Girder Viaduct";
            s.subType = "Box Girder";
            break;
        case ApexAssets::Road::BridgeType::SteelTrussViaduct:
            s.name = "Warren Through-Truss Steel Bridge";
            s.subType = "Steel Truss";
            break;
        case ApexAssets::Road::BridgeType::ParabolicArch:
            s.name = "Fixed Parabolic Arch Highway Bridge";
            s.subType = "Arch Rib";
            break;
        case ApexAssets::Road::BridgeType::ExtradosedBridge:
            s.name = "Extradosed Hybrid Girder-Stay Bridge";
            s.subType = "Extradosed";
            break;
        case ApexAssets::Road::BridgeType::BasculeDrawbridge:
            s.name = "Double-Leaf Movable Bascule Drawbridge";
            s.subType = "Bascule Drawbridge";
            break;
        case ApexAssets::Road::BridgeType::CantileverTruss:
            s.name = "Balanced Cantilever Steel Truss Bridge";
            s.subType = "Cantilever Truss";
            break;
        case ApexAssets::Road::BridgeType::CableStayedCurved:
            s.name = "Asymmetric Curved Cable-Stayed Bridge";
            s.subType = "Curved Cable-Stayed";
            break;
        case ApexAssets::Road::BridgeType::MasonryViaductRoman:
            s.name = "Multi-Span Roman Masonry Stone Arch Viaduct";
            s.subType = "Masonry Viaduct";
            break;
        case ApexAssets::Road::BridgeType::PostTensionedOverpass:
            s.name = "Post-Tensioned Voided Slab Highway Overpass";
            s.subType = "Flyover Overpass";
            break;
        case ApexAssets::Road::BridgeType::PedestrianStressRibbon:
            s.name = "Prestressed Catenary Stress-Ribbon Footbridge";
            s.subType = "Stress-Ribbon";
            break;
        case ApexAssets::Road::BridgeType::PedestrianCableStayed:
            s.name = "Asymmetric Cable-Stayed Pedestrian Promenade";
            s.subType = "Cable Footbridge";
            break;
        case ApexAssets::Road::BridgeType::PedestrianTimberArch:
            s.name = "Glulam Timber Parabolic Tied-Arch Footbridge";
            s.subType = "Timber Arch";
            break;
        case ApexAssets::Road::BridgeType::SteelPlateGirder:
            s.name = "Weathering Steel Composite Multi-Plate Girder Bridge";
            s.subType = "Steel Plate Girder";
            break;
        case ApexAssets::Road::BridgeType::PedestrianCoveredSkywalk:
            s.name = "Glazed Tubular Steel Covered Pedestrian Skywalk";
            s.subType = "Covered Skywalk";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = spanLengthM;
    s.widthMeters = deckWidthM;
    s.heightMeters = pylonHeightM + p.deckClearanceM;
    s.curvatureRadiusM = (std::abs(curvatureBendDeg) > 0.01f) ? (spanLengthM / (curvatureBendDeg * 3.14159f / 180.0f)) : 0.0f;
    s.bankingAngleDeg = bankingDeg;
    s.civilStandard = "AASHTO LRFD Bridge Design Specs (9th Edition) / Eurocode EN 1991-2";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Dynamic Rendering / PBR Material Pass / 4x MSAA";

    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];

    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateTollPlaza(
    ApexAssets::Road::TollPlazaType type,
    uint32_t laneCount,
    float laneWidthM,
    float canopyClearanceM,
    float plazaLengthM,
    bool hasDynamicVMS,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::TollParams p;
    p.type = type;
    p.laneCount = laneCount;
    p.laneWidthM = laneWidthM;
    p.canopyClearanceM = canopyClearanceM;
    p.plazaLengthM = plazaLengthM;
    p.hasDynamicVMSDisplay = hasDynamicVMS;

    auto mesh = ApexAssets::Road::TollPlazaGenerator::GenerateTollMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& tv : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = tv.pos[0]; v.pos[1] = tv.pos[1]; v.pos[2] = tv.pos[2];
        v.normal[0] = tv.normal[0]; v.normal[1] = tv.normal[1]; v.normal[2] = tv.normal[2];
        v.uv[0] = tv.uv[0]; v.uv[1] = tv.uv[1];
        CopyColorOrFallback(tv, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_toll";
    switch (type) {
        case ApexAssets::Road::TollPlazaType::CanopyHighwayPlaza:
            s.name = "Monumental Highway Canopy Toll Plaza";
            s.subType = "Canopy Plaza";
            break;
        case ApexAssets::Road::TollPlazaType::OverheadETCGantry:
            s.name = "Free-Flow Electronic Toll Collection (ETC) Gantry";
            s.subType = "ETC Gantry";
            break;
        case ApexAssets::Road::TollPlazaType::ExpressBorderCheckpoint:
            s.name = "Express Highway Border Inspection Checkpoint";
            s.subType = "Border Checkpoint";
            break;
        case ApexAssets::Road::TollPlazaType::RampBarrierGate:
            s.name = "Interchange Ramp Automated Barrier Gate";
            s.subType = "Ramp Gate";
            break;
        case ApexAssets::Road::TollPlazaType::SmartSolarCanopyPlaza:
            s.name = "Smart Solar Photovoltaic Canopy Toll Plaza";
            s.subType = "Solar Canopy";
            break;
        case ApexAssets::Road::TollPlazaType::WeighInMotionPlaza:
            s.name = "High-Speed Dynamic Weigh-in-Motion (WIM) Scale Station";
            s.subType = "Weigh-in-Motion";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = plazaLengthM;
    s.widthMeters = laneCount * laneWidthM + (laneCount + 1) * p.boothIslandWidthM;
    s.heightMeters = canopyClearanceM + 4.5f;
    s.curvatureRadiusM = 0.0f;
    s.bankingAngleDeg = 0.0f;
    s.civilStandard = "FHWA Toll Facility Guidelines / ASECAP European Tolling Standards";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Push Constants (128B) / Alpha Blending (Booth Glass)";

    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];

    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateTunnel(
    ApexAssets::Road::TunnelBoreType type,
    float internalRadiusM,
    float tunnelLengthM,
    float curveBendDeg,
    float jetFanSpacingM,
    bool hasFlaredPortal,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::TunnelParams p;
    p.boreType = type;
    p.internalRadiusM = internalRadiusM;
    p.tunnelLengthM = tunnelLengthM;
    p.curveBendAngleDeg = curveBendDeg;
    p.jetFanSpacingM = jetFanSpacingM;
    p.hasFlaredPortalHood = hasFlaredPortal;

    auto mesh = ApexAssets::Road::TunnelGenerator::GenerateTunnelMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& tu : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = tu.pos[0]; v.pos[1] = tu.pos[1]; v.pos[2] = tu.pos[2];
        v.normal[0] = tu.normal[0]; v.normal[1] = tu.normal[1]; v.normal[2] = tu.normal[2];
        v.uv[0] = tu.uv[0]; v.uv[1] = tu.uv[1];
        CopyColorOrFallback(tu, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_tunnel";
    switch (type) {
        case ApexAssets::Road::TunnelBoreType::MountainHorseshoe:
            s.name = "NATM Mountain Horseshoe Arch Tunnel";
            s.subType = "Horseshoe Vault";
            break;
        case ApexAssets::Road::TunnelBoreType::TwinBoreCircularTBM:
            s.name = "TBM Circular Mechanized Twin Bore with Segment Rings";
            s.subType = "Circular TBM";
            break;
        case ApexAssets::Road::TunnelBoreType::CutAndCoverBox:
            s.name = "Cut-and-Cover Multi-Cell Reinforced Box Tunnel";
            s.subType = "Box Cut & Cover";
            break;
        case ApexAssets::Road::TunnelBoreType::FlaredPortalTransition:
            s.name = "Acoustic Flared Mountain Portal Transition";
            s.subType = "Flared Portal";
            break;
        case ApexAssets::Road::TunnelBoreType::SubseaImmersedTube:
            s.name = "Subsea Immersed Precast Concrete Tube Tunnel";
            s.subType = "Immersed Tube";
            break;
        case ApexAssets::Road::TunnelBoreType::BiLevelCircularTunnel:
            s.name = "Double-Deck Stacked Multi-Lane Shield Tunnel";
            s.subType = "Bi-Level Circular";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = tunnelLengthM;
    s.widthMeters = internalRadiusM * 2.2f;
    s.heightMeters = internalRadiusM * 2.0f;
    s.curvatureRadiusM = (std::abs(curveBendDeg) > 0.01f) ? (tunnelLengthM / (curveBendDeg * 3.14159f / 180.0f)) : 0.0f;
    s.bankingAngleDeg = 0.0f;
    s.civilStandard = "PIARC Road Tunnels Manual / NFPA 502 Standard for Road Tunnels";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Subterranean Emissive Lighting / Reverse-Z Depth Buffer";

    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];

    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateInterchange(
    ApexAssets::Road::InterchangeType type,
    float loopRadiusM,
    float totalArcAngleDeg,
    float rampWidthM,
    float elevationDeltaM,
    float bankingDeg,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::InterchangeParams p;
    p.type = type;
    p.loopRadiusM = loopRadiusM;
    p.totalArcAngleDeg = totalArcAngleDeg;
    p.rampWidthM = rampWidthM;
    p.gradeElevationDeltaM = elevationDeltaM;
    p.superelevationBankingDeg = bankingDeg;

    auto mesh = ApexAssets::Road::InterchangeGen::GenerateInterchangeMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& iv : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = iv.pos[0]; v.pos[1] = iv.pos[1]; v.pos[2] = iv.pos[2];
        v.normal[0] = iv.normal[0]; v.normal[1] = iv.normal[1]; v.normal[2] = iv.normal[2];
        v.uv[0] = iv.uv[0]; v.uv[1] = iv.uv[1];
        CopyColorOrFallback(iv, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_interchange";
    switch (type) {
        case ApexAssets::Road::InterchangeType::CloverleafLoop:
            s.name = "270-Degree Cloverleaf Loop Slip Ramp";
            s.subType = "Cloverleaf";
            break;
        case ApexAssets::Road::InterchangeType::DirectionalFlyover:
            s.name = "High-Speed Directional Flyover Ramp";
            s.subType = "Flyover";
            break;
        case ApexAssets::Road::InterchangeType::DiamondSlipRamp:
            s.name = "Diamond Deceleration Slip Ramp";
            s.subType = "Diamond Ramp";
            break;
        case ApexAssets::Road::InterchangeType::ModernRoundabout:
            s.name = "Modern Multilane Roundabout with Mountable Apron";
            s.subType = "Roundabout";
            break;
        case ApexAssets::Road::InterchangeType::SinglePointUrbanInterchange:
            s.name = "Single-Point Urban Interchange (SPUI)";
            s.subType = "SPUI";
            break;
        case ApexAssets::Road::InterchangeType::DivergingDiamondInterchange:
            s.name = "Diverging Diamond Interchange (DDI)";
            s.subType = "DDI";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = loopRadiusM * 3.14159f * (totalArcAngleDeg / 180.0f);
    s.widthMeters = rampWidthM;
    s.heightMeters = elevationDeltaM;
    s.curvatureRadiusM = loopRadiusM;
    s.bankingAngleDeg = bankingDeg;
    s.civilStandard = "AASHTO Geometric Design / Euler Clothoid Transition Curves";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Curvature Extrusion Shader / 4x MSAA";

    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];

    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateRetainingWall(
    ApexAssets::Road::RetainingWallType type,
    float wallHeightM,
    float wallLengthM,
    float batterAngleDeg,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::RetainingWallParams p;
    p.type = type;
    p.wallHeightM = wallHeightM;
    p.wallLengthM = wallLengthM;
    p.wallBatterAngleDeg = batterAngleDeg;

    auto mesh = ApexAssets::Road::RetainingWallGen::GenerateWallMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& wv : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = wv.pos[0]; v.pos[1] = wv.pos[1]; v.pos[2] = wv.pos[2];
        v.normal[0] = wv.normal[0]; v.normal[1] = wv.normal[1]; v.normal[2] = wv.normal[2];
        v.uv[0] = wv.uv[0]; v.uv[1] = wv.uv[1];
        CopyColorOrFallback(wv, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_retaining_wall";
    switch (type) {
        case ApexAssets::Road::RetainingWallType::MSEModularPanel:
            s.name = "Mechanically Stabilized Earth (MSE) Cruciform Panel Wall";
            s.subType = "MSE Panels";
            break;
        case ApexAssets::Road::RetainingWallType::SteppedRockGabion:
            s.name = "Terraced Double-Twisted Wire Mesh Rock Gabion Wall";
            s.subType = "Rock Gabion";
            break;
        case ApexAssets::Road::RetainingWallType::CantileverRCWall:
            s.name = "Cast-in-Place Reinforced Concrete Cantilever Stem Wall";
            s.subType = "RC Cantilever";
            break;
        case ApexAssets::Road::RetainingWallType::SoilNailShotcrete:
            s.name = "Soil-Nailed Slope Stabilization Shotcrete Facing";
            s.subType = "Soil Nail";
            break;
        case ApexAssets::Road::RetainingWallType::CribWallCellular:
            s.name = "Interlocking Precast Concrete Cellular Crib Wall";
            s.subType = "Crib Wall";
            break;
        case ApexAssets::Road::RetainingWallType::ContiguousBoredPileWall:
            s.name = "Secant Contiguous Bored Pile Retaining Wall";
            s.subType = "Bored Pile Wall";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = wallLengthM;
    s.widthMeters = p.strapReinforcementLengthM;
    s.heightMeters = wallHeightM;
    s.curvatureRadiusM = 0.0f;
    s.bankingAngleDeg = batterAngleDeg;
    s.civilStandard = "FHWA GEC 011 MSE Walls / Eurocode 7 Geotechnical Design";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Triplanar Normal Mapping / Instanced Panels";

    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];

    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateSignpost(
    ApexAssets::Road::SignpostType type,
    float gantrySpanM,
    float clearanceH,
    float boardWidthM,
    uint32_t signCount,
    bool hasDMS,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::SignpostParams p;
    p.type = type;
    p.gantrySpanWidthM = gantrySpanM;
    p.clearanceHeightM = clearanceH;
    p.signBoardWidthM = boardWidthM;
    p.signCount = signCount;
    p.hasEmissiveDMS = hasDMS;

    auto mesh = ApexAssets::Road::SignpostGenerator::GenerateSignpostMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& sv : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = sv.pos[0]; v.pos[1] = sv.pos[1]; v.pos[2] = sv.pos[2];
        v.normal[0] = sv.normal[0]; v.normal[1] = sv.normal[1]; v.normal[2] = sv.normal[2];
        v.uv[0] = sv.uv[0]; v.uv[1] = sv.uv[1];
        CopyColorOrFallback(sv, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_signpost";
    switch (type) {
        case ApexAssets::Road::SignpostType::CantileverHighwaySign:
            s.name = "Cantilever Truss Highway Guide Sign";
            s.subType = "Cantilever Sign";
            break;
        case ApexAssets::Road::SignpostType::FullSpanTrussGantry:
            s.name = "Full-Span Overhead Warren Truss Gantry";
            s.subType = "Truss Gantry";
            break;
        case ApexAssets::Road::SignpostType::VariableMessageMatrix:
            s.name = "Dynamic LED Variable Message Sign (VMS/DMS)";
            s.subType = "Variable Message";
            break;
        case ApexAssets::Road::SignpostType::RegulatoryChevronCurveSet:
            s.name = "MUTCD Regulatory & Curve Chevron Sign Series";
            s.subType = "Chevron Curve Set";
            break;
        case ApexAssets::Road::SignpostType::ButterflySplitSign:
            s.name = "Dual-Wing Gore Split Butterfly Highway Sign";
            s.subType = "Butterfly Gore Sign";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = 18.0f;
    s.widthMeters = gantrySpanM;
    s.heightMeters = clearanceH + p.signBoardHeightM + 1.0f;
    s.curvatureRadiusM = 0.0f;
    s.bankingAngleDeg = 0.0f;
    s.civilStandard = "FHWA MUTCD 11th Edition / AASHTO LTS-6 Sign Supports";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Retroreflective PBR + Emissive LED Matrix";
    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];
    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateExpressway(
    ApexAssets::Road::ExpresswayType type,
    uint32_t lanesPerDir,
    float sectionLengthM,
    float curveBendDeg,
    float bankingDeg,
    bool hasLighting,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::AsphaltParams p;
    p.type = type;
    p.laneCountPerDir = lanesPerDir;
    p.sectionLengthM = sectionLengthM;
    p.curveBendAngleDeg = curveBendDeg;
    p.superelevationDeg = bankingDeg;
    p.hasStreetLighting = hasLighting;

    auto mesh = ApexAssets::Road::ProceduralAsphalt::GenerateExpresswayMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& ev : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = ev.pos[0]; v.pos[1] = ev.pos[1]; v.pos[2] = ev.pos[2];
        v.normal[0] = ev.normal[0]; v.normal[1] = ev.normal[1]; v.normal[2] = ev.normal[2];
        v.uv[0] = ev.uv[0]; v.uv[1] = ev.uv[1];
        CopyColorOrFallback(ev, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_expressway";
    switch (type) {
        case ApexAssets::Road::ExpresswayType::SixLaneDividedFreeway:
            s.name = "Divided Multi-Lane Controlled-Access Freeway";
            s.subType = "Divided Freeway";
            break;
        case ApexAssets::Road::ExpresswayType::ElevatedUrbanViaductDeck:
            s.name = "Elevated Urban Expressway Concrete Viaduct";
            s.subType = "Elevated Viaduct";
            break;
        case ApexAssets::Road::ExpresswayType::ExpresswayTollMergeTaper:
            s.name = "Expressway Gore Exit & Deceleration Taper";
            s.subType = "Gore Exit Taper";
            break;
        case ApexAssets::Road::ExpresswayType::SmartManagedEVCorridor:
            s.name = "Smart Managed Inductive EV & BRT Express Corridor";
            s.subType = "Smart EV Corridor";
            break;
        case ApexAssets::Road::ExpresswayType::MountainSwitchbackCut:
            s.name = "Superelevated Mountain Rock-Cut Bench Expressway";
            s.subType = "Mountain Bench Cut";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = sectionLengthM;
    s.widthMeters = lanesPerDir * 2.0f * p.laneWidthM + 6.0f;
    s.heightMeters = (type == ApexAssets::Road::ExpresswayType::ElevatedUrbanViaductDeck) ? 14.0f : 8.5f;
    s.curvatureRadiusM = (std::abs(curveBendDeg) > 0.01f) ? (sectionLengthM / (curveBendDeg * 3.14159f / 180.0f)) : 0.0f;
    s.bankingAngleDeg = bankingDeg;
    s.civilStandard = "AASHTO Geometric Design of Highways / Eurocode EN 13108";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Superelevated Asphalt & Cobra LED Pass";
    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];
    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateBarrier(
    ApexAssets::Road::BarrierType type,
    float barrierLengthM,
    float barrierHeightM,
    float curveBendDeg,
    float postSpacingM,
    bool hasReflectors,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::BarrierParams p;
    p.type = type;
    p.barrierLengthM = barrierLengthM;
    p.barrierHeightM = barrierHeightM;
    p.curveBendAngleDeg = curveBendDeg;
    p.postSpacingM = postSpacingM;
    p.hasReflectorDelineators = hasReflectors;

    auto mesh = ApexAssets::Road::BarrierKit::GenerateBarrierMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& bv : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = bv.pos[0]; v.pos[1] = bv.pos[1]; v.pos[2] = bv.pos[2];
        v.normal[0] = bv.normal[0]; v.normal[1] = bv.normal[1]; v.normal[2] = bv.normal[2];
        v.uv[0] = bv.uv[0]; v.uv[1] = bv.uv[1];
        CopyColorOrFallback(bv, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_barrier";
    switch (type) {
        case ApexAssets::Road::BarrierType::NewJerseyFShapeConcrete:
            s.name = "MASH F-Shape Precast Concrete Jersey Barrier";
            s.subType = "Jersey Concrete";
            break;
        case ApexAssets::Road::BarrierType::GalvanizedWBeamGuardrail:
            s.name = "Corrugated W-Beam Steel Guardrail & SKT Terminal";
            s.subType = "W-Beam Guardrail";
            break;
        case ApexAssets::Road::BarrierType::HighTensionCableBarrier:
            s.name = "4-Strand High-Tension Median Cable Barrier";
            s.subType = "Tension Cable";
            break;
        case ApexAssets::Road::BarrierType::AcousticNoiseBarrierWall:
            s.name = "Highway Acoustic Noise Barrier Sound Wall";
            s.subType = "Acoustic Noise Wall";
            break;
        case ApexAssets::Road::BarrierType::RollerImpactCrashCushion:
            s.name = "Rotating EVA Roller Impact Crash Guardrail";
            s.subType = "EVA Roller Rail";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = barrierLengthM;
    s.widthMeters = 4.2f;
    s.heightMeters = barrierHeightM;
    s.curvatureRadiusM = (std::abs(curveBendDeg) > 0.01f) ? (barrierLengthM / (curveBendDeg * 3.14159f / 180.0f)) : 0.0f;
    s.bankingAngleDeg = 0.0f;
    s.civilStandard = "MASH 2016 Crash Criteria / EN 1317 Road Restraint Systems";
    s.vulkanPipelineConfig = "Vulkan 1.3 / Galvanized Steel PBR & Retroreflective Tabs";
    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];
    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::GenerateRoadBlock(
    ApexAssets::Road::RoadBlockType type,
    float roadWidthM,
    float zoneLengthM,
    uint32_t unitCount,
    bool hasBeacons,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::RoadBlockParams p;
    p.type = type;
    p.roadWidthM = roadWidthM;
    p.zoneLengthM = zoneLengthM;
    p.unitCount = unitCount;
    p.hasWarningBeacons = hasBeacons;

    auto mesh = ApexAssets::Road::RoadBlockGenerator::GenerateRoadBlockMesh(p);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(mesh.vertices.size());

    for (const auto& rv : mesh.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = rv.pos[0]; v.pos[1] = rv.pos[1]; v.pos[2] = rv.pos[2];
        v.normal[0] = rv.normal[0]; v.normal[1] = rv.normal[1]; v.normal[2] = rv.normal[2];
        v.uv[0] = rv.uv[0]; v.uv[1] = rv.uv[1];
        CopyColorOrFallback(rv, v);
        outVertices.push_back(v);
    }
    outIndices = mesh.indices;

    RoadComponentMeshSummary s;
    s.componentId = "modular_roadblock";
    switch (type) {
        case ApexAssets::Road::RoadBlockType::JerseyAndWaterBarricadeLine:
            s.name = "Water-Filled Jersey Barricade & Saw-Horse Line";
            s.subType = "Water Barricades";
            break;
        case ApexAssets::Road::RoadBlockType::HydraAntiRamBollardArray:
            s.name = "ASTM F2656 K12 Hydraulic Anti-Ram Bollard Array";
            s.subType = "Anti-Ram Bollards";
            break;
        case ApexAssets::Road::RoadBlockType::TacticalSpikeAndChicane:
            s.name = "Tactical Checkpoint Slalom Chicane & Spike Strip";
            s.subType = "Tactical Chicane";
            break;
        case ApexAssets::Road::RoadBlockType::ConstructionLaneClosureZone:
            s.name = "MUTCD Workzone Drum Taper & LED Arrow Trailer";
            s.subType = "Workzone Taper";
            break;
        case ApexAssets::Road::RoadBlockType::ConcreteDragonTeethChicane:
            s.name = "Staggered Precast Concrete Dragon-Teeth Blocks";
            s.subType = "Dragon Teeth";
            break;
    }
    s.vertexCount = static_cast<uint32_t>(outVertices.size());
    s.triangleCount = static_cast<uint32_t>(outIndices.size() / 3);
    s.lengthMeters = zoneLengthM;
    s.widthMeters = roadWidthM;
    s.heightMeters = 3.2f;
    s.curvatureRadiusM = 0.0f;
    s.bankingAngleDeg = 0.0f;
    s.civilStandard = "FHWA MUTCD Part 6 Temporary Control / ASTM F2656 K12";
    s.vulkanPipelineConfig = "Vulkan 1.3 / High-Visibility Safety Orange & Warning LEDs";
    s.boundingBoxMin[0] = mesh.minAABB[0]; s.boundingBoxMin[1] = mesh.minAABB[1]; s.boundingBoxMin[2] = mesh.minAABB[2];
    s.boundingBoxMax[0] = mesh.maxAABB[0]; s.boundingBoxMax[1] = mesh.maxAABB[1]; s.boundingBoxMax[2] = mesh.maxAABB[2];
    return s;
}

RoadComponentMeshSummary VulkanRoadComponentEngine::AssembleConnectedHighwayScene(
    float sceneScale,
    std::vector<RoadEngineVertex>& outVertices,
    std::vector<uint32_t>& outIndices
) {
    ApexAssets::Road::BridgeParams bridgeP;
    bridgeP.type = ApexAssets::Road::BridgeType::CableStayed;
    bridgeP.spanLengthM = 160.0f;
    bridgeP.deckWidthM = 24.0f;
    bridgeP.pylonHeightM = 58.0f;
    bridgeP.curveBendAngleDeg = 12.0f;
    bridgeP.superelevationDeg = 3.5f;

    ApexAssets::Road::TollParams tollP;
    tollP.laneCount = 6;
    tollP.plazaLengthM = 58.0f;

    ApexAssets::Road::TunnelParams tunnelP;
    tunnelP.internalRadiusM = 6.2f;
    tunnelP.tunnelLengthM = 110.0f;
    tunnelP.curveBendAngleDeg = 16.0f;

    ApexAssets::Road::InterchangeParams interchangeP;
    interchangeP.loopRadiusM = 50.0f;

    ApexAssets::Road::RetainingWallParams wallP;
    wallP.wallHeightM = 6.5f;
    wallP.wallLengthM = 52.0f;

    auto fullScene = m_sceneOrchestrator.AssembleFullConnectedScene(bridgeP, tollP, tunnelP, interchangeP, wallP);

    outVertices.clear();
    outIndices.clear();
    outVertices.reserve(fullScene.vertices.size());

    for (const auto& sv : fullScene.vertices) {
        RoadEngineVertex v{};
        v.pos[0] = sv.pos[0] * sceneScale;
        v.pos[1] = sv.pos[1] * sceneScale;
        v.pos[2] = sv.pos[2] * sceneScale;
        v.normal[0] = sv.normal[0]; v.normal[1] = sv.normal[1]; v.normal[2] = sv.normal[2];
        v.uv[0] = sv.uv[0]; v.uv[1] = sv.uv[1];
        CopyColorOrFallback(sv, v);
        outVertices.push_back(v);
    }
    outIndices = fullScene.indices;

    RoadComponentMeshSummary s;
    s.componentId = "connected_highway_scene";
    s.name = "Complete Connected Highway Corridor (Toll -> Bridge -> Tunnel)";
    s.subType = "Full Highway System";
    s.vertexCount = fullScene.totalVertices;
    s.triangleCount = fullScene.totalTriangles;
    s.lengthMeters = fullScene.totalRoadLengthKm * 1000.0f;
    s.widthMeters = 36.0f;
    s.heightMeters = 85.0f;
    s.curvatureRadiusM = 350.0f;
    s.bankingAngleDeg = 4.0f;
    s.civilStandard = "Combined AASHTO, FHWA, NFPA 502, Eurocode LRFD Highway Standards";
    s.vulkanPipelineConfig = "Sascha Willems Vulkan 1.3 / Dynamic Rendering / Push Constant Geometry / 4x MSAA";

    ComputeEngineAABB(outVertices, s.boundingBoxMin, s.boundingBoxMax);
    return s;
}

} // namespace custom_vulkan
