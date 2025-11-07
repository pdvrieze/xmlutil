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

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.Serializable

@Serializable(ElementSerializer::class)
public expect interface Element : Node {

    public fun getNamespaceURI(): String?

    public fun getPrefix(): String?

    public fun getLocalName(): String

    public fun getTagName(): String

    public fun getAttributes(): NamedNodeMap


    public fun getAttribute(qualifiedName: String): String?
    public fun getAttributeNS(namespace: String?, localName: String): String?

    public fun setAttribute(qualifiedName: String, value: String)
    public fun setAttributeNS(namespace: String?, cName: String, value: String)

    public fun removeAttribute(qualifiedName: String)
    public fun removeAttributeNS(namespace: String?, localName: String)

    public fun hasAttribute(qualifiedName: String): Boolean
    public fun hasAttributeNS(namespace: String?, localName: String): Boolean

    public fun getAttributeNode(qualifiedName: String): Attr?
    public fun getAttributeNodeNS(namespace: String?, localName: String): Attr?

    public fun setAttributeNode(attr: Attr): Attr?
    public fun setAttributeNodeNS(attr: Attr): Attr?
    public fun removeAttributeNode(attr: Attr): Attr

    public fun getElementsByTagName(qualifiedName: String): NodeList
    public fun getElementsByTagNameNS(namespace: String?, localName: String): NodeList
}

public val Element.namespaceURI: String? get() = getNamespaceURI()
public val Element.prefix: String? get() = getPrefix()
public val Element.localName: String get() = getLocalName()
public val Element.tagName: String get() = getTagName()
public val Element.attributes: NamedNodeMap get() = getAttributes()
