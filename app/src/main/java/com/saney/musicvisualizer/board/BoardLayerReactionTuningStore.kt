package com.saney.musicvisualizer.board

import android.content.Context
import com.saney.musicvisualizer.theme.PlaybackThemeId

/** Layer- and theme-scoped preferences, never shared with global GF controls. */
class BoardLayerReactionTuningStore(context: Context) {
    private val prefs = context.getSharedPreferences(
        "faric_board_layer_reaction_v1", Context.MODE_PRIVATE,
    )

    fun load(theme: PlaybackThemeId, layer: BoardLayerId): BoardLayerReactionTuning {
        val key = theme.name + "_" + layer.name + "_"
        return BoardLayerReactionTuning(
            rotationSwayDegrees = prefs.getFloat(key + "rotation", 0f),
            stereoShiftFraction = prefs.getFloat(key + "stereo", 0f),
            bassFloatFraction = prefs.getFloat(key + "bass", 0f),
        ).sanitized()
    }

    fun loadAll(theme: PlaybackThemeId): Map<BoardLayerId, BoardLayerReactionTuning> =
        listOf(BoardLayerId.FRAME, BoardLayerId.FX, BoardLayerId.CREATURE, BoardLayerId.WORDMARK)
            .associateWith { layer -> load(theme, layer) }

    fun save(theme: PlaybackThemeId, layer: BoardLayerId, tuning: BoardLayerReactionTuning) {
        require(layer in setOf(
            BoardLayerId.FRAME, BoardLayerId.FX, BoardLayerId.CREATURE, BoardLayerId.WORDMARK,
        ))
        val key = theme.name + "_" + layer.name + "_"
        val safe = tuning.sanitized()
        prefs.edit()
            .putFloat(key + "rotation", safe.rotationSwayDegrees)
            .putFloat(key + "stereo", safe.stereoShiftFraction)
            .putFloat(key + "bass", safe.bassFloatFraction)
            .apply()
    }

    fun resetAll(theme: PlaybackThemeId) {
        for (layer in listOf(
            BoardLayerId.FRAME, BoardLayerId.FX, BoardLayerId.CREATURE, BoardLayerId.WORDMARK,
        )) save(theme, layer, BoardLayerReactionTuning())
    }
}
