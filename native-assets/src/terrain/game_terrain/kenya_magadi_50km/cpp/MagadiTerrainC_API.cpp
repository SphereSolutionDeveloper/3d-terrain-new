// Kenya Magadi Road (C58) 50km x 50km Terrain System - C ABI Implementation
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainC_API.cpp

#include "MagadiTerrainC_API.h"
#include "MagadiTerrainChunkManager.h"

using namespace Magadi;

MagadiManagerHandle Magadi_Create(void) {
    return new (std::nothrow) MagadiTerrainChunkManager();
}

void Magadi_Destroy(MagadiManagerHandle handle) {
    if (handle) {
        delete static_cast<MagadiTerrainChunkManager*>(handle);
    }
}

bool Magadi_Initialize(MagadiManagerHandle handle, const char* manifestPath) {
    if (!handle) return false;
    auto* mgr = static_cast<MagadiTerrainChunkManager*>(handle);
    return mgr->Initialize(manifestPath ? manifestPath : "");
}

void Magadi_SetContourConfig(MagadiManagerHandle handle, float contourInterval, float indexContourInterval, float stepTerracingFactor) {
    if (!handle) return;
    auto* mgr = static_cast<MagadiTerrainChunkManager*>(handle);
    ContourConfig cfg = mgr->GetContourConfig();
    cfg.contourInterval = contourInterval;
    cfg.indexContourInterval = indexContourInterval;
    cfg.stepTerracingFactor = stepTerracingFactor;
    mgr->SetContourConfig(cfg);
}

float Magadi_GetElevationAt(MagadiManagerHandle handle, float worldX, float worldZ) {
    if (!handle) return 590.0f;
    return static_cast<MagadiTerrainChunkManager*>(handle)->GetElevationAt(worldX, worldZ);
}

float Magadi_GetFrictionAt(MagadiManagerHandle handle, float worldX, float worldZ) {
    if (!handle) return 0.75f;
    return static_cast<MagadiTerrainChunkManager*>(handle)->GetFrictionAt(worldX, worldZ);
}

int Magadi_GetSurfaceAt(MagadiManagerHandle handle, float worldX, float worldZ) {
    if (!handle) return 0;
    return static_cast<int>(static_cast<MagadiTerrainChunkManager*>(handle)->GetSurfaceAt(worldX, worldZ));
}

void Magadi_LocalMetersToWGS84(MagadiManagerHandle handle, float worldX, float worldZ, double* outLat, double* outLon) {
    if (!handle || !outLat || !outLon) return;
    static_cast<MagadiTerrainChunkManager*>(handle)->LocalMetersToWGS84(worldX, worldZ, *outLat, *outLon);
}

void Magadi_WGS84ToLocalMeters(MagadiManagerHandle handle, double lat, double lon, float* outWorldX, float* outWorldZ) {
    if (!handle || !outWorldX || !outWorldZ) return;
    static_cast<MagadiTerrainChunkManager*>(handle)->WGS84ToLocalMeters(lat, lon, *outWorldX, *outWorldZ);
}

void Magadi_UpdateActiveArea(MagadiManagerHandle handle, float vehicleX, float vehicleZ, int radiusChunks) {
    if (!handle) return;
    static_cast<MagadiTerrainChunkManager*>(handle)->UpdateActiveArea(vehicleX, vehicleZ, radiusChunks);
}

uint32_t Magadi_GetChunkVertexCount(MagadiManagerHandle handle, int col, int row) {
    if (!handle) return 0;
    auto* chunk = static_cast<MagadiTerrainChunkManager*>(handle)->GetChunk(col, row);
    return chunk ? chunk->GetVertexCount() : 0;
}

uint32_t Magadi_GetChunkIndexCount(MagadiManagerHandle handle, int col, int row) {
    if (!handle) return 0;
    auto* chunk = static_cast<MagadiTerrainChunkManager*>(handle)->GetChunk(col, row);
    return chunk ? chunk->GetIndexCount() : 0;
}

void Magadi_GetChunkBounds(MagadiManagerHandle handle, int col, int row, float* outMinX, float* outMinY, float* outMinZ, float* outMaxX, float* outMaxY, float* outMaxZ) {
    if (!handle) return;
    auto* chunk = static_cast<MagadiTerrainChunkManager*>(handle)->GetChunk(col, row);
    if (chunk) {
        const auto& b = chunk->GetBoundingBox();
        if (outMinX) *outMinX = b.minX;
        if (outMinY) *outMinY = b.minY;
        if (outMinZ) *outMinZ = b.minZ;
        if (outMaxX) *outMaxX = b.maxX;
        if (outMaxY) *outMaxY = b.maxY;
        if (outMaxZ) *outMaxZ = b.maxZ;
    }
}
