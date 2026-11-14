package com.example.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class Projected2D(
    val screenX: Float,
    val screenY: Float,
    val depth: Float
)

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val l = length()
        return if (l > 0.0001f) Vec3(x / l, y / l, z / l) else Vec3(0f, 1f, 0f)
    }

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
}

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val isVisible: Boolean
)

data class ProjectedTriangle(
    val p1: ProjectedPoint,
    val p2: ProjectedPoint,
    val p3: ProjectedPoint,
    val normal: Vec3,
    val averageDepth: Float,
    val lightingFactor: Float,
    val u: Float = 0.5f,
    val v: Float = 0.5f,
    val centerViewPos: Vec3 = Vec3(0f, 0f, 0f),
    val faceIndex: Int = 0
)

object Math3D {

    fun project3DTo2D(
        x: Float, y: Float, z: Float,
        yaw: Float, pitch: Float,
        scale: Float,
        cx: Float, cy: Float,
        centerX: Float = 0f, centerY: Float = 0f, centerZ: Float = 0f,
        cameraDist: Float = 80f,
        isPerspective: Boolean = true,
        fovDeg: Float = 60f
    ): Projected2D {
        val rx0 = x - centerX
        val ry0 = y - centerY
        val rz0 = z - centerZ

        val cosY = cos(yaw)
        val sinY = sin(yaw)
        val x1 = rx0 * cosY - rz0 * sinY
        val z1 = rx0 * sinY + rz0 * cosY

        val cosP = cos(pitch)
        val sinP = sin(pitch)
        val y2 = ry0 * cosP - z1 * sinP
        val z2 = ry0 * sinP + z1 * cosP

        val perspectiveFactor = if (isPerspective) {
            val fovRad = Math.toRadians(fovDeg.toDouble()).toFloat()
            val tanHalfFov = tan(fovRad * 0.5f).coerceAtLeast(0.18f)
            val distToCamera = (cameraDist - z2).coerceAtLeast(cameraDist * 0.20f)
            (cameraDist / distToCamera) * (0.577f / tanHalfFov)
        } else {
            1.0f
        }

        val screenX = cx + (x1 * scale * perspectiveFactor)
        val screenY = cy - (y2 * scale * perspectiveFactor)

        return Projected2D(screenX, screenY, z2)
    }

    fun rotateY(v: Vec3, angleRad: Float): Vec3 {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vec3(
            v.x * cosA + v.z * sinA,
            v.y,
            -v.x * sinA + v.z * cosA
        )
    }

    fun rotateX(v: Vec3, angleRad: Float): Vec3 {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vec3(
            v.x,
            v.y * cosA - v.z * sinA,
            v.y * sinA + v.z * cosA
        )
    }

    fun rotateZ(v: Vec3, angleRad: Float): Vec3 {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vec3(
            v.x * cosA - v.y * sinA,
            v.x * sinA + v.y * cosA,
            v.z
        )
    }

    fun transformVertex(
        vertex: Vec3,
        yawRad: Float,
        pitchRad: Float,
        scale: Float,
        panX: Float,
        panY: Float,
        cameraDistance: Float
    ): Vec3 {
        // Rotate around Y then X
        var p = rotateY(vertex * scale, yawRad)
        p = rotateX(p, pitchRad)
        // Camera translation (camera looking down -Z axis, asset at origin)
        return Vec3(p.x + panX, p.y + panY, p.z - cameraDistance)
    }

    fun projectPerspective(
        viewPos: Vec3,
        screenWidth: Float,
        screenHeight: Float,
        fovFactor: Float = 1.6f
    ): ProjectedPoint {
        // In view space, camera is at origin, object is in negative z
        val z = -viewPos.z
        if (z < 0.1f) {
            return ProjectedPoint(0f, 0f, z, isVisible = false)
        }
        val fov = (screenWidth.coerceAtMost(screenHeight)) * fovFactor
        val projX = (viewPos.x / z) * fov + screenWidth / 2f
        val projY = (-viewPos.y / z) * fov + screenHeight / 2f // Invert Y for screen coords

        return ProjectedPoint(projX, projY, z, isVisible = true)
    }

    fun calculateFaceNormal(v1: Vec3, v2: Vec3, v3: Vec3): Vec3 {
        val edge1 = v2 - v1
        val edge2 = v3 - v1
        return edge1.cross(edge2).normalized()
    }
}
