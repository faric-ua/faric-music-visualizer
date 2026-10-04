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
 * 1 Big Equalizer
 * 2 GF / Graphic Figures
 * 3 GIF / Animation
 * 4 Effects
 * 5 PulseDeck HUD (LOCKED)
 * 6 Service Overlay
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
        BIG_EQUALIZER(1),
        GRAPHIC_FIGURES(2),
        GIF_ANIMATION(3),
        EFFECTS(4),
        PULSEDECK_LOCKED(5),
        SERVICE_OVERLAY(6),
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
