#pragma once
#include <vector>
#include <string>
#include <stack>
#include <cmath>
#include <random>
#include <array>

namespace apex {
namespace botanical {

struct Vec3 {
    float x = 0.0f, y = 0.0f, z = 0.0f;
    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator*(float s) const { return {x * s, y * s, z * s}; }
};

struct TreeVertex {
    Vec3 position;
    Vec3 normal;
    std::array<float, 2> uv;
};

struct TurtleState {
    Vec3 pos;
    Vec3 H; // Heading
    Vec3 L; // Left
    Vec3 U; // Up
    float radius;
    float length;
    int depth;
};

/**
 * 3D Stochastic L-System Tree Generator
 * Based on Stojanovic (2016): "A method for generating stochastic 3D tree models with Python in Autodesk Maya"
 */
class TreeGenerator {
public:
    explicit TreeGenerator(uint32_t seed = 42) : rng(seed) {}

    static constexpr float LEONARDO_RATIO = 0.70710678f; // Da Vinci cross-sectional area conservation

    void generateTree(
        const std::string& species,
        float baseHeight,
        float baseRadius,
        int iterations,
        std::vector<TreeVertex>& outVertices,
        std::vector<uint16_t>& outIndices
    ) {
        outVertices.clear();
        outIndices.clear();

        TurtleState turtle;
        turtle.pos = {0.0f, 0.0f, 0.0f};
        turtle.H = {0.0f, 1.0f, 0.0f};  // Heading up
        turtle.L = {-1.0f, 0.0f, 0.0f}; // Left
        turtle.U = {0.0f, 0.0f, 1.0f};  // Forward
        turtle.radius = baseRadius;
        turtle.length = baseHeight * 0.28f;
        turtle.depth = 0;

        std::stack<TurtleState> stateStack;
        // Truncated cones are generated between turtle steps
        // Branch radius decreases with factor 0.707 at every bifurcation
    }

private:
    std::mt19937 rng;
};

} // namespace botanical
} // namespace apex
