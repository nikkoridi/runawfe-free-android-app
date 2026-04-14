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
        // DataStore uses Dispatchers.IO, this coroutine dispatcher won't change it
        CoroutineScope(Dispatchers.Default).launch {
            preferencesManager.getValueFlow(
                PreferencesManager.CHECK_DELAY,
                CHECK_INTERVAL.toInt() / (1000 * 60)
            ).collect { value ->
                CHECK_INTERVAL = (value * 1000 * 60).toLong()
                /*
                The value is nonzero initially (checked in MainActivity)
                stopSelf() should be called only after service start
                */
                if (CHECK_INTERVAL == 0L) {
                    stopSelf()
                }
            }
        }

        val lastCheck = OffsetDateTime.parse(
            preferencesManager.getValue(PreferencesManager.LAST_CHECK,
                    OffsetDateTime.now(ZoneOffset.UTC).toString())
        )
            .withOffsetSameLocal(ZoneOffset.UTC)
        lastTasksCheck = lastCheck
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
                for (room in chatRooms) {
                    val newMessagesCount = room.newMessagesCount?.toInt() ?: 0
                    if (newMessagesCount > 0) {
                        val chatRoomMessages =
                            room.id?.let {
                                ApiClient.chatService.getChatMessagesUsingGET(it).body()
                            }
                        chatRoomMessages?.let {
                            newChatMessagesNotification(room.id, it.subList(0, newMessagesCount))
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(this::class.simpleName, ex.message.toString())
        }
    }

    private fun newChatMessagesNotification(
        roomId: Long?,
        newMessages: List<MessageAddedBroadcast>
    ) {
        if (newMessages.isNotEmpty()) {
            val title: String
            val notificationMessage: String
            val basicTitle = if (roomId != null) {
                "${getString(R.string.messages_chat_id_template, roomId)}:"
            } else {
                this.getString(R.string.new_data_notifications)
            }

            if (newMessages.size > 1) {
                title = "$basicTitle ${
                    resources.getQuantityString(
                        R.plurals.messages_count,
                        newMessages.size,
                        newMessages.size
                    )
                }"
                notificationMessage = StringBuilder().apply {
                    for (newMessage in newMessages) {
                        appendLine("${newMessage.author?.name}: ${newMessage.text}")
                    }
                }.toString()
            } else {
                title = basicTitle
                notificationMessage = newMessages[0].text.toString()
            }

            notificationLogic.showNotification(
                title,
                notificationMessage,
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
                newTasksNotification(newTasks)
                lastTasksCheck = OffsetDateTime.now(ZoneOffset.UTC)
            }
        } catch (ex: Exception) {
            Log.e(this::class.simpleName, ex.message.toString())
        }
    }

    private fun newTasksNotification(newTasks: List<WfeTask>) {
        if (newTasks.isNotEmpty()) {
            val title = "${this.getString(R.string.new_data_notifications)} ${
                resources.getQuantityString(
                    R.plurals.tasks_count,
                    newTasks.size,
                    newTasks.size
                )
            }"

            val notificationMessage: String = if (newTasks.size > 1) {
                StringBuilder().apply {
                    for (newTask in newTasks) {
                        appendLine("${newTask.name}")
                    }
                }.toString()
            } else {
                newTasks[0].name.toString()
            }

            notificationLogic.showNotification(
                title,
                notificationMessage,
                NotificationType.TASK
            )
        }
    }

    companion object {
        private var CHECK_INTERVAL: Long = 3 * 60 * 1000
        private var lastTasksCheck: OffsetDateTime = OffsetDateTime.now()
    }
}

