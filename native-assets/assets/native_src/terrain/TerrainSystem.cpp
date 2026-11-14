// Procedural Vulkan Asset Suite
// Path: src/terrain/TerrainSystem.cpp

#include "TerrainSystem.h"
#include <cmath>
#include <algorithm>

namespace Terrain {

TerrainSystem::TerrainSystem()
    : m_config() {
    initialize(m_config);
}

TerrainSystem::TerrainSystem(const TerrainConfig& config)
    : m_config(config) {
    initialize(m_config);
}

TerrainSystem::~TerrainSystem() = default;

void TerrainSystem::initialize(const TerrainConfig& config) {
    m_config = config;

    // Configure Heightmap Generator
    HeightmapParams hParams{};
    hParams.seed = config.worldSeed;
    hParams.maxElevation = config.maxElevation;
    hParams.baseFrequency = 1.0f / config.worldSizeMeters * 2.0f;
    hParams.octaves = 8;
    hParams.lacunarity = 2.0f;
    hParams.persistence = 0.5f;
    m_heightmapGen.configure(hParams);

    // Configure Splatmap Generator
    SplatParams sParams{};
    sParams.seed = config.worldSeed + 101;
    sParams.cliffSlopeThreshold = 0.65f; // steepness for rock/cliff
    sParams.snowHeightStart = config.maxElevation * 0.75f;
    m_splatGenerator.configure(sParams);

    // Configure Cliff Generator
    CliffParams cParams{};
    cParams.seed = config.worldSeed + 202;
    cParams.maxHeight = config.maxElevation;
    m_cliffGen.configure(cParams);

    // Configure Road Cut
    RoadCutParams rParams{};
    rParams.blendDistance = 12.0f;
    rParams.maxCutSlope = 1.2f;
    m_roadCut.configure(rParams);

    // Configure Cave Volumetrics
    CaveParams cvParams{};
    cvParams.seed = config.worldSeed + 303;
    cvParams.seaLevel = 10.0f;
    cvParams.caveDensityThreshold = 0.0f;
    m_caveGen.configure(cvParams);

    // Configure Tile & LOD managers
    m_tileIndex.initialize(config.worldSizeMeters, config.baseTileScale);
    m_lodManager.initialize(5, config.baseTileScale); // 5 LOD levels

    m_initialized = true;
}

void TerrainSystem::update(float cameraX, float cameraY, float cameraZ) {
    m_lodManager.update(glm::vec3(cameraX, cameraY, cameraZ));
}

float TerrainSystem::getHeightAt(float worldX, float worldZ) const {
    float h = m_heightmapGen.sampleHeight(worldX, worldZ);
    if (m_config.enableRoadCuts) {
        h = m_roadCut.applyEmbankment(worldX, worldZ, h);
    }
    return h;
}

glm::vec3 TerrainSystem::getNormalAt(float worldX, float worldZ) const {
    const float eps = 0.5f;
    float hL = getHeightAt(worldX - eps, worldZ);
    float hR = getHeightAt(worldX + eps, worldZ);
    float hD = getHeightAt(worldX, worldZ - eps);
    float hU = getHeightAt(worldX, worldZ + eps);

    glm::vec3 normal(hL - hR, 2.0f * eps, hD - hU);
    return glm::normalize(normal);
}

glm::vec4 TerrainSystem::getSplatWeightsAt(float worldX, float worldZ) const {
    float height = getHeightAt(worldX, worldZ);
    glm::vec3 normal = getNormalAt(worldX, worldZ);
    return m_splatGenerator.computeWeights(worldX, worldZ, height, normal);
}

ProceduralTerrainMesh TerrainSystem::generateTileMesh(const TileCoord& coord, uint32_t lodLevel) {
    ProceduralTerrainMesh mesh;
    uint32_t step = 1 << lodLevel;
    uint32_t res = (m_config.tileResolution - 1) / step + 1;
    float tileSize = m_config.baseTileScale * (1 << lodLevel);
    float worldOriginX = coord.x * m_config.baseTileScale;
    float worldOriginZ = coord.y * m_config.baseTileScale;
    float dx = tileSize / static_cast<float>(res - 1);
    float dz = tileSize / static_cast<float>(res - 1);

    mesh.vertices.reserve(res * res);
    mesh.indices.reserve((res - 1) * (res - 1) * 6);

    for (uint32_t iz = 0; iz < res; ++iz) {
        for (uint32_t ix = 0; ix < res; ++ix) {
            float wx = worldOriginX + ix * dx;
            float wz = worldOriginZ + iz * dz;
            float wy = getHeightAt(wx, wz);
            glm::vec3 norm = getNormalAt(wx, wz);
            glm::vec4 splat = getSplatWeightsAt(wx, wz);

            TerrainVertex v{};
            v.position = glm::vec3(wx, wy, wz);
            v.normal = norm;
            v.uv = glm::vec2(static_cast<float>(ix) / (res - 1), static_cast<float>(iz) / (res - 1));
            v.splatWeights = splat;
            v.tangent = glm::vec4(1.0f, 0.0f, 0.0f, 1.0f);
            v.elevation = wy;

            mesh.vertices.push_back(v);
        }
    }

    for (uint32_t iz = 0; iz < res - 1; ++iz) {
        for (uint32_t ix = 0; ix < res - 1; ++ix) {
            uint32_t topLeft = iz * res + ix;
            uint32_t topRight = topLeft + 1;
            uint32_t bottomLeft = (iz + 1) * res + ix;
            uint32_t bottomRight = bottomLeft + 1;

            mesh.indices.push_back(topLeft);
            mesh.indices.push_back(bottomLeft);
            mesh.indices.push_back(topRight);

            mesh.indices.push_back(topRight);
            mesh.indices.push_back(bottomLeft);
            mesh.indices.push_back(bottomRight);
        }
    }

    mesh.aabbMin = glm::vec3(worldOriginX, 0.0f, worldOriginZ);
    mesh.aabbMax = glm::vec3(worldOriginX + tileSize, m_config.maxElevation, worldOriginZ + tileSize);
    return mesh;
}

void TerrainSystem::bakeSplatmapToBuffer(const TileCoord& coord, std::vector<uint8_t>& outRgbaBuffer, uint32_t resolution) {
    outRgbaBuffer.resize(resolution * resolution * 4);
    float tileSize = m_config.baseTileScale;
    float originX = coord.x * tileSize;
    float originZ = coord.y * tileSize;

    for (uint32_t y = 0; y < resolution; ++y) {
        for (uint32_t x = 0; x < resolution; ++x) {
            float wx = originX + (static_cast<float>(x) / (resolution - 1)) * tileSize;
            float wz = originZ + (static_cast<float>(y) / (resolution - 1)) * tileSize;
            glm::vec4 weights = getSplatWeightsAt(wx, wz);

            uint32_t pixelIdx = (y * resolution + x) * 4;
            outRgbaBuffer[pixelIdx + 0] = static_cast<uint8_t>(std::clamp(weights.x * 255.0f, 0.0f, 255.0f));
            outRgbaBuffer[pixelIdx + 1] = static_cast<uint8_t>(std::clamp(weights.y * 255.0f, 0.0f, 255.0f));
            outRgbaBuffer[pixelIdx + 2] = static_cast<uint8_t>(std::clamp(weights.z * 255.0f, 0.0f, 255.0f));
            outRgbaBuffer[pixelIdx + 3] = static_cast<uint8_t>(std::clamp(weights.w * 255.0f, 0.0f, 255.0f));
        }
    }
}

} // namespace Terrain
