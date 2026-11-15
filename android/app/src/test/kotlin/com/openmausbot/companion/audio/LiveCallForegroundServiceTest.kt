package com.openmausbot.companion.audio

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.openmausbot.companion.R
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Runtime checks for the actual microphone service. Pure manager tests alone
 * cannot prove the notification, stop action, or no-call safety gate.
 * Real Android background execution and mic privacy need device testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveCallForegroundServiceTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @After
    fun resetServiceFlags() {
        LiveCallForegroundState.active = false
        LiveCallForegroundState.stopRequested = false
    }

    @Test
    fun serviceCreatesDedicatedNotificationChannel() {
        Robolectric.buildService(LiveCallForegroundService::class.java).create()
        val notifications = context.getSystemService(NotificationManager::class.java)
        val channel = notifications.getNotificationChannel(LiveCallForegroundService.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
    }

    @Test
    fun serviceRefusesToPromoteWhenNoLiveCallExists() {
        val service = Robolectric.buildService(LiveCallForegroundService::class.java).create().get()
        val intent = Intent(context, LiveCallForegroundService::class.java)
        val result = service.onStartCommand(intent, 0, 1)
        assertEquals(Service.START_NOT_STICKY, result)
        assertNull(shadowOf(service).lastForegroundNotification)
        assertFalse(LiveCallForegroundState.active)
        assertNull(service.onBind(intent))
    }

    @Test
    fun serviceStartAndStopUseExplicitComponentAndGate() {
        LiveCallForegroundService.stop(context)
        assertTrue(LiveCallForegroundState.stopRequested)
        LiveCallForegroundService.start(context)
        assertFalse(LiveCallForegroundState.stopRequested)
        val started = shadowOf(RuntimeEnvironment.getApplication()).peekNextStartedService()
        assertNotNull(started)
        assertEquals(LiveCallForegroundService::class.java.name, started.component?.className)
        LiveCallForegroundService.stop(context)
        assertTrue(LiveCallForegroundState.stopRequested)
        val stopped = shadowOf(RuntimeEnvironment.getApplication()).nextStoppedService
        assertEquals(LiveCallForegroundService::class.java.name, stopped.component?.className)
    }

    @Test
    fun ongoingNotificationExposesExplicitHangUpControl() {
        val service = Robolectric.buildService(LiveCallForegroundService::class.java).create().get()
        val method = LiveCallForegroundService::class.java.getDeclaredMethod("notification")
        method.isAccessible = true
        val notification = method.invoke(service) as Notification
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(1, NotificationCompat.getActionCount(notification))
        val action = NotificationCompat.getAction(notification, 0)
        assertNotNull(action)
        assertEquals(context.getString(R.string.live_call_hang_up), action.title.toString())
        val intent = shadowOf(action.actionIntent).savedIntent
        assertEquals(LiveCallForegroundService::class.java.name, intent.component?.className)
        assertEquals("com.openmausbot.companion.action.HANG_UP_LIVE", intent.action)
        assertNotNull(notification.contentIntent)
    }
}
