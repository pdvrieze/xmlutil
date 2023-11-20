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

@file:OptIn(ExperimentalForeignApi::class)
package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlinx.cinterop.*
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import platform.posix.*

@ExperimentalXmlUtilApi
@OptIn(UnsafeNumber::class)
public class FileOutputStream(public val filePtr: CPointer<FILE>) : OutputStream() {

    public constructor(pathName: String, mode: FileMode = Mode.TRUNCATED) : this(
        fopen(pathName, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        })

    public constructor(fileHandle: Int, mode: FileMode = Mode.TRUNCATED) : this(
        fdopen(fileHandle, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        })

    public fun write(buffer: UByteArray, begin: Int = 0, end: Int = buffer.size - begin): Unit {
        var loopBegin: Int = begin
        var remaining = MPSizeT((end - loopBegin).toULong())
        buffer.usePinned { buf ->
            while (remaining.value > 0uL) {
                val bufferPtr = buf.addressOf(loopBegin)
                val written = writePtr(bufferPtr, remaining)
                loopBegin += written.value.toInt()
                remaining -= written
            }
        }
    }

    override fun write(b: Int) {
        write(byteArrayOf(b.toByte()))
    }

    public override fun write(buffer: ByteArray, begin: Int, end: Int): Unit {
        var loopBegin: Int = begin
        var remaining = MPSizeT((end - loopBegin).toULong())
        buffer.usePinned { buf ->
            while (remaining.value > 0uL) {
                val bufStart = buf.addressOf(loopBegin)
                val written = writePtr(bufStart, remaining)
                loopBegin += written.value.toInt()
                remaining -= written
            }
        }
    }

    /** Write buffers to the underlying file (where valid). */
    @OptIn(ExperimentalForeignApi::class)
    public fun flush() {
        if (fflush(filePtr) != 0) {
            throw IOException.fromErrno()
        }
    }

    public override fun <T : CPointed> writePtr(buffer: CArrayPointer<T>, size: MPSizeT, count: MPSizeT): MPSizeT {
        clearerr(filePtr)
        val elemsWritten = MPSizeT(fwrite(buffer, size.value.convert(), count.value.convert(), filePtr))
        if (elemsWritten.value == 0uL && count.value != 0uL) {
            val e = ferror(filePtr)
            throw IOException.fromErrno(e)
        }
        return elemsWritten
    }

    public override fun <T : CPointed> writeAllPtr(buffer: CArrayPointer<T>, size: MPSizeT, count: MPSizeT) {

        clearerr(filePtr)
        var elemsRemaining: ULong = count.value
        var currentBufferPointer = buffer
        while (elemsRemaining > 0u) {
            val elemsWritten: ULong = fwrite(currentBufferPointer, size.value.convert(), count.value.convert(), filePtr).convert()
            if (elemsWritten == 0uL) {
                val e = ferror(filePtr)
                throw IOException.fromErrno(e)
            }
            elemsRemaining -= elemsWritten

            currentBufferPointer =
                interpretCPointer((currentBufferPointer.rawValue + (elemsWritten * size.value).toLong()))!!
        }
    }

    override fun close() {
        if (fclose(filePtr) != 0) {
            throw IOException.fromErrno()
        }
    }

    public enum class Mode(public override val modeString: String) : FileMode {
        TRUNCATED("w"),
        APPEND("a");
    }
}

