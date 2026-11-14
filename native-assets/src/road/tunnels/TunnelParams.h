// ==============================================================================
// Modular Road Component: Tunnel Architecture & Subterranean Engineering
// Path: native-assets/src/road/tunnels/TunnelParams.h
// Follows real-world tunneling standards (PIARC, NFPA 502, Eurocode 7)
// ==============================================================================
#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace ApexAssets {
namespace Road {

enum class TunnelBoreType : uint32_t {
    MountainHorseshoe = 0, // NATM drill & blast horseshoe vault with shotcrete + cast RC lining
    TwinBoreCircularTBM,   // Mechanized TBM circular tunnel with bolted precast segmental rings
    CutAndCoverBox,        // Rectangular reinforced concrete multi-cell box culvert
    FlaredPortalTransition,// Monumental entrance portal with acoustic flare & daylight louvers
    SubseaImmersedTube,    // Precast immersed tube tunnel with ballast compartments & shear keys
    BiLevelCircularTunnel  // Double-deck circular shield tunnel with stacked upper & lower roadways
};

enum class TunnelLiningFinish : uint32_t {
    CastInPlaceConcrete = 0, // Smooth reflective enamel or painted concrete panels
    PrecastSegmentRings,     // Segmental concrete rings with visible gasket joints
    ExposedRockShotcrete,    // Rough fiber-reinforced shotcrete with rock bolts
    CeramicTiledLowerWall    // High-reflectance white vitreous enamel panels up to 3m
};

struct TunnelParams {
    TunnelBoreType boreType = TunnelBoreType::MountainHorseshoe;
    TunnelLiningFinish liningFinish = TunnelLiningFinish::CastInPlaceConcrete;
    
    // Bore Dimensions & Size Manipulation
    float internalRadiusM = 5.8f;          // Clear radius from tunnel centerline (m)
    float tunnelLengthM = 120.0f;          // Longitudinal length of the tunnel segment/bore (m)
    float roadwayWidthM = 10.5f;           // Invert roadway width (2 lanes + inspection walkways)
    float walkwayWidthM = 1.2f;            // Raised emergency egress walkway width on both sides (m)
    float walkwayHeightM = 0.35f;          // Walkway curb elevation above asphalt (m)
    
    // Horizontal Bending & Curvature
    float curveRadiusM = 0.0f;             // 0.0f = straight bore; >0 = horizontal curve radius (m)
    float curveBendAngleDeg = 0.0f;        // Total bend angle through mountain (-45° to +45°)
    float gradientSlopePct = 1.5f;         // Longitudinal grade slope (%)
    
    // Portal Entrance Architecture
    bool hasFlaredPortalHood = true;       // Beveled acoustic flare entrance hood to suppress sonic boom
    float portalRetainingWallHeightM = 12.0f; // Headwall retaining mountain face (m)
    float daylightAdaptationLouverLengthM = 15.0f; // Louver trellis mitigating driver "black hole" effect
    
    // Safety, Life Support & MEP Systems (NFPA 502)
    bool hasJetVentilationFans = true;     // Ceiling-mounted longitudinal reversible jet fans
    float jetFanSpacingM = 35.0f;          // Interval distance between twin jet fan pairs (m)
    bool hasEmergencyEgressNiches = true;  // Pressurized SOS phone niches & fire hose cabinets every 50m
    bool hasLinearLEDLightingStrips = true;// Continuous high-efficiency LED optical guide bands along springline
    bool hasCrossPassageBreakouts = true;  // Interconnected escape doors to parallel second bore
    
    // Materials & Photometrics
    float wallReflectanceAlbedo = 0.72f;   // High-reflectance white wall lining for energy saving
    float wallRoughness = 0.35f;
    float ledLuminaireIntensity = 6.5f;    // Entrance zone luminance boost (cd/m2)
};

} // namespace Road
} // namespace ApexAssets
