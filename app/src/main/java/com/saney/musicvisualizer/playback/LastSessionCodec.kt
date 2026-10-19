package com.saney.musicvisualizer.playback

/**
 * V65 (faric 19.10 23:17): Android may close the app while faric is in another app
 * (paused music, nothing keeps the process alive). On the next start the player was
 * empty — «FARIC PulseDeck · Оберіть музику». The queue, the current song and the
 * position are saved, and the player comes back paused on the same song.
 *
 * Pure text codec (no Android) so it is tested on JVM. Control characters never
 * appear in file names / content URIs, so they are safe separators.
 */
object LastSessionCodec {
    data class Track(
        val uri: String,
        val name: String,
        val artist: String? = null,
        val album: String? = null,
    )

    private const val FIELD = '\u001F'
    private const val ROW = '\u001E'

    /** The end of a song counts as «finished»: start it from 0:00 next time. */
    const val NEAR_END_MS = 3_000L

    fun encode(tracks: List<Track>): String =
        tracks.joinToString(ROW.toString()) { track ->
            listOf(track.uri, track.name, track.artist.orEmpty(), track.album.orEmpty())
                .joinToString(FIELD.toString()) { it.replace(FIELD, ' ').replace(ROW, ' ') }
        }

    fun decode(text: String?): List<Track> {
        if (text.isNullOrBlank()) return emptyList()
        return text.split(ROW).mapNotNull { row ->
            val parts = row.split(FIELD)
            val uri = parts.getOrNull(0)?.trim().orEmpty()
            if (uri.isEmpty()) return@mapNotNull null
            Track(
                uri = uri,
                name = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: uri.substringAfterLast('/'),
                artist = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                album = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
            )
        }
    }

    fun safeIndex(index: Int, size: Int): Int = if (size <= 0) 0 else index.coerceIn(0, size - 1)

    fun resumePosition(positionMs: Long, durationMs: Long): Long =
        when {
            positionMs <= 0L -> 0L
            durationMs > 0L && positionMs >= durationMs - NEAR_END_MS -> 0L
            else -> positionMs
        }
}
