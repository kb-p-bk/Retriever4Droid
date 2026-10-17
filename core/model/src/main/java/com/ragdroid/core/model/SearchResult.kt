package com.ragdroid.core.model

/**
 * A search result pairing a matched chunk with its similarity score
 * and parent document reference.
 */
data class SearchResult(
    val chunk: TextChunk,
    val document: Document,
    /** Cosine similarity score (0.0 to 1.0, higher = more relevant). */
    val score: Float
)
