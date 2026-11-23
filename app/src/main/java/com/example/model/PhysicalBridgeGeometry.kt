package com.example.model

import kotlin.math.*

data class PhysicalBridgeSpec(
    val lengthM: Float,
    val deckWidthM: Float,
    val isFootbridge: Boolean,
    val spanCount: Int,
    val baySpanM: Float,
    val girderDepthM: Float,
    val girderCount: Int,
    val columnDiameterM: Float,
    val columnCountPerBent: Int,
    val parapetWidthM: Float,
    val expansionJointWidthM: Float,
    val wingWallLenM: Float,
    val minClearanceM: Float
)

fun computePhysicalBridgeSpec(
    lengthM: Float,
    widthM: Float,
    lanes: Int,
    highwayType: String
): PhysicalBridgeSpec {
    val isFootbridge = highwayType in setOf("footway", "path", "cycleway", "pedestrian", "steps", "bridleway")
    val safeLenM = lengthM.coerceAtLeast(if (isFootbridge) 4.0f else 6.0f)
    val minLaneWidthM = if (isFootbridge) 3.0f else (lanes.coerceIn(1, 6) * 3.35f + 1.2f)
    val safeWidthM = max(widthM, minLaneWidthM).coerceIn(2.6f, 36.0f)
    val maxBayM = when {
        isFootbridge -> 16.0f
        highwayType in setOf("motorway", "trunk", "primary") -> 28.0f
        highwayType in setOf("secondary", "tertiary") -> 24.0f
        else -> 20.0f
    }
    val spanCount = ceil(safeLenM / maxBayM).toInt().coerceIn(1, 32)
    val baySpanM = safeLenM / spanCount.toFloat()
    val girderDepthM = if (isFootbridge) {
        (baySpanM / 20.0f).coerceIn(0.42f, 1.05f)
    } else {
        (baySpanM / 18.0f).coerceIn(0.75f, 2.25f)
    }
    val girderCount = when {
        safeWidthM < 5.2f -> 2
        safeWidthM < 7.8f -> 3
        else -> max(4, ceil(safeWidthM / 2.65f).toInt())
    }
    val colDiamM = if (isFootbridge) 0.55f else (0.90f + safeWidthM * 0.032f).coerceIn(0.95f, 1.65f)
    val colCount = when {
        safeWidthM < 5.2f -> 1
        safeWidthM < 13.0f -> 2
        else -> 3
    }
    return PhysicalBridgeSpec(
        lengthM = safeLenM,
        deckWidthM = safeWidthM,
        isFootbridge = isFootbridge,
        spanCount = spanCount,
        baySpanM = baySpanM,
        girderDepthM = girderDepthM,
        girderCount = girderCount,
        columnDiameterM = colDiamM,
        columnCountPerBent = colCount,
        parapetWidthM = if (isFootbridge) 0.24f else 0.45f,
        expansionJointWidthM = 0.28f,
        wingWallLenM = (safeWidthM * 0.45f).coerceIn(2.2f, 5.4f),
        minClearanceM = if (isFootbridge) 3.0f else 4.2f
    )
}

/**
 * Constructs a true-to-scale 3D bridge superstructure + grounded piers/columns + steel girders
 * directly from world-meter points, DEM ground elevations, and exact scene scale factors.
 */
fun buildScaledTerrainBridge3D(
    osmId: Long,
    name: String,
    highwayType: String,
    lanes: Int,
    widthM: Float,
    lengthM: Float,
    isSelected: Boolean,
    scenePointsXZ: List<Pair<Float, Float>>,
    groundElevationsM: List<Float>,
    baseDatumElevM: Float,
    scaleXZ: Float,
    scaleYPerM: Float,
    roadElevationOffsetScene: Float = 0.14f
): TerrainBridge3D? {
    if (scenePointsXZ.size < 2 || groundElevationsM.size != scenePointsXZ.size) return null

    // Subdivide short-node bridges into >= 6 segments so camber, piers & ground profile are accurate
    val rawN = scenePointsXZ.size
    val cumFrac = FloatArray(rawN)
    var totalSceneLen = 0f
    for (i in 1 until rawN) {
        val seg = hypot(
            scenePointsXZ[i].first - scenePointsXZ[i - 1].first,
            scenePointsXZ[i].second - scenePointsXZ[i - 1].second
        )
        totalSceneLen += seg
        cumFrac[i] = totalSceneLen
    }
    if (totalSceneLen < 1e-4f) return null
    for (i in 1 until rawN) cumFrac[i] /= totalSceneLen

    val spec = computePhysicalBridgeSpec(lengthM, widthM, lanes, highwayType)
    val slabThicknessM = if (spec.isFootbridge) 0.24f else 0.34f
    val structuralDepthM = slabThicknessM + spec.girderDepthM

    val startGroundM = groundElevationsM.first()
    val endGroundM = groundElevationsM.last()
    val camberRiseM = (spec.baySpanM * 0.015f).coerceIn(0.25f, 1.15f)
    val clearanceBufferM = if (spec.isFootbridge) 1.6f else 1.8f

    fun sampleAtT(t: Float): Triple<Float, Float, Float> {
        val ct = t.coerceIn(0f, 1f)
        for (i in 0 until rawN - 1) {
            if (ct <= cumFrac[i + 1] || i == rawN - 2) {
                val span = (cumFrac[i + 1] - cumFrac[i]).coerceAtLeast(1e-5f)
                val u = ((ct - cumFrac[i]) / span).coerceIn(0f, 1f)
                val x = scenePointsXZ[i].first + (scenePointsXZ[i + 1].first - scenePointsXZ[i].first) * u
                val z = scenePointsXZ[i].second + (scenePointsXZ[i + 1].second - scenePointsXZ[i].second) * u
                val g = groundElevationsM[i] + (groundElevationsM[i + 1] - groundElevationsM[i]) * u
                return Triple(x, g, z)
            }
        }
        return Triple(scenePointsXZ.last().first, groundElevationsM.last(), scenePointsXZ.last().second)
    }

    fun dirAtT(t: Float): Pair<Float, Float> {
        val t0 = (t - 0.02f).coerceIn(0f, 1f)
        val t1 = (t + 0.02f).coerceIn(0f, 1f)
        val a = sampleAtT(t0)
        val b = sampleAtT(t1)
        val dx = b.first - a.first
        val dz = b.third - a.third
        val dl = hypot(dx, dz).coerceAtLeast(1e-4f)
        return (dx / dl) to (dz / dl)
    }

    val sampleSteps = max(spec.spanCount * 3, 8).coerceAtMost(48)
    val centerlineXYZ = ArrayList<Triple<Float, Float, Float>>(sampleSteps + 1)
    val groundSceneY = ArrayList<Float>(sampleSteps + 1)
    val deckElevListM = FloatArray(sampleSteps + 1)

    for (s in 0..sampleSteps) {
        val t = s.toFloat() / sampleSteps.toFloat()
        val (sx, gElev, sz) = sampleAtT(t)
        val approachLinearM = startGroundM + (endGroundM - startGroundM) * t
        // Smooth physical parabolic camber rising over middle while keeping ends anchored to approach terrain
        val envelope = sin(t * PI).toFloat()
        val desiredOverGroundM = gElev + structuralDepthM + clearanceBufferM * envelope
        val deckElevM = max(
            approachLinearM + camberRiseM * envelope + 0.18f,
            desiredOverGroundM
        )
        deckElevListM[s] = deckElevM
        val deckSceneY = (deckElevM - baseDatumElevM) * scaleYPerM + roadElevationOffsetScene
        val gSceneY = (gElev - baseDatumElevM) * scaleYPerM
        centerlineXYZ.add(Triple(sx, deckSceneY, sz))
        groundSceneY.add(gSceneY)
    }

    val girderDepthScene = spec.girderDepthM * scaleYPerM
    val deckThicknessScene = slabThicknessM * scaleYPerM
    val halfWidthScene = (spec.deckWidthM * 0.5f * scaleXZ).coerceAtLeast(0.08f)
    val colRadiusScene = (spec.columnDiameterM * 0.5f * scaleXZ).coerceAtLeast(0.03f)
    val capHalfWidthScene = halfWidthScene + (0.45f * scaleXZ).coerceAtLeast(0.04f)
    val capHalfThickScene = (0.55f * scaleXZ).coerceAtLeast(0.04f)
    val footingHalfWidthScene = capHalfWidthScene + (0.65f * scaleXZ).coerceAtLeast(0.05f)

    fun evalDeckYAtT(t: Float): Pair<Float, Float> {
        val idxF = (t.coerceIn(0f, 1f) * sampleSteps)
        val i0 = floor(idxF).toInt().coerceIn(0, sampleSteps - 1)
        val i1 = (i0 + 1).coerceAtMost(sampleSteps)
        val u = (idxF - i0).coerceIn(0f, 1f)
        val dy = centerlineXYZ[i0].second + (centerlineXYZ[i1].second - centerlineXYZ[i0].second) * u
        val gy = groundSceneY[i0] + (groundSceneY[i1] - groundSceneY[i0]) * u
        return dy to gy
    }

    // Build end abutments + intermediate pier support bents
    val piers = ArrayList<TerrainBridgePier3D>(spec.spanCount + 1)
    val expansionFractions = ArrayList<Float>(spec.spanCount + 1)
    val totalStructuralDepthScene = deckThicknessScene + girderDepthScene

    for (p in 0..spec.spanCount) {
        val t = p.toFloat() / spec.spanCount.toFloat()
        expansionFractions.add(t)
        val (px, _, pz) = sampleAtT(t)
        val (dx, dz) = dirAtT(t)
        val (deckY, groundY) = evalDeckYAtT(t)
        val seatY = deckY - totalStructuralDepthScene
        piers.add(
            TerrainBridgePier3D(
                x = px,
                z = pz,
                groundY = groundY,
                seatY = seatY,
                dirX = dx,
                dirZ = dz,
                colCount = spec.columnCountPerBent,
                colRadiusScene = colRadiusScene,
                capHalfWidthScene = capHalfWidthScene,
                capHalfThickScene = capHalfThickScene,
                footingHalfWidthScene = footingHalfWidthScene
            )
        )
    }

    return TerrainBridge3D(
        osmId = osmId,
        name = name,
        centerlineXYZ = centerlineXYZ,
        groundElevY = groundSceneY,
        halfWidthScene = halfWidthScene,
        girderDepthScene = girderDepthScene,
        deckThicknessScene = deckThicknessScene,
        girderCount = spec.girderCount,
        piers = piers,
        expansionFractions = expansionFractions,
        isSelected = isSelected
    )
}
