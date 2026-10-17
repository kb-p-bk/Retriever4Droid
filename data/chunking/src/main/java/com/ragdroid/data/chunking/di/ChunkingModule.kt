package com.ragdroid.data.chunking.di

import com.ragdroid.data.chunking.LangChainChunkingService
import com.ragdroid.domain.service.ChunkingPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChunkingModule {

    @Binds
    @Singleton
    abstract fun bindChunkingPort(service: LangChainChunkingService): ChunkingPort
}
