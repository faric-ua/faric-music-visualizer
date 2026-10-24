package com.saney.musicvisualizer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.theme.PlaybackThemeId
import com.saney.musicvisualizer.ui.HeroThemeView
import com.saney.musicvisualizer.ui.NeonHeroMath
import com.saney.musicvisualizer.ui.NeonHeroSettings
import com.saney.musicvisualizer.ui.NeonHeroStore
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import com.saney.musicvisualizer.ui.UtilityUi
import com.saney.musicvisualizer.ui.UtilityUi.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * V191 «Неон-герой» — settings (faric 25.10 01:26: «налаштувань багатенько — все можна під себе
 * підігнати»; макет v4 у чаті). Every change is saved at once and seen in the live preview at the top
 * (a pretend song, 128 BPM); the main screen and the video take the same settings.
 */
class PulseDeckNeonHeroActivity : ComponentActivity() {
    private var s = NeonHeroSettings()
    private lateinit var content: LinearLayout
    private lateinit var preview: HeroThemeView
    private val handler = Handler(Looper.getMainLooper())
    private var beatIndex = 0
    private var lastBeat = 0L

    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            // Only read permission — the photo stays where it is (no copy).
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            change(s.copy(photoUri = uri.toString(), photoOn = true), rebuild = true)
        }

    private val pretendSong =
        object : Runnable {
            override fun run() {
                val now = SystemClock.elapsedRealtime()
                val t = now / 1000f
                var beat = 0f
                var pan = 0f
                if (now - lastBeat > 469) {
                    lastBeat = now
                    beatIndex++
                    beat = if (beatIndex % 8 == 0) 1f else if (beatIndex % 4 == 0) 0.85f else if (beatIndex % 2 == 0) 0.5f else 0.35f
                    pan = if (beatIndex % 3 == 0) -0.6f else if (beatIndex % 5 == 0) 0.6f else 0f
                }
                val kick = exp(-((now - lastBeat) / 469f) * 5f)
                val spectrum = FloatArray(32) { i -> ((if (i < 8) kick * (1f - i / 8f) else 0f) + 0.3f * abs(sin(t * 3f + i * 0.7f)) * (0.4f + i / 64f)).coerceIn(0f, 1f) }
                preview.updateSignal(SceneSignal(amplitude = 0.4f + kick * 0.5f, bass = kick, mid = 0.4f, high = 0.3f, beatStrength = beat, stereoPan = pan, spectrum = spectrum))
                handler.postDelayed(this, 33L)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        s = NeonHeroStore.load(this)
        content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(24))
            }
        val scroll =
            ScrollView(this).apply {
                addView(content)
            }
        preview =
            HeroThemeView(this).apply {
                setTheme(PlaybackThemeId.NEON_HERO)
                setPlaying(true)
                setMetadata("MIDNIGHT PULSE", "FARIC")
                albumName = "VOL. 1 — MIDNIGHT PULSE"
            }
        // V191 (faric 25.10 02:30, відео: «прев'ю має стояти, а налаштування — прокручуватись у віконці під
        // ним, щоб я бачив, що змінюю»): the title and the live preview stay at the top; only the settings scroll.
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(UtilityUi.BG)
            }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(4)) }
        header.addView(UtilityUi.button(this, "‹") { finish() }.apply { contentDescription = "Назад" }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(UtilityUi.text(this, "Неон-герой", 22f, bold = true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        val previewHeight = (resources.displayMetrics.heightPixels * 0.40f).toInt()
        root.addView(preview, LinearLayout.LayoutParams(-1, previewHeight))
        root.addView(
            android.view.View(this).apply { setBackgroundColor(UtilityUi.PANEL_EDGE) },
            LinearLayout.LayoutParams(-1, dp(1)),
        )
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        UtilityUi.padForSystemBars(root)
        build()
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        handler.post(pretendSong)
    }

    override fun onPause() {
        handler.removeCallbacks(pretendSong)
        super.onPause()
    }

    private fun build() {
        content.removeAllViews()
        content.addView(
            UtilityUi.text(this, "Угорі — живе прев'ю (музика уявна, 128 BPM). На головному екрані й у відео — так само, від справжнього звуку.", 13f, UtilityUi.MUTED),
            block(0),
        )

        section("Герой у центрі")
        toggle("Герой", s.heroOn) { change(s.copy(heroOn = !s.heroOn), rebuild = true) }
        slider("Видимість героя", 10, 100, s.heroAlpha) { change(s.copy(heroAlpha = it)) }
        content.addView(
            UtilityUi.button(this, "Форма: " + NeonHeroMath.SHAPE_NAMES[s.shape] + " ▾") {
                PulseDeckDialogs.showSingleChoice(context = this, title = "Форма героя", labels = NeonHeroMath.SHAPE_NAMES, checkedIndex = s.shape) { i ->
                    change(s.copy(shape = i), rebuild = true)
                }
            },
            block(6),
        )
        slider("Гумовість", 20, 100, s.rubber) { change(s.copy(rubber = it)) }
        slider("Вм'ятина від удару", 0, 150, s.dent) { change(s.copy(dent = it)) }
        toggle("Маятник (відлітає в бік басу)", s.pendulumOn) { change(s.copy(pendulumOn = !s.pendulumOn), rebuild = true) }
        slider("Сила маятника", 20, 150, s.pendulum) { change(s.copy(pendulum = it)) }
        toggle("Аура на ударі", s.auraOn) { change(s.copy(auraOn = !s.auraOn), rebuild = true) }
        slider("Сила аури", 30, 150, s.aura) { change(s.copy(aura = it)) }
        toggle("Еквалайзер і шкали L / R усередині", s.eqOn) { change(s.copy(eqOn = !s.eqOn), rebuild = true) }
        toggle("Підписи L і R", s.lrOn) { change(s.copy(lrOn = !s.lrOn), rebuild = true) }
        toggle("Назва пісні (слово — рядок)", s.titleOn) { change(s.copy(titleOn = !s.titleOn), rebuild = true) }
        toggle("Альбом під героєм", s.albumOn) { change(s.copy(albumOn = !s.albumOn), rebuild = true) }

        section("Фото фону")
        content.addView(
            UtilityUi.text(
                this,
                if (s.photoUri.isBlank()) "Фото не вибрано — темна ніч." else "Фото вибрано (застосунок лише читає його, не копіює).",
                12f,
                UtilityUi.MUTED,
            ),
            block(2),
        )
        val photoRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        photoRow.addView(UtilityUi.button(this, if (s.photoUri.isBlank()) "Вибрати фото" else "Інше фото") { pickPhoto.launch(arrayOf("image/*")) }, UtilityUi.weighted(context = this))
        if (s.photoUri.isNotBlank()) {
            photoRow.addView(UtilityUi.button(this, "Без фото") { change(s.copy(photoUri = ""), rebuild = true) }, UtilityUi.weighted(context = this))
        }
        content.addView(photoRow, block(6))
        toggle("Фото видно", s.photoOn) { change(s.copy(photoOn = !s.photoOn), rebuild = true) }
        slider("Видимість фото", 0, 100, s.photoAlpha) { change(s.copy(photoAlpha = it)) }
        toggle("Стрибок фото на ударі", s.fxJump) { change(s.copy(fxJump = !s.fxJump), rebuild = true) }
        toggle("Тряска на сильному ударі", s.fxShake) { change(s.copy(fxShake = !s.fxShake), rebuild = true) }
        toggle("Спотворення смугами (і напису)", s.fxGlitch) { change(s.copy(fxGlitch = !s.fxGlitch), rebuild = true) }

        section("Краплі й хвилі")
        toggle("Хвилі від крапель", s.ripplesOn) { change(s.copy(ripplesOn = !s.ripplesOn), rebuild = true) }
        slider("Кількість хвиль", 10, 100, s.rippleCount) { change(s.copy(rippleCount = it)) }
        slider("Розмір хвилі", 20, 100, s.rippleSize) { change(s.copy(rippleSize = it)) }
        slider("Швидкість розпливання", 20, 100, s.rippleSpeed) { change(s.copy(rippleSpeed = it)) }
        slider("Збільшення (лінза)", 0, 100, s.rippleZoom) { change(s.copy(rippleZoom = it)) }
        slider("Переломлення фото", 0, 100, s.rippleRefract) { change(s.copy(rippleRefract = it)) }

        section("Дощ")
        toggle("Дощ", s.rainOn) { change(s.copy(rainOn = !s.rainOn), rebuild = true) }
        slider("Кількість", 10, 100, s.rainCount) { change(s.copy(rainCount = it)) }
        slider("Прозорість", 10, 100, s.rainAlpha) { change(s.copy(rainAlpha = it)) }
        slider("Швидкість", 20, 150, s.rainSpeed) { change(s.copy(rainSpeed = it)) }
        slider("Довжина крапель", 30, 200, s.rainLength) { change(s.copy(rainLength = it)) }

        section("Краплі-овали з країв")
        toggle("Овали", s.blobsOn) { change(s.copy(blobsOn = !s.blobsOn), rebuild = true) }
        slider("Кількість", 10, 100, s.blobCount) { change(s.copy(blobCount = it)) }
        slider("Прозорість", 10, 100, s.blobAlpha) { change(s.copy(blobAlpha = it)) }

        content.addView(
            UtilityUi.button(this, "Скинути все до стандартних", UtilityUi.Kind.WARNING) {
                PulseDeckDialogs.showConfirm(
                    context = this,
                    title = "Скинути «Неон-герой»?",
                    message = "Форма, сила ударів, дощ, хвилі й овали стануть стандартними. Вибране фото лишиться.",
                    positiveLabel = "Так, скинути",
                    destructive = true,
                ) { change(NeonHeroSettings(photoUri = s.photoUri), rebuild = true) }
            },
            block(24),
        )
    }

    private fun section(title: String) {
        content.addView(UtilityUi.text(this, title, 17f, bold = true), block(22))
    }

    private fun toggle(
        label: String,
        on: Boolean,
        onClick: () -> Unit,
    ) {
        content.addView(
            UtilityUi.button(this, (if (on) "✓ " else "") + label + (if (on) ": увімкнено" else ": вимкнено"), if (on) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL, onClick),
            block(6),
        )
    }

    /** Label, then [−] slider [+] — as in «Текст і окантовка». */
    private fun slider(
        name: String,
        min: Int,
        max: Int,
        value: Int,
        onChange: (Int) -> Unit,
    ) {
        fun label(v: Int) = "$name: $v %"
        val caption: TextView = UtilityUi.text(this, label(value), 13f, UtilityUi.MUTED)
        content.addView(caption, block(8))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val bar =
            SeekBar(this).apply {
                this.max = max - min
                progress = (value - min).coerceIn(0, max - min)
                progressTintList = android.content.res.ColorStateList.valueOf(UtilityUi.CYAN)
                thumbTintList = android.content.res.ColorStateList.valueOf(UtilityUi.CYAN)
                minimumHeight = dp(48)
            }
        fun set(p: Int) {
            val c = p.coerceIn(0, max - min)
            if (bar.progress != c) bar.progress = c
            caption.text = label(c + min)
            onChange(c + min)
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
        next: NeonHeroSettings,
        rebuild: Boolean = false,
    ) {
        s = next
        NeonHeroStore.save(this, next)
        if (rebuild) {
            val scroll = content.parent as? ScrollView // only the settings rebuild; the preview stays
            val y = scroll?.scrollY ?: 0
            build()
            scroll?.post { scroll.scrollTo(0, y) }
        }
    }

    private fun block(topDp: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(topDp) }
}
