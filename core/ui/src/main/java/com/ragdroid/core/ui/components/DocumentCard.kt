package com.ragdroid.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ragdroid.core.model.Document
import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.core.model.IngestionStatus
import com.ragdroid.core.ui.theme.FormatDocx
import com.ragdroid.core.ui.theme.FormatEpub
import com.ragdroid.core.ui.theme.FormatMobi
import com.ragdroid.core.ui.theme.FormatPdf
import com.ragdroid.core.ui.theme.FormatTxt
import com.ragdroid.core.ui.theme.RetrieverTheme
import com.ragdroid.core.ui.theme.StatusComplete
import com.ragdroid.core.ui.theme.StatusFailed
import com.ragdroid.core.ui.theme.StatusPending
import com.ragdroid.core.ui.theme.StatusProcessing

/**
 * Card representing a single ingested [Document] in the Library screen.
 *
 * Displays the document title, format badge, chunk count, and ingestion status.
 *
 * @param document  The document to display.
 * @param onClick   Called when the card is tapped.
 * @param modifier  Optional [Modifier].
 */
@Composable
fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Format icon
            Icon(
                imageVector = if (document.format in listOf(DocumentFormat.EPUB, DocumentFormat.MOBI))
                    Icons.Default.MenuBook else Icons.Default.Description,
                contentDescription = document.format.name,
                tint = document.format.color(),
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Author (if available) and file name
                document.author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Format badge
                    FormatBadge(format = document.format)

                    // Chunk count
                    if (document.chunkCount > 0) {
                        Text(
                            text = "${document.chunkCount} chunks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Status indicator
            StatusDot(status = document.status)
        }
    }
}

@Composable
private fun FormatBadge(format: DocumentFormat) {
    Surface(
        color = format.color().copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = format.name,
            style = MaterialTheme.typography.labelSmall,
            color = format.color(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusDot(status: IngestionStatus) {
    val color = when (status) {
        IngestionStatus.COMPLETED -> StatusComplete
        IngestionStatus.FAILED -> StatusFailed
        IngestionStatus.PENDING -> StatusPending
        IngestionStatus.PARSING,
        IngestionStatus.CHUNKING,
        IngestionStatus.EMBEDDING -> StatusProcessing
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(50),
        modifier = Modifier.size(10.dp)
    ) {}
}

private fun DocumentFormat.color(): Color = when (this) {
    DocumentFormat.PDF -> FormatPdf
    DocumentFormat.EPUB -> FormatEpub
    DocumentFormat.MOBI -> FormatMobi
    DocumentFormat.DOCX -> FormatDocx
    DocumentFormat.TXT -> FormatTxt
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun DocumentCardPreview() {
    RetrieverTheme {
        DocumentCard(
            document = Document(
                id = "1",
                title = "Attention Is All You Need",
                author = "Vaswani et al.",
                format = DocumentFormat.PDF,
                sourceUri = "content://example/paper.pdf",
                fileName = "paper.pdf",
                fileSizeBytes = 1_024_000,
                chunkCount = 42,
                status = IngestionStatus.COMPLETED
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentCardProcessingPreview() {
    RetrieverTheme {
        DocumentCard(
            document = Document(
                id = "2",
                title = "The Pragmatic Programmer",
                format = DocumentFormat.EPUB,
                sourceUri = "content://example/book.epub",
                fileName = "book.epub",
                status = IngestionStatus.EMBEDDING
            ),
            onClick = {}
        )
    }
}
