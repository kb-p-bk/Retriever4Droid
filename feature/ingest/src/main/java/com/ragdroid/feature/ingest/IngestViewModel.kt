package com.ragdroid.feature.ingest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragdroid.core.common.Result
import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.IngestionStatus
import com.ragdroid.data.repository.SettingsRepository
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.usecase.IngestDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IngestViewModel @Inject constructor(
    private val ingestDocumentUseCase: IngestDocumentUseCase,
    private val documentRepository: DocumentRepository,
    private val notificationHelper: IngestionNotificationHelper,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngestUiState(chunkConfig = settingsRepository.getChunkConfig()))
    val uiState: StateFlow<IngestUiState> = _uiState.asStateFlow()

    init {
        // Observe available tags from user's library
        viewModelScope.launch {
            documentRepository.observeAllDocuments().collect { docs ->
                val allTags = docs.flatMap { it.tags }.toSet()
                _uiState.update { it.copy(availableTags = allTags) }
            }
        }
    }

    fun onFilesSelected(uris: List<String>, names: List<String>) {
        _uiState.update {
            it.copy(
                selectedFileUris = uris,
                selectedFileNames = names,
                error = null,
                isComplete = false,
                ingestedDocuments = emptyList()
            )
        }
    }

    fun onChunkConfigChanged(config: ChunkConfig) {
        _uiState.update { it.copy(chunkConfig = config) }
    }

    fun onStartIngestion() {
        val uris = _uiState.value.selectedFileUris
        if (uris.isEmpty()) return

        _uiState.update {
            it.copy(
                isIngesting = true,
                totalFiles = uris.size,
                filesCompleted = 0,
                error = null,
                isComplete = false,
                ingestedDocuments = emptyList()
            )
        }

        viewModelScope.launch {
            var completed = 0
            for (uri in uris) {
                ingestDocumentUseCase(uri, _uiState.value.chunkConfig).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val progress = result.data
                            _uiState.update {
                                it.copy(
                                    currentFileName = progress.fileName,
                                    currentStatus = progress.status,
                                    progressMessage = progress.message,
                                    chunksProcessed = progress.chunksProcessed,
                                    totalChunks = progress.totalChunks
                                )
                            }

                            // Post live system notification for background progress
                            notificationHelper.showProgressNotification(
                                title = "Ingesting ${progress.fileName}",
                                message = progress.message,
                                progress = progress.chunksProcessed,
                                max = progress.totalChunks
                            )

                            if (progress.status == IngestionStatus.COMPLETED) {
                                completed++
                                _uiState.update { it.copy(filesCompleted = completed) }
                            }
                        }
                        is Result.Error -> {
                            completed++
                            _uiState.update {
                                it.copy(
                                    filesCompleted = completed,
                                    error = result.message
                                )
                            }
                        }
                        is Result.Loading -> { /* handled by isIngesting flag */ }
                    }
                }
            }

            // Fetch newly ingested documents to allow tagging right away
            val recentDocs = documentRepository.observeAllDocuments().firstOrNull() ?: emptyList()
            val justIngested = recentDocs.take(completed)

            notificationHelper.showCompletionNotification(
                title = "Ingestion Complete!",
                message = "$completed file(s) indexed successfully."
            )

            _uiState.update {
                it.copy(
                    isIngesting = false,
                    isComplete = true,
                    ingestedDocuments = justIngested,
                    progressMessage = "All files processed!"
                )
            }
        }
    }

    fun onUpdateDocumentTags(documentId: String, newTags: Set<String>) {
        viewModelScope.launch {
            val doc = documentRepository.getDocument(documentId) ?: return@launch
            val updated = doc.copy(tags = newTags, updatedAt = System.currentTimeMillis())
            documentRepository.upsertDocument(updated)

            _uiState.update { state ->
                val updatedList = state.ingestedDocuments.map { if (it.id == documentId) updated else it }
                state.copy(ingestedDocuments = updatedList)
            }
        }
    }

    fun onReset() {
        notificationHelper.cancelNotification()
        _uiState.value = IngestUiState(chunkConfig = settingsRepository.getChunkConfig())
    }
}
