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

    private companion object {
        const val PREFS = "pulsedeck_layout_profiles"
    }
}
