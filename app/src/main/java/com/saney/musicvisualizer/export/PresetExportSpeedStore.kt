package com.saney.musicvisualizer.export

import android.content.Context
import com.saney.musicvisualizer.projectm.ProjectMLibraryManager
import java.io.File

/** V131: the remembered speed of every visualization in videos (all exports add to it). */
class PresetExportSpeedStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun idOf(path: String): String = runCatching { ProjectMLibraryManager.presetId(File(path)) }.getOrDefault(path)

    fun all(): Map<String, PresetSpeed> = PresetExportSpeed.decode(prefs.getString(KEY, null))

    fun markOf(path: String): PresetSpeedMark = PresetExportSpeed.mark(all()[idOf(path)])

    /** Add one export's numbers (each visualization's frames and whole-frame time). */
    @Synchronized
    fun record(timings: List<ShortVideoExportProof.PresetTiming>) {
        val useful = timings.filter { it.frames > 0 && it.path != "—" }
        if (useful.isEmpty()) return
        val map = all().toMutableMap()
        useful.forEach { t ->
            val id = idOf(t.path)
            map[id] = (map[id] ?: PresetSpeed(0, 0L)).plus(t.frames, t.totalMs)
        }
        prefs.edit().putString(KEY, PresetExportSpeed.encode(map)).apply()
    }

    /** For the crash journal: how many are ✓ / ? / ! and which are slow. */
    fun summary(): String {
        val map = all()
        if (map.isEmpty()) return "Візуалізації у відео: ще нема замірів."
        val marked = map.mapValues { PresetExportSpeed.mark(it.value) }
        fun count(mark: PresetSpeedMark) = marked.values.count { it == mark }
        return buildString {
            append("Візуалізації у відео (середній час кадру):\n")
            append("✓ ${count(PresetSpeedMark.OK)} нормальні · ? ${count(PresetSpeedMark.SLOW)} повільні · ")
            append("! ${count(PresetSpeedMark.TOO_SLOW)} дуже повільні · · ${count(PresetSpeedMark.UNKNOWN)} мало замірів")
            map.entries
                .filter { PresetExportSpeed.mark(it.value) == PresetSpeedMark.SLOW || PresetExportSpeed.mark(it.value) == PresetSpeedMark.TOO_SLOW }
                .sortedByDescending { it.value.msPerFrame }
                .take(15)
                .forEach { (id, speed) ->
                    append("\n")
                    append(PresetExportSpeed.mark(speed).sign)
                    append(" ")
                    append(speed.msPerFrame)
                    append(" мс · ")
                    append(speed.frames / 30)
                    append(" с · ")
                    append(id.substringAfterLast('/').removeSuffix(".milk").take(48))
                }
        }
    }

    private companion object {
        const val PREFS = "pulsedeck_preset_export_speed"
        const val KEY = "speeds"
    }
}
