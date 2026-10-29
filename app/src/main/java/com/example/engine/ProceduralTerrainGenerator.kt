package com.example.engine

import com.example.model.*
import kotlin.math.*

/**
 * Procedural Terrain Generator
 * Mirrors the C++ logic in native-assets/src/terrain/ (FbmNoise, Erosion, SplatGenerator, RoadCut, CaveGen)
 */
object ProceduralTerrainGenerator {

    // Simple pseudo-random permutation table seeded dynamically
    private fun hash2D(x: Int, z: Int, seed: Long): Float {
        var n = (x * 374761393L + z * 668265263L + seed * 1013904223L).toInt()
        n = (n xor (n ushr 13)) * 1274126177
        n = n xor (n ushr 16)
        return ((n and 0x7FFFFFFF).toFloat() / 2147483647.0f)
    }

    private fun smoothNoise2D(x: Float, z: Float, seed: Long): Float {
        val ix = floor(x).toInt()
        val iz = floor(z).toInt()
        val fx = x - ix
        val fz = z - iz

        // Quintic hermite curve: 6t^5 - 15t^4 + 10t^3
        val ux = fx * fx * fx * (fx * (fx * 6f - 15f) + 10f)
        val uz = fz * fz * fz * (fz * (fz * 6f - 15f) + 10f)

        val v00 = hash2D(ix, iz, seed)
        val v10 = hash2D(ix + 1, iz, seed)
        val v01 = hash2D(ix, iz + 1, seed)
        val v11 = hash2D(ix + 1, iz + 1, seed)

        val nx0 = v00 * (1f - ux) + v10 * ux
        val nx1 = v01 * (1f - ux) + v11 * ux
        return nx0 * (1f - uz) + nx1 * uz
    }

    fun sampleFbm(
        x: Float,
        z: Float,
        octaves: Int,
        persistence: Float,
        lacunarity: Float,
        seed: Long,
        domainWarp: Float = 0f,
        ridged: Boolean = false
    ): Float {
        var sampleX = x
        var sampleZ = z

        // Domain warping: Q = (fbm(p + a), fbm(p + b))
        if (domainWarp > 0.001f) {
            val qx = smoothNoise2D(sampleX * 0.8f + 5.2f, sampleZ * 0.8f + 1.3f, seed + 100)
            val qz = smoothNoise2D(sampleX * 0.8f + 1.7f, sampleZ * 0.8f + 9.2f, seed + 200)
            sampleX += (qx - 0.5f) * domainWarp * 4.0f
            sampleZ += (qz - 0.5f) * domainWarp * 4.0f
        }

        var total = 0f
        var frequency = 1f
        var amplitude = 1f
        var maxValue = 0f

        for (i in 0 until octaves) {
            var n = smoothNoise2D(sampleX * frequency, sampleZ * frequency, seed + i * 37)
            if (ridged) {
                // Ridged multifractal: 1.0 - abs(noise * 2 - 1)
                n = 1.0f - abs(n * 2.0f - 1.0f)
                n = n * n
            }
            total += n * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }

        return if (maxValue > 0f) total / maxValue else 0f
    }

    /**
     * Generate full 3D Terrain Mesh
     */
    fun generateTerrainMesh(
        state: TerrainState,
        gridSize: Int = 33, // 33x33 vertices (suitable for 60fps real-time mobile canvas)
        worldSize: Float = 100f
    ): TerrainMesh3D {
        val vertices = ArrayList<TerrainVertex>(gridSize * gridSize)
        val step = worldSize / (gridSize - 1)
        val halfSize = worldSize * 0.5f

        // 1. Raw height calculation
        val heights = FloatArray(gridSize * gridSize)
        var minH = Float.MAX_VALUE
        var maxH = -Float.MAX_VALUE

        val roadCenterZ = 0.0f
        val roadWidthHalf = state.roadWidth * 0.5f

        for (iz in 0 until gridSize) {
            for (ix in 0 until gridSize) {
                val wx = ix * step - halfSize
                val wz = iz * step - halfSize

                // Base procedural elevation
                val nx = (wx + halfSize) / worldSize * 4.0f
                val nz = (wz + halfSize) / worldSize * 4.0f
                var hNorm = sampleFbm(
                    nx, nz,
                    state.octaves,
                    state.persistence,
                    state.lacunarity,
                    state.seed,
                    state.domainWarpStrength,
                    state.ridgedMultifractal
                )

                // Scale to elevation
                var elev = hNorm * state.heightScale

                // Road Corridor Cut & Fill modification
                val distToRoad = abs(wz - roadCenterZ)
                if (distToRoad < roadWidthHalf) {
                    // Level road surface
                    elev = state.heightScale * 0.35f
                } else if (distToRoad < roadWidthHalf + 6.0f) {
                    // Embankment cut / fill slope blend
                    val t = (distToRoad - roadWidthHalf) / 6.0f
                    val smoothT = t * t * (3f - 2f * t)
                    val targetRoadH = state.heightScale * 0.35f
                    elev = targetRoadH * (1f - smoothT) + elev * smoothT
                }

                heights[iz * gridSize + ix] = elev
                if (elev < minH) minH = elev
                if (elev > maxH) maxH = elev
            }
        }

        // 2. Normal, Slope & Splat Weight Evaluation
        var totalSlope = 0f
        for (iz in 0 until gridSize) {
            for (ix in 0 until gridSize) {
                val wx = ix * step - halfSize
                val wz = iz * step - halfSize
                val y = heights[iz * gridSize + ix]

                // Finite difference for normals
                val left = if (ix > 0) heights[iz * gridSize + (ix - 1)] else y
                val right = if (ix < gridSize - 1) heights[iz * gridSize + (ix + 1)] else y
                val up = if (iz > 0) heights[(iz - 1) * gridSize + ix] else y
                val down = if (iz < gridSize - 1) heights[(iz + 1) * gridSize + ix] else y

                val dx = (right - left) / (2f * step)
                val dz = (down - up) / (2f * step)

                val len = sqrt(dx * dx + 1f + dz * dz)
                val nx = -dx / len
                val ny = 1f / len
                val nz = -dz / len

                // Slope angle in degrees from vertical (0 = flat horizontal, 90 = sheer cliff)
                val cosTheta = ny.coerceIn(0f, 1f)
                val slopeDeg = acos(cosTheta) * (180f / PI.toFloat())
                totalSlope += slopeDeg

                // Splat weights computation:
                // Grass on low flatlands, Dirt on moderate slopes, Rock on steep cliffs/high slopes, Snow on peaks
                val isCliff = slopeDeg >= state.cliffSlopeThresholdDeg
                val cliffFactor = ((slopeDeg - (state.cliffSlopeThresholdDeg - 8f)) / 12f).coerceIn(0f, 1f)

                var grassW = 0f
                var dirtW = 0f
                var rockW = 0f
                var snowW = 0f

                if (y >= state.snowElevationMin && !isCliff) {
                    val snowT = ((y - state.snowElevationMin) / (state.heightScale - state.snowElevationMin).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    snowW = snowT
                    rockW = 1f - snowT
                } else if (isCliff) {
                    rockW = 0.85f + 0.15f * cliffFactor
                    dirtW = 0.15f * (1f - cliffFactor)
                } else if (y <= state.grassElevationMax) {
                    grassW = 0.9f - 0.5f * cliffFactor
                    dirtW = 0.1f + 0.5f * cliffFactor
                } else {
                    val blend = ((y - state.grassElevationMax) / (state.dirtElevationMax - state.grassElevationMax).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    grassW = (1f - blend) * (1f - cliffFactor)
                    dirtW = blend * (1f - cliffFactor)
                    rockW = cliffFactor
                }

                val totalW = (grassW + dirtW + rockW + snowW).coerceAtLeast(0.001f)
                grassW /= totalW
                dirtW /= totalW
                rockW /= totalW
                snowW /= totalW

                // Cave hollow flag (subterranean voxels near river valley)
                val isCave = (ix in (gridSize / 3)..(2 * gridSize / 3)) && (iz in (gridSize / 3)..(2 * gridSize / 3)) &&
                        (smoothNoise2D(ix * 0.45f, iz * 0.45f, state.seed + 999) > state.caveThreshold)

                val isRoad = abs(wz - roadCenterZ) <= roadWidthHalf + 0.5f

                vertices.add(
                    TerrainVertex(
                        x = wx,
                        y = y,
                        z = wz,
                        nx = nx,
                        ny = ny,
                        nz = nz,
                        slopeDeg = slopeDeg,
                        grassWeight = grassW,
                        dirtWeight = dirtW,
                        rockWeight = rockW,
                        snowWeight = snowW,
                        isCaveHollow = isCave,
                        isRoadSurface = isRoad
                    )
                )
            }
        }

        // 3. Triangle Indices (Grid quads)
        val indices = ArrayList<Int>((gridSize - 1) * (gridSize - 1) * 6)
        for (iz in 0 until gridSize - 1) {
            for (ix in 0 until gridSize - 1) {
                val topLeft = iz * gridSize + ix
                val topRight = topLeft + 1
                val bottomLeft = (iz + 1) * gridSize + ix
                val bottomRight = bottomLeft + 1

                // First triangle
                indices.add(topLeft)
                indices.add(bottomLeft)
                indices.add(topRight)

                // Second triangle
                indices.add(topRight)
                indices.add(bottomLeft)
                indices.add(bottomRight)
            }
        }

        val avgSlope = totalSlope / (gridSize * gridSize)
        return TerrainMesh3D(gridSize, gridSize, vertices, indices, minH, maxH, avgSlope)
    }

    /**
     * Simulate Hydraulic Erosion step
     */
    fun simulateErosionCycle(state: TerrainState): TerrainState {
        val newPersistence = (state.persistence * 0.98f).coerceAtLeast(0.3f)
        val newDroplets = state.rainDropletCount + 500
        return state.copy(
            persistence = newPersistence,
            rainDropletCount = newDroplets,
            cliffSlopeThresholdDeg = (state.cliffSlopeThresholdDeg * 0.97f).coerceAtLeast(26f)
        )
    }

    /**
     * Generate Test Ground 001 Mesh
     * Exact mathematical implementation matching transport_sim_test_001.json:
     * - 320m x 320m world scale, base elevation 1700m
     * - West Hill (+40m peak, sigma 45)
     * - Central Lake (-15m depth, sigma 22)
     * - River Trench (-8m cut, X ~ 160m, Y in 185..320m)
     * - East Ditches (dx=280..290m)
     * - Superellipse Asphalt Road (n=4, half_side=125m, width=5m)
     * - Mud hazard zones (Y in 25..45m)
     * - 43 3D Assets (Garage, Bridge, Start Line, 30 Forest Trees, 10 Boulders)
     */
    fun generateTestGround001Mesh(gridSize: Int = 49): TerrainMesh3D {
        val TOTAL_M = 320.0f
        val HALF_TOTAL = TOTAL_M * 0.5f // 160.0f
        val step = TOTAL_M / (gridSize - 1)
        val heights = FloatArray(gridSize * gridSize)
        val surfaceTypes = Array(gridSize * gridSize) { "grass" }

        val westX = 10f
        val westY = 160f
        val lakeX = 160f
        val lakeY = 160f

        // Precompute superellipse road centerline points (n=4 superellipse)
        val roadSamples = 360
        val roadPtsX = FloatArray(roadSamples)
        val roadPtsY = FloatArray(roadSamples)
        val halfSide = 125.0f
        for (i in 0 until roadSamples) {
            val th = (i.toFloat() / roadSamples) * 2f * PI.toFloat()
            val cosT = cos(th)
            val sinT = sin(th)
            val signCos = if (cosT >= 0f) 1f else -1f
            val signSin = if (sinT >= 0f) 1f else -1f
            val absCos = abs(cosT).toDouble().pow(0.5).toFloat() // 2/n where n=4 -> 0.5
            val absSin = abs(sinT).toDouble().pow(0.5).toFloat()
            roadPtsX[i] = 160f + halfSide * signCos * absCos
            roadPtsY[i] = 160f + halfSide * signSin * absSin
        }

        var minH = Float.MAX_VALUE
        var maxH = -Float.MAX_VALUE

        // 1. Calculate raw elevation & surface classification
        for (iz in 0 until gridSize) {
            val yWorld = iz * step // 0 .. 320m
            for (ix in 0 until gridSize) {
                val xWorld = ix * step // 0 .. 320m
                var elev = 1700.0f

                // West Hill
                val dWestSq = (xWorld - westX) * (xWorld - westX) + (yWorld - westY) * (yWorld - westY)
                elev += 40.0f * exp(-dWestSq / (2f * 45f * 45f))

                // Central Lake
                val dLakeSq = (xWorld - lakeX) * (xWorld - lakeX) + (yWorld - lakeY) * (yWorld - lakeY)
                val distLake = sqrt(dLakeSq)
                elev -= 15.0f * exp(-dLakeSq / (2f * 22f * 22f))

                // River Trench
                val inRiverX = abs(xWorld - 160f) < 5.0f
                val inRiverY = yWorld in 185.0f..320.0f
                val isRiver = inRiverX && inRiverY
                if (isRiver) {
                    elev -= 8.0f
                }

                // East Ditches
                val ditches = listOf(
                    Triple(280f, 220f, 1.5f),
                    Triple(290f, 160f, 1.0f),
                    Triple(280f, 100f, 0.5f)
                )
                var nearDitch = false
                for ((dx, dy, depth) in ditches) {
                    val dSq = (xWorld - dx) * (xWorld - dx) + (yWorld - dy) * (yWorld - dy)
                    elev -= depth * exp(-dSq / (2f * 8f * 8f))
                    if (sqrt(dSq) < 10f) nearDitch = true
                }

                // Surface zone mapping
                var surface = "grass"
                if (distLake < 20f || isRiver) {
                    surface = "water"
                } else if (yWorld in 25.0f..45.0f && xWorld in 50.0f..270.0f) {
                    surface = "mud"
                } else if (nearDitch) {
                    surface = "gravel"
                }

                // Check distance to road centerline
                var minDistToRoad = Float.MAX_VALUE
                for (r in 0 until roadSamples) {
                    val rDist = sqrt((xWorld - roadPtsX[r]) * (xWorld - roadPtsX[r]) + (yWorld - roadPtsY[r]) * (yWorld - roadPtsY[r]))
                    if (rDist < minDistToRoad) minDistToRoad = rDist
                }
                if (minDistToRoad < 3.5f) { // ROAD_WIDTH=5.0m
                    surface = "asphalt"
                    // Bridge crossing: elevate road over river
                    if (inRiverY && abs(xWorld - 160f) < 8.0f) {
                        elev = 1700.0f // Bridge level deck
                    }
                }

                val idx = iz * gridSize + ix
                heights[idx] = elev
                surfaceTypes[idx] = surface
                if (elev < minH) minH = elev
                if (elev > maxH) maxH = elev
            }
        }

        // 2. Build vertices with normals and surface attributes
        val vertices = ArrayList<TerrainVertex>(gridSize * gridSize)
        var totalSlope = 0f
        // Centered world coordinates in range [-80..+80] for ideal 3D canvas framing
        val meshScale = 0.5f // scales 320m -> 160m canvas units

        for (iz in 0 until gridSize) {
            val yWorld = iz * step
            val wz = (yWorld - HALF_TOTAL) * meshScale
            for (ix in 0 until gridSize) {
                val xWorld = ix * step
                val wx = (xWorld - HALF_TOTAL) * meshScale
                val rawElev = heights[iz * gridSize + ix]
                // Centered height relative to base 1700 with strict 1:1:1 equal scale ratio
                val wy = (rawElev - 1700.0f) * meshScale

                val left = if (ix > 0) heights[iz * gridSize + (ix - 1)] else rawElev
                val right = if (ix < gridSize - 1) heights[iz * gridSize + (ix + 1)] else rawElev
                val up = if (iz > 0) heights[(iz - 1) * gridSize + ix] else rawElev
                val down = if (iz < gridSize - 1) heights[(iz + 1) * gridSize + ix] else rawElev

                val dx = (right - left) / (2f * step)
                val dz = (down - up) / (2f * step)
                val len = sqrt(dx * dx + 1f + dz * dz)
                val nx = -dx / len
                val ny = 1f / len
                val nz = -dz / len

                val cosTheta = ny.coerceIn(0f, 1f)
                val slopeDeg = acos(cosTheta) * (180f / PI.toFloat())
                totalSlope += slopeDeg

                val surf = surfaceTypes[iz * gridSize + ix]
                val isRoad = surf == "asphalt"
                val isWater = surf == "water"
                val isMud = surf == "mud"
                val isGravel = surf == "gravel"

                vertices.add(
                    TerrainVertex(
                        x = wx,
                        y = wy,
                        z = wz,
                        nx = nx,
                        ny = ny,
                        nz = nz,
                        slopeDeg = slopeDeg,
                        grassWeight = if (surf == "grass") 1f else 0f,
                        dirtWeight = if (isMud || isGravel) 1f else 0f,
                        rockWeight = if (slopeDeg > 28f) 0.8f else 0f,
                        snowWeight = if (wy > 30f) 0.9f else 0f,
                        isCaveHollow = false,
                        isRoadSurface = isRoad,
                        surfaceType = surf,
                        isWaterSurface = isWater,
                        isMudSurface = isMud,
                        isGravelSurface = isGravel
                    )
                )
            }
        }

        // 3. Grid triangle indices
        val indices = ArrayList<Int>((gridSize - 1) * (gridSize - 1) * 6)
        for (iz in 0 until gridSize - 1) {
            for (ix in 0 until gridSize - 1) {
                val topLeft = iz * gridSize + ix
                val topRight = topLeft + 1
                val bottomLeft = (iz + 1) * gridSize + ix
                val bottomRight = bottomLeft + 1

                indices.add(topLeft)
                indices.add(bottomLeft)
                indices.add(topRight)

                indices.add(topRight)
                indices.add(bottomLeft)
                indices.add(bottomRight)
            }
        }

        // 4. Asset placements matching transport_sim_test_001.json
        val assets = ArrayList<GameTerrainAssetPlacement>()
        // Garage Hub
        assets.add(
            GameTerrainAssetPlacement(
                id = "garage_001",
                type = "garage",
                glbPath = "assets/structures/garage_hub.glb",
                posX = (300f - HALF_TOTAL) * meshScale,
                posY = 0f,
                posZ = (300f - HALF_TOTAL) * meshScale,
                rotY = 0f,
                scale = 1.0f
            )
        )
        // Concrete Bridge
        assets.add(
            GameTerrainAssetPlacement(
                id = "bridge_001",
                type = "bridge",
                glbPath = "assets/structures/concrete_bridge.glb",
                posX = (160f - HALF_TOTAL) * meshScale,
                posY = 0f,
                posZ = (285f - HALF_TOTAL) * meshScale,
                rotY = 0f,
                scale = 1.0f
            )
        )
        // Start Line
        assets.add(
            GameTerrainAssetPlacement(
                id = "start_line",
                type = "spawn",
                glbPath = "assets/markers/start_grid.glb",
                posX = (300f - HALF_TOTAL) * meshScale,
                posY = 0f,
                posZ = (285f - HALF_TOTAL) * meshScale,
                rotY = -90f,
                scale = 1.0f
            )
        )
        // 30 Trees in East forest
        val treeRand = java.util.Random(42L)
        for (i in 0 until 30) {
            val tx = 250f + treeRand.nextFloat() * 65f
            val ty = 30f + treeRand.nextFloat() * 260f
            assets.add(
                GameTerrainAssetPlacement(
                    id = "tree_$i",
                    type = "tree",
                    glbPath = "assets/nature/palm_tree.glb",
                    posX = (tx - HALF_TOTAL) * meshScale,
                    posY = 0f,
                    posZ = (ty - HALF_TOTAL) * meshScale,
                    rotY = treeRand.nextFloat() * 360f,
                    scale = 0.8f + treeRand.nextFloat() * 0.4f
                )
            )
        }
        // 10 Boulders on West hill
        val rockRand = java.util.Random(1337L)
        for (i in 0 until 10) {
            val rx = 15f + rockRand.nextFloat() * 20f
            val ry = 120f + rockRand.nextFloat() * 60f
            val dWestSq = (rx - westX) * (rx - westX) + (ry - westY) * (ry - westY)
            val rElev = 40.0f * exp(-dWestSq / (2f * 45f * 45f))
            assets.add(
                GameTerrainAssetPlacement(
                    id = "rock_$i",
                    type = "rock",
                    glbPath = "assets/nature/boulder_1.5m.glb",
                    posX = (rx - HALF_TOTAL) * meshScale,
                    posY = rElev * meshScale,
                    posZ = (ry - HALF_TOTAL) * meshScale,
                    rotY = rockRand.nextFloat() * 360f,
                    scale = 0.9f + rockRand.nextFloat() * 0.5f
                )
            )
        }

        val avgSlope = totalSlope / (gridSize * gridSize)
        return TerrainMesh3D(
            resolutionX = gridSize,
            resolutionZ = gridSize,
            vertices = vertices,
            indices = indices,
            minHeight = (minH - 1700f) * meshScale,
            maxHeight = (maxH - 1700f) * meshScale,
            avgSlope = avgSlope,
            assetPlacements = assets
        )
    }

    /**
     * Helper: Great Rift Valley / Magadi elevation function (50km x 50km)
     */
    fun getMagadiElevation(wx: Float, wy: Float): Float {
        val t = (wx / 50000f * 0.45f + wy / 50000f * 0.55f).coerceIn(0f, 1f)
        val baseElev = 590.0f + (1950.0f - 590.0f) * t.toDouble().pow(1.35).toFloat()

        val faultPhase = wx / 3500.0f
        val faultScarp = 45.0f * sin(faultPhase) + 20.0f * sin(faultPhase * 2.3f + wy * 0.0003f)

        val dxVolc = wx - 21500.0f
        val dyVolc = wy - 23000.0f
        val distVolcSq = dxVolc * dxVolc + dyVolc * dyVolc
        val volcano = 550.0f * exp(-distVolcSq / (2.0f * 5500.0f * 5500.0f))

        val dxLake = wx - 6500.0f
        val dyLake = wy - 5000.0f
        val distLakeSq = dxLake * dxLake + dyLake * dyLake
        val lake = -45.0f * exp(-distLakeSq / (2.0f * 7000.0f * 7000.0f))

        val undulation = 12.0f * sin(wx * 0.0012f) * cos(wy * 0.0011f)

        return (baseElev + faultScarp + volcano + lake + undulation).coerceIn(590f, 1980f)
    }

    /**
     * Distance to C58 Magadi Road corridor
     */
    private fun distanceToMagadiRoad(wx: Float, wy: Float): Pair<Float, Float> {
        val waypoints = listOf(
            Triple(48500f, 49200f, 1950f),
            Triple(44000f, 44200f, 1620f),
            Triple(38000f, 37500f, 1380f),
            Triple(31500f, 29000f, 1150f),
            Triple(24000f, 22000f, 1010f),
            Triple(18000f, 15000f, 840f),
            Triple(12000f, 9200f, 690f),
            Triple(7200f, 5100f, 600f),
            Triple(6000f, 3500f, 595f)
        )
        var minDist = Float.MAX_VALUE
        var roadElev = 1000f
        for (i in 0 until waypoints.size - 1) {
            val p0 = waypoints[i]
            val p1 = waypoints[i + 1]
            val dx = p1.first - p0.first
            val dy = p1.second - p0.second
            val lenSq = dx * dx + dy * dy
            val u = if (lenSq > 0.001f) {
                (((wx - p0.first) * dx + (wy - p0.second) * dy) / lenSq).coerceIn(0f, 1f)
            } else 0f
            val px = p0.first + u * dx
            val py = p0.second + u * dy
            val d = sqrt((wx - px) * (wx - px) + (wy - py) * (wy - py))
            if (d < minDist) {
                minDist = d
                roadElev = p0.third + u * (p1.third - p0.third)
            }
        }
        return Pair(minDist, roadElev)
    }

    /**
     * Generate 50km x 50km Master Overview Mesh
     * Displays all 25 chunks and the complete 101km C58 Magadi Road corridor in 1:1:1 scale
     */
    fun generateMagadiOverviewMesh(gridSize: Int = 51): TerrainMesh3D {
        val totalSpanM = 50000.0f
        val step = totalSpanM / (gridSize - 1)
        val meshScale = 160.0f / totalSpanM // 0.0032f (1:1:1 equal scale)
        val halfSpan = totalSpanM * 0.5f

        val heights = FloatArray(gridSize * gridSize)
        val surfaces = Array(gridSize * gridSize) { "savannah_scrub" }
        var minH = Float.MAX_VALUE
        var maxH = -Float.MAX_VALUE

        for (iz in 0 until gridSize) {
            val wy = iz * step
            for (ix in 0 until gridSize) {
                val wx = ix * step
                val (distRoad, rElev) = distanceToMagadiRoad(wx, wy)
                val rawElev = getMagadiElevation(wx, wy)

                val elev = if (distRoad < 300f) {
                    val t = (distRoad / 300f)
                    rElev * (1f - t) + rawElev * t
                } else rawElev

                val surf = when {
                    distRoad < 350f -> "asphalt"
                    wx < 12000f && wy < 12000f && elev < 620f -> "water"
                    elev > 1600f -> "escarpment_rock"
                    sqrt((wx - 21500f) * (wx - 21500f) + (wy - 23000f) * (wy - 23000f)) < 5500f -> "volcanic_ash_talus"
                    else -> "savannah_scrub"
                }

                val idx = iz * gridSize + ix
                heights[idx] = elev
                surfaces[idx] = surf
                if (elev < minH) minH = elev
                if (elev > maxH) maxH = elev
            }
        }

        val vertices = ArrayList<TerrainVertex>(gridSize * gridSize)
        var totalSlope = 0f
        val datumElev = 1000.0f // center reference datum

        for (iz in 0 until gridSize) {
            val wy = iz * step
            val wz = (wy - halfSpan) * meshScale
            for (ix in 0 until gridSize) {
                val wx = ix * step
                val wxScaled = (wx - halfSpan) * meshScale
                val rawElev = heights[iz * gridSize + ix]
                // 1:1:1 Equal Scaling on all three axes
                val wyScaled = (rawElev - datumElev) * meshScale

                val left = if (ix > 0) heights[iz * gridSize + (ix - 1)] else rawElev
                val right = if (ix < gridSize - 1) heights[iz * gridSize + (ix + 1)] else rawElev
                val up = if (iz > 0) heights[(iz - 1) * gridSize + ix] else rawElev
                val down = if (iz < gridSize - 1) heights[(iz + 1) * gridSize + ix] else rawElev

                val dx = (right - left) / (2f * step)
                val dz = (down - up) / (2f * step)
                val len = sqrt(dx * dx + 1f + dz * dz)
                val nx = -dx / len
                val ny = 1f / len
                val nz = -dz / len
                val slopeDeg = acos(ny.coerceIn(0f, 1f)) * (180f / PI.toFloat())
                totalSlope += slopeDeg

                val surf = surfaces[iz * gridSize + ix]
                val isRoad = surf == "asphalt"
                val isWater = surf == "water"

                vertices.add(
                    TerrainVertex(
                        x = wxScaled,
                        y = wyScaled,
                        z = wz,
                        nx = nx,
                        ny = ny,
                        nz = nz,
                        slopeDeg = slopeDeg,
                        grassWeight = if (surf == "savannah_scrub") 1f else 0f,
                        dirtWeight = if (surf == "volcanic_ash_talus") 1f else 0f,
                        rockWeight = if (surf == "escarpment_rock" || slopeDeg > 30f) 0.85f else 0f,
                        snowWeight = 0f,
                        isCaveHollow = false,
                        isRoadSurface = isRoad,
                        surfaceType = surf,
                        isWaterSurface = isWater,
                        isMudSurface = false,
                        isGravelSurface = false
                    )
                )
            }
        }

        val indices = ArrayList<Int>((gridSize - 1) * (gridSize - 1) * 6)
        for (iz in 0 until gridSize - 1) {
            for (ix in 0 until gridSize - 1) {
                val topLeft = iz * gridSize + ix
                val topRight = topLeft + 1
                val bottomLeft = (iz + 1) * gridSize + ix
                val bottomRight = bottomLeft + 1

                indices.add(topLeft)
                indices.add(bottomLeft)
                indices.add(topRight)

                indices.add(topRight)
                indices.add(bottomLeft)
                indices.add(bottomRight)
            }
        }

        // Add iconic waypoints as asset markers along C58 road
        val assets = listOf(
            GameTerrainAssetPlacement("kiserian", "spawn", "assets/markers/start_grid.glb", (48500f - halfSpan) * meshScale, (1950f - datumElev) * meshScale, (49200f - halfSpan) * meshScale, 0f, 1f),
            GameTerrainAssetPlacement("corner_baridi", "rock", "assets/nature/boulder_1.5m.glb", (44000f - halfSpan) * meshScale, (1620f - datumElev) * meshScale, (44200f - halfSpan) * meshScale, 0f, 1f),
            GameTerrainAssetPlacement("olorgesailie", "tree", "assets/nature/palm_tree.glb", (21500f - halfSpan) * meshScale, (1760f - datumElev) * meshScale, (23000f - halfSpan) * meshScale, 0f, 1f),
            GameTerrainAssetPlacement("magadi_town", "garage", "assets/structures/garage_hub.glb", (7200f - halfSpan) * meshScale, (600f - datumElev) * meshScale, (5100f - halfSpan) * meshScale, 0f, 1f),
            GameTerrainAssetPlacement("hot_springs", "bridge", "assets/structures/concrete_bridge.glb", (6000f - halfSpan) * meshScale, (595f - datumElev) * meshScale, (3500f - halfSpan) * meshScale, 0f, 1f)
        )

        return TerrainMesh3D(
            resolutionX = gridSize,
            resolutionZ = gridSize,
            vertices = vertices,
            indices = indices,
            minHeight = (minH - datumElev) * meshScale,
            maxHeight = (maxH - datumElev) * meshScale,
            avgSlope = totalSlope / (gridSize * gridSize),
            assetPlacements = assets
        )
    }

    /**
     * Generate Single 10km x 10km Tile Mesh
     * Strict 1:1:1 scale ratio for local high-definition driving
     */
    fun generateMagadiChunkMesh(col: Int, row: Int, gridSize: Int = 41): TerrainMesh3D {
        val chunkSpanM = 10000.0f
        val step = chunkSpanM / (gridSize - 1)
        val meshScale = 160.0f / chunkSpanM // 0.016f (1:1:1 equal scale)
        val halfSpan = chunkSpanM * 0.5f

        val originX = col * chunkSpanM
        val originY = row * chunkSpanM

        val heights = FloatArray(gridSize * gridSize)
        val surfaces = Array(gridSize * gridSize) { "savannah_scrub" }
        var minH = Float.MAX_VALUE
        var maxH = -Float.MAX_VALUE

        for (iz in 0 until gridSize) {
            val wy = originY + iz * step
            for (ix in 0 until gridSize) {
                val wx = originX + ix * step
                val (distRoad, rElev) = distanceToMagadiRoad(wx, wy)
                val rawElev = getMagadiElevation(wx, wy)

                val elev = if (distRoad < 8f) {
                    rElev
                } else if (distRoad < 25f) {
                    val t = (distRoad - 8f) / 17f
                    rElev * (1f - t) + rawElev * t
                } else rawElev

                val surf = when {
                    distRoad < 8f -> "asphalt"
                    distRoad < 15f -> "gravel"
                    wx < 12000f && wy < 12000f && elev < 620f -> "water"
                    elev > 1600f -> "escarpment_rock"
                    sqrt((wx - 21500f) * (wx - 21500f) + (wy - 23000f) * (wy - 23000f)) < 5500f -> "volcanic_ash_talus"
                    else -> "savannah_scrub"
                }

                val idx = iz * gridSize + ix
                heights[idx] = elev
                surfaces[idx] = surf
                if (elev < minH) minH = elev
                if (elev > maxH) maxH = elev
            }
        }

        val vertices = ArrayList<TerrainVertex>(gridSize * gridSize)
        var totalSlope = 0f
        val midH = (minH + maxH) * 0.5f

        for (iz in 0 until gridSize) {
            val localZ = iz * step
            val wz = (localZ - halfSpan) * meshScale
            for (ix in 0 until gridSize) {
                val localX = ix * step
                val wxScaled = (localX - halfSpan) * meshScale
                val rawElev = heights[iz * gridSize + ix]
                // 1:1:1 Equal Scaling
                val wyScaled = (rawElev - midH) * meshScale

                val left = if (ix > 0) heights[iz * gridSize + (ix - 1)] else rawElev
                val right = if (ix < gridSize - 1) heights[iz * gridSize + (ix + 1)] else rawElev
                val up = if (iz > 0) heights[(iz - 1) * gridSize + ix] else rawElev
                val down = if (iz < gridSize - 1) heights[(iz + 1) * gridSize + ix] else rawElev

                val dx = (right - left) / (2f * step)
                val dz = (down - up) / (2f * step)
                val len = sqrt(dx * dx + 1f + dz * dz)
                val nx = -dx / len
                val ny = 1f / len
                val nz = -dz / len
                val slopeDeg = acos(ny.coerceIn(0f, 1f)) * (180f / PI.toFloat())
                totalSlope += slopeDeg

                val surf = surfaces[iz * gridSize + ix]
                val isRoad = surf == "asphalt"
                val isWater = surf == "water"

                vertices.add(
                    TerrainVertex(
                        x = wxScaled,
                        y = wyScaled,
                        z = wz,
                        nx = nx,
                        ny = ny,
                        nz = nz,
                        slopeDeg = slopeDeg,
                        grassWeight = if (surf == "savannah_scrub") 1f else 0f,
                        dirtWeight = if (surf == "volcanic_ash_talus" || surf == "gravel") 1f else 0f,
                        rockWeight = if (surf == "escarpment_rock" || slopeDeg > 30f) 0.85f else 0f,
                        snowWeight = 0f,
                        isCaveHollow = false,
                        isRoadSurface = isRoad,
                        surfaceType = surf,
                        isWaterSurface = isWater,
                        isMudSurface = false,
                        isGravelSurface = surf == "gravel"
                    )
                )
            }
        }

        val indices = ArrayList<Int>((gridSize - 1) * (gridSize - 1) * 6)
        for (iz in 0 until gridSize - 1) {
            for (ix in 0 until gridSize - 1) {
                val topLeft = iz * gridSize + ix
                val topRight = topLeft + 1
                val bottomLeft = (iz + 1) * gridSize + ix
                val bottomRight = bottomLeft + 1

                indices.add(topLeft)
                indices.add(bottomLeft)
                indices.add(topRight)

                indices.add(topRight)
                indices.add(bottomLeft)
                indices.add(bottomRight)
            }
        }

        return TerrainMesh3D(
            resolutionX = gridSize,
            resolutionZ = gridSize,
            vertices = vertices,
            indices = indices,
            minHeight = (minH - midH) * meshScale,
            maxHeight = (maxH - midH) * meshScale,
            avgSlope = totalSlope / (gridSize * gridSize),
            assetPlacements = emptyList()
        )
    }
}
