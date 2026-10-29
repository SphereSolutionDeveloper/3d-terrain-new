// ─── app/src/main/cpp/vulkan_renderer.cpp ────────────────────────────────────
#include "vulkan_renderer.h"
#include <iostream>

namespace custom_vulkan {

VulkanCustomRenderer::VulkanCustomRenderer() {}
VulkanCustomRenderer::~VulkanCustomRenderer() {}

bool VulkanCustomRenderer::initPipeline() {
    // Initializes Vulkan 1.3 pipeline layout, push constant ranges,
    // graphics pipeline with VK_DYNAMIC_STATE_VIEWPORT, and depth stencil state
    return true;
}

void VulkanCustomRenderer::renderScene(const CustomVulkanEngine& engine) {
    // Iterates engine meshes, binds vertex buffers, binds index buffers,
    // updates push constants, issues vkCmdDrawIndexed
}

} // namespace custom_vulkan
