package ru.runa.wfe.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.paging.PagingData
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.runa.wfe.MainActivity
import ru.runa.wfe.R
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.dto.WfChatRoom
import ru.runa.wfe.rest.dto.WfeChatMessage
import ru.runa.wfe.rest.dto.WfePagedList
import ru.runa.wfe.rest.dto.WfeTask
import ru.runa.wfe.ui.notification.PermissionsConstants
import java.util.Date

class NotificationService : Service() {
    private val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }
    private lateinit var prefs: SharedPreferences
    private lateinit var thread: HandlerThread
    private lateinit var notificationServiceScope: CoroutineScope

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val permissionGranted = intent.getBooleanExtra(
                "permission_granted",
                false)
            if (permissionGranted) setNotifications()
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        registerReceiver(permissionReceiver,
            IntentFilter(PermissionsConstants.ACTION_REQUEST_PERMISSION.actionName),
            Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!checkPermission()) {
            stopSelf()
            Log.e("NotificationsManager", "No required permission: "
                    + Manifest.permission.POST_NOTIFICATIONS)
            val permissionRequestIntent =
                Intent(PermissionsConstants.ACTION_REQUEST_PERMISSION.actionName).apply {
                putExtra("permission", Manifest.permission.POST_NOTIFICATIONS)
            }
            sendBroadcast(permissionRequestIntent)
            return START_NOT_STICKY
        }

        setNotifications()
        return START_STICKY
    }

    override fun onDestroy() {
        if (::notificationServiceScope.isInitialized) {
            notificationServiceScope.cancel()
        }
        thread.quitSafely()
        unregisterReceiver(permissionReceiver)
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun setNotifications() {
        thread = HandlerThread("notificationsCheck")
        thread.start()
        val handler = Handler(thread.looper)
        notificationServiceScope = CoroutineScope(handler.asCoroutineDispatcher())

        // Notify about service start
        val serviceStartNotification = NotificationCompat.Builder(this,
            NotificationType.DEFAULT.channelId)
            .setContentTitle(getString(R.string.notifications_service_title))
            .setContentText(getString(R.string.notifications_service_message))
            .build()
        val serviceChannel = createChannel(1,
            NotificationType.DEFAULT,
            this.getString(R.string.notifications_settings),
            this.getString(R.string.notifications_service_message))
        notificationManager.createNotificationChannel(serviceChannel)
        startForeground(1, serviceStartNotification)

        createNotificationChannels()

        val checkDelay: Long = prefs.getString("checkDelay",
            CHECK_INTERVAL.toString())?.toLong() ?: CHECK_INTERVAL
        if (checkDelay != CHECK_INTERVAL) {
            CHECK_INTERVAL = checkDelay
        }

        notificationServiceScope.launch {
            while (isActive) {
                checkNewChatMessages()
                checkNewTasks()
                delay(CHECK_INTERVAL)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            tasksChannel = createChannel(importance,
                NotificationType.TASK,
                this.getString(R.string.tasks_channel_title),
                this.getString(R.string.tasks_channel_description))

            messagesChannel = createChannel(importance,
                NotificationType.MESSAGE,
                this.getString(R.string.messages_channel_title),
                this.getString(R.string.messages_channel_description))

            notificationManager.createNotificationChannel(tasksChannel)
            notificationManager.createNotificationChannel(messagesChannel)
        }
    }

    private fun createChannel(importance: Int,
                              type: NotificationType,
                              title: String,
                              channelDescription: String): NotificationChannel {
        return NotificationChannel(
            type.channelId,
            title,
            importance
        ).apply {
            description = channelDescription
        }
    }

    private suspend fun checkNewChatMessages() {
        val chatRooms: Collection<WfChatRoom>? = ApiClient.chatService.getChatRooms().body()
        if (chatRooms != null) {
            val newMessages = ArrayList<WfeChatMessage>()
            for (room in chatRooms) {
                if (room.newMessagesCount > 0) {
                    val processId = room.getId()
                    val chatRoomMessages = ApiClient.chatService.getChatMessages(processId).body()
                    if (chatRoomMessages != null) {
                        val chat: Iterator<WfeChatMessage> = chatRoomMessages.iterator()
                        var readAllNew = false
                        while (!readAllNew && chat.hasNext()) {
                            val message = chat.next()
                            if (message.createDate.compareTo(lastChatsCheck) >= 0) {
                                newMessages.add(message)
                            }
                            else {
                                readAllNew = true
                            }
                        }
                    }
                }
            }
            newChatMessagesNotification(newMessages)
        }
        lastChatsCheck = Date()
    }

    private fun newChatMessagesNotification(newMessages: ArrayList<WfeChatMessage>) {
        if (newMessages.size > 1) {
            val notificationMessageText = StringBuilder().apply {
                for (newMessage in newMessages) {
                    append("${newMessage.author}: ${newMessage.text}\n")
                }
            }
            showNotification(
                "${newMessages.size} ${this.getString(R.string.new_data_notifications)} ${resources.getQuantityString(R.plurals.tasks_count, newMessages.size)}",
                notificationMessageText.toString(),
                NotificationType.MESSAGE
            )
        }
    }

    private suspend fun checkNewTasks() {
        val newTasks = ArrayList<WfeTask>()
        val tasks: WfePagedList<WfeTask>? = ApiClient.taskService.getMyTasks(
            PagingData.from(
                newTasks
            )
        ).body()
        if (tasks != null && tasks.data.isNotEmpty()) {
            for (task in tasks.data) {
                if (task.createDate.compareTo(lastTasksCheck) >= 0) {
                    newTasks.add(task)
                }
            }
            newMessagesNotification(newTasks)
        }
        lastTasksCheck = Date()
    }

    private fun newMessagesNotification(newTasks: ArrayList<WfeTask>) {
        if (newTasks.size > 1) {
            val notificationMessageText = StringBuilder().apply {
                for (newTask in newTasks) {
                    append("${newTask.name}\n")
                }
            }
            showNotification(
                "${newTasks.size} ${this.getString(R.string.new_data_notifications)} ${resources.getQuantityString(R.plurals.tasks_count, newTasks.size)}",
                notificationMessageText.toString(),
                NotificationType.TASK
            )
        } else if (newTasks.size > 0) {
            showNotification(newTasks[0].name, newTasks[0].description,  NotificationType.TASK)
        }
    }

    private fun showNotification(title: String, message: String, type: NotificationType) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notificationBuilder =  NotificationCompat.Builder(this, type.channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        val notification = notificationBuilder.build()

        // Android 13 (API level 33) and higher requires a permission
        if (checkPermission()) {
            notificationManager.notify(
                NOTIFICATION_ID + 1,
                notification)
            NOTIFICATION_ID += 1
        }
    }

    private fun checkPermission(): Boolean {
        // Is true when
        // * Current API level is higher than 33 (Android 13) and permission granted
        // * Or the Android version is lower and there's no need in permissions
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)
    }

    companion object {
        private var CHECK_INTERVAL: Long = 2*60*1000
        private var NOTIFICATION_ID = 1
        private lateinit var tasksChannel: NotificationChannel
        private lateinit var messagesChannel: NotificationChannel
        private var lastTasksCheck: Date = Date()
        private var lastChatsCheck: Date = Date()
    }
}

enum class NotificationType(val channelId: String, val soundKey: String) {
    DEFAULT("ru.runa.wfe.notifications", ""),
    TASK("ru.runa.wfe.notifications.tasks", "tasksSound"),
    MESSAGE("ru.runa.wfe.notifications.messages", "messagesSound")
}
