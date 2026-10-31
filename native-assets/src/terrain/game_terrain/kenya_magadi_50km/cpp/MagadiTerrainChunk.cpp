// Kenya Magadi Road (C58) 50km x 50km Terrain System - Vulkan Chunk Implementation
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunk.cpp

#include "MagadiTerrainChunk.h"
#include <algorithm>
#include <cmath>

namespace Magadi {

static constexpr float PI_F = 3.14159265358979323846f;
static constexpr float RAD_TO_DEG = 180.0f / PI_F;

// 2D pseudo-random gradient hash for terrain detail
static float Hash2D(int x, int z) {
    int n = x * 374761393 + z * 668265263;
    n = (n ^ (n >> 13)) * 1274126177;
    n = n ^ (n >> 16);
    return static_cast<float>(n & 0x7FFFFFFF) / 2147483647.0f;
}

static float SmoothNoise2D(float x, float z) {
    int ix = static_cast<int>(std::floor(x));
    int iz = static_cast<int>(std::floor(z));
    float fx = x - static_cast<float>(ix);
    float fz = z - static_cast<float>(iz);

    // Quintic polynomial hermite curve: 6t^5 - 15t^4 + 10t^3
    float ux = fx * fx * fx * (fx * (fx * 6.0f - 15.0f) + 10.0f);
    float uz = fz * fz * fz * (fz * (fz * 6.0f - 15.0f) + 10.0f);

    float v00 = Hash2D(ix, iz);
    float v10 = Hash2D(ix + 1, iz);
    float v01 = Hash2D(ix, iz + 1);
    float v11 = Hash2D(ix + 1, iz + 1);

    float nx0 = v00 * (1.0f - ux) + v10 * ux;
    float nx1 = v01 * (1.0f - ux) + v11 * ux;
    return nx0 * (1.0f - uz) + nx1 * uz;
}

MagadiTerrainChunk::MagadiTerrainChunk(int col, int row, float originX, float originZ, float sizeM)
    : m_col(col), m_row(row), m_originX(originX), m_originZ(originZ), m_sizeM(sizeM) {
    // Check if C58 corridor intersects this chunk
    // Road runs diagonally from (~7000, 5000) to (~49000, 49000)
    m_hasRoad = (col == 0 && row == 0) ||
                (col == 1 && row == 0) ||
                (col == 1 && row == 1) ||
                (col == 2 && row == 1) ||
                (col == 2 && row == 2) ||
                (col == 3 && row == 2) ||
                (col == 3 && row == 3) ||
                (col == 3 && row == 4) ||
                (col == 4 && row == 4);
}

// Distance to C58 Road Centerline in local meters & Road profile elevation
float MagadiTerrainChunk::DistanceToRoadCenterline(float worldX, float worldZ, float& outRoadElev) const {
    // 5 Key survey control waypoints along Kenya C58 corridor:
    // W0: Lake Magadi Soda Factory   (X: 7200,  Z: 5100,  Elev: 600m)
    // W1: Olorgesailie Prehistoric   (X: 21500, Z: 23000, Elev: 1040m)
    // W2: Tinga Trading Post         (X: 30000, Z: 32000, Elev: 1260m)
    // W3: Corner Baridi Hairpins     (X: 44000, Z: 44200, Elev: 1620m)
    // W4: Kiserian / Nairobi Summit  (X: 48500, Z: 49200, Elev: 1950m)

    struct Waypoint { float x, z, elev; };
    static const Waypoint waypoints[5] = {
        {  7200.0f,  5100.0f,  600.0f },
        { 21500.0f, 23000.0f, 1040.0f },
        { 30000.0f, 32000.0f, 1260.0f },
        { 44000.0f, 44200.0f, 1620.0f },
        { 48500.0f, 49200.0f, 1950.0f }
    };

    float minDistSq = 1e12f;
    outRoadElev = 600.0f;

    for (int i = 0; i < 4; ++i) {
        float x1 = waypoints[i].x;
        float z1 = waypoints[i].z;
        float x2 = waypoints[i + 1].x;
        float z2 = waypoints[i + 1].z;

        float dx = x2 - x1;
        float dz = z2 - z1;
        float segLenSq = dx * dx + dz * dz;

        float t = 0.0f;
        if (segLenSq > 0.001f) {
            t = ((worldX - x1) * dx + (worldZ - z1) * dz) / segLenSq;
            t = std::max(0.0f, std::min(1.0f, t));
        }

        float projX = x1 + t * dx;
        float projZ = z1 + t * dz;
        float dX = worldX - projX;
        float dZ = worldZ - projZ;
        float distSq = dX * dX + dZ * dZ;

        if (distSq < minDistSq) {
            minDistSq = distSq;
            outRoadElev = waypoints[i].elev * (1.0f - t) + waypoints[i + 1].elev * t;
        }
    }

    return std::sqrt(minDistSq);
}

// Sample contour-defined elevation
float MagadiTerrainChunk::SampleContourElevation(float worldX, float worldZ, const ContourConfig& cfg) const {
    // 1. Regional Elevation Slope: East African Rift Valley floor (590m) to Eastern Escarpment (1980m)
    float u = std::max(0.0f, std::min(1.0f, (worldX + worldZ * 0.95f) / 95000.0f));
    float regionalElev = 590.0f + u * 1390.0f;

    // 2. Rift Valley Fault Scarps: North-South tectonic stepped contour faults
    // Stepped terraces define sharp contour transitions typical of the Gregory Rift
    float fault1 = std::sin(worldX * 0.00035f + 0.8f) * 65.0f;
    float fault2 = std::sin(worldX * 0.00095f + worldZ * 0.00025f) * 35.0f;

    // 3. Olorgesailie volcanic cones and basalt ridges
    float dxVolc = worldX - 21500.0f;
    float dzVolc = worldZ - 23000.0f;
    float distVolc = std::sqrt(dxVolc * dxVolc + dzVolc * dzVolc);
    float volcFeature = 0.0f;
    if (distVolc < 9000.0f) {
        float vt = 1.0f - distVolc / 9000.0f;
        volcFeature = vt * vt * 180.0f;
    }

    // 4. Fine fractal geological noise
    float noise1 = SmoothNoise2D(worldX * 0.00015f, worldZ * 0.00015f) * 45.0f;
    float noise2 = SmoothNoise2D(worldX * 0.00055f, worldZ * 0.00055f) * 15.0f;

    float continuousElev = regionalElev + fault1 + fault2 + volcFeature + noise1 + noise2;

    // 5. Lake Magadi Trona basin clamp (lake bed flat at ~590m)
    if (worldX < 11000.0f && worldZ < 11000.0f) {
        float lakeDist = std::sqrt(worldX * worldX + worldZ * worldZ);
        if (lakeDist < 9500.0f) {
            float lt = std::min(1.0f, lakeDist / 9500.0f);
            continuousElev = 590.0f + (continuousElev - 590.0f) * (lt * lt);
        }
    }

    // 6. Topographic Contour Shaping & Terracing
    // Contours explicitly define elevation steps when terracing is enabled
    float shapedElev = continuousElev;
    if (cfg.stepTerracingFactor > 0.001f) {
        float stepH = cfg.contourInterval;
        float baseStep = std::floor(continuousElev / stepH) * stepH;
        float frac = (continuousElev - baseStep) / stepH;
        // Smoothstep transition at contour boundaries
        float steppedFrac = frac * frac * (3.0f - 2.0f * frac);
        float terracedElev = baseStep + steppedFrac * stepH;
        shapedElev = continuousElev * (1.0f - cfg.stepTerracingFactor) + terracedElev * cfg.stepTerracingFactor;
    }

    // 7. C58 Road Cut & Fill Corridor blending
    if (cfg.enableRoadCutEmbankment) {
        float roadElev = 0.0f;
        float distToRoad = DistanceToRoadCenterline(worldX, worldZ, roadElev);

        // Road crown width: 8m, Embankment zone: 35m
        if (distToRoad < 8.0f) {
            shapedElev = roadElev;
        } else if (distToRoad < 35.0f) {
            float t = (distToRoad - 8.0f) / 27.0f;
            float smoothT = t * t * (3.0f - 2.0f * t);
            shapedElev = roadElev * (1.0f - smoothT) + shapedElev * smoothT;
        }
    }

    return std::max(590.0f, std::min(1980.0f, shapedElev));
}

void MagadiTerrainChunk::CalculateNormal(float worldX, float worldZ, const ContourConfig& cfg, float step, float outNormal[3]) const {
    float left  = SampleContourElevation(worldX - step, worldZ, cfg);
    float right = SampleContourElevation(worldX + step, worldZ, cfg);
    float up    = SampleContourElevation(worldX, worldZ - step, cfg);
    float down  = SampleContourElevation(worldX, worldZ + step, cfg);

    float dx = (right - left) / (2.0f * step);
    float dz = (down - up) / (2.0f * step);
    float len = std::sqrt(dx * dx + 1.0f + dz * dz);

    outNormal[0] = -dx / len;
    outNormal[1] = 1.0f / len;
    outNormal[2] = -dz / len;
}

MagadiSurfaceType MagadiTerrainChunk::DetermineSurfaceType(float worldX, float worldZ, float elev, float distRoad) const {
    if (distRoad < 8.0f) {
        return MagadiSurfaceType::Asphalt;
    }
    if (distRoad < 18.0f) {
        return MagadiSurfaceType::Gravel;
    }
    if (worldX < 11000.0f && worldZ < 11000.0f && elev < 615.0f) {
        return (elev < 598.0f) ? MagadiSurfaceType::TronaSodaCrust : MagadiSurfaceType::AlkalineMud;
    }
    if (elev > 1550.0f) {
        return MagadiSurfaceType::EscarpmentRock;
    }
    float dxVolc = worldX - 21500.0f;
    float dzVolc = worldZ - 23000.0f;
    if (std::sqrt(dxVolc * dxVolc + dzVolc * dzVolc) < 5500.0f) {
        return MagadiSurfaceType::VolcanicAshTalus;
    }
    return MagadiSurfaceType::SavannahScrub;
}

void MagadiTerrainChunk::GenerateMesh(const ContourConfig& contourConfig, ChunkLOD lod) {
    uint32_t resolution = 65; // LOD0 default
    if (lod == ChunkLOD::LOD1_Medium) resolution = 33;
    else if (lod == ChunkLOD::LOD2_Low) resolution = 17;

    uint32_t totalVerts = resolution * resolution;
    m_vertices.clear();
    m_vertices.reserve(totalVerts);

    float step = m_sizeM / static_cast<float>(resolution - 1);
    float halfSize = m_sizeM * 0.5f;

    m_minElevation = 1e6f;
    m_maxElevation = -1e6f;

    // 1. Generate Vertices with Contour Data
    for (uint32_t iz = 0; iz < resolution; ++iz) {
        float localZ = iz * step;
        float worldZ = m_originZ + localZ;

        for (uint32_t ix = 0; ix < resolution; ++ix) {
            float localX = ix * step;
            float worldX = m_originX + localX;

            float rawElev = SampleContourElevation(worldX, worldZ, contourConfig);
            if (rawElev < m_minElevation) m_minElevation = rawElev;
            if (rawElev > m_maxElevation) m_maxElevation = rawElev;

            float normal[3];
            CalculateNormal(worldX, worldZ, contourConfig, step, normal);

            float slopeRad = std::acos(std::max(0.0f, std::min(1.0f, normal[1])));
            float slopeDeg = slopeRad * RAD_TO_DEG;

            float roadElev = 0.0f;
            float distRoad = DistanceToRoadCenterline(worldX, worldZ, roadElev);
            MagadiSurfaceType surf = DetermineSurfaceType(worldX, worldZ, rawElev, distRoad);

            // Contour isoline factor for GPU fragment shader (anti-aliased lines)
            float contourInterval = contourConfig.contourInterval;
            float cFrac = std::fmod(rawElev, contourInterval) / contourInterval;
            float isolineFactor = std::abs(cFrac - 0.5f) * 2.0f;

            // Normalized contour band [0.0 - 1.0] across Magadi elevation range [590m - 1980m]
            float contourBand = (rawElev - 590.0f) / 1390.0f;

            TerrainVertex v{};
            // Position centered around chunk origin in local coordinates
            v.pos[0] = localX - halfSize;
            v.pos[1] = rawElev;
            v.pos[2] = localZ - halfSize;

            v.normal[0] = normal[0];
            v.normal[1] = normal[1];
            v.normal[2] = normal[2];

            v.uv[0] = localX / m_sizeM;
            v.uv[1] = localZ / m_sizeM;

            v.rawElevation = rawElev;
            v.contourBand = contourBand;
            v.isolineFactor = isolineFactor;
            v.slopeDeg = slopeDeg;
            v.surfaceType = static_cast<uint32_t>(surf);

            // Splat weights for PBR shaders
            v.splatWeights[0] = (surf == MagadiSurfaceType::SavannahScrub) ? 1.0f : 0.0f;
            v.splatWeights[1] = (surf == MagadiSurfaceType::EscarpmentRock || slopeDeg > 35.0f) ? 1.0f : 0.0f;
            v.splatWeights[2] = (surf == MagadiSurfaceType::TronaSodaCrust || surf == MagadiSurfaceType::AlkalineMud) ? 1.0f : 0.0f;
            v.splatWeights[3] = (surf == MagadiSurfaceType::Asphalt || surf == MagadiSurfaceType::Gravel) ? 1.0f : 0.0f;

            m_vertices.push_back(v);
        }
    }

    // 2. Generate Triangle Indices for Vulkan Primitive Topology (Triangle List)
    uint32_t quadCount = (resolution - 1) * (resolution - 1);
    m_indices.clear();
    m_indices.reserve(quadCount * 6);

    for (uint32_t iz = 0; iz < resolution - 1; ++iz) {
        for (uint32_t ix = 0; ix < resolution - 1; ++ix) {
            uint32_t topLeft = iz * resolution + ix;
            uint32_t topRight = topLeft + 1;
            uint32_t bottomLeft = (iz + 1) * resolution + ix;
            uint32_t bottomRight = bottomLeft + 1;

            // Triangle 1
            m_indices.push_back(topLeft);
            m_indices.push_back(bottomLeft);
            m_indices.push_back(topRight);

            // Triangle 2
            m_indices.push_back(topRight);
            m_indices.push_back(bottomLeft);
            m_indices.push_back(bottomRight);
        }
    }

    // 3. Update Bounding Box
    m_bounds.minX = -halfSize;
    m_bounds.maxX =  halfSize;
    m_bounds.minY = m_minElevation;
    m_bounds.maxY = m_maxElevation;
    m_bounds.minZ = -halfSize;
    m_bounds.maxZ =  halfSize;
}

} // namespace Magadi
