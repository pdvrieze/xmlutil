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

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"
)
public actual abstract class InputStream : Closeable {
    @Suppress("NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING")
    public abstract val eof: Boolean

    public actual open fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        require(offset in buffer.indices) { "Offset before start of array" }
        require(offset + len <= buffer.size) { "Range size beyond buffer size" }

        for (i in offset until (offset + len)) {
            val byte = read()
            if (byte < 0) {
                return i
            }
            buffer[i] = byte.toByte()
        }
        return len
    }

    public actual fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    public actual abstract fun read(): Int

    @ExperimentalForeignApi
    public abstract fun <T : CPointed> read(buffer: CArrayPointer<T>, size: MPSizeT, bufferSize: MPSizeT): MPSizeT
    actual override fun close() {}
}

/**
 * Wrapper type to stand in for `size_t` as that type has inconsistent sizes in different architectures.
 */
public value class MPSizeT(public val value: ULong) {
    public constructor(value: UInt): this(value.toULong())
    public operator fun minus(other: MPSizeT): MPSizeT = MPSizeT(value - other.value)
    public operator fun plus(other: MPSizeT): MPSizeT = MPSizeT(value + other.value)
}
