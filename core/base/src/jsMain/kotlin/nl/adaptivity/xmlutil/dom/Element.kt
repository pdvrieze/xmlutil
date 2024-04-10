/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.dom

public actual external interface Element : Node {
    public actual fun getAttribute(qualifiedName: String): String?
    public actual fun getAttributeNS(namespace: String?, localName: String): String?
    public actual fun setAttribute(qualifiedName: String, value: String)
    public actual fun setAttributeNS(namespace: String?, cName: String, value: String)
    public actual fun removeAttribute(qualifiedName: String)
    public actual fun removeAttributeNS(namespace: String?, localName: String)
    public actual fun hasAttribute(qualifiedName: String): Boolean
    public actual fun hasAttributeNS(namespace: String?, localName: String): Boolean
    public actual fun getAttributeNode(qualifiedName: String): Attr?
    public actual fun getAttributeNodeNS(namespace: String?, localName: String): Attr?
    public actual fun setAttributeNode(attr: Attr): Attr?
    public actual fun setAttributeNodeNS(attr: Attr): Attr?
    public actual fun removeAttributeNode(attr: Attr): Attr
    public actual fun getElementsByTagName(qualifiedName: String): NodeList
    public actual fun getElementsByTagNameNS(namespace: String?, localName: String): NodeList
}

public actual fun Element.getNamespaceURI(): String? = asDynamic().namespaceURI as String?
public actual fun Element.getPrefix(): String? = asDynamic().prefix as String?
public actual fun Element.getLocalName(): String? = asDynamic().localName as String?
public actual fun Element.getTagName(): String = (asDynamic().tagName as String?) ?: nodeName
public actual fun Element.getAttributes(): NamedNodeMap = asDynamic().attributes

