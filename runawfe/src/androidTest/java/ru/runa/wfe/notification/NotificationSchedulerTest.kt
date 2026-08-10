package ru.runa.wfe.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.mockk.Runs
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.ServerCheckResult
import java.time.OffsetDateTime

class NotificationSchedulerTest {

    private lateinit var context: Context
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private lateinit var workManager: WorkManager
    private val preferencesManagerMock = mockk<PreferencesManager>(relaxed = true)
    private val inactiveWorkStates = setOf(null, WorkInfo.State.CANCELLED)
    private val acceptableWorkStates =
        setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.SUCCEEDED)

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        workManager = WorkManager.getInstance(context)
        ApiClient.setServerUrl(ServerCheckResult.Valid(ApiClient.DEFAULT_SERVER_URL))
        mockkObject(PreferencesManager.Companion)
        io.mockk.every {
            PreferencesManager.Companion.getInstance(any())
        } returns preferencesManagerMock

        io.mockk.coEvery {
            preferencesManagerMock.setKey<Any>(any(), any())
        } just Runs
    }

    @After
    fun tearDown() {
        // Note: don't call NotificationScheduler.stop(), coroutine scope breaks the mock
        notificationManager.cancelAll()
        unmockkAll()
    }

    private fun checkUniqueWorkState(uniqueName: String): WorkInfo.State? {
        val uniqueWork = workManager.getWorkInfosForUniqueWork(uniqueName)
        return uniqueWork.get().map { info -> info.state }.firstOrNull()
    }

    private fun setLastCheck(lastCheck: Long) {
        io.mockk.every {
            preferencesManagerMock.getValue(PreferencesManager.LAST_CHECK, any())
        } returns OffsetDateTime.now().minusMinutes(lastCheck).toString() // Fast work: < 15 min

        io.mockk.every {
            preferencesManagerMock.getValueFlow(PreferencesManager.LAST_CHECK, any())
        } returns flowOf(OffsetDateTime.now().minusMinutes(lastCheck).toString())
    }

    @Test
    fun startOneTimeWork() {
        setLastCheck(2)
        io.mockk.every {
            preferencesManagerMock.getValueFlow(PreferencesManager.POLLING_INTERVAL, any())
        } returns flowOf(1 * 60) // Fast work: < 15 min

        NotificationScheduler.start(context)

        Thread.sleep(1000)
        // Fast work is created
        val fastWorkState =
            checkUniqueWorkState(NotificationScheduler.FAST_NOTIFICATION_CHECK_WORK_NAME)
        assertTrue(
            "expected to be one of: $acceptableWorkStates but was: $fastWorkState",
            fastWorkState in acceptableWorkStates
        )
        // Periodic work shouldn't be created
        val periodicWorkState =
            checkUniqueWorkState(NotificationScheduler.NORMAL_NOTIFICATION_CHECK_WORK_NAME)
        assertTrue(
            "expected to be one of: ${inactiveWorkStates} but was: $periodicWorkState",
            periodicWorkState in inactiveWorkStates
        )

        workManager.cancelUniqueWork(NotificationScheduler.FAST_NOTIFICATION_CHECK_WORK_NAME)
    }

    @Test
    fun startPeriodicWork() {
        setLastCheck(21)
        io.mockk.every {
            preferencesManagerMock.getValueFlow(PreferencesManager.POLLING_INTERVAL, any())
        } returns flowOf(20 * 60)

        NotificationScheduler.start(context)

        Thread.sleep(1000)
        // Periodic work is created
        val periodicWorkState =
            checkUniqueWorkState(NotificationScheduler.NORMAL_NOTIFICATION_CHECK_WORK_NAME)
        assertTrue(
            "expected to be one of: $acceptableWorkStates but was: $periodicWorkState",
            periodicWorkState in acceptableWorkStates
        )
        // OneTime work is cancelled
        val fastWorkState =
            checkUniqueWorkState(NotificationScheduler.FAST_NOTIFICATION_CHECK_WORK_NAME)
        assertTrue(
            "expected to be one of: $inactiveWorkStates but was: $fastWorkState",
            fastWorkState in inactiveWorkStates
        )

        workManager.cancelUniqueWork(NotificationScheduler.NORMAL_NOTIFICATION_CHECK_WORK_NAME)
    }
}