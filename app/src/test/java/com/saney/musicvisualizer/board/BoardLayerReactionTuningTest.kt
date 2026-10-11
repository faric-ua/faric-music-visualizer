package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLayerReactionTuningTest {
    private val signal = BoardAudioState(0.5f, 0.8f, 0.7f, 0.4f, 0.3f)

    @Test fun parentValuesAreInheritedAsAbsoluteLayerValues() {
        val parent = BoardGroupReaction(
            rotationSwayDegrees = 3.3f, stereoShiftFraction = 0.022f,
            bassFloatFraction = 0.021f,
        )
        val inherited = BoardLayerReactionTuning.inherited(parent)
        assertEquals(3.3f, inherited.rotationSwayDegrees, 0.0001f)
        assertEquals(0.022f, inherited.stereoShiftFraction, 0.0001f)
        assertEquals(0.021f, inherited.bassFloatFraction, 0.0001f)
        assertEquals(BoardGroupReaction(0f, 0f, 0f), BoardGroupReaction.neutral())
    }

    @Test fun zeroDisablesAllMusicMotionIncludingBuiltInBeatResponse() {
        val preset = BoardLayerReaction(
            baseScale = 0.94f, bassScale = 0.03f,
            beatRotationDeg = 9f, beatScale = 0.15f,
            midScale = 0.05f, beatTranslateYFraction = 0.02f,
        )
        val reaction = BoardLayerReactionTuning().apply(preset)
        val quiet = BoardLayerMotionEvaluator.evaluate(
            reaction, BoardAudioState(0f,0f,0f,0f,0f), 1.5f, 0.5f,
        )
        val loud = BoardLayerMotionEvaluator.evaluate(reaction, signal, 1.5f, 0.5f)
        assertEquals(quiet, loud)
        assertEquals(0.94f, loud.scale, 0.0001f)
    }

    @Test fun childOverrideIsIndependentOfParent() {
        val parent = BoardGroupReaction(3.3f, 0.022f, 0.021f)
        val inherited = BoardLayerReactionTuning.inherited(parent)
        val child = inherited.copy(stereoShiftFraction = 0f)
        assertEquals(0f, child.stereoShiftFraction, 0.0001f)
        assertEquals(0.022f, inherited.stereoShiftFraction, 0.0001f)
        val motion = BoardLayerMotionEvaluator.evaluate(
            child.apply(BoardLayerReaction()), signal, 1f, 0.5f,
        )
        assertEquals(0f, motion.translateXFraction, 0.0001f)
    }

    @Test fun parentRebroadcastReplacesPreviousChildOverride() {
        val oldChild = BoardLayerReactionTuning(1.1f, 0f, 0.033f)
        val parent = BoardGroupReaction(4f, 0.05f, 0.01f)
        val newChild = BoardLayerReactionTuning.inherited(parent)
        assertTrue(oldChild != newChild)
        assertEquals(0.05f, newChild.stereoShiftFraction, 0.0001f)
    }

    @Test fun bassFloatAndSwayRemainIndependentlyReactive() {
        val base = BoardLayerReaction(baseScale = 1f)
        val tuned = BoardLayerReactionTuning(
            rotationSwayDegrees = 4f, bassFloatFraction = 0.05f,
        ).apply(base)
        val old = BoardLayerMotionEvaluator.evaluate(
            BoardLayerReactionTuning().apply(base), signal, 1.8f,
        )
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
