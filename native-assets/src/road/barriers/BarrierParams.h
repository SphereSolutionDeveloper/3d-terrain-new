// ==============================================================================
// Modular Road Component: Safety Barrier & Guardrail Architecture
// Path: native-assets/src/road/barriers/BarrierParams.h
// Standards: MASH 2016 Crash Test Criteria & EN 1317 Road Restraint Systems
// ==============================================================================
#pragma once

#include <cstdint>

namespace ApexAssets {
namespace Road {

enum class BarrierType : uint32_t {
    NewJerseyFShapeConcrete = 0, // Sloped concrete F-shape Jersey crash barrier with scuppers & delineators
    GalvanizedWBeamGuardrail,    // Corrugated W-beam steel guardrail on driven I-posts + SKT impact terminal
    HighTensionCableBarrier,     // 4-strand tensioned steel cable barrier on driven I-posts + concrete mow strip
    AcousticNoiseBarrierWall,    // Highway sound-absorption wall with steel H-piles, ribbed panels & clear top
    RollerImpactCrashCushion     // High-visibility rotating yellow EVA roller barrel guardrail on pipe rails
};

struct BarrierParams {
    BarrierType type = BarrierType::NewJerseyFShapeConcrete;

    float barrierLengthM = 48.0f;      // Longitudinal deployment length along curved highway shoulder (m)
    float barrierHeightM = 1.10f;      // Vertical height above pavement datum (0.75m - 4.5m for noise walls)
    float curveBendAngleDeg = 12.0f;   // Horizontal sweep curvature along highway edge (deg)
    float postSpacingM = 2.0f;         // Longitudinal post / bay spacing (m)
    bool hasReflectorDelineators = true; // Retroreflective amber/white delineator tabs & safety reflectors
};

} // namespace Road
} // namespace ApexAssets
