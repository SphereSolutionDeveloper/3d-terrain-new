cmake_minimum_required(VERSION 3.15)
project(GameTerrain_TestGround001 LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(TERRAIN_SOURCES
    GameTerrainLoader.h
    GameTerrainLoader.cpp
    GameTerrainC_API.h
    GameTerrainC_API.cpp
)

# Shared library for engine runtime (Unity, Unreal, Godot plugins)
add_library(GameTerrainShared SHARED ${TERRAIN_SOURCES})
target_include_directories(GameTerrainShared PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

# Static library for direct C++ linking
add_library(GameTerrainStatic STATIC ${TERRAIN_SOURCES})
target_include_directories(GameTerrainStatic PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

# Standalone CLI Converter & Verification Tool
add_executable(terrain_converter main_converter.cpp ${TERRAIN_SOURCES})
target_include_directories(terrain_converter PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
