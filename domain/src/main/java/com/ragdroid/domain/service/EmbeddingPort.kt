package com.ragdroid.domain.service

/**
 * Port interface for text embedding generation.
 * The domain layer defines WHAT embedding must produce (FloatArray);
 * the data:embedding module defines HOW (ONNX Runtime, MediaPipe, etc.).
 */
interface EmbeddingPort {

    /**
     * Generate an embedding vector for a single text input.
     *
     * @param text The text to embed.
     * @return A float array representing the embedding vector.
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Generate embedding vectors for a batch of text inputs.
     * More efficient than calling [embed] repeatedly.
     *
     * @param texts The list of texts to embed.
     * @return List of float arrays, one per input text (same order).
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>

    /**
     * Get the dimensionality of the embedding model's output vectors.
     */
    fun dimensions(): Int

    /**
     * Check if the embedding model is loaded and ready.
     */
    suspend fun isReady(): Boolean
}
