// ─── app/src/main/cpp/vulkan_magadi_terrain.h ────────────────────────────────────
// Kenya Magadi Road 50km Vulkan Terrain Pipeline
// Following Sascha Willems Vulkan Infinite Procedural Terrain Architecture
#pragma once

#include "vulkan_engine.h"
#include <string>
#include <vector>
#include <memory>
#include <cstdint>

namespace custom_vulkan {

// Push constant block for Magadi contour terrain shader
struct MagadiTerrainPushConstants {
    float mvpMatrix[16];           // 64 bytes: Model-View-Projection matrix
    float lightDir[4];             // 16 bytes: Directional sunlight
    float cameraPos[4];            // 16 bytes: Eye position
    float contourInterval;         // 4 bytes: Topographic contour interval (e.g., 25.0m)
    float indexContourInterval;    // 4 bytes: Bold index contour interval (e.g., 100.0m)
    float stepTerracingFactor;     // 4 bytes: Stepped contour terracing [0.0 - 1.0]
    uint32_t showContourIsolines;  // 4 bytes: 1 = render contour lines on surface
    uint32_t polygonMode;          // 4 bytes: 0 = solid, 1 = wireframe
    uint32_t activeChunkCol;       // 4 bytes: Active chunk column [0..4]
    uint32_t activeChunkRow;       // 4 bytes: Active chunk row [0..4]
    float padding[2];              // 8 bytes padding to align to 128 bytes
};

// Represents a GPU-ready Magadi Terrain Chunk Buffer in Vulkan memory
struct MagadiGPUChunk {
    int col = 0;
    int row = 0;
    std::string chunkId;
    float elevMin = 590.0f;
    float elevMax = 1980.0f;
    bool hasRoad = false;
    uint32_t vertexCount = 0;
    uint32_t indexCount = 0;
    std::vector<EngineVertex> vertices;
    std::vector<uint32_t> indices;
};

class VulkanMagadiTerrain {
public:
    VulkanMagadiTerrain();
    ~VulkanMagadiTerrain() = default;

    // Initialize 5x5 chunk grid with Sascha Willems contour-driven elevation
    bool initialize(float contourInterval = 25.0f, float stepTerracingFactor = 0.0f);

    // Update contour parameters
    void setContourParameters(float interval, float indexInterval, float terracingFactor, bool showIsolines);

    // Get specific chunk for rendering
    const MagadiGPUChunk* getChunk(int col, int row) const;
    const std::vector<MagadiGPUChunk>& getAllChunks() const { return m_gpuChunks; }

    // Metrics for native telemetry
    uint32_t getTotalVertices() const;
    uint32_t getTotalTriangles() const;
    const MagadiTerrainPushConstants& getPushConstants() const { return m_pushConstants; }

private:
    float m_contourInterval = 25.0f;
    float m_indexContourInterval = 100.0f;
    float m_stepTerracing = 0.0f;
    bool m_showContours = true;

    std::vector<MagadiGPUChunk> m_gpuChunks;
    MagadiTerrainPushConstants m_pushConstants{};

    void generateGpuChunk(int col, int row, MagadiGPUChunk& outChunk);
    float sampleContourElevation(float wx, float wz) const;
};

} // namespace custom_vulkan
