package com.saney.musicvisualizer.analysis

import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumMathTest {
    @Test
    fun lowFrequencyBinRaisesBassMoreThanMid() {
        val fft = ByteArray(1024)
        fft[8] = 120
        fft[9] = 60
        val energy = SpectrumMath.fromFft(fft, 48_000_000)
        assertTrue(energy.bass > energy.mid)
        assertTrue(energy.bass > energy.high)
    }

    @Test
    fun narrowFrequencySpikeRemainsStrongInsteadOfBeingAveragedAway() {
        val fft = ByteArray(1024)
        fft[10] = 120
        fft[11] = 100
        val energy = SpectrumMath.fromFft(fft, 48_000_000)
        assertTrue(energy.bass > 0.35f)
    }

    @Test
    fun emptyFftReturnsZeroEnergy() {
        val energy = SpectrumMath.fromFft(ByteArray(0), 48_000_000)
        assertTrue(energy.amplitude == 0f)
        assertTrue(energy.bass == 0f)
        assertTrue(energy.mid == 0f)
        assertTrue(energy.high == 0f)
    }
}
