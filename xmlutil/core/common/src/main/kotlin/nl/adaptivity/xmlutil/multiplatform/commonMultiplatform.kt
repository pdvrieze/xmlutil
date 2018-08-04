/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.multiplatform

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
expect annotation class JvmStatic()

@Target(AnnotationTarget.TYPE)
@MustBeDocumented
expect annotation class JvmWildcard()

expect annotation class JvmField()

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.FILE)
expect annotation class JvmName(val name:String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@MustBeDocumented
expect annotation class JvmOverloads()

@Target(AnnotationTarget.FILE)
expect annotation class JvmMultifileClass()

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR)
expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

expect val KClass<*>.name: String

expect fun assert(value: Boolean, lazyMessage: () -> String)

expect fun assert(value: Boolean)

expect interface AutoCloseable {
    fun close()
}

expect interface Closeable: AutoCloseable
