package ru.runa.wfe.notification

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.runa.wfe.R
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.notification.NotificationHelpers.NotificationType
import ru.runa.wfe.ui.notification.DurationPreference
import java.time.OffsetDateTime
import java.time.ZoneOffset

class NotificationService : Service() {
    private lateinit var notificationHelpers: NotificationHelpers
    private lateinit var notificationLogic: NotificationLogic
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var thread: HandlerThread
    private lateinit var notificationServiceScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        notificationHelpers = NotificationHelpers(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // DataStore uses Dispatchers.IO, this coroutine dispatcher won't change it
        CoroutineScope(Dispatchers.Default).launch {
            preferencesManager.getValueFlow(
                PreferencesManager.POLLING_INTERVAL,
                pollingInterval
            ).collect { value ->
                pollingInterval = value
                /*
                The value is nonzero initially (checked in MainActivity)
                stopSelf() should be called only after service start
                */
                if (pollingInterval == 0L) {
                    stopSelf()
                }
            }
        }

        val lastCheck = OffsetDateTime.parse(
            preferencesManager.getValue(
                PreferencesManager.LAST_CHECK,
                OffsetDateTime.now(ZoneOffset.UTC).toString()
            )
        )
            .withOffsetSameLocal(ZoneOffset.UTC)
        notificationLogic = NotificationLogic(this, notificationHelpers, lastCheck)

        setNotifications()
        return START_STICKY
    }

    override fun onDestroy() {
        if (this::notificationServiceScope.isInitialized) {
            notificationServiceScope.cancel()
        }
        if (this::thread.isInitialized) {
            thread.quitSafely()
        }
        Log.i(this.javaClass.name, "Notification service destroyed")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            preferencesManager.setKey(
                PreferencesManager.LAST_CHECK,
                notificationLogic.lastTasksCheck.toString()
            )
        }
        Log.i(this.javaClass.name, "Notification service task removed")
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun setNotifications() {
        thread = HandlerThread("notificationsCheck")
        thread.start()
        val handler = Handler(thread.looper)
        notificationServiceScope = CoroutineScope(handler.asCoroutineDispatcher())

        // Notify about service start
        val serviceStartNotification = NotificationCompat.Builder(
            this,
            NotificationType.DEFAULT.channelId
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notifications_service_title))
            .setContentText(getString(R.string.notifications_service_message))
            .build()
        val serviceChannel = notificationHelpers.getOrCreateChannel(NotificationType.DEFAULT)
        serviceChannel.setShowBadge(false)
        NotificationManagerCompat.from(this).createNotificationChannel(serviceChannel)
        startForeground(NOTIFICATION_SERVICE_ID, serviceStartNotification)

        notificationHelpers.createNotificationChannels()

        // Main polling loop for notifications
        notificationServiceScope.launch {
            while (isActive) {
                val tasksJob = if (NotificationHelpers.isChannelEnabled(NotificationType.TASK, this@NotificationService)) {
                    launch { notificationLogic.checkNewTasksAndNotify() }
                } else null
                val chatJob = if (NotificationHelpers.isChannelEnabled(NotificationType.MESSAGE, this@NotificationService)) {
                    launch { notificationLogic.checkNewChatMessagesAndNotify() }
                } else null
                tasksJob?.join()
                chatJob?.join()
                delay(pollingInterval)
            }
        }
    }

    companion object {
        const val NOTIFICATION_SERVICE_ID = 1
        private var pollingInterval: Long = DurationPreference.DEFAULT * 60 * 1000
    }
}