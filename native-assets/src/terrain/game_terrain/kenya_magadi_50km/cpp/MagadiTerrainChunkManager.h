// Kenya Magadi Road (C58) 50km x 50km Terrain System
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainChunkManager.h
#pragma once
#include <string>
#include <vector>
#include <unordered_map>
#include <memory>

namespace Magadi {
enum class MagadiSurfaceType : uint8_t {
    SavannahScrub = 0, Asphalt = 1, Gravel = 2,
    TronaSodaCrust = 3, AlkalineMud = 4,
    EscarpmentRock = 5, VolcanicAshTalus = 6
};

class MagadiTerrainChunkManager {
public:
    bool Initialize(const std::string& manifestPath);
    float GetElevationAt(float worldX, float worldZ);
    float GetFrictionAt(float worldX, float worldZ);
    MagadiSurfaceType GetSurfaceAt(float worldX, float worldZ);

    void LocalMetersToWGS84(float worldX, float worldZ, double& outLat, double& outLon) const;
    void WGS84ToLocalMeters(double lat, double lon, float& outWorldX, float& outWorldZ) const;

    void UpdateActiveArea(float vehicleX, float vehicleZ, int streamingRadiusChunks = 1);
};
}
