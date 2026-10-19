package com.saney.musicvisualizer.playback

import android.content.Context
import java.io.File

/** Where V65 keeps the last queue (a small text file) and the song index / position (prefs). */
class LastSessionStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("faric_last_session_v1", Context.MODE_PRIVATE)
    private val queueFile = File(app.filesDir, "last_session_queue_v1.txt")

    fun saveQueue(tracks: List<LastSessionCodec.Track>, index: Int) {
        runCatching { queueFile.writeText(LastSessionCodec.encode(tracks)) }
        prefs.edit().putInt(KEY_INDEX, index).putLong(KEY_POSITION, 0L).putLong(KEY_DURATION, 0L).apply()
    }

    fun savePosition(index: Int, positionMs: Long, durationMs: Long) {
        prefs.edit()
            .putInt(KEY_INDEX, index)
            .putLong(KEY_POSITION, positionMs.coerceAtLeast(0L))
            .putLong(KEY_DURATION, durationMs.coerceAtLeast(0L))
            .apply()
    }

    fun loadTracks(): List<LastSessionCodec.Track> =
        runCatching { if (queueFile.isFile) LastSessionCodec.decode(queueFile.readText()) else emptyList() }
            .getOrDefault(emptyList())

    fun index(): Int = prefs.getInt(KEY_INDEX, 0)

    fun position(): Long =
        LastSessionCodec.resumePosition(prefs.getLong(KEY_POSITION, 0L), prefs.getLong(KEY_DURATION, 0L))

    fun clear() {
        runCatching { queueFile.delete() }
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_INDEX = "index"
        const val KEY_POSITION = "position_ms"
        const val KEY_DURATION = "duration_ms"
    }
}
