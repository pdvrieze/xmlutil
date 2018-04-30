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

import java.util.UUID

actual typealias JvmStatic = kotlin.jvm.JvmStatic

actual typealias JvmWildcard = kotlin.jvm.JvmWildcard

actual typealias JvmField = kotlin.jvm.JvmField

actual typealias JvmName = kotlin.jvm.JvmName

actual typealias JvmOverloads = kotlin.jvm.JvmOverloads

actual typealias JvmMultifileClass = kotlin.jvm.JvmMultifileClass

actual typealias URI = java.net.URI

@Suppress("NOTHING_TO_INLINE")
actual inline fun createUri(s: String): URI = URI.create(s)

actual typealias Class<T> = java.lang.Class<T>

actual typealias Throws = kotlin.jvm.Throws

actual typealias UUID = UUID

actual fun String.toUUID(): UUID = UUID.fromString(this)

@Suppress("NOTHING_TO_INLINE")
actual fun <T> fill(array: Array<T>, element: T, fromIndex: Int, toIndex: Int) {
    java.util.Arrays.fill(array, fromIndex, toIndex, element)
}

@Suppress("NOTHING_TO_INLINE")
actual fun arraycopy(src: Any, srcPos:Int, dest:Any, destPos:Int, length:Int) =
    java.lang.System.arraycopy(src, srcPos, dest, destPos, length)

actual inline fun <reified T:Any> isTypeOf(value: Any):Boolean = value::class.java == T::class.java