package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLayerMotionEvaluatorTest {
    @Test
    fun bassCanDriveFrameScaleWithoutDrivingAlpha() {
        val reaction =
            BoardLayerReaction(
                baseScale = 0.9f,
                bassScale = 0.1f,
                baseAlpha = 0.8f,
            )

        val motion =
            BoardLayerMotionEvaluator.evaluate(
                reaction = reaction,
                audio =
                    BoardAudioState(
                        amplitude = 0f,
                        bass = 0.5f,
                        mid = 0f,
                        high = 0f,
                        beat = 0f,
                    ),
                timeSeconds = 2f,
            )

        assertEquals(0.95f, motion.scale, 0.0001f)
        assertEquals(0.8f, motion.alpha, 0.0001f)
    }

    @Test
    fun highsAndBeatCanDriveFxIndependently() {
        val reaction =
            BoardLayerReaction(
                baseScale = 1f,
                highScale = 0.05f,
                beatScale = 0.08f,
                baseAlpha = 0.2f,
                highAlpha = 0.4f,
                beatAlpha = 0.3f,
            )

        val quiet =
            BoardLayerMotionEvaluator.evaluate(
                reaction,
                BoardAudioState(0f, 0f, 0f, 0f, 0f),
                0f,
            )

        val active =
            BoardLayerMotionEvaluator.evaluate(
                reaction,
                BoardAudioState(0f, 0f, 0f, 1f, 1f),
                0f,
            )

        assertTrue(active.scale > quiet.scale)
        assertTrue(active.alpha > quiet.alpha)
    }

    @Test
    fun alphaIsClamped() {
        val motion =
            BoardLayerMotionEvaluator.evaluate(
                reaction =
                    BoardLayerReaction(
                        baseAlpha = 0.9f,
                        highAlpha = 0.8f,
                        beatAlpha = 0.8f,
                    ),
                audio =
                    BoardAudioState(
                        amplitude = 0f,
                        bass = 0f,
                        mid = 0f,
                        high = 1f,
                        beat = 1f,
                    ),
                timeSeconds = 0f,
            )

        assertEquals(1f, motion.alpha, 0.0001f)
    }
}
