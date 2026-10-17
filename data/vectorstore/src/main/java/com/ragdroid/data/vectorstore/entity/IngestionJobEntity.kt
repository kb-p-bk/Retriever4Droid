package com.ragdroid.data.vectorstore.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * ObjectBox entity that tracks the progress of a background ingestion job.
 *
 * One [IngestionJobEntity] is created per ingestion request and updated as the
 * pipeline advances through its stages. This lets [IngestWorker] and the UI
 * resume or display progress even after a process restart.
 */
@Entity
data class IngestionJobEntity(
    @Id var id: Long = 0,

    /** Matches the [com.ragdroid.data.vectorstore.entity.DocumentEntity.documentId]. */
    var documentId: String = "",

    /** URI of the source file being ingested. */
    var sourceUri: String = "",

    /** Human-readable file name for display purposes. */
    var fileName: String = "",

    /**
     * Current pipeline stage. One of:
     *   PENDING, PARSING, CHUNKING, EMBEDDING, COMPLETED, FAILED
     */
    var status: String = "PENDING",

    /** Total number of chunks produced by the chunking step (0 until chunking completes). */
    var totalChunks: Int = 0,

    /** Number of chunks that have been embedded and stored so far. */
    var processedChunks: Int = 0,

    /** Human-readable progress message shown in notifications and the UI. */
    var progressMessage: String = "",

    /** Error message if [status] == "FAILED", otherwise empty. */
    var errorMessage: String = "",

    /** Epoch milliseconds when the job was created. */
    var createdAt: Long = System.currentTimeMillis(),

    /** Epoch milliseconds of the most recent status update. */
    var updatedAt: Long = System.currentTimeMillis(),

    /**
     * WorkManager request ID string, set when the job is enqueued so it can
     * be cancelled or observed via [WorkManager].
     */
    var workManagerId: String = ""
)
