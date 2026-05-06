package ru.runa.wfe.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import ru.runa.wfe.notification.NotificationHelpers.NotificationType

class NotificationHelpersTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var helpers: NotificationHelpers
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val type = NotificationType.DEFAULT
    @get:Rule
    val notificationPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun setUp() {
        helpers = NotificationHelpers(context)
    }

    @After
    fun cleanUp() {
        NotificationType.entries.forEach { type ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.deleteNotificationChannel(type.channelId)
            }
        }
    }

    @Test
    fun isChannelEnabled_PermissionGranted() {
        assertTrue(helpers.isChannelEnabled(type))
    }

    @Test
    fun getOrCreateChannel_CreateAndCheckExistenceChannel() {
        val importance = 3
        val channel = helpers.getOrCreateChannel(type, importance)

        assertNotNull("Channel ${type.channelId} should exist after getOrCreateChannel() call",
            channel)
        assertEquals(channel.importance, importance)
        assertEquals(channel.id, type.channelId)
        assertEquals(channel.name, context.getString(type.titleResId))
        assertEquals(channel.description, context.getString(type.descriptionResId))

        val getCreatedChannel = helpers.getOrCreateChannel(type, importance)
        assertEquals(getCreatedChannel, channel)
    }

    @Test
    fun createNotificationChannels_CheckAll() {
        helpers.createNotificationChannels()
        NotificationType.entries.forEach { type ->
            val channel = notificationManager.getNotificationChannel(type.channelId)
            assertNotNull("Channel ${type.channelId} should exist", channel)
            assertEquals(type.channelId, channel.id)
            assertEquals(context.getString(type.titleResId), channel.name)
            assertEquals(context.getString(type.descriptionResId), channel.description)
        }
    }
}