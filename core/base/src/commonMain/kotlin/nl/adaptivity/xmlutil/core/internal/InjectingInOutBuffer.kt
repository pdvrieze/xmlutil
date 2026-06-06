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

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.InOutBuffer
import nl.adaptivity.xmlutil.isXmlWhitespace

internal class InjectingInOutBuffer(val base: InOutBuffer): InOutBuffer {

    val isInjecting: Boolean
        get() = stack.isNotEmpty()

    private val stack = mutableListOf<Elem>()

    override val offset: Int
        get() = stack.lastOrNull()?.pos ?: base.offset

    override val line: Int
        get() = if (stack.isEmpty()) base.line else 1

    override val column: Int
        get() = stack.lastOrNull ()?.run { pos + 1 } ?: base.column

    override val copySequenceState: InOutBuffer.State
        get() = base.copySequenceState

    override fun startCopySequence() {
        base.startCopySequence()
    }

    override fun flushCopySequence() {
        base.flushCopySequence()
    }

    override fun pauseCopySequence() {
        base.pauseCopySequence()
    }

    override fun resumeCopySequence() {
        base.resumeCopySequence()
    }

    override fun finalizeCopySequence(): CharSequence {
        return base.finalizeCopySequence()
    }


    override fun addToCopySequence(seq: CharSequence) {
        base.addToCopySequence(seq)
    }


    override fun addToCopySequence(char: Char) {
        base.addToCopySequence(char)
    }


    override fun addDelimitedToCopySequence(
        delimiter: String,
        pauseOnDelimiter: Boolean,
        consumeDelimiter: Boolean
    ) {
        when {
            stack.isEmpty() -> base.addDelimitedToCopySequence(delimiter, pauseOnDelimiter, consumeDelimiter)
            else -> super.addDelimitedToCopySequence(delimiter, pauseOnDelimiter, consumeDelimiter)
        }
    }

    override fun addDelimitedToCopySequence(
        delimiter: Char,
        pauseOnDelimiter: Boolean,
        consumeDelimiter: Boolean
    ) {
        when {
            stack.isEmpty() -> base.addDelimitedToCopySequence(delimiter, pauseOnDelimiter, consumeDelimiter)
            else -> super.addDelimitedToCopySequence(delimiter, pauseOnDelimiter, consumeDelimiter)
        }
    }

    override fun skip(count: Int) {
        for (i in 0 until count) check(read()>=0) { "Unexpected end of stream" }
    }

    override fun markPeekedAsRead() {
        val _ = readChar()
    }

    override fun readToCopyBuffer() {
        val _ = readChar()
    }

    override fun readSubRange(start: Int, end: Int): CharSequence {
        return stack.lastOrNull()?.source?.subSequence(start, end) ?: base.readSubRange(start, end)
    }

    override fun peek(offset: Int): Int {
        val lastStack = stack.lastOrNull() ?: return base.peek(offset)
        if (lastStack.pos + offset < lastStack.source.length) {
            return lastStack.source[lastStack.pos + offset].code
        }

        var stackPos = stack.size - 2
        var remainingOffset = offset - (lastStack.source.length - lastStack.pos)

        while (stackPos >= 0 && remainingOffset > (stack[stackPos].let { s -> s.source.length - s.pos })) {
            val s = stack[stackPos]
            remainingOffset -= (s.source.length - s.pos)
            stackPos -= 1
        }
        if (stackPos >= 0) {
            val s = stack[stackPos]
            return s.source[s.pos + remainingOffset].code
        }

        return base.peek(remainingOffset)
    }

    /**
     * Specialisation of skipWS that handles injected stacks better.
     */
    override fun skipWS() {
        val lastStack = stack.lastOrNull() ?: return base.skipWS()
        var i = lastStack.pos
        val l = lastStack.source.length
        while (i < l) {
            val c = lastStack.source[i]
            if (!isXmlWhitespace(c)) {
                lastStack.pos = i
                break
            }
            i += 1
        }
        if (i >= l) { // if we read the end of the stack, just recurse.
            stack.removeLast()
            skipWS() // recurse more whitespace
        }
    }

    override fun read(): Int {
        val lastStack = stack.lastOrNull() ?: return base.read()
        val pos = lastStack.pos
        val r = lastStack.source[pos].code
        if (copySequenceState == State.ACTIVE && r>=0) {
            base.addToCopySequence(r.toChar())
        }

        val newPos = pos + 1
        when {
            newPos < lastStack.source.length -> lastStack.pos = newPos
            else -> stack.removeLast()
        }

        return r
    }

    fun inject(name: String, text: CharSequence, entityLocation: XmlReader.LocationInfo?) {
        if (base.copySequenceState == State.ACTIVE) {
            // this ensures any pending is written to the buffer
            base.pauseCopySequence()
            base.resumeCopySequence()
        }
        stack.add(Elem(0, name, text, entityLocation))
    }

    override fun toString(): String {
        return buildString {
            append("InjectingInputBuffer(")
            for (s in stack.reversed()) {
                append('&').append(s.entityName).append("; - [")
                appendRange(s.source, s.pos, s.source.length)
                append("], ")
            }
            append("base = $base")
            append(')')
        }
    }

    private typealias State = InOutBuffer.State

    private class Elem(var pos: Int, val entityName: String, val source: CharSequence, val entityLocationInfo: XmlReader.LocationInfo?)
}
