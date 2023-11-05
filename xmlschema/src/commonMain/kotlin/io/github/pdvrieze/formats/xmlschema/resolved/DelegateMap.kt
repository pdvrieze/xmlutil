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
import nl.adaptivity.xmlutil.namespaceURI

class DelegateMap<T: Any, V : NamedPart> :
    AbstractMap<String, V> {

    private val targetNamespace: String
    private val _delegate: Map<String, T> // force this to use the getter at runtime
    private val delegate: Map<String, T> get() = _delegate
    private val transform: (T) -> V

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
                    return object : Map.Entry<String, V> {
                        override val key: String = k

                        override val value: V get() = checkNotNull(get(key))
                    }
                }
            }
        }

    }

    override val values: Collection<V> = object : AbstractCollection<V>() {
        override val size: Int get() = delegate.size

        override fun iterator(): Iterator<V> {
            return object : Iterator<V> {
                private val keyIterator = delegate.keys.iterator()

                override fun hasNext(): Boolean = keyIterator.hasNext()

                override fun next(): V {
                    val k = keyIterator.next()
                    return checkNotNull(get(k)) {
                        "Key $k does not resolve to a non-null value"
                    }
                }

            }
        }
    }

    constructor(targetNamespace: String, delegate: Map<QName, T>, dummy: Boolean = false, transform: (T) -> V) : super() {
        this.targetNamespace = targetNamespace
        this._delegate = delegate.mapKeys { it.key.localPart }
        this.transform = transform

        for (k in delegate.keys) {
            require(k.namespaceURI == targetNamespace) {
                "Target namespace mismatch (${k.namespaceURI} != $targetNamespace)"
            }
        }

    }

    constructor(targetNamespace: String, delegate: Map<String, T>, transform: (T) -> V) : super() {
        this.targetNamespace = targetNamespace
        this._delegate = delegate.toMap() // defensive copy
        this.transform = transform
    }


    override fun containsKey(key: String): Boolean {
        return delegate.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        val name = value.mdlQName
        return targetNamespace == name.namespaceURI && delegate.containsKey(name.localPart)
    }

    override val keys: Set<String>
        get() = delegate.keys


    override fun get(key: String): V? {
        return lazyStore.getOrPut(key) {
            val orig = delegate[key] ?: return null
            transform(orig)
        }
    }
}
