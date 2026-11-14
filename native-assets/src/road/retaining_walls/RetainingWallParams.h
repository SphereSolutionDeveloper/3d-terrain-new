// ==============================================================================
// Modular Road Component: Retaining Wall & Highway Embankment Architecture
// Path: native-assets/src/road/retaining_walls/RetainingWallParams.h
// Follows FHWA GEC 011 & Eurocode 7 Geotechnical Design
// ==============================================================================
#pragma once

#include <cstdint>

namespace ApexAssets {
namespace Road {

enum class RetainingWallType : uint32_t {
    MSEModularPanel = 0, // Mechanically Stabilized Earth with precast cruciform panels
    SteppedRockGabion,   // Double-twisted wire mesh baskets with basalt/granite rip-rap
    CantileverRCWall,    // Cast-in-place reinforced concrete T-stem wall with footing
    SoilNailShotcrete,   // Top-down soil nailed wall with patterned bearing plates
    CribWallCellular,    // Interlocking precast concrete cellular crib boxes with stone backfill
    ContiguousBoredPileWall // Overlapping secant bored shaft piles with capping waler beam
};

struct RetainingWallParams {
    RetainingWallType type = RetainingWallType::MSEModularPanel;
    
    float wallHeightM = 6.5f;              // Total retained vertical height (3.0m to 18.0m)
    float wallLengthM = 60.0f;             // Longitudinal stretch along highway corridor (m)
    float wallBatterAngleDeg = 4.0f;       // Inward lean backward batter angle (deg)
    
    // MSE Panel Geometry
    float panelWidthM = 1.5f;              // Precast cruciform panel width (m)
    float panelHeightM = 1.5f;             // Precast cruciform panel height (m)
    float strapReinforcementLengthM = 5.2f;// Galvanized steel geogrid strap embedment (m)
    
    // Drainage & Safety Systems
    bool hasWeepholes = true;              // Perforated hydrostatic relief drainage pipes
    bool hasTopCopigSlab = true;           // Precast concrete coping cap beam
    bool hasHighwayJerseyBarrier = true;   // Integrated TL-4 bridge/wall barrier on top
    
    // PBR Texturing
    float concretePanelRoughness = 0.68f;
    float wireGabionMetallic = 0.45f;
};

} // namespace Road
} // namespace ApexAssets
