package com.saney.musicvisualizer.playback

import com.saney.musicvisualizer.playback.PlayRecovery.After
import com.saney.musicvisualizer.playback.PlayRecovery.Before
import com.saney.musicvisualizer.playback.PlayRecovery.Phase
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayRecoveryTest {
    @Test
    fun idlePlayerIsPreparedBeforePlay() {
        assertEquals(Before.PREPARE, PlayRecovery.beforePlay(Phase.IDLE))
        assertEquals(Before.RESTART, PlayRecovery.beforePlay(Phase.ENDED))
        assertEquals(Before.NONE, PlayRecovery.beforePlay(Phase.READY))
        assertEquals(Before.NONE, PlayRecovery.beforePlay(Phase.BUFFERING))
    }

    @Test
    fun playingOrPausedAgainIsDone() {
        assertEquals(After.DONE, PlayRecovery.afterWait(true, true, Phase.READY, false, 0, 0))
        assertEquals(After.DONE, PlayRecovery.afterWait(false, false, Phase.IDLE, false, 0, 0))
    }

    @Test
    fun loadingWaitsThenReloads() {
        assertEquals(After.WAIT, PlayRecovery.afterWait(false, true, Phase.BUFFERING, false, 0, 0))
        assertEquals(After.RELOAD, PlayRecovery.afterWait(false, true, Phase.BUFFERING, false, PlayRecovery.MAX_WAITS, 0))
    }

    @Test
    fun stuckPlayerIsReloadedOnceThenUserIsTold() {
        assertEquals(After.RELOAD, PlayRecovery.afterWait(false, true, Phase.IDLE, false, 0, 0))
        assertEquals(After.GIVE_UP, PlayRecovery.afterWait(false, true, Phase.IDLE, false, 0, 1))
        assertEquals(After.RELOAD, PlayRecovery.afterWait(false, true, Phase.READY, false, 0, 0))
    }

    @Test
    fun anotherAppHoldingSoundIsReportedNotReloaded() {
        assertEquals(After.AUDIO_BUSY, PlayRecovery.afterWait(false, true, Phase.READY, true, 0, 0))
    }
}
