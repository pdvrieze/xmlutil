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

package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Implementation of a FileInputStream based upon native file access.
 * @property filePtr A pointer to the underlying file.
 *
 * @constructor Directly wrap (and take ownership of) the file pointer given.
 */
@OptIn(ExperimentalForeignApi::class)
public class FileInputStream(public val filePtr: FilePtr) : InputStream() {

    /**
     * Create an input stream for a file handle. Will create the needed file pointer.
     * @param fileHandle The file handle to use
     * @param mode The mode to use to open the file.
     */
    public constructor(fileHandle: Int, mode: FileMode = Mode.READ) : this(
        FilePtr(fdopen(fileHandle, mode.modeString) ?: throw IOException.fromErrno()))

    /**
     * Create an input stream for a file name. Will create the needed file pointer.
     * @param pathName The name of the file to open. If relative depends on the current working directory.
     * @param mode The mode to use to open the file.
     */
    public constructor(pathName: String, mode: FileMode = Mode.READ) : this(
        FilePtr(fopen(pathName, mode.modeString) ?: throw IOException.fromErrno()))

    /**
     * Close the file (neither this object is valid afterwards, nor the pointer.
     */
    override fun close() {
        if (fclose(filePtr.value) != 0) {
            throw IOException.fromErrno()
        }
    }

    /**
     * Determine whether the end of the file has been reached.
     */
    public override val eof: Boolean
        get() = feof(filePtr.value) != 0

    /**
     * Read into the given native buffer. It will check for errors, but does not indicate end of file.
     *
     * @param buffer The buffer to read into
     * @param size The size of individual items (in bytes)
     * @param bufferSize The maximum amount of items to be read.
     */
    @OptIn(UnsafeNumber::class)
    public override fun <T : CPointed> read(buffer: CArrayPointer<T>, size: SizeT, bufferSize: SizeT): SizeT {
        clearerr(filePtr.value)
        val itemsRead = SizeT(
            fread(buffer, size.value.convert<size_t>(), bufferSize.value.convert<size_t>(), filePtr.value))
        if (itemsRead.toULong() == 0uL) {
            val error = ferror(filePtr.value)
            if (error != 0) {
                throw IOException.fromErrno(error)
            }
        }
        return itemsRead
    }

    /**
     * Read a single byte value. This is not buffered in any way, and possibly slow.
     * @return -1 if end of file, otherwise the byte value
     */
    @OptIn(UnsafeNumber::class)
    public override fun read(): Int {
        clearerr(filePtr.value)
        memScoped {
            val bytePtr = alloc<UByteVar>()
            val itemsRead: ULong = fread(bytePtr.ptr, 1u.convert(), 1u.convert(), filePtr.value).convert()
            if (itemsRead == 0uL) {
                val error = ferror(filePtr.value)
                if (error != 0) {
                    throw IOException.fromErrno(error)
                } else if (feof(filePtr.value) != 0) {
                    return -1
                }
            }
            return bytePtr.value.toInt()
        }
    }

    /**
     * Read an array of bytes from the file.
     * @param buffer The buffer to read into
     * @param offset The starting point in the buffer to start reading
     * @param len The amount of data to read.
     * @return The amount of bytes read or -1 if end of file.
     */
    @OptIn(UnsafeNumber::class)
    override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        val endIdx = offset + len
        require(offset in buffer.indices) { "Offset before start of array" }
        require(endIdx <= buffer.size) { "Range size beyond buffer size" }
        val result = buffer.usePinned { buf ->
            read(buf.addressOf(offset), sizeT(sizeOf<ByteVar>()), sizeT(len)).toInt()
        }
        if (result == 0 && eof) return -1
        return result
    }

    /**
     * Read an array of bytes from the file.
     * @param buffer The buffer to read into
     * @param offset The starting point in the buffer to start reading
     * @param len The amount of data to read.
     * @return The amount of bytes read or -1 if end of file.
     */
    public fun read(buffer: UByteArray, offset: Int = 0, len: Int = buffer.size - offset): Int {
        val result = buffer.usePinned { buf ->
            read(buf.addressOf(offset), sizeT(1), sizeT(len)).toInt()
        }
        if (result == 0 && eof) return -1
        return result
    }

    /**
     * Supported read modes.
     */
    public enum class Mode(public override val modeString: String) : FileMode {
        /**
         * Support reading only
         */
        READ("r"),

        /**
         * By default read, but also support writing (this requires unsafe access to the file pointer).
         */
        READWRITE("r+");
    }

}

public interface FileMode {
    public val modeString: String
}
