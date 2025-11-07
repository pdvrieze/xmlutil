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

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix
import nl.adaptivity.xmlutil.dom.PlatformNode as Node1
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2

public actual interface PlatformDocument : Node1 {

    public fun getImplementation(): PlatformDOMImplementation

    public fun getDoctype(): PlatformDocumentType?

    public fun getDocumentElement(): PlatformElement?

    public val characterSet: String?

    public fun getInputEncoding(): String?

    public fun createElement(localName: String): PlatformElement

    public fun createElementNS(namespaceURI: String, qualifiedName: String): PlatformElement

    public fun Document.createElementNS(qName: QName): Element = when {
        qName.prefix.isEmpty() -> createElementNS(qName.namespaceURI, qName.localPart)
        else -> createElementNS(qName.namespaceURI, "${qName.prefix}:${qName.localPart}")
    }

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
    is PlatformAttr -> createAttributeNS(node.getNamespaceURI(), node.getName())
    is PlatformCDATASection -> createCDATASection(node.getData())
    is PlatformComment -> createComment(node.getData())
    is PlatformDocument -> {
        val newDt = node.getDoctype()
            ?.let { dt -> getImplementation().createDocumentType(dt.getName(), dt.getPublicId(), dt.getSystemId()) }
        getImplementation().createDocument(null, getNodeName(), newDt)
    }

    is PlatformDocumentFragment -> createDocumentFragment().also { f ->
        for (n in node.getChildNodes()) {
            f.appendChild(adoptNode(n))
        }
    }

    is PlatformDocumentType -> getImplementation().createDocumentType(node.getName(), node.getPublicId(), node.getSystemId())
    is PlatformElement -> createElementNS(node.getNamespaceURI() ?: "", node.getTagName()).also { e ->
        for (a in node.getAttributes()) {
            e.setAttributeNS(a.getNamespaceURI(), a.getName(), a.getValue())
        }
        for (n in node.getChildNodes()) {
            e.appendChild(adoptNode(n))
        }
    }

    is PlatformProcessingInstruction -> createProcessingInstruction(node.getTarget(), node.getData())
    is PlatformText -> createTextNode(node.getData())
    else -> error("Node type ${node.getNodetype()} not supported")

}
