package com.saney.musicvisualizer.playback

/**
 * ▶ must always start the song (faric 20.10 18:17: after a long time in
 * another app the restored song did not play — ▶ stayed ▶, the slider moved,
 * no sound). Pure rules so they can be unit-tested; the controller maps
 * ExoPlayer state onto [Phase].
 *
 * 1. Before play: a player left idle (error / never prepared) is prepared
 *    again; a finished song starts from the beginning.
 * 2. [WAIT_MS] after ▶: still not playing → wait while loading, otherwise
 *    reload the same queue at the same place once; if that also fails — tell
 *    the user instead of silently showing ▶.
 */
object PlayRecovery {
    enum class Phase { IDLE, BUFFERING, READY, ENDED }

    enum class Before { NONE, PREPARE, RESTART }

    enum class After { DONE, WAIT, RELOAD, AUDIO_BUSY, GIVE_UP }

    const val WAIT_MS = 1_500L
    const val MAX_WAITS = 4
    const val MAX_RELOADS = 1

    fun beforePlay(phase: Phase): Before =
        when (phase) {
            Phase.IDLE -> Before.PREPARE
            Phase.ENDED -> Before.RESTART
            else -> Before.NONE
        }

    /**
     * @param waits how many times we already waited for loading
     * @param reloads how many times we already reloaded the queue
     */
    fun afterWait(
        isPlaying: Boolean,
        wantsToPlay: Boolean,
        phase: Phase,
        suppressed: Boolean,
        waits: Int,
        reloads: Int,
    ): After =
        when {
            isPlaying || !wantsToPlay -> After.DONE
            phase == Phase.BUFFERING && waits < MAX_WAITS -> After.WAIT
            phase == Phase.READY && suppressed -> After.AUDIO_BUSY
            reloads < MAX_RELOADS -> After.RELOAD
            else -> After.GIVE_UP
        }
}
