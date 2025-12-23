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
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix

public actual interface PlatformDocument : PlatformNode {

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

    public fun importNode(node: PlatformNode): PlatformNode = importNode(node, false)
    public fun importNode(node: PlatformNode, deep: Boolean): PlatformNode

    public fun adoptNode(node: PlatformNode): PlatformNode

    public fun createAttribute(localName: String): PlatformAttr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): PlatformAttr

}


public actual fun Document.adoptNode(node: PlatformNode): Node = adoptNode(node)
