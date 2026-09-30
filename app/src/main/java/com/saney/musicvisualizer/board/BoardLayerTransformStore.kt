package com.saney.musicvisualizer.board

import android.content.Context
import com.saney.musicvisualizer.theme.PlaybackThemeId

class BoardLayerTransformStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            "faric_board_layer_transform",
            Context.MODE_PRIVATE,
        )

    fun load(
        themeId: PlaybackThemeId,
        layerId: BoardLayerId,
    ): BoardLayerTransform {
        val prefix =
            prefix(
                themeId,
                layerId,
            )

        return BoardLayerTransform(
            offsetXFraction =
                prefs.getFloat(
                    key(prefix, "x"),
                    0f,
                ),
            offsetYFraction =
                prefs.getFloat(
                    key(prefix, "y"),
                    0f,
                ),
            scale =
                prefs.getFloat(
                    key(prefix, "scale"),
                    1f,
                ),
            rotationDegrees =
                prefs.getFloat(
                    key(prefix, "rotation"),
                    0f,
                ),
            opacity =
                prefs.getFloat(
                    key(prefix, "opacity"),
                    1f,
                ),
        ).sanitized()
    }

    fun loadAll(
        themeId: PlaybackThemeId,
    ): Map<BoardLayerId, BoardLayerTransform> =
        BoardLayerId.entries
            .associateWith { layerId ->
                load(
                    themeId,
                    layerId,
                )
            }

    fun save(
        themeId: PlaybackThemeId,
        layerId: BoardLayerId,
        transform: BoardLayerTransform,
    ) {
        val safe =
            transform.sanitized()

        val prefix =
            prefix(
                themeId,
                layerId,
            )

        prefs.edit()
            .putFloat(
                key(prefix, "x"),
                safe.offsetXFraction,
            )
            .putFloat(
                key(prefix, "y"),
                safe.offsetYFraction,
            )
            .putFloat(
                key(prefix, "scale"),
                safe.scale,
            )
            .putFloat(
                key(prefix, "rotation"),
                safe.rotationDegrees,
            )
            .putFloat(
                key(prefix, "opacity"),
                safe.opacity,
            )
            .apply()
    }

    fun reset(
        themeId: PlaybackThemeId,
        layerId: BoardLayerId,
    ): BoardLayerTransform {
        val value =
            BoardLayerTransform.default()

        save(
            themeId,
            layerId,
            value,
        )

        return value
    }

    fun resetAll(
        themeId: PlaybackThemeId,
    ) {
        BoardLayerId.entries
            .forEach { layerId ->
                reset(
                    themeId,
                    layerId,
                )
            }
    }

    private fun prefix(
        themeId: PlaybackThemeId,
        layerId: BoardLayerId,
    ): String =
        "${themeId.name}_${layerId.name}"

    private fun key(
        prefix: String,
        name: String,
    ): String =
        "${prefix}_${name}"
}
