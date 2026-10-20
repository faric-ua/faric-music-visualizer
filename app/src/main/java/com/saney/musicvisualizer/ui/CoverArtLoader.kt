package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

/** V91: the picture embedded in the song file (cover), scaled down to at most [maxPx]. */
object CoverArtLoader {
    fun load(
        context: Context,
        uri: Uri,
        maxPx: Int = 1024,
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        val bytes =
            try {
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { retriever.release() }
            } ?: return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }
}
