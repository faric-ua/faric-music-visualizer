package com.saney.musicvisualizer.playback

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.util.Locale

/**
 * Read-only device music catalog. Use on a worker thread only.
 * Device permissions are controlled by the Activity; the query never mutates
 * files, indexes, playback state, or MediaStore.
 */
data class LocalMusicTrack(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

object LocalMusicLibrary {
    fun scan(context: Context): List<LocalMusicTrack> {
        val columns = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )
        val tracks = mutableListOf<LocalMusicTrack>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val title = cursor.getString(titleIndex).orEmpty().ifBlank { "Без назви" }
                val artist = cursor.getString(artistIndex).orEmpty().let {
                    if (it.isBlank() || it == "<unknown>") "Невідомий виконавець" else it
                }
                val album = cursor.getString(albumIndex).orEmpty()
                val duration = if (cursor.isNull(durationIndex)) 0L else cursor.getLong(durationIndex)
                tracks += LocalMusicTrack(
                    id = id,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ),
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration.coerceAtLeast(0L),
                )
            }
        }
        return tracks.sortedWith(
            compareBy<LocalMusicTrack> { it.title.lowercase(Locale.ROOT) }
                .thenBy { it.artist.lowercase(Locale.ROOT) }
                .thenBy { it.id },
        )
    }
}

object LocalMusicSearch {
    fun matches(
        title: String,
        artist: String,
        album: String,
        query: String,
    ): Boolean {
        val needle = query.trim()
        return needle.isEmpty() ||
            title.contains(needle, ignoreCase = true) ||
            artist.contains(needle, ignoreCase = true) ||
            album.contains(needle, ignoreCase = true)
    }
}
