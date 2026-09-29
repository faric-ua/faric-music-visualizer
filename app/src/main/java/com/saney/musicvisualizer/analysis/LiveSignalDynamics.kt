package com.saney.musicvisualizer.analysis

import kotlin.math.max

/**
 * Restores local musical contrast after Android Visualizer's absolute FFT level
 * has settled. Layer 1 should keep moving with small bass/mid/high changes
 * instead of reacting mainly to the initial jump from silence to playback.
 */
class LiveSignalDynamics {
    private val amplitude =
        AdaptiveBandContrast(
            relativeWindow = 0.050f,
            gain = 0.50f,
        )

    private val bass =
        AdaptiveBandContrast(
            relativeWindow = 0.035f,
            gain = 0.60f,
        )

    private val mid =
        AdaptiveBandContrast(
            relativeWindow = 0.055f,
            gain = 0.52f,
        )

    private val high =
        AdaptiveBandContrast(
            relativeWindow = 0.060f,
            gain = 0.50f,
        )

    private var quietFrames = 0
    private var needsRebase = true

    fun update(raw: BandEnergy): BandEnergy {
        if (raw.amplitude <= SILENCE_THRESHOLD) {
            quietFrames++

            if (quietFrames >= QUIET_FRAMES_TO_REBASE) {
                needsRebase = true
            }

            return ZERO
        }

        if (needsRebase) {
            needsRebase = false
            quietFrames = 0

            amplitude.reset(raw.amplitude)
            bass.reset(raw.bass)
            mid.reset(raw.mid)
            high.reset(raw.high)

            return BandEnergy(
                amplitude = amplitude.neutral(raw.amplitude),
                bass = bass.neutral(raw.bass),
                mid = mid.neutral(raw.mid),
                high = high.neutral(raw.high),
            )
        }

        quietFrames = 0

        return BandEnergy(
            amplitude = amplitude.update(raw.amplitude),
            bass = bass.update(raw.bass),
            mid = mid.update(raw.mid),
            high = high.update(raw.high),
        )
    }

    private companion object {
        const val SILENCE_THRESHOLD = 0.025f
        const val QUIET_FRAMES_TO_REBASE = 4

        val ZERO =
            BandEnergy(
                amplitude = 0f,
                bass = 0f,
                mid = 0f,
                high = 0f,
            )
    }
}

internal class AdaptiveBandContrast(
    private val relativeWindow: Float,
    private val gain: Float,
    private val minimumWindow: Float = 0.012f,
    private val neutralLevel: Float = 0.28f,
    private val rawBlend: Float = 0.16f,
    private val riseAlpha: Float = 0.012f,
    private val fallAlpha: Float = 0.035f,
) {
    private var baseline = 0f
    private var initialized = false

    fun reset(value: Float) {
        baseline = value.coerceIn(0f, 1f)
        initialized = true
    }

    fun neutral(value: Float): Float {
        val v = value.coerceIn(0f, 1f)

        return (
            neutralLevel * (1f - rawBlend) +
                v * rawBlend
            ).coerceIn(0f, 1f)
    }

    fun update(value: Float): Float {
        val v = value.coerceIn(0f, 1f)

        if (!initialized) {
            reset(v)
            return neutral(v)
        }

        val window =
            max(
                minimumWindow,
                baseline * relativeWindow,
            )

        val signedDeviation =
            (v - baseline) /
                window

        val contrasted =
            (
                neutralLevel +
                    signedDeviation * gain
                ).coerceIn(0f, 1f)

        val shaped =
            (
                contrasted * (1f - rawBlend) +
                    v * rawBlend
                ).coerceIn(0f, 1f)

        val alpha =
            if (v > baseline) {
                riseAlpha
            } else {
                fallAlpha
            }

        baseline +=
            (v - baseline) *
                alpha

        return shaped
    }
}
