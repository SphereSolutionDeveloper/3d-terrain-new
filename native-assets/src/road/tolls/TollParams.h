// ==============================================================================
// Modular Road Component: Toll Plaza Architecture & Telemetry
// Path: native-assets/src/road/tolls/TollParams.h
// Follows real-world highway plaza architecture (FHWA & ASECAP guidelines)
// ==============================================================================
#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace ApexAssets {
namespace Road {

enum class TollPlazaType : uint32_t {
    CanopyHighwayPlaza = 0, // Monumental multi-lane spatial canopy with concrete islands & booths
    OverheadETCGantry,      // High-speed barrier-free electronic toll collection (RFID + ALPR)
    ExpressBorderCheckpoint,// Weigh-in-motion dynamic scale plates, customs inspection bays
    RampBarrierGate,        // Modular interchange slip ramp gate with automated dispenser & boom arm
    SmartSolarCanopyPlaza,  // Photovoltaic aerodynamic solar canopy with EV charge lane markers
    WeighInMotionPlaza      // Dynamic high-speed axle scale station with automated bypass enforcement
};

enum class CanopyRoofStyle : uint32_t {
    AerodynamicAirfoil = 0, // Curved high-tech composite wing profile
    SpaceFrameTruss,        // Welded structural tetrahedral steel space grid
    TensileMembrane,        // PTFE coated fiberglass membrane over steel masts
    FlatCantileveredSlab    // Modernist precast post-tensioned concrete slab
};

struct TollParams {
    TollPlazaType type = TollPlazaType::CanopyHighwayPlaza;
    
    // Geometry & Lane Configuration
    uint32_t laneCount = 6;                // Number of active toll lanes (2 to 14)
    float laneWidthM = 3.8f;               // Width per toll lane (m)
    float plazaLengthM = 64.0f;            // Longitudinal concrete apron length (m)
    float canopyClearanceM = 5.5f;         // Vertical vehicle height clearance (standard 5.2m + 0.3m buffer)
    float canopyOverhangM = 3.5f;          // Front/back weather cantilever overhang (m)
    CanopyRoofStyle canopyStyle = CanopyRoofStyle::AerodynamicAirfoil;
    
    // Toll Booths & Island Engineering
    float boothIslandLengthM = 18.0f;      // Reinforced concrete protective island length (m)
    float boothIslandWidthM = 1.8f;        // Concrete crash bullnose curb width (m)
    float boothHeightM = 2.8f;             // Operator/automated booth structure height (m)
    bool hasCrashAttenuators = true;       // QuadGuard impact collision cushions at island nose
    bool hasAutomatedBarrierArms = true;   // High-speed articulated barrier gates (0.6s cycle)
    
    // Electronics, Sensors & Signage
    bool hasDynamicVMSDisplay = true;      // Overhead Variable Message Signs per lane (Green Arrow / Red X)
    bool hasALPRCameras = true;            // Automatic License Plate Recognition dual infrared cameras
    bool hasLaserVehicleProfiler = true;   // LiDAR height/axle 3D volumetric scanner
    bool hasTreadleAxleCounters = true;    // Embedded piezoelectric weight sensors in pavement
    
    // Bending & Plaza Transition Tapers
    float approachTaperRatio = 1.0f / 5.0f; // 1:5 geometric flare from standard 2-lane highway to 6-lane plaza
    float curveBendAngleDeg = 0.0f;        // Curvature if positioned on sweeping highway approach (-30° to +30°)
    
    // Vulkan PBR Attributes
    float canopyMetallic = 0.78f;
    float canopyRoughness = 0.25f;
    float pavementConcreteRoughness = 0.70f;
    float ledEmissiveIntensity = 5.0f;
};

} // namespace Road
} // namespace ApexAssets
