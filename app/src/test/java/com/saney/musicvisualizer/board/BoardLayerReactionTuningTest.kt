package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLayerReactionTuningTest {
    private val signal = BoardAudioState(0.5f, 0.8f, 0.7f, 0.4f, 0.3f)

    @Test fun defaultsPreserveOldMotion() {
        val base = BoardLayerReaction(baseScale = 0.94f, bassScale = 0.03f)
        val untouched = BoardLayerMotionEvaluator.evaluate(base, signal, 1.5f, 0.5f)
        val tuned = BoardLayerMotionEvaluator.evaluate(
            BoardLayerReactionTuning().apply(base), signal, 1.5f, 0.5f,
        )
        assertEquals(untouched, tuned)
    }

    @Test fun stereoMovesOnlySelectedObject() {
        val base = BoardLayerReaction()
        val tuned = BoardLayerReactionTuning(stereoShiftFraction = 0.08f).apply(base)
        val still = BoardLayerMotionEvaluator.evaluate(base, signal, 1f, 0.5f)
        val moved = BoardLayerMotionEvaluator.evaluate(tuned, signal, 1f, 0.5f)
        assertEquals(0f, still.translateXFraction, 0.0001f)
        assertEquals(0.04f, moved.translateXFraction, 0.0001f)
    }

    @Test fun bassFloatAndSwayAreIndependentOfSourceId() {
        val base = BoardLayerReaction(baseScale = 1f)
        val tuned = BoardLayerReactionTuning(
            rotationSwayDegrees = 4f, bassFloatFraction = 0.05f,
        ).apply(base)
        val old = BoardLayerMotionEvaluator.evaluate(base, signal, 1.8f)
        val changed = BoardLayerMotionEvaluator.evaluate(tuned, signal, 1.8f)
        assertTrue(changed.rotationDegrees != old.rotationDegrees)
        assertTrue(changed.translateYFraction != old.translateYFraction)
        assertEquals(old.scale, changed.scale, 0.0001f)
    }

    @Test fun invalidInputsAreClamped() {
        val safe = BoardLayerReactionTuning(
            rotationSwayDegrees = Float.NaN, stereoShiftFraction = 5f,
            bassFloatFraction = -1f,
        ).sanitized()
        assertEquals(0f, safe.rotationSwayDegrees, 0.0001f)
        assertEquals(0.1f, safe.stereoShiftFraction, 0.0001f)
        assertEquals(0f, safe.bassFloatFraction, 0.0001f)
    }
}
