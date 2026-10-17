package com.ragdroid.core.model

/**
 * Represents a document that has been (or is being) ingested into the system.
 * This is the domain-level representation — storage entities map to/from this.
 */
data class Document(
    val id: String,
    val title: String,
    val author: String? = null,
    val format: DocumentFormat,
    val sourceUri: String,
    val fileName: String,
    val fileSizeBytes: Long = 0,
    val chunkCount: Int = 0,
    val status: IngestionStatus = IngestionStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
    val tags: Set<String> = emptySet(),
    val embeddingModel: String = "all-MiniLM-L6-v2"
) {
    /**
     * Calculate approximate index size in bytes.
     * Each chunk stores:
     * - Text (avg ~500 chars = ~500 bytes)
     * - Embedding vector (384 floats × 4 bytes = 1536 bytes)
     * - Metadata (~100 bytes)
     * Total: ~2136 bytes per chunk
     */
    val indexSizeBytes: Long
        get() = chunkCount * 2136L

    /**
     * Human-readable index size (e.g., "2.1 MB")
     */
    val indexSizeFormatted: String
        get() = formatBytes(indexSizeBytes)

    /**
     * Human-readable original file size (e.g., "5.2 MB")
     */
    val fileSizeFormatted: String
        get() = formatBytes(fileSizeBytes)

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}

/**
 * Tracks the ingestion lifecycle of a document.
 */
enum class IngestionStatus {
    /** Queued but not started. */
    PENDING,
    /** Currently being parsed. */
    PARSING,
    /** Currently being chunked. */
    CHUNKING,
    /** Currently generating embeddings. */
    EMBEDDING,
    /** Successfully ingested and searchable. */
    COMPLETED,
    /** Ingestion failed — see error metadata for details. */
    FAILED
}
