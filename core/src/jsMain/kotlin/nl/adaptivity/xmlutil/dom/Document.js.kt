/*
 * Copyright (c) 2023.
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

public actual external interface Document: Node {
    public val implementation: DOMImplementation
    public val doctype: DocumentType?
    public val documentElement: Element?
    public val inputEncoding: String

    public fun createElement(localName: String): Element

    public fun createElementNS(namespaceURI: String, qualifiedName: String): Element

    public fun createDocumentFragment(): DocumentFragment

    public fun createTextNode(data: String): Text

    public fun createCDATASection(data: String): CDATASection

    public fun createComment(data: String): Comment

    public fun createProcessingInstruction(target: String, data: String): ProcessingInstruction
    public actual fun importNode(node: Node, deep: Boolean): Node

    public actual fun adoptNode(node: Node): Node

    public actual fun createAttribute(localName: String): Attr

    public actual fun createAttributeNS(namespace: String?, qualifiedName: String): Attr
}

public actual inline fun Document.getImplementation(): DOMImplementation = implementation
public actual inline fun Document.getDoctype(): DocumentType? = doctype
public actual inline fun Document.getDocumentElement(): Element? = documentElement
public actual inline fun Document.getInputEncoding(): String? = inputEncoding
public actual val Document.supportsWhitespaceAtToplevel: Boolean get() = true

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createElement(localName: String): Element = createElement(localName)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createElementNS(namespaceURI: String, qualifiedName: String): Element =
    createElementNS(namespaceURI, qualifiedName)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createDocumentFragment(): DocumentFragment =
    createDocumentFragment()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createTextNode(data: String): Text =
    createTextNode(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createCDATASection(data: String): CDATASection =
    createCDATASection(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createComment(data: String): Comment =
    createComment(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createProcessingInstruction(target: String, data: String): ProcessingInstruction =
    createProcessingInstruction(target, data)

