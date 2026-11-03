package com.saney.musicvisualizer.ui

/**
 * V206 (faric 03.11 15:52: «налаштування розкидані в 4–5 місцях… хочу, щоб у меню, де всі шари, біля кожного
 * була кнопка ⚙: відкриває окреме вікно з його налаштуваннями, які вже стоять»).
 *
 * Which settings the ⚙ window of a layer or of a PulseDeck part lists. Every entry only opens a setting that
 * already lives somewhere else (Утиліти, рамка ⋮, права сторінка, теми) — the old places stay as they were.
 * Pure — tested on JVM; MainActivity turns each key into its window.
 */
enum class LayerSetting {
    /** projectM: «⚙ FG» — the front layer's own editor. */
    PROJECTM_FOREGROUND,

    /** projectM: «Зміна візуалізацій: різко / плавно 1 с / 2 с» (V198). */
    PROJECTM_BLEND,

    /** projectM: the right page (BG / CENTER / EDGE FX, AUTO, group, ratings). */
    PROJECTM_RIGHT_PAGE,

    /** GF: the playback theme (hero, vinyl, «Неон-герой» …). */
    THEME,

    /** GF: «Неон-герой» settings screen — only while that theme is on. */
    NEON_HERO,

    /** GF: which Graphic Figure. */
    GRAPHIC_FIGURE,

    /** GF: Board — background / glow and the other GF parts (move, size). */
    GF_BOARD,

    /** The frame at the top: picture / story / lyrics / together. */
    COVER_CONTENT,

    /** The frame: the neon glow under it — which sound it follows. */
    COVER_GLOW,

    /** The frame: the story's language. */
    STORY_LANGUAGE,

    /** The frame: text (see-through, size, dimming) and the equalizer edge — «Текст і окантовка». */
    COVER_TEXT_AND_EDGE,

    /** Place, size, see-through, on/off — Утиліти › Розкладка, one per position. */
    PLACE_PORTRAIT,
    PLACE_LANDSCAPE,
    PLACE_VIDEO,
}

object LayerSettingsMap {
    /** The layer's own window (the parts under it have their own ⚙). Empty — no ⚙ shown. */
    fun forLayer(
        layerName: String,
        neonHeroTheme: Boolean = false,
    ): List<LayerSetting> =
        when (layerName) {
            "VISUALIZER" ->
                listOf(
                    LayerSetting.PROJECTM_FOREGROUND,
                    LayerSetting.PROJECTM_BLEND,
                    LayerSetting.PROJECTM_RIGHT_PAGE,
                )

            "GRAPHIC_FIGURES" ->
                buildList {
                    add(LayerSetting.THEME)
                    if (neonHeroTheme) add(LayerSetting.NEON_HERO)
                    add(LayerSetting.GRAPHIC_FIGURE)
                    add(LayerSetting.GF_BOARD)
                }

            else -> emptyList()
        }

    /** A PulseDeck part (the HUD): its own settings first, then where it sits in each position. */
    fun forHudObject(objectId: String): List<LayerSetting> =
        buildList {
            if (objectId == COVER_BOX) {
                add(LayerSetting.COVER_CONTENT)
                add(LayerSetting.COVER_GLOW)
                add(LayerSetting.STORY_LANGUAGE)
                add(LayerSetting.COVER_TEXT_AND_EDGE)
            }
            if (LayoutGroups.groupOf(objectId) != null) {
                add(LayerSetting.PLACE_PORTRAIT)
                add(LayerSetting.PLACE_LANDSCAPE)
                add(LayerSetting.PLACE_VIDEO)
            }
        }

    fun profileOf(setting: LayerSetting): LayoutProfileId? =
        when (setting) {
            LayerSetting.PLACE_PORTRAIT -> LayoutProfileId.PORTRAIT
            LayerSetting.PLACE_LANDSCAPE -> LayoutProfileId.LANDSCAPE
            LayerSetting.PLACE_VIDEO -> LayoutProfileId.VIDEO_9X16
            else -> null
        }

    /** The layout group the editor opens on for a part (null — the part has no place of its own). */
    fun layoutGroupOf(objectId: String): String? = LayoutGroups.groupOf(objectId)

    private const val COVER_BOX = "cover_box"
}
