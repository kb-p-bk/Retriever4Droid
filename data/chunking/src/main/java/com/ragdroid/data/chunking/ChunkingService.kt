package com.ragdroid.data.chunking

import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.TextChunk
import com.ragdroid.domain.service.ParsingPort

/**
 * Internal interface for chunking strategy implementations within the data:chunking module.
 *
 * This sits below [com.ragdroid.domain.service.ChunkingPort] in the layering:
 *   - Domain sees only [ChunkingPort] (the port/adapter boundary).
 *   - [LangChainChunkingService] implements [ChunkingPort] and may delegate to
 *     concrete [ChunkingService] strategy classes.
 *
 * Each strategy focuses on a single splitting algorithm, making it easy to swap or
 * extend without modifying the top-level service.
 */
interface ChunkingService {

    /**
     * Split a parsed document's content into chunks.
     *
     * @param documentId ID of the parent document — assigned to every produced chunk.
     * @param content The parsed content to split.
     * @param config Chunking parameters (size, overlap).
     * @return Ordered list of [TextChunk] instances.
     */
    suspend fun chunk(
        documentId: String,
        content: ParsingPort.ParsedContent,
        config: ChunkConfig
    ): List<TextChunk>
}
