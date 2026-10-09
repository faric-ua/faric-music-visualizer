package com.saney.musicvisualizer.playback

import android.content.Context

/**
 * Device-only user choices. Does not modify audio files or remote accounts.
 * Play history means played through FARIC, not recently imported to MediaStore.
 */
class LocalMusicUserState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("faric-local-media-user-state", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = prefs.getStringSet("favorite-uris", emptySet())?.toSet() ?: emptySet()

    fun isFavorite(uri: String): Boolean = uri in favorites()

    fun toggleFavorite(uri: String): Boolean {
        val copy = favorites().toMutableSet()
        val enabled = if (uri in copy) { copy.remove(uri); false } else { copy.add(uri); true }
        prefs.edit().putStringSet("favorite-uris", copy).apply()
        return enabled
    }

    fun recentlyPlayed(): List<String> {
        return prefs.getString("recent-uris", "").orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }
            .distinct()
            .take(100)
    }

    fun recordPlay(uri: String) {
        val list = (listOf(uri) + recentlyPlayed().filterNot { it == uri }).take(100)
        prefs.edit().putString("recent-uris", list.joinToString("\n")).apply()
    }
}
