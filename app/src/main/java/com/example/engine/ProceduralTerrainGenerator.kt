// ─── app/src/main/java/com/example/engine/ProceduralTerrainGenerator.kt ───────
// Explicitly powered by Native C++ Engine (libapex_vulkan.so / MagadiSceneEngine.cpp)
// All procedural mathematics, FBM noise, and terrain chunks run in native C/C++
package com.example.engine

import com.example.model.GameTerrainAssetPlacement
import com.example.model.TerrainMesh3D
import com.example.model.TerrainState
import com.example.model.TerrainVertex
import kotlin.math.*

object ProceduralTerrainGenerator {

    /**
     * Samples continuous elevation across the 50km Magadi Road corridor.
     * Evaluated directly in C++ via MagadiSceneEngine::getMagadiElevation.
     */
    fun getMagadiElevation(
        worldXMeters: Float,
        worldYMeters: Float,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): Float {
        if (NativeEngineBridge.isLoaded()) {
            return NativeEngineBridge.nativeGetElevation(worldXMeters, worldYMeters, contourInterval, stepTerracing)
        }
        // Minimal JNI initialization fallback
        val ny = (worldYMeters / 50000f).coerceIn(0f, 1f)
        val base = 598.0f + (ny.pow(1.42f) * 1382.0f)
        return if (stepTerracing > 0.001f && contourInterval > 0.5f) {
            val stepped = (base / contourInterval).roundToInt() * contourInterval
            base * (1f - stepTerracing) + stepped * stepTerracing
        } else {
            base
        }
    }

    /**
     * Generates Detailed 3D Human Eye-Level Viewpoint Scene at Magadi Road Start (598m).
     * Computed directly by Native C++ Engine (MagadiSceneEngine.cpp).
     */
    fun generateMagadiHumanStartMesh(
        walkDistanceM: Float = 0f,
        gridSize: Int = 45,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D {
        if (NativeEngineBridge.isLoaded()) {
            val packedVerts = NativeEngineBridge.nativeGenerateHumanStartVertices(
                walkDistanceM, gridSize, contourInterval, stepTerracing
            )
            val nativeIndices = NativeEngineBridge.nativeGenerateHumanStartIndices(
                walkDistanceM, gridSize, contourInterval, stepTerracing
            )

            val vertCount = packedVerts.size / 11
            val vertices = ArrayList<TerrainVertex>(vertCount)
            var minH = Float.MAX_VALUE
            var maxH = -Float.MAX_VALUE
            var totalSlope = 0f

            for (i in 0 until vertCount) {
                val b = i * 11
                val x = packedVerts[b + 0]
                val y = packedVerts[b + 1]
                val z = packedVerts[b + 2]
                val nx = packedVerts[b + 3]
                val ny = packedVerts[b + 4]
                val nz = packedVerts[b + 5]
                val slopeDeg = packedVerts[b + 6]
                val rawElev = packedVerts[b + 7]
                val contourBand = packedVerts[b + 8]
                val isolineFactor = packedVerts[b + 9]
                val flag = packedVerts[b + 10]

                if (y < minH) minH = y
                if (y > maxH) maxH = y
                totalSlope += slopeDeg

                val surfType = when (flag.toInt()) {
                    1 -> "asphalt"
                    2 -> "water"
                    3 -> "trona_soda_crust"
                    4 -> "gravel"
                    5 -> "escarpment_rock"
                    else -> "savannah_scrub"
                }

                vertices.add(
                    TerrainVertex(
                        x = x,
                        y = y,
                        z = z,
                        nx = nx,
                        ny = ny,
                        nz = nz,
                        slopeDeg = slopeDeg,
                        grassWeight = if (surfType == "savannah_scrub") 1f else 0f,
                        dirtWeight = if (surfType == "gravel" || flag == 4f) 1f else 0f,
                        rockWeight = if (surfType == "escarpment_rock" || slopeDeg > 28f) 0.85f else 0f,
                        snowWeight = if (surfType == "trona_soda_crust") 1f else 0f,
                        isCaveHollow = false,
                        isRoadSurface = flag == 1.0f,
                        surfaceType = surfType,
                        isWaterSurface = flag == 2.0f,
                        isMudSurface = flag == 3.0f,
                        isGravelSurface = flag == 4.0f,
                        rawElevation = rawElev,
                        contourBand = contourBand,
                        isolineFactor = isolineFactor
                    )
                )
            }

            val indices = ArrayList<Int>(nativeIndices.size)
            for (idx in nativeIndices) {
                indices.add(idx)
            }

            val meshScale = 0.08f
            val assetMarkers = listOf(
                GameTerrainAssetPlacement("c58_start_sign", "spawn", "assets/markers/start_grid.glb", 0f, 0.8f * meshScale, (20f - 400f) * meshScale, 0f, 1f),
                GameTerrainAssetPlacement("magadi_soda_factory", "garage", "assets/structures/garage_hub.glb", -180f * meshScale, 0f, (80f - 400f) * meshScale, 0f, 1.2f),
                GameTerrainAssetPlacement("causeway_bridge", "bridge", "assets/structures/concrete_bridge.glb", 0f, 0.8f * meshScale, (320f - 400f) * meshScale, 0f, 1f),
                GameTerrainAssetPlacement("acacia_clump", "tree", "assets/nature/palm_tree.glb", 18f * meshScale, 0f, (120f - 400f) * meshScale, 0f, 0.9f),
                GameTerrainAssetPlacement("basalt_boulder", "rock", "assets/nature/boulder_1.5m.glb", -16f * meshScale, 0f, (240f - 400f) * meshScale, 0f, 1.3f)
            )

            return TerrainMesh3D(
                resolutionX = gridSize,
                resolutionZ = gridSize,
                vertices = vertices,
                indices = indices,
                minHeight = minH,
                maxHeight = maxH,
                avgSlope = if (vertCount > 0) totalSlope / vertCount else 5f,
                assetPlacements = assetMarkers
            )
        }

        // Bridge stub generating C++ coordinate space
        return generateFallbackMesh(gridSize)
    }

    fun generateMagadiOverviewMesh(
        gridSize: Int = 45,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D = generateMagadiHumanStartMesh(0f, gridSize, contourInterval, stepTerracing)

    fun generateMagadiChunkMesh(
        col: Int,
        row: Int,
        gridSize: Int = 41,
        contourInterval: Float = 25f,
        stepTerracing: Float = 0f
    ): TerrainMesh3D = generateMagadiHumanStartMesh(row * 2000f, gridSize, contourInterval, stepTerracing)

    fun generateTestGround001Mesh(gridSize: Int = 49): TerrainMesh3D =
        generateMagadiHumanStartMesh(0f, gridSize, 25f, 0f)

    fun generateTerrainMesh(state: TerrainState, gridSize: Int = 33, worldSize: Float = 100f): TerrainMesh3D =
        generateMagadiHumanStartMesh(0f, gridSize, state.contourInterval, state.stepTerracingFactor)

    fun simulateErosionCycle(state: TerrainState): TerrainState =
        state.copy(erosionRate = (state.erosionRate + 0.05f).coerceAtMost(1.0f))

    private fun generateFallbackMesh(gridSize: Int): TerrainMesh3D {
        val total = gridSize * gridSize
        val verts = ArrayList<TerrainVertex>(total)
        for (iz in 0 until gridSize) {
            for (ix in 0 until gridSize) {
                verts.add(
                    TerrainVertex(
                        x = (ix - gridSize / 2f) * 2f,
                        y = 0f,
                        z = (iz - gridSize / 2f) * 2f,
                        nx = 0f, ny = 1f, nz = 0f,
                        slopeDeg = 0f,
                        grassWeight = 1f, dirtWeight = 0f, rockWeight = 0f, snowWeight = 0f,
                        isCaveHollow = false, isRoadSurface = ix == gridSize / 2,
                        surfaceType = "asphalt", isWaterSurface = false,
                        isMudSurface = false, isGravelSurface = false,
                        rawElevation = 598f, contourBand = 0f, isolineFactor = 0f
                    )
                )
            }
        }
        val indices = ArrayList<Int>((gridSize - 1) * (gridSize - 1) * 6)
        for (iz in 0 until gridSize - 1) {
            for (ix in 0 until gridSize - 1) {
                val tl = iz * gridSize + ix
                val tr = tl + 1
                val bl = (iz + 1) * gridSize + ix
                val br = bl + 1
                indices.add(tl); indices.add(bl); indices.add(tr)
                indices.add(tr); indices.add(bl); indices.add(br)
            }
        }
        return TerrainMesh3D(gridSize, gridSize, verts, indices, 0f, 10f, 0f, emptyList())
    }
}
