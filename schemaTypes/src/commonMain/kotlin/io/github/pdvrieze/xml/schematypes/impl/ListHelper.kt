/*
 * Copyright (c) 2026.
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

package io.github.pdvrieze.xml.schematypes.impl

import nl.adaptivity.xmlutil.XmlUtilInternal

/** Interface that provides default implementations for various list functions */
@XmlUtilInternal
interface ListHelper<T>: List<T> {
    override fun contains(element: T): Boolean {
        for (i in indices) {
            if (element == this[i]) return true
        }
        return false
    }

    /**
     * Checks that all elements of the collection are contained in this list.
     *
     * **Note** that this implementation is not efficient.
     */
    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { contains(it) }
    }

    override fun indexOf(element: T): Int {
        for (i in 0 until size) {
            if (element == this[i]) return i
        }
        return -1
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun iterator(): Iterator<T> {
        return ListIteratorImpl(this, 0)
    }

    override fun lastIndexOf(element: T): Int {
        for (i in (size - 1) downTo 0) {
            if (element == this[i]) return i
        }
        return -1
    }

    override fun listIterator(): ListIterator<T> {
        return ListIteratorImpl(this, 0)
    }

    override fun listIterator(index: Int): ListIterator<T> {
        if(index !in 0 .. size) throw IndexOutOfBoundsException("Index $index out of bounds")
        return ListIteratorImpl(this, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        if(fromIndex !in 0 .. size) throw IndexOutOfBoundsException("FromIndex $fromIndex out of bounds")
        if(toIndex !in toIndex .. size) throw IndexOutOfBoundsException("ToIndex $fromIndex out of bounds")

        return ListView(this, fromIndex, size - toIndex)
    }

    private class ListView<T>(val context: List<T>, val first: Int, val lastOffset: Int): AbstractList<T>() {
        override fun get(index: Int): T {
            checkIndex(index)
            return context[index + first]
        }

        val last = context.size - lastOffset

        override val size: Int get() = last - first

        fun checkIndex(index: Int) {
            if(index !in 0 .. size) throw IndexOutOfBoundsException("Index $index out of bounds")
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<T> {
            checkIndex(fromIndex)
            if (toIndex !in fromIndex ..< size) throw IndexOutOfBoundsException("Index $toIndex out of bounds")
            return super.subList(first + fromIndex, last - toIndex)
        }
    }

    private class ListIteratorImpl<T> (val context: List<T>, var nextPos: Int): ListIterator<T> {
        override fun next(): T {
            return context[nextPos++]
        }

        override fun hasNext(): Boolean {
            return nextPos < context.size
        }

        override fun hasPrevious(): Boolean {
            return nextPos > 0
        }

        override fun previous(): T {
            return context[--nextPos]
        }

        override fun nextIndex(): Int {
            return nextPos
        }

        override fun previousIndex(): Int {
            return nextPos-1
        }

    }
}

