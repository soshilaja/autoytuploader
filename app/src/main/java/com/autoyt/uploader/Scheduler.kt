package com.autoyt.uploader

import android.content.Context
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** New videos are caught instantly by a media trigger, with a periodic sweep as the safety net. */
object Scheduler {
    private const val UPLOAD = "upload"
    private const val SETTLE = "settle"
    private const val SWEEP = "sweep"
    private const val TRIGGER = "media-trigger"

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun start(context: Context) {
        val work = WorkManager.getInstance(context)
        work.enqueueUniquePeriodicWork(
            SWEEP,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES).setConstraints(online).build(),
        )
        armTrigger(context, ExistingWorkPolicy.KEEP)
    }

    fun uploadNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>().setConstraints(online).build()
        WorkManager.getInstance(context).enqueueUniqueWork(UPLOAD, ExistingWorkPolicy.KEEP, request)
    }

    /** Separate name so a running worker can schedule this without colliding with itself. */
    fun uploadAfterSettling(context: Context) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(online)
            .setInitialDelay(UploadRules.SETTLE_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SETTLE, ExistingWorkPolicy.REPLACE, request)
    }

    /** Content triggers fire once, so each firing queues the next one behind itself. */
    fun armTrigger(context: Context, policy: ExistingWorkPolicy) {
        val constraints = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentUpdateDelay(10, TimeUnit.SECONDS)
            .build()
        val request = OneTimeWorkRequestBuilder<MediaTriggerWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork(TRIGGER, policy, request)
    }
}

class MediaTriggerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Scheduler.uploadNow(applicationContext)
        Scheduler.armTrigger(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}
