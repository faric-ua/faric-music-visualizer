package com.saney.musicvisualizer.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.saney.musicvisualizer.MainActivity

/**
 * One MediaSession and ONE shared ExoPlayer for in-app, lockscreen and
 * notification transport. No duplicate player, queue or audio processor.
 */
@UnstableApi
class PlaybackMediaSessionService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val openPlayer = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_OPEN_SCREEN, "NOW_PLAYING")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val intent = PendingIntent.getActivity(
            this, 1401, openPlayer,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val sharedEngine = PlaybackControllerHolder.obtain(application)
        session = MediaSession.Builder(this, sharedEngine.sessionPlayer())
            .setSessionActivity(intent)
            .build()
        // We explicitly start this service on queue/play, not exclusively via
        // MediaController binding, so register the session immediately.
        session?.let(::addSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session

    override fun onDestroy() {
        session?.release()
        session = null
        // Engine is application-scoped and may still be used by the visible
        // Activity after this stopped background service is recreated.
        super.onDestroy()
    }

    companion object {
        fun ensureStarted(context: Context) {
            // Called only on a user-initiated queue or transport action. Do
            // not repeatedly spin up foreground services from background jobs.
            context.startService(Intent(context, PlaybackMediaSessionService::class.java))
        }
    }
}
