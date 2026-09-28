package com.saney.musicvisualizer.analysis

import kotlin.math.hypot
import kotlin.math.ln1p

data class BandEnergy(
    val amplitude: Float,
    val bass: Float,
    val mid: Float,
    val high: Float,
)

object SpectrumMath {
    fun fromFft(fft: ByteArray, samplingRateMilliHz: Int): BandEnergy {
        if (fft.size < 8 || samplingRateMilliHz <= 0) return BandEnergy(0f, 0f, 0f, 0f)
        val sampleRateHz = samplingRateMilliHz / 1000.0
        var bassSum = 0.0; var bassCount = 0
        var midSum = 0.0; var midCount = 0
        var highSum = 0.0; var highCount = 0
        var allSum = 0.0; var allCount = 0
        val bins = fft.size / 2
        for (bin in 1 until bins) {
            val index = bin * 2
            if (index + 1 >= fft.size) break
            val magnitude = hypot(fft[index].toDouble(), fft[index + 1].toDouble())
            val normalized = (ln1p(magnitude) / ln1p(181.0)).coerceIn(0.0, 1.0)
            val frequencyHz = bin * sampleRateHz / fft.size
            when {
                frequencyHz in 40.0..250.0 -> { bassSum += normalized; bassCount++ }
                frequencyHz <= 2_000.0 -> { midSum += normalized; midCount++ }
                frequencyHz <= 8_000.0 -> { highSum += normalized; highCount++ }
            }
            if (frequencyHz <= 8_000.0) { allSum += normalized; allCount++ }
        }
        fun avg(sum: Double, count: Int) =
            if (count == 0) 0f else (sum / count).toFloat().coerceIn(0f, 1f)
        return BandEnergy(avg(allSum, allCount), avg(bassSum, bassCount), avg(midSum, midCount), avg(highSum, highCount))
    }
}
