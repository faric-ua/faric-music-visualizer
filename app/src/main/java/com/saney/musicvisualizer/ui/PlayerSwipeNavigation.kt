package com.saney.musicvisualizer.ui

import kotlin.math.abs

/** Simple direction contract; the Activity decides which screen to open. */
object PlayerSwipeNavigation {
    enum class Destination { PROJECTM, LIBRARY }

    fun resolve(
        deltaX: Float,
        deltaY: Float,
        minimumDistance: Float,
        maximumVerticalDrift: Float,
    ): Destination? {
        if (!deltaX.isFinite() || !deltaY.isFinite() ||
            minimumDistance <= 0f || maximumVerticalDrift < 0f
        ) return null
        if (abs(deltaX) < minimumDistance ||
            abs(deltaY) > maximumVerticalDrift ||
            abs(deltaX) < abs(deltaY) * 1.65f
        ) return null
        return if (deltaX < 0f) Destination.PROJECTM else Destination.LIBRARY
    }
}
