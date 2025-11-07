/*
 * Copyright (c) 2025.
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

public actual interface Document : Node {
    public actual fun getImplementation(): DOMImplementation
    public actual fun getDoctype(): DocumentType?
    public actual fun getDocumentElement(): Element?
    public actual fun getInputEncoding(): String?
    public actual fun importNode(node: Node, deep: Boolean): Node
    public actual fun adoptNode(node: Node): Node
    public actual fun createAttribute(localName: String): Attr
    public actual fun createAttributeNS(namespace: String?, qualifiedName: String): Attr
    public actual fun createElement(localName: String): Element
    public actual fun createElementNS(namespaceURI: String, qualifiedName: String): Element
    public actual fun createDocumentFragment(): DocumentFragment
    public actual fun createTextNode(data: String): Text
    public actual fun createCDATASection(data: String): CDATASection
    public actual fun createComment(data: String): Comment
    public actual fun createProcessingInstruction(target: String, data: String): ProcessingInstruction
}
