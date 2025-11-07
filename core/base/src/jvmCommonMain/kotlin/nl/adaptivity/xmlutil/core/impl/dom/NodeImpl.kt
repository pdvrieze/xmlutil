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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.*
import nl.adaptivity.xmlutil.dom2.NodeType
import org.w3c.dom.Text
import org.w3c.dom.UserDataHandler
import nl.adaptivity.xmlutil.dom2.Attr as Attr2
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Attr as DomAttr
import org.w3c.dom.CDATASection as DomCDATASection
import org.w3c.dom.Comment as DomComment
import org.w3c.dom.Document as DomDocument
import org.w3c.dom.DocumentFragment as DomDocumentFragment
import org.w3c.dom.DocumentType as DomDocumentType
import org.w3c.dom.Element as DomElement
import org.w3c.dom.Node as DomNode
import org.w3c.dom.ProcessingInstruction as DomProcessingInstruction

internal abstract class NodeImpl<N : DomNode>(delegate: N) : INode {
    @Suppress("UNCHECKED_CAST")
    override val delegate: N = delegate.unWrap() as N

    final override fun getOwnerDocument(): IDocument = delegate.ownerDocument.wrap()

    final override fun getParentNode(): INode? = delegate.parentNode?.wrap()

    override fun getFirstChild(): INode? = delegate.firstChild?.wrap()

    override fun getLastChild(): INode? = delegate.lastChild?.wrap()

    final override fun getPreviousSibling(): INode? = delegate.previousSibling?.wrap()

    final override fun getNextSibling(): INode? = delegate.nextSibling?.wrap()

    final override fun getNodeName(): String = delegate.nodeName
    final override fun getNodetype(): NodeType = NodeType(delegate.nodeType)

    final override fun getNodeType(): Short = delegate.nodeType

    final override fun getTextContent(): String? = delegate.textContent

    final override fun setTextContent(textContent: String) {
        delegate.textContent = textContent
    }

    final override fun getChildNodes(): INodeList = WrappingNodeList(delegate.childNodes)

    final override fun getNodeValue(): String = delegate.nodeValue

    final override fun setNodeValue(nodeValue: String?) {
        delegate.nodeValue = nodeValue
    }

    final override fun insertBefore(newChild: DomNode?, refChild: DomNode?): INode {
        return delegate.insertBefore(newChild?.unWrap(), refChild?.unWrap()).wrap()
    }

    final override fun hasChildNodes(): Boolean = delegate.hasChildNodes()

    final override fun cloneNode(deep: Boolean): INode {
        return delegate.cloneNode(deep).wrap()
    }

    final override fun normalize() {
        delegate.normalize()
    }

    final override fun isSupported(feature: String?, version: String?): Boolean {
        return delegate.isSupported(feature, version)
    }

    final override fun getNamespaceURI(): String? = delegate.namespaceURI

    final override fun getPrefix(): String? = delegate.prefix

    final override fun setPrefix(prefix: String?) {
        delegate.prefix = prefix
    }

    override fun getLocalName(): String? = delegate.localName

    final override fun hasAttributes(): Boolean = delegate.hasAttributes()

    final override fun getBaseURI(): String = delegate.baseURI

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

    final override fun getFeature(feature: String, version: String?): Any? {
        return delegate.getFeature(feature, version)
    }

    final override fun setUserData(key: String, data: Any?, handler: UserDataHandler?): Any? {
        return delegate.setUserData(key, data, handler)
    }

    final override fun getUserData(key: String): Any? {
        return delegate.getUserData(key)
    }

    final override fun appendChild(node: INode): INode {
        return delegate.appendChild(node.unWrap()).wrap()
    }

    final override fun appendChild(newChild: DomNode): INode {
        return delegate.appendChild(newChild.unWrap()).wrap()
    }

    final override fun replaceChild(newChild: INode, oldChild: INode): INode {
        return delegate.replaceChild(newChild.unWrap(), oldChild.unWrap()).wrap()
    }

    final override fun replaceChild(newChild: DomNode, oldChild: DomNode): INode {
        return delegate.replaceChild(newChild.unWrap(), oldChild.unWrap()).wrap()
    }

    final override fun removeChild(node: INode): INode {
        return delegate.removeChild(node.unWrap()).wrap()
    }

    final override fun removeChild(oldChild: DomNode): INode {
        return delegate.removeChild(oldChild.unWrap()).wrap()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeImpl<*>

        return delegate == other.delegate
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun toString(): String = delegate.toString()

}

internal fun INode.unWrap(): DomNode = delegate

internal fun DomNode.unWrap(): DomNode = when (this) {
    is INode -> delegate
    else -> this
}

internal fun DomAttr.unWrap(): DomAttr = when (this) {
    is IAttr -> delegate as DomAttr
    else -> this
}

internal fun Attr2.unWrap(): DomAttr = when (this) {
    is IAttr -> delegate as DomAttr
    is DomAttr -> this
    else -> throw IllegalArgumentException("Attribute can not be resolved")
}

internal fun Node2.unWrap(): DomNode = when (this) {
    is INode -> delegate
    else -> this.wrap() // has to be actually wrapped to "work"
}

internal fun DomNode.wrap(): INode = when (this) {
    is INode -> this
    is DomAttr -> AttrImpl(this)
    is DomCDATASection -> CDATASectionImpl(this)
    is DomComment -> CommentImpl(this)
    is DomDocument -> DocumentImpl(this)
    is DomDocumentFragment -> DocumentFragmentImpl(this)
    is DomDocumentType -> DocumentTypeImpl(this)
    is DomElement -> ElementImpl(this)
    is DomProcessingInstruction -> ProcessingInstructionImpl(this)
    is Text -> TextImpl(this)
    else -> error("Node type ${NodeType(nodeType)} not supported")
}

internal fun Node2.wrap(): INode = when (this) {
    is INode -> this
    else -> error("Node type ${getNodetype()} not supported")
}

internal fun DomDocument.wrap(): IDocument = when (this) {
    is IDocument -> this
    else -> DocumentImpl(this)
}

internal fun DomElement.wrap(): IElement = when (this) {
    is IElement -> this
    else -> ElementImpl(this)
}

internal fun Text.wrap(): IText = when (this) {
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
