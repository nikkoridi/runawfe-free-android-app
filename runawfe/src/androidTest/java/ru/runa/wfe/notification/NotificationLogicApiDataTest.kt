package ru.runa.wfe.notification

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.restapi.client.ChatControllerApi
import ru.runa.wfe.restapi.client.TaskControllerApi
import ru.runa.wfe.restapi.model.MessageAddedBroadcast
import ru.runa.wfe.restapi.model.WfChatRoom
import ru.runa.wfe.restapi.model.WfePagedListOfWfeTask
import java.time.OffsetDateTime

class NotificationLogicApiDataTest {
    @MockK
    lateinit var mockChatService: ChatControllerApi

    @MockK
    lateinit var mockTaskService: TaskControllerApi

    private lateinit var context: Context
    private lateinit var resources: Resources
    private val notificationHelpers = mockk<NotificationHelpers>(relaxed = true)
    private lateinit var notificationLogic: NotificationLogic


    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        mockkObject(ApiClient)
        context = ApplicationProvider.getApplicationContext()
        resources = context.resources
        notificationLogic =
            NotificationLogic(context, notificationHelpers, OffsetDateTime.now().minusMinutes(3))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun checkNewChatMessagesAndNotify() = runTest {
        val listSize = 10
        val new: Long = 1
        val messages = List(listSize) { MessageAddedBroadcast(text = "test") }
        val rooms = WfChatRoom(id = 1, newMessagesCount = new)
        io.mockk.every {
            ApiClient.chatService
        } returns mockChatService
        coEvery {
            mockChatService.getChatRoomsUsingGET()
        } returns retrofit2.Response.success(listOf(rooms))
        coEvery {
            mockChatService.getChatMessagesUsingGET(any())
        } returns retrofit2.Response.success(messages)
        notificationLogic.checkNewChatMessagesAndNotify()
        verify { notificationHelpers.showNotification(any(), any(), any()) }
    }

    @Test
    fun checkNewTasksAndNotify() = runTest {
        val tasks = TestHelpers.generateNewTasks(3, 10)
        val response = WfePagedListOfWfeTask(tasks, tasks.size)

        io.mockk.every {
            ApiClient.taskService
        } returns mockTaskService
        coEvery {
            ApiClient.taskService.getMyTasksUsingPOST(any())
        } returns retrofit2.Response.success(response)

        notificationLogic.checkNewTasksAndNotify()

        verify { notificationHelpers.showNotification(any(), any(), any()) }
    }

}