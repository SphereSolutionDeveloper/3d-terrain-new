// ─── native-assets/cpp/C58CorridorEngine.h ────────────────────────────────────
// Native C++ Route C58 Non-Box 3 km Corridor Engine (1.5 km Left + 1.5 km Right of Centerline)
// Live OSM Centerline Ingestion, UTM Zone 37S <-> WGS84 projection, curvilinear (u, v) 1m DEM,
// NNW hillshade, water bathymetry, non-box raster masking, and curved 3D sector/1m ribbon meshes.
#pragma once

#include "MagadiTownEngine.h"
#include <vector>
#include <string>
#include <cstdint>
#include <unordered_map>
#include <shared_mutex>

namespace custom_vulkan {

struct C58CenterlineVertex {
    double lat = 0.0;
    double lon = 0.0;
    double utmEast = 0.0;
    double utmNorth = 0.0;
    double chainageM = 0.0;
    double rightNormE = 1.0; // Unit perpendicular pointing +Right (looking S -> N)
    double rightNormN = 0.0;
    double leftUtmE = 0.0;   // -1500 m Left edge
    double leftUtmN = 0.0;
    double rightUtmE = 0.0;  // +1500 m Right edge
    double rightUtmN = 0.0;
};

struct C58ProjectionResult {
    double utmEast = 0.0;
    double utmNorth = 0.0;
    double chainageM = 0.0;
    float lateralOffsetM = 0.0f; // negative = Left (-1500..0), positive = Right (0..+1500)
    bool insideCorridor = false; // abs(lateralOffsetM) <= 1500.0f and within chainage ends
};

struct C58PointTelemetry {
    double lat = 0.0;
    double lon = 0.0;
    double utmEast = 0.0;
    double utmNorth = 0.0;
    double chainageM = 0.0;
    float lateralOffsetM = 0.0f;
    bool insideCorridor = false;
    float elevM = 600.0f;
    float slopeDeg = 0.0f;
    float waterDepthM = 0.0f;
    bool isWater = false;
};

struct C58CorridorSummary {
    double totalLengthM = 101000.0;
    float halfWidthM = 1500.0f;
    int centerlinePoints = 0;
    double minUtmEast = 0.0;
    double maxUtmEast = 0.0;
    double minUtmNorth = 0.0;
    double maxUtmNorth = 0.0;
    double minLat = 0.0;
    double maxLat = 0.0;
    double minLon = 0.0;
    double maxLon = 0.0;
};

struct C58Sector3dTransform {
    double centerUtmE = 0.0;
    double centerUtmN = 0.0;
    float sceneScaleXZ = 0.006f;
    float yScalePerM = 0.04f;
    float baseDatumElev = 580.0f;
};

class C58CorridorEngine {
public:
    static C58CorridorEngine& getInstance();

    static constexpr float HALF_WIDTH_M = 1500.0f;
    static constexpr float FULL_WIDTH_M = 3000.0f;

    // Coordinate Transforms (WGS84 <-> UTM Zone 37S EPSG:32737)
    void latLonToUtm37S(double lat, double lon, double& outEasting, double& outNorthing) const;
    void utm37SToLatLon(double easting, double northing, double& outLat, double& outLon) const;

    // Default anchor Route C58 centerline (Magadi Causeway -> Bomas/Galleria)
    std::vector<double> getDefaultCenterlineLatLon() const;

    // Replace centerline with stitched live OSM Route C58 coordinates [lat0, lon0, lat1, lon1, ...]
    void setCenterline(const std::vector<double>& latLonPairs);

    C58CorridorSummary getSummary() const;

    // Returns flattened centerline + corridor boundary coordinates:
    // stride = 7 doubles: [centerE, centerN, leftE, leftN, rightE, rightN, chainageM]
    std::vector<double> getCorridorRibbonUtm() const;

    // Project any UTM Zone 37S point or WGS84 coordinate onto the Route C58 centerline
    C58ProjectionResult projectUtmPoint(double utmE, double utmN) const;
    C58ProjectionResult projectLatLon(double lat, double lon) const;

    // Curvilinear DEM control grid over u in [-1500m..+1500m], v in [0..totalLengthM]
    void setDemControlGrid(int uCols, int vRows, const std::vector<float>& elevationsM);
    bool hasLiveDemData() const;

    // Configure Lake / Water datum & water polygons using full precision UTM 37S doubles
    void setLakeAndWaterPolygonsUtm(float lakeLevelAslM,
                                    const std::vector<int>& polyMetaPacked,
                                    const std::vector<double>& coordsEN);
    float getLakeLevelAslM() const;

    // Register roads ([e0, n0, e1, n1, halfWidthM] * N) and building pads ([minE, minN, maxE, maxN] * M)
    void setFeatureFootprintsUtm(const std::vector<double>& roadSegsUtm5,
                                 const std::vector<double>& buildingRectsUtm4);

    // Sample continuous 1m elevation at UTM (E, N)
    float sampleElevationAtUtm(double utmE, double utmN,
                               float contourInterval = 0.0f,
                               float stepTerracing = 0.0f) const;

    // Full telemetry at UTM (E, N)
    C58PointTelemetry getTelemetryAtUtm(double utmE, double utmN) const;

    // Sample packed 2D raster over UTM bbox:
    // cols * rows * 4 floats: [elevM, hillshadeFactor, waterDepthM, insideCorridorMask(1.0 or 0.0)]
    std::vector<float> sampleUtmRaster(double minUtmE, double maxUtmE,
                                       double minUtmN, double maxUtmN,
                                       int cols, int rows) const;

    // Compute exact UTM -> 3D scene transform matching generateSector3dMesh
    C58Sector3dTransform getSector3dTransform(double sectorStartM, double sectorEndM,
                                              int resU = 45, int resV = 85,
                                              float verticalExaggeration = 2.2f) const;

    // Generate true curved non-box 3 km-wide 3D sector mesh for chainage [sectorStartM .. sectorEndM]
    MagadiSceneMesh generateSector3dMesh(double sectorStartM, double sectorEndM,
                                         int resU = 45, int resV = 85,
                                         float contourInterval = 15.0f,
                                         float stepTerracing = 0.0f,
                                         float verticalExaggeration = 2.2f) const;

    // Generate true 1.0m x 1.0m resolution local 3D window centered at UTM (centerE, centerN)
    MagadiSceneMesh generateWindow1mMesh(double centerUtmE, double centerUtmN,
                                         int windowCells = 64,
                                         float contourInterval = 2.0f,
                                         float stepTerracing = 0.0f,
                                         float verticalExaggeration = 1.6f) const;

private:
    C58CorridorEngine();

    float pseudoNoise2D(float x, float y) const;
    float fbmNoise(float x, float y, int octaves, float lacunarity, float gain) const;
    float computeBaselineProfileElev(double chainageM, float offsetM) const;
    float sampleDemCurvilinear(double chainageM, float offsetM) const;
    float sampleLandElevationRaw(double utmE, double utmN, const C58ProjectionResult& proj) const;
    bool isPointOnRoadUtm(double utmE, double utmN) const;
    bool isPointOnBuildingUtm(double utmE, double utmN) const;
    bool isUtmInWaterPolygon(double utmE, double utmN, bool& outIsSaltPan, float& outDepthFactor) const;
    void evaluateWaterAndElevation(double utmE, double utmN, const C58ProjectionResult& proj,
                                   float& outElevM, float& outWaterDepthM, bool& outIsWater) const;
    C58ProjectionResult projectUtmPointUnlocked(double utmE, double utmN) const;
    float sampleElevationAtUtmUnlocked(double utmE, double utmN,
                                       float contourInterval = 0.0f,
                                       float stepTerracing = 0.0f) const;
    void rebuildGeometryMetrics();

    struct RoadSegUtm {
        double e0 = 0.0, n0 = 0.0, e1 = 0.0, n1 = 0.0;
        double minE = 0.0, maxE = 0.0, minN = 0.0, maxN = 0.0;
        float halfWidthM = 4.0f;
    };
    struct BldgRectUtm {
        double minE = 0.0, minN = 0.0, maxE = 0.0, maxN = 0.0;
    };
    struct WaterPolyUtm {
        std::vector<double> coordsEN;
        double minE = 0.0, minN = 0.0, maxE = 0.0, maxN = 0.0;
        bool isSaltPan = false;
    };

    static constexpr double SPATIAL_CELL_M = 200.0;
    static int64_t spatialKey(int32_t cx, int32_t cy) {
        return (static_cast<int64_t>(cx) << 32) | static_cast<uint32_t>(cy);
    }

    mutable std::shared_mutex m_mutex;
    std::vector<C58CenterlineVertex> m_centerline;
    C58CorridorSummary m_summary;

    bool m_hasLiveDem = false;
    int m_demCols = 0;
    int m_demRows = 0;
    std::vector<float> m_demElevations;

    float m_lakeLevelAslM = 602.5f;
    std::vector<WaterPolyUtm> m_waterPolys;
    std::vector<RoadSegUtm> m_roadSegs;
    std::vector<BldgRectUtm> m_bldgRects;
    std::unordered_map<int64_t, std::vector<uint32_t>> m_roadGrid;
    std::unordered_map<int64_t, std::vector<uint32_t>> m_bldgGrid;
};

} // namespace custom_vulkan
