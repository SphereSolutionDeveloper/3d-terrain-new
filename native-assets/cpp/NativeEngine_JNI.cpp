// ─── app/src/main/cpp/NativeEngine_JNI.cpp ───────────────────────────────────
// High-Speed JNI Interface linking Android Kotlin shell directly to C++ Vulkan Native Engine
#include <jni.h>
#include "MagadiSceneEngine.h"
#include "MagadiTownEngine.h"
#include "C58CorridorEngine.h"
#include "ProceduralSceneEngine.h"
#include "GlbParserEngine.h"
#include "Math3D.h"
#include "vulkan_road_components.h"
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
    if (result && floatCount > 0) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    }
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
    if (result && count > 0) {
        env->SetIntArrayRegion(result, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    }
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadSegmentVertices(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateRoadSegmentScene(
        segmentIndex, gridSize, contourInterval, stepTerracing);

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
        else if (v.grassWeight > 0.5f) flag = 6.0f;
        packed[base + 10] = flag;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(floatCount));
    if (result && floatCount > 0) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    }
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadSegmentIndices(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateRoadSegmentScene(
        segmentIndex, gridSize, contourInterval, stepTerracing);

    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray result = env->NewIntArray(count);
    if (result && count > 0) {
        env->SetIntArrayRegion(result, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    }
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadSubSegmentVertices(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jint subSegmentIndex, jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateRoadSubSegmentScene(
        segmentIndex, subSegmentIndex, gridSize, contourInterval, stepTerracing);

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
        else if (v.grassWeight > 0.5f) flag = 6.0f;
        packed[base + 10] = flag;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(floatCount));
    if (result && floatCount > 0) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    }
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadSubSegmentIndices(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jint subSegmentIndex, jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateRoadSubSegmentScene(
        segmentIndex, subSegmentIndex, gridSize, contourInterval, stepTerracing);

    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray result = env->NewIntArray(count);
    if (result && count > 0) {
        env->SetIntArrayRegion(result, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    }
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerate1mResolutionVertices(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jfloat centerForwardM, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generate1mResolutionScene(
        segmentIndex, centerForwardM, contourInterval, stepTerracing);

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
        else if (v.grassWeight > 0.5f) flag = 6.0f;
        packed[base + 10] = flag;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(floatCount));
    if (result && floatCount > 0) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    }
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerate1mResolutionIndices(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jfloat centerForwardM, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generate1mResolutionScene(
        segmentIndex, centerForwardM, contourInterval, stepTerracing);

    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray result = env->NewIntArray(count);
    if (result && count > 0) {
        env->SetIntArrayRegion(result, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    }
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateWholeCombinedTerrainVertices(
    JNIEnv* env, jobject /* this */,
    jint gridResX, jint gridResZ, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateWholeCombinedTerrainScene(
        gridResX, gridResZ, contourInterval, stepTerracing);

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
        else if (v.grassWeight > 0.5f) flag = 6.0f;
        packed[base + 10] = flag;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(floatCount));
    if (result && floatCount > 0) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    }
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateWholeCombinedTerrainIndices(
    JNIEnv* env, jobject /* this */,
    jint gridResX, jint gridResZ, jfloat contourInterval, jfloat stepTerracing) {
    MagadiSceneMesh mesh = MagadiSceneEngine::getInstance().generateWholeCombinedTerrainScene(
        gridResX, gridResZ, contourInterval, stepTerracing);

    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray result = env->NewIntArray(count);
    if (result && count > 0) {
        env->SetIntArrayRegion(result, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    }
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetCombinedRoadCenterline(
    JNIEnv* env, jobject /* this */,
    jint numPoints) {
    int points = std::clamp(numPoints, 16, 256);
    const float depthM = 101000.0f;
    const float widthM = 2400.0f;
    const float sceneWidth = 28.0f;
    const float sceneDepth = 220.0f;
    const float baseDatumElev = 1289.0f;

    std::vector<float> packed(points * 13);
    float step = depthM / (points - 1);

    for (int i = 0; i < points; ++i) {
        float gM = i * step;
        int seg = std::clamp(static_cast<int>(gM / 10100.0f) + 1, 1, 10);
        float localFwd = gM - (seg - 1) * 10100.0f;
        RoadWaypoint wp = MagadiSceneEngine::getInstance().getRoadPointAt(seg, localFwd);

        size_t b = i * 13;
        packed[b + 0] = (wp.lateralM / widthM) * sceneWidth;
        packed[b + 1] = ((wp.rawElevation - baseDatumElev) / 100.0f) * 12.0f + 0.25f;
        packed[b + 2] = ((gM - depthM * 0.5f) / depthM) * sceneDepth;
        packed[b + 3] = wp.rawElevation;
        packed[b + 4] = localFwd;
        packed[b + 5] = wp.globalKmM;
        packed[b + 6] = wp.lateralM;
        packed[b + 7] = wp.gradePercent;
        packed[b + 8] = wp.headingDeg;
        packed[b + 9] = wp.naturalElevM;
        packed[b + 10] = wp.cutFillDepthM;
        packed[b + 11] = wp.nextContourElevM;
        packed[b + 12] = wp.distToNextContourM;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(packed.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(packed.size()), packed.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetRoadSegmentInfo(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex) {
    RoadSegmentInfo info = MagadiSceneEngine::getInstance().getRoadSegmentInfo(segmentIndex);

    jfloatArray result = env->NewFloatArray(8);
    jfloat buffer[8] = {
        static_cast<jfloat>(info.segmentIndex),
        info.startKmM,
        info.endKmM,
        info.startElevM,
        info.endElevM,
        info.avgGradePercent,
        info.minElevM,
        info.maxElevM
    };
    env->SetFloatArrayRegion(result, 0, 8, buffer);
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetGroundPieceMetrics(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jfloat groundDatumM) {
    RoadSegmentInfo info = MagadiSceneEngine::getInstance().getRoadSegmentInfo(segmentIndex);
    float datum = (groundDatumM > 0.0f) ? groundDatumM : MagadiSceneEngine::GROUND_PIECE_DATUM_ASL;
    float depthMin = std::max(0.0f, info.minElevM - datum);
    float depthMax = std::max(0.0f, info.maxElevM - datum);
    float depthRoadStart = std::max(0.0f, info.startElevM - datum);
    float depthRoadEnd = std::max(0.0f, info.endElevM - datum);
    float verticalRelief = info.maxElevM - info.minElevM;

    jfloatArray result = env->NewFloatArray(6);
    jfloat buffer[6] = {
        datum,
        depthMin,
        depthMax,
        depthRoadStart,
        depthRoadEnd,
        verticalRelief
    };
    env->SetFloatArrayRegion(result, 0, 6, buffer);
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateFirst10kmSegmentVertices(
    JNIEnv* env, jobject /* this */,
    jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    return Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadSegmentVertices(
        env, nullptr, 1, gridSize, contourInterval, stepTerracing);
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateFirst10kmSegmentIndices(
    JNIEnv* env, jobject /* this */,
    jint gridSize, jfloat contourInterval, jfloat stepTerracing) {
    return Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadSegmentIndices(
        env, nullptr, 1, gridSize, contourInterval, stepTerracing);
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

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetRoadCenterline(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jint numPoints) {
    std::vector<RoadWaypoint> points = MagadiSceneEngine::getInstance().generateRoadCenterline(segmentIndex, numPoints);
    jsize pointCount = static_cast<jsize>(points.size());
    jsize floatCount = pointCount * 13;
    std::vector<jfloat> packed(floatCount);
    for (jsize i = 0; i < pointCount; ++i) {
        jsize b = i * 13;
        packed[b + 0] = points[i].x;
        packed[b + 1] = points[i].y;
        packed[b + 2] = points[i].z;
        packed[b + 3] = points[i].rawElevation;
        packed[b + 4] = points[i].forwardM;
        packed[b + 5] = points[i].globalKmM;
        packed[b + 6] = points[i].lateralM;
        packed[b + 7] = points[i].gradePercent;
        packed[b + 8] = points[i].headingDeg;
        packed[b + 9] = points[i].naturalElevM;
        packed[b + 10] = points[i].cutFillDepthM;
        packed[b + 11] = points[i].nextContourElevM;
        packed[b + 12] = points[i].distToNextContourM;
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetRoadPointAt(
    JNIEnv* env, jobject /* this */,
    jint segmentIndex, jfloat forwardM) {
    RoadWaypoint wp = MagadiSceneEngine::getInstance().getRoadPointAt(segmentIndex, forwardM);
    jfloat buffer[13] = {
        wp.x, wp.y, wp.z, wp.rawElevation,
        wp.forwardM, wp.globalKmM, wp.lateralM, wp.gradePercent, wp.headingDeg,
        wp.naturalElevM, wp.cutFillDepthM, wp.nextContourElevM, wp.distToNextContourM
    };
    jfloatArray result = env->NewFloatArray(13);
    env->SetFloatArrayRegion(result, 0, 13, buffer);
    return result;
}

// ----------------------------------------------------------------------------
// Vulkan Road Components JNI Bindings (Real-World Architecture & Bending)
// ----------------------------------------------------------------------------
static VulkanRoadComponentEngine s_roadEngine;

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadBridgeVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat spanM, jfloat deckWidthM, jfloat pylonHeightM,
    jfloat curveBendDeg, jfloat bankingDeg, jint cablePairs) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateBridge(
        static_cast<ApexAssets::Road::BridgeType>(type),
        spanM, deckWidthM, pylonHeightM, curveBendDeg, bankingDeg, cablePairs, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadBridgeIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat spanM, jfloat deckWidthM, jfloat pylonHeightM,
    jfloat curveBendDeg, jfloat bankingDeg, jint cablePairs) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateBridge(
        static_cast<ApexAssets::Road::BridgeType>(type),
        spanM, deckWidthM, pylonHeightM, curveBendDeg, bankingDeg, cablePairs, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateTollPlazaVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jint laneCount, jfloat laneWidthM, jfloat canopyClearanceM,
    jfloat plazaLengthM, jboolean hasDynamicVMS) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateTollPlaza(
        static_cast<ApexAssets::Road::TollPlazaType>(type),
        laneCount, laneWidthM, canopyClearanceM, plazaLengthM, hasDynamicVMS, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateTollPlazaIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jint laneCount, jfloat laneWidthM, jfloat canopyClearanceM,
    jfloat plazaLengthM, jboolean hasDynamicVMS) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateTollPlaza(
        static_cast<ApexAssets::Road::TollPlazaType>(type),
        laneCount, laneWidthM, canopyClearanceM, plazaLengthM, hasDynamicVMS, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateTunnelVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat radiusM, jfloat lengthM, jfloat curveBendDeg,
    jfloat jetFanSpacingM, jboolean hasFlaredPortal) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateTunnel(
        static_cast<ApexAssets::Road::TunnelBoreType>(type),
        radiusM, lengthM, curveBendDeg, jetFanSpacingM, hasFlaredPortal, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateTunnelIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat radiusM, jfloat lengthM, jfloat curveBendDeg,
    jfloat jetFanSpacingM, jboolean hasFlaredPortal) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateTunnel(
        static_cast<ApexAssets::Road::TunnelBoreType>(type),
        radiusM, lengthM, curveBendDeg, jetFanSpacingM, hasFlaredPortal, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateConnectedHighwayVertices(
    JNIEnv* env, jobject /* this */, jfloat scale) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.AssembleConnectedHighwayScene(scale, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateConnectedHighwayIndices(
    JNIEnv* env, jobject /* this */, jfloat scale) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.AssembleConnectedHighwayScene(scale, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateInterchangeVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat loopRadiusM, jfloat totalArcAngleDeg,
    jfloat rampWidthM, jfloat elevationDeltaM, jfloat bankingDeg) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateInterchange(
        static_cast<ApexAssets::Road::InterchangeType>(type),
        loopRadiusM, totalArcAngleDeg, rampWidthM, elevationDeltaM, bankingDeg, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateInterchangeIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat loopRadiusM, jfloat totalArcAngleDeg,
    jfloat rampWidthM, jfloat elevationDeltaM, jfloat bankingDeg) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateInterchange(
        static_cast<ApexAssets::Road::InterchangeType>(type),
        loopRadiusM, totalArcAngleDeg, rampWidthM, elevationDeltaM, bankingDeg, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRetainingWallVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat wallHeightM, jfloat wallLengthM, jfloat batterAngleDeg) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateRetainingWall(
        static_cast<ApexAssets::Road::RetainingWallType>(type),
        wallHeightM, wallLengthM, batterAngleDeg, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRetainingWallIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat wallHeightM, jfloat wallLengthM, jfloat batterAngleDeg) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateRetainingWall(
        static_cast<ApexAssets::Road::RetainingWallType>(type),
        wallHeightM, wallLengthM, batterAngleDeg, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateSignpostVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat gantrySpanM, jfloat clearanceH, jfloat boardWidthM,
    jint signCount, jboolean hasDMS) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateSignpost(
        static_cast<ApexAssets::Road::SignpostType>(type),
        gantrySpanM, clearanceH, boardWidthM, static_cast<uint32_t>(signCount), hasDMS, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateSignpostIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat gantrySpanM, jfloat clearanceH, jfloat boardWidthM,
    jint signCount, jboolean hasDMS) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateSignpost(
        static_cast<ApexAssets::Road::SignpostType>(type),
        gantrySpanM, clearanceH, boardWidthM, static_cast<uint32_t>(signCount), hasDMS, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateExpresswayVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jint lanesPerDir, jfloat sectionLengthM, jfloat curveBendDeg,
    jfloat bankingDeg, jboolean hasLighting) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateExpressway(
        static_cast<ApexAssets::Road::ExpresswayType>(type),
        static_cast<uint32_t>(lanesPerDir), sectionLengthM, curveBendDeg, bankingDeg, hasLighting, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateExpresswayIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jint lanesPerDir, jfloat sectionLengthM, jfloat curveBendDeg,
    jfloat bankingDeg, jboolean hasLighting) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateExpressway(
        static_cast<ApexAssets::Road::ExpresswayType>(type),
        static_cast<uint32_t>(lanesPerDir), sectionLengthM, curveBendDeg, bankingDeg, hasLighting, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateBarrierVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat barrierLengthM, jfloat barrierHeightM, jfloat curveBendDeg,
    jfloat postSpacingM, jboolean hasReflectors) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateBarrier(
        static_cast<ApexAssets::Road::BarrierType>(type),
        barrierLengthM, barrierHeightM, curveBendDeg, postSpacingM, hasReflectors, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateBarrierIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat barrierLengthM, jfloat barrierHeightM, jfloat curveBendDeg,
    jfloat postSpacingM, jboolean hasReflectors) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateBarrier(
        static_cast<ApexAssets::Road::BarrierType>(type),
        barrierLengthM, barrierHeightM, curveBendDeg, postSpacingM, hasReflectors, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadBlockVertices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat roadWidthM, jfloat zoneLengthM, jint unitCount, jboolean hasBeacons) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateRoadBlock(
        static_cast<ApexAssets::Road::RoadBlockType>(type),
        roadWidthM, zoneLengthM, static_cast<uint32_t>(unitCount), hasBeacons, verts, indices);

    jsize floatCount = static_cast<jsize>(verts.size() * 12);
    std::vector<jfloat> packed(floatCount);
    for (size_t i = 0; i < verts.size(); ++i) {
        size_t b = i * 12;
        packed[b + 0] = verts[i].pos[0];
        packed[b + 1] = verts[i].pos[1];
        packed[b + 2] = verts[i].pos[2];
        packed[b + 3] = verts[i].normal[0];
        packed[b + 4] = verts[i].normal[1];
        packed[b + 5] = verts[i].normal[2];
        packed[b + 6] = verts[i].uv[0];
        packed[b + 7] = verts[i].uv[1];
        packed[b + 8] = verts[i].color[0];
        packed[b + 9] = verts[i].color[1];
        packed[b + 10] = verts[i].color[2];
        packed[b + 11] = verts[i].color[3];
    }
    jfloatArray result = env->NewFloatArray(floatCount);
    env->SetFloatArrayRegion(result, 0, floatCount, packed.data());
    return result;
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateRoadBlockIndices(
    JNIEnv* env, jobject /* this */,
    jint type, jfloat roadWidthM, jfloat zoneLengthM, jint unitCount, jboolean hasBeacons) {
    std::vector<RoadEngineVertex> verts;
    std::vector<uint32_t> indices;
    s_roadEngine.GenerateRoadBlock(
        static_cast<ApexAssets::Road::RoadBlockType>(type),
        roadWidthM, zoneLengthM, static_cast<uint32_t>(unitCount), hasBeacons, verts, indices);

    jsize idxCount = static_cast<jsize>(indices.size());
    std::vector<jint> intIndices(idxCount);
    for (size_t i = 0; i < indices.size(); ++i) {
        intIndices[i] = static_cast<jint>(indices[i]);
    }
    jintArray result = env->NewIntArray(idxCount);
    env->SetIntArrayRegion(result, 0, idxCount, intIndices.data());
    return result;
}

// ----------------------------------------------------------------------------
// Magadi Town 3 km x 5 km (1 m Grid Resolution) JNI Bindings
// ----------------------------------------------------------------------------

static jfloatArray packMagadiMeshVertices(JNIEnv* env, const MagadiSceneMesh& mesh) {
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
        else if (v.grassWeight > 0.5f) flag = 6.0f;
        packed[base + 10] = flag;
    }

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(floatCount));
    if (result && floatCount > 0) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(floatCount), packed.data());
    }
    return result;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetMagadiTownBounds(
    JNIEnv* env, jobject /* this */) {
    MagadiTownGeoBounds b = MagadiTownEngine::getInstance().getGeoBounds();
    jdouble buf[12] = {
        b.centerLat, b.centerLon,
        b.widthM, b.heightM,
        b.minLat, b.maxLat,
        b.minLon, b.maxLon,
        b.metersPerDegLat, b.metersPerDegLon,
        b.originUtmEast, b.originUtmNorth
    };
    jdoubleArray res = env->NewDoubleArray(12);
    env->SetDoubleArrayRegion(res, 0, 12, buf);
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeMagadiTownLatLonToLocalAndUtm(
    JNIEnv* env, jobject /* this */, jdouble lat, jdouble lon) {
    float xM = 0.0f, yM = 0.0f;
    double utmE = 0.0, utmN = 0.0;
    auto& eng = MagadiTownEngine::getInstance();
    eng.latLonToLocalMetric(lat, lon, xM, yM);
    eng.latLonToUtm37S(lat, lon, utmE, utmN);
    jdouble buf[4] = { static_cast<jdouble>(xM), static_cast<jdouble>(yM), utmE, utmN };
    jdoubleArray res = env->NewDoubleArray(4);
    env->SetDoubleArrayRegion(res, 0, 4, buf);
    return res;
}

JNIEXPORT void JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSetMagadiTownDemGrid(
    JNIEnv* env, jobject /* this */, jint cols, jint rows, jfloatArray elevationsM) {
    if (!env || !elevationsM) return;
    jsize len = env->GetArrayLength(elevationsM);
    if (len <= 0) return;
    std::vector<float> buf(static_cast<size_t>(len));
    env->GetFloatArrayRegion(elevationsM, 0, len, buf.data());
    MagadiTownEngine::getInstance().setDemControlGrid(cols, rows, buf);
}

JNIEXPORT void JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSetMagadiTownFootprints(
    JNIEnv* env, jobject /* this */, jfloatArray roadSegsPacked, jfloatArray buildingRectsPacked) {
    if (!env) return;
    jsize rLen = roadSegsPacked ? env->GetArrayLength(roadSegsPacked) : 0;
    std::vector<float> rBuf(static_cast<size_t>(std::max<jsize>(0, rLen)));
    if (rLen > 0) env->GetFloatArrayRegion(roadSegsPacked, 0, rLen, rBuf.data());

    std::vector<MagadiTownRoadSegment2D> roads;
    for (int i = 0; i + 4 < rLen; i += 5) {
        roads.push_back(MagadiTownRoadSegment2D{
            rBuf[i], rBuf[i + 1], rBuf[i + 2], rBuf[i + 3], rBuf[i + 4]
        });
    }

    jsize bLen = buildingRectsPacked ? env->GetArrayLength(buildingRectsPacked) : 0;
    std::vector<float> bBuf(static_cast<size_t>(std::max<jsize>(0, bLen)));
    if (bLen > 0) env->GetFloatArrayRegion(buildingRectsPacked, 0, bLen, bBuf.data());

    std::vector<MagadiTownBuildingRect2D> bldgs;
    for (int i = 0; i + 3 < bLen; i += 4) {
        bldgs.push_back(MagadiTownBuildingRect2D{
            bBuf[i], bBuf[i + 1], bBuf[i + 2], bBuf[i + 3]
        });
    }

    MagadiTownEngine::getInstance().setFeatureFootprints(roads, bldgs);
}

JNIEXPORT void JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSetMagadiTownLakeAndWater(
    JNIEnv* env, jobject /* this */,
    jfloat lakeLevelAslM, jintArray polyMetaPacked, jfloatArray coordsXY) {
    if (!env) return;
    jsize metaLen = polyMetaPacked ? env->GetArrayLength(polyMetaPacked) : 0;
    std::vector<jint> metaBuf(static_cast<size_t>(std::max<jsize>(0, metaLen)));
    if (metaLen > 0) env->GetIntArrayRegion(polyMetaPacked, 0, metaLen, metaBuf.data());

    jsize cLen = coordsXY ? env->GetArrayLength(coordsXY) : 0;
    std::vector<float> coordBuf(static_cast<size_t>(std::max<jsize>(0, cLen)));
    if (cLen > 0) env->GetFloatArrayRegion(coordsXY, 0, cLen, coordBuf.data());

    std::vector<MagadiTownWaterPolygon2D> polys;
    int offset = 0;
    for (int i = 0; i + 1 < metaLen; i += 2) {
        int ptCount = metaBuf[i];
        bool isSaltPan = (metaBuf[i + 1] != 0);
        int floatsNeeded = ptCount * 2;
        if (ptCount >= 3 && offset + floatsNeeded <= cLen) {
            MagadiTownWaterPolygon2D p;
            p.isSaltPan = isSaltPan;
            p.coordsXY.assign(coordBuf.begin() + offset, coordBuf.begin() + offset + floatsNeeded);
            float minX = 1e9f, minY = 1e9f, maxX = -1e9f, maxY = -1e9f;
            for (int k = 0; k < ptCount; ++k) {
                float px = p.coordsXY[k * 2];
                float py = p.coordsXY[k * 2 + 1];
                if (px < minX) minX = px;
                if (px > maxX) maxX = px;
                if (py < minY) minY = py;
                if (py > maxY) maxY = py;
            }
            p.minX = minX; p.minY = minY; p.maxX = maxX; p.maxY = maxY;
            polys.push_back(std::move(p));
        }
        offset += std::max(0, floatsNeeded);
    }

    MagadiTownEngine::getInstance().setLakeAndWaterPolygons(lakeLevelAslM, polys);
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSampleMagadiTownRaster(
    JNIEnv* env, jobject /* this */,
    jfloat minXM, jfloat maxXM, jfloat minYM, jfloat maxYM, jint cols, jint rows) {
    std::vector<float> raster = MagadiTownEngine::getInstance().sampleElevationRaster(
        minXM, maxXM, minYM, maxYM, cols, rows);
    jsize len = static_cast<jsize>(raster.size());
    jfloatArray res = env->NewFloatArray(len);
    if (res && len > 0) env->SetFloatArrayRegion(res, 0, len, raster.data());
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetMagadiTownTelemetryAt(
    JNIEnv* env, jobject /* this */, jfloat xM, jfloat yM) {
    MagadiTownPointTelemetry t = MagadiTownEngine::getInstance().getTelemetryAt(xM, yM);
    jdouble buf[12] = {
        static_cast<jdouble>(t.xM),
        static_cast<jdouble>(t.yM),
        static_cast<jdouble>(t.cellCol),
        static_cast<jdouble>(t.cellRow),
        t.lat,
        t.lon,
        t.utmEast,
        t.utmNorth,
        static_cast<jdouble>(t.elevM),
        static_cast<jdouble>(t.slopeDeg),
        static_cast<jdouble>(t.waterDepthM),
        t.isWater ? 1.0 : 0.0
    };
    jdoubleArray res = env->NewDoubleArray(12);
    env->SetDoubleArrayRegion(res, 0, 12, buf);
    return res;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateMagadiTownOverviewVertices(
    JNIEnv* env, jobject /* this */,
    jint resX, jint resZ, jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = MagadiTownEngine::getInstance().generateOverview3x5Mesh(
        resX, resZ, contourInterval, stepTerracing, verticalExaggeration);
    return packMagadiMeshVertices(env, mesh);
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateMagadiTownOverviewIndices(
    JNIEnv* env, jobject /* this */,
    jint resX, jint resZ, jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = MagadiTownEngine::getInstance().generateOverview3x5Mesh(
        resX, resZ, contourInterval, stepTerracing, verticalExaggeration);
    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray res = env->NewIntArray(count);
    if (res && count > 0) env->SetIntArrayRegion(res, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    return res;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateMagadiTown1mWindowVertices(
    JNIEnv* env, jobject /* this */,
    jfloat centerXM, jfloat centerYM, jint windowCells, jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = MagadiTownEngine::getInstance().generateLocal1mWindowMesh(
        centerXM, centerYM, windowCells, contourInterval, stepTerracing, verticalExaggeration);
    return packMagadiMeshVertices(env, mesh);
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateMagadiTown1mWindowIndices(
    JNIEnv* env, jobject /* this */,
    jfloat centerXM, jfloat centerYM, jint windowCells, jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = MagadiTownEngine::getInstance().generateLocal1mWindowMesh(
        centerXM, centerYM, windowCells, contourInterval, stepTerracing, verticalExaggeration);
    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray res = env->NewIntArray(count);
    if (res && count > 0) env->SetIntArrayRegion(res, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    return res;
}

// ----------------------------------------------------------------------------
// Route C58 Non-Box 3 km Corridor (+/- 1.5 km Left/Right) JNI Bindings
// ----------------------------------------------------------------------------

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetC58DefaultCenterlineLatLon(
    JNIEnv* env, jobject /* this */) {
    std::vector<double> coords = C58CorridorEngine::getInstance().getDefaultCenterlineLatLon();
    jsize len = static_cast<jsize>(coords.size());
    jdoubleArray res = env->NewDoubleArray(len);
    if (res && len > 0) env->SetDoubleArrayRegion(res, 0, len, coords.data());
    return res;
}

JNIEXPORT void JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSetC58Centerline(
    JNIEnv* env, jobject /* this */, jdoubleArray latLonPairs) {
    if (!env || !latLonPairs) return;
    jsize len = env->GetArrayLength(latLonPairs);
    if (len <= 0) return;
    std::vector<double> buf(static_cast<size_t>(len));
    env->GetDoubleArrayRegion(latLonPairs, 0, len, buf.data());
    C58CorridorEngine::getInstance().setCenterline(buf);
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetC58Summary(
    JNIEnv* env, jobject /* this */) {
    C58CorridorSummary s = C58CorridorEngine::getInstance().getSummary();
    jdouble buf[11] = {
        s.totalLengthM,
        static_cast<jdouble>(s.halfWidthM),
        static_cast<jdouble>(s.centerlinePoints),
        s.minUtmEast,
        s.maxUtmEast,
        s.minUtmNorth,
        s.maxUtmNorth,
        s.minLat,
        s.maxLat,
        s.minLon,
        s.maxLon
    };
    jdoubleArray res = env->NewDoubleArray(11);
    env->SetDoubleArrayRegion(res, 0, 11, buf);
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetC58CorridorRibbonUtm(
    JNIEnv* env, jobject /* this */) {
    std::vector<double> ribbon = C58CorridorEngine::getInstance().getCorridorRibbonUtm();
    jsize len = static_cast<jsize>(ribbon.size());
    jdoubleArray res = env->NewDoubleArray(len);
    if (res && len > 0) env->SetDoubleArrayRegion(res, 0, len, ribbon.data());
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeProjectLatLonToC58(
    JNIEnv* env, jobject /* this */, jdouble lat, jdouble lon) {
    C58ProjectionResult p = C58CorridorEngine::getInstance().projectLatLon(lat, lon);
    jdouble buf[5] = {
        p.utmEast,
        p.utmNorth,
        p.chainageM,
        static_cast<jdouble>(p.lateralOffsetM),
        p.insideCorridor ? 1.0 : 0.0
    };
    jdoubleArray res = env->NewDoubleArray(5);
    if (res) env->SetDoubleArrayRegion(res, 0, 5, buf);
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeC58UtmToLatLon(
    JNIEnv* env, jobject /* this */, jdouble utmEast, jdouble utmNorth) {
    double lat = 0.0, lon = 0.0;
    C58CorridorEngine::getInstance().utm37SToLatLon(utmEast, utmNorth, lat, lon);
    jdouble buf[2] = { lat, lon };
    jdoubleArray res = env->NewDoubleArray(2);
    if (res) env->SetDoubleArrayRegion(res, 0, 2, buf);
    return res;
}

JNIEXPORT void JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSetC58DemGrid(
    JNIEnv* env, jobject /* this */,
    jint uCols, jint vRows, jfloatArray elevationsM) {
    if (!env || !elevationsM) return;
    jsize len = env->GetArrayLength(elevationsM);
    if (len <= 0) return;
    std::vector<float> buf(static_cast<size_t>(len));
    env->GetFloatArrayRegion(elevationsM, 0, len, buf.data());
    C58CorridorEngine::getInstance().setDemControlGrid(uCols, vRows, buf);
}

JNIEXPORT void JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSetC58Footprints(
    JNIEnv* env, jobject /* this */,
    jdoubleArray roadSegsUtm5, jdoubleArray buildingRectsUtm4) {
    if (!env) return;
    jsize rLen = roadSegsUtm5 ? env->GetArrayLength(roadSegsUtm5) : 0;
    std::vector<double> rBuf(static_cast<size_t>(std::max<jsize>(0, rLen)));
    if (rLen > 0) env->GetDoubleArrayRegion(roadSegsUtm5, 0, rLen, rBuf.data());

    jsize bLen = buildingRectsUtm4 ? env->GetArrayLength(buildingRectsUtm4) : 0;
    std::vector<double> bBuf(static_cast<size_t>(std::max<jsize>(0, bLen)));
    if (bLen > 0) env->GetDoubleArrayRegion(buildingRectsUtm4, 0, bLen, bBuf.data());

    C58CorridorEngine::getInstance().setFeatureFootprintsUtm(rBuf, bBuf);
}

JNIEXPORT void JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSetC58LakeAndWater(
    JNIEnv* env, jobject /* this */,
    jfloat lakeLevelAslM, jintArray polyMetaPacked, jdoubleArray coordsEN) {
    if (!env) return;
    jsize metaLen = polyMetaPacked ? env->GetArrayLength(polyMetaPacked) : 0;
    std::vector<int> metaBuf(static_cast<size_t>(std::max<jsize>(0, metaLen)));
    if (metaLen > 0) {
        std::vector<jint> tmp(static_cast<size_t>(metaLen));
        env->GetIntArrayRegion(polyMetaPacked, 0, metaLen, tmp.data());
        for (jsize i = 0; i < metaLen; ++i) metaBuf[i] = static_cast<int>(tmp[i]);
    }
    jsize cLen = coordsEN ? env->GetArrayLength(coordsEN) : 0;
    std::vector<double> coordBuf(static_cast<size_t>(std::max<jsize>(0, cLen)));
    if (cLen > 0) env->GetDoubleArrayRegion(coordsEN, 0, cLen, coordBuf.data());

    C58CorridorEngine::getInstance().setLakeAndWaterPolygonsUtm(lakeLevelAslM, metaBuf, coordBuf);
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeSampleC58UtmRaster(
    JNIEnv* env, jobject /* this */,
    jdouble minUtmE, jdouble maxUtmE, jdouble minUtmN, jdouble maxUtmN, jint cols, jint rows) {
    std::vector<float> raster = C58CorridorEngine::getInstance().sampleUtmRaster(
        minUtmE, maxUtmE, minUtmN, maxUtmN, cols, rows);
    jsize len = static_cast<jsize>(raster.size());
    jfloatArray res = env->NewFloatArray(len);
    if (res && len > 0) env->SetFloatArrayRegion(res, 0, len, raster.data());
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetC58TelemetryAtUtm(
    JNIEnv* env, jobject /* this */, jdouble utmE, jdouble utmN) {
    C58PointTelemetry t = C58CorridorEngine::getInstance().getTelemetryAtUtm(utmE, utmN);
    jdouble buf[11] = {
        t.lat,
        t.lon,
        t.utmEast,
        t.utmNorth,
        t.chainageM,
        static_cast<jdouble>(t.lateralOffsetM),
        t.insideCorridor ? 1.0 : 0.0,
        static_cast<jdouble>(t.elevM),
        static_cast<jdouble>(t.slopeDeg),
        static_cast<jdouble>(t.waterDepthM),
        t.isWater ? 1.0 : 0.0
    };
    jdoubleArray res = env->NewDoubleArray(11);
    env->SetDoubleArrayRegion(res, 0, 11, buf);
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGetC58Sector3dTransform(
    JNIEnv* env, jobject /* this */,
    jdouble sectorStartM, jdouble sectorEndM, jint resU, jint resV, jfloat verticalExaggeration) {
    C58Sector3dTransform tr = C58CorridorEngine::getInstance().getSector3dTransform(
        sectorStartM, sectorEndM, resU, resV, verticalExaggeration);
    jdouble buf[5] = {
        tr.centerUtmE,
        tr.centerUtmN,
        static_cast<jdouble>(tr.sceneScaleXZ),
        static_cast<jdouble>(tr.yScalePerM),
        static_cast<jdouble>(tr.baseDatumElev)
    };
    jdoubleArray res = env->NewDoubleArray(5);
    env->SetDoubleArrayRegion(res, 0, 5, buf);
    return res;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateC58SectorVertices(
    JNIEnv* env, jobject /* this */,
    jdouble sectorStartM, jdouble sectorEndM, jint resU, jint resV,
    jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = C58CorridorEngine::getInstance().generateSector3dMesh(
        sectorStartM, sectorEndM, resU, resV, contourInterval, stepTerracing, verticalExaggeration);
    return packMagadiMeshVertices(env, mesh);
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateC58SectorIndices(
    JNIEnv* env, jobject /* this */,
    jdouble sectorStartM, jdouble sectorEndM, jint resU, jint resV,
    jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = C58CorridorEngine::getInstance().generateSector3dMesh(
        sectorStartM, sectorEndM, resU, resV, contourInterval, stepTerracing, verticalExaggeration);
    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray res = env->NewIntArray(count);
    if (res && count > 0) env->SetIntArrayRegion(res, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    return res;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateC581mWindowVertices(
    JNIEnv* env, jobject /* this */,
    jdouble centerUtmE, jdouble centerUtmN, jint windowCells,
    jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = C58CorridorEngine::getInstance().generateWindow1mMesh(
        centerUtmE, centerUtmN, windowCells, contourInterval, stepTerracing, verticalExaggeration);
    return packMagadiMeshVertices(env, mesh);
}

JNIEXPORT jintArray JNICALL
Java_com_example_engine_NativeEngineBridge_nativeGenerateC581mWindowIndices(
    JNIEnv* env, jobject /* this */,
    jdouble centerUtmE, jdouble centerUtmN, jint windowCells,
    jfloat contourInterval, jfloat stepTerracing, jfloat verticalExaggeration) {
    MagadiSceneMesh mesh = C58CorridorEngine::getInstance().generateWindow1mMesh(
        centerUtmE, centerUtmN, windowCells, contourInterval, stepTerracing, verticalExaggeration);
    jsize count = static_cast<jsize>(mesh.indices.size());
    jintArray res = env->NewIntArray(count);
    if (res && count > 0) env->SetIntArrayRegion(res, 0, count, reinterpret_cast<const jint*>(mesh.indices.data()));
    return res;
}

} // extern "C"


