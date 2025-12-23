/*
 * Copyright (c) 2024-2025.
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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

internal class QNameMap<T : Any> private constructor(
    private var namespaces: Array<String?>,
    private var maps: Array<HashMap<String, T>?>,
    private var namespaceCount: Int,
    size: Int,
) : MutableMap<QName, T> {

    constructor() : this(arrayOfNulls(8), arrayOfNulls(8), 0, 0)

    override var size: Int = size
        private set

    override val entries = object : MutableSet<MutableMap.MutableEntry<QName, T>> {
        override val size: Int get() = this@QNameMap.size

        override fun isEmpty(): Boolean = this@QNameMap.isEmpty()

        override fun clear() {
            this@QNameMap.clear()
        }

        override fun contains(element: MutableMap.MutableEntry<QName, T>): Boolean {
            return this@QNameMap[element.key] == element.value
        }

        override fun containsAll(elements: Collection<MutableMap.MutableEntry<QName, T>>): Boolean {
            return elements.all { this@QNameMap[it.key] == it.value }
        }

        @IgnorableReturnValue
        override fun retainAll(elements: Collection<MutableMap.MutableEntry<QName, T>>): Boolean {
            val perNS =
                elements.groupByTo(HashMap(), { it.key.namespaceURI }, { SimpleEntry(it.key.localPart, it.value) })

            var changed = false
            for (i in 0 until namespaceCount) {
                val entries = perNS[namespaces[i]]
                if (entries != null) {
                    changed = changed || maps[i]!!.entries.retainAll(entries)
                }
            }
            return changed
        }

        @IgnorableReturnValue
        override fun removeAll(elements: Collection<MutableMap.MutableEntry<QName, T>>): Boolean {
            var newSize = 0
            for (i in 0 until namespaceCount) {
                val ns = namespaces[i]!!
                val elems = elements.asSequence()
                    .filter { it.key.namespaceURI == ns }
                    .map { SimpleEntry(it.key.localPart, it.value) }
                    .toList()
                val map = maps[i]!!
                map.entries.removeAll(elems)
                newSize += map.size
            }
            return (this@QNameMap.size != newSize).also { this@QNameMap.size = newSize }
        }

        @IgnorableReturnValue
        override fun remove(element: MutableMap.MutableEntry<QName, T>): Boolean {
            val ns = element.key.namespaceURI
            withNamespace(ns) {
                if(maps[it]!!.entries.remove(SimpleEntry(element.key.localPart, element.value))) {
                    this@QNameMap.size -= 1
                    return true
                } else return false
            }
            return false
        }

        @IgnorableReturnValue
        override fun add(element: MutableMap.MutableEntry<QName, T>): Boolean {
            if(this@QNameMap.put(element.key, element.value) == null) {
                this@QNameMap.size += 1
                return true
            }
            return false
        }

        @IgnorableReturnValue
        override fun addAll(elements: Collection<MutableMap.MutableEntry<QName, T>>): Boolean {
            var changed = false
            for (e in elements) {
                changed = changed || add(e)
            }
            return true
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<QName, T>> {
            return SimpleIterator { pos, ns, e -> QNameEntry(pos, e) }
        }
    }

    override val keys = object : MutableSet<QName> {
        @IgnorableReturnValue
        override fun add(element: QName): Nothing {
            throw UnsupportedOperationException()
        }

        override val size: Int
            get() = this@QNameMap.size

        @IgnorableReturnValue
        override fun addAll(elements: Collection<QName>): Nothing {
            throw UnsupportedOperationException()
        }

        override fun clear() { this@QNameMap.clear()
        }

        override fun contains(element: QName): Boolean {
            return containsKey(element)
        }

        override fun containsAll(elements: Collection<QName>): Boolean {
            return elements.all { contains(it) }
        }

        override fun isEmpty(): Boolean = this@QNameMap.isEmpty()

        override fun iterator(): MutableIterator<QName> {
            return SimpleIterator { nsPos, ns, (ln, _) -> QName(ns, ln) }
        }

        @IgnorableReturnValue
        override fun remove(element: QName): Boolean {
            return this@QNameMap.remove(element) != null
        }

        @IgnorableReturnValue
        override fun removeAll(elements: Collection<QName>): Boolean {
            var changed = false
            for (e in elements) { changed = changed || remove(e) }
            return changed
        }

        @IgnorableReturnValue
        override fun retainAll(elements: Collection<QName>): Boolean {
            val perNS = elements.groupBy({ it.namespaceURI}, { it.localPart })

            var changed = false
            for (i in 0 until namespaceCount) {
                val entries = perNS[namespaces[i]]
                if (entries != null) {
                    changed = changed || maps[i]!!.keys.retainAll(entries)
                }
            }
            return changed
        }
    }

    override val values = object:  MutableCollection<T> {
        @IgnorableReturnValue
        override fun add(element: T): Nothing {
            throw UnsupportedOperationException()
        }

        override val size: Int
            get() = this@QNameMap.size

        @IgnorableReturnValue
        override fun addAll(elements: Collection<T>): Nothing {
            throw UnsupportedOperationException()
        }

        override fun clear() { this@QNameMap.clear()
        }

        override fun contains(element: T): Boolean {
            return containsValue(element)
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            return elements.all { contains(it) }
        }

        override fun isEmpty(): Boolean = this@QNameMap.isEmpty()

        override fun iterator(): MutableIterator<T> {
            return SimpleIterator { _, _, (_, v) -> v }
        }

        @IgnorableReturnValue
        override fun remove(element: T): Nothing {
            throw UnsupportedOperationException()
        }

        @IgnorableReturnValue
        override fun removeAll(elements: Collection<T>): Nothing {
            throw UnsupportedOperationException()
        }

        @IgnorableReturnValue
        override fun retainAll(elements: Collection<T>): Nothing {
            throw UnsupportedOperationException()
        }

    }

    override fun containsKey(key: QName): Boolean {
        return containsKey(key.namespaceURI, key.localPart)
    }

    fun containsKey(ns: String, localPart: String): Boolean {
        for (i in 0 until size) {
            if (namespaces[i] == ns) {
                return maps[i]!!.containsKey(localPart)
            }
        }
        return false
    }

    fun containsNS(namespace: String): Boolean {
        val ns = namespace
        for (i in 0 until size) {
            if (namespaces[i] == ns) return true
        }
        return false
    }

    override fun containsValue(value: T): Boolean {
        for (i in 0 until size) {
            if (maps[i]!!.containsValue(value)) return true
        }
        return false
    }

    override fun get(key: QName): T? {
        return get(key.namespaceURI, key.localPart)
    }

    operator fun get(namespace: String, localPart: String): T? {
        withNamespace(namespace) { ns ->
            maps[ns]!![localPart]?.let { return it }
        }
        return null
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    fun copyOf(): QNameMap<T> {
        val newMaps = Array<HashMap<String, T>?>(maps.size) { maps[it]?.toMap(HashMap())}
        return QNameMap(namespaces.copyOf(), newMaps, namespaceCount, size)
    }

    override fun clear() {
        namespaces = arrayOfNulls(8)
        maps = arrayOfNulls(8)
        namespaceCount = 0
        size = 0
    }

    @IgnorableReturnValue
    override fun put(key: QName, value: T): T? {
        val namespace = key.namespaceURI
        val localPart = key.localPart
        return put(namespace, localPart, value)
    }

    @IgnorableReturnValue
    fun put(namespace: String, localPart: String, value: T): T? {
        withNamespace(namespace) { idx ->
            val newMap = maps[idx]
            // Auto sort (mostly, it only moves one at a time)
            if (idx > 0 && newMap!!.size >= maps[idx - 1]!!.size) {
                maps[idx] = maps[idx - 1]
                maps[idx - 1] = newMap
                namespaces[idx] = namespaces[idx -1]
                namespaces[idx -1] = namespace
            }
            val old = newMap!!.put(localPart, value)
            if (old == null) ++size
            return old
        }
        if (namespaceCount == namespaces.size) {
            val newSize = namespaceCount * 2
            namespaces = namespaces.copyOf(newSize)
            maps = maps.copyOf(newSize)
        }
        namespaces[namespaceCount] = namespace
        val r: T?
        maps[namespaceCount++] = HashMap<String, T>().apply { r = put(localPart, value) }
        ++size
        return r
    }

    override fun putAll(from: Map<out QName, T>) {
        for (f in from.entries) {
            put(f.key, f.value)
        }
    }

    @IgnorableReturnValue
    override fun remove(key: QName): T? {
        val namespace = key.namespaceURI
        val localPart = key.localPart
        return remove(namespace, localPart)
    }

    @IgnorableReturnValue
    fun remove(namespace: String, localPart: String): T? {
        for (i in 0 until namespaceCount) {
            if (namespaces[i] == namespace) {
                val m = maps[i]
                val removed = m!!.remove(localPart)
                if (removed != null) {
                    size -= 1
                    if (m.isEmpty()) {
                        namespaces.copyInto(namespaces, i, i + 1, namespaceCount)
                        maps.copyInto(maps, i, i + 1, namespaceCount)
                        namespaces[--namespaceCount] = null
                        maps[namespaceCount] = null
                    }
                }

                return removed
            }
        }
        return null
    }

    private inline fun withNamespace(namespace: String, action: (Int) -> Unit) {
        for (i in 0 until namespaceCount) {
            if (namespaces[i] == namespace) {
                action(i)
                return
            }
        }
    }

    private inner class QNameEntry(val pos: Int, val subEntry: Map.Entry<String, T>) :
        MutableMap.MutableEntry<QName, T> {
        override val key: QName get() = QName(namespaces[pos]!!, subEntry.key)
        override val value: T get() = subEntry.value

        @IgnorableReturnValue
        override fun setValue(newValue: T): T {
            withNamespace(key.namespaceURI) {
                maps[it]!!.set(key.localPart, newValue)
            }
            return value
        }
    }

    private class SimpleEntry<V>(override val key: String, override val value: V) : MutableMap.MutableEntry<String, V> {
        @IgnorableReturnValue
        override fun setValue(newValue: V) = throw UnsupportedOperationException()
    }

    private inner class SimpleIterator<R>(private val transform: (Int, String, MutableMap.MutableEntry<String, T>) -> R) :
        MutableIterator<R> {
        private var currentPos = 0
        private var currentIterator: MutableIterator<MutableMap.MutableEntry<String, T>>? =
            entryIteratorFor(0)

        override fun hasNext(): Boolean {
            while (currentPos < namespaceCount) {
                val it = currentIterator ?: return false
                if (it.hasNext()) return true

                currentIterator = entryIteratorFor(++currentPos)
            }
            return false
        }

        override fun next(): R {
            while (currentPos < namespaceCount) {
                if (currentIterator!!.hasNext()) {
                    val e = currentIterator!!.next()
                    return transform(currentPos, namespaces[currentPos]!!, e)
                }

                currentIterator = entryIteratorFor(++currentPos)
            }
            throw NoSuchElementException()
        }

        override fun remove() {
            currentIterator!!.remove()
            this@QNameMap.size -= 1
            if (maps[currentPos]!!.isEmpty()) {
                maps.copyInto(maps, currentPos, currentPos + 1, namespaceCount)
                --namespaceCount
            }
            currentIterator = entryIteratorFor(currentPos)
        }
    }

    private fun entryIteratorFor(pos: Int): MutableIterator<MutableMap.MutableEntry<String, T>>? =
        when {
            pos < namespaceCount -> maps[pos]!!.iterator()
            else -> null
        }

    override fun toString(): String = buildString {
        append('{')
        (0 until namespaceCount).forEach { i ->
            val ns = namespaces[i]
            maps[i]!!.entries.joinTo(this) {
                "{$ns}${it.key} = \"${it.value}\""
            }
        }
        append('}')
    }
}
