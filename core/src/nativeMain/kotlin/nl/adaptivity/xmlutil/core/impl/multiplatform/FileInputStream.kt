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
import platform.posix.*

@OptIn(ExperimentalUnsignedTypes::class)
public class FileInputStream(public val filePtr: CPointer<FILE>) : Closeable {

    public constructor(fileHandle: Int, mode: FileMode = Mode.READ) : this(
        fdopen(fileHandle, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        })

    public constructor(pathName: String, mode: FileMode = Mode.READ) : this(
        fopen(pathName, mode.modeString) ?: kotlin.run {
            throw IOException.fromErrno()
        })

    override fun close() {
        if (fclose(filePtr) != 0) {
            throw IOException.fromErrno()
        }
    }

    public val eof: Boolean
        get() = feof(filePtr) != 0

    public fun <T : CPointed> read(buffer: CArrayPointer<T>, size: size_t, bufferSize: size_t): size_t {
        clearerr(filePtr)
        val itemsRead = fread(buffer, size, bufferSize, filePtr)
        if (itemsRead == 0UL) {
            val error = ferror(filePtr)
            if (error != 0) {
                throw IOException.fromErrno(error)
            }
        }
        return itemsRead
    }

    public fun <T : CPointed> read(): Int {
        clearerr(filePtr)
        memScoped {
            val bytePtr = alloc<UByteVar>()
            val itemsRead = fread(bytePtr.ptr, 1, 1, filePtr)
            if (itemsRead == 0UL) {
                val error = ferror(filePtr)
                if (error != 0) {
                    throw IOException.fromErrno(error)
                } else if (feof(filePtr) != 0) {
                    return -1
                }
            }
            return bytePtr.value.toInt()
        }
    }

    public fun read(buffer: UByteArray, offset: Int = 0, len: Int = buffer.size - offset): Int {
        buffer.usePinned { buf ->
            return read(buf.addressOf(offset),1UL, len.toULong()).toInt()
        }
    }

    public enum class Mode(public override val modeString: String): FileMode {
        READ("r"),
        READWRITE("r+");
    }

}

public interface FileMode {
    public val modeString: String
}
