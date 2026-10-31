// ─── app/src/main/cpp/Math3D.h ───────────────────────────────────────────────
// Native C++ 3D Mathematics Engine for Vulkan Scene Graph & Procedural Terrain
// Complete Vector, Matrix, Projection, Camera, and Geometric Utilities in C++
#pragma once

#include <cmath>
#include <vector>
#include <algorithm>
#include <cstdint>

namespace custom_vulkan {

constexpr float PI = 3.14159265358979323846f;
constexpr float DEG2RAD = PI / 180.0f;
constexpr float RAD2DEG = 180.0f / PI;

struct Vec2 {
    float x = 0.0f;
    float y = 0.0f;

    Vec2() = default;
    Vec2(float _x, float _y) : x(_x), y(_y) {}

    Vec2 operator+(const Vec2& o) const { return {x + o.x, y + o.y}; }
    Vec2 operator-(const Vec2& o) const { return {x - o.x, y - o.y}; }
    Vec2 operator*(float s) const { return {x * s, y * s}; }
    float length() const { return std::sqrt(x * x + y * y); }
};

struct Vec3 {
    float x = 0.0f;
    float y = 0.0f;
    float z = 0.0f;

    Vec3() = default;
    Vec3(float _x, float _y, float _z) : x(_x), y(_y), z(_z) {}

    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator*(float s) const { return {x * s, y * s, z * s}; }
    Vec3 operator/(float s) const { return {x / s, y / s, z / s}; }

    Vec3& operator+=(const Vec3& o) { x += o.x; y += o.y; z += o.z; return *this; }
    Vec3& operator-=(const Vec3& o) { x -= o.x; y -= o.y; z -= o.z; return *this; }
    Vec3& operator*=(float s) { x *= s; y *= s; z *= s; return *this; }

    float lengthSquared() const { return x * x + y * y + z * z; }
    float length() const { return std::sqrt(lengthSquared()); }

    Vec3 normalized() const {
        float l = length();
        return l > 0.00001f ? (*this / l) : Vec3(0, 1, 0);
    }

    float dot(const Vec3& o) const { return x * o.x + y * o.y + z * o.z; }

    Vec3 cross(const Vec3& o) const {
        return {
            y * o.z - z * o.y,
            z * o.x - x * o.z,
            x * o.y - y * o.x
        };
    }
};

struct Vec4 {
    float x = 0.0f;
    float y = 0.0f;
    float z = 0.0f;
    float w = 1.0f;

    Vec4() = default;
    Vec4(float _x, float _y, float _z, float _w = 1.0f) : x(_x), y(_y), z(_z), w(_w) {}
    Vec4(const Vec3& v, float _w = 1.0f) : x(v.x), y(v.y), z(v.z), w(_w) {}

    Vec3 xyz() const { return {x, y, z}; }
};

struct Mat4 {
    float m[16] = {
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    };

    static Mat4 identity();
    static Mat4 translation(float tx, float ty, float tz);
    static Mat4 translation(const Vec3& t) { return translation(t.x, t.y, t.z); }
    static Mat4 scaling(float sx, float sy, float sz);
    static Mat4 scaling(const Vec3& s) { return scaling(s.x, s.y, s.z); }
    static Mat4 rotationX(float rad);
    static Mat4 rotationY(float rad);
    static Mat4 rotationZ(float rad);
    static Mat4 rotationYawPitchRoll(float yaw, float pitch, float roll);
    static Mat4 lookAt(const Vec3& eye, const Vec3& target, const Vec3& up);
    static Mat4 perspective(float fovRad, float aspect, float zNear, float zFar);
    static Mat4 orthographic(float left, float right, float bottom, float top, float zNear, float zFar);

    Mat4 operator*(const Mat4& o) const;
    Vec4 operator*(const Vec4& v) const;
    Vec3 transformPoint(const Vec3& p) const;
    Vec3 transformDirection(const Vec3& d) const;
};

struct NativeAABB {
    Vec3 min = { 1e9f,  1e9f,  1e9f};
    Vec3 max = {-1e9f, -1e9f, -1e9f};

    void expand(const Vec3& p) {
        min.x = std::min(min.x, p.x);
        min.y = std::min(min.y, p.y);
        min.z = std::min(min.z, p.z);
        max.x = std::max(max.x, p.x);
        max.y = std::max(max.y, p.y);
        max.z = std::max(max.z, p.z);
    }

    Vec3 center() const { return (min + max) * 0.5f; }
    Vec3 size() const { return max - min; }
};

} // namespace custom_vulkan
