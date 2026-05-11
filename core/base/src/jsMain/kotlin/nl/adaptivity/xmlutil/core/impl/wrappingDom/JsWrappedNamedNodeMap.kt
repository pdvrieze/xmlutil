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

import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import org.w3c.dom.Attr as DomAttr
import org.w3c.dom.NamedNodeMap as DomNamedNodeMap

internal class JsWrappedNamedNodeMap(val delegate: DomNamedNodeMap) : NamedNodeMap {
    override val size: Int get() = delegate.length

    @Deprecated("Use size instead", replaceWith = ReplaceWith("size"), level = DeprecationLevel.WARNING)
    override fun getLength(): Int = delegate.length

    override fun item(index: Int): JsWrappedAttr? {
        return delegate.item(index)?.wrapAttr()
    }

    override fun get(index: Int): JsWrappedAttr? {
        return item(index)
    }

    override fun getNamedItem(qualifiedName: String): JsWrappedAttr? {
        return delegate.getNamedItem(qualifiedName)?.wrapAttr()
    }

    override fun getNamedItemNS(namespace: String?, localName: String): JsWrappedAttr? {
        return delegate.getNamedItemNS(namespace, localName)?.wrapAttr()
    }

    fun setNamedItem(attr: DomAttr): JsWrappedAttr? {
        return delegate.setNamedItem(attr.unWrap())?.wrapAttr()
    }

    override fun setNamedItem(attr: PlatformNode): Attr? {
        return delegate.setNamedItem((attr as DomAttr).unWrap())?.wrapAttr()
    }

    fun setNamedItemNS(attr: DomAttr): JsWrappedAttr? {
        return delegate.setNamedItemNS(attr.unWrap())?.wrapAttr()
    }

    override fun setNamedItemNS(attr: PlatformNode): JsWrappedAttr? {
        return delegate.setNamedItemNS((attr as DomAttr).unWrap())?.wrapAttr()
    }

    override fun removeNamedItem(qualifiedName: String): JsWrappedAttr {
        return delegate.removeNamedItem(qualifiedName).wrapAttr()
    }

    override fun removeNamedItemNS(namespace: String?, localName: String): JsWrappedAttr {
        return delegate.removeNamedItemNS(namespace, localName).wrapAttr()
    }

    override fun iterator(): Iterator<JsWrappedAttr> {
        return IteratorImpl(delegate)
    }

    private class IteratorImpl(private val delegate: DomNamedNodeMap) : Iterator<JsWrappedAttr> {
        private var next: Int = 0
        override fun next(): JsWrappedAttr {
            return delegate.item(next++)!!.wrapAttr()
        }

        override fun hasNext(): Boolean {
            return next < delegate.length
        }
    }
}


