/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
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
public expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

@XmlUtilInternal
public expect val KClass<*>.name: String

@XmlUtilInternal
public expect fun assert(value: Boolean, lazyMessage: () -> String)

@XmlUtilInternal
public expect fun assert(value: Boolean)

@XmlUtilInternal
public expect interface AutoCloseable {
    public fun close()
}

@XmlUtilInternal
public expect interface Closeable : AutoCloseable

@XmlUtilInternal
public expect inline fun <T : Closeable?, R> T.use(block: (T) -> R): R

@XmlUtilInternal
public expect val KClass<*>.maybeAnnotations: List<Annotation>


@XmlUtilInternal
public expect abstract class Writer : Appendable {
    public open fun write(text: String)
    open override fun append(value: CharSequence?): Appendable
    abstract override fun append(value: Char): Appendable
    abstract override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable
    public open fun flush()
}

@XmlUtilInternal
public expect open class StringWriter() : Writer {
    override fun toString(): String
    override fun append(value: Char): Appendable
    override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable
}

@XmlUtilInternal
public expect abstract class OutputStream : Closeable {
    public abstract fun write(b: Int)

    public open fun write(b: ByteArray)

    public open fun write(b: ByteArray, off: Int, len: Int)

    public override fun close()
}

@XmlUtilInternal
public expect abstract class Reader {
    protected constructor()
    public open fun read(): Int
    public abstract fun read(buf: CharArray, offset: Int, len: Int): Int
    public abstract fun close()
}

@XmlUtilInternal
public expect abstract class InputStream : Closeable {
    public open fun read(buffer: ByteArray, offset: Int, len: Int): Int

    public open fun read(b: ByteArray): Int
    public abstract fun read(): Int
    public override fun close()
}

@XmlUtilInternal
public expect open class StringReader(source: String) : Reader {
    override fun read(buf: CharArray, offset: Int, len: Int): Int
    override fun close()
}

@XmlUtilInternal
public expect annotation class Language(
    val value: String,
    val prefix: String = "",
    val suffix: String = ""
)

@XmlUtilInternal
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
expect public annotation class MpJvmDefaultWithoutCompatibility()

@XmlUtilInternal
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
expect public annotation class MpJvmDefaultWithCompatibility()

@XmlUtilInternal
public expect inline fun <K,V> MutableMap<K,V>.computeIfAbsent(key: K, crossinline defaultValue: () -> V): V
