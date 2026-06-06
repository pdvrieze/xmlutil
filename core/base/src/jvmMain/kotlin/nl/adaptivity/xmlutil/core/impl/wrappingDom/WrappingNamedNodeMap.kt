/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.PlatformNamedNodeMap
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.NamedNodeMap

internal class WrappingNamedNodeMap<out T : WrappedJvmNode<*>>(val delegate: PlatformNamedNodeMap) :
    NamedNodeMap<T> {

    override val size: Int get() = delegate.length

    override fun item(index: Int): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate.item(index)?.wrap() as T?
    }

    override fun getNamedItem(qualifiedName: String): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate.getNamedItem(qualifiedName)?.wrap() as T?
    }

    override fun getNamedItemNS(namespace: String?, localName: String): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate.getNamedItemNS(namespace, localName)?.wrap() as T?
    }

    override fun setNamedItem(attr: PlatformNode): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate.setNamedItem(attr.unWrap())?.wrap() as T?
    }

    override fun setNamedItemNS(attr: PlatformNode): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate.setNamedItemNS(attr.unWrap())?.wrap() as T?
    }

    override fun removeNamedItem(qualifiedName: String): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate.removeNamedItem(qualifiedName)?.wrap() as T?
    }

    override fun removeNamedItemNS(namespace: String?, localName: String): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate.removeNamedItemNS(namespace, localName)?.wrap() as T?
    }

    override fun iterator(): Iterator<T> {
        return IteratorImpl(delegate)
    }

    @Deprecated("Use size instead", ReplaceWith("size"))
    override fun getLength(): Int = size

    override fun get(index: Int): T? = item(index)

    private class IteratorImpl<T : WrappedJvmNode<*>>(private val delegate: PlatformNamedNodeMap) : Iterator<T> {
        private var next: Int = 0
        override fun next(): T {
            @Suppress("UNCHECKED_CAST")
            return delegate.item(next++).wrap() as T
        }

        override fun hasNext(): Boolean {
            return next < delegate.length
        }
    }
}


