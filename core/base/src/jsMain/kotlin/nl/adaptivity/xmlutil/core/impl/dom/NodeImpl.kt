/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.*
import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.dom2.NodeType
import org.w3c.dom.DocumentFragment
import nl.adaptivity.xmlutil.dom.Attr as Attr1
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Attr as DomAttr
import org.w3c.dom.CDATASection as DomCDATASection
import org.w3c.dom.Comment as DomComment
import org.w3c.dom.Document as DomDocument
import org.w3c.dom.DocumentType as DomDocumentType
import org.w3c.dom.Element as DomElement
import org.w3c.dom.Node as DomNode
import org.w3c.dom.ProcessingInstruction as DomProcessingInstruction
import org.w3c.dom.Text as DomText

internal abstract class NodeImpl<N : DomNode>(delegate: N) : INode {
    @Suppress("UNCHECKED_CAST")
    override val delegate: N = delegate.unWrap() as N

    override val ownerDocument: IDocument get() = delegate.ownerDocument!!.wrap()

    override val parentNode: INode? get() = delegate.parentNode?.wrap()

    override val firstChild: INode? get() = delegate.firstChild?.wrap()

    override val lastChild: INode? get() = delegate.lastChild?.wrap()

    override val previousSibling: INode? get() = delegate.previousSibling?.wrap()

    override val nextSibling: INode? get() = delegate.nextSibling?.wrap()

    override val nodeName: String get() = delegate.nodeName

    override val nodeType: Short get() = delegate.nodeType

    override val baseURI: String get() = delegate.baseURI

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

    override val childNodes: INodeList
        get() = WrappingNodeList(delegate.childNodes)

    final override fun insertBefore(newChild: DomNode?, refChild: DomNode?): INode {
        return delegate.insertBefore(newChild!!, refChild?.unWrap()).wrap()
    }

    final override fun hasChildNodes(): Boolean = delegate.hasChildNodes()

    final override fun cloneNode(deep: Boolean): INode {
        return delegate.cloneNode(deep).wrap()
    }

    final override fun normalize() {
        delegate.normalize()
    }

    final override fun compareDocumentPosition(other: DomNode): Short {
        return delegate.compareDocumentPosition(other.unWrap())
    }

    final override fun isSameNode(other: DomNode?): Boolean = delegate.isSameNode(other?.unWrap())

    final override fun lookupPrefix(namespace: String): String? = delegate.lookupPrefix(namespace)

    final override fun isDefaultNamespace(namespaceURI: String): Boolean = delegate.isDefaultNamespace(namespaceURI)

    final override fun lookupNamespaceURI(prefix: String): String? = delegate.lookupNamespaceURI(prefix)

    final override fun isEqualNode(arg: DomNode): Boolean {
        return delegate.isEqualNode(arg.unWrap())
    }

    final override fun appendChild(newChild: DomNode): INode {
        return delegate.appendChild(newChild.unWrap()).wrap()
    }

    final override fun replaceChild(newChild: DomNode, oldChild: DomNode): INode {
        return delegate.replaceChild(oldChild.unWrap(), newChild.unWrap()).wrap()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as NodeImpl<*>

        return delegate == other.delegate
    }

    final override fun removeChild(oldChild: DomNode): INode =
        delegate.removeChild(oldChild).wrap()

    override fun hashCode(): Int {
        return delegate.hashCode()
    }


}

internal fun INode.unWrap(): DomNode = delegate

internal fun IDocumentType.unWrap(): DomDocumentType = delegate

internal fun DomNode.unWrap(): DomNode = when (this) {
    is INode -> delegate
    else -> this
}

internal fun Node1.unWrap(): DomNode = when (this) {
    is INode -> delegate
    else -> this as DomNode // works in JavaScript
}

internal fun DomAttr.unWrap(): DomAttr = when (this) {
    is IAttr -> delegate as DomAttr
    else -> this
}

internal fun Attr1.unWrap(): DomAttr = when (this) {
    is IAttr -> delegate as DomAttr
    else -> this as DomAttr
}

internal fun Attr2.unWrap(): DomAttr = when (this) {
    is IAttr -> delegate as DomAttr
    is DomAttr -> this
    else -> throw IllegalArgumentException("Attribute can not be resolved")
}

internal fun Node2.unWrap(): DomNode = when (this) {
    is INode -> delegate
    else -> throw IllegalArgumentException("Can not be unwrapped") // has to be actually wrapped to "work"
}

internal fun DomNode.wrap(): INode = when (this) {
    is INode -> this
    else -> when (nodeType) {
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
}

internal fun Node1.wrap(): INode =
    (this as DomNode).wrap()

internal fun Node2.wrap(): INode = when (this) {
    is INode -> this
    else -> error("Node type $nodetype not supported")
}

internal fun DomDocument.wrap(): IDocument = when (this) {
    is IDocument -> this
    else -> DocumentImpl(this)
}

internal fun DomElement.wrap(): IElement = when (this) {
    is IElement -> this
    else -> ElementImpl(this)
}

internal fun DomText.wrap(): IText = when (this) {
    is IText -> this
    else -> TextImpl(this)
}

internal fun DomDocumentType.wrap(): IDocumentType = when (this) {
    is IDocumentType -> this
    else -> DocumentTypeImpl(this)
}

internal fun DomAttr.wrap(): IAttr = when (this) {
    is IAttr -> this
    else -> AttrImpl(this)
}
