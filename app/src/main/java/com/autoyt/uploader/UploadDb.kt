package com.autoyt.uploader

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

enum class UploadStatus { UPLOADING, DONE, FAILED }

data class UploadRecord(
    val mediaId: Long,
    val name: String,
    val status: UploadStatus,
    val sessionUrl: String?,
    val videoId: String?,
    val message: String?,
    val albumName: String? = null,
    val updatedAt: Long = 0,
) {
    companion object {
        const val LEFT_IN_FOLDER = "Left in its folder"
    }
}

/** Tracks every video the app has touched: what prevents duplicates and enables resuming. */
class UploadDb(context: Context) : SQLiteOpenHelper(context, "uploads.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE uploads (
                media_id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                status TEXT NOT NULL,
                session_url TEXT,
                video_id TEXT,
                message TEXT,
                album_name TEXT,
                updated_at INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE uploads ADD COLUMN album_name TEXT")
    }

    fun find(mediaId: Long): UploadRecord? =
        query("media_id = ?", arrayOf(mediaId.toString()), null).firstOrNull()

    fun recent(limit: Int = 50): List<UploadRecord> =
        query(null, null, limit.toString())

    fun save(record: UploadRecord) {
        val values = ContentValues().apply {
            put("media_id", record.mediaId)
            put("name", record.name)
            put("status", record.status.name)
            put("session_url", record.sessionUrl)
            put("video_id", record.videoId)
            put("message", record.message)
            put("album_name", record.albumName)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("uploads", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun retryFailed() {
        writableDatabase.delete("uploads", "status = ?", arrayOf(UploadStatus.FAILED.name))
    }

    /** Forgetting the video puts it back in the queue. */
    fun retry(mediaId: Long) {
        writableDatabase.delete(
            "uploads", "media_id = ? AND status = ?", arrayOf(mediaId.toString(), UploadStatus.FAILED.name),
        )
    }

    fun doneSince(millis: Long): Int =
        DatabaseUtils.queryNumEntries(
            readableDatabase, "uploads", "status = ? AND updated_at >= ?",
            arrayOf(UploadStatus.DONE.name, millis.toString()),
        ).toInt()

    private fun query(where: String?, args: Array<String>?, limit: String?): List<UploadRecord> =
        readableDatabase.query("uploads", null, where, args, null, null, "updated_at DESC", limit).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        UploadRecord(
                            mediaId = c.getLong(c.getColumnIndexOrThrow("media_id")),
                            name = c.getString(c.getColumnIndexOrThrow("name")),
                            status = UploadStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status"))),
                            sessionUrl = c.getString(c.getColumnIndexOrThrow("session_url")),
                            videoId = c.getString(c.getColumnIndexOrThrow("video_id")),
                            message = c.getString(c.getColumnIndexOrThrow("message")),
                            albumName = c.getString(c.getColumnIndexOrThrow("album_name")),
                            updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at")),
                        )
                    )
                }
            }
        }
}
