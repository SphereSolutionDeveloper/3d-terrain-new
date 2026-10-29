#pragma once

#include <vector>
#include <string>
#include <cmath>
#include "procedural_garage.hpp"

namespace apex {
namespace procedural {

enum class PowerPostType {
    TANGENT_STRAIGHT,        // Standard linear run pole with single crossarm
    CORNER_ANGLE,           // Turn/angle pole with dual crossarms and guy wire stay
    TRANSFORMER_DISTRIBUTION,// Pole with mounted step-down transformer tank & cutouts
    DEAD_END_TERMINAL       // Terminal termination pole with double guy wire anchors
};

struct PostConfig {
    PowerPostType type = PowerPostType::CORNER_ANGLE;
    float poleHeight = 10.5f;       // Mast height in meters
    float mastRadius = 0.18f;       // Tapered pole average radius
    float crossarmWidth = 2.4f;     // Crossarm span
    bool hasTransformer = false;    // 50kVA cylindrical transformer can
    bool hasStreetlight = true;     // Cantilever luminaire arm
    bool isStreetlightOn = true;    // Emissive yellow/warm white lamp
    bool hasGuyWire = true;         // Tension counterbalance guy wire stay
    float cornerAngleDeg = 75.0f;   // Horizontal deflection angle at corner
};

struct GridNetworkConfig {
    int numPosts = 4;               // 3 or 4 posts in grid sequence
    float spanDistanceM = 16.0f;    // Distance between poles (14m - 20m)
    float cornerAngleDeg = 85.0f;   // Turn angle at corner post (e.g. 75°-90°)
    float wireSagM = 0.55f;         // Catenary sag drop at mid-span
    bool energized = true;          // Power status
    bool hasStreetlights = true;
    bool streetlightGlow = true;
    float wireThickness = 0.035f;   // Extruded conductor diameter
};

class ProceduralPowerGrid {
public:
    // Builds a single modular electric pole with full mechanical fixtures
    static std::vector<Vertex3D> buildSinglePost(
        const PostConfig& config,
        float originX = 0.0f,
        float originY = 0.0f,
        float originZ = 0.0f,
        float orientationYawRad = 0.0f
    );

    // Builds a connected power grid network (3-4 poles with at least one corner turn and sagging cables)
    static std::vector<Vertex3D> buildPowerGridNetwork(const GridNetworkConfig& config);

    // Utility: generates multi-segment catenary cable between two 3D attachment points
    static void addCatenaryWire(
        std::vector<Vertex3D>& verts,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float sag,
        int numSegments,
        float thickness,
        float r, float g, float b, float a = 1.0f
    );
};

} // namespace procedural
} // namespace apex
