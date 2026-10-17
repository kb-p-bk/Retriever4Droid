package com.ragdroid.data.vectorstore

import com.ragdroid.core.model.Document
import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.core.model.EmbeddingVector
import com.ragdroid.core.model.IngestionStatus
import com.ragdroid.core.model.SearchResult
import com.ragdroid.core.model.TextChunk
import com.ragdroid.data.vectorstore.entity.ChunkEntity
import com.ragdroid.data.vectorstore.entity.ChunkEntity_
import com.ragdroid.data.vectorstore.entity.DocumentEntity
import com.ragdroid.data.vectorstore.entity.DocumentEntity_
import com.ragdroid.domain.repository.DocumentRepository
import com.ragdroid.domain.repository.VectorRepository
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.flow
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Combined implementation of [DocumentRepository] and [VectorRepository]
 * backed by ObjectBox. Uses HNSW index on ChunkEntity.embedding for
 * nearest-neighbor search.
 */
@Singleton
class ObjectBoxVectorStore @Inject constructor(
    private val provider: ObjectBoxProvider
) : VectorRepository, DocumentRepository {

    private val documentBox by lazy { provider.boxStore.boxFor<DocumentEntity>() }
    private val chunkBox by lazy { provider.boxStore.boxFor<ChunkEntity>() }

    // ─── VectorRepository ────────────────────────────────────────────

    override suspend fun storeChunksWithEmbeddings(
        chunks: List<TextChunk>,
        embeddings: List<EmbeddingVector>
    ) = withContext(Dispatchers.IO) {
        val embeddingMap = embeddings.associateBy { it.chunkId }
        val entities = chunks.map { chunk ->
            val embedding = embeddingMap[chunk.id]
                ?: throw IllegalArgumentException("Missing embedding for chunk ${chunk.id}")
            ChunkEntity(
                chunkId = chunk.id,
                documentId = chunk.documentId,
                text = chunk.text,
                chunkIndex = chunk.chunkIndex,
                chapterTitle = chunk.chapterTitle,
                startOffset = chunk.startOffset,
                endOffset = chunk.endOffset,
                metadataJson = JSONObject(chunk.metadata).toString(),
                embedding = embedding.vector
            )
        }
        chunkBox.put(entities)
    }

    override suspend fun findNearest(
        queryEmbedding: FloatArray,
        topK: Int,
        minScore: Float,
        documentIds: Set<String>?
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val query = chunkBox.query(
            ChunkEntity_.embedding.nearestNeighbors(queryEmbedding, topK)
        ).build()

        val results = query.findWithScores()
        query.close()

        results.mapNotNull { scoredEntity ->
            val entity = scoredEntity.get()
            
            // Filter by document IDs if specified
            if (documentIds != null && entity.documentId !in documentIds) {
                return@mapNotNull null
            }
            
            val score = scoredEntity.score.toFloat()

            // ObjectBox returns distance; convert to similarity score
            val similarity = 1.0f / (1.0f + score)
            if (similarity < minScore) return@mapNotNull null

            val document = findDocumentEntity(entity.documentId)?.toDomain()
                ?: return@mapNotNull null

            SearchResult(
                chunk = entity.toDomain(),
                document = document,
                score = similarity
            )
        }
    }

    override suspend fun deleteByDocumentId(documentId: String) = withContext(Dispatchers.IO) {
        val query = chunkBox.query(
            ChunkEntity_.documentId.equal(documentId)
        ).build()
        query.remove()
        query.close()
    }

    override suspend fun getChunkCount(documentId: String): Int = withContext(Dispatchers.IO) {
        val query = chunkBox.query(
            ChunkEntity_.documentId.equal(documentId)
        ).build()
        val count = query.count().toInt()
        query.close()
        count
    }

    override suspend fun getChunksForDocument(documentId: String): List<TextChunk> =
        withContext(Dispatchers.IO) {
            val query = chunkBox.query(
                ChunkEntity_.documentId.equal(documentId)
            ).order(ChunkEntity_.chunkIndex).build()
            val results = query.find().map { it.toDomain() }
            query.close()
            results
        }

    // ─── DocumentRepository ──────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllDocuments(): Flow<List<Document>> {
        val query = documentBox.query()
            .order(DocumentEntity_.updatedAt, QueryBuilder.DESCENDING)
            .build()
        return query.flow().map { entities -> entities.map { it.toDomain() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDocument(documentId: String): Flow<Document?> {
        val query = documentBox.query(
            DocumentEntity_.documentId.equal(documentId)
        ).build()
        return query.flow().map { entities -> entities.firstOrNull()?.toDomain() }
    }

    override suspend fun getDocument(documentId: String): Document? =
        withContext(Dispatchers.IO) {
            findDocumentEntity(documentId)?.toDomain()
        }

    override suspend fun getDocumentsByStatus(status: IngestionStatus): List<Document> =
        withContext(Dispatchers.IO) {
            val query = documentBox.query(
                DocumentEntity_.status.equal(status.name)
            ).build()
            val results = query.find().map { it.toDomain() }
            query.close()
            results
        }

    override suspend fun upsertDocument(document: Document) {
        withContext(Dispatchers.IO) {
            val existing = findDocumentEntity(document.id)
            val entity = document.toEntity(existing?.id ?: 0)
            documentBox.put(entity)
        }
    }

    override suspend fun updateStatus(documentId: String, status: IngestionStatus) {
        withContext(Dispatchers.IO) {
            val entity = findDocumentEntity(documentId) ?: return@withContext
            entity.status = status.name
            entity.updatedAt = System.currentTimeMillis()
            documentBox.put(entity)
        }
    }

    override suspend fun deleteDocument(documentId: String) {
        withContext(Dispatchers.IO) {
            val entity = findDocumentEntity(documentId) ?: return@withContext
            documentBox.remove(entity)
        }
    }

    override suspend fun getDocumentCount(): Int = withContext(Dispatchers.IO) {
        documentBox.count().toInt()
    }

    override suspend fun addTags(documentId: String, tags: Set<String>) {
        withContext(Dispatchers.IO) {
            val entity = findDocumentEntity(documentId) ?: return@withContext
            val currentTags = try {
                val jsonArray = org.json.JSONArray(entity.tagsJson)
                (0 until jsonArray.length()).mapTo(mutableSetOf()) { jsonArray.getString(it) }
            } catch (e: Exception) {
                mutableSetOf()
            }
            currentTags.addAll(tags)
            entity.tagsJson = org.json.JSONArray(currentTags.toList()).toString()
            entity.updatedAt = System.currentTimeMillis()
            documentBox.put(entity)
        }
    }

    override suspend fun removeTags(documentId: String, tags: Set<String>) {
        withContext(Dispatchers.IO) {
            val entity = findDocumentEntity(documentId) ?: return@withContext
            val currentTags = try {
                val jsonArray = org.json.JSONArray(entity.tagsJson)
                (0 until jsonArray.length()).mapTo(mutableSetOf()) { jsonArray.getString(it) }
            } catch (e: Exception) {
                mutableSetOf()
            }
            currentTags.removeAll(tags)
            entity.tagsJson = org.json.JSONArray(currentTags.toList()).toString()
            entity.updatedAt = System.currentTimeMillis()
            documentBox.put(entity)
        }
    }

    override suspend fun getAllTags(): Set<String> = withContext(Dispatchers.IO) {
        val allTags = mutableSetOf<String>()
        documentBox.all.forEach { entity ->
            try {
                val jsonArray = org.json.JSONArray(entity.tagsJson)
                for (i in 0 until jsonArray.length()) {
                    allTags.add(jsonArray.getString(i))
                }
            } catch (e: Exception) {
                // Skip malformed tag data
            }
        }
        allTags
    }

    override suspend fun getDocumentsByTags(tags: Set<String>): List<Document> =
        withContext(Dispatchers.IO) {
            if (tags.isEmpty()) return@withContext emptyList()
            
            documentBox.all.filter { entity ->
                try {
                    val jsonArray = org.json.JSONArray(entity.tagsJson)
                    val docTags = (0 until jsonArray.length()).map { jsonArray.getString(it) }.toSet()
                    docTags.intersect(tags).isNotEmpty()
                } catch (e: Exception) {
                    false
                }
            }.map { it.toDomain() }
        }

    // ─── Mappers ─────────────────────────────────────────────────────

    private fun findDocumentEntity(documentId: String): DocumentEntity? {
        val query = documentBox.query(
            DocumentEntity_.documentId.equal(documentId)
        ).build()
        val result = query.findFirst()
        query.close()
        return result
    }

    private fun DocumentEntity.toDomain() = Document(
        id = documentId,
        title = title,
        author = author,
        format = DocumentFormat.valueOf(format),
        sourceUri = sourceUri,
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        chunkCount = chunkCount,
        status = IngestionStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = updatedAt,
        metadata = try {
            val json = JSONObject(metadataJson)
            json.keys().asSequence().associateWith { json.getString(it) }
        } catch (e: Exception) {
            emptyMap()
        },
        tags = try {
            val jsonArray = org.json.JSONArray(tagsJson)
            (0 until jsonArray.length()).mapTo(mutableSetOf()) { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptySet()
        },
        embeddingModel = embeddingModel?.takeIf { it.isNotBlank() } ?: "all-MiniLM-L6-v2"
    )

    private fun Document.toEntity(existingId: Long = 0) = DocumentEntity(
        id = existingId,
        documentId = id,
        title = title,
        author = author,
        format = format.name,
        sourceUri = sourceUri,
        fileName = fileName,
        fileSizeBytes = fileSizeBytes,
        chunkCount = chunkCount,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        metadataJson = JSONObject(metadata).toString(),
        tagsJson = org.json.JSONArray(tags.toList()).toString(),
        embeddingModel = embeddingModel
    )

    private fun ChunkEntity.toDomain() = TextChunk(
        id = chunkId,
        documentId = documentId,
        text = text,
        chunkIndex = chunkIndex,
        chapterTitle = chapterTitle,
        startOffset = startOffset,
        endOffset = endOffset,
        metadata = try {
            val json = JSONObject(metadataJson)
            json.keys().asSequence().associateWith { json.getString(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    )
}
