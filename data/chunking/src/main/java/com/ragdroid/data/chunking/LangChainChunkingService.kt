package com.ragdroid.data.chunking

import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.ChunkStrategy
import com.ragdroid.core.model.TextChunk
import com.ragdroid.domain.service.ChunkingPort
import com.ragdroid.domain.service.ParsingPort
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter
import dev.langchain4j.data.document.splitter.DocumentSplitters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Chunking service implementation using LangChain4j text splitters.
 * Supports recursive, paragraph, and chapter-based strategies.
 */
class LangChainChunkingService @Inject constructor() : ChunkingPort {

    override suspend fun chunk(
        documentId: String,
        content: ParsingPort.ParsedContent,
        config: ChunkConfig
    ): List<TextChunk> = withContext(Dispatchers.Default) {
        when (config.strategy) {
            ChunkStrategy.RECURSIVE -> chunkRecursive(documentId, content, config)
            ChunkStrategy.PARAGRAPH -> chunkByParagraph(documentId, content, config)
            ChunkStrategy.CHAPTER -> chunkByChapter(documentId, content)
        }
    }

    private fun chunkRecursive(
        documentId: String,
        content: ParsingPort.ParsedContent,
        config: ChunkConfig
    ): List<TextChunk> {
        val splitter = DocumentSplitters.recursive(config.chunkSize, config.chunkOverlap)
        val doc = Document.from(content.rawText)
        val segments = splitter.split(doc)

        return segments.mapIndexed { index, segment ->
            TextChunk(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                text = segment.text(),
                chunkIndex = index,
                chapterTitle = findChapterForOffset(content.chapters, segment.text()),
                metadata = mapOf("strategy" to "recursive")
            )
        }
    }

    private fun chunkByParagraph(
        documentId: String,
        content: ParsingPort.ParsedContent,
        config: ChunkConfig
    ): List<TextChunk> {
        val splitter = DocumentByParagraphSplitter(config.chunkSize, config.chunkOverlap)
        val doc = Document.from(content.rawText)
        val segments = splitter.split(doc)

        return segments.mapIndexed { index, segment ->
            TextChunk(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                text = segment.text(),
                chunkIndex = index,
                chapterTitle = findChapterForOffset(content.chapters, segment.text()),
                metadata = mapOf("strategy" to "paragraph")
            )
        }
    }

    private fun chunkByChapter(
        documentId: String,
        content: ParsingPort.ParsedContent
    ): List<TextChunk> {
        return content.chapters.mapIndexed { index, chapter ->
            TextChunk(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                text = chapter.content,
                chunkIndex = index,
                chapterTitle = chapter.title,
                metadata = mapOf("strategy" to "chapter")
            )
        }
    }

    /**
     * Best-effort: find which chapter a chunk of text belongs to
     * by checking which chapter's content contains the chunk text.
     */
    private fun findChapterForOffset(
        chapters: List<ParsingPort.Chapter>,
        chunkText: String
    ): String? {
        val preview = chunkText.take(50)
        return chapters.firstOrNull { it.content.contains(preview) }?.title
    }
}
