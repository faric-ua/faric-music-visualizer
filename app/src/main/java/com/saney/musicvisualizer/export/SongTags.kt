package com.saney.musicvisualizer.export

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/**
 * V93: the song's own tags (title, artist, album, year, genre, cover) for the MP4 and for
 * sorting videos into album folders. Missing tags → what the player shows ([fallbackTitle]).
 */
object SongTags {
    private const val MAX_COVER_BYTES = 4 * 1024 * 1024

    fun read(
        context: Context,
        uri: Uri,
        fallbackTitle: String?,
        fallbackArtist: String?,
    ): Mp4Tagger.Tags {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            fun tag(key: Int): String? =
                runCatching { retriever.extractMetadata(key) }.getOrNull()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && it != "<unknown>" }
            Mp4Tagger.Tags(
                // A tag that lost emoji («????», V97) is worse than the name the player shows.
                title = tag(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { '?' !in it } ?: fallbackTitle,
                artist =
                    tag(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: tag(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        ?: fallbackArtist?.takeIf { it.isNotBlank() && it != "Невідомий виконавець" },
                album = tag(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                year =
                    tag(MediaMetadataRetriever.METADATA_KEY_YEAR)
                        ?: tag(MediaMetadataRetriever.METADATA_KEY_DATE)?.take(4)?.takeIf { it.all(Char::isDigit) },
                genre = tag(MediaMetadataRetriever.METADATA_KEY_GENRE),
                cover = runCatching { retriever.embeddedPicture }.getOrNull()?.takeIf { it.size in 9..MAX_COVER_BYTES },
                track = tag(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
            )
        } catch (_: Throwable) {
            Mp4Tagger.Tags(title = fallbackTitle, artist = fallbackArtist?.takeIf { it.isNotBlank() })
        } finally {
            runCatching { retriever.release() }
        }
    }
}
