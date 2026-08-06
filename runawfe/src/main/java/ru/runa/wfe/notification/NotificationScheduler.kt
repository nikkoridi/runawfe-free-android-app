package ru.runa.wfe.notification

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.ui.notification.DurationPreference
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val NOTIFICATION_SERVICE_ID = 1
    private const val FAST_NOTIFICATION_CHECK_WORK = "fastNotificationCheck"
    private const val NORMAL_NOTIFICATION_CHECK_WORK = "normalNotificationCheck"
    private const val FLEX_INTERVAL: Long = 15 * 60 * 1_000
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresDeviceIdle(false)
        .build()
    private var pollingInterval: Long = DurationPreference.DEFAULT.toLong() * 60 * 1_000
    private var fastCheck: Boolean = false

    fun start(context: Context) {
        val preferencesManager = PreferencesManager.getInstance(context)
        // DataStore uses Dispatchers.IO, this coroutine dispatcher won't change it
        CoroutineScope(Dispatchers.Default).launch {
            preferencesManager.getValueFlow(
                PreferencesManager.POLLING_INTERVAL,
                pollingInterval.toInt() / (1_000 * 60)
            ).collect { value ->
                pollingInterval = (value * 1_000 * 60).toLong()
                /*
                The value is nonzero initially (checked in MainActivity)
                stopSelf() should be called only after service start
                */
                if (pollingInterval == 0L) {
                    stop(context)
                } else {
                    fastCheck = true.takeIf { pollingInterval < 15 * 60 * 1_000 } ?: false
                    if (fastCheck) {
                        WorkManager.getInstance(context).cancelUniqueWork(NORMAL_NOTIFICATION_CHECK_WORK)
                    } else {
                        WorkManager.getInstance(context).cancelUniqueWork(FAST_NOTIFICATION_CHECK_WORK)
                    }
                    scheduleNext(context)
                }
            }
        }

        val lastCheck = OffsetDateTime.parse(
            preferencesManager.getValue(
                PreferencesManager.LAST_CHECK,
                OffsetDateTime.now(ZoneOffset.UTC).toString()
            )
        )
            .withOffsetSameLocal(ZoneOffset.UTC)
        NotificationLogic.lastTasksCheck = lastCheck

        NotificationHelpers(context).createNotificationChannels()
    }

    private fun stopWorks(context: Context) {
       WorkManager.getInstance(context).cancelUniqueWork(
           if (fastCheck) FAST_NOTIFICATION_CHECK_WORK else NORMAL_NOTIFICATION_CHECK_WORK
       )
    }

    fun stop(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            PreferencesManager.getInstance(context).setKey(
                PreferencesManager.LAST_CHECK,
                NotificationLogic.lastTasksCheck.toString()
            )
        }
        stopWorks(context)
        Log.i(this.javaClass.name, "Notification check work removed")
    }

    fun scheduleNext(context: Context) {
        val workManager = WorkManager.getInstance(context)
        if (fastCheck) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(workDataOf("fastCheck" to true))
                .setInitialDelay(pollingInterval, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                // This will turn ForegroundService mode. It works only with OneTimeWorkRequest
                // .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            workManager.enqueueUniqueWork(
                FAST_NOTIFICATION_CHECK_WORK,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        } else {
            val periodicRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                // Repeat interval
                pollingInterval, TimeUnit.MILLISECONDS,
                // How long can the work take (will wait this time before start next repeat)
                FLEX_INTERVAL, TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                NORMAL_NOTIFICATION_CHECK_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        }
    }
}