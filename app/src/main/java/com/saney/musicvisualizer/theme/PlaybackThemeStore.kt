package com.saney.musicvisualizer.theme

import android.content.Context

class PlaybackThemeStore(context: Context) {
    private val prefs =
        context.getSharedPreferences(
            "faric_playback_theme",
            Context.MODE_PRIVATE,
        )

    var selectedThemeId: PlaybackThemeId
        get() {
            val raw =
                prefs.getString(
                    KEY_SELECTED_THEME,
                    PlaybackThemeId.VISUALIZER.name,
                )
                    ?: PlaybackThemeId.VISUALIZER.name

            return runCatching {
                PlaybackThemeId.valueOf(raw)
            }.getOrDefault(PlaybackThemeId.VISUALIZER)
        }
        set(value) {
            prefs.edit()
                .putString(
                    KEY_SELECTED_THEME,
                    value.name,
                )
                .apply()
        }

    companion object {
        private const val KEY_SELECTED_THEME =
            "selected_theme"
    }
}
