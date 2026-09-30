package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardTransformTest {
    @Test
    fun sanitizesOutOfRangeValues() {
        val safe =
            BoardTransform(
                xFraction = -5f,
                yFraction = 5f,
                sizeFraction = 4f,
                rotationDegrees = 180f,
                opacity = 0f,
            ).sanitized()

        assertEquals(0.05f, safe.xFraction, 0.0001f)
        assertEquals(0.92f, safe.yFraction, 0.0001f)
        assertEquals(1.10f, safe.sizeFraction, 0.0001f)
        assertEquals(45f, safe.rotationDegrees, 0.0001f)
        assertEquals(0.20f, safe.opacity, 0.0001f)
    }

    @Test
    fun safeAreaPresetIsSmallerThanDefault() {
        val normal = BoardTransform.default()
        val fitted = BoardTransform.fitSafeArea()

        assertEquals(true, fitted.sizeFraction < normal.sizeFraction)
        assertEquals(true, fitted.yFraction < normal.yFraction)
    }
}
