package ru.runa.wfe.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                    description = R.string.notification_channel_description.toString()
                }
            notificationManager.createNotificationChannel(channel)
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
                        for (message in chatRoomMessages) {
                            if (message.createDate!! >= lastChatsCheck) {
                                newMessages.add(message)
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
                "${newMessages.size} ${R.string.several_messages_notification_title}",
                notificationMessageText.toString()
            )
        } else if (newMessages.size > 0) {
            showNotification(
                R.string.new_chat_message_title.toString(),
                "${newMessages[0].author}: ${newMessages[0].text}"
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
                if (task.createDate >= lastTasksCheck) {
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
                "${newTasks.size} ${R.string.several_tasks_notification_title}",
                notificationMessageText.toString()
            )
        } else if (newTasks.size > 0) {
            showNotification(newTasks[0].name, newTasks[0].description)
        }
    }


    private fun showNotification(title: String, message: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        // Android 13 (API level 33) and higher requires a permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                notificationManager.notify(
                    NOTIFICATION_ID +1,
                    notification)
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
        private const val CHECK_INTERVAL: Long = 2*60*1000
        private const val CHANNEL_ID = "ru.runa.wfe.notifications"
        private const val CHANNEL_NAME = "RunaWfeNotifications"
        private const val NOTIFICATION_ID = 1
        private lateinit var channel: NotificationChannel
        private var lastTasksCheck: Date = Date()
        private var lastChatsCheck: Date = Date()
    }
}