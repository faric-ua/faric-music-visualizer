package com.saney.musicvisualizer.export

/**
 * V172 (faric 23.10 20:20, запис екрана повного експорту: «шкала нерівно показує — 80 %, 92 %… дуже
 * довго йде; якби вона живіша була»). The old bar gave the picture 0–75 %, then stood at «Кодую аудіо ·
 * 80 %» for 30 s and at «Збираю та зберігаю MP4 · 92 %» for 10 s — while the picture is ~92 % of the
 * time. Now each stage gets the share of the bar it takes in time (measured 23.10: 452 s picture,
 * 30 s sound, 10 s file), the sound and the file stages move inside too, the bar has tenths of a
 * percent, and the line says how long is left. Pure maths — ExportProgressTest.
 */
object ExportProgress {
    const val RENDER_END = 91f
    const val AUDIO_END = 97f
    const val MUX_END = 99.5f

    fun render(
        framesDone: Int,
        frameCount: Int,
    ): Float = if (frameCount <= 0) 0f else (framesDone.toFloat() / frameCount).coerceIn(0f, 1f) * RENDER_END

    fun audio(fraction: Float): Float = RENDER_END + (AUDIO_END - RENDER_END) * fraction.coerceIn(0f, 1f)

    fun mux(fraction: Float): Float = AUDIO_END + (MUX_END - AUDIO_END) * fraction.coerceIn(0f, 1f)

    fun stage(percent: Float): String =
        when {
            percent < RENDER_END -> "Рендерю відео"
            percent < AUDIO_END -> "Кодую аудіо"
            percent < 100f -> "Збираю та зберігаю MP4"
            else -> "MP4 успішно збережено"
        }

    /**
     * «лишилось ~3 хв 20 с» from the time so far and the share done ([startPercent] — where this run's
     * own work began, e.g. after preparing PCM). Null while it is too early to tell (< 3 % or < 5 s).
     */
    fun remaining(
        elapsedMs: Long,
        percent: Float,
        startPercent: Float = 0f,
    ): String? {
        val done = percent - startPercent
        val left = 100f - percent
        if (done < 3f || elapsedMs < 5_000L || left <= 0f) return null
        val seconds = (elapsedMs / 1000.0 * left / done).toLong()
        return "лишилось ~" + duration(seconds)
    }

    fun duration(seconds: Long): String =
        when {
            seconds < 60 -> "${((seconds + 4) / 5 * 5).coerceAtLeast(5)} с"
            seconds < 600 -> {
                val rounded = (seconds + 5) / 10 * 10
                val m = rounded / 60
                val s = rounded % 60
                if (s == 0L) "$m хв" else "$m хв $s с"
            }
            else -> "${(seconds + 30) / 60} хв"
        }
}
