// ─── app/src/main/java/com/example/engine/NativeEngineBridge.kt ───────────────
// Native JNI Bridge to C/C++ Vulkan Procedural Engine & Scene (libapex_vulkan.so)
// All engine logic, scene graphs, terrain generation, and 3D math reside in C/C++
package com.example.engine

import android.util.Log

object NativeEngineBridge {
    private const val TAG = "NativeEngineBridge"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("apex_vulkan")
            isNativeLoaded = true
            Log.i(TAG, "Successfully loaded native C++ engine library: libapex_vulkan.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library load failed: ${e.message}")
            isNativeLoaded = false
        }
    }

    fun isLoaded(): Boolean = isNativeLoaded

    // Native C++ Magadi Elevation calculation
    external fun nativeGetElevation(wx: Float, wy: Float, interval: Float, terracing: Float): Float

    // Native C++ Human Viewpoint Metrics
    external fun nativeCalculateHumanMetrics(walkDistanceM: Float, eyeHeightOffsetM: Float): FloatArray

    // Native C++ Magadi Human Start Mesh Vertices & Indices
    external fun nativeGenerateHumanStartVertices(
        walkDistanceM: Float,
        gridSize: Int,
        contourInterval: Float,
        stepTerracing: Float
    ): FloatArray

    external fun nativeGenerateHumanStartIndices(
        walkDistanceM: Float,
        gridSize: Int,
        contourInterval: Float,
        stepTerracing: Float
    ): IntArray

    // Native C++ Procedural Asset Generation
    external fun nativeBuildPowerGridVertices(postCount: Int, spanDistance: Float, cornerAngleDeg: Float): FloatArray
    external fun nativeBuildPowerGridIndices(postCount: Int, spanDistance: Float, cornerAngleDeg: Float): IntArray
}
