// ─── app/src/main/cpp/MagadiSceneEngine.h ─────────────────────────────────────
// Native C++ Magadi Road Elevation & Human Viewpoint Scene Engine
// Adhering to Sascha Willems Vulkan Infinite Procedural Terrain Architecture
#pragma once

#include "Math3D.h"
#include <vector>
#include <string>
#include <cstdint>

namespace custom_vulkan {

// Represents a 3D vertex in the Magadi terrain mesh
struct MagadiVertex {
    float x = 0.0f;
    float y = 0.0f;
    float z = 0.0f;
    float nx = 0.0f;
    float ny = 1.0f;
    float nz = 0.0f;
    float slopeDeg = 0.0f;
    float grassWeight = 0.0f;
    float dirtWeight = 0.0f;
    float rockWeight = 0.0f;
    float snowWeight = 0.0f; // Trona soda crust
    bool isRoadSurface = false;
    bool isWaterSurface = false;
    bool isMudSurface = false;
    bool isGravelSurface = false;
    float rawElevation = 598.0f;
    float contourBand = 0.0f;
    float isolineFactor = 0.0f;
    char surfaceType[32] = "savannah_scrub";
};

// Represents a 3D asset placement within the human viewpoint scene
struct MagadiAssetMarker {
    std::string id;
    std::string type;
    std::string modelPath;
    float posX = 0.0f;
    float posY = 0.0f;
    float posZ = 0.0f;
    float rotYDeg = 0.0f;
    float scale = 1.0f;
};

// Complete 3D Terrain Scene Mesh
struct MagadiSceneMesh {
    int resolutionX = 45;
    int resolutionZ = 45;
    std::vector<MagadiVertex> vertices;
    std::vector<uint32_t> indices;
    float minHeight = 0.0f;
    float maxHeight = 0.0f;
    float avgSlope = 0.0f;
    std::vector<MagadiAssetMarker> assetPlacements;
};

// Telemetry & human perception metrics
struct HumanViewpointMetrics {
    float groundDatumElev = 598.0f;      // Lake Magadi causeway base ASL
    float observerEyeElev = 599.70f;     // 1.70m standing datum
    float currentRoadElev = 598.0f;      // At walkDistanceM
    float horizonEscarpmentElev = 1980.0f; // Ngong summit / Corner Baridi
    float verticalClimbM = 1382.0f;      // Total climb to horizon
    float headingDeg = 28.0f;            // 028° NNE along C58 corridor
    float heatMirageDistM = 450.0f;      // Silvery water mirage optical distance
};

class MagadiSceneEngine {
public:
    static MagadiSceneEngine& getInstance();

    // Sample continuous contour-terraced elevation across the 50km corridor
    float getMagadiElevation(float worldXMeters, float worldYMeters,
                             float contourInterval = 25.0f,
                             float stepTerracing = 0.0f) const;

    // Generate detailed 3D Human Eye-Level Viewpoint Mesh at Lake Magadi Road Start (598m)
    MagadiSceneMesh generateHumanStartScene(float walkDistanceM = 0.0f,
                                           int gridSize = 45,
                                           float contourInterval = 25.0f,
                                           float stepTerracing = 0.0f) const;

    // Generate 50km Regional Corridor Overview Mesh
    MagadiSceneMesh generateOverviewScene(int gridSize = 45,
                                         float contourInterval = 25.0f,
                                         float stepTerracing = 0.0f) const;

    // Generate 10km Local Tile Chunk Mesh
    MagadiSceneMesh generateChunkScene(int col, int row,
                                      int gridSize = 41,
                                      float contourInterval = 25.0f,
                                      float stepTerracing = 0.0f) const;

    // Calculate perceptual telemetry at specified observer position and height
    HumanViewpointMetrics calculateHumanMetrics(float walkDistanceM, float eyeHeightOffsetM) const;

private:
    MagadiSceneEngine() = default;

    // Multi-octave FBM procedural noise generator in pure C++
    float pseudoNoise2D(float x, float y) const;
    float fbmNoise(float x, float y, int octaves, float lacunarity, float gain) const;
};

} // namespace custom_vulkan
