package com.ragdroid.data.chunking

import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.TextChunk
import com.ragdroid.domain.service.ParsingPort
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentSplitters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Chunking strategy that uses LangChain4j's [DocumentSplitters.recursive] splitter.
 *
 * The recursive splitter tries to split on paragraph boundaries first, then sentence
 * boundaries, then word boundaries, and finally character boundaries — preserving as
 * much semantic coherence as possible at each level.
 *
 * This is the recommended default strategy for most document types.
 */
class RecursiveChunkingService @Inject constructor() : ChunkingService {

    override suspend fun chunk(
        documentId: String,
        content: ParsingPort.ParsedContent,
        config: ChunkConfig
    ): List<TextChunk> = withContext(Dispatchers.Default) {
        val splitter = DocumentSplitters.recursive(config.chunkSize, config.chunkOverlap)
        val doc = Document.from(content.rawText)
        val segments = splitter.split(doc)

        segments.mapIndexed { index, segment ->
            TextChunk(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                text = segment.text(),
                chunkIndex = index,
                chapterTitle = findChapter(content.chapters, segment.text()),
                metadata = mapOf("strategy" to "recursive")
            )
        }
    }

    /**
     * Best-effort chapter attribution: find which chapter contains the
     * first 50 characters of the chunk.
     */
    private fun findChapter(
        chapters: List<ParsingPort.Chapter>,
        chunkText: String
    ): String? {
        val preview = chunkText.take(50)
        return chapters.firstOrNull { it.content.contains(preview) }?.title
    }
}
