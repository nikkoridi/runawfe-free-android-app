package ru.runa.wfe.notification

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.runa.wfe.R
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.restapi.model.MessageAddedBroadcast
import ru.runa.wfe.restapi.model.WfChatRoom
import ru.runa.wfe.restapi.model.WfePagedListFilter
import ru.runa.wfe.restapi.model.WfeTask
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NotificationLogic(
    private val context: Context,
    private val notificationHelpers: NotificationHelpers,
    private val preferencesManager: PreferencesManager
) {

    fun saveLastCheckData(lastCheck: String) {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesManager.setKey(
                PreferencesManager.LAST_CHECK,
                lastCheck
            )
        }
    }

    fun loadLastCheckData() {
        val lastCheck = OffsetDateTime.parse(
            preferencesManager.getValue(
                PreferencesManager.LAST_CHECK,
                OffsetDateTime.now(ZoneOffset.UTC).toString()
            )
        )
            .withOffsetSameLocal(ZoneOffset.UTC)
        lastTasksCheck = lastCheck
    }

    suspend fun checkNewChatMessagesAndNotify() {
        try {
            val chatRooms: List<WfChatRoom>? = ApiClient.chatService.getChatRoomsUsingGET().body()
            if (!chatRooms.isNullOrEmpty()) {
                for (room in chatRooms) {
                    val newMessages = checkNewMessages(room)
                    if (!newMessages.isNullOrEmpty()) {
                        val content = newChatMessagesNotificationContent(room.id, newMessages)
                        notificationHelpers.showNotification(
                            content.title, content.message,
                            NotificationHelpers.NotificationType.MESSAGE
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(this::class.simpleName, ex.message.toString())
        }
    }

    private suspend fun checkNewMessages(room: WfChatRoom): List<MessageAddedBroadcast>? {
        val newMessagesCount = room.newMessagesCount?.toInt() ?: 0
        if (newMessagesCount > 0) {
            try {
                val chatRoomMessages =
                    room.id?.let {
                        ApiClient.chatService.getChatMessagesUsingGET(it).body()
                    }
                if (!chatRoomMessages.isNullOrEmpty() &&
                    newMessagesCount < chatRoomMessages.size - 1
                ) {
                    return chatRoomMessages.subList(0, newMessagesCount)
                }
            } catch (ex: Exception) {
                Log.e(this::class.simpleName, ex.message.toString())
            }
        }
        return null
    }

    private fun newChatMessagesNotificationContent(
        roomId: Long?,
        newMessages: List<MessageAddedBroadcast>
    ): NotificationContent {
        val title: String
        val notificationMessage: String
        val basicTitle = if (roomId != null) {
            "${context.getString(R.string.messages_chat_id_template, roomId)}:"
        } else {
            context.getString(R.string.new_data_notifications)
        }

        if (newMessages.size > 1) {
            title = "$basicTitle ${
                context.resources.getQuantityString(
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

        return NotificationContent(title, notificationMessage)
    }

    suspend fun checkNewTasksAndNotify() {
        try {
            val tasks: List<WfeTask>? = ApiClient.taskService.getMyTasksUsingPOST(
                WfePagedListFilter()
            ).body()?.data

            val newTasks = checkNewTasks(tasks)
            if (!newTasks.isNullOrEmpty()) {
                val content = newTasksNotificationContent(newTasks)
                notificationHelpers.showNotification(
                    content.title, content.message,
                    NotificationHelpers.NotificationType.TASK
                )
            }
        } catch (ex: Exception) {
            Log.e(this::class.simpleName, ex.message.toString())
        }
    }

    private fun checkNewTasks(tasks: List<WfeTask>?): List<WfeTask>? {
        if (!tasks.isNullOrEmpty()) {
            val newTasks = ArrayList<WfeTask>()
            for (task in tasks) {
                // New variable because smartcast won't work with custom getter
                val assignDate = task.assignDate
                if (assignDate != null &&
                    lastTasksCheck.isBefore(assignDate)
                ) {
                    newTasks.add(task)
                }
            }
            return newTasks
        }
        lastTasksCheck = OffsetDateTime.now(ZoneOffset.UTC)
        saveLastCheckData(lastTasksCheck.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        return null
    }

    private fun newTasksNotificationContent(newTasks: List<WfeTask>): NotificationContent {
        val title = "${context.getString(R.string.new_data_notifications)} ${
            context.resources.getQuantityString(
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

        return NotificationContent(title, notificationMessage)
    }

    data class NotificationContent(
        val title: String,
        val message: String
    )

    companion object {
        private var lastTasksCheck: OffsetDateTime = OffsetDateTime.now()
    }
}