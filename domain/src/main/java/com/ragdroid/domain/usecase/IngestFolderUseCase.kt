package com.ragdroid.domain.usecase

import com.ragdroid.core.common.DispatcherProvider
import com.ragdroid.core.common.Result
import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Recursively ingests all supported documents within a folder.
 * Delegates to [IngestDocumentUseCase] for each discovered file.
 */
class IngestFolderUseCase(
    private val fileRepository: FileRepository,
    private val ingestDocumentUseCase: IngestDocumentUseCase,
    private val dispatchers: DispatcherProvider
) {
    data class FolderProgress(
        val totalFiles: Int,
        val completedFiles: Int,
        val currentFile: IngestDocumentUseCase.IngestionProgress?
    )

    /**
     * Ingest all supported files in the given folder URI.
     *
     * @param folderUri The folder URI to scan.
     * @param chunkConfig Configuration for text chunking.
     * @return A Flow of [Result] wrapping [FolderProgress] updates.
     */
    operator fun invoke(
        folderUri: String,
        chunkConfig: ChunkConfig = ChunkConfig()
    ): Flow<Result<FolderProgress>> = flow {
        val files = fileRepository.resolveFiles(listOf(folderUri))

        if (files.isEmpty()) {
            emit(Result.Error("No supported files found in folder"))
            return@flow
        }

        val totalFiles = files.size
        var completed = 0

        emit(Result.Success(FolderProgress(totalFiles, completed, null)))

        files.forEach { file ->
            ingestDocumentUseCase(file.uri, chunkConfig).collect { result ->
                when (result) {
                    is Result.Success -> {
                        if (result.data.status == com.ragdroid.core.model.IngestionStatus.COMPLETED) {
                            completed++
                        }
                        emit(Result.Success(FolderProgress(totalFiles, completed, result.data)))
                    }
                    is Result.Error -> {
                        completed++
                        emit(Result.Success(
                            FolderProgress(totalFiles, completed, null)
                        ))
                    }
                    is Result.Loading -> { /* skip */ }
                }
            }
        }
    }.flowOn(dispatchers.io)
}
