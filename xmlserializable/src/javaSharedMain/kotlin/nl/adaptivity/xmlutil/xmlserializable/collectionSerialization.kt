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

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.smartStartTag
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * Created by pdvrieze on 27/04/16.
 */

public abstract class BaseSerializableContainer<T : XmlSerializable>(public val name: QName) : XmlSerializable {
    public abstract val children: Iterable<T>

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(name) {
            writeChildren(children)
        }
    }
}

public class SerializableContainer<T : XmlSerializable>(name: QName, delegate: Iterable<T>) :
    BaseSerializableContainer<T>(name), Iterable<T> by delegate {
    override val children: Iterable<T> get() = this
}

@Suppress("unused")
public fun <T : XmlSerializable> Iterable<T>.asSerializable(name: QName): SerializableContainer<T> =
    SerializableContainer(name, this)

public class SerializableCollection<T : XmlSerializable>(name: QName, private val delegate: Collection<T>) :
    BaseSerializableContainer<T>(name), Collection<T> {
    override val children: Iterable<T> get() = this

    override val size: Int get() = delegate.size

    override fun contains(element: T): Boolean = delegate.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean {
        return delegate.containsAll(elements)
    }

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun iterator(): Iterator<T> {
        return delegate.iterator()
    }
}

@Suppress("unused")
public fun <T : XmlSerializable> Collection<T>.asSerializable(name: QName): SerializableCollection<T> =
    SerializableCollection(name, this)

public class SerializableList<T : XmlSerializable>(name: QName, private val delegate: List<T>) :
    BaseSerializableContainer<T>(name), List<T> {
    override val children: Iterable<T> get() = this

    override val size: Int get() = delegate.size

    override fun contains(element: T): Boolean {
        return delegate.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return delegate.containsAll(elements)
    }

    override fun get(index: Int): T = delegate.get(index)

    override fun indexOf(element: T): Int {
        return delegate.indexOf(element)
    }

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun iterator(): MutableIterator<T> = delegate.iterator() as MutableIterator<T>

    override fun lastIndexOf(element: T): Int = delegate.lastIndexOf(element)

    override fun listIterator(): MutableListIterator<T> = delegate.listIterator() as MutableListIterator<T>

    override fun listIterator(index: Int): MutableListIterator<T> = delegate.listIterator(index) as MutableListIterator<T>

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        return delegate.subList(fromIndex, toIndex) as MutableList<T>
    }

    override fun forEach(action: Consumer<in T>?) {
        // Explicit delegation allows the list to optimize this
        delegate.forEach(action)
    }

    override fun spliterator(): Spliterator<T> {
        return delegate.spliterator()
    }

    override fun parallelStream(): Stream<T> {
        return delegate.parallelStream()
    }

    override fun stream(): Stream<T> {
        return delegate.stream()
    }
}

@Suppress("unused")
public fun <T : XmlSerializable> List<T>.asSerializable(name: QName): SerializableList<T> = SerializableList(name, this)
