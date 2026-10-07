package com.saney.musicvisualizer.theme

enum class PlaybackThemeId {
    VISUALIZER,
    CYBER_SHARK,
    CYBER_PANTHER,
    PORTRAIT_HALO,
    VINYL,
    CASSETTE,
    GLASS_CORE,
    NEON_EMBLEM,
    ENERGY_CORE,
    ORBITAL_CROWN,
    STAR_SEED,
    WAVE_IDOL,
    POSTER,
}

enum class ThemeFamily {
    REACTIVE,
    RETRO,
    HERO,
    TYPOGRAPHY,
}

enum class ThemeCapability {
    COVER_ART,
    CUSTOM_LOGO,
    BACKGROUND_IMAGE,
    BACKGROUND_VIDEO,
    PROJECTM_BACKGROUND,
    TRACK_METADATA,
    TIMER,
    WAVEFORM,
    REACTIVE_BANDS,
}

data class PlaybackThemeSpec(
    val id: PlaybackThemeId,
    val title: String,
    val subtitle: String,
    val family: ThemeFamily,
    val capabilities: Set<ThemeCapability>,
    val previewOrder: Int,
)

data class ThemeInput(
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val amplitude: Float = 0f,
    val bass: Float = 0f,
    val mid: Float = 0f,
    val high: Float = 0f,
    val beat: Float = 0f,
)

object PlaybackThemeRegistry {
    val all: List<PlaybackThemeSpec> =
        listOf(
            PlaybackThemeSpec(
                id = PlaybackThemeId.CYBER_SHARK,
                title = "Cyber Shark",
                subtitle = "Layered Board · frame / FX / creature / FARIC",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.CUSTOM_LOGO,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.PROJECTM_BACKGROUND,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 5,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.CYBER_PANTHER,
                title = "Cyber Panther",
                subtitle = "Layered GF · background / FX / panther",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.PROJECTM_BACKGROUND,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 6,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.NEON_EMBLEM,
                title = "Neon Emblem",
                subtitle = "Жива емблема, енергетична корона, частинки",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.CUSTOM_LOGO,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.PROJECTM_BACKGROUND,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.TIMER,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 10,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.ENERGY_CORE,
                title = "Energy Core",
                subtitle = "Центральне ядро, bass-імпульси, shockwave",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.COVER_ART,
                    ThemeCapability.CUSTOM_LOGO,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.REACTIVE_BANDS,
                    ThemeCapability.TRACK_METADATA,
                ),
                previewOrder = 20,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.PORTRAIT_HALO,
                title = "Portrait Halo",
                subtitle = "Фото/обкладинка, круговий спектр, частинки",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.COVER_ART,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.TIMER,
                    ThemeCapability.WAVEFORM,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 30,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.GLASS_CORE,
                title = "Glass Core",
                subtitle = "Скляний псевдо-3D об'єкт, refraction і glow",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.COVER_ART,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.WAVEFORM,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 40,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.ORBITAL_CROWN,
                title = "Orbital Crown",
                subtitle = "Орбіти, корона і локальні beat-деформації",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.CUSTOM_LOGO,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.PROJECTM_BACKGROUND,
                    ThemeCapability.REACTIVE_BANDS,
                    ThemeCapability.TRACK_METADATA,
                ),
                previewOrder = 50,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.STAR_SEED,
                title = "Star Seed",
                subtitle = "Космічне ядро, промені та глибина",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.COVER_ART,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.PROJECTM_BACKGROUND,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 60,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.WAVE_IDOL,
                title = "Wave Idol",
                subtitle = "Органічний центральний силует із музичних хвиль",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.CUSTOM_LOGO,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.REACTIVE_BANDS,
                    ThemeCapability.TRACK_METADATA,
                ),
                previewOrder = 65,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.WAVE_IDOL,
                title = "Wave Idol",
                subtitle = "Живий центральний силует із хвиль та аури",
                family = ThemeFamily.HERO,
                capabilities = setOf(
                    ThemeCapability.CUSTOM_LOGO,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 65,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.VINYL,
                title = "Vinyl",
                subtitle = "Платівка, label, назва треку та виконавець",
                family = ThemeFamily.RETRO,
                capabilities = setOf(
                    ThemeCapability.COVER_ART,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.TIMER,
                ),
                previewOrder = 70,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.CASSETTE,
                title = "Cassette",
                subtitle = "Касета, котушки, custom label і artwork",
                family = ThemeFamily.RETRO,
                capabilities = setOf(
                    ThemeCapability.COVER_ART,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.TIMER,
                ),
                previewOrder = 80,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.POSTER,
                title = "Poster",
                subtitle = "Арт + типографіка + стримані reactive FX",
                family = ThemeFamily.TYPOGRAPHY,
                capabilities = setOf(
                    ThemeCapability.COVER_ART,
                    ThemeCapability.BACKGROUND_IMAGE,
                    ThemeCapability.BACKGROUND_VIDEO,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.WAVEFORM,
                ),
                previewOrder = 90,
            ),
            PlaybackThemeSpec(
                id = PlaybackThemeId.VISUALIZER,
                title = "Visualizer",
                subtitle = "FARIC + projectM layered visualizer",
                family = ThemeFamily.REACTIVE,
                capabilities = setOf(
                    ThemeCapability.PROJECTM_BACKGROUND,
                    ThemeCapability.CUSTOM_LOGO,
                    ThemeCapability.TRACK_METADATA,
                    ThemeCapability.REACTIVE_BANDS,
                ),
                previewOrder = 100,
            ),
        ).sortedBy { it.previewOrder }

    fun byId(id: PlaybackThemeId): PlaybackThemeSpec =
        all.first { it.id == id }
}
