package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EdgeEqTest {
    @Test fun barsUseTheBottomEqualizerCurve() {
        assertEquals(0f, EdgeEqMath.shape(0f), 1e-5f)
        assertEquals(1f, EdgeEqMath.shape(1f), 1e-5f)
        assertEquals(1f, EdgeEqMath.shape(3f), 1e-5f)
        // a medium level stays clearly below the top — room to jump on a peak
        assertEquals(0.32f, EdgeEqMath.shape(0.5f), 1e-5f)
        assertEquals(0.0582f, EdgeEqMath.shape(0.15f), 1e-4f)
    }

    @Test fun solidColourStepsMatchTheSweep() {
        val c = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        assertEquals(0xFF000000.toInt(), EdgeEqMath.colorAt(c, 0f))
        assertEquals(0xFFFFFFFF.toInt(), EdgeEqMath.colorAt(c, 0.5f))
        assertEquals(0xFF808080.toInt(), EdgeEqMath.colorAt(c, 0.25f))
        assertEquals(EdgeEqMath.colorAt(c, 0.25f), EdgeEqMath.colorAt(c, 1.25f))
        assertEquals(0, EdgeEqMath.bucket(0f, 0f))
        assertEquals(EdgeEqMath.COLOR_BUCKETS / 2, EdgeEqMath.bucket(0.5f, 0f))
        // turned by a quarter: what was at 0,5 now shows the colour of 0,25
        assertEquals(EdgeEqMath.COLOR_BUCKETS / 4, EdgeEqMath.bucket(0.5f, 0.25f))
        assertEquals(EdgeEqMath.COLOR_BUCKETS - 1, EdgeEqMath.bucket(0f, 0.001f))
        assertEquals(0f, EdgeEqMath.angle01(2f, 0f, 0f, 0f), 1e-4f)
        assertEquals(0.25f, EdgeEqMath.angle01(0f, 2f, 0f, 0f), 1e-4f) // screen y goes down → clockwise
        assertEquals(0.5f, EdgeEqMath.angle01(-2f, 0f, 0f, 0f), 1e-4f)
    }

    @Test fun settingsRoundTripAndBrokenFallsBack() {
        val s = EdgeEqSettings(on = true, scroll = -40, bars = EdgeLayer(false, 3, 90, 10, 150))
        assertEquals(s, EdgeEqSettings.decode(s.encode()))
        assertEquals(EdgeEqSettings.DEFAULT, EdgeEqSettings.decode("garbage"))
        assertEquals(EdgeEqSettings.DEFAULT, EdgeEqSettings.decode(null))
        val clamped = EdgeEqSettings.decode("v1|1|500|1,99,500,0,900|x|1,1,1,1,1")
        assertEquals(100, clamped.scroll)
        assertEquals(EdgePalettes.ALL.lastIndex, clamped.bars.palette)
        assertEquals(EdgeEqSettings.DEFAULT_DOTS, clamped.dots)
    }

    @Test fun bassAtTheBottomHighsAtTheTopSymmetric() {
        assertEquals(0, EdgeEqMath.band(0f, 40))
        assertEquals(39, EdgeEqMath.band(0.5f, 40))
        assertEquals(EdgeEqMath.band(0.2f, 40), EdgeEqMath.band(0.8f, 40))
        // the wave listens only to the middle of the spectrum
        val mid = EdgeEqMath.band(0.5f, 40, 0.2f, 0.6f)
        assertTrue(mid in 8..24)
    }

    @Test fun ringStartsBottomCentreAndPointsOutward() {
        val ring = EdgeEqMath.ring(0f, 0f, 200f, 300f, 20f, 100)
        assertEquals(100f, ring[0], 1e-3f)
        assertEquals(300f, ring[1], 1e-3f)
        assertEquals(1f, ring[3], 1e-3f) // normal points down (out)
        for (i in 0 until 100) {
            val x = ring[i * 4]
            val y = ring[i * 4 + 1]
            assertTrue(x in -0.01f..200.01f && y in -0.01f..300.01f)
            val nx = ring[i * 4 + 2]
            val ny = ring[i * 4 + 3]
            assertEquals(1f, kotlin.math.hypot(nx, ny), 1e-3f)
        }
        // halfway round = top centre, normal up
        assertEquals(100f, ring[50 * 4], 2f)
        assertEquals(0f, ring[50 * 4 + 1], 1e-3f)
    }

    @Test fun smoothingAndTurn() {
        assertTrue(EdgeEqMath.smooth(0f, 1f) > 0.5f)
        assertTrue(EdgeEqMath.smooth(1f, 0f) > 0.8f)
        assertEquals(0f, EdgeEqMath.turn(12_345L, 0), 1e-6f)
        assertEquals(0.5f, EdgeEqMath.turn(5_000L, 100), 1e-4f)
        assertTrue(abs(EdgeEqMath.turn(5_000L, -100) - 0.5f) < 1e-4f)
        assertEquals(48 + 200, EdgeEqMath.sampleCount(100))
    }

    @Test fun equalizerFitsInsideTheFrame() {
        assertEquals(1080f * EdgeEqMath.REACH, EdgeEqMath.inset(EdgeEqSettings.DEFAULT, 1080f), 1e-3f)
        assertEquals(0f, EdgeEqMath.inset(EdgeEqSettings.DEFAULT.copy(on = false), 1080f), 1e-6f)
        val allOff = EdgeEqSettings.DEFAULT.copy(bars = EdgeEqSettings.DEFAULT_BARS.copy(enabled = false), dots = EdgeEqSettings.DEFAULT_DOTS.copy(enabled = false), wave = EdgeEqSettings.DEFAULT_WAVE.copy(enabled = false))
        assertEquals(0f, EdgeEqMath.inset(allOff, 1080f), 1e-6f)
    }
}
