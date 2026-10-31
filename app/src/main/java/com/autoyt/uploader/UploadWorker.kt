package com.autoyt.uploader

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.sync.Mutex
import java.io.IOException

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val app = applicationContext
    private val settings = Settings(app)
    private val db = UploadDb(app)
    private val library = VideoLibrary(app)
    private val uploader = YouTubeUploader(app)

    override suspend fun doWork(): Result {
        // The sweep and the trigger can overlap; the active run rescans, so a second one can just exit.
        if (!running.tryLock()) return Result.success()
        return try {
            uploadAll()
        } finally {
            running.unlock()
        }
    }

    private suspend fun uploadAll(): Result {
        val albumId = settings.albumId ?: return Result.success()
        if (System.currentTimeMillis() < settings.pausedUntil) return Result.success()

        var tokenRetries = 0
        var token = fetchToken() ?: return Result.success()
        while (true) {
            val now = System.currentTimeMillis()
            val waiting = library.videos(albumId).filter { isWaiting(it) }
            val video = waiting.firstOrNull {
                UploadRules.isSettled(it.modifiedMillis, now) && allowedNow(it)
            }
            if (video == null) {
                if (waiting.any { !UploadRules.isSettled(it.modifiedMillis, now) }) {
                    Scheduler.uploadAfterSettling(app)
                }
                return Result.success()
            }
            try {
                uploadOne(video, token)
            } catch (e: TokenRejected) {
                if (++tokenRetries > 2) return Result.retry()
                YouTubeAuth.clearToken(app, token)
                token = fetchToken() ?: return Result.success()
            } catch (e: UploadStopped) {
                return if (isStopped) Result.retry() else Result.success()
            } catch (e: QuotaExceeded) {
                settings.pausedUntil = UploadRules.nextQuotaReset(System.currentTimeMillis())
                Notifications.alert(app, "YouTube's daily limit was reached. Uploads resume after midnight Pacific time.")
                return Result.success()
            } catch (e: UploadRejected) {
                fail(video, e.message)
            } catch (e: SecurityException) {
                fail(video, "Storage permission is missing")
            } catch (e: IOException) {
                return Result.retry()
            }
        }
    }

    private fun isWaiting(video: Video): Boolean {
        val status = db.find(video.id)?.status
        return status != UploadStatus.DONE && status != UploadStatus.FAILED
    }

    private suspend fun fetchToken(): String? {
        val token = try {
            YouTubeAuth.tokenOrNull(app)
        } catch (e: Exception) {
            null
        }
        if (token == null && !settings.needsSignIn) {
            settings.needsSignIn = true
            Notifications.alert(app, "Open the app to sign in to YouTube again.")
        }
        return token
    }

    private suspend fun uploadOne(video: Video, token: String) {
        showProgress(video)
        val session = db.find(video.id)?.sessionUrl ?: newSession(video, token)
        val videoId = send(video, session, token)
            ?: send(video, newSession(video, token), token)
            ?: throw IOException("Upload session expired twice")
        val moved = library.moveToUploaded(video)
        db.save(
            UploadRecord(
                video.id, video.name, UploadStatus.DONE, null, videoId,
                if (moved) null else "Uploaded, but could not be moved to the Uploaded album",
            )
        )
    }

    private fun newSession(video: Video, token: String): String {
        val session = uploader.startSession(video, token)
        db.save(UploadRecord(video.id, video.name, UploadStatus.UPLOADING, session, null, null))
        return session
    }

    private fun send(video: Video, session: String, token: String): String? =
        uploader.upload(
            video, session, token,
            canContinue = { !isStopped && allowedNow(video) },
            onProgress = { sent ->
                setForegroundAsync(foregroundInfo(video.name, (sent * 100 / video.sizeBytes).toInt()))
            },
        )

    private fun fail(video: Video, message: String?) {
        db.save(UploadRecord(video.id, video.name, UploadStatus.FAILED, null, null, message))
        Notifications.alert(app, "${video.name} could not be uploaded.")
    }

    private fun allowedNow(video: Video): Boolean {
        val manager = app.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        val unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return UploadRules.allowedOnNetwork(video.sizeBytes, unmetered, settings.mobileLimitMb)
    }

    /** Android may refuse a background start; the upload then runs unprotected and resumes if killed. */
    private suspend fun showProgress(video: Video) {
        try {
            setForeground(foregroundInfo(video.name, 0))
        } catch (e: IllegalStateException) {
            Unit
        }
    }

    private fun foregroundInfo(name: String, percent: Int) = ForegroundInfo(
        Notifications.PROGRESS_ID,
        Notifications.progress(app, name, percent),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo("Preparing upload", 0)

    private companion object {
        val running = Mutex()
    }
}
