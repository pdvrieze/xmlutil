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

import kotlinx.cinterop.*
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import platform.posix.*

@OptIn(ExperimentalUnsignedTypes::class)
@ExperimentalXmlUtilApi
public class FileOutputStream(public val filePtr: CPointer<FILE>) : Closeable {

    public constructor(pathName: String, mode: Mode = Mode.TRUNCATED) : this(
        fopen(pathName, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        })

    public constructor(fileHandle: Int, mode: Mode = Mode.TRUNCATED) : this(
        fdopen(fileHandle, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        })

    public fun write(buffer: UByteArray, begin: Int = 0, end: Int = buffer.size - begin): Unit {
        var loopBegin: Int = begin
        var remaining: size_t = (end - loopBegin).toULong()
        buffer.usePinned {buf ->
            while (remaining > 0UL) {
                val bufferPtr = buf.addressOf(loopBegin)
                val written = writePtr(bufferPtr, remaining)
                loopBegin += written.toInt()
                remaining -= written
            }
        }
    }

    public fun write(buffer: ByteArray, begin: Int = 0, end: Int = buffer.size - begin): Unit {
        var loopBegin: Int = begin
        var remaining: size_t = end.toULong() - loopBegin.toULong()
        buffer.usePinned { buf ->
            while (remaining > 0UL) {
                val bufStart = buf.addressOf(loopBegin)
                val written = writePtr(bufStart, remaining)
                loopBegin += written.toInt()
                remaining -= written
            }
        }
    }

    public inline fun <reified T : CVariable> writePtr(buffer: CArrayPointer<T>, count: size_t): size_t {
        return writePtr(buffer, (sizeOf<T>().toULong()), count)
    }

    public inline fun <reified T : CVariable> writePtr(buffer: CArrayPointer<T>, count: Int): size_t {
        return writePtr(buffer, sizeOf<T>().toULong(), count.toULong())
    }

    public fun <T : CPointed> writePtr(buffer: CArrayPointer<T>, size: size_t, count: size_t): size_t {
        clearerr(filePtr)
        val elemsWritten = fwrite(buffer, size, count, filePtr)
        if (elemsWritten == 0UL && count != 0UL) {
            val e = ferror(filePtr)
            throw IOException.fromErrno(e)
        }
        return elemsWritten
    }

    public inline fun <reified T : CVariable> writeAllPtr(buffer: CArrayPointer<T>, count: Int) {
        writeAllPtr(buffer, sizeOf<T>().toULong(), count.toULong())
    }


    public fun <T : CPointed> writeAllPtr(buffer: CArrayPointer<T>, size: size_t, count: size_t) {

        clearerr(filePtr)
        var elemsRemaining = count
        var currentBufferPointer = buffer
        while (elemsRemaining>0u) {
            val elemsWritten = fwrite(currentBufferPointer, size, count, filePtr)
            if (elemsWritten == 0UL) {
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

    public enum class Mode(public val modeString: String) {
        TRUNCATED("w"),
        APPEND("a");
    }
}
