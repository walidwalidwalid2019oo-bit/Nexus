package com.nexus.app

import java.io.Reader

internal data class TextPreviewResult(
    val text: String,
    val truncated: Boolean
)

/**
 * Reads at most maxChars + 1 UTF-16 code units so callers can detect truncation
 * without loading an entire document into memory.
 */
internal fun readTextPreview(
    reader: Reader,
    maxChars: Int = 1_000_000
): TextPreviewResult {
    require(maxChars in 0..1_000_000) {
        "maxChars must be between 0 and 1,000,000"
    }

    val readLimit = maxChars + 1
    val content = StringBuilder(minOf(readLimit, 8_192))
    val buffer = CharArray(8_192)

    while (content.length < readLimit) {
        val requested = minOf(buffer.size, readLimit - content.length)
        val count = reader.read(buffer, 0, requested)
        when {
            count < 0 -> break
            count == 0 -> {
                val next = reader.read()
                if (next < 0) break
                content.append(next.toChar())
            }
            else -> content.append(buffer, 0, count)
        }
    }

    val truncated = content.length > maxChars
    var end = minOf(content.length, maxChars)

    // Do not leave an unmatched UTF-16 high surrogate at the preview boundary.
    if (truncated && end > 0 && Character.isHighSurrogate(content[end - 1])) {
        end -= 1
    }

    return TextPreviewResult(content.substring(0, end), truncated)
}
