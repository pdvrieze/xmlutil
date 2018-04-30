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
expect public annotation class JvmOverloads()


@Target(AnnotationTarget.FILE)
expect annotation class JvmMultifileClass()

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR)
expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

expect class URI {
    fun getPath(): String
}

expect class UUID

expect fun String.toUUID(): UUID

inline val URI.path get() = getPath()

expect inline fun createUri(s: String): URI

fun Appendable.append(d: Double) = append(d.toString())
fun Appendable.append(i: Int) = append(i.toString())

expect class Class<T:Any?>

expect fun arraycopy(src: Any, srcPos:Int, dest:Any, destPos:Int, length:Int)

expect fun <T> fill(array: Array<T>, element: T, fromIndex: Int = 0, toIndex: Int = array.size)

expect fun assert(value: Boolean, lazyMessage: () -> String)

expect fun assert(value: Boolean)

expect interface AutoCloseable {
    fun close()
}

expect inline fun <reified T:Any> isTypeOf(value: Any):Boolean