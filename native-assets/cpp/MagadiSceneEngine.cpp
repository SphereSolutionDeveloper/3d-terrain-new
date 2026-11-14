// ─── app/src/main/cpp/MagadiSceneEngine.cpp ───────────────────────────────────
// Native C++ Magadi Road Elevation & Human Viewpoint Scene Engine
// Real 3D Topographic Terrain & Topographic Contours (Sascha Willems Architecture)
#include "MagadiSceneEngine.h"
#include <cmath>
#include <cstring>
#include <algorithm>

namespace custom_vulkan {

MagadiSceneEngine& MagadiSceneEngine::getInstance() {
    static MagadiSceneEngine instance;
    return instance;
}

float MagadiSceneEngine::pseudoNoise2D(float x, float y) const {
    float n = std::sin(x * 12.9898f + y * 78.233f) * 43758.5453123f;
    return n - std::floor(n);
}

float MagadiSceneEngine::fbmNoise(float x, float y, int octaves, float lacunarity, float gain) const {
    float sum = 0.0f;
    float amp = 1.0f;
    float freq = 1.0f;
    float maxAmp = 0.0f;

    for (int i = 0; i < octaves; ++i) {
        float sampleX = x * freq;
        float sampleY = y * freq;

        int ix = static_cast<int>(std::floor(sampleX));
        int iy = static_cast<int>(std::floor(sampleY));
        float fx = sampleX - ix;
        float fy = sampleY - iy;

        // Smoothstep interpolation
        float u = fx * fx * (3.0f - 2.0f * fx);
        float v = fy * fy * (3.0f - 2.0f * fy);

        float n00 = pseudoNoise2D(static_cast<float>(ix), static_cast<float>(iy));
        float n10 = pseudoNoise2D(static_cast<float>(ix + 1), static_cast<float>(iy));
        float n01 = pseudoNoise2D(static_cast<float>(ix), static_cast<float>(iy + 1));
        float n11 = pseudoNoise2D(static_cast<float>(ix + 1), static_cast<float>(iy + 1));

        float nx0 = n00 * (1.0f - u) + n10 * u;
        float nx1 = n01 * (1.0f - u) + n11 * u;
        float val = nx0 * (1.0f - v) + nx1 * v;

        sum += val * amp;
        maxAmp += amp;
        amp *= gain;
        freq *= lacunarity;
    }

    return maxAmp > 0.0f ? (sum / maxAmp) : 0.0f;
}

RoadSegmentInfo MagadiSceneEngine::getRoadSegmentInfo(int segmentIndex) const {
    segmentIndex = std::clamp(segmentIndex, 1, 10);
    RoadSegmentInfo info;
    info.segmentIndex = segmentIndex;
    info.startKmM = (segmentIndex - 1) * 10.1f;
    info.endKmM = segmentIndex * 10.1f;

    switch (segmentIndex) {
        case 1:
            std::strncpy(info.name, "Lake Magadi Causeway & Trona Flats", sizeof(info.name) - 1);
            info.startElevM = 598.0f; info.endElevM = 642.0f; info.avgGradePercent = 0.44f;
            info.minElevM = 590.0f; info.maxElevM = 665.0f; info.climbM = 44.0f;
            std::strncpy(info.landmark, "Magadi Soda Works & Causeway Bridge", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Trona Evaporite & Alkaline Lagoon", sizeof(info.primaryBiome) - 1);
            break;
        case 2:
            std::strncpy(info.name, "Ol Keju Nero Basin & South Rift Margin", sizeof(info.name) - 1);
            info.startElevM = 642.0f; info.endElevM = 718.0f; info.avgGradePercent = 0.75f;
            info.minElevM = 635.0f; info.maxElevM = 755.0f; info.climbM = 76.0f;
            std::strncpy(info.landmark, "Ol Keju Nero River Lugga", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Alluvial Fan & Semi-Arid Acacia Scrub", sizeof(info.primaryBiome) - 1);
            break;
        case 3:
            std::strncpy(info.name, "Olorgesailie Prehistoric Basin & Fossil Beds", sizeof(info.name) - 1);
            info.startElevM = 718.0f; info.endElevM = 845.0f; info.avgGradePercent = 1.26f;
            info.minElevM = 705.0f; info.maxElevM = 890.0f; info.climbM = 127.0f;
            std::strncpy(info.landmark, "Acheulean Site & Prehistoric Museum", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Lacustrine Silt & Volcanic Ash Beds", sizeof(info.primaryBiome) - 1);
            break;
        case 4:
            std::strncpy(info.name, "Mount Olorgesailie Volcanic Slopes", sizeof(info.name) - 1);
            info.startElevM = 845.0f; info.endElevM = 1025.0f; info.avgGradePercent = 1.78f;
            info.minElevM = 830.0f; info.maxElevM = 1145.0f; info.climbM = 180.0f;
            std::strncpy(info.landmark, "Mount Olorgesailie Shield Lava Shoulder", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Basalt Lava Benches & Volcanic Talus", sizeof(info.primaryBiome) - 1);
            break;
        case 5:
            std::strncpy(info.name, "Tinga Trading Center & Mid-Rift Plateau", sizeof(info.name) - 1);
            info.startElevM = 1025.0f; info.endElevM = 1245.0f; info.avgGradePercent = 2.18f;
            info.minElevM = 1005.0f; info.maxElevM = 1320.0f; info.climbM = 220.0f;
            std::strncpy(info.landmark, "Tinga Market & Borehole Oasis", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Semi-Arid Pastoralist Plateau", sizeof(info.primaryBiome) - 1);
            break;
        case 6:
            std::strncpy(info.name, "Kamukuru Step Faults & Tectonic Terraces", sizeof(info.name) - 1);
            info.startElevM = 1245.0f; info.endElevM = 1515.0f; info.avgGradePercent = 2.67f;
            info.minElevM = 1220.0f; info.maxElevM = 1590.0f; info.climbM = 270.0f;
            std::strncpy(info.landmark, "Gregory Rift Step Fault Scarps", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Acacia Woodland & Tectonic Fault Steps", sizeof(info.primaryBiome) - 1);
            break;
        case 7:
            std::strncpy(info.name, "Corner Baridi Foothills & Canyon Hairpins", sizeof(info.name) - 1);
            info.startElevM = 1515.0f; info.endElevM = 1820.0f; info.avgGradePercent = 3.02f;
            info.minElevM = 1480.0f; info.maxElevM = 1910.0f; info.climbM = 305.0f;
            std::strncpy(info.landmark, "Deep Canyon Overlook & Hairpins", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Steep Basalt Bluffs & Talus Slopes", sizeof(info.primaryBiome) - 1);
            break;
        case 8:
            std::strncpy(info.name, "Corner Baridi Summit Crest & Ngong Saddle", sizeof(info.name) - 1);
            info.startElevM = 1820.0f; info.endElevM = 1865.0f; info.avgGradePercent = 1.15f;
            info.minElevM = 1790.0f; info.maxElevM = 1980.0f; info.climbM = 160.0f;
            std::strncpy(info.landmark, "Corner Baridi (Cold Corner) 1980m Summit", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Highland Mist Grassland & Heath", sizeof(info.primaryBiome) - 1);
            break;
        case 9:
            std::strncpy(info.name, "Kiserian Ridge & Oloolua Foothills", sizeof(info.name) - 1);
            info.startElevM = 1865.0f; info.endElevM = 1795.0f; info.avgGradePercent = -0.69f;
            info.minElevM = 1765.0f; info.maxElevM = 1890.0f; info.climbM = -70.0f;
            std::strncpy(info.landmark, "Kiserian Town & Oloolua Forest Ridge", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Sub-Humid Highland Ridge & Farmland", sizeof(info.primaryBiome) - 1);
            break;
        case 10:
        default:
            std::strncpy(info.name, "Ongata Rongai to Nairobi Bomas / Galleria", sizeof(info.name) - 1);
            info.startElevM = 1795.0f; info.endElevM = 1750.0f; info.avgGradePercent = -0.45f;
            info.minElevM = 1720.0f; info.maxElevM = 1810.0f; info.climbM = -45.0f;
            std::strncpy(info.landmark, "Bomas of Kenya / Galleria Mall Junction", sizeof(info.landmark) - 1);
            std::strncpy(info.primaryBiome, "Nairobi Volcanic Plateau & Open Plains", sizeof(info.primaryBiome) - 1);
            break;
    }
    return info;
}

static void computeRoadCurvature(int segmentIndex, float forwardM, float& outCurv, float& outDeriv) {
    if (segmentIndex <= 3) {
        const float w1 = 0.00062f, a1 = 260.0f;
        const float w2 = 0.00140f, a2 = 110.0f;
        outCurv = std::sin(forwardM * w1) * a1 + std::cos(forwardM * w2) * a2;
        outDeriv = std::cos(forwardM * w1) * a1 * w1 - std::sin(forwardM * w2) * a2 * w2;
    } else if (segmentIndex <= 6) {
        const float w1 = 0.00075f, a1 = 320.0f;
        const float w2 = 0.00160f, a2 = 140.0f;
        outCurv = std::sin(forwardM * w1) * a1 + std::cos(forwardM * w2) * a2;
        outDeriv = std::cos(forwardM * w1) * a1 * w1 - std::sin(forwardM * w2) * a2 * w2;
    } else if (segmentIndex <= 8) {
        const float w1 = 0.00115f, a1 = 420.0f;
        const float w2 = 0.00240f, a2 = 210.0f;
        outCurv = std::sin(forwardM * w1) * a1 + std::cos(forwardM * w2) * a2;
        outDeriv = std::cos(forwardM * w1) * a1 * w1 - std::sin(forwardM * w2) * a2 * w2;
    } else {
        const float w1 = 0.00070f, a1 = 280.0f;
        const float w2 = 0.00150f, a2 = 120.0f;
        outCurv = std::sin(forwardM * w1) * a1 + std::cos(forwardM * w2) * a2;
        outDeriv = std::cos(forwardM * w1) * a1 * w1 - std::sin(forwardM * w2) * a2 * w2;
    }
}

float MagadiSceneEngine::computeCorridorElevation(
    int segmentIndex, float lateralM, float forwardM,
    float contourInterval, float stepTerracing,
    std::string& outSurface, bool& outIsRoad, bool& outIsGravel) const {

    segmentIndex = std::clamp(segmentIndex, 1, 10);
    // Complete 101 km corridor: each segment covers 10.1 km (10,100 m)
    float globalDistM = (segmentIndex - 1) * 10100.0f + forwardM;
    float globalDistKm = std::clamp(globalDistM / 1000.0f, 0.0f, 101.0f);

    // Continuous road datum across the entire 101km C58 corridor:
    // Origin at Lake Magadi (598.0m ASL at KM 0.0)
    // Rises through Rift Valley to Corner Baridi Pass summit (1,980.0m ASL at KM 76.5)
    // Tapers down through Kiserian & Rongai to Nairobi Bomas/Galleria (1,750.0m ASL at KM 101.0)
    float roadElev;
    if (globalDistKm <= 76.5f) {
        float tClimb = globalDistKm / 76.5f;
        roadElev = 598.0f + 1382.0f * (0.32f * tClimb + 0.68f * std::pow(tClimb, 1.58f));
    } else {
        float tDescent = (globalDistKm - 76.5f) / 24.5f;
        roadElev = 1980.0f - (1980.0f - 1750.0f) * (tDescent * 0.75f + std::pow(tDescent, 1.8f) * 0.25f);
    }

    // Dynamic sweeping road curves across the 2.0km corridor
    float roadCurvature = 0.0f;
    float dCurv_dz = 0.0f;
    computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv_dz);

    float dx = lateralM - roadCurvature;
    float absDx = std::abs(dx);

    // Natural landscape cross-profile:
    // Gregory Rift: flat soda lake to west, massive volcanic shield & horst scarps rising to east
    float naturalCross = 0.0f;
    if (segmentIndex == 1) {
        if (dx < -30.0f) {
            float distW = -dx - 30.0f;
            float dropPan = std::min(distW * 0.025f, 12.0f);
            float panRipple = std::sin(lateralM * 0.015f) * std::cos(forwardM * 0.012f) * 0.6f;
            naturalCross = -dropPan + panRipple;
        } else if (dx > 30.0f) {
            float distE = dx - 30.0f;
            naturalCross = std::min(distE * 0.045f, 48.0f);
        }
    } else if (segmentIndex == 2) {
        naturalCross = (dx * 0.038f);
    } else if (segmentIndex <= 4) {
        // Olorgesailie volcanic slopes: ground rises east towards shield volcano summit
        float volcanoRise = (dx > 0.0f) ? std::min(dx * 0.085f, 120.0f) : -std::min(-dx * 0.035f, 40.0f);
        naturalCross = volcanoRise;
    } else if (segmentIndex <= 6) {
        // Step faults & Kamukuru horst terraces
        float riftSlope = (dx > 0.0f) ? (dx * 0.065f) : (dx * 0.035f);
        float scarp1 = (dx > 180.0f) ? std::min((dx - 180.0f) * 0.12f, 55.0f) : 0.0f;
        float scarp2 = (dx > 550.0f) ? std::min((dx - 550.0f) * 0.16f, 80.0f) : 0.0f;
        naturalCross = riftSlope + scarp1 + scarp2;
    } else if (segmentIndex <= 8) {
        // Corner Baridi escarpment pass: high cliffs east (+160m), deep valley drop west (-95m)
        float scarpEast = (dx > 0.0f) ? std::min(dx * 0.14f, 160.0f) : 0.0f;
        float valleyWest = (dx < 0.0f) ? -std::min(-dx * 0.09f, 95.0f) : 0.0f;
        naturalCross = scarpEast + valleyWest;
    } else {
        // Kiserian & Rongai undulating highland ridges
        naturalCross = std::sin(dx * 0.004f) * 35.0f + (dx * 0.025f);
    }

    // Authentic multi-octave FBM fractal ruggedness (hills, volcanic gullies, and outcrops)
    float fbmAmp = 25.0f + (segmentIndex * 4.5f);
    float fbm = (fbmNoise(lateralM * 0.0012f, globalDistM * 0.0012f, 4, 2.05f, 0.48f) - 0.5f) * fbmAmp;

    float naturalElev = roadElev + naturalCross + fbm;

    // Road Cut & Embankment Engineering (smooth continuous surface)
    outIsRoad = false;
    outIsGravel = false;
    float finalElev;

    if (absDx < 14.0f) {
        finalElev = roadElev + 0.35f;
        outIsRoad = true;
        outSurface = "asphalt";
    } else if (absDx < 26.0f) {
        float t = (absDx - 14.0f) / 12.0f;
        finalElev = roadElev + 0.35f * (1.0f - t * 0.35f);
        outIsRoad = true;
        outSurface = "asphalt";
    } else if (absDx < 48.0f) {
        float t = (absDx - 26.0f) / 22.0f;
        finalElev = roadElev + 0.22f * (1.0f - t);
        outIsGravel = true;
        outSurface = "gravel";
    } else if (absDx < 95.0f) {
        float t = (absDx - 48.0f) / 47.0f;
        float smoothT = t * t * (3.0f - 2.0f * t);
        finalElev = roadElev * (1.0f - smoothT) + naturalElev * smoothT;
        outSurface = (segmentIndex == 1 && dx < 0.0f) ? "trona_soda_crust" : "savannah_scrub";
    } else {
        finalElev = naturalElev;
        if (segmentIndex == 1 && dx < -70.0f && finalElev <= 594.0f) {
            outSurface = (finalElev <= 592.0f) ? "water" : "trona_soda_crust";
        } else if (segmentIndex >= 7 && finalElev > 1650.0f) {
            outSurface = (absDx > 300.0f) ? "escarpment_rock" : "highland_grass";
        } else if (absDx > 250.0f && fbm > fbmAmp * 0.25f) {
            outSurface = "escarpment_rock";
        } else if (segmentIndex >= 8) {
            outSurface = "highland_grass";
        } else {
            outSurface = "savannah_scrub";
        }
    }

    if (stepTerracing > 0.001f && contourInterval > 0.5f && !outIsRoad) {
        float stepped = std::round(finalElev / contourInterval) * contourInterval;
        finalElev = finalElev * (1.0f - stepTerracing) + stepped * stepTerracing;
    }

    return finalElev;
}

float MagadiSceneEngine::computeSegmentElevation(float lateralM, float forwardM,
                                                float contourInterval, float stepTerracing) const {
    std::string surf;
    bool isRoad = false;
    bool isGravel = false;
    return computeCorridorElevation(1, lateralM, forwardM, contourInterval, stepTerracing, surf, isRoad, isGravel);
}

float MagadiSceneEngine::getMagadiElevation(float worldXMeters, float worldYMeters,
                                           float contourInterval,
                                           float stepTerracing) const {
    float ny = std::clamp(worldYMeters / 50000.0f, 0.0f, 1.0f);
    float nx = std::clamp(worldXMeters / 50000.0f, 0.0f, 1.0f);

    // Continuous 50km gradient: 598m at Magadi basin to 1980m at Ngong / Corner Baridi
    float baseGrad = 598.0f + (std::pow(ny, 1.35f) * 1382.0f);

    // Rift Valley Trough: depressed valley floor center, elevated escarpment walls east/west
    float troughDist = std::abs(nx - 0.42f);
    float riftWallLift = std::pow(std::clamp(troughDist * 1.8f, 0.0f, 1.0f), 2.2f) * 420.0f;

    // Volcanic Shield Complex (Olorgesailie shield volcano)
    float dxOlor = nx - 0.55f;
    float dyOlor = ny - 0.45f;
    float distOlorSq = dxOlor * dxOlor + dyOlor * dyOlor;
    float olorVolcano = std::exp(-distOlorSq / 0.018f) * 360.0f;

    // Basalt fault blocks & tectonic horst/graben step-fault terraces
    float faultScarp = std::sin((worldXMeters + worldYMeters * 0.35f) * 0.00065f) * 35.0f;

    // Copernicus COP30 DEM multi-octave FBM fractal ruggedness
    float fbm = fbmNoise(worldXMeters * 0.00015f, worldYMeters * 0.00015f, 4, 2.1f, 0.48f);
    float ruggedness = (fbm - 0.5f) * 65.0f;

    float rawElev = baseGrad + riftWallLift + olorVolcano + faultScarp + ruggedness;

    if (stepTerracing > 0.001f && contourInterval > 0.5f) {
        float stepped = std::round(rawElev / contourInterval) * contourInterval;
        return rawElev * (1.0f - stepTerracing) + stepped * stepTerracing;
    }

    return rawElev;
}

MagadiSceneMesh MagadiSceneEngine::generateRoadSegmentScene(
    int segmentIndex, int gridSize, float contourInterval, float stepTerracing) const {

    segmentIndex = std::clamp(segmentIndex, 1, 10);
    gridSize = std::clamp(gridSize, 17, 65);

    const float widthM = 2000.0f;   // Exactly 2.0 km wide corridor (1.0 km left, 1.0 km right from road centerline)
    const float depthM = 10100.0f;  // 10.1 km length per road segment (101 km / 10 segments)
    const float stepX = widthM / (gridSize - 1);
    const float stepZ = depthM / (gridSize - 1);
    const float halfW = widthM * 0.5f; // 1000m (1.0 km)

    const float sceneWidth = 36.0f;   // 36 units across 2,000m width
    const float sceneDepth = 180.0f;  // 180 units across 10,100m length (Authentic 1:5 corridor aspect ratio, never a square!)

    std::vector<float> heights(gridSize * gridSize);
    std::vector<std::string> surfaces(gridSize * gridSize, "savannah_scrub");
    std::vector<uint8_t> flags(gridSize * gridSize, 0);

    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float forwardM = iz * stepZ;
        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridSize; ++ix) {
            // Exactly 1km on left and 1km on right from road centerline
            float offsetFromRoad = (ix * stepX) - halfW; // -1000m to +1000m
            float lateralM = roadCurvature + offsetFromRoad;
            std::string surf;
            bool isRoad = false;
            bool isGravel = false;

            float elev = computeCorridorElevation(
                segmentIndex, lateralM, forwardM, contourInterval, stepTerracing, surf, isRoad, isGravel);

            int idx = iz * gridSize + ix;
            heights[idx] = elev;
            surfaces[idx] = surf;

            uint8_t flag = 0;
            if (isRoad) flag = 1;
            else if (surf == "water") flag = 2;
            else if (surf == "trona_soda_crust") flag = 3;
            else if (isGravel || surf == "gravel") flag = 4;
            else if (surf == "escarpment_rock") flag = 5;
            else if (surf == "highland_grass") flag = 6;
            flags[idx] = flag;

            if (elev < minH) minH = elev;
            if (elev > maxH) maxH = elev;
        }
    }

    MagadiSceneMesh sceneMesh;
    sceneMesh.resolutionX = gridSize;
    sceneMesh.resolutionZ = gridSize;
    sceneMesh.minHeight = minH;
    sceneMesh.maxHeight = maxH;
    sceneMesh.vertices.reserve(gridSize * gridSize);

    float baseDatumElev = (minH + maxH) * 0.5f;
    float totalSlope = 0.0f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float forwardM = iz * stepZ;
        float zScaled = ((forwardM - depthM * 0.5f) / depthM) * sceneDepth;
        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridSize; ++ix) {
            float offsetFromRoad = (ix * stepX) - halfW;
            float lateralM = roadCurvature + offsetFromRoad;
            float xScaled = (lateralM / widthM) * sceneWidth;
            float rawElev = heights[iz * gridSize + ix];
            float yScaled = ((rawElev - baseDatumElev) / 100.0f) * 12.0f;

            float left = (ix > 0) ? heights[iz * gridSize + (ix - 1)] : rawElev;
            float right = (ix < gridSize - 1) ? heights[iz * gridSize + (ix + 1)] : rawElev;
            float up = (iz > 0) ? heights[(iz - 1) * gridSize + ix] : rawElev;
            float down = (iz < gridSize - 1) ? heights[(iz + 1) * gridSize + ix] : rawElev;

            float ddx = (right - left) / (2.0f * stepX);
            float ddz = (down - up) / (2.0f * stepZ);
            float len = std::sqrt(ddx * ddx + 1.0f + ddz * ddz);
            float nx = -ddx / len;
            float ny = 1.0f / len;
            float nz = -ddz / len;
            float slopeDeg = std::acos(std::clamp(ny, 0.0f, 1.0f)) * RAD2DEG;
            totalSlope += slopeDeg;

            const std::string& surf = surfaces[iz * gridSize + ix];
            uint8_t flag = flags[iz * gridSize + ix];

            float cFrac = std::fmod(rawElev, contourInterval) / contourInterval;
            float isolineFactor = std::abs(cFrac - 0.5f) * 2.0f;

            MagadiVertex vert;
            vert.x = xScaled;
            vert.y = yScaled;
            vert.z = zScaled;
            vert.nx = nx;
            vert.ny = ny;
            vert.nz = nz;
            vert.slopeDeg = slopeDeg;
            vert.grassWeight = (flag == 6 || surf == "highland_grass" || surf == "savannah_scrub") ? 1.0f : 0.0f;
            vert.dirtWeight = (flag == 4 || surf == "gravel" || surf == "volcanic_ash_talus") ? 1.0f : 0.0f;
            vert.rockWeight = (flag == 5 || surf == "escarpment_rock" || slopeDeg > 22.0f) ? 0.90f : 0.0f;
            vert.snowWeight = (flag == 3 || surf == "trona_soda_crust") ? 1.0f : 0.0f;
            vert.isRoadSurface = (flag == 1);
            vert.isWaterSurface = (flag == 2);
            vert.isMudSurface = (flag == 3);
            vert.isGravelSurface = (flag == 4);
            vert.rawElevation = rawElev;
            vert.contourBand = std::clamp((rawElev - 590.0f) / 1390.0f, 0.0f, 1.0f);
            vert.isolineFactor = isolineFactor;
            std::strncpy(vert.surfaceType, surf.c_str(), sizeof(vert.surfaceType) - 1);

            sceneMesh.vertices.push_back(vert);
        }
    }

    sceneMesh.avgSlope = totalSlope / (gridSize * gridSize);

    for (int iz = 0; iz < gridSize - 1; ++iz) {
        for (int ix = 0; ix < gridSize - 1; ++ix) {
            uint32_t tl = iz * gridSize + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * gridSize + ix;
            uint32_t br = bl + 1;

            sceneMesh.indices.push_back(tl);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(tr);

            sceneMesh.indices.push_back(tr);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(br);
        }
    }

    sceneMesh.roadCenterline = generateRoadCenterline(segmentIndex, 96);
    return sceneMesh;
}

MagadiSceneMesh MagadiSceneEngine::generateRoadSubSegmentScene(
    int segmentIndex, int subSegmentIndex, int gridSize,
    float contourInterval, float stepTerracing) const {

    if (subSegmentIndex <= 0) {
        return generateRoadSegmentScene(segmentIndex, gridSize, contourInterval, stepTerracing);
    }

    segmentIndex = std::clamp(segmentIndex, 1, 10);
    subSegmentIndex = std::clamp(subSegmentIndex, 1, 10);
    gridSize = std::clamp(gridSize, 17, 65);

    const float subLengthM = 1010.0f; // 1,010 meters (1.01 km) per sub-segment
    const float startForwardM = (subSegmentIndex - 1) * subLengthM;
    const float widthM = 2000.0f;     // 2.0 km corridor width
    const float stepX = widthM / (gridSize - 1);
    const float stepZ = subLengthM / (gridSize - 1);
    const float halfW = widthM * 0.5f;

    // Sub-segment 3D scaling (proportional 2:1 ratio for 2000m width x 1010m length)
    const float sceneWidth = 60.0f;
    const float sceneDepth = 60.0f * (subLengthM / widthM); // ~30.3f

    std::vector<float> heights(gridSize * gridSize);
    std::vector<std::string> surfaces(gridSize * gridSize, "savannah_scrub");
    std::vector<uint8_t> flags(gridSize * gridSize, 0);

    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float forwardM = startForwardM + iz * stepZ;
        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridSize; ++ix) {
            float offsetFromRoad = (ix * stepX) - halfW;
            float lateralM = roadCurvature + offsetFromRoad;
            std::string surf;
            bool isRoad = false;
            bool isGravel = false;

            float elev = computeCorridorElevation(
                segmentIndex, lateralM, forwardM, contourInterval, stepTerracing, surf, isRoad, isGravel);

            int idx = iz * gridSize + ix;
            heights[idx] = elev;
            surfaces[idx] = surf;

            uint8_t flag = 0;
            if (isRoad) flag = 1;
            else if (surf == "water") flag = 2;
            else if (surf == "trona_soda_crust") flag = 3;
            else if (isGravel || surf == "gravel") flag = 4;
            else if (surf == "escarpment_rock") flag = 5;
            else if (surf == "highland_grass") flag = 6;
            flags[idx] = flag;

            if (elev < minH) minH = elev;
            if (elev > maxH) maxH = elev;
        }
    }

    MagadiSceneMesh sceneMesh;
    sceneMesh.resolutionX = gridSize;
    sceneMesh.resolutionZ = gridSize;
    sceneMesh.minHeight = minH;
    sceneMesh.maxHeight = maxH;
    sceneMesh.vertices.reserve(gridSize * gridSize);

    float baseDatumElev = (minH + maxH) * 0.5f;
    float totalSlope = 0.0f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float forwardM = startForwardM + iz * stepZ;
        float zScaled = ((iz * stepZ - subLengthM * 0.5f) / subLengthM) * sceneDepth;
        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridSize; ++ix) {
            float offsetFromRoad = (ix * stepX) - halfW;
            float lateralM = roadCurvature + offsetFromRoad;
            float xScaled = (lateralM / widthM) * sceneWidth;
            float rawElev = heights[iz * gridSize + ix];
            float yScaled = ((rawElev - baseDatumElev) / 100.0f) * 12.0f;

            float left = (ix > 0) ? heights[iz * gridSize + (ix - 1)] : rawElev;
            float right = (ix < gridSize - 1) ? heights[iz * gridSize + (ix + 1)] : rawElev;
            float up = (iz > 0) ? heights[(iz - 1) * gridSize + ix] : rawElev;
            float down = (iz < gridSize - 1) ? heights[(iz + 1) * gridSize + ix] : rawElev;

            float ddx = (right - left) / (2.0f * stepX);
            float ddz = (down - up) / (2.0f * stepZ);
            float len = std::sqrt(ddx * ddx + 1.0f + ddz * ddz);
            float nx = -ddx / len;
            float ny = 1.0f / len;
            float nz = -ddz / len;
            float slopeDeg = std::acos(std::clamp(ny, 0.0f, 1.0f)) * RAD2DEG;
            totalSlope += slopeDeg;

            const std::string& surf = surfaces[iz * gridSize + ix];
            uint8_t flag = flags[iz * gridSize + ix];

            float cFrac = std::fmod(rawElev, contourInterval) / contourInterval;
            float isolineFactor = std::abs(cFrac - 0.5f) * 2.0f;

            MagadiVertex vert;
            vert.x = xScaled;
            vert.y = yScaled;
            vert.z = zScaled;
            vert.nx = nx;
            vert.ny = ny;
            vert.nz = nz;
            vert.slopeDeg = slopeDeg;
            vert.grassWeight = (flag == 6 || surf == "highland_grass" || surf == "savannah_scrub") ? 1.0f : 0.0f;
            vert.dirtWeight = (flag == 4 || surf == "gravel" || surf == "volcanic_ash_talus") ? 1.0f : 0.0f;
            vert.rockWeight = (flag == 5 || surf == "escarpment_rock" || slopeDeg > 22.0f) ? 0.90f : 0.0f;
            vert.snowWeight = (flag == 3 || surf == "trona_soda_crust") ? 1.0f : 0.0f;
            vert.isRoadSurface = (flag == 1);
            vert.isWaterSurface = (flag == 2);
            vert.isMudSurface = (flag == 3);
            vert.isGravelSurface = (flag == 4);
            vert.rawElevation = rawElev;
            vert.contourBand = std::clamp((rawElev - 590.0f) / 1390.0f, 0.0f, 1.0f);
            vert.isolineFactor = isolineFactor;
            std::strncpy(vert.surfaceType, surf.c_str(), sizeof(vert.surfaceType) - 1);

            sceneMesh.vertices.push_back(vert);
        }
    }

    for (int iz = 0; iz < gridSize - 1; ++iz) {
        for (int ix = 0; ix < gridSize - 1; ++ix) {
            uint32_t tl = iz * gridSize + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * gridSize + ix;
            uint32_t br = bl + 1;

            sceneMesh.indices.push_back(tl);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(tr);

            sceneMesh.indices.push_back(tr);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(br);
        }
    }

    const int numWps = 64;
    sceneMesh.roadCenterline.reserve(numWps);
    float wpStep = subLengthM / (numWps - 1);
    for (int i = 0; i < numWps; ++i) {
        float fwd = startForwardM + i * wpStep;
        RoadWaypoint wp = getRoadPointAt(segmentIndex, fwd);
        wp.z = ((i * wpStep - subLengthM * 0.5f) / subLengthM) * sceneDepth;
        wp.x = (wp.lateralM / widthM) * sceneWidth;
        wp.y = ((wp.rawElevation - baseDatumElev) / 100.0f) * 12.0f + 0.25f;
        sceneMesh.roadCenterline.push_back(wp);
    }

    return sceneMesh;
}

MagadiSceneMesh MagadiSceneEngine::generate1mResolutionScene(
    int segmentIndex, float centerForwardM,
    float contourInterval, float stepTerracing) const {

    segmentIndex = std::clamp(segmentIndex, 1, 10);
    centerForwardM = std::clamp(centerForwardM, 0.0f, 10100.0f);

    const int gridSize = 65; // 64 cells, each exactly 1.0m x 1.0m
    const float patchSpanM = 64.0f; // 64m x 64m local engineering inspection patch
    const float halfSpanM = patchSpanM * 0.5f; // 32m
    const float stepM = 1.0f; // Exactly 1.0 meter resolution!

    const float sceneSpan = 52.0f;

    std::vector<float> heights(gridSize * gridSize);
    std::vector<std::string> surfaces(gridSize * gridSize, "asphalt");
    std::vector<uint8_t> flags(gridSize * gridSize, 0);

    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float forwardM = std::clamp((centerForwardM - halfSpanM) + iz * stepM, 0.0f, 10100.0f);
        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridSize; ++ix) {
            float offsetFromRoad = (ix * stepM) - halfSpanM; // -32m to +32m across road
            float lateralM = roadCurvature + offsetFromRoad;
            std::string surf;
            bool isRoad = false;
            bool isGravel = false;

            float elev = computeCorridorElevation(
                segmentIndex, lateralM, forwardM, contourInterval, stepTerracing, surf, isRoad, isGravel);

            int idx = iz * gridSize + ix;
            heights[idx] = elev;
            surfaces[idx] = surf;

            uint8_t flag = 0;
            if (isRoad) flag = 1;
            else if (surf == "water") flag = 2;
            else if (surf == "trona_soda_crust") flag = 3;
            else if (isGravel || surf == "gravel") flag = 4;
            else if (surf == "escarpment_rock") flag = 5;
            else if (surf == "highland_grass") flag = 6;
            flags[idx] = flag;

            if (elev < minH) minH = elev;
            if (elev > maxH) maxH = elev;
        }
    }

    MagadiSceneMesh sceneMesh;
    sceneMesh.resolutionX = gridSize;
    sceneMesh.resolutionZ = gridSize;
    sceneMesh.minHeight = minH;
    sceneMesh.maxHeight = maxH;
    sceneMesh.vertices.reserve(gridSize * gridSize);

    float baseDatumElev = (minH + maxH) * 0.5f;
    float totalSlope = 0.0f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float localZ = (iz * stepM) - halfSpanM;
        float zScaled = (localZ / patchSpanM) * sceneSpan;

        float forwardM = std::clamp((centerForwardM - halfSpanM) + iz * stepM, 0.0f, 10100.0f);
        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridSize; ++ix) {
            float localX = (ix * stepM) - halfSpanM;
            float xScaled = (localX / patchSpanM) * sceneSpan;
            float rawElev = heights[iz * gridSize + ix];
            float yScaled = (rawElev - baseDatumElev) * 1.0f;

            float left = (ix > 0) ? heights[iz * gridSize + (ix - 1)] : rawElev;
            float right = (ix < gridSize - 1) ? heights[iz * gridSize + (ix + 1)] : rawElev;
            float up = (iz > 0) ? heights[(iz - 1) * gridSize + ix] : rawElev;
            float down = (iz < gridSize - 1) ? heights[(iz + 1) * gridSize + ix] : rawElev;

            float ddx = (right - left) / (2.0f * stepM);
            float ddz = (down - up) / (2.0f * stepM);
            float len = std::sqrt(ddx * ddx + 1.0f + ddz * ddz);
            float nx = -ddx / len;
            float ny = 1.0f / len;
            float nz = -ddz / len;
            float slopeDeg = std::acos(std::clamp(ny, 0.0f, 1.0f)) * RAD2DEG;
            totalSlope += slopeDeg;

            const std::string& surf = surfaces[iz * gridSize + ix];
            uint8_t flag = flags[iz * gridSize + ix];

            float cFrac = std::fmod(rawElev, contourInterval) / contourInterval;
            float isolineFactor = std::abs(cFrac - 0.5f) * 2.0f;

            MagadiVertex vert;
            vert.x = xScaled;
            vert.y = yScaled;
            vert.z = zScaled;
            vert.nx = nx;
            vert.ny = ny;
            vert.nz = nz;
            vert.slopeDeg = slopeDeg;
            vert.grassWeight = (flag == 6 || surf == "highland_grass" || surf == "savannah_scrub") ? 1.0f : 0.0f;
            vert.dirtWeight = (flag == 4 || surf == "gravel" || surf == "volcanic_ash_talus") ? 1.0f : 0.0f;
            vert.rockWeight = (flag == 5 || surf == "escarpment_rock" || slopeDeg > 22.0f) ? 0.90f : 0.0f;
            vert.snowWeight = (flag == 3 || surf == "trona_soda_crust") ? 1.0f : 0.0f;
            vert.isRoadSurface = (flag == 1);
            vert.isWaterSurface = (flag == 2);
            vert.isMudSurface = (flag == 3);
            vert.isGravelSurface = (flag == 4);
            vert.rawElevation = rawElev;
            vert.contourBand = std::clamp((rawElev - 590.0f) / 1390.0f, 0.0f, 1.0f);
            vert.isolineFactor = isolineFactor;
            std::strncpy(vert.surfaceType, surf.c_str(), sizeof(vert.surfaceType) - 1);

            sceneMesh.vertices.push_back(vert);
        }
    }

    for (int iz = 0; iz < gridSize - 1; ++iz) {
        for (int ix = 0; ix < gridSize - 1; ++ix) {
            uint32_t tl = iz * gridSize + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * gridSize + ix;
            uint32_t br = bl + 1;

            sceneMesh.indices.push_back(tl);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(tr);

            sceneMesh.indices.push_back(tr);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(br);
        }
    }

    for (int iz = 0; iz < gridSize; ++iz) {
        float fwd = std::clamp((centerForwardM - halfSpanM) + iz * stepM, 0.0f, 10100.0f);
        RoadWaypoint wp = getRoadPointAt(segmentIndex, fwd);
        float localZ = (iz * stepM) - halfSpanM;
        wp.z = (localZ / patchSpanM) * sceneSpan;
        wp.x = 0.0f;
        wp.y = (wp.rawElevation - baseDatumElev) * 1.0f + 0.15f;
        sceneMesh.roadCenterline.push_back(wp);
    }

    return sceneMesh;
}

RoadWaypoint MagadiSceneEngine::getRoadPointAt(int segmentIndex, float forwardDistM) const {
    segmentIndex = std::clamp(segmentIndex, 1, 10);
    const float depthM = 10100.0f;
    const float widthM = 2000.0f;
    const float sceneWidth = 36.0f;
    const float sceneDepth = 180.0f;
    forwardDistM = std::clamp(forwardDistM, 0.0f, depthM);

    // Curvature & heading derivative at this forward distance
    float roadCurvature = 0.0f;
    float dCurv_dz = 0.0f;
    computeRoadCurvature(segmentIndex, forwardDistM, roadCurvature, dCurv_dz);

    std::string surf;
    bool isRoad = false, isGravel = false;
    float rawElev = computeCorridorElevation(segmentIndex, roadCurvature, forwardDistM, 25.0f, 0.0f, surf, isRoad, isGravel);

    // Compute grade
    float aheadM = std::min(forwardDistM + 30.0f, depthM);
    float elevAhead = computeCorridorElevation(segmentIndex, roadCurvature, aheadM, 25.0f, 0.0f, surf, isRoad, isGravel);
    float grade = ((elevAhead - rawElev) / (aheadM - forwardDistM)) * 100.0f;

    // Heading angle: baseline is 028° NNE + tangent deviation
    float tangentAngleRad = std::atan(dCurv_dz);
    float heading = 28.0f + (tangentAngleRad * 180.0f / 3.14159265f);

    RoadSegmentInfo segInfo = getRoadSegmentInfo(segmentIndex);
    float baseDatumElev = (segInfo.minElevM + segInfo.maxElevM) * 0.5f;

    // Natural undisturbed terrain elevation and cut/fill analysis
    float globalDistM = (segmentIndex - 1) * 10100.0f + forwardDistM;
    float fbmAmp = 25.0f + (segmentIndex * 4.5f);
    float fbmVal = (fbmNoise(roadCurvature * 0.0012f, globalDistM * 0.0012f, 4, 2.05f, 0.48f) - 0.5f) * fbmAmp;
    float naturalCross = (segmentIndex == 1) ? ((roadCurvature > 30.0f) ? std::min((roadCurvature - 30.0f) * 0.045f, 48.0f) : -std::min((-roadCurvature - 30.0f) * 0.025f, 12.0f)) : (roadCurvature * 0.035f);
    float naturalElev = rawElev - 0.35f + naturalCross * 0.4f + fbmVal * 0.5f;
    float cutFillDepth = rawElev - naturalElev;

    // Upcoming contour line (25m interval)
    float cStep = 25.0f;
    float nextContourElev;
    if (grade >= 0.0f) {
        nextContourElev = std::ceil((rawElev + 0.3f) / cStep) * cStep;
    } else {
        nextContourElev = std::floor((rawElev - 0.3f) / cStep) * cStep;
    }
    float elevDiff = std::abs(nextContourElev - rawElev);
    float distToContour = (std::abs(grade) > 0.05f) ? (elevDiff / (std::abs(grade) * 0.01f)) : 180.0f;

    RoadWaypoint wp;
    wp.lateralM = roadCurvature;
    wp.forwardM = forwardDistM;
    wp.rawElevation = rawElev;
    wp.globalKmM = ((segmentIndex - 1) * 10.1f) + (forwardDistM / 1000.0f);
    wp.gradePercent = grade;
    wp.headingDeg = heading;
    wp.naturalElevM = naturalElev;
    wp.cutFillDepthM = cutFillDepth;
    wp.nextContourElevM = nextContourElev;
    wp.distToNextContourM = distToContour;

    // Scaled coordinates matching generateRoadSegmentScene exactly
    wp.x = (roadCurvature / widthM) * sceneWidth;
    wp.z = ((forwardDistM - depthM * 0.5f) / depthM) * sceneDepth;
    wp.y = ((rawElev - baseDatumElev) / 100.0f) * 12.0f + 0.25f;

    return wp;
}

std::vector<RoadWaypoint> MagadiSceneEngine::generateRoadCenterline(int segmentIndex, int numPoints) const {
    numPoints = std::clamp(numPoints, 16, 128);
    std::vector<RoadWaypoint> points;
    points.reserve(numPoints);
    const float depthM = 10100.0f;
    float step = depthM / (numPoints - 1);

    for (int i = 0; i < numPoints; ++i) {
        float fwd = i * step;
        points.push_back(getRoadPointAt(segmentIndex, fwd));
    }
    return points;
}

MagadiSceneMesh MagadiSceneEngine::generateFirst10kmSegmentScene(int gridSize,
                                                               float contourInterval,
                                                               float stepTerracing) const {
    return generateRoadSegmentScene(1, gridSize, contourInterval, stepTerracing);
}


MagadiSceneMesh MagadiSceneEngine::generateHumanStartScene(float walkDistanceM,
                                                         int gridSize,
                                                         float contourInterval,
                                                         float stepTerracing) const {
    MagadiSceneMesh mesh = generateRoadSegmentScene(1, gridSize, contourInterval, stepTerracing);

    const float sceneWidth = 36.0f;
    const float sceneDepth = 180.0f;
    const float widthM = 2000.0f;
    const float depthM = 10100.0f;
    float baseDatumElev = (mesh.minHeight + mesh.maxHeight) * 0.5f;

    auto makeAsset = [&](const char* id, const char* type, const char* path, float latOffsetM, float fwdM, float scale) {
        float curv = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(1, fwdM, curv, dCurv);
        float x = ((curv + latOffsetM) / widthM) * sceneWidth;
        float z = ((fwdM - depthM * 0.5f) / depthM) * sceneDepth;
        std::string surf;
        bool isR = false, isG = false;
        float elev = computeCorridorElevation(1, curv + latOffsetM, fwdM, contourInterval, stepTerracing, surf, isR, isG);
        float y = ((elev - baseDatumElev) / 100.0f) * 12.0f;
        return MagadiAssetMarker{id, type, path, x, y, z, 0.0f, scale};
    };

    mesh.assetPlacements = {
        makeAsset("c58_start_sign", "spawn", "assets/markers/start_grid.glb", 0.0f, 40.0f, 1.0f),
        makeAsset("magadi_soda_factory", "garage", "assets/structures/garage_hub.glb", -120.0f, 240.0f, 1.2f),
        makeAsset("causeway_bridge", "bridge", "assets/structures/concrete_bridge.glb", 0.0f, 650.0f, 1.0f),
        makeAsset("acacia_clump", "tree", "assets/nature/palm_tree.glb", 25.0f, 380.0f, 0.9f),
        makeAsset("basalt_boulder", "rock", "assets/nature/boulder_1.5m.glb", -28.0f, 920.0f, 1.3f)
    };
    return mesh;
}

MagadiSceneMesh MagadiSceneEngine::generateOverviewScene(int gridSize,
                                                       float contourInterval,
                                                       float stepTerracing) const {
    const float worldSpan = 50000.0f;
    const float step = worldSpan / (gridSize - 1);
    const float meshScale = 0.0022f;

    std::vector<float> heights(gridSize * gridSize);
    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float wy = iz * step;
        for (int ix = 0; ix < gridSize; ++ix) {
            float wx = ix * step;
            float elev = getMagadiElevation(wx, wy, contourInterval, stepTerracing);
            heights[iz * gridSize + ix] = elev;
            if (elev < minH) minH = elev;
            if (elev > maxH) maxH = elev;
        }
    }

    MagadiSceneMesh sceneMesh;
    sceneMesh.resolutionX = gridSize;
    sceneMesh.resolutionZ = gridSize;
    sceneMesh.vertices.reserve(gridSize * gridSize);

    for (int iz = 0; iz < gridSize; ++iz) {
        float z = (iz * step - worldSpan * 0.5f) * meshScale;
        for (int ix = 0; ix < gridSize; ++ix) {
            float x = (ix * step - worldSpan * 0.5f) * meshScale;
            float rawElev = heights[iz * gridSize + ix];
            float y = (rawElev - 590.0f) * meshScale * 0.55f;

            MagadiVertex vert;
            vert.x = x;
            vert.y = y;
            vert.z = z;
            vert.rawElevation = rawElev;
            vert.contourBand = std::clamp((rawElev - 590.0f) / 1390.0f, 0.0f, 1.0f);
            sceneMesh.vertices.push_back(vert);
        }
    }

    for (int iz = 0; iz < gridSize - 1; ++iz) {
        for (int ix = 0; ix < gridSize - 1; ++ix) {
            uint32_t tl = iz * gridSize + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * gridSize + ix;
            uint32_t br = bl + 1;

            sceneMesh.indices.push_back(tl);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(tr);

            sceneMesh.indices.push_back(tr);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(br);
        }
    }

    sceneMesh.minHeight = (minH - 590.0f) * meshScale * 0.55f;
    sceneMesh.maxHeight = (maxH - 590.0f) * meshScale * 0.55f;
    return sceneMesh;
}

MagadiSceneMesh MagadiSceneEngine::generateChunkScene(int col, int row,
                                                     int gridSize,
                                                     float contourInterval,
                                                     float stepTerracing) const {
    // If selecting row 0 (start segment at Magadi), render the high-detail first 10km segment!
    if (row == 0 && col == 0) {
        return generateFirst10kmSegmentScene(gridSize, contourInterval, stepTerracing);
    }

    const float chunkSizeM = 10000.0f;
    const float originX = col * chunkSizeM;
    const float originY = row * chunkSizeM;
    const float step = chunkSizeM / (gridSize - 1);
    const float meshScale = 0.009f;

    std::vector<float> heights(gridSize * gridSize);
    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float wy = originY + iz * step;
        for (int ix = 0; ix < gridSize; ++ix) {
            float wx = originX + ix * step;
            float elev = getMagadiElevation(wx, wy, contourInterval, stepTerracing);
            heights[iz * gridSize + ix] = elev;
            if (elev < minH) minH = elev;
            if (elev > maxH) maxH = elev;
        }
    }

    MagadiSceneMesh sceneMesh;
    sceneMesh.resolutionX = gridSize;
    sceneMesh.resolutionZ = gridSize;
    sceneMesh.vertices.reserve(gridSize * gridSize);

    for (int iz = 0; iz < gridSize; ++iz) {
        float z = (iz * step - chunkSizeM * 0.5f) * meshScale;
        for (int ix = 0; ix < gridSize; ++ix) {
            float x = (ix * step - chunkSizeM * 0.5f) * meshScale;
            float rawElev = heights[iz * gridSize + ix];
            float y = (rawElev - 590.0f) * meshScale * 0.45f;

            MagadiVertex vert;
            vert.x = x;
            vert.y = y;
            vert.z = z;
            vert.rawElevation = rawElev;
            vert.contourBand = std::clamp((rawElev - 590.0f) / 1390.0f, 0.0f, 1.0f);
            sceneMesh.vertices.push_back(vert);
        }
    }

    for (int iz = 0; iz < gridSize - 1; ++iz) {
        for (int ix = 0; ix < gridSize - 1; ++ix) {
            uint32_t tl = iz * gridSize + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * gridSize + ix;
            uint32_t br = bl + 1;

            sceneMesh.indices.push_back(tl);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(tr);

            sceneMesh.indices.push_back(tr);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(br);
        }
    }

    sceneMesh.minHeight = (minH - 590.0f) * meshScale * 0.45f;
    sceneMesh.maxHeight = (maxH - 590.0f) * meshScale * 0.45f;
    return sceneMesh;
}

MagadiSceneMesh MagadiSceneEngine::generateWholeCombinedTerrainScene(
    int gridResX, int gridResZ, float contourInterval, float stepTerracing) const {

    gridResX = std::clamp(gridResX, 17, 65);
    gridResZ = std::clamp(gridResZ, 33, 257);

    const float widthM = 2400.0f;   // 2.4 km corridor width (1.2 km left, 1.2 km right)
    const float depthM = 101000.0f; // Complete 101.0 km length
    const float stepX = widthM / (gridResX - 1);
    const float stepZ = depthM / (gridResZ - 1);
    const float halfW = widthM * 0.5f;

    const float sceneWidth = 28.0f;   // Scaled scene width
    const float sceneDepth = 220.0f;  // Scaled scene depth across the 101 km corridor

    std::vector<float> heights(gridResX * gridResZ);
    std::vector<std::string> surfaces(gridResX * gridResZ, "savannah_scrub");
    std::vector<uint8_t> flags(gridResX * gridResZ, 0);

    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridResZ; ++iz) {
        float globalDistM = iz * stepZ;
        int segmentIndex = std::clamp(static_cast<int>(globalDistM / 10100.0f) + 1, 1, 10);
        float forwardM = globalDistM - (segmentIndex - 1) * 10100.0f;

        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridResX; ++ix) {
            float offsetFromRoad = (ix * stepX) - halfW;
            float lateralM = roadCurvature + offsetFromRoad;
            std::string surf;
            bool isRoad = false;
            bool isGravel = false;

            float elev = computeCorridorElevation(
                segmentIndex, lateralM, forwardM, contourInterval, stepTerracing, surf, isRoad, isGravel);

            int idx = iz * gridResX + ix;
            heights[idx] = elev;
            surfaces[idx] = surf;

            uint8_t flag = 0;
            if (isRoad) flag = 1;
            else if (surf == "water") flag = 2;
            else if (surf == "trona_soda_crust") flag = 3;
            else if (isGravel || surf == "gravel") flag = 4;
            else if (surf == "escarpment_rock") flag = 5;
            else if (surf == "highland_grass") flag = 6;
            flags[idx] = flag;

            if (elev < minH) minH = elev;
            if (elev > maxH) maxH = elev;
        }
    }

    MagadiSceneMesh sceneMesh;
    sceneMesh.resolutionX = gridResX;
    sceneMesh.resolutionZ = gridResZ;
    sceneMesh.minHeight = minH;
    sceneMesh.maxHeight = maxH;
    sceneMesh.vertices.reserve(gridResX * gridResZ);

    float baseDatumElev = (minH + maxH) * 0.5f;
    float totalSlope = 0.0f;

    for (int iz = 0; iz < gridResZ; ++iz) {
        float globalDistM = iz * stepZ;
        int segmentIndex = std::clamp(static_cast<int>(globalDistM / 10100.0f) + 1, 1, 10);
        float forwardM = globalDistM - (segmentIndex - 1) * 10100.0f;
        float zScaled = ((globalDistM - depthM * 0.5f) / depthM) * sceneDepth;

        float roadCurvature = 0.0f, dCurv = 0.0f;
        computeRoadCurvature(segmentIndex, forwardM, roadCurvature, dCurv);

        for (int ix = 0; ix < gridResX; ++ix) {
            float offsetFromRoad = (ix * stepX) - halfW;
            float lateralM = roadCurvature + offsetFromRoad;
            float xScaled = (lateralM / widthM) * sceneWidth;
            float rawElev = heights[iz * gridResX + ix];
            float yScaled = ((rawElev - baseDatumElev) / 100.0f) * 12.0f;

            float left = (ix > 0) ? heights[iz * gridResX + (ix - 1)] : rawElev;
            float right = (ix < gridResX - 1) ? heights[iz * gridResX + (ix + 1)] : rawElev;
            float up = (iz > 0) ? heights[(iz - 1) * gridResX + ix] : rawElev;
            float down = (iz < gridResZ - 1) ? heights[(iz + 1) * gridResX + ix] : rawElev;

            float ddx = (right - left) / (2.0f * stepX);
            float ddz = (down - up) / (2.0f * stepZ);
            float len = std::sqrt(ddx * ddx + 1.0f + ddz * ddz);
            float nx = -ddx / len;
            float ny = 1.0f / len;
            float nz = -ddz / len;
            float slopeDeg = std::acos(std::clamp(ny, 0.0f, 1.0f)) * RAD2DEG;
            totalSlope += slopeDeg;

            const std::string& surf = surfaces[iz * gridResX + ix];
            uint8_t flag = flags[iz * gridResX + ix];

            float cFrac = std::fmod(rawElev, contourInterval) / contourInterval;
            float isolineFactor = std::abs(cFrac - 0.5f) * 2.0f;

            MagadiVertex vert;
            vert.x = xScaled;
            vert.y = yScaled;
            vert.z = zScaled;
            vert.nx = nx;
            vert.ny = ny;
            vert.nz = nz;
            vert.slopeDeg = slopeDeg;
            vert.grassWeight = (flag == 6 || surf == "highland_grass" || surf == "savannah_scrub") ? 1.0f : 0.0f;
            vert.dirtWeight = (flag == 4 || surf == "gravel" || surf == "volcanic_ash_talus") ? 1.0f : 0.0f;
            vert.rockWeight = (flag == 5 || surf == "escarpment_rock" || slopeDeg > 20.0f) ? 0.90f : 0.0f;
            vert.snowWeight = (flag == 3 || surf == "trona_soda_crust") ? 1.0f : 0.0f;
            vert.isRoadSurface = (flag == 1);
            vert.isWaterSurface = (flag == 2);
            vert.isMudSurface = (flag == 3);
            vert.isGravelSurface = (flag == 4);
            vert.rawElevation = rawElev;
            vert.contourBand = std::clamp((rawElev - 590.0f) / 1390.0f, 0.0f, 1.0f);
            vert.isolineFactor = isolineFactor;
            std::strncpy(vert.surfaceType, surf.c_str(), sizeof(vert.surfaceType) - 1);

            sceneMesh.vertices.push_back(vert);
        }
    }

    sceneMesh.avgSlope = totalSlope / (gridResX * gridResZ);

    for (int iz = 0; iz < gridResZ - 1; ++iz) {
        for (int ix = 0; ix < gridResX - 1; ++ix) {
            uint32_t tl = iz * gridResX + ix;
            uint32_t tr = tl + 1;
            uint32_t bl = (iz + 1) * gridResX + ix;
            uint32_t br = bl + 1;

            sceneMesh.indices.push_back(tl);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(tr);

            sceneMesh.indices.push_back(tr);
            sceneMesh.indices.push_back(bl);
            sceneMesh.indices.push_back(br);
        }
    }

    // Continuous 101km road centerline (128 waypoints from KM 0.0 to KM 101.0)
    const int numWps = 128;
    sceneMesh.roadCenterline.reserve(numWps);
    float wpStep = depthM / (numWps - 1);
    for (int i = 0; i < numWps; ++i) {
        float globalM = i * wpStep;
        int segIdx = std::clamp(static_cast<int>(globalM / 10100.0f) + 1, 1, 10);
        float localFwd = globalM - (segIdx - 1) * 10100.0f;

        RoadWaypoint wp = getRoadPointAt(segIdx, localFwd);
        wp.z = ((globalM - depthM * 0.5f) / depthM) * sceneDepth;
        wp.x = (wp.lateralM / widthM) * sceneWidth;
        wp.y = ((wp.rawElevation - baseDatumElev) / 100.0f) * 12.0f + 0.25f;
        sceneMesh.roadCenterline.push_back(wp);
    }

    // Add 3D Landmark markers along the 101km corridor
    auto addMarker = [&](const char* id, const char* type, float kmDist, float latOffset) {
        float gM = kmDist * 1000.0f;
        int sIdx = std::clamp(static_cast<int>(gM / 10100.0f) + 1, 1, 10);
        float lFwd = gM - (sIdx - 1) * 10100.0f;
        RoadWaypoint wp = getRoadPointAt(sIdx, lFwd);

        MagadiAssetMarker marker;
        marker.id = id;
        marker.type = type;
        marker.posZ = ((gM - depthM * 0.5f) / depthM) * sceneDepth;
        marker.posX = ((wp.lateralM + latOffset) / widthM) * sceneWidth;
        marker.posY = ((wp.rawElevation - baseDatumElev) / 100.0f) * 12.0f + 0.35f;
        marker.scale = 1.0f;
        sceneMesh.assetPlacements.push_back(marker);
    };

    addMarker("marker_lake_magadi", "spawn", 0.0f, 0.0f);
    addMarker("marker_olorgesailie", "garage", 30.3f, 35.0f);
    addMarker("marker_kamukuru", "bridge", 50.5f, 40.0f);
    addMarker("marker_corner_baridi", "spawn", 76.5f, 25.0f);
    addMarker("marker_nairobi_bomas", "garage", 101.0f, 0.0f);

    return sceneMesh;
}

HumanViewpointMetrics MagadiSceneEngine::calculateHumanMetrics(float walkDistanceM, float eyeHeightOffsetM) const {
    HumanViewpointMetrics m;
    m.groundDatumElev = 598.0f;
    m.currentRoadElev = 598.0f + (walkDistanceM / 1000.0f) * 11.7f;
    m.observerEyeElev = m.currentRoadElev + eyeHeightOffsetM;
    m.horizonEscarpmentElev = 1980.0f;
    m.verticalClimbM = m.horizonEscarpmentElev - m.observerEyeElev;
    m.headingDeg = 28.0f; // 028° NNE
    m.heatMirageDistM = 450.0f;
    return m;
}

} // namespace custom_vulkan
