package com.ragdroid.domain.service

import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.TextChunk

/**
 * Port interface for text chunking.
 * The domain layer defines WHAT chunking must produce;
 * the data:chunking module defines HOW (which splitting algorithm to use).
 */
interface ChunkingPort {

    /**
     * Split parsed document content into chunks according to the given configuration.
     *
     * @param documentId The parent document's ID (assigned to each chunk).
     * @param content The parsed document content to chunk.
     * @param config Chunking parameters (size, overlap, strategy).
     * @return Ordered list of [TextChunk] instances.
     */
    suspend fun chunk(
        documentId: String,
        content: ParsingPort.ParsedContent,
        config: ChunkConfig
    ): List<TextChunk>
}
