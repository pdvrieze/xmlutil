/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.isXmlWhitespace

@XmlUtilInternal
public interface InOutBuffer {
    public val offset: Int

    public val line: Int

    public val column: Int

    public val locationInfo: XmlReader.LocationInfo
        get() = XmlReader.ExtLocationInfo(col = column, line = line, offset = offset)

    public val copySequenceState: State

    /**
     * Mark the start of a sequence that will be copied to string later. By default
     * this will just store the start position. It however also triggers handling of special cases,
     * that may trigger the use of a StringBuilder to store the sequence:
     *  - A line ending involving a '\r' (must be exposed as '\n')
     *  - Buffer swaps
     */
    public fun startCopySequence()

    public fun flushCopySequence() {
        if (copySequenceState == State.ACTIVE) {
            pauseCopySequence()
            resumeCopySequence()
        }
    }

    /**
     * Pause a copy sequence. This means that reading will not add further tokens to the sequence.
     */
    public abstract fun pauseCopySequence()

    public fun resumeCopySequence()

    /**
     * Finish/finalise a copy sequence. This means it cannot be appended to anymore
     */
    public fun finalizeCopySequence(): CharSequence

    /**
     * Read tokens into the sequence up to the expected delimiters. The delimiters will
     * also be consumed.
     *
     * If an end of stream is encountered this function should throw an exception.
     *
     * @param delimiter The expected delimiter.
     * @param pauseOnDelimiter Pause the sequence on observation of the delimiter.
     * @param consumeDelimiter If true, the delimiter will be consumed.
     */
    public fun addDelimitedToCopySequence(delimiter: String, pauseOnDelimiter: Boolean = true, consumeDelimiter: Boolean = true) {
        addDelimitedToCopySequence(delimiter, pauseOnDelimiter, consumeDelimiter) { false }
    }

    public fun addDelimitedToCopySequence(delimiter: Char, pauseOnDelimiter: Boolean = true, consumeDelimiter: Boolean = true) {
        addDelimitedToCopySequence(delimiter, pauseOnDelimiter, consumeDelimiter) { false }
    }

    /**
     * Add the given character to the copy sequence. This requires an active copy sequence.
     * It will force buffering of the underlying read characters if needed.
     */
    public fun addToCopySequence(char: Char)

    public fun addCodepointToCopySequence(codepoint: Int) {
        val c = codepoint
        if (c < 0) error("UNEXPECTED EOF")

        if (c <= 0xffff) {
            addToCopySequence(c.toChar())
            return
        }

        // This comparison works as surrogates are in the 0xd800-0xdfff range
        // write high Unicode value as surrogate pair
        val offset = c - 0x010000

        val high = ((offset ushr 10) + 0xd800).toChar() // high surrogate
        val low = ((offset and 0x3ff) + 0xdc00).toChar() // low surrogate
        addToCopySequence(high)
        addToCopySequence(low)
    }

    public fun addToCopySequence(seq: CharSequence) {
        seq.forEach { addToCopySequence(it) }
    }

    /**
     * Read a range of characters from the input buffer into a sequence.
     * Note that this requires the characters to be still in the buffer. It is intended
     * for handling peeks of entity reference names.
     *
     * Note the caller is required to ensure there are no '\r' characters present
     */
    public fun readSubRange(start: Int, end: Int): CharSequence

    /** Try to read the next character without increasing the position  */
    public fun peek(): Int = peek(0)

    public fun peekChar(): Char {
        val c = peek()
        if (c < 0) error("Unexpected EOF")
        return c.toChar()
    }

    public fun peekChar(offset: Int): Char {
        val c = peek(offset)
        if (c < 0) error("Unexpected EOF")
        return c.toChar()
    }

    /** Determine whether the next character is the expected character, but do not consume it. */
    public fun peek(expected: Char): Boolean = peek(0, expected)

    /**
     * Try to read the next character starting at the given offset.
     * @param offset The offset to use. Note that large offset may break due to missing checks
     */
    public fun peek(offset: Int): Int

    /**
     * Determine whether the following characters match the expected character sequence starting
     * at the given offset.
     *
     * @param expected The expected character sequence. Note that this may not contain '\r'
     * characters as the implementation does not normalize end-of-line.
     */
    public fun peek(offset: Int, expected: CharSequence): Boolean {
        return expected.indices.all { peek(offset + it) == expected[it].code }
    }

    /**
     * Determine whether the following characters match the expected character sequence starting
     * at the given offset.
     *
     * @param expected The expected character. Note that this may not be '\r'
     * characters as the implementation does not normalize end-of-line.
     */
    public fun peek(offset: Int, expected: Char): Boolean {
        return peek(offset) == expected.code
    }

    /**
     * Determine whether the following characters match the expected character sequence)
     *
     * @param expected The expected character sequence. Note that this may not contain '\r'
     * characters as the implementation does not normalize end-of-line.
     */
    public fun peek(expected: CharSequence): Boolean = peek(0, expected)

    /**
     * Skip the given amount of characters. This function should not be used to skip over line endings.
     */
    public fun skip(count: Int)

    public fun markPeekedAsRead(): Unit {
        val _ = read()
    }

    /** Does never read more than needed  */
    public fun read(): Int

    /**
     * Check that the next character is the expected character. If so, consume it.
     */
    public fun tryRead(expected: Char): Boolean = when {
        peek(expected) -> {
            markPeekedAsRead()
            true
        }

        else -> false
    }

    /**
     * Read a sequence of at least 1 whitespace character.
     */
    public fun readWS() {
        val firstChar = readChar()
        require (isXmlWhitespace(firstChar)) { "Expected whitespace, but found non-whitespace: '$firstChar'" }

        while (peek().let { it == '\t'.code || it == '\r'.code || it == ' '.code || it == '\n'.code || it == 0x85 || it == 0x2028 }) {
            markPeekedAsRead() //needs line ending handling
        }
    }

    /**
     * Read a sequence of whitespace characters.
     */
    public fun skipWS() {
        var cnt = 0
        var c = peek()
        while (c >= 0) {
            when (c.toChar()) {
                '\t', ' ' -> cnt += 1
                '\n', '\r', '\u0085', '\u2028' -> {
                    if (cnt > 0) skip(cnt)
                    cnt = 0
                    markPeekedAsRead() // does newlines for us
                }

                else -> break
            }
            c = peek(cnt)
        }
        if (cnt > 0) skip(cnt)
    }

    public fun readChar(): Char {
        val c = read()
        if (c < 0) error("Unexpected EOF")
        return c.toChar()
    }

    /**
     * Read the current character to the copy buffer.
     */
    public fun readToCopyBuffer()

    @XmlUtilInternal
    public enum class State(internal val value: Int) {
        FINALIZED(-3), INACTIVE(-2), PAUSED(-1), ACTIVE(0)
    }
}

internal inline fun InOutBuffer.createCopySequence(block: () -> Unit): CharSequence {
    startCopySequence()
    block()
    return finalizeCopySequence()
}

/**
 * @param stopSequenceOnChar Determines whether the sequence parsing should stop when the character
 * is encountered. It also allows for verifying that the character is allowed.
 */
@XmlUtilInternal
public inline fun InOutBuffer.addDelimitedToCopySequence(delimiter: String, pauseOnDelimiter: Boolean = true, consumeDelimiter: Boolean = true, stopSequenceOnChar: (Char) -> Boolean) {
    var c = peekChar()
    val firstDelim = delimiter[0]
    val otherDelimRange = 1 until delimiter.length
    while (true) {
        when(c) {
            firstDelim if (otherDelimRange.all { peek(it) == delimiter[it].code }) -> {
                if (pauseOnDelimiter) pauseCopySequence()
                if (consumeDelimiter) skip(delimiter.length)
                return
            }

            else if stopSequenceOnChar(c) -> {
                if (pauseOnDelimiter) pauseCopySequence()
                if (consumeDelimiter) skip(delimiter.length)
                return
            }

            else -> {
                markPeekedAsRead()
                c = peekChar()
            }
        }
    }
}

/**
 * @param stopSequenceOnChar Determines whether the sequence parsing should stop when the character
 * is encountered. It also allows for verifying that the character is allowed.
 */
@XmlUtilInternal
public inline fun InOutBuffer.addDelimitedToCopySequence(delimiter: Char, pauseOnDelimiter: Boolean = true, consumeDelimiter: Boolean = true, stopSequenceOnChar: (Char) -> Boolean) {
    var c = peekChar()
    while (c != delimiter && !stopSequenceOnChar(c)) {
        markPeekedAsRead()
        c = peekChar()
    }
    if (pauseOnDelimiter) pauseCopySequence()
    if (consumeDelimiter) markPeekedAsRead()
    return
}
