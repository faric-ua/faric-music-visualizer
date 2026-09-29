package com.saney.musicvisualizer.analysis

import kotlin.math.max

class AdaptiveBeatDetector(
    private val cooldownMs: Long = 125L,
    private val thresholdMultiplier: Float = 1.25f,
) {
    private var baseline = 0f
    private var initialized = false
    private var lastBeatMs = Long.MIN_VALUE / 2

    fun update(bassEnergy: Float, nowMs: Long): Float {
        val energy = bassEnergy.coerceIn(0f, 1f)

        if (energy <= SILENCE_FLOOR) {
            baseline = 0f
            initialized = false
            return 0f
        }

        if (!initialized) {
            baseline = energy
            initialized = true
            return 0f
        }

        val threshold = max(0.08f, baseline * thresholdMultiplier)
        val canFire = nowMs - lastBeatMs >= cooldownMs

        val strength = if (canFire && energy > threshold) {
            lastBeatMs = nowMs
            ((energy - threshold) / max(0.06f, 1f - threshold))
                .coerceIn(0.32f, 1f)
        } else {
            0f
        }

        // Fast enough to follow track dynamics, slow enough not to chase every spike.
        val alpha = if (energy > baseline) 0.055f else 0.11f
        baseline += (energy - baseline) * alpha

        return strength
    }

    private companion object {
        const val SILENCE_FLOOR = 0.015f
    }
}
