// ==============================================================================
// Shared Vulkan 3D Solid Geometry Primitives & Tangent-Space PBR Helpers
// Path: native-assets/src/road/common/RoadMeshPrimitives.h
// Architecture: Sascha Willems (SaschaWillems/Vulkan) & Khronos glTF 2.0 PBR
// ==============================================================================
#pragma once

#include <cmath>
#include <cstdint>
#include <algorithm>

namespace ApexAssets {
namespace Road {
namespace Primitives {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float DEG2RAD = PI / 180.0f;

template <typename VertexT>
inline void ComputeTangentForQuad(
    const float c0[3], const float c1[3], const float c3[3],
    const float norm[3], float outTangent[4]
) {
    float ex = c1[0] - c0[0];
    float ey = c1[1] - c0[1];
    float ez = c1[2] - c0[2];
    float len = std::sqrt(ex * ex + ey * ey + ez * ez);
    if (len > 1e-5f) {
        ex /= len; ey /= len; ez /= len;
    } else {
        ex = 1.0f; ey = 0.0f; ez = 0.0f;
    }
    // Gram-Schmidt orthogonalize against normal
    float ndote = norm[0] * ex + norm[1] * ey + norm[2] * ez;
    ex -= norm[0] * ndote;
    ey -= norm[1] * ndote;
    ez -= norm[2] * ndote;
    float oLen = std::sqrt(ex * ex + ey * ey + ez * ez);
    if (oLen > 1e-5f) {
        ex /= oLen; ey /= oLen; ez /= oLen;
    }
    (void)c3;
    outTangent[0] = ex;
    outTangent[1] = ey;
    outTangent[2] = ez;
    outTangent[3] = 1.0f;
}

template <typename BufferT, typename VertexT>
inline void AddQuad(
    BufferT& buffer,
    const float c0[3], const float c1[3], const float c2[3], const float c3[3],
    const float norm[3],
    uint32_t materialId,
    float r, float g, float b, float a = 1.0f,
    float u0 = 0.0f, float u1 = 1.0f,
    float v0 = 0.0f, float v1 = 1.0f,
    float roughness = 0.55f, float metallic = 0.1f, float emissive = 0.0f
) {
    uint32_t sIdx = static_cast<uint32_t>(buffer.vertices.size());
    float tangent[4];
    ComputeTangentForQuad<VertexT>(c0, c1, c3, norm, tangent);

    const float* corners[4] = { c0, c1, c2, c3 };
    for (int i = 0; i < 4; ++i) {
        VertexT v{};
        v.pos[0] = corners[i][0]; v.pos[1] = corners[i][1]; v.pos[2] = corners[i][2];
        v.normal[0] = norm[0]; v.normal[1] = norm[1]; v.normal[2] = norm[2];
        v.uv[0] = (i == 1 || i == 2) ? u1 : u0;
        v.uv[1] = (i >= 2) ? v1 : v0;
        v.tangent[0] = tangent[0]; v.tangent[1] = tangent[1];
        v.tangent[2] = tangent[2]; v.tangent[3] = tangent[3];
        v.color[0] = r; v.color[1] = g; v.color[2] = b; v.color[3] = a;
        v.pbrParams[0] = roughness; v.pbrParams[1] = metallic;
        v.pbrParams[2] = 1.0f;      v.pbrParams[3] = emissive;
        v.materialId = materialId;
        buffer.vertices.push_back(v);
    }
    buffer.indices.push_back(sIdx + 0); buffer.indices.push_back(sIdx + 1); buffer.indices.push_back(sIdx + 2);
    buffer.indices.push_back(sIdx + 0); buffer.indices.push_back(sIdx + 2); buffer.indices.push_back(sIdx + 3);
}

template <typename BufferT, typename VertexT>
inline void AddOrientedBox(
    BufferT& buffer,
    float cx, float cy, float cz,
    float dimNormal, float dimVertical, float dimTangent,
    float nx, float nz,
    float tx, float tz,
    uint32_t materialId,
    float r, float g, float b, float a = 1.0f,
    float roughness = 0.55f, float metallic = 0.1f, float emissive = 0.0f
) {
    float hn = dimNormal * 0.5f;
    float hy = dimVertical * 0.5f;
    float ht = dimTangent * 0.5f;

    float nLen = std::sqrt(nx * nx + nz * nz);
    if (nLen > 1e-5f) { nx /= nLen; nz /= nLen; } else { nx = 1.0f; nz = 0.0f; }

    float tLen = std::sqrt(tx * tx + tz * tz);
    if (tLen > 1e-5f) { tx /= tLen; tz /= tLen; } else { tx = 0.0f; tz = 1.0f; }

    float corners[8][3];
    const float signs[8][3] = {
        {-1.0f, -1.0f, -1.0f},
        { 1.0f, -1.0f, -1.0f},
        { 1.0f,  1.0f, -1.0f},
        {-1.0f,  1.0f, -1.0f},
        {-1.0f, -1.0f,  1.0f},
        { 1.0f, -1.0f,  1.0f},
        { 1.0f,  1.0f,  1.0f},
        {-1.0f,  1.0f,  1.0f}
    };

    for (int i = 0; i < 8; ++i) {
        corners[i][0] = cx + signs[i][0] * hn * nx + signs[i][2] * ht * tx;
        corners[i][1] = cy + signs[i][1] * hy;
        corners[i][2] = cz + signs[i][0] * hn * nz + signs[i][2] * ht * tz;
    }

    struct Face { float norm[3]; int c[4]; };
    Face faces[6] = {
        {{ tx,  0.0f,  tz}, {4, 7, 6, 5}}, // Front (+T)
        {{-tx,  0.0f, -tz}, {0, 1, 2, 3}}, // Back (-T)
        {{0.0f, 1.0f, 0.0f}, {3, 2, 6, 7}}, // Top (+Y)
        {{0.0f,-1.0f, 0.0f}, {0, 4, 5, 1}}, // Bottom (-Y)
        {{-nx,  0.0f, -nz}, {3, 7, 4, 0}}, // Left (-N)
        {{ nx,  0.0f,  nz}, {2, 1, 5, 6}}  // Right (+N)
    };

    for (int f = 0; f < 6; ++f) {
        AddQuad<BufferT, VertexT>(
            buffer,
            corners[faces[f].c[0]], corners[faces[f].c[1]],
            corners[faces[f].c[2]], corners[faces[f].c[3]],
            faces[f].norm, materialId,
            r, g, b, a,
            0.0f, 1.0f, 0.0f, 1.0f,
            roughness, metallic, emissive
        );
    }
}

template <typename BufferT, typename VertexT>
inline void AddBox(
    BufferT& buffer,
    float cx, float cy, float cz,
    float sx, float sy, float sz,
    uint32_t materialId,
    float r, float g, float b, float a = 1.0f,
    float roughness = 0.55f, float metallic = 0.1f, float emissive = 0.0f
) {
    AddOrientedBox<BufferT, VertexT>(
        buffer, cx, cy, cz, sx, sy, sz,
        1.0f, 0.0f, 0.0f, 1.0f,
        materialId, r, g, b, a, roughness, metallic, emissive
    );
}

template <typename BufferT, typename VertexT>
inline void AddBeam(
    BufferT& buffer,
    float x1, float y1, float z1,
    float x2, float y2, float z2,
    float width, float height,
    uint32_t materialId,
    float r, float g, float b, float a = 1.0f,
    float roughness = 0.35f, float metallic = 0.75f, float emissive = 0.0f
) {
    float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
    float len = std::sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 1e-4f) return;

    dx /= len; dy /= len; dz /= len;

    float upX = 0.0f, upY = 1.0f, upZ = 0.0f;
    if (std::abs(dy) > 0.95f) { upX = 1.0f; upY = 0.0f; upZ = 0.0f; }

    float rx = dy * upZ - dz * upY;
    float ry = dz * upX - dx * upZ;
    float rz = dx * upY - dy * upX;
    float rLen = std::sqrt(rx * rx + ry * ry + rz * rz);
    if (rLen > 1e-5f) { rx /= rLen; ry /= rLen; rz /= rLen; }

    float ux = ry * dz - rz * dy;
    float uy = rz * dx - rx * dz;
    float uz = rx * dy - ry * dx;

    float hw = width * 0.5f;
    float hh = height * 0.5f;

    float c1[4][3], c2[4][3];
    const float signs[4][2] = {{-1.0f, -1.0f}, {1.0f, -1.0f}, {1.0f, 1.0f}, {-1.0f, 1.0f}};
    for (int i = 0; i < 4; ++i) {
        float offX = signs[i][0] * hw * rx + signs[i][1] * hh * ux;
        float offY = signs[i][0] * hw * ry + signs[i][1] * hh * uy;
        float offZ = signs[i][0] * hw * rz + signs[i][1] * hh * uz;
        c1[i][0] = x1 + offX; c1[i][1] = y1 + offY; c1[i][2] = z1 + offZ;
        c2[i][0] = x2 + offX; c2[i][1] = y2 + offY; c2[i][2] = z2 + offZ;
    }

    for (int side = 0; side < 4; ++side) {
        int next = (side + 1) % 4;
        float e1x = c1[next][0] - c1[side][0], e1y = c1[next][1] - c1[side][1], e1z = c1[next][2] - c1[side][2];
        float e2x = c2[side][0] - c1[side][0], e2y = c2[side][1] - c1[side][1], e2z = c2[side][2] - c1[side][2];
        float fnx = e1y * e2z - e1z * e2y;
        float fny = e1z * e2x - e1x * e2z;
        float fnz = e1x * e2y - e1y * e2x;
        float fnLen = std::sqrt(fnx * fnx + fny * fny + fnz * fnz);
        if (fnLen > 1e-5f) { fnx /= fnLen; fny /= fnLen; fnz /= fnLen; }
        float fn[3] = {fnx, fny, fnz};

        AddQuad<BufferT, VertexT>(
            buffer,
            c1[side], c1[next], c2[next], c2[side],
            fn, materialId, r, g, b, a,
            0.0f, 1.0f, 0.0f, 1.0f, roughness, metallic, emissive
        );
    }

    float nCap1[3] = {-dx, -dy, -dz};
    AddQuad<BufferT, VertexT>(buffer, c1[0], c1[3], c1[2], c1[1], nCap1, materialId, r, g, b, a, 0.0f, 1.0f, 0.0f, 1.0f, roughness, metallic, emissive);
    float nCap2[3] = {dx, dy, dz};
    AddQuad<BufferT, VertexT>(buffer, c2[0], c2[1], c2[2], c2[3], nCap2, materialId, r, g, b, a, 0.0f, 1.0f, 0.0f, 1.0f, roughness, metallic, emissive);
}

template <typename BufferT, typename VertexT>
inline void AddCylinder(
    BufferT& buffer,
    float x1, float y1, float z1,
    float x2, float y2, float z2,
    float radiusBottom, float radiusTop,
    int slices,
    uint32_t materialId,
    float r, float g, float b, float a = 1.0f,
    float roughness = 0.50f, float metallic = 0.25f, float emissive = 0.0f
) {
    float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
    float len = std::sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 1e-4f || slices < 4) return;
    dx /= len; dy /= len; dz /= len;

    float upX = 0.0f, upY = 1.0f, upZ = 0.0f;
    if (std::abs(dy) > 0.95f) { upX = 1.0f; upY = 0.0f; upZ = 0.0f; }

    float rx = dy * upZ - dz * upY;
    float ry = dz * upX - dx * upZ;
    float rz = dx * upY - dy * upX;
    float rLen = std::sqrt(rx * rx + ry * ry + rz * rz);
    if (rLen > 1e-5f) { rx /= rLen; ry /= rLen; rz /= rLen; }

    float ux = ry * dz - rz * dy;
    float uy = rz * dx - rx * dz;
    float uz = rx * dy - ry * dx;

    for (int s = 0; s < slices; ++s) {
        float a0 = (static_cast<float>(s) / slices) * 2.0f * PI;
        float a1 = (static_cast<float>(s + 1) / slices) * 2.0f * PI;
        float c0 = std::cos(a0), s0 = std::sin(a0);
        float c1 = std::cos(a1), s1 = std::sin(a1);

        float p00[3] = { x1 + (c0 * rx + s0 * ux) * radiusBottom, y1 + (c0 * ry + s0 * uy) * radiusBottom, z1 + (c0 * rz + s0 * uz) * radiusBottom };
        float p01[3] = { x1 + (c1 * rx + s1 * ux) * radiusBottom, y1 + (c1 * ry + s1 * uy) * radiusBottom, z1 + (c1 * rz + s1 * uz) * radiusBottom };
        float p11[3] = { x2 + (c1 * rx + s1 * ux) * radiusTop,    y2 + (c1 * ry + s1 * uy) * radiusTop,    z2 + (c1 * rz + s1 * uz) * radiusTop };
        float p10[3] = { x2 + (c0 * rx + s0 * ux) * radiusTop,    y2 + (c0 * ry + s0 * uy) * radiusTop,    z2 + (c0 * rz + s0 * uz) * radiusTop };

        float midA = 0.5f * (a0 + a1);
        float nx = std::cos(midA) * rx + std::sin(midA) * ux;
        float ny = std::cos(midA) * ry + std::sin(midA) * uy;
        float nz = std::cos(midA) * rz + std::sin(midA) * uz;
        float nVec[3] = {nx, ny, nz};

        AddQuad<BufferT, VertexT>(
            buffer, p00, p01, p11, p10,
            nVec, materialId, r, g, b, a,
            static_cast<float>(s) / slices, static_cast<float>(s + 1) / slices,
            0.0f, 1.0f, roughness, metallic, emissive
        );
    }
}

template <typename BufferT>
inline void ComputeMeshAABB(BufferT& buffer) {
    if (buffer.vertices.empty()) {
        for (int i = 0; i < 3; ++i) { buffer.minAABB[i] = 0.0f; buffer.maxAABB[i] = 0.0f; }
        return;
    }
    buffer.minAABB[0] = buffer.maxAABB[0] = buffer.vertices[0].pos[0];
    buffer.minAABB[1] = buffer.maxAABB[1] = buffer.vertices[0].pos[1];
    buffer.minAABB[2] = buffer.maxAABB[2] = buffer.vertices[0].pos[2];
    for (const auto& v : buffer.vertices) {
        buffer.minAABB[0] = std::min(buffer.minAABB[0], v.pos[0]);
        buffer.minAABB[1] = std::min(buffer.minAABB[1], v.pos[1]);
        buffer.minAABB[2] = std::min(buffer.minAABB[2], v.pos[2]);
        buffer.maxAABB[0] = std::max(buffer.maxAABB[0], v.pos[0]);
        buffer.maxAABB[1] = std::max(buffer.maxAABB[1], v.pos[1]);
        buffer.maxAABB[2] = std::max(buffer.maxAABB[2], v.pos[2]);
    }
}

} // namespace Primitives
} // namespace Road
} // namespace ApexAssets
