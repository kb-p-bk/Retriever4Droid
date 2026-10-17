package com.ragdroid.data.vectorstore.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

/**
 * ObjectBox entity for storing document metadata.
 */
@Entity
data class DocumentEntity(
    @Id var id: Long = 0,
    var documentId: String = "",
    var title: String = "",
    var author: String? = null,
    var format: String = "",
    var sourceUri: String = "",
    var fileName: String = "",
    var fileSizeBytes: Long = 0,
    var chunkCount: Int = 0,
    var status: String = "PENDING",
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var metadataJson: String = "{}",
    var tagsJson: String = "[]",
    var embeddingModel: String? = "all-MiniLM-L6-v2"
)
