package com.ragdroid.data.chunking

import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.TextChunk
import com.ragdroid.domain.service.ParsingPort
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Chunking strategy that splits on paragraph boundaries using LangChain4j's
 * [DocumentByParagraphSplitter].
 *
 * Paragraph splitting is best for well-structured documents (e.g. academic papers,
 * reports) where paragraphs represent coherent units of meaning. Chunks that exceed
 * [ChunkConfig.chunkSize] are further split by the underlying splitter.
 */
class ParagraphChunkingService @Inject constructor() : ChunkingService {

    override suspend fun chunk(
        documentId: String,
        content: ParsingPort.ParsedContent,
        config: ChunkConfig
    ): List<TextChunk> = withContext(Dispatchers.Default) {
        val splitter = DocumentByParagraphSplitter(config.chunkSize, config.chunkOverlap)
        val doc = Document.from(content.rawText)
        val segments = splitter.split(doc)

        segments.mapIndexed { index, segment ->
            TextChunk(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                text = segment.text(),
                chunkIndex = index,
                chapterTitle = findChapter(content.chapters, segment.text()),
                metadata = mapOf("strategy" to "paragraph")
            )
        }
    }

    private fun findChapter(
        chapters: List<ParsingPort.Chapter>,
        chunkText: String
    ): String? {
        val preview = chunkText.take(50)
        return chapters.firstOrNull { it.content.contains(preview) }?.title
    }
}
