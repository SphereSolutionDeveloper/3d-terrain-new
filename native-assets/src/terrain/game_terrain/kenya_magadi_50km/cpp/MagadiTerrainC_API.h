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

MAGADI_API MagadiManagerHandle Magadi_Create(void);
MAGADI_API void                Magadi_Destroy(MagadiManagerHandle handle);
MAGADI_API bool                Magadi_Initialize(MagadiManagerHandle handle, const char* manifestPath);
MAGADI_API float               Magadi_GetElevationAt(MagadiManagerHandle handle, float worldX, float worldZ);
MAGADI_API float               Magadi_GetFrictionAt(MagadiManagerHandle handle, float worldX, float worldZ);
MAGADI_API void                Magadi_LocalMetersToWGS84(MagadiManagerHandle handle, float worldX, float worldZ, double* outLat, double* outLon);
MAGADI_API void                Magadi_UpdateActiveArea(MagadiManagerHandle handle, float vehicleX, float vehicleZ, int radiusChunks);

#ifdef __cplusplus
}
#endif
