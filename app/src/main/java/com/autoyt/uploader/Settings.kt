package com.autoyt.uploader

import android.content.Context

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var albumId: String?
        get() = prefs.getString("album_id", null)
        set(value) = prefs.edit().putString("album_id", value).apply()

    var albumName: String?
        get() = prefs.getString("album_name", null)
        set(value) = prefs.edit().putString("album_name", value).apply()

    var mobileLimitMb: Int
        get() = prefs.getInt("mobile_limit_mb", 100)
        set(value) = prefs.edit().putInt("mobile_limit_mb", value).apply()

    var pausedUntil: Long
        get() = prefs.getLong("paused_until", 0)
        set(value) = prefs.edit().putLong("paused_until", value).apply()

    var needsSignIn: Boolean
        get() = prefs.getBoolean("needs_sign_in", true)
        set(value) = prefs.edit().putBoolean("needs_sign_in", value).apply()
}
