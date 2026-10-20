package com.saney.musicvisualizer.ui

import kotlin.math.abs

/** Simple direction contract; the Activity decides which screen to open. */
object PlayerSwipeNavigation {
    enum class Destination { PROJECTM, LIBRARY, EXPORT }

    fun resolve(
        deltaX: Float,
        deltaY: Float,
        minimumDistance: Float,
        maximumVerticalDrift: Float,
    ): Destination? {
        if (!deltaX.isFinite() || !deltaY.isFinite() ||
            minimumDistance <= 0f || maximumVerticalDrift < 0f
        ) return null
        // Horizontal: ← projectM page, → library.
        if (abs(deltaX) >= minimumDistance &&
            abs(deltaY) <= maximumVerticalDrift &&
            abs(deltaX) >= abs(deltaY) * 1.65f
        ) {
            return if (deltaX < 0f) Destination.PROJECTM else Destination.LIBRARY
        }
        // Down («як стрілка завантаження», faric 20.10 05:36): Export Lab, even with every
        // HUD layer hidden. Longer than a horizontal swipe so a sloppy tap never triggers it.
        if (deltaY >= minimumDistance * 1.3f &&
            abs(deltaX) <= maximumVerticalDrift &&
            deltaY >= abs(deltaX) * 1.65f
        ) {
            return Destination.EXPORT
        }
        return null
    }
}
