# ─── app/src/main/cpp/CMakeLists.txt ──────────────────────────────────────────
# CMake build script for compiling native Vulkan 1.3 C++ procedural engine and scene
cmake_minimum_required(VERSION 3.22.1)
project(apex_vulkan LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Vulkan SDK package
find_package(Vulkan QUIET)

add_library(apex_vulkan SHARED
    vulkan_engine.cpp
    vulkan_asset_models.cpp
    vulkan_renderer.cpp
    vulkan_power_grid.cpp
    continuous_cable_cylinder.cpp
    catenary_cable_physics.cpp
    procedural_models.cpp
    vulkan_magadi_terrain.cpp
    Math3D.cpp
    MagadiSceneEngine.cpp
    ProceduralSceneEngine.cpp
    GlbParserEngine.cpp
    NativeEngine_JNI.cpp
)

target_include_directories(apex_vulkan PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(apex_vulkan PRIVATE
    log
    android
)

if(Vulkan_FOUND)
    target_link_libraries(apex_vulkan PRIVATE
        Vulkan::Vulkan
    )
    target_compile_definitions(apex_vulkan PRIVATE VK_USE_PLATFORM_ANDROID_KHR)
endif()
