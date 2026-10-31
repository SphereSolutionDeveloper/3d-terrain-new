// ─── app/src/main/cpp/NativeEngine_JNI.cpp ───────────────────────────────────
// High-Speed JNI Interface linking Android Kotlin shell directly to C++ Vulkan Native Engine
#include <jni.h>
#include "MagadiSceneEngine.h"
#include "ProceduralSceneEngine.h"
#include "GlbParserEngine.h"
#include "Math3D.h"
#include <vector>
#include <string>

using namespace custom_vulkan;

extern "C" {

JNIEXPORT jfloat JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetElevation(
    JNIEnv* env, jobject /* this */,
    jfloat wx, jfloat wy, jfloat interval, jfloat terracing) {
    return MagadiSceneEngine::getInstance().getMagadiElevation(wx, wy, interval, terracing);
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeCalculateHumanMetrics(
    JNIEnv* env, jobject /* this */,
    jfloat walkDistanceM, jfloat eyeHeightOffsetM) {
    HumanViewpointMetrics m = MagadiSceneEngine::getInstance().calculateHumanMetrics(walkDistanceM, eyeHeightOffsetM);

    jfloatArray result = env->NewFloatArray(7);
    jfloat buffer[7] = {
        m.groundDatumElev,
        m.observerEyeElev,
        m.currentRoadElev,
        m.horizonEscarpmentElev,
        m.verticalClimbM,
        m.headingDeg,
        m.heatMirageDistM
    };
    env->SetFloatArrayRegion(result, 0, 7, buffer);
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateHumanStartVertices(
    JNIEnv* env, jobject /* this */,
    jfloat walkDistanceM, jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateHumanStartScene(
        walkDistanceM, gridSize, contourInterval, stepTerracing);

    // Pack vertices: x, y, z, nx, ny, nz, slopeDeg, rawElev, contourBand, isolineFactor, flags (11 floats per vertex)
    size_t vertCount = mesh.vertices.size();
    size_t floatCount = vertCount * 11;
    std::vector<float> packed(floatCount);

    for (size_t i = 0; i < vertCount; ++i) {
        const auto& v = mesh.vertices[i];
        size_t base = i * 11;
        packed[base + 0] = v.x;
        packed[base + 1] = v.y;
        packed[base + 2] = v.z;
        packed[base + 3] = v.nx;
        packed[base + 4] = v.ny;
        packed[base + 5] = v.nz;
        packed[base + 6] = v.slopeDeg;
        packed[base + 7] = v.rawElevation;
        packed[base + 8] = v.contourBand;
        packed[base + 9] = v.isolineFactor;

        float flag = 0.0f;
        if (v.isRoadSurface) flag = 1.0f;
        else if (v.isWaterSurface) flag = 2.0f;
        else if (v.isMudSurface) flag = 3.0f;
        else if (v.isGravelSurface) flag = 4.0f;
        else if (v.rockWeight > 0.5f) flag = 5.0f;
        packed[base + 10] = flag;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(floatCount));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateHumanStartIndices(
    JNIEnv* env, jobject /* this */,
    jfloat walkDistanceM, jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateHumanStartScene(
        walkDistanceM, gridSize, contourInterval, stepTerracing);

    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray result = env->NewIntArray(count);
    env->SetIntArrayRegion(result, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeBuildPowerGridVertices(
    JNIEnv* env, jobject /* this */,
    jint postCount, jfloat spanDistance, jfloat cornerAngleDeg) {
    NativeModelMesh mesh = ProceduralSceneEngine::getInstance().buildPowerGridNetwork(postCount, spanDistance, cornerAngleDeg);

    size_t vertCount = mesh.vertices.size();
    size_t floatCount = vertCount * 10; // pos(3), norm(3), uv(2), color(2 RG)
    std::vector<float> packed(floatCount);

    for (size_t i = 0; i < vertCount; ++i) {
        const auto& v = mesh.vertices[i];
        size_t b = i * 10;
        packed[b + 0] = v.position.x;
        packed[b + 1] = v.position.y;
        packed[b + 2] = v.position.z;
        packed[b + 3] = v.normal.x;
        packed[b + 4] = v.normal.y;
        packed[b + 5] = v.normal.z;
        packed[b + 6] = v.uv.x;
        packed[b + 7] = v.uv.y;
        packed[b + 8] = v.color.x;
        packed[b + 9] = v.color.y;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(floatCount));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeBuildPowerGridIndices(
    JNIEnv* env, jobject /* this */,
    jint postCount, jfloat spanDistance, jfloat cornerAngleDeg) {
    NativeModelMesh mesh = ProceduralSceneEngine::getInstance().buildPowerGridNetwork(postCount, spanDistance, cornerAngleDeg);

    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray result = env->NewIntArray(count);
    env->SetIntArrayRegion(result, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    return result;
}

} // extern "C"
