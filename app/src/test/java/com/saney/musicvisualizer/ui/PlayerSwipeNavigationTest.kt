package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSwipeNavigationTest {
    @Test fun rightToLeftOpensProjectM() {
        assertEquals(PlayerSwipeNavigation.Destination.PROJECTM,
            PlayerSwipeNavigation.resolve(-170f, 12f, 100f, 65f))
    }
    @Test fun leftToRightOpensLibrary() {
        assertEquals(PlayerSwipeNavigation.Destination.LIBRARY,
            PlayerSwipeNavigation.resolve(160f, 10f, 100f, 65f))
    }
    @Test fun verticalOrSmallMovementIsIgnored() {
        assertNull(PlayerSwipeNavigation.resolve(90f, 10f, 100f, 65f))
        assertNull(PlayerSwipeNavigation.resolve(155f, 90f, 100f, 65f))
        assertNull(PlayerSwipeNavigation.resolve(10f, 110f, 100f, 65f)) // too short for «down»
        assertNull(PlayerSwipeNavigation.resolve(5f, -200f, 100f, 65f)) // up does nothing
        assertNull(PlayerSwipeNavigation.resolve(0f, 0f, 100f, 65f))
        assertNull(PlayerSwipeNavigation.resolve(Float.NaN, 0f, 100f, 65f))
    }
    @Test fun downOpensExportLab() {
        assertEquals(PlayerSwipeNavigation.Destination.EXPORT,
            PlayerSwipeNavigation.resolve(12f, 180f, 100f, 65f))
        assertNull(PlayerSwipeNavigation.resolve(90f, 180f, 100f, 65f)) // too slanted
    }
}
