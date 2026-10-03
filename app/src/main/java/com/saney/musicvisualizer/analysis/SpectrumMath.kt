package com.saney.musicvisualizer.analysis

import kotlin.math.hypot
import kotlin.math.ln1p
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

data class BandEnergy(
    val amplitude: Float,
    val bass: Float,
    val mid: Float,
    val high: Float,
)

object SpectrumMath {

    private data class Accumulator(
        var sumSquares: Double = 0.0,
        var peak: Double = 0.0,
        var count: Int = 0,
    ) {
        fun add(value: Double) {
            sumSquares += value * value
            if (value > peak) peak = value
            count++
        }

        fun energy(): Float {
            if (count == 0) return 0f
            val rms = sqrt(sumSquares / count)
            // Peak keeps narrow frequency hits visible; RMS keeps the result stable.
            return (peak * 0.62 + rms * 0.38).toFloat().coerceIn(0f, 1f)
        }
    }

    fun frequencyBins(
        fft: ByteArray,
        samplingRateMilliHz: Int,
        bandCount: Int = 32,
        minHz: Double = 60.0,
        maxHz: Double = 12_000.0,
    ): FloatArray {
        if (fft.size < 8 || samplingRateMilliHz <= 0 || bandCount <= 0) {
            return FloatArray(maxOf(0, bandCount))
        }

        val sampleRateHz = samplingRateMilliHz / 1000.0
        val nyquist = sampleRateHz * 0.5
        val upperHz = minOf(maxHz, nyquist)
        if (upperHz <= minHz) return FloatArray(bandCount)

        val sums = DoubleArray(bandCount)
        val counts = IntArray(bandCount)
        val logMin = ln(minHz)
        val logRange = ln(upperHz) - logMin
        val bins = fft.size / 2

        for (bin in 1 until bins) {
            val index = bin * 2
            if (index + 1 >= fft.size) break
            val frequencyHz = bin * sampleRateHz / fft.size
            if (frequencyHz < minHz || frequencyHz > upperHz) continue

            val magnitude = hypot(fft[index].toDouble(), fft[index + 1].toDouble())
            val normalized = (ln1p(magnitude) / ln1p(181.0)).coerceIn(0.0, 1.0)
            val reactive = normalized.pow(0.72)
            val position = ((ln(frequencyHz) - logMin) / logRange).coerceIn(0.0, 0.999999)
            val band = (position * bandCount).toInt().coerceIn(0, bandCount - 1)
            sums[band] += reactive
            counts[band]++
        }

        return FloatArray(bandCount) { band ->
            if (counts[band] == 0) {
                0f
            } else {
                (sums[band] / counts[band]).toFloat().coerceIn(0f, 1f)
            }
        }
    }

    fun fromFft(fft: ByteArray, samplingRateMilliHz: Int): BandEnergy {
        if (fft.size < 8 || samplingRateMilliHz <= 0) {
            return BandEnergy(0f, 0f, 0f, 0f)
        }

        val sampleRateHz = samplingRateMilliHz / 1000.0
        val bass = Accumulator()
        val mid = Accumulator()
        val high = Accumulator()
        val all = Accumulator()

        val bins = fft.size / 2
        for (bin in 1 until bins) {
            val index = bin * 2
            if (index + 1 >= fft.size) break

            val magnitude = hypot(fft[index].toDouble(), fft[index + 1].toDouble())
            val normalized = (ln1p(magnitude) / ln1p(181.0)).coerceIn(0.0, 1.0)
            // Slight expansion makes quiet bins visible without flattening loud peaks.
            val reactive = normalized.pow(0.72)
            val frequencyHz = bin * sampleRateHz / fft.size

            when {
                frequencyHz in 30.0..250.0 -> bass.add(reactive)
                frequencyHz in 250.0..2_500.0 -> mid.add(reactive)
                frequencyHz in 2_500.0..12_000.0 -> high.add(reactive)
            }
            if (frequencyHz <= 12_000.0) all.add(reactive)
        }

        val bassEnergy = bass.energy()
        val midEnergy = mid.energy()
        val highEnergy = high.energy()
        val spectralPeak = maxOf(bassEnergy, midEnergy, highEnergy)
        val amplitude = maxOf(all.energy(), spectralPeak * 0.92f).coerceIn(0f, 1f)

        return BandEnergy(
            amplitude = amplitude,
            bass = bassEnergy,
            mid = midEnergy,
            high = highEnergy,
        )
    }
}
