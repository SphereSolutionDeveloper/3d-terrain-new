// ─── app/src/main/cpp/vulkan_engine.h ────────────────────────────────────────
// Customized Vulkan 1.3 C++ Game Engine Architecture
// Zero Kotlin in rendering or modeling: Pure C++ Vulkan Native Engine
#pragma once

#include <vector>
#include <string>
#include <memory>
#include <cstdint>

namespace custom_vulkan {

// Vertex memory layout for Vulkan vertex buffer binding
struct EngineVertex {
    float pos[3];       // Position: X, Y, Z
    float normal[3];    // Normal: Nx, Ny, Nz (for PBR lighting)
    float uv[2];        // UV: U, V
    float color[4];     // Color: R, G, B, A
};

// Push Constants block (128 bytes max, zero descriptor overhead)
struct EnginePushConstants {
    float mvpMatrix[16];   // Model-View-Projection matrix
    float lightDir[4];     // Directional light vector + intensity
    float cameraPos[4];    // Camera eye position
    float cableSag;        // Catenary cable sag (meters)
    float cableTension;    // Cable tension (kN)
    uint32_t isEnergized;  // 1 = High-voltage corona glow active
    uint32_t polygonMode;  // 0 = Solid, 1 = Wireframe
};

// Mesh buffer representation in GPU memory
struct GPUMeshBuffer {
    std::string name;
    std::vector<EngineVertex> vertices;
    std::vector<uint32_t> indices;
    uint32_t vertexCount = 0;
    uint32_t indexCount = 0;
    uint64_t vmaAllocationBytes = 0;
};

// High-Performance Custom Vulkan Engine State
class CustomVulkanEngine {
public:
    CustomVulkanEngine();
    ~CustomVulkanEngine();

    // Engine Lifecycle
    bool initialize(uint32_t width, uint32_t height);
    void shutdown();
    void resize(uint32_t width, uint32_t height);

    // Asset Management (All in C++, Zero Kotlin)
    void loadAssetModel(const std::string& assetName);
    void updateCableCatenaryPhysics(float tensionKn, float linearMassKgM, float radiusM, uint32_t radialFacets);
    void setCameraTransform(float yawDeg, float pitchDeg, float distance, float panX, float panY);

    // Frame Rendering
    void recordCommandBuffer(uint32_t imageIndex);
    void renderFrame();

    // Telemetry & Metrics
    uint32_t getTotalVertices() const;
    uint32_t getTotalTriangles() const;
    uint32_t getDrawCallCount() const;
    float getLastFrameTimeMs() const;
    const std::vector<GPUMeshBuffer>& getLoadedMeshes() const { return m_meshes; }

private:
    uint32_t m_width = 1280;
    uint32_t m_height = 720;
    bool m_initialized = false;
    float m_lastFrameTimeMs = 0.14f;

    std::vector<GPUMeshBuffer> m_meshes;
    EnginePushConstants m_pushConstants;
};

} // namespace custom_vulkan
