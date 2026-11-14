// ==============================================================================
// Modular Road Component: Expressway & Multi-Lane Highway Architecture
// Path: native-assets/src/road/asphalt/AsphaltParams.h
// Standards: AASHTO A Policy on Geometric Design of Highways & Eurocode EN 13108
// ==============================================================================
#pragma once

#include <cstdint>

namespace ApexAssets {
namespace Road {

enum class ExpresswayType : uint32_t {
    SixLaneDividedFreeway = 0, // Divided 3+3 freeway with Jersey median, cobra-head poles & W-beam shoulders
    ElevatedUrbanViaductDeck,  // Elevated expressway viaduct on cylindrical piers + acoustic parapets
    ExpresswayTollMergeTaper,  // Deceleration/acceleration exit gore wedge with chevron hatchings & diverge arrow
    SmartManagedEVCorridor,    // Managed EV/BRT express corridor with inductive charge strips & V2X smart poles
    MountainSwitchbackCut      // Superelevated curved hillside highway bench with battered rock cut & guardrail
};

struct AsphaltParams {
    ExpresswayType type = ExpresswayType::SixLaneDividedFreeway;

    uint32_t laneCountPerDir = 3;      // Active lanes per direction (2 to 4)
    float laneWidthM = 3.65f;          // Standard AASHTO expressway lane width (m)
    float sectionLengthM = 120.0f;     // Longitudinal expressway section length (m)
    float curveBendAngleDeg = 14.0f;   // Horizontal sweep angle (-45° to +45°)
    float superelevationDeg = 4.5f;    // Banking cross-slope angle (deg)
    bool hasMedianBarrier = true;      // Central double-sided concrete Jersey divider + delineators
    bool hasStreetLighting = true;     // Dual cobra-head LED highway luminaires & V2X masts
};

} // namespace Road
} // namespace ApexAssets
