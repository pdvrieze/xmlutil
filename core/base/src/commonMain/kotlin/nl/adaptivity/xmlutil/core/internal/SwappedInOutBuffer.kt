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
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.multiplatform.ifAssertions


private const val BUF_SIZE = 4096

@XmlUtilInternal
public class SwappedInOutBuffer(public val reader: Reader): InOutBuffer {

    /** Current position in the buffer */
    internal var srcBufPos: Int = 0

    /** Combined count of characters in the buffer */
    internal var srcBufCount: Int = 0

    /** Current buffer to parse from */
    internal var bufLeft = CharArray(BUF_SIZE)

    /** Next pending buffer */
    internal var bufRight: CharArray// = CharArray(BUF_SIZE)

    /**
     * Rather than update offset on each character read, we only update it on buffer swap.
     */
    private var offsetBase = 0

    override val offset: Int
        get() = offsetBase + srcBufPos

    override var line: Int = 1

    private var lastColumnStart: Int = 0
        set

    override val column: Int get() = offset - lastColumnStart + 1 // first char is 1, not 0

    init { // Read the first buffers on creation, rather than delayed
        var cnt = readUntilFullOrEOF(bufLeft)
        require(cnt >= 0) { "Trying to parse an empty file (that is not valid XML)" }
        if (cnt < BUF_SIZE) {
            bufRight = CharArray(0)
            srcBufCount = cnt
        } else {
            val newRight = CharArray(BUF_SIZE)
            bufRight = newRight
            cnt = readUntilFullOrEOF(newRight).coerceAtLeast(0) // in case the EOF is exactly at the boundary
            srcBufCount = BUF_SIZE + cnt
        }

        if (bufLeft[0].code == 0x0feff) {
            srcBufPos = 1 /* drop BOM */
            lastColumnStart = 1
        }
    }

    private fun swapInputBuffer() {
        // invalidate the copy sequence
        delayedCopySequence.copiedStart = -3
        delayedCopySequence.copiedEnd = -3

        if (copySequenceStart in 0..<BUF_SIZE) {
            // Store the "left" copy sequence into a buffer and start it at the beginning of the
            // right (new left) buffer
            val b = copyBuilder ?: StringBuilder(16).also { copyBuilder = it }
            val p = srcBufPos
            b.appendRange(bufLeft, copySequenceStart, p.coerceAtMost(BUF_SIZE))

            copySequenceStart = 0
        } else if (copySequenceStart >= BUF_SIZE) {
            // If somehow the sequence is beyond the left buffer, adjust it for the swap
            copySequenceStart -= BUF_SIZE
        }
        val rightBufCount = srcBufCount - BUF_SIZE
        if (rightBufCount < 0) error("End of stream while swapping inputs")

        val oldLeft = bufLeft
        bufLeft = bufRight
        bufRight = oldLeft

        srcBufPos -= BUF_SIZE
        offsetBase += BUF_SIZE

        if (rightBufCount >= BUF_SIZE) {
            val newRead = readUntilFullOrEOF(bufRight)
            srcBufCount = when {
                newRead < 0 -> rightBufCount
                else -> rightBufCount + newRead
            }
        } else {
            srcBufCount = rightBufCount
        }
    }

    private var copySequenceStart = State.INACTIVE.value
    private var copyBuilder: StringBuilder? = null
    private var preInitCopyBuilder: StringBuilder = StringBuilder(256)

    override val copySequenceState: State
        get() = when {
            copySequenceStart >= 0 -> State.ACTIVE
            copySequenceStart == State.INACTIVE.value -> State.INACTIVE
            copySequenceStart == State.PAUSED.value -> State.PAUSED
            else -> State.FINALIZED
        }

    override fun startCopySequence() {
        // invalidate
        delayedCopySequence.copiedStart = -3
        delayedCopySequence.copiedEnd = -3

        // no copy builder anymore
        copyBuilder = null

        copySequenceStart = srcBufPos
    }

    override fun flushCopySequence() {
        if (copySequenceStart >= 0) {
            val b = copyBuilder ?: StringBuilder(offset - copySequenceStart).also { copyBuilder = it }
            b.appendRange(bufLeft, copySequenceStart, srcBufPos)
            copySequenceStart = srcBufPos
        }
    }

    override fun pauseCopySequence() {
        check(copySequenceStart >= State.ACTIVE.value) { "Copy sequence not active (either not started or already suspended)" }
        val b = copyBuilder ?: preInitCopyBuilder.also { it.clear(); copyBuilder = it }
        b.appendRange(bufLeft, copySequenceStart, srcBufPos)
        copySequenceStart = State.PAUSED.value // mark as paused
    }

    override fun resumeCopySequence() {
        check(copySequenceStart == State.PAUSED.value) { "Copy sequence is not paused" }
        copySequenceStart = srcBufPos
    }

    override fun finalizeCopySequence(): CharSequence {
        val b = copyBuilder
        val start = copySequenceStart
        if (b == null && start < 0) error("No copy sequence started")

        copySequenceStart = State.FINALIZED.value
        delayedCopySequence.copiedStart = start
        delayedCopySequence.copiedEnd = srcBufPos

        return delayedCopySequence
    }

    override fun readToCopyBuffer() {
        if (read() < 0) error("End of stream while adding character to copy buffer")
    }

    /**
     * Helper function that ensures a copy builder. It also ensures that the pending writes are
     * added to ensure there is no reordering.
     */
    private fun ensureCopyBuilder(sizeHint: Int = 16): StringBuilder {
        val b = copyBuilder ?: preInitCopyBuilder.also { it.clear(); copyBuilder = it }

        if (copySequenceStart >= 0 && srcBufPos > copySequenceStart) { // active. Needs temporary pause
            b.appendRange(bufLeft, copySequenceStart, srcBufPos)
            copySequenceStart = srcBufPos
        }
        return b
    }

    override fun addToCopySequence(char: Char) {
        this.ifAssertions {
            assert(copySequenceStart >= State.PAUSED.value) {
                "Copy sequence not active (either not started or suspended): $copySequenceStart"
            }
        }

        val b = ensureCopyBuilder()

        b.append(char)
    }

    override fun addToCopySequence(seq: CharSequence) {
        this.ifAssertions {
            assert(copySequenceStart >= State.PAUSED.value || copyBuilder != null) {
                "Copy sequence not active (either not started or suspended)"
            }
        }

        val b = ensureCopyBuilder()

        b.append(seq)
    }

    override fun readSubRange(start: Int, end: Int): CharSequence {
        val bufStart = start - offsetBase
        val bufEnd = end - offsetBase
        if (bufEnd >= srcBufCount) error("End of file in subrange")
        return when {
            bufStart >= BUF_SIZE ->
                bufRight.concatToString(bufStart - BUF_SIZE, bufEnd - BUF_SIZE)

            bufEnd <= BUF_SIZE -> bufLeft.concatToString(bufStart, bufEnd)


            else -> buildString {
                appendRange(bufLeft, bufStart, BUF_SIZE)
                appendRange(bufRight, 0, (bufEnd - BUF_SIZE))
            }
        }

    }

    /** Try to read the next character without increasing the position  */
    override fun peek(): Int {
        return peekCommon(srcBufPos)
    }

    override fun peek(offset: Int): Int {
        return peekCommon(srcBufPos + offset)
    }

    private fun rawPeek(bufPos: Int):Int = when {
        bufPos >= srcBufCount -> State.INACTIVE.value
        bufPos >= BUF_SIZE -> bufRight[bufPos - BUF_SIZE].code
        else -> bufLeft[bufPos].code
    }

    private fun peekCommon(bufPos: Int): Int {
        // end of buffer. This implies bufPos < 2 * BUF_SIZE
        if (bufPos >= srcBufCount) return -1
        val c = rawPeek(bufPos)

        return when (c) {
            '\r'.code, 0x85, 0x2028 -> '\n'.code
            else -> c
        }
    }

    /**
     * This implementation matches, but also assumes that the expected sequence is not such large
     * as to not require range checks for the buffers (only end of file).
     */
    override fun peek(expected: CharSequence): Boolean {
        return peekCommon(srcBufPos, expected)
    }

    /**
     * This implementation matches, but also assumes that the expected sequence is not such large
     * as to not require range checks for the buffers (only end of file).
     */
    override fun peek(offset: Int, expected: CharSequence): Boolean {
        return peekCommon(srcBufPos + offset, expected)
    }

    private fun peekCommon(bufStart: Int, expected: CharSequence): Boolean {
        val l = expected.length
        if (bufStart + l > srcBufCount) return false // must be end of file
        when {
            bufStart + l <= BUF_SIZE -> { // most common
                val start = bufStart
                return (0..<l).all { i -> expected[i] == bufLeft[start + i] }
            }

            bufStart < BUF_SIZE -> { // overlap, a bit more likely than the last option
                val split = l - (BUF_SIZE - bufStart)
                return (0..<split).all { i -> expected[i] == bufLeft[bufStart + i] } &&
                        (split..<l).all { i -> expected[i] == bufRight[i - split] }
            }

            else -> { // strings only in the right buffer
                val start = bufStart - BUF_SIZE
                return (0..<l).all { i -> expected[i] == bufRight[start + i] }
            }
        }
    }

    override fun skip(count: Int) {
        val newPos = srcBufPos + count
        // allow skipping just past the last character.
        if (newPos > srcBufCount) error("End of file while skipping")
        srcBufPos = newPos
        if (newPos >= BUF_SIZE) swapInputBuffer()
    }

    override fun markPeekedAsRead() {

        val oldPos = srcBufPos
        val peeked = rawPeek (oldPos).toChar()
        when (peeked) {
            '\r' -> handle2CharLineEnd(oldPos)
            '\u0085', '\u2028' -> {
                bufLeft[oldPos] = '\n'
                handleLineEnd(oldPos + 1)
            }

            '\n' -> handleLineEnd(oldPos + 1)

            else -> srcBufPos = oldPos + 1
        }
        if (srcBufPos >= BUF_SIZE) swapInputBuffer()
    }

    private fun handleLineEnd(newPos: Int) {
        if (copySequenceState == State.ACTIVE && rawPeek(srcBufPos) != '\n'.code) {
            pauseCopySequence()
            addToCopySequence('\n')
            srcBufPos = newPos
            resumeCopySequence()
        } else {
            srcBufPos = newPos
        }
        lastColumnStart = offsetBase + newPos
        line += 1
    }

    private fun handle2CharLineEnd(oldPos: Int) {
        val nextChar = peek(1)
        val inc = when (nextChar) {
            0xA, 0x85 -> 2

            else -> {
                bufLeft[oldPos] = '\n'
                1
            }
        }
        handleLineEnd(oldPos + inc)
    }

    /** Does never read more than needed  */
    override fun read(): Int {
        // In this case we *may* need the right buffer, otherwise not
        // optimize this implementation for the "happy" path
        var oldPos = srcBufPos
        if (oldPos >= srcBufCount) return -1

        if (oldPos >= BUF_SIZE) {
            // swap if needed. Note that peek will work
            // correctly and we can get a little further for line endings.
            swapInputBuffer()
            oldPos = srcBufPos // need to update oldPos
        }

        when (val char = bufLeft[oldPos]) {
            '\r' -> { // as \r is always transformed to \n, this requires a stringBuilder.
                handle2CharLineEnd(oldPos)
                return '\n'.code
            }

            '\u0085',
            '\u2028' -> {
                bufLeft[srcBufPos] = '\n'
                handleLineEnd(oldPos + 1)
                return '\n'.code
            }

            '\n' -> {
                handleLineEnd(oldPos + 1)
                return '\n'.code
            }

            else -> {
                srcBufPos = oldPos + 1
                return char.code
            }
        }
    }

    private fun readUntilFullOrEOF(buffer: CharArray): Int {
        val bufSize = buffer.size
        var totalRead: Int = reader.read(buffer, 0, bufSize)
        if (totalRead < 0) return -1
        while (totalRead < bufSize) {
            val lastRead = reader.read(buffer, totalRead, bufSize - totalRead)
            if (lastRead < 0) return totalRead
            totalRead += lastRead
        }
        return totalRead
    }

    override fun toString(): String {
        return buildString {
            append("SwappedInputBuffer(")
            append("Next = '")
            if (srcBufPos <BUF_SIZE) appendRange(bufLeft, srcBufPos, (srcBufPos + 10).coerceAtMost(srcBufCount))
            else appendRange(bufRight, srcBufPos-BUF_SIZE, ((srcBufPos + 10).coerceAtMost(srcBufCount) - BUF_SIZE).coerceAtMost(BUF_SIZE))
            append("', output buffer = ")
            val b = copyBuilder



            if (copySequenceStart < 0 && b == null) {
                append("null)")
            } else {
                append('\'')
                if (b!=null) append(b)
                if (copySequenceStart>=0) appendRange(bufLeft, copySequenceStart, srcBufPos.coerceAtMost(BUF_SIZE))
                if (srcBufPos>=BUF_SIZE) appendRange(bufRight, 0, (srcBufPos - BUF_SIZE).coerceAtMost(BUF_SIZE))
                append("')")
            }
        }
    }

    private val delayedCopySequence = BufferView()

    /**
     * Class that presents a view of the copy sequence without actually creating a string, or even
     * a new instance. This avoids allocations until a reader actually reads the content. This may
     * not happen, for example with ignorable whitespace.
     */
    private inner class BufferView(var copiedStart: Int = -1, var copiedEnd: Int = -1) : CharSequence {
        override fun get(index: Int): Char {
            val b = copyBuilder
            var idx = index
            if (b != null) {
                if (index < b.length) return b[index]
                idx -= b.length
            }
            if (idx > srcBufCount) error("Index out of bounds")
            if (idx <BUF_SIZE) return bufLeft[idx]
            return bufRight[idx - BUF_SIZE]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            throw UnsupportedOperationException("Subsequences are not optimal for this implementation.")
        }

        override val length: Int
            get() {
                val b = copyBuilder
                return when {
                    b == null -> copiedEnd - copiedStart
                    copiedStart >= 0 -> copiedEnd - copiedStart + b.length
                    else -> b.length
                }
            }

        override fun toString(): String {
            val b = copyBuilder
            val start = copiedStart

            when {
                b == null && (start < 0) -> error("No copy sequence started")

                b == null -> return bufLeft.concatToString(start, copiedEnd)

                start >= 0 ->
                    return b.appendRange(bufLeft, start, copiedEnd).toString()

                else -> return b.toString()
            }

        }
    }


    private typealias State = InOutBuffer.State
}
