package com.ragdroid.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragdroid.core.common.Result
import com.ragdroid.data.repository.SettingsRepository
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val documentRepository: DocumentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            expandChunksByDefault = settingsRepository.getExpandChunksByDefault(),
            topK = settingsRepository.getTopKDefault()
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Continuously observe available documents and derive available tags in real-time
        viewModelScope.launch {
            documentRepository.observeAllDocuments().collect { docs ->
                val allTags = docs.flatMap { it.tags }.toSet()
                _uiState.update {
                    it.copy(
                        availableDocuments = docs,
                        availableTags = allTags,
                        expandChunksByDefault = settingsRepository.getExpandChunksByDefault()
                    )
                }
            }
        }
    }

    fun onToggleFilterBarVisibility() {
        _uiState.update { it.copy(isFilterBarExpanded = !it.isFilterBarExpanded) }
    }

    fun onTopKChanged(topK: Int) {
        _uiState.update { it.copy(topK = topK.coerceIn(5, 100)) }
    }

    fun onToggleChunkExpanded(chunkId: String) {
        _uiState.update { state ->
            val isExpanded = state.isChunkExpanded(chunkId)
            if (isExpanded) {
                state.copy(
                    userExpandedChunkIds = state.userExpandedChunkIds - chunkId,
                    userCollapsedChunkIds = state.userCollapsedChunkIds + chunkId
                )
            } else {
                state.copy(
                    userExpandedChunkIds = state.userExpandedChunkIds + chunkId,
                    userCollapsedChunkIds = state.userCollapsedChunkIds - chunkId
                )
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onDocFilterSearchQueryChanged(query: String) {
        _uiState.update { it.copy(docFilterSearchQuery = query) }
    }

    fun onTagSelected(tag: String) {
        val currentTags = _uiState.value.selectedTags
        val newTags = if (tag in currentTags) {
            currentTags - tag
        } else {
            currentTags + tag
        }
        _uiState.update { it.copy(selectedTags = newTags) }
    }

    fun onToggleDocumentSelection(documentId: String) {
        val currentDocs = _uiState.value.selectedDocumentIds
        val newDocs = if (documentId in currentDocs) {
            currentDocs - documentId
        } else {
            currentDocs + documentId
        }
        _uiState.update { it.copy(selectedDocumentIds = newDocs) }
    }

    fun onSelectAllFilteredDocuments() {
        val allFilteredIds = _uiState.value.filteredAvailableDocuments.map { it.id }.toSet()
        _uiState.update { it.copy(selectedDocumentIds = allFilteredIds) }
    }

    fun onClearDocumentSelection() {
        _uiState.update { it.copy(selectedDocumentIds = emptySet()) }
    }

    fun onClearFilters() {
        _uiState.update {
            it.copy(
                selectedDocumentIds = emptySet(),
                selectedTags = emptySet(),
                docFilterSearchQuery = ""
            )
        }
    }

    fun onSearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                performSearch(query)
            }
        }
    }

    fun onClearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList(),
                hasSearched = false,
                error = null
            )
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update {
            it.copy(
                isSearching = true,
                error = null,
                expandChunksByDefault = settingsRepository.getExpandChunksByDefault()
            )
        }

        // Determine document IDs to search within
        val documentIdsToSearch = if (_uiState.value.selectedTags.isNotEmpty()) {
            // Get docs with selected tags
            val docsWithTags = documentRepository.getDocumentsByTags(_uiState.value.selectedTags)
            val tagDocIds = docsWithTags.map { it.id }.toSet()

            // Intersect with selected document IDs if any
            if (_uiState.value.selectedDocumentIds.isNotEmpty()) {
                tagDocIds.intersect(_uiState.value.selectedDocumentIds)
            } else {
                tagDocIds
            }
        } else if (_uiState.value.selectedDocumentIds.isNotEmpty()) {
            _uiState.value.selectedDocumentIds
        } else {
            null // Search all
        }

        when (val result = searchDocumentsUseCase(
            query = query,
            topK = _uiState.value.topK,
            documentIds = documentIdsToSearch
        )) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        results = result.data,
                        isSearching = false,
                        hasSearched = true,
                        error = null
                    )
                }
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        hasSearched = true,
                        error = result.message
                    )
                }
            }
            is Result.Loading -> {
                // Already handled
            }
        }
    }
}
