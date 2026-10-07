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

    var autoSwitchSeconds: Int
        get() =
            prefs.getInt(
                KEY_AUTO_SWITCH_SECONDS,
                10,
            )
                .takeIf {
                    it in
                        AUTO_SWITCH_OPTIONS
                }
                ?: 10
        set(value) {
            val safe =
                value.takeIf {
                    it in
                        AUTO_SWITCH_OPTIONS
                }
                    ?: 10

            prefs.edit()
                .putInt(
                    KEY_AUTO_SWITCH_SECONDS,
                    safe,
                )
                .apply()
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

    fun foregroundTuning():
        ProjectMForegroundTuning =
        ProjectMForegroundTuning(
            centerScale =
                prefs.getFloat(
                    KEY_FG_CENTER_SCALE,
                    1f,
                ),
            centerRotationDegrees =
                prefs.getFloat(
                    KEY_FG_CENTER_ROTATION,
                    0f,
                ),
            centerOpacity =
                prefs.getFloat(
                    KEY_FG_CENTER_OPACITY,
                    1f,
                ),
            centerBassGain =
                prefs.getFloat(
                    KEY_FG_CENTER_BASS,
                    1f,
                ),
            centerMidGain =
                prefs.getFloat(
                    KEY_FG_CENTER_MID,
                    1f,
                ),
            centerHighGain =
                prefs.getFloat(
                    KEY_FG_CENTER_HIGH,
                    1f,
                ),
            centerBeatGain =
                prefs.getFloat(
                    KEY_FG_CENTER_BEAT,
                    1f,
                ),
            edgeOpacity =
                prefs.getFloat(
                    KEY_FG_EDGE_OPACITY,
                    1f,
                ),
            edgeBassGain =
                prefs.getFloat(
                    KEY_FG_EDGE_BASS,
                    1f,
                ),
            edgeHighGain =
                prefs.getFloat(
                    KEY_FG_EDGE_HIGH,
                    1f,
                ),
            edgeBeatGain =
                prefs.getFloat(
                    KEY_FG_EDGE_BEAT,
                    1f,
                ),
        )
            .sanitized()

    fun saveForegroundTuning(
        tuning: ProjectMForegroundTuning,
    ) {
        val safe =
            tuning.sanitized()

        prefs.edit()
            .putFloat(
                KEY_FG_CENTER_SCALE,
                safe.centerScale,
            )
            .putFloat(
                KEY_FG_CENTER_ROTATION,
                safe.centerRotationDegrees,
            )
            .putFloat(
                KEY_FG_CENTER_OPACITY,
                safe.centerOpacity,
            )
            .putFloat(
                KEY_FG_CENTER_BASS,
                safe.centerBassGain,
            )
            .putFloat(
                KEY_FG_CENTER_MID,
                safe.centerMidGain,
            )
            .putFloat(
                KEY_FG_CENTER_HIGH,
                safe.centerHighGain,
            )
            .putFloat(
                KEY_FG_CENTER_BEAT,
                safe.centerBeatGain,
            )
            .putFloat(
                KEY_FG_EDGE_OPACITY,
                safe.edgeOpacity,
            )
            .putFloat(
                KEY_FG_EDGE_BASS,
                safe.edgeBassGain,
            )
            .putFloat(
                KEY_FG_EDGE_HIGH,
                safe.edgeHighGain,
            )
            .putFloat(
                KEY_FG_EDGE_BEAT,
                safe.edgeBeatGain,
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
        private const val KEY_AUTO_SWITCH_SECONDS =
            "auto_switch_seconds"
        private const val KEY_LAST_PRESET_PATH = "last_preset_path"
        private const val KEY_FOREGROUND_SAMPLE = "foreground_sample"
        private const val KEY_BACKGROUND_VISIBLE =
            "background_visible"
        private const val KEY_FOREGROUND_CENTER_VISIBLE =
            "foreground_center_visible"
        private const val KEY_FOREGROUND_EDGE_FX_VISIBLE =
            "foreground_edge_fx_visible"

        private const val KEY_FG_CENTER_SCALE =
            "fg_center_scale"
        private const val KEY_FG_CENTER_ROTATION =
            "fg_center_rotation"
        private const val KEY_FG_CENTER_OPACITY =
            "fg_center_opacity"
        private const val KEY_FG_CENTER_BASS =
            "fg_center_bass"
        private const val KEY_FG_CENTER_MID =
            "fg_center_mid"
        private const val KEY_FG_CENTER_HIGH =
            "fg_center_high"
        private const val KEY_FG_CENTER_BEAT =
            "fg_center_beat"
        private const val KEY_FG_EDGE_OPACITY =
            "fg_edge_opacity"
        private const val KEY_FG_EDGE_BASS =
            "fg_edge_bass"
        private const val KEY_FG_EDGE_HIGH =
            "fg_edge_high"
        private const val KEY_FG_EDGE_BEAT =
            "fg_edge_beat"

        val AUTO_SWITCH_OPTIONS =
            setOf(
                5,
                10,
                15,
            )
    }
}
