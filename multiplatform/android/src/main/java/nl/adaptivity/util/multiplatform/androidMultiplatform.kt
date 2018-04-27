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

actual typealias Class<T> = java.lang.Class<T>

actual typealias Throws = kotlin.jvm.Throws

actual typealias UUID = java.util.UUID

actual fun String.toUUID(): UUID = UUID.fromString(this)


@Suppress("NOTHING_TO_INLINE")
actual fun <T> fill(array: Array<T>, element: T, fromIndex: Int, toIndex: Int) {
    java.util.Arrays.fill(array, fromIndex, toIndex, element)
}

@Suppress("NOTHING_TO_INLINE")
actual inline fun arraycopy(src: Any, srcPos:Int, dest:Any, destPos:Int, length:Int) =
    java.lang.System.arraycopy(src, srcPos, dest, destPos, length)
