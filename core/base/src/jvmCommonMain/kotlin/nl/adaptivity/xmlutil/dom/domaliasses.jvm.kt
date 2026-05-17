/*
 * Copyright (c) 2024-2026.
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

import nl.adaptivity.xmlutil.core.impl.wrappingDom.wrap
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Node
import org.w3c.dom.Entity as DomEntity
import org.w3c.dom.EntityReference as DomEntityReference
import org.w3c.dom.NodeList as DomNodeList
import org.w3c.dom.Notation as DomNotation


public actual typealias PlatformDocument = org.w3c.dom.Document

public actual fun Document.adoptNode(node: PlatformNode): Node? = adoptNode(node.wrap())
public actual val PlatformDocument.childNodes: PlatformNodeList
    get() = childNodes

public actual typealias PlatformNode = org.w3c.dom.Node
public actual val PlatformNode.ownerDocument: PlatformDocument? get() = this.ownerDocument
public actual val PlatformNode.nodeType: Short get() = nodeType

public actual typealias PlatformAttr = org.w3c.dom.Attr

public actual fun PlatformAttr.getNamespaceURI(): String? = getNamespaceURI()
public actual fun PlatformAttr.getName(): String = getName()
public actual fun PlatformAttr.getLocalName(): String? = getLocalName()
public actual fun PlatformAttr.getPrefix(): String? = getPrefix()
public actual fun PlatformAttr.getValue(): String = getValue()


public actual typealias PlatformDocumentFragment = org.w3c.dom.DocumentFragment
public actual val PlatformDocumentFragment.childNodes: PlatformNodeList
    get() = childNodes

public actual typealias PlatformElement = org.w3c.dom.Element

public actual val PlatformElement.namespaceURI: String? get() = getNamespaceURI()
public actual val PlatformElement.prefix: String? get() = getPrefix()
public actual val PlatformElement.name: String get() = getNodeName()
public actual val PlatformElement.localName: String get() = getLocalName()
public actual val PlatformElement.attributes: PlatformNamedNodeMap get() = getAttributes()
public actual val PlatformElement.childNodes: PlatformNodeList get() = childNodes

public actual typealias PlatformText = org.w3c.dom.Text

public actual typealias PlatformCharacterData = org.w3c.dom.CharacterData
public actual fun PlatformCharacterData.getData(): String = data

public actual typealias PlatformCDATASection = org.w3c.dom.CDATASection

public actual typealias PlatformComment = org.w3c.dom.Comment

public actual typealias PlatformProcessingInstruction = org.w3c.dom.ProcessingInstruction
public actual fun PlatformProcessingInstruction.getNodeName(): String = target
public actual fun PlatformProcessingInstruction.getData(): String = data

public actual typealias PlatformDOMImplementation = org.w3c.dom.DOMImplementation

public actual typealias PlatformDocumentType = org.w3c.dom.DocumentType
public actual fun PlatformDocumentType.getOwnerDocument(): PlatformDocument? = ownerDocument
public actual fun PlatformDocumentType.getName(): String = name
public actual fun PlatformDocumentType.getPublicId(): String = publicId
public actual fun PlatformDocumentType.getSystemId(): String = systemId

public actual typealias PlatformNamedNodeMap = org.w3c.dom.NamedNodeMap

public actual operator fun PlatformNamedNodeMap.iterator(): Iterator<PlatformAttr> = IteratorImpl(this)
public actual val PlatformNamedNodeMap.length: Int get() = length
public actual fun PlatformNamedNodeMap.getNamedItemNS(namespace: String?, localName: String): PlatformNode? {
    return getNamedItemNS(namespace, localName)
}


private class IteratorImpl<N: PlatformNode>(private val map: PlatformNamedNodeMap): Iterator<N> {
    var idx = 0

    override fun hasNext(): Boolean = idx < map.length

    override fun next(): N {
        @Suppress("UNCHECKED_CAST")
        return map.item(idx++) as N
    }
}


public actual typealias PlatformNodeList = DomNodeList
public actual val PlatformNodeList.length: Int get() = length


private class NodeListIterator(private val list: PlatformNodeList) : Iterator<PlatformNode> {
    private var index = 0
    override fun hasNext(): Boolean = index < list.length
    override fun next(): PlatformNode = list.item(index++)!!
}

public actual operator fun PlatformNodeList.iterator(): Iterator<PlatformNode> =
    NodeListIterator(this)

public actual typealias PlatformNotation = DomNotation
public actual val PlatformNotation.publicId: String? get() = publicId
public actual val PlatformNotation.systemId: String? get() = systemId

public actual typealias PlatformEntity = DomEntity
public actual val PlatformEntity.publicId: String? get() = publicId
public actual val PlatformEntity.systemId: String? get() = systemId

public actual typealias PlatformEntityReference = DomEntityReference
