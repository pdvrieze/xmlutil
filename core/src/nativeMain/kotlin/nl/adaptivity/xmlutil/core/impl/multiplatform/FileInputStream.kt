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

@OptIn(ExperimentalForeignApi::class)
public class FileInputStream(public val filePtr: CPointer<FILE>) : InputStream() {

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

    public override val eof: Boolean
        get() = feof(filePtr) != 0

    public override fun <T : CPointed> read(buffer: CArrayPointer<T>, size: MPSizeT, bufferSize: MPSizeT): MPSizeT {
        clearerr(filePtr)
        val itemsRead = MPSizeT(fread(buffer, size.sizeT, bufferSize.sizeT, filePtr).convert<uint64_t>())
        if (itemsRead.value == 0uL) {
            val error = ferror(filePtr)
            if (error != 0) {
                throw IOException.fromErrno(error)
            }
        }
        return itemsRead
    }

    public override fun read(): Int {
        clearerr(filePtr)
        memScoped {
            val bytePtr = alloc<UByteVar>()
            val itemsRead: size_t = fread(bytePtr.ptr, 1u, 1u, filePtr)
            if (itemsRead == SIZE0) {
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

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val endIdx = off + len
        require(off in b.indices) { "Offset before start of array" }
        require(endIdx <= b.size) { "Range size beyond buffer size" }
        return b.usePinned { buf ->
            read(buf.addressOf(off), MPSizeT(sizeOf<ByteVar>().convert()), MPSizeT(len.convert())).value.convert()
        }
    }

    public fun read(buffer: UByteArray, offset: Int = 0, len: Int = buffer.size - offset): Int {
        buffer.usePinned { buf ->
            return read(buf.addressOf(offset), MPSizeT(1u), MPSizeT(len.toULong())).value.toInt()
        }
    }

    public enum class Mode(public override val modeString: String) : FileMode {
        READ("r"),
        READWRITE("r+");
    }

}

public interface FileMode {
    public val modeString: String
}
