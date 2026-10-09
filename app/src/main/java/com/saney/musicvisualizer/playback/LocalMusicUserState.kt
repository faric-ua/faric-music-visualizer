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

    fun playlists(): Map<String, List<String>> {
        val json = runCatching { org.json.JSONObject(prefs.getString("playlist-json", "{}")) }
            .getOrDefault(org.json.JSONObject())
        val result = linkedMapOf<String, List<String>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val array = json.optJSONArray(name) ?: continue
            result[name] = (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }.distinct()
        }
        return result
    }

    fun createPlaylist(name: String): Boolean {
        val title = name.trim().take(60)
        if (title.isEmpty()) return false
        val saved = playlists()
        if (saved.keys.any { it.equals(title, ignoreCase = true) }) return false
        savePlaylists(saved + (title to emptyList()))
        return true
    }

    fun deletePlaylist(name: String) {
        savePlaylists(playlists().filterKeys { it != name })
    }

    fun setPlaylistTrack(name: String, uri: String, enabled: Boolean) {
        val saved = playlists()
        val current = saved[name] ?: return
        val updated = if (enabled) (current + uri).distinct() else current.filterNot { it == uri }
        savePlaylists(saved + (name to updated))
    }

    private fun savePlaylists(items: Map<String, List<String>>) {
        val json = org.json.JSONObject()
        items.forEach { (name, uris) -> json.put(name, org.json.JSONArray(uris)) }
        prefs.edit().putString("playlist-json", json.toString()).apply()
    }


    fun recordPlay(uri: String) {
        val list = (listOf(uri) + recentlyPlayed().filterNot { it == uri }).take(100)
        prefs.edit().putString("recent-uris", list.joinToString("\n")).apply()
    }
}
