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

package nl.adaptivity.xmlutil.dom

@Suppress("DEPRECATION")
public actual interface PlatformElement : PlatformNode {
    public val namespaceURI: String?
    public val prefix: String?
    public val localName: String
    public val tagName: String

    public val attributes: PlatformNamedNodeMap
    public fun getAttribute(qualifiedName: String): String?
    public fun getAttributeNS(namespace: String?, localName: String): String?

    public fun setAttribute(qualifiedName: String, value: String)
    public fun setAttributeNS(namespace: String?, cName: String, value: String)

    public fun removeAttribute(qualifiedName: String)
    public fun removeAttributeNS(namespace: String?, localName: String)

    public fun hasAttribute(qualifiedName: String): Boolean
    public fun hasAttributeNS(namespace: String?, localName: String): Boolean

    public fun getAttributeNode(qualifiedName: String): PlatformAttr?
    public fun getAttributeNodeNS(namespace: String?, localName: String): PlatformAttr?

    public fun setAttributeNode(attr: PlatformAttr): PlatformAttr?
    public fun setAttributeNodeNS(attr: PlatformAttr): PlatformAttr?
    public fun removeAttributeNode(attr: PlatformAttr): PlatformAttr

    public fun getElementsByTagName(qualifiedName: String): PlatformNodeList
    public fun getElementsByTagNameNS(namespace: String?, localName: String): PlatformNodeList
}

public actual fun PlatformElement.getNamespaceURI(): String? = namespaceURI
public actual fun PlatformElement.getPrefix(): String? = prefix
public actual fun PlatformElement.getLocalName(): String? = localName
public actual fun PlatformElement.getTagName(): String = tagName
public actual fun PlatformElement.getAttributes(): PlatformNamedNodeMap = attributes

internal actual fun PlatformElement.getAttribute(qualifiedName: String): String? = getAttribute(qualifiedName)
internal actual fun PlatformElement.getAttributeNS(namespace: String?, localName: String): String? = getAttributeNS(namespace, localName)

internal actual fun PlatformElement.setAttribute(qualifiedName: String, value: String) = setAttribute(qualifiedName, value)
internal actual fun PlatformElement.setAttributeNS(namespace: String?, cName: String, value: String) = setAttributeNS(namespace, cName, value)

internal actual fun PlatformElement.removeAttribute(qualifiedName: String) = removeAttribute(qualifiedName)
internal actual fun PlatformElement.removeAttributeNS(namespace: String?, localName: String) = removeAttributeNS(namespace, localName)

internal actual fun PlatformElement.hasAttribute(qualifiedName: String): Boolean = hasAttribute(qualifiedName)
internal actual fun PlatformElement.hasAttributeNS(namespace: String?, localName: String): Boolean = hasAttributeNS(namespace, localName)

internal actual fun PlatformElement.getAttributeNode(qualifiedName: String): PlatformAttr? = getAttributeNode(qualifiedName)
internal actual fun PlatformElement.getAttributeNodeNS(namespace: String?, localName: String): PlatformAttr? = getAttributeNodeNS(namespace, localName)

internal actual fun PlatformElement.setAttributeNode(attr: PlatformAttr): PlatformAttr? = setAttributeNode(attr)
internal actual fun PlatformElement.setAttributeNodeNS(attr: PlatformAttr): PlatformAttr? = setAttributeNodeNS(attr)
internal actual fun PlatformElement.removeAttributeNode(attr: PlatformAttr): PlatformAttr = removeAttributeNode(attr)

internal actual fun PlatformElement.getElementsByTagName(qualifiedName: String): PlatformNodeList = getElementsByTagName(qualifiedName)
internal actual fun PlatformElement.getElementsByTagNameNS(namespace: String?, localName: String): PlatformNodeList = getElementsByTagNameNS(namespace, localName)
