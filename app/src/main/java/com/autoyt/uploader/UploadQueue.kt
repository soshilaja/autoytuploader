package com.autoyt.uploader

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow

data class ActiveUpload(val mediaId: Long, val percent: Int)

/**
 * The video being sent right now. Lives in memory because an UPLOADING row in [UploadDb] only means
 * a session can be resumed: it stays there after the worker stops.
 */
object UploadProgress {
    val current = MutableStateFlow<ActiveUpload?>(null)
}

/** The rules for what is waiting and what may go now, shared by the worker and the screens. */
class UploadQueue(context: Context) {
    private val app = context.applicationContext
    private val settings = Settings(app)
    private val db = UploadDb(app)
    private val library = VideoLibrary(app)

    /** Active folders only, in folder order, so earlier folders are emptied first. */
    fun waiting(): List<Video> = settings.activeAlbums.flatMap(::waitingIn)

    fun waitingIn(album: Album): List<Video> = library.videos(album).filter(::isWaiting)

    fun allowedNow(video: Video): Boolean {
        val manager = app.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        val unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val limit = UploadRules.mobileLimit(settings.wifiOnly, settings.mobileLimitMb)
        return UploadRules.allowedOnNetwork(video.sizeBytes, unmetered, limit)
    }

    private fun isWaiting(video: Video): Boolean {
        val status = db.find(video.id)?.status
        return status != UploadStatus.DONE && status != UploadStatus.FAILED
    }
}
