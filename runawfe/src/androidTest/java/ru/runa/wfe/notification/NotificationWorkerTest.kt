package ru.runa.wfe.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NotificationWorkerTest  {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNotificationWorkerNormalWork() {
        val worker = TestListenableWorkerBuilder<NotificationWorker>(context)
            .build()
        runBlocking {
            val result = worker.doWork()
            assertEquals(Result.success(), result)
        }
    }

    @Test
    fun testNotificationWorkerFastWork() {
        val worker = TestListenableWorkerBuilder<NotificationWorker>(context, workDataOf("fastCheck" to true)).build()
        runBlocking {
            val result = worker.doWork()
            assertEquals(Result.success(), result)
        }
    }
}