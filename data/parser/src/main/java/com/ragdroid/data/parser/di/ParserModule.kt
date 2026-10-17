package com.ragdroid.data.parser.di

import com.ragdroid.data.parser.DocumentParserFactory
import com.ragdroid.domain.service.ParsingPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @Singleton
    abstract fun bindParsingPort(factory: DocumentParserFactory): ParsingPort
}
