package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardGroupReactionTest {
    @Test
    fun clampsReactionStrengths() {
        val value =
            BoardGroupReaction(
                rotationSwayDegrees = 99f,
                stereoShiftFraction = -1f,
                bassFloatFraction = 2f,
            ).sanitized()

        assertEquals(6f, value.rotationSwayDegrees, 0.0001f)
        assertEquals(0f, value.stereoShiftFraction, 0.0001f)
        assertEquals(0.06f, value.bassFloatFraction, 0.0001f)
    }
}
