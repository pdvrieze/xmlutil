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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformElement
import nl.adaptivity.xmlutil.dom2.Element
import org.w3c.dom.TypeInfo

internal class ElementImpl(delegate: PlatformElement) : AbstractNodeImpl<PlatformElement>(delegate), Element {
    override fun getLocalName(): String = delegate.localName ?: delegate.tagName

    override fun getTagName(): String = delegate.tagName

    override fun getNodeValue(): Nothing? = null

    override fun getOwnerDocument(): DocumentImpl {
        return checkNotNull(super.getOwnerDocument())
    }

    override fun getElementsByTagName(qualifiedName: String): WrappingNodeList {
        return WrappingNodeList(delegate.getElementsByTagName(qualifiedName))
    }

    override fun getElementsByTagNameNS(namespace: String?, localName: String): WrappingNodeList {
        return WrappingNodeList(delegate.getElementsByTagNameNS(namespace, localName))
    }

    override fun getSchemaTypeInfo(): TypeInfo = delegate.schemaTypeInfo

    override fun getAttributes(): WrappingNamedNodeMap = WrappingNamedNodeMap(delegate.attributes)

    override fun getAttributeNode(qualifiedName: String): AttrImpl? {
        return delegate.getAttributeNode(qualifiedName)?.wrapAttr()
    }

    override fun getAttributeNodeNS(namespace: String?, localName: String): AttrImpl? {
        return delegate.getAttributeNodeNS(namespace, localName)?.wrapAttr()
    }

    override fun setAttributeNode(attr: PlatformAttr): AttrImpl? {
        return delegate.setAttributeNode(attr.unWrap())?.wrap()
    }

    override fun setAttributeNodeNS(attr: PlatformAttr): AttrImpl? {
        return delegate.setAttributeNodeNS(attr.unWrap())?.wrap()
    }

    override fun removeAttributeNode(attr: PlatformAttr): AttrImpl =
        delegate.removeAttributeNode(attr.unWrap()).wrap()

    override fun getAttribute(qualifiedName: String): String? =
        delegate.getAttribute(qualifiedName)

    override fun setAttribute(qualifiedName: String, value: String) =
        delegate.setAttribute(qualifiedName, value)

    override fun removeAttribute(qualifiedName: String) = delegate.removeAttribute(qualifiedName)

    override fun getAttributeNS(namespace: String?, localName: String): String? =
        delegate.getAttributeNS(namespace?.takeIf { it.isNotEmpty() }, localName)

    override fun setAttributeNS(namespace: String?, cName: String, value: String) =
        delegate.setAttributeNS(namespace, cName, value)

    override fun removeAttributeNS(namespace: String?, localName: String) =
        delegate.removeAttributeNS(namespace, localName)

    override fun hasAttribute(qualifiedName: String): Boolean = delegate.hasAttribute(qualifiedName)

    override fun hasAttributeNS(namespace: String?, localName: String): Boolean =
        delegate.hasAttributeNS(namespace, localName)

    override fun setIdAttribute(name: String, isId: Boolean) {
        delegate.setIdAttribute(name, isId)
    }

    override fun setIdAttributeNS(namespaceURI: String?, localName: String, isId: Boolean) {
        delegate.setIdAttributeNS(namespaceURI, localName, isId)
    }

    override fun setIdAttributeNode(idAttr: PlatformAttr?, isId: Boolean) {
        delegate.setIdAttributeNode(idAttr, isId)
    }
}
