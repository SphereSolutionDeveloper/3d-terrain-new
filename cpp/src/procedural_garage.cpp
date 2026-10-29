#include "procedural_garage.hpp"

namespace apex {
namespace procedural {

void ProceduralGarage::addBox(std::vector<Vertex3D>& verts,
                              float cx, float cy, float cz,
                              float sx, float sy, float sz,
                              float r, float g, float b, float a) {
    float hx = sx * 0.5f;
    float hy = sy * 0.5f;
    float hz = sz * 0.5f;

    struct Face {
        float nx, ny, nz;
        float v[4][3];
    };

    Face faces[6] = {
        // Front (+Z)
        {0, 0, 1, {{-hx, -hy, hz}, {hx, -hy, hz}, {hx, hy, hz}, {-hx, hy, hz}}},
        // Back (-Z)
        {0, 0, -1, {{hx, -hy, -hz}, {-hx, -hy, -hz}, {-hx, hy, -hz}, {hx, hy, -hz}}},
        // Top (+Y)
        {0, 1, 0, {{-hx, hy, hz}, {hx, hy, hz}, {hx, hy, -hz}, {-hx, hy, -hz}}},
        // Bottom (-Y)
        {0, -1, 0, {{-hx, -hy, -hz}, {hx, -hy, -hz}, {hx, -hy, hz}, {-hx, -hy, hz}}},
        // Right (+X)
        {1, 0, 0, {{hx, -hy, hz}, {hx, -hy, -hz}, {hx, hy, -hz}, {hx, hy, hz}}},
        // Left (-X)
        {-1, 0, 0, {{-hx, -hy, -hz}, {-hx, -hy, hz}, {-hx, hy, hz}, {-hx, hy, -hz}}}
    };

    for (int i = 0; i < 6; ++i) {
        const auto& f = faces[i];
        int idxs[6] = {0, 1, 2, 0, 2, 3};
        for (int k = 0; k < 6; ++k) {
            int vi = idxs[k];
            Vertex3D vert;
            vert.x = cx + f.v[vi][0];
            vert.y = cy + f.v[vi][1];
            vert.z = cz + f.v[vi][2];
            vert.nx = f.nx;
            vert.ny = f.ny;
            vert.nz = f.nz;
            vert.r = r;
            vert.g = g;
            vert.b = b;
            vert.a = a;
            verts.push_back(vert);
        }
    }
}

std::vector<Vertex3D> ProceduralGarage::buildGarage001(const GarageConfig& config) {
    std::vector<Vertex3D> verts;

    // 1. Main Depot Warehouse (14m x 6m x 22m, Steel Slate Gray #384252 -> 0.22, 0.26, 0.32)
    // Centered at local origin (X=0, Y=3, Z=0)
    addBox(verts, 0.0f, 3.0f, 0.0f, 14.0f, 6.0f, 22.0f, 0.22f, 0.26f, 0.32f);

    // 2. Roof with Crimson Red Trim (15m x 0.4m x 23m, #D93838 -> 0.85, 0.22, 0.22)
    addBox(verts, 0.0f, 6.2f, 0.0f, 15.0f, 0.4f, 23.0f, 0.85f, 0.22f, 0.22f);

    // 3. Service Bay Rollup Doors (West wall at X = -7.0m facing apron)
    // Bay 1 (Z = -4.0m)
    float maxH = 3.6f;
    float h1 = maxH * (1.0f - config.bay1OpenPct);
    float y1 = maxH - (h1 * 0.5f);
    if (h1 > 0.05f) {
        addBox(verts, -7.0f, y1, -4.0f, 0.2f, h1, 5.0f, 0.75f, 0.78f, 0.82f);
    }

    // Bay 2 (Z = +4.0m)
    float h2 = maxH * (1.0f - config.bay2OpenPct);
    float y2 = maxH - (h2 * 0.5f);
    if (h2 > 0.05f) {
        addBox(verts, -7.0f, y2, 4.0f, 0.2f, h2, 5.0f, 0.75f, 0.78f, 0.82f);
    }

    // 4. Workshop Signboard Banner (West wall at X = -7.1m, Y = 4.8m)
    if (config.neonSignOn) {
        addBox(verts, -7.15f, 4.8f, 0.0f, 0.1f, 1.0f, 10.0f, 0.0f, 0.85f, 0.95f); // Bright cyan glow
    } else {
        addBox(verts, -7.15f, 4.8f, 0.0f, 0.1f, 1.0f, 10.0f, 0.0f, 0.40f, 0.50f); // Inactive dark cyan
    }

    // 5. Paved Garage Apron (28m x 0.12m x 24m, Asphalt Dark #333840 -> 0.20, 0.22, 0.25)
    if (config.showApron) {
        addBox(verts, -21.0f, 0.06f, 0.0f, 28.0f, 0.12f, 24.0f, 0.20f, 0.22f, 0.25f);
    }

    // 6. Yellow Bay Guidelines on the Apron (14m x 0.02m x 0.22m, #FFD54F)
    if (config.showGuidelines) {
        addBox(verts, -14.0f, 0.13f, -4.0f, 14.0f, 0.02f, 0.22f, 1.0f, 0.85f, 0.2f);
        addBox(verts, -14.0f, 0.13f, 4.0f, 14.0f, 0.02f, 0.22f, 1.0f, 0.85f, 0.2f);
    }

    // 7. Connecting Driveway to Road (24m x 0.12m x 14m, #262C33 -> 0.18, 0.20, 0.24)
    if (config.showDriveway) {
        addBox(verts, -46.0f, 0.08f, -16.0f, 24.0f, 0.12f, 14.0f, 0.18f, 0.20f, 0.24f);
    }

    return verts;
}

} // namespace procedural
} // namespace apex
