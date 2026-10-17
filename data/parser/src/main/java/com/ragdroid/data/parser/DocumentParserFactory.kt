package com.ragdroid.data.parser

import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.domain.service.ParsingPort
import java.io.InputStream
import javax.inject.Inject

/**
 * Factory that routes parsing requests to the correct format-specific parser.
 * Implements the domain's [ParsingPort] interface.
 */
class DocumentParserFactory @Inject constructor(
    private val pdfParser: PdfDocumentParser,
    private val epubParser: EpubDocumentParser,
    private val mobiParser: MobiDocumentParser,
    private val docxParser: DocxDocumentParser,
    private val txtParser: TxtDocumentParser
) : ParsingPort {

    private val parsers: Map<DocumentFormat, DocumentParser> by lazy {
        listOf(pdfParser, epubParser, mobiParser, docxParser, txtParser)
            .associateBy { it.supportedFormat }
    }

    override suspend fun parse(
        inputStream: InputStream,
        fileName: String,
        format: DocumentFormat
    ): ParsingPort.ParsedContent {
        val parser = parsers[format]
            ?: throw UnsupportedOperationException("No parser available for format: $format")
        return parser.parse(inputStream, fileName)
    }

    override fun supports(format: DocumentFormat): Boolean = format in parsers
}
