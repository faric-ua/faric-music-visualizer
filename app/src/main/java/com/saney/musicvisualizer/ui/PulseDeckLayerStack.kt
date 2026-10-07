package com.saney.musicvisualizer.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Canonical PulseDeck render stack.
 *
 * Z-order is explicit and stable:
 * 0 Visualizer / projectM
 * 1 Visualizer / FARIC Reactive
 * 2 Over-visualization
 * 3 Big Equalizer
 * 4 GF / Graphic Figures
 * 5 GIF / Animation
 * 6 Effects
 * 7 PulseDeck HUD (LOCKED)
 * 8 Service Overlay
 *
 * Layer numbers define depth only. Content and visibility are independent.
 */
class PulseDeckLayerStack(
    context: Context,
) : FrameLayout(context) {

    enum class Layer(
        val z: Int,
    ) {
        VISUALIZER(0),
        FARIC_REACTIVE(1),
        OVER_VISUALIZATION(2),
        BIG_EQUALIZER(3),
        GRAPHIC_FIGURES(4),
        GIF_ANIMATION(5),
        EFFECTS(6),
        PULSEDECK_LOCKED(7),
        SERVICE_OVERLAY(8),
    }

    private val slots: Map<Layer, FrameLayout> =
        Layer.entries
            .sortedBy { it.z }
            .associateWith { layer ->
                FrameLayout(context).also { slot ->
                    slot.tag = "pulsedeck-layer-${layer.z}-${layer.name.lowercase()}"
                    addView(
                        slot,
                        LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
            }

    fun setContent(
        layer: Layer,
        view: View?,
    ) {
        val slot = requireNotNull(slots[layer])
        slot.removeAllViews()
        view ?: return

        (view.parent as? ViewGroup)?.removeView(view)
        slot.addView(
            view,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun slot(
        layer: Layer,
    ): FrameLayout = requireNotNull(slots[layer])

    fun setLayerVisible(
        layer: Layer,
        visible: Boolean,
    ) {
        requireNotNull(slots[layer]).visibility =
            if (visible) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    fun isLayerVisible(
        layer: Layer,
    ): Boolean = requireNotNull(slots[layer]).visibility == View.VISIBLE
}
