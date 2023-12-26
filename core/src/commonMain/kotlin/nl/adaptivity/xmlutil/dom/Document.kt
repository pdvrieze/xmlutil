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

import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.Document", "nl.adaptivity.xmlutil.dom2")
)
public expect interface Document : Node {

    public fun importNode(node: Node, deep: Boolean): Node

    public fun adoptNode(node: Node): Node

    public fun createAttribute(localName: String): Attr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): Attr

}

public expect fun Document.createElement(localName: String): Element

public expect fun Document.createElementNS(namespaceURI: String, qualifiedName: String): Element

public expect fun Document.createDocumentFragment(): DocumentFragment

public expect fun Document.createTextNode(data: String): Text

public expect fun Document.createCDATASection(data: String): CDATASection

public expect fun Document.createComment(data: String): Comment

public expect fun Document.createProcessingInstruction(target: String, data: String): ProcessingInstruction


public expect inline fun Document.getImplementation(): DOMImplementation
public expect inline fun Document.getDoctype(): DocumentType?
public expect inline fun Document.getDocumentElement(): Element?
public expect inline fun Document.getInputEncoding(): String?

public inline val Document.characterSet: String? get() = getInputEncoding()

public fun Document.importNode(node: Node): Node = importNode(node, false)

public inline val Document.implementation: DOMImplementation get() = getImplementation()

public inline val Document.doctype: DocumentType? get() = getDoctype()

public inline val Document.documentElement: Element? get() = getDocumentElement()

public inline val Document.inputEncoding: String? get() = getInputEncoding()

public expect val Document.supportsWhitespaceAtToplevel: Boolean

public expect fun Document2.adoptNode(node: Node): Node2
