package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardLayerTransformTest {
    @Test
    fun clampsPerLayerOverrides() {
        val safe =
            BoardLayerTransform(
                offsetXFraction = 2f,
                offsetYFraction = -2f,
                scale = 4f,
                rotationDegrees = 200f,
                opacity = -1f,
            ).sanitized()

        assertEquals(
            0.35f,
            safe.offsetXFraction,
            0.0001f,
        )
        assertEquals(
            -0.35f,
            safe.offsetYFraction,
            0.0001f,
        )
        assertEquals(
            1.80f,
            safe.scale,
            0.0001f,
        )
        assertEquals(
            90f,
            safe.rotationDegrees,
            0.0001f,
        )
        assertEquals(
            0f,
            safe.opacity,
            0.0001f,
        )
    }
}
