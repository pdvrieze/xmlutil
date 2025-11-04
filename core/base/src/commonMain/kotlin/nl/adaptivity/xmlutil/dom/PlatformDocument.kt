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

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.Document", "nl.adaptivity.xmlutil.dom2")
)
public expect interface PlatformDocument : PlatformNode

@XmlUtilInternal
public expect inline fun PlatformDocument.createElement(localName: String): PlatformElement

@XmlUtilInternal
public expect inline fun PlatformDocument.createElementNS(namespaceURI: String, qualifiedName: String): PlatformElement

@XmlUtilInternal
public expect inline fun PlatformDocument.createDocumentFragment(): PlatformDocumentFragment

@XmlUtilInternal
public expect inline fun PlatformDocument.createTextNode(data: String): PlatformText

@XmlUtilInternal
public expect inline fun PlatformDocument.createCDATASection(data: String): PlatformCDATASection

@XmlUtilInternal
public expect inline fun PlatformDocument.createComment(data: String): PlatformComment

@XmlUtilInternal
public expect inline fun PlatformDocument.createProcessingInstruction(target: String, data: String): PlatformProcessingInstruction

@XmlUtilInternal
public expect inline fun PlatformDocument.importNode(node: PlatformNode, deep: Boolean): PlatformNode

@XmlUtilInternal
public expect inline fun PlatformDocument.adoptNode(node: PlatformNode): PlatformNode

@XmlUtilInternal
public expect inline fun PlatformDocument.createAttribute(localName: String): PlatformAttr

@XmlUtilInternal
public expect inline fun PlatformDocument.createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr

public expect inline fun PlatformDocument.getImplementation(): PlatformDOMImplementation
public expect inline fun PlatformDocument.getDoctype(): PlatformDocumentType?
public expect inline fun PlatformDocument.getDocumentElement(): PlatformElement?
public expect inline fun PlatformDocument.getInputEncoding(): String?

public inline val PlatformDocument.characterSet: String? get() = getInputEncoding()

public fun PlatformDocument.importNode(node: PlatformNode): PlatformNode = importNode(node, false)

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getImplementation()"))
public inline val PlatformDocument.implementation: PlatformDOMImplementation get() = getImplementation()

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getDoctype()"))
public inline val PlatformDocument.doctype: PlatformDocumentType? get() = getDoctype()

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getDocumentElement()"))
public inline val PlatformDocument.documentElement: PlatformElement? get() = getDocumentElement()

@Deprecated("Use accessor methods for dom2 compatibility", ReplaceWith("getInputEncoding()"))
public inline val PlatformDocument.inputEncoding: String? get() = getInputEncoding()

public expect val PlatformDocument.supportsWhitespaceAtToplevel: Boolean

public expect fun Document2.adoptNode(node: PlatformNode): Node2
