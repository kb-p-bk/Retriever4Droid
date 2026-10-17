package com.ragdroid.domain.repository

import com.ragdroid.core.model.DocumentFormat
import java.io.InputStream

/**
 * Repository for file system access — abstracts SAF and direct file access.
 */
interface FileRepository {

    /**
     * Represents a resolved file with its metadata.
     */
    data class ResolvedFile(
        val uri: String,
        val fileName: String,
        val format: DocumentFormat,
        val sizeBytes: Long,
        val mimeType: String? = null
    )

    /**
     * Resolve files from a list of URIs. For folder URIs, recursively
     * discovers all supported files within the folder.
     *
     * @param uris List of file or folder URI strings.
     * @return List of resolved files with their metadata.
     */
    suspend fun resolveFiles(uris: List<String>): List<ResolvedFile>

    /**
     * Open an input stream for reading file content.
     * Caller is responsible for closing the stream.
     *
     * @param uri The file URI to open.
     * @return An [InputStream] for reading the file.
     */
    suspend fun openInputStream(uri: String): InputStream

    /**
     * Check if a URI points to a directory/folder.
     */
    suspend fun isDirectory(uri: String): Boolean
}
