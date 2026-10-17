package com.ragdroid.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ragdroid.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File repository implementation using Android's Storage Access Framework (SAF).
 * Handles both single files and recursive folder traversal.
 */
@Singleton
class SafFileAccessService @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {

    override suspend fun resolveFiles(uris: List<String>): List<FileRepository.ResolvedFile> =
        withContext(Dispatchers.IO) {
            uris.flatMap { uriString ->
                val uri = Uri.parse(uriString)
                val docFile = DocumentFile.fromSingleUri(context, uri)
                    ?: DocumentFile.fromTreeUri(context, uri)
                    ?: return@flatMap emptyList()

                if (docFile.isDirectory) {
                    resolveDirectory(docFile)
                } else {
                    listOfNotNull(resolveFile(docFile))
                }
            }
        }

    override suspend fun openInputStream(uri: String): InputStream =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(Uri.parse(uri))
                ?: throw IllegalArgumentException("Cannot open input stream for: $uri")
        }

    override suspend fun isDirectory(uri: String): Boolean = withContext(Dispatchers.IO) {
        val docFile = DocumentFile.fromSingleUri(context, Uri.parse(uri))
            ?: DocumentFile.fromTreeUri(context, Uri.parse(uri))
        docFile?.isDirectory == true
    }

    /**
     * Recursively discover all supported files within a directory.
     */
    private fun resolveDirectory(directory: DocumentFile): List<FileRepository.ResolvedFile> {
        val files = mutableListOf<FileRepository.ResolvedFile>()

        directory.listFiles().forEach { child ->
            if (child.isDirectory) {
                files.addAll(resolveDirectory(child))
            } else {
                resolveFile(child)?.let { files.add(it) }
            }
        }

        return files
    }

    /**
     * Resolve a single file into a [FileRepository.ResolvedFile] if it has a supported format.
     */
    private fun resolveFile(file: DocumentFile): FileRepository.ResolvedFile? {
        val name = file.name ?: return null
        val format = FileTypeDetector.fromFileName(name)
            ?: file.type?.let { FileTypeDetector.fromMimeType(it) }
            ?: return null

        return FileRepository.ResolvedFile(
            uri = file.uri.toString(),
            fileName = name,
            format = format,
            sizeBytes = file.length(),
            mimeType = file.type
        )
    }
}
