package com.saney.musicvisualizer.board

/**
 * Absolute music response for ONE GF object, never an additive offset to
 * the parent. The parent controls supply the initial values and can broadcast
 * replacements to every child. A child can then be adjusted independently.
 *
 * Values of zero disable the matching audio-driven motion (not the artist's
 * explicit manual transform or intentionally time-based frame auto-spin).
 */
data class BoardLayerReactionTuning(
    val rotationSwayDegrees: Float = 0f,
    val stereoShiftFraction: Float = 0f,
    val bassFloatFraction: Float = 0f,
) {
    fun sanitized(): BoardLayerReactionTuning = copy(
        rotationSwayDegrees = rotationSwayDegrees.takeIf { it.isFinite() }?.coerceIn(0f, 6f) ?: 0f,
        stereoShiftFraction = stereoShiftFraction.takeIf { it.isFinite() }?.coerceIn(0f, 0.10f) ?: 0f,
        bassFloatFraction = bassFloatFraction.takeIf { it.isFinite() }?.coerceIn(0f, 0.06f) ?: 0f,
    )

    fun apply(base: BoardLayerReaction): BoardLayerReaction = sanitized().let { value ->
        base.copy(
            // Legacy layer presets also contain built-in musical beat pulses.
            // Without clearing those coefficients, 0/0/0 would still react
            // despite the user explicitly switching all reactions off.
            amplitudeScale = 0f,
            bassScale = 0f,
            midScale = 0f,
            highScale = 0f,
            beatScale = 0f,
            amplitudeAlpha = 0f,
            bassAlpha = 0f,
            midAlpha = 0f,
            highAlpha = 0f,
            beatAlpha = 0f,
            beatRotationDeg = 0f,
            beatTranslateYFraction = 0f,
            rotationSwayDegrees = value.rotationSwayDegrees,
            stereoShiftFraction = value.stereoShiftFraction,
            bassFloatFraction = value.bassFloatFraction,
        )
    }

    companion object {
        fun inherited(group: BoardGroupReaction): BoardLayerReactionTuning =
            group.sanitized().let {
                BoardLayerReactionTuning(
                    rotationSwayDegrees = it.rotationSwayDegrees,
                    stereoShiftFraction = it.stereoShiftFraction,
                    bassFloatFraction = it.bassFloatFraction,
                )
            }
    }
}
