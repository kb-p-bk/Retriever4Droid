package com.ragdroid.data.parser

import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.domain.service.ParsingPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.InputStream
import javax.inject.Inject

/**
 * EPUB parser using Epublib for EPUB structure and jsoup for HTML stripping.
 * Preserves chapter structure from the EPUB spine.
 */
class EpubDocumentParser @Inject constructor() : DocumentParser {

    override val supportedFormat: DocumentFormat = DocumentFormat.EPUB

    override suspend fun parse(
        inputStream: InputStream,
        fileName: String
    ): ParsingPort.ParsedContent = withContext(Dispatchers.IO) {
        val book = EpubReader().readEpub(inputStream)
        val metadata = book.metadata

        val chapters = book.spine.spineReferences.mapIndexed { index, ref ->
            val resource = ref.resource
            val htmlContent = String(resource.data, Charsets.UTF_8)
            val textContent = Jsoup.parse(htmlContent).text().trim()
            val title = ref.resource.title
                ?: Jsoup.parse(htmlContent).selectFirst("h1, h2, h3")?.text()

            ParsingPort.Chapter(
                title = title,
                content = textContent,
                index = index
            )
        }.filter { it.content.isNotBlank() }

        val rawText = chapters.joinToString("\n\n") { it.content }

        ParsingPort.ParsedContent(
            title = metadata.firstTitle?.takeIf { it.isNotBlank() },
            author = metadata.authors.firstOrNull()?.let { "${it.firstname} ${it.lastname}".trim() },
            chapters = chapters,
            rawText = rawText,
            metadata = buildMap {
                metadata.descriptions.firstOrNull()?.let { put("description", it) }
                metadata.publishers.firstOrNull()?.let { put("publisher", it) }
                put("chapterCount", chapters.size.toString())
            }
        )
    }
}
