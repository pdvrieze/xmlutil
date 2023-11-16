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

@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER")

package nl.adaptivity.xmlutil.dom

public actual typealias Node = org.w3c.dom.Node

public actual typealias Attr = org.w3c.dom.Attr

public actual typealias DocumentFragment = org.w3c.dom.DocumentFragment

public actual typealias Element = org.w3c.dom.Element

public actual typealias Text = org.w3c.dom.Text

public actual typealias CharacterData = org.w3c.dom.CharacterData

public actual typealias CDATASection = org.w3c.dom.CDATASection

public actual typealias Comment = org.w3c.dom.Comment

public actual typealias ProcessingInstruction = org.w3c.dom.ProcessingInstruction

public actual typealias DOMImplementation = org.w3c.dom.DOMImplementation

public actual typealias DocumentType = org.w3c.dom.DocumentType

public actual typealias NamedNodeMap = org.w3c.dom.NamedNodeMap

public actual typealias NodeList = org.w3c.dom.NodeList

public actual inline fun Node.getNodeType(): Short = nodeType
public actual inline fun Node.getNodeName(): String = nodeName
public actual inline fun Node.getOwnerDocument(): Document = ownerDocument
public actual inline fun Node.getParentNode(): Node? = parentNode
public actual inline fun Node.getTextContent(): String? = textContent
public actual inline fun Node.getChildNodes(): NodeList = childNodes
public actual inline fun Node.getFirstChild(): Node? = firstChild
public actual inline fun Node.getLastChild(): Node? = lastChild
public actual inline fun Node.getPreviousSibling(): Node? = previousSibling
public actual inline fun Node.getNextSibling(): Node? = nextSibling

public actual inline fun Attr.getNamespaceURI(): String? = namespaceURI
public actual inline fun Attr.getPrefix(): String? = prefix
public actual inline fun Attr.getLocalName(): String? = localName
public actual inline fun Attr.getName(): String = name
public actual inline fun Attr.getValue(): String = value
public actual inline fun Attr.setValue(value: String) { this.value = value }
public actual inline fun Attr.getOwnerElement(): Element? = ownerElement

public actual inline fun CharacterData.getData(): String = data
public actual inline fun CharacterData.setData(value: String) { data = value }

public actual inline fun Document.getImplementation(): DOMImplementation = implementation
public actual inline fun Document.getDoctype(): DocumentType? = doctype
public actual inline fun Document.getDocumentElement(): Element? = documentElement
public actual inline fun Document.getInputEncoding(): String? = inputEncoding

public actual fun Element.getNamespaceURI(): String? = namespaceURI
public actual fun Element.getPrefix(): String? = prefix
public actual fun Element.getLocalName(): String? = localName
public actual fun Element.getTagName(): String = tagName
public actual fun Element.getAttributes(): NamedNodeMap = attributes

public actual inline fun NamedNodeMap.getLength(): Int = length
public actual inline fun NodeList.getLength(): Int = length

public actual inline fun ProcessingInstruction.getTarget(): String = target
public actual inline fun ProcessingInstruction.getData(): String = data
public actual inline fun ProcessingInstruction.setData(data: String) { this.data = data }
public actual val Document.supportsWhitespaceAtToplevel: Boolean get() = false

