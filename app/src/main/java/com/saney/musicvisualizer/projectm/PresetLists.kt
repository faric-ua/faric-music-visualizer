package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.File

/**
 * V221 (faric 04.11 20:18: «щоб можна було створювати свої списки, групувати» → 05.11 17:25: «давай свої списки»).
 * faric's own lists of visualizations, e.g. «Для альбому White Side». A visualization can be in many lists; the
 * files themselves are never touched — only their ids are kept.
 */
class ProjectMPresetListsStore(context: Context) {
    private val prefs = context.getSharedPreferences("projectm_preset_lists", Context.MODE_PRIVATE)

    fun lists(): List<PresetLists.Entry> = PresetLists.decode(prefs.getString(KEY_LISTS, null))

    fun find(id: String?): PresetLists.Entry? = lists().firstOrNull { it.id == id }

    fun create(name: String): PresetLists.Entry {
        val existing = lists()
        val entry = PresetLists.Entry(PresetLists.newId(existing, System.currentTimeMillis()), PresetLists.cleanName(name))
        prefs.edit().putString(KEY_LISTS, PresetLists.encode(existing + entry)).apply()
        return entry
    }

    fun rename(
        id: String,
        name: String,
    ) {
        val next = lists().map { if (it.id == id) it.copy(name = PresetLists.cleanName(name)) else it }
        prefs.edit().putString(KEY_LISTS, PresetLists.encode(next)).apply()
    }

    /** The list goes; the visualizations stay where they are (only the list's ticks are removed). */
    fun delete(id: String) {
        val editor = prefs.edit()
        editor.putString(KEY_LISTS, PresetLists.encode(lists().filterNot { it.id == id }))
        prefs.all.keys.filter { PresetLists.isMemberKeyOf(it, id) }.forEach { editor.remove(it) }
        editor.apply()
    }

    fun contains(
        id: String,
        file: File,
    ): Boolean = prefs.getBoolean(PresetLists.memberKey(id, ProjectMLibraryManager.presetId(file)), false)

    fun setMember(
        id: String,
        file: File,
        member: Boolean,
    ) {
        val key = PresetLists.memberKey(id, ProjectMLibraryManager.presetId(file))
        prefs.edit().apply { if (member) putBoolean(key, true) else remove(key) }.apply()
    }

    fun count(id: String): Int = prefs.all.keys.count { PresetLists.isMemberKeyOf(it, id) }

    companion object {
        private const val KEY_LISTS = "lists"
    }
}

/** Pure part — JVM tests. */
object PresetLists {
    data class Entry(
        val id: String,
        val name: String,
    )

    private const val SEP = '\u001F'
    private const val LINE = '\n'
    const val MAX_NAME = 40

    fun encode(entries: List<Entry>): String = entries.joinToString(LINE.toString()) { it.id + SEP + it.name }

    fun decode(raw: String?): List<Entry> =
        raw.orEmpty()
            .split(LINE)
            .mapNotNull { line ->
                val cut = line.indexOf(SEP)
                if (cut <= 0) return@mapNotNull null
                Entry(line.substring(0, cut), line.substring(cut + 1))
            }

    /** One line, no separators, not empty, at most [MAX_NAME] letters. */
    fun cleanName(raw: String): String =
        raw.replace(SEP, ' ').replace(LINE, ' ').replace(Regex("\\s+"), " ").trim().take(MAX_NAME).ifBlank { "Мій список" }

    fun newId(
        existing: List<Entry>,
        nowMs: Long,
    ): String {
        var id = "l" + nowMs.toString(36)
        var n = 1
        while (existing.any { it.id == id }) id = "l" + nowMs.toString(36) + "-" + n++
        return id
    }

    fun memberKey(
        listId: String,
        presetId: String,
    ): String = "m:$listId:$presetId"

    fun isMemberKeyOf(
        key: String,
        listId: String,
    ): Boolean = key.startsWith("m:$listId:")
}
