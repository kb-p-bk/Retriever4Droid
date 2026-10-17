package com.ragdroid.data.embedding

/**
 * Internal interface for embedding backends within the data:embedding module.
 *
 * Mirrors [com.ragdroid.domain.service.EmbeddingPort] but lives in the data layer
 * so that alternative implementations (ONNX, MediaPipe, etc.) can be expressed as
 * concrete classes without directly depending on the domain port.
 *
 * [OnnxEmbeddingService] implements [EmbeddingPort] directly, but this interface
 * makes it straightforward to add a second backend (e.g. MediaPipe) and switch
 * between them in [EmbeddingModule] without touching domain code.
 */
interface EmbeddingService {

    /**
     * Generate a single embedding vector for the given text.
     *
     * @param text The input text to embed.
     * @return A float array of length [dimensions].
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Generate embedding vectors for a batch of texts.
     * Implementations should process the batch efficiently (e.g. batched inference).
     *
     * @param texts Input texts to embed.
     * @return List of float arrays in the same order as [texts].
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>

    /**
     * The output dimensionality of this embedding model.
     * For all-MiniLM-L6-v2 this is 384.
     */
    fun dimensions(): Int

    /**
     * Returns true if the model is loaded and ready to produce embeddings.
     * Call this before the first [embed] if you want to surface a loading state.
     */
    suspend fun isReady(): Boolean
}
