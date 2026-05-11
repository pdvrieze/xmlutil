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
import nl.adaptivity.xmlutil.dom2.*
import org.w3c.dom.DocumentFragment
import nl.adaptivity.xmlutil.dom.PlatformAttr as Attr1
import nl.adaptivity.xmlutil.dom.PlatformNode as Node1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2
import org.w3c.dom.Attr as DomAttr
import org.w3c.dom.CDATASection as DomCDATASection
import org.w3c.dom.Comment as DomComment
import org.w3c.dom.Document as DomDocument
import org.w3c.dom.DocumentType as DomDocumentType
import org.w3c.dom.Element as DomElement
import org.w3c.dom.Node as DomNode
import org.w3c.dom.ProcessingInstruction as DomProcessingInstruction
import org.w3c.dom.Text as DomText

internal abstract class NodeImpl<out N : DomNode>(delegate: N) : Node {
    @Suppress("UNCHECKED_CAST")
    val delegate: N = delegate.unWrap() as N

    override val ownerDocument: DocumentImpl? get() = delegate.ownerDocument?.wrap()

    override val parentNode: Node? get() = delegate.parentNode?.wrap()
    override val parentElement: Element? get() = parentNode as? Element

    override val firstChild: Node? get() = delegate.firstChild?.wrap()

    override val lastChild: Node? get() = delegate.lastChild?.wrap()

    override val previousSibling: Node? get() = delegate.previousSibling?.wrap()

    override val nextSibling: Node? get() = delegate.nextSibling?.wrap()

    override val nodeName: String get() = delegate.nodeName

    override val nodeType: Short get() = delegate.nodeType

    val baseURI: String get() = delegate.baseURI

    override var nodeValue: String?
        get() = delegate.nodeValue
        set(value) {
            delegate.nodeValue = value
        }

    override var textContent: String?
        get() = delegate.textContent
        set(value) {
            delegate.textContent = value
        }

    override fun getTextContent(): String? = textContent
    override fun setTextContent(value: String?) {
        textContent = value
    }

    override val childNodes: WrappingNodeList
        get() = WrappingNodeList(delegate.childNodes)

    fun insertBefore(newChild: DomNode?, refChild: DomNode?): Node {
        return delegate.insertBefore(newChild!!, refChild?.unWrap()).wrap()
    }

    fun hasChildNodes(): Boolean = delegate.hasChildNodes()

    fun cloneNode(deep: Boolean): Node {
        return delegate.cloneNode(deep).wrap()
    }

    fun normalize() {
        delegate.normalize()
    }

    fun compareDocumentPosition(other: DomNode): Short {
        return delegate.compareDocumentPosition(other.unWrap())
    }

    fun isSameNode(other: DomNode?): Boolean = delegate.isSameNode(other?.unWrap())

    final override fun lookupPrefix(namespace: String): String? = delegate.lookupPrefix(namespace)

    fun isDefaultNamespace(namespaceURI: String): Boolean = delegate.isDefaultNamespace(namespaceURI)

    final override fun lookupNamespaceURI(prefix: String): String? = delegate.lookupNamespaceURI(prefix)

    fun isEqualNode(arg: DomNode): Boolean {
        return delegate.isEqualNode(arg.unWrap())
    }

    override fun appendChild(node: Node1): Node = appendChild(node.unWrap())


    fun appendChild(newChild: DomNode): Node {
        return delegate.appendChild(newChild.unWrap()).wrap()
    }

    override fun replaceChild(newChild: Node1, oldChild: Node1): Node {
        return delegate.replaceChild(oldChild.unWrap(), newChild.unWrap()).wrap()
    }

    fun replaceChild(newChild: DomNode, oldChild: DomNode): Node {
        return delegate.replaceChild(oldChild.unWrap(), newChild.unWrap()).wrap()
    }

    override fun removeChild(node: Node1): Node = removeChild(node.unWrap())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as NodeImpl<*>

        return delegate == other.delegate
    }

    fun removeChild(oldChild: DomNode): Node =
        delegate.removeChild(oldChild).wrap()

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun getNodeName(): String = nodeName
    override fun getOwnerDocument(): DocumentImpl? = delegate.ownerDocument?.wrap()
    override fun getParentNode(): Node? = parentNode
    override fun getParentElement(): Element? = parentElement
    override fun getFirstChild(): Node? = firstChild
    override fun getLastChild(): Node? = lastChild
    override fun getPreviousSibling(): Node? = previousSibling
    override fun getNextSibling(): Node? = nextSibling
    override fun getChildNodes(): NodeList = childNodes

    override fun getNodetype(): NodeType = NodeType(nodeType)

}


@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomNode.unWrap(): DomNode = when (this) {
    is Node -> (this as NodeImpl<*>).delegate
    else -> this
}

internal fun Node1.unWrap(): DomNode = when (this) {
    is NodeImpl<*> -> delegate
    else -> this as DomNode // works in JavaScript
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomAttr.unWrap(): DomAttr = when (this) {
    is Attr2 -> (this as AttrImpl).delegate
    else -> this
}

internal fun Attr1.unWrap(): DomAttr = when (this) {
    is AttrImpl -> delegate
    else -> this as DomAttr
}

internal fun Attr2.unWrap(): DomAttr = when (this) {
    is AttrImpl -> delegate
    is DomAttr -> this
    else -> throw IllegalArgumentException("Attribute can not be resolved")
}

internal fun Node.unWrap(): DomNode = when (this) {
    is NodeImpl<*> -> delegate
    else -> throw IllegalArgumentException("Can not be unwrapped") // has to be actually wrapped to "work"
}

internal fun DomNode.wrap(): NodeImpl<DomNode> = when (nodeType) {
    NodeConsts.ATTRIBUTE_NODE -> AttrImpl(this as DomAttr)
    NodeConsts.CDATA_SECTION_NODE -> CDATASectionImpl(this as DomCDATASection)
    NodeConsts.COMMENT_NODE -> CommentImpl(this as DomComment)
    NodeConsts.DOCUMENT_NODE -> DocumentImpl(this as DomDocument)
    NodeConsts.DOCUMENT_FRAGMENT_NODE -> DocumentFragmentImpl(this as DocumentFragment)
    NodeConsts.DOCUMENT_TYPE_NODE -> DocumentTypeImpl(this as DomDocumentType)
    NodeConsts.ELEMENT_NODE -> ElementImpl(this as DomElement)
    NodeConsts.PROCESSING_INSTRUCTION_NODE -> ProcessingInstructionImpl(this as DomProcessingInstruction)
    NodeConsts.TEXT_NODE -> TextImpl(this as DomText)
    else -> error("Node type ${NodeType(nodeType)} not supported")
}

internal fun Node1.wrap(): NodeImpl<*> = when (this) {
    is NodeImpl<*> -> this
    else -> (this as DomNode).wrap()
}

internal fun Node.wrap(): NodeImpl<*> = when (this) {
    is NodeImpl<*> -> this
    else -> error("Node type ${getNodetype()} not supported")
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomDocument.wrap(): DocumentImpl = when (this) {
    is Document -> this as DocumentImpl
    else -> DocumentImpl(this)
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomElement.wrap(): ElementImpl = when (this) {
    is Element -> this as ElementImpl
    else -> ElementImpl(this)
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomText.wrap(): TextImpl = when (this) {
    is Text -> this as TextImpl
    else -> TextImpl(this)
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomDocumentType.wrap(): DocumentTypeImpl = when (this) {
    is DocumentType -> this as DocumentTypeImpl
    else -> DocumentTypeImpl(this)
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal fun DomAttr.wrap(): AttrImpl = when (this) {
    is Attr2 -> this as AttrImpl
    else -> AttrImpl(this)
}
