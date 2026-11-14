// ─── app/src/main/cpp/vulkan_magadi_terrain.cpp ──────────────────────────────────
// Kenya Magadi Road 50km Vulkan Terrain Pipeline Implementation
// Following Sascha Willems Vulkan Infinite Procedural Terrain Architecture

#include "vulkan_magadi_terrain.h"
#include <cmath>
#include <algorithm>

namespace custom_vulkan {

static constexpr float CHUNK_SIZE_M = 10000.0f;
static constexpr int CHUNK_RES = 33; // 33x33 vertices per chunk

VulkanMagadiTerrain::VulkanMagadiTerrain() {
    m_pushConstants.contourInterval = 25.0f;
    m_pushConstants.indexContourInterval = 100.0f;
    m_pushConstants.stepTerracingFactor = 0.0f;
    m_pushConstants.showContourIsolines = 1;
    m_pushConstants.polygonMode = 0;
}

bool VulkanMagadiTerrain::initialize(float contourInterval, float stepTerracingFactor) {
    m_contourInterval = contourInterval;
    m_stepTerracing = stepTerracingFactor;
    m_gpuChunks.clear();
    m_gpuChunks.resize(25);

    for (int r = 0; r < 5; ++r) {
        for (int c = 0; c < 5; ++c) {
            int idx = r * 5 + c;
            generateGpuChunk(c, r, m_gpuChunks[idx]);
        }
    }
    return true;
}

void VulkanMagadiTerrain::setContourParameters(float interval, float indexInterval, float terracingFactor, bool showIsolines) {
    m_contourInterval = interval;
    m_indexContourInterval = indexInterval;
    m_stepTerracing = terracingFactor;
    m_showContours = showIsolines;

    m_pushConstants.contourInterval = interval;
    m_pushConstants.indexContourInterval = indexInterval;
    m_pushConstants.stepTerracingFactor = terracingFactor;
    m_pushConstants.showContourIsolines = showIsolines ? 1 : 0;

    // Regenerate chunks with updated contour terracing
    for (int r = 0; r < 5; ++r) {
        for (int c = 0; c < 5; ++c) {
            int idx = r * 5 + c;
            generateGpuChunk(c, r, m_gpuChunks[idx]);
        }
    }
}

float VulkanMagadiTerrain::sampleContourElevation(float wx, float wz) const {
    // 1. Regional gradient: 590m at Lake Magadi (SW) to 1980m at Ngong Hills (NE)
    float u = std::max(0.0f, std::min(1.0f, (wx + wz * 0.95f) / 95000.0f));
    float baseElev = 590.0f + u * 1390.0f;

    // 2. Rift Valley tectonic fault terraces
    float fault1 = std::sin(wx * 0.00035f + 0.8f) * 65.0f;
    float fault2 = std::sin(wx * 0.00095f + wz * 0.00025f) * 35.0f;

    float rawElev = baseElev + fault1 + fault2;

    // 3. Lake Magadi basin clamp
    if (wx < 11000.0f && wz < 11000.0f) {
        float lDist = std::sqrt(wx * wx + wz * wz);
        if (lDist < 9500.0f) {
            float t = std::min(1.0f, lDist / 9500.0f);
            rawElev = 590.0f + (rawElev - 590.0f) * (t * t);
        }
    }

    // 4. Contour terracing shaping
    if (m_stepTerracing > 0.001f) {
        float stepH = m_contourInterval;
        float baseStep = std::floor(rawElev / stepH) * stepH;
        float frac = (rawElev - baseStep) / stepH;
        float steppedFrac = frac * frac * (3.0f - 2.0f * frac);
        float terracedElev = baseStep + steppedFrac * stepH;
        rawElev = rawElev * (1.0f - m_stepTerracing) + terracedElev * m_stepTerracing;
    }

    return std::max(590.0f, std::min(1980.0f, rawElev));
}

void VulkanMagadiTerrain::generateGpuChunk(int col, int row, MagadiGPUChunk& outChunk) {
    char idBuf[32];
    snprintf(idBuf, sizeof(idBuf), "chunk_%02d_%02d", col, row);
    outChunk.chunkId = idBuf;
    outChunk.col = col;
    outChunk.row = row;

    float originX = col * CHUNK_SIZE_M;
    float originZ = row * CHUNK_SIZE_M;
    float step = CHUNK_SIZE_M / static_cast<float>(CHUNK_RES - 1);
    float halfSize = CHUNK_SIZE_M * 0.5f;

    outChunk.elevMin = 1e6f;
    outChunk.elevMax = -1e6f;
    outChunk.vertices.clear();
    outChunk.vertices.reserve(CHUNK_RES * CHUNK_RES);

    for (int iz = 0; iz < CHUNK_RES; ++iz) {
        float lz = iz * step;
        float wz = originZ + lz;

        for (int ix = 0; ix < CHUNK_RES; ++ix) {
            float lx = ix * step;
            float wx = originX + lx;

            float elev = sampleContourElevation(wx, wz);
            if (elev < outChunk.elevMin) outChunk.elevMin = elev;
            if (elev > outChunk.elevMax) outChunk.elevMax = elev;

            // Central differencing normal
            float left  = sampleContourElevation(wx - step, wz);
            float right = sampleContourElevation(wx + step, wz);
            float up    = sampleContourElevation(wx, wz - step);
            float down  = sampleContourElevation(wx, wz + step);

            float dx = (right - left) / (2.0f * step);
            float dz = (down - up) / (2.0f * step);
            float len = std::sqrt(dx * dx + 1.0f + dz * dz);

            EngineVertex v{};
            v.pos[0] = lx - halfSize;
            v.pos[1] = elev;
            v.pos[2] = lz - halfSize;

            v.normal[0] = -dx / len;
            v.normal[1] = 1.0f / len;
            v.normal[2] = -dz / len;

            v.uv[0] = lx / CHUNK_SIZE_M;
            v.uv[1] = lz / CHUNK_SIZE_M;

            // Contour-based hypsometric tinting for vertex color
            float t = (elev - 590.0f) / 1390.0f;
            if (t < 0.15f) {
                // Trona soda white/cyan
                v.color[0] = 0.85f; v.color[1] = 0.92f; v.color[2] = 0.95f; v.color[3] = 1.0f;
            } else if (t < 0.55f) {
                // Savannah golden khaki
                v.color[0] = 0.72f; v.color[1] = 0.65f; v.color[2] = 0.38f; v.color[3] = 1.0f;
            } else if (t < 0.80f) {
                // Highland volcanic slope
                v.color[0] = 0.42f; v.color[1] = 0.52f; v.color[2] = 0.28f; v.color[3] = 1.0f;
            } else {
                // Escarpment basalt cliff
                v.color[0] = 0.35f; v.color[1] = 0.32f; v.color[2] = 0.30f; v.color[3] = 1.0f;
            }

            outChunk.vertices.push_back(v);
        }
    }

    outChunk.indices.clear();
    outChunk.indices.reserve((CHUNK_RES - 1) * (CHUNK_RES - 1) * 6);

    for (int iz = 0; iz < CHUNK_RES - 1; ++iz) {
        for (int ix = 0; ix < CHUNK_RES - 1; ++ix) {
            uint32_t topLeft = iz * CHUNK_RES + ix;
            uint32_t topRight = topLeft + 1;
            uint32_t bottomLeft = (iz + 1) * CHUNK_RES + ix;
            uint32_t bottomRight = bottomLeft + 1;

            outChunk.indices.push_back(topLeft);
            outChunk.indices.push_back(bottomLeft);
            outChunk.indices.push_back(topRight);

            outChunk.indices.push_back(topRight);
            outChunk.indices.push_back(bottomLeft);
            outChunk.indices.push_back(bottomRight);
        }
    }

    outChunk.vertexCount = static_cast<uint32_t>(outChunk.vertices.size());
    outChunk.indexCount = static_cast<uint32_t>(outChunk.indices.size());
}

const MagadiGPUChunk* VulkanMagadiTerrain::getChunk(int col, int row) const {
    if (col < 0 || col >= 5 || row < 0 || row >= 5) return nullptr;
    size_t idx = static_cast<size_t>(row * 5 + col);
    if (idx < m_gpuChunks.size()) return &m_gpuChunks[idx];
    return nullptr;
}

uint32_t VulkanMagadiTerrain::getTotalVertices() const {
    uint32_t sum = 0;
    for (const auto& c : m_gpuChunks) sum += c.vertexCount;
    return sum;
}

uint32_t VulkanMagadiTerrain::getTotalTriangles() const {
    uint32_t sum = 0;
    for (const auto& c : m_gpuChunks) sum += (c.indexCount / 3);
    return sum;
}

} // namespace custom_vulkan
