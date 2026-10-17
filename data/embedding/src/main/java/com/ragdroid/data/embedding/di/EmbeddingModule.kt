package com.ragdroid.data.embedding.di

import com.ragdroid.data.embedding.OnnxEmbeddingService
import com.ragdroid.domain.service.EmbeddingPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmbeddingModule {

    @Binds
    @Singleton
    abstract fun bindEmbeddingPort(service: OnnxEmbeddingService): EmbeddingPort
}
