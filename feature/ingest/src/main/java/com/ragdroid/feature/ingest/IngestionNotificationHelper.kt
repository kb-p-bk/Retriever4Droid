package com.ragdroid.feature.ingest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System notification helper for posting live background ingestion progress.
 */
@Singleton
class IngestionNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {
    companion object {
        const val CHANNEL_ID = "semantic_retriever_ingestion"
        const val NOTIFICATION_ID = 2001
    }

    init {
        ensureChannel()
    }

    fun showProgressNotification(title: String, message: String, progress: Int = 0, max: Int = 0) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (max > 0) {
            builder.setProgress(max, progress, false)
        } else {
            builder.setProgress(100, 0, true)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Handled if system notification permission is restricted
        }
    }

    fun showCompletionNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Handled if system notification permission is restricted
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Document Ingestion Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress in system notification bar while documents are being indexed"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
