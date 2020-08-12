/*
 * Copyright (c) 2020. 
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

package nl.adaptivity.xmlutil.serialization.impl

internal class LazyList<T> (source: List<() -> T>) : List<T> {

    private val source: List<Lazy<T>> = source.map { lazy(it) }

    override val size: Int get() = source.size

    override fun contains(element: T): Boolean {
        throw UnsupportedOperationException("contains is inefficient in a lazy list")
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException("contains is inefficient in a lazy list")
    }

    override fun get(index: Int): T = source.get(index).value

    override fun indexOf(element: T): Int {
        throw UnsupportedOperationException("indexOf is inefficient in a lazy list")
    }

    override fun isEmpty(): Boolean = source.isEmpty()

    override fun iterator(): Iterator<T> {
        TODO("not implemented")
    }

    override fun lastIndexOf(element: T): Int {
        TODO("not implemented")
    }

    override fun listIterator(): ListIterator<T> {
        TODO("not implemented")
    }

    override fun listIterator(index: Int): ListIterator<T> {
        TODO("not implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        TODO("not implemented")
    }
}
