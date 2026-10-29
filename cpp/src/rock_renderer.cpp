#include "rock_renderer.hpp"
#include <cmath>
#include <algorithm>

namespace apex {
namespace rendering {

RockRenderer::RockRenderer() : noise(1337) {
    initGrid10x10();
}

void RockRenderer::initGrid10x10(uint32_t seed) {
    noise.reseed(seed);
    gridCells.clear();
    gridCells.reserve(100);

    const std::vector<std::string> archetypes = {
        "rock_granite", "rock_basalt", "rock_meteorite", "rock_crystal", "rock_strata"
    };

    for (int r = 0; r < 10; ++r) {
        float z = (r - 4.5f) * 1.0f; // -4.5m to +4.5m
        for (int c = 0; c < 10; ++c) {
            float x = (c - 4.5f) * 1.0f; // -4.5m to +4.5m
            float y = noise.perlin3D(c * 0.4f, r * 0.4f, 1.2f) * 0.25f + 0.10f; // local elevation

            GridCell1M cell;
            cell.row = r;
            cell.col = c;
            cell.worldPosition = {x, y, z};
            cell.cellSize = {1.0f, 0.0f, 1.0f};

            int archIdx = (r * 3 + c * 7) % archetypes.size();
            cell.archetype = archetypes[archIdx];

            float scaleVar = 0.85f + noise.perlin3D(c * 0.8f, r * 0.8f, 5.5f) * 0.15f;
            if (cell.archetype == "rock_basalt") {
                cell.rockScale = {0.50f * scaleVar, 0.92f * scaleVar, 0.50f * scaleVar};
            } else if (cell.archetype == "rock_strata") {
                cell.rockScale = {0.85f * scaleVar, 0.42f * scaleVar, 0.70f * scaleVar};
            } else {
                cell.rockScale = {0.80f * scaleVar, 0.65f * scaleVar, 0.75f * scaleVar};
            }

            cell.volumeM3 = cell.rockScale.x * cell.rockScale.y * cell.rockScale.z * 0.52f;
            float density = (cell.archetype == "rock_meteorite") ? 3400.0f : 2700.0f;
            cell.massKg = cell.volumeM3 * density;
            cell.thermalErosionAmount = 0.18f;
            cell.hydraulicErosionAmount = 0.22f;

            gridCells.push_back(cell);
        }
    }
}

void RockRenderer::generateRockMesh(
    const std::string& archetype,
    float baseRadius,
    const Vec3& scale,
    std::vector<RockVertex>& outVertices,
    std::vector<uint16_t>& outIndices,
    bool applyErosion
) {
    outVertices.clear();
    outIndices.clear();

    const float phi = (1.0f + std::sqrt(5.0f)) * 0.5f;
    std::vector<Vec3> baseVerts = {
        {-1,  phi, 0}, { 1,  phi, 0}, {-1, -phi, 0}, { 1, -phi, 0},
        { 0, -1,  phi}, { 0,  1,  phi}, { 0, -1, -phi}, { 0,  1, -phi},
        { phi, 0, -1}, { phi, 0,  1}, {-phi, 0, -1}, {-phi, 0,  1}
    };

    for (auto& v : baseVerts) {
        float len = std::sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        v = {v.x / len, v.y / len, v.z / len};

        // 1. Domain warped fBm multi-octave base
        float warpedVal = noise.domainWarpFBM(v.x * 2.2f, v.y * 2.2f, v.z * 2.2f, 0.35f, 4);

        // 2. Ridged noise (1 - |noise|) for sharp tectonic crags & fractures
        float ridgedVal = noise.ridgedNoise3D(v.x * 2.8f, v.y * 2.8f, v.z * 2.8f, 3);

        // 3. Worley cellular cleavage planes
        auto [d1, d2] = noise.worley3D(v.x * 1.8f, v.y * 1.8f, v.z * 1.8f, 1.5f);
        float voronoiDisp = (d2 - d1) * 0.22f;

        // Combine procedural displacements
        float displacement = 1.0f + warpedVal * 0.30f + ridgedVal * 0.20f - voronoiDisp;

        // 4. Simulated Erosion pass: soften top crests and settle at talus base
        if (applyErosion) {
            float slope = std::abs(v.y);
            if (slope > 0.65f) { // Steep crest erosion
                displacement *= 0.92f;
            } else if (v.y < -0.3f) { // Talus base deposition
                displacement *= 1.08f;
            }
        }

        float r = baseRadius * displacement;
        Vec3 pos = {v.x * r * scale.x, v.y * r * scale.y, v.z * r * scale.z};

        float u = 0.5f + (std::atan2(v.z, v.x) / (2.0f * 3.14159265f));
        float vCoord = 0.5f - (std::asin(std::max(-1.0f, std::min(1.0f, v.y))) / 3.14159265f);

        // Ambient occlusion estimate based on concave displacement
        float aoFactor = std::clamp(1.0f - (voronoiDisp * 1.5f) + (v.y * 0.15f), 0.35f, 1.0f);

        outVertices.push_back({pos, v, {u, vCoord}, aoFactor});
    }
}

std::array<RockLodMesh, 3> RockRenderer::generateRockLODs(
    const std::string& archetype,
    float baseRadius,
    const Vec3& scale
) {
    std::array<RockLodMesh, 3> lods;

    // LOD0: High detail with full erosion
    lods[0].lodLevel = 0;
    generateRockMesh(archetype, baseRadius, scale, lods[0].vertices, lods[0].indices, true);

    // LOD1: Medium detail
    lods[1].lodLevel = 1;
    generateRockMesh(archetype, baseRadius, scale, lods[1].vertices, lods[1].indices, false);

    // LOD2: Simplified collision hull
    lods[2].lodLevel = 2;
    generateRockMesh(archetype, baseRadius * 0.95f, scale, lods[2].vertices, lods[2].indices, false);

    return lods;
}

Vec3 RockRenderer::sampleTriplanarColor(
    const Vec3& worldPos,
    const Vec3& normal,
    float textureScale
) const {
    // Triplanar blending weights based on normal
    Vec3 blending = {std::abs(normal.x), std::abs(normal.y), std::abs(normal.z)};
    float blendSum = blending.x + blending.y + blending.z + 0.0001f;
    blending = {blending.x / blendSum, blending.y / blendSum, blending.z / blendSum};

    // Procedural noise samples along X, Y, Z projection planes
    float colX = noise.fbm3D(worldPos.y * textureScale, worldPos.z * textureScale, 1.0f, 3);
    float colY = noise.fbm3D(worldPos.x * textureScale, worldPos.z * textureScale, 2.0f, 3);
    float colZ = noise.fbm3D(worldPos.x * textureScale, worldPos.y * textureScale, 3.0f, 3);

    float finalVal = colX * blending.x + colY * blending.y + colZ * blending.z;
    return {finalVal, finalVal * 0.95f, finalVal * 0.88f};
}

Vec3 RockRenderer::shadePbrPixel(
    const Vec3& fragPos,
    const Vec3& normal,
    const Vec3& viewDir,
    const Vec3& lightDir,
    const Vec3& lightColor,
    const PbrMaterial& mat,
    float ao
) const {
    float nDotL = std::max(0.0f, normal.x * lightDir.x + normal.y * lightDir.y + normal.z * lightDir.z);

    // Diffuse Lambertian with Ambient Occlusion
    Vec3 diffuse = {
        mat.baseColorFactor[0] * lightColor.x * nDotL * ao,
        mat.baseColorFactor[1] * lightColor.y * nDotL * ao,
        mat.baseColorFactor[2] * lightColor.z * nDotL * ao
    };

    // Ambient term
    Vec3 ambient = {
        mat.baseColorFactor[0] * 0.15f * ao,
        mat.baseColorFactor[1] * 0.15f * ao,
        mat.baseColorFactor[2] * 0.15f * ao
    };

    return diffuse + ambient;
}

} // namespace rendering
} // namespace apex
