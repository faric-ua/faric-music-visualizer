package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinishedExportWindowTest {
    @Test fun shownScreenGetsTheWindowAtOnce() {
        assertEquals(FinishedExportWindow.Where.HERE, FinishedExportWindow.where(thisScreenShown = true, otherScreenShown = false))
    }

    @Test fun rebuiltScreenTakesItWhenTheOldOneIsGone() {
        assertEquals(FinishedExportWindow.Where.OTHER_SCREEN, FinishedExportWindow.where(thisScreenShown = false, otherScreenShown = true))
    }

    @Test fun hiddenAppKeepsTheReportForTheReturn() {
        // faric 06.11 02:29: the batch ended while another app was open — no window then, it waits.
        assertEquals(FinishedExportWindow.Where.WAIT, FinishedExportWindow.where(thisScreenShown = false, otherScreenShown = false))
    }

    @Test fun onReturnTheWaitingReportOpensUnlessAnExportStillRuns() {
        assertTrue(FinishedExportWindow.showWaitingOnReturn(exportRunning = false, hasWaitingReport = true))
        assertFalse(FinishedExportWindow.showWaitingOnReturn(exportRunning = true, hasWaitingReport = true))
        assertFalse(FinishedExportWindow.showWaitingOnReturn(exportRunning = false, hasWaitingReport = false))
    }
}
