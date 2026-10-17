package com.ragdroid.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class — Hilt entry point.
 * Initializes libraries that need application-level setup and configures
 * WorkManager to use Hilt for worker injection.
 */
@HiltAndroidApp
class RetrieverApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize PdfBox-Android resources
        PDFBoxResourceLoader.init(this)
    }
}
