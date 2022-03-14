/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

public actual inline fun <T: Closeable?, R> T.use(block: (T) -> R): R {
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
    }}

public actual val KClass<*>.maybeAnnotations: List<Annotation> get() = emptyList()


public actual abstract class Writer : Appendable {
    public abstract fun write(text: String)
}

public actual open class StringWriter : Writer() {
    private val buffer = StringBuilder()
    override fun write(text: String) {
        buffer.append(text)
    }

    override fun toString(): String {
        return buffer.toString()
    }

    override fun append(value: Char): Appendable = apply {
        buffer.append(value)
    }

    override fun append(value: CharSequence?): Appendable = apply {
        buffer.append(value)
    }

    override fun append(
        value: CharSequence?,
        startIndex: Int,
        endIndex: Int
    ): Appendable = apply {
        buffer.append(value, startIndex, endIndex)
    }
}

public actual abstract class Reader {
    public actual open fun read(): Int {
        val b = CharArray(1)
        if (read(b, 0, 1) < 0) return -1
        return b[0].code
    }
    public actual abstract fun read(buf: CharArray, offset: Int, len: Int): Int
}

public actual open class StringReader(source: CharSequence): Reader() {
    private val source = source.toString()

    public actual constructor(source: String): this(source as CharSequence)

    private var srcOffset: Int = 0

    override fun read(): Int = when {
        srcOffset >= source.length -> -1
        else -> source[srcOffset++].code
    }

    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        if (srcOffset >= source.length) return -1
        val count = minOf(len, source.length - srcOffset)
        for (i in 0 until count) {
            buf[i + offset] = source[srcOffset + i]
        }
        srcOffset+=count
        return count
    }
}


public inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}
