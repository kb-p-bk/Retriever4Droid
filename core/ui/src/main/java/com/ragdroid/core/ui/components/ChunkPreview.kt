package com.ragdroid.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ragdroid.core.model.Document
import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.core.model.SearchResult
import com.ragdroid.core.model.TextChunk
import com.ragdroid.core.ui.theme.RetrieverTheme
import com.ragdroid.core.ui.theme.StatusComplete
import com.ragdroid.core.ui.theme.StatusFailed
import com.ragdroid.core.ui.theme.StatusProcessing

/**
 * Card that previews a matched [SearchResult] chunk in the Search screen.
 *
 * Displays:
 * - Similarity score bar
 * - Source document title + chapter (if available)
 * - A snippet of the chunk text with the query terms optionally highlighted
 *
 * @param result      The search result to display.
 * @param query       The user's original query — used for term highlighting.
 * @param onClick     Called when the card is tapped (navigate to full chunk detail).
 * @param modifier    Optional [Modifier].
 */
@Composable
fun ChunkPreview(
    result: SearchResult,
    query: String = "",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Header: score + source ───────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Score badge
                ScoreBadge(score = result.score)

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Document title
                    Text(
                        text = result.document.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Chapter title (if present)
                    result.chunk.chapterTitle?.let { chapter ->
                        Text(
                            text = chapter,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Score bar ────────────────────────────────────────────────────
            LinearProgressIndicator(
                progress = { result.score },
                modifier = Modifier.fillMaxWidth(),
                color = scoreColor(result.score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            // ── Chunk text with query term highlighting ───────────────────────
            val highlightColor = MaterialTheme.colorScheme.primary
            val bodyText = buildAnnotatedString {
                val text = result.chunk.text.take(300).let {
                    if (result.chunk.text.length > 300) "$it…" else it
                }
                if (query.isBlank()) {
                    append(text)
                } else {
                    val lowerText = text.lowercase()
                    val terms = query.lowercase().split(" ").filter { it.length > 2 }
                    var cursor = 0

                    // Simple greedy term highlighting — does not use regex to stay lightweight
                    while (cursor < text.length) {
                        val matchedTerm = terms.firstOrNull { term ->
                            lowerText.startsWith(term, cursor)
                        }
                        if (matchedTerm != null) {
                            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                                append(text.substring(cursor, cursor + matchedTerm.length))
                            }
                            cursor += matchedTerm.length
                        } else {
                            append(text[cursor])
                            cursor++
                        }
                    }
                }
            }

            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScoreBadge(score: Float) {
    val percent = (score * 100).toInt()
    Surface(
        color = scoreColor(score).copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = scoreColor(score),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun scoreColor(score: Float) = when {
    score >= 0.75f -> StatusComplete
    score >= 0.50f -> StatusProcessing
    else -> StatusFailed
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ChunkPreviewHighScorePreview() {
    RetrieverTheme {
        ChunkPreview(
            result = SearchResult(
                chunk = TextChunk(
                    id = "c1",
                    documentId = "d1",
                    text = "The Transformer model relies solely on attention mechanisms, dispensing with recurrence and convolutions entirely. Experiments on two machine translation tasks show these models to be superior in quality while being more parallelizable.",
                    chunkIndex = 3,
                    chapterTitle = "3. Model Architecture"
                ),
                document = Document(
                    id = "d1",
                    title = "Attention Is All You Need",
                    author = "Vaswani et al.",
                    format = DocumentFormat.PDF,
                    sourceUri = "content://example/paper.pdf",
                    fileName = "paper.pdf"
                ),
                score = 0.89f
            ),
            query = "attention mechanism transformer"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChunkPreviewLowScorePreview() {
    RetrieverTheme {
        ChunkPreview(
            result = SearchResult(
                chunk = TextChunk(
                    id = "c2",
                    documentId = "d2",
                    text = "Chapter 1 introduces the basic concepts of software craftsmanship and the philosophy behind writing clean, maintainable code.",
                    chunkIndex = 0
                ),
                document = Document(
                    id = "d2",
                    title = "The Pragmatic Programmer",
                    format = DocumentFormat.EPUB,
                    sourceUri = "content://example/book.epub",
                    fileName = "book.epub"
                ),
                score = 0.42f
            ),
            query = "transformer architecture"
        )
    }
}
