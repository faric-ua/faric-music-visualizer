package com.saney.musicvisualizer.export

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.saney.musicvisualizer.ui.FileInfoText

/** V138: «Пісня · MP3 · 48 кГц · 320 кбіт/с» for the song file (only reads it). */
object SongSourceInfo {
    fun read(
        context: Context,
        uri: Uri,
        fileName: String?,
    ): String? {
        var mime: String? = null
        var sampleRate: Int? = null
        var bitRate: Long? = null
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!trackMime.startsWith("audio/")) continue
                mime = trackMime
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                if (format.containsKey(MediaFormat.KEY_BIT_RATE)) bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                break
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { extractor.release() }
        }
        if (bitRate == null || bitRate <= 0L) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                bitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
                if (mime == null) mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            } catch (_: Throwable) {
            } finally {
                runCatching { retriever.release() }
            }
        }
        return FileInfoText.source(FileInfoText.formatOf(mime, fileName), sampleRate, bitRate)
    }

    /** What PulseDeck makes: the MP4 of [width] × [height] at [fps]. */
    fun videoLine(
        width: Int,
        height: Int,
        fps: Int,
    ): String = FileInfoText.video(width, height, fps, ExportStorageBudget.videoBitRate(width, height), AudioClipTranscoder.AUDIO_BITRATE.toLong())
}
