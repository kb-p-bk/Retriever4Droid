package com.ragdroid.data.repository.di

import com.ragdroid.core.common.DefaultDispatcherProvider
import com.ragdroid.core.common.DispatcherProvider
import com.ragdroid.data.file.SafFileAccessService
import com.ragdroid.data.vectorstore.ObjectBoxVectorStore
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.repository.FileRepository
import com.ragdroid.domain.repository.VectorRepository
import com.ragdroid.domain.service.ChunkingPort
import com.ragdroid.domain.service.EmbeddingPort
import com.ragdroid.domain.service.ParsingPort
import com.ragdroid.domain.usecase.DeleteDocumentUseCase
import com.ragdroid.domain.usecase.GetIngestionStatusUseCase
import com.ragdroid.domain.usecase.IngestDocumentUseCase
import com.ragdroid.domain.usecase.IngestFolderUseCase
import com.ragdroid.domain.usecase.SearchDocumentsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides use case instances and the dispatcher provider.
 * Use cases are constructor-injected with their dependencies.
 * 
 * Note: Repository implementations are bound in their respective modules:
 * - DocumentRepository & VectorRepository → VectorStoreModule (via ObjectBoxVectorStore)
 * - FileRepository → FileModule (via SafFileAccessService)
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideIngestDocumentUseCase(
        fileRepository: FileRepository,
        documentRepository: DocumentRepository,
        vectorRepository: VectorRepository,
        parsingPort: ParsingPort,
        chunkingPort: ChunkingPort,
        embeddingPort: EmbeddingPort,
        dispatchers: DispatcherProvider
    ): IngestDocumentUseCase = IngestDocumentUseCase(
        fileRepository, documentRepository, vectorRepository,
        parsingPort, chunkingPort, embeddingPort, dispatchers
    )

    @Provides
    @Singleton
    fun provideIngestFolderUseCase(
        fileRepository: FileRepository,
        ingestDocumentUseCase: IngestDocumentUseCase,
        dispatchers: DispatcherProvider
    ): IngestFolderUseCase = IngestFolderUseCase(
        fileRepository, ingestDocumentUseCase, dispatchers
    )

    @Provides
    @Singleton
    fun provideSearchDocumentsUseCase(
        vectorRepository: VectorRepository,
        embeddingPort: EmbeddingPort,
        dispatchers: DispatcherProvider
    ): SearchDocumentsUseCase = SearchDocumentsUseCase(
        vectorRepository, embeddingPort, dispatchers
    )

    @Provides
    @Singleton
    fun provideDeleteDocumentUseCase(
        documentRepository: DocumentRepository,
        vectorRepository: VectorRepository,
        dispatchers: DispatcherProvider
    ): DeleteDocumentUseCase = DeleteDocumentUseCase(
        documentRepository, vectorRepository, dispatchers
    )

    @Provides
    @Singleton
    fun provideGetIngestionStatusUseCase(
        documentRepository: DocumentRepository
    ): GetIngestionStatusUseCase = GetIngestionStatusUseCase(documentRepository)
}
