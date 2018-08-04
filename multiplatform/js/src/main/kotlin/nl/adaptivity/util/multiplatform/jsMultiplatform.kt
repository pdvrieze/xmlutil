/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util.multiplatform

import kotlin.reflect.KClass

actual val KClass<*>.name get() = js.name

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR)
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

actual interface Closeable: AutoCloseable