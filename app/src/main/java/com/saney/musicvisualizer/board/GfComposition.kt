package com.saney.musicvisualizer.board

/**
 * A source asset is immutable and reusable by any GF composition, regardless
 * of which hero originally supplied it. Do not infer ownership from a wordmark.
 */
enum class GfArtType(val fileStem: String) {
    FRAME("frame"),
    FX("fx"),
    CREATURE("creature"),
    CREATURE_HALF("creature-half"),
    WORDMARK("wordmark"),
}

data class GfAssetRef(
    val sourcePackId: String,
    val type: GfArtType,
) {
    init {
        require(PACK_ID.matches(sourcePackId)) { "Invalid GF source pack ID" }
    }

    val assetId: String get() = "$sourcePackId:${type.fileStem}"

    companion object {
        private val PACK_ID = Regex("[A-Za-z0-9_-]{1,96}")
    }
}

/** Coordinates are offsets relative to the composition canvas, not PulseDeck HUD centers. */
data class GfLayerInstance(
    val instanceId: String,
    val asset: GfAssetRef,
    val visible: Boolean = true,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val autoRotationDegreesPerSecond: Float = 0f,
    val opacity: Float = 1f,
    val zOrder: Int = 0,
) {
    init {
        require(instanceId.isNotBlank() && instanceId.length <= 96) {
            "Invalid GF instance ID"
        }
    }

    fun sanitized(): GfLayerInstance = copy(
        x = x.finiteOr(0f).coerceIn(-2f, 2f),
        y = y.finiteOr(0f).coerceIn(-2f, 2f),
        scale = scale.finiteOr(1f).coerceIn(0.1f, 4f),
        rotationDegrees = rotationDegrees.finiteOr(0f).coerceIn(-360f, 360f),
        autoRotationDegreesPerSecond = autoRotationDegreesPerSecond.finiteOr(0f).coerceIn(-180f, 180f),
        opacity = opacity.finiteOr(1f).coerceIn(0f, 1f),
    )
}

data class GfComposition(
    val compositionId: String,
    val title: String,
    val instances: List<GfLayerInstance>,
) {
    init {
        require(compositionId.isNotBlank() && compositionId.length <= 96) {
            "Invalid GF composition ID"
        }
        require(title.isNotBlank() && title.length <= 200) { "Invalid GF title" }
        require(instances.map { it.instanceId }.distinct().size == instances.size) {
            "GF instance IDs must be unique within a composition"
        }
        require(instances.size <= 64) { "Too many GF layer instances" }
    }

    /** Stable order for rendering and exporting even if zOrder is tied. */
    fun orderedInstances(): List<GfLayerInstance> =
        instances.withIndex().sortedWith(
            compareBy<IndexedValue<GfLayerInstance>> { it.value.zOrder }
                .thenBy { it.index },
        ).map { it.value }

    fun sanitized(): GfComposition =
        copy(instances = instances.map { it.sanitized() })

    companion object {
        /** Backward-compatible default: four independent parts from one pack. */
        fun fromSourcePack(
            compositionId: String,
            title: String,
            sourcePackId: String,
        ): GfComposition = GfComposition(
            compositionId = compositionId,
            title = title,
            instances = listOf(
                GfArtType.FRAME,
                GfArtType.FX,
                GfArtType.CREATURE,
                GfArtType.WORDMARK,
            ).mapIndexed { index, type ->
                GfLayerInstance(
                    instanceId = "layer-${index + 1}",
                    asset = GfAssetRef(sourcePackId, type),
                    zOrder = index,
                )
            },
        )
    }
}

private fun Float.finiteOr(fallback: Float): Float =
    if (isFinite()) this else fallback
