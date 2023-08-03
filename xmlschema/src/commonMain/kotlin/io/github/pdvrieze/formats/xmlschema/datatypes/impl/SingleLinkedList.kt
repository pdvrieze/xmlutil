/*
 * Copyright (c) 2022.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.impl

/** Simple read-only list that allows for fast recursion by having a back chain only */
sealed class SingleLinkedList<T> private constructor(dummy: Unit = Unit): List<T> {

    abstract operator fun plus(other: T): SingleLinkedList<T>

    abstract fun dropLast(n: Int = 1): SingleLinkedList<T>
    abstract fun dropLastOrEmpty(n: Int = 1): SingleLinkedList<T>

    sealed class Empty<T> : SingleLinkedList<T>() {
        override val size: Int get() = 0

        override fun contains(element: T): Boolean = false

        override fun containsAll(elements: Collection<T>): Boolean = elements.isEmpty()

        override fun get(index: Int): Nothing {
            throw IndexOutOfBoundsException("Empty list has no elements: $index")
        }

        override fun dropLast(n: Int): SingleLinkedList<T> {
            if (n > 0) {
                throw IndexOutOfBoundsException("Empty list cannot be shortened")
            }
            return this
        }

        override fun dropLastOrEmpty(n: Int): SingleLinkedList<T> = this

        override fun indexOf(element: T): Int = -1

        override fun iterator(): Iterator<Nothing> {
            return emptyList<Nothing>().iterator()
        }

        override fun lastIndexOf(element: T): Int = -1

        override fun listIterator(): ListIterator<Nothing> {
            return emptyList<Nothing>().listIterator()
        }

        override fun listIterator(index: Int): ListIterator<Nothing> {
            if (index!=0) throw IndexOutOfBoundsException("Iterator index out of bounds")
            return emptyList<Nothing>().listIterator(index)
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<T> {
            if (fromIndex!= 0 || toIndex!=0) throw IndexOutOfBoundsException("from or to on empty list out of bounds")
            return this
        }

        override fun plus(other: T): SingleLinkedList<T> = Head(other)

        final override fun isEmpty(): Boolean = true

        override fun toString(): String = "[]"
    }

    private object EmptyImpl: Empty<Nothing>()

    sealed class ValuedElement<T>(val elem: T): SingleLinkedList<T>() {
        override fun plus(other: T): SingleLinkedList<T> = Tail(this, other)
        final override fun isEmpty(): Boolean = false

        abstract fun toString(appendable: StringBuilder)
    }

    class Head<T>(elem: T) : ValuedElement<T>(elem) {
        override val size: Int get() = 1

        override fun contains(element: T): Boolean = elem == element

        override fun containsAll(elements: Collection<T>): Boolean {
            return elements.all { it == elem }
        }

        override fun get(index: Int): T {
            if (index != 0) throw IndexOutOfBoundsException("$index is not in range 0..0")
            return elem
        }

        override fun indexOf(element: T): Int {
            return if (elem == element) 0 else -1
        }

        override fun dropLast(n: Int): SingleLinkedList<T> = when (n) {
            0 -> this
            1 -> empty()
            else -> throw IndexOutOfBoundsException("requested to drop more elements than present")
        }

        override fun dropLastOrEmpty(n: Int): SingleLinkedList<T> = when (n) {
            0 -> this
            else -> empty()
        }

        override fun iterator(): Iterator<T> {
            return object : Iterator<T> {
                private var read: Boolean = false
                override fun hasNext(): Boolean {
                    return !read
                }

                override fun next(): T {
                    require(read == false)
                    read = true
                    return elem
                }
            }
        }

        override fun lastIndexOf(element: T): Int {
            return indexOf(element)
        }

        override fun listIterator(): ListIterator<T> {
            return listOf(elem).listIterator()
        }

        override fun listIterator(index: Int): ListIterator<T> {
            return listOf(elem).listIterator(index)
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<T> {
            return listOf(elem).subList(fromIndex, toIndex)
        }

        override fun toString(appendable: StringBuilder) {
            appendable.append(elem)
        }

        override fun toString(): String {
            return "$elem"
        }
    }

    class Tail<T>(val pred: ValuedElement<T>, elem: T) : ValuedElement<T>(elem) {
        override val size: Int = pred.size + 1

        override fun contains(element: T): Boolean {
            return elem == element || pred.contains(element)
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            val copy = elements.toMutableSet()
            var current: ValuedElement<T> = this
            while (current is Tail) {
                copy.remove(current.elem)
                current = current.pred
            }
            copy.remove(current.elem)
            return copy.isEmpty()
        }

        override fun get(index: Int): T {
            if (! (index in 0 until size)) throw IndexOutOfBoundsException("No element at $index")
            return if (index == size-1) elem else pred.get(index)
        }

        override fun indexOf(element: T): Int {
            val prev = pred.indexOf(element)
            return when {
                prev >= 0 -> prev
                element == elem -> size -1
                else -> -1
            }
        }

        override fun lastIndexOf(element: T): Int {
            return if (elem == element) size-1 else pred.indexOf(element)
        }

        override fun dropLast(n: Int): SingleLinkedList<T> = when(n) {
            0 -> this
            1 -> pred
            size -> empty()
            else -> when {
                n > size -> throw IndexOutOfBoundsException("Attempting to drop more than the amount of elements")
                else -> pred.dropLast(n - 1)
            }
        }

        override fun dropLastOrEmpty(n: Int): SingleLinkedList<T> = when(n) {
            0 -> this
            1 -> pred
            size -> empty()
            else -> when {
                n > size -> empty()
                else -> pred.dropLast(n - 1)
            }
        }

        private fun regularList(): List<T> {
            val data = arrayOfNulls<Any>(size)
            var current: ValuedElement<T> = this
            while (current is Tail) {
                data[current.size-1] = current.elem
                current = current.pred
            }
            data[0] = current.elem

            @Suppress("UNCHECKED_CAST")
            return data.toList() as List<T>
        }

        override fun iterator(): Iterator<T> {
            return regularList().iterator()
        }

        override fun listIterator(): ListIterator<T> {
            return regularList().listIterator()
        }

        override fun listIterator(index: Int): ListIterator<T> {
            return regularList().listIterator(index)
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<T> {
            return regularList().subList(fromIndex, toIndex)
        }

        override fun toString(appendable: StringBuilder) {
            pred.toString(appendable)
            appendable.append(", ").append(elem)
        }

        override fun toString(): String {
            return buildString { append('['); toString(this); append("]") }
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): Empty<T> = EmptyImpl as Empty<T>
    }
}

fun <T> SingleLinkedList(content: T): SingleLinkedList<T> = SingleLinkedList.Head(content)

fun <T> SingleLinkedList(): SingleLinkedList<T> = SingleLinkedList.empty()

//fun SingleLinkedList(): SingleLinkedList<Nothing> = SingleLinkedList.Empty
