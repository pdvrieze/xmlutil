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

@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.implementation
import nl.adaptivity.xmlutil.dom.PlatformNode as Node1
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2

public actual interface PlatformDocument : Node1 {

    public val implementation: PlatformDOMImplementation

    public val doctype: PlatformDocumentType?

    public val documentElement: PlatformElement?

    public val characterSet: String?

    public val inputEncoding: String? get() = characterSet

    public fun createElement(localName: String): PlatformElement

    public fun createElementNS(namespaceURI: String, qualifiedName: String): PlatformElement

    public fun createDocumentFragment(): PlatformDocumentFragment

    public fun createTextNode(data: String): PlatformText

    public fun createCDATASection(data: String): PlatformCDATASection

    public fun createComment(data: String): PlatformComment

    public fun createProcessingInstruction(target: String, data: String): PlatformProcessingInstruction

    public fun importNode(node: Node1): Node1 = importNode(node, false)
    public fun importNode(node: Node1, deep: Boolean): Node1

    public fun adoptNode(node: Node1): Node1

    public fun createAttribute(localName: String): PlatformAttr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr

}


public actual fun Document2.adoptNode(node: Node1): Node2 = when (node) {
    is INode -> adoptNode(node)
    is PlatformAttr -> createAttributeNS(node.namespaceURI, node.name)
    is PlatformCDATASection -> createCDATASection(node.data)
    is PlatformComment -> createComment(node.data)
    is PlatformDocument -> {
        val newDt = node.doctype?.let { dt -> implementation.createDocumentType(dt.name, dt.publicId, dt.systemId) }
        implementation.createDocument(null, getNodeName(), newDt)
    }

    is PlatformDocumentFragment -> createDocumentFragment().also { f ->
        for (n in node.childNodes) {
            f.appendChild(adoptNode(n))
        }
    }

    is PlatformDocumentType -> implementation.createDocumentType(node.name, node.publicId, node.systemId)
    is PlatformElement -> createElementNS(node.namespaceURI ?: "", node.tagName).also { e ->
        for (a in node.attributes) {
            e.setAttributeNS(a.namespaceURI, a.name, a.value)
        }
        for (n in node.childNodes) {
            e.appendChild(adoptNode(n))
        }
    }

    is PlatformProcessingInstruction -> createProcessingInstruction(node.target, node.data)
    is PlatformText -> createTextNode(node.data)
    else -> error("Node type ${NodeType(node.nodeType)} not supported")

}
