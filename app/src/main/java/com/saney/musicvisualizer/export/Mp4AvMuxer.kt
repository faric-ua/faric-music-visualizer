package com.saney.musicvisualizer.export

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException

/**
 * Stream the already encoded video and AAC into the final MP4.
 *
 * For Android Q+, writes straight to a PENDING MediaStore movie through its
 * seekable file descriptor. Unlike the old muxedTemp -> MediaStore copy path,
 * this does not allocate a second full-length MP4 in the app cache at the
 * *end* of a potentially hours-long export.
 */
object Mp4AvMuxer {
    const val DEFAULT_RELATIVE_PATH = ExportFolders.VIDEO

    fun mux(
        videoFile: File,
        audioFile: File,
        outputFile: File,
        shouldCancel: () -> Boolean = { false },
        tags: Mp4Tagger.Tags? = null,
        onProgress: (Float) -> Unit = {},
    ) {
        val muxer = MediaMuxer(
            outputFile.absolutePath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        )
        muxTracks(videoFile, audioFile, muxer, shouldCancel, onProgress)
        if (tags != null && !tags.isEmpty) {
            runCatching {
                java.io.RandomAccessFile(outputFile, "rw").use { Mp4Tagger.apply(it.channel, it.channel, tags) }
            }.onFailure { android.util.Log.w("FARIC-export", "MP4 tags not written", it) }
        }
    }

    /** Caller must run this off the main thread. Android 10+ only. */
    fun muxToMediaStore(
        context: Context,
        videoFile: File,
        audioFile: File,
        displayName: String,
        shouldCancel: () -> Boolean = { false },
        relativePath: String = DEFAULT_RELATIVE_PATH,
        tags: Mp4Tagger.Tags? = null,
        onProgress: (Float) -> Unit = {},
    ): Uri {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "MediaStore pending video requires Android Q+"
        }
        check(!shouldCancel()) { "Export cancelled before publishing" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Cannot create pending MP4 in $relativePath")
        try {
            val fd = resolver.openFileDescriptor(uri, "rw")
                ?: error("Cannot open pending MP4 for muxing")
            fd.use {
                val muxer = MediaMuxer(
                    it.fileDescriptor,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                )
                muxTracks(videoFile, audioFile, muxer, shouldCancel, onProgress)
                // V93: song tags (title, artist, album, cover) into the finished MP4. Streams over the
                // MediaStore descriptor are not closed here — the ParcelFileDescriptor owns it.
                if (tags != null && !tags.isEmpty) {
                    runCatching {
                        Mp4Tagger.apply(
                            java.io.FileInputStream(it.fileDescriptor).channel,
                            java.io.FileOutputStream(it.fileDescriptor).channel,
                            tags,
                        )
                    }.onFailure { error -> android.util.Log.w("FARIC-export", "MP4 tags not written", error) }
                }
            }
            if (shouldCancel()) throw CancellationException("Export cancelled")
            val published = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            check(resolver.update(uri, published, null, null) == 1) {
                "MP4 was muxed, but MediaStore could not publish it"
            }
            return uri
        } catch (error: Throwable) {
            // Never leave an invisible IS_PENDING=1 movie after cancel/failure.
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    private fun muxTracks(
        videoFile: File,
        audioFile: File,
        muxer: MediaMuxer,
        shouldCancel: () -> Boolean,
        onProgress: (Float) -> Unit,
    ) {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxerStarted = false
        try {
            check(videoFile.isFile && videoFile.length() > 0L) {
                "Rendered video is missing or empty"
            }
            check(audioFile.isFile && audioFile.length() > 0L) {
                "Transcoded audio is missing or empty"
            }
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)
            val videoTrack = findTrack(videoExtractor, "video/")
            val audioTrack = findTrack(audioExtractor, "audio/")
            check(videoTrack >= 0) { "Encoded video track missing" }
            check(audioTrack >= 0) { "Encoded audio track missing" }
            videoExtractor.selectTrack(videoTrack)
            audioExtractor.selectTrack(audioTrack)
            val videoMuxTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoTrack))
            val audioMuxTrack = muxer.addTrack(audioExtractor.getTrackFormat(audioTrack))
            if (shouldCancel()) throw CancellationException("Export cancelled")
            muxer.start()
            muxerStarted = true
            // V172: progress by bytes copied (the picture is almost all of them).
            val total = (videoFile.length() + audioFile.length()).coerceAtLeast(1L).toFloat()
            val videoBytes = copyTrack(videoExtractor, muxer, videoMuxTrack, shouldCancel) { bytes -> onProgress(bytes / total) }
            copyTrack(audioExtractor, muxer, audioMuxTrack, shouldCancel) { bytes -> onProgress((videoBytes + bytes) / total) }
            if (shouldCancel()) throw CancellationException("Export cancelled")
            muxer.stop()
            muxerStarted = false
        } finally {
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int =
        (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith(prefix) == true
        } ?: -1

    private fun copyTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        targetTrack: Int,
        shouldCancel: () -> Boolean,
        onBytes: (Long) -> Unit = {},
    ): Long {
        val buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024)
        val info = MediaCodec.BufferInfo()
        var copied = 0L
        var bytes = 0L
        while (true) {
            if (shouldCancel()) throw CancellationException("Export cancelled")
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            check(size <= buffer.capacity()) { "MP4 sample exceeds mux buffer capacity" }
            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
            info.flags = extractor.sampleFlags
            buffer.position(0)
            buffer.limit(size)
            muxer.writeSampleData(targetTrack, buffer, info)
            copied++
            bytes += size
            if (copied % 30L == 0L) onBytes(bytes)
            if (!extractor.advance()) break
        }
        check(copied > 0L) { "MP4 mux input track had no samples" }
        onBytes(bytes)
        return bytes
    }
}
