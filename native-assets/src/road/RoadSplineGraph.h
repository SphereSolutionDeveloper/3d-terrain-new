// ==============================================================================
// Modular Road Component: Hermite & Catmull-Rom Spline Alignment 3D Generator
// Path: native-assets/src/road/RoadSplineGraph.h
// Standards: AASHTO Table 3-7 Superelevation (tan(theta) = v^2 / (g * R))
// ==============================================================================
#pragma once

#include <cstdint>
#include <vector>

namespace ApexAssets {
namespace Road {

enum class SplineCurveType : uint32_t {
    ClothoidSCurve = 0,     // Inflecting reverse S-curve with smooth superelevation roll transition
    HairpinSwitchback,      // 180-degree climbing mountain hairpin with widened turn pad & guardrail
    CatmullRomCorridor,     // Multi-point undulating Catmull-Rom highway ribbon with valley piers
    BankedSpiralHelix       // Ascending 300-degree corkscrew flyover ramp with outer banking
};

struct RoadSplineParams {
    SplineCurveType type = SplineCurveType::ClothoidSCurve;
    float splineLengthM = 110.0f;      // Path extent / corridor span (60.0m to 180.0m)
    float roadWidthM = 10.5f;          // Asphalt pavement width (7.0m to 16.0m)
    float designSpeedKmH = 80.0f;      // Used to compute physics banking angle theta = atan(v^2 / (g*R))
    float elevationGainM = 8.0f;       // Vertical climb across spline length (0.0m to 22.0m)
    float curvatureIntensity = 1.0f;   // Control-point lateral amplitude multiplier (0.3 to 1.8)
};

struct RoadSplineVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float tangent[4];
    float color[4];      // Linear RGBA albedo
    float pbrParams[4];  // Roughness, Metallic, AO, Emissive
    uint32_t materialId; // 0=Asphalt, 1=Concrete/Curb/Pier, 2=Galvanized Steel, 3=Lane Markings, 4=Delin LED
};

struct RoadSplineMeshBuffer {
    std::vector<RoadSplineVertex> vertices;
    std::vector<uint32_t> indices;

    float minAABB[3];
    float maxAABB[3];

    uint32_t vertexCount = 0;
    uint32_t triangleCount = 0;
    float maxSuperelevationDeg = 0.0f;
    float minRadiusM = 0.0f;
};

class RoadSplineGraph {
public:
    RoadSplineGraph() = default;
    ~RoadSplineGraph() = default;

    static void EvaluateCatmullRom(
        const float p0[3], const float p1[3], const float p2[3], const float p3[3],
        float t, float outPos[3]
    );

    static float ComputeSuperelevationRad(float designSpeedKmH, float radiusM);

    static RoadSplineMeshBuffer GenerateSplineMesh(const RoadSplineParams& params);
};

} // namespace Road

// Legacy top-level symbol preserved
class RoadSplineGraph {
public:
    RoadSplineGraph() = default;
    virtual ~RoadSplineGraph() = default;
};

} // namespace ApexAssets
