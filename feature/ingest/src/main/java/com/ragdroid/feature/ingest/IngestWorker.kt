package com.ragdroid.feature.ingest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ragdroid.core.common.Result
import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.ChunkStrategy
import com.ragdroid.domain.usecase.IngestDocumentUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect

/**
 * WorkManager [CoroutineWorker] that runs the full ingestion pipeline
 * (parse → chunk → embed → store) in the background.
 *
 * The worker is enqueued by [IngestViewModel] when the user selects files.
 * It runs as a foreground service (via [ForegroundInfo]) so Android does not
 * kill it during long-running embedding tasks.
 *
 * ## Input Data Keys
 * - [KEY_FILE_URI]         — URI of the file to ingest (required)
 * - [KEY_CHUNK_SIZE]       — chunk size in characters (optional, default 512)
 * - [KEY_CHUNK_OVERLAP]    — overlap between chunks (optional, default 50)
 * - [KEY_CHUNK_STRATEGY]   — chunking strategy name (optional, default "RECURSIVE")
 *
 * ## Output Data Keys
 * - [KEY_DOCUMENT_ID]      — ID of the ingested document on success
 * - [KEY_CHUNK_COUNT]      — total number of chunks indexed on success
 * - [KEY_ERROR_MESSAGE]    — human-readable error description on failure
 */
@HiltWorker
class IngestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val ingestDocumentUseCase: IngestDocumentUseCase
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_FILE_URI = "file_uri"
        const val KEY_CHUNK_SIZE = "chunk_size"
        const val KEY_CHUNK_OVERLAP = "chunk_overlap"
        const val KEY_CHUNK_STRATEGY = "chunk_strategy"

        const val KEY_DOCUMENT_ID = "document_id"
        const val KEY_CHUNK_COUNT = "chunk_count"
        const val KEY_ERROR_MESSAGE = "error_message"

        private const val NOTIFICATION_CHANNEL_ID = "retriever4droid_ingestion"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val uri = inputData.getString(KEY_FILE_URI)
            ?: return Result.failure(workDataOf(KEY_ERROR_MESSAGE to "Missing file URI"))

        val chunkConfig = ChunkConfig(
            chunkSize = inputData.getInt(KEY_CHUNK_SIZE, 512),
            chunkOverlap = inputData.getInt(KEY_CHUNK_OVERLAP, 50),
            strategy = runCatching {
                ChunkStrategy.valueOf(
                    inputData.getString(KEY_CHUNK_STRATEGY) ?: ChunkStrategy.RECURSIVE.name
                )
            }.getOrDefault(ChunkStrategy.RECURSIVE)
        )

        // Promote to a foreground service so the OS won't kill us mid-embedding
        setForeground(buildForegroundInfo("Starting ingestion…"))

        var lastDocumentId: String? = null
        var lastChunkCount = 0
        var errorMessage: String? = null

        ingestDocumentUseCase(uri, chunkConfig).collect { result ->
            when (result) {
                is com.ragdroid.core.common.Result.Success -> {
                    val progress = result.data
                    lastDocumentId = progress.documentId
                    lastChunkCount = progress.totalChunks

                    // Update notification with current stage message
                    updateNotification(progress.message)

                    // Publish progress so observers (e.g. IngestViewModel) can react
                    setProgress(
                        workDataOf(
                            "status" to progress.status.name,
                            "message" to progress.message,
                            "chunksProcessed" to progress.chunksProcessed,
                            "totalChunks" to progress.totalChunks
                        )
                    )
                }

                is com.ragdroid.core.common.Result.Error -> {
                    errorMessage = result.message
                }

                is com.ragdroid.core.common.Result.Loading -> { /* no-op */ }
            }
        }

        return if (errorMessage != null) {
            Result.failure(workDataOf(KEY_ERROR_MESSAGE to errorMessage))
        } else {
            Result.success(
                workDataOf(
                    KEY_DOCUMENT_ID to lastDocumentId,
                    KEY_CHUNK_COUNT to lastChunkCount
                )
            )
        }
    }

    // ─── Notification helpers ─────────────────────────────────────────────────

    private fun buildForegroundInfo(message: String): ForegroundInfo {
        ensureNotificationChannel()
        val notification = buildNotification(message)
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(message: String) {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun buildNotification(message: String) =
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Retriever4Droid — Ingesting document")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Document Ingestion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while documents are being indexed"
            }
            val manager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
