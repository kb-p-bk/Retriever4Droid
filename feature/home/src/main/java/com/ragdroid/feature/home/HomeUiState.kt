package com.ragdroid.feature.home

import com.ragdroid.core.model.Document

data class HomeUiState(
    val documentCount: Int = 0,
    val recentDocuments: List<Document> = emptyList(),
    val allDocuments: List<Document> = emptyList(),
    val isLoading: Boolean = true
) {
    /**
     * Total index size across all documents
     */
    val totalIndexSize: Long
        get() = allDocuments.sumOf { it.indexSizeBytes }

    /**
     * Total original file size across all documents
     */
    val totalFileSize: Long
        get() = allDocuments.sumOf { it.fileSizeBytes }

    /**
     * Total chunks across all documents
     */
    val totalChunks: Int
        get() = allDocuments.sumOf { it.chunkCount }

    /**
     * Formatted storage summary
     */
    val storageSummary: String
        get() {
            if (totalIndexSize == 0L) return "No storage used"
            return "Using ${formatBytes(totalIndexSize)} for index"
        }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
