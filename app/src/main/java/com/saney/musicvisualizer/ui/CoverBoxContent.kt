package com.saney.musicvisualizer.ui

/**
 * V135 (faric 22.10 05:44 / 17:16): what the frame at the top («Обкладинка вгорі», V114) shows.
 * «Авто»: картинка → історія пісні → слова пісні → нічого нема — рамка ховається.
 * Pure Kotlin — tested in CoverBoxContentTest.
 */
enum class CoverBoxMode(
    val label: String,
) {
    AUTO("Авто: картинка → історія → слова → сховати"),
    PICTURE("Лише картинка (нема — сховати рамку)"),
    PICTURE_PLACEHOLDER("Лише картинка (нема — заглушка ♪)"),
    STORY("Історія пісні (титри)"),
    LYRICS("Слова пісні (титри)"),
    ;

    /** Does this mode ever show the story (then the export asks its language). */
    val usesStory: Boolean get() = this == AUTO || this == STORY

    companion object {
        fun fromIndex(index: Int): CoverBoxMode = entries.getOrNull(index) ?: AUTO

        /** V114 kept 0 = «сховати», 1 = «заглушка ♪». */
        fun fromOldFallback(old: Int): CoverBoxMode = if (old == 1) PICTURE_PLACEHOLDER else PICTURE
    }
}

enum class CoverBoxShow { PICTURE, PLACEHOLDER, TEXT, HIDDEN }

data class CoverBoxChoice(
    val show: CoverBoxShow,
    val text: String? = null,
)

object CoverBoxContent {
    fun resolve(
        mode: CoverBoxMode,
        hasPicture: Boolean,
        story: String?,
        lyrics: String?,
    ): CoverBoxChoice {
        val storyText = story?.trim()?.takeIf { it.isNotEmpty() }
        val lyricsText = lyrics?.trim()?.takeIf { it.isNotEmpty() }
        return when (mode) {
            CoverBoxMode.AUTO ->
                when {
                    hasPicture -> CoverBoxChoice(CoverBoxShow.PICTURE)
                    storyText != null -> CoverBoxChoice(CoverBoxShow.TEXT, storyText)
                    lyricsText != null -> CoverBoxChoice(CoverBoxShow.TEXT, lyricsText)
                    else -> CoverBoxChoice(CoverBoxShow.HIDDEN)
                }
            CoverBoxMode.PICTURE -> CoverBoxChoice(if (hasPicture) CoverBoxShow.PICTURE else CoverBoxShow.HIDDEN)
            CoverBoxMode.PICTURE_PLACEHOLDER -> CoverBoxChoice(if (hasPicture) CoverBoxShow.PICTURE else CoverBoxShow.PLACEHOLDER)
            CoverBoxMode.STORY -> storyText?.let { CoverBoxChoice(CoverBoxShow.TEXT, it) } ?: CoverBoxChoice(CoverBoxShow.HIDDEN)
            CoverBoxMode.LYRICS -> lyricsText?.let { CoverBoxChoice(CoverBoxShow.TEXT, it) } ?: CoverBoxChoice(CoverBoxShow.HIDDEN)
        }
    }

    /**
     * The credits crawl: the text starts just under the frame and leaves over its top exactly at the
     * song's end — the same on screen and in the MP4 (both give the song position as [fraction]).
     * Returns the text's top relative to the frame's inner top.
     */
    fun crawlTop(
        fraction: Float,
        innerHeight: Float,
        textHeight: Float,
    ): Float = innerHeight - fraction.coerceIn(0f, 1f) * (innerHeight + textHeight)
}

/** V135 (faric 22.10 17:16): the story is written in Ukrainian and English — asked once per export. */
enum class StoryLanguage(
    val code: String,
    val label: String,
    val albumHeader: String,
) {
    UK("uk", "Українська", "Про альбом"),
    EN("en", "English", "About the album"),
    ;

    companion object {
        fun fromCode(code: String?): StoryLanguage = entries.firstOrNull { it.code == code } ?: UK
    }
}

/**
 * The story tags written by Audio Converter (index.json «faric-story-index-v1», 22.10):
 * FARIC_STORY_UK / _EN — the track; FARIC_ALBUM_STORY_UK / _EN — the album. [lyrics] — the song's
 * words (ID3 USLT / Vorbis LYRICS).
 */
data class SongStoryTags(
    val trackUk: String? = null,
    val trackEn: String? = null,
    val albumUk: String? = null,
    val albumEn: String? = null,
    val lyrics: String? = null,
) {
    val hasStory: Boolean get() = listOf(trackUk, trackEn, albumUk, albumEn).any { !it.isNullOrBlank() }

    /** Both languages present — only then is there something to ask. */
    val hasBothLanguages: Boolean
        get() = (!trackUk.isNullOrBlank() || !albumUk.isNullOrBlank()) && (!trackEn.isNullOrBlank() || !albumEn.isNullOrBlank())

    companion object {
        val EMPTY = SongStoryTags()

        /** From a tag map (keys upper-case, as [com.saney.musicvisualizer.export.StoryTagParser] gives them). */
        fun fromTags(tags: Map<String, String>): SongStoryTags =
            SongStoryTags(
                trackUk = tags["FARIC_STORY_UK"],
                trackEn = tags["FARIC_STORY_EN"],
                albumUk = tags["FARIC_ALBUM_STORY_UK"],
                albumEn = tags["FARIC_ALBUM_STORY_EN"],
                lyrics = tags["LYRICS"] ?: tags["UNSYNCEDLYRICS"],
            )
    }
}

object StoryText {
    /** Track story, then «Про альбом» and the album story. A missing language falls back to the other. */
    fun compose(
        tags: SongStoryTags,
        language: StoryLanguage,
    ): String? {
        fun pick(
            uk: String?,
            en: String?,
        ): String? {
            val first = if (language == StoryLanguage.UK) uk else en
            val second = if (language == StoryLanguage.UK) en else uk
            return first?.trim()?.takeIf { it.isNotEmpty() } ?: second?.trim()?.takeIf { it.isNotEmpty() }
        }
        val track = pick(tags.trackUk, tags.trackEn)
        val album = pick(tags.albumUk, tags.albumEn)
        return when {
            track != null && album != null -> track + "\n\n— " + language.albumHeader + " —\n\n" + album
            else -> track ?: album
        }
    }
}

/** V135 (faric 22.10 17:17–17:18): neon light from under the frame, pulsing with one part of the music. */
enum class CoverGlowBand(
    val label: String,
) {
    OFF("Вимкнено (лише тонкий край)"),
    BASS("Бас"),
    MID("Середні частоти"),
    HIGH("Високі частоти"),
    BEAT("Удари (такт)"),
    ;

    companion object {
        fun fromIndex(index: Int): CoverGlowBand = entries.getOrNull(index) ?: BASS
    }
}
