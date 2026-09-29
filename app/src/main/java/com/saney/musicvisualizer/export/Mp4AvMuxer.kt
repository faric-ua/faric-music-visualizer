package com.saney.musicvisualizer.export

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

object Mp4AvMuxer {
    fun mux(
        videoFile: File,
        audioFile: File,
        outputFile: File,
    ) {
        val videoExtractor =
            MediaExtractor()

        val audioExtractor =
            MediaExtractor()

        videoExtractor.setDataSource(
            videoFile.absolutePath,
        )

        audioExtractor.setDataSource(
            audioFile.absolutePath,
        )

        val videoTrack =
            findTrack(
                videoExtractor,
                "video/",
            )

        val audioTrack =
            findTrack(
                audioExtractor,
                "audio/",
            )

        check(videoTrack >= 0) {
            "Encoded video track missing"
        }

        check(audioTrack >= 0) {
            "Encoded audio track missing"
        }

        videoExtractor.selectTrack(
            videoTrack,
        )

        audioExtractor.selectTrack(
            audioTrack,
        )

        val muxer =
            MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            )

        try {
            val videoMuxTrack =
                muxer.addTrack(
                    videoExtractor.getTrackFormat(
                        videoTrack,
                    ),
                )

            val audioMuxTrack =
                muxer.addTrack(
                    audioExtractor.getTrackFormat(
                        audioTrack,
                    ),
                )

            muxer.start()

            copyTrack(
                extractor =
                    videoExtractor,
                muxer =
                    muxer,
                targetTrack =
                    videoMuxTrack,
            )

            copyTrack(
                extractor =
                    audioExtractor,
                muxer =
                    muxer,
                targetTrack =
                    audioMuxTrack,
            )

            muxer.stop()
        } finally {
            videoExtractor.release()
            audioExtractor.release()
            muxer.release()
        }
    }

    private fun findTrack(
        extractor: MediaExtractor,
        prefix: String,
    ): Int =
        (0 until extractor.trackCount)
            .firstOrNull { index ->
                extractor
                    .getTrackFormat(index)
                    .getString(
                        MediaFormat.KEY_MIME,
                    )
                    ?.startsWith(prefix) ==
                    true
            }
            ?: -1

    private fun copyTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        targetTrack: Int,
    ) {
        // Encoded proof samples are small, but use a generous reusable
        // buffer so this function does not depend on track ordering or optional
        // KEY_MAX_INPUT_SIZE metadata.
        val buffer =
            ByteBuffer.allocate(
                4 * 1024 * 1024,
            )

        val info =
            MediaCodec.BufferInfo()

        while (true) {
            buffer.clear()

            val size =
                extractor.readSampleData(
                    buffer,
                    0,
                )

            if (size < 0) {
                break
            }

            info.offset = 0
            info.size = size
            info.presentationTimeUs =
                extractor.sampleTime
                    .coerceAtLeast(0L)
            info.flags =
                extractor.sampleFlags

            buffer.position(0)
            buffer.limit(size)

            muxer.writeSampleData(
                targetTrack,
                buffer,
                info,
            )

            if (!extractor.advance()) {
                break
            }
        }
    }
}
