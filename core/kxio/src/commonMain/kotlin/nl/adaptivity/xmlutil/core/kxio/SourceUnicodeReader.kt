/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.core.kxio

import kotlinx.io.Source
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.impl.multiplatform.IOException
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader

/**
 * An implementation of a reader that reads UTF data from a kotlinx.io.Source
 */
@XmlUtilInternal
internal class SourceUnicodeReader(val source: Source) : Reader() {
    private val inputBuffer = ByteArray(INPUT_BYTE_BUFFER_SIZE)
    private var inputBufferOffset = 0
    private var inputBufferEnd = 0
    private var pendingLowSurrogate: Char = '\u0000'

    private fun reloadBuffer() {
        if (! source.exhausted()) {
            if (inputBufferOffset < inputBufferEnd) {
                inputBuffer.copyInto(inputBuffer, 0, inputBufferOffset, inputBufferEnd)
                inputBufferEnd -= inputBufferOffset
                inputBufferOffset = 0
            }
            val readCount = source.readAtMostTo(inputBuffer, inputBufferOffset, (inputBuffer.size - inputBufferOffset))
            if (readCount < 0) {
                return
            }
            inputBufferEnd = inputBufferOffset + readCount
        }
    }

    private fun nextByte(): Int = peekByte().also { if (it >= 0) inputBufferOffset++ }

    private fun peekByte(): Int {
        try {
            if (inputBufferOffset == inputBufferEnd) reloadBuffer()
            if (inputBufferOffset == inputBufferEnd) return -1
            return inputBuffer[inputBufferOffset].toInt()
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Unexpected indexing error: offset: $inputBufferOffset, bufferSize: ${inputBuffer.size}", e)
        }
    }

    private fun continuationByte(): UInt {
        val bOrError = nextByte()
        if (bOrError < 0) throw IOException("End of file while reading continuation byte in utf 8")
        val b = bOrError.toUInt()
        if (b and 0xC0u != 0x80u) throw IOException("Expected continuation byte but did not find it")
        return b and 0x3fu
    }

    public fun readLine(): String? {
        val builder = StringBuilder()
        while (true) {
            val code = nextByte()
            when {
                code < 0 -> return when {
                    builder.isEmpty() -> null
                    else -> builder.toString()
                }

                code == 0x0D -> {
                    if (peekByte() == 0x0A) inputBufferOffset++ // Skip multi line endings
                    return builder.toString().also { builder.clear() }
                }

                code == 0x0A -> {
                    if (peekByte() == 0x0D) inputBufferOffset++ // Skip multi line endings
                    return builder.toString().also { builder.clear() }
                }

                code < 0x80 -> builder.append(Char(code))

                else -> {
                    val codePoint = readMultiByteFrom(code)
                    val pt = codePoint - 0x10000u
                    val highSurrogate = Char(pt.shr(10).toUShort() or 0xD800u)
                    val lowSurrogate = Char((pt and 0x3ffu).toUShort() or 0xDC00u)
                    builder.append(highSurrogate).append(lowSurrogate)
                }
            }
        }
    }

    public fun lines(): Sequence<String> {
        return generateSequence { readLine() }
    }

    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        var outPos = offset
        val endPos = minOf(buf.size, offset + len)
        if (pendingLowSurrogate != '\u0000' && outPos < endPos) {
            buf[outPos++] = pendingLowSurrogate
            pendingLowSurrogate = '\u0000'
        }
        if (peekByte() < 0) return -1
        while (outPos < endPos) {
            val code = nextByte()
            if (code < 0) break

            if (code and 0x80 != 0) { // It is an UTF 8 number
                val codePoint: UInt = readMultiByteFrom(code)

                val pt = codePoint - 0x10000u
                val highSurrogate = Char(pt.shr(10).toUShort() or 0xD800u)
                val lowSurrogate = Char((pt and 0x3ffu).toUShort() or 0xDC00u)
                buf[outPos++] = highSurrogate
                if (outPos == endPos) {
                    pendingLowSurrogate = lowSurrogate
                } else {
                    buf[outPos++] = lowSurrogate
                }

            } else {
                buf[outPos++] = Char(code)
            }
        }
        return outPos - offset
    }

    private fun readMultiByteFrom(code: Int): UInt {
        val codePoint: UInt
        when {
            code and 0xE0 == 0xD0 -> { // 2 bytes
                codePoint = ((code and 0x1f) shl 6).toUInt() or continuationByte()
                if (codePoint < 0x80u) {
                    throw IOException("Overlong UTF8 encoding for ASCII character")
                }
            }

            code and 0xF0 == 0xE0 -> { // 3 bytes
                codePoint = ((code and 0x0f).toUInt() shl 12) or
                        (continuationByte() shl 6) or
                        continuationByte()
                if (codePoint < 0x800u) {
                    throw IOException("Overlong UTF8 encoding for ASCII character")
                }
            }

            code and 0xf8 == 0xf0 -> { // 4 bytes
                codePoint = ((code and 0x07).toUInt() shl 18) or
                        (continuationByte() shl 12) or
                        (continuationByte() shl 6) or
                        continuationByte()
                if (codePoint < 0x10000u) {
                    throw IOException("Overlong UTF8 encoding for ASCII character")
                }
            }

            else -> throw IOException("Invalid UTF8 sequence")
        }

        if (codePoint in 0xd800u..0xdfffu ||
            codePoint > 0x10ffffu
        ) throw IOException("Invalid codepoint ${codePoint.toString(16)}")
        return codePoint
    }


    override fun close() {
        source.close()
    }

    private companion object {
        private const val INPUT_BYTE_BUFFER_SIZE = 0x2000
    }
}
