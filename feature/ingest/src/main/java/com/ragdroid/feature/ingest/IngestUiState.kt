package com.ragdroid.feature.ingest

import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.Document
import com.ragdroid.core.model.IngestionStatus

data class IngestUiState(
    val selectedFileUris: List<String> = emptyList(),
    val selectedFileNames: List<String> = emptyList(),
    val chunkConfig: ChunkConfig = ChunkConfig(),
    val isIngesting: Boolean = false,
    val currentFileName: String? = null,
    val currentStatus: IngestionStatus? = null,
    val progressMessage: String? = null,
    val chunksProcessed: Int = 0,
    val totalChunks: Int = 0,
    val filesCompleted: Int = 0,
    val totalFiles: Int = 0,
    val error: String? = null,
    val isComplete: Boolean = false,
    val ingestedDocuments: List<Document> = emptyList(),
    val availableTags: Set<String> = emptySet()
)
