package com.ragdroid.domain.usecase

import com.ragdroid.core.model.Document
import com.ragdroid.core.model.IngestionStatus
import com.ragdroid.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Provides reactive observation of ingestion status for the UI.
 */
class GetIngestionStatusUseCase(
    private val documentRepository: DocumentRepository
) {
    /** Observe all documents to track their ingestion status in real time. */
    fun observeAll(): Flow<List<Document>> =
        documentRepository.observeAllDocuments()

    /** Observe a specific document's status. */
    fun observe(documentId: String): Flow<Document?> =
        documentRepository.observeDocument(documentId)

    /** Get all documents currently being processed. */
    suspend fun getInProgress(): List<Document> =
        documentRepository.getDocumentsByStatus(IngestionStatus.PARSING) +
        documentRepository.getDocumentsByStatus(IngestionStatus.CHUNKING) +
        documentRepository.getDocumentsByStatus(IngestionStatus.EMBEDDING)
}
