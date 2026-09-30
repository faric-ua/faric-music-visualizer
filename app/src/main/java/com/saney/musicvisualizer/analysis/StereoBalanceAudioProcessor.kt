package com.saney.musicvisualizer.analysis

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * Pass-through PCM processor that measures left/right energy before audio reaches the sink.
 *
 * stereoPan:
 * -1 = left-heavy
 *  0 = centered / mono
 * +1 = right-heavy
 *
 * Audio bytes are copied unchanged to the output buffer.
 */
@UnstableApi
class StereoBalanceAudioProcessor(
    private val onPan: (Float) -> Unit,
) : BaseAudioProcessor() {
    private var channelCount = 0
    private var smoothedPan = 0f

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        channelCount = inputAudioFormat.channelCount

        return if (
            inputAudioFormat.encoding ==
            C.ENCODING_PCM_16BIT &&
            channelCount >= 2
        ) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }

        measure(inputBuffer)

        val output =
            replaceOutputBuffer(
                inputBuffer.remaining(),
            )

        output.put(inputBuffer)
        output.flip()
    }

    override fun onFlush() {
        smoothedPan = 0f
        onPan(0f)
    }

    override fun onReset() {
        channelCount = 0
        smoothedPan = 0f
        onPan(0f)
    }

    private fun measure(inputBuffer: ByteBuffer) {
        if (channelCount < 2) {
            smoothedPan *= 0.90f
            onPan(smoothedPan)
            return
        }

        val samples =
            inputBuffer
                .duplicate()
                .order(inputBuffer.order())
                .asShortBuffer()

        var leftSquares = 0.0
        var rightSquares = 0.0
        var frames = 0

        while (
            samples.remaining() >=
            channelCount
        ) {
            val left =
                samples.get().toDouble() /
                    Short.MAX_VALUE

            val right =
                samples.get().toDouble() /
                    Short.MAX_VALUE

            leftSquares += left * left
            rightSquares += right * right
            frames++

            repeat(channelCount - 2) {
                samples.get()
            }
        }

        if (frames <= 0) {
            return
        }

        val leftRms =
            sqrt(leftSquares / frames)
        val rightRms =
            sqrt(rightSquares / frames)

        val total =
            leftRms + rightRms

        val rawPan =
            if (total < 0.0005) {
                0f
            } else {
                (
                    (rightRms - leftRms) /
                        total
                    )
                    .toFloat()
                    .coerceIn(-1f, 1f)
            }

        smoothedPan +=
            (rawPan - smoothedPan) *
                0.22f

        onPan(
            smoothedPan.coerceIn(
                -1f,
                1f,
            ),
        )
    }
}
