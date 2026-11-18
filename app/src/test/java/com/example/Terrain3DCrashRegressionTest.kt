package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.engine.NativeEngineBridge
import com.example.model.GameTerrainAssetPlacement
import com.example.model.RoadSymbolType3D
import com.example.model.RoadWaypoint3D
import com.example.model.TerrainBuilding3D
import com.example.model.TerrainMesh3D
import com.example.model.TerrainRenderMode
import com.example.model.TerrainRoadSeg3D
import com.example.model.TerrainRoadSymbol3D
import com.example.model.TerrainVertex
import com.example.ui.components.Terrain3DCanvas
import com.example.ui.components.TerrainProceduralScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Terrain3DCrashRegressionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun `native bridge terrain methods safely fallback when jni unavailable`() {
        val m1 = NativeEngineBridge.generate1mResolutionMesh(1, 0f)
        val mSeg = NativeEngineBridge.generateRoadSegmentMesh(1)
        val mSub = NativeEngineBridge.generateRoadSubSegmentMesh(1, 1)
        val mComb = NativeEngineBridge.generateWholeCombinedTerrainMesh()
        val mTown = NativeEngineBridge.generateMagadiTownOverviewMesh()
        val mC58 = NativeEngineBridge.generateC58Sector3dMesh(0.0, 10000.0)
        assertNotNull(m1)
        assertNotNull(mSeg)
        assertNotNull(mSub)
        assertNotNull(mComb)
        assertNotNull(mTown)
        assertNotNull(mC58)
    }

    @Test
    fun `terrain procedural screen opens and switches between 3d terrain tabs without crashing`() {
        composeRule.setContent {
            MyApplicationTheme {
                TerrainProceduralScreen()
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("terrain_3d_canvas").assertExists()

        composeRule.onNodeWithTag("terrain_switch_magadi_town_3x5").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("terrain_switch_c58_corridor_3km").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `terrain 3d canvas handles empty mesh mismatched grid out of bounds indices and zero zoom`() {
        val mismatchedMesh = TerrainMesh3D(
            resolutionX = 65,
            resolutionZ = 65,
            vertices = listOf(
                TerrainVertex(x = -10f, y = 0f, z = -10f, rawElevation = 602f),
                TerrainVertex(x = 10f, y = 2f, z = -10f, rawElevation = 606f),
                TerrainVertex(x = 10f, y = 4f, z = 10f, rawElevation = 610f),
                TerrainVertex(x = -10f, y = 1f, z = 10f, rawElevation = 604f)
            ),
            indices = listOf(0, 1, 2, 0, 2, 3, 0, 1, 9999, -5, 2, 3),
            minHeight = 0f,
            maxHeight = 4f,
            avgSlope = 5f
        )

        var lastZoomFactor = 1f
        composeRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(400.dp, 400.dp)) {
                    Terrain3DCanvas(
                        mesh = mismatchedMesh,
                        renderMode = TerrainRenderMode.CONTOURS_3D_ELEVATION,
                        yaw = 0.78f,
                        pitch = 0.61f,
                        zoom = 0f,
                        panX = 0f,
                        panY = 0f,
                        lightAngleRad = 0.85f,
                        showGroundPiece = true,
                        lakeLevelAslM = 605f,
                        onRotateDrag = { _, _ -> },
                        onPanDrag = { _, _ -> },
                        onZoomChange = { lastZoomFactor = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("terrain_zoom_reset_btn").performClick()
        composeRule.onNodeWithTag("terrain_zoom_badge").performClick()
        composeRule.waitForIdle()
        assertNotNull(lastZoomFactor)
    }

    @Test
    fun `terrain 3d canvas handles non finite floats buildings roads symbols and all render modes`() {
        val adversarialMesh = TerrainMesh3D(
            resolutionX = 2,
            resolutionZ = 2,
            vertices = listOf(
                TerrainVertex(
                    x = -10f, y = Float.NaN, z = -10f,
                    nx = Float.NaN, ny = Float.POSITIVE_INFINITY, nz = 0f,
                    slopeDeg = Float.NaN, rawElevation = 600f,
                    isWaterSurface = true
                ),
                TerrainVertex(
                    x = 10f, y = 5f, z = -10f,
                    slopeDeg = 45f, rawElevation = Float.NaN,
                    isRoadSurface = true
                ),
                TerrainVertex(
                    x = 10f, y = Float.POSITIVE_INFINITY, z = 10f,
                    rawElevation = 750f, isGravelSurface = true
                ),
                TerrainVertex(
                    x = -10f, y = 2f, z = 10f,
                    rawElevation = 610f
                )
            ),
            indices = listOf(0, 1, 2, 0, 2, 3),
            minHeight = Float.NaN,
            maxHeight = Float.POSITIVE_INFINITY,
            avgSlope = Float.NaN,
            assetPlacements = listOf(
                GameTerrainAssetPlacement("g1", "garage", "", 0f, 1f, 0f, 0f, 1f),
                GameTerrainAssetPlacement("bad", "spawn", "", Float.NaN, 1f, Float.POSITIVE_INFINITY, 0f, 1f)
            ),
            roadCenterline = listOf(
                RoadWaypoint3D(-5f, 1f, -5f, 600f, 0f, 0f, 0f, 0f, 0f),
                RoadWaypoint3D(5f, 3f, 5f, Float.NaN, 100f, 0.1f, 0f, 2f, 45f),
                RoadWaypoint3D(Float.NaN, 4f, 8f, 650f, 200f, 0.2f, 0f, 2f, 45f)
            )
        )

        val buildings = listOf(
            TerrainBuilding3D(
                id = 1L,
                label = "Magadi House",
                baseCornersXZ = listOf(-2f to -2f, 2f to -2f, 2f to 2f, -2f to 2f),
                baseY = 1f,
                roofY = 4f
            ),
            TerrainBuilding3D(
                id = 2L,
                label = "Tiny Far House",
                baseCornersXZ = listOf(0f to 0f, 0.01f to 0f, 0.01f to 0.01f),
                baseY = Float.NaN,
                roofY = Float.NaN
            ),
            TerrainBuilding3D(
                id = 3L,
                label = "Corrupt Corner",
                baseCornersXZ = listOf(Float.NaN to 0f, 1f to 0f, 1f to 1f),
                baseY = 1f,
                roofY = 4f
            )
        )

        val roadSegs = listOf(
            TerrainRoadSeg3D(-4f, 1f, -4f, 4f, 2f, 4f, isPrimary = true),
            TerrainRoadSeg3D(Float.NaN, 1f, 0f, 2f, 2f, 2f)
        )

        val symbols = listOf(
            TerrainRoadSymbol3D(0f, 2f, 0f, 1f, 0f, "C58", RoadSymbolType3D.ROUTE_SHIELD),
            TerrainRoadSymbol3D(1f, 2f, 1f, 0f, 1f, "N", RoadSymbolType3D.DIRECTION_ARROW),
            TerrainRoadSymbol3D(Float.NaN, 2f, 0f, 1f, 0f, "BAD", RoadSymbolType3D.BRIDGE_ABUTMENT)
        )

        val activeModeState = androidx.compose.runtime.mutableStateOf(TerrainRenderMode.SHADED_TRIPLANAR)
        composeRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.size(400.dp, 400.dp)) {
                    Terrain3DCanvas(
                        mesh = adversarialMesh,
                        renderMode = activeModeState.value,
                        yaw = Float.NaN,
                        pitch = Float.NaN,
                        zoom = Float.NaN,
                        panX = Float.NaN,
                        panY = Float.POSITIVE_INFINITY,
                        lightAngleRad = Float.NaN,
                        isWireframeOverlay = true,
                        showGridContours = true,
                        contourInterval = 0f,
                        isHumanView = true,
                        is1mResolution = true,
                        walkDistanceM = Float.NaN,
                        selectedRoadDistanceM = Float.NaN,
                        showGroundPiece = true,
                        lakeLevelAslM = Float.NaN,
                        overlayBuildings3D = buildings,
                        overlayRoadSegments3D = roadSegs,
                        overlayRoadSymbols3D = symbols,
                        onRotateDrag = { _, _ -> },
                        onPanDrag = { _, _ -> },
                        onZoomChange = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        for (mode in TerrainRenderMode.values()) {
            composeRule.runOnUiThread {
                activeModeState.value = mode
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun `main activity boots on initial 3d terrain view without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
