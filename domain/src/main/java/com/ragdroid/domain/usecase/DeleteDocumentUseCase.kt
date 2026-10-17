package com.ragdroid.domain.usecase

import com.ragdroid.core.common.DispatcherProvider
import com.ragdroid.core.common.Result
import com.ragdroid.core.common.runCatchingResult
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.repository.VectorRepository
import kotlinx.coroutines.withContext

/**
 * Deletes a document and all its associated chunks and embeddings.
 */
class DeleteDocumentUseCase(
    private val documentRepository: DocumentRepository,
    private val vectorRepository: VectorRepository,
    private val dispatchers: DispatcherProvider
) {
    /**
     * Delete a document and all its indexed data.
     *
     * @param documentId The ID of the document to delete.
     * @return [Result] indicating success or failure.
     */
    suspend operator fun invoke(documentId: String): Result<Unit> =
        withContext(dispatchers.io) {
            runCatchingResult {
                // Delete vectors/chunks first, then the document metadata
                vectorRepository.deleteByDocumentId(documentId)
                documentRepository.deleteDocument(documentId)
            }
        }
}
