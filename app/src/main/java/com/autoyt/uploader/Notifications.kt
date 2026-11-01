package com.autoyt.uploader

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifications {
    const val PROGRESS_ID = 1
    private const val ALERT_ID = 2
    private const val BATCH_ID = 3
    private const val CHANNEL = "uploads"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Uploads", NotificationManager.IMPORTANCE_LOW)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun progress(context: Context, name: String, percent: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_upload)
            .setContentTitle("Uploading to YouTube")
            .setContentText(name)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setSilent(true)
            .build()

    /** Uses its own id so it can't replace a failure alert that is still unread. */
    fun batchDone(context: Context, count: Int) =
        alert(context, "${UploadRules.videoCount(count)} uploaded privately.", BATCH_ID)

    fun alert(context: Context, text: String, id: Int = ALERT_ID) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_upload)
            .setContentTitle("Auto YT Uploader")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
