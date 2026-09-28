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
        val captureRate = (Visualizer.getMaxCaptureRate() / 2).coerceAtLeast(1)
        visualizer.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null) return
                    val bands = SpectrumMath.fromFft(fft, samplingRate)
                    onSignal(
                        SceneSignal(
                            bands.amplitude,
                            bands.bass,
                            bands.mid,
                            bands.high,
                            beatDetector.update(bands.bass, SystemClock.elapsedRealtime()),
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
