package com.saney.musicvisualizer.analysis

import android.media.audiofx.Visualizer
import android.os.SystemClock

class AudioCaptureAnalyzer(
    audioSessionId: Int,
    private val onSignal: (SceneSignal) -> Unit,
    private val onPcm: ((ShortArray) -> Unit)? = null,
) : AutoCloseable {
    private val beatDetector = AdaptiveBeatDetector()
    private val liveDynamics = LiveSignalDynamics()
    private val visualizer = Visualizer(audioSessionId)

    init {
        visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
        visualizer.scalingMode = Visualizer.SCALING_MODE_NORMALIZED

        val captureRate = Visualizer.getMaxCaptureRate().coerceAtLeast(1)

        visualizer.setDataCaptureListener(
            object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int,
                ) {
                    if (waveform == null || onPcm == null) return

                    // Android Visualizer waveform is 8-bit unsigned PCM around 128.
                    // Convert it to signed 16-bit mono for the projectM spike.
                    val pcm = ShortArray(waveform.size)
                    for (index in waveform.indices) {
                        val unsigned = waveform[index].toInt() and 0xFF
                        pcm[index] = ((unsigned - 128) shl 8).toShort()
                    }
                    onPcm.invoke(pcm)
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int,
                ) {
                    if (fft == null) return

                    val rawBands = SpectrumMath.fromFft(fft, samplingRate)
                    val bands = liveDynamics.update(rawBands)
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
            true,
            true,
        )
        visualizer.enabled = true
    }

    override fun close() {
        runCatching { visualizer.enabled = false }
        runCatching { visualizer.release() }
    }
}
