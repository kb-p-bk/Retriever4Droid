package com.ragdroid.data.parser

import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.domain.service.ParsingPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream
import javax.inject.Inject

/**
 * DOCX parser using Apache POI.
 * Extracts text from paragraphs and tables.
 */
class DocxDocumentParser @Inject constructor() : DocumentParser {

    override val supportedFormat: DocumentFormat = DocumentFormat.DOCX

    override suspend fun parse(
        inputStream: InputStream,
        fileName: String
    ): ParsingPort.ParsedContent = withContext(Dispatchers.IO) {
        XWPFDocument(inputStream).use { document ->
            val properties = document.properties?.coreProperties

            // Group paragraphs by heading style into chapters
            val chapters = mutableListOf<ParsingPort.Chapter>()
            var currentTitle: String? = null
            val currentContent = StringBuilder()
            var chapterIndex = 0

            for (paragraph in document.paragraphs) {
                val style = paragraph.style
                val isHeading = style != null && style.startsWith("Heading", ignoreCase = true)

                if (isHeading && currentContent.isNotBlank()) {
                    chapters.add(
                        ParsingPort.Chapter(
                            title = currentTitle,
                            content = currentContent.toString().trim(),
                            index = chapterIndex++
                        )
                    )
                    currentContent.clear()
                    currentTitle = paragraph.text
                } else if (isHeading) {
                    currentTitle = paragraph.text
                } else {
                    if (paragraph.text.isNotBlank()) {
                        currentContent.appendLine(paragraph.text)
                    }
                }
            }

            // Add remaining content
            if (currentContent.isNotBlank()) {
                chapters.add(
                    ParsingPort.Chapter(
                        title = currentTitle,
                        content = currentContent.toString().trim(),
                        index = chapterIndex
                    )
                )
            }

            // Also extract table text
            for (table in document.tables) {
                val tableText = table.rows.joinToString("\n") { row ->
                    row.tableCells.joinToString(" | ") { it.text }
                }
                if (tableText.isNotBlank()) {
                    chapters.add(
                        ParsingPort.Chapter(
                            title = "Table",
                            content = tableText,
                            index = chapters.size
                        )
                    )
                }
            }

            val rawText = chapters.joinToString("\n\n") { it.content }

            ParsingPort.ParsedContent(
                title = properties?.title?.takeIf { it.isNotBlank() } ?: fileName.removeSuffix(".docx"),
                author = properties?.creator?.takeIf { it.isNotBlank() },
                chapters = chapters.ifEmpty {
                    listOf(ParsingPort.Chapter(title = null, content = rawText, index = 0))
                },
                rawText = rawText,
                metadata = buildMap {
                    properties?.description?.let { put("description", it) }
                    properties?.category?.let { put("category", it) }
                }
            )
        }
    }
}
