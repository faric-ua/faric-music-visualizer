package com.saney.musicvisualizer.board

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.saney.musicvisualizer.R
import com.saney.musicvisualizer.theme.PlaybackThemeId

data class GraphicFigureAssets(
    val frame: Bitmap?,
    val fx: Bitmap?,
    val creature: Bitmap?,
    val wordmark: Bitmap?,
)

object GraphicFigureCatalog {
    val ids: List<PlaybackThemeId> =
        listOf(
            PlaybackThemeId.CYBER_SHARK,
            PlaybackThemeId.CYBER_PANTHER,
        )

    fun normalize(
        themeId: PlaybackThemeId,
    ): PlaybackThemeId =
        if (themeId in ids) {
            themeId
        } else {
            PlaybackThemeId.CYBER_SHARK
        }

    fun title(
        themeId: PlaybackThemeId,
    ): String =
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                "Cyber Panther"

            else ->
                "Cyber Shark"
        }

    fun creatureLabel(
        themeId: PlaybackThemeId,
    ): String =
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                "Panther"

            else ->
                "Shark"
        }

    fun supportedLayers(
        themeId: PlaybackThemeId,
    ): Set<BoardLayerId> =
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                BoardLayerId.entries.toSet()

            else ->
                BoardLayerId.entries.toSet()
        }

    fun supports(
        themeId: PlaybackThemeId,
        layerId: BoardLayerId,
    ): Boolean =
        layerId in
            supportedLayers(
                themeId,
            )

    fun loadAssets(
        context: Context,
        themeId: PlaybackThemeId,
    ): GraphicFigureAssets =
        when (normalize(themeId)) {
            PlaybackThemeId.CYBER_PANTHER ->
                GraphicFigureAssets(
                    frame =
                        decode(
                            context,
                            R.drawable.cyber_panther_frame,
                        ),
                    fx =
                        decode(
                            context,
                            R.drawable.cyber_panther_fx,
                        ),
                    creature =
                        decode(
                            context,
                            R.drawable.cyber_panther_creature,
                        ),
                    wordmark =
                        decode(
                            context,
                            R.drawable.cyber_panther_wordmark,
                        ),
                )

            else ->
                GraphicFigureAssets(
                    frame =
                        decode(
                            context,
                            R.drawable.cyber_shark_frame,
                        ),
                    fx = null,
                    creature =
                        decode(
                            context,
                            R.drawable.cyber_shark_creature,
                        ),
                    wordmark =
                        decode(
                            context,
                            R.drawable.cyber_shark_wordmark,
                        ),
                )
        }

    private fun decode(
        context: Context,
        drawable: Int,
    ): Bitmap? =
        runCatching {
            BitmapFactory.decodeResource(
                context.resources,
                drawable,
                BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig =
                        Bitmap.Config.ARGB_8888
                },
            )
        }.getOrNull()
}
