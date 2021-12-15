/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.resolved

class DelegateList<T : Any, V : ResolvedPart>(private val delegate: List<T>, private val transform: (T) -> V) :
    AbstractList<V>() {
    private val lazyStore: MutableList<V?> = MutableList(delegate.size) { null }

    override val size: Int get() = delegate.size

    override fun contains(element: V): Boolean {
        return delegate.contains(element.rawPart)
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return delegate.containsAll(elements.map { it.rawPart })
    }

    override fun indexOf(element: V): Int {
        return delegate.indexOf(element.rawPart)
    }

    override fun get(index: Int): V {
        val value: V = lazyStore[index] ?: run {
            transform(delegate[index]).also { v -> lazyStore[index] = v }
        }
        return value
    }
}
