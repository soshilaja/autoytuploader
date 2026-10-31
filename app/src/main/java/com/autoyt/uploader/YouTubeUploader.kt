package com.autoyt.uploader

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class UploadStopped : Exception()
class QuotaExceeded : Exception()
class TokenRejected : Exception()
class UploadRejected(message: String) : Exception(message)

/** YouTube's resumable upload protocol. IOExceptions mean "try again later". */
class YouTubeUploader(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .writeTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    fun startSession(video: Video, token: String): String {
        val metadata = JSONObject()
            .put("snippet", JSONObject().put("title", UploadRules.title(video.name)))
            .put("status", JSONObject().put("privacyStatus", "private"))
        val request = Request.Builder()
            .url(START_URL)
            .header("Authorization", "Bearer $token")
            .header("X-Upload-Content-Length", video.sizeBytes.toString())
            .header("X-Upload-Content-Type", video.mimeType)
            .post(metadata.toString().toByteArray().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throwFor(response)
            return response.header("Location") ?: throw IOException("Upload session had no location")
        }
    }

    /** Returns the YouTube video id, or null when [sessionUrl] has expired and must be restarted. */
    fun upload(
        video: Video,
        sessionUrl: String,
        token: String,
        canContinue: () -> Boolean,
        onProgress: (Long) -> Unit,
    ): String? {
        var offset = when (val state = queryOffset(sessionUrl, video, token)) {
            is SessionState.Finished -> return state.videoId
            SessionState.Expired -> return null
            is SessionState.Resume -> state.offset
        }
        val stream = context.contentResolver.openInputStream(video.uri)
            ?: throw UploadRejected("The file could not be opened")
        stream.use { input ->
            skipFully(input, offset)
            val buffer = ByteArray(UploadRules.CHUNK_BYTES)
            while (true) {
                if (!canContinue()) throw UploadStopped()
                val read = readChunk(input, buffer)
                if (read <= 0) throw UploadRejected("The file got shorter during upload")
                val request = Request.Builder()
                    .url(sessionUrl)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Range", UploadRules.contentRange(offset, read, video.sizeBytes))
                    .put(buffer.toRequestBody(video.mimeType.toMediaType(), 0, read))
                    .build()
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 308 -> {
                            val next = UploadRules.nextOffset(response.header("Range"))
                            if (next != offset + read) {
                                throw IOException("Server stored $next bytes, expected ${offset + read}")
                            }
                            offset = next
                            onProgress(offset)
                        }
                        response.isSuccessful -> return videoId(response)
                        response.code == 404 || response.code == 410 -> return null
                        else -> throwFor(response)
                    }
                }
            }
        }
    }

    private fun queryOffset(sessionUrl: String, video: Video, token: String): SessionState {
        val request = Request.Builder()
            .url(sessionUrl)
            .header("Authorization", "Bearer $token")
            .header("Content-Range", "bytes */${video.sizeBytes}")
            .put(ByteArray(0).toRequestBody())
            .build()
        client.newCall(request).execute().use { response ->
            return when {
                response.code == 308 -> SessionState.Resume(UploadRules.nextOffset(response.header("Range")))
                response.isSuccessful -> SessionState.Finished(videoId(response))
                response.code == 404 || response.code == 410 -> SessionState.Expired
                else -> throwFor(response)
            }
        }
    }

    private fun videoId(response: Response): String =
        JSONObject(response.body?.string().orEmpty()).getString("id")

    private fun throwFor(response: Response): Nothing {
        val body = response.body?.string().orEmpty()
        when {
            response.code == 401 -> throw TokenRejected()
            UploadRules.isQuotaError(body) -> throw QuotaExceeded()
            response.code == 408 || response.code == 429 || response.code >= 500 ->
                throw IOException("YouTube returned ${response.code}")
            else -> throw UploadRejected("YouTube returned ${response.code}: ${body.take(300)}")
        }
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) throw UploadRejected("The file got shorter during upload")
            remaining -= skipped
        }
    }

    private fun readChunk(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = input.read(buffer, total, buffer.size - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    private sealed interface SessionState {
        data class Resume(val offset: Long) : SessionState
        data class Finished(val videoId: String) : SessionState
        data object Expired : SessionState
    }

    private companion object {
        const val START_URL =
            "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status"
        val JSON = "application/json; charset=UTF-8".toMediaType()
    }
}
