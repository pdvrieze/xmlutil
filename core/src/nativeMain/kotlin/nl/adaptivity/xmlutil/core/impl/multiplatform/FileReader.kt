/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlinx.cinterop.CPointer
import nl.adaptivity.xmlutil.core.impl.multiplatform.FileInputStream.Mode
import platform.posix.FILE
import platform.posix.fdopen
import platform.posix.fopen

@OptIn(ExperimentalUnsignedTypes::class)
public class FileReader(public val inputStream: FileInputStream) : Reader() {
    private val inputBuffer = UByteArray(INPUT_BYTE_BUFFER_SIZE)
    private var inputBufferOffset = 0
    private var inputBufferEnd = 0
    private var pendingLowSurrogate: Char = '\u0000'

    public constructor(filePtr: CPointer<FILE>) : this(FileInputStream(filePtr))

    public constructor(pathName: String, mode: FileMode = Mode.READ) :
            this(FileInputStream(pathName, mode))

    public constructor(fileHandle: Int, mode: FileMode = Mode.READ) :
            this(FileInputStream(fileHandle, mode))

    private fun reloadBuffer() {
        if (!inputStream.eof) {
            if (inputBufferOffset < inputBufferEnd) {
                inputBuffer.copyInto(inputBuffer, 0, inputBufferOffset, inputBufferEnd)
                inputBufferOffset = inputBufferEnd - inputBufferOffset
            }
            inputBufferEnd = inputStream.read(inputBuffer, inputBufferOffset) + inputBufferOffset
        }
    }

    private fun nextByte(): Int = peekByte().also { if (it >= 0) inputBufferOffset++ }

    private fun peekByte(): Int {
        if (inputBufferOffset == inputBufferEnd) reloadBuffer()
        if (inputBufferOffset == inputBufferEnd) return -1
        return inputBuffer[inputBufferOffset].toInt()
    }

    private fun continuationByte(): Int {
        val b = nextByte()
        if (b and 0xC0 != 0x80) throw IOException("Expected continuation byte but did not find it")
        return b and 0x3f
    }

    public fun readLine(): String? {
        val builder = StringBuilder()
        while (true) {
            val code = nextByte()
            when {
                code < 0 -> if (builder.isEmpty()) return null else return builder.toString()
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
                    val pt = codePoint - 0x10000
                    val highSurrogate = Char(pt.shr(10) or 0xD800)
                    val lowSurrogate = Char((pt and 0x3ff) or 0xDC00)
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
        while (outPos < endPos) {
            val code = nextByte()
            if (code < 0) return outPos - offset

            if (code and 0x80 != 0) { // It is an UTF 8 number
                val codePoint: Int = readMultiByteFrom(code)

                val pt = codePoint - 0x10000
                val highSurrogate = Char(pt.shr(10) or 0xD800)
                val lowSurrogate = Char((pt and 0x3ff) or 0xDC00)
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

    private fun readMultiByteFrom(code: Int): Int {
        val codePoint: Int
        when {
            code and 0xE0 == 0xD0 -> { // 2 bytes
                codePoint = ((code and 0x1f) shl 6) or continuationByte()
                if (codePoint < 0x80) {
                    throw IOException("Overlong UTF8 encoding for ASCII character")
                }
            }

            code and 0xF0 == 0xE0 -> { // 3 bytes
                codePoint = ((code and 0x0f) shl 12) or
                        (continuationByte() shl 6) or
                        continuationByte()
                if (codePoint < 0x800) {
                    throw IOException("Overlong UTF8 encoding for ASCII character")
                }
            }

            code and 0xf8 == 0xf0 -> { // 4 bytes
                codePoint = ((code and 0x07) shl 18) or
                        (continuationByte() shl 12) or
                        (continuationByte() shl 6) or
                        continuationByte()
                if (codePoint < 0x10000) {
                    throw IOException("Overlong UTF8 encoding for ASCII character")
                }
            }

            else -> throw IOException("Invalid UTF8 sequence")
        }

        if (codePoint in 0xd800..0xdfff ||
            codePoint > 0x10ffff
        ) throw IOException("Invalid codepoint ${codePoint.toString(16)}")
        return codePoint
    }

}

private const val INPUT_BYTE_BUFFER_SIZE = 0x2000
