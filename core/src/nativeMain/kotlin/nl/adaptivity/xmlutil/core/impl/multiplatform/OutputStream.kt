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
import platform.posix.uint64_t

public actual abstract class OutputStream : Closeable {
    /**
     * Write the buffer with the given amount of elements. It gets the element size from the type parameter.
     */
    @ExperimentalForeignApi
    public inline fun <reified T : CVariable> writePtr(buffer: CArrayPointer<T>, count: MPSizeT): MPSizeT {
        return writePtr(buffer, MPSizeT(sizeOf<T>().convert<uint64_t>()), count)
    }

    /**
     * Write the buffer to the underlying stream. Effectively wrapping fwrite.
     */
    @ExperimentalForeignApi
    public abstract fun <T : CPointed> writePtr(buffer: CArrayPointer<T>, size: MPSizeT, count: MPSizeT): MPSizeT

    @ExperimentalForeignApi
    public inline fun <reified T : CVariable> writeAllPtr(buffer: CArrayPointer<T>, count: Int) {
        writeAllPtr(buffer, MPSizeT(sizeOf<T>().convert<uint64_t>()), MPSizeT(count.toULong()))
    }


    @ExperimentalForeignApi
    public abstract fun <T : CPointed> writeAllPtr(buffer: CArrayPointer<T>, size: MPSizeT, count: MPSizeT)

    public actual abstract fun write(b: Int)

    public actual open fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    @OptIn(ExperimentalForeignApi::class)
    public actual open fun write(b: ByteArray, off: Int, len: Int) {
        val endIdx = off + len
        require(off in 0 until b.size) { "Offset before start of array" }
        require(endIdx <= b.size) { "Range size beyond buffer size" }

        b.usePinned { writePtr(it.addressOf(off), MPSizeT(sizeOf<ByteVar>().convert<uint64_t>()), MPSizeT(len.toULong())) }
    }
}
