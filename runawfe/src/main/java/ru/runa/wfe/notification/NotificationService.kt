package ru.runa.wfe.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.runa.wfe.MainActivity
import ru.runa.wfe.R
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.dto.WfChatRoom
import ru.runa.wfe.rest.dto.WfeChatMessage
import ru.runa.wfe.rest.dto.WfePagedList
import ru.runa.wfe.rest.dto.WfeTask
import ru.runa.wfe.ui.notification.NotificationPermissionActivity
import java.util.Date

class NotificationService : Service() {
    private val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannels()
        val delay: Long = prefs.getString("checkDelay",
            CHECK_INTERVAL.toString())?.toLong() ?: CHECK_INTERVAL
        if (delay != CHECK_INTERVAL) {
            CHECK_INTERVAL = delay
        }
        notificationServiceScope.launch {
            checkNewChatMessages()
            checkNewTasks()
            delay(CHECK_INTERVAL)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        notificationServiceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

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
                "${newMessages.size} ${this.getString(R.string.several_messages_notification_title)}",
                notificationMessageText.toString(),
                NotificationType.MESSAGE
            )
        } else if (newMessages.size > 0) {
            showNotification(
                this.getString(R.string.new_chat_message_title),
                "${newMessages[0].author}: ${newMessages[0].text}",
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
                "${newTasks.size} ${this.getString(R.string.several_tasks_notification_title)}",
                notificationMessageText.toString(),
                NotificationType.TASK
            )
        } else if (newTasks.size > 0) {
            showNotification(newTasks[0].name, newTasks[0].description,  NotificationType.TASK)
        }
    }

    private fun getCustomNotificationSound(builder: NotificationCompat.Builder, type: NotificationType) {
        val notificationSound = prefs.getString(type.soundKey, null)
        if (notificationSound != null) {
            val soundUri = Uri.parse(notificationSound)
            builder.setSound(soundUri)
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
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                notificationManager.notify(
                    NOTIFICATION_ID + 1,
                    notification)
                NOTIFICATION_ID += 1
            }
            else -> {
                requestPermissionFromActivity()
            }
        }
    }

    private fun requestPermissionFromActivity() {
        val intent = Intent(this, NotificationPermissionActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        startActivity(intent)
    }

    companion object {
        private val notificationServiceScope = CoroutineScope(Dispatchers.IO)
        private var CHECK_INTERVAL: Long = 2*60*1000
        private var NOTIFICATION_ID = 1
        private lateinit var tasksChannel: NotificationChannel
        private lateinit var messagesChannel: NotificationChannel
        private var lastTasksCheck: Date = Date()
        private var lastChatsCheck: Date = Date()
    }
}

enum class NotificationType(val channelId: String, val soundKey: String) {
    TASK("ru.runa.wfe.notifications.tasks", "tasksSound"),
    MESSAGE("ru.runa.wfe.notifications.messages", "messagesSound")
}
