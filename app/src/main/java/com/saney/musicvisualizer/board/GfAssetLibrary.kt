package com.saney.musicvisualizer.board

import android.content.Context
import android.graphics.Bitmap
import com.saney.musicvisualizer.theme.PlaybackThemeId

/**
 * Shared assets are selected by their OWN source pack, not by the currently
 * playing Hero theme. Returned bitmaps keep their native alpha/coordinates.
 * Consumers own their bitmap lifecycle; do not decode in an animation frame.
 */
object GfAssetLibrary {
    fun availableAssets(context: Context): List<GfAssetRef> =
        GraphicFigureCatalog.ids.flatMap { packId ->
            if (!GraphicFigureCatalog.isInstalled(context, packId)) {
                emptyList()
            } else {
                val types = GfArtType.entries.filter { type ->
                    when {
                        type == GfArtType.CREATURE_HALF ->
                            UserHeroPack.find(packId) != null &&
                                UserHeroPack.hasLayer(context, packId, type.fileStem)
                        packId == PlaybackThemeId.CYBER_SHARK && type == GfArtType.FX ->
                            false
                        else -> true
                    }
                }
                types.map { GfAssetRef(packId.name, it) }
            }
        } + ExternalPngHeroLibrary(context).list()
            .map { GfAssetRef(it.id, GfArtType.CREATURE) }

    fun title(context: Context, asset: GfAssetRef): String =
        if (asset.sourcePackId.startsWith("png_")) {
            ExternalPngHeroLibrary(context).title(asset.sourcePackId) ?: "Зовнішній PNG"
        } else {
            GraphicFigureCatalog.title(PlaybackThemeId.valueOf(asset.sourcePackId))
        }

    fun available(context: Context, asset: GfAssetRef): Boolean =
        asset in availableAssets(context)

    fun load(context: Context, asset: GfAssetRef): Bitmap? {
        if (!available(context, asset)) return null
        if (asset.sourcePackId.startsWith("png_")) {
            return if (asset.type == GfArtType.CREATURE) {
                ExternalPngHeroLibrary(context).decode(asset.sourcePackId)
            } else null
        }
        val packId = PlaybackThemeId.valueOf(asset.sourcePackId)
        return GraphicFigureCatalog.loadIndividualAsset(context, packId, asset.type)
    }
}
