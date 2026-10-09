package com.saney.musicvisualizer.playback

import java.util.Locale

enum class LocalMusicCategory(val title: String, val icon: String) {
    ALL("Усі треки", "♫"),
    FOLDERS("Теки", "▰"),
    ALBUMS("Альбоми", "◉"),
    ARTISTS("Виконавці", "●"),
    GENRES("Жанри", "✦"),
    YEARS("Роки", "20"),
    FAVORITES("Улюблені", "♥"),
    RECENT("Нещодавні", "↺"),
    PLAYLISTS("Мої добірки", "▤");

    val grouped: Boolean
        get() = this in listOf(FOLDERS, ALBUMS, ARTISTS, GENRES, YEARS, PLAYLISTS)
}

data class LocalMusicGroup(
    val title: String,
    val tracks: List<LocalMusicTrack>,
)

object LocalMusicCatalog {
    fun groups(category: LocalMusicCategory, tracks: List<LocalMusicTrack>): List<LocalMusicGroup> {
        val key: (LocalMusicTrack) -> String = when (category) {
            LocalMusicCategory.FOLDERS -> { t -> t.folder.ifBlank { "Інші" } }
            LocalMusicCategory.ALBUMS -> { t -> t.album.ifBlank { "Невідомий альбом" } }
            LocalMusicCategory.ARTISTS -> { t -> t.artist.ifBlank { "Невідомий виконавець" } }
            LocalMusicCategory.GENRES -> { t -> t.genre.ifBlank { "Невідомий жанр" } }
            LocalMusicCategory.YEARS -> { t -> if (t.year > 0) t.year.toString() else "Рік невідомий" }
            else -> return emptyList()
        }
        val groups = tracks.groupBy(key).map { (name, items) ->
            LocalMusicGroup(name, items)
        }
        return if (category == LocalMusicCategory.YEARS) {
            groups.sortedWith(
                compareByDescending<LocalMusicGroup> { it.title.toIntOrNull() ?: -1 }
                    .thenBy { it.title.lowercase(Locale.ROOT) },
            )
        } else {
            groups.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        }
    }

    fun sortTracks(items: List<LocalMusicTrack>, sort: LocalMusicSort): List<LocalMusicTrack> {
        return when (sort) {
            LocalMusicSort.TITLE -> items.sortedWith(
                compareBy<LocalMusicTrack> { it.title.lowercase(Locale.ROOT) }.thenBy { it.id },
            )
            LocalMusicSort.ARTIST -> items.sortedWith(
                compareBy<LocalMusicTrack> { it.artist.lowercase(Locale.ROOT) }
                    .thenBy { it.album.lowercase(Locale.ROOT) }
                    .thenBy { it.title.lowercase(Locale.ROOT) },
            )
            LocalMusicSort.ALBUM_ORDER -> items.sortedWith(
                compareBy<LocalMusicTrack> { it.album.lowercase(Locale.ROOT) }
                    .thenBy { it.trackNumber.takeIf { value -> value > 0 } ?: Int.MAX_VALUE }
                    .thenBy { it.title.lowercase(Locale.ROOT) },
            )
            LocalMusicSort.NEWEST -> items.sortedByDescending { it.addedAtSec }
            LocalMusicSort.DURATION -> items.sortedWith(
                compareByDescending<LocalMusicTrack> { it.durationMs }.thenBy { it.title.lowercase(Locale.ROOT) },
            )
        }
    }

    fun recentTracks(items: List<LocalMusicTrack>, recentlyPlayedUri: List<String>): List<LocalMusicTrack> {
        val byUri = items.associateBy { it.uri.toString() }
        return recentlyPlayedUri.mapNotNull(byUri::get)
    }

    fun favoriteTracks(items: List<LocalMusicTrack>, favoriteUris: Set<String>): List<LocalMusicTrack> {
        return items.filter { it.uri.toString() in favoriteUris }
    }
}

enum class LocalMusicSort(val label: String) {
    TITLE("Назва"), ARTIST("Виконавець"), ALBUM_ORDER("Альбом"), NEWEST("Додані"), DURATION("Тривалість");
    fun next(): LocalMusicSort = entries[(ordinal + 1) % entries.size]
}
