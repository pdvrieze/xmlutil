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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.EmptyNamedNodeMap
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.NodeList
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom2.addAttrPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addCharacterDataPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addDocumentPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addDocumentTypePropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addElementPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addNamedNodeMapPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addNodeListPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addNodePropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addProcessingInstructionPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addTextPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.impl.AbstractAttr
import nl.adaptivity.xmlutil.dom2.impl.AbstractAttrStorage
import nl.adaptivity.xmlutil.dom2.impl.AbstractCharacterData
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocument
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocumentType
import nl.adaptivity.xmlutil.dom2.impl.AbstractElement
import nl.adaptivity.xmlutil.dom2.impl.AbstractNode
import nl.adaptivity.xmlutil.dom2.impl.AbstractProcessingInstruction
import nl.adaptivity.xmlutil.dom2.impl.AbstractText
import nl.adaptivity.xmlutil.dom2.impl.EmptyNodeList
import nl.adaptivity.xmlutil.dom2.impl.LinearNodeStorage
import nl.adaptivity.xmlutil.dom2.impl.LinkedNodeList
import nl.adaptivity.xmlutil.dom2.impl.NodeListImpl
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Attr as DomAttr
import org.w3c.dom.CDATASection as DomCDATASection
import org.w3c.dom.Comment as DomComment
import org.w3c.dom.Document as DomDocument
import org.w3c.dom.DocumentType as DomDocumentType
import org.w3c.dom.Element as DomElement
import org.w3c.dom.Node as DomNode
import org.w3c.dom.ProcessingInstruction as DomProcessingInstruction
import org.w3c.dom.Text as DomText

internal abstract class JsWrappedNode<out N : DomNode>(delegate: N) : Node {
    @Suppress("UNCHECKED_CAST")
    val delegate: N = delegate.unWrap() as N

    val parentNode: Node? get() = delegate.parentNode?.wrap()
    val parentElement: Element? get() = parentNode as? Element

    val firstChild: Node? get() = delegate.firstChild?.wrap()

    val lastChild: Node? get() = delegate.lastChild?.wrap()

    val previousSibling: Node? get() = delegate.previousSibling?.wrap()

    val nextSibling: Node? get() = delegate.nextSibling?.wrap()

    val nodeName: String get() = delegate.nodeName

    override val nodeType: Short get() = delegate.nodeType

    val baseURI: String get() = delegate.baseURI

    var textContent: String?
        get() = delegate.textContent
        set(value) {
            delegate.textContent = value
        }

    override fun getTextContent(): String? = textContent
    override fun setTextContent(value: String?) {
        textContent = value
    }

    val childNodes: JsWrappedNodeList
        get() = JsWrappedNodeList(delegate.childNodes)

    fun insertBefore(newChild: DomNode?, refChild: DomNode?): Node {
        return delegate.insertBefore(newChild!!, refChild?.unWrap()).wrap()
    }

    override fun hasChildNodes(): Boolean = delegate.hasChildNodes()

    override fun cloneNode(deep: Boolean): Node {
        return delegate.cloneNode(deep).wrap()
    }

    override fun normalize() {
        delegate.normalize()
    }

    fun compareDocumentPosition(other: DomNode): Short {
        return delegate.compareDocumentPosition(other.unWrap())
    }

    override fun getBaseURI(): String? {
        return delegate.baseURI
    }

    fun isSameNode(other: DomNode?): Boolean = delegate.isSameNode(other?.unWrap())

    final override fun lookupPrefix(namespace: String): String? = delegate.lookupPrefix(namespace)

    override fun isDefaultNamespace(namespaceURI: String): Boolean = delegate.isDefaultNamespace(namespaceURI)

    final override fun lookupNamespaceURI(prefix: String): String? = delegate.lookupNamespaceURI(prefix)

    fun isEqualNode(arg: DomNode): Boolean {
        return delegate.isEqualNode(arg.unWrap())
    }

    override fun appendChild(node: PlatformNode): Node = appendChild(node.unWrap())


    fun appendChild(newChild: DomNode): Node {
        return delegate.appendChild(newChild.unWrap()).wrap()
    }

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Node {
        return delegate.replaceChild(oldChild.unWrap(), newChild.unWrap()).wrap()
    }

    fun replaceChild(newChild: DomNode, oldChild: DomNode): Node {
        return delegate.replaceChild(oldChild.unWrap(), newChild.unWrap()).wrap()
    }

    override fun removeChild(node: PlatformNode): Node = removeChild(node.unWrap())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as JsWrappedNode<*>

        return delegate == other.delegate
    }

    fun removeChild(oldChild: DomNode): Node =
        delegate.removeChild(oldChild).wrap()

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun getNodeName(): String = nodeName
    override fun getOwnerDocument(): JsWrappedDocument? = delegate.ownerDocument?.wrap()
    override fun getParentNode(): Node? = parentNode
    override fun getParentElement(): Element? = parentElement
    override fun getFirstChild(): Node? = firstChild
    override fun getLastChild(): Node? = lastChild
    override fun getPreviousSibling(): Node? = previousSibling
    override fun getNextSibling(): Node? = nextSibling
    override fun getChildNodes(): NodeList = childNodes

    override fun getNodetype(): NodeType = NodeType(nodeType)

    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Node? {
        return delegate.insertBefore(newChild.unWrap(), refChild?.unWrap()).wrap()
    }

    override fun isSameNode(other: PlatformNode): Boolean {
        return delegate.isSameNode(other.unWrap())
    }

    override fun isEqualNode(other: PlatformNode): Boolean {
        return delegate.isEqualNode(other.unWrap())
    }


    companion object {
        init {
            addNodePropertiesToPrototype(JsWrappedNode::class.js.asDynamic().prototype)
            addAttrPropertiesToPrototype(JsWrappedAttr::class.js.asDynamic().prototype, false)
            addCharacterDataPropertiesToPrototype(JsWrappedCharacterData::class.js.asDynamic().prototype, false)
            addDocumentPropertiesToPrototype(JsWrappedDocument::class.js.asDynamic().prototype, false)
            addDocumentTypePropertiesToPrototype(JsWrappedDocumentType::class.js.asDynamic().prototype, false)
            addElementPropertiesToPrototype(JsWrappedElement::class.js.asDynamic().prototype, false)
            addNamedNodeMapPropertiesToPrototype(JsWrappedNamedNodeMap::class.js.asDynamic().prototype)

            addNodeListPropertiesToPrototype(JsWrappedNodeList::class.js.asDynamic().prototype)

            addProcessingInstructionPropertiesToPrototype(JsWrappedProcessingInstruction::class.js.asDynamic().prototype, false)
            addTextPropertiesToPrototype(JsWrappedText::class.js.asDynamic().prototype, false)
        }
    }
}


internal fun DomNode.unWrap(): DomNode = when (val n = this as Any) {
    is JsWrappedNode<*> -> n.delegate
    else -> this
}

internal fun PlatformNode.unWrap(): DomNode = when (this) {
    is JsWrappedNode<*> -> delegate
    else -> this as DomNode // works in JavaScript
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomAttr.unWrap(): DomAttr = when (val n = this as Any) {
    is JsWrappedAttr -> n.delegate
    else -> this
}

internal fun PlatformAttr.unWrap(): DomAttr = when (this) {
    is JsWrappedAttr -> delegate
    else -> this as DomAttr
}

internal fun DomNode.wrap(): JsWrappedNode<DomNode> = when (nodeType) {
    NodeConsts.ATTRIBUTE_NODE -> JsWrappedAttr(this as DomAttr)
    NodeConsts.CDATA_SECTION_NODE -> JsWrappedCDATASection(this as DomCDATASection)
    NodeConsts.COMMENT_NODE -> JsWrappedComment(this as DomComment)
    NodeConsts.DOCUMENT_NODE -> JsWrappedDocument(this as DomDocument)
    NodeConsts.DOCUMENT_FRAGMENT_NODE -> JsWrappedDocumentFragment(this as DocumentFragment)
    NodeConsts.DOCUMENT_TYPE_NODE -> JsWrappedDocumentType(this as DomDocumentType)
    NodeConsts.ELEMENT_NODE -> JsWrappedElement(this as DomElement)
    NodeConsts.PROCESSING_INSTRUCTION_NODE -> JsWrappedProcessingInstruction(this as DomProcessingInstruction)
    NodeConsts.TEXT_NODE -> JsWrappedText(this as DomText)
    else -> error("Node type ${NodeType(nodeType)} not supported")
}

internal fun PlatformNode.wrap(): JsWrappedNode<*> = when (val n = this as Any) {
    is JsWrappedNode<*> -> n
    else -> (this as DomNode).wrap()
}

internal fun Node.wrap(): JsWrappedNode<*> = when (val n = this as Any) {
    is JsWrappedNode<*> -> n
    is DomNode -> n.wrap()
    else -> error("Node type ${getNodetype()} not supported")
}

internal fun DomDocument.wrap(): JsWrappedDocument = when (val n = this as Any) {
    is JsWrappedDocument -> n
    else -> JsWrappedDocument(this)
}

internal fun DomElement.wrap(): JsWrappedElement = when (val n = this as Any) {
    is JsWrappedElement -> n
    else -> JsWrappedElement(this)
}

internal fun DomText.wrap(): JsWrappedText = when (val n = this as Any) {
    is JsWrappedText -> n
    else -> JsWrappedText(this)
}

internal fun DomDocumentType.wrap(): JsWrappedDocumentType = when (val n = this as Any) {
    is JsWrappedDocumentType -> n
    else -> JsWrappedDocumentType(this)
}

internal fun DomAttr.wrap(): JsWrappedAttr = when (val n = this as Any) {
    is JsWrappedAttr -> n
    else -> JsWrappedAttr(this)
}
