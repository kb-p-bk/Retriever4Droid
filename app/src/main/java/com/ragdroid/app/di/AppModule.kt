package com.ragdroid.app.di

import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level Hilt bindings.
 *
 * Provides Android framework singletons that are not self-injectable via
 * constructor injection — [WorkManager] and [NotificationManager].
 * All application-scoped, so every consumer gets the same instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the [WorkManager] singleton for the application.
     * Used by [IngestWorker] and any other background tasks.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)

    /**
     * Provides [NotificationManager] for posting ingestion progress notifications
     * and other system-level notifications.
     */
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
