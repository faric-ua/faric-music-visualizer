package com.saney.musicvisualizer.analysis

import kotlin.math.max

class AdaptiveBeatDetector(
    private val cooldownMs: Long = 180L,
    private val thresholdMultiplier: Float = 1.45f,
) {
    private var baseline = 0f
    private var initialized = false
    private var lastBeatMs = Long.MIN_VALUE / 2

    fun update(bassEnergy: Float, nowMs: Long): Float {
        val energy = bassEnergy.coerceIn(0f, 1f)
        if (!initialized) {
            baseline = energy
            initialized = true
            return 0f
        }
        val threshold = max(0.12f, baseline * thresholdMultiplier)
        val canFire = nowMs - lastBeatMs >= cooldownMs
        val strength = if (canFire && energy > threshold) {
            lastBeatMs = nowMs
            ((energy - threshold) / max(0.08f, 1f - threshold)).coerceIn(0.25f, 1f)
        } else 0f
        val alpha = if (energy > baseline) 0.035f else 0.08f
        baseline += (energy - baseline) * alpha
        return strength
    }
}
