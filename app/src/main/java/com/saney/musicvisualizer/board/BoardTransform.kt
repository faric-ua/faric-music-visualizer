package com.saney.musicvisualizer.board

data class BoardTransform(
    val xFraction: Float = DEFAULT_X,
    val yFraction: Float = DEFAULT_Y,
    val sizeFraction: Float = DEFAULT_SIZE,
    val rotationDegrees: Float = 0f,
    val opacity: Float = 1f,
) {
    fun sanitized(): BoardTransform =
        copy(
            xFraction = xFraction.coerceIn(0.05f, 0.95f),
            yFraction = yFraction.coerceIn(0.08f, 0.92f),
            sizeFraction = sizeFraction.coerceIn(0.30f, 1.10f),
            rotationDegrees = rotationDegrees.coerceIn(-45f, 45f),
            opacity = opacity.coerceIn(0.20f, 1f),
        )

    companion object {
        const val DEFAULT_X = 0.50f
        const val DEFAULT_Y = 0.275f
        const val DEFAULT_SIZE = 0.66f

        fun default(): BoardTransform =
            BoardTransform()

        fun fitSafeArea(): BoardTransform =
            BoardTransform(
                xFraction = 0.50f,
                yFraction = 0.255f,
                sizeFraction = 0.60f,
                rotationDegrees = 0f,
                opacity = 1f,
            )
    }
}
