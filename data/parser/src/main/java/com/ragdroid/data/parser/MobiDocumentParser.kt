package com.ragdroid.data.parser

import com.ragdroid.core.model.DocumentFormat
import com.ragdroid.domain.service.ParsingPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Best-effort MOBI/PalmDB parser.
 *
 * MOBI is a legacy format built on PalmDB with additional MOBI headers.
 * This parser reads the PalmDB records and extracts text content.
 * For complex MOBI files (DRM, KF8 dual format), it may produce
 * incomplete results — users should be advised to convert to EPUB.
 */
class MobiDocumentParser @Inject constructor() : DocumentParser {

    override val supportedFormat: DocumentFormat = DocumentFormat.MOBI

    override suspend fun parse(
        inputStream: InputStream,
        fileName: String
    ): ParsingPort.ParsedContent = withContext(Dispatchers.IO) {
        val data = inputStream.readBytes()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        try {
            // PalmDB header: first 32 bytes = name, then various fields
            val name = String(data, 0, 32).trim('\u0000')

            // Number of records at offset 76
            buffer.position(76)
            val numRecords = buffer.short.toInt() and 0xFFFF

            // Record offsets start at position 78
            val recordOffsets = (0 until numRecords).map {
                buffer.int // offset
                buffer.int // attributes + uniqueID (skip)
                buffer.position() - 8
                data.let {
                    ByteBuffer.wrap(data, 78 + (it.size.coerceAtMost(0) * 0) + buffer.position() - 8 - 78, 4)
                        .order(ByteOrder.BIG_ENDIAN).int
                }
            }

            // Re-read record offsets properly
            buffer.position(78)
            val offsets = mutableListOf<Int>()
            for (i in 0 until numRecords) {
                offsets.add(buffer.int)
                buffer.int // skip attributes
            }

            // First record contains MOBI header, text starts from record 1
            // Read MOBI header from record 0 to get text encoding and record count
            val record0Start = offsets[0]
            val mobi = ByteBuffer.wrap(data, record0Start, minOf(data.size - record0Start, 512))
                .order(ByteOrder.BIG_ENDIAN)

            // PalmDOC header (first 16 bytes of record 0)
            val compression = mobi.short.toInt() and 0xFFFF
            mobi.short // unused
            val textLength = mobi.int
            val textRecordCount = mobi.short.toInt() and 0xFFFF

            // Extract text records
            val textBuilder = StringBuilder()
            for (i in 1..minOf(textRecordCount, numRecords - 1)) {
                val start = offsets[i]
                val end = if (i + 1 < offsets.size) offsets[i + 1] else data.size
                val recordData = data.sliceArray(start until end)

                val text = when (compression) {
                    1 -> String(recordData, Charsets.UTF_8) // No compression
                    2 -> decompressPalmDoc(recordData)       // PalmDOC compression
                    else -> String(recordData, Charsets.UTF_8) // Fallback
                }
                textBuilder.append(text)
            }

            val rawText = textBuilder.toString().let { text ->
                // Strip HTML if present (MOBI often contains HTML)
                if (text.contains('<') && text.contains('>')) {
                    Jsoup.parse(text).text()
                } else {
                    text
                }
            }.trim()

            val chapters = rawText.split(Regex("\\n{3,}"))
                .mapIndexed { index, section ->
                    ParsingPort.Chapter(
                        title = null,
                        content = section.trim(),
                        index = index
                    )
                }
                .filter { it.content.isNotBlank() }

            ParsingPort.ParsedContent(
                title = name.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.'),
                author = null,
                chapters = chapters.ifEmpty {
                    listOf(ParsingPort.Chapter(title = null, content = rawText, index = 0))
                },
                rawText = rawText,
                metadata = mapOf(
                    "format" to "MOBI",
                    "compression" to compression.toString(),
                    "textRecords" to textRecordCount.toString()
                )
            )
        } catch (e: Exception) {
            // Fallback: treat as raw bytes → string
            val fallbackText = String(data, Charsets.UTF_8)
                .filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,!?;:'-\"()" }
                .trim()

            ParsingPort.ParsedContent(
                title = fileName.substringBeforeLast('.'),
                author = null,
                chapters = listOf(
                    ParsingPort.Chapter(title = null, content = fallbackText, index = 0)
                ),
                rawText = fallbackText,
                metadata = mapOf(
                    "format" to "MOBI",
                    "parseWarning" to "Best-effort parse; consider converting to EPUB for better results"
                )
            )
        }
    }

    /**
     * Decompress PalmDOC LZ77-variant compressed data.
     */
    private fun decompressPalmDoc(compressed: ByteArray): String {
        val output = StringBuilder()
        var i = 0
        while (i < compressed.size) {
            val byte = compressed[i].toInt() and 0xFF
            when {
                byte == 0 -> {
                    output.append('\u0000')
                    i++
                }
                byte in 1..8 -> {
                    // Copy next 'byte' bytes literally
                    for (j in 0 until byte) {
                        if (i + 1 + j < compressed.size) {
                            output.append(compressed[i + 1 + j].toInt().toChar())
                        }
                    }
                    i += byte + 1
                }
                byte in 9..0x7F -> {
                    output.append(byte.toChar())
                    i++
                }
                byte in 0x80..0xBF -> {
                    // Distance-length pair
                    if (i + 1 < compressed.size) {
                        val next = compressed[i + 1].toInt() and 0xFF
                        val distance = ((byte shl 8) or next) shr 3 and 0x7FF
                        val length = (next and 0x07) + 3
                        val start = output.length - distance
                        for (j in 0 until length) {
                            if (start + j >= 0 && start + j < output.length) {
                                output.append(output[start + j])
                            }
                        }
                        i += 2
                    } else {
                        i++
                    }
                }
                else -> {
                    // 0xC0..0xFF: space + char
                    output.append(' ')
                    output.append((byte xor 0x80).toChar())
                    i++
                }
            }
        }
        return output.toString()
    }
}
