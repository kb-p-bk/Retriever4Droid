package com.ragdroid.domain.usecase

import com.ragdroid.core.common.DispatcherProvider
import com.ragdroid.core.common.Result
import com.ragdroid.core.common.runCatchingResult
import com.ragdroid.core.model.SearchResult
import com.ragdroid.domain.repository.VectorRepository
import com.ragdroid.domain.service.EmbeddingPort
import kotlinx.coroutines.withContext

/**
 * Embeds a text query and performs vector similarity search
 * against the stored document chunks.
 */
class SearchDocumentsUseCase(
    private val vectorRepository: VectorRepository,
    private val embeddingPort: EmbeddingPort,
    private val dispatchers: DispatcherProvider
) {
    /**
     * Search for document chunks most similar to the given query.
     *
     * @param query The natural language search query.
     * @param topK Maximum number of results to return.
     * @param minScore Minimum similarity threshold (0.0 to 1.0).
     * @param documentIds Optional set of document IDs to restrict search to.
     * @return [Result] containing ranked [SearchResult] list.
     */
    suspend operator fun invoke(
        query: String,
        topK: Int = 10,
        minScore: Float = 0.0f,
        documentIds: Set<String>? = null
    ): Result<List<SearchResult>> = withContext(dispatchers.io) {
        runCatchingResult {
            require(query.isNotBlank()) { "Search query must not be blank" }

            // 1. Embed the query
            val queryEmbedding = embeddingPort.embed(query)

            // 2. Vector similarity search
            vectorRepository.findNearest(
                queryEmbedding = queryEmbedding,
                topK = topK,
                minScore = minScore,
                documentIds = documentIds
            )
        }
    }
}
