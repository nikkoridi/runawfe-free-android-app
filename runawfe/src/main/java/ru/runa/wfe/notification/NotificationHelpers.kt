package ru.runa.wfe.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.runa.wfe.MainActivity
import ru.runa.wfe.R

class NotificationLogic(val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            tasksChannel = getOrCreateChannel(
                importance,
                NotificationType.TASK,
                context.getString(R.string.tasks_channel_title),
                context.getString(R.string.tasks_channel_description)
            )

            messagesChannel = getOrCreateChannel(
                importance,
                NotificationType.MESSAGE,
                context.getString(R.string.messages_channel_title),
                context.getString(R.string.messages_channel_description)
            )

            notificationManager.createNotificationChannel(tasksChannel)
            notificationManager.createNotificationChannel(messagesChannel)
        }
    }

    fun getOrCreateChannel(
        importance: Int,
        type: NotificationType,
        title: String,
        channelDescription: String
    ): NotificationChannel {
        val channel = notificationManager.getNotificationChannel(type.channelId)
            ?: return NotificationChannel(
                type.channelId,
                title,
                importance
            ).apply {
                description = channelDescription
            }
        return channel
    }

    fun showNotification(title: String, message: String, type: NotificationType) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notificationBuilder = NotificationCompat.Builder(context, type.channelId)
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
                notification
            )
            NOTIFICATION_ID += 1
        }
    }

    fun checkPermission(): Boolean {
        // Is true when
        // * Current API level is higher than 33 (Android 13) and permission granted
        // * Or the Android version is lower and there's no need in permissions
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
                || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)
    }

    enum class NotificationType(val channelId: String, val soundKey: String) {
        DEFAULT("ru.runa.wfe.notifications", ""),
        TASK("ru.runa.wfe.notifications.tasks", "tasksSound"),
        MESSAGE("ru.runa.wfe.notifications.messages", "messagesSound")
    }

    companion object {
        private var NOTIFICATION_ID = 2
        private lateinit var tasksChannel: NotificationChannel
        private lateinit var messagesChannel: NotificationChannel
    }
}