package com.ragdroid.data.embedding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the ONNX model file lifecycle:
 * - Bundled assets model (default all-MiniLM-L6-v2)
 * - Custom ONNX model download from Hugging Face / remote URL
 * - Local .onnx model file import
 */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MODEL_ASSET_NAME = "models/all-MiniLM-L6-v2.onnx"
        const val MODEL_CACHE_NAME = "all-MiniLM-L6-v2.onnx"
        const val CUSTOM_MODEL_CACHE_NAME = "custom_model.onnx"
        const val PREF_CUSTOM_MODEL_ACTIVE = "pref_custom_model_active"
        const val PREF_CUSTOM_MODEL_NAME = "pref_custom_model_name"
    }

    private val prefs = context.getSharedPreferences("semantic_retriever_model_prefs", Context.MODE_PRIVATE)

    suspend fun getModelPath(): String = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "models")
        cacheDir.mkdirs()

        val isCustomActive = prefs.getBoolean(PREF_CUSTOM_MODEL_ACTIVE, false)
        if (isCustomActive) {
            val customFile = File(cacheDir, CUSTOM_MODEL_CACHE_NAME)
            if (customFile.exists() && customFile.length() > 0) {
                return@withContext customFile.absolutePath
            }
        }

        // Default bundled asset model
        val defaultCachedModel = File(cacheDir, MODEL_CACHE_NAME)
        if (!defaultCachedModel.exists()) {
            context.assets.open(MODEL_ASSET_NAME).use { input ->
                defaultCachedModel.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        defaultCachedModel.absolutePath
    }

    fun getActiveModelName(): String {
        return if (prefs.getBoolean(PREF_CUSTOM_MODEL_ACTIVE, false)) {
            prefs.getString(PREF_CUSTOM_MODEL_NAME, "Custom ONNX Model") ?: "Custom ONNX Model"
        } else {
            "all-MiniLM-L6-v2 (INT8 Quantized)"
        }
    }

    fun isCustomModelActive(): Boolean {
        return prefs.getBoolean(PREF_CUSTOM_MODEL_ACTIVE, false)
    }

    suspend fun downloadModel(urlStr: String, modelName: String, onProgress: (Float) -> Unit): String = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "models")
        cacheDir.mkdirs()
        val customFile = File(cacheDir, CUSTOM_MODEL_CACHE_NAME)

        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IllegalStateException("Server returned HTTP ${connection.responseCode}: ${connection.responseMessage}")
        }

        val fileLength = connection.contentLength
        connection.inputStream.use { input ->
            customFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (fileLength > 0) {
                        onProgress(totalRead.toFloat() / fileLength.toFloat())
                    }
                }
            }
        }

        prefs.edit()
            .putBoolean(PREF_CUSTOM_MODEL_ACTIVE, true)
            .putString(PREF_CUSTOM_MODEL_NAME, modelName)
            .apply()

        customFile.absolutePath
    }

    suspend fun importLocalModel(inputStream: InputStream, fileName: String): String = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "models")
        cacheDir.mkdirs()
        val customFile = File(cacheDir, CUSTOM_MODEL_CACHE_NAME)

        inputStream.use { input ->
            customFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        prefs.edit()
            .putBoolean(PREF_CUSTOM_MODEL_ACTIVE, true)
            .putString(PREF_CUSTOM_MODEL_NAME, fileName)
            .apply()

        customFile.absolutePath
    }

    suspend fun restoreDefaultModel() = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "models")
        val customFile = File(cacheDir, CUSTOM_MODEL_CACHE_NAME)
        if (customFile.exists()) {
            customFile.delete()
        }

        prefs.edit()
            .putBoolean(PREF_CUSTOM_MODEL_ACTIVE, false)
            .remove(PREF_CUSTOM_MODEL_NAME)
            .apply()
    }

    suspend fun isModelAvailable(): Boolean = withContext(Dispatchers.IO) {
        val cachedModel = File(File(context.filesDir, "models"), MODEL_CACHE_NAME)
        if (cachedModel.exists()) return@withContext true

        return@withContext try {
            context.assets.open(MODEL_ASSET_NAME).use { true }
        } catch (e: Exception) {
            false
        }
    }
}
