package com.ragdroid.data.repository

import android.content.Context
import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.core.model.ChunkStrategy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("semantic_retriever_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CHUNK_SIZE = "key_chunk_size"
        private const val KEY_CHUNK_OVERLAP = "key_chunk_overlap"
        private const val KEY_CHUNK_STRATEGY = "key_chunk_strategy"
        private const val KEY_EXPAND_CHUNKS_DEFAULT = "key_expand_chunks_default"
        private const val KEY_TOP_K_DEFAULT = "key_top_k_default"
    }

    fun getChunkConfig(): ChunkConfig {
        val size = prefs.getInt(KEY_CHUNK_SIZE, 512)
        val overlap = prefs.getInt(KEY_CHUNK_OVERLAP, 50)
        val strategyName = prefs.getString(KEY_CHUNK_STRATEGY, ChunkStrategy.RECURSIVE.name)
        val strategy = runCatching { ChunkStrategy.valueOf(strategyName!!) }.getOrDefault(ChunkStrategy.RECURSIVE)

        return ChunkConfig(
            chunkSize = size.coerceIn(128, 2048),
            chunkOverlap = overlap.coerceAtMost(size - 1),
            strategy = strategy
        )
    }

    fun saveChunkConfig(config: ChunkConfig) {
        prefs.edit()
            .putInt(KEY_CHUNK_SIZE, config.chunkSize)
            .putInt(KEY_CHUNK_OVERLAP, config.chunkOverlap)
            .putString(KEY_CHUNK_STRATEGY, config.strategy.name)
            .apply()
    }

    fun getExpandChunksByDefault(): Boolean {
        return prefs.getBoolean(KEY_EXPAND_CHUNKS_DEFAULT, false)
    }

    fun saveExpandChunksByDefault(expand: Boolean) {
        prefs.edit()
            .putBoolean(KEY_EXPAND_CHUNKS_DEFAULT, expand)
            .apply()
    }

    fun getTopKDefault(): Int {
        return prefs.getInt(KEY_TOP_K_DEFAULT, 20).coerceIn(5, 100)
    }

    fun saveTopKDefault(topK: Int) {
        prefs.edit()
            .putInt(KEY_TOP_K_DEFAULT, topK.coerceIn(5, 100))
            .apply()
    }

    fun resetToDefaults(): ChunkConfig {
        val defaultConfig = ChunkConfig()
        saveChunkConfig(defaultConfig)
        saveExpandChunksByDefault(false)
        saveTopKDefault(20)
        return defaultConfig
    }
}
