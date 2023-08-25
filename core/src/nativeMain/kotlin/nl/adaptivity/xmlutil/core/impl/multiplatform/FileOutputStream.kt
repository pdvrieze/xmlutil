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
        var remaining: size_t = (end - loopBegin).convert()
        buffer.usePinned { buf ->
            while (remaining > SIZE0) {
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

    public override fun write(buffer: ByteArray, begin: Int, end: Int): Unit {
        var loopBegin: Int = begin
        var remaining: size_t = end.convert<size_t>() - loopBegin.convert<size_t>()
        buffer.usePinned { buf ->
            while (remaining > SIZE0) {
                val bufStart = buf.addressOf(loopBegin)
                val written = writePtr(bufStart, remaining)
                loopBegin += written.toInt()
                remaining -= written
            }
        }
    }

    /** Write buffers to the underlying file (where valid). */
    public fun flush() {
        if (fflush(filePtr) != 0) {
            throw IOException.fromErrno()
        }
    }

    public override fun <T : CPointed> writePtr(buffer: CArrayPointer<T>, size: size_t, count: size_t): size_t {
        clearerr(filePtr)
        val elemsWritten: size_t = fwrite(buffer, size, count, filePtr)
        if (elemsWritten == SIZE0 && count != SIZE0) {
            val e = ferror(filePtr)
            throw IOException.fromErrno(e)
        }
        return elemsWritten
    }

    public override fun <T : CPointed> writeAllPtr(buffer: CArrayPointer<T>, size: size_t, count: size_t) {

        clearerr(filePtr)
        var elemsRemaining: size_t = count
        var currentBufferPointer = buffer
        while (elemsRemaining > SIZE0) {
            val elemsWritten : size_t = fwrite(currentBufferPointer, size, count, filePtr)
            if (elemsWritten == SIZE0) {
                val e = ferror(filePtr)
                throw IOException.fromErrno(e)
            }
            elemsRemaining -= elemsWritten

            currentBufferPointer = interpretCPointer((currentBufferPointer.rawValue + (elemsWritten * size).toLong()))!!
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

internal val SIZE0: size_t = 0u.convert<size_t>()
