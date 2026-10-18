package com.ragdroid.feature.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ragdroid.core.model.SearchResult
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onNavigateToChunk: (documentId: String, chunkIndex: Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDocListExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input Row with Filter Icon & Search Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::onClearQuery) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.onSearch() })
            )

            // Compact Filter Bar Toggle Button
            IconButton(
                onClick = viewModel::onToggleFilterBarVisibility
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Toggle Filters",
                    tint = if (uiState.isFilterBarExpanded || uiState.activeFilterCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Filter Summary Badge when Filter Bar is Collapsed
        if (!uiState.isFilterBarExpanded && uiState.activeFilterCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = viewModel::onToggleFilterBarVisibility,
                    label = {
                        Text(
                            text = "Filter Active: ${uiState.selectedTags.size} tags, ${uiState.selectedDocumentIds.size} docs"
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                )

                TextButton(onClick = viewModel::onClearFilters) {
                    Text("Clear Filters")
                }
            }
        }

        // Expandable Filter Section (Only shown when filter icon is toggled)
        AnimatedVisibility(visible = uiState.isFilterBarExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Results Limit Config
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Top Search Results Limit: ${uiState.topK} chunks",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = uiState.topK.toFloat(),
                            onValueChange = { newTopK ->
                                val topK = (newTopK / 5).roundToInt() * 5
                                viewModel.onTopKChanged(topK.coerceIn(5, 100))
                            },
                            valueRange = 5f..100f,
                            steps = 18
                        )
                    }
                }
                // Tag Filters (Max 1/3 screen height scroll container)
                if (uiState.availableTags.isNotEmpty()) {
                    Text(
                        text = "Filter by Tags:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.availableTags.forEach { tag ->
                                FilterChip(
                                    selected = tag in uiState.selectedTags,
                                    onClick = { viewModel.onTagSelected(tag) },
                                    label = { Text(tag) },
                                    leadingIcon = if (tag in uiState.selectedTags) {
                                        { Icon(Icons.Default.Label, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Multi-Select Document List Filter
                if (uiState.availableDocuments.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDocListExpanded = !isDocListExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = buildString {
                                            append(" Filter Documents ")
                                            if (uiState.selectedDocumentIds.isNotEmpty()) {
                                                append("(${uiState.selectedDocumentIds.size}/${uiState.availableDocuments.size} selected)")
                                            } else {
                                                append("(All ${uiState.availableDocuments.size} books)")
                                            }
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                IconButton(onClick = { isDocListExpanded = !isDocListExpanded }) {
                                    Icon(
                                        imageVector = if (isDocListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle Document List"
                                    )
                                }
                            }

                            if (isDocListExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Document Name Search Field
                                OutlinedTextField(
                                    value = uiState.docFilterSearchQuery,
                                    onValueChange = viewModel::onDocFilterSearchQueryChanged,
                                    placeholder = { Text("Search document names...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Selection Action Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(onClick = viewModel::onSelectAllFilteredDocuments) {
                                        Text("Select All")
                                    }
                                    TextButton(onClick = viewModel::onClearDocumentSelection) {
                                        Text("Clear Selection")
                                    }
                                }

                                // Scrollable Multi-Select List of Documents
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        uiState.filteredAvailableDocuments.forEach { doc ->
                                            val isSelected = doc.id in uiState.selectedDocumentIds
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.onToggleDocumentSelection(doc.id) }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { viewModel.onToggleDocumentSelection(doc.id) }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = doc.title.ifBlank { doc.fileName },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${doc.format.displayName} · ${doc.chunkCount} chunks · Model: ${doc.embeddingModel}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Clear All Filters Button
                if (uiState.selectedTags.isNotEmpty() || uiState.selectedDocumentIds.isNotEmpty()) {
                    OutlinedButton(
                        onClick = viewModel::onClearFilters,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Filters")
                    }
                }
            }
        }

        // Loading indicator
        AnimatedVisibility(
            visible = uiState.isSearching,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // Error
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Results
        if (uiState.hasSearched && uiState.results.isEmpty() && !uiState.isSearching) {
            val filterMsg = when {
                uiState.selectedTags.isNotEmpty() -> " in selected tags"
                uiState.selectedDocumentIds.isNotEmpty() -> " in selected documents"
                else -> ""
            }
            Text(
                text = "No results found for \"${uiState.query}\"$filterMsg",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.results, key = { it.chunk.id }) { result ->
                val isChunkExpanded = uiState.isChunkExpanded(result.chunk.id)

                SearchResultCard(
                    result = result,
                    isChunkExpanded = isChunkExpanded,
                    onToggleExpandChunk = {
                        viewModel.onToggleChunkExpanded(result.chunk.id)
                    },
                    onOpenViewer = {
                        onNavigateToChunk(result.document.id, result.chunk.chunkIndex)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchResultCard(
    result: SearchResult,
    isChunkExpanded: Boolean,
    onToggleExpandChunk: () -> Unit,
    onOpenViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: document title + score + copy icon + open viewer icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.document.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(result.score * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Chunk Text", result.chunk.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Chunk copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy Chunk Text",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onOpenViewer) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in Chunk Viewer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Chapter info
            result.chunk.chapterTitle?.let { chapter ->
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chunk text preview (Truncated vs Full Expanded)
            Text(
                text = result.chunk.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isChunkExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )

            // In-line Expand / Collapse Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleExpandChunk) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isChunkExpanded) "Show Less" else "Expand Full Text")
                        Icon(
                            imageVector = if (isChunkExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }

                Text(
                    text = "Chunk ${result.chunk.chunkIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Format badge & embedding model
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                CompactBadge(
                    text = result.document.format.displayName,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                CompactBadge(
                    text = result.document.embeddingModel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun CompactBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    androidx.compose.material3.Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
