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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.runa.wfe.MainActivity
import ru.runa.wfe.R
import ru.runa.wfe.ui.notification.PermissionsConstants

class NotificationHelpers(val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private var notificationIdCounter = 2000 // To prevent id conflicts with other notifications

    fun createNotificationChannels() {
        if (notificationManager.areNotificationsEnabled()) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationType.entries.forEach { type ->
                val channel = getOrCreateChannel(type, importance)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun isChannelEnabled(type: NotificationType): Boolean {
        return (notificationManager.areNotificationsEnabled()
                && (notificationManager.getNotificationChannel(type.channelId)?.importance
            ?: NotificationManager.IMPORTANCE_UNSPECIFIED) != NotificationManager.IMPORTANCE_NONE)
    }

    fun getOrCreateChannel(
        type: NotificationType,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ): NotificationChannel {
        val channel = notificationManager.getNotificationChannel(type.channelId)
            ?: return NotificationChannel(
                type.channelId,
                context.getString(type.titleResId),
                importance
            ).apply {
                description = context.getString(type.descriptionResId)
            }
        return channel
    }

    fun showNotification(title: String, message: String, type: NotificationType) {
        // Android 13 (API level 33) and higher requires a permission
        // for notificationManager.notify() call
        if (context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU)
        ) {
            val notificationIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                PermissionsConstants.ACTION_REQUEST_PERMISSION.requestCode,
                notificationIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            val notification = buildNotification(type, message, title, pendingIntent)
            notificationManager.notify(type.channelId, notificationIdCounter++, notification)
        }
    }

    private fun buildNotification(
        type: NotificationType,
        message: String,
        title: String,
        pendingIntent: PendingIntent?
    ): Notification {
        return NotificationCompat.Builder(context, type.channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()
    }

    enum class NotificationType(
        val channelId: String,
        val titleResId: Int,
        val descriptionResId: Int,
        val soundKey: String
    ) {
        DEFAULT(
            "ru.runa.wfe.notifications",
            R.string.notifications_settings,
            R.string.notifications_service_message,
            ""
        ),
        TASK(
            "ru.runa.wfe.notifications.tasks",
            R.string.tasks_channel_title,
            R.string.tasks_channel_description,
            "tasksSound"
        ),
        MESSAGE(
            "ru.runa.wfe.notifications.messages",
            R.string.messages_channel_title,
            R.string.messages_channel_description,
            "messagesSound"
        )
    }
}