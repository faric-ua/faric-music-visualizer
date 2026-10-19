package com.saney.musicvisualizer.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import kotlin.math.min

/**
 * Canonical PulseDeck window body (v0.19.75, faric 19.10 21:18: «візьми шаблон
 * інформаційного вікна і на нього наклади все»).
 *
 * A vertical panel that is never taller than [maxHeightFraction] of the CURRENT
 * screen (re-read on every measure, so it also fits after a rotation / split screen).
 * Short content keeps its natural height. When the content is taller, only the
 * [flexible] child (the scrollable middle) shrinks and scrolls — the title above it
 * and the action buttons below it always stay visible.
 *
 * Before this, each window opted into scrolling by hand (`scrollableMessage`), and a
 * long confirm message (batch test, 8 songs) pushed «Почати» / «Скасувати» off screen.
 */
class BoundedDialogPanel(
    context: Context,
    private val maxHeightFraction: Float = MAX_HEIGHT_FRACTION,
) : LinearLayout(context) {
    /** The child that may shrink and scroll (usually the message ScrollView). */
    var flexible: View? = null

    init {
        orientation = VERTICAL
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val screenCap = (resources.displayMetrics.heightPixels * maxHeightFraction).toInt()
        val available =
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                screenCap
            } else {
                min(screenCap, MeasureSpec.getSize(heightMeasureSpec))
            }
        val lp = flexible?.layoutParams as? LayoutParams
        // Pass 1: natural size.
        if (lp != null) {
            lp.height = LayoutParams.WRAP_CONTENT
            lp.weight = 0f
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST))
        // Pass 2: too tall → the flexible middle takes only what is left.
        if (lp != null && measuredHeight >= available) {
            lp.height = 0
            lp.weight = 1f
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY))
        }
    }

    companion object {
        const val MAX_HEIGHT_FRACTION = 0.82f
    }
}
