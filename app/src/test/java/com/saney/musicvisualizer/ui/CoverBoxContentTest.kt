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
        // V143: chosen words the song has not → the picture stays (or ♪), the frame does not vanish
        assertEquals(CoverBoxShow.PICTURE, CoverBoxContent.resolve(CoverBoxMode.LYRICS, true, "s", null).show)
        assertEquals(CoverBoxShow.PLACEHOLDER, CoverBoxContent.resolve(CoverBoxMode.STORY, false, null, "l").show)
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

    @Test fun bothLanguagesUkrainianThenEnglish() {
        val tags = SongStoryTags(trackUk = "Трек", trackEn = "Track", albumUk = "Альбом", albumEn = "Album")
        assertEquals(
            "Трек\n\n— Про альбом —\n\nАльбом\n\n— English —\n\nTrack\n\n— About the album —\n\nAlbum",
            StoryText.compose(tags, StoryLanguage.BOTH),
        )
        assertTrue(CoverBoxContent.isHeading(StoryText.ENGLISH_HEADER))
        // only one language in the tags — shown once, not twice
        assertEquals("Трек", StoryText.compose(SongStoryTags(trackUk = "Трек"), StoryLanguage.BOTH))
        assertEquals("Track", StoryText.compose(SongStoryTags(trackEn = "Track"), StoryLanguage.BOTH))
        assertEquals(StoryLanguage.BOTH, StoryLanguage.fromCode("both"))
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

    @Test fun framePanelIsTheFrameCutToTheScreen() {
        // the frame as «Розкладка» puts it — kept as is
        val kept = CoverBoxContent.framePanel(LayoutBox(80f, 250f, 1000f, 1000f), 1080f, 2340f, text = true)
        assertEquals(LayoutBox(80f, 250f, 1000f, 1000f), kept)
        // enlarged past the screen — cut to the screen with 2 % margins
        val cut = CoverBoxContent.framePanel(LayoutBox(-1500f, 250f, 2580f, 1000f), 1080f, 2340f, text = false)
        assertEquals(21.6f, cut.left, 1e-3f)
        assertEquals(1058.4f, cut.right, 1e-3f)
        // a small frame: a picture stays small, text gets a readable minimum
        val square = LayoutBox(432f, 320f, 648f, 536f)
        assertEquals(square, CoverBoxContent.framePanel(square, 1080f, 2340f, text = false))
        val small = CoverBoxContent.framePanel(square, 1080f, 2340f, text = true)
        assertEquals(648f, small.right - small.left, 1e-3f)
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

    @Test fun mixPutsTextOverThePicture() {
        val m = CoverBoxMode.MIX
        assertEquals(CoverBoxChoice(CoverBoxShow.TEXT, "s", false, pictureUnder = true), CoverBoxContent.resolve(m, true, "s", "l"))
        // story not ticked → the words over the picture
        assertEquals(CoverBoxChoice(CoverBoxShow.TEXT, "l", true, pictureUnder = true), CoverBoxContent.resolve(m, true, "s", "l", CoverMix(true, false, true)))
        // no text in the song → just the picture; nothing → hidden
        assertEquals(CoverBoxShow.PICTURE, CoverBoxContent.resolve(m, true, null, null).show)
        assertEquals(CoverBoxShow.HIDDEN, CoverBoxContent.resolve(m, false, null, null).show)
        // picture not ticked → text alone
        assertEquals(CoverBoxChoice(CoverBoxShow.TEXT, "s"), CoverBoxContent.resolve(m, true, "s", null, CoverMix(false, true, false)))
        assertEquals(CoverMix(true, false, true), CoverMix.decode(CoverMix(true, false, true).encode()))
        assertEquals(CoverMix(), CoverMix.decode("x"))
    }

    @Test fun textSettingsAndSteadyCrawl() {
        val t = CoverTextSettings(70, 40, 120)
        assertEquals(t, CoverTextSettings.decode(t.encode()))
        assertEquals(CoverTextSettings(10, 90, 160), CoverTextSettings.decode("0,99,999"))
        assertEquals(CoverTextSettings(), CoverTextSettings.decode("bad"))
        assertEquals(0.5f, CoverBoxContent.steadyFraction(0.5f, 0.499f, 0.004f), 1e-6f)
        assertEquals(0.2f, CoverBoxContent.steadyFraction(0.5f, 0.2f, 0.004f), 1e-6f)
        assertEquals(0.51f, CoverBoxContent.steadyFraction(0.5f, 0.51f, 0.004f), 1e-6f)
    }

    @Test fun clockOnlyNudgedByPositionUpdates() {
        // an old position (250 ms behind) barely moves the running clock
        assertEquals(10_000L - 37L, CoverBoxContent.smoothClock(10_000L, 9_750L, true, true))
        // a seek resets
        assertEquals(60_000L, CoverBoxContent.smoothClock(10_000L, 60_000L, true, true))
        // start / stop take the real position
        assertEquals(9_750L, CoverBoxContent.smoothClock(10_000L, 9_750L, false, true))
        assertEquals(9_750L, CoverBoxContent.smoothClock(10_000L, 9_750L, true, false))
    }
}
