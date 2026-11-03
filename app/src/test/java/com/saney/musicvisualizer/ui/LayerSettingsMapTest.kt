package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerSettingsMapTest {
    @Test fun projectMLayerGathersItsThreePlaces() {
        assertEquals(
            listOf(LayerSetting.PROJECTM_FOREGROUND, LayerSetting.PROJECTM_BLEND, LayerSetting.PROJECTM_RIGHT_PAGE),
            LayerSettingsMap.forLayer("VISUALIZER"),
        )
    }

    @Test fun neonHeroSettingsOnlyWhileThatThemeIsOn() {
        assertTrue(LayerSetting.NEON_HERO !in LayerSettingsMap.forLayer("GRAPHIC_FIGURES"))
        val neon = LayerSettingsMap.forLayer("GRAPHIC_FIGURES", neonHeroTheme = true)
        assertEquals(LayerSetting.THEME, neon.first())
        assertTrue(LayerSetting.NEON_HERO in neon)
        assertTrue(LayerSetting.GF_BOARD in neon)
    }

    @Test fun layersWithoutSettingsGetNoGear() {
        assertTrue(LayerSettingsMap.forLayer("BIG_EQUALIZER").isEmpty())
        assertTrue(LayerSettingsMap.forLayer("PULSEDECK_LOCKED").isEmpty())
    }

    @Test fun frameHasItsOwnSettingsThenPlaceInEveryPosition() {
        val cover = LayerSettingsMap.forHudObject("cover_box")
        assertEquals(LayerSetting.COVER_CONTENT, cover.first())
        assertTrue(LayerSetting.COVER_TEXT_AND_EDGE in cover)
        assertEquals(
            listOf(LayerSetting.PLACE_PORTRAIT, LayerSetting.PLACE_LANDSCAPE, LayerSetting.PLACE_VIDEO),
            cover.takeLast(3),
        )
        assertEquals("cover", LayerSettingsMap.layoutGroupOf("cover_box"))
    }

    @Test fun fileInfoAndTitlePartsOpenTheirLayoutGroup() {
        assertEquals("info", LayerSettingsMap.layoutGroupOf("file_info"))
        assertEquals("track", LayerSettingsMap.layoutGroupOf("track_artist"))
        assertEquals(3, LayerSettingsMap.forHudObject("file_info").size)
    }

    @Test fun partWithoutAPlaceHasNoGear() {
        // 👍 👎 ⋮ sit inside the frame — they move with it.
        assertTrue(LayerSettingsMap.forHudObject("cover_menu").isEmpty())
    }

    @Test fun everyPositionHasItsProfile() {
        assertEquals(LayoutProfileId.VIDEO_9X16, LayerSettingsMap.profileOf(LayerSetting.PLACE_VIDEO))
        assertEquals(LayoutProfileId.LANDSCAPE, LayerSettingsMap.profileOf(LayerSetting.PLACE_LANDSCAPE))
        assertEquals(null, LayerSettingsMap.profileOf(LayerSetting.THEME))
    }
}
