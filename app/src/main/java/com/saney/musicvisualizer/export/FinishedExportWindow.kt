package com.saney.musicvisualizer.export

/**
 * V238 (faric 06.11 02:29: «я сидів в іншій програмі, в фоні конвертувалось, повернувся — немає вікна сумарної
 * інформації в кінці»).
 *
 * Where the «export finished» window (batch summary, single export, modes test) goes. The report is always saved
 * first as «not seen yet»; a window is opened only on a PulseDeck screen that is on show, and only then is the
 * report marked seen. A window opened while PulseDeck is hidden can be lost with the screen (Android rebuilds a
 * hidden screen from what it saved before the window existed) — so it waits for PulseDeck to come back instead.
 * Pure — tested on JVM.
 */
object FinishedExportWindow {
    enum class Where {
        /** The screen that ran the export is on show — open the window there now. */
        HERE,

        /** That screen is gone or hidden, but another PulseDeck screen is on show (rebuilt meanwhile) — open it there. */
        OTHER_SCREEN,

        /** Nothing is on show — the report waits; the window opens when PulseDeck comes back (marked «поки тебе не було»). */
        WAIT,
    }

    fun where(
        thisScreenShown: Boolean,
        otherScreenShown: Boolean,
    ): Where =
        when {
            thisScreenShown -> Where.HERE
            otherScreenShown -> Where.OTHER_SCREEN
            else -> Where.WAIT
        }

    /** Back on screen: a waiting report is shown unless an export is still running (its own window comes at the end). */
    fun showWaitingOnReturn(
        exportRunning: Boolean,
        hasWaitingReport: Boolean,
    ): Boolean = hasWaitingReport && !exportRunning

    const val AWAY_SUFFIX = " (поки тебе не було)"
}
