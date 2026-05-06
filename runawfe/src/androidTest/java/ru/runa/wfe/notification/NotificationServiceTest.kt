package ru.runa.wfe.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.runa.wfe.R
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.ServerCheckResult

class NotificationServiceTest {
    private lateinit var context: Context
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @get:Rule
    val foregroundPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.FOREGROUND_SERVICE)

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        ApiClient.setServerUrl(ServerCheckResult.Valid(ApiClient.DEFAULT_SERVER_URL))
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
    }

    @Test
    fun serviceStartAndNotificationTest() {
        val intent = Intent(context, NotificationService::class.java)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        val serviceStartDeadline = SystemClock.uptimeMillis() + 60 * 1000
        var started = false

        while (SystemClock.uptimeMillis() < serviceStartDeadline) {
            if (notificationManager.activeNotifications.any {
                    it.id == NotificationService.NOTIFICATION_SERVICE_ID
                }) {
                started = true
                break
            }
            Thread.sleep(50)
        }

        assertTrue("Foreground service didn't post a notification", started)

        // Get active notifications list
        val activeNotifications = notificationManager.activeNotifications
        assertTrue(activeNotifications.isNotEmpty())

        // Find and check service's notification text
        val foundServiceNotification = activeNotifications.any { sbn ->
            NotificationService.NOTIFICATION_SERVICE_ID == sbn.id ||
                    context.getString(R.string.notifications_settings) == sbn.notification
                .extras.getString(Notification.EXTRA_TITLE)
        }
        assertTrue(foundServiceNotification)

        context.stopService(intent)
    }
}