package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardLayerTransformTest {
    @Test
    fun frameAutoRotationDefaultsOffAndClampsSignedSpeed() {
        val default = BoardLayerTransform.default()
        assertEquals(0f, default.autoRotationOffsetAt(10f), 0.0001f)

        val clockwise = BoardLayerTransform(
            rotationDegrees = 60f,
            autoRotationDegreesPerSecond = 30f,
        ).sanitized()
        assertEquals(60f, clockwise.rotationDegrees, 0.0001f)
        assertEquals(90f, clockwise.autoRotationOffsetAt(3f), 0.0001f)
        assertEquals(0f, clockwise.autoRotationOffsetAt(12f), 0.0001f)

        val counterclockwise = clockwise.copy(
            autoRotationDegreesPerSecond = -30f,
        ).sanitized()
        assertEquals(-90f, counterclockwise.autoRotationOffsetAt(3f), 0.0001f)

        assertEquals(
            180f,
            clockwise.copy(autoRotationDegreesPerSecond = 800f)
                .sanitized().autoRotationDegreesPerSecond,
            0.0001f,
        )
        assertEquals(
            -180f,
            clockwise.copy(autoRotationDegreesPerSecond = -800f)
                .sanitized().autoRotationDegreesPerSecond,
            0.0001f,
        )
    }

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
