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

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix

public expect interface Document : Node {

    public fun getImplementation(): DOMImplementation

    public fun getDoctype(): DocumentType?

    public fun getDocumentElement(): Element?

    public fun getInputEncoding(): String?

    public fun importNode(node: Node, deep: Boolean /*= false*/): Node

    public fun adoptNode(node: Node): Node

    public fun createAttribute(localName: String): Attr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): Attr

    public fun createElement(localName: String): Element

    public fun createElementNS(namespaceURI: String, qualifiedName: String): Element

    public fun createDocumentFragment(): DocumentFragment

    public fun createTextNode(data: String): Text

    public fun createCDATASection(data: String): CDATASection

    public fun createComment(data: String): Comment

    public fun createProcessingInstruction(target: String, data: String): ProcessingInstruction

}

@Deprecated("Use implementation", ReplaceWith("implementation.supportsWhitespaceAtToplevel"))
public val Document.supportsWhitespaceAtToplevel: Boolean get() = getImplementation().supportsWhitespaceAtToplevel


public inline val Document.implementation: DOMImplementation get() = getImplementation()
public inline val Document.doctype: DocumentType? get() = getDoctype()
public inline val Document.documentElement: Element? get() = getDocumentElement()
public inline val Document.inputEncoding: String? get() = getInputEncoding()

public expect fun Document.importNode(node: PlatformNode, deep: Boolean): Node
public fun Document.importNode(node: Node): Node = importNode(node, false)
public inline val Document.characterSet: String? get() = getInputEncoding()

public fun Document.createElementNS(qName: QName): Element = when {
    qName.prefix.isEmpty() -> createElementNS(qName.namespaceURI, qName.localPart)
    else -> createElementNS(qName.namespaceURI, "${qName.prefix}:${qName.localPart}")
}

