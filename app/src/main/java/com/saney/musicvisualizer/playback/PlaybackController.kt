package com.saney.musicvisualizer.playback

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.saney.musicvisualizer.analysis.AudioCaptureAnalyzer
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.analysis.StereoBalanceAudioProcessor
import com.saney.musicvisualizer.projectm.ProjectMBridge

data class QueueTrack(
    val uri: Uri,
    val displayName: String,
    val artist: String? = null,
    val album: String? = null,
)

data class PlaybackSnapshot(
    val trackName: String? = null,
    val trackArtist: String? = null,
    val trackAlbum: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val analysisActive: Boolean = false,
    val queueSize: Int = 0,
    val queueIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val status: String = "Оберіть локальний аудіофайл",
)

@UnstableApi
class PlaybackController(application: Application) : AndroidViewModel(application) {
    interface Listener {
        fun onPlaybackSnapshot(snapshot: PlaybackSnapshot)
        fun onSceneSignal(signal: SceneSignal)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var latestStereoPan = 0f

    private val stereoBalanceProcessor =
        StereoBalanceAudioProcessor { pan ->
            latestStereoPan = pan
        }

    private val renderersFactory =
        object : DefaultRenderersFactory(application) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean,
            ): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(
                        enableFloatOutput,
                    )
                    .setEnableAudioOutputPlaybackParameters(
                        enableAudioOutputPlaybackParams,
                    )
                    .setAudioProcessors(
                        arrayOf<AudioProcessor>(
                            stereoBalanceProcessor,
                        ),
                    )
                    .build()
        }

    private val player =
        ExoPlayer.Builder(
            application,
            renderersFactory,
        ).build().apply {
            setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
        }
    private var analyzer: AudioCaptureAnalyzer? = null
    private var currentSessionId = C.AUDIO_SESSION_ID_UNSET
    private var analysisPermissionGranted = false
    private var trackName: String? = null
    private var trackArtist: String? = null
    private var trackAlbum: String? = null
    private var trackUri: Uri? = null
    private var status = "Оберіть локальний аудіофайл"
    private var analysisActive = false
    var listener: Listener? = null

    // V65: last queue / song / position survive Android closing the app in the background.
    private val lastSession = LastSessionStore(application)
    private var restoringLastSession = false
    private var ticksSinceSave = 0

    private val progressTicker = object : Runnable {
        override fun run() {
            emitCurrentState()
            if (player.isPlaying && ++ticksSinceSave >= 20) {
                ticksSinceSave = 0
                saveLastPosition()
            }
            mainHandler.postDelayed(this, 250L)
        }
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                currentSessionId = audioSessionId
                reconnectAnalyzer()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) restoringLastSession = false
                saveLastPosition()
                status = if (isPlaying) {
                    "Відтворення"
                } else if (trackName == null) {
                    "Оберіть локальний аудіофайл"
                } else {
                    "Пауза"
                }
                emitCurrentState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncTrackFromPlayer()
                if (!restoringLastSession) saveLastPosition()
                emitCurrentState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                status = when (playbackState) {
                    Player.STATE_BUFFERING -> "Буферизація…"
                    Player.STATE_READY -> if (player.isPlaying) "Відтворення" else "Готово"
                    Player.STATE_ENDED -> "Завершено"
                    else -> status
                }
                emitCurrentState()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (restoringLastSession) {
                    // The saved song is gone (file moved / no access): start clean, no error.
                    restoringLastSession = false
                    lastSession.clear()
                    player.clearMediaItems()
                    syncTrackFromPlayer()
                    status = "Оберіть локальний аудіофайл"
                    emitCurrentState()
                    return
                }
                status = "Помилка відтворення: ${error.errorCodeName}"
                emitCurrentState()
            }
        })
        mainHandler.post(progressTicker)
        restoreLastSession()
    }

    private fun mediaItemOf(track: QueueTrack): MediaItem =
        MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.uri.toString())
            .setTag(track.displayName)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.displayName)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build(),
            )
            .build()

    /** Once per process (this controller is an app-wide singleton): paused, same song, same place. */
    private fun restoreLastSession() {
        val saved = lastSession.loadTracks()
        if (saved.isEmpty() || player.mediaItemCount > 0) return
        val tracks =
            saved.mapNotNull { track ->
                runCatching { QueueTrack(Uri.parse(track.uri), track.name, track.artist, track.album) }.getOrNull()
            }
        if (tracks.isEmpty()) return
        restoringLastSession = true
        val index = LastSessionCodec.safeIndex(lastSession.index(), tracks.size)
        player.setMediaItems(tracks.map(::mediaItemOf), index, lastSession.position())
        player.playWhenReady = false
        player.prepare()
        syncTrackFromPlayer()
        status = "Пауза"
        emitCurrentState()
    }

    private fun saveLastPosition() {
        if (player.mediaItemCount == 0) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        lastSession.savePosition(player.currentMediaItemIndex, player.currentPosition, duration)
    }

    /** Exact ExoPlayer used by MediaSessionService and all in-app screens. */
    fun sessionPlayer(): Player = player

    private fun ensureNotificationSession() {
        runCatching {
            PlaybackMediaSessionService.ensureStarted(getApplication<Application>())
        }
    }

    fun load(uri: Uri, displayName: String) {
        loadQueue(listOf(QueueTrack(uri, displayName)), 0)
    }

    fun loadQueue(tracks: List<QueueTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        ensureNotificationSession()
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val items = tracks.map(::mediaItemOf)
        restoringLastSession = false
        lastSession.saveQueue(
            tracks.map { LastSessionCodec.Track(it.uri.toString(), it.displayName, it.artist, it.album) },
            safeIndex,
        )
        status = "Завантаження…"
        player.setMediaItems(items, safeIndex, 0L)
        player.prepare()
        syncTrackFromPlayer()
        emitCurrentState()
    }

    fun previous() {
        ensureNotificationSession()
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else if (player.mediaItemCount > 0) {
            player.seekTo(0, 0L)
            player.play()
        }
    }

    fun next() {
        ensureNotificationSession()
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        }
    }

    fun toggleShuffle(): Boolean {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        emitCurrentState()
        return player.shuffleModeEnabled
    }

    private fun syncTrackFromPlayer() {
        val item = player.currentMediaItem
        trackUri = item?.localConfiguration?.uri
        trackName = item?.localConfiguration?.tag as? String
            ?: item?.mediaMetadata?.title?.toString()
            ?: trackUri?.lastPathSegment
        trackArtist = item?.mediaMetadata?.artist?.toString()?.takeIf { it.isNotBlank() }
        trackAlbum = item?.mediaMetadata?.albumTitle?.toString()?.takeIf { it.isNotBlank() }
    }

    fun currentTrackUri(): Uri? =
        trackUri

    fun play() {
        if (trackName == null) return
        ensureNotificationSession()
        // faric 20.10 18:17: after a long time in another app ▶ did nothing.
        when (PlayRecovery.beforePlay(phase())) {
            PlayRecovery.Before.PREPARE -> {
                Log.w(PLAY_LOG, "play: player idle (error=${player.playerError?.errorCodeName}) -> prepare")
                player.prepare()
            }
            PlayRecovery.Before.RESTART -> player.seekToDefaultPosition(player.currentMediaItemIndex)
            PlayRecovery.Before.NONE -> Unit
        }
        player.play()
        playWaits = 0
        playReloads = 0
        schedulePlayCheck()
    }

    private var playWaits = 0
    private var playReloads = 0
    private val playCheck = Runnable { checkPlayStarted() }

    private fun phase(): PlayRecovery.Phase =
        when (player.playbackState) {
            Player.STATE_BUFFERING -> PlayRecovery.Phase.BUFFERING
            Player.STATE_READY -> PlayRecovery.Phase.READY
            Player.STATE_ENDED -> PlayRecovery.Phase.ENDED
            else -> PlayRecovery.Phase.IDLE
        }

    private fun schedulePlayCheck() {
        mainHandler.removeCallbacks(playCheck)
        mainHandler.postDelayed(playCheck, PlayRecovery.WAIT_MS)
    }

    private fun checkPlayStarted() {
        if (player.mediaItemCount == 0) return
        val decision =
            PlayRecovery.afterWait(
                isPlaying = player.isPlaying,
                wantsToPlay = player.playWhenReady,
                phase = phase(),
                suppressed = player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE,
                waits = playWaits,
                reloads = playReloads,
            )
        if (decision != PlayRecovery.After.DONE) {
            Log.w(
                PLAY_LOG,
                "play did not start: $decision state=${player.playbackState} " +
                    "suppression=${player.playbackSuppressionReason} error=${player.playerError?.errorCodeName}",
            )
        }
        when (decision) {
            PlayRecovery.After.DONE -> Unit
            PlayRecovery.After.WAIT -> {
                playWaits++
                schedulePlayCheck()
            }
            PlayRecovery.After.RELOAD -> {
                playReloads++
                playWaits = 0
                reloadQueueInPlace()
                schedulePlayCheck()
            }
            PlayRecovery.After.AUDIO_BUSY -> {
                status = "Звук зайнятий іншою програмою — натисни ▶ ще раз"
                emitCurrentState()
            }
            PlayRecovery.After.GIVE_UP -> {
                player.pause()
                status = "Не вдалося відтворити — відкрий пісню з медіатеки ще раз"
                emitCurrentState()
            }
        }
    }

    /** Same queue, same song, same second — fresh decoder and file access. */
    private fun reloadQueueInPlace() {
        val count = player.mediaItemCount
        if (count == 0) return
        val items = (0 until count).map { player.getMediaItemAt(it) }
        val index = player.currentMediaItemIndex.coerceIn(0, count - 1)
        val position = player.currentPosition.coerceAtLeast(0L)
        player.stop()
        player.setMediaItems(items, index, position)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun toggleRepeatOne(): Boolean {
        player.repeatMode =
            if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                Player.REPEAT_MODE_OFF
            } else {
                Player.REPEAT_MODE_ONE
            }
        return player.repeatMode == Player.REPEAT_MODE_ONE
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration
        if (duration != C.TIME_UNSET && duration > 0L) {
            player.seekTo(positionMs.coerceIn(0L, duration))
            saveLastPosition()
            emitCurrentState()
        }
    }

    fun setAnalysisPermissionGranted(granted: Boolean) {
        analysisPermissionGranted = granted
        reconnectAnalyzer()
    }

    fun emitCurrentState() {
        val duration = player.duration.takeUnless { it == C.TIME_UNSET } ?: 0L
        listener?.onPlaybackSnapshot(
            PlaybackSnapshot(
                trackName = trackName,
                trackArtist = trackArtist,
                trackAlbum = trackAlbum,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration.coerceAtLeast(0L),
                analysisActive = analysisActive,
                queueSize = player.mediaItemCount,
                queueIndex = player.currentMediaItemIndex.takeIf { player.mediaItemCount > 0 } ?: -1,
                shuffleEnabled = player.shuffleModeEnabled,
                status = if (!analysisPermissionGranted && trackName != null) {
                    "$status · реакція на звук вимкнена без дозволу"
                } else {
                    status
                },
            ),
        )
    }

    private fun reconnectAnalyzer() {
        analyzer?.close()
        analyzer = null
        analysisActive = false

        if (!analysisPermissionGranted || currentSessionId == C.AUDIO_SESSION_ID_UNSET) {
            emitCurrentState()
            return
        }

        try {
            analyzer = AudioCaptureAnalyzer(
                audioSessionId = currentSessionId,
                onSignal = { signal ->
                    val enriched =
                        signal.copy(
                            stereoPan =
                                latestStereoPan,
                        )

                    ProjectMBridge.pushSignalIfActive(
                        enriched,
                    )
                    mainHandler.post {
                        listener?.onSceneSignal(
                            enriched,
                        )
                    }
                },
                onPcm = { pcm ->
                    ProjectMBridge.pushPcmIfActive(pcm)
                },
            )
            analysisActive = true
        } catch (_: Throwable) {
            status = "Аудіо грає, але системний Visualizer недоступний"
        }

        emitCurrentState()
    }

    override fun onCleared() {
        mainHandler.removeCallbacks(progressTicker)
        analyzer?.close()
        player.release()
        super.onCleared()
    }
}

private const val PLAY_LOG = "PulseDeck-play"

/**
 * Application-scoped playback owner. The service and MainActivity always
 * reference the same player; rotation cannot instantiate another ExoPlayer.
 * The process cleans up this bounded singleton on termination.
 */
object PlaybackControllerHolder {
    @Volatile private var active: PlaybackController? = null

    @Synchronized
    fun obtain(application: Application): PlaybackController =
        active ?: PlaybackController(application).also { active = it }
}
