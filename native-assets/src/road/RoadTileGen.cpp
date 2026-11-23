// ==============================================================================
// Modular Road Component: Streaming LOD Road Tile & Surface State 3D Engine
// Path: native-assets/src/road/RoadTileGen.cpp
// ==============================================================================
#include "RoadTileGen.h"
#include "common/RoadMeshPrimitives.h"
#include <cmath>
#include <algorithm>
#include <vector>

namespace ApexAssets {
namespace Road {

static float PseudoHash2(int ix, int iz) {
    uint32_t n = static_cast<uint32_t>(ix * 73856093 ^ iz * 19349663);
    n = (n << 13u) ^ n;
    return static_cast<float>((n * (n * n * 15731u + 789221u) + 1376312589u) & 0x7fffffffu) / 2147483647.0f;
}

RoadTileMeshBuffer RoadTileGen::GenerateRoadTileMesh(const RoadTileParams& params) {
    RoadTileMeshBuffer buf;

    RoadTileParams p = params;
    p.tileSizeM = std::clamp(params.tileSizeM, 20.0f, 80.0f);
    p.roadWidthM = std::clamp(params.roadWidthM, 7.5f, 18.0f);
    p.cutFillHeightM = std::clamp(params.cutFillHeightM, 0.0f, 12.0f);
    p.distressDepthM = std::clamp(params.distressDepthM, 0.0f, 0.16f);

    float halfL = p.tileSizeM * 0.5f;
    float halfR = p.roadWidthM * 0.5f;
    float lateralSpan = std::max(p.tileSizeM * 0.52f, halfR + 8.5f);

    const int cols = 28;
    const int rows = 28;
    std::vector<float> gridH((cols + 1) * (rows + 1));
    std::vector<uint32_t> gridMat((cols + 1) * (rows + 1), 0u);

    auto idxAt = [&](int c, int r) { return r * (cols + 1) + c; };

    switch (p.type) {
        case RoadTileType::CutFillHillsideTile:
            buf.frictionMu = 0.80f;
            buf.rollingResistance = 0.014f;
            break;
        case RoadTileType::QuadtreeUrbanTile:
            buf.frictionMu = 0.84f;
            buf.rollingResistance = 0.012f;
            break;
        case RoadTileType::WetPuddleStateTile:
            buf.frictionMu = 0.62f;
            buf.rollingResistance = 0.022f;
            break;
        case RoadTileType::MuddyGravelRutTile:
            buf.frictionMu = 0.34f;
            buf.rollingResistance = 0.085f;
            break;
        case RoadTileType::PotholedCrackedTile:
            buf.frictionMu = 0.64f;
            buf.rollingResistance = 0.038f;
            break;
        case RoadTileType::SnowyIcyCorridorTile:
            buf.frictionMu = 0.18f;
            buf.rollingResistance = 0.055f;
            break;
    }

    // 1. Sample 2.5D terrain-conforming cross-section & roadbed displacement heightfield
    for (int r = 0; r <= rows; ++r) {
        float vz = static_cast<float>(r) / rows;
        float z = -halfL + vz * p.tileSizeM;
        for (int c = 0; c <= cols; ++c) {
            float u = static_cast<float>(c) / cols;
            float x = -lateralSpan + u * (lateralSpan * 2.0f);
            float absX = std::abs(x);

            float y = 0.45f; // Base pavement datum above lowest trench
            uint32_t mId = 0u;

            if (absX <= halfR) {
                // Parabolic AASHTO road crown (~2%)
                float crown = 0.08f * (1.0f - (x / halfR) * (x / halfR));
                y += crown;

                // Surface state deformations across the carriageway
                if (p.type == RoadTileType::WetPuddleStateTile) {
                    float wheelDist = std::min(std::abs(absX - halfR * 0.45f), std::abs(absX - halfR * 0.22f));
                    float rut = std::exp(-wheelDist * wheelDist * 3.5f) * (0.04f + p.distressDepthM * 1.4f);
                    y -= rut;
                } else if (p.type == RoadTileType::MuddyGravelRutTile) {
                    float wheelDist = std::abs(absX - halfR * 0.42f);
                    float deepRut = std::exp(-wheelDist * wheelDist * 2.4f) * (0.08f + p.distressDepthM * 2.2f);
                    float washboard = std::sin(z * 2.6f + x * 0.3f) * (0.03f + p.distressDepthM * 0.5f);
                    y = y - deepRut + washboard;
                } else if (p.type == RoadTileType::PotholedCrackedTile) {
                    // 3 sculpted jagged pothole craters along the tile
                    float d1 = std::hypot(x - halfR * 0.38f, z - halfL * 0.25f);
                    float d2 = std::hypot(x + halfR * 0.35f, z + halfL * 0.28f);
                    float d3 = std::hypot(x - halfR * 0.15f, z + halfL * 0.05f);
                    float cr = std::min({d1, d2, d3});
                    if (cr < 1.85f) {
                        float hole = (1.0f - cr / 1.85f);
                        y -= hole * hole * (0.12f + p.distressDepthM * 2.0f);
                        mId = 3u; // exposed crushed aggregate crater base
                    }
                } else if (p.type == RoadTileType::SnowyIcyCorridorTile) {
                    // Tire channels packed down to ice glaze while median / edges hold snowpack
                    float trackDist = std::abs(absX - halfR * 0.42f);
                    float trackMelt = std::exp(-trackDist * trackDist * 2.2f);
                    y += (1.0f - trackMelt) * (0.06f + p.distressDepthM * 0.8f);
                }
            } else {
                // Shoulder / Embankment / Sidewalk / Cut-and-Fill slopes
                float shoulderDist = absX - halfR;
                if (p.type == RoadTileType::CutFillHillsideTile) {
                    if (x < 0.0f) {
                        // Left V-drainage ditch then steep rocky cut face climbing upward
                        if (shoulderDist < 1.8f && p.hasDrainageAndCurbs) {
                            y -= std::sin((shoulderDist / 1.8f) * Primitives::PI) * 0.55f;
                            mId = 1u;
                        } else {
                            float slopeT = std::clamp((shoulderDist - 1.8f) / std::max(3.5f, lateralSpan - halfR - 1.8f), 0.0f, 1.0f);
                            float strata = std::sin(z * 0.35f + slopeT * 5.0f) * 0.14f;
                            y += slopeT * p.cutFillHeightM + strata * slopeT;
                            mId = 1u;
                        }
                    } else {
                        // Right gravel fill embankment sloping down to natural toe
                        float slopeT = std::clamp(shoulderDist / std::max(4.0f, lateralSpan - halfR), 0.0f, 1.0f);
                        y -= slopeT * (p.cutFillHeightM * 0.65f + 0.6f);
                        mId = 3u;
                    }
                } else if (p.type == RoadTileType::QuadtreeUrbanTile) {
                    // Raised granite kerb + concrete sidewalk apron
                    y += (p.hasDrainageAndCurbs ? 0.18f : 0.04f);
                    mId = 1u;
                } else if (p.type == RoadTileType::SnowyIcyCorridorTile) {
                    // Snowplow berm windrows pushed along shoulders
                    float berm = std::exp(-((shoulderDist - 1.4f) * (shoulderDist - 1.4f)) * 0.45f) * (0.55f + p.cutFillHeightM * 0.08f);
                    y += berm;
                    mId = 3u;
                } else {
                    // Gentle vegetated/gravel drainage swale & berm
                    if (shoulderDist < 2.2f && p.hasDrainageAndCurbs) {
                        y -= std::sin((shoulderDist / 2.2f) * Primitives::PI) * 0.32f;
                    } else {
                        y += 0.06f * std::sin(shoulderDist * 0.5f + z * 0.2f);
                    }
                    mId = 3u;
                }
            }

            gridH[idxAt(c, r)] = y;
            gridMat[idxAt(c, r)] = mId;
        }
    }

    // 2. Emit continuous surface mesh quads colored by tile type & road-surface state
    for (int r = 0; r < rows; ++r) {
        float z0 = -halfL + (static_cast<float>(r) / rows) * p.tileSizeM;
        float z1 = -halfL + (static_cast<float>(r + 1) / rows) * p.tileSizeM;

        for (int c = 0; c < cols; ++c) {
            float x0 = -lateralSpan + (static_cast<float>(c) / cols) * (lateralSpan * 2.0f);
            float x1 = -lateralSpan + (static_cast<float>(c + 1) / cols) * (lateralSpan * 2.0f);
            float midX = 0.5f * (x0 + x1);
            bool onRoad = std::abs(midX) <= halfR;

            float y00 = gridH[idxAt(c, r)];
            float y10 = gridH[idxAt(c + 1, r)];
            float y11 = gridH[idxAt(c + 1, r + 1)];
            float y01 = gridH[idxAt(c, r + 1)];

            float p00[3] = {x0, y00, z0};
            float p10[3] = {x1, y10, z0};
            float p11[3] = {x1, y11, z1};
            float p01[3] = {x0, y01, z1};

            // Compute quad geometric face normal
            float ax = x1 - x0, ay = y10 - y00, az = 0.0f;
            float bx = 0.0f, by = y01 - y00, bz = z1 - z0;
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            if (ny < 0.0f) { nx = -nx; ny = -ny; nz = -nz; }
            float nLen = std::sqrt(nx * nx + ny * ny + nz * nz);
            if (nLen > 1e-5f) { nx /= nLen; ny /= nLen; nz /= nLen; } else { nx = 0.0f; ny = 1.0f; nz = 0.0f; }
            float norm[3] = {nx, ny, nz};

            float jitter = (PseudoHash2(c, r) - 0.5f) * 0.04f;
            float col[3] = {0.20f, 0.22f, 0.25f};
            float rough = 0.85f, metal = 0.02f;
            uint32_t mId = gridMat[idxAt(c, r)];

            if (onRoad) {
                switch (p.type) {
                    case RoadTileType::WetPuddleStateTile:
                        col[0] = 0.11f + jitter * 0.4f; col[1] = 0.13f + jitter * 0.4f; col[2] = 0.16f + jitter * 0.4f;
                        rough = 0.18f; metal = 0.12f;
                        break;
                    case RoadTileType::MuddyGravelRutTile:
                        col[0] = 0.42f + jitter; col[1] = 0.30f + jitter * 0.8f; col[2] = 0.20f + jitter * 0.5f;
                        rough = 0.90f; metal = 0.01f;
                        break;
                    case RoadTileType::PotholedCrackedTile:
                        if (mId == 3u) {
                            col[0] = 0.38f + jitter; col[1] = 0.35f + jitter; col[2] = 0.31f + jitter;
                            rough = 0.94f;
                        } else {
                            col[0] = 0.24f + jitter; col[1] = 0.25f + jitter; col[2] = 0.27f + jitter;
                            rough = 0.88f;
                        }
                        break;
                    case RoadTileType::SnowyIcyCorridorTile: {
                        float trackDist = std::abs(std::abs(midX) - halfR * 0.42f);
                        if (trackDist < 1.1f) {
                            // Glazed black ice tire lane
                            col[0] = 0.36f; col[1] = 0.48f; col[2] = 0.58f;
                            rough = 0.12f; metal = 0.15f;
                        } else {
                            // Crisp white snowpack
                            col[0] = 0.88f + jitter * 0.3f; col[1] = 0.92f + jitter * 0.3f; col[2] = 0.97f;
                            rough = 0.78f;
                        }
                        break;
                    }
                    default:
                        col[0] = 0.19f + jitter * 0.5f; col[1] = 0.21f + jitter * 0.5f; col[2] = 0.24f + jitter * 0.5f;
                        rough = 0.84f;
                        break;
                }
            } else {
                // Shoulder / Slope colors
                switch (p.type) {
                    case RoadTileType::CutFillHillsideTile:
                        if (midX < 0.0f) {
                            col[0] = 0.48f + jitter; col[1] = 0.44f + jitter; col[2] = 0.40f + jitter;
                        } else {
                            col[0] = 0.44f + jitter; col[1] = 0.50f + jitter; col[2] = 0.32f + jitter;
                        }
                        rough = 0.92f;
                        break;
                    case RoadTileType::QuadtreeUrbanTile:
                        col[0] = 0.64f + jitter; col[1] = 0.67f + jitter; col[2] = 0.71f + jitter;
                        rough = 0.74f;
                        break;
                    case RoadTileType::SnowyIcyCorridorTile:
                        col[0] = 0.92f; col[1] = 0.95f; col[2] = 0.99f;
                        rough = 0.80f;
                        break;
                    case RoadTileType::MuddyGravelRutTile:
                        col[0] = 0.36f + jitter; col[1] = 0.26f + jitter; col[2] = 0.17f + jitter;
                        rough = 0.92f;
                        break;
                    default:
                        col[0] = 0.42f + jitter; col[1] = 0.39f + jitter; col[2] = 0.34f + jitter;
                        rough = 0.88f;
                        break;
                }
            }

            Primitives::AddQuad<RoadTileMeshBuffer, RoadTileVertex>(
                buf, p00, p01, p11, p10, norm, mId,
                col[0], col[1], col[2], 1.0f,
                static_cast<float>(c) / cols, static_cast<float>(c + 1) / cols,
                static_cast<float>(r) / rows, static_cast<float>(r + 1) / rows,
                rough, metal
            );
        }
    }

    // 3. Add Tile Details: Markings, Repair Patches, Puddle Water Sheen, Storm Grates & Marker Poles
    // Dashed centerline & edge paint lines (omitted on muddy unpaved gravel tile)
    if (p.type != RoadTileType::MuddyGravelRutTile) {
        int dashCount = std::max(2, static_cast<int>(p.tileSizeM / 6.0f));
        float dashPitch = p.tileSizeM / dashCount;
        float paintFade = (p.type == RoadTileType::PotholedCrackedTile) ? 0.65f : 0.95f;
        for (int d = 0; d < dashCount; ++d) {
            float cz = -halfL + (d + 0.5f) * dashPitch;
            Primitives::AddBox<RoadTileMeshBuffer, RoadTileVertex>(
                buf, 0.0f, 0.55f, cz, 0.18f, 0.025f, dashPitch * 0.45f,
                3, paintFade, paintFade * 0.82f, 0.16f, 1.0f, 0.45f, 0.04f
            );
        }
        // Continuous solid white edge lines
        for (int s : {-1, 1}) {
            Primitives::AddBox<RoadTileMeshBuffer, RoadTileVertex>(
                buf, s * (halfR - 0.35f), 0.49f, 0.0f, 0.16f, 0.02f, p.tileSizeM * 0.96f,
                3, paintFade, paintFade, paintFade, 1.0f, 0.45f, 0.04f
            );
        }
    }

    if (p.type == RoadTileType::WetPuddleStateTile) {
        // Reflective standing rainwater sheets in depressed wheel tracks
        for (int s : {-1, 1}) {
            Primitives::AddBox<RoadTileMeshBuffer, RoadTileVertex>(
                buf, s * (halfR * 0.44f), 0.485f, 0.0f, 1.75f, 0.018f, p.tileSizeM * 0.72f,
                4, 0.18f, 0.52f, 0.68f, 0.88f, 0.05f, 0.35f, 0.2f
            );
        }
    }

    if (p.type == RoadTileType::PotholedCrackedTile) {
        // Fresh dark asphalt repair patch slab + yellow construction hazard cones near craters
        Primitives::AddBox<RoadTileMeshBuffer, RoadTileVertex>(
            buf, -halfR * 0.35f, 0.51f, halfL * 0.32f, 2.4f, 0.03f, 3.2f,
            0, 0.12f, 0.13f, 0.14f, 1.0f, 0.65f, 0.04f
        );
        // Orange traffic warning cone by primary pothole
        Primitives::AddCylinder<RoadTileMeshBuffer, RoadTileVertex>(
            buf, halfR * 0.58f, 0.48f, halfL * 0.25f, halfR * 0.58f, 1.28f, halfL * 0.25f,
            0.28f, 0.07f, 8, 3, 0.98f, 0.42f, 0.06f, 1.0f, 0.40f, 0.02f
        );
    }

    if (p.type == RoadTileType::QuadtreeUrbanTile && p.hasDrainageAndCurbs) {
        // Cast-iron storm drain catch basin grates & manhole covers
        for (int s : {-1, 1}) {
            for (float gz : {-halfL * 0.45f, halfL * 0.45f}) {
                Primitives::AddBox<RoadTileMeshBuffer, RoadTileVertex>(
                    buf, s * (halfR + 0.15f), 0.47f, gz, 0.75f, 0.06f, 1.15f,
                    2, 0.26f, 0.29f, 0.32f, 1.0f, 0.42f, 0.82f
                );
            }
        }
        Primitives::AddCylinder<RoadTileMeshBuffer, RoadTileVertex>(
            buf, 0.0f, 0.52f, -halfL * 0.22f, 0.0f, 0.56f, -halfL * 0.22f,
            0.46f, 0.46f, 12, 2, 0.28f, 0.30f, 0.33f, 1.0f, 0.40f, 0.85f
        );
    }

    if (p.type == RoadTileType::SnowyIcyCorridorTile && p.hasDrainageAndCurbs) {
        // High-visibility alpine snow-depth marker poles along shoulders
        for (int s : {-1, 1}) {
            for (float pz : {-halfL * 0.65f, 0.0f, halfL * 0.65f}) {
                float px = s * (halfR + 1.2f);
                Primitives::AddCylinder<RoadTileMeshBuffer, RoadTileVertex>(
                    buf, px, 0.55f, pz, px, 2.85f, pz, 0.055f, 0.055f, 6, 3, 0.98f, 0.36f, 0.06f, 1.0f, 0.35f, 0.05f
                );
                Primitives::AddBox<RoadTileMeshBuffer, RoadTileVertex>(
                    buf, px, 2.65f, pz, 0.14f, 0.26f, 0.14f, 4, 0.98f, 0.85f, 0.20f, 1.0f, 0.2f, 0.0f, 1.8f
                );
            }
        }
    }

    if (p.type == RoadTileType::MuddyGravelRutTile) {
        // Scattered roadside boulders / rip-rap stones
        for (int b = 0; b < 8; ++b) {
            float sign = (b % 2 == 0) ? 1.0f : -1.0f;
            float rx = sign * (halfR + 1.1f + PseudoHash2(b, 17) * 3.0f);
            float rz = -halfL * 0.8f + (static_cast<float>(b) / 7.0f) * p.tileSizeM * 0.8f;
            float rad = 0.28f + PseudoHash2(b, 31) * 0.35f;
            Primitives::AddBox<RoadTileMeshBuffer, RoadTileVertex>(
                buf, rx, 0.42f + rad * 0.4f, rz, rad * 1.4f, rad, rad * 1.2f,
                3, 0.45f, 0.42f, 0.38f, 1.0f, 0.90f, 0.02f
            );
        }
    }

    Primitives::ComputeMeshAABB(buf);
    buf.vertexCount = static_cast<uint32_t>(buf.vertices.size());
    buf.triangleCount = static_cast<uint32_t>(buf.indices.size() / 3);
    return buf;
}

} // namespace Road
} // namespace ApexAssets
