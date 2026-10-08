package com.saney.musicvisualizer.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.saney.musicvisualizer.board.GraphicFigureCatalog
import com.saney.musicvisualizer.board.UserHeroPack
import org.junit.Test

class PlaybackThemeRegistryTest {
    @Test
    fun allThemeIdsAreUnique() {
        val ids = PlaybackThemeRegistry.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun importedHeroNamesAreUniqueAndAllRegistered() {
        val heroes = UserHeroPack.heroes
        assertEquals(10, heroes.size)
        assertEquals(10, heroes.map { it.id }.distinct().size)
        assertEquals(10, heroes.map { it.sourceFilename }.distinct().size)
        assertTrue(GraphicFigureCatalog.ids.containsAll(heroes.map { it.id }))
        assertTrue(PlaybackThemeRegistry.all.map { it.id }.containsAll(heroes.map { it.id }))
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
