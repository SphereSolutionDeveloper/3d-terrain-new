cmake_minimum_required(VERSION 3.22.1)
project(apex_rock_renderer VERSION 2.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(include)

add_library(apex_rock_renderer STATIC
    src/rock_renderer.cpp
    src/procedural_garage.cpp
    src/procedural_vehicle.cpp
    src/procedural_power_grid.cpp
)

target_include_directories(apex_rock_renderer PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>
)
