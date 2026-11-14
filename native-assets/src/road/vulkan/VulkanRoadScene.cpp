// ==============================================================================
// Vulkan 1.3 Road Scene Engine & Modular Component Assembler Implementation
// Path: native-assets/src/road/vulkan/VulkanRoadScene.cpp
// Follows Sascha Willems Vulkan Framework (SaschaWillems/Vulkan)
// ==============================================================================
#include "VulkanRoadScene.h"
#include "../common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>

namespace ApexAssets {
namespace Road {

VulkanRoadScene::VulkanRoadScene() {
    m_specs.apiVersion = "Vulkan 1.3.268 (Core Dynamic Rendering)";
    m_specs.shaderModel = "GLSL 4.60 SPIR-V (vk::ShaderModule)";
    m_specs.polygonMode = "VK_POLYGON_MODE_FILL";
    m_specs.msaaSamples = "VK_SAMPLE_COUNT_4_BIT";
    m_specs.dynamicRenderingEnabled = true;
    m_specs.pipelineCaching = true;
    m_specs.validationLayers = true;
}

VulkanPipelineSpecs VulkanRoadScene::GetPipelineSpecs() const {
    return m_specs;
}

template <typename SrcVertex>
static VulkanSceneVertex ToSceneVertex(const SrcVertex& src, float offsetX, float offsetY, float offsetZ) {
    VulkanSceneVertex sv{};
    sv.pos[0] = src.pos[0] + offsetX;
    sv.pos[1] = src.pos[1] + offsetY;
    sv.pos[2] = src.pos[2] + offsetZ;
    sv.normal[0] = src.normal[0];
    sv.normal[1] = src.normal[1];
    sv.normal[2] = src.normal[2];
    sv.uv[0] = src.uv[0];
    sv.uv[1] = src.uv[1];
    sv.tangent[0] = src.tangent[0];
    sv.tangent[1] = src.tangent[1];
    sv.tangent[2] = src.tangent[2];
    sv.tangent[3] = src.tangent[3];
    sv.color[0] = src.color[0];
    sv.color[1] = src.color[1];
    sv.color[2] = src.color[2];
    sv.color[3] = src.color[3];
    sv.pbrParams[0] = src.pbrParams[0];
    sv.pbrParams[1] = src.pbrParams[1];
    sv.pbrParams[2] = src.pbrParams[2];
    sv.pbrParams[3] = src.pbrParams[3];
    sv.materialId = src.materialId;
    return sv;
}

static void AppendCorridorRampSegment(
    VulkanAssembledScene& scene,
    float x0, float y0, float z0, float nx0, float nz0,
    float x1, float y1, float z1, float nx1, float nz1,
    float halfW, float depth, bool addPierAtStart
) {
    float botW = halfW * 0.65f;
    float faciaH = 0.38f;
    float tl0[3] = {x0 - nx0 * halfW, y0, z0 - nz0 * halfW};
    float tr0[3] = {x0 + nx0 * halfW, y0, z0 + nz0 * halfW};
    float tl1[3] = {x1 - nx1 * halfW, y1, z1 - nz1 * halfW};
    float tr1[3] = {x1 + nx1 * halfW, y1, z1 + nz1 * halfW};

    float fl0[3] = {tl0[0], y0 - faciaH, tl0[2]};
    float fr0[3] = {tr0[0], y0 - faciaH, tr0[2]};
    float fl1[3] = {tl1[0], y1 - faciaH, tl1[2]};
    float fr1[3] = {tr1[0], y1 - faciaH, tr1[2]};

    float bl0[3] = {x0 - nx0 * botW, std::max(-0.5f, y0 - depth), z0 - nz0 * botW};
    float br0[3] = {x0 + nx0 * botW, std::max(-0.5f, y0 - depth), z0 + nz0 * botW};
    float bl1[3] = {x1 - nx1 * botW, std::max(-0.5f, y1 - depth), z1 - nz1 * botW};
    float br1[3] = {x1 + nx1 * botW, std::max(-0.5f, y1 - depth), z1 + nz1 * botW};

    float nUp[3] = {0.0f, 1.0f, 0.0f};
    Primitives::AddQuad<VulkanAssembledScene, VulkanSceneVertex>(
        scene, tl0, tr0, tr1, tl1, nUp, 0, 0.20f, 0.22f, 0.25f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, 0.85f, 0.02f, 0.0f
    );
    // Yellow median centerline
    float c0[3] = {x0 - nx0 * 0.14f, y0 + 0.025f, z0 - nz0 * 0.14f};
    float c1[3] = {x0 + nx0 * 0.14f, y0 + 0.025f, z0 + nz0 * 0.14f};
    float c2[3] = {x1 + nx1 * 0.14f, y1 + 0.025f, z1 + nz1 * 0.14f};
    float c3[3] = {x1 - nx1 * 0.14f, y1 + 0.025f, z1 - nz1 * 0.14f};
    Primitives::AddQuad<VulkanAssembledScene, VulkanSceneVertex>(
        scene, c0, c1, c2, c3, nUp, 3, 0.96f, 0.78f, 0.14f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f, 0.45f, 0.05f, 0.0f
    );

    float nL[3] = {-nx0, 0.0f, -nz0};
    float nR[3] = { nx0, 0.0f,  nz0};
    float nDown[3] = {0.0f, -1.0f, 0.0f};
    Primitives::AddQuad<VulkanAssembledScene, VulkanSceneVertex>(
        scene, tl0, tl1, fl1, fl0, nL, 1, 0.68f, 0.71f, 0.75f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.68f, 0.04f, 0.0f
    );
    Primitives::AddQuad<VulkanAssembledScene, VulkanSceneVertex>(
        scene, tr0, fr0, fr1, tr1, nR, 1, 0.68f, 0.71f, 0.75f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.68f, 0.04f, 0.0f
    );
    Primitives::AddQuad<VulkanAssembledScene, VulkanSceneVertex>(
        scene, fl0, fl1, bl1, bl0, nL, 1, 0.56f, 0.59f, 0.63f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.72f, 0.04f, 0.0f
    );
    Primitives::AddQuad<VulkanAssembledScene, VulkanSceneVertex>(
        scene, fr0, br0, br1, fr1, nR, 1, 0.56f, 0.59f, 0.63f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.72f, 0.04f, 0.0f
    );
    Primitives::AddQuad<VulkanAssembledScene, VulkanSceneVertex>(
        scene, bl0, bl1, br1, br0, nDown, 1, 0.46f, 0.49f, 0.54f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.74f, 0.04f, 0.0f
    );

    if (addPierAtStart && y0 > 2.0f) {
        float seatY = y0 - depth;
        float tx = -nz0, tz = nx0;
        Primitives::AddOrientedBox<VulkanAssembledScene, VulkanSceneVertex>(
            scene, x0, 0.7f, z0, halfW * 1.4f, 1.4f, 4.2f,
            nx0, nz0, tx, tz, 1, 0.50f, 0.53f, 0.57f, 1.0f, 0.72f, 0.04f, 0.0f
        );
        for (int side : {-1, 1}) {
            float sx = x0 + nx0 * (halfW * 0.48f * side);
            float sz = z0 + nz0 * (halfW * 0.48f * side);
            Primitives::AddCylinder<VulkanAssembledScene, VulkanSceneVertex>(
                scene, sx, 1.3f, sz, sx, seatY, sz,
                0.95f, 0.80f, 10, 1, 0.66f, 0.70f, 0.74f, 1.0f, 0.66f, 0.04f, 0.0f
            );
        }
        Primitives::AddOrientedBox<VulkanAssembledScene, VulkanSceneVertex>(
            scene, x0, seatY - 0.55f, z0, halfW * 1.55f, 1.1f, 2.8f,
            nx0, nz0, tx, tz, 1, 0.62f, 0.65f, 0.69f, 1.0f, 0.68f, 0.04f, 0.0f
        );
    }
}

VulkanAssembledScene VulkanRoadScene::AssembleFullConnectedScene(
    const BridgeParams& bridgeParams,
    const TollParams& tollParams,
    const TunnelParams& tunnelParams,
    const InterchangeParams& interchangeParams,
    const RetainingWallParams& wallParams
) {
    (void)interchangeParams;
    VulkanAssembledScene scene;

    float tollCenterZ = -35.0f;
    float tollExitZ = tollCenterZ + tollParams.plazaLengthM * 0.5f; // e.g. -3.0m

    // 1. Approach Highway Roadway & Twin Flanking MSE Retaining Walls (-115m -> Toll Entrance)
    float approachStartZ = tollCenterZ - tollParams.plazaLengthM * 0.5f - wallParams.wallLengthM;
    float approachEndZ = tollCenterZ - tollParams.plazaLengthM * 0.5f;
    float approachCenterZ = 0.5f * (approachStartZ + approachEndZ);

    for (int s = 0; s < 10; ++s) {
        float t0 = static_cast<float>(s) / 10.0f;
        float t1 = static_cast<float>(s + 1) / 10.0f;
        float z0 = approachStartZ + t0 * (approachEndZ - approachStartZ);
        float z1 = approachStartZ + t1 * (approachEndZ - approachStartZ);
        AppendCorridorRampSegment(scene, 0.0f, 0.02f, z0, 1.0f, 0.0f,
                                  0.0f, 0.02f, z1, 1.0f, 0.0f,
                                  bridgeParams.deckWidthM * 0.52f, 0.65f, false);
    }

    WallMeshBuffer wallBuf = RetainingWallGen::GenerateWallMesh(wallParams);
    scene.wallVertexOffset = static_cast<uint32_t>(scene.vertices.size());
    scene.wallIndexOffset = static_cast<uint32_t>(scene.indices.size());
    // Left Retaining Wall flanking approach highway
    for (const auto& wv : wallBuf.vertices) {
        scene.vertices.push_back(ToSceneVertex(wv, -14.5f, 0.0f, approachCenterZ));
    }
    for (uint32_t idx : wallBuf.indices) {
        scene.indices.push_back(scene.wallVertexOffset + idx);
    }
    // Right Retaining Wall mirrored across highway centerline (+X)
    uint32_t rightWallOffset = static_cast<uint32_t>(scene.vertices.size());
    for (const auto& wv : wallBuf.vertices) {
        VulkanSceneVertex sv = ToSceneVertex(wv, 14.5f, 0.0f, approachCenterZ);
        sv.pos[0] = 14.5f - wv.pos[0];
        sv.normal[0] = -wv.normal[0];
        scene.vertices.push_back(sv);
    }
    for (size_t i = 0; i + 2 < wallBuf.indices.size(); i += 3) {
        scene.indices.push_back(rightWallOffset + wallBuf.indices[i + 0]);
        scene.indices.push_back(rightWallOffset + wallBuf.indices[i + 2]);
        scene.indices.push_back(rightWallOffset + wallBuf.indices[i + 1]);
    }

    // 1b. Overhead Full-Span Warren Truss Sign Gantry leading into Toll Plaza
    SignpostParams signP;
    signP.type = SignpostType::FullSpanTrussGantry;
    signP.gantrySpanWidthM = bridgeParams.deckWidthM + 2.4f;
    signP.clearanceHeightM = 5.8f;
    signP.signBoardWidthM = 6.2f;
    signP.signCount = 3;
    signP.hasEmissiveDMS = true;
    SignpostMeshBuffer gantryBuf = SignpostGenerator::GenerateSignpostMesh(signP);
    uint32_t gantryVertOffset = static_cast<uint32_t>(scene.vertices.size());
    float gantryZ = tollCenterZ - tollParams.plazaLengthM * 0.5f - 14.0f;
    for (const auto& sv : gantryBuf.vertices) {
        scene.vertices.push_back(ToSceneVertex(sv, 0.0f, 0.03f, gantryZ));
    }
    for (uint32_t idx : gantryBuf.indices) {
        scene.indices.push_back(gantryVertOffset + idx);
    }

    // 2. Multi-Lane Canopy Toll Plaza flush on Ground Grade (Y = 0)
    TollMeshBuffer tollBuf = TollPlazaGenerator::GenerateTollMesh(tollParams);
    scene.tollVertexOffset = static_cast<uint32_t>(scene.vertices.size());
    scene.tollIndexOffset = static_cast<uint32_t>(scene.indices.size());
    for (const auto& tv : tollBuf.vertices) {
        scene.vertices.push_back(ToSceneVertex(tv, 0.0f, 0.0f, tollCenterZ));
    }
    for (uint32_t idx : tollBuf.indices) {
        scene.indices.push_back(scene.tollVertexOffset + idx);
    }

    // 3. Bridge Start/End Anchor Sampling & Seamless Rising Approach Viaduct
    float bx0, by0, bz0, btx0, bty0, btz0, bbank0;
    float bx1, by1, bz1, btx1, bty1, btz1, bbank1;
    BridgeGenerator::SampleCenterline(0.0f, bridgeParams, bx0, by0, bz0, btx0, bty0, btz0, bbank0);
    BridgeGenerator::SampleCenterline(1.0f, bridgeParams, bx1, by1, bz1, btx1, bty1, btz1, bbank1);

    float bridgeStartZ = tollExitZ + 42.0f;
    float bridgeShiftX = -bx0;
    float bridgeShiftZ = bridgeStartZ - bz0;

    // Rising Viaduct connecting Toll Plaza Exit (0, 0, tollExitZ) smoothly to Bridge Entry (0, by0, bridgeStartZ)
    const int riseSegs = 12;
    for (int s = 0; s < riseSegs; ++s) {
        float u0 = static_cast<float>(s) / riseSegs;
        float u1 = static_cast<float>(s + 1) / riseSegs;
        float y0 = by0 * (3.0f * u0 * u0 - 2.0f * u0 * u0 * u0);
        float y1 = by0 * (3.0f * u1 * u1 - 2.0f * u1 * u1 * u1);
        float z0 = tollExitZ + u0 * (bridgeStartZ - tollExitZ);
        float z1 = tollExitZ + u1 * (bridgeStartZ - tollExitZ);
        AppendCorridorRampSegment(scene, 0.0f, y0, z0, 1.0f, 0.0f,
                                  0.0f, y1, z1, 1.0f, 0.0f,
                                  bridgeParams.deckWidthM * 0.5f, 1.65f, (s == 6 || s == 10));
    }

    // 4. Bridge Superstructure spanning river gorge
    BridgeMeshBuffer bridgeBuf = BridgeGenerator::GenerateBridgeMesh(bridgeParams);
    scene.bridgeVertexOffset = static_cast<uint32_t>(scene.vertices.size());
    scene.bridgeIndexOffset = static_cast<uint32_t>(scene.indices.size());
    for (const auto& bv : bridgeBuf.vertices) {
        scene.vertices.push_back(ToSceneVertex(bv, bridgeShiftX, 0.0f, bridgeShiftZ));
    }
    for (uint32_t idx : bridgeBuf.indices) {
        scene.indices.push_back(scene.bridgeVertexOffset + idx);
    }

    // 5. Departure Viaduct Ramp connecting Bridge Exit smoothly to Tunnel Portal Entrance
    float bEndX = bx1 + bridgeShiftX;
    float bEndZ = bz1 + bridgeShiftZ;
    float tx0, ty0, tz0, tx1, ty1, tz1;
    float tux0, tuy0, tuz0, ttx0, tty0, ttz0;
    TunnelGenerator::SampleTunnelCenterline(0.0f, tunnelParams, tux0, tuy0, tuz0, ttx0, tty0, ttz0);
    (void)tx0; (void)ty0; (void)tz0; (void)tx1; (void)ty1; (void)tz1;

    float rampExitLen = 40.0f;
    float dirX = btx1, dirZ = btz1;
    float dirLen = std::sqrt(dirX * dirX + dirZ * dirZ);
    if (dirLen > 1e-4f) { dirX /= dirLen; dirZ /= dirLen; } else { dirX = 0.0f; dirZ = 1.0f; }
    float bnx1 = dirZ, bnz1 = -dirX;

    float tunnelEntryX = bEndX + dirX * rampExitLen;
    float tunnelEntryZ = bEndZ + dirZ * rampExitLen;
    float tunnelEntryY = 0.5f;

    const int descSegs = 12;
    for (int s = 0; s < descSegs; ++s) {
        float u0 = static_cast<float>(s) / descSegs;
        float u1 = static_cast<float>(s + 1) / descSegs;
        float x0 = bEndX + dirX * (u0 * rampExitLen);
        float z0 = bEndZ + dirZ * (u0 * rampExitLen);
        float y0 = by1 + (tunnelEntryY - by1) * (3.0f * u0 * u0 - 2.0f * u0 * u0 * u0);
        float x1 = bEndX + dirX * (u1 * rampExitLen);
        float z1 = bEndZ + dirZ * (u1 * rampExitLen);
        float y1 = by1 + (tunnelEntryY - by1) * (3.0f * u1 * u1 - 2.0f * u1 * u1 * u1);
        float hw = bridgeParams.deckWidthM * 0.5f * (1.0f - 0.35f * u0);
        AppendCorridorRampSegment(scene, x0, y0, z0, bnx1, bnz1,
                                  x1, y1, z1, bnx1, bnz1,
                                  hw, 1.55f, (s == 2 || s == 6));
    }

    // 6. Mountain Tunnel Portal & Subterranean Horseshoe Bore aligned cleanly to Ramp Exit
    TunnelMeshBuffer tunnelBuf = TunnelGenerator::GenerateTunnelMesh(tunnelParams);
    scene.tunnelVertexOffset = static_cast<uint32_t>(scene.vertices.size());
    scene.tunnelIndexOffset = static_cast<uint32_t>(scene.indices.size());
    float tunShiftX = tunnelEntryX - tux0;
    float tunShiftY = tunnelEntryY - tuy0;
    float tunShiftZ = tunnelEntryZ - tuz0;
    for (const auto& tu : tunnelBuf.vertices) {
        scene.vertices.push_back(ToSceneVertex(tu, tunShiftX, tunShiftY, tunShiftZ));
    }
    for (uint32_t idx : tunnelBuf.indices) {
        scene.indices.push_back(scene.tunnelVertexOffset + idx);
    }

    // Compute Aggregate Scene Telemetry
    scene.totalVertices = static_cast<uint32_t>(scene.vertices.size());
    scene.totalTriangles = static_cast<uint32_t>(scene.indices.size() / 3);
    scene.totalRoadLengthKm = (wallParams.wallLengthM + tollParams.plazaLengthM + 82.0f +
                               bridgeParams.spanLengthM + tunnelParams.tunnelLengthM) * 0.001f;
    scene.estimatedVramMegabytes = (scene.vertices.size() * sizeof(VulkanSceneVertex) +
                                    scene.indices.size() * sizeof(uint32_t)) / (1024.0f * 1024.0f);

    return scene;
}

} // namespace Road
} // namespace ApexAssets
