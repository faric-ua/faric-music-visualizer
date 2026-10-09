package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.Log
import android.widget.ImageView
import java.lang.ref.WeakReference
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Embedded album artwork for locally accessible MediaStore content URIs.
 *
 * Important: decoding NEVER runs on the UI thread and cannot hold the UI
 * hostage when an audio file has missing or corrupt cover metadata.
 * No network access; empty covers use the caller's artwork placeholder.
 * A bounded queue prevents unbounded thumbnail jobs during fast scrolling.
 */
internal object LocalAlbumArtwork {
    private const val TAG = "FARIC-album-art"
    private const val THUMBNAIL_MAX_PX = 220
    private data class Result(val bitmap: Bitmap?)

    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val cache = object : LruCache<String, Result>(20 * 1024) {
        override fun sizeOf(key: String, value: Result): Int =
            ((value.bitmap?.byteCount ?: 1024) / 1024).coerceAtLeast(1)
    }
    private val requests = mutableMapOf<String, MutableList<WeakReference<ImageView>>>()
    private val executor = ThreadPoolExecutor(
        2, 2, 10L, TimeUnit.SECONDS,
        ArrayBlockingQueue(40),
        { task -> Thread(task, "faric-embedded-album-art").apply { isDaemon = true } },
    )

    fun bind(context: Context, view: ImageView, uri: Uri?) {
        val key = uri?.toString()
        view.tag = key
        view.setImageDrawable(null)
        if (key == null || uri == null) return

        synchronized(lock) {
            val hit = cache.get(key)
            if (hit != null) {
                view.setImageBitmap(hit.bitmap)
                return
            }
            val waiting = requests[key]
            if (waiting != null) {
                waiting.add(WeakReference(view))
                return
            }
            requests[key] = mutableListOf(WeakReference(view))
        }

        try {
            executor.execute {
                val bitmap = decode(context.applicationContext, uri)
                val listeners: List<WeakReference<ImageView>>
                synchronized(lock) {
                    cache.put(key, Result(bitmap))
                    listeners = requests.remove(key).orEmpty()
                }
                main.post {
                    for (weak in listeners) {
                        val target = weak.get() ?: continue
                        if (target.tag == key) {
                            target.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
            synchronized(lock) { requests.remove(key) }
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            val bytes = try {
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture
            } finally {
                retriever.release()
            } ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > THUMBNAIL_MAX_PX * 2 ||
                bounds.outHeight / sample > THUMBNAIL_MAX_PX * 2
            ) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                })
        } catch (error: Exception) {
            Log.d(TAG, "No readable embedded cover: ${error.javaClass.simpleName}")
            null
        }
    }
}
