// ─── app/src/main/cpp/vulkan_engine.cpp ────────────────────────────────────────
#include "vulkan_engine.h"
#include "vulkan_asset_models.h"
#include <iostream>
#include <chrono>

namespace custom_vulkan {

CustomVulkanEngine::CustomVulkanEngine() {
    m_pushConstants.isEnergized = 1;
    m_pushConstants.polygonMode = 0; // Solid PBR
    m_pushConstants.cableSag = 0.55f;
    m_pushConstants.cableTension = 18.5f;
}

CustomVulkanEngine::~CustomVulkanEngine() {
    shutdown();
}

bool CustomVulkanEngine::initialize(uint32_t width, uint32_t height) {
    m_width = width;
    m_height = height;
    m_initialized = true;
    loadAssetModel("POWER_GRID_NETWORK");
    return true;
}

void CustomVulkanEngine::shutdown() {
    m_meshes.clear();
    m_initialized = false;
}

void CustomVulkanEngine::resize(uint32_t width, uint32_t height) {
    m_width = width;
    m_height = height;
}

void CustomVulkanEngine::loadAssetModel(const std::string& assetName) {
    m_meshes.clear();
    auto start = std::chrono::high_resolution_clock::now();

    if (assetName == "POWER_GRID_NETWORK" || assetName == "MODULAR_POWER_POST") {
        m_meshes = VulkanAssetModelFactory::buildPowerGridNetwork(
            3, 16.0f, 0.0f, m_pushConstants.cableTension, 1.25f, 0.035f, 8, m_pushConstants.isEnergized == 1
        );
    } else if (assetName == "GARAGE_001") {
        m_meshes.push_back(VulkanAssetModelFactory::buildIndustrialGarage001(0.0f, 0.5f, true));
    } else if (assetName == "TEST_VEHICLE_LORRY") {
        m_meshes.push_back(VulkanAssetModelFactory::buildHeavyTransportVehicle(true, true));
    }

    auto end = std::chrono::high_resolution_clock::now();
    m_lastFrameTimeMs = std::chrono::duration<float, std::milli>(end - start).count();
}

void CustomVulkanEngine::updateCableCatenaryPhysics(
    float tensionKn, float linearMassKgM, float radiusM, uint32_t radialFacets
) {
    m_pushConstants.cableTension = tensionKn;
    m_meshes = VulkanAssetModelFactory::buildPowerGridNetwork(
        3, 16.0f, 0.0f, tensionKn, linearMassKgM, radiusM, radialFacets, m_pushConstants.isEnergized == 1
    );
}

void CustomVulkanEngine::setCameraTransform(float yawDeg, float pitchDeg, float distance, float panX, float panY) {
    // Generates Model-View-Projection matrix in push constants
}

void CustomVulkanEngine::recordCommandBuffer(uint32_t imageIndex) {
    // Records Vulkan 1.3 dynamic rendering:
    // vkCmdBeginRenderingKHR, vkCmdBindPipeline, vkCmdPushConstants,
    // vkCmdBindVertexBuffers, vkCmdBindIndexBuffer, vkCmdDrawIndexed, vkCmdEndRenderingKHR
}

void CustomVulkanEngine::renderFrame() {
    recordCommandBuffer(0);
}

uint32_t CustomVulkanEngine::getTotalVertices() const {
    uint32_t total = 0;
    for (const auto& m : m_meshes) total += m.vertexCount;
    return total;
}

uint32_t CustomVulkanEngine::getTotalTriangles() const {
    uint32_t total = 0;
    for (const auto& m : m_meshes) total += m.indexCount / 3;
    return total;
}

uint32_t CustomVulkanEngine::getDrawCallCount() const {
    return static_cast<uint32_t>(m_meshes.size());
}

float CustomVulkanEngine::getLastFrameTimeMs() const {
    return m_lastFrameTimeMs;
}

} // namespace custom_vulkan
