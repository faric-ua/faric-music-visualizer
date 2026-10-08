package com.saney.musicvisualizer.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackThemeRegistryTest {
    @Test
    fun allThemeIdsAreUnique() {
        val ids = PlaybackThemeRegistry.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun allLiveHeroThemesExistInCatalog() {
        val implemented = setOf(
            PlaybackThemeId.CYBER_SHARK,
            PlaybackThemeId.CYBER_PANTHER,
            PlaybackThemeId.NEON_EMBLEM,
            PlaybackThemeId.ENERGY_CORE,
            PlaybackThemeId.ORBITAL_CROWN,
            PlaybackThemeId.STAR_SEED,
            PlaybackThemeId.WAVE_IDOL,
            PlaybackThemeId.VINYL,
            PlaybackThemeId.CASSETTE,
            PlaybackThemeId.VISUALIZER,
        )
        assertTrue(PlaybackThemeRegistry.all.map { it.id }.containsAll(implemented))
    }
}
