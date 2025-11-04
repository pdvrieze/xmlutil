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

import nl.adaptivity.xmlutil.XmlUtilInternal

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

@XmlUtilInternal
public actual inline fun PlatformAttr.getNamespaceURI(): String? = namespaceURI
@XmlUtilInternal
public actual inline fun PlatformAttr.getPrefix(): String? = prefix
@XmlUtilInternal
public actual inline fun PlatformAttr.getLocalName(): String? = localName
@XmlUtilInternal
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

@XmlUtilInternal
public actual inline fun PlatformElement.getNamespaceURI(): String? = namespaceURI
@XmlUtilInternal
public actual inline fun PlatformElement.getPrefix(): String? = prefix
@XmlUtilInternal
public actual inline fun PlatformElement.getLocalName(): String? = localName
@XmlUtilInternal
public actual inline fun PlatformElement.getTagName(): String = tagName
@XmlUtilInternal
public actual inline fun PlatformElement.getAttributes(): PlatformNamedNodeMap = attributes
internal actual inline fun PlatformElement.getAttribute(qualifiedName: String): String? = getAttribute(qualifiedName)
internal actual inline fun PlatformElement.getAttributeNS(namespace: String?, localName: String): String? = getAttributeNS(namespace, localName)

internal actual inline fun PlatformElement.setAttribute(qualifiedName: String, value: String) = setAttribute(qualifiedName, value)
internal actual inline fun PlatformElement.setAttributeNS(namespace: String?, cName: String, value: String) = setAttributeNS(namespace, cName, value)

internal actual inline fun PlatformElement.removeAttribute(qualifiedName: String) = removeAttribute(qualifiedName)
internal actual inline fun PlatformElement.removeAttributeNS(namespace: String?, localName: String) = removeAttributeNS(namespace, localName)

internal actual inline fun PlatformElement.hasAttribute(qualifiedName: String): Boolean = hasAttribute(qualifiedName)
internal actual inline fun PlatformElement.hasAttributeNS(namespace: String?, localName: String): Boolean = hasAttributeNS(namespace, localName)

internal actual inline fun PlatformElement.getAttributeNode(qualifiedName: String): PlatformAttr? = getAttributeNode(qualifiedName)
internal actual inline fun PlatformElement.getAttributeNodeNS(namespace: String?, localName: String): PlatformAttr? = getAttributeNodeNS(namespace, localName)

internal actual inline fun PlatformElement.setAttributeNode(attr: PlatformAttr): PlatformAttr? = setAttributeNode(attr)
internal actual inline fun PlatformElement.setAttributeNodeNS(attr: PlatformAttr): PlatformAttr? = setAttributeNodeNS(attr)
internal actual inline fun PlatformElement.removeAttributeNode(attr: PlatformAttr): PlatformAttr = removeAttributeNode(attr)

internal actual inline fun PlatformElement.getElementsByTagName(qualifiedName: String): PlatformNodeList = getElementsByTagName(qualifiedName)
internal actual inline fun PlatformElement.getElementsByTagNameNS(namespace: String?, localName: String): PlatformNodeList = getElementsByTagNameNS(namespace, localName)

@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.getLength(): Int = length
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.item(index: Int): PlatformNode? = item(index)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.getNamedItem(qualifiedName: String): PlatformNode? = getNamedItem(qualifiedName)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.getNamedItemNS(namespace: String?, localName: String): PlatformNode? = getNamedItemNS(namespace, localName)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.setNamedItem(attr: PlatformNode): PlatformNode? = setNamedItem(attr)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.setNamedItemNS(attr: PlatformNode): PlatformNode? = setNamedItemNS(attr)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.removeNamedItem(qualifiedName: String): PlatformNode? = removeNamedItem(qualifiedName)
@XmlUtilInternal
public actual inline fun PlatformNamedNodeMap.removeNamedItemNS(namespace: String?, localName: String): PlatformNode? = removeNamedItemNS(namespace, localName)


@XmlUtilInternal
public actual inline fun PlatformNodeList.getLength(): Int = length

@XmlUtilInternal
public actual inline operator fun PlatformNodeList.get(index: Int): PlatformNode? = item(index)

@XmlUtilInternal
public actual inline fun PlatformProcessingInstruction.getTarget(): String = target

@XmlUtilInternal
public actual inline fun PlatformProcessingInstruction.getData(): String = data
@XmlUtilInternal
public actual inline fun PlatformProcessingInstruction.setData(data: String) {
    this.data = data
}

public actual val PlatformDocument.supportsWhitespaceAtToplevel: Boolean get() = false


@XmlUtilInternal
public actual inline fun PlatformDOMImplementation.createDocumentType(qualifiedName: String, publicId: String, systemId: String): PlatformDocumentType =
    createDocumentType(qualifiedName, publicId, systemId)

@XmlUtilInternal
public actual inline fun PlatformDOMImplementation.createDocument(namespace: String?, qualifiedName: String?, documentType: PlatformDocumentType?): PlatformDocument =
    createDocument(namespace, qualifiedName, documentType)
