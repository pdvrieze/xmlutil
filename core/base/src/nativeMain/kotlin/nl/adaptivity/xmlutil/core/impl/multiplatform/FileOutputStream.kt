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

@file:OptIn(ExperimentalForeignApi::class)
package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlinx.cinterop.*
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import platform.posix.*

@ExperimentalXmlUtilApi
public class FileOutputStream(public val filePtr: FilePtr) : NativeOutputStream() {

    public constructor(pathName: String, mode: FileMode = Mode.TRUNCATED) : this(
        FilePtr(fopen(pathName, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        }))

    public constructor(fileHandle: Int, mode: FileMode = Mode.TRUNCATED) : this(
        FilePtr(fdopen(fileHandle, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        }))

    @OptIn(ExperimentalForeignApi::class)
    public fun write(buffer: UByteArray, begin: Int = 0, end: Int = buffer.size - begin) {
        var loopBegin: Int = begin
        var remaining = sizeT((end - loopBegin).toULong())
        buffer.usePinned { buf ->
            while (remaining.toULong() > 0uL) {
                val bufferPtr = buf.addressOf(loopBegin)
                val written = writePtr(bufferPtr, remaining)
                loopBegin += written.toInt()
                remaining -= written
            }
        }
    }

    override fun write(b: Int) {
        write(byteArrayOf(b.toByte()))
    }

    public override fun write(b: ByteArray, off: Int, len: Int) {
        var loopBegin: Int = off
        var remaining = sizeT((len - loopBegin).toULong())
        b.usePinned { buf ->
            while (remaining.toULong() > 0uL) {
                val bufStart = buf.addressOf(loopBegin)
                val written = writePtr(bufStart, remaining)
                loopBegin += written.toInt()
                remaining -= written
            }
        }
    }

    /** Write buffers to the underlying file (where valid). */
    @OptIn(ExperimentalForeignApi::class)
    public fun flush() {
        if (fflush(filePtr.value) != 0) {
            throw IOException.fromErrno()
        }
    }

    @OptIn(UnsafeNumber::class)
    public override fun <T : CPointed> writePtr(buffer: CArrayPointer<T>, size: SizeT, count: SizeT): SizeT {
        clearerr(filePtr.value)
        val elemsWritten = SizeT(fwrite(buffer, size.value.convert(), count.value.convert(), filePtr.value))
        if (elemsWritten.toULong() == 0uL && count.toULong() != 0uL) {
            val e = ferror(filePtr.value)
            throw IOException.fromErrno(e)
        }
        return elemsWritten
    }

    @OptIn(UnsafeNumber::class)
    public override fun <T : CPointed> writeAllPtr(buffer: CArrayPointer<T>, size: SizeT, count: SizeT) {

        clearerr(filePtr.value)
        var elemsRemaining: ULong = count.toULong()
        var currentBufferPointer = buffer
        while (elemsRemaining > 0u) {
            val elemsWritten: ULong = fwrite(currentBufferPointer, size.value.convert(), count.value.convert(), filePtr.value).convert()
            if (elemsWritten == 0uL) {
                val e = ferror(filePtr.value)
                throw IOException.fromErrno(e)
            }
            elemsRemaining -= elemsWritten

            currentBufferPointer =
                interpretCPointer((currentBufferPointer.rawValue + (elemsWritten * size.value).toLong()))!!
        }
    }

    override fun close() {
        if (fclose(filePtr.value) != 0) {
            throw IOException.fromErrno()
        }
    }

    public enum class Mode(public override val modeString: String) : FileMode {
        TRUNCATED("w"),
        APPEND("a");
    }
}

