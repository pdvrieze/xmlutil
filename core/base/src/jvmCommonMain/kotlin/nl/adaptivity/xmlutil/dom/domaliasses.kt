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

@file:Suppress(
    "NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER",
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING"
)

package nl.adaptivity.xmlutil.dom

public actual typealias PlatformNode = org.w3c.dom.Node

public actual typealias PlatformAttr = org.w3c.dom.Attr

public actual typealias PlatformDocumentFragment = org.w3c.dom.DocumentFragment

public actual typealias PlatformElement = org.w3c.dom.Element

public actual typealias PlatformText = org.w3c.dom.Text

public actual typealias PlatformCharacterData = org.w3c.dom.CharacterData

public actual typealias PlatformCDATASection = org.w3c.dom.CDATASection

public actual typealias PlatformComment = org.w3c.dom.Comment

public actual typealias PlatformProcessingInstruction = org.w3c.dom.ProcessingInstruction

public actual typealias PlatformDOMImplementation = org.w3c.dom.DOMImplementation

public actual typealias PlatformDocumentType = org.w3c.dom.DocumentType

public actual typealias PlatformNamedNodeMap = org.w3c.dom.NamedNodeMap

public actual typealias PlatformNodeList = org.w3c.dom.NodeList

public actual inline fun PlatformNode.getNodeType(): Short = nodeType
public actual inline fun PlatformNode.getNodeName(): String = nodeName
public actual inline fun PlatformNode.getOwnerDocument(): PlatformDocument = ownerDocument
public actual inline fun PlatformNode.getParentNode(): PlatformNode? = parentNode
public actual inline fun PlatformNode.getTextContent(): String? = textContent
public actual inline fun PlatformNode.getChildNodes(): PlatformNodeList = childNodes
public actual inline fun PlatformNode.getFirstChild(): PlatformNode? = firstChild
public actual inline fun PlatformNode.getLastChild(): PlatformNode? = lastChild
public actual inline fun PlatformNode.getPreviousSibling(): PlatformNode? = previousSibling
public actual inline fun PlatformNode.getNextSibling(): PlatformNode? = nextSibling

public actual inline fun PlatformAttr.getNamespaceURI(): String? = namespaceURI
public actual inline fun PlatformAttr.getPrefix(): String? = prefix
public actual inline fun PlatformAttr.getLocalName(): String? = localName
public actual inline fun PlatformAttr.getName(): String = name

internal actual inline fun PlatformAttr.getValue(): String = value
internal actual inline fun PlatformAttr.setValue(value: String) {
    this.value = value
}

internal actual inline fun PlatformAttr.getOwnerElement(): PlatformElement? = ownerElement

public actual inline fun PlatformCharacterData.getData(): String = data
public actual inline fun PlatformCharacterData.setData(value: String) {
    data = value
}

public actual inline fun PlatformDocument.getImplementation(): PlatformDOMImplementation = implementation
public actual inline fun PlatformDocument.getDoctype(): PlatformDocumentType? = doctype
public actual inline fun PlatformDocument.getDocumentElement(): PlatformElement? = documentElement
public actual inline fun PlatformDocument.getInputEncoding(): String? = inputEncoding

public actual fun PlatformElement.getNamespaceURI(): String? = namespaceURI
public actual fun PlatformElement.getPrefix(): String? = prefix
public actual fun PlatformElement.getLocalName(): String? = localName
public actual fun PlatformElement.getTagName(): String = tagName
public actual fun PlatformElement.getAttributes(): PlatformNamedNodeMap = attributes
internal actual fun PlatformElement.getAttribute(qualifiedName: String): String? = getAttribute(qualifiedName)
internal actual fun PlatformElement.getAttributeNS(namespace: String?, localName: String): String? = getAttributeNS(namespace, localName)

internal actual fun PlatformElement.setAttribute(qualifiedName: String, value: String) = setAttribute(qualifiedName, value)
internal actual fun PlatformElement.setAttributeNS(namespace: String?, cName: String, value: String) = setAttributeNS(namespace, cName, value)

internal actual fun PlatformElement.removeAttribute(qualifiedName: String) = removeAttribute(qualifiedName)
internal actual fun PlatformElement.removeAttributeNS(namespace: String?, localName: String) = removeAttributeNS(namespace, localName)

internal actual fun PlatformElement.hasAttribute(qualifiedName: String): Boolean = hasAttribute(qualifiedName)
internal actual fun PlatformElement.hasAttributeNS(namespace: String?, localName: String): Boolean = hasAttributeNS(namespace, localName)

internal actual fun PlatformElement.getAttributeNode(qualifiedName: String): PlatformAttr? = getAttributeNode(qualifiedName)
internal actual fun PlatformElement.getAttributeNodeNS(namespace: String?, localName: String): PlatformAttr? = getAttributeNodeNS(namespace, localName)

internal actual fun PlatformElement.setAttributeNode(attr: PlatformAttr): PlatformAttr? = setAttributeNode(attr)
internal actual fun PlatformElement.setAttributeNodeNS(attr: PlatformAttr): PlatformAttr? = setAttributeNodeNS(attr)
internal actual fun PlatformElement.removeAttributeNode(attr: PlatformAttr): PlatformAttr = removeAttributeNode(attr)

internal actual fun PlatformElement.getElementsByTagName(qualifiedName: String): PlatformNodeList = getElementsByTagName(qualifiedName)
internal actual fun PlatformElement.getElementsByTagNameNS(namespace: String?, localName: String): PlatformNodeList = getElementsByTagNameNS(namespace, localName)

public actual inline fun PlatformNamedNodeMap.getLength(): Int = length
public actual fun PlatformNamedNodeMap.item(index: Int): PlatformNode? = item(index)
public actual fun PlatformNamedNodeMap.getNamedItem(qualifiedName: String): PlatformNode? = getNamedItem(qualifiedName)
public actual fun PlatformNamedNodeMap.getNamedItemNS(namespace: String?, localName: String): PlatformNode? = getNamedItemNS(namespace, localName)
public actual fun PlatformNamedNodeMap.setNamedItem(attr: PlatformNode): PlatformNode? = setNamedItem(attr)
public actual fun PlatformNamedNodeMap.setNamedItemNS(attr: PlatformNode): PlatformNode? = setNamedItemNS(attr)
public actual fun PlatformNamedNodeMap.removeNamedItem(qualifiedName: String): PlatformNode? = removeNamedItem(qualifiedName)
public actual fun PlatformNamedNodeMap.removeNamedItemNS(namespace: String?, localName: String): PlatformNode? = removeNamedItemNS(namespace, localName)


public actual inline fun PlatformNodeList.getLength(): Int = length
public actual operator fun PlatformNodeList.get(index: Int): PlatformNode? = item(index)

public actual inline fun PlatformProcessingInstruction.getTarget(): String = target

public actual inline fun PlatformProcessingInstruction.getData(): String = data
public actual inline fun PlatformProcessingInstruction.setData(data: String) {
    this.data = data
}

public actual val PlatformDocument.supportsWhitespaceAtToplevel: Boolean get() = false


public actual fun PlatformDOMImplementation.createDocumentType(qualifiedName: String, publicId: String, systemId: String): PlatformDocumentType =
    createDocumentType(qualifiedName, publicId, systemId)

public actual fun PlatformDOMImplementation.createDocument(namespace: String?, qualifiedName: String?, documentType: PlatformDocumentType?): PlatformDocument =
    createDocument(namespace, qualifiedName, documentType)
