/*
 * Copyright (c) 2022.
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

@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package nl.adaptivity.xmlutil.dom

public expect interface Element : Node {
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

public expect fun Element.getNamespaceURI(): String?
public expect fun Element.getPrefix(): String?
public expect fun Element.getLocalName(): String
public expect fun Element.getTagName(): String
public expect fun Element.getAttributes(): NamedNodeMap


public inline val Element.namespaceURI: String? get(): String? = getNamespaceURI()

public inline val Element.prefix: String? get(): String? = getPrefix()

public inline val Element.localName: String get(): String = getLocalName()

public inline val Element.tagName: String get(): String = getTagName()

public inline val Element.attributes: NamedNodeMap get(): NamedNodeMap = getAttributes()

