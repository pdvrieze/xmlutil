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

@file:Suppress("DEPRECATION", "EXTENSION_SHADOWED_BY_MEMBER", "KotlinRedundantDiagnosticSuppress")

package nl.adaptivity.xmlutil.dom

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.Element", "nl.adaptivity.xmlutil.dom2")
)
public expect interface PlatformElement : PlatformNode

internal expect fun PlatformElement.getAttribute(qualifiedName: String): String?
internal expect fun PlatformElement.getAttributeNS(namespace: String?, localName: String): String?

internal expect fun PlatformElement.setAttribute(qualifiedName: String, value: String)
internal expect fun PlatformElement.setAttributeNS(namespace: String?, cName: String, value: String)

internal expect fun PlatformElement.removeAttribute(qualifiedName: String)
internal expect fun PlatformElement.removeAttributeNS(namespace: String?, localName: String)

internal expect fun PlatformElement.hasAttribute(qualifiedName: String): Boolean
internal expect fun PlatformElement.hasAttributeNS(namespace: String?, localName: String): Boolean

internal expect fun PlatformElement.getAttributeNode(qualifiedName: String): PlatformAttr?
internal expect fun PlatformElement.getAttributeNodeNS(namespace: String?, localName: String): PlatformAttr?

internal expect fun PlatformElement.setAttributeNode(attr: PlatformAttr): PlatformAttr?
internal expect fun PlatformElement.setAttributeNodeNS(attr: PlatformAttr): PlatformAttr?
internal expect fun PlatformElement.removeAttributeNode(attr: PlatformAttr): PlatformAttr

internal expect fun PlatformElement.getElementsByTagName(qualifiedName: String): PlatformNodeList
internal expect fun PlatformElement.getElementsByTagNameNS(namespace: String?, localName: String): PlatformNodeList

public expect fun PlatformElement.getNamespaceURI(): String?
public expect fun PlatformElement.getPrefix(): String?
public expect fun PlatformElement.getLocalName(): String?
public expect fun PlatformElement.getTagName(): String
public expect fun PlatformElement.getAttributes(): PlatformNamedNodeMap


