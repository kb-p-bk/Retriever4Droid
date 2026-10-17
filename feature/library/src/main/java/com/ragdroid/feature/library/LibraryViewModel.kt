package com.ragdroid.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragdroid.core.model.Document
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.usecase.DeleteDocumentUseCase
import com.ragdroid.domain.usecase.GetIngestionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getIngestionStatusUseCase: GetIngestionStatusUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        getIngestionStatusUseCase.observeAll()
            .onEach { documents ->
                _uiState.update {
                    it.copy(
                        documents = documents,
                        isLoading = false
                    )
                }
                updateFilteredDocuments()
                loadAllTags()
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateFilteredDocuments()
    }

    fun onTagSelected(tag: String) {
        val currentTags = _uiState.value.selectedTags
        val newTags = if (tag in currentTags) {
            currentTags - tag
        } else {
            currentTags + tag
        }
        _uiState.update { it.copy(selectedTags = newTags) }
        updateFilteredDocuments()
    }

    fun onClearTagFilter() {
        _uiState.update { it.copy(selectedTags = emptySet()) }
        updateFilteredDocuments()
    }

    fun onToggleGroupByTag() {
        _uiState.update { it.copy(groupByTag = !it.groupByTag) }
    }

    fun onOpenTagDialog(documentId: String) {
        val doc = _uiState.value.documents.find { it.id == documentId }
        _uiState.update {
            it.copy(
                tagDialogDocId = documentId,
                tagDialogTags = doc?.tags ?: emptySet()
            )
        }
    }

    fun onAddTagToDialog(tag: String) {
        _uiState.update {
            it.copy(tagDialogTags = it.tagDialogTags + tag.trim())
        }
    }

    fun onRemoveTagFromDialog(tag: String) {
        _uiState.update {
            it.copy(tagDialogTags = it.tagDialogTags - tag)
        }
    }

    fun onSaveTags() {
        val docId = _uiState.value.tagDialogDocId ?: return
        val newTags = _uiState.value.tagDialogTags
        val doc = _uiState.value.documents.find { it.id == docId } ?: return

        viewModelScope.launch {
            val tagsToAdd = newTags - doc.tags
            val tagsToRemove = doc.tags - newTags

            if (tagsToAdd.isNotEmpty()) {
                documentRepository.addTags(docId, tagsToAdd)
            }
            if (tagsToRemove.isNotEmpty()) {
                documentRepository.removeTags(docId, tagsToRemove)
            }

            _uiState.update {
                it.copy(tagDialogDocId = null, tagDialogTags = emptySet())
            }
            loadAllTags()
        }
    }

    fun onCancelTagDialog() {
        _uiState.update {
            it.copy(tagDialogDocId = null, tagDialogTags = emptySet())
        }
    }

    fun onDeleteRequested(documentId: String) {
        _uiState.update { it.copy(deleteConfirmDocId = documentId) }
    }

    fun onDeleteConfirmed() {
        val docId = _uiState.value.deleteConfirmDocId ?: return
        _uiState.update {
            it.copy(
                deleteConfirmDocId = null,
                deletingDocId = docId,
                isDeleting = true
            )
        }

        viewModelScope.launch {
            try {
                deleteDocumentUseCase(docId)
            } finally {
                _uiState.update {
                    it.copy(
                        deletingDocId = null,
                        isDeleting = false
                    )
                }
            }
        }
    }

    fun onDeleteCancelled() {
        _uiState.update { it.copy(deleteConfirmDocId = null) }
    }

    private fun updateFilteredDocuments() {
        val query = _uiState.value.searchQuery.lowercase()
        val selectedTags = _uiState.value.selectedTags
        val allDocs = _uiState.value.documents

        val filtered = allDocs.filter { doc ->
            val matchesSearch = query.isBlank() ||
                doc.title.lowercase().contains(query) ||
                doc.fileName.lowercase().contains(query) ||
                doc.author?.lowercase()?.contains(query) == true

            val matchesTags = selectedTags.isEmpty() ||
                doc.tags.intersect(selectedTags).isNotEmpty()

            matchesSearch && matchesTags
        }

        _uiState.update { it.copy(filteredDocuments = filtered) }
    }

    private fun loadAllTags() {
        viewModelScope.launch {
            val tags = documentRepository.getAllTags()
            _uiState.update { it.copy(allTags = tags) }
        }
    }
}
