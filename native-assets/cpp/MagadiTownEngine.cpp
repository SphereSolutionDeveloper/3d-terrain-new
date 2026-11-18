// ─── native-assets/cpp/MagadiTownEngine.cpp ───────────────────────────────────
// Native C++ Magadi Town 3 km x 5 km (15 km2) @ 1.0 m Grid Resolution Terrain Engine
#include "MagadiTownEngine.h"
#include <cmath>
#include <cstring>
#include <algorithm>
#include <mutex>

namespace custom_vulkan {

namespace {
constexpr double PI_D = 3.14159265358979323846;
constexpr double DEG2RAD_D = PI_D / 180.0;
}

MagadiTownEngine& MagadiTownEngine::getInstance() {
    static MagadiTownEngine instance;
    return instance;
}

MagadiTownEngine::MagadiTownEngine() {
    m_bounds.centerLat = CENTER_LAT;
    m_bounds.centerLon = CENTER_LON;
    m_bounds.widthM = DOMAIN_WIDTH_M;
    m_bounds.heightM = DOMAIN_HEIGHT_M;
    m_bounds.metersPerDegLat = 110574.0;
    m_bounds.metersPerDegLon = 111320.0 * std::cos(CENTER_LAT * DEG2RAD_D);

    double halfLatDeg = (DOMAIN_HEIGHT_M * 0.5) / m_bounds.metersPerDegLat;
    double halfLonDeg = (DOMAIN_WIDTH_M * 0.5) / m_bounds.metersPerDegLon;

    m_bounds.minLat = CENTER_LAT - halfLatDeg;
    m_bounds.maxLat = CENTER_LAT + halfLatDeg;
    m_bounds.minLon = CENTER_LON - halfLonDeg;
    m_bounds.maxLon = CENTER_LON + halfLonDeg;

    latLonToUtm37S(m_bounds.minLat, m_bounds.minLon, m_bounds.originUtmEast, m_bounds.originUtmNorth);
}

MagadiTownGeoBounds MagadiTownEngine::getGeoBounds() const {
    return m_bounds;
}

void MagadiTownEngine::latLonToLocalMetric(double lat, double lon, float& outXM, float& outYM) const {
    outXM = static_cast<float>((lon - m_bounds.minLon) * m_bounds.metersPerDegLon);
    outYM = static_cast<float>((lat - m_bounds.minLat) * m_bounds.metersPerDegLat);
}

void MagadiTownEngine::localMetricToLatLon(float xM, float yM, double& outLat, double& outLon) const {
    outLon = m_bounds.minLon + static_cast<double>(xM) / m_bounds.metersPerDegLon;
    outLat = m_bounds.minLat + static_cast<double>(yM) / m_bounds.metersPerDegLat;
}

void MagadiTownEngine::latLonToUtm37S(double lat, double lon, double& outEasting, double& outNorthing) const {
    constexpr double a = 6378137.0;
    constexpr double f = 1.0 / 298.257223563;
    constexpr double e2 = f * (2.0 - f);
    constexpr double ep2 = e2 / (1.0 - e2);
    constexpr double k0 = 0.9996;
    constexpr double lon0 = 39.0 * DEG2RAD_D;

    double phi = lat * DEG2RAD_D;
    double lam = lon * DEG2RAD_D;
    double sinP = std::sin(phi);
    double cosP = std::cos(phi);
    double tanP = std::tan(phi);

    double N = a / std::sqrt(1.0 - e2 * sinP * sinP);
    double T = tanP * tanP;
    double C = ep2 * cosP * cosP;
    double A = cosP * (lam - lon0);

    double M = a * ((1.0 - e2 / 4.0 - 3.0 * e2 * e2 / 64.0 - 5.0 * e2 * e2 * e2 / 256.0) * phi
        - (3.0 * e2 / 8.0 + 3.0 * e2 * e2 / 32.0 + 45.0 * e2 * e2 * e2 / 1024.0) * std::sin(2.0 * phi)
        + (15.0 * e2 * e2 / 256.0 + 45.0 * e2 * e2 * e2 / 1024.0) * std::sin(4.0 * phi)
        - (35.0 * e2 * e2 * e2 / 3072.0) * std::sin(6.0 * phi));

    outEasting = 500000.0 + k0 * N * (A + (1.0 - T + C) * A * A * A / 6.0
        + (5.0 - 18.0 * T + T * T + 72.0 * C - 58.0 * ep2) * std::pow(A, 5) / 120.0);
    double northingRaw = k0 * (M + N * tanP * (A * A / 2.0
        + (5.0 - T + 9.0 * C + 4.0 * C * C) * std::pow(A, 4) / 24.0
        + (61.0 - 58.0 * T + T * T + 600.0 * C - 330.0 * ep2) * std::pow(A, 6) / 720.0));
    outNorthing = (lat < 0.0) ? (10000000.0 + northingRaw) : northingRaw;
}

float MagadiTownEngine::pseudoNoise2D(float x, float y) const {
    float n = std::sin(x * 12.9898f + y * 78.233f) * 43758.5453123f;
    return n - std::floor(n);
}

float MagadiTownEngine::fbmNoise(float x, float y, int octaves, float lacunarity, float gain) const {
    float sum = 0.0f;
    float amp = 1.0f;
    float freq = 1.0f;
    float maxAmp = 0.0f;

    for (int i = 0; i < octaves; ++i) {
        float sx = x * freq;
        float sy = y * freq;
        int ix = static_cast<int>(std::floor(sx));
        int iy = static_cast<int>(std::floor(sy));
        float fx = sx - ix;
        float fy = sy - iy;
        float u = fx * fx * (3.0f - 2.0f * fx);
        float v = fy * fy * (3.0f - 2.0f * fy);

        float n00 = pseudoNoise2D(static_cast<float>(ix), static_cast<float>(iy));
        float n10 = pseudoNoise2D(static_cast<float>(ix + 1), static_cast<float>(iy));
        float n01 = pseudoNoise2D(static_cast<float>(ix), static_cast<float>(iy + 1));
        float n11 = pseudoNoise2D(static_cast<float>(ix + 1), static_cast<float>(iy + 1));

        float nx0 = n00 * (1.0f - u) + n10 * u;
        float nx1 = n01 * (1.0f - u) + n11 * u;
        sum += (nx0 * (1.0f - v) + nx1 * v) * amp;
        maxAmp += amp;
        amp *= gain;
        freq *= lacunarity;
    }
    return maxAmp > 0.0f ? (sum / maxAmp) : 0.0f;
}

void MagadiTownEngine::setDemControlGrid(int cols, int rows, const std::vector<float>& elevationsM) {
    if (cols >= 2 && rows >= 2 && static_cast<int>(elevationsM.size()) >= cols * rows) {
        std::unique_lock<std::shared_mutex> lock(m_mutex);
        m_demCols = cols;
        m_demRows = rows;
        const int count = cols * rows;
        m_demElevations.resize(static_cast<size_t>(count));
        for (int i = 0; i < count; ++i) {
            float e = elevationsM[static_cast<size_t>(i)];
            m_demElevations[static_cast<size_t>(i)] = std::isfinite(e) ? e : 620.0f;
        }
        m_hasLiveDem = true;
    }
}

bool MagadiTownEngine::hasLiveDemData() const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return m_hasLiveDem;
}

void MagadiTownEngine::setLakeAndWaterPolygons(
    float lakeLevelAslM,
    const std::vector<MagadiTownWaterPolygon2D>& waterPolys) {
    std::unique_lock<std::shared_mutex> lock(m_mutex);
    m_lakeLevelAslM = std::isfinite(lakeLevelAslM) ? lakeLevelAslM : 602.5f;
    m_waterPolygons = waterPolys;
}

float MagadiTownEngine::getLakeLevelAslM() const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return m_lakeLevelAslM;
}

void MagadiTownEngine::setFeatureFootprints(
    const std::vector<MagadiTownRoadSegment2D>& roads,
    const std::vector<MagadiTownBuildingRect2D>& buildings) {
    std::unique_lock<std::shared_mutex> lock(m_mutex);
    m_roadSegments = roads;
    m_buildingRects = buildings;
}

static float catmullRom(float p0, float p1, float p2, float p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5f * ((2.0f * p1)
        + (-p0 + p2) * t
        + (2.0f * p0 - 5.0f * p1 + 4.0f * p2 - p3) * t2
        + (-p0 + 3.0f * p1 - 3.0f * p2 + p3) * t3);
}

float MagadiTownEngine::sampleBilinearDem(float xM, float yM) const {
    if (!m_hasLiveDem || m_demCols < 2 || m_demRows < 2) {
        return computeBaselineMagadiElev(xM, yM);
    }

    float nx = std::clamp(xM / static_cast<float>(DOMAIN_WIDTH_M), 0.0f, 1.0f);
    float ny = std::clamp(yM / static_cast<float>(DOMAIN_HEIGHT_M), 0.0f, 1.0f);

    float gx = nx * static_cast<float>(m_demCols - 1);
    float gy = ny * static_cast<float>(m_demRows - 1);

    int ix = std::clamp(static_cast<int>(std::floor(gx)), 0, m_demCols - 2);
    int iy = std::clamp(static_cast<int>(std::floor(gy)), 0, m_demRows - 2);
    float tx = gx - ix;
    float ty = gy - iy;

    auto at = [&](int c, int r) -> float {
        int cc = std::clamp(c, 0, m_demCols - 1);
        int rr = std::clamp(r, 0, m_demRows - 1);
        return m_demElevations[rr * m_demCols + cc];
    };

    float rowVals[4];
    for (int j = -1; j <= 2; ++j) {
        rowVals[j + 1] = catmullRom(
            at(ix - 1, iy + j),
            at(ix, iy + j),
            at(ix + 1, iy + j),
            at(ix + 2, iy + j),
            tx
        );
    }
    return catmullRom(rowVals[0], rowVals[1], rowVals[2], rowVals[3], ty);
}

float MagadiTownEngine::computeBaselineMagadiElev(float xM, float yM) const {
    float nx = std::clamp(xM / static_cast<float>(DOMAIN_WIDTH_M), 0.0f, 1.0f);
    float ny = std::clamp(yM / static_cast<float>(DOMAIN_HEIGHT_M), 0.0f, 1.0f);

    // Physical Rift basin floor (Lake Magadi ~596m on west) rising sharply to Magadi peninsula & eastern scarp (~685-760m)
    float lakeBasin = 595.5f;
    float eastRise = std::pow(std::max(0.0f, nx - 0.24f) / 0.76f, 1.38f) * 105.0f;
    float northGrad = ny * 26.0f;
    float townBench = std::exp(-((nx - 0.56f) * (nx - 0.56f) / 0.055f + (ny - 0.48f) * (ny - 0.48f) / 0.075f)) * 24.0f;
    float scarpNoise = (fbmNoise(xM * 0.0014f, yM * 0.0014f, 4, 2.0f, 0.5f) - 0.5f) * 28.0f;
    return lakeBasin + eastRise + northGrad + townBench + scarpNoise;
}

bool MagadiTownEngine::isPointInWaterPolygon(
    float xM, float yM, bool& outIsSaltPan, float& outDepthFactor) const {

    for (const auto& poly : m_waterPolygons) {
        if (xM < poly.minX || xM > poly.maxX || yM < poly.minY || yM > poly.maxY) continue;
        size_t n = poly.coordsXY.size() / 2;
        if (n < 3) continue;

        bool inside = false;
        float minEdgeDistSq = 1e12f;
        for (size_t i = 0, j = n - 1; i < n; j = i++) {
            float xi = poly.coordsXY[i * 2];
            float yi = poly.coordsXY[i * 2 + 1];
            float xj = poly.coordsXY[j * 2];
            float yj = poly.coordsXY[j * 2 + 1];

            float denom = yj - yi;
            bool intersect = ((yi > yM) != (yj > yM)) &&
                (std::abs(denom) > 1e-6f) &&
                (xM < (xj - xi) * (yM - yi) / denom + xi);
            if (intersect) inside = !inside;

            float dx = xj - xi;
            float dy = yj - yi;
            float lenSq = dx * dx + dy * dy;
            float t = (lenSq > 1e-4f) ? std::clamp(((xM - xi) * dx + (yM - yi) * dy) / lenSq, 0.0f, 1.0f) : 0.0f;
            float px = xi + t * dx;
            float py = yi + t * dy;
            float dSq = (xM - px) * (xM - px) + (yM - py) * (yM - py);
            if (dSq < minEdgeDistSq) minEdgeDistSq = dSq;
        }
        if (inside) {
            outIsSaltPan = poly.isSaltPan;
            float distEdgeM = std::sqrt(minEdgeDistSq);
            outDepthFactor = std::clamp(distEdgeM / 90.0f, 0.12f, 1.0f);
            return true;
        }
    }
    outIsSaltPan = false;
    outDepthFactor = 0.0f;
    return false;
}

float MagadiTownEngine::sampleLandElevationRaw(float xM, float yM) const {
    float cx = std::clamp(xM, 0.0f, static_cast<float>(DOMAIN_WIDTH_M));
    float cy = std::clamp(yM, 0.0f, static_cast<float>(DOMAIN_HEIGHT_M));

    float macroElev = sampleBilinearDem(cx, cy);

    // Rich 1m-resolution rocky scarp & gulley micro-relief (attenuated inside engineered roads/buildings)
    float meso = (fbmNoise(cx * 0.0085f, cy * 0.0085f, 3, 2.0f, 0.5f) - 0.5f) * 2.4f;
    float micro = (fbmNoise(cx * 0.048f, cy * 0.048f, 3, 2.1f, 0.45f) - 0.5f) * 1.15f;
    return macroElev + meso + micro;
}

float MagadiTownEngine::sampleElevationAt1mUnlocked(float xM, float yM,
                                                     float contourInterval,
                                                     float stepTerracing) const {
    float cx = std::clamp(xM, 0.0f, static_cast<float>(DOMAIN_WIDTH_M));
    float cy = std::clamp(yM, 0.0f, static_cast<float>(DOMAIN_HEIGHT_M));

    float rawElev = sampleLandElevationRaw(cx, cy);
    if (!std::isfinite(rawElev)) rawElev = 620.0f;

    bool isSaltPan = false;
    float edgeFactor = 0.0f;
    if (isPointInWaterPolygon(cx, cy, isSaltPan, edgeFactor)) {
        float maxDepth = isSaltPan ? 1.8f : 4.2f;
        float bedNoise = fbmNoise(cx * 0.012f, cy * 0.012f, 2, 2.0f, 0.5f) * 0.8f;
        float targetBed = m_lakeLevelAslM - (0.35f + edgeFactor * maxDepth + bedNoise);
        rawElev = std::min(rawElev, targetBed);
    }

    if (stepTerracing > 0.001f && std::isfinite(contourInterval) && contourInterval > 0.25f) {
        float stepped = std::round(rawElev / contourInterval) * contourInterval;
        rawElev = rawElev * (1.0f - stepTerracing) + stepped * stepTerracing;
    }
    return std::isfinite(rawElev) ? rawElev : 620.0f;
}

float MagadiTownEngine::sampleElevationAt1m(float xM, float yM,
                                            float contourInterval,
                                            float stepTerracing) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return sampleElevationAt1mUnlocked(xM, yM, contourInterval, stepTerracing);
}

bool MagadiTownEngine::isPointOnRoad(float xM, float yM) const {
    for (const auto& seg : m_roadSegments) {
        float minX = std::min(seg.x0, seg.x1) - seg.halfWidthM;
        float maxX = std::max(seg.x0, seg.x1) + seg.halfWidthM;
        float minY = std::min(seg.y0, seg.y1) - seg.halfWidthM;
        float maxY = std::max(seg.y0, seg.y1) + seg.halfWidthM;
        if (xM < minX || xM > maxX || yM < minY || yM > maxY) continue;

        float dx = seg.x1 - seg.x0;
        float dy = seg.y1 - seg.y0;
        float lenSq = dx * dx + dy * dy;
        float t = (lenSq > 1e-4f) ? std::clamp(((xM - seg.x0) * dx + (yM - seg.y0) * dy) / lenSq, 0.0f, 1.0f) : 0.0f;
        float px = seg.x0 + t * dx;
        float py = seg.y0 + t * dy;
        float distSq = (xM - px) * (xM - px) + (yM - py) * (yM - py);
        if (distSq <= seg.halfWidthM * seg.halfWidthM) return true;
    }
    return false;
}

bool MagadiTownEngine::isPointOnBuilding(float xM, float yM) const {
    for (const auto& b : m_buildingRects) {
        if (xM >= b.minX && xM <= b.maxX && yM >= b.minY && yM <= b.maxY) {
            return true;
        }
    }
    return false;
}

MagadiTownPointTelemetry MagadiTownEngine::getTelemetryAt(float xM, float yM) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    MagadiTownPointTelemetry t;
    t.xM = std::clamp(xM, 0.0f, static_cast<float>(DOMAIN_WIDTH_M));
    t.yM = std::clamp(yM, 0.0f, static_cast<float>(DOMAIN_HEIGHT_M));
    t.cellCol = std::clamp(static_cast<int>(std::floor(t.xM)), 0, static_cast<int>(DOMAIN_WIDTH_M) - 1);
    t.cellRow = std::clamp(static_cast<int>(std::floor(t.yM)), 0, static_cast<int>(DOMAIN_HEIGHT_M) - 1);

    localMetricToLatLon(t.xM, t.yM, t.lat, t.lon);
    latLonToUtm37S(t.lat, t.lon, t.utmEast, t.utmNorth);

    float eC = sampleElevationAt1mUnlocked(t.xM, t.yM);
    float eL = sampleElevationAt1mUnlocked(t.xM - 1.0f, t.yM);
    float eR = sampleElevationAt1mUnlocked(t.xM + 1.0f, t.yM);
    float eD = sampleElevationAt1mUnlocked(t.xM, t.yM - 1.0f);
    float eU = sampleElevationAt1mUnlocked(t.xM, t.yM + 1.0f);

    float ddx = (eR - eL) * 0.5f;
    float ddy = (eU - eD) * 0.5f;
    float grad = std::sqrt(ddx * ddx + ddy * ddy);
    t.elevM = eC;
    t.slopeDeg = std::atan(grad) * RAD2DEG;

    bool isSaltPan = false;
    float depthFactor = 0.0f;
    bool inWaterPoly = isPointInWaterPolygon(t.xM, t.yM, isSaltPan, depthFactor);
    t.isWater = inWaterPoly || (eC <= m_lakeLevelAslM);
    t.waterDepthM = t.isWater ? std::max(0.10f, m_lakeLevelAslM - eC) : 0.0f;
    return t;
}

std::vector<float> MagadiTownEngine::sampleElevationRaster(
    float minXM, float maxXM, float minYM, float maxYM, int cols, int rows) const {

    std::shared_lock<std::shared_mutex> lock(m_mutex);
    cols = std::clamp(cols, 2, 160);
    rows = std::clamp(rows, 2, 160);
    float spanX = std::max(1.0f, maxXM - minXM);
    float spanY = std::max(1.0f, maxYM - minYM);
    float dxStep = spanX / static_cast<float>(cols - 1);
    float dyStep = spanY / static_cast<float>(rows - 1);
    float gradSpan = std::max(1.0f, std::min(dxStep, dyStep));

    // Returns 3 floats per cell: [elevM, directionalHillshade, waterDepthM]
    std::vector<float> out(static_cast<size_t>(cols * rows) * 3);
    for (int r = 0; r < rows; ++r) {
        float yM = minYM + r * dyStep;
        for (int c = 0; c < cols; ++c) {
            float xM = minXM + c * dxStep;
            float e = sampleElevationAt1mUnlocked(xM, yM);
            float eL = sampleElevationAt1mUnlocked(xM - gradSpan, yM);
            float eR = sampleElevationAt1mUnlocked(xM + gradSpan, yM);
            float eD = sampleElevationAt1mUnlocked(xM, yM - gradSpan);
            float eU = sampleElevationAt1mUnlocked(xM, yM + gradSpan);

            // Enhanced NNW directional hillshade (exaggerated relief for crisp valleys & ridges)
            float gx = ((eR - eL) / (2.0f * gradSpan)) * 3.0f;
            float gy = ((eU - eD) / (2.0f * gradSpan)) * 3.0f;
            float nLen = std::sqrt(gx * gx + gy * gy + 1.0f);
            float nx = -gx / nLen;
            float ny = -gy / nLen;
            float nz = 1.0f / nLen;
            // Sun from NNW at 34 deg altitude -> crisp ridge highlights + trough depth shadows
            float dot = nx * (-0.58f) + ny * (0.58f) + nz * (0.57f);
            float hillshade = std::clamp(0.32f + 0.96f * dot, 0.28f, 1.30f);

            bool isSalt = false;
            float depthFac = 0.0f;
            bool inWater = isPointInWaterPolygon(xM, yM, isSalt, depthFac);
            float waterDepth = (e <= m_lakeLevelAslM || inWater) ? std::max(0.15f, m_lakeLevelAslM - e) : 0.0f;

            size_t idx = static_cast<size_t>(r * cols + c) * 3;
            out[idx + 0] = e;
            out[idx + 1] = hillshade;
            out[idx + 2] = waterDepth;
        }
    }
    return out;
}

MagadiSceneMesh MagadiTownEngine::generateOverview3x5Mesh(
    int resX, int resZ, float contourInterval, float stepTerracing, float verticalExaggeration) const {

    std::shared_lock<std::shared_mutex> lock(m_mutex);
    resX = std::clamp(resX, 17, 73);
    resZ = std::clamp(resZ, 17, 121);
    float vExag = std::clamp(verticalExaggeration, 0.5f, 6.0f);

    const float widthM = static_cast<float>(DOMAIN_WIDTH_M);
    const float heightM = static_cast<float>(DOMAIN_HEIGHT_M);
    const float stepX = widthM / static_cast<float>(resX - 1);
    const float stepZ = heightM / static_cast<float>(resZ - 1);

    // Exact 3:5 aspect ratio in 3D scene space
    const float sceneWidth = 48.0f;
    const float sceneDepth = 80.0f;

    std::vector<float> heights(resX * resZ);
    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < resZ; ++iz) {
        float yM = iz * stepZ;
        for (int ix = 0; ix < resX; ++ix) {
            float xM = ix * stepX;
            float e = sampleElevationAt1mUnlocked(xM, yM, contourInterval, stepTerracing);
            heights[iz * resX + ix] = e;
            if (e < minH) minH = e;
            if (e > maxH) maxH = e;
        }
    }

    MagadiSceneMesh mesh;
    mesh.resolutionX = resX;
    mesh.resolutionZ = resZ;
    mesh.minHeight = minH;
    mesh.maxHeight = maxH;
    mesh.vertices.reserve(resX * resZ);

    float baseDatumElev = (minH + maxH) * 0.5f;
    float totalSlope = 0.0f;
    float reliefSpan = std::max(10.0f, maxH - minH);
    float yScalePerM = 0.12f * vExag;

    for (int iz = 0; iz < resZ; ++iz) {
        float yM = iz * stepZ;
        float zScaled = ((yM - heightM * 0.5f) / heightM) * sceneDepth;
        for (int ix = 0; ix < resX; ++ix) {
            float xM = ix * stepX;
            float xScaled = ((xM - widthM * 0.5f) / widthM) * sceneWidth;
            float rawElev = heights[iz * resX + ix];
            float yScaled = (rawElev - baseDatumElev) * yScalePerM;

            float left = (ix > 0) ? heights[iz * resX + (ix - 1)] : rawElev;
            float right = (ix < resX - 1) ? heights[iz * resX + (ix + 1)] : rawElev;
            float down = (iz > 0) ? heights[(iz - 1) * resX + ix] : rawElev;
            float up = (iz < resZ - 1) ? heights[(iz + 1) * resX + ix] : rawElev;

            float realDx = (right - left) / (2.0f * stepX);
            float realDz = (up - down) / (2.0f * stepZ);
            float realLen = std::sqrt(realDx * realDx + 1.0f + realDz * realDz);
            float slopeDeg = std::acos(std::clamp(1.0f / realLen, 0.0f, 1.0f)) * RAD2DEG;
            totalSlope += slopeDeg;

            // Visually amplified normal so scarps, valleys & lake depression catch bold directional lighting
            float vDx = realDx * (vExag * 2.2f);
            float vDz = realDz * (vExag * 2.2f);
            float vLen = std::sqrt(vDx * vDx + 1.0f + vDz * vDz);
            float nx = -vDx / vLen;
            float ny = 1.0f / vLen;
            float nz = -vDz / vLen;

            bool onRoad = isPointOnRoad(xM, yM);
            bool onBldg = isPointOnBuilding(xM, yM);
            bool isSalt = false;
            float depthFac = 0.0f;
            bool inWaterPoly = isPointInWaterPolygon(xM, yM, isSalt, depthFac);
            bool isLake = (rawElev <= m_lakeLevelAslM + 0.05f || inWaterPoly);
            bool isTrona = (!isLake && rawElev <= m_lakeLevelAslM + 3.5f);

            MagadiVertex v{};
            v.x = xScaled;
            v.y = yScaled;
            v.z = zScaled;
            v.nx = nx;
            v.ny = ny;
            v.nz = nz;
            v.slopeDeg = slopeDeg;
            v.isRoadSurface = onRoad && !isLake;
            v.isWaterSurface = isLake;
            v.isMudSurface = isTrona;
            v.isGravelSurface = onBldg && !isLake;
            v.grassWeight = (!onRoad && !isLake && !isTrona && slopeDeg < 18.0f) ? 1.0f : 0.0f;
            v.dirtWeight = (onBldg && !isLake) ? 1.0f : 0.0f;
            v.rockWeight = (slopeDeg >= 18.0f) ? 0.90f : 0.0f;
            v.snowWeight = isTrona ? 1.0f : 0.0f;
            v.rawElevation = rawElev;
            v.contourBand = std::clamp((rawElev - minH) / reliefSpan, 0.0f, 1.0f);
            float cInt = std::max(1.0f, contourInterval);
            v.isolineFactor = std::abs(std::fmod(rawElev, cInt) / cInt - 0.5f) * 2.0f;
            std::strncpy(v.surfaceType,
                         isLake ? "water" : (v.isRoadSurface ? "asphalt" : (v.isGravelSurface ? "gravel" : (isTrona ? "trona_soda_crust" : "savannah_scrub"))),
                         sizeof(v.surfaceType) - 1);
            mesh.vertices.push_back(v);
        }
    }

    mesh.avgSlope = totalSlope / static_cast<float>(resX * resZ);
    mesh.indices.reserve((resX - 1) * (resZ - 1) * 6);
    for (int iz = 0; iz < resZ - 1; ++iz) {
        for (int ix = 0; ix < resX - 1; ++ix) {
            uint32_t tl = iz * resX + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * resX + ix;
            uint32_t br = bl + 1;
            mesh.indices.push_back(tl);
            mesh.indices.push_back(bl);
            mesh.indices.push_back(tr);
            mesh.indices.push_back(tr);
            mesh.indices.push_back(bl);
            mesh.indices.push_back(br);
        }
    }
    return mesh;
}

MagadiSceneMesh MagadiTownEngine::generateLocal1mWindowMesh(
    float centerXM, float centerYM, int windowCells,
    float contourInterval, float stepTerracing, float verticalExaggeration) const {

    std::shared_lock<std::shared_mutex> lock(m_mutex);
    windowCells = std::clamp(windowCells, 24, 80);
    int gridSize = windowCells + 1;
    float halfSpan = static_cast<float>(windowCells) * 0.5f;
    float vExag = std::clamp(verticalExaggeration, 0.5f, 6.0f);

    float cX = std::clamp(centerXM, halfSpan, static_cast<float>(DOMAIN_WIDTH_M) - halfSpan);
    float cY = std::clamp(centerYM, halfSpan, static_cast<float>(DOMAIN_HEIGHT_M) - halfSpan);

    const float stepM = 1.0f; // Exact 1.0m grid resolution!
    const float sceneSpan = 56.0f;

    std::vector<float> heights(gridSize * gridSize);
    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float yM = (cY - halfSpan) + iz * stepM;
        for (int ix = 0; ix < gridSize; ++ix) {
            float xM = (cX - halfSpan) + ix * stepM;
            float e = sampleElevationAt1mUnlocked(xM, yM, contourInterval, stepTerracing);
            heights[iz * gridSize + ix] = e;
            if (e < minH) minH = e;
            if (e > maxH) maxH = e;
        }
    }

    MagadiSceneMesh mesh;
    mesh.resolutionX = gridSize;
    mesh.resolutionZ = gridSize;
    mesh.minHeight = minH;
    mesh.maxHeight = maxH;
    mesh.vertices.reserve(gridSize * gridSize);

    float baseDatumElev = (minH + maxH) * 0.5f;
    float totalSlope = 0.0f;
    float reliefSpan = std::max(2.0f, maxH - minH);
    float yScalePerM = (sceneSpan / static_cast<float>(windowCells)) * vExag;

    for (int iz = 0; iz < gridSize; ++iz) {
        float yM = (cY - halfSpan) + iz * stepM;
        float localZ = (iz * stepM) - halfSpan;
        float zScaled = (localZ / static_cast<float>(windowCells)) * sceneSpan;
        for (int ix = 0; ix < gridSize; ++ix) {
            float xM = (cX - halfSpan) + ix * stepM;
            float localX = (ix * stepM) - halfSpan;
            float xScaled = (localX / static_cast<float>(windowCells)) * sceneSpan;
            float rawElev = heights[iz * gridSize + ix];
            float yScaled = (rawElev - baseDatumElev) * yScalePerM;

            float left = (ix > 0) ? heights[iz * gridSize + (ix - 1)] : rawElev;
            float right = (ix < gridSize - 1) ? heights[iz * gridSize + (ix + 1)] : rawElev;
            float down = (iz > 0) ? heights[(iz - 1) * gridSize + ix] : rawElev;
            float up = (iz < gridSize - 1) ? heights[(iz + 1) * gridSize + ix] : rawElev;

            float realDx = (right - left) / (2.0f * stepM);
            float realDz = (up - down) / (2.0f * stepM);
            float realLen = std::sqrt(realDx * realDx + 1.0f + realDz * realDz);
            float slopeDeg = std::acos(std::clamp(1.0f / realLen, 0.0f, 1.0f)) * RAD2DEG;
            totalSlope += slopeDeg;

            float vDx = realDx * (vExag * 1.8f);
            float vDz = realDz * (vExag * 1.8f);
            float vLen = std::sqrt(vDx * vDx + 1.0f + vDz * vDz);
            float nx = -vDx / vLen;
            float ny = 1.0f / vLen;
            float nz = -vDz / vLen;

            bool onRoad = isPointOnRoad(xM, yM);
            bool onBldg = isPointOnBuilding(xM, yM);
            bool isSalt = false;
            float depthFac = 0.0f;
            bool inWaterPoly = isPointInWaterPolygon(xM, yM, isSalt, depthFac);
            bool isLake = (rawElev <= m_lakeLevelAslM + 0.05f || inWaterPoly);
            bool isTrona = (!isLake && rawElev <= m_lakeLevelAslM + 3.0f);

            MagadiVertex v{};
            v.x = xScaled;
            v.y = yScaled;
            v.z = zScaled;
            v.nx = nx;
            v.ny = ny;
            v.nz = nz;
            v.slopeDeg = slopeDeg;
            v.isRoadSurface = onRoad && !isLake;
            v.isWaterSurface = isLake;
            v.isMudSurface = isTrona;
            v.isGravelSurface = onBldg && !isLake;
            v.grassWeight = (!onRoad && !isLake && !isTrona && slopeDeg < 18.0f) ? 1.0f : 0.0f;
            v.dirtWeight = (onBldg && !isLake) ? 1.0f : 0.0f;
            v.rockWeight = (slopeDeg >= 18.0f) ? 0.90f : 0.0f;
            v.snowWeight = isTrona ? 1.0f : 0.0f;
            v.rawElevation = rawElev;
            v.contourBand = std::clamp((rawElev - minH) / reliefSpan, 0.0f, 1.0f);
            float cInt = std::max(0.5f, contourInterval);
            v.isolineFactor = std::abs(std::fmod(rawElev, cInt) / cInt - 0.5f) * 2.0f;
            std::strncpy(v.surfaceType,
                         isLake ? "water" : (v.isRoadSurface ? "asphalt" : (v.isGravelSurface ? "gravel" : (isTrona ? "trona_soda_crust" : "savannah_scrub"))),
                         sizeof(v.surfaceType) - 1);
            mesh.vertices.push_back(v);
        }
    }

    mesh.avgSlope = totalSlope / static_cast<float>(gridSize * gridSize);
    mesh.indices.reserve((gridSize - 1) * (gridSize - 1) * 6);
    for (int iz = 0; iz < gridSize - 1; ++iz) {
        for (int ix = 0; ix < gridSize - 1; ++ix) {
            uint32_t tl = iz * gridSize + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * gridSize + ix;
            uint32_t br = bl + 1;
            mesh.indices.push_back(tl);
            mesh.indices.push_back(bl);
            mesh.indices.push_back(tr);
            mesh.indices.push_back(tr);
            mesh.indices.push_back(bl);
            mesh.indices.push_back(br);
        }
    }
    return mesh;
}

} // namespace custom_vulkan
