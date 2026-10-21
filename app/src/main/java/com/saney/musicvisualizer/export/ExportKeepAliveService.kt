package com.saney.musicvisualizer.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.saney.musicvisualizer.MainActivity
import com.saney.musicvisualizer.R

/**
 * V2 / V107 (faric 21.10 03:32: «оставив конвертувати, пішов в другу програму, повернувся — його
 * викинуло»): while an MP4 export runs, PulseDeck is a foreground app for Android (a notification
 * «Експорт MP4» in the shade) and keeps the CPU awake. Without it Samsung's background cleaner
 * («AL_Kill», see PROJECT_LEDGER 18.10) closes the app within minutes of leaving it.
 *
 * The export itself still runs where it did (MainActivity's worker); this service only keeps the
 * process alive. Started by beginGuardedExport(), stopped by endGuardedExport().
 */
class ExportKeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: DEFAULT_TEXT
        val notification = buildNotification(this, text)
        val started =
            runCatching {
                if (Build.VERSION.SDK_INT >= 35) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        if (started.isFailure) {
            Log.e("FARIC-export", "Export keep-alive could not start", started.exceptionOrNull())
            stopSelf()
            return START_NOT_STICKY
        }
        if (wakeLock == null) {
            wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PulseDeck:export")
                    .apply {
                        setReferenceCounted(false)
                        acquire(MAX_WAKE_MS)
                    }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        // Android 15 limits long media processing (6 h a day) — stop politely, the export ends by itself.
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "pulsedeck_export"
        private const val NOTIFICATION_ID = 4107
        private const val EXTRA_TEXT = "text"
        private const val DEFAULT_TEXT = "Створюється відео. Можна перейти в іншу програму."
        private const val MAX_WAKE_MS = 6L * 60L * 60L * 1000L

        @Volatile
        private var running = false

        /** Start (or update the text of) the export notification. Never throws. */
        fun start(
            context: Context,
            text: String = DEFAULT_TEXT,
        ) {
            val app = context.applicationContext
            val result =
                runCatching {
                    ensureChannel(app)
                    val intent = Intent(app, ExportKeepAliveService::class.java).putExtra(EXTRA_TEXT, text)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        app.startForegroundService(intent)
                    } else {
                        app.startService(intent)
                    }
                }
            running = result.isSuccess
            if (result.isFailure) Log.e("FARIC-export", "Export keep-alive start failed", result.exceptionOrNull())
        }

        /** Change the shade text while the export runs (e.g. «на паузі»). */
        fun update(
            context: Context,
            text: String,
        ) {
            if (!running) return
            runCatching {
                val app = context.applicationContext
                ensureChannel(app)
                (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIFICATION_ID, buildNotification(app, text))
            }
        }

        fun stop(context: Context) {
            running = false
            runCatching {
                val app = context.applicationContext
                app.stopService(Intent(app, ExportKeepAliveService::class.java))
            }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Експорт відео", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Показується, поки PulseDeck створює MP4"
                    setShowBadge(false)
                },
            )
        }

        private fun buildNotification(
            context: Context,
            text: String,
        ): Notification {
            val open =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(context, CHANNEL_ID)
                } else {
                    @Suppress("DEPRECATION")
                    Notification.Builder(context)
                }
            return builder
                .setSmallIcon(R.drawable.media3_notification_small_icon)
                .setContentTitle("PulseDeck — експорт MP4")
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open)
                .build()
        }
    }
}
