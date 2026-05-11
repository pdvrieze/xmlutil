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
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.NamedNodeMap

internal class WrappingNamedNodeMap(val delegate: PlatformNamedNodeMap) : NamedNodeMap {
    override val size: Int get() = delegate.length

    override fun item(index: Int): AttrImpl? {
        return delegate.item(index)?.wrapAttr()
    }

    override fun getNamedItem(qualifiedName: String): AttrImpl? {
        return delegate.getNamedItem(qualifiedName)?.wrapAttr()
    }

    override fun getNamedItemNS(namespace: String?, localName: String): AttrImpl? {
        return delegate.getNamedItemNS(namespace, localName)?.wrapAttr()
    }

    override fun setNamedItem(attr: PlatformNode): Attr? {
        return delegate.setNamedItem(attr.unWrap())?.wrapAttr()
    }

    override fun setNamedItemNS(attr: PlatformNode): AttrImpl? {
        return delegate.setNamedItemNS(attr.unWrap())?.wrapAttr()
    }

    override fun removeNamedItem(qualifiedName: String): AttrImpl? {
        return delegate.removeNamedItem(qualifiedName)?.wrapAttr()
    }

    override fun removeNamedItemNS(namespace: String?, localName: String): AttrImpl? {
        return delegate.removeNamedItemNS(namespace, localName)?.wrapAttr()
    }

    override fun iterator(): Iterator<AttrImpl> {
        return IteratorImpl(delegate)
    }

    @Deprecated("Use size instead", ReplaceWith("size"))
    public override fun getLength(): Int = size

    override fun get(index: Int): AttrImpl? = item(index)

    private class IteratorImpl(private val delegate: PlatformNamedNodeMap) : Iterator<AttrImpl> {
        private var next: Int = 0
        override fun next(): AttrImpl {
            return delegate.item(next++).wrapAttr()
        }

        override fun hasNext(): Boolean {
            return next < delegate.length
        }
    }
}


