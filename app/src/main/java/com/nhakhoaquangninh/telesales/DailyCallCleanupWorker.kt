package com.nhakhoaquangninh.telesales

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhakhoaquangninh.telesales.core.FileLogger
import com.nhakhoaquangninh.telesales.data.local.room.TelesalesDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyCallCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            ServiceLocator.init(applicationContext)
            val startOfTodayMillis = DailyCallCleanupPolicy.startOfCurrentDayMillis()
            withContext(Dispatchers.IO) {
                val database = TelesalesDatabase.getDatabase(applicationContext)
                database.callRecordDao().deleteTerminalRecordsBefore(startOfTodayMillis)
                database.failedCallDao().deleteBefore(startOfTodayMillis)
            }
            DailyCallCleanupScheduler.markCompleted(applicationContext)
            applicationContext.sendBroadcast(
                android.content.Intent(REFRESH_RECORDINGS_ACTION).setPackage(applicationContext.packageName)
            )
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            FileLogger.logException(
                applicationContext,
                "DAILY_CALL_CLEANUP_FAILED",
                "Không thể dọn dữ liệu cuộc gọi ngày cũ: ${error.message}",
                error
            )
            Result.retry()
        } finally {
            DailyCallCleanupScheduler.schedule(applicationContext)
        }
    }

    companion object {
        const val REFRESH_RECORDINGS_ACTION = "com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS"
    }
}
