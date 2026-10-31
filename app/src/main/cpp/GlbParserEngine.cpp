// ─── app/src/main/cpp/GlbParserEngine.cpp ─────────────────────────────────────
// Native C++ Binary GLB / glTF 2.0 Parser Engine Implementation
#include "GlbParserEngine.h"
#include <cstring>
#include <algorithm>

namespace custom_vulkan {

GlbParserEngine& GlbParserEngine::getInstance() {
    static GlbParserEngine instance;
    return instance;
}

// GLB Header: Magic (0x46546C67 = "glTF"), Version, Length
struct GlbHeader {
    uint32_t magic;
    uint32_t version;
    uint32_t length;
};

// Chunk Header: ChunkLength, ChunkType (0x4E4F534A = "JSON", 0x004E4942 = "BIN\0")
struct GlbChunkHeader {
    uint32_t chunkLength;
    uint32_t chunkType;
};

GlbParsedMesh GlbParserEngine::parseGlbBuffer(const uint8_t* data, size_t length) {
    GlbParsedMesh result;
    if (!data || length < sizeof(GlbHeader) + sizeof(GlbChunkHeader)) {
        return result;
    }

    GlbHeader header;
    std::memcpy(&header, data, sizeof(GlbHeader));

    // Check magic "glTF"
    if (header.magic != 0x46546C67 || header.version < 2) {
        return result;
    }

    size_t offset = sizeof(GlbHeader);
    const uint8_t* binChunkData = nullptr;
    size_t binChunkLength = 0;

    // Read chunks
    while (offset + sizeof(GlbChunkHeader) <= length) {
        GlbChunkHeader chunk;
        std::memcpy(&chunk, data + offset, sizeof(GlbChunkHeader));
        offset += sizeof(GlbChunkHeader);

        if (chunk.chunkType == 0x004E4942) { // "BIN\0"
            binChunkData = data + offset;
            binChunkLength = chunk.chunkLength;
            break;
        }
        offset += chunk.chunkLength;
    }

    if (binChunkData && binChunkLength >= 36) {
        // Extract raw vertex positions from binary buffer
        size_t floatCount = binChunkLength / sizeof(float);
        size_t vertCount = floatCount / 6; // pos + normal
        if (vertCount > 0) {
            result.positions.reserve(vertCount);
            result.normals.reserve(vertCount);
            const float* fData = reinterpret_cast<const float*>(binChunkData);
            for (size_t i = 0; i < vertCount; ++i) {
                Vec3 pos(fData[i * 6], fData[i * 6 + 1], fData[i * 6 + 2]);
                Vec3 norm(fData[i * 6 + 3], fData[i * 6 + 4], fData[i * 6 + 5]);
                result.positions.push_back(pos);
                result.normals.push_back(norm);
                result.aabb.expand(pos);
            }
            result.isValid = true;
        }
    }

    return result;
}

} // namespace custom_vulkan
