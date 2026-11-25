package com.nexus.app

import java.io.Reader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextPreviewReaderTest {
    @Test
    fun keepsContentWhenItFitsWithinTheLimit() {
        val result = readTextPreview(StringReader("hello"), 5)

        assertEquals("hello", result.text)
        assertFalse(result.truncated)
    }

    @Test
    fun truncatesContentBeyondTheLimit() {
        val result = readTextPreview(StringReader("hello world"), 5)

        assertEquals("hello", result.text)
        assertTrue(result.truncated)
    }

    @Test
    fun readsOnlyOneExtraCharacterToDetectTruncation() {
        val reader = CountingReader("x".repeat(100))

        val result = readTextPreview(reader, 5)

        assertEquals("xxxxx", result.text)
        assertTrue(result.truncated)
        assertEquals(6, reader.charactersRead)
    }

    @Test
    fun handlesEmptyInput() {
        val result = readTextPreview(StringReader(""), 5)

        assertEquals("", result.text)
        assertFalse(result.truncated)
    }

    @Test
    fun zeroLimitReturnsNoTextButReportsNonEmptyInput() {
        val result = readTextPreview(StringReader("a"), 0)

        assertEquals("", result.text)
        assertTrue(result.truncated)
    }

    @Test
    fun doesNotReturnAnUnmatchedHighSurrogate() {
        val result = readTextPreview(StringReader("a\uD83D\uDE00b"), 2)

        assertEquals("a", result.text)
        assertTrue(result.truncated)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLimitsAboveTheSupportedMaximum() {
        readTextPreview(StringReader("hello"), 1_000_001)
    }

    private class CountingReader(private val source: String) : Reader() {
        var charactersRead: Int = 0
            private set

        override fun read(buffer: CharArray, offset: Int, length: Int): Int {
            if (charactersRead >= source.length) return -1
            val count = minOf(length, source.length - charactersRead)
            source.toCharArray(
                destination = buffer,
                destinationOffset = offset,
                startIndex = charactersRead,
                endIndex = charactersRead + count
            )
            charactersRead += count
            return count
        }

        override fun close() = Unit
    }
}
