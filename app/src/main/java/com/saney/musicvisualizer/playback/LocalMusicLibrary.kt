package com.saney.musicvisualizer.playback

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import java.io.File
import java.util.Locale

/**
 * Read-only MediaStore catalog, scanned off the UI thread. Extended metadata
 * powers real albums/artists/folders/genres/years rather than placeholder cards.
 */
data class LocalMusicTrack(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val folder: String = "Інші",
    val genre: String = "Невідомий жанр",
    val year: Int = 0,
    val addedAtSec: Long = 0L,
    val trackNumber: Int = 0,
)

object LocalMusicLibrary {
    @Volatile private var cache: List<LocalMusicTrack>? = null
    @Volatile private var cacheAtMs: Long = 0L
    private const val CACHE_TTL_MS = 45_000L

    fun invalidate() {
        cache = null
    }

    @Synchronized
    fun scan(context: Context, force: Boolean = false): List<LocalMusicTrack> {
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force) {
            val existing = cache
            if (existing != null && now - cacheAtMs < CACHE_TTL_MS) return existing
        }
        val baseColumns = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
        )
        val pathColumn = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.DATA
        }
        baseColumns += pathColumn
        val includeGenre = Build.VERSION.SDK_INT >= 30
        val projected = baseColumns + if (includeGenre) listOf(MediaStore.Audio.Media.GENRE) else emptyList()
        val tracks = mutableListOf<LocalMusicTrack>()

        fun query(columns: List<String>, genreFromColumn: Boolean) {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                columns.toTypedArray(),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val year = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val added = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackNo = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val path = cursor.getColumnIndexOrThrow(pathColumn)
                val genre = if (genreFromColumn) cursor.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
                while (cursor.moveToNext()) {
                    val trackId = cursor.getLong(id)
                    val rawFolder = cursor.getString(path).orEmpty()
                    val normalizedFolder = if (Build.VERSION.SDK_INT >= 29) {
                        rawFolder.trim().trim('/')
                    } else {
                        File(rawFolder).parent.orEmpty().trimEnd('/')
                    }
                    tracks += LocalMusicTrack(
                        id = trackId,
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId),
                        title = cursor.getString(title).orEmpty().ifBlank { "Без назви" },
                        artist = cursor.getString(artist).orEmpty()
                            .takeUnless { it.isBlank() || it == "<unknown>" } ?: "Невідомий виконавець",
                        album = cursor.getString(album).orEmpty()
                            .takeUnless { it.isBlank() || it == "<unknown>" } ?: "Невідомий альбом",
                        durationMs = if (cursor.isNull(duration)) 0L else cursor.getLong(duration).coerceAtLeast(0L),
                        folder = normalizedFolder.ifBlank { "Інші" },
                        genre = if (genre >= 0) cursor.getString(genre).orEmpty()
                            .takeUnless { it.isBlank() || it == "<unknown>" } ?: "Невідомий жанр"
                        else "Невідомий жанр",
                        year = if (cursor.isNull(year)) 0 else cursor.getInt(year).takeIf { it in 1850..2200 } ?: 0,
                        addedAtSec = if (cursor.isNull(added)) 0L else cursor.getLong(added).coerceAtLeast(0L),
                        trackNumber = if (cursor.isNull(trackNo)) 0 else cursor.getInt(trackNo).coerceAtLeast(0),
                    )
                }
            }
        }

        // Some OEM MediaStore providers reject optional metadata projections.
        // Fall back to the base columns while preserving the whole library.
        if (includeGenre) {
            val result = runCatching { query(projected, true) }
            if (result.isFailure) {
                tracks.clear()
                query(baseColumns, false)
            }
        } else {
            query(baseColumns, false)
        }

        // Older Android does not expose GENRE in MediaStore.Audio.Media.
        // Read the legacy genre relationship index once per scan, not per row.
        if (!includeGenre && tracks.isNotEmpty()) {
            val labels = mutableMapOf<Long, String>()
            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                    null, null, null,
                )?.use { genres ->
                    val genreId = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                    val genreName = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                    while (genres.moveToNext()) {
                        val label = genres.getString(genreName).orEmpty().trim()
                        if (label.isBlank()) continue
                        context.contentResolver.query(
                            MediaStore.Audio.Genres.Members.getContentUri("external", genres.getLong(genreId)),
                            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                            null, null, null,
                        )?.use { members ->
                            val audioId = members.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                            while (members.moveToNext()) labels.putIfAbsent(members.getLong(audioId), label)
                        }
                    }
                }
            }
            if (labels.isNotEmpty()) {
                for (i in tracks.indices) {
                    val value = labels[tracks[i].id] ?: continue
                    tracks[i] = tracks[i].copy(genre = value)
                }
            }
        }

        val sorted = tracks.sortedWith(
            compareBy<LocalMusicTrack> { it.title.lowercase(Locale.ROOT) }
                .thenBy { it.artist.lowercase(Locale.ROOT) }
                .thenBy { it.id },
        )
        cache = sorted
        cacheAtMs = android.os.SystemClock.elapsedRealtime()
        return sorted
    }
}

object LocalMusicSearch {
    fun matches(title: String, artist: String, album: String, query: String): Boolean {
        val needle = query.trim()
        return needle.isEmpty() ||
            title.contains(needle, ignoreCase = true) ||
            artist.contains(needle, ignoreCase = true) ||
            album.contains(needle, ignoreCase = true)
    }

    fun matches(track: LocalMusicTrack, query: String): Boolean {
        return matches(track.title, track.artist, track.album, query) ||
            (query.isNotBlank() && (
                track.folder.contains(query.trim(), ignoreCase = true) ||
                track.genre.contains(query.trim(), ignoreCase = true) ||
                track.year.toString() == query.trim()
            ))
    }
}
