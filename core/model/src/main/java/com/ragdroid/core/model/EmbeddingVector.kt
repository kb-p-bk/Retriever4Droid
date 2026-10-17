package com.ragdroid.core.model

/**
 * Wraps embedding vector data with its source chunk reference.
 */
data class EmbeddingVector(
    val chunkId: String,
    val vector: FloatArray,
    val dimensions: Int = vector.size
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingVector) return false
        return chunkId == other.chunkId && vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        var result = chunkId.hashCode()
        result = 31 * result + vector.contentHashCode()
        return result
    }
}
