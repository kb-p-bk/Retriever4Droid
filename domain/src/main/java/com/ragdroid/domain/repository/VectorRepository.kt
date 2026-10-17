package com.ragdroid.domain.repository

import com.ragdroid.core.model.EmbeddingVector
import com.ragdroid.core.model.SearchResult
import com.ragdroid.core.model.TextChunk

/**
 * Repository for storing and querying text chunks with their embedding vectors.
 * Wraps the vector database (ObjectBox with HNSW index).
 */
interface VectorRepository {

    /**
     * Store a batch of chunks with their corresponding embedding vectors.
     * Each chunk and its embedding are stored atomically.
     */
    suspend fun storeChunksWithEmbeddings(
        chunks: List<TextChunk>,
        embeddings: List<EmbeddingVector>
    )

    /**
     * Find the top-K most similar chunks to the given query embedding.
     *
     * @param queryEmbedding The embedding vector of the search query.
     * @param topK Maximum number of results to return.
     * @param minScore Minimum similarity score threshold (0.0 to 1.0).
     * @param documentIds Optional list of document IDs to restrict search to.
     * @return Ranked list of [SearchResult] with scores, highest first.
     */
    suspend fun findNearest(
        queryEmbedding: FloatArray,
        topK: Int = 10,
        minScore: Float = 0.0f,
        documentIds: Set<String>? = null
    ): List<SearchResult>

    /**
     * Delete all chunks and embeddings associated with a document.
     */
    suspend fun deleteByDocumentId(documentId: String)

    /**
     * Get the count of chunks stored for a specific document.
     */
    suspend fun getChunkCount(documentId: String): Int

    /**
     * Get all chunks for a specific document, ordered by chunk index.
     */
    suspend fun getChunksForDocument(documentId: String): List<TextChunk>
}
