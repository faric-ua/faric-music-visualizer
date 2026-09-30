package com.saney.musicvisualizer.playback

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.MediaItem
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

data class PlaybackSnapshot(
    val trackName: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val analysisActive: Boolean = false,
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
        ).build()
    private var analyzer: AudioCaptureAnalyzer? = null
    private var currentSessionId = C.AUDIO_SESSION_ID_UNSET
    private var analysisPermissionGranted = false
    private var trackName: String? = null
    private var trackUri: Uri? = null
    private var status = "Оберіть локальний аудіофайл"
    private var analysisActive = false
    var listener: Listener? = null

    private val progressTicker = object : Runnable {
        override fun run() {
            emitCurrentState()
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
                status = if (isPlaying) {
                    "Відтворення"
                } else if (trackName == null) {
                    "Оберіть локальний аудіофайл"
                } else {
                    "Пауза"
                }
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
                status = "Помилка відтворення: ${error.errorCodeName}"
                emitCurrentState()
            }
        })
        mainHandler.post(progressTicker)
    }

    fun load(uri: Uri, displayName: String) {
        trackName = displayName
        trackUri = uri
        status = "Завантаження…"
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        emitCurrentState()
    }

    fun currentTrackUri(): Uri? =
        trackUri

    fun play() {
        if (trackName != null) player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration
        if (duration != C.TIME_UNSET && duration > 0L) {
            player.seekTo(positionMs.coerceIn(0L, duration))
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
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration.coerceAtLeast(0L),
                analysisActive = analysisActive,
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
