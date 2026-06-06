/*
 * Copyright (c) 2025-2026.
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

import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformNode

public actual interface Document : Node, PlatformDocument {
    actual override fun getImplementation(): DOMImplementation

    actual override fun getDoctype(): DocumentType?

    actual override fun getDocumentElement(): Element?

    actual override fun createElement(localName: String): Element

    actual override fun createElementNS(namespaceURI: String, qualifiedName: String): Element

    actual override fun createDocumentFragment(): DocumentFragment

    actual override fun createTextNode(data: String): Text

    actual override fun createCDATASection(data: String): CDATASection

    actual override fun createComment(data: String): Comment

    actual override fun createProcessingInstruction(
        target: String,
        data: String
    ): ProcessingInstruction

    actual override fun importNode(node: PlatformNode, deep: Boolean): Node

    actual override fun adoptNode(node: PlatformNode): Node?

    actual override fun createAttribute(localName: String): Attr

    actual override fun createAttributeNS(namespace: String?, qualifiedName: String): Attr

    actual override fun getOwnerDocument(): Nothing?

    actual override fun getNodeValue(): Nothing?

    actual override fun getAttributes(): Nothing?

    public actual fun getElementById(elementId: String): Element?
    public actual fun getElementsByTagName(qualifiedName: String): NodeList
    public actual fun getElementsByTagNameNS(namespace: String?, localName: String): NodeList

    public actual override fun cloneNode(deep: Boolean): Document

    public actual fun setDocumentURI(documentURI: String?)
}

public actual fun Document.importNode(node: PlatformNode, deep: Boolean): Node {
    return importNode(node, deep)
}
