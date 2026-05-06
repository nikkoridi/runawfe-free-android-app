package ru.runa.wfe.notification

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ru.runa.wfe.restapi.model.WfChatRoom
import ru.runa.wfe.restapi.model.WfeTask
import java.time.OffsetDateTime

class NotificationLogicTest {
    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var notificationHelpers: NotificationHelpers
    private lateinit var notificationLogic: NotificationLogic

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        resources = context.resources
        notificationHelpers = NotificationHelpers(context)
        notificationLogic = NotificationLogic(context, notificationHelpers)
    }

    /*
    checkNewMessages() requires a mock of API Client
    It is better to be done in NotificationLogicApiDataTest.kt
     */

    @Test
    fun checkNewMessages_EmptyList() = runTest {
        val newMessages = notificationLogic.checkNewMessages(WfChatRoom(), 5)
        assertEquals(null, newMessages)
    }

    @Test
    fun checkNewMessages_NoMessages() = runTest {
        val room = WfChatRoom(id = 10, newMessagesCount = 0)
        val newMessages = notificationLogic.checkNewMessages(room, 0)
        assertEquals(null, newMessages)
    }

    @Test
    fun checkNewTasks() = runTest {
        val listSize = 10
        val new = 3
        val expectedNewTasks =
            List(new) { WfeTask(name = "new", assignDate = OffsetDateTime.now(), firstOpen = true) }
        val tasks = (List(listSize - new) { WfeTask(name = "test") } + expectedNewTasks).shuffled()

        val newTasks = notificationLogic.checkNewTasks(tasks)

        assertEquals(expectedNewTasks.count(), newTasks?.count())
    }

    @Test
    fun checkNewTasks_NoTasks() {
        val tasks = List(3) { WfeTask() }
        val newTasks = notificationLogic.checkNewTasks(tasks)
        assertEquals(emptyList<WfeTask>(), newTasks) // TODO: think about return value
    }

    @Test
    fun checkNewTasks_EmptyList() {
        val tasks: List<WfeTask> = List(0) { WfeTask() }
        val newTasks = notificationLogic.checkNewTasks(tasks)
        assertEquals(null, newTasks)
    }

}