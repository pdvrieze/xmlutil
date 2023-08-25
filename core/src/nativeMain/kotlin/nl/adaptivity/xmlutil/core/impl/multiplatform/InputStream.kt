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

import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.size_t

public actual abstract class InputStream : Closeable {
    public abstract val eof: Boolean

    public actual open fun read(b: ByteArray, off: Int, len: Int): Int {
        require(off in 0 until b.size) { "Offset before start of array" }
        require(off + len <= b.size) { "Range size beyond buffer size" }

        for (i in off until (off + len)) {
            val byte = read()
            if (byte < 0) {
                return i
            }
            b[i] = byte.toByte()
        }
        return len
    }

    public actual fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    public actual abstract fun read(): Int

    @ExperimentalForeignApi
    public abstract fun <T : CPointed> read(buffer: CArrayPointer<T>, size: size_t, bufferSize: size_t): size_t
}
