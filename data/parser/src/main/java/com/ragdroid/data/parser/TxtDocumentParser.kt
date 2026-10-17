package com.ragdroid.data.parser

import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.domain.service.ParsingPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

/**
 * Plain text / Markdown parser using standard Kotlin IO.
 * Splits on double newlines to create paragraph-level chapters.
 */
class TxtDocumentParser @Inject constructor() : DocumentParser {

    override val supportedFormat: DocumentFormat = DocumentFormat.TXT

    override suspend fun parse(
        inputStream: InputStream,
        fileName: String
    ): ParsingPort.ParsedContent = withContext(Dispatchers.IO) {
        val rawText = inputStream.bufferedReader().readText()

        // Split by double newlines into sections
        val sections = rawText.split(Regex("\\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val chapters = sections.mapIndexed { index, section ->
            // Use first line as title if it looks like a heading (short, no period)
            val lines = section.lines()
            val firstLine = lines.firstOrNull()?.trim()
            val isHeading = firstLine != null &&
                firstLine.length < 100 &&
                !firstLine.endsWith('.') &&
                (firstLine.startsWith('#') || lines.size > 1)

            ParsingPort.Chapter(
                title = if (isHeading) firstLine?.removePrefix("#")?.trim() else null,
                content = if (isHeading && lines.size > 1) {
                    lines.drop(1).joinToString("\n").trim()
                } else {
                    section
                },
                index = index
            )
        }.filter { it.content.isNotBlank() }

        ParsingPort.ParsedContent(
            title = fileName.substringBeforeLast('.'),
            author = null,
            chapters = chapters,
            rawText = rawText,
            metadata = mapOf("charCount" to rawText.length.toString())
        )
    }
}
