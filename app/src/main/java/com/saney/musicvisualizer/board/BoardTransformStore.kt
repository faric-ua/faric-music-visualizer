package com.saney.musicvisualizer.board

import android.content.Context
import com.saney.musicvisualizer.theme.PlaybackThemeId

class BoardTransformStore(context: Context) {
    private val prefs =
        context.getSharedPreferences(
            "faric_board_transform",
            Context.MODE_PRIVATE,
        )

    fun load(themeId: PlaybackThemeId): BoardTransform {
        val prefix = themeId.name

        return BoardTransform(
            xFraction =
                prefs.getFloat(
                    key(prefix, "x"),
                    BoardTransform.DEFAULT_X,
                ),
            yFraction =
                prefs.getFloat(
                    key(prefix, "y"),
                    BoardTransform.DEFAULT_Y,
                ),
            sizeFraction =
                prefs.getFloat(
                    key(prefix, "size"),
                    BoardTransform.DEFAULT_SIZE,
                ),
            rotationDegrees =
                prefs.getFloat(
                    key(prefix, "rotation"),
                    0f,
                ),
            opacity =
                prefs.getFloat(
                    key(prefix, "opacity"),
                    1f,
                ),
        ).sanitized()
    }

    fun save(
        themeId: PlaybackThemeId,
        transform: BoardTransform,
    ) {
        val safe = transform.sanitized()
        val prefix = themeId.name

        prefs.edit()
            .putFloat(key(prefix, "x"), safe.xFraction)
            .putFloat(key(prefix, "y"), safe.yFraction)
            .putFloat(key(prefix, "size"), safe.sizeFraction)
            .putFloat(key(prefix, "rotation"), safe.rotationDegrees)
            .putFloat(key(prefix, "opacity"), safe.opacity)
            .apply()
    }

    fun reset(themeId: PlaybackThemeId): BoardTransform {
        val value = BoardTransform.default()
        save(themeId, value)
        return value
    }

    fun fitSafeArea(themeId: PlaybackThemeId): BoardTransform {
        val value = BoardTransform.fitSafeArea()
        save(themeId, value)
        return value
    }

    private fun key(
        prefix: String,
        name: String,
    ): String =
        "${prefix}_${name}"
}
