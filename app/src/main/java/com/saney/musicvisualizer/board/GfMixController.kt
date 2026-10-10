package com.saney.musicvisualizer.board

import android.content.Context
import com.saney.musicvisualizer.theme.PlaybackThemeId
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import java.util.UUID

/**
 * Four-object GF Quick Mix. All sources stay untouched: only references and
 * per-layer transforms are saved. Named presets are immutable until explicitly
 * copied into the working draft; switching whole heroes simply deactivates it.
 */
class GfMixController(private val context: Context) {
    private val prefs = context.getSharedPreferences("faric_gf_mix_selection_v1", Context.MODE_PRIVATE)
    private val store = GfCompositionStore(context)

    fun active(): GfComposition? =
        prefs.getString("active_id", null)?.let(store::load)

    fun clear() {
        prefs.edit().remove("active_id").apply()
    }

    private fun typeOf(layer: BoardLayerId): GfArtType? = when (layer) {
        BoardLayerId.FRAME -> GfArtType.FRAME
        BoardLayerId.FX -> GfArtType.FX
        BoardLayerId.CREATURE -> GfArtType.CREATURE
        BoardLayerId.WORDMARK -> GfArtType.WORDMARK
        else -> null
    }

    fun overrides(): Map<BoardLayerId, GfAssetRef> =
        active()?.instances?.mapNotNull { part ->
            BoardLayerId.entries.firstOrNull { typeOf(it) == part.asset.type }
                ?.let { it to part.asset }
        }?.toMap() ?: emptyMap()

    fun transforms(
        baseline: Map<BoardLayerId, BoardLayerTransform>,
    ): Map<BoardLayerId, BoardLayerTransform> {
        val active = active() ?: return baseline
        return baseline + active.instances.mapNotNull { part ->
            val layerId = BoardLayerId.entries.firstOrNull { typeOf(it) == part.asset.type }
            layerId?.let {
                it to BoardLayerTransform(
                    offsetXFraction = part.x,
                    offsetYFraction = part.y,
                    scale = part.scale,
                    rotationDegrees = part.rotationDegrees,
                    autoRotationDegreesPerSecond = part.autoRotationDegreesPerSecond,
                    opacity = part.opacity,
                ).sanitized()
            }
        }.toMap()
    }

    fun visible(layer: BoardLayerId, original: Boolean): Boolean {
        val type = typeOf(layer) ?: return original
        return active()?.instances?.firstOrNull { it.asset.type == type }?.visible ?: original
    }

    fun editVisibility(layer: BoardLayerId, visible: Boolean): Boolean {
        val type = typeOf(layer) ?: return false
        val mix = active() ?: return false
        store.save(mix.copy(instances = mix.instances.map { part ->
            if (part.asset.type == type) part.copy(visible = visible) else part
        }))
        return true
    }

    fun editTransform(layer: BoardLayerId, value: BoardLayerTransform): Boolean {
        val type = typeOf(layer) ?: return false
        val mix = active() ?: return false
        val safe = value.sanitized()
        store.save(mix.copy(instances = mix.instances.map { part ->
            if (part.asset.type == type) part.copy(
                x = safe.offsetXFraction,
                y = safe.offsetYFraction,
                scale = safe.scale,
                rotationDegrees = safe.rotationDegrees,
                autoRotationDegreesPerSecond = safe.autoRotationDegreesPerSecond,
                opacity = safe.opacity,
            ) else part
        }))
        return true
    }

    private fun working(
        themeId: PlaybackThemeId,
        baseline: Map<BoardLayerId, BoardLayerTransform>,
        visible: (BoardLayerId) -> Boolean,
    ): GfComposition {
        active()?.let { return it }
        val types = listOf(
            BoardLayerId.FRAME to GfArtType.FRAME,
            BoardLayerId.FX to GfArtType.FX,
            BoardLayerId.CREATURE to GfArtType.CREATURE,
            BoardLayerId.WORDMARK to GfArtType.WORDMARK,
        )
        return GfComposition(
            compositionId = DRAFT_ID,
            title = "Мій GF",
            instances = types.mapIndexed { i, (layer, type) ->
                // Cyber Shark has no independent FX artwork. Choose the
                // installed Panther FX as a concrete user-replaceable starter.
                val source = if (themeId == PlaybackThemeId.CYBER_SHARK &&
                    type == GfArtType.FX
                ) PlaybackThemeId.CYBER_PANTHER else themeId
                val t = baseline[layer] ?: BoardLayerTransform.default()
                GfLayerInstance(
                    instanceId = "part-${type.fileStem}",
                    asset = GfAssetRef(source.name, type),
                    visible = visible(layer),
                    x = t.offsetXFraction, y = t.offsetYFraction,
                    scale = t.scale, rotationDegrees = t.rotationDegrees,
                    autoRotationDegreesPerSecond = t.autoRotationDegreesPerSecond,
                    opacity = t.opacity, zOrder = i,
                )
            },
        )
    }

    private fun activate(candidate: GfComposition, onChanged: () -> Unit): Boolean {
        val needed = setOf(GfArtType.FRAME, GfArtType.FX, GfArtType.CREATURE, GfArtType.WORDMARK)
        val refs = candidate.instances.filter { it.asset.type in needed }.map { it.asset }
        val installed = GfAssetLibrary.availableAssets(context).toSet()
        if (refs.map { it.type }.toSet() != needed || refs.any { it !in installed }) {
            PulseDeckDialogs.showMessage(
                context, "Об'єкт недоступний",
                "Деяких вихідних шарів немає. Додай підготовлений пакет; інший герой не підставляється автоматично.",
            )
            return false
        }
        val draft = candidate.copy(compositionId = DRAFT_ID).sanitized()
        store.save(draft)
        prefs.edit().putString("active_id", DRAFT_ID).apply()
        onChanged()
        return true
    }

    fun show(
        themeId: PlaybackThemeId,
        baseline: Map<BoardLayerId, BoardLayerTransform>,
        visible: (BoardLayerId) -> Boolean,
        onChanged: () -> Unit,
    ) {
        val mix = working(themeId, baseline, visible)
        val parts = listOf(
            GfArtType.FRAME to "Рамка",
            GfArtType.FX to "Ефекти",
            GfArtType.CREATURE to "Герой",
            GfArtType.WORDMARK to "Напис",
        )
        val titles = parts.map { (type, title) ->
            val part = mix.instances.firstOrNull { it.asset.type == type }
            val source = part?.asset?.sourcePackId?.let {
                runCatching { GraphicFigureCatalog.title(PlaybackThemeId.valueOf(it)) }.getOrNull()
            } ?: "відсутній"
            "$title · $source"
        }
        PulseDeckDialogs.showActionList(
            context, "GF Мікс · об'єкти",
            titles + listOf(
                "Зберегти як шаблон…",
                "Відкрити збережений шаблон",
                "Відновити оригінальну емблему",
            ),
        ) { index ->
            when {
                index < parts.size -> {
                    val type = parts[index].first
                    val available = GfAssetLibrary.availableAssets(context).filter { it.type == type }
                    if (available.isEmpty()) {
                        PulseDeckDialogs.showMessage(context, "Немає об'єктів", "Немає встановлених PNG для цього шару.")
                    } else {
                        val checked = available.indexOf(mix.instances.firstOrNull { it.asset.type == type }?.asset)
                        PulseDeckDialogs.showSingleChoice(
                            context, "Обрати · ${parts[index].second}",
                            available.map { GraphicFigureCatalog.title(PlaybackThemeId.valueOf(it.sourcePackId)) },
                            checkedIndex = checked,
                        ) { i ->
                            val changed = mix.copy(instances = mix.instances.map { part ->
                                if (part.asset.type == type) part.copy(asset = available[i]) else part
                            })
                            if (activate(changed, onChanged)) {
                                show(themeId, baseline, visible, onChanged)
                            }
                        }
                    }
                }
                index == 4 -> PulseDeckDialogs.showTextInput(
                    context, "Зберегти GF-шаблон",
                    hint = "Назва шаблона", initialValue = mix.title,
                ) { text ->
                    val title = text.trim().take(200)
                    if (title.isNotEmpty()) {
                        val preset = mix.copy(
                            compositionId = "gf-preset-" + UUID.randomUUID().toString(),
                            title = title,
                        )
                        store.save(preset)
                        activate(preset, onChanged)
                    }
                }
                index == 5 -> {
                    val presets = store.listAll().filter { it.compositionId != DRAFT_ID }
                    if (presets.isEmpty()) {
                        PulseDeckDialogs.showMessage(context, "GF-шаблони", "Збережених шаблонів ще немає.")
                    } else {
                        PulseDeckDialogs.showSingleChoice(
                            context, "Відкрити GF-шаблон",
                            presets.map { it.title },
                        ) { i ->
                            if (activate(presets[i], onChanged)) {
                                show(themeId, baseline, visible, onChanged)
                            }
                        }
                    }
                }
                index == 6 -> {
                    clear()
                    onChanged()
                }
            }
        }
    }

    companion object {
        private const val DRAFT_ID = "gf-working-v1"
    }
}
