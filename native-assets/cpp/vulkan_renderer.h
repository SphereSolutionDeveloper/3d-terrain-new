// ─── app/src/main/cpp/vulkan_renderer.h ──────────────────────────────────────
// Custom Vulkan 1.3 C++ Native Renderer Pipeline
#pragma once

#include "vulkan_engine.h"
#include <string>

namespace custom_vulkan {

class VulkanCustomRenderer {
public:
    VulkanCustomRenderer();
    ~VulkanCustomRenderer();

    bool initPipeline();
    void renderScene(const CustomVulkanEngine& engine);

    std::string getDeviceInfo() const {
        return "Custom Vulkan 1.3 Renderer (Adreno/Mali/Intel/NVIDIA Native Engine)";
    }
};

} // namespace custom_vulkan
