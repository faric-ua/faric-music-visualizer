package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.File

/**
 * V220 (faric 04.11 20:18: «є ТОП, ВСІ, 👍, легкі / середні / важкі — хочу ще улюблені: сердечко нажимаєш, і вони
 * окремо списком»). ♡ is separate from 👍 / 👎: a visualization can be 👍 and ♡ at once; ♡ — the chosen best ones.
 * Kept by preset id (as the other stores), the file itself is never touched.
 */
class ProjectMFavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("projectm_preset_favorites", Context.MODE_PRIVATE)

    fun isFavorite(file: File): Boolean = prefs.getBoolean(PresetFavorites.key(ProjectMLibraryManager.presetId(file)), false)

    /** ♡ on / off; returns the new state. */
    fun toggle(file: File): Boolean {
        val key = PresetFavorites.key(ProjectMLibraryManager.presetId(file))
        val now = !prefs.getBoolean(key, false)
        prefs.edit().apply { if (now) putBoolean(key, true) else remove(key) }.apply()
        return now
    }

    fun count(): Int = PresetFavorites.count(prefs.all.keys)
}

/** Pure part — JVM tests. */
object PresetFavorites {
    private const val PREFIX = "fav:"

    fun key(presetId: String): String = PREFIX + presetId

    fun count(keys: Collection<String>): Int = keys.count { it.startsWith(PREFIX) }

    /** ♥ when chosen, ♡ when not — the button on the right page and in the list. */
    fun icon(favorite: Boolean): String = if (favorite) "♥" else "♡"
}
