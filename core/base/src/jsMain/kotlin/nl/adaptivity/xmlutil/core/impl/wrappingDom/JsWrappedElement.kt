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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom.PlatformAttr as DomAttr
import org.w3c.dom.Element as DomElement

internal class JsWrappedElement(delegate: DomElement) : JsWrappedNode<DomElement>(delegate), Element {

    override fun getNamespaceURI(): String? = delegate.namespaceURI

    override fun getPrefix(): String? = delegate.prefix

    override fun getLocalName(): String = delegate.localName

    override fun getTagName(): String = delegate.tagName

    override fun getNodeValue(): Nothing? = null

    override val nodeType: Short get() = delegate.nodeType

    @ExperimentalXmlUtilApi
    override fun setNodeValue(value: String?) {}

    override fun getOwnerDocument(): JsWrappedDocument {
        return super.getOwnerDocument() as JsWrappedDocument
    }

    override fun getElementsByTagName(qualifiedName: String): JsWrappedNodeList {
        return JsWrappedNodeList(delegate.getElementsByTagName(qualifiedName))
    }

    override fun getElementsByTagNameNS(namespace: String?, localName: String): JsWrappedNodeList {
        return JsWrappedNodeList(delegate.getElementsByTagNameNS(namespace, localName))
    }

    override fun getAttributes(): JsWrappedNamedNodeMap = JsWrappedNamedNodeMap(delegate.attributes)

    @ExperimentalXmlUtilApi
    override fun hasAttributes(): Boolean = delegate.hasAttributes()

    override fun getAttributeNode(qualifiedName: String): JsWrappedAttr? {
        return delegate.getAttributeNode(qualifiedName)?.wrapAttr()
    }

    override fun getAttributeNodeNS(namespace: String?, localName: String): JsWrappedAttr? {
        return delegate.getAttributeNodeNS(namespace, localName)?.wrapAttr()
    }

    override fun setAttributeNode(attr: DomAttr): JsWrappedAttr? {
        return delegate.setAttributeNode(attr.unWrap())?.wrap()
    }

    override fun setAttributeNodeNS(attr: DomAttr): JsWrappedAttr? =
        delegate.setAttributeNodeNS(attr.unWrap())?.wrap()

    override fun removeAttributeNode(attr: DomAttr): JsWrappedAttr =
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

    override fun cloneNode(deep: Boolean): JsWrappedElement {
        return JsWrappedElement(delegate.cloneNode(deep) as DomElement)
    }
}
