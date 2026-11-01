package com.autoyt.uploader

import android.content.Context
import android.os.Environment
import android.os.PowerManager
import java.util.concurrent.TimeUnit

data class QueuedVideo(val video: Video, val state: QueueState, val percent: Int)

/** Everything the screens show, read together off the main thread. */
data class Dashboard(
    val queue: List<QueuedVideo> = emptyList(),
    val history: List<UploadRecord> = emptyList(),
    val waitingByFolder: Map<String, Int> = emptyMap(),
    val uploadedThisWeek: Int = 0,
    val needsSignIn: Boolean = false,
    val hasStorage: Boolean = true,
    val unrestricted: Boolean = true,
    val dailyLimitHit: Boolean = false,
) {
    val isUploading: Boolean get() = queue.any { it.state == QueueState.UPLOADING }
}

class DashboardLoader(context: Context) {
    private val app = context.applicationContext
    private val settings = Settings(app)
    private val db = UploadDb(app)
    private val queue = UploadQueue(app)

    fun load(): Dashboard {
        val now = System.currentTimeMillis()
        val hasStorage = Environment.isExternalStorageManager()
        val paused = settings.pausedAlbums
        val active = UploadProgress.current.value
        val dailyLimitHit = now < settings.pausedUntil

        val waiting = if (hasStorage) settings.albums.associateWith(queue::waitingIn) else emptyMap()
        // A paused folder's video may still be finishing, so it stays listed until it is done.
        val queued = waiting.flatMap { (album, videos) ->
            videos.filter { album.id !in paused || it.id == active?.mediaId }
        }.map { video ->
            val isActive = video.id == active?.mediaId
            val state = UploadRules.queueState(
                isActive, dailyLimitHit, UploadRules.isSettled(video.modifiedMillis, now), queue.allowedNow(video),
            )
            QueuedVideo(video, state, if (isActive) active?.percent ?: 0 else 0)
        }.sortedBy { it.state != QueueState.UPLOADING }

        return Dashboard(
            queue = queued,
            history = db.recent().filter { it.status != UploadStatus.UPLOADING },
            waitingByFolder = waiting.entries.associate { (album, videos) -> album.id to videos.size },
            uploadedThisWeek = db.doneSince(now - TimeUnit.DAYS.toMillis(7)),
            needsSignIn = settings.needsSignIn,
            hasStorage = hasStorage,
            unrestricted = app.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(app.packageName),
            dailyLimitHit = dailyLimitHit,
        )
    }
}
