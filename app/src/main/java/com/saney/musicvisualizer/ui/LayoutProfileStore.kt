package com.saney.musicvisualizer.ui

import android.content.Context

/** V104: the saved «Книжка» / «Альбом» / «Відео 9:16» sets (Утиліти → Розкладка екрана → Зберегти). */
class LayoutProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(id: LayoutProfileId): Map<String, LayoutAdjust> = LayoutProfileCodec.decode(prefs.getString(key(id), null))

    fun loadAll(): Map<LayoutProfileId, Map<String, LayoutAdjust>> = LayoutProfileId.entries.associateWith { load(it) }

    fun save(
        id: LayoutProfileId,
        adjusts: Map<String, LayoutAdjust>,
    ) {
        prefs.edit().putString(key(id), LayoutProfileCodec.encode(adjusts)).apply()
    }

    /** «Скинути весь екран» — back to the approved template for this position only. */
    fun clear(id: LayoutProfileId) {
        prefs.edit().remove(key(id)).apply()
    }

    private fun key(id: LayoutProfileId) = "profile_" + id.name.lowercase()

    /** V125 (faric 22.10 01:45: «зберігати файл налаштувань — раз зберіг, інший раз загрузив»). */
    data class Snapshot(
        val id: String,
        val name: String,
        val savedAt: Long,
        val adjusts: Map<String, LayoutAdjust>,
    )

    private val snaps = context.applicationContext.getSharedPreferences(SNAPS, Context.MODE_PRIVATE)

    fun snapshots(profile: LayoutProfileId): List<Snapshot> {
        val prefix = "${profile.name}|"
        return snaps.all.keys
            .filter { it.startsWith(prefix) && it.endsWith("|data") }
            .mapNotNull { key ->
                val id = key.removePrefix(prefix).removeSuffix("|data")
                val name = snaps.getString("$prefix$id|name", null) ?: return@mapNotNull null
                Snapshot(
                    id = id,
                    name = name,
                    savedAt = snaps.getLong("$prefix$id|at", 0L),
                    adjusts = LayoutProfileCodec.decode(snaps.getString(key, null)),
                )
            }
            .sortedByDescending { it.savedAt }
    }

    fun saveSnapshot(
        profile: LayoutProfileId,
        name: String,
        adjusts: Map<String, LayoutAdjust>,
    ) {
        val now = System.currentTimeMillis()
        val id = now.toString()
        val prefix = "${profile.name}|$id"
        snaps.edit()
            .putString("$prefix|name", name.trim().ifBlank { "Без назви" }.take(60))
            .putLong("$prefix|at", now)
            .putString("$prefix|data", LayoutProfileCodec.encode(adjusts))
            .apply()
    }

    fun deleteSnapshot(
        profile: LayoutProfileId,
        id: String,
    ) {
        val prefix = "${profile.name}|$id"
        snaps.edit().remove("$prefix|name").remove("$prefix|at").remove("$prefix|data").apply()
    }

    private companion object {
        const val PREFS = "pulsedeck_layout_profiles"
        const val SNAPS = "pulsedeck_layout_snapshots"
    }
}
