package com.ragdroid.data.parser

/**
 * Intermediate representation of a parsed document, internal to the data:parser module.
 * This maps 1:1 to [com.ragdroid.domain.service.ParsingPort.ParsedContent] but lives
 * in the data layer so parsers can construct it directly without importing domain types.
 *
 * The [DocumentParserFactory] converts this to [ParsingPort.ParsedContent] before
 * returning it to the domain layer.
 */
data class ParsedDocument(
    val title: String?,
    val author: String?,
    val chapters: List<ParsedChapter>,
    val rawText: String,
    val metadata: Map<String, String> = emptyMap()
)

data class ParsedChapter(
    val title: String?,
    val content: String,
    val index: Int
)
