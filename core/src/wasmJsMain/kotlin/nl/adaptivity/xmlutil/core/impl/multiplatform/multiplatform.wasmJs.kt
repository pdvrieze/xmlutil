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

import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR
)
public actual annotation class Throws actual constructor(actual vararg val exceptionClasses: KClass<out Throwable>)

@XmlUtilInternal
public actual val KClass<*>.name: String
    get() = throw UnsupportedOperationException("Reflection is not supported in wasmJs")

@XmlUtilInternal
public actual fun assert(value: Boolean, lazyMessage: () -> String) {
    if(!value) { throw AssertionError(lazyMessage()) }
}

@XmlUtilInternal
public actual fun assert(value: Boolean) {
    if (!value) throw AssertionError()
}

public actual abstract class OutputStream : Closeable {
    public actual abstract fun write(b: Int)

    public actual open fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    public actual open fun write(b: ByteArray, off: Int, len: Int) {
        val endIdx = off + len
        require(off in 0 until b.size) { "Offset before start of array" }
        require(endIdx <= b.size) { "Range size beyond buffer size" }
        for (idx in off until endIdx) {
            write(b[idx].toInt())
        }
    }

    public actual override fun close() {}
}

public actual abstract class InputStream : Closeable {
    public actual open fun read(b: ByteArray, off: Int, len: Int): Int {
        val endIdx = off + len
        require(off in 0 until b.size) { "Offset before start of array" }
        require(endIdx <= b.size) { "Range size beyond buffer size" }

        for (i in off until endIdx) {
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

    public actual override fun close() {}
}
