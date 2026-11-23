// ==============================================================================
// Procedural Vulkan Asset Suite - Shared Road Texture Descriptor Spec
// Path: native-assets/src/road/textures/spec/road_common.h
// ==============================================================================
#pragma once

#include <cstdint>

namespace ApexAssets {
namespace Road {

enum class RoadTextureChannelFormat : uint32_t {
    RGBA8_SRGB   = 0, // Albedo / Markings / Signage / Hazard Stripes
    RGBA8_UNORM  = 1, // Tangent-Space Normal Maps & ORM Packed Maps
    R16F_HEIGHT  = 2, // Micro-Aggregate / Rut / Crack Heightfields
    R8_STENCIL   = 3  // Crosswalk / Arrow / Retroreflective Stencil Masks
};

struct RoadProceduralTextureSpec {
    uint32_t widthPx = 1024;
    uint32_t heightPx = 1024;
    uint32_t mipLevels = 10;
    RoadTextureChannelFormat albedoFormat = RoadTextureChannelFormat::RGBA8_SRGB;
    RoadTextureChannelFormat normalFormat = RoadTextureChannelFormat::RGBA8_UNORM;
    RoadTextureChannelFormat ormFormat    = RoadTextureChannelFormat::RGBA8_UNORM;
    float texelScaleMeters = 4.0f; // Physical UV tile span in world meters
};

} // namespace Road
} // namespace ApexAssets
