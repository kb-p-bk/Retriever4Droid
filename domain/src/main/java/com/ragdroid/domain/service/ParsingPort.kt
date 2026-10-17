package com.ragdroid.domain.service

import com.ragdroid.core.model.DocumentFormat
import java.io.InputStream

/**
 * Port interface for document parsing.
 * The domain layer defines WHAT parsing must do;
 * the data:parser module defines HOW (which libraries to use).
 */
interface ParsingPort {

    /**
     * Intermediate representation of a parsed document.
     */
    data class ParsedContent(
        val title: String?,
        val author: String?,
        val chapters: List<Chapter>,
        val rawText: String,
        val metadata: Map<String, String> = emptyMap()
    )

    data class Chapter(
        val title: String?,
        val content: String,
        val index: Int
    )

    /**
     * Parse a document from an input stream into structured text content.
     *
     * @param inputStream The file content stream.
     * @param fileName The original file name (used for metadata/fallback title).
     * @param format The document format.
     * @return Parsed content with extracted text, chapters, and metadata.
     */
    suspend fun parse(
        inputStream: InputStream,
        fileName: String,
        format: DocumentFormat
    ): ParsedContent

    /**
     * Check if this parser supports the given format.
     */
    fun supports(format: DocumentFormat): Boolean
}
