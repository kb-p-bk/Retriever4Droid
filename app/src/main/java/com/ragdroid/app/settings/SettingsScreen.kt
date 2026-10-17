package com.ragdroid.app.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ragdroid.core.model.ChunkStrategy
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var customModelUrl by remember { mutableStateOf("") }
    var customModelName by remember { mutableStateOf("Custom ONNX Model") }

    // Launcher for local .onnx file selection
    val modelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = uri.lastPathSegment ?: "custom_model.onnx"
            viewModel.onImportLocalModel(context, uri, fileName)
        }
    }

    val modelPresets = listOf(
        Triple(
            "all-MiniLM-L6-v2 (23MB Quantized)",
            "https://huggingface.co/xenova/all-MiniLM-L6-v2/resolve/main/onnx/model_quantized.onnx",
            "Fast 384D quantized sentence-transformer model"
        ),
        Triple(
            "bge-small-en-v1.5 (384D)",
            "https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/onnx/model.onnx",
            "High accuracy 384D English embedding model"
        ),
        Triple(
            "gte-small (384D)",
            "https://huggingface.co/Supabase/gte-small/resolve/main/onnx/model.onnx",
            "General Text Embeddings 384D model"
        )
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default Chunking Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "  Chunking Defaults",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        OutlinedButton(onClick = viewModel::onResetDefaults) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null)
                            Text(" Reset")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chunk Size
                    Text(
                        text = "Default Chunk Size: ${uiState.chunkConfig.chunkSize} chars",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.chunkConfig.chunkSize.toFloat(),
                        onValueChange = { newSize ->
                            val size = (newSize / 64).roundToInt() * 64
                            val validOverlap = uiState.chunkConfig.chunkOverlap.coerceAtMost(size - 1)
                            viewModel.onChunkConfigChanged(
                                uiState.chunkConfig.copy(
                                    chunkSize = size.coerceIn(128, 2048),
                                    chunkOverlap = validOverlap
                                )
                            )
                        },
                        valueRange = 128f..2048f,
                        steps = 14
                    )

                    // Chunk Overlap
                    Text(
                        text = "Default Chunk Overlap: ${uiState.chunkConfig.chunkOverlap} chars",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.chunkConfig.chunkOverlap.toFloat(),
                        onValueChange = { newOverlap ->
                            val overlap = (newOverlap / 10).roundToInt() * 10
                            val validOverlap = overlap.coerceAtMost(uiState.chunkConfig.chunkSize - 1)
                            viewModel.onChunkConfigChanged(
                                uiState.chunkConfig.copy(chunkOverlap = validOverlap)
                            )
                        },
                        valueRange = 0f..200f,
                        steps = 19
                    )

                    // Strategy Chips
                    Text(
                        text = "Default Strategy",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChunkStrategy.entries.forEach { strategy ->
                            FilterChip(
                                selected = uiState.chunkConfig.strategy == strategy,
                                onClick = {
                                    viewModel.onChunkConfigChanged(
                                        uiState.chunkConfig.copy(strategy = strategy)
                                    )
                                },
                                label = { Text(strategy.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            // Search Results Display & Top-K Limit Preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default Top Search Results Count: ${uiState.defaultTopK} chunks",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Number of top matching vector chunks returned per search query.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = uiState.defaultTopK.toFloat(),
                        onValueChange = { newTopK ->
                            val topK = (newTopK / 5).roundToInt() * 5
                            viewModel.onTopKDefaultChanged(topK.coerceIn(5, 100))
                        },
                        valueRange = 5f..100f,
                        steps = 18
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Expand Search Result Chunks by Default",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Show full chunk text in search results list so you don't need to open the chunk viewer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = uiState.expandChunksByDefault,
                            onCheckedChange = viewModel::onToggleExpandChunksByDefault
                        )
                    }
                }
            }

            // Active Embedding Model & Engine Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "  Active AI Embedding Model",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (uiState.isCustomModelActive) {
                            OutlinedButton(onClick = viewModel::onRestoreDefaultModel) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null)
                                Text(" Restore Default")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Model: ${uiState.activeModelName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Output Dimensions: ${uiState.embeddingDimensions}D Float Vectors",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Vector Database: ${uiState.vectorStoreEngine}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text("ONNX Runtime") }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("HNSW Index") }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(if (uiState.isCustomModelActive) "Custom ONNX Active" else "100% On-Device") }
                        )
                    }
                }
            }

            // Custom ONNX Model Download & File Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "  Download / Supply Custom ONNX Model",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Supply any 384D ONNX sentence-transformer model by choosing a preset, entering a Hugging Face URL, or picking a local .onnx file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Chips
                    Text(
                        text = "Select Model Preset:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        modelPresets.forEach { (name, url, _) ->
                            SuggestionChip(
                                onClick = {
                                    customModelUrl = url
                                    customModelName = name
                                },
                                label = { Text(name) }
                            )
                        }
                    }

                    // Custom URL Input
                    OutlinedTextField(
                        value = customModelUrl,
                        onValueChange = { customModelUrl = it },
                        label = { Text("ONNX Model Download URL") },
                        placeholder = { Text("https://huggingface.co/.../model.onnx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val name = customModelName.ifBlank { "Custom ONNX Model" }
                                viewModel.onDownloadModel(customModelUrl, name)
                            },
                            enabled = customModelUrl.isNotBlank() && !uiState.isDownloadingModel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Text(" Download", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { modelFilePicker.launch("*/*") },
                            enabled = !uiState.isDownloadingModel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Text(" Pick .onnx File", maxLines = 1)
                        }
                    }

                    // Progress indicator
                    AnimatedVisibility(visible = uiState.isDownloadingModel) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            LinearProgressIndicator(
                                progress = { uiState.downloadProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            uiState.modelStatusMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Status Message
                    uiState.modelStatusMessage?.let { msg ->
                        if (!uiState.isDownloadingModel) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "  $msg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Error display
                    uiState.error?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
