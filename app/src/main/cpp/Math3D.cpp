// ─── app/src/main/cpp/Math3D.cpp ─────────────────────────────────────────────
// Native C++ 3D Mathematics Implementation
#include "Math3D.h"
#include <cstring>

namespace custom_vulkan {

Mat4 Mat4::identity() {
    Mat4 res;
    std::memset(res.m, 0, sizeof(res.m));
    res.m[0] = 1.0f;
    res.m[5] = 1.0f;
    res.m[10] = 1.0f;
    res.m[15] = 1.0f;
    return res;
}

Mat4 Mat4::translation(float tx, float ty, float tz) {
    Mat4 res = identity();
    res.m[12] = tx;
    res.m[13] = ty;
    res.m[14] = tz;
    return res;
}

Mat4 Mat4::scaling(float sx, float sy, float sz) {
    Mat4 res = identity();
    res.m[0] = sx;
    res.m[5] = sy;
    res.m[10] = sz;
    return res;
}

Mat4 Mat4::rotationX(float rad) {
    Mat4 res = identity();
    float c = std::cos(rad);
    float s = std::sin(rad);
    res.m[5] = c;
    res.m[6] = s;
    res.m[9] = -s;
    res.m[10] = c;
    return res;
}

Mat4 Mat4::rotationY(float rad) {
    Mat4 res = identity();
    float c = std::cos(rad);
    float s = std::sin(rad);
    res.m[0] = c;
    res.m[2] = -s;
    res.m[8] = s;
    res.m[10] = c;
    return res;
}

Mat4 Mat4::rotationZ(float rad) {
    Mat4 res = identity();
    float c = std::cos(rad);
    float s = std::sin(rad);
    res.m[0] = c;
    res.m[1] = s;
    res.m[4] = -s;
    res.m[5] = c;
    return res;
}

Mat4 Mat4::rotationYawPitchRoll(float yaw, float pitch, float roll) {
    return rotationY(yaw) * rotationX(pitch) * rotationZ(roll);
}

Mat4 Mat4::lookAt(const Vec3& eye, const Vec3& target, const Vec3& up) {
    Vec3 f = (target - eye).normalized();
    Vec3 s = f.cross(up).normalized();
    Vec3 u = s.cross(f);

    Mat4 res = identity();
    res.m[0] = s.x;
    res.m[4] = s.y;
    res.m[8] = s.z;

    res.m[1] = u.x;
    res.m[5] = u.y;
    res.m[9] = u.z;

    res.m[2] = -f.x;
    res.m[6] = -f.y;
    res.m[10] = -f.z;

    res.m[12] = -s.dot(eye);
    res.m[13] = -u.dot(eye);
    res.m[14] = f.dot(eye);

    return res;
}

Mat4 Mat4::perspective(float fovRad, float aspect, float zNear, float zFar) {
    float tanHalfFov = std::tan(fovRad * 0.5f);
    Mat4 res;
    std::memset(res.m, 0, sizeof(res.m));

    res.m[0] = 1.0f / (aspect * tanHalfFov);
    res.m[5] = 1.0f / tanHalfFov;
    res.m[10] = -(zFar + zNear) / (zFar - zNear);
    res.m[11] = -1.0f;
    res.m[14] = -(2.0f * zFar * zNear) / (zFar - zNear);

    return res;
}

Mat4 Mat4::orthographic(float left, float right, float bottom, float top, float zNear, float zFar) {
    Mat4 res = identity();
    res.m[0] = 2.0f / (right - left);
    res.m[5] = 2.0f / (top - bottom);
    res.m[10] = -2.0f / (zFar - zNear);
    res.m[12] = -(right + left) / (right - left);
    res.m[13] = -(top + bottom) / (top - bottom);
    res.m[14] = -(zFar + zNear) / (zFar - zNear);
    return res;
}

Mat4 Mat4::operator*(const Mat4& o) const {
    Mat4 res;
    for (int col = 0; col < 4; ++col) {
        for (int row = 0; row < 4; ++row) {
            float sum = 0.0f;
            for (int k = 0; k < 4; ++k) {
                sum += m[k * 4 + row] * o.m[col * 4 + k];
            }
            res.m[col * 4 + row] = sum;
        }
    }
    return res;
}

Vec4 Mat4::operator*(const Vec4& v) const {
    return {
        m[0] * v.x + m[4] * v.y + m[8]  * v.z + m[12] * v.w,
        m[1] * v.x + m[5] * v.y + m[9]  * v.z + m[13] * v.w,
        m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * v.w,
        m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * v.w
    };
}

Vec3 Mat4::transformPoint(const Vec3& p) const {
    Vec4 res = (*this) * Vec4(p, 1.0f);
    if (std::abs(res.w) > 0.00001f) {
        return {res.x / res.w, res.y / res.w, res.z / res.w};
    }
    return {res.x, res.y, res.z};
}

Vec3 Mat4::transformDirection(const Vec3& d) const {
    Vec4 res = (*this) * Vec4(d, 0.0f);
    return {res.x, res.y, res.z};
}

} // namespace custom_vulkan
