package com.ragdroid.data.vectorstore.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

/**
 * ObjectBox entity for storing text chunks with their embedding vectors.
 * The embedding field has an HNSW index for fast nearest-neighbor search.
 */
@Entity
data class ChunkEntity(
    @Id var id: Long = 0,
    var chunkId: String = "",
    var documentId: String = "",
    var text: String = "",
    var chunkIndex: Int = 0,
    var chapterTitle: String? = null,
    var startOffset: Int = 0,
    var endOffset: Int = 0,
    var metadataJson: String = "{}",

    /** 384-dimensional embedding vector with HNSW index for similarity search. */
    @HnswIndex(dimensions = 384)
    var embedding: FloatArray = floatArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkEntity) return false
        return id == other.id && chunkId == other.chunkId
    }

    override fun hashCode(): Int = chunkId.hashCode()
}
