package com.saney.musicvisualizer.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Canonical PulseDeck render stack.
 *
 * Z-order is explicit and stable:
 * 0 Visualizer
 * 1 Over-visualization
 * 2 Big Equalizer
 * 3 GF / Graphic Figures
 * 4 GIF / Animation
 * 5 Effects
 * 6 PulseDeck HUD (LOCKED)
 * 7 Service Overlay
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
        OVER_VISUALIZATION(1),
        BIG_EQUALIZER(2),
        GRAPHIC_FIGURES(3),
        GIF_ANIMATION(4),
        EFFECTS(5),
        PULSEDECK_LOCKED(6),
        SERVICE_OVERLAY(7),
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
