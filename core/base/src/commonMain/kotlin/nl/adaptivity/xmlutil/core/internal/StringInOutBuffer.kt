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

package nl.adaptivity.xmlutil.core.internal

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.InOutBuffer
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.ifAssertions


@XmlUtilInternal
public class StringInOutBuffer(public val input: CharSequence): InOutBuffer {

    /** Current position in the buffer */
    override var offset: Int
        private set

    init {
        offset = when {
            input.isEmpty() || input[0] != '\uFEFF' -> 0
            else -> 1
        }
    }

    private val offsetBase: Int get() = 0

    override var line: Int = 1

    private var lastColumnStart: Int = 0
        set

    override val column: Int get() = offset - lastColumnStart + 1 // first char is 1, not 0

    private var copySequenceStart = -1
    private var copyBuilder: StringBuilder? = null

    override val copySequenceState: State
        get() = when {
            copySequenceStart >= 0 -> State.ACTIVE
            copySequenceStart == -1 -> State.INACTIVE
            else -> State.PAUSED
        }

    override fun startCopySequence() {
        if (DEBUG) {
            assert(copySequenceStart < 0 && copyBuilder == null) { "Copy sequence already started" }
        }
        copySequenceStart = offset
    }

    override fun flushCopySequence() {
        if (copySequenceStart >= 0) {
            val b = copyBuilder ?: StringBuilder(offset - copySequenceStart).also { copyBuilder = it }
            b.appendRange(input, copySequenceStart, offset)
            copySequenceStart = offset
        }
    }

    override fun pauseCopySequence() {
        check(copySequenceStart>=0) { "Copy sequence not active (either not started or already suspended)" }
        val b = copyBuilder ?: StringBuilder(offset - copySequenceStart).also { copyBuilder = it }
        b.appendRange(input, copySequenceStart, offset)
        copySequenceStart = -2 // mark as paused
    }

    override fun resumeCopySequence() {
        check(copySequenceStart < -1 && copyBuilder != null) { "Copy sequence is not paused" }
        copySequenceStart = offset
    }

    override fun finalizeCopySequence(): String {
        val b = copyBuilder
        val start = copySequenceStart
        copySequenceStart = -1
        copyBuilder = null

        when {
            b == null && (start < 0) -> error("No copy sequence started")

            b == null -> return input.substring(start, offset)

            start >= 0 ->
                return b.appendRange(input, start, offset).toString()

            else -> return b.toString()
        }
    }

    override fun readToCopyBuffer() {
        if (read() < 0) error("End of stream while adding character to copy buffer")
    }

    /**
     * Helper function that ensures a copy builder. It also ensures that the pending writes are
     * added to ensure there is no reordering.
     */
    private fun ensureCopyBuilder(sizeHint: Int = 16): StringBuilder {
        val b = copyBuilder ?: StringBuilder(sizeHint).also { copyBuilder = it }

        if (copySequenceStart >= 0 && offset > copySequenceStart) { // active. Needs temporary pause
            b.appendRange(input, copySequenceStart, offset)
            copySequenceStart = offset
        }
        return b
    }

    override fun addToCopySequence(char: Char) {
        this.ifAssertions {
            assert(copySequenceState != State.INACTIVE) {
                "Copy sequence not active (either not started or suspended)"
            }
        }

        val b = ensureCopyBuilder()

        b.append(char)
    }

    override fun addToCopySequence(seq: CharSequence) {
        this.ifAssertions {
            assert(copySequenceStart >= 0 || copyBuilder != null) {
                "Copy sequence not active (either not started or suspended)"
            }
        }

        val b = ensureCopyBuilder()

        b.append(seq)
    }

    override fun readSubRange(start: Int, end: Int): CharSequence {
        return input.subSequence(start, end)
    }

    /** Try to read the next character without increasing the position  */
    override fun peek(): Int {
        return peekCommon(offset)
    }

    override fun peek(offset: Int): Int {
        return peekCommon(this.offset + offset)
    }

    private fun peekCommon(bufPos: Int): Int {
        return if (bufPos >= input.length) -1
        else {
            when (val c = input[bufPos]) {
                '\r', '\u0085', '\u2028' -> '\n'.code
                else -> c.code
            }
        }
    }

    /**
     * This implementation matches, but also assumes that the expected sequence is not such large
     * as to not require range checks for the buffers (only end of file).
     */
    override fun peek(expected: CharSequence): Boolean {
        return peekCommon(offset, expected)
    }

    /**
     * This implementation matches, but also assumes that the expected sequence is not such large
     * as to not require range checks for the buffers (only end of file).
     */
    override fun peek(offset: Int, expected: CharSequence): Boolean {
        return peekCommon(this.offset + offset, expected)
    }

    private fun peekCommon(bufPos: Int, expected: CharSequence): Boolean {
        val l = expected.length
        if (bufPos + l > input.length) return false // must be end of file
        // most common
        for (i in 0 until l) {
            if (expected[i] != input[bufPos + i]) return false
        }
        return true
    }

    override fun skip(count: Int) {
        val newPos = offset + count
        // allow skipping just past the last character.
        if (newPos > input.length) error("End of file while skipping")
        offset = newPos
    }

    override fun markPeekedAsRead() {

        val oldPos = offset
        val peeked = input[oldPos]
        when (peeked) {
            '\r' -> handle2CharLineEnd(oldPos)
            '\n', '\u0085', '\u2028' -> handleLineEnd(oldPos + 1)
            else -> offset = oldPos + 1
        }
    }

    private fun handleLineEnd(newPos: Int) {
        // this cannot use peek as peek normalized (so the test will not work)
        if (copySequenceState == State.ACTIVE && input[offset] != '\n') {
            pauseCopySequence()
            addToCopySequence('\n')
            offset = newPos
            resumeCopySequence()
        } else {
            offset = newPos
        }
        lastColumnStart = newPos
        line += 1
    }

    private fun handle2CharLineEnd(oldPos: Int) {
        val nextChar = peek(1)
        val inc = if(nextChar == 0xA || nextChar == 0x85) 2 else 1
        handleLineEnd(oldPos + inc)
    }


    /** Does never read more than needed  */
    override fun read(): Int {
        // In this case we *may* need the right buffer, otherwise not
        // optimize this implementation for the "happy" path
        val oldPos = offset
        if (oldPos >= input.length) return -1

        when (val char = input[oldPos]) {
            '\r' -> { // as \r is always transformed to \n, this requires a stringBuilder.
                handle2CharLineEnd(oldPos)
                return '\n'.code
            }

            '\u0085',
            '\u2028' -> {
                handleLineEnd(oldPos + 1)
                return '\n'.code
            }

            '\n' -> {
                offset = oldPos + 1
                lastColumnStart = oldPos + 1
                return '\n'.code
            }

            else -> {
                offset = oldPos + 1
                return char.code
            }
        }
    }


    override fun toString(): String {
        return buildString {
            append("StringInputBuffer(")
            append("Next = '")
                .append(input.substring(offset, (offset + 10).coerceAtMost(input.length).coerceAtMost(input.length)))
                .append("', output buffer = ")

            val b = copyBuilder
            if (copySequenceStart < 0 && b == null) {
                append("null)")
            } else {
                append('\'')
                if (b!=null) append(b)
                if (copySequenceStart>=0) appendRange(input, copySequenceStart, offset)
                append("')")
            }
        }
    }

    private typealias State = InOutBuffer.State
}
