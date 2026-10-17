package com.ragdroid.data.file.di

import com.ragdroid.data.file.SafFileAccessService
import com.ragdroid.domain.repository.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FileModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(service: SafFileAccessService): FileRepository
}
