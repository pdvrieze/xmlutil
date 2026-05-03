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

import nl.adaptivity.xmlutil.dom.PlatformAttr as DomAttr
import nl.adaptivity.xmlutil.dom2.Attr as Attr2
import nl.adaptivity.xmlutil.dom2.Element as Element2
import org.w3c.dom.Element as DomElement

internal class ElementImpl(delegate: DomElement) : NodeImpl<DomElement>(delegate), Element2 {
    override val ownerDocument: DocumentImpl get() = checkNotNull(super.ownerDocument)

    override fun getNamespaceURI(): String? = delegate.namespaceURI

    override fun getPrefix(): String? = delegate.prefix

    override fun getLocalName(): String = delegate.localName

    override fun getTagName(): String = delegate.tagName

    override fun getNodeValue(): Nothing? = null

    override fun getOwnerDocument(): DocumentImpl {
        return super.getOwnerDocument() as DocumentImpl
    }

    override fun getElementsByTagName(qualifiedName: String): WrappingNodeList {
        return WrappingNodeList(delegate.getElementsByTagName(qualifiedName))
    }

    override fun getElementsByTagNameNS(namespace: String?, localName: String): WrappingNodeList {
        return WrappingNodeList(delegate.getElementsByTagNameNS(namespace, localName))
    }

    override fun getAttributes(): WrappingNamedNodeMap = WrappingNamedNodeMap(delegate.attributes)

    override fun getAttributeNode(qualifiedName: String): Attr2? {
        return delegate.getAttributeNode(qualifiedName)?.wrapAttr()
    }

    override fun getAttributeNodeNS(namespace: String?, localName: String): Attr2? {
        return delegate.getAttributeNodeNS(namespace, localName)?.wrapAttr()
    }

    override fun setAttributeNode(attr: DomAttr): Attr2? {
        return delegate.setAttributeNode(attr.unWrap())?.wrap()
    }

    override fun setAttributeNodeNS(attr: DomAttr): Attr2? =
        delegate.setAttributeNodeNS(attr.unWrap())?.wrap()

    override fun removeAttributeNode(attr: DomAttr): Attr2 =
        delegate.removeAttributeNode(attr.unWrap()).wrap()

    override fun getAttribute(qualifiedName: String): String? =
        delegate.getAttribute(qualifiedName)

    override fun setAttribute(qualifiedName: String, value: String) =
        delegate.setAttribute(qualifiedName, value)

    override fun removeAttribute(qualifiedName: String) = delegate.removeAttribute(qualifiedName)

    override fun getAttributeNS(namespace: String?, localName: String): String? =
        delegate.getAttributeNS(namespace, localName)

    override fun setAttributeNS(namespace: String?, cName: String, value: String) =
        delegate.setAttributeNS(namespace, cName, value)

    override fun removeAttributeNS(namespace: String?, localName: String) =
        delegate.removeAttributeNS(namespace, localName)

    override fun hasAttribute(qualifiedName: String): Boolean = delegate.hasAttribute(qualifiedName)

    override fun hasAttributeNS(namespace: String?, localName: String): Boolean =
        delegate.hasAttributeNS(namespace, localName)
}
