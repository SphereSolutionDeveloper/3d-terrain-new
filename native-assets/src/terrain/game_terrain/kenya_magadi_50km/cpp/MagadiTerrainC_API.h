// Kenya Magadi Road (C58) 50km x 50km Terrain System - C ABI
// Path: native-assets/src/terrain/game_terrain/kenya_magadi_50km/cpp/MagadiTerrainC_API.h
#pragma once
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef MAGADI_API
#define MAGADI_API
#endif

typedef void* MagadiManagerHandle;

// Lifecycle
MAGADI_API MagadiManagerHandle Magadi_Create(void);
MAGADI_API void                Magadi_Destroy(MagadiManagerHandle handle);
MAGADI_API bool                Magadi_Initialize(MagadiManagerHandle handle, const char* manifestPath);

// Contour Configuration
MAGADI_API void  Magadi_SetContourConfig(MagadiManagerHandle handle, float contourInterval, float indexContourInterval, float stepTerracingFactor);

// Elevation & Physics Queries
MAGADI_API float Magadi_GetElevationAt(MagadiManagerHandle handle, float worldX, float worldZ);
MAGADI_API float Magadi_GetFrictionAt(MagadiManagerHandle handle, float worldX, float worldZ);
MAGADI_API int   Magadi_GetSurfaceAt(MagadiManagerHandle handle, float worldX, float worldZ);

// Transforms
MAGADI_API void  Magadi_LocalMetersToWGS84(MagadiManagerHandle handle, float worldX, float worldZ, double* outLat, double* outLon);
MAGADI_API void  Magadi_WGS84ToLocalMeters(MagadiManagerHandle handle, double lat, double lon, float* outWorldX, float* outWorldZ);
MAGADI_API void  Magadi_UpdateActiveArea(MagadiManagerHandle handle, float vehicleX, float vehicleZ, int radiusChunks);

// Chunk Mesh Query (for Vulkan vertex/index buffer upload)
MAGADI_API uint32_t Magadi_GetChunkVertexCount(MagadiManagerHandle handle, int col, int row);
MAGADI_API uint32_t Magadi_GetChunkIndexCount(MagadiManagerHandle handle, int col, int row);
MAGADI_API void     Magadi_GetChunkBounds(MagadiManagerHandle handle, int col, int row, float* outMinX, float* outMinY, float* outMinZ, float* outMaxX, float* outMaxY, float* outMaxZ);

#ifdef __cplusplus
}
#endif
