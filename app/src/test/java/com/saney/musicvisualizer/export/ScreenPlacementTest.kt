package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenPlacementTest {
    private val phone = 2340f / 1080f

    @Test
    fun tiktokFillsTheFrameAndCutsALittle() {
        val p = ScreenPlacement.compute(1080, 1920, phone, fitWholeScreen = false)
        assertEquals(1080, p.virtualWidth)
        assertEquals(2340, p.virtualHeight)
        assertEquals(1f, p.scale, 0.001f)
        assertEquals(0f, p.dx, 0.5f)
        assertEquals(-210f, p.dy, 0.5f) // 210 px cut top and bottom
    }

    @Test
    fun youtubeHeroIsSmallerAndCentred() {
        val p = ScreenPlacement.compute(1920, 1080, phone, fitWholeScreen = false)
        assertEquals(1080f / 2340f, p.scale, 0.001f)
        assertEquals(0f, p.dy, 0.5f)
        val shownWidth = p.virtualWidth * p.scale
        assertEquals((1920f - shownWidth) / 2f, p.dx, 0.5f)
        // the centre of the screen stays the centre of the frame
        assertEquals(960f, p.mapX(p.virtualWidth / 2f), 0.5f)
        assertEquals(540f, p.mapY(p.virtualHeight / 2f), 0.5f)
    }

    @Test
    fun squareAndFeedFitBecauseFillingWouldCutTooMuch() {
        for ((w, h) in listOf(1080 to 1080, 1080 to 1350)) {
            val p = ScreenPlacement.compute(w, h, phone, fitWholeScreen = false)
            assertTrue(p.virtualHeight * p.scale <= h + 0.5f)
        }
    }

    @Test
    fun fitWholeScreenNeverCuts() {
        val p = ScreenPlacement.compute(1080, 1920, phone, fitWholeScreen = true)
        assertTrue(p.virtualHeight * p.scale <= 1920.5f)
        assertTrue(p.dx >= 0f && p.dy >= -0.5f)
    }

    @Test
    fun badAspectFallsBackSafely() {
        val p = ScreenPlacement.compute(1080, 1920, Float.NaN, fitWholeScreen = false)
        assertTrue(p.scale > 0f)
    }
}
