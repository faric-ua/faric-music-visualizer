package com.saney.musicvisualizer.board

import android.content.Context
import com.saney.musicvisualizer.theme.PlaybackThemeId

/**
 * Stored values are absolute child values, not additive differences.
 *
 * Backwards compatible: pre-v0.19.70 children had no keys and displayed
 * misleading zeroes while inheriting motion from the physical GF group.
 * Missing fields now inherit the saved parent value without rewriting data.
 * Explicit saved zero stays zero until the user changes "Усе GF" again.
 */
class BoardLayerReactionTuningStore(context: Context) {
    private val prefs = context.getSharedPreferences(
        "faric_board_layer_reaction_v1", Context.MODE_PRIVATE,
    )

    private val layers = listOf(
        BoardLayerId.FRAME, BoardLayerId.FX, BoardLayerId.CREATURE, BoardLayerId.WORDMARK,
    )

    fun load(
        theme: PlaybackThemeId,
        layer: BoardLayerId,
        parent: BoardGroupReaction,
    ): BoardLayerReactionTuning {
        val key = theme.name + "_" + layer.name + "_"
        val inherited = BoardLayerReactionTuning.inherited(parent)
        return BoardLayerReactionTuning(
            rotationSwayDegrees = prefs.getFloat(key + "rotation", inherited.rotationSwayDegrees),
            stereoShiftFraction = prefs.getFloat(key + "stereo", inherited.stereoShiftFraction),
            bassFloatFraction = prefs.getFloat(key + "bass", inherited.bassFloatFraction),
        ).sanitized()
    }

    fun loadAll(
        theme: PlaybackThemeId,
        parent: BoardGroupReaction,
    ): Map<BoardLayerId, BoardLayerReactionTuning> =
        layers.associateWith { layer -> load(theme, layer, parent) }

    fun save(theme: PlaybackThemeId, layer: BoardLayerId, tuning: BoardLayerReactionTuning) {
        require(layer in layers)
        val key = theme.name + "_" + layer.name + "_"
        val safe = tuning.sanitized()
        prefs.edit()
            .putFloat(key + "rotation", safe.rotationSwayDegrees)
            .putFloat(key + "stereo", safe.stereoShiftFraction)
            .putFloat(key + "bass", safe.bassFloatFraction)
            .apply()
    }

    /**
     * Every parent slider change is an atomic fan-out of all three values.
     * Previous per-child overrides are replaced, as explicitly requested.
     */
    fun applyParentToAll(theme: PlaybackThemeId, parent: BoardGroupReaction) {
        val inherited = BoardLayerReactionTuning.inherited(parent)
        prefs.edit().apply {
            for (layer in layers) {
                val key = theme.name + "_" + layer.name + "_"
                putFloat(key + "rotation", inherited.rotationSwayDegrees)
                putFloat(key + "stereo", inherited.stereoShiftFraction)
                putFloat(key + "bass", inherited.bassFloatFraction)
            }
        }.apply()
    }

    fun resetAll(theme: PlaybackThemeId, parent: BoardGroupReaction) =
        applyParentToAll(theme, parent)
}
