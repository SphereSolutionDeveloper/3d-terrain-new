# ─── native-assets/CMakeLists.txt ──────────────────────────────────────────
# Clean Architecture: Native Assets CMake Build Definition
# Compiles the native Vulkan 1.3 C++ procedural engine and JNI bridge
cmake_minimum_required(VERSION 3.22.1)
project(apex_vulkan LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Vulkan SDK package
find_package(Vulkan QUIET)

add_library(apex_vulkan SHARED
    cpp/vulkan_engine.cpp
    cpp/vulkan_asset_models.cpp
    cpp/vulkan_renderer.cpp
    cpp/vulkan_power_grid.cpp
    cpp/continuous_cable_cylinder.cpp
    cpp/catenary_cable_physics.cpp
    cpp/procedural_models.cpp
    cpp/vulkan_magadi_terrain.cpp
    cpp/Math3D.cpp
    cpp/MagadiSceneEngine.cpp
    cpp/MagadiTownEngine.cpp
    cpp/ProceduralSceneEngine.cpp
    cpp/GlbParserEngine.cpp
    cpp/NativeEngine_JNI.cpp
    cpp/vulkan_road_components.cpp
    src/road/bridges/BridgeGenerator.cpp
    src/road/tolls/TollPlazaGenerator.cpp
    src/road/tunnels/TunnelGenerator.cpp
    src/road/interchanges/InterchangeGen.cpp
    src/road/retaining_walls/RetainingWallGen.cpp
    src/road/asphalt/ProceduralAsphalt.cpp
    src/road/barriers/BarrierKit.cpp
    src/road/signage/SignpostGenerator.cpp
    src/road/roadblocks/RoadBlockGenerator.cpp
    src/road/vulkan/VulkanRoadScene.cpp
)

target_include_directories(apex_vulkan PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/base
    ${CMAKE_CURRENT_SOURCE_DIR}/src/road
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
