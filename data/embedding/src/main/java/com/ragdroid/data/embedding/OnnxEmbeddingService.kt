package com.ragdroid.data.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.ragdroid.domain.service.EmbeddingPort
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * On-device embedding service using ONNX Runtime.
 * Loads the active ONNX model dynamically from [ModelManager].
 */
@Singleton
class OnnxEmbeddingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager,
    private val tokenizer: TokenizerWrapper
) : EmbeddingPort {

    companion object {
        const val EMBEDDING_DIMENSIONS = 384
        const val MAX_SEQUENCE_LENGTH = 128
    }

    private val mutex = Mutex()
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    suspend fun resetSession() = mutex.withLock {
        try {
            ortSession?.close()
        } catch (e: Exception) {
            // Ignore session close exceptions
        }
        ortSession = null
    }

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val session = getOrCreateSession()
        val tokens = tokenizer.tokenize(text, MAX_SEQUENCE_LENGTH)
        runInference(session, tokens)
    }

    override suspend fun embedBatch(texts: List<String>): List<FloatArray> =
        withContext(Dispatchers.Default) {
            // Process sequentially to control memory usage on mobile
            texts.map { embed(it) }
        }

    override fun dimensions(): Int = EMBEDDING_DIMENSIONS

    override suspend fun isReady(): Boolean = try {
        getOrCreateSession()
        true
    } catch (e: Exception) {
        false
    }

    private suspend fun getOrCreateSession(): OrtSession = mutex.withLock {
        ortSession ?: run {
            val env = OrtEnvironment.getEnvironment()
            ortEnvironment = env

            val modelPath = modelManager.getModelPath()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            }
            val session = env.createSession(modelPath, sessionOptions)
            ortSession = session
            session
        }
    }

    private fun runInference(session: OrtSession, tokens: TokenizerWrapper.TokenizedInput): FloatArray {
        val env = ortEnvironment ?: throw IllegalStateException("ORT environment not initialized")

        val inputIds = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(tokens.inputIds),
            longArrayOf(1, tokens.inputIds.size.toLong())
        )
        val attentionMask = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(tokens.attentionMask),
            longArrayOf(1, tokens.attentionMask.size.toLong())
        )
        val tokenTypeIds = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(tokens.tokenTypeIds),
            longArrayOf(1, tokens.tokenTypeIds.size.toLong())
        )

        val inputs = mapOf(
            "input_ids" to inputIds,
            "attention_mask" to attentionMask,
            "token_type_ids" to tokenTypeIds
        )

        return session.run(inputs).use { result ->
            // Output shape: [1, sequence_length, 384]
            @Suppress("UNCHECKED_CAST")
            val output = result[0].value as Array<Array<FloatArray>>

            // Mean pooling over the sequence dimension, masked by attention
            meanPooling(output[0], tokens.attentionMask)
        }.also {
            inputIds.close()
            attentionMask.close()
            tokenTypeIds.close()
        }
    }

    /**
     * Mean pooling: average token embeddings weighted by attention mask,
     * then L2-normalize the result.
     */
    private fun meanPooling(tokenEmbeddings: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        val dims = tokenEmbeddings[0].size
        val pooled = FloatArray(dims)
        var maskSum = 0f

        for (i in tokenEmbeddings.indices) {
            val mask = attentionMask[i].toFloat()
            maskSum += mask
            for (j in 0 until dims) {
                pooled[j] += tokenEmbeddings[i][j] * mask
            }
        }

        if (maskSum > 0) {
            for (j in 0 until dims) {
                pooled[j] /= maskSum
            }
        }

        // L2 normalize
        val norm = sqrt(pooled.sumOf { (it * it).toDouble() }).toFloat()
        if (norm > 0) {
            for (j in 0 until dims) {
                pooled[j] /= norm
            }
        }

        return pooled
    }
}
