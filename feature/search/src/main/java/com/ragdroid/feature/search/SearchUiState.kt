package com.ragdroid.feature.search

import com.ragdroid.core.model.Document
import com.ragdroid.core.model.SearchResult

/**
 * UI state for the Search screen.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val availableDocuments: List<Document> = emptyList(),
    val docFilterSearchQuery: String = "",
    val selectedDocumentIds: Set<String> = emptySet(), // empty = search all
    val availableTags: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet(), // empty = no tag filter
    val topK: Int = 20,
    val isFilterBarExpanded: Boolean = false,
    val expandChunksByDefault: Boolean = false,
    val userExpandedChunkIds: Set<String> = emptySet(),
    val userCollapsedChunkIds: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
) {
    val filteredAvailableDocuments: List<Document>
        get() {
            if (docFilterSearchQuery.isBlank()) return availableDocuments
            val q = docFilterSearchQuery.trim().lowercase()
            return availableDocuments.filter {
                it.title.lowercase().contains(q) || it.fileName.lowercase().contains(q)
            }
        }

    val activeFilterCount: Int
        get() = selectedTags.size + selectedDocumentIds.size

    fun isChunkExpanded(chunkId: String): Boolean {
        if (chunkId in userExpandedChunkIds) return true
        if (chunkId in userCollapsedChunkIds) return false
        return expandChunksByDefault
    }
}
