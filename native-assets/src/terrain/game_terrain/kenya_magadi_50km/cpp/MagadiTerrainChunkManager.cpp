// Kenya Magadi Road (C58) 50km x 50km Terrain System - Chunk Manager Implementation
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunkManager.cpp

#include "MagadiTerrainChunkManager.h"
#include <cmath>
#include <algorithm>

namespace Magadi {

MagadiTerrainChunkManager::MagadiTerrainChunkManager() {
    // Default contour settings for Kenya Magadi Rift Valley
    m_contourConfig.contourInterval = 25.0f;
    m_contourConfig.indexContourInterval = 100.0f;
    m_contourConfig.stepTerracingFactor = 0.0f;
    m_contourConfig.enableRoadCutEmbankment = true;
}

bool MagadiTerrainChunkManager::Initialize(const std::string& /*manifestPath*/) {
    m_chunks.clear();
    m_chunks.reserve(m_gridDim * m_gridDim);

    // Initialize all 25 chunks (5x5 grid of 10km x 10km tiles)
    for (int row = 0; row < m_gridDim; ++row) {
        float originZ = row * m_chunkSpanM;
        for (int col = 0; col < m_gridDim; ++col) {
            float originX = col * m_chunkSpanM;
            auto chunk = std::make_unique<MagadiTerrainChunk>(col, row, originX, originZ, m_chunkSpanM);
            // Pre-generate LOD0 mesh for active simulation
            chunk->GenerateMesh(m_contourConfig, ChunkLOD::LOD0_High);
            m_chunks.push_back(std::move(chunk));
        }
    }
    return true;
}

void MagadiTerrainChunkManager::SetContourConfig(const ContourConfig& config) {
    m_contourConfig = config;
    // Regenerate all chunks with the updated contour parameters
    for (auto& chunk : m_chunks) {
        if (chunk) {
            chunk->GenerateMesh(m_contourConfig, ChunkLOD::LOD0_High);
        }
    }
}

MagadiTerrainChunk* MagadiTerrainChunkManager::GetChunk(int col, int row) {
    if (col < 0 || col >= m_gridDim || row < 0 || row >= m_gridDim) {
        return nullptr;
    }
    size_t index = static_cast<size_t>(row * m_gridDim + col);
    if (index < m_chunks.size()) {
        return m_chunks[index].get();
    }
    return nullptr;
}

float MagadiTerrainChunkManager::GetElevationAt(float worldX, float worldZ) {
    int col = static_cast<int>(std::floor(worldX / m_chunkSpanM));
    int row = static_cast<int>(std::floor(worldZ / m_chunkSpanM));
    col = std::max(0, std::min(m_gridDim - 1, col));
    row = std::max(0, std::min(m_gridDim - 1, row));

    MagadiTerrainChunk* chunk = GetChunk(col, row);
    if (chunk) {
        return chunk->SampleContourElevation(worldX, worldZ, m_contourConfig);
    }

    // Continuous rift valley elevation gradient for coordinates
    float u = std::max(0.0f, std::min(1.0f, (worldX + worldZ) / 100000.0f));
    return 590.0f + u * 1390.0f;
}

MagadiSurfaceType MagadiTerrainChunkManager::GetSurfaceAt(float worldX, float worldZ) {
    int col = static_cast<int>(std::floor(worldX / m_chunkSpanM));
    int row = static_cast<int>(std::floor(worldZ / m_chunkSpanM));
    col = std::max(0, std::min(m_gridDim - 1, col));
    row = std::max(0, std::min(m_gridDim - 1, row));

    MagadiTerrainChunk* chunk = GetChunk(col, row);
    if (chunk) {
        float elev = chunk->SampleContourElevation(worldX, worldZ, m_contourConfig);
        float roadElev = 0.0f;
        float distRoad = chunk->DistanceToRoadCenterline(worldX, worldZ, roadElev);
        return chunk->DetermineSurfaceType(worldX, worldZ, elev, distRoad);
    }
    return MagadiSurfaceType::SavannahScrub;
}

float MagadiTerrainChunkManager::GetFrictionAt(float worldX, float worldZ) {
    MagadiSurfaceType surf = GetSurfaceAt(worldX, worldZ);
    switch (surf) {
        case MagadiSurfaceType::Asphalt:          return 0.88f;
        case MagadiSurfaceType::Gravel:           return 0.62f;
        case MagadiSurfaceType::SavannahScrub:    return 0.75f;
        case MagadiSurfaceType::TronaSodaCrust:   return 0.52f;
        case MagadiSurfaceType::AlkalineMud:      return 0.22f; // Low friction slick mud
        case MagadiSurfaceType::EscarpmentRock:   return 0.82f;
        case MagadiSurfaceType::VolcanicAshTalus: return 0.58f;
        default:                                  return 0.75f;
    }
}

// Convert local metric coordinates [0 .. 50,000m] to WGS84 Geodetic
// Bounds: Lon 36.25°E to 36.70°E, Lat -1.90°S to -1.45°S
void MagadiTerrainChunkManager::LocalMetersToWGS84(float worldX, float worldZ, double& outLat, double& outLon) const {
    double normX = std::max(0.0, std::min(1.0, static_cast<double>(worldX) / m_totalSpanM));
    double normZ = std::max(0.0, std::min(1.0, static_cast<double>(worldZ) / m_totalSpanM));

    outLon = 36.2500 + normX * (36.7000 - 36.2500);
    outLat = -1.9000 + normZ * (-1.4500 - (-1.9000));
}

void MagadiTerrainChunkManager::WGS84ToLocalMeters(double lat, double lon, float& outWorldX, float& outWorldZ) const {
    double normX = (lon - 36.2500) / (36.7000 - 36.2500);
    double normZ = (lat - (-1.9000)) / (-1.4500 - (-1.9000));

    outWorldX = static_cast<float>(std::max(0.0, std::min(1.0, normX)) * m_totalSpanM);
    outWorldZ = static_cast<float>(std::max(0.0, std::min(1.0, normZ)) * m_totalSpanM);
}

void MagadiTerrainChunkManager::UpdateActiveArea(float vehicleX, float vehicleZ, int streamingRadiusChunks) {
    int centerCol = static_cast<int>(std::floor(vehicleX / m_chunkSpanM));
    int centerRow = static_cast<int>(std::floor(vehicleZ / m_chunkSpanM));

    for (int r = 0; r < m_gridDim; ++r) {
        for (int c = 0; c < m_gridDim; ++c) {
            int dist = std::max(std::abs(c - centerCol), std::abs(r - centerRow));
            MagadiTerrainChunk* chunk = GetChunk(c, r);
            if (!chunk) continue;

            if (dist <= streamingRadiusChunks) {
                // High detail LOD0 for nearby driving chunks
                chunk->GenerateMesh(m_contourConfig, ChunkLOD::LOD0_High);
            } else if (dist <= streamingRadiusChunks + 1) {
                // Medium LOD1
                chunk->GenerateMesh(m_contourConfig, ChunkLOD::LOD1_Medium);
            } else {
                // Far LOD2
                chunk->GenerateMesh(m_contourConfig, ChunkLOD::LOD2_Low);
            }
        }
    }
}

} // namespace Magadi
