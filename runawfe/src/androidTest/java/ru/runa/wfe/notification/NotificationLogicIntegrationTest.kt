package ru.runa.wfe.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.ServerCheckResult
import ru.runa.wfe.restapi.infrastructure.Serializer.jacksonObjectMapper
import ru.runa.wfe.restapi.model.Actor
import ru.runa.wfe.restapi.model.MessageAddedBroadcast
import ru.runa.wfe.restapi.model.WfChatRoom
import ru.runa.wfe.restapi.model.WfePagedListOfWfeTask

class NotificationLogicIntegrationTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context
    private lateinit var notificationLogic: NotificationLogic
    private val mockedNotificationHelpers = mockk<NotificationHelpers>(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationLogic = NotificationLogic(
            context,
            mockedNotificationHelpers
        )

        mockWebServer = MockWebServer()
        mockWebServer.start()
        ApiClient.setServerUrl(ServerCheckResult.Valid(mockWebServer.url("/").toString()))
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        unmockkAll()
    }

    private fun newTasksResponseGenerator(new: Int, size: Int): String {
        val tasks = TestHelpers.generateNewTasks(new, size)
        return jacksonObjectMapper.writeValueAsString(WfePagedListOfWfeTask(tasks, tasks.size))
            ?: ""
    }

    private fun successfulResponseGenerator(json: String) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json")
        )
    }

    private fun badResponseGenerator() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("")
                .addHeader("Content-Type", "application/json")
        )
    }

    @Test
    fun severalNewTasks() = runTest {
        successfulResponseGenerator((newTasksResponseGenerator(3, 7)))
        notificationLogic.checkNewTasksAndNotify()
        verify(timeout = 2_000, exactly = 1) {
            mockedNotificationHelpers.showNotification(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun oneNewTask() = runTest {
        successfulResponseGenerator(newTasksResponseGenerator(1, 5))
        notificationLogic.checkNewTasksAndNotify()
        verify(exactly = 1) { mockedNotificationHelpers.showNotification(any(), any(), any()) }
    }

    @Test
    fun noNewTasks() = runTest {
        successfulResponseGenerator(newTasksResponseGenerator(0, 2))
        notificationLogic.checkNewTasksAndNotify()
        verify(exactly = 0) { mockedNotificationHelpers.showNotification(any(), any(), any()) }
    }

    @Test
    fun badResponseNewTasks() = runTest {
        badResponseGenerator()
        notificationLogic.checkNewTasksAndNotify()
        verify(exactly = 0) { mockedNotificationHelpers.showNotification(any(), any(), any()) }
    }

    @Test
    fun severalNewChatMessages() = runTest {
        val rooms = listOf(
            WfChatRoom(id = 1, newMessagesCount = 3),
            WfChatRoom(id = 2, newMessagesCount = 0),
            WfChatRoom(id = 3, newMessagesCount = 1)
        )
        val jsonRooms = jacksonObjectMapper.writeValueAsString(rooms)
        successfulResponseGenerator(jsonRooms)
        val messages1 = List(5) {
            MessageAddedBroadcast(
                author = Actor(name = "testAuthor1"),
                text = "test msg 1"
            )
        }
        successfulResponseGenerator(jacksonObjectMapper.writeValueAsString(messages1))
        val messages2 = List(1) {
            MessageAddedBroadcast(
                author = Actor(name = "testAuthor2"),
                text = "test msg 2"
            )
        }
        successfulResponseGenerator(jacksonObjectMapper.writeValueAsString(messages2))

        val calls = mutableListOf<Unit>()
        io.mockk.every {
            mockedNotificationHelpers.showNotification(any(), any(), any())
        } answers { calls.add(Unit) }

        notificationLogic.checkNewChatMessagesAndNotify()

        assertEquals(2, calls.size)
    }

    @Test
    fun oneNewChatMessage() = runTest {
        val rooms = listOf(
            WfChatRoom(id = 1, newMessagesCount = 3),
            WfChatRoom(id = 2, newMessagesCount = 0),
            WfChatRoom(id = 3, newMessagesCount = 0)
        )
        val jsonRooms = jacksonObjectMapper.writeValueAsString(rooms)
        successfulResponseGenerator(jsonRooms)
        val messages1 = List(5) {
            MessageAddedBroadcast(
                author = Actor(name = "testAuthor"),
                text = "test msg"
            )
        }
        successfulResponseGenerator(jacksonObjectMapper.writeValueAsString(messages1))

        notificationLogic.checkNewChatMessagesAndNotify()

        verify(exactly = 1) { mockedNotificationHelpers.showNotification(any(), any(), any()) }
    }

    @Test
    fun noNewChatMessages() = runTest {
        val rooms = listOf(
            WfChatRoom(id = 1, newMessagesCount = 0),
            WfChatRoom(id = 2, newMessagesCount = 0),
            WfChatRoom(id = 3, newMessagesCount = 0)
        )
        val jsonRooms = jacksonObjectMapper.writeValueAsString(rooms)
        successfulResponseGenerator(jsonRooms)

        notificationLogic.checkNewChatMessagesAndNotify()

        verify(exactly = 0) { mockedNotificationHelpers.showNotification(any(), any(), any()) }
    }

    @Test
    fun badResponseChatMessages() = runTest {
        badResponseGenerator()
        notificationLogic.checkNewChatMessagesAndNotify()
        verify(exactly = 0) { mockedNotificationHelpers.showNotification(any(), any(), any()) }
    }

}