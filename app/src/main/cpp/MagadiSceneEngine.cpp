// ─── app/src/main/cpp/MagadiSceneEngine.cpp ───────────────────────────────────
// Native C++ Magadi Road Elevation & Human Viewpoint Scene Engine
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

float MagadiSceneEngine::getMagadiElevation(float worldXMeters, float worldYMeters,
                                           float contourInterval,
                                           float stepTerracing) const {
    float ny = std::clamp(worldYMeters / 50000.0f, 0.0f, 1.0f);
    float nx = std::clamp(worldXMeters / 50000.0f, 0.0f, 1.0f);

    // Lake Magadi basin at south (598m) to Ngong Hills / Corner Baridi escarpment at north (1980m)
    float baseGrad = 598.0f + (std::pow(ny, 1.42f) * 1382.0f);

    // Rift Valley Trough: depressed valley floor center, elevated escarpment walls east/west
    float troughDist = std::abs(nx - 0.42f);
    float riftWallLift = std::pow(std::clamp(troughDist * 1.8f, 0.0f, 1.0f), 2.2f) * 480.0f;

    // Volcanic Shield Complex (Olorgesailie shield volcano centered at nx=0.55, ny=0.45)
    float dxOlor = nx - 0.55f;
    float dyOlor = ny - 0.45f;
    float distOlorSq = dxOlor * dxOlor + dyOlor * dyOlor;
    float olorVolcano = std::exp(-distOlorSq / 0.018f) * 360.0f;

    // Basalt fault blocks & tectonic horst/graben step-fault terraces
    float faultScarp = std::sin((worldXMeters + worldYMeters * 0.35f) * 0.00065f) * 45.0f;

    // Copernicus COP30 DEM multi-octave FBM fractal ruggedness
    float fbm = fbmNoise(worldXMeters * 0.00015f, worldYMeters * 0.00015f, 4, 2.1f, 0.48f);
    float ruggedness = (fbm - 0.5f) * 95.0f;

    float rawElev = baseGrad + riftWallLift + olorVolcano + faultScarp + ruggedness;

    // Apply Sascha Willems stepped contour terracing if enabled
    if (stepTerracing > 0.001f && contourInterval > 0.5f) {
        float stepped = std::round(rawElev / contourInterval) * contourInterval;
        return rawElev * (1.0f - stepTerracing) + stepped * stepTerracing;
    }

    return rawElev;
}

MagadiSceneMesh MagadiSceneEngine::generateHumanStartScene(float walkDistanceM,
                                                         int gridSize,
                                                         float contourInterval,
                                                         float stepTerracing) const {
    const float widthM = 1200.0f;
    const float depthM = 3200.0f;
    const float stepX = widthM / (gridSize - 1);
    const float stepZ = depthM / (gridSize - 1);
    const float halfW = widthM * 0.5f;

    // Lake Magadi causeway road start origin
    const float originX = 6800.0f;
    const float originY = 4800.0f + walkDistanceM;
    const float meshScale = 0.08f;
    const float baseDatumElev = 598.0f;

    std::vector<float> heights(gridSize * gridSize);
    std::vector<std::string> surfaces(gridSize * gridSize, "savannah_scrub");
    float minH = 1e9f;
    float maxH = -1e9f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float localForward = iz * stepZ;
        float wy = originY + localForward;
        for (int ix = 0; ix < gridSize; ++ix) {
            float localLateral = (ix * stepX) - halfW;
            float wx = originX + localLateral;

            float distToCenterline = std::abs(localLateral);
            // Road gently climbs out of the lake bed: 598m at start, rising +18.5m per km
            float roadGradeElev = baseDatumElev + (localForward / 1000.0f) * 18.5f;
            float naturalElev = getMagadiElevation(wx, wy, contourInterval, stepTerracing);

            float elev;
            if (distToCenterline < 5.0f) {
                // Asphalt roadbed slightly elevated 0.8m above salt crust
                elev = roadGradeElev + 0.8f;
            } else if (distToCenterline < 12.0f) {
                // Gravel verge slope down to salt bed
                float t = (distToCenterline - 5.0f) / 7.0f;
                elev = (roadGradeElev + 0.8f) * (1.0f - t) + naturalElev * t;
            } else {
                elev = naturalElev;
            }

            std::string surf;
            if (distToCenterline < 5.0f) {
                surf = "asphalt";
            } else if (distToCenterline < 12.0f) {
                surf = "gravel";
            } else if (localLateral < -150.0f && elev < 608.0f) {
                surf = "trona_soda_crust"; // Lake Magadi salt pan to the west
            } else if (localLateral < -50.0f && elev < 602.0f) {
                surf = "water"; // Alkaline brine pools
            } else if (localLateral > 250.0f && elev > 640.0f) {
                surf = "escarpment_rock"; // First fault scarp on the east
            } else if (localLateral > 180.0f) {
                surf = "volcanic_ash_talus";
            } else {
                surf = "savannah_scrub";
            }

            int idx = iz * gridSize + ix;
            heights[idx] = elev;
            surfaces[idx] = surf;
            if (elev < minH) minH = elev;
            if (elev > maxH) maxH = elev;
        }
    }

    MagadiSceneMesh sceneMesh;
    sceneMesh.resolutionX = gridSize;
    sceneMesh.resolutionZ = gridSize;
    sceneMesh.vertices.reserve(gridSize * gridSize);
    float totalSlope = 0.0f;

    for (int iz = 0; iz < gridSize; ++iz) {
        float localForward = iz * stepZ;
        float zScaled = (localForward - 400.0f) * meshScale;
        for (int ix = 0; ix < gridSize; ++ix) {
            float localLateral = (ix * stepX) - halfW;
            float xScaled = localLateral * meshScale;
            float rawElev = heights[iz * gridSize + ix];
            float yScaled = (rawElev - baseDatumElev) * meshScale;

            float left = (ix > 0) ? heights[iz * gridSize + (ix - 1)] : rawElev;
            float right = (ix < gridSize - 1) ? heights[iz * gridSize + (ix + 1)] : rawElev;
            float up = (iz > 0) ? heights[(iz - 1) * gridSize + ix] : rawElev;
            float down = (iz < gridSize - 1) ? heights[(iz + 1) * gridSize + ix] : rawElev;

            float dx = (right - left) / (2.0f * stepX);
            float dz = (down - up) / (2.0f * stepZ);
            float len = std::sqrt(dx * dx + 1.0f + dz * dz);
            float nx = -dx / len;
            float ny = 1.0f / len;
            float nz = -dz / len;
            float slopeDeg = std::acos(std::clamp(ny, 0.0f, 1.0f)) * RAD2DEG;
            totalSlope += slopeDeg;

            const std::string& surf = surfaces[iz * gridSize + ix];
            bool isRoad = (surf == "asphalt");
            bool isWater = (surf == "water");
            bool isMud = (surf == "trona_soda_crust");
            bool isGravel = (surf == "gravel");

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
            vert.grassWeight = (surf == "savannah_scrub") ? 1.0f : 0.0f;
            vert.dirtWeight = (surf == "volcanic_ash_talus" || isGravel) ? 1.0f : 0.0f;
            vert.rockWeight = (surf == "escarpment_rock" || slopeDeg > 28.0f) ? 0.85f : 0.0f;
            vert.snowWeight = (surf == "trona_soda_crust") ? 1.0f : 0.0f;
            vert.isRoadSurface = isRoad;
            vert.isWaterSurface = isWater;
            vert.isMudSurface = isMud;
            vert.isGravelSurface = isGravel;
            vert.rawElevation = rawElev;
            vert.contourBand = std::clamp((rawElev - 590.0f) / 1390.0f, 0.0f, 1.0f);
            vert.isolineFactor = isolineFactor;
            std::strncpy(vert.surfaceType, surf.c_str(), sizeof(vert.surfaceType) - 1);

            sceneMesh.vertices.push_back(vert);
        }
    }

    // Indices for triangle grid
    sceneMesh.indices.reserve((gridSize - 1) * (gridSize - 1) * 6);
    for (int iz = 0; iz < gridSize - 1; ++iz) {
        for (int ix = 0; ix < gridSize - 1; ++ix) {
            uint32_t topLeft = iz * gridSize + ix;
            uint32_t topRight = topLeft + 1;
            uint32_t bottomLeft = (iz + 1) * gridSize + ix;
            uint32_t bottomRight = bottomLeft + 1;

            sceneMesh.indices.push_back(topLeft);
            sceneMesh.indices.push_back(bottomLeft);
            sceneMesh.indices.push_back(topRight);

            sceneMesh.indices.push_back(topRight);
            sceneMesh.indices.push_back(bottomLeft);
            sceneMesh.indices.push_back(bottomRight);
        }
    }

    // 3D Point-of-Interest Assets in the immediate Human View
    sceneMesh.assetPlacements = {
        {"c58_start_sign", "spawn", "assets/markers/start_grid.glb", 0.0f, 0.8f * meshScale, (20.0f - 400.0f) * meshScale, 0.0f, 1.0f},
        {"magadi_soda_factory", "garage", "assets/structures/garage_hub.glb", -180.0f * meshScale, 0.0f, (80.0f - 400.0f) * meshScale, 0.0f, 1.2f},
        {"causeway_bridge", "bridge", "assets/structures/concrete_bridge.glb", 0.0f, 0.8f * meshScale, (320.0f - 400.0f) * meshScale, 0.0f, 1.0f},
        {"acacia_clump", "tree", "assets/nature/palm_tree.glb", 18.0f * meshScale, 0.0f, (120.0f - 400.0f) * meshScale, 0.0f, 0.9f},
        {"basalt_boulder", "rock", "assets/nature/boulder_1.5m.glb", -16.0f * meshScale, 0.0f, (240.0f - 400.0f) * meshScale, 0.0f, 1.3f}
    };

    sceneMesh.minHeight = (minH - baseDatumElev) * meshScale;
    sceneMesh.maxHeight = (maxH - baseDatumElev) * meshScale;
    sceneMesh.avgSlope = totalSlope / (gridSize * gridSize);

    return sceneMesh;
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

MagadiSceneMesh MagadiSceneEngine::generateChunkScene(int col, int row,
                                                     int gridSize,
                                                     float contourInterval,
                                                     float stepTerracing) const {
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

HumanViewpointMetrics MagadiSceneEngine::calculateHumanMetrics(float walkDistanceM, float eyeHeightOffsetM) const {
    HumanViewpointMetrics m;
    m.groundDatumElev = 598.0f;
    m.currentRoadElev = 598.0f + (walkDistanceM / 1000.0f) * 18.5f;
    m.observerEyeElev = m.currentRoadElev + eyeHeightOffsetM;
    m.horizonEscarpmentElev = 1980.0f;
    m.verticalClimbM = m.horizonEscarpmentElev - m.observerEyeElev;
    m.headingDeg = 28.0f; // 028° NNE
    m.heatMirageDistM = 450.0f;
    return m;
}

} // namespace custom_vulkan
