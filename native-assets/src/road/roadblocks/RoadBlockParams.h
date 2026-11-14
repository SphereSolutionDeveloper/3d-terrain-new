// ==============================================================================
// Modular Road Component: Road Block & Security Checkpoint Architecture
// Path: native-assets/src/road/roadblocks/RoadBlockParams.h
// Standards: FHWA MUTCD Part 6 Temporary Traffic Control & ASTM F2656 K12 Anti-Ram
// ==============================================================================
#pragma once

#include <cstdint>

namespace ApexAssets {
namespace Road {

enum class RoadBlockType : uint32_t {
    JerseyAndWaterBarricadeLine = 0, // Interlocking orange/white water-filled barriers + saw-horse signs + cones
    HydraAntiRamBollardArray,        // Steel hydraulic K12 anti-ram bollards with LED collars & security gate arm
    TacticalSpikeAndChicane,         // Slalom concrete checkpoint chicane + tire-spike strip + guard booth + arrow sign
    ConstructionLaneClosureZone,     // MUTCD orange workzone drum taper + K-rails + trailer-mounted flashing LED arrow board
    ConcreteDragonTeethChicane       // Staggered monolithic precast concrete blocks & hazard striped deflection walls
};

struct RoadBlockParams {
    RoadBlockType type = RoadBlockType::JerseyAndWaterBarricadeLine;

    float roadWidthM = 12.0f;     // Active pavement width covered by road block deployment (m)
    float zoneLengthM = 34.0f;    // Longitudinal pavement zone length (m)
    uint32_t unitCount = 6;       // Number of primary barrier/bollard/drum modules across deployment
    float spacingM = 2.4f;        // Module pitch/gap spacing (m)
    bool hasWarningBeacons = true;// Amber/Red emissive warning lights & arrow board LEDs
};

} // namespace Road
} // namespace ApexAssets
