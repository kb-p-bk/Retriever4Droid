package com.ragdroid.core.model

/**
 * Supported document formats for ingestion.
 */
enum class DocumentFormat(
    val displayName: String,
    val extensions: List<String>,
    val mimeTypes: List<String>
) {
    PDF(
        displayName = "PDF",
        extensions = listOf("pdf"),
        mimeTypes = listOf("application/pdf")
    ),
    EPUB(
        displayName = "EPUB",
        extensions = listOf("epub"),
        mimeTypes = listOf("application/epub+zip")
    ),
    MOBI(
        displayName = "MOBI",
        extensions = listOf("mobi", "azw", "azw3"),
        mimeTypes = listOf("application/x-mobipocket-ebook", "application/vnd.amazon.ebook")
    ),
    DOCX(
        displayName = "DOCX",
        extensions = listOf("docx"),
        mimeTypes = listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ),
    TXT(
        displayName = "Text",
        extensions = listOf("txt", "md", "markdown"),
        mimeTypes = listOf("text/plain", "text/markdown")
    );

    companion object {
        /** All supported file extensions across all formats. */
        val allExtensions: List<String> = entries.flatMap { it.extensions }

        /** Resolve format from a file extension (case-insensitive). Returns null if unsupported. */
        fun fromExtension(extension: String): DocumentFormat? =
            entries.firstOrNull { format ->
                format.extensions.any { it.equals(extension, ignoreCase = true) }
            }

        /** Resolve format from a MIME type. Returns null if unsupported. */
        fun fromMimeType(mimeType: String): DocumentFormat? =
            entries.firstOrNull { format ->
                format.mimeTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
    }
}
