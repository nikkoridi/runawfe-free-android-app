package ru.runa.wfe.notification

import android.Manifest
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
import ru.runa.wfe.notification.NotificationLogic.NotificationType
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.restapi.model.MessageAddedBroadcast
import ru.runa.wfe.restapi.model.WfChatRoom
import ru.runa.wfe.restapi.model.WfePagedListFilter
import ru.runa.wfe.restapi.model.WfePagedListOfWfeTask
import ru.runa.wfe.restapi.model.WfeTask
import ru.runa.wfe.ui.notification.PermissionsConstants
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NotificationService : Service() {
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
        notificationLogic = NotificationLogic(this)
        registerReceiver(
            permissionReceiver,
            IntentFilter(PermissionsConstants.ACTION_REQUEST_PERMISSION.actionName),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val minutesCheckInterval: Int = CHECK_INTERVAL.toInt() / (1000 * 60)
        val checkDelay: Int = preferencesManager.getValue(
            PreferencesManager.CHECK_DELAY, minutesCheckInterval
        )
        if (checkDelay != minutesCheckInterval) {
            CHECK_INTERVAL = (checkDelay * 1000 * 60).toLong()
        }

        if (!notificationLogic.checkPermission() || checkDelay == 0) {
            stopSelf()
            Log.e(
                "NotificationsManager", "No required permission: "
                        + Manifest.permission.POST_NOTIFICATIONS
            )
            val permissionRequestIntent =
                Intent(PermissionsConstants.ACTION_REQUEST_PERMISSION.actionName).apply {
                    putExtra("permission", Manifest.permission.POST_NOTIFICATIONS)
                }
            sendBroadcast(permissionRequestIntent)
            return START_NOT_STICKY
        }
        val lastCheck = OffsetDateTime.parse(
            preferencesManager.getValue(PreferencesManager.LAST_CHECK,
                    OffsetDateTime.now(ZoneOffset.UTC).toString())
        )
            .withOffsetSameLocal(ZoneOffset.UTC)
        lastTasksCheck = lastCheck
        lastChatsCheck = lastCheck

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
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveLastCheckData()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun saveLastCheckData() {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesManager.setKey(
                PreferencesManager.LAST_CHECK,
                OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )
        }
    }

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
        val serviceChannel = notificationLogic.getOrCreateChannel(
            1,
            NotificationType.DEFAULT,
            this.getString(R.string.notifications_settings),
            this.getString(R.string.notifications_service_message)
        )
        NotificationManagerCompat.from(this).createNotificationChannel(serviceChannel)
        startForeground(1, serviceStartNotification)

        notificationLogic.createNotificationChannels()

        notificationServiceScope.launch {
            while (isActive) {
                checkNewChatMessages()
                checkNewTasks()
                saveLastCheckData()
                delay(CHECK_INTERVAL)
            }
        }
    }

    private suspend fun checkNewChatMessages() {
        try {
            val chatRooms: List<WfChatRoom>? = ApiClient.chatService.getChatRoomsUsingGET().body()
            if (chatRooms != null) {
                val newMessages = ArrayList<MessageAddedBroadcast>()
                for (room in chatRooms) {
                    if ((room.newMessagesCount ?: 0) > 0) {
                        val chatRoomMessages =
                            room.id?.let {
                                ApiClient.chatService.getChatMessagesUsingGET(it).body()
                            }
                        if (chatRoomMessages != null) {
                            val chat: Iterator<MessageAddedBroadcast> = chatRoomMessages.iterator()
                            var readAllNew = false
                            while (!readAllNew && chat.hasNext()) {
                                val message = chat.next()
                                // New variable because smartcast won't work with custom getter
                                val createDate = message.createDate
                                if (createDate != null
                                    && lastChatsCheck.isBefore(createDate)
                                ) {
                                    newMessages.add(message)
                                } else {
                                    readAllNew = true
                                }
                            }
                        }
                    }
                }
                newChatMessagesNotification(newMessages)
            }
            lastChatsCheck = OffsetDateTime.now(ZoneOffset.UTC)
        } catch (ex: Exception) {
            Log.e(this::class.simpleName, ex.message.toString())
        }
    }

    private fun newChatMessagesNotification(newMessages: ArrayList<MessageAddedBroadcast>) {
        if (newMessages.size > 1) {
            val notificationMessageText = StringBuilder().apply {
                for (newMessage in newMessages) {
                    append("${newMessage.author?.name}: ${newMessage.text}\n")
                }
            }
            notificationLogic.showNotification(
                "${this.getString(R.string.new_data_notifications)} ${
                    resources.getQuantityString(
                        R.plurals.messages_count,
                        newMessages.size,
                        newMessages.size
                    )
                }",
                notificationMessageText.toString(),
                NotificationType.MESSAGE
            )
        }
    }

    private suspend fun checkNewTasks() {
        val newTasks = ArrayList<WfeTask>()
        try {
            val tasks: WfePagedListOfWfeTask? = ApiClient.taskService.getMyTasksUsingPOST(
                WfePagedListFilter()
            ).body()
            if (tasks?.data != null) {
                for (task in tasks.data) {
                    // New variable because smartcast won't work with custom getter
                    val assignDate = task.assignDate
                    if (assignDate != null &&
                        lastTasksCheck.isBefore(assignDate)
                    ) {
                        newTasks.add(task)
                    }
                }
                newMessagesNotification(newTasks)
                lastTasksCheck = OffsetDateTime.now(ZoneOffset.UTC)
            }
        } catch (ex: Exception) {
            Log.e(this::class.simpleName, ex.message.toString())
        }
    }

    private fun newMessagesNotification(newTasks: ArrayList<WfeTask>) {
        if (newTasks.size > 1) {
            val notificationMessageText = StringBuilder().apply {
                for (newTask in newTasks) {
                    append("${newTask.name}\n")
                }
            }
            notificationLogic.showNotification(
                "${this.getString(R.string.new_data_notifications)} ${
                    resources.getQuantityString(
                        R.plurals.tasks_count,
                        newTasks.size,
                        newTasks.size
                    )
                }",
                notificationMessageText.toString(),
                NotificationType.TASK
            )
        } else if (newTasks.size > 0) {
            notificationLogic.showNotification(
                newTasks[0].name.toString(),
                newTasks[0].description.toString(),
                NotificationType.TASK
            )
        }
    }

    companion object {
        private var CHECK_INTERVAL: Long = 3 * 60 * 1000
        private var lastTasksCheck: OffsetDateTime = OffsetDateTime.now()
        private var lastChatsCheck: OffsetDateTime = OffsetDateTime.now()
    }
}

