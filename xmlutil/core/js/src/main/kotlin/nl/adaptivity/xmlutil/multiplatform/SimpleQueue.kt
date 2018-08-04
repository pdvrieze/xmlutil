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

package nl.adaptivity.xmlutil.multiplatform

actual class SimpleQueue<E> {
    private var data = js("[]")
    actual constructor()

    actual val size: Int
        get() = data.length

    actual fun peekFirst(): E? = when(size) {
        0 -> null
        else -> data[0]
    }

    actual fun peekLast(): E? = when(size) {
        0 -> null
        else -> data[size-1]
    }

    actual fun removeFirst(): E = data.shift()

    actual fun removeLast(): E = data.pop()

    actual fun addLast(e: E) { data.push(e) }
    actual fun add(element: E): Boolean {
        data.push(element)
        return true
    }

    actual fun clear() {
        data = js("[]")
    }

}