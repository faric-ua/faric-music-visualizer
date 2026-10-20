package com.saney.musicvisualizer.export

/**
 * V94 (faric 20.10 22:44): «8 файлів експортую — візуалізація одна й та сама; а на головному екрані
 * AUTO за 5 с міняє». The video follows the same AUTO as the screen:
 *  - [Mode.Timer]: a new visualization every N seconds of the video;
 *  - [Mode.PerSong] («AUTO ПІСНЯ»): one visualization per song, the next song gets the next one;
 * and a batch carries the number on: song 2 starts after the last one song 1 used. The caller
 * saves [lastUsed] after every song, so the app goes on from there.
 * Pure Kotlin — tested on JVM.
 */
class ExportPresetSchedule(
    private val pool: List<String>,
    /** Index in [pool] of the visualization on screen now (not found → 0). */
    startIndex: Int,
    private val mode: Mode,
) {
    sealed class Mode {
        data class Timer(val seconds: Int) : Mode()
        object PerSong : Mode()
    }

    private var songStart = startIndex.coerceAtLeast(0)
    private var lastIndex = songStart
    private var songs = 0

    /** Call before each song (the first song starts with the visualization on screen). */
    fun beginSong() {
        if (songs > 0) songStart = lastIndex + 1
        lastIndex = songStart
        songs++
    }

    /** Visualization for [videoTimeMs] from the start of this song's video. */
    fun presetAt(videoTimeMs: Long): String {
        val index =
            when (mode) {
                is Mode.Timer -> songStart + (videoTimeMs.coerceAtLeast(0L) / (mode.seconds.coerceAtLeast(1) * 1000L)).toInt()
                Mode.PerSong -> songStart
            }
        if (index > lastIndex) lastIndex = index
        return pool[Math.floorMod(index, pool.size)]
    }

    /** The last visualization used so far — saved so the app continues from it. */
    val lastUsed: String
        get() = pool[Math.floorMod(lastIndex, pool.size)]

    companion object {
        /** null when there is nothing to switch (AUTO OFF or an empty list). */
        fun createOrNull(
            pool: List<String>,
            current: String?,
            autoEnabled: Boolean,
            autoSeconds: Int,
            perSong: Boolean,
        ): ExportPresetSchedule? {
            if (pool.isEmpty()) return null
            val mode =
                when {
                    perSong -> Mode.PerSong
                    autoEnabled -> Mode.Timer(autoSeconds)
                    else -> return null
                }
            return ExportPresetSchedule(pool, pool.indexOf(current).coerceAtLeast(0), mode)
        }
    }
}
