package com.saney.musicvisualizer.projectm

import android.content.Context
import java.io.File

enum class ProjectMBackgroundMode {
    TOP,
    ALL,
}

class ProjectMStateStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("projectm_visualizer_state", Context.MODE_PRIVATE)

    var backgroundMode: ProjectMBackgroundMode
        get() = runCatching {
            ProjectMBackgroundMode.valueOf(
                prefs.getString(KEY_BACKGROUND_MODE, ProjectMBackgroundMode.TOP.name)
                    ?: ProjectMBackgroundMode.TOP.name,
            )
        }.getOrDefault(ProjectMBackgroundMode.TOP)
        set(value) {
            prefs.edit().putString(KEY_BACKGROUND_MODE, value.name).apply()
        }

    var autoEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_ENABLED, value).apply()
        }

    var lastPresetPath: String?
        get() = prefs.getString(KEY_LAST_PRESET_PATH, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) {
                    remove(KEY_LAST_PRESET_PATH)
                } else {
                    putString(KEY_LAST_PRESET_PATH, value)
                }
            }.apply()
        }

    var foregroundSample: FaricForegroundSample
        get() {
            val saved = prefs.getInt(
                KEY_FOREGROUND_SAMPLE,
                FaricForegroundSample.PULSE_RAYS.ordinal,
            )
            return FaricForegroundSample.entries.getOrElse(saved) {
                FaricForegroundSample.PULSE_RAYS
            }
        }
        set(value) {
            prefs.edit().putInt(KEY_FOREGROUND_SAMPLE, value.ordinal).apply()
        }

    var backgroundVisible: Boolean
        get() =
            prefs.getBoolean(
                KEY_BACKGROUND_VISIBLE,
                true,
            )
        set(value) {
            prefs.edit()
                .putBoolean(
                    KEY_BACKGROUND_VISIBLE,
                    value,
                )
                .apply()
        }

    var foregroundCenterVisible: Boolean
        get() =
            prefs.getBoolean(
                KEY_FOREGROUND_CENTER_VISIBLE,
                true,
            )
        set(value) {
            prefs.edit()
                .putBoolean(
                    KEY_FOREGROUND_CENTER_VISIBLE,
                    value,
                )
                .apply()
        }

    var foregroundEdgeFxVisible: Boolean
        get() =
            prefs.getBoolean(
                KEY_FOREGROUND_EDGE_FX_VISIBLE,
                true,
            )
        set(value) {
            prefs.edit()
                .putBoolean(
                    KEY_FOREGROUND_EDGE_FX_VISIBLE,
                    value,
                )
                .apply()
        }

    fun lastPresetFileOrNull(): File? =
        lastPresetPath
            ?.let(::File)
            ?.takeIf { it.isFile }

    companion object {
        private const val KEY_BACKGROUND_MODE = "background_mode"
        private const val KEY_AUTO_ENABLED = "auto_enabled"
        private const val KEY_LAST_PRESET_PATH = "last_preset_path"
        private const val KEY_FOREGROUND_SAMPLE = "foreground_sample"
        private const val KEY_BACKGROUND_VISIBLE =
            "background_visible"
        private const val KEY_FOREGROUND_CENTER_VISIBLE =
            "foreground_center_visible"
        private const val KEY_FOREGROUND_EDGE_FX_VISIBLE =
            "foreground_edge_fx_visible"
    }
}
