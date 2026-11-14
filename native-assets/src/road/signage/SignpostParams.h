// ==============================================================================
// Modular Road Component: Signpost & Overhead Gantry Architecture
// Path: native-assets/src/road/signage/SignpostParams.h
// Standards: MUTCD (Manual on Uniform Traffic Control Devices) & AASHTO LTS-6
// ==============================================================================
#pragma once

#include <cstdint>

namespace ApexAssets {
namespace Road {

enum class SignpostType : uint32_t {
    CantileverHighwaySign = 0, // Monotube / truss cantilever roadside mast with large reflective guide board
    FullSpanTrussGantry,       // Dual-column overhead Warren space-truss straddling multi-lane highway
    VariableMessageMatrix,     // Electronic dynamic LED VMS / DMS message cabinet with cooling louvres
    RegulatoryChevronCurveSet, // Roadside curve chevron alignment series + speed limit & milepost poles
    ButterflySplitSign         // Dual-wing gore split T-mast with dual opposing exit/through directional panels
};

struct SignpostParams {
    SignpostType type = SignpostType::CantileverHighwaySign;

    float gantrySpanWidthM = 18.0f;   // Total horizontal span for gantry or shoulder-to-sign cantilever arm
    float clearanceHeightM = 5.8f;    // Clear vertical height above asphalt pavement (standard >= 5.2m)
    float signBoardWidthM = 6.5f;     // Main aluminum retroreflective sign face width (m)
    float signBoardHeightM = 3.2f;    // Main aluminum retroreflective sign face height (m)
    uint32_t signCount = 3;           // Number of panels / chevrons along gantry or curve
    bool hasServiceCatwalk = true;    // Maintenance walkway + safety handrail under full-span gantry
    bool hasEmissiveDMS = true;       // Luminaire floodlights & glowing LED matrix panels
};

} // namespace Road
} // namespace ApexAssets
