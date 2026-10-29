#include "GameTerrainLoader.h"
#include <fstream>
#include <sstream>
#include <cmath>
#include <algorithm>
#include <cstring>
#include <iostream>

namespace GameEngine {
namespace Terrain {

// Fast, zero-dependency JSON parser for mobile game runtimes
namespace FastJson {

static void SkipWhitespace(const std::string& str, size_t& pos) {
    while (pos < str.size() && (str[pos] == ' ' || str[pos] == '\t' || str[pos] == '\n' || str[pos] == '\r')) {
        pos++;
    }
}

static std::string ParseString(const std::string& str, size_t& pos) {
    SkipWhitespace(str, pos);
    if (pos >= str.size() || str[pos] != '"') return "";
    pos++; // skip "
    size_t start = pos;
    while (pos < str.size() && str[pos] != '"') {
        if (str[pos] == '\\' && pos + 1 < str.size()) pos += 2;
        else pos++;
    }
    std::string result = str.substr(start, pos - start);
    if (pos < str.size()) pos++; // skip closing "
    return result;
}

static double ParseNumber(const std::string& str, size_t& pos) {
    SkipWhitespace(str, pos);
    size_t start = pos;
    if (pos < str.size() && (str[pos] == '-' || str[pos] == '+')) pos++;
    while (pos < str.size() && ((str[pos] >= '0' && str[pos] <= '9') || str[pos] == '.' || str[pos] == 'e' || str[pos] == 'E')) {
        pos++;
    }
    try {
        return std::stod(str.substr(start, pos - start));
    } catch (...) {
        return 0.0;
    }
}

static size_t FindKey(const std::string& str, const std::string& key, size_t startPos = 0) {
    std::string needle = "\"" + key + "\"";
    return str.find(needle, startPos);
}

} // namespace FastJson

bool GameTerrain::LoadFromJSON(const std::string& filepath) {
    std::ifstream file(filepath);
    if (!file.is_open()) {
        std::cerr << "[GameTerrain] Failed to open JSON file: " << filepath << std::endl;
        return false;
    }
    std::stringstream buffer;
    buffer << file.rdbuf();
    return LoadFromJSONString(buffer.str());
}

bool GameTerrain::LoadFromJSONString(const std::string& json) {
    // 1. Meta parsing
    size_t metaPos = FastJson::FindKey(json, "meta");
    if (metaPos != std::string::npos) {
        size_t mapNamePos = FastJson::FindKey(json, "map_name", metaPos);
        if (mapNamePos != std::string::npos) {
            size_t colon = json.find(':', mapNamePos);
            size_t valPos = colon + 1;
            m_meta.mapName = FastJson::ParseString(json, valPos);
        }

        size_t resPos = FastJson::FindKey(json, "heightmap_resolution_m", metaPos);
        if (resPos != std::string::npos) {
            size_t colon = json.find(':', resPos);
            size_t valPos = colon + 1;
            m_meta.heightmapResolutionM = static_cast<float>(FastJson::ParseNumber(json, valPos));
        }

        size_t cellPos = FastJson::FindKey(json, "cell_size_m", metaPos);
        if (cellPos != std::string::npos) {
            size_t colon = json.find(':', cellPos);
            size_t valPos = colon + 1;
            m_meta.cellSizeM = static_cast<float>(FastJson::ParseNumber(json, valPos));
        }

        size_t wPos = FastJson::FindKey(json, "map_width_m", metaPos);
        if (wPos != std::string::npos) {
            size_t colon = json.find(':', wPos);
            size_t valPos = colon + 1;
            m_meta.mapWidthM = static_cast<float>(FastJson::ParseNumber(json, valPos));
        }

        size_t dPos = FastJson::FindKey(json, "map_depth_m", metaPos);
        if (dPos != std::string::npos) {
            size_t colon = json.find(':', dPos);
            size_t valPos = colon + 1;
            m_meta.mapDepthM = static_cast<float>(FastJson::ParseNumber(json, valPos));
        }

        size_t baseElevPos = FastJson::FindKey(json, "base_elevation_m", metaPos);
        if (baseElevPos != std::string::npos) {
            size_t colon = json.find(':', baseElevPos);
            size_t valPos = colon + 1;
            m_meta.baseElevationM = static_cast<float>(FastJson::ParseNumber(json, valPos));
        }
    }

    // 2. Terrain dimensions and heights
    size_t terrainPos = FastJson::FindKey(json, "terrain");
    if (terrainPos == std::string::npos) return false;

    size_t twPos = FastJson::FindKey(json, "width", terrainPos);
    if (twPos != std::string::npos) {
        size_t colon = json.find(':', twPos);
        size_t valPos = colon + 1;
        m_width = static_cast<int>(FastJson::ParseNumber(json, valPos));
    }

    size_t tdPos = FastJson::FindKey(json, "depth", terrainPos);
    if (tdPos != std::string::npos) {
        size_t colon = json.find(':', tdPos);
        size_t valPos = colon + 1;
        m_depth = static_cast<int>(FastJson::ParseNumber(json, valPos));
    }

    if (m_width <= 0 || m_depth <= 0) return false;

    // Parse heights array
    size_t heightsPos = FastJson::FindKey(json, "heights", terrainPos);
    if (heightsPos != std::string::npos) {
        size_t bracket = json.find('[', heightsPos);
        if (bracket != std::string::npos) {
            size_t pos = bracket + 1;
            m_heights.clear();
            m_heights.reserve(m_width * m_depth);
            while (pos < json.size() && json[pos] != ']') {
                FastJson::SkipWhitespace(json, pos);
                if (pos >= json.size() || json[pos] == ']') break;
                if (json[pos] == ',') { pos++; continue; }
                float h = static_cast<float>(FastJson::ParseNumber(json, pos));
                m_heights.push_back(h);
            }
        }
    }

    // 3. Surface Zones array
    size_t zonesPos = FastJson::FindKey(json, "surface_zones");
    if (zonesPos != std::string::npos) {
        size_t typesPos = FastJson::FindKey(json, "types", zonesPos);
        if (typesPos != std::string::npos) {
            size_t bracket = json.find('[', typesPos);
            if (bracket != std::string::npos) {
                size_t pos = bracket + 1;
                m_surfaceZones.clear();
                m_surfaceZones.reserve(m_width * m_depth);
                while (pos < json.size() && json[pos] != ']') {
                    FastJson::SkipWhitespace(json, pos);
                    if (pos >= json.size() || json[pos] == ']') break;
                    if (json[pos] == ',') { pos++; continue; }
                    std::string strType = FastJson::ParseString(json, pos);
                    m_surfaceZones.push_back(StringToSurfaceType(strType));
                }
            }
        }
    }

    // 4. Asset placements
    size_t assetsPos = FastJson::FindKey(json, "assets");
    if (assetsPos != std::string::npos) {
        size_t bracket = json.find('[', assetsPos);
        if (bracket != std::string::npos) {
            size_t pos = bracket + 1;
            m_assets.clear();
            while (pos < json.size() && json[pos] != ']') {
                FastJson::SkipWhitespace(json, pos);
                if (pos >= json.size() || json[pos] == ']') break;
                if (json[pos] == '{') {
                    size_t objEnd = json.find('}', pos);
                    if (objEnd == std::string::npos) break;
                    std::string objStr = json.substr(pos, objEnd - pos + 1);

                    AssetInstance inst;
                    // id
                    size_t idPos = FastJson::FindKey(objStr, "id");
                    if (idPos != std::string::npos) {
                        size_t colon = objStr.find(':', idPos);
                        size_t valPos = colon + 1;
                        inst.id = FastJson::ParseString(objStr, valPos);
                    }
                    // type
                    size_t typePos = FastJson::FindKey(objStr, "type");
                    if (typePos != std::string::npos) {
                        size_t colon = objStr.find(':', typePos);
                        size_t valPos = colon + 1;
                        inst.type = FastJson::ParseString(objStr, valPos);
                    }
                    // glb
                    size_t glbPos = FastJson::FindKey(objStr, "glb");
                    if (glbPos != std::string::npos) {
                        size_t colon = objStr.find(':', glbPos);
                        size_t valPos = colon + 1;
                        inst.glbPath = FastJson::ParseString(objStr, valPos);
                    }
                    // position [x, y, z]
                    size_t pPos = FastJson::FindKey(objStr, "position");
                    if (pPos != std::string::npos) {
                        size_t pBra = objStr.find('[', pPos);
                        if (pBra != std::string::npos) {
                            size_t pCur = pBra + 1;
                            inst.position.x = static_cast<float>(FastJson::ParseNumber(objStr, pCur));
                            if (objStr.find(',', pCur) != std::string::npos) pCur = objStr.find(',', pCur) + 1;
                            inst.position.y = static_cast<float>(FastJson::ParseNumber(objStr, pCur));
                            if (objStr.find(',', pCur) != std::string::npos) pCur = objStr.find(',', pCur) + 1;
                            inst.position.z = static_cast<float>(FastJson::ParseNumber(objStr, pCur));
                        }
                    }
                    m_assets.push_back(inst);
                    pos = objEnd + 1;
                } else {
                    pos++;
                }
            }
        }
    }

    RecomputeBounds();
    return true;
}

void GameTerrain::RecomputeBounds() {
    if (m_heights.empty()) return;
    float minH = m_heights[0];
    float maxH = m_heights[0];
    for (float h : m_heights) {
        if (h < minH) minH = h;
        if (h > maxH) maxH = h;
    }
    m_meta.minElevationM = minH;
    m_meta.maxElevationM = maxH;
}

// Binary header signature: "TG01" (Terrain Ground 001)
struct BinaryHeader {
    char magic[4]{'T', 'G', '0', '1'};
    uint32_t version{1};
    uint32_t width{0};
    uint32_t depth{0};
    float resolutionM{2.0f};
    float mapWidthM{320.0f};
    float mapDepthM{320.0f};
    float baseElevationM{1700.0f};
    uint32_t assetCount{0};
};

bool GameTerrain::SaveToBinary(const std::string& filepath) const {
    std::ofstream out(filepath, std::ios::binary);
    if (!out.is_open()) return false;

    BinaryHeader hdr;
    hdr.width = static_cast<uint32_t>(m_width);
    hdr.depth = static_cast<uint32_t>(m_depth);
    hdr.resolutionM = m_meta.heightmapResolutionM;
    hdr.mapWidthM = m_meta.mapWidthM;
    hdr.mapDepthM = m_meta.mapDepthM;
    hdr.baseElevationM = m_meta.baseElevationM;
    hdr.assetCount = static_cast<uint32_t>(m_assets.size());

    out.write(reinterpret_cast<const char*>(&hdr), sizeof(hdr));

    // Write heights as floats
    if (!m_heights.empty()) {
        out.write(reinterpret_cast<const char*>(m_heights.data()), m_heights.size() * sizeof(float));
    }

    // Write surface zones as uint8_t
    if (!m_surfaceZones.empty()) {
        out.write(reinterpret_cast<const char*>(m_surfaceZones.data()), m_surfaceZones.size() * sizeof(uint8_t));
    }

    return true;
}

bool GameTerrain::LoadFromBinary(const std::string& filepath) {
    std::ifstream in(filepath, std::ios::binary);
    if (!in.is_open()) return false;

    BinaryHeader hdr;
    in.read(reinterpret_cast<char*>(&hdr), sizeof(hdr));
    if (std::memcmp(hdr.magic, "TG01", 4) != 0) return false;

    m_width = static_cast<int>(hdr.width);
    m_depth = static_cast<int>(hdr.depth);
    m_meta.heightmapResolutionM = hdr.resolutionM;
    m_meta.mapWidthM = hdr.mapWidthM;
    m_meta.mapDepthM = hdr.mapDepthM;
    m_meta.baseElevationM = hdr.baseElevationM;

    size_t count = static_cast<size_t>(m_width) * m_depth;
    m_heights.resize(count);
    in.read(reinterpret_cast<char*>(m_heights.data()), count * sizeof(float));

    m_surfaceZones.resize(count);
    in.read(reinterpret_cast<char*>(m_surfaceZones.data()), count * sizeof(uint8_t));

    RecomputeBounds();
    return true;
}

float GameTerrain::GetElevationAt(float worldX, float worldZ) const {
    if (m_width < 2 || m_depth < 2 || m_heights.empty()) return m_meta.baseElevationM;

    float normX = (worldX / m_meta.mapWidthM) * (m_width - 1);
    float normZ = (worldZ / m_meta.mapDepthM) * (m_depth - 1);

    int ix = static_cast<int>(std::floor(normX));
    int iz = static_cast<int>(std::floor(normZ));

    ix = std::max(0, std::min(ix, m_width - 2));
    iz = std::max(0, std::min(iz, m_depth - 2));

    float fx = normX - ix;
    float fz = normZ - iz;

    float h00 = m_heights[iz * m_width + ix];
    float h10 = m_heights[iz * m_width + (ix + 1)];
    float h01 = m_heights[(iz + 1) * m_width + ix];
    float h11 = m_heights[(iz + 1) * m_width + (ix + 1)];

    // Bilinear interpolation
    float hBottom = h00 * (1.0f - fx) + h10 * fx;
    float hTop    = h01 * (1.0f - fx) + h11 * fx;
    return hBottom * (1.0f - fz) + hTop * fz;
}

SurfaceType GameTerrain::GetSurfaceTypeAt(float worldX, float worldZ) const {
    if (m_width <= 0 || m_depth <= 0 || m_surfaceZones.empty()) return SurfaceType::Grass;

    int ix = static_cast<int>(std::round((worldX / m_meta.mapWidthM) * (m_width - 1)));
    int iz = static_cast<int>(std::round((worldZ / m_meta.mapDepthM) * (m_depth - 1)));

    ix = std::max(0, std::min(ix, m_width - 1));
    iz = std::max(0, std::min(iz, m_depth - 1));

    return m_surfaceZones[iz * m_width + ix];
}

float GameTerrain::GetFrictionAt(float worldX, float worldZ) const {
    SurfaceType st = GetSurfaceTypeAt(worldX, worldZ);
    switch (st) {
        case SurfaceType::Asphalt: return m_meta.physics.asphaltFriction;
        case SurfaceType::Mud:     return m_meta.physics.mudFriction;
        case SurfaceType::Gravel:  return m_meta.physics.gravelFriction;
        case SurfaceType::Water:   return m_meta.physics.waterFriction;
        default:                   return m_meta.physics.defaultFriction;
    }
}

Vec3 GameTerrain::GetNormalAt(float worldX, float worldZ) const {
    float eps = m_meta.heightmapResolutionM * 0.5f;
    float hL = GetElevationAt(worldX - eps, worldZ);
    float hR = GetElevationAt(worldX + eps, worldZ);
    float hD = GetElevationAt(worldX, worldZ - eps);
    float hU = GetElevationAt(worldX, worldZ + eps);

    Vec3 normal(-(hR - hL), 2.0f * eps, -(hU - hD));
    float len = std::sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z);
    if (len > 0.0001f) {
        normal.x /= len;
        normal.y /= len;
        normal.z /= len;
    }
    return normal;
}

bool GameTerrain::Raycast(const Vec3& rayOrigin, const Vec3& rayDir, float maxDist, Vec3& outHitPoint, Vec3& outNormal) const {
    float stepDist = m_meta.heightmapResolutionM * 0.5f;
    float currDist = 0.0f;

    while (currDist < maxDist) {
        Vec3 testPt(
            rayOrigin.x + rayDir.x * currDist,
            rayOrigin.y + rayDir.y * currDist,
            rayOrigin.z + rayDir.z * currDist
        );

        float terrainH = GetElevationAt(testPt.x, testPt.z);
        if (testPt.y <= terrainH) {
            // Binary search refinement
            float t0 = currDist - stepDist;
            float t1 = currDist;
            for (int k = 0; k < 6; ++k) {
                float tm = (t0 + t1) * 0.5f;
                Vec3 p(rayOrigin.x + rayDir.x * tm, rayOrigin.y + rayDir.y * tm, rayOrigin.z + rayDir.z * tm);
                if (p.y <= GetElevationAt(p.x, p.z)) t1 = tm;
                else t0 = tm;
            }
            outHitPoint = Vec3(rayOrigin.x + rayDir.x * t1, GetElevationAt(rayOrigin.x + rayDir.x * t1, rayOrigin.z + rayDir.z * t1), rayOrigin.z + rayDir.z * t1);
            outNormal = GetNormalAt(outHitPoint.x, outHitPoint.z);
            return true;
        }
        currDist += stepDist;
    }
    return false;
}

void GameTerrain::GenerateMeshData(std::vector<TerrainVertex>& outVertices, std::vector<uint32_t>& outIndices, int stepLOD) const {
    if (m_width < 2 || m_depth < 2) return;
    int stepSize = std::max(1, stepLOD);

    int subW = (m_width - 1) / stepSize + 1;
    int subD = (m_depth - 1) / stepSize + 1;

    outVertices.clear();
    outVertices.reserve(subW * subD);
    outIndices.clear();
    outIndices.reserve((subW - 1) * (subD - 1) * 6);

    float stepX = m_meta.mapWidthM / (m_width - 1);
    float stepZ = m_meta.mapDepthM / (m_depth - 1);

    for (int iz = 0; iz < m_depth; iz += stepSize) {
        for (int ix = 0; ix < m_width; ix += stepSize) {
            TerrainVertex vert;
            float wx = ix * stepX;
            float wz = iz * stepZ;
            float wy = m_heights[iz * m_width + ix];

            vert.position = Vec3(wx, wy, wz);
            vert.normal = GetNormalAt(wx, wz);
            vert.uv[0] = static_cast<float>(ix) / (m_width - 1);
            vert.uv[1] = static_cast<float>(iz) / (m_depth - 1);

            SurfaceType st = m_surfaceZones[iz * m_width + ix];
            vert.surfaceId = static_cast<uint8_t>(st);

            // Compute splat weights
            vert.splatWeights[0] = (st == SurfaceType::Grass) ? 1.0f : 0.0f;
            vert.splatWeights[1] = (st == SurfaceType::Asphalt) ? 1.0f : 0.0f;
            vert.splatWeights[2] = (st == SurfaceType::Mud) ? 1.0f : 0.0f;
            vert.splatWeights[3] = (st == SurfaceType::Gravel) ? 1.0f : 0.0f;

            outVertices.push_back(vert);
        }
    }

    for (int iz = 0; iz < subD - 1; ++iz) {
        for (int ix = 0; ix < subW - 1; ++ix) {
            uint32_t i00 = iz * subW + ix;
            uint32_t i10 = iz * subW + (ix + 1);
            uint32_t i01 = (iz + 1) * subW + ix;
            uint32_t i11 = (iz + 1) * subW + (ix + 1);

            // Triangle 1
            outIndices.push_back(i00);
            outIndices.push_back(i01);
            outIndices.push_back(i10);

            // Triangle 2
            outIndices.push_back(i10);
            outIndices.push_back(i01);
            outIndices.push_back(i11);
        }
    }
}

} // namespace Terrain
} // namespace GameEngine
