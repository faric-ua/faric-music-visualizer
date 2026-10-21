package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * V104: the look of «Утиліти» — the same PulseDeck colours as the main screen (dark panel, cyan
 * frame, orange main action, green «вибрано»). Buttons are TextViews (no stock Android buttons — CI).
 */
object UtilityUi {
    val BG = Color.rgb(5, 8, 12)
    val PANEL = Color.rgb(14, 24, 34)
    val PANEL_EDGE = Color.rgb(42, 59, 74)
    val CYAN = Color.rgb(64, 200, 255)
    val ORANGE = Color.rgb(255, 154, 31)
    val GREEN = Color.rgb(61, 220, 132)
    val GREEN_FILL = Color.rgb(15, 58, 38)
    val MUTED = Color.rgb(169, 188, 203)
    val RED = Color.rgb(255, 90, 78)
    val RED_FILL = Color.rgb(42, 18, 12)

    fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun text(
        context: Context,
        value: String,
        size: Float,
        color: Int = Color.WHITE,
        bold: Boolean = false,
    ): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    fun frame(
        fill: Int,
        edge: Int,
        edgeDp: Int,
        radiusDp: Int,
        context: Context,
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = context.dp(radiusDp).toFloat()
            setStroke(context.dp(edgeDp), edge)
        }

    /** A tappable pill; min 48 dp, text wraps and the pill grows (UI_STYLE_CONTRACT). */
    fun button(
        context: Context,
        label: String,
        kind: Kind = Kind.NORMAL,
        onClick: () -> Unit,
    ): TextView =
        TextView(context).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            minHeight = context.dp(48)
            setPadding(context.dp(10), context.dp(8), context.dp(10), context.dp(8))
            style(this, kind)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

    fun style(
        view: TextView,
        kind: Kind,
    ) {
        val c = view.context
        when (kind) {
            Kind.NORMAL -> {
                view.setTextColor(Color.WHITE)
                view.typeface = Typeface.DEFAULT
                view.background = frame(PANEL, PANEL_EDGE, 1, 12, c)
            }
            Kind.MAIN -> {
                view.setTextColor(Color.rgb(26, 14, 0))
                view.typeface = Typeface.DEFAULT_BOLD
                view.background = frame(ORANGE, ORANGE, 1, 12, c)
            }
            Kind.CHOSEN -> {
                view.setTextColor(Color.WHITE)
                view.typeface = Typeface.DEFAULT_BOLD
                view.background = frame(GREEN_FILL, GREEN, 2, 12, c)
            }
            Kind.WARNING -> {
                view.setTextColor(Color.rgb(255, 177, 153))
                view.typeface = Typeface.DEFAULT
                view.background = frame(RED_FILL, Color.rgb(122, 59, 42), 1, 12, c)
            }
            Kind.OUTSIDE -> {
                view.setTextColor(Color.WHITE)
                view.typeface = Typeface.DEFAULT
                view.background = frame(RED_FILL, RED, 2, 12, c)
            }
        }
    }

    enum class Kind { NORMAL, MAIN, CHOSEN, WARNING, OUTSIDE }

    /** Keep the content off the status / navigation bars (Android 15 draws edge to edge). */
    fun padForSystemBars(root: View) {
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
    }

    fun weighted(
        widthDp: Int = 0,
        weight: Float = 1f,
        context: Context,
        marginDp: Int = 3,
    ): ViewGroup.MarginLayoutParams =
        android.widget.LinearLayout.LayoutParams(
            if (widthDp > 0) context.dp(widthDp) else 0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (widthDp > 0) 0f else weight,
        ).apply {
            marginStart = context.dp(marginDp)
            marginEnd = context.dp(marginDp)
        }
}
