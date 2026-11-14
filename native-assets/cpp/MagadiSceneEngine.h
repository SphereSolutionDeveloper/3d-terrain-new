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

// Represents a 3D asset placement within the scene
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

// Represents a 3D road waypoint along the corridor
struct RoadWaypoint {
    float x = 0.0f;           // Scaled 3D mesh X coordinate (-45 to +45)
    float y = 0.0f;           // Scaled 3D mesh Y coordinate
    float z = 0.0f;           // Scaled 3D mesh Z coordinate (-50 to +50)
    float rawElevation = 0.0f;// Real-world elevation ASL in meters (road surface datum)
    float forwardM = 0.0f;    // Distance along segment in meters (0 to 10100m)
    float globalKmM = 0.0f;   // Total distance from Magadi (0 to 101 km)
    float lateralM = 0.0f;    // Real-world lateral offset from corridor center
    float gradePercent = 0.0f;// Gradient/slope percentage
    float headingDeg = 28.0f; // Compass heading
    float naturalElevM = 0.0f;// Undisturbed natural terrain elevation
    float cutFillDepthM = 0.0f;// roadElev - naturalElev (positive = fill embankment, negative = cut)
    float nextContourElevM = 0.0f; // Upcoming contour line elevation (e.g. 625m, 650m)
    float distToNextContourM = 0.0f; // Distance along road to next contour crossing
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
    std::vector<RoadWaypoint> roadCenterline;
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

// Information struct for human-friendly road segment telemetry (Segments 1 to 10)
struct RoadSegmentInfo {
    int segmentIndex = 1;          // 1 to 10
    char name[64] = "";
    float startKmM = 0.0f;
    float endKmM = 10.1f;
    float startElevM = 598.0f;
    float endElevM = 640.0f;
    float avgGradePercent = 0.44f;
    float minElevM = 590.0f;
    float maxElevM = 715.0f;
    float climbM = 42.0f;
    char landmark[64] = "";
    char primaryBiome[48] = "";
};

class MagadiSceneEngine {
public:
    static MagadiSceneEngine& getInstance();

    // Sample continuous contour-terraced elevation across the 101km corridor
    float getMagadiElevation(float worldXMeters, float worldYMeters,
                             float contourInterval = 25.0f,
                             float stepTerracing = 0.0f) const;

    // Road Segments 1 to 10 procedural generation in 100% C++
    RoadSegmentInfo getRoadSegmentInfo(int segmentIndex) const;
    RoadWaypoint getRoadPointAt(int segmentIndex, float forwardDistM) const;
    std::vector<RoadWaypoint> generateRoadCenterline(int segmentIndex, int numPoints = 64) const;

    MagadiSceneMesh generateRoadSegmentScene(int segmentIndex, int gridSize = 49,
                                            float contourInterval = 25.0f,
                                            float stepTerracing = 0.0f) const;

    // Further subdivide each 10.1km segment into 10 sub-segments (1.01km each)
    MagadiSceneMesh generateRoadSubSegmentScene(int segmentIndex, int subSegmentIndex,
                                               int gridSize = 49,
                                               float contourInterval = 25.0f,
                                               float stepTerracing = 0.0f) const;

    // Ultra-high 1m resolution scene (1m x 1m grid cells) around road position
    MagadiSceneMesh generate1mResolutionScene(int segmentIndex, float centerForwardM,
                                             float contourInterval = 5.0f,
                                             float stepTerracing = 0.0f) const;

    // 3D Geological Ground Piece Datum (400m ASL baseline)
    static constexpr float GROUND_PIECE_DATUM_ASL = 400.0f;
    float getGroundPieceDatumASL() const { return GROUND_PIECE_DATUM_ASL; }
    float getGroundPieceDepth(float elevationASL) const {
        return std::max(0.0f, elevationASL - GROUND_PIECE_DATUM_ASL);
    }

    // Generate Whole Combined 101km Terrain Corridor (All 10 segments unified into one 3D landscape)
    MagadiSceneMesh generateWholeCombinedTerrainScene(int gridResX = 33, int gridResZ = 129,
                                                     float contourInterval = 50.0f,
                                                     float stepTerracing = 0.0f) const;

    // Generate real 3D First 10km Segment Terrain (Magadi Basin to Fault Scarps)
    MagadiSceneMesh generateFirst10kmSegmentScene(int gridSize = 49,
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

    // Continuous corridor elevation computation for segments 1 to 10
    float computeCorridorElevation(int segmentIndex, float lateralM, float forwardM,
                                   float contourInterval, float stepTerracing,
                                   std::string& outSurface, bool& outIsRoad, bool& outIsGravel) const;

    // Natural elevation computation for the first 10km segment
    float computeSegmentElevation(float lateralM, float forwardM,
                                  float contourInterval, float stepTerracing) const;
};

} // namespace custom_vulkan
