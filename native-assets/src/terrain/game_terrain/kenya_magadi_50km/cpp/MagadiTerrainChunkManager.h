// Kenya Magadi Road (C58) 50km x 50km Terrain System - Chunk Manager
// Reference: Sascha Willems Vulkan Infinite Procedural Terrain (SaschaWillems/Vulkan)
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunkManager.h
#pragma once

#include "MagadiTerrainChunk.h"
#include <string>
#include <vector>
#include <memory>

namespace Magadi {

class MagadiTerrainChunkManager {
public:
    MagadiTerrainChunkManager();
    ~MagadiTerrainChunkManager() = default;

    bool Initialize(const std::string& manifestPath = "");
    void SetContourConfig(const ContourConfig& config);
    const ContourConfig& GetContourConfig() const { return m_contourConfig; }

    float GetElevationAt(float worldX, float worldZ);
    float GetFrictionAt(float worldX, float worldZ);
    MagadiSurfaceType GetSurfaceAt(float worldX, float worldZ);

    void LocalMetersToWGS84(float worldX, float worldZ, double& outLat, double& outLon) const;
    void WGS84ToLocalMeters(double lat, double lon, float& outWorldX, float& outWorldZ) const;

    void UpdateActiveArea(float vehicleX, float vehicleZ, int streamingRadiusChunks = 1);

    MagadiTerrainChunk* GetChunk(int col, int row);
    const std::vector<std::unique_ptr<MagadiTerrainChunk>>& GetAllChunks() const { return m_chunks; }
    size_t GetTotalChunkCount() const { return m_chunks.size(); }

private:
    ContourConfig m_contourConfig;
    std::vector<std::unique_ptr<MagadiTerrainChunk>> m_chunks;
    float m_totalSpanM = 50000.0f;
    float m_chunkSpanM = 10000.0f;
    int m_gridDim = 5;
};

} // namespace Magadi
