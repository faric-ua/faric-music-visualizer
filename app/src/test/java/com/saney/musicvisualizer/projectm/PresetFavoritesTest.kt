package com.saney.musicvisualizer.projectm

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetFavoritesTest {
    @Test fun keysAndCount() {
        assertEquals("fav:martin/X Portal.milk", PresetFavorites.key("martin/X Portal.milk"))
        assertEquals(2, PresetFavorites.count(listOf("fav:a", "fav:b", "rating:c", "seed:poweramp:v1")))
    }

    @Test fun heartShowsTheState() {
        assertEquals("♥", PresetFavorites.icon(true))
        assertEquals("♡", PresetFavorites.icon(false))
    }

    @Test fun favoritesIsItsOwnGroup() {
        assertEquals(ProjectMBackgroundMode.LIKED.ordinal + 1, ProjectMBackgroundMode.FAVORITE.ordinal)
    }
}
