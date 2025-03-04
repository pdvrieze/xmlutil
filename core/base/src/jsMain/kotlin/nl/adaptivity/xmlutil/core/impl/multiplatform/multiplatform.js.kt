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

import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.reflect.KClass

public actual val KClass<*>.name: String get() = js.name

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class Throws(actual vararg val exceptionClasses: KClass<out Throwable>)


public actual fun assert(value: Boolean, lazyMessage: () -> String) {
    if (!value) console.error("Assertion failed: ${lazyMessage()}")
}

public actual fun assert(value: Boolean) {
    if (!value) console.error("Assertion failed")
}

public actual interface AutoCloseable {
    public actual fun close()
}

public actual interface Closeable : AutoCloseable

public actual inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            this == null -> {}
            exception == null -> close()
            else ->
                try {
                    close()
                } catch (closeException: Throwable) {
                    // cause.addSuppressed(closeException) // ignored here
                }
        }
    }
}

public actual val KClass<*>.maybeAnnotations: List<Annotation> get() = emptyList()


public actual abstract class Writer : Appendable {
    public actual open fun write(text: String) {
        append(text)
    }

    actual open override fun append(value: CharSequence?): Appendable {
        return append(value, 0, value?.length ?: 0)
    }

    public actual open fun flush() {
    }
}

public actual open class StringWriter : Writer() {
    private val buffer = StringBuilder()
    override fun write(text: String) {
        buffer.append(text)
    }

    actual override fun toString(): String {
        return buffer.toString()
    }

    actual override fun append(value: Char): Appendable = apply {
        buffer.append(value)
    }

    override fun append(value: CharSequence?): Appendable = apply {
        buffer.append(value)
    }

    actual override fun append(
        value: CharSequence?,
        startIndex: Int,
        endIndex: Int
    ): Appendable = apply {
        buffer.append(value, startIndex, endIndex)
    }
}

public actual abstract class Reader protected actual constructor() {
    public actual open fun read(): Int {
        val b = CharArray(1)
        if (read(b, 0, 1) < 0) return -1
        return b[0].code
    }

    public actual abstract fun read(buf: CharArray, offset: Int, len: Int): Int
    public actual abstract fun close()
}

public actual open class StringReader(source: CharSequence) : Reader() {
    private val source = source.toString()

    public actual constructor(source: String) : this(source as CharSequence)

    private var srcOffset: Int = 0

    override fun read(): Int = when {
        srcOffset >= source.length -> -1
        else -> source[srcOffset++].code
    }

    actual override fun read(buf: CharArray, offset: Int, len: Int): Int {
        if (srcOffset >= source.length) return -1
        val count = minOf(len, source.length - srcOffset)
        for (i in 0 until count) {
            buf[i + offset] = source[srcOffset + i]
        }
        srcOffset += count
        return count
    }

    actual override fun close() {}
}

public actual abstract class InputStream : Closeable {
    public actual open fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        val endIdx = offset + len
        require(offset in buffer.indices) { "Offset before start of array" }
        require(endIdx <= buffer.size) { "Range size beyond buffer size" }

        for (i in offset until endIdx) {
            val byte = read()
            if (byte < 0) {
                return i
            }
            buffer[i] = byte.toByte()
        }
        return len
    }

    public actual open fun read(b: ByteArray): Int {
        return read(b, 0, b.size)
    }

    public actual abstract fun read(): Int

    public actual override fun close() {}
}

public actual abstract class OutputStream : Closeable {
    public actual abstract fun write(b: Int)

    public actual open fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    public actual open fun write(b: ByteArray, off: Int, len: Int) {
        val endIdx = off + len
        require(off in b.indices) { "Offset before start of array" }
        require(endIdx <= b.size) { "Range size beyond buffer size" }
        for (idx in off until endIdx) {
            write(b[idx].toInt())
        }
    }

    public actual override fun close() {}
}

@MpJvmDefaultWithoutCompatibility
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
public actual annotation class Language(
    actual val value: String,
    actual val prefix: String,
    actual val suffix: String
)


public inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@XmlUtilInternal
public actual annotation class MpJvmDefaultWithoutCompatibility

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@XmlUtilInternal
public actual annotation class MpJvmDefaultWithCompatibility

@XmlUtilInternal
public actual inline fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, crossinline defaultValue: () -> V): V {
    return getOrPut(key, defaultValue)
}
