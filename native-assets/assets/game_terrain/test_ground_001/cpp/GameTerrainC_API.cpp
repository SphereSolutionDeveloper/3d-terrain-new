#include "GameTerrainC_API.h"
#include "GameTerrainLoader.h"
#include <cstring>

using namespace GameEngine::Terrain;

TerrainHandle GameTerrain_Create(void) {
    return new GameTerrain();
}

void GameTerrain_Destroy(TerrainHandle handle) {
    if (handle) {
        delete static_cast<GameTerrain*>(handle);
    }
}

bool GameTerrain_LoadJSON(TerrainHandle handle, const char* filepath) {
    if (!handle || !filepath) return false;
    return static_cast<GameTerrain*>(handle)->LoadFromJSON(filepath);
}

bool GameTerrain_LoadBinary(TerrainHandle handle, const char* filepath) {
    if (!handle || !filepath) return false;
    return static_cast<GameTerrain*>(handle)->LoadFromBinary(filepath);
}

bool GameTerrain_SaveBinary(TerrainHandle handle, const char* filepath) {
    if (!handle || !filepath) return false;
    return static_cast<GameTerrain*>(handle)->SaveToBinary(filepath);
}

float GameTerrain_GetElevation(TerrainHandle handle, float worldX, float worldZ) {
    if (!handle) return 0.0f;
    return static_cast<GameTerrain*>(handle)->GetElevationAt(worldX, worldZ);
}

uint8_t GameTerrain_GetSurfaceType(TerrainHandle handle, float worldX, float worldZ) {
    if (!handle) return 0;
    return static_cast<uint8_t>(static_cast<GameTerrain*>(handle)->GetSurfaceTypeAt(worldX, worldZ));
}

float GameTerrain_GetFriction(TerrainHandle handle, float worldX, float worldZ) {
    if (!handle) return 0.85f;
    return static_cast<GameTerrain*>(handle)->GetFrictionAt(worldX, worldZ);
}

void GameTerrain_GetNormal(TerrainHandle handle, float worldX, float worldZ, float* outNormal3) {
    if (!handle || !outNormal3) return;
    Vec3 n = static_cast<GameTerrain*>(handle)->GetNormalAt(worldX, worldZ);
    outNormal3[0] = n.x;
    outNormal3[1] = n.y;
    outNormal3[2] = n.z;
}

bool GameTerrain_Raycast(
    TerrainHandle handle,
    float originX, float originY, float originZ,
    float dirX, float dirY, float dirZ,
    float maxDist,
    float* outHitX, float* outHitY, float* outHitZ,
    float* outNormX, float* outNormY, float* outNormZ
) {
    if (!handle) return false;
    Vec3 hitPt, hitNorm;
    bool hit = static_cast<GameTerrain*>(handle)->Raycast(
        Vec3(originX, originY, originZ),
        Vec3(dirX, dirY, dirZ),
        maxDist,
        hitPt, hitNorm
    );
    if (hit) {
        if (outHitX) *outHitX = hitPt.x;
        if (outHitY) *outHitY = hitPt.y;
        if (outHitZ) *outHitZ = hitPt.z;
        if (outNormX) *outNormX = hitNorm.x;
        if (outNormY) *outNormY = hitNorm.y;
        if (outNormZ) *outNormZ = hitNorm.z;
    }
    return hit;
}

int GameTerrain_GetWidth(TerrainHandle handle) {
    if (!handle) return 0;
    return static_cast<GameTerrain*>(handle)->GetGridWidth();
}

int GameTerrain_GetDepth(TerrainHandle handle) {
    if (!handle) return 0;
    return static_cast<GameTerrain*>(handle)->GetGridDepth();
}

float GameTerrain_GetMapWidthM(TerrainHandle handle) {
    if (!handle) return 0.0f;
    return static_cast<GameTerrain*>(handle)->GetMeta().mapWidthM;
}

float GameTerrain_GetMapDepthM(TerrainHandle handle) {
    if (!handle) return 0.0f;
    return static_cast<GameTerrain*>(handle)->GetMeta().mapDepthM;
}

float GameTerrain_GetMinElevation(TerrainHandle handle) {
    if (!handle) return 0.0f;
    return static_cast<GameTerrain*>(handle)->GetMeta().minElevationM;
}

float GameTerrain_GetMaxElevation(TerrainHandle handle) {
    if (!handle) return 0.0f;
    return static_cast<GameTerrain*>(handle)->GetMeta().maxElevationM;
}

int GameTerrain_GetAssetCount(TerrainHandle handle) {
    if (!handle) return 0;
    return static_cast<int>(static_cast<GameTerrain*>(handle)->GetAssets().size());
}

bool GameTerrain_GetAssetInfo(
    TerrainHandle handle,
    int index,
    char* outId, int maxIdLen,
    char* outType, int maxTypeLen,
    char* outGlbPath, int maxGlbLen,
    float* outPos3,
    float* outRot3,
    float* outScale3
) {
    if (!handle) return false;
    const auto& assets = static_cast<GameTerrain*>(handle)->GetAssets();
    if (index < 0 || static_cast<size_t>(index) >= assets.size()) return false;

    const auto& inst = assets[index];
    if (outId && maxIdLen > 0) {
        std::strncpy(outId, inst.id.c_str(), maxIdLen - 1);
        outId[maxIdLen - 1] = '\0';
    }
    if (outType && maxTypeLen > 0) {
        std::strncpy(outType, inst.type.c_str(), maxTypeLen - 1);
        outType[maxTypeLen - 1] = '\0';
    }
    if (outGlbPath && maxGlbLen > 0) {
        std::strncpy(outGlbPath, inst.glbPath.c_str(), maxGlbLen - 1);
        outGlbPath[maxGlbLen - 1] = '\0';
    }
    if (outPos3) {
        outPos3[0] = inst.position.x;
        outPos3[1] = inst.position.y;
        outPos3[2] = inst.position.z;
    }
    if (outRot3) {
        outRot3[0] = inst.rotation.x;
        outRot3[1] = inst.rotation.y;
        outRot3[2] = inst.rotation.z;
    }
    if (outScale3) {
        outScale3[0] = inst.scale.x;
        outScale3[1] = inst.scale.y;
        outScale3[2] = inst.scale.z;
    }
    return true;
}
