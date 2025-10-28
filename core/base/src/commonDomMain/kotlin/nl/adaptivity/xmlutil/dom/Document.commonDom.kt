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

import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.implementation
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2

public actual interface Document : Node1 {

    public val implementation: DOMImplementation

    public val doctype: DocumentType?

    public val documentElement: Element?

    public val characterSet: String?

    public val inputEncoding: String? get() = characterSet

    public fun createElement(localName: String): Element

    public fun createElementNS(namespaceURI: String, qualifiedName: String): Element

    public fun createDocumentFragment(): DocumentFragment

    public fun createTextNode(data: String): Text

    public fun createCDATASection(data: String): CDATASection

    public fun createComment(data: String): Comment

    public fun createProcessingInstruction(target: String, data: String): ProcessingInstruction

    public fun importNode(node: Node1): Node1 = importNode(node, false)
    public actual fun importNode(node: Node1, deep: Boolean): Node1

    public actual fun adoptNode(node: Node1): Node1

    public actual fun createAttribute(localName: String): Attr

    public actual fun createAttributeNS(namespace: String?, qualifiedName: String): Attr

}

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Document.getImplementation(): DOMImplementation = implementation

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Document.getDoctype(): DocumentType? = doctype

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Document.getDocumentElement(): Element? = documentElement

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Document.getInputEncoding(): String? = inputEncoding

@Suppress("NOTHING_TO_INLINE")
public actual inline val Document.supportsWhitespaceAtToplevel: Boolean get() = true


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

public actual fun Document2.adoptNode(node: Node1): Node2 = when (node) {
    is INode -> adoptNode(node)
    is Attr -> createAttributeNS(node.namespaceURI, node.name)
    is CDATASection -> createCDATASection(node.data)
    is Comment -> createComment(node.data)
    is Document -> {
        val newDt = node.doctype?.let { dt -> implementation.createDocumentType(dt.name, dt.publicId, dt.systemId) }
        implementation.createDocument(null, getNodeName(), newDt)
    }

    is DocumentFragment -> createDocumentFragment().also { f ->
        for (n in node.childNodes) {
            f.appendChild(adoptNode(n))
        }
    }

    is DocumentType -> implementation.createDocumentType(node.name, node.publicId, node.systemId)
    is Element -> createElementNS(node.namespaceURI ?: "", node.tagName).also { e ->
        for (a in node.getAttributes()) {
            e.setAttributeNS(a.namespaceURI, a.name, a.value)
        }
        for (n in node.getChildNodes()) {
            e.appendChild(adoptNode(n))
        }
    }

    is ProcessingInstruction -> createProcessingInstruction(node.target, node.data)
    is Text -> createTextNode(node.data)
    else -> error("Node type ${NodeType(node.nodeType)} not supported")

}
