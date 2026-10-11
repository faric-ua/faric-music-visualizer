package com.saney.musicvisualizer

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Global user preference for Android STATUS and NAVIGATION bars.
 *
 * Default is ALWAYS VISIBLE. A user may opt back into the former immersive
 * presentation through PulseDeck tools; old stored profiles get the new,
 * readable default without altering their Hero/Board/Export data.
 *
 * Changes apply only to the Activity window. This never modifies the locked
 * PulseDeck center calibration or resizes/restarts the live renderer.
 */
object PulseDeckSystemBars {
    internal const val PREFS_NAME = "faric.preferences"
    internal const val KEY_ALWAYS_VISIBLE = "faric.system_bars_always_visible"

    fun alwaysVisible(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ALWAYS_VISIBLE, true)

    fun setAlwaysVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ALWAYS_VISIBLE, visible)
            .apply()
    }

    fun apply(activity: Activity) {
        runCatching {
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                if (alwaysVisible(activity)) {
                    show(WindowInsetsCompat.Type.systemBars())
                } else {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            window.decorView.requestApplyInsets()
        }.onFailure { error ->
            Log.e("PulseDeck-system-bars", "Could not update Android system bars", error)
        }
    }
}
