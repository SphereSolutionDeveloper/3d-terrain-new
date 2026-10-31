// ─── app/src/main/cpp/GlbParserEngine.h ─────────────────────────────────────────
// Native C++ Binary GLB / glTF 2.0 Parser Engine
#pragma once

#include "Math3D.h"
#include <vector>
#include <string>
#include <cstdint>

namespace custom_vulkan {

struct GlbParsedMesh {
    std::string name;
    std::vector<Vec3> positions;
    std::vector<Vec3> normals;
    std::vector<Vec2> uvs;
    std::vector<uint32_t> indices;
    NativeAABB aabb;
    bool isValid = false;
};

class GlbParserEngine {
public:
    static GlbParserEngine& getInstance();

    // Parse binary GLB buffer into raw 3D mesh geometry
    GlbParsedMesh parseGlbBuffer(const uint8_t* data, size_t length);

private:
    GlbParserEngine() = default;
};

} // namespace custom_vulkan
