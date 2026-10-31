package com.autoyt.uploader

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Video.Media

data class Album(val id: String, val name: String)

data class Video(
    val id: Long,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedMillis: Long,
)

class VideoLibrary(private val context: Context) {
    private val collection = Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    fun albums(): List<Album> {
        val projection = arrayOf(Media.BUCKET_ID, Media.BUCKET_DISPLAY_NAME)
        val albums = linkedMapOf<String, Album>()
        context.contentResolver.query(collection, projection, null, null, Media.BUCKET_DISPLAY_NAME)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                if (name != UPLOADED_ALBUM) albums[id] = Album(id, name)
            }
        }
        return albums.values.toList()
    }

    /** MediaStore hides IS_PENDING rows by default, so in-progress recordings are excluded. */
    fun videos(albumId: String): List<Video> {
        val projection = arrayOf(Media._ID, Media.DISPLAY_NAME, Media.SIZE, Media.MIME_TYPE, Media.DATE_MODIFIED)
        val result = mutableListOf<Video>()
        context.contentResolver.query(
            collection, projection, "${Media.BUCKET_ID} = ?", arrayOf(albumId), "${Media.DATE_ADDED} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val size = c.getLong(2)
                if (size <= 0) continue
                val id = c.getLong(0)
                result += Video(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = c.getString(1) ?: "video",
                    sizeBytes = size,
                    mimeType = c.getString(3) ?: "video/*",
                    modifiedMillis = c.getLong(4) * 1000,
                )
            }
        }
        return result
    }

    fun moveToUploaded(video: Video): Boolean {
        val values = ContentValues().apply {
            put(Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$UPLOADED_ALBUM/")
        }
        return try {
            context.contentResolver.update(video.uri, values, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val UPLOADED_ALBUM = "Uploaded"
    }
}
