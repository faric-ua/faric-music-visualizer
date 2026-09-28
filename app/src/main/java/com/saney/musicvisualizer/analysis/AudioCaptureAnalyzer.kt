package com.saney.musicvisualizer.analysis

import android.media.audiofx.Visualizer
import android.os.SystemClock

class AudioCaptureAnalyzer(
    audioSessionId: Int,
    private val onSignal: (SceneSignal) -> Unit,
) : AutoCloseable {
    private val beatDetector = AdaptiveBeatDetector()
    private val visualizer = Visualizer(audioSessionId)

    init {
        visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
        visualizer.scalingMode = Visualizer.SCALING_MODE_NORMALIZED

        // Use the highest capture rate exposed by Android Visualizer.
        // The previous /2 rate made frequency changes visibly late.
        val captureRate = Visualizer.getMaxCaptureRate().coerceAtLeast(1)

        visualizer.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit

                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null) return

                    val bands = SpectrumMath.fromFft(fft, samplingRate)
                    val beat = beatDetector.update(bands.bass, SystemClock.elapsedRealtime())

                    onSignal(
                        SceneSignal(
                            amplitude = bands.amplitude,
                            bass = bands.bass,
                            mid = bands.mid,
                            high = bands.high,
                            beatStrength = beat,
                        ),
                    )
                }
            },
            captureRate,
            false,
            true,
        )
        visualizer.enabled = true
    }

    override fun close() {
        runCatching { visualizer.enabled = false }
        runCatching { visualizer.release() }
    }
}
