package com.captain.kplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.media3.exoplayer.ExoPlayer
import com.captain.kplayer.MainActivity

/**
 * Foreground service (mediaPlayback type) that keeps audio playing when
 * the app is backgrounded. It does NOT own the player: the single
 * ExoPlayer instance lives in [com.captain.kplayer.viewmodel.PlayerViewModel],
 * which publishes it here via [setPlayer] when the player is built and
 * clears it (null) when the ViewModel is cleared. The service only
 * controls that shared instance through its Play/Pause/Stop notification
 * actions — it never creates, prepares or releases a player itself.
 *
 * OUT OF SCOPE (deliberate): there is no MediaSession /
 * MediaSessionService here, so there are NO lock-screen media controls
 * and no Bluetooth / headset / Android Auto integration — only this
 * notification's actions. Adding those needs a MediaSessionService
 * refactor, not a change to this file.
 *
 * Lifecycle: [PlayerViewModel] calls [start] when playback starts with
 * background play enabled, and [stop] when playback pauses or stops.
 * The service itself also stops when its Stop action is tapped or when
 * the player reference is cleared. Notification channel importance is
 * LOW so background playback does not buzz the user.
 *
 * NOTE: uses only framework android.app.Notification APIs on purpose —
 * the app declares no androidx.core dependency, so NotificationCompat is
 * not available here.
 */
class PlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "kplayer_playback"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_PLAY = "com.captain.kplayer.action.PLAY"
        private const val ACTION_PAUSE = "com.captain.kplayer.action.PAUSE"
        private const val ACTION_STOP = "com.captain.kplayer.action.STOP"

        /**
         * The active player, published by PlayerViewModel. Volatile
         * because it is written from the ViewModel and read on the
         * service's main thread. Null when no player exists.
         */
        @Volatile
        var player: ExoPlayer? = null
            private set

        /** Called by PlayerViewModel after buildPlayer / on player release. */
        fun setPlayer(exoPlayer: ExoPlayer?) {
            player = exoPlayer
        }

        /**
         * Starts the service in the foreground. Safe to call while it is
         * already running (re-delivers onStartCommand and refreshes the
         * notification). On Android 12+ a foreground-service start from
         * the background can be refused by the OS; that throws here, so
         * it is swallowed — background audio then simply has no service
         * and the process may be killed sooner, instead of crashing.
         */
        fun start(context: Context) {
            try {
                context.startForegroundService(
                    Intent(context, PlaybackService::class.java)
                )
            } catch (_: Exception) {
                // ForegroundServiceStartNotAllowedException (API 31+) or
                // similar: no service, no crash. See class doc.
            }
        }

        /** Stops the service (and removes its notification). No-op if not running. */
        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> player?.play()
            ACTION_PAUSE -> player?.pause()
            ACTION_STOP -> {
                // NOTE: pausing directly here does not save the resume
                // position (that lives in PlayerViewModel.togglePlay /
                // pause); the position is re-saved on the next
                // onLeavePlayer / onCleared instead.
                player?.pause()
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
        }
        val currentPlayer = player
        if (currentPlayer == null) {
            // Nothing to keep alive — never sit foreground with no player.
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundWithType(buildNotification(currentPlayer))
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps audio playing when K-Player is in the background"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(currentPlayer: ExoPlayer): Notification {
        val isPlaying = currentPlayer.isPlaying
        val title = currentPlayer.currentMediaItem?.mediaMetadata?.title?.toString()
            ?: "K-Player"
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleAction = if (isPlaying) {
            Notification.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                actionIntent(ACTION_PAUSE, 1)
            ).build()
        } else {
            Notification.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play",
                actionIntent(ACTION_PLAY, 1)
            ).build()
        }
        val stopAction = Notification.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            actionIntent(ACTION_STOP, 2)
        ).build()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(toggleAction)
            .addAction(stopAction)
            .setStyle(
                Notification.MediaStyle().setShowActionsInCompactView(0, 1)
            )
            .build()
    }
}
