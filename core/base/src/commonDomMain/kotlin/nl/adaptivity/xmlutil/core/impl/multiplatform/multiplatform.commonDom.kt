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

import kotlin.reflect.KClass

public actual abstract class Reader protected actual constructor() {
    public actual open fun read(): Int {
        val b = CharArray(1)
        if (read(b, 0, 1) < 0) return -1
        return b[0].code
    }

    public actual abstract fun read(buf: CharArray, offset: Int, len: Int): Int

    public actual abstract fun close()
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

    actual abstract override fun append(value: Char): Appendable

    actual open override fun append(value: CharSequence?): Appendable {
        return append(value, 0, value?.length ?: 0)
    }

    /** Write buffers to the underlying file (where valid). */
    public actual open fun flush() {}
}

public actual open class StringWriter : Writer() {
    private val buffer = StringBuilder()
    override fun write(text: String) {
        buffer.append(text)
    }

    actual override fun toString(): String {
        return buffer.toString()
    }

    actual open override fun append(value: Char): Appendable = apply {
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

/*
public actual abstract class Reader {
    public actual open fun read(): Int {
        val b = CharArray(1)
        if (read(b, 0, 1) < 0) return -1
        return b[0].code
    }

    public actual abstract fun read(buf: CharArray, offset: Int, len: Int): Int
}
*/

public actual open class StringReader(private val source: CharSequence) : Reader() {

    public actual constructor(source: String) : this(source as CharSequence)

    private var pos: Int = 0

    override fun read(): Int = when {
        pos >= source.length -> -1
        else -> source[pos++].code
    }

    actual override fun read(buf: CharArray, offset: Int, len: Int): Int {
        if (pos >= source.length) return -1
        val count = minOf(len, source.length - pos)
        for (i in 0 until count) {
            buf[offset + i] = source[pos + i]
        }
        pos += count
        return count
    }

    actual override fun close() {}
}

@MpJvmDefaultWithoutCompatibility
public actual annotation class Language actual constructor(
    actual val value: String,
    actual val prefix: String,
    actual val suffix: String
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class MpJvmDefaultWithoutCompatibility

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class MpJvmDefaultWithCompatibility

public actual inline fun <K, V> MutableMap<K, V>.computeIfAbsent(key: K, defaultValue: () -> V): V {
    return getOrPut(key, defaultValue)
}
