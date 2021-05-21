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

actual val KClass<*>.name get() = js.name

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR
       )
@Retention(AnnotationRetention.SOURCE)
actual annotation class Throws(actual vararg val exceptionClasses: KClass<out Throwable>)


actual fun assert(value: Boolean, lazyMessage: () -> String) {
    if (!value) console.error("Assertion failed: ${lazyMessage()}")
}

actual fun assert(value: Boolean) {
    if (!value) console.error("Assertion failed")
}

actual interface AutoCloseable {
    actual fun close()
}

actual interface Closeable : AutoCloseable

actual val KClass<*>.maybeAnnotations: List<Annotation> get() = emptyList()


actual abstract class Writer: Appendable {
    abstract fun write(text: String)
}

actual open class StringWriter: Writer() {
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

inline fun <T: Closeable, R> T.use(block: (T)->R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}
