package com.ragdroid.data.parser

import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.domain.service.ParsingPort
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

/**
 * PDF parser using PdfBox-Android.
 * Extracts text page-by-page to preserve document structure.
 */
class PdfDocumentParser @Inject constructor() : DocumentParser {

    override val supportedFormat: DocumentFormat = DocumentFormat.PDF

    override suspend fun parse(
        inputStream: InputStream,
        fileName: String
    ): ParsingPort.ParsedContent = withContext(Dispatchers.IO) {
        PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper()
            val info = document.documentInformation
            val totalPages = document.numberOfPages

            // Extract text page-by-page as "chapters"
            val chapters = (1..totalPages).map { page ->
                stripper.startPage = page
                stripper.endPage = page
                val pageText = stripper.getText(document).trim()
                ParsingPort.Chapter(
                    title = "Page $page",
                    content = pageText,
                    index = page - 1
                )
            }.filter { it.content.isNotBlank() }

            val rawText = chapters.joinToString("\n\n") { it.content }

            ParsingPort.ParsedContent(
                title = info?.title?.takeIf { it.isNotBlank() },
                author = info?.author?.takeIf { it.isNotBlank() },
                chapters = chapters,
                rawText = rawText,
                metadata = buildMap {
                    info?.subject?.let { put("subject", it) }
                    info?.keywords?.let { put("keywords", it) }
                    put("pageCount", totalPages.toString())
                }
            )
        }
    }
}
