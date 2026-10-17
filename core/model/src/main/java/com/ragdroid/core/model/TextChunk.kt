package com.ragdroid.core.model

/**
 * A chunk of text extracted from a document, ready for embedding.
 */
data class TextChunk(
    val id: String,
    val documentId: String,
    val text: String,
    val chunkIndex: Int,
    val chapterTitle: String? = null,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)
