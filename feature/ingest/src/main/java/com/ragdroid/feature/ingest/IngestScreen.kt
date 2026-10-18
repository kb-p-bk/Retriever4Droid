package com.ragdroid.feature.ingest

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ragdroid.core.model.ChunkStrategy
import com.ragdroid.core.model.Document
import com.ragdroid.core.model.IngestionStatus
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngestScreen(
    viewModel: IngestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showChunkSettings by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onFilesSelected(
                uris = uris.map { it.toString() },
                names = uris.map { it.lastPathSegment ?: "Unknown" }
            )
        }
    }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onFilesSelected(
                uris = listOf(it.toString()),
                names = listOf(it.lastPathSegment ?: "Folder")
            )
        }
    }

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
            // File & Folder Selection Buttons Row (50 / 50)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isIngesting
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Text(" Pick Files", maxLines = 1)
                }

                OutlinedButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isIngesting
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Text(" Pick Folder", maxLines = 1)
                }
            }

            // Selected files display
            if (uiState.selectedFileNames.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${uiState.selectedFileNames.size} file(s) selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.selectedFileNames.forEach { name ->
                            Text(
                                text = "• $name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Chunking Settings Card (Optional) with Caret Expand/Collapse
            if (!uiState.isIngesting && !uiState.isComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChunkSettings = !showChunkSettings },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "  Chunking Settings (Optional)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            IconButton(onClick = { showChunkSettings = !showChunkSettings }) {
                                Icon(
                                    imageVector = if (showChunkSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showChunkSettings) "Collapse settings" else "Expand settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (showChunkSettings) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Chunk Size
                            Text(
                                text = "Chunk Size: ${uiState.chunkConfig.chunkSize} chars",
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
                                text = "Chunk Overlap: ${uiState.chunkConfig.chunkOverlap} chars",
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
                                text = "Strategy",
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
                }
            }

            // Start ingestion button
            AnimatedVisibility(visible = uiState.selectedFileNames.isNotEmpty() && !uiState.isIngesting && !uiState.isComplete) {
                Button(
                    onClick = viewModel::onStartIngestion,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Text("  Start Ingestion")
                }
            }

            // Vertical Stepper Progress Section
            AnimatedVisibility(visible = uiState.isIngesting) {
                VerticalStepperIngestionProgress(uiState = uiState)
            }

            // Completion Section + Direct Tagging UI
            AnimatedVisibility(visible = uiState.isComplete) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Ingestion Complete!",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${uiState.filesCompleted} file(s) processed",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Direct Tagging Section
                    if (uiState.ingestedDocuments.isNotEmpty()) {
                        uiState.ingestedDocuments.forEach { doc ->
                            IngestDocumentTagCard(
                                document = doc,
                                availableTags = uiState.availableTags,
                                onUpdateTags = { newTags ->
                                    viewModel.onUpdateDocumentTags(doc.id, newTags)
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::onReset,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add More Documents")
                    }
                }
            }

            // Error display
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private enum class StepState { PENDING, ACTIVE, COMPLETED }

@Composable
private fun VerticalStepperIngestionProgress(
    uiState: IngestUiState,
    modifier: Modifier = Modifier
) {
    val currentStatus = uiState.currentStatus

    val step1State = when (currentStatus) {
        IngestionStatus.PARSING -> StepState.ACTIVE
        IngestionStatus.CHUNKING, IngestionStatus.EMBEDDING, IngestionStatus.COMPLETED -> StepState.COMPLETED
        else -> StepState.PENDING
    }

    val step2State = when (currentStatus) {
        IngestionStatus.CHUNKING -> StepState.ACTIVE
        IngestionStatus.EMBEDDING, IngestionStatus.COMPLETED -> StepState.COMPLETED
        else -> StepState.PENDING
    }

    val step3State = when (currentStatus) {
        IngestionStatus.EMBEDDING -> StepState.ACTIVE
        IngestionStatus.COMPLETED -> StepState.COMPLETED
        else -> StepState.PENDING
    }

    val step4State = when (currentStatus) {
        IngestionStatus.COMPLETED -> StepState.COMPLETED
        else -> StepState.PENDING
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ingesting ${uiState.currentFileName ?: "Document"}...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "File ${uiState.filesCompleted + 1} of ${uiState.totalFiles}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Parsing
            VerticalStepRow(
                stepNumber = 1,
                title = "Parse Document Structure",
                subtitle = when (step1State) {
                    StepState.ACTIVE -> "Extracting text & chapter hierarchy..."
                    StepState.COMPLETED -> "Text & chapters extracted"
                    StepState.PENDING -> "Pending"
                },
                state = step1State,
                isLast = false
            )

            // Step 2: Chunking
            VerticalStepRow(
                stepNumber = 2,
                title = "Chunk Text Content",
                subtitle = when (step2State) {
                    StepState.ACTIVE -> "Splitting text into chunks (${uiState.chunkConfig.chunkSize} chars)..."
                    StepState.COMPLETED -> "${uiState.totalChunks.coerceAtLeast(1)} chunks created"
                    StepState.PENDING -> "Pending"
                },
                state = step2State,
                isLast = false
            )

            // Step 3: Embeddings
            VerticalStepRow(
                stepNumber = 3,
                title = "Generate Vector Embeddings",
                subtitle = when (step3State) {
                    StepState.ACTIVE -> "ONNX 384D Inference: ${uiState.chunksProcessed}/${uiState.totalChunks} chunks"
                    StepState.COMPLETED -> "${uiState.totalChunks} vector embeddings generated"
                    StepState.PENDING -> "Pending"
                },
                state = step3State,
                isLast = false,
                progressContent = if (step3State == StepState.ACTIVE && uiState.totalChunks > 0) {
                    {
                        LinearProgressIndicator(
                            progress = { uiState.chunksProcessed.toFloat() / uiState.totalChunks },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                } else null
            )

            // Step 4: Storage
            VerticalStepRow(
                stepNumber = 4,
                title = "Vector DB Indexing",
                subtitle = when (step4State) {
                    StepState.COMPLETED -> "Stored in ObjectBox HNSW database"
                    else -> "Pending ObjectBox commit"
                },
                state = step4State,
                isLast = true
            )
        }
    }
}

@Composable
private fun VerticalStepRow(
    stepNumber: Int,
    title: String,
    subtitle: String,
    state: StepState,
    isLast: Boolean,
    progressContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Step Icon + Connecting Vertical Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            when (state) {
                StepState.COMPLETED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                StepState.ACTIVE -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StepState.PENDING -> {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$stepNumber",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(2.dp)
                        .height(28.dp)
                        .background(
                            if (state == StepState.COMPLETED) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        // Right Column: Title + Subtitle + Optional Progress Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = when (state) {
                    StepState.ACTIVE -> MaterialTheme.colorScheme.primary
                    StepState.COMPLETED -> MaterialTheme.colorScheme.onSurface
                    StepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            progressContent?.invoke()
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngestDocumentTagCard(
    document: Document,
    availableTags: Set<String>,
    onUpdateTags: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var tagInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "  ${document.title}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Tags
            if (document.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    document.tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { onUpdateTags(document.tags - tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier.clickable { onUpdateTags(document.tags - tag) }
                                )
                            }
                        )
                    }
                }
            }

            // Compact Custom Tag Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("Add tag...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val trimmed = tagInput.trim()
                        if (trimmed.isNotBlank()) {
                            onUpdateTags(document.tags + trimmed)
                            tagInput = ""
                        }
                    },
                    enabled = tagInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add tag", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Existing Library Tags Suggestions (only show tags from user's library)
            val suggestions = availableTags.filter { it !in document.tags }
            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onUpdateTags(document.tags + suggestion) },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }
        }
    }
}
