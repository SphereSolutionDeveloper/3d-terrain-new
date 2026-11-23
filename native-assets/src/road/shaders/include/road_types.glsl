// ==============================================================================
// Shared Road Network SSBO & UBO Struct Definitions (Vulkan 1.3 GLSL)
// Path: src/road/shaders/include/road_types.glsl
// ==============================================================================
#ifndef APEX_ROAD_TYPES_GLSL
#define APEX_ROAD_TYPES_GLSL

struct RoadGpuVertex {
    vec4 posAndRoughness;   // xyz = position (m), w = PBR roughness
    vec4 normalAndMetallic; // xyz = normal,       w = PBR metallic
    vec4 uvAndAoEmissive;   // xy  = UV,           z = AO, w = emissive
    vec4 color;             // rgba linear albedo
};

struct RoadSplineControlPoint {
    vec4 posAndWidth;       // xyz = centerline pos (m), w = roadWidthM
    vec4 speedBankPad;      // x = designSpeedKmH, y = elevationGainM, z = bankRad, w = curvature
};

struct RoadTileDescriptor {
    vec4 originAndSize;     // xy = world center XZ, z = tileSizeM, w = roadWidthM
    uvec4 flags;            // x = surfaceState (0..9), y = lodLevel, z = hasDrainage, w = seed
};

#endif // APEX_ROAD_TYPES_GLSL
