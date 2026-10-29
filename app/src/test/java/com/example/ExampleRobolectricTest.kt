package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.Math3D
import com.example.engine.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Apex 3D Assets", appName)
    }

    @Test
    fun `math3d vector operations and normalization`() {
        val v = Vec3(3f, 4f, 0f)
        assertEquals(5f, v.length(), 0.001f)
        val norm = v.normalized()
        assertEquals(1f, norm.length(), 0.001f)
        assertEquals(0.6f, norm.x, 0.001f)
        assertEquals(0.8f, norm.y, 0.001f)
    }

    @Test
    fun `math3d rotation preserving length`() {
        val v = Vec3(1f, 2f, 3f)
        val rotated = Math3D.rotateY(v, 1.57f)
        assertEquals(v.length(), rotated.length(), 0.01f)
    }

    @Test
    fun `grass botanical species height strictly does not exceed 11cm max`() {
        for (species in com.example.model.GrassSpecies.values()) {
            assertTrue(
                "Species ${species.commonName} max height must be <= 0.11m (11cm)",
                species.maxHeightMeters <= 0.1101f
            )
            assertTrue(
                "Species ${species.commonName} min height must be >= 0.06m",
                species.minHeightMeters >= 0.060f
            )
            assertTrue(
                "Species ${species.commonName} blade width must be in botanical range (1mm to 6mm)",
                species.minWidthMeters >= 0.001f && species.maxWidthMeters <= 0.006f
            )
        }
    }

    @Test
    fun `grass view mode supports single and variety presentation`() {
        val modes = com.example.model.GrassViewMode.values().map { it.name }
        assertTrue(modes.contains("SINGLE"))
        assertTrue(modes.contains("VARIETY"))
    }

    @Test
    fun `grass blends include weathered brown thatch and senescent gold`() {
        val blends = com.example.model.GrassBlendType.values()
        assertEquals(6, blends.size)
        assertTrue(blends.any { it.isWeatheredBrown && it.blendName.contains("Thatch") })
        assertTrue(blends.any { it.isWeatheredBrown && it.blendName.contains("Senescent") })
    }
}
