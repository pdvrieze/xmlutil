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

expect class SimpleQueue<E> {
    constructor()

    val size: Int

    fun peekFirst(): E?
    fun peekLast(): E?

    fun removeFirst(): E
    fun removeLast(): E

    fun addLast(e:E)
    fun add(element: E): Boolean

    fun clear()
}

fun SimpleQueue<*>.isNotEmpty() = size>0
fun <E> SimpleQueue<E>.addAll(elements: Iterable<E>):Boolean {
    return elements.fold(false) { acc, e ->
        acc or add(e)
    }
}