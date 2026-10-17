package com.ragdroid.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragdroid.core.model.TextChunk
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.repository.VectorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChunkViewerUiState(
    val documentTitle: String = "",
    val allChunks: List<TextChunk> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChunkViewerViewModel @Inject constructor(
    private val vectorRepository: VectorRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChunkViewerUiState())
    val uiState: StateFlow<ChunkViewerUiState> = _uiState.asStateFlow()

    fun loadDocument(documentId: String, initialChunkIndex: Int) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Load document metadata
                val document = documentRepository.getDocument(documentId)
                if (document == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Document not found"
                        )
                    }
                    return@launch
                }

                // Load all chunks for the document
                val chunks = vectorRepository.getChunksForDocument(documentId)
                
                _uiState.update {
                    it.copy(
                        documentTitle = document.title,
                        allChunks = chunks,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load document: ${e.message}"
                    )
                }
            }
        }
    }
}
