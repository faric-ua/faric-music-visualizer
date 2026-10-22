package com.saney.musicvisualizer

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.saney.musicvisualizer.ui.CoverTextSettings
import com.saney.musicvisualizer.ui.EdgeEqPreviewView
import com.saney.musicvisualizer.ui.EdgeEqSettings
import com.saney.musicvisualizer.ui.EdgeLayer
import com.saney.musicvisualizer.ui.EdgePalettes
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import com.saney.musicvisualizer.ui.UtilityUi
import com.saney.musicvisualizer.ui.UtilityUi.dp

/**
 * V141 (faric 23.10 00:35: «всі варіанти додати і щоб можна було просто налаштовувати — колір,
 * щільність, товщину, реакцію на музику… і щоб прокручувались»). Opened from ⋮ in the frame.
 * Every change is saved at once and shown in the live preview at the top; the main screen and the
 * video take the same settings.
 */
class PulseDeckEdgeEqActivity : ComponentActivity() {
    private var settings = EdgeEqSettings.DEFAULT
    private var text = CoverTextSettings()
    private lateinit var preview: EdgeEqPreviewView
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = load()
        text = CoverTextSettings.decode(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TEXT, null))
        content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(24))
            }
        val scroll =
            ScrollView(this).apply {
                setBackgroundColor(UtilityUi.BG)
                addView(content)
            }
        UtilityUi.padForSystemBars(scroll)
        preview = EdgeEqPreviewView(this)
        build()
        setContentView(scroll)
    }

    private fun build() {
        content.removeAllViews()
        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
        header.addView(UtilityUi.button(this, "‹") { finish() }.apply { contentDescription = "Назад" }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(UtilityUi.text(this, "Текст і окантовка", 22f, bold = true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        content.addView(header)
        content.addView(
            UtilityUi.text(
                this,
                "Еквалайзер навколо рамки вгорі: смужки — ритм, крапки — високі, хвиля — мелодія. Угорі — живе прев'ю (музика уявна); на головному й у відео — так само, від справжнього звуку.",
                13f,
                UtilityUi.MUTED,
            ),
            block(8),
        )
        // V148 (faric 23.10 02:54: «текст не дуже видно… поставити йому окремо прозорість»).
        content.addView(UtilityUi.text(this, "Текст у рамці (історія, слова)", 17f, bold = true), block(14))
        content.addView(
            UtilityUi.text(this, "Свій, окремо від прозорості плитки в «Розкладці».", 12f, UtilityUi.MUTED),
            block(2),
        )
        slider({ v -> "Видимість тексту: ${v + 10} %" }, 90, text.opacity - 10) { v -> changeText(text.copy(opacity = v + 10)) }
        slider({ v -> "Затемнення під текстом: $v %" }, 90, text.veil) { v -> changeText(text.copy(veil = v)) }
        slider({ v -> "Розмір тексту: ${v + 70} %" }, 90, text.size - 70) { v -> changeText(text.copy(size = v + 70)) }
        content.addView(UtilityUi.text(this, "Окантовка-еквалайзер", 17f, bold = true), block(22))

        (preview.parent as? android.view.ViewGroup)?.removeView(preview)
        preview.renderer.settings = settings
        content.addView(preview, LinearLayout.LayoutParams(-1, dp(300)).apply { topMargin = dp(10) })

        content.addView(
            toggle(if (settings.on) "Окантовка: увімкнено" else "Окантовка: вимкнено", settings.on) { change(settings.copy(on = !settings.on)) },
            block(12),
        )
        slider(
            label = { v -> "Прокручування: " + scrollLabel(v - 100) },
            max = 200,
            value = settings.scroll + 100,
        ) { v -> change(settings.copy(scroll = v - 100), rebuild = false) }

        section("Смужки — ритм", settings.bars) { layer -> settings.copy(bars = layer) }
        section("Крапки — високі звуки", settings.dots) { layer -> settings.copy(dots = layer) }
        section("Хвиля — мелодія", settings.wave) { layer -> settings.copy(wave = layer) }

        content.addView(
            UtilityUi.button(this, "Скинути все до стандартних", UtilityUi.Kind.WARNING) {
                PulseDeckDialogs.showConfirm(
                    context = this,
                    title = "Скинути текст і окантовку?",
                    message = "Видимість, затемнення й розмір тексту, а також кольори, щільність, товщина, реакція й прокручування окантовки стануть стандартними.",
                    positiveLabel = "Так, скинути",
                    destructive = true,
                ) {
                    changeText(CoverTextSettings())
                    change(EdgeEqSettings.DEFAULT)
                }
            },
            block(24),
        )
    }

    private fun section(
        title: String,
        layer: EdgeLayer,
        apply: (EdgeLayer) -> EdgeEqSettings,
    ) {
        content.addView(UtilityUi.text(this, title, 17f, bold = true), block(22))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(
            toggle(if (layer.enabled) "Увімкнено" else "Вимкнено", layer.enabled) { change(apply(current(title).let { it.copy(enabled = !it.enabled) })) },
            UtilityUi.weighted(context = this),
        )
        row.addView(
            UtilityUi.button(this, "Колір: " + EdgePalettes.get(layer.palette).name + " ▾") {
                PulseDeckDialogs.showSingleChoice(
                    context = this,
                    title = "$title — колір",
                    labels = EdgePalettes.ALL.map { it.name },
                    checkedIndex = current(title).palette,
                ) { index -> change(apply(current(title).copy(palette = index))) }
            },
            UtilityUi.weighted(weight = 1.4f, context = this),
        )
        content.addView(row, block(6))
        slider({ v -> "Щільність: $v" }, 99, layer.density - 1) { v -> change(apply(current(title).copy(density = v + 1)), rebuild = false) }
        slider({ v -> "Товщина: $v" }, 99, layer.thickness - 1) { v -> change(apply(current(title).copy(thickness = v + 1)), rebuild = false) }
        slider({ v -> "Реакція на музику: $v %" }, 200, layer.reaction) { v -> change(apply(current(title).copy(reaction = v)), rebuild = false) }
    }

    /** The layer as it is now (sliders change it without rebuilding the screen). */
    private fun current(title: String): EdgeLayer =
        when {
            title.startsWith("Смужки") -> settings.bars
            title.startsWith("Крапки") -> settings.dots
            else -> settings.wave
        }

    private fun scrollLabel(v: Int): String =
        when {
            v == 0 -> "стоїть"
            v > 0 -> "за годинниковою, $v"
            else -> "проти годинникової, ${-v}"
        }

    private fun toggle(
        label: String,
        on: Boolean,
        onClick: () -> Unit,
    ): TextView = UtilityUi.button(this, label, if (on) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL, onClick)

    /** Label, then [−] slider [+] (faric 22.10 02:23: ±1 buttons at the ends). */
    private fun slider(
        label: (Int) -> String,
        max: Int,
        value: Int,
        onChange: (Int) -> Unit,
    ) {
        val caption = UtilityUi.text(this, label(value), 13f, UtilityUi.MUTED)
        content.addView(caption, block(8))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val bar =
            SeekBar(this).apply {
                this.max = max
                progress = value
                progressTintList = android.content.res.ColorStateList.valueOf(UtilityUi.CYAN)
                thumbTintList = android.content.res.ColorStateList.valueOf(UtilityUi.CYAN)
                minimumHeight = dp(48)
            }
        fun set(v: Int) {
            val clamped = v.coerceIn(0, max)
            if (bar.progress != clamped) bar.progress = clamped
            caption.text = label(clamped)
            onChange(clamped)
        }
        bar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) set(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            },
        )
        row.addView(UtilityUi.button(this, "−") { set(bar.progress - 1) }, LinearLayout.LayoutParams(dp(52), dp(48)))
        row.addView(bar, LinearLayout.LayoutParams(0, dp(48), 1f))
        row.addView(UtilityUi.button(this, "+") { set(bar.progress + 1) }, LinearLayout.LayoutParams(dp(52), dp(48)))
        content.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun change(
        next: EdgeEqSettings,
        rebuild: Boolean = true,
    ) {
        settings = next
        preview.renderer.settings = next
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, next.encode()).apply()
        if (rebuild) {
            val scroll = content.parent as? ScrollView
            val y = scroll?.scrollY ?: 0
            build()
            scroll?.post { scroll.scrollTo(0, y) }
        }
    }

    private fun changeText(next: CoverTextSettings) {
        text = next
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_TEXT, next.encode()).apply()
    }

    private fun load(): EdgeEqSettings = EdgeEqSettings.decode(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, null))

    private fun block(topDp: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(topDp) }

    companion object {
        /** Same file as MainActivity.layerPrefs(). */
        const val PREFS = "pulsedeck_layers"
        const val KEY = "pulsedeck.edge_eq"
        const val KEY_TEXT = "pulsedeck.cover_text"
    }
}
