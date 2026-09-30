package com.saney.musicvisualizer.board

data class BoardGroupReaction(
    val rotationSwayDegrees: Float = DEFAULT_ROTATION_SWAY_DEGREES,
    val stereoShiftFraction: Float = DEFAULT_STEREO_SHIFT_FRACTION,
    val bassFloatFraction: Float = DEFAULT_BASS_FLOAT_FRACTION,
) {
    fun sanitized(): BoardGroupReaction =
        copy(
            rotationSwayDegrees =
                rotationSwayDegrees.coerceIn(
                    0f,
                    MAX_ROTATION_SWAY_DEGREES,
                ),
            stereoShiftFraction =
                stereoShiftFraction.coerceIn(
                    0f,
                    MAX_STEREO_SHIFT_FRACTION,
                ),
            bassFloatFraction =
                bassFloatFraction.coerceIn(
                    0f,
                    MAX_BASS_FLOAT_FRACTION,
                ),
        )

    companion object {
        const val DEFAULT_ROTATION_SWAY_DEGREES = 2.4f
        const val DEFAULT_STEREO_SHIFT_FRACTION = 0.035f
        const val DEFAULT_BASS_FLOAT_FRACTION = 0.018f

        const val MAX_ROTATION_SWAY_DEGREES = 6f
        const val MAX_STEREO_SHIFT_FRACTION = 0.10f
        const val MAX_BASS_FLOAT_FRACTION = 0.06f

        fun default(): BoardGroupReaction =
            BoardGroupReaction()
    }
}
