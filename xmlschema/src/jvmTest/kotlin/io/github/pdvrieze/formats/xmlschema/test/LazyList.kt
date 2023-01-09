/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xmlschema.test

class LazyList<T, V>(private val delegate: List<T>, private val transform: (T) -> V) :
    AbstractList<V>() {
    private val lazyStore: MutableList<V> = mutableListOf()

    override val size: Int get() = delegate.size

    override fun get(index: Int): V {
        if (index >= delegate.size) throw IndexOutOfBoundsException("Beyond schema list size")

        while (index <= lazyStore.size) {
            lazyStore.add(UNSETMARKER)
        }

        val e = lazyStore[index]

        @Suppress("UNCHECKED_CAST")
        val value: V = if (e != UNSETMARKER) (e as V) else run {
            transform(delegate[index]).also { v -> lazyStore[index] = v }
        }
        return value
    }

    @Suppress("UNCHECKED_CAST")
    private val UNSETMARKER: V = Any() as V
}
