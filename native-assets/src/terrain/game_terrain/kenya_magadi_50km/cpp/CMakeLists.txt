# Kenya Magadi 50km Terrain System CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project(magadi_terrain LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(magadi_terrain STATIC
    MagadiTerrainChunk.cpp
    MagadiTerrainChunkManager.cpp
    MagadiTerrainC_API.cpp
)

target_include_directories(magadi_terrain PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)
