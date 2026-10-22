package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverBoxContentTest {
    @Test fun autoGoesPictureThenStoryThenLyricsThenHides() {
        val m = CoverBoxMode.AUTO
        assertEquals(CoverBoxShow.PICTURE, CoverBoxContent.resolve(m, true, "s", "l").show)
        assertEquals(CoverBoxChoice(CoverBoxShow.TEXT, "s"), CoverBoxContent.resolve(m, false, " s ", "l"))
        assertEquals(CoverBoxChoice(CoverBoxShow.TEXT, "l", lyrics = true), CoverBoxContent.resolve(m, false, "  ", "l"))
        assertEquals(CoverBoxShow.HIDDEN, CoverBoxContent.resolve(m, false, null, null).show)
    }

    @Test fun fixedModes() {
        assertEquals(CoverBoxShow.PLACEHOLDER, CoverBoxContent.resolve(CoverBoxMode.PICTURE_PLACEHOLDER, false, "s", null).show)
        assertEquals(CoverBoxShow.HIDDEN, CoverBoxContent.resolve(CoverBoxMode.PICTURE, false, "s", null).show)
        assertEquals(CoverBoxChoice(CoverBoxShow.TEXT, "s"), CoverBoxContent.resolve(CoverBoxMode.STORY, true, "s", "l"))
        assertEquals(CoverBoxShow.HIDDEN, CoverBoxContent.resolve(CoverBoxMode.LYRICS, true, "s", null).show)
        assertEquals(CoverBoxMode.PICTURE_PLACEHOLDER, CoverBoxMode.fromOldFallback(1))
        assertEquals(CoverBoxMode.PICTURE, CoverBoxMode.fromOldFallback(0))
        assertTrue(CoverBoxMode.AUTO.usesStory)
        assertFalse(CoverBoxMode.LYRICS.usesStory)
    }

    @Test fun crawlStartsUnderTheFrameAndEndsAboveIt() {
        assertEquals(200f, CoverBoxContent.crawlTop(0f, 200f, 1000f), 1e-3f)
        assertEquals(-1000f, CoverBoxContent.crawlTop(1f, 200f, 1000f), 1e-3f)
        assertEquals(-400f, CoverBoxContent.crawlTop(0.5f, 200f, 1000f), 1e-3f)
    }

    @Test fun storyComposesTrackThenAlbumAndFallsBackToOtherLanguage() {
        val tags = SongStoryTags(trackUk = "Трек", albumUk = "Альбом", albumEn = "Album")
        assertEquals("Трек\n\n— Про альбом —\n\nАльбом", StoryText.compose(tags, StoryLanguage.UK))
        assertEquals("Трек\n\n— About the album —\n\nAlbum", StoryText.compose(tags, StoryLanguage.EN))
        assertNull(StoryText.compose(SongStoryTags.EMPTY, StoryLanguage.UK))
        assertTrue(tags.hasBothLanguages)
        assertFalse(SongStoryTags(trackUk = "a").hasBothLanguages)
        val fromTags = SongStoryTags.fromTags(mapOf("FARIC_STORY_EN" to "x", "LYRICS" to "w"))
        assertEquals("x", fromTags.trackEn)
        assertEquals("w", fromTags.lyrics)
        assertEquals(StoryLanguage.EN, StoryLanguage.fromCode("en"))
        assertEquals(StoryLanguage.UK, StoryLanguage.fromCode(null))
    }

    @Test fun textPanelStaysOnScreenWhateverTheFrameScale() {
        // faric's cover enlarged ~5× — far wider than the screen.
        val panel = CoverBoxContent.textPanel(LayoutBox(-1500f, 250f, 2580f, 1000f), 1080f, 2340f)
        assertEquals(43.2f, panel.left, 1e-3f)
        assertEquals(1036.8f, panel.right, 1e-3f)
        assertEquals(250f, panel.top, 1e-3f)
        assertEquals(1000f, panel.bottom, 1e-3f)
        // a small frame still gets a readable height
        val small = CoverBoxContent.textPanel(LayoutBox(432f, 320f, 648f, 536f), 1080f, 2340f)
        assertEquals(270f, small.bottom - small.top, 1e-3f)
    }

    @Test fun paragraphsAndHeadings() {
        val text = StoryText.compose(SongStoryTags(trackUk = "Смисл. А\n\nЗвучання. Б", albumUk = "Альбом"), StoryLanguage.UK)!!
        val parts = CoverBoxContent.paragraphs(text)
        assertEquals(listOf("Смисл. А", "Звучання. Б", "— Про альбом —", "Альбом"), parts)
        assertTrue(CoverBoxContent.isHeading(parts[2]))
        assertFalse(CoverBoxContent.isHeading(parts[0]))
        assertTrue(CoverBoxContent.resolve(CoverBoxMode.LYRICS, true, null, "la").lyrics)
    }
}
