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

    /** V142 (faric 23.10 01:09): picture underneath, the text going over it — chosen by ticks. */
    MIX("Разом — галочками (картинка + текст поверх)"),
    ;

    /** Does this mode ever show the story (then the export asks its language). */
    val usesStory: Boolean get() = this == AUTO || this == STORY || this == MIX

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
    /** V137: the song's words (lines centred) rather than a story (justified paragraphs). */
    val lyrics: Boolean = false,
    /** V142: with TEXT — the picture is drawn underneath (dimmed), the text goes over it. */
    val pictureUnder: Boolean = false,
)

/**
 * V148 (faric 23.10 02:54: «текст не дуже видно… він бере прозорість плитки 50 %… поставити йому
 * окремо, скажімо, 70 %»): the text in the frame has its own look, apart from the tile's see-through.
 */
data class CoverTextSettings(
    /** 10–100 %: how solid the letters are (not tied to the tile's «Розкладка» transparency). */
    val opacity: Int = 100,
    /** 0–90 %: the dark veil under the text (over the picture in «Разом»). */
    val veil: Int = 60,
    /** 70–160 %: letter size, 100 = a bit bigger than the song title. */
    val size: Int = 100,
) {
    fun encode(): String = "$opacity,$veil,$size"

    companion object {
        fun decode(text: String?): CoverTextSettings {
            val p = text?.split(',')?.map { it.trim().toIntOrNull() } ?: return CoverTextSettings()
            if (p.size != 3 || p.any { it == null }) return CoverTextSettings()
            return CoverTextSettings(p[0]!!.coerceIn(10, 100), p[1]!!.coerceIn(0, 90), p[2]!!.coerceIn(70, 160))
        }
    }
}

/** V142: the ticks of «Разом»: what goes into the frame together. */
data class CoverMix(
    val picture: Boolean = true,
    val story: Boolean = true,
    val lyrics: Boolean = false,
) {
    fun encode(): String = listOf(picture, story, lyrics).joinToString("") { if (it) "1" else "0" }

    companion object {
        fun decode(text: String?): CoverMix =
            text?.takeIf { it.length == 3 && it.all { c -> c == '0' || c == '1' } }
                ?.let { CoverMix(it[0] == '1', it[1] == '1', it[2] == '1') }
                ?: CoverMix()
    }
}

object CoverBoxContent {
    fun resolve(
        mode: CoverBoxMode,
        hasPicture: Boolean,
        story: String?,
        lyrics: String?,
        mix: CoverMix = CoverMix(),
    ): CoverBoxChoice {
        val storyText = story?.trim()?.takeIf { it.isNotEmpty() }
        val lyricsText = lyrics?.trim()?.takeIf { it.isNotEmpty() }
        return when (mode) {
            CoverBoxMode.AUTO ->
                when {
                    hasPicture -> CoverBoxChoice(CoverBoxShow.PICTURE)
                    storyText != null -> CoverBoxChoice(CoverBoxShow.TEXT, storyText)
                    lyricsText != null -> CoverBoxChoice(CoverBoxShow.TEXT, lyricsText, lyrics = true)
                    else -> CoverBoxChoice(CoverBoxShow.HIDDEN)
                }
            CoverBoxMode.PICTURE -> CoverBoxChoice(if (hasPicture) CoverBoxShow.PICTURE else CoverBoxShow.HIDDEN)
            CoverBoxMode.PICTURE_PLACEHOLDER -> CoverBoxChoice(if (hasPicture) CoverBoxShow.PICTURE else CoverBoxShow.PLACEHOLDER)
            // V143 (запис faric 23.10 01:49: вибрав «Слова пісні» — і рамка зникла вся, бо слів у пісні
            // нема): a text chosen by hand that the song does not have → the picture (or ♪) stays,
            // so the frame, its buttons and the edge do not vanish.
            CoverBoxMode.STORY -> storyText?.let { CoverBoxChoice(CoverBoxShow.TEXT, it) } ?: stayVisible(hasPicture)
            CoverBoxMode.LYRICS -> lyricsText?.let { CoverBoxChoice(CoverBoxShow.TEXT, it, lyrics = true) } ?: stayVisible(hasPicture)
            CoverBoxMode.MIX -> {
                // Story first, then the words — whichever is ticked and is in the song.
                val (text, isLyrics) =
                    when {
                        mix.story && storyText != null -> storyText to false
                        mix.lyrics && lyricsText != null -> lyricsText to true
                        else -> null to false
                    }
                val picture = mix.picture && hasPicture
                when {
                    text != null -> CoverBoxChoice(CoverBoxShow.TEXT, text, isLyrics, pictureUnder = picture)
                    picture -> CoverBoxChoice(CoverBoxShow.PICTURE)
                    else -> CoverBoxChoice(CoverBoxShow.HIDDEN)
                }
            }
        }
    }

    /**
     * The credits crawl: the text starts just under the frame and leaves over its top exactly at the
     * song's end — the same on screen and in the MP4 (both give the song position as [fraction]).
     * Returns the text's top relative to the frame's inner top.
     */
    private fun stayVisible(hasPicture: Boolean) = CoverBoxChoice(if (hasPicture) CoverBoxShow.PICTURE else CoverBoxShow.PLACEHOLDER)

    /**
     * V137 / V138 (faric 22.10 20:24 і 22:50: «габарити плитки не відповідають тим, що виставили в
     * утилітах»): the frame is drawn on the SCREEN exactly where and as big as «Розкладка» puts it —
     * only cut to the screen (2 % margins), never wider. Text gets a readable minimum (60 % of the width,
     * a quarter of the width high) only when the frame is smaller than that.
     */
    fun framePanel(
        frameOnScreen: LayoutBox,
        screenW: Float,
        screenH: Float,
        text: Boolean,
    ): LayoutBox {
        val minLeft = screenW * 0.02f
        val maxRight = screenW * 0.98f
        val minTop = screenH * 0.02f
        val maxBottom = screenH * 0.98f
        var left = frameOnScreen.left.coerceAtLeast(minLeft)
        var right = frameOnScreen.right.coerceAtMost(maxRight)
        var top = frameOnScreen.top.coerceAtLeast(minTop)
        var bottom = frameOnScreen.bottom.coerceAtMost(maxBottom)
        if (text) {
            val minW = screenW * 0.6f
            val minH = screenW * 0.25f
            if (right - left < minW) {
                val cx = ((left + right) / 2f).coerceIn(minLeft + minW / 2f, maxRight - minW / 2f)
                left = cx - minW / 2f
                right = cx + minW / 2f
            }
            if (bottom - top < minH) {
                val cy = ((top + bottom) / 2f).coerceIn(minTop + minH / 2f, maxBottom - minH / 2f)
                top = cy - minH / 2f
                bottom = cy + minH / 2f
            }
        }
        if (right < left) right = left
        if (bottom < top) bottom = top
        return LayoutBox(left, top, right, bottom)
    }

    /** Paragraphs of a story: split on blank lines; «— Про альбом —» is a heading (centred). */
    fun paragraphs(text: String): List<String> = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }

    fun isHeading(paragraph: String): Boolean = paragraph.startsWith("— ") && paragraph.endsWith(" —") && paragraph.length <= 60

    /**
     * V148 (faric 23.10 02:54: «інколи текст ривками прокручується»): the live position arrives 4× a
     * second and the guess between arrivals can run a hair ahead — the next arrival then pulled the
     * text back a few pixels. A small step back is ignored (the text waits); a big one (seek) is taken.
     */
    fun steadyFraction(
        shown: Float,
        target: Float,
        maxBack: Float,
    ): Float = if (target < shown && shown - target <= maxBack) shown else target

    /**
     * V150 (запис faric 23.10 03:38: «іде плавно, потім хоп — зупинився, далі йде»; кадри: зупинки по
     * ~0,2 с кожні 2–3 с): the song position also comes in with old snapshots (a screen refresh re-sends
     * the last one), so the clock was pulled back and the text waited. Now a new position only nudges
     * the running clock (15 % of the difference); a big difference (seek, new song) or a stop/start
     * resets it. Returns the clock's position for «now».
     */
    fun smoothClock(
        predictedMs: Long,
        sampleMs: Long,
        runningBefore: Boolean,
        runningNow: Boolean,
    ): Long {
        val error = sampleMs - predictedMs
        return if (!runningBefore || !runningNow || kotlin.math.abs(error) > 1_500L) sampleMs else predictedMs + (error * 0.15f).toLong()
    }

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
