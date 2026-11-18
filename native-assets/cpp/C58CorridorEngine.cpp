// ─── native-assets/cpp/C58CorridorEngine.cpp ──────────────────────────────────
// Native C++ Route C58 Non-Box 3 km Corridor Engine (1.5 km Left + 1.5 km Right)
#include "C58CorridorEngine.h"
#include <cmath>
#include <cstring>
#include <algorithm>
#include <limits>
#include <mutex>

namespace custom_vulkan {

namespace {
constexpr double PI_D = 3.14159265358979323846;
constexpr double DEG2RAD_D = PI_D / 180.0;
constexpr double RAD2DEG_D = 180.0 / PI_D;

float catmullRom(float p0, float p1, float p2, float p3, float t) {
    float t2 = t * t;
    float t3 = t2 * t;
    return 0.5f * ((2.0f * p1)
        + (-p0 + p2) * t
        + (2.0f * p0 - 5.0f * p1 + 4.0f * p2 - p3) * t2
        + (-p0 + 3.0f * p1 - 3.0f * p2 + p3) * t3);
}
} // namespace

C58CorridorEngine& C58CorridorEngine::getInstance() {
    static C58CorridorEngine instance;
    return instance;
}

C58CorridorEngine::C58CorridorEngine() {
    setCenterline(getDefaultCenterlineLatLon());
}

void C58CorridorEngine::latLonToUtm37S(double lat, double lon, double& outEasting, double& outNorthing) const {
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

void C58CorridorEngine::utm37SToLatLon(double easting, double northing, double& outLat, double& outLon) const {
    constexpr double a = 6378137.0;
    constexpr double f = 1.0 / 298.257223563;
    constexpr double e2 = f * (2.0 - f);
    constexpr double ep2 = e2 / (1.0 - e2);
    constexpr double k0 = 0.9996;
    constexpr double lon0 = 39.0 * DEG2RAD_D;

    double e1 = (1.0 - std::sqrt(1.0 - e2)) / (1.0 + std::sqrt(1.0 - e2));
    double x = easting - 500000.0;
    double y = northing - 10000000.0;

    double M = y / k0;
    double mu = M / (a * (1.0 - e2 / 4.0 - 3.0 * e2 * e2 / 64.0 - 5.0 * e2 * e2 * e2 / 256.0));

    double phi1 = mu
        + (3.0 * e1 / 2.0 - 27.0 * std::pow(e1, 3) / 32.0) * std::sin(2.0 * mu)
        + (21.0 * e1 * e1 / 16.0 - 55.0 * std::pow(e1, 4) / 32.0) * std::sin(4.0 * mu)
        + (151.0 * std::pow(e1, 3) / 96.0) * std::sin(6.0 * mu)
        + (1097.0 * std::pow(e1, 4) / 512.0) * std::sin(8.0 * mu);

    double sinP1 = std::sin(phi1);
    double cosP1 = std::cos(phi1);
    double tanP1 = std::tan(phi1);
    double N1 = a / std::sqrt(1.0 - e2 * sinP1 * sinP1);
    double T1 = tanP1 * tanP1;
    double C1 = ep2 * cosP1 * cosP1;
    double R1 = a * (1.0 - e2) / std::pow(1.0 - e2 * sinP1 * sinP1, 1.5);
    double D = x / (N1 * k0);

    double latRad = phi1 - (N1 * tanP1 / R1) * (
        D * D / 2.0
        - (5.0 + 3.0 * T1 + 10.0 * C1 - 4.0 * C1 * C1 - 9.0 * ep2) * std::pow(D, 4) / 24.0
        + (61.0 + 90.0 * T1 + 298.0 * C1 + 45.0 * T1 * T1 - 252.0 * ep2 - 3.0 * C1 * C1) * std::pow(D, 6) / 720.0
    );

    double lonRad = lon0 + (
        D
        - (1.0 + 2.0 * T1 + C1) * std::pow(D, 3) / 6.0
        + (5.0 - 2.0 * C1 + 28.0 * T1 - 3.0 * C1 * C1 + 8.0 * ep2 + 24.0 * T1 * T1) * std::pow(D, 5) / 120.0
    ) / cosP1;

    outLat = latRad * RAD2DEG_D;
    outLon = lonRad * RAD2DEG_D;
}

std::vector<double> C58CorridorEngine::getDefaultCenterlineLatLon() const {
    struct Anchor { double lat, lon; };
    static const Anchor kAnchors[] = {
        {-1.9012, 36.2870}, // Magadi Town / Lake Magadi Causeway
        {-1.8620, 36.3060}, // Magadi Soda North Gate
        {-1.7890, 36.3420}, // Ol Keju Nero River Drift
        {-1.7180, 36.3780}, // Kwenia Escarpment Base
        {-1.6480, 36.4125}, // Kwenia Volcanic Step
        {-1.5785, 36.4472}, // Olorgesailie Prehistoric Site
        {-1.5250, 36.4950}, // Tinga Trading Center
        {-1.4820, 36.5480}, // Kamukuru Rift Climb
        {-1.4480, 36.6050}, // Corner Baridi Summit Pass
        {-1.4295, 36.6820}, // Kiserian Town Junction
        {-1.4110, 36.7160}, // Nkoroi / Empakasi Ridge
        {-1.3945, 36.7438}, // Ongata Rongai CBD
        {-1.3640, 36.7565}, // Rimpa / Maasai Lodge Turnoff
        {-1.3340, 36.7660}  // Bomas of Kenya / Galleria End (C58 Terminus)
    };
    const int n = static_cast<int>(sizeof(kAnchors) / sizeof(kAnchors[0]));
    std::vector<double> out;
    out.reserve((n - 1) * 5 * 2 + 2);
    for (int i = 0; i < n - 1; ++i) {
        const Anchor& p0 = kAnchors[std::max(0, i - 1)];
        const Anchor& p1 = kAnchors[i];
        const Anchor& p2 = kAnchors[i + 1];
        const Anchor& p3 = kAnchors[std::min(n - 1, i + 2)];
        const int sub = 5;
        for (int s = 0; s < sub; ++s) {
            float t = static_cast<float>(s) / static_cast<float>(sub);
            double lat = catmullRom(static_cast<float>(p0.lat), static_cast<float>(p1.lat),
                                    static_cast<float>(p2.lat), static_cast<float>(p3.lat), t);
            double lon = catmullRom(static_cast<float>(p0.lon), static_cast<float>(p1.lon),
                                    static_cast<float>(p2.lon), static_cast<float>(p3.lon), t);
            out.push_back(lat);
            out.push_back(lon);
        }
    }
    out.push_back(kAnchors[n - 1].lat);
    out.push_back(kAnchors[n - 1].lon);
    return out;
}

void C58CorridorEngine::setCenterline(const std::vector<double>& latLonPairs) {
    if (latLonPairs.size() < 4) return;
    std::vector<C58CenterlineVertex> pts;
    pts.reserve(latLonPairs.size() / 2);

    for (size_t i = 0; i + 1 < latLonPairs.size(); i += 2) {
        double lat = latLonPairs[i];
        double lon = latLonPairs[i + 1];
        if (!std::isfinite(lat) || !std::isfinite(lon)) continue;
        double e = 0.0, n = 0.0;
        latLonToUtm37S(lat, lon, e, n);
        if (!pts.empty()) {
            double de = e - pts.back().utmEast;
            double dn = n - pts.back().utmNorth;
            if (de * de + dn * dn < 4.0) continue;
        }
        C58CenterlineVertex v;
        v.lat = lat;
        v.lon = lon;
        v.utmEast = e;
        v.utmNorth = n;
        pts.push_back(v);
    }

    if (pts.size() >= 2) {
        std::unique_lock<std::shared_mutex> lock(m_mutex);
        m_centerline = std::move(pts);
        rebuildGeometryMetrics();
    }
}

void C58CorridorEngine::rebuildGeometryMetrics() {
    const int n = static_cast<int>(m_centerline.size());
    if (n < 2) return;

    double cumS = 0.0;
    double minE = std::numeric_limits<double>::max();
    double maxE = -std::numeric_limits<double>::max();
    double minN = std::numeric_limits<double>::max();
    double maxN = -std::numeric_limits<double>::max();
    double minLat = 90.0, maxLat = -90.0, minLon = 180.0, maxLon = -180.0;

    for (int i = 0; i < n; ++i) {
        if (i > 0) {
            double de = m_centerline[i].utmEast - m_centerline[i - 1].utmEast;
            double dn = m_centerline[i].utmNorth - m_centerline[i - 1].utmNorth;
            cumS += std::hypot(de, dn);
        }
        m_centerline[i].chainageM = cumS;

        int iPrev = std::max(0, i - 1);
        int iNext = std::min(n - 1, i + 1);
        double te = m_centerline[iNext].utmEast - m_centerline[iPrev].utmEast;
        double tn = m_centerline[iNext].utmNorth - m_centerline[iPrev].utmNorth;
        double tLen = std::hypot(te, tn);
        if (tLen < 1e-6) { te = 0.0; tn = 1.0; tLen = 1.0; }
        te /= tLen;
        tn /= tLen;

        // Right-hand perpendicular vector looking forward (S -> N): (+tn, -te)
        double rE = tn;
        double rN = -te;
        m_centerline[i].rightNormE = rE;
        m_centerline[i].rightNormN = rN;
        m_centerline[i].leftUtmE = m_centerline[i].utmEast - rE * HALF_WIDTH_M;
        m_centerline[i].leftUtmN = m_centerline[i].utmNorth - rN * HALF_WIDTH_M;
        m_centerline[i].rightUtmE = m_centerline[i].utmEast + rE * HALF_WIDTH_M;
        m_centerline[i].rightUtmN = m_centerline[i].utmNorth + rN * HALF_WIDTH_M;

        const double candidatesE[3] = { m_centerline[i].utmEast, m_centerline[i].leftUtmE, m_centerline[i].rightUtmE };
        const double candidatesN[3] = { m_centerline[i].utmNorth, m_centerline[i].leftUtmN, m_centerline[i].rightUtmN };
        for (int k = 0; k < 3; ++k) {
            minE = std::min(minE, candidatesE[k]);
            maxE = std::max(maxE, candidatesE[k]);
            minN = std::min(minN, candidatesN[k]);
            maxN = std::max(maxN, candidatesN[k]);
        }
        minLat = std::min(minLat, m_centerline[i].lat);
        maxLat = std::max(maxLat, m_centerline[i].lat);
        minLon = std::min(minLon, m_centerline[i].lon);
        maxLon = std::max(maxLon, m_centerline[i].lon);
    }

    double padLat = (HALF_WIDTH_M + 100.0) / 110574.0;
    double padLon = (HALF_WIDTH_M + 100.0) / (111320.0 * std::cos(m_centerline[n / 2].lat * DEG2RAD_D));

    m_summary.totalLengthM = cumS;
    m_summary.halfWidthM = HALF_WIDTH_M;
    m_summary.centerlinePoints = n;
    m_summary.minUtmEast = minE;
    m_summary.maxUtmEast = maxE;
    m_summary.minUtmNorth = minN;
    m_summary.maxUtmNorth = maxN;
    m_summary.minLat = minLat - padLat;
    m_summary.maxLat = maxLat + padLat;
    m_summary.minLon = minLon - padLon;
    m_summary.maxLon = maxLon + padLon;
}

C58CorridorSummary C58CorridorEngine::getSummary() const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return m_summary;
}

std::vector<double> C58CorridorEngine::getCorridorRibbonUtm() const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    std::vector<double> out;
    out.reserve(m_centerline.size() * 7);
    for (const auto& v : m_centerline) {
        out.push_back(v.utmEast);
        out.push_back(v.utmNorth);
        out.push_back(v.leftUtmE);
        out.push_back(v.leftUtmN);
        out.push_back(v.rightUtmE);
        out.push_back(v.rightUtmN);
        out.push_back(v.chainageM);
    }
    return out;
}

C58ProjectionResult C58CorridorEngine::projectUtmPointUnlocked(double utmE, double utmN) const {
    C58ProjectionResult res;
    res.utmEast = utmE;
    res.utmNorth = utmN;

    const int n = static_cast<int>(m_centerline.size());
    if (n < 2) return res;

    if (utmE < m_summary.minUtmEast - 500.0 || utmE > m_summary.maxUtmEast + 500.0 ||
        utmN < m_summary.minUtmNorth - 500.0 || utmN > m_summary.maxUtmNorth + 500.0) {
        res.insideCorridor = false;
        res.lateralOffsetM = 9999.0f;
        return res;
    }

    double bestDistSq = std::numeric_limits<double>::max();
    double bestChainage = 0.0;
    double bestSignedOffset = 0.0;
    bool bestAtInterior = false;

    for (int i = 0; i < n - 1; ++i) {
        const auto& a = m_centerline[i];
        const auto& b = m_centerline[i + 1];
        double dx = b.utmEast - a.utmEast;
        double dy = b.utmNorth - a.utmNorth;
        double segLenSq = dx * dx + dy * dy;
        double segLen = std::sqrt(segLenSq);
        if (segLen < 1e-6) continue;

        double tRaw = ((utmE - a.utmEast) * dx + (utmN - a.utmNorth) * dy) / segLenSq;
        double tClamped = std::clamp(tRaw, 0.0, 1.0);
        double projE = a.utmEast + tClamped * dx;
        double projN = a.utmNorth + tClamped * dy;
        double errE = utmE - projE;
        double errN = utmN - projN;
        double distSq = errE * errE + errN * errN;

        if (distSq < bestDistSq) {
            bestDistSq = distSq;
            bestChainage = a.chainageM + tClamped * segLen;

            double rE = (dy / segLen);
            double rN = -(dx / segLen);
            double mag = std::sqrt(distSq);
            double dotR = errE * rE + errN * rN;
            double sign = (dotR >= 0.0) ? 1.0 : -1.0;
            bestSignedOffset = sign * mag;

            bool clippedBeforeStart = (i == 0 && tRaw < -HALF_WIDTH_M / segLen);
            bool clippedAfterEnd = (i == n - 2 && tRaw > 1.0 + HALF_WIDTH_M / segLen);
            bestAtInterior = !(clippedBeforeStart || clippedAfterEnd);
        }
    }

    res.chainageM = bestChainage;
    res.lateralOffsetM = static_cast<float>(bestSignedOffset);
    res.insideCorridor = bestAtInterior && (std::abs(bestSignedOffset) <= static_cast<double>(HALF_WIDTH_M) + 0.5);
    return res;
}

C58ProjectionResult C58CorridorEngine::projectUtmPoint(double utmE, double utmN) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return projectUtmPointUnlocked(utmE, utmN);
}

C58ProjectionResult C58CorridorEngine::projectLatLon(double lat, double lon) const {
    double e = 0.0, n = 0.0;
    latLonToUtm37S(lat, lon, e, n);
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return projectUtmPointUnlocked(e, n);
}

void C58CorridorEngine::setDemControlGrid(int uCols, int vRows, const std::vector<float>& elevationsM) {
    if (uCols >= 2 && vRows >= 2 && static_cast<int>(elevationsM.size()) >= uCols * vRows) {
        std::unique_lock<std::shared_mutex> lock(m_mutex);
        m_demCols = uCols;
        m_demRows = vRows;
        const int count = uCols * vRows;
        m_demElevations.resize(static_cast<size_t>(count));
        for (int i = 0; i < count; ++i) {
            float v = elevationsM[static_cast<size_t>(i)];
            m_demElevations[static_cast<size_t>(i)] = std::isfinite(v) ? v : 1000.0f;
        }
        m_hasLiveDem = true;
    }
}

bool C58CorridorEngine::hasLiveDemData() const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return m_hasLiveDem;
}

void C58CorridorEngine::setLakeAndWaterPolygonsUtm(
    float lakeLevelAslM,
    const std::vector<int>& polyMetaPacked,
    const std::vector<double>& coordsEN) {
    std::unique_lock<std::shared_mutex> lock(m_mutex);
    m_lakeLevelAslM = std::isfinite(lakeLevelAslM) ? lakeLevelAslM : 602.5f;
    m_waterPolys.clear();
    int offset = 0;
    int metaCount = static_cast<int>(polyMetaPacked.size());
    int cLen = static_cast<int>(coordsEN.size());
    for (int i = 0; i + 1 < metaCount; i += 2) {
        int ptCount = polyMetaPacked[i];
        bool isSalt = (polyMetaPacked[i + 1] != 0);
        int doublesNeeded = ptCount * 2;
        if (ptCount >= 3 && offset + doublesNeeded <= cLen) {
            WaterPolyUtm p;
            p.isSaltPan = isSalt;
            p.coordsEN.assign(coordsEN.begin() + offset, coordsEN.begin() + offset + doublesNeeded);
            double minE = 1e12, minN = 1e12, maxE = -1e12, maxN = -1e12;
            for (int k = 0; k < ptCount; ++k) {
                double e = p.coordsEN[k * 2];
                double n = p.coordsEN[k * 2 + 1];
                minE = std::min(minE, e); maxE = std::max(maxE, e);
                minN = std::min(minN, n); maxN = std::max(maxN, n);
            }
            p.minE = minE; p.maxE = maxE; p.minN = minN; p.maxN = maxN;
            m_waterPolys.push_back(std::move(p));
        }
        offset += std::max(0, doublesNeeded);
    }
}

float C58CorridorEngine::getLakeLevelAslM() const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return m_lakeLevelAslM;
}

void C58CorridorEngine::setFeatureFootprintsUtm(
    const std::vector<double>& roadSegsUtm5,
    const std::vector<double>& buildingRectsUtm4) {
    std::unique_lock<std::shared_mutex> lock(m_mutex);
    m_roadSegs.clear();
    m_roadGrid.clear();
    m_roadSegs.reserve(roadSegsUtm5.size() / 5);
    for (size_t i = 0; i + 4 < roadSegsUtm5.size(); i += 5) {
        RoadSegUtm r;
        r.e0 = roadSegsUtm5[i + 0];
        r.n0 = roadSegsUtm5[i + 1];
        r.e1 = roadSegsUtm5[i + 2];
        r.n1 = roadSegsUtm5[i + 3];
        r.halfWidthM = static_cast<float>(std::clamp(roadSegsUtm5[i + 4], 1.5, 16.0));
        r.minE = std::min(r.e0, r.e1) - r.halfWidthM;
        r.maxE = std::max(r.e0, r.e1) + r.halfWidthM;
        r.minN = std::min(r.n0, r.n1) - r.halfWidthM;
        r.maxN = std::max(r.n0, r.n1) + r.halfWidthM;
        uint32_t idx = static_cast<uint32_t>(m_roadSegs.size());
        m_roadSegs.push_back(r);

        int32_t cx0 = static_cast<int32_t>(std::floor(r.minE / SPATIAL_CELL_M));
        int32_t cx1 = static_cast<int32_t>(std::floor(r.maxE / SPATIAL_CELL_M));
        int32_t cy0 = static_cast<int32_t>(std::floor(r.minN / SPATIAL_CELL_M));
        int32_t cy1 = static_cast<int32_t>(std::floor(r.maxN / SPATIAL_CELL_M));
        // Guard against anomalous segments spanning huge bbox
        if ((cx1 - cx0) <= 64 && (cy1 - cy0) <= 64) {
            for (int32_t cx = cx0; cx <= cx1; ++cx) {
                for (int32_t cy = cy0; cy <= cy1; ++cy) {
                    m_roadGrid[spatialKey(cx, cy)].push_back(idx);
                }
            }
        }
    }

    m_bldgRects.clear();
    m_bldgGrid.clear();
    m_bldgRects.reserve(buildingRectsUtm4.size() / 4);
    for (size_t i = 0; i + 3 < buildingRectsUtm4.size(); i += 4) {
        BldgRectUtm b;
        b.minE = buildingRectsUtm4[i + 0];
        b.minN = buildingRectsUtm4[i + 1];
        b.maxE = buildingRectsUtm4[i + 2];
        b.maxN = buildingRectsUtm4[i + 3];
        uint32_t idx = static_cast<uint32_t>(m_bldgRects.size());
        m_bldgRects.push_back(b);

        int32_t cx0 = static_cast<int32_t>(std::floor(b.minE / SPATIAL_CELL_M));
        int32_t cx1 = static_cast<int32_t>(std::floor(b.maxE / SPATIAL_CELL_M));
        int32_t cy0 = static_cast<int32_t>(std::floor(b.minN / SPATIAL_CELL_M));
        int32_t cy1 = static_cast<int32_t>(std::floor(b.maxN / SPATIAL_CELL_M));
        if ((cx1 - cx0) <= 32 && (cy1 - cy0) <= 32) {
            for (int32_t cx = cx0; cx <= cx1; ++cx) {
                for (int32_t cy = cy0; cy <= cy1; ++cy) {
                    m_bldgGrid[spatialKey(cx, cy)].push_back(idx);
                }
            }
        }
    }
}

bool C58CorridorEngine::isPointOnRoadUtm(double utmE, double utmN) const {
    if (m_roadGrid.empty()) return false;
    int32_t cx = static_cast<int32_t>(std::floor(utmE / SPATIAL_CELL_M));
    int32_t cy = static_cast<int32_t>(std::floor(utmN / SPATIAL_CELL_M));
    auto it = m_roadGrid.find(spatialKey(cx, cy));
    if (it == m_roadGrid.end()) return false;

    for (uint32_t idx : it->second) {
        const auto& s = m_roadSegs[idx];
        if (utmE < s.minE || utmE > s.maxE || utmN < s.minN || utmN > s.maxN) continue;
        double dx = s.e1 - s.e0;
        double dy = s.n1 - s.n0;
        double lenSq = dx * dx + dy * dy;
        double t = (lenSq > 1e-4) ? std::clamp(((utmE - s.e0) * dx + (utmN - s.n0) * dy) / lenSq, 0.0, 1.0) : 0.0;
        double px = s.e0 + t * dx;
        double py = s.n0 + t * dy;
        double dSq = (utmE - px) * (utmE - px) + (utmN - py) * (utmN - py);
        if (dSq <= static_cast<double>(s.halfWidthM * s.halfWidthM)) return true;
    }
    return false;
}

bool C58CorridorEngine::isPointOnBuildingUtm(double utmE, double utmN) const {
    if (m_bldgGrid.empty()) return false;
    int32_t cx = static_cast<int32_t>(std::floor(utmE / SPATIAL_CELL_M));
    int32_t cy = static_cast<int32_t>(std::floor(utmN / SPATIAL_CELL_M));
    auto it = m_bldgGrid.find(spatialKey(cx, cy));
    if (it == m_bldgGrid.end()) return false;

    for (uint32_t idx : it->second) {
        const auto& b = m_bldgRects[idx];
        if (utmE >= b.minE && utmE <= b.maxE && utmN >= b.minN && utmN <= b.maxN) return true;
    }
    return false;
}

float C58CorridorEngine::pseudoNoise2D(float x, float y) const {
    float n = std::sin(x * 12.9898f + y * 78.233f) * 43758.5453123f;
    return n - std::floor(n);
}

float C58CorridorEngine::fbmNoise(float x, float y, int octaves, float lacunarity, float gain) const {
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
        sum += ((n00 * (1.0f - u) + n10 * u) * (1.0f - v) + (n01 * (1.0f - u) + n11 * u) * v) * amp;
        maxAmp += amp;
        amp *= gain;
        freq *= lacunarity;
    }
    return maxAmp > 0.0f ? (sum / maxAmp) : 0.0f;
}

float C58CorridorEngine::computeBaselineProfileElev(double chainageM, float offsetM) const {
    double km = std::clamp(chainageM / 1000.0, 0.0, 105.0);
    double roadElev;
    if (km < 22.0) {
        roadElev = 602.0 + km * 6.5;
    } else if (km < 42.0) {
        double t = (km - 22.0) / 20.0;
        roadElev = 745.0 + t * 235.0;
    } else if (km < 68.0) {
        double t = (km - 42.0) / 26.0;
        double smoothT = t * t * (3.0 - 2.0 * t);
        roadElev = 980.0 + smoothT * 800.0;
    } else if (km < 88.0) {
        double t = (km - 68.0) / 20.0;
        roadElev = 1780.0 - std::sin(t * PI_D) * 115.0 - t * 60.0;
    } else {
        double t = std::clamp((km - 88.0) / 14.0, 0.0, 1.0);
        roadElev = 1720.0 + t * 48.0;
    }

    float absOff = std::abs(offsetM);
    float roadMask = 1.0f - std::clamp((absOff - 6.0f) / 24.0f, 0.0f, 1.0f);
    float sideSlope = (offsetM / 1500.0f) * (km < 65.0 ? 45.0f : -30.0f);
    float valleyWall = std::pow(absOff / 1500.0f, 1.6f) * 38.0f;
    float micro = (fbmNoise(static_cast<float>(km * 1.4), offsetM * 0.0022f, 4, 2.05f, 0.5f) - 0.5f) * 28.0f;

    return static_cast<float>(roadElev) + (sideSlope + valleyWall + micro) * (1.0f - roadMask * 0.92f);
}

float C58CorridorEngine::sampleDemCurvilinear(double chainageM, float offsetM) const {
    if (!m_hasLiveDem || m_demCols < 2 || m_demRows < 2 || m_summary.totalLengthM < 1.0) {
        return computeBaselineProfileElev(chainageM, offsetM);
    }

    float nu = std::clamp((offsetM + HALF_WIDTH_M) / FULL_WIDTH_M, 0.0f, 1.0f);
    float nv = static_cast<float>(std::clamp(chainageM / m_summary.totalLengthM, 0.0, 1.0));

    float gx = nu * static_cast<float>(m_demCols - 1);
    float gy = nv * static_cast<float>(m_demRows - 1);
    int c1 = std::clamp(static_cast<int>(std::floor(gx)), 0, m_demCols - 1);
    int r1 = std::clamp(static_cast<int>(std::floor(gy)), 0, m_demRows - 1);
    float tx = gx - static_cast<float>(c1);
    float ty = gy - static_cast<float>(r1);

    auto getCell = [&](int c, int r) -> float {
        int cc = std::clamp(c, 0, m_demCols - 1);
        int rr = std::clamp(r, 0, m_demRows - 1);
        return m_demElevations[rr * m_demCols + cc];
    };

    float rowVals[4];
    for (int j = -1; j <= 2; ++j) {
        rowVals[j + 1] = catmullRom(
            getCell(c1 - 1, r1 + j),
            getCell(c1,     r1 + j),
            getCell(c1 + 1, r1 + j),
            getCell(c1 + 2, r1 + j),
            tx
        );
    }
    return catmullRom(rowVals[0], rowVals[1], rowVals[2], rowVals[3], ty);
}

bool C58CorridorEngine::isUtmInWaterPolygon(double utmE, double utmN, bool& outIsSaltPan, float& outDepthFactor) const {
    outIsSaltPan = false;
    outDepthFactor = 0.0f;
    for (const auto& wp : m_waterPolys) {
        if (utmE < wp.minE || utmE > wp.maxE || utmN < wp.minN || utmN > wp.maxN) continue;
        const int n = static_cast<int>(wp.coordsEN.size()) / 2;
        if (n < 3) continue;
        bool inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = wp.coordsEN[i * 2], yi = wp.coordsEN[i * 2 + 1];
            double xj = wp.coordsEN[j * 2], yj = wp.coordsEN[j * 2 + 1];
            double denom = yj - yi;
            bool intersect = ((yi > utmN) != (yj > utmN)) &&
                (std::abs(denom) > 1e-9) &&
                (utmE < (xj - xi) * (utmN - yi) / denom + xi);
            if (intersect) inside = !inside;
        }
        if (inside) {
            outIsSaltPan = wp.isSaltPan;
            outDepthFactor = wp.isSaltPan ? 0.45f : 1.0f;
            return true;
        }
    }
    return false;
}

float C58CorridorEngine::sampleLandElevationRaw(double utmE, double utmN, const C58ProjectionResult& proj) const {
    float clampedOff = std::clamp(proj.lateralOffsetM, -HALF_WIDTH_M, HALF_WIDTH_M);
    float demVal = sampleDemCurvilinear(proj.chainageM, clampedOff);
    float absOff = std::abs(clampedOff);
    float micro = (fbmNoise(static_cast<float>(proj.chainageM * 0.032), clampedOff * 0.032f, 3, 2.0f, 0.48f) - 0.5f) * 0.85f;
    float roadDamp = (absOff < 6.0f || isPointOnRoadUtm(utmE, utmN)) ? 0.1f : 1.0f;
    return demVal + micro * roadDamp;
}

void C58CorridorEngine::evaluateWaterAndElevation(
    double utmE, double utmN, const C58ProjectionResult& proj,
    float& outElevM, float& outWaterDepthM, bool& outIsWater) const {
    float land = sampleLandElevationRaw(utmE, utmN, proj);
    bool isSaltPan = false;
    float polyDepthFactor = 0.0f;
    bool inWaterPoly = isUtmInWaterPolygon(utmE, utmN, isSaltPan, polyDepthFactor);

    if (inWaterPoly) {
        float targetBed = std::min(land, m_lakeLevelAslM - (isSaltPan ? 0.8f : 1.8f));
        outElevM = targetBed;
        outWaterDepthM = std::max(0.2f, m_lakeLevelAslM - targetBed);
        outIsWater = true;
        return;
    }

    if (land < m_lakeLevelAslM && proj.chainageM < 18000.0) {
        outElevM = land;
        outWaterDepthM = m_lakeLevelAslM - land;
        outIsWater = true;
        return;
    }

    outElevM = land;
    outWaterDepthM = 0.0f;
    outIsWater = false;
}

float C58CorridorEngine::sampleElevationAtUtmUnlocked(
    double utmE, double utmN,
    float contourInterval, float stepTerracing) const {
    C58ProjectionResult proj = projectUtmPointUnlocked(utmE, utmN);
    float elevM = 0.0f, depthM = 0.0f;
    bool isW = false;
    evaluateWaterAndElevation(utmE, utmN, proj, elevM, depthM, isW);
    if (!std::isfinite(elevM)) elevM = 1000.0f;
    if (stepTerracing > 0.01f && std::isfinite(contourInterval) && contourInterval > 0.1f && !isW) {
        float q = std::floor(elevM / contourInterval) * contourInterval;
        elevM = elevM * (1.0f - stepTerracing) + q * stepTerracing;
    }
    return std::isfinite(elevM) ? elevM : 1000.0f;
}

float C58CorridorEngine::sampleElevationAtUtm(
    double utmE, double utmN,
    float contourInterval, float stepTerracing) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    return sampleElevationAtUtmUnlocked(utmE, utmN, contourInterval, stepTerracing);
}

C58PointTelemetry C58CorridorEngine::getTelemetryAtUtm(double utmE, double utmN) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    C58PointTelemetry t;
    t.utmEast = utmE;
    t.utmNorth = utmN;
    utm37SToLatLon(utmE, utmN, t.lat, t.lon);

    C58ProjectionResult proj = projectUtmPointUnlocked(utmE, utmN);
    t.chainageM = proj.chainageM;
    t.lateralOffsetM = proj.lateralOffsetM;
    t.insideCorridor = proj.insideCorridor;

    evaluateWaterAndElevation(utmE, utmN, proj, t.elevM, t.waterDepthM, t.isWater);

    const float step = 2.0f;
    float eL = sampleElevationAtUtmUnlocked(utmE - step, utmN, 0.0f, 0.0f);
    float eR = sampleElevationAtUtmUnlocked(utmE + step, utmN, 0.0f, 0.0f);
    float eD = sampleElevationAtUtmUnlocked(utmE, utmN - step, 0.0f, 0.0f);
    float eU = sampleElevationAtUtmUnlocked(utmE, utmN + step, 0.0f, 0.0f);
    float dEdx = (eR - eL) / (2.0f * step);
    float dEdy = (eU - eD) / (2.0f * step);
    t.slopeDeg = std::atan(std::sqrt(dEdx * dEdx + dEdy * dEdy)) * static_cast<float>(RAD2DEG_D);
    return t;
}

std::vector<float> C58CorridorEngine::sampleUtmRaster(
    double minUtmE, double maxUtmE,
    double minUtmN, double maxUtmN,
    int cols, int rows) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    int c = std::clamp(cols, 2, 160);
    int r = std::clamp(rows, 2, 160);
    std::vector<float> out(static_cast<size_t>(c * r * 4), 0.0f);

    double spanE = std::max(1.0, maxUtmE - minUtmE);
    double spanN = std::max(1.0, maxUtmN - minUtmN);
    double dSample = std::clamp(spanE / static_cast<double>(c), 1.0, 20.0);

    for (int row = 0; row < r; ++row) {
        double nCoord = minUtmN + (static_cast<double>(row) / static_cast<double>(r - 1)) * spanN;
        for (int col = 0; col < c; ++col) {
            double eCoord = minUtmE + (static_cast<double>(col) / static_cast<double>(c - 1)) * spanE;
            C58ProjectionResult proj = projectUtmPointUnlocked(eCoord, nCoord);

            float elev = 0.0f, depth = 0.0f;
            bool isW = false;
            evaluateWaterAndElevation(eCoord, nCoord, proj, elev, depth, isW);

            float shade = 0.85f;
            if (proj.insideCorridor) {
                float eW = sampleElevationAtUtmUnlocked(eCoord - dSample, nCoord);
                float eE = sampleElevationAtUtmUnlocked(eCoord + dSample, nCoord);
                float eS = sampleElevationAtUtmUnlocked(eCoord, nCoord - dSample);
                float eN = sampleElevationAtUtmUnlocked(eCoord, nCoord + dSample);
                float gx = (eE - eW) / static_cast<float>(2.0 * dSample);
                float gy = (eN - eS) / static_cast<float>(2.0 * dSample);
                float nLen = std::sqrt(gx * gx + gy * gy + 1.0f);
                float nx = -gx / nLen, ny = -gy / nLen, nz = 1.0f / nLen;
                float dot = std::clamp(nx * (-0.52f) + ny * 0.56f + nz * 0.64f, 0.25f, 1.0f);
                shade = 0.60f + 0.48f * dot;
            }

            size_t base = static_cast<size_t>((row * c + col) * 4);
            out[base + 0] = elev;
            out[base + 1] = shade;
            out[base + 2] = depth;
            out[base + 3] = proj.insideCorridor ? 1.0f : 0.0f;
        }
    }
    return out;
}

C58Sector3dTransform C58CorridorEngine::getSector3dTransform(
    double sectorStartM, double sectorEndM, int resU, int resV, float verticalExaggeration) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    int ru = std::clamp(resU, 9, 81);
    int rv = std::clamp(resV, 15, 141);
    double totalLen = std::max(1000.0, m_summary.totalLengthM);
    double s0 = std::clamp(sectorStartM, 0.0, totalLen - 500.0);
    double s1 = std::clamp(sectorEndM, s0 + 500.0, totalLen);

    auto evalAtChainage = [&](double s, double& outCE, double& outCN, double& outRE, double& outRN) {
        const int n = static_cast<int>(m_centerline.size());
        if (n < 2) { outCE = 0; outCN = 0; outRE = 1; outRN = 0; return; }
        for (int i = 0; i < n - 1; ++i) {
            if (s <= m_centerline[i + 1].chainageM || i == n - 2) {
                double span = std::max(1e-3, m_centerline[i + 1].chainageM - m_centerline[i].chainageM);
                double t = std::clamp((s - m_centerline[i].chainageM) / span, 0.0, 1.0);
                outCE = m_centerline[i].utmEast + (m_centerline[i + 1].utmEast - m_centerline[i].utmEast) * t;
                outCN = m_centerline[i].utmNorth + (m_centerline[i + 1].utmNorth - m_centerline[i].utmNorth) * t;
                outRE = m_centerline[i].rightNormE + (m_centerline[i + 1].rightNormE - m_centerline[i].rightNormE) * t;
                outRN = m_centerline[i].rightNormN + (m_centerline[i + 1].rightNormN - m_centerline[i].rightNormN) * t;
                double rl = std::hypot(outRE, outRN);
                if (rl > 1e-6) { outRE /= rl; outRN /= rl; }
                return;
            }
        }
    };

    double sumE = 0.0, sumN = 0.0;
    double minE = 1e12, maxE = -1e12, minN = 1e12, maxN = -1e12;
    float minRaw = 1e9f;

    for (int iv = 0; iv < rv; ++iv) {
        double s = s0 + (static_cast<double>(iv) / static_cast<double>(rv - 1)) * (s1 - s0);
        double ce = 0.0, cn = 0.0, re = 1.0, rn = 0.0;
        evalAtChainage(s, ce, cn, re, rn);
        for (int iu = 0; iu < ru; ++iu) {
            float uRatio = (static_cast<float>(iu) / static_cast<float>(ru - 1)) * 2.0f - 1.0f;
            float offsetM = uRatio * HALF_WIDTH_M;
            double pe = ce + re * offsetM;
            double pn = cn + rn * offsetM;
            sumE += pe; sumN += pn;
            minE = std::min(minE, pe); maxE = std::max(maxE, pe);
            minN = std::min(minN, pn); maxN = std::max(maxN, pn);

            C58ProjectionResult proj = projectUtmPointUnlocked(pe, pn);
            float elev = 0.0f, depth = 0.0f; bool w = false;
            evaluateWaterAndElevation(pe, pn, proj, elev, depth, w);
            if (elev < minRaw) minRaw = elev;
        }
    }

    C58Sector3dTransform tr;
    tr.centerUtmE = sumE / static_cast<double>(ru * rv);
    tr.centerUtmN = sumN / static_cast<double>(ru * rv);
    double maxSpan = std::max({1000.0, maxE - minE, maxN - minN});
    tr.sceneScaleXZ = static_cast<float>(84.0 / maxSpan);
    tr.yScalePerM = tr.sceneScaleXZ * verticalExaggeration * 2.4f;
    tr.baseDatumElev = minRaw - 18.0f;
    return tr;
}

MagadiSceneMesh C58CorridorEngine::generateSector3dMesh(
    double sectorStartM, double sectorEndM,
    int resU, int resV,
    float contourInterval, float stepTerracing, float verticalExaggeration) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    MagadiSceneMesh mesh;
    int ru = std::clamp(resU, 9, 81);
    int rv = std::clamp(resV, 15, 141);

    double totalLen = std::max(1000.0, m_summary.totalLengthM);
    double s0 = std::clamp(sectorStartM, 0.0, totalLen - 500.0);
    double s1 = std::clamp(sectorEndM, s0 + 500.0, totalLen);

    auto evalAtChainage = [&](double s, double& outCE, double& outCN, double& outRE, double& outRN) {
        const int n = static_cast<int>(m_centerline.size());
        if (n < 2) { outCE = 0; outCN = 0; outRE = 1; outRN = 0; return; }
        for (int i = 0; i < n - 1; ++i) {
            if (s <= m_centerline[i + 1].chainageM || i == n - 2) {
                double span = std::max(1e-3, m_centerline[i + 1].chainageM - m_centerline[i].chainageM);
                double t = std::clamp((s - m_centerline[i].chainageM) / span, 0.0, 1.0);
                outCE = m_centerline[i].utmEast + (m_centerline[i + 1].utmEast - m_centerline[i].utmEast) * t;
                outCN = m_centerline[i].utmNorth + (m_centerline[i + 1].utmNorth - m_centerline[i].utmNorth) * t;
                outRE = m_centerline[i].rightNormE + (m_centerline[i + 1].rightNormE - m_centerline[i].rightNormE) * t;
                outRN = m_centerline[i].rightNormN + (m_centerline[i + 1].rightNormN - m_centerline[i].rightNormN) * t;
                double rl = std::hypot(outRE, outRN);
                if (rl > 1e-6) { outRE /= rl; outRN /= rl; }
                return;
            }
        }
    };

    double sumE = 0.0, sumN = 0.0;
    double minE = 1e12, maxE = -1e12, minN = 1e12, maxN = -1e12;
    std::vector<double> gridUtmE(static_cast<size_t>(ru * rv));
    std::vector<double> gridUtmN(static_cast<size_t>(ru * rv));
    std::vector<float> gridOffsets(static_cast<size_t>(ru * rv));

    for (int iv = 0; iv < rv; ++iv) {
        double s = s0 + (static_cast<double>(iv) / static_cast<double>(rv - 1)) * (s1 - s0);
        double ce = 0.0, cn = 0.0, re = 1.0, rn = 0.0;
        evalAtChainage(s, ce, cn, re, rn);
        for (int iu = 0; iu < ru; ++iu) {
            float uRatio = (static_cast<float>(iu) / static_cast<float>(ru - 1)) * 2.0f - 1.0f;
            float offsetM = uRatio * HALF_WIDTH_M;
            double pe = ce + re * offsetM;
            double pn = cn + rn * offsetM;
            size_t idx = static_cast<size_t>(iv * ru + iu);
            gridUtmE[idx] = pe;
            gridUtmN[idx] = pn;
            gridOffsets[idx] = offsetM;
            sumE += pe; sumN += pn;
            minE = std::min(minE, pe); maxE = std::max(maxE, pe);
            minN = std::min(minN, pn); maxN = std::max(maxN, pn);
        }
    }

    double centerE = sumE / static_cast<double>(ru * rv);
    double centerN = sumN / static_cast<double>(ru * rv);
    double maxSpan = std::max({1000.0, maxE - minE, maxN - minN});
    const float sceneScaleXZ = static_cast<float>(84.0 / maxSpan);

    std::vector<float> rawElev(static_cast<size_t>(ru * rv));
    std::vector<bool> isWater(static_cast<size_t>(ru * rv), false);
    float minRaw = 1e9f, maxRaw = -1e9f;
    for (int i = 0; i < ru * rv; ++i) {
        C58ProjectionResult proj = projectUtmPointUnlocked(gridUtmE[i], gridUtmN[i]);
        float depth = 0.0f;
        bool w = false;
        evaluateWaterAndElevation(gridUtmE[i], gridUtmN[i], proj, rawElev[i], depth, w);
        if (stepTerracing > 0.01f && contourInterval > 0.1f && !w) {
            float q = std::floor(rawElev[i] / contourInterval) * contourInterval;
            rawElev[i] = rawElev[i] * (1.0f - stepTerracing) + q * stepTerracing;
        }
        isWater[i] = w;
        if (rawElev[i] < minRaw) minRaw = rawElev[i];
        if (rawElev[i] > maxRaw) maxRaw = rawElev[i];
    }

    const float baseDatum = minRaw - 18.0f;
    const float yScale = sceneScaleXZ * verticalExaggeration * 2.4f;
    const float reliefSpan = std::max(2.0f, maxRaw - minRaw);
    const float step = 4.0f;

    mesh.resolutionX = ru;
    mesh.resolutionZ = rv;
    mesh.minHeight = minRaw;
    mesh.maxHeight = maxRaw;
    float totalSlope = 0.0f;

    mesh.vertices.resize(static_cast<size_t>(ru * rv));
    for (int iv = 0; iv < rv; ++iv) {
        for (int iu = 0; iu < ru; ++iu) {
            size_t idx = static_cast<size_t>(iv * ru + iu);
            double pe = gridUtmE[idx];
            double pn = gridUtmN[idx];
            float rawE = rawElev[idx];
            float offAbs = std::abs(gridOffsets[idx]);

            MagadiVertex v{};
            v.x = static_cast<float>((pe - centerE) * sceneScaleXZ);
            v.z = static_cast<float>((pn - centerN) * sceneScaleXZ);
            v.y = (rawE - baseDatum) * yScale;
            v.rawElevation = rawE;

            float eL = sampleElevationAtUtmUnlocked(pe - step, pn);
            float eR = sampleElevationAtUtmUnlocked(pe + step, pn);
            float eD = sampleElevationAtUtmUnlocked(pe, pn - step);
            float eU = sampleElevationAtUtmUnlocked(pe, pn + step);
            float dEdx = (eR - eL) / (2.0f * step);
            float dEdy = (eU - eD) / (2.0f * step);
            float nLen = std::sqrt(dEdx * dEdx + dEdy * dEdy + 1.0f);
            v.nx = -dEdx / nLen;
            v.ny = 1.0f / nLen;
            v.nz = -dEdy / nLen;
            v.slopeDeg = std::atan(std::sqrt(dEdx * dEdx + dEdy * dEdy)) * static_cast<float>(RAD2DEG_D);
            totalSlope += v.slopeDeg;

            float cInt = std::isfinite(contourInterval) ? std::max(contourInterval, 2.0f) : 15.0f;
            v.contourBand = std::clamp((rawE - minRaw) / reliefSpan, 0.0f, 1.0f);
            v.isolineFactor = std::abs(std::fmod(rawE, cInt) / cInt - 0.5f) * 2.0f;

            bool onRoad = isPointOnRoadUtm(pe, pn) || offAbs <= 22.0f;
            bool onBldg = isPointOnBuildingUtm(pe, pn);
            v.isWaterSurface = isWater[idx];
            v.isRoadSurface = (!v.isWaterSurface && onRoad);
            v.isMudSurface = (!v.isWaterSurface && rawE <= m_lakeLevelAslM + 3.0f && s0 < 15000.0);
            v.isGravelSurface = (!v.isWaterSurface && onBldg);
            v.rockWeight = (v.slopeDeg > 18.0f) ? 0.90f : 0.0f;
            v.grassWeight = (!v.isRoadSurface && !v.isWaterSurface && !v.isMudSurface && v.slopeDeg <= 18.0f) ? 1.0f : 0.0f;
            v.dirtWeight = v.isGravelSurface ? 1.0f : 0.0f;
            v.snowWeight = v.isMudSurface ? 1.0f : 0.0f;
            std::strncpy(v.surfaceType,
                         v.isWaterSurface ? "water" : (v.isRoadSurface ? "asphalt" : (v.isGravelSurface ? "gravel" : (v.isMudSurface ? "trona_soda_crust" : "savannah_scrub"))),
                         sizeof(v.surfaceType) - 1);
            mesh.vertices[idx] = v;
        }
    }
    mesh.avgSlope = totalSlope / static_cast<float>(ru * rv);

    mesh.indices.reserve(static_cast<size_t>((ru - 1) * (rv - 1) * 6));
    for (int iv = 0; iv < rv - 1; ++iv) {
        for (int iu = 0; iu < ru - 1; ++iu) {
            uint32_t tl = static_cast<uint32_t>(iv * ru + iu);
            uint32_t tr = tl + 1;
            uint32_t bl = static_cast<uint32_t>((iv + 1) * ru + iu);
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

MagadiSceneMesh C58CorridorEngine::generateWindow1mMesh(
    double centerUtmE, double centerUtmN,
    int windowCells, float contourInterval, float stepTerracing, float verticalExaggeration) const {
    std::shared_lock<std::shared_mutex> lock(m_mutex);
    MagadiSceneMesh mesh;
    int cells = std::clamp(windowCells, 24, 96);
    int res = cells + 1;
    float halfSpan = static_cast<float>(cells) * 0.5f;
    float sceneSpan = 56.0f;
    float yScale = (sceneSpan / static_cast<float>(cells)) * verticalExaggeration;

    double e0 = centerUtmE - halfSpan;
    double n0 = centerUtmN - halfSpan;
    float minRaw = 1e9f, maxRaw = -1e9f;
    std::vector<float> raws(static_cast<size_t>(res * res));
    std::vector<bool> waters(static_cast<size_t>(res * res), false);
    std::vector<float> offsets(static_cast<size_t>(res * res), 0.0f);

    for (int rz = 0; rz < res; ++rz) {
        double pn = n0 + static_cast<double>(rz);
        for (int rx = 0; rx < res; ++rx) {
            double pe = e0 + static_cast<double>(rx);
            C58ProjectionResult proj = projectUtmPointUnlocked(pe, pn);
            size_t idx = static_cast<size_t>(rz * res + rx);
            offsets[idx] = proj.lateralOffsetM;
            float depth = 0.0f;
            bool w = false;
            evaluateWaterAndElevation(pe, pn, proj, raws[idx], depth, w);
            if (stepTerracing > 0.01f && std::isfinite(contourInterval) && contourInterval > 0.1f && !w) {
                float q = std::floor(raws[idx] / contourInterval) * contourInterval;
                raws[idx] = raws[idx] * (1.0f - stepTerracing) + q * stepTerracing;
            }
            waters[idx] = w;
            if (raws[idx] < minRaw) minRaw = raws[idx];
            if (raws[idx] > maxRaw) maxRaw = raws[idx];
        }
    }

    float baseDatum = (minRaw + maxRaw) * 0.5f;
    float reliefSpan = std::max(2.0f, maxRaw - minRaw);
    float totalSlope = 0.0f;

    mesh.resolutionX = res;
    mesh.resolutionZ = res;
    mesh.minHeight = minRaw;
    mesh.maxHeight = maxRaw;
    mesh.vertices.resize(static_cast<size_t>(res * res));

    for (int rz = 0; rz < res; ++rz) {
        double pn = n0 + static_cast<double>(rz);
        float zLocal = static_cast<float>(rz) - halfSpan;
        for (int rx = 0; rx < res; ++rx) {
            double pe = e0 + static_cast<double>(rx);
            float xLocal = static_cast<float>(rx) - halfSpan;
            size_t idx = static_cast<size_t>(rz * res + rx);
            float rawE = raws[idx];

            MagadiVertex v{};
            v.x = (xLocal / static_cast<float>(cells)) * sceneSpan;
            v.z = (zLocal / static_cast<float>(cells)) * sceneSpan;
            v.y = (rawE - baseDatum) * yScale;
            v.rawElevation = rawE;

            float eL = sampleElevationAtUtmUnlocked(pe - 1.0, pn);
            float eR = sampleElevationAtUtmUnlocked(pe + 1.0, pn);
            float eD = sampleElevationAtUtmUnlocked(pe, pn - 1.0);
            float eU = sampleElevationAtUtmUnlocked(pe, pn + 1.0);
            float gx = (eR - eL) * 0.5f;
            float gy = (eU - eD) * 0.5f;
            float nl = std::sqrt(gx * gx + gy * gy + 1.0f);
            v.nx = -gx / nl;
            v.ny = 1.0f / nl;
            v.nz = -gy / nl;
            v.slopeDeg = std::atan(std::sqrt(gx * gx + gy * gy)) * static_cast<float>(RAD2DEG_D);
            totalSlope += v.slopeDeg;

            float cInt = std::isfinite(contourInterval) ? std::max(contourInterval, 0.5f) : 2.0f;
            v.contourBand = std::clamp((rawE - minRaw) / reliefSpan, 0.0f, 1.0f);
            v.isolineFactor = std::abs(std::fmod(rawE, cInt) / cInt - 0.5f) * 2.0f;

            bool onRoad = isPointOnRoadUtm(pe, pn) || std::abs(offsets[idx]) <= 5.5f;
            bool onBldg = isPointOnBuildingUtm(pe, pn);
            v.isWaterSurface = waters[idx];
            v.isRoadSurface = (!waters[idx] && onRoad);
            v.isMudSurface = (!waters[idx] && std::abs(offsets[idx]) > HALF_WIDTH_M);
            v.isGravelSurface = (!waters[idx] && onBldg);
            v.rockWeight = (v.slopeDeg > 20.0f) ? 0.90f : 0.0f;
            v.grassWeight = (!v.isRoadSurface && !v.isWaterSurface && !v.isMudSurface && v.slopeDeg <= 20.0f) ? 1.0f : 0.0f;
            v.dirtWeight = v.isGravelSurface ? 1.0f : 0.0f;
            v.snowWeight = v.isMudSurface ? 1.0f : 0.0f;
            std::strncpy(v.surfaceType,
                         v.isWaterSurface ? "water" : (v.isRoadSurface ? "asphalt" : (v.isGravelSurface ? "gravel" : (v.isMudSurface ? "trona_soda_crust" : "savannah_scrub"))),
                         sizeof(v.surfaceType) - 1);
            mesh.vertices[idx] = v;
        }
    }
    mesh.avgSlope = totalSlope / static_cast<float>(res * res);

    mesh.indices.reserve(static_cast<size_t>(cells * cells * 6));
    for (int rz = 0; rz < cells; ++rz) {
        for (int rx = 0; rx < cells; ++rx) {
            uint32_t tl = static_cast<uint32_t>(rz * res + rx);
            uint32_t tr = tl + 1;
            uint32_t bl = static_cast<uint32_t>((rz + 1) * res + rx);
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
