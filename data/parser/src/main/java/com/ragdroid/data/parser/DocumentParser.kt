package com.ragdroid.data.parser

import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.domain.service.ParsingPort
import java.io.InputStream

/**
 * Interface for format-specific document parsers.
 * Each parser handles exactly one [DocumentFormat].
 */
interface DocumentParser {

    /** The format this parser handles. */
    val supportedFormat: DocumentFormat

    /**
     * Parse raw file content into structured text.
     *
     * @param inputStream The file content stream.
     * @param fileName The original file name.
     * @return Parsed content with extracted text and metadata.
     */
    suspend fun parse(inputStream: InputStream, fileName: String): ParsingPort.ParsedContent
}
