package com.ragdroid.feature.library

import com.ragdroid.core.model.Document

data class LibraryUiState(
    val documents: List<Document> = emptyList(),
    val filteredDocuments: List<Document> = emptyList(),
    val isLoading: Boolean = true,
    val deleteConfirmDocId: String? = null,
    val deletingDocId: String? = null,
    val isDeleting: Boolean = false,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val allTags: Set<String> = emptySet(),
    val groupByTag: Boolean = false,
    val tagDialogDocId: String? = null,
    val tagDialogTags: Set<String> = emptySet()
) {
    /**
     * Total index size across all documents
     */
    val totalIndexSize: Long
        get() = documents.sumOf { it.indexSizeBytes }

    /**
     * Total original file size across all documents
     */
    val totalFileSize: Long
        get() = documents.sumOf { it.fileSizeBytes }

    /**
     * Formatted total sizes for display
     */
    val storageSummary: String
        get() {
            val fileSize = formatBytes(totalFileSize)
            val indexSize = formatBytes(totalIndexSize)
            return "Files: $fileSize • Index: $indexSize"
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
