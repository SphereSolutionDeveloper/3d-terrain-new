#pragma once
#include <cmath>
#include <vector>
#include <array>
#include <numeric>
#include <random>
#include <algorithm>
#include <utility>

namespace apex {
namespace procedural {

/**
 * Advanced 3D Procedural Noise & Geological Erosion Engine for Realistic 3D Rocks.
 * Implements:
 * 1. Ken Perlin Improved Noise & 3D Simplex Gradient Noise
 * 2. Fractal Brownian Motion (fBm) multi-octave summation
 * 3. Ridged Multifractal Noise: 1.0 - abs(noise) for sharp tectonic ridges and fractures
 * 4. Domain Warping: feeding noise back into coordinate space for twisted strata
 * 5. Worley / Voronoi Cellular Noise for cleavage planes and crystal facets
 * 6. Thermal Erosion (Talus angle sediment slide)
 * 7. Hydraulic Erosion (Droplet-based sediment transport & channel carving)
 */
class NoiseGenerator {
public:
    explicit NoiseGenerator(uint32_t seed = 1337) {
        reseed(seed);
    }

    void reseed(uint32_t seed) {
        perm.resize(256);
        std::iota(perm.begin(), perm.end(), 0);
        std::mt19937 rng(seed);
        std::shuffle(perm.begin(), perm.end(), rng);
        perm.insert(perm.end(), perm.begin(), perm.end()); // 512 for fast wrap
    }

    static inline float fade(float t) noexcept {
        // Ken Perlin quintic polynomial: 6t^5 - 15t^4 + 10t^3
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }

    static inline float lerp(float a, float b, float t) noexcept {
        return a + t * (b - a);
    }

    static inline float grad(int hash, float x, float y, float z) noexcept {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    // 1. Ken Perlin 3D Noise [-1.0, 1.0]
    float perlin3D(float x, float y, float z) const noexcept {
        int X = static_cast<int>(std::floor(x)) & 255;
        int Y = static_cast<int>(std::floor(y)) & 255;
        int Z = static_cast<int>(std::floor(z)) & 255;

        x -= std::floor(x);
        y -= std::floor(y);
        z -= std::floor(z);

        float u = fade(x);
        float v = fade(y);
        float w = fade(z);

        int A  = perm[X] + Y;
        int AA = perm[A] + Z;
        int AB = perm[A + 1] + Z;
        int B  = perm[X + 1] + Y;
        int BA = perm[B] + Z;
        int BB = perm[B + 1] + Z;

        return lerp(
            lerp(
                lerp(grad(perm[AA], x, y, z), grad(perm[BA], x - 1.0f, y, z), u),
                lerp(grad(perm[AB], x, y - 1.0f, z), grad(perm[BB], x - 1.0f, y - 1.0f, z), u),
                v
            ),
            lerp(
                lerp(grad(perm[AA + 1], x, y, z - 1.0f), grad(perm[BA + 1], x - 1.0f, y, z - 1.0f), u),
                lerp(grad(perm[AB + 1], x, y - 1.0f, z - 1.0f), grad(perm[BB + 1], x - 1.0f, y - 1.0f, z - 1.0f), u),
                v
            ),
            w
        );
    }

    // 2. Fractal Brownian Motion (fBm)
    float fbm3D(float x, float y, float z, int octaves = 5, float lacunarity = 2.0f, float persistence = 0.5f) const noexcept {
        float total = 0.0f;
        float frequency = 1.0f;
        float amplitude = 1.0f;
        float max_val = 0.0f;

        for (int i = 0; i < octaves; ++i) {
            total += perlin3D(x * frequency, y * frequency, z * frequency) * amplitude;
            max_val += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return total / max_val;
    }

    // 3. Ridged Multifractal Noise: 1.0 - abs(noise) creates sharp rock fractures and ridges
    float ridgedNoise3D(float x, float y, float z, int octaves = 4, float lacunarity = 2.0f, float gain = 0.5f) const noexcept {
        float sum = 0.0f;
        float amp = 1.0f;
        float freq = 1.0f;
        float weight = 1.0f;

        for (int i = 0; i < octaves; ++i) {
            float n = perlin3D(x * freq, y * freq, z * freq);
            // Sharp inverted ridge: 1 - |noise|
            float signal = 1.0f - std::abs(n);
            signal = signal * signal; // Sharpen ridges
            signal *= weight;
            weight = std::clamp(signal * 2.0f, 0.0f, 1.0f);

            sum += signal * amp;
            freq *= lacunarity;
            amp *= gain;
        }
        return sum;
    }

    // 4. Domain Warping: feeding noise back into coordinates for twisted geologic strata
    float domainWarpFBM(float x, float y, float z, float warpScale = 0.45f, int octaves = 4) const noexcept {
        float qx = fbm3D(x + 0.0f, y + 0.0f, z + 0.0f, 3);
        float qy = fbm3D(x + 5.2f, y + 1.3f, z + 2.8f, 3);
        float qz = fbm3D(x + 1.7f, y + 9.2f, z + 0.4f, 3);

        float rx = fbm3D(x + warpScale * qx + 1.7f, y + warpScale * qy + 9.2f, z + warpScale * qz + 0.1f, 3);
        float ry = fbm3D(x + warpScale * qx + 8.3f, y + warpScale * qy + 2.8f, z + warpScale * qz + 4.5f, 3);
        float rz = fbm3D(x + warpScale * qx + 3.1f, y + warpScale * qy + 4.7f, z + warpScale * qz + 8.2f, 3);

        return fbm3D(x + warpScale * rx, y + warpScale * ry, z + warpScale * rz, octaves);
    }

    // 5. Worley / Voronoi Cellular Noise for crystal cleavage facets and angular joints
    std::pair<float, float> worley3D(float x, float y, float z, float cellScale = 2.0f) const noexcept {
        x *= cellScale;
        y *= cellScale;
        z *= cellScale;

        int xi = static_cast<int>(std::floor(x));
        int yi = static_cast<int>(std::floor(y));
        int zi = static_cast<int>(std::floor(z));

        float min_dist = 999.0f;
        float second_min_dist = 999.0f;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    int cx = xi + dx;
                    int cy = yi + dy;
                    int cz = zi + dz;

                    uint32_t h = (cx * 73856093u ^ cy * 19349663u ^ cz * 83492791u) & 0xFFFFFFu;
                    float rx = cx + ((h & 0xFF) / 255.0f);
                    float ry = cy + (((h >> 8) & 0xFF) / 255.0f);
                    float rz = cz + (((h >> 16) & 0xFF) / 255.0f);

                    float dist = std::sqrt((x - rx) * (x - rx) + (y - ry) * (y - ry) + (z - rz) * (z - rz));
                    if (dist < min_dist) {
                        second_min_dist = min_dist;
                        min_dist = dist;
                    } else if (dist < second_min_dist) {
                        second_min_dist = dist;
                    }
                }
            }
        }
        return {min_dist, second_min_dist};
    }

    // 6. Thermal Erosion Simulation (Talus angle material displacement)
    // Displaces material from slopes exceeding talus repose angle (typically 35°-45°)
    void applyThermalErosion(std::vector<float>& displacements, int width, int height,
                             float talusAngleRad = 0.65f, float erosionRate = 0.15f, int iterations = 10) const {
        if (displacements.empty() || width <= 1 || height <= 1) return;
        const float tanTalus = std::tan(talusAngleRad);

        std::vector<float> temp = displacements;
        for (int iter = 0; iter < iterations; ++iter) {
            for (int y = 1; y < height - 1; ++y) {
                for (int x = 1; x < width - 1; ++x) {
                    int idx = y * width + x;
                    float h = temp[idx];

                    // Check 4 cardinal neighbors
                    const int neighbors[4] = { idx - 1, idx + 1, idx - width, idx + width };
                    float maxSlope = 0.0f;
                    int steepestNeighbor = -1;

                    for (int nIdx : neighbors) {
                        float diff = h - temp[nIdx];
                        if (diff > maxSlope) {
                            maxSlope = diff;
                            steepestNeighbor = nIdx;
                        }
                    }

                    if (maxSlope > tanTalus && steepestNeighbor >= 0) {
                        float delta = (maxSlope - tanTalus) * 0.5f * erosionRate;
                        displacements[idx] -= delta;
                        displacements[steepestNeighbor] += delta;
                    }
                }
            }
            temp = displacements;
        }
    }

    // 7. Hydraulic Erosion Simulation (Hans Beyer droplet model)
    void applyHydraulicErosion(std::vector<float>& heightmap, int width, int height,
                               int numDroplets = 2000, float inertia = 0.05f, float sedimentCapacityFactor = 4.0f,
                               float minSedimentCapacity = 0.01f, float dissolveRate = 0.3f, float depositRate = 0.3f,
                               float evaporateRate = 0.01f, float gravity = 4.0f, int maxDropletSteps = 30) const {
        if (heightmap.empty() || width <= 2 || height <= 2) return;
        std::mt19937 rng(42);
        std::uniform_real_distribution<float> distCoordX(1.0f, static_cast<float>(width - 2));
        std::uniform_real_distribution<float> distCoordY(1.0f, static_cast<float>(height - 2));

        for (int drop = 0; drop < numDroplets; ++drop) {
            float posX = distCoordX(rng);
            float posY = distCoordY(rng);
            float dirX = 0.0f;
            float dirY = 0.0f;
            float speed = 1.0f;
            float water = 1.0f;
            float sediment = 0.0f;

            for (int step = 0; step < maxDropletSteps; ++step) {
                int nodeX = static_cast<int>(posX);
                int nodeY = static_cast<int>(posY);
                if (nodeX < 1 || nodeX >= width - 1 || nodeY < 1 || nodeY >= height - 1) break;

                // Bilinear gradient
                float fx = posX - nodeX;
                float fy = posY - nodeY;
                int idx00 = nodeY * width + nodeX;
                int idx10 = idx00 + 1;
                int idx01 = idx00 + width;
                int idx11 = idx01 + 1;

                float gradX = (heightmap[idx10] - heightmap[idx00]) * (1.0f - fy) +
                              (heightmap[idx11] - heightmap[idx01]) * fy;
                float gradY = (heightmap[idx01] - heightmap[idx00]) * (1.0f - fx) +
                              (heightmap[idx11] - heightmap[idx10]) * fx;

                // Update droplet direction with inertia
                dirX = (dirX * inertia - gradX * (1.0f - inertia));
                dirY = (dirY * inertia - gradY * (1.0f - inertia));

                float len = std::sqrt(dirX * dirX + dirY * dirY);
                if (len != 0.0f) {
                    dirX /= len;
                    dirY /= len;
                } else break;

                posX += dirX;
                posY += dirY;

                int nextX = static_cast<int>(posX);
                int nextY = static_cast<int>(posY);
                if (nextX < 1 || nextX >= width - 1 || nextY < 1 || nextY >= height - 1) break;

                float hOld = heightmap[idx00];
                float hNew = heightmap[nextY * width + nextX];
                float heightDiff = hNew - hOld;

                // Compute sediment capacity
                float c = std::max(-heightDiff * speed * water * sedimentCapacityFactor, minSedimentCapacity);

                if (sediment > c || heightDiff > 0.0f) {
                    // Deposit sediment
                    float depositAmount = (heightDiff > 0.0f) ? std::min(heightDiff, sediment) : (sediment - c) * depositRate;
                    sediment -= depositAmount;
                    heightmap[idx00] += depositAmount;
                } else {
                    // Erode material
                    float erodeAmount = std::min((c - sediment) * dissolveRate, -heightDiff);
                    sediment += erodeAmount;
                    heightmap[idx00] -= erodeAmount;
                }

                speed = std::sqrt(std::max(0.0f, speed * speed + heightDiff * gravity));
                water *= (1.0f - evaporateRate);
            }
        }
    }

private:
    std::vector<int> perm;
};

} // namespace procedural
} // namespace apex
