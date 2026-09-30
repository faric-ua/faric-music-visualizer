package com.saney.musicvisualizer.board

import android.content.Context
import com.saney.musicvisualizer.theme.PlaybackThemeId

class BoardGroupReactionStore(context: Context) {
    private val prefs =
        context.getSharedPreferences(
            "faric_board_group_reaction",
            Context.MODE_PRIVATE,
        )

    fun load(
        themeId: PlaybackThemeId,
    ): BoardGroupReaction {
        val prefix = themeId.name

        return BoardGroupReaction(
            rotationSwayDegrees =
                prefs.getFloat(
                    key(prefix, "rotation_sway"),
                    BoardGroupReaction.DEFAULT_ROTATION_SWAY_DEGREES,
                ),
            stereoShiftFraction =
                prefs.getFloat(
                    key(prefix, "stereo_shift"),
                    BoardGroupReaction.DEFAULT_STEREO_SHIFT_FRACTION,
                ),
            bassFloatFraction =
                prefs.getFloat(
                    key(prefix, "bass_float"),
                    BoardGroupReaction.DEFAULT_BASS_FLOAT_FRACTION,
                ),
        ).sanitized()
    }

    fun save(
        themeId: PlaybackThemeId,
        reaction: BoardGroupReaction,
    ) {
        val safe = reaction.sanitized()
        val prefix = themeId.name

        prefs.edit()
            .putFloat(
                key(prefix, "rotation_sway"),
                safe.rotationSwayDegrees,
            )
            .putFloat(
                key(prefix, "stereo_shift"),
                safe.stereoShiftFraction,
            )
            .putFloat(
                key(prefix, "bass_float"),
                safe.bassFloatFraction,
            )
            .apply()
    }

    fun reset(
        themeId: PlaybackThemeId,
    ): BoardGroupReaction {
        val value = BoardGroupReaction.default()
        save(themeId, value)
        return value
    }

    private fun key(
        prefix: String,
        name: String,
    ): String =
        "${prefix}_${name}"
}
