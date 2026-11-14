# CMakeLists.txt for libterrain.a
# Procedural Vulkan Asset Suite - Terrain Engine

cmake_minimum_required(VERSION 3.20)
project(TerrainModule LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(TERRAIN_SOURCES
    TerrainSystem.cpp
    heightmaps/HeightmapGen.cpp
    heightmaps/FbmNoise.cpp
    heightmaps/Erosion.cpp
    heightmaps/Ridged.cpp
    heightmaps/Warp.cpp
    splatmaps/SplatGenerator.cpp
    cliffs/CliffGen.cpp
    cliffs/CliffErosion.cpp
    cliffs/CliffModular.cpp
    roads_cut/RoadCut.cpp
    caves/CaveGen.cpp
    caves/MarchingCubes.cpp
    caves/CaveVoxel.cpp
    meta/TileIndex.cpp
    meta/TileLOD.cpp
)

set(TERRAIN_HEADERS
    TerrainSystem.h
    heightmaps/HeightmapGen.h
    heightmaps/FbmNoise.h
    heightmaps/Erosion.h
    heightmaps/TerrainParams.h
    splatmaps/SplatGenerator.h
    splatmaps/SplatParams.h
    cliffs/CliffGen.h
    cliffs/CliffParams.h
    roads_cut/RoadCut.h
    roads_cut/RoadCutParams.h
    caves/CaveGen.h
    caves/MarchingCubes.h
    caves/CaveParams.h
    meta/TileIndex.h
    meta/TileLOD.h
)

add_library(terrain STATIC ${TERRAIN_SOURCES} ${TERRAIN_HEADERS})

target_include_directories(terrain PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}>
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/..>
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/../../base>
)

if (TARGET glm::glm)
    target_link_libraries(terrain PUBLIC glm::glm)
endif()

message(STATUS "Configured libterrain.a with pure procedural C++ generators")
