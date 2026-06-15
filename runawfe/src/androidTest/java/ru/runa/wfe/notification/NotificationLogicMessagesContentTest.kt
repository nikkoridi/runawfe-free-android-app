package ru.runa.wfe.notification

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ru.runa.wfe.R
import ru.runa.wfe.notification.NotificationLogic.NotificationContent
import ru.runa.wfe.restapi.model.Actor
import ru.runa.wfe.restapi.model.MessageAddedBroadcast
import ru.runa.wfe.restapi.model.WfeTask

class NotificationLogicMessagesContentTest {
    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var notificationHelpers: NotificationHelpers
    private lateinit var notificationLogic: NotificationLogic

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        resources = context.resources
        notificationHelpers = NotificationHelpers(context)
        notificationLogic = NotificationLogic(context, notificationHelpers)
    }

    private fun tasksPluralsSuffixes(n: Int): String {
        return when {
            n % 100 in 11..14 -> "задач"
            n % 10 == 1 -> "задача"
            n % 10 in 2..4 -> "задачи"
            else -> "задач"
        }
    }

    private fun messagesPluralsSuffixes(n: Int): String {
        return when {
            n % 100 in 11..14 -> "сообщений"
            n % 10 == 1 -> "сообщение"
            n % 10 in 2..4 -> "сообщения"
            else -> "сообщения"
        }
    }

    @Test
    fun newChatMessagesNotificationContent_OneMessage() {
        val message = "msg"
        val roomId: Long = 1
        val oneMessageInOnChat =
            List(1) { MessageAddedBroadcast(author = Actor(name = "Автор"), text = message) }

        val content =
            notificationLogic.newChatMessagesNotificationContent(roomId, oneMessageInOnChat)

        assertEquals(
            content.title,
            "${context.getString(R.string.messages_chat_id_template, roomId)}:"
        )
        assertEquals(message, content.message)
    }

    // NOTE: If this test fails, check device language settings. Not having Russian as a main language may cause locale strings mismatch.
    @Test
    fun newChatMessagesNotificationContent_SeveralMessages() {
        val testChatAuthorOne = Actor(name = "Первый отправитель")
        val message = "msg"
        val roomId: Long = 1
        val numberOfMessages = 3
        val oneMessageInOnChat =
            List(numberOfMessages) {
                MessageAddedBroadcast(
                    author = testChatAuthorOne,
                    text = message
                )
            }

        val content =
            notificationLogic.newChatMessagesNotificationContent(roomId, oneMessageInOnChat)

        assertEquals(
            "${
                context.getString(
                    R.string.messages_chat_id_template,
                    roomId
                )
            }: $numberOfMessages ${messagesPluralsSuffixes(numberOfMessages)}", content.title
        )
        val messages =
            "${testChatAuthorOne.name}: $message\n".repeat(3)
        assertEquals(messages, content.message)
    }

    @Test
    fun newTasksNotificationContent_SeveralTasks() {
        val taskName = "Task test"
        val cases = listOf(2, 3, 5, 10, 11, 12, 13, 14, 21, 22)
        val content = cases.forEach {
            notificationLogic.newTasksNotificationContent(
                List(it) { WfeTask(name = taskName) }
            )
        }

        val results = cases.forEach {
            NotificationContent(
                "${context.getString(R.string.new_data_notifications)} ${tasksPluralsSuffixes(it)}",
                taskName
            )
        }

        assertEquals(content, results)
    }

    @Test
    fun newTasksNotificationContent_OneTask() {
        val taskName = "One task test"
        val oneTaskList: List<WfeTask> = listOf(WfeTask(name = taskName))

        val content = notificationLogic.newTasksNotificationContent(oneTaskList)

        assertEquals(
            content,
            NotificationContent(
                "${context.getString(R.string.new_data_notifications)} 1 задача",
                taskName
            )
        )
    }
}