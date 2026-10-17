package com.ragdroid.domain.repository

import com.ragdroid.core.model.Document
import com.ragdroid.core.model.IngestionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for document metadata CRUD operations.
 * Implemented in the data:repository module.
 */
interface DocumentRepository {

    /** Observe all documents, ordered by most recently updated. */
    fun observeAllDocuments(): Flow<List<Document>>

    /** Observe a single document by its ID. */
    fun observeDocument(documentId: String): Flow<Document?>

    /** Get a document by ID (one-shot). */
    suspend fun getDocument(documentId: String): Document?

    /** Get documents filtered by ingestion status. */
    suspend fun getDocumentsByStatus(status: IngestionStatus): List<Document>

    /** Insert or update a document. */
    suspend fun upsertDocument(document: Document)

    /** Update only the ingestion status of a document. */
    suspend fun updateStatus(documentId: String, status: IngestionStatus)

    /** Delete a document and all its associated chunks/embeddings. */
    suspend fun deleteDocument(documentId: String)

    /** Get the total count of ingested documents. */
    suspend fun getDocumentCount(): Int

    /** Add tags to a document. */
    suspend fun addTags(documentId: String, tags: Set<String>)

    /** Remove tags from a document. */
    suspend fun removeTags(documentId: String, tags: Set<String>)

    /** Get all unique tags across all documents. */
    suspend fun getAllTags(): Set<String>

    /** Get documents that have at least one of the specified tags. */
    suspend fun getDocumentsByTags(tags: Set<String>): List<Document>
}
