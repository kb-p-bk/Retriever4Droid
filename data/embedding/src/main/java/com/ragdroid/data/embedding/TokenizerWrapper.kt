package com.ragdroid.data.embedding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WordPiece tokenizer for the all-MiniLM-L6-v2 model.
 *
 * Loads vocabulary from assets and performs:
 * 1. Lowercasing
 * 2. Basic punctuation splitting
 * 3. WordPiece subword tokenization
 * 4. Special token insertion ([CLS], [SEP])
 * 5. Padding/truncation to max sequence length
 */
@Singleton
class TokenizerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val VOCAB_ASSET_NAME = "models/vocab.json"
        const val CLS_TOKEN = "[CLS]"
        const val SEP_TOKEN = "[SEP]"
        const val UNK_TOKEN = "[UNK]"
        const val PAD_TOKEN = "[PAD]"
        const val WORDPIECE_PREFIX = "##"
    }

    data class TokenizedInput(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray
    )

    private var vocab: Map<String, Int>? = null

    private suspend fun getVocab(): Map<String, Int> {
        vocab?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open(VOCAB_ASSET_NAME)
                .bufferedReader().readText()
            val jsonObj = JSONObject(json)
            val map = mutableMapOf<String, Int>()
            jsonObj.keys().forEach { key ->
                map[key] = jsonObj.getInt(key)
            }
            vocab = map
            map
        }
    }

    /**
     * Tokenize input text into model-ready tensors.
     */
    suspend fun tokenize(text: String, maxLength: Int): TokenizedInput {
        val vocabulary = getVocab()
        val clsId = vocabulary[CLS_TOKEN] ?: 101
        val sepId = vocabulary[SEP_TOKEN] ?: 102
        val padId = vocabulary[PAD_TOKEN] ?: 0
        val unkId = vocabulary[UNK_TOKEN] ?: 100

        // 1. Lowercase and split into words
        val words = text.lowercase().trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        // 2. WordPiece tokenization
        val tokenIds = mutableListOf(clsId.toLong())
        for (word in words) {
            if (tokenIds.size >= maxLength - 1) break

            val subTokens = wordPieceTokenize(word, vocabulary, unkId)
            for (subToken in subTokens) {
                if (tokenIds.size >= maxLength - 1) break
                tokenIds.add(subToken.toLong())
            }
        }
        tokenIds.add(sepId.toLong())

        // 3. Pad to maxLength
        val paddingSize = maxLength - tokenIds.size
        val attentionMask = LongArray(maxLength) { if (it < tokenIds.size) 1L else 0L }
        val inputIds = LongArray(maxLength) { if (it < tokenIds.size) tokenIds[it] else padId.toLong() }
        val tokenTypeIds = LongArray(maxLength) { 0L }

        return TokenizedInput(inputIds, attentionMask, tokenTypeIds)
    }

    private fun wordPieceTokenize(word: String, vocabulary: Map<String, Int>, unkId: Int): List<Int> {
        val tokens = mutableListOf<Int>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var found = false

            while (start < end) {
                val substr = if (start == 0) {
                    word.substring(start, end)
                } else {
                    WORDPIECE_PREFIX + word.substring(start, end)
                }

                val id = vocabulary[substr]
                if (id != null) {
                    tokens.add(id)
                    start = end
                    found = true
                    break
                }
                end--
            }

            if (!found) {
                tokens.add(unkId)
                start++
            }
        }

        return tokens
    }
}
