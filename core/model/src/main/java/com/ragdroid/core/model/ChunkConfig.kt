package com.ragdroid.core.model

/**
 * Configuration for text chunking strategies.
 */
data class ChunkConfig(
    /** Maximum number of characters per chunk. */
    val chunkSize: Int = 512,
    /** Number of overlapping characters between consecutive chunks. */
    val chunkOverlap: Int = 50,
    /** The chunking strategy to use. */
    val strategy: ChunkStrategy = ChunkStrategy.RECURSIVE
) {
    init {
        require(chunkSize > 0) { "chunkSize must be positive, was $chunkSize" }
        require(chunkOverlap >= 0) { "chunkOverlap must be non-negative, was $chunkOverlap" }
        require(chunkOverlap < chunkSize) { "chunkOverlap ($chunkOverlap) must be less than chunkSize ($chunkSize)" }
    }
}

/**
 * Available text chunking strategies.
 */
enum class ChunkStrategy {
    /** Recursive splitting by sentence → paragraph → character boundaries. */
    RECURSIVE,
    /** Splitting on paragraph boundaries. */
    PARAGRAPH,
    /** One chunk per chapter/section (for structured documents like EPUBs). */
    CHAPTER
}
