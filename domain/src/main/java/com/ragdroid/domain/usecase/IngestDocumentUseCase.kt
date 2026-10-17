package com.ragdroid.domain.usecase

import com.ragdroid.core.common.DispatcherProvider
import com.ragdroid.core.common.Result
import com.ragdroid.core.common.runCatchingResult
import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.Document
import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.core.model.IngestionStatus
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.repository.FileRepository
import com.ragdroid.domain.repository.VectorRepository
import com.ragdroid.domain.service.ChunkingPort
import com.ragdroid.domain.service.EmbeddingPort
import com.ragdroid.domain.service.ParsingPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

/**
 * Orchestrates the full ingestion pipeline for a single document:
 *   resolve file → parse → chunk → embed → store
 *
 * Emits [IngestionProgress] events as a Flow so the UI can track progress.
 */
class IngestDocumentUseCase(
    private val fileRepository: FileRepository,
    private val documentRepository: DocumentRepository,
    private val vectorRepository: VectorRepository,
    private val parsingPort: ParsingPort,
    private val chunkingPort: ChunkingPort,
    private val embeddingPort: EmbeddingPort,
    private val dispatchers: DispatcherProvider
) {
    data class IngestionProgress(
        val documentId: String,
        val fileName: String,
        val status: IngestionStatus,
        val message: String,
        val chunksProcessed: Int = 0,
        val totalChunks: Int = 0
    )

    /**
     * Ingest a single file by URI.
     *
     * @param uri The file URI to ingest.
     * @param chunkConfig Configuration for text chunking.
     * @return A Flow of [Result] wrapping [IngestionProgress] updates.
     */
    operator fun invoke(
        uri: String,
        chunkConfig: ChunkConfig = ChunkConfig()
    ): Flow<Result<IngestionProgress>> = flow {
        val documentId = UUID.randomUUID().toString()

        // 1. Resolve file metadata
        val resolvedFiles = fileRepository.resolveFiles(listOf(uri))
        val file = resolvedFiles.firstOrNull()
            ?: throw IllegalArgumentException("Could not resolve file: $uri")

        // 2. Create document record
        val document = Document(
            id = documentId,
            title = file.fileName,
            format = file.format,
            sourceUri = file.uri,
            fileName = file.fileName,
            fileSizeBytes = file.sizeBytes,
            status = IngestionStatus.PENDING
        )
        documentRepository.upsertDocument(document)

        try {
            // 3. Parse
            emit(Result.Success(progress(documentId, file.fileName, IngestionStatus.PARSING, "Parsing document...")))
            documentRepository.updateStatus(documentId, IngestionStatus.PARSING)

            val inputStream = fileRepository.openInputStream(file.uri)
            val parsed = inputStream.use { stream ->
                parsingPort.parse(stream, file.fileName, file.format)
            }

            // Update document with parsed metadata
            documentRepository.upsertDocument(
                document.copy(
                    title = parsed.title ?: file.fileName,
                    author = parsed.author,
                    status = IngestionStatus.CHUNKING
                )
            )

            // 4. Chunk
            emit(Result.Success(progress(documentId, file.fileName, IngestionStatus.CHUNKING, "Chunking text...")))
            documentRepository.updateStatus(documentId, IngestionStatus.CHUNKING)

            val chunks = chunkingPort.chunk(documentId, parsed, chunkConfig)

            // 5. Embed in batches
            emit(Result.Success(progress(documentId, file.fileName, IngestionStatus.EMBEDDING, "Generating embeddings...")))
            documentRepository.updateStatus(documentId, IngestionStatus.EMBEDDING)

            val batchSize = 32
            val totalChunks = chunks.size
            var processed = 0

            chunks.chunked(batchSize).forEach { batch ->
                val embeddings = embeddingPort.embedBatch(batch.map { it.text })
                val embeddingVectors = batch.zip(embeddings).map { (chunk, vector) ->
                    com.ragdroid.core.model.EmbeddingVector(
                        chunkId = chunk.id,
                        vector = vector
                    )
                }
                vectorRepository.storeChunksWithEmbeddings(batch, embeddingVectors)

                processed += batch.size
                emit(Result.Success(
                    progress(
                        documentId, file.fileName, IngestionStatus.EMBEDDING,
                        "Embedding chunks ($processed/$totalChunks)...",
                        chunksProcessed = processed,
                        totalChunks = totalChunks
                    )
                ))
            }

            // 6. Mark complete
            documentRepository.upsertDocument(
                document.copy(
                    title = parsed.title ?: file.fileName,
                    author = parsed.author,
                    chunkCount = totalChunks,
                    status = IngestionStatus.COMPLETED,
                    updatedAt = System.currentTimeMillis()
                )
            )
            emit(Result.Success(
                progress(
                    documentId, file.fileName, IngestionStatus.COMPLETED,
                    "Ingestion complete! $totalChunks chunks indexed.",
                    chunksProcessed = totalChunks,
                    totalChunks = totalChunks
                )
            ))
        } catch (e: Exception) {
            documentRepository.updateStatus(documentId, IngestionStatus.FAILED)
            emit(Result.Error("Ingestion failed for ${file.fileName}: ${e.message}", e))
        }
    }.flowOn(dispatchers.io)

    private fun progress(
        documentId: String,
        fileName: String,
        status: IngestionStatus,
        message: String,
        chunksProcessed: Int = 0,
        totalChunks: Int = 0
    ) = IngestionProgress(documentId, fileName, status, message, chunksProcessed, totalChunks)
}
