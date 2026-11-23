package com.example

import com.example.repository.C58CorridorRepository
import com.example.repository.MagadiTownRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BinaryGisEnrichmentV2Test {

    private data class RoadSeg(
        val ax: Double,
        val ay: Double,
        val bx: Double,
        val by: Double,
        val clearanceM: Double
    ) {
        val minX = min(ax, bx) - clearanceM
        val maxX = max(ax, bx) + clearanceM
        val minY = min(ay, by) - clearanceM
        val maxY = max(ay, by) + clearanceM
    }

    private fun resolveAssetFile(filename: String): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(cwd, "native-assets/assets/terrain_grid/$filename"),
            File(cwd.parentFile, "native-assets/assets/terrain_grid/$filename"),
            File(cwd, "app/src/main/assets/terrain_grid/$filename"),
            File(cwd, "src/main/assets/terrain_grid/$filename")
        )
        return candidates.firstOrNull { it.exists() && it.length() > 0L }
            ?: error("Could not locate binary GIS pack $filename from ${cwd.absolutePath}")
    }

    private fun pointToSegmentDistSq(
        px: Double, py: Double,
        ax: Double, ay: Double,
        bx: Double, by: Double
    ): Double {
        val vx = bx - ax
        val vy = by - ay
        val lenSq = vx * vx + vy * vy
        if (lenSq <= 1e-9) {
            val dx = px - ax
            val dy = py - ay
            return dx * dx + dy * dy
        }
        val t = (((px - ax) * vx + (py - ay) * vy) / lenSq).coerceIn(0.0, 1.0)
        val cx = ax + t * vx
        val cy = ay + t * vy
        val dx = px - cx
        val dy = py - cy
        return dx * dx + dy * dy
    }

    private fun orientation(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double): Double {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
    }

    private fun onSegment(ax: Double, ay: Double, bx: Double, by: Double, px: Double, py: Double): Boolean {
        return px >= min(ax, bx) - 1e-6 && px <= max(ax, bx) + 1e-6 &&
            py >= min(ay, by) - 1e-6 && py <= max(ay, by) + 1e-6
    }

    private fun segmentsIntersect(
        ax: Double, ay: Double, bx: Double, by: Double,
        cx: Double, cy: Double, dx: Double, dy: Double
    ): Boolean {
        val o1 = orientation(ax, ay, bx, by, cx, cy)
        val o2 = orientation(ax, ay, bx, by, dx, dy)
        val o3 = orientation(cx, cy, dx, dy, ax, ay)
        val o4 = orientation(cx, cy, dx, dy, bx, by)
        if (((o1 > 1e-7 && o2 < -1e-7) || (o1 < -1e-7 && o2 > 1e-7)) &&
            ((o3 > 1e-7 && o4 < -1e-7) || (o3 < -1e-7 && o4 > 1e-7))
        ) {
            return true
        }
        if (kotlin.math.abs(o1) <= 1e-7 && onSegment(ax, ay, bx, by, cx, cy)) return true
        if (kotlin.math.abs(o2) <= 1e-7 && onSegment(ax, ay, bx, by, dx, dy)) return true
        if (kotlin.math.abs(o3) <= 1e-7 && onSegment(cx, cy, dx, dy, ax, ay)) return true
        if (kotlin.math.abs(o4) <= 1e-7 && onSegment(cx, cy, dx, dy, bx, by)) return true
        return false
    }

    private fun segmentToSegmentDistM(
        ax: Double, ay: Double, bx: Double, by: Double,
        cx: Double, cy: Double, dx: Double, dy: Double
    ): Double {
        if (segmentsIntersect(ax, ay, bx, by, cx, cy, dx, dy)) return 0.0
        val d1 = pointToSegmentDistSq(ax, ay, cx, cy, dx, dy)
        val d2 = pointToSegmentDistSq(bx, by, cx, cy, dx, dy)
        val d3 = pointToSegmentDistSq(cx, cy, ax, ay, bx, by)
        val d4 = pointToSegmentDistSq(dx, dy, ax, ay, bx, by)
        return sqrt(min(min(d1, d2), min(d3, d4)))
    }

    private fun buildRoadGrid(
        roadPolylines: List<Pair<Float, List<Pair<Double, Double>>>>,
        cellSizeM: Double = 50.0
    ): Pair<Double, Map<Long, List<RoadSeg>>> {
        val grid = HashMap<Long, MutableList<RoadSeg>>()
        fun packCell(cx: Int, cy: Int): Long = (cx.toLong() shl 32) xor (cy.toLong() and 0xFFFFFFFFL)

        for ((widthM, nodes) in roadPolylines) {
            val clearanceM = max(widthM.toDouble() * 0.5, 2.2) + 1.0
            for (i in 0 until nodes.size - 1) {
                val (ax, ay) = nodes[i]
                val (bx, by) = nodes[i + 1]
                val seg = RoadSeg(ax, ay, bx, by, clearanceM)
                val x0 = floor(seg.minX / cellSizeM).toInt()
                val x1 = ceil(seg.maxX / cellSizeM).toInt()
                val y0 = floor(seg.minY / cellSizeM).toInt()
                val y1 = ceil(seg.maxY / cellSizeM).toInt()
                for (cx in x0..x1) {
                    for (cy in y0..y1) {
                        grid.getOrPut(packCell(cx, cy)) { ArrayList(4) }.add(seg)
                    }
                }
            }
        }
        return cellSizeM to grid
    }

    private fun countRoadCollisionsForFences(
        cellSizeM: Double,
        grid: Map<Long, List<RoadSeg>>,
        fences: List<List<Pair<Double, Double>>>
    ): Int {
        fun packCell(cx: Int, cy: Int): Long = (cx.toLong() shl 32) xor (cy.toLong() and 0xFFFFFFFFL)
        var violations = 0
        val seen = HashSet<RoadSeg>()
        for (poly in fences) {
            if (poly.size < 3) continue
            var hasViolation = false
            for (i in poly.indices) {
                val (fx0, fy0) = poly[i]
                val (fx1, fy1) = poly[(i + 1) % poly.size]
                val x0 = floor(min(fx0, fx1) / cellSizeM).toInt() - 1
                val x1 = ceil(max(fx0, fx1) / cellSizeM).toInt() + 1
                val y0 = floor(min(fy0, fy1) / cellSizeM).toInt() - 1
                val y1 = ceil(max(fy0, fy1) / cellSizeM).toInt() + 1
                seen.clear()
                for (cx in x0..x1) {
                    for (cy in y0..y1) {
                        val bucket = grid[packCell(cx, cy)] ?: continue
                        for (seg in bucket) {
                            if (!seen.add(seg)) continue
                            if (max(fx0, fx1) < seg.minX || min(fx0, fx1) > seg.maxX ||
                                max(fy0, fy1) < seg.minY || min(fy0, fy1) > seg.maxY
                            ) continue
                            val dist = segmentToSegmentDistM(fx0, fy0, fx1, fy1, seg.ax, seg.ay, seg.bx, seg.by)
                            if (dist < seg.clearanceM - 0.15) {
                                hasViolation = true
                                break
                            }
                        }
                        if (hasViolation) break
                    }
                    if (hasViolation) break
                }
                if (hasViolation) break
            }
            if (hasViolation) violations++
        }
        return violations
    }

    @Test
    fun testMagadiTownBinaryV2EnrichmentAndZeroRoadCollisions() {
        val binFile = resolveAssetFile("magadi_town_3x5km.bin")
        val bundle = MagadiTownRepository.parseBinaryBytes(binFile.readBytes())
        assertTrue("Expected Magadi metadata binary flag", bundle.metadata.isBinaryServed)
        assertTrue("Expected >= 250 buildings in Magadi Town", bundle.buildings.size >= 250)

        val distinctTypes = bundle.buildings.map { it.buildingType }.toSet()
        val distinctShapes = bundle.buildings.map { it.shapeType }.toSet()
        val distinctRoofs = bundle.buildings.map { it.roofType }.toSet()
        val distinctRoofMats = bundle.buildings.map { it.roofMaterial }.toSet()
        val distinctWalls = bundle.buildings.map { it.wallType }.toSet()
        val distinctWindows = bundle.buildings.map { it.windowType }.toSet()

        assertTrue("Expected diverse building types: $distinctTypes", distinctTypes.size >= 5)
        assertTrue("Expected diverse footprint shapes: $distinctShapes", distinctShapes.size >= 4)
        assertTrue("Expected diverse roof types: $distinctRoofs", distinctRoofs.size >= 4)
        assertTrue("Expected diverse roof materials: $distinctRoofMats", distinctRoofMats.size >= 3)
        assertTrue("Expected diverse wall types: $distinctWalls", distinctWalls.size >= 3)
        assertTrue("Expected diverse window types: $distinctWindows", distinctWindows.size >= 3)
        assertTrue("Expected multi-storey structures", bundle.buildings.any { it.levels > 1 })
        assertTrue(
            "Expected compound-contained structures",
            bundle.buildings.any { it.parentCompoundId != 0L && it.parentCompoundName.isNotBlank() }
        )

        val fenced = bundle.buildings.filter { it.hasFence }
        assertTrue("Expected fenced properties in Magadi Town", fenced.size >= 200)
        for (b in fenced) {
            assertTrue("Fence height must be > 0 for ${b.osmId}", b.fenceHeightM > 0f)
            assertTrue("Fence polygon >= 3 pts for ${b.osmId}", b.fencePolygon.size >= 3)
            assertTrue("Every fenced building must have a gate (${b.osmId})", b.hasGate)
            assertTrue("Gate width must be > 0 for ${b.osmId}", b.gateWidthM > 0f)
            assertNotNull("Gate coordinate must be populated for ${b.osmId}", b.gateCoord)
        }

        val sharedCoMembers = bundle.buildings.filter { it.parentCompoundName.startsWith("Shared Compound #") }
        val byId = bundle.buildings.associateBy { it.osmId }
        for (cm in sharedCoMembers) {
            assertTrue("Shared compound co-member ${cm.osmId} must not duplicate fence", !cm.hasFence)
            val rep = byId[cm.parentCompoundId]
            assertNotNull("Shared compound rep ${cm.parentCompoundId} must exist", rep)
            assertTrue("Shared compound rep ${cm.parentCompoundId} must own fence & gate", rep!!.hasFence && rep.hasGate)
        }

        val (cellSizeM, grid) = buildRoadGrid(
            bundle.roads.map { r -> r.widthM to r.nodes.map { it.utmEast to it.utmNorth } }
        )
        val collisions = countRoadCollisionsForFences(
            cellSizeM,
            grid,
            fenced.map { b -> b.fencePolygon.map { it.utmEast to it.utmNorth } }
        )
        assertEquals("Magadi fences must have 0 road overlaps/crossings", 0, collisions)
    }

    @Test
    fun testC58CorridorBinaryV2EnrichmentAndZeroRoadCollisions() {
        val binFile = resolveAssetFile("c58_corridor_3km.bin")
        val bundle = C58CorridorRepository.parseBinaryBytes(binFile.readBytes())
        assertTrue("Expected C58 metadata binary flag", bundle.metadata.isBinaryServed)
        assertTrue("Expected > 30,000 houses in C58 corridor", bundle.houses.size > 30_000)

        val distinctTypes = bundle.houses.map { it.buildingType }.toSet()
        val distinctShapes = bundle.houses.map { it.shapeType }.toSet()
        val distinctRoofs = bundle.houses.map { it.roofType }.toSet()
        val distinctRoofMats = bundle.houses.map { it.roofMaterial }.toSet()
        val distinctWalls = bundle.houses.map { it.wallType }.toSet()
        val distinctWindows = bundle.houses.map { it.windowType }.toSet()

        assertTrue("Expected >= 8 building types in C58: $distinctTypes", distinctTypes.size >= 8)
        assertTrue(
            "Expected canonical footprint shapes (SQUARE, RECTANGULAR, L_SHAPE, T_SHAPE, STAR_CROSS, CIRCULAR_SILO)",
            distinctShapes.containsAll(listOf("SQUARE", "RECTANGULAR", "L_SHAPE", "T_SHAPE", "STAR_CROSS", "CIRCULAR_SILO"))
        )
        assertTrue("Expected >= 5 roof types in C58: $distinctRoofs", distinctRoofs.size >= 5)
        assertTrue("Expected >= 4 roof materials in C58: $distinctRoofMats", distinctRoofMats.size >= 4)
        assertTrue("Expected >= 4 wall types in C58: $distinctWalls", distinctWalls.size >= 4)
        assertTrue("Expected >= 4 window types in C58: $distinctWindows", distinctWindows.size >= 4)
        assertTrue("Expected multi-storey structures in C58", bundle.houses.any { it.levels >= 3 })

        val compoundChildren = bundle.houses.filter { it.parentCompoundId != 0L && it.parentCompoundName.isNotBlank() }
        assertTrue("Expected >= 1,500 compound-contained & shared-compound structures in C58", compoundChildren.size >= 1_500)

        val sharedCoMembers = bundle.houses.filter { it.parentCompoundName.startsWith("Shared Compound #") }
        assertTrue("Expected >= 1,000 shared compound co-members (<=2.25m clustered) in C58", sharedCoMembers.size >= 1_000)
        val byId = bundle.houses.associateBy { it.osmId }
        for (cm in sharedCoMembers) {
            assertTrue("Shared compound co-member ${cm.osmId} must not duplicate fence", !cm.hasFence)
            val rep = byId[cm.parentCompoundId]
            assertNotNull("Shared compound rep ${cm.parentCompoundId} must exist", rep)
            assertTrue("Shared compound rep ${cm.parentCompoundId} must own shared fence & gate", rep!!.hasFence && rep.hasGate)
        }

        val fenced = bundle.houses.filter { it.hasFence }
        assertTrue("Expected >= 6,000 fenced compounds in C58", fenced.size >= 6_000)
        for (h in fenced) {
            assertTrue("Fence height must be > 0 for ${h.osmId}", h.fenceHeightM > 0f)
            assertTrue("Fence polygon >= 3 pts for ${h.osmId}", h.fencePolygon.size >= 3)
            assertTrue("Every fenced structure must have a gate (${h.osmId})", h.hasGate)
            assertTrue("Gate width must be > 0 for ${h.osmId}", h.gateWidthM > 0f)
            assertNotNull("Gate coordinate must be populated for ${h.osmId}", h.gateCoord)
        }

        val (cellSizeM, grid) = buildRoadGrid(
            bundle.roads.map { r -> r.widthM to r.nodes.map { it.utmEast to it.utmNorth } }
        )
        val collisions = countRoadCollisionsForFences(
            cellSizeM,
            grid,
            fenced.map { h -> h.fencePolygon.map { it.utmEast to it.utmNorth } }
        )
        assertEquals("C58 corridor fences must have 0 road overlaps/crossings", 0, collisions)
    }
}
