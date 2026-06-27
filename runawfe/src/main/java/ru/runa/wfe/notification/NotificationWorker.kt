package ru.runa.wfe.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class NotificationWorker(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val fastCheckEnabled: Boolean = inputData.getBoolean("fastCheck", false)
            withContext(Dispatchers.IO) {
                val notificationCheck = launch {
                    NotificationLogic(
                        context,
                        NotificationHelpers(context)
                    ).collectNotificationData()
                }
                notificationCheck.join()
                if (fastCheckEnabled) {
                    NotificationScheduler.scheduleNext(context)
                }
            }
        } catch (ex: IOException) {
            Log.i(this.javaClass.name, ex.message.toString())
            return Result.retry()
        } catch (ex: Exception) {
            Log.e(this.javaClass.name, ex.message.toString())
            return Result.failure()
        }
        return Result.success()
    }
}