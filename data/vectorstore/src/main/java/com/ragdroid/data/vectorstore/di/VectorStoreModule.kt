package com.ragdroid.data.vectorstore.di

import com.ragdroid.data.vectorstore.ObjectBoxVectorStore
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.repository.VectorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VectorStoreModule {

    @Binds
    @Singleton
    abstract fun bindVectorRepository(store: ObjectBoxVectorStore): VectorRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(store: ObjectBoxVectorStore): DocumentRepository
}
