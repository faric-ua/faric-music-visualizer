package com.saney.musicvisualizer.analysis

import kotlin.math.max

class AdaptiveBeatDetector(
    private val cooldownMs: Long = 110L,
    private val thresholdMultiplier: Float = 1.14f,
    private val transientRise: Float = 0.085f,
) {
    private var baseline = 0f
    private var previousEnergy = 0f
    private var initialized = false
    private var lastBeatMs = Long.MIN_VALUE / 2

    fun update(
        bands: BandEnergy,
        nowMs: Long,
    ): Float {
        val broadband =
            (
                bands.amplitude * 0.50f +
                    bands.mid * 0.30f +
                    bands.high * 0.20f
                )
                .coerceIn(0f, 1f)

        val impact =
            max(
                bands.bass,
                broadband,
            )

        return updateEnergy(
            energy = impact,
            nowMs = nowMs,
        )
    }

    fun update(
        bassEnergy: Float,
        nowMs: Long,
    ): Float =
        updateEnergy(
            energy =
                bassEnergy
                    .coerceIn(0f, 1f),
            nowMs = nowMs,
        )

    private fun updateEnergy(
        energy: Float,
        nowMs: Long,
    ): Float {
        if (energy <= SILENCE_FLOOR) {
            baseline = 0f
            previousEnergy = 0f
            initialized = false
            return 0f
        }

        if (!initialized) {
            baseline = energy
            previousEnergy = energy
            initialized = true
            return 0f
        }

        val threshold =
            max(
                MIN_THRESHOLD,
                baseline *
                    thresholdMultiplier,
            )

        val rise =
            (
                energy -
                    previousEnergy
                )
                .coerceAtLeast(0f)

        val canFire =
            nowMs -
                lastBeatMs >=
                cooldownMs

        val thresholdHit =
            energy >
                threshold

        val transientHit =
            energy >=
                MIN_TRANSIENT_ENERGY &&
                rise >=
                transientRise

        val strength =
            if (
                canFire &&
                (
                    thresholdHit ||
                        transientHit
                    )
            ) {
                lastBeatMs = nowMs

                val thresholdStrength =
                    (
                        (energy - threshold) /
                            max(
                                0.06f,
                                1f - threshold,
                            )
                        )
                        .coerceIn(0f, 1f)

                val transientStrength =
                    (
                        rise /
                            0.30f
                        )
                        .coerceIn(0f, 1f)

                max(
                    thresholdStrength,
                    transientStrength,
                )
                    .coerceIn(
                        0.30f,
                        1f,
                    )
            } else {
                0f
            }

        val alpha =
            if (energy > baseline) {
                0.035f
            } else {
                0.09f
            }

        baseline +=
            (energy - baseline) *
                alpha

        previousEnergy = energy

        return strength
    }

    private companion object {
        const val SILENCE_FLOOR = 0.015f
        const val MIN_THRESHOLD = 0.07f
        const val MIN_TRANSIENT_ENERGY = 0.20f
    }
}
