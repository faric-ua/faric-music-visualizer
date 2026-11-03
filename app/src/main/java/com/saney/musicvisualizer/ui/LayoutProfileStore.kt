package com.saney.musicvisualizer.ui

import android.content.Context

/** V104: the saved «Книжка» / «Альбом» / «Відео 9:16» sets (Утиліти → Розкладка екрана → Зберегти). */
class LayoutProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** V203: the display's long ÷ short side — how much bigger «Альбом» parts were before v0.21.68. */
    private val longOverShort: Float =
        context.applicationContext.resources.displayMetrics.let {
            maxOf(it.widthPixels, it.heightPixels).toFloat() / minOf(it.widthPixels, it.heightPixels).coerceAtLeast(1)
        }

    fun load(id: LayoutProfileId): Map<String, LayoutAdjust> {
        val raw = prefs.getString(key(id), null)
        val flag = key(id) + MIGRATED
        if (raw.isNullOrBlank() || prefs.getBoolean(flag, false)) return LayoutProfileCodec.decode(raw)
        val migrated = migrate(id, LayoutProfileCodec.decode(raw))
        prefs.edit().putString(key(id), LayoutProfileCodec.encode(migrated)).putBoolean(flag, true).apply()
        return migrated
    }

    private fun migrate(
        id: LayoutProfileId,
        adjusts: Map<String, LayoutAdjust>,
    ): Map<String, LayoutAdjust> {
        val split = LayoutMigration.splitTrack(adjusts)
        return if (id == LayoutProfileId.LANDSCAPE) LayoutMigration.albumSizes(split, longOverShort) else split
    }

    fun loadAll(): Map<LayoutProfileId, Map<String, LayoutAdjust>> = LayoutProfileId.entries.associateWith { load(it) }

    fun save(
        id: LayoutProfileId,
        adjusts: Map<String, LayoutAdjust>,
    ) {
        prefs.edit().putString(key(id), LayoutProfileCodec.encode(adjusts)).putBoolean(key(id) + MIGRATED, true).apply()
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
                val flag = "$prefix$id|$MIGRATED"
                var adjusts = LayoutProfileCodec.decode(snaps.getString(key, null))
                if (!snaps.getBoolean(flag, false)) {
                    adjusts = migrate(profile, adjusts)
                    snaps.edit().putString(key, LayoutProfileCodec.encode(adjusts)).putBoolean(flag, true).apply()
                }
                Snapshot(
                    id = id,
                    name = name,
                    savedAt = snaps.getLong("$prefix$id|at", 0L),
                    adjusts = adjusts,
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
            .putString("$prefix|name", SnapshotName.clean(name))
            .putLong("$prefix|at", now)
            .putString("$prefix|data", LayoutProfileCodec.encode(adjusts))
            .putBoolean("$prefix|$MIGRATED", true)
            .apply()
    }

    fun deleteSnapshot(
        profile: LayoutProfileId,
        id: String,
    ) {
        val prefix = "${profile.name}|$id"
        snaps.edit().remove("$prefix|name").remove("$prefix|at").remove("$prefix|data").remove("$prefix|$MIGRATED").apply()
    }

    private companion object {
        const val PREFS = "pulsedeck_layout_profiles"
        const val SNAPS = "pulsedeck_layout_snapshots"
        const val MIGRATED = "v203"
    }
}
