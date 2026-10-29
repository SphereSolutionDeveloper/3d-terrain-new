// ─── app/src/main/cpp/vulkan_asset_models.cpp ────────────────────────────────
#include "vulkan_asset_models.h"
#include <cmath>

namespace custom_vulkan {

void VulkanAssetModelFactory::appendBoxMesh(
    std::vector<EngineVertex>& vertices,
    std::vector<uint32_t>& indices,
    float cx, float cy, float cz,
    float sx, float sy, float sz,
    float r, float g, float b, float a
) {
    float hx = sx * 0.5f; float hy = sy * 0.5f; float hz = sz * 0.5f;
    uint32_t baseIdx = static_cast<uint32_t>(vertices.size());

    // 6 faces * 4 vertices = 24 vertices
    // +Z Face (Front)
    vertices.push_back({{cx - hx, cy - hy, cz + hz}, {0, 0, 1}, {0, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy - hy, cz + hz}, {0, 0, 1}, {1, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy + hy, cz + hz}, {0, 0, 1}, {1, 1}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy + hy, cz + hz}, {0, 0, 1}, {0, 1}, {r, g, b, a}});

    // -Z Face (Back)
    vertices.push_back({{cx + hx, cy - hy, cz - hz}, {0, 0, -1}, {0, 0}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy - hy, cz - hz}, {0, 0, -1}, {1, 0}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy + hy, cz - hz}, {0, 0, -1}, {1, 1}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy + hy, cz - hz}, {0, 0, -1}, {0, 1}, {r, g, b, a}});

    // +X Face (Right)
    vertices.push_back({{cx + hx, cy - hy, cz + hz}, {1, 0, 0}, {0, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy - hy, cz - hz}, {1, 0, 0}, {1, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy + hy, cz - hz}, {1, 0, 0}, {1, 1}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy + hy, cz + hz}, {1, 0, 0}, {0, 1}, {r, g, b, a}});

    // -X Face (Left)
    vertices.push_back({{cx - hx, cy - hy, cz - hz}, {-1, 0, 0}, {0, 0}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy - hy, cz + hz}, {-1, 0, 0}, {1, 0}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy + hy, cz + hz}, {-1, 0, 0}, {1, 1}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy + hy, cz - hz}, {-1, 0, 0}, {0, 1}, {r, g, b, a}});

    // +Y Face (Top)
    vertices.push_back({{cx - hx, cy + hy, cz + hz}, {0, 1, 0}, {0, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy + hy, cz + hz}, {0, 1, 0}, {1, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy + hy, cz - hz}, {0, 1, 0}, {1, 1}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy + hy, cz - hz}, {0, 1, 0}, {0, 1}, {r, g, b, a}});

    // -Y Face (Bottom)
    vertices.push_back({{cx - hx, cy - hy, cz - hz}, {0, -1, 0}, {0, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy - hy, cz - hz}, {0, -1, 0}, {1, 0}, {r, g, b, a}});
    vertices.push_back({{cx + hx, cy - hy, cz + hz}, {0, -1, 0}, {1, 1}, {r, g, b, a}});
    vertices.push_back({{cx - hx, cy - hy, cz + hz}, {0, -1, 0}, {0, 1}, {r, g, b, a}});

    for (int f = 0; f < 6; ++f) {
        uint32_t off = baseIdx + f * 4;
        indices.push_back(off + 0); indices.push_back(off + 1); indices.push_back(off + 2);
        indices.push_back(off + 0); indices.push_back(off + 2); indices.push_back(off + 3);
    }
}

GPUMeshBuffer VulkanAssetModelFactory::buildModularPowerPost(
    const std::string& postType,
    bool hasTransformer,
    bool hasStreetlight,
    bool streetlightOn,
    bool hasGuyWire
) {
    GPUMeshBuffer mesh;
    mesh.name = "vulkan_modular_post_" + postType;

    // 1. Concrete foundation pier
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 0.225f, 0.0f, 0.65f, 0.45f, 0.65f, 0.42f, 0.44f, 0.48f, 1.0f);
    // 2. Lower & upper utility mast shaft (Class-1 wood / steel)
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 3.25f, 0.0f, 0.42f, 6.0f, 0.42f, 0.36f, 0.28f, 0.22f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 8.75f, 0.0f, 0.34f, 5.5f, 0.34f, 0.39f, 0.30f, 0.24f, 1.0f);
    // 3. Galvanized steel crossarm channel (2.4m wide)
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 10.05f, 0.0f, 2.40f, 0.14f, 0.16f, 0.58f, 0.62f, 0.66f, 1.0f);
    // 4. Pin Insulators (Porcelain bell sheds)
    appendBoxMesh(mesh.vertices, mesh.indices, -1.0f, 10.35f, 0.0f, 0.16f, 0.45f, 0.16f, 0.48f, 0.24f, 0.18f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 11.55f, 0.0f, 0.16f, 0.45f, 0.16f, 0.48f, 0.24f, 0.18f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 1.0f, 10.35f, 0.0f, 0.16f, 0.45f, 0.16f, 0.48f, 0.24f, 0.18f, 1.0f);

    if (hasTransformer) {
        appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 8.0f, 0.45f, 0.55f, 0.90f, 0.55f, 0.28f, 0.32f, 0.36f, 1.0f);
    }
    if (hasStreetlight) {
        appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 7.8f, -1.8f, 0.32f, 0.14f, 0.55f, 0.20f, 0.22f, 0.25f, 1.0f);
        if (streetlightOn) {
            appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 7.72f, -1.8f, 0.28f, 0.04f, 0.48f, 1.0f, 0.94f, 0.45f, 1.0f);
        }
    }

    mesh.vertexCount = static_cast<uint32_t>(mesh.vertices.size());
    mesh.indexCount = static_cast<uint32_t>(mesh.indices.size());
    mesh.vmaAllocationBytes = mesh.vertices.size() * sizeof(EngineVertex) + mesh.indices.size() * sizeof(uint32_t);
    return mesh;
}

GPUMeshBuffer VulkanAssetModelFactory::buildContinuousCatenaryCables(
    const apex::Vec3D& poleA,
    const apex::Vec3D& poleB,
    float tensionKn,
    float linearMassKgM,
    float conductorRadiusM,
    uint32_t radialFacets,
    uint32_t curveSegments,
    bool isEnergized
) {
    GPUMeshBuffer buffer;
    buffer.name = "vulkan_continuous_cable_cylinder";

    float cr = isEnergized ? 0.95f : 0.45f;
    float cg = isEnergized ? 0.65f : 0.48f;
    float cb = isEnergized ? 0.18f : 0.52f;

    // Phase A (Left)
    apex::Vec3D spanDir = (poleB - poleA).normalized();
    apex::Vec3D lat(spanDir.z, 0.0f, -spanDir.x);
    apex::ContinuousCableBendingFunction curveA(poleA + lat * 1.0f + apex::Vec3D(0, 10.05f, 0), poleB + lat * 1.0f + apex::Vec3D(0, 10.05f, 0), tensionKn, linearMassKgM);
    apex::ContinuousCylinderMesh meshA(conductorRadiusM, radialFacets, curveSegments);
    meshA.generate(curveA, cr, cg, cb);

    // Phase B (Center Ridge - Higher)
    apex::ContinuousCableBendingFunction curveB(poleA + apex::Vec3D(0, 11.25f, 0), poleB + apex::Vec3D(0, 11.25f, 0), tensionKn, linearMassKgM);
    apex::ContinuousCylinderMesh meshB(conductorRadiusM, radialFacets, curveSegments);
    meshB.generate(curveB, cr, cg, cb);

    // Phase C (Right)
    apex::ContinuousCableBendingFunction curveC(poleA - lat * 1.0f + apex::Vec3D(0, 10.05f, 0), poleB - lat * 1.0f + apex::Vec3D(0, 10.05f, 0), tensionKn, linearMassKgM);
    apex::ContinuousCylinderMesh meshC(conductorRadiusM, radialFacets, curveSegments);
    meshC.generate(curveC, cr, cg, cb);

    auto appendCyl = [&](const apex::ContinuousCylinderMesh& cyl) {
        uint32_t base = static_cast<uint32_t>(buffer.vertices.size());
        for (const auto& v : cyl.vertices) {
            EngineVertex ev;
            ev.pos[0] = v.pos.x; ev.pos[1] = v.pos.y; ev.pos[2] = v.pos.z;
            ev.normal[0] = v.normal.x; ev.normal[1] = v.normal.y; ev.normal[2] = v.normal.z;
            ev.uv[0] = v.u; ev.uv[1] = v.v;
            ev.color[0] = v.r; ev.color[1] = v.g; ev.color[2] = v.b; ev.color[3] = v.a;
            buffer.vertices.push_back(ev);
        }
        for (uint32_t idx : cyl.indices) {
            buffer.indices.push_back(base + idx);
        }
    };

    appendCyl(meshA);
    appendCyl(meshB);
    appendCyl(meshC);

    buffer.vertexCount = static_cast<uint32_t>(buffer.vertices.size());
    buffer.indexCount = static_cast<uint32_t>(buffer.indices.size());
    buffer.vmaAllocationBytes = buffer.vertices.size() * sizeof(EngineVertex) + buffer.indices.size() * sizeof(uint32_t);
    return buffer;
}

std::vector<GPUMeshBuffer> VulkanAssetModelFactory::buildPowerGridNetwork(
    uint32_t numPosts,
    float spanDistanceM,
    float cornerAngleDeg,
    float tensionKn,
    float linearMassKgM,
    float conductorRadiusM,
    uint32_t radialFacets,
    bool isEnergized
) {
    std::vector<GPUMeshBuffer> network;

    // 1. Posts
    for (uint32_t i = 0; i < numPosts; ++i) {
        bool hasTrans = (i == 1);
        bool hasLight = (i % 2 == 0);
        GPUMeshBuffer post = buildModularPowerPost("POST_" + std::to_string(i), hasTrans, hasLight, true, false);
        // Translate post vertices
        float posX = (static_cast<float>(i) - static_cast<float>(numPosts) * 0.5f) * spanDistanceM;
        for (auto& v : post.vertices) {
            v.pos[0] += posX;
        }
        network.push_back(post);
    }

    // 2. Continuous Cables between posts
    for (uint32_t i = 0; i < (numPosts - 1); ++i) {
        float pAx = (static_cast<float>(i) - static_cast<float>(numPosts) * 0.5f) * spanDistanceM;
        float pBx = (static_cast<float>(i + 1) - static_cast<float>(numPosts) * 0.5f) * spanDistanceM;
        GPUMeshBuffer cables = buildContinuousCatenaryCables(
            apex::Vec3D(pAx, 0, 0), apex::Vec3D(pBx, 0, 0),
            tensionKn, linearMassKgM, conductorRadiusM, radialFacets, 24, isEnergized
        );
        network.push_back(cables);
    }

    return network;
}

GPUMeshBuffer VulkanAssetModelFactory::buildIndustrialGarage001(
    float bay1OpenPct, float bay2OpenPct, bool neonSignOn
) {
    GPUMeshBuffer mesh;
    mesh.name = "vulkan_industrial_garage_001";

    // 1. Concrete Foundation Floor Slab (12.4m x 0.2m x 16.4m)
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 0.10f, 0.0f, 12.4f, 0.20f, 16.4f, 0.23f, 0.26f, 0.30f, 1.0f);
    // Interior safety floor markings
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 0.21f, -2.85f, 6.0f, 0.02f, 3.6f, 1.0f, 0.85f, 0.2f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 0.21f, 2.85f, 6.0f, 0.02f, 3.6f, 1.0f, 0.85f, 0.2f, 1.0f);

    // 2. Standalone Garage House Hollow Exterior Walls
    // Rear Wall (East, X = +5.9m)
    appendBoxMesh(mesh.vertices, mesh.indices, 5.9f, 2.9f, 0.0f, 0.35f, 5.4f, 16.0f, 0.22f, 0.26f, 0.32f, 1.0f);
    // Left Side Wall (North, Z = +7.9m)
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 2.9f, 7.9f, 12.0f, 5.4f, 0.35f, 0.22f, 0.26f, 0.32f, 1.0f);
    // Right Side Wall (South, Z = -7.9m)
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 2.9f, -7.9f, 12.0f, 5.4f, 0.35f, 0.22f, 0.26f, 0.32f, 1.0f);

    // Front Entrance Portal (West, X = -5.9m) with doorway openings
    appendBoxMesh(mesh.vertices, mesh.indices, -5.9f, 2.9f, 6.4f, 0.35f, 5.4f, 3.0f, 0.22f, 0.26f, 0.32f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -5.9f, 2.9f, -6.4f, 0.35f, 5.4f, 3.0f, 0.22f, 0.26f, 0.32f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -5.9f, 2.9f, 0.0f, 0.35f, 5.4f, 0.8f, 0.27f, 0.35f, 0.39f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -5.9f, 4.85f, 0.0f, 0.35f, 1.5f, 16.0f, 0.22f, 0.26f, 0.32f, 1.0f);

    // 3. Roof Slab & Crimson Trim & Ceiling Trusses
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 5.65f, 0.0f, 12.6f, 0.25f, 16.6f, 0.12f, 0.14f, 0.17f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 0.0f, 5.85f, 0.0f, 13.0f, 0.20f, 17.0f, 0.85f, 0.22f, 0.22f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -2.0f, 5.35f, 0.0f, 0.25f, 0.35f, 15.4f, 0.27f, 0.35f, 0.39f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 2.0f, 5.35f, 0.0f, 0.25f, 0.35f, 15.4f, 0.27f, 0.35f, 0.39f, 1.0f);

    // Facade Signboard
    float sr = neonSignOn ? 0.95f : 0.27f;
    float sg = neonSignOn ? 0.65f : 0.35f;
    float sb = neonSignOn ? 0.18f : 0.39f;
    appendBoxMesh(mesh.vertices, mesh.indices, -6.15f, 4.95f, 0.0f, 0.12f, 0.85f, 8.0f, sr, sg, sb, 1.0f);

    // 4. Modular Articulated Door Assembly (Bay 1 & Bay 2)
    // Tracks
    appendBoxMesh(mesh.vertices, mesh.indices, -5.75f, 2.05f, -4.95f, 0.08f, 3.9f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -5.75f, 2.05f, -0.75f, 0.08f, 3.9f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -3.2f, 4.05f, -4.95f, 5.1f, 0.08f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -3.2f, 4.05f, -0.75f, 5.1f, 0.08f, 0.08f, 0.47f, 0.56f, 0.61f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -5.7f, 4.2f, -2.85f, 0.14f, 0.14f, 4.1f, 0.22f, 0.28f, 0.31f, 1.0f);

    // Bay 1 Modular Door Panels (Retract along overhead ceiling track when opened)
    float travel1 = (bay1OpenPct < 0.0f ? 0.0f : (bay1OpenPct > 1.0f ? 1.0f : bay1OpenPct)) * 3.8f;
    for (int k = 1; k <= 4; ++k) {
        float baseY = 0.65f + static_cast<float>(k - 1) * 0.95f;
        float curPos = baseY + travel1;
        if (curPos <= 3.8f) {
            appendBoxMesh(mesh.vertices, mesh.indices, -5.85f, curPos, -2.85f, 0.10f, 0.88f, 4.1f, 0.70f, 0.74f, 0.78f, 1.0f);
        } else {
            float overshoot = curPos - 3.8f;
            appendBoxMesh(mesh.vertices, mesh.indices, -5.85f + overshoot, 4.12f, -2.85f, 0.88f, 0.10f, 4.1f, 0.65f, 0.70f, 0.75f, 1.0f);
        }
    }

    // Bay 2 Modular Door Panels
    float travel2 = (bay2OpenPct < 0.0f ? 0.0f : (bay2OpenPct > 1.0f ? 1.0f : bay2OpenPct)) * 3.8f;
    for (int k = 1; k <= 4; ++k) {
        float baseY = 0.65f + static_cast<float>(k - 1) * 0.95f;
        float curPos = baseY + travel2;
        if (curPos <= 3.8f) {
            appendBoxMesh(mesh.vertices, mesh.indices, -5.85f, curPos, 2.85f, 0.10f, 0.88f, 4.1f, 0.70f, 0.74f, 0.78f, 1.0f);
        } else {
            float overshoot = curPos - 3.8f;
            appendBoxMesh(mesh.vertices, mesh.indices, -5.85f + overshoot, 4.12f, 2.85f, 0.88f, 0.10f, 4.1f, 0.65f, 0.70f, 0.75f, 1.0f);
        }
    }

    // 5. DETAILED WORKSHOP INTERIOR (VISIBLE INSIDE THROUGH OPEN DOOR!)
    // Hydraulic 2-Post Vehicle Hoist
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 2.1f, -4.3f, 0.35f, 4.0f, 0.35f, 0.08f, 0.40f, 0.75f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 2.1f, -1.4f, 0.35f, 4.0f, 0.35f, 0.08f, 0.40f, 0.75f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 4.15f, -2.85f, 0.25f, 0.20f, 3.1f, 0.05f, 0.28f, 0.63f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 0.95f, -2.85f, 2.4f, 0.16f, 1.8f, 1.0f, 0.70f, 0.0f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 2.0f, -4.55f, 0.22f, 0.75f, 0.22f, 0.13f, 0.13f, 0.13f, 1.0f);

    // Mechanic Workbench & Tool Pegboard
    appendBoxMesh(mesh.vertices, mesh.indices, 4.8f, 0.55f, -2.85f, 1.2f, 0.9f, 3.6f, 0.15f, 0.20f, 0.22f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 4.8f, 1.05f, -2.85f, 1.35f, 0.10f, 3.8f, 0.55f, 0.43f, 0.39f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 5.6f, 1.95f, -2.85f, 0.08f, 1.6f, 3.6f, 0.38f, 0.49f, 0.55f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 4.2f, 1.22f, -4.2f, 0.25f, 0.25f, 0.25f, 0.78f, 0.16f, 0.16f, 1.0f);

    // Rolling Multi-Drawer Tool Cabinet
    appendBoxMesh(mesh.vertices, mesh.indices, 4.5f, 0.75f, 0.0f, 0.9f, 1.3f, 1.1f, 0.83f, 0.18f, 0.18f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 4.0f, 0.75f, 0.0f, 0.06f, 1.2f, 0.9f, 0.81f, 0.85f, 0.86f, 1.0f);

    // Industrial Pallet Racking & Shelves & Oil Drums
    appendBoxMesh(mesh.vertices, mesh.indices, 1.5f, 1.8f, 7.1f, 4.5f, 3.4f, 0.75f, 0.27f, 0.35f, 0.39f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 1.5f, 1.3f, 7.1f, 4.4f, 0.08f, 0.7f, 0.94f, 0.42f, 0.0f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 2.0f, 1.6f, 7.1f, 1.4f, 0.5f, 0.6f, 0.22f, 0.28f, 0.31f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -2.5f, 0.65f, 7.1f, 0.75f, 1.1f, 0.75f, 0.05f, 0.28f, 0.63f, 1.0f);

    // Wall-Mounted Tire Storage Rack
    appendBoxMesh(mesh.vertices, mesh.indices, 2.0f, 2.4f, -7.2f, 3.0f, 0.5f, 0.65f, 0.22f, 0.28f, 0.31f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 2.0f, 2.5f, -7.2f, 2.8f, 0.75f, 0.6f, 0.13f, 0.13f, 0.13f, 1.0f);

    // Overhead High-Bay LED Shop Light Fixtures
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 5.25f, -2.85f, 0.5f, 0.12f, 2.6f, 1.0f, 0.98f, 0.77f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -0.5f, 5.25f, 2.85f, 0.5f, 0.12f, 2.6f, 1.0f, 0.98f, 0.77f, 1.0f);

    mesh.vertexCount = static_cast<uint32_t>(mesh.vertices.size());
    mesh.indexCount = static_cast<uint32_t>(mesh.indices.size());
    mesh.vmaAllocationBytes = mesh.vertices.size() * sizeof(EngineVertex) + mesh.indices.size() * sizeof(uint32_t);
    return mesh;
}

GPUMeshBuffer VulkanAssetModelFactory::buildHeavyTransportVehicle(
    bool headlightsOn, bool taillightsOn
) {
    GPUMeshBuffer mesh;
    mesh.name = "vulkan_heavy_transport_lorry";
    // Heavy chassis frame rails & engine block
    appendBoxMesh(mesh.vertices, mesh.indices, 0, 0.85f, 0, 6.8f, 0.35f, 2.1f, 0.22f, 0.24f, 0.26f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 2.2f, 1.3f, 0, 1.8f, 1.1f, 1.6f, 0.28f, 0.30f, 0.34f, 1.0f);
    // Reinforced driver cab
    appendBoxMesh(mesh.vertices, mesh.indices, 1.8f, 2.4f, 0, 2.4f, 1.8f, 2.3f, 0.12f, 0.55f, 0.70f, 1.0f);
    // High-cube cargo container box
    appendBoxMesh(mesh.vertices, mesh.indices, -1.2f, 2.4f, 0, 4.8f, 2.4f, 2.4f, 0.85f, 0.50f, 0.15f, 1.0f);
    // 6 heavy-duty wheels
    appendBoxMesh(mesh.vertices, mesh.indices, 2.0f, 0.55f, 1.15f, 0.9f, 0.9f, 0.35f, 0.12f, 0.12f, 0.12f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, 2.0f, 0.55f, -1.15f, 0.9f, 0.9f, 0.35f, 0.12f, 0.12f, 0.12f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -1.6f, 0.55f, 1.15f, 0.9f, 0.9f, 0.35f, 0.12f, 0.12f, 0.12f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -1.6f, 0.55f, -1.15f, 0.9f, 0.9f, 0.35f, 0.12f, 0.12f, 0.12f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -2.7f, 0.55f, 1.15f, 0.9f, 0.9f, 0.35f, 0.12f, 0.12f, 0.12f, 1.0f);
    appendBoxMesh(mesh.vertices, mesh.indices, -2.7f, 0.55f, -1.15f, 0.9f, 0.9f, 0.35f, 0.12f, 0.12f, 0.12f, 1.0f);

    mesh.vertexCount = static_cast<uint32_t>(mesh.vertices.size());
    mesh.indexCount = static_cast<uint32_t>(mesh.indices.size());
    mesh.vmaAllocationBytes = mesh.vertices.size() * sizeof(EngineVertex) + mesh.indices.size() * sizeof(uint32_t);
    return mesh;
}

} // namespace custom_vulkan
