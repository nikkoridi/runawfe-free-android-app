package ru.runa.wfe.notification

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.runa.wfe.R
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.notification.NotificationHelpers.NotificationType
import ru.runa.wfe.ui.notification.DurationPreference
import ru.runa.wfe.ui.notification.PermissionsConstants
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NotificationService : Service() {
    private lateinit var notificationHelpers: NotificationHelpers
    private lateinit var notificationLogic: NotificationLogic
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var thread: HandlerThread
    private lateinit var notificationServiceScope: CoroutineScope

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val permissionGranted = intent.getBooleanExtra(
                "permission_granted",
                false
            )
            if (permissionGranted) setNotifications()
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        notificationHelpers = NotificationHelpers(this)
        registerReceiver(
            permissionReceiver,
            IntentFilter(PermissionsConstants.ACTION_REQUEST_PERMISSION.actionName),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // DataStore uses Dispatchers.IO, this coroutine dispatcher won't change it
        CoroutineScope(Dispatchers.Default).launch {
            preferencesManager.getValueFlow(
                PreferencesManager.POLLING_INTERVAL,
                pollingInterval.toInt() / (1000 * 60)
            ).collect { value ->
                pollingInterval = (value * 1000 * 60).toLong()
                /*
                The value is nonzero initially (checked in MainActivity)
                stopSelf() should be called only after service start
                */
                if (pollingInterval == 0L) {
                    stopSelf()
                }
            }
        }
        notificationLogic = NotificationLogic(this, notificationHelpers, preferencesManager)
        notificationLogic.loadLastCheckData()
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
        unregisterReceiver(permissionReceiver)
        Log.i(this.javaClass.name, "Notification service destroyed")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        notificationLogic.saveLastCheckData(
            OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )
        stopSelf()
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
        val serviceChannel = notificationHelpers.getOrCreateChannel(
            1,
            NotificationType.DEFAULT,
            this.getString(R.string.notifications_settings),
            this.getString(R.string.notifications_service_message)
        )
        serviceChannel.setShowBadge(false)
        NotificationManagerCompat.from(this).createNotificationChannel(serviceChannel)
        startForeground(NOTIFICATION_SERVICE_ID, serviceStartNotification)

        notificationHelpers.createNotificationChannels()

        notificationServiceScope.launch {
            while (isActive) {
                val tasksJob = launch { notificationLogic.checkNewChatMessagesAndNotify() }
                val chatJob = launch { notificationLogic.checkNewTasksAndNotify() }
                tasksJob.join()
                chatJob.join()
                delay(pollingInterval)
            }
        }
    }

    companion object {
        const val NOTIFICATION_SERVICE_ID = 1
        private var pollingInterval: Long = DurationPreference.DEFAULT.toLong() * 60 * 1000
    }
}