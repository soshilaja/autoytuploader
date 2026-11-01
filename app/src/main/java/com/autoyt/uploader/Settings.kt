package com.autoyt.uploader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Folders to upload from, highest priority first. */
    var albums: List<Album>
        get() {
            val saved = prefs.getString("albums", null) ?: return singleAlbumFromOlderVersion()
            val array = JSONArray(saved)
            return List(array.length()) { index ->
                val album = array.getJSONObject(index)
                Album(album.getString("id"), album.getString("name"))
            }
        }
        set(value) {
            val array = JSONArray()
            value.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name)) }
            prefs.edit().putString("albums", array.toString()).apply()
        }

    /** Folder ids skipped by the worker. Kept apart from [albums] so a folder keeps its place while paused. */
    var pausedAlbums: Set<String>
        get() = prefs.getStringSet("paused_albums", null).orEmpty()
        set(value) = prefs.edit().putStringSet("paused_albums", value).apply()

    val activeAlbums: List<Album>
        get() = albums.filter { it.id !in pausedAlbums }

    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", false)
        set(value) = prefs.edit().putBoolean("wifi_only", value).apply()

    var moveAfterUpload: Boolean
        get() = prefs.getBoolean("move_after_upload", true)
        set(value) = prefs.edit().putBoolean("move_after_upload", value).apply()

    var batchNotifications: Boolean
        get() = prefs.getBoolean("batch_notifications", false)
        set(value) = prefs.edit().putBoolean("batch_notifications", value).apply()

    var mobileLimitMb: Int
        get() = prefs.getInt("mobile_limit_mb", 100)
        set(value) = prefs.edit().putInt("mobile_limit_mb", value).apply()

    var pausedUntil: Long
        get() = prefs.getLong("paused_until", 0)
        set(value) = prefs.edit().putLong("paused_until", value).apply()

    var needsSignIn: Boolean
        get() = prefs.getBoolean("needs_sign_in", true)
        set(value) = prefs.edit().putBoolean("needs_sign_in", value).apply()

    private fun singleAlbumFromOlderVersion(): List<Album> {
        val id = prefs.getString("album_id", null) ?: return emptyList()
        return listOf(Album(id, prefs.getString("album_name", null) ?: id))
    }
}
