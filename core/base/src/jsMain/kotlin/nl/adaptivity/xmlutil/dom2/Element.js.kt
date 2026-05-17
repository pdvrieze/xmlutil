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

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformElement

@Serializable(with = ElementSerializer::class)
public actual interface Element : Node, PlatformElement {
    override val ownerDocument: Document get() = getOwnerDocument()

    public actual fun getTagName(): String
    public actual override fun getAttributes(): NamedNodeMap<Attr>
    public actual override fun getAttribute(qualifiedName: String): String?
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

    public actual override fun getOwnerDocument(): Document
    public actual override fun getNodeValue(): Nothing?
    public actual override fun cloneNode(deep: Boolean): Element
}
