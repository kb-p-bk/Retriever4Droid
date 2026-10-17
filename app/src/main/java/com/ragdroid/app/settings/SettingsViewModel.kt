package com.ragdroid.app.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragdroid.core.model.ChunkConfig
import com.ragdroid.data.embedding.ModelManager
import com.ragdroid.data.embedding.OnnxEmbeddingService
import com.ragdroid.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val chunkConfig: ChunkConfig = ChunkConfig(),
    val expandChunksByDefault: Boolean = false,
    val defaultTopK: Int = 20,
    val activeModelName: String = "all-MiniLM-L6-v2 (INT8 Quantized)",
    val embeddingDimensions: Int = 384,
    val vectorStoreEngine: String = "ObjectBox HNSW Vector Index",
    val isCustomModelActive: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val downloadProgress: Float = 0f,
    val modelStatusMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val modelManager: ModelManager,
    private val onnxEmbeddingService: OnnxEmbeddingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            chunkConfig = settingsRepository.getChunkConfig(),
            expandChunksByDefault = settingsRepository.getExpandChunksByDefault(),
            defaultTopK = settingsRepository.getTopKDefault(),
            activeModelName = modelManager.getActiveModelName(),
            isCustomModelActive = modelManager.isCustomModelActive()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onChunkConfigChanged(newConfig: ChunkConfig) {
        settingsRepository.saveChunkConfig(newConfig)
        _uiState.update { it.copy(chunkConfig = newConfig) }
    }

    fun onToggleExpandChunksByDefault(expand: Boolean) {
        settingsRepository.saveExpandChunksByDefault(expand)
        _uiState.update { it.copy(expandChunksByDefault = expand) }
    }

    fun onTopKDefaultChanged(topK: Int) {
        settingsRepository.saveTopKDefault(topK)
        _uiState.update { it.copy(defaultTopK = topK) }
    }

    fun onResetDefaults() {
        val defaultConfig = settingsRepository.resetToDefaults()
        _uiState.update {
            it.copy(
                chunkConfig = defaultConfig,
                expandChunksByDefault = false,
                defaultTopK = 20
            )
        }
    }

    fun onDownloadModel(url: String, modelName: String) {
        if (url.isBlank()) return

        _uiState.update {
            it.copy(
                isDownloadingModel = true,
                downloadProgress = 0f,
                modelStatusMessage = "Connecting to $url...",
                error = null
            )
        }

        viewModelScope.launch {
            try {
                modelManager.downloadModel(url, modelName) { progress ->
                    _uiState.update { state ->
                        state.copy(
                            downloadProgress = progress,
                            modelStatusMessage = "Downloading ONNX model: ${(progress * 100).toInt()}%"
                        )
                    }
                }

                onnxEmbeddingService.resetSession()

                _uiState.update {
                    it.copy(
                        isDownloadingModel = false,
                        activeModelName = modelManager.getActiveModelName(),
                        isCustomModelActive = true,
                        modelStatusMessage = "Custom model activated successfully!",
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDownloadingModel = false,
                        error = "Failed to download model: ${e.message}"
                    )
                }
            }
        }
    }

    fun onImportLocalModel(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(modelStatusMessage = "Importing $fileName...", error = null) }
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Could not open file stream for $fileName")

                modelManager.importLocalModel(inputStream, fileName)
                onnxEmbeddingService.resetSession()

                _uiState.update {
                    it.copy(
                        activeModelName = modelManager.getActiveModelName(),
                        isCustomModelActive = true,
                        modelStatusMessage = "Imported $fileName successfully!",
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to import model: ${e.message}")
                }
            }
        }
    }

    fun onRestoreDefaultModel() {
        viewModelScope.launch {
            try {
                modelManager.restoreDefaultModel()
                onnxEmbeddingService.resetSession()

                _uiState.update {
                    it.copy(
                        activeModelName = modelManager.getActiveModelName(),
                        isCustomModelActive = false,
                        modelStatusMessage = "Restored default all-MiniLM-L6-v2 model",
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to restore default model: ${e.message}") }
            }
        }
    }
}
