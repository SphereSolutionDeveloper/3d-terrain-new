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
    MagadiTownEngine.cpp
    C58CorridorEngine.cpp
    ProceduralSceneEngine.cpp
    GlbParserEngine.cpp
    NativeEngine_JNI.cpp
    vulkan_road_components.cpp
    ../src/road/bridges/BridgeGenerator.cpp
    ../src/road/tolls/TollPlazaGenerator.cpp
    ../src/road/tunnels/TunnelGenerator.cpp
    ../src/road/interchanges/InterchangeGen.cpp
    ../src/road/retaining_walls/RetainingWallGen.cpp
    ../src/road/asphalt/ProceduralAsphalt.cpp
    ../src/road/barriers/BarrierKit.cpp
    ../src/road/signage/SignpostGenerator.cpp
    ../src/road/roadblocks/RoadBlockGenerator.cpp
    ../src/road/RoadNetwork.cpp
    ../src/road/RoadSplineGraph.cpp
    ../src/road/RoadTileGen.cpp
    ../src/road/vulkan/VulkanRoadScene.cpp
)

target_include_directories(apex_vulkan PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/../src/road
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
