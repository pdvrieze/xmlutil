/*
 * Copyright (c) 2025-2026.
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

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformElement
import org.w3c.dom.TypeInfo

@Serializable(with = ElementSerializer::class)
public actual interface Element : Node, PlatformElement {
    public actual override fun getNamespaceURI(): String?
    public actual override fun getPrefix(): String?
    public actual override fun getLocalName(): String
    public actual override fun getTagName(): String
    public actual override fun getAttributes(): NamedNodeMap
    @Suppress("WRONG_TYPE_FOR_JAVA_OVERRIDE") // we don't return empty value if missing
    public actual override fun getAttribute(qualifiedName: String): String?
    @Suppress("WRONG_TYPE_FOR_JAVA_OVERRIDE") // we don't return empty value if missing
    public actual override fun getAttributeNS(namespace: String?, localName: String): String?
    public actual override fun setAttribute(qualifiedName: String, value: String)
    public actual override fun setAttributeNS(namespace: String?, cName: String, value: String)
    public actual override fun removeAttribute(qualifiedName: String)
    public actual override fun removeAttributeNS(namespace: String?, localName: String)
    public actual override fun hasAttribute(qualifiedName: String): Boolean
    public actual override fun hasAttributeNS(namespace: String?, localName: String): Boolean
    public actual override fun getAttributeNode(qualifiedName: String): Attr?
    public actual override fun getAttributeNodeNS(namespace: String?, localName: String): Attr?
    public actual override fun setAttributeNode(attr: PlatformAttr): Attr?
    public actual override fun setAttributeNodeNS(attr: PlatformAttr): Attr?
    public actual override fun removeAttributeNode(attr: PlatformAttr): Attr
    public actual override fun getElementsByTagName(qualifiedName: String): NodeList
    public actual override fun getElementsByTagNameNS(
        namespace: String?,
        localName: String
    ): NodeList

    actual override fun getNodeValue(): Nothing?
    actual override fun getOwnerDocument(): Document

    override fun cloneNode(deep: Boolean): Element {
        val e = when (val u = namespaceURI) {
            null, "" -> ownerDocument.createElement(localName)
            else -> ownerDocument.createElementNS(u, tagName)
        }
        for (a in attributes) when (val n = a.namespaceURI) {
            null, "" -> setAttribute(a.name, a.value)
            else -> setAttributeNS(n, a.name, a.value)
        }

        if (deep) for (c in getChildNodes()) e.appendChild(c.cloneNode(true))
        return e
    }

    override fun hasAttributes(): Boolean = attributes.size > 0

    @Deprecated("No-op for now")
    override fun setNodeValue(nodeValue: String?) {}

    override fun getSchemaTypeInfo(): TypeInfo? = null

    @Deprecated("Not implemented")
    override fun setIdAttribute(name: String, isId: Boolean) {
        throw UnsupportedOperationException("setIdAttribute is not supported yet")
    }

    @Deprecated("Not implemented")
    override fun setIdAttributeNS(
        namespaceURI: String?,
        localName: String,
        isId: Boolean
    ) {
        throw UnsupportedOperationException("setIdAttribute is not supported yet")
    }

    @Deprecated("No-op for now")
    override fun setIdAttributeNode(idAttr: org.w3c.dom.Attr?, isId: Boolean) {
        throw UnsupportedOperationException("setIdAttribute is not supported yet")
    }
}
