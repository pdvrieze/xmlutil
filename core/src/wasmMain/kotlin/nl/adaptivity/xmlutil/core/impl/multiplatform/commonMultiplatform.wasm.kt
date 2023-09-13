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

public inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}


@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class Throws actual constructor(actual vararg val exceptionClasses: KClass<out Throwable>)

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
@XmlUtilInternal
public actual val KClass<*>.name: String
    get() = "foobar"//className(this as Clazz)

internal external interface Clazz

@JsFun("(clazz) => clazz.name")
internal external fun className(clazz: Clazz): String

@XmlUtilInternal
public actual fun assert(value: Boolean, lazyMessage: () -> String) {
    if (!value) {
        throw AssertionError(lazyMessage())
    }
}

@XmlUtilInternal
public actual fun assert(value: Boolean) {
    if (!value) {
        throw AssertionError()
    }
}

public actual abstract class OutputStream : Closeable {
    public actual abstract fun write(b: Int)
    public actual open fun write(b: ByteArray) {
        for (byte in b) write(byte.toInt())
    }

    public actual open fun write(b: ByteArray, off: Int, len: Int) {
        for (i in off until (off + len)) {
            write(b[i].toInt())
        }
    }

}

public actual abstract class InputStream : Closeable {
    public actual open fun read(b: ByteArray, off: Int, len: Int): Int {
        for (i in off until (off + len)) {
            val value = read()
            if (value < 0) return i - off - 1
            b[i] = value.toByte()
        }
        return len
    }

    public actual fun read(b: ByteArray): Int {
        for (i in b.indices) {
            val value = read()
            if (value < 0) return i - 1
            b[i] = value.toByte()
        }
        return b.size
    }

    public actual abstract fun read(): Int

}
