package com.ragdroid.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ragdroid.core.model.Document
import com.ragdroid.core.model.IngestionStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    onNavigateToChunk: (documentId: String, chunkIndex: Int) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isTagFilterExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {

        // Storage summary
        if (uiState.documents.isNotEmpty()) {
            Text(
                text = uiState.storageSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Search bar & Filter Icon row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search document names...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )

            // Filter Icon Button to Toggle Tag Filters
            if (uiState.allTags.isNotEmpty()) {
                IconButton(
                    onClick = { isTagFilterExpanded = !isTagFilterExpanded }
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Toggle Tag Filters",
                        tint = if (isTagFilterExpanded || uiState.selectedTags.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Tag Filter Badge when collapsed
        if (!isTagFilterExpanded && uiState.selectedTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { isTagFilterExpanded = true },
                    label = { Text("Tags Filter Active (${uiState.selectedTags.size})") },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )

                TextButton(onClick = viewModel::onClearTagFilter) {
                    Text("Clear")
                }
            }
        }

        // Tag Filter Area (Hidden by default, shown when Filter Icon is clicked)
        AnimatedVisibility(visible = isTagFilterExpanded && uiState.allTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedTags.isEmpty(),
                    onClick = viewModel::onClearTagFilter,
                    label = { Text("All (${uiState.documents.size})") }
                )

                uiState.allTags.forEach { tag ->
                    val count = uiState.documents.count { tag in it.tags }
                    FilterChip(
                        selected = tag in uiState.selectedTags,
                        onClick = { viewModel.onTagSelected(tag) },
                        label = { Text("$tag ($count)") },
                        leadingIcon = if (tag in uiState.selectedTags) {
                            { Icon(Icons.Default.Label, contentDescription = null) }
                        } else null
                    )
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (uiState.filteredDocuments.isEmpty()) {
            Text(
                text = if (uiState.documents.isEmpty()) {
                    "No documents yet. Add some to get started!"
                } else {
                    "No documents match your filters."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
        } else {
            // Document List
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.filteredDocuments, key = { it.id }) { document ->
                    DocumentCard(
                        document = document,
                        isDeleting = (uiState.deletingDocId == document.id),
                        onDelete = { viewModel.onDeleteRequested(document.id) },
                        onEditTags = { viewModel.onOpenTagDialog(document.id) },
                        onOpenChunkViewer = { onNavigateToChunk(document.id, 0) }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    uiState.deleteConfirmDocId?.let { docId ->
        val doc = uiState.documents.find { it.id == docId }
        AlertDialog(
            onDismissRequest = viewModel::onDeleteCancelled,
            title = { Text("Delete Document?") },
            text = {
                Text("Delete \"${doc?.title ?: docId}\" and all its indexed chunks? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteCancelled) {
                    Text("Cancel")
                }
            }
        )
    }

    // Deleting progress modal indicator
    if (uiState.isDeleting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Deleting Document...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("Removing document and vector index...")
                }
            },
            confirmButton = {}
        )
    }

    // Tag editor dialog
    uiState.tagDialogDocId?.let { docId ->
        val doc = uiState.documents.find { it.id == docId }
        TagEditorDialog(
            documentTitle = doc?.title ?: "",
            currentTags = uiState.tagDialogTags,
            onAddTag = viewModel::onAddTagToDialog,
            onRemoveTag = viewModel::onRemoveTagFromDialog,
            onSave = viewModel::onSaveTags,
            onDismiss = viewModel::onCancelTagDialog
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentCard(
    document: Document,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onEditTags: () -> Unit,
    onOpenChunkViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpenChunkViewer,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                StatusBadge(status = document.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Format Badge, Author & Chunks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = document.format.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = buildString {
                        document.author?.let { append("$it · ") }
                        append("${document.chunkCount} chunks")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Storage Sizes & AI Model
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "File: ${document.fileSizeFormatted} · Index: ${document.indexSizeFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Text(
                    text = "Model: ${document.embeddingModel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Row 4: Tags on Left + Action Icons on Right
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags List
                if (document.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        document.tags.forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Label,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = " $tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Actions: Tag Edit + Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditTags, enabled = !isDeleting) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = "Edit tags",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: IngestionStatus) {
    val (color, text) = when (status) {
        IngestionStatus.COMPLETED -> MaterialTheme.colorScheme.primary to "COMPLETED"
        IngestionStatus.FAILED -> MaterialTheme.colorScheme.error to "FAILED"
        IngestionStatus.PARSING -> MaterialTheme.colorScheme.secondary to "PARSING"
        IngestionStatus.CHUNKING -> MaterialTheme.colorScheme.secondary to "CHUNKING"
        IngestionStatus.EMBEDDING -> MaterialTheme.colorScheme.secondary to "EMBEDDING"
        IngestionStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant to "PENDING"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditorDialog(
    documentTitle: String,
    currentTags: Set<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var newTagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tags: $documentTitle") },
        text = {
            Column {
                // Input for new tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add tag...") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newTagText.isNotBlank()) {
                                onAddTag(newTagText)
                                newTagText = ""
                            }
                        },
                        enabled = newTagText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current tags
                if (currentTags.isNotEmpty()) {
                    Text(
                        text = "Current Tags:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTags.forEach { tag ->
                            SuggestionChip(
                                onClick = { onRemoveTag(tag) },
                                label = { Text(tag) },
                                icon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove $tag"
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No tags yet. Add some above!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
