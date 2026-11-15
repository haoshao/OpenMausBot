package com.openmausbot.companion.audio

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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.openmausbot.companion.MainActivity
import com.openmausbot.companion.OpenMausApp
import com.openmausbot.companion.R

/** A Live call requires a microphone-type foreground service, not a timer or an Activity. */
internal interface LiveCallForeground {
    fun start()
    fun stop()
}

internal object NoopLiveCallForeground : LiveCallForeground {
    override fun start() = Unit
    override fun stop() = Unit
}

internal class AndroidLiveCallForeground(private val context: Context) : LiveCallForeground {
    override fun start() = LiveCallForegroundService.start(context)
    override fun stop() = LiveCallForegroundService.stop(context)
}

/** Shared with SessionLingerController: keep the companion stream alive while the call is foreground-serviced. */
internal object LiveCallForegroundState {
    @Volatile var active: Boolean = false
    @Volatile var stopRequested: Boolean = false
}

/**
 * Holds the already-authorized, user-started microphone call after Home or screen lock.
 * It is NOT sticky: a restarted process does not own the old WebRTC session.
 * A persistent notification keeps the microphone use visible and offers Hang up.
 */
class LiveCallForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_live_call),
                NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notification_channel_live_call_desc)
                setShowBadge(false)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as OpenMausApp
        if (intent?.action == ACTION_HANG_UP) {
            app.liveCalls.hangUp()
            stopSelf()
            return START_NOT_STICKY
        }
        // A pending start can be cancelled while the OS is still scheduling this Service.
        if (!app.liveCalls.state.value.holdsMedia || LiveCallForegroundState.stopRequested) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
            LiveCallForegroundState.active = true
            // Close a 25-second linger window which may have started before the
            // service became foreground, otherwise its timer would disconnect the Live call.
            app.linger.onAlwaysOnStarted()
            app.session.connect()
        } catch (_: RuntimeException) {
            // No background microphone without the required foreground notification.
            app.liveCalls.foregroundUnavailable()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        val wasActive = LiveCallForegroundState.active
        val requested = LiveCallForegroundState.stopRequested
        LiveCallForegroundState.active = false
        val app = application as OpenMausApp
        // If Android removed the service instead of the user hanging up,
        // release the WebRTC microphone rather than recording without a notification.
        if (wasActive && !requested) app.liveCalls.foregroundUnavailable()
        app.linger.onExternalForegroundStopped()
        super.onDestroy()
    }

    private fun notification(): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }, flags,
        )
        val hangUp = PendingIntent.getService(
            this, 1, Intent(this, LiveCallForegroundService::class.java).setAction(ACTION_HANG_UP), flags,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_maus_mark)
            .setContentTitle(getString(R.string.live_call_notification_title))
            .setContentText(getString(R.string.live_call_notification_text))
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.live_call_hang_up), hangUp)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "live_call_microphone"
        const val NOTIFICATION_ID = 4202
        private const val ACTION_HANG_UP = "com.openmausbot.companion.action.HANG_UP_LIVE"

        fun start(context: Context) {
            LiveCallForegroundState.stopRequested = false
            ContextCompat.startForegroundService(
                context, Intent(context, LiveCallForegroundService::class.java),
            )
        }

        fun stop(context: Context) {
            LiveCallForegroundState.stopRequested = true
            context.stopService(Intent(context, LiveCallForegroundService::class.java))
        }
    }
}
