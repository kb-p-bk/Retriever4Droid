package com.ragdroid.data.file

import com.ragdroid.core.model.DocumentFormat

/**
 * Detects document format from file extension or MIME type.
 */
object FileTypeDetector {

    /**
     * Determine the [DocumentFormat] from a file name.
     * Returns null if the format is not supported.
     */
    fun fromFileName(fileName: String): DocumentFormat? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return DocumentFormat.fromExtension(extension)
    }

    /**
     * Determine the [DocumentFormat] from a MIME type string.
     * Returns null if the format is not supported.
     */
    fun fromMimeType(mimeType: String): DocumentFormat? {
        return DocumentFormat.fromMimeType(mimeType)
    }

    /**
     * Check if a file name has a supported document extension.
     */
    fun isSupported(fileName: String): Boolean = fromFileName(fileName) != null
}
