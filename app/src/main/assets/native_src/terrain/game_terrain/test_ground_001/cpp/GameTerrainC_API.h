#pragma once

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#if defined(_WIN32)
  #define TERRAIN_API __declspec(dllexport)
#else
  #define TERRAIN_API __attribute__((visibility("default")))
#endif

typedef void* TerrainHandle;

// Lifecycle
TERRAIN_API TerrainHandle GameTerrain_Create(void);
TERRAIN_API void GameTerrain_Destroy(TerrainHandle handle);

// Loading & Export
TERRAIN_API bool GameTerrain_LoadJSON(TerrainHandle handle, const char* filepath);
TERRAIN_API bool GameTerrain_LoadBinary(TerrainHandle handle, const char* filepath);
TERRAIN_API bool GameTerrain_SaveBinary(TerrainHandle handle, const char* filepath);

// Queries
TERRAIN_API float GameTerrain_GetElevation(TerrainHandle handle, float worldX, float worldZ);
TERRAIN_API uint8_t GameTerrain_GetSurfaceType(TerrainHandle handle, float worldX, float worldZ);
TERRAIN_API float GameTerrain_GetFriction(TerrainHandle handle, float worldX, float worldZ);
TERRAIN_API void GameTerrain_GetNormal(TerrainHandle handle, float worldX, float worldZ, float* outNormal3);

// Raycasting (Collision & Tire Contact)
TERRAIN_API bool GameTerrain_Raycast(
    TerrainHandle handle,
    float originX, float originY, float originZ,
    float dirX, float dirY, float dirZ,
    float maxDist,
    float* outHitX, float* outHitY, float* outHitZ,
    float* outNormX, float* outNormY, float* outNormZ
);

// Metadata & Dimensions
TERRAIN_API int GameTerrain_GetWidth(TerrainHandle handle);
TERRAIN_API int GameTerrain_GetDepth(TerrainHandle handle);
TERRAIN_API float GameTerrain_GetMapWidthM(TerrainHandle handle);
TERRAIN_API float GameTerrain_GetMapDepthM(TerrainHandle handle);
TERRAIN_API float GameTerrain_GetMinElevation(TerrainHandle handle);
TERRAIN_API float GameTerrain_GetMaxElevation(TerrainHandle handle);

// Asset Placement API
TERRAIN_API int GameTerrain_GetAssetCount(TerrainHandle handle);
TERRAIN_API bool GameTerrain_GetAssetInfo(
    TerrainHandle handle,
    int index,
    char* outId, int maxIdLen,
    char* outType, int maxTypeLen,
    char* outGlbPath, int maxGlbLen,
    float* outPos3,
    float* outRot3,
    float* outScale3
);

#ifdef __cplusplus
}
#endif
