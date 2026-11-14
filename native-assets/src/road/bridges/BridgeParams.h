// ==============================================================================
// Modular Road Component: Bridge Architecture & Parametric Modeler
// Path: native-assets/src/road/bridges/BridgeParams.h
// Follows real-world civil engineering (AASHTO LRFD / Eurocode 1)
// Vulkan Scene Pipeline: Sascha Willems / Khronos Vulkan Samples
// ==============================================================================
#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace ApexAssets {
namespace Road {

enum class BridgeType : uint32_t {
    CableStayed = 0,        // Inverted-Y A-frame pylons, non-overlapping semi-fan stays, aerodynamic box deck
    SuspensionBridge,       // Twin portal towers, massive gravity shore anchorages, catenary main cables & hangers
    PedestrianFootbridge,   // Signature pedestrian foot bridge, arched tubular steel spine, glass parapets, stepped ramps
    ConcreteBoxGirder,      // Post-tensioned variable-depth trapezoidal box girder, sculpted hammerhead piers
    SteelTrussViaduct,      // Warren/Pratt through-truss with sway bracing, portal frames and floorbeams
    ParabolicArch,          // Concrete/steel arch rib with vertical wire rope hangers and skewback thrust blocks
    ExtradosedBridge,       // Hybrid girder-cable with low-profile pylons and shallow stay angles
    BasculeDrawbridge,      // Movable double-leaf bascule with trunnion counterweight piers and operator tower
    CantileverTruss,        // Balanced cantilever steel truss with anchor spans and suspended drop-in center span
    CableStayedCurved,      // Asymmetric curved deck with backward-inclined pylon counteracting centrifugal curve
    MasonryViaductRoman,    // Classical multi-tier stone masonry Roman arch viaduct with triangular cutwaters
    PostTensionedOverpass,  // Highway diamond interchange overpass with flared piers and slim voided slab
    PedestrianStressRibbon, // Prestressed catenary stress-ribbon with precast concrete steps & stainless mesh balustrade
    PedestrianCableStayed,  // Asymmetric single inclined needle mast with timber deck & scenic observation outlooks
    PedestrianTimberArch,   // Glulam timber double arch ribs with vertical hangers, cedar deck and lanterns
    SteelPlateGirder,       // Multi-girder weathering steel composite with longitudinal I-beams & cross diaphragms
    PedestrianCoveredSkywalk // Glazed tubular steel canopy skywalk promenade with illuminated ribs and cylindrical piers
};

enum class CableArrangement : uint32_t {
    SemiFan = 0,        // Highest efficiency, cables converge near pylon saddle
    Harp,               // Parallel cables with uniform inclination
    FullFan             // Concentrated at single focal apex point
};

enum class PierFoundationType : uint32_t {
    HammerheadSingle = 0, // Single monolithic cylindrical/flared shaft with cantilever crosshead
    TwinColumnBent,       // Two circular RC columns with cap beam
    HollowBoxPier,        // High-rise hollow box pier for deep gorge crossing
    ThrustAbutment        // Heavy reinforced mass-concrete abutment
};

struct BridgeParams {
    BridgeType type = BridgeType::CableStayed;
    
    // Primary Span & Deck Geometry
    float spanLengthM = 160.0f;           // Main span distance between piers/pylons (m)
    float deckWidthM = 24.0f;             // Total deck width (m) (supports 4-6 lanes + shoulders)
    float deckThicknessM = 2.8f;          // Structural box/girder depth (m)
    float deckClearanceM = 25.0f;         // Vertical navigational/valley clearance (m)
    uint32_t laneCount = 4;               // Number of traffic lanes
    float laneWidthM = 3.65f;             // Standard lane width (m)
    
    // Curvature & Bending Manipulation
    float horizontalCurveRadiusM = 0.0f;  // 0.0f = straight bridge; >0 = curved horizontal radius (m)
    float curveBendAngleDeg = 0.0f;       // Total arc bend in degrees (-45° to +45°)
    float superelevationDeg = 3.5f;       // Banking cross-slope angle for high-speed vehicle stability
    float longitudinalGradePct = 1.2f;    // Longitudinal vertical slope (%)
    
    // Cable-Stayed Specific Parameters
    float pylonHeightM = 58.0f;           // Pylon tower height above deck (m)
    uint32_t cablePairsCount = 14;        // Number of stay cable pairs along the span
    float cableDiameterMm = 140.0f;       // Stay cable strand diameter with HDPE casing (mm)
    CableArrangement cablePattern = CableArrangement::SemiFan;
    bool twinPylons = true;               // Dual towers for balanced double-span layout
    
    // Substructure & Pier Geometry
    PierFoundationType pierType = PierFoundationType::HammerheadSingle;
    uint32_t pierCount = 3;               // Intermediate piers for viaduct / girder bridge
    float pierWidthM = 4.2f;              // Pier base thickness (m)
    
    // Safety & Barriers
    bool hasConcreteNewJerseyBarrier = true; // High-containment median and edge barrier
    bool hasPedestrianSidewalk = true;       // Cantilevered pedestrian inspection walkway
    bool hasAviationWarningLights = true;    // Red/white flashing LED beacons at pylon tips
    bool hasInspectionGantries = true;       // Underside maintenance travelers
    
    // Vulkan PBR Material Constants
    float concreteRoughness = 0.65f;
    float steelReflectivity = 0.88f;
    float asphaltWearingFriction = 0.85f;
};

} // namespace Road
} // namespace ApexAssets
