// ==============================================================================
// Modular Road Component: Interchange & Ramp Architecture
// Path: native-assets/src/road/interchanges/InterchangeParams.h
// Follows AASHTO Green Book: A Policy on Geometric Design of Highways and Streets
// ==============================================================================
#pragma once

#include <cstdint>
#include <string>

namespace ApexAssets {
namespace Road {

enum class InterchangeType : uint32_t {
    CloverleafLoop = 0,    // 270-degree loop slip ramp connecting intersecting grade levels
    DirectionalFlyover,    // Elevated continuous curved flyover bridge ramp with superelevation
    DiamondSlipRamp,       // Diagonal entrance/exit slip lane with gore deceleration taper
    ModernRoundabout,      // Circulating roadway with truck apron, splitter islands and yield lanes
    SinglePointUrbanInterchange, // SPUI - compact single multi-phase traffic signal over crossroad
    DivergingDiamondInterchange  // DDI - criss-cross geometric crossover for free freeway access
};

struct InterchangeParams {
    InterchangeType type = InterchangeType::CloverleafLoop;
    
    // Geometric Curve & Bending Dimensions
    float loopRadiusM = 55.0f;             // Radius of loop ramp (45m to 85m)
    float totalArcAngleDeg = 270.0f;       // Arc length in degrees (e.g. 270° for cloverleaf)
    float rampWidthM = 7.5f;               // 1-lane ramp with wide inner/outer emergency shoulders
    float gradeElevationDeltaM = 7.5f;     // Vertical grade separation height (m)
    
    // Superelevation & Clothoid Spiral Transition
    float superelevationBankingDeg = 6.0f; // Cross-slope banking angle e (%)
    float spiralClothoidLengthM = 45.0f;   // Euler spiral transition segment to prevent lateral jerk
    float designSpeedKmH = 45.0f;          // Geometric ramp design speed
    
    // Safety & Barriers
    bool hasConcreteJerseyBarriers = true;
    bool hasOverheadGantrySignage = true;
    bool hasChevronAlignmentSigns = true;  // Yellow/black reflective directional chevrons
    
    // Roundabout Specifics
    float centralIslandRadiusM = 18.0f;    // Inscribed diameter of modern roundabout
    float truckApronWidthM = 2.5f;         // Mountable concrete apron for articulated tractor-trailers
    uint32_t approachLegsCount = 4;        // 3 to 6 approach roadways
};

} // namespace Road
} // namespace ApexAssets
