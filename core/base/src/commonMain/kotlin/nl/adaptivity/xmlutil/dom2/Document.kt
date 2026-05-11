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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix
import nl.adaptivity.xmlutil.dom2.Element as Element2

public expect interface Document : Node, PlatformDocument {
    override fun getOwnerDocument(): Nothing?

    public fun getImplementation(): DOMImplementation

    public fun getDoctype(): DocumentType?

    public fun getDocumentElement(): Element2?

    public fun getInputEncoding(): String?

    public fun importNode(node: PlatformNode, deep: Boolean /*= false*/): Node

    public fun adoptNode(node: PlatformNode): Node?

    public fun createAttribute(localName: String): Attr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): Attr

    public fun createElement(localName: String): Element2

    public fun createElementNS(namespaceURI: String, qualifiedName: String): Element2

    public fun createDocumentFragment(): DocumentFragment

    public fun createTextNode(data: String): Text

    public fun createCDATASection(data: String): CDATASection

    public fun createComment(data: String): Comment

    public fun createProcessingInstruction(target: String, data: String): ProcessingInstruction

    override fun getNodeValue(): Nothing?

    override fun getAttributes(): Nothing?

    override fun cloneNode(deep: Boolean): Document
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Document.implementation: DOMImplementation get() = getImplementation()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Document.doctype: DocumentType? get() = getDoctype()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Document.documentElement: Element2? get() = getDocumentElement()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Document.inputEncoding: String? get() = getInputEncoding()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use member instead", level = DeprecationLevel.HIDDEN)
public expect fun Document.importNode(node: PlatformNode, deep: Boolean): Node

/**
 * Helper extension for cross platform use. Some implementations have member implementations.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use member instead", level = DeprecationLevel.HIDDEN)
public fun Document.importNode(nodeX: Node): Node = importNode(node = nodeX as PlatformNode, false)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline val Document.characterSet: String? get() = getInputEncoding()

public fun Document.createElementNS(qName: QName): Element2 = when {
    qName.prefix.isEmpty() -> createElementNS(qName.namespaceURI, qName.localPart)
    else -> createElementNS(qName.namespaceURI, "${qName.prefix}:${qName.localPart}")
}

