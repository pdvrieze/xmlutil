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

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart

class DelegateMap<T: Any, V : NamedPart>(private val targetNamespace: String, private val delegate: Map<QName, T>, private val transform: (T) -> V) :
    AbstractMap<String, V>() {
    private val lazyStore: MutableMap<String, V?> = mutableMapOf()

    override val size: Int get() = delegate.size

    override val entries: Set<Map.Entry<String, V>> = object : AbstractSet<Map.Entry<String, V>>() {
        override val size: Int get() = delegate.size

        override fun iterator(): Iterator<Map.Entry<String, V>> {

            return object : Iterator<Map.Entry<String, V>> {
                private val keyIterator = delegate.keys.iterator()

                override fun hasNext(): Boolean = keyIterator.hasNext()

                override fun next(): Map.Entry<String, V> {
                    val k = keyIterator.next()
                    return object: Map.Entry<String, V> {
                        override val key: String = k.localPart

                        override val value: V get() = checkNotNull(get(key))
                    }
                }
            }
        }

    }


    override fun containsKey(key: String): Boolean {
        return delegate.containsKey(QName(targetNamespace, key))
    }

    override fun containsValue(value: V): Boolean {
        return delegate.containsKey(QName(targetNamespace, value.name.xmlString))
    }

    override val keys: Set<String>
        get() = delegate.keys.mapTo(HashSet()) { it.localPart }

    override val values: Collection<V> = object : AbstractCollection<V>() {
        override val size: Int get() = delegate.size

        override fun iterator(): Iterator<V> {
            return object : Iterator<V> {
                private val keyIterator = delegate.keys.iterator()

                override fun hasNext(): Boolean = keyIterator.hasNext()

                override fun next(): V {
                    val k = keyIterator.next().localPart
                    return checkNotNull(get(k)) {
                        "Key $k does not resolve to a non-null value"
                    }
                }

            }
        }
    }


    override fun get(key: String): V? {
        return lazyStore.getOrPut(key) {
            val orig = delegate[QName(targetNamespace, key)] ?: return null
            transform(orig)
        }
    }
}
