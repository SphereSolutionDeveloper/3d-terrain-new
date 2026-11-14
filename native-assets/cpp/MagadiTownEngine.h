// ─── native-assets/cpp/MagadiTownEngine.h ─────────────────────────────────────
// Native C++ Magadi Town 3 km x 5 km (15 km2) @ 1.0 m Grid Resolution Terrain Engine
// High-Res DEM Calibration (Terrarium ~9.5m / SRTM 30m), WGS84 <-> Local Metric 1m Grid <-> UTM 37S,
// Lake Magadi Water Level & Bathymetry, Road & Building Footprints, and Relief Depth Meshes
#pragma once

#include "MagadiSceneEngine.h"
#include <vector>
#include <string>
#include <cstdint>

namespace custom_vulkan {

struct MagadiTownGeoBounds {
    double centerLat = -1.9012;
    double centerLon = 36.2870;
    double widthM = 3000.0;   // East-West 3.0 km (X: 0 .. 3000)
    double heightM = 5000.0;  // North-South 5.0 km (Y: 0 .. 5000)
    double minLat = 0.0;
    double maxLat = 0.0;
    double minLon = 0.0;
    double maxLon = 0.0;
    double metersPerDegLat = 110574.0;
    double metersPerDegLon = 111258.7;
    double originUtmEast = 0.0;
    double originUtmNorth = 0.0;
};

struct MagadiTownPointTelemetry {
    float xM = 0.0f;         // 0 .. 3000m (East from West edge)
    float yM = 0.0f;         // 0 .. 5000m (North from South edge)
    int cellCol = 0;         // 0 .. 2999
    int cellRow = 0;         // 0 .. 4999
    double lat = 0.0;
    double lon = 0.0;
    double utmEast = 0.0;
    double utmNorth = 0.0;
    float elevM = 600.0f;
    float slopeDeg = 0.0f;
    float waterDepthM = 0.0f;
    bool isWater = false;
};

struct MagadiTownRoadSegment2D {
    float x0 = 0.0f, y0 = 0.0f;
    float x1 = 0.0f, y1 = 0.0f;
    float halfWidthM = 3.5f;
};

struct MagadiTownBuildingRect2D {
    float minX = 0.0f, minY = 0.0f;
    float maxX = 0.0f, maxY = 0.0f;
};

struct MagadiTownWaterPolygon2D {
    std::vector<float> coordsXY; // interleaved [x0, y0, x1, y1, ...]
    float minX = 0.0f, minY = 0.0f;
    float maxX = 0.0f, maxY = 0.0f;
    bool isSaltPan = false;
};

class MagadiTownEngine {
public:
    static MagadiTownEngine& getInstance();

    static constexpr double DOMAIN_WIDTH_M = 3000.0;
    static constexpr double DOMAIN_HEIGHT_M = 5000.0;
    static constexpr double CENTER_LAT = -1.9012;
    static constexpr double CENTER_LON = 36.2870;

    MagadiTownGeoBounds getGeoBounds() const;

    // Coordinate Transforms (WGS84 <-> Local 3000x5000m 1m Grid <-> UTM Zone 37S)
    void latLonToLocalMetric(double lat, double lon, float& outXM, float& outYM) const;
    void localMetricToLatLon(float xM, float yM, double& outLat, double& outLon) const;
    void latLonToUtm37S(double lat, double lon, double& outEasting, double& outNorthing) const;

    // Ingest real high-res DEM control grid (Terrarium ~9.5m / SRTM 30m / COP-DEM) covering 3km x 5km
    void setDemControlGrid(int cols, int rows, const std::vector<float>& elevationsM);
    bool hasLiveDemData() const { return m_hasLiveDem; }

    // Configure Lake Magadi water level datum (meters ASL) and extracted OSM water/salt-pan polygons
    void setLakeAndWaterPolygons(float lakeLevelAslM, const std::vector<MagadiTownWaterPolygon2D>& waterPolys);
    float getLakeLevelAslM() const { return m_lakeLevelAslM; }

    // Register extracted road segments & building bounding pads for 1m terrain surface classification
    void setFeatureFootprints(const std::vector<MagadiTownRoadSegment2D>& roads,
                              const std::vector<MagadiTownBuildingRect2D>& buildings);

    // Sample continuous 1m-resolution elevation at any (xM, yM) in [0..3000] x [0..5000]
    float sampleElevationAt1m(float xM, float yM,
                              float contourInterval = 0.0f,
                              float stepTerracing = 0.0f) const;

    MagadiTownPointTelemetry getTelemetryAt(float xM, float yM) const;

    // Sample packed 2D raster for viewport (cols * rows * 3 floats: [elevM, hillshadeFactor, waterDepthM])
    std::vector<float> sampleElevationRaster(float minXM, float maxXM,
                                             float minYM, float maxYM,
                                             int cols, int rows) const;

    // Generate full 3 km x 5 km 3D Terrain Overview Mesh (proportional 3:5 aspect ratio + relief depth)
    MagadiSceneMesh generateOverview3x5Mesh(int resX = 61, int resZ = 101,
                                            float contourInterval = 10.0f,
                                            float stepTerracing = 0.0f,
                                            float verticalExaggeration = 2.4f) const;

    // Generate true 1.0m x 1.0m resolution local 3D Terrain Window centered at (centerXM, centerYM)
    MagadiSceneMesh generateLocal1mWindowMesh(float centerXM, float centerYM,
                                              int windowCells = 64,
                                              float contourInterval = 2.0f,
                                              float stepTerracing = 0.0f,
                                              float verticalExaggeration = 1.6f) const;

private:
    MagadiTownEngine();

    float pseudoNoise2D(float x, float y) const;
    float fbmNoise(float x, float y, int octaves, float lacunarity, float gain) const;
    float sampleBilinearDem(float xM, float yM) const;
    float computeBaselineMagadiElev(float xM, float yM) const;
    bool isPointOnRoad(float xM, float yM) const;
    bool isPointOnBuilding(float xM, float yM) const;
    bool isPointInWaterPolygon(float xM, float yM, bool& outIsSaltPan, float& outDepthFactor) const;
    float sampleLandElevationRaw(float xM, float yM) const;

    MagadiTownGeoBounds m_bounds;
    bool m_hasLiveDem = false;
    int m_demCols = 0;
    int m_demRows = 0;
    std::vector<float> m_demElevations;

    float m_lakeLevelAslM = 602.5f;
    std::vector<MagadiTownWaterPolygon2D> m_waterPolygons;

    std::vector<MagadiTownRoadSegment2D> m_roadSegments;
    std::vector<MagadiTownBuildingRect2D> m_buildingRects;
};

} // namespace custom_vulkan
