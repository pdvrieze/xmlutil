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
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.isXmlWhitespace
import nl.adaptivity.xmlutil.dom.PlatformAttr as Attr1
import nl.adaptivity.xmlutil.dom.PlatformCDATASection as CDATASection1
import nl.adaptivity.xmlutil.dom.PlatformComment as Comment1
import nl.adaptivity.xmlutil.dom.PlatformDocument as Document1
import nl.adaptivity.xmlutil.dom.PlatformDocumentFragment as DocumentFragment1
import nl.adaptivity.xmlutil.dom.PlatformDocumentType as DocumentType1
import nl.adaptivity.xmlutil.dom.PlatformElement as Element1
import nl.adaptivity.xmlutil.dom.PlatformNode as Node1
import nl.adaptivity.xmlutil.dom.PlatformProcessingInstruction as ProcessingInstruction1
import nl.adaptivity.xmlutil.dom.PlatformText as Text1
import nl.adaptivity.xmlutil.dom2.Attr as Attr2
import nl.adaptivity.xmlutil.dom2.CDATASection as CDATASection2
import nl.adaptivity.xmlutil.dom2.Comment as Comment2
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.DocumentFragment as DocumentFragment2
import nl.adaptivity.xmlutil.dom2.DocumentType as DocumentType2
import nl.adaptivity.xmlutil.dom2.Element as Element2
import nl.adaptivity.xmlutil.dom2.Node as Node2
import nl.adaptivity.xmlutil.dom2.ProcessingInstruction as ProcessingInstruction2
import nl.adaptivity.xmlutil.dom2.Text as Text2

internal class DocumentImpl(doctype: DocumentTypeImpl?) : IDocument {

    constructor(doctype1: DocumentType1?) : this(doctype = doctype1?.let(DocumentTypeImpl::coerce))

    constructor(doctype2: DocumentType2?) : this(doctype = doctype2?.let(DocumentTypeImpl::coerce))

    constructor(idoctype: IDocumentType?) : this(doctype = idoctype?.let { DocumentTypeImpl.coerce(it as DocumentType2) })

    override val doctype: IDocumentType? = doctype

    override val implementation: IDOMImplementation get() = SimpleDOMImplementation

    private var _documentElement: ElementImpl? = null
    override val documentElement: IElement? get() = _documentElement

    override var characterSet: String = "UTF-8"

    override val nodetype: NodeType get() = NodeType.DOCUMENT_NODE

    override fun getNodeName(): String = "#document"

    override fun getOwnerDocument(): IDocument = this

    override var parentNode: INode?
        get() = null
        set(_) {
            throw UnsupportedOperationException()
        }

    private val _childNodes: NodeListImpl = NodeListImpl()

    override fun getChildNodes(): INodeList = _childNodes

    override fun getFirstChild(): INode? = _childNodes.elements.firstOrNull()

    override fun getLastChild(): INode? = _childNodes.elements.lastOrNull()

    override fun getPreviousSibling(): INode? = null

    override fun getNextSibling(): INode? = null

    override fun getTextContent(): String? = null

    override fun setTextContent(value: String) {
        throw UnsupportedOperationException("Documents have no (direct) text content")
    }

    override fun getInputEncoding(): String? = null

    override fun adoptNode(node: Node1): INode {
        if (node !is NodeImpl) throw DOMException("Not supported")
        if (node.getOwnerDocument() === this) return node
        node.getParentNode()?.removeChild((node as Node1))
        node.ownerDocument = this
        return node
    }

    override fun adoptNode(node: Node2): INode {
        if (node !is NodeImpl) throw DOMException("Not supported")
        if (node.getOwnerDocument() === this) return node
        node.getParentNode()?.removeChild((node as Node1))
        node.ownerDocument = this
        return node
    }

    override fun importNode(node: Node1, deep: Boolean): INode {
        return importNodeX(node as NodeImpl, deep)
    }

    override fun importNode(node: Node2, deep: Boolean): INode {
        return importNodeX(node as NodeImpl, deep)
    }

    private fun importNodeX(
        node: INode,
        deep: Boolean
    ): INode {
        return when (node) {
            is Attr1 -> AttrImpl(this, node)
            is Attr2 -> AttrImpl(this, node)
            is CDATASection1 -> CDATASectionImpl(this, node)
            is CDATASection2 -> CDATASectionImpl(this, node)
            is Comment1 -> CommentImpl(this, node)
            is Comment2 -> CommentImpl(this, node)
            is Document1 -> throw DOMException("Documents cannot be imported")
            is Document2 -> throw DOMException("Documents cannot be imported")
            is DocumentFragment1,
            is DocumentFragment2 -> DocumentFragmentImpl(this).also { cpy ->
                if (deep) {
                    for (child in node.getChildNodes()) {
                        cpy.appendChild(importNodeX(child, deep))
                    }
                }
            }

            is Element1 -> ElementImpl(this, node).also { cpy ->
                if (deep) {
                    for (child in node.getChildNodes()) {
                        cpy.appendChild(importNodeX(child, deep))
                    }
                }
            }

            is Element2 -> ElementImpl(this, node).also { cpy ->
                if (deep) {
                    for (child in node.getChildNodes()) {
                        cpy.appendChild(importNodeX(child, deep))
                    }
                }
            }

            is ProcessingInstruction1 -> ProcessingInstructionImpl(this, node)
            is ProcessingInstruction2 -> ProcessingInstructionImpl(this, node)
            is Text1 -> TextImpl(this, node)
            is Text2 -> TextImpl(this, node)
            else -> throw DOMException("Unsupported node subtype")
        }
    }

    override fun appendChild(node: INode): INode {
        val n = checkNode(node)
        when (n) {
            is DocumentFragmentImpl -> for (child in n.getChildNodes()) {
                appendChild(child)
                n._childNodes.elements.clear()
            }

            is ElementImpl -> {
                if (getDocumentElement() != null) throw UnsupportedOperationException("Only one root element is supported for now")
                _documentElement = n
            }

            is Comment1,
            is ProcessingInstructionImpl -> Unit // fine

            is TextImpl -> require(isXmlWhitespace(n.getData())) { "Non-whitespace nodes cannot be added directly to a document" }
            else -> throw IllegalArgumentException("Attempting to add node ${n.getNodeType()} where not permitted")
        }
        n.parentNode = this
        _childNodes.elements.add(n)

        return n
    }

    override fun removeChild(node: INode): INode {
        if (node != _documentElement) throw DOMException("Node is not a child of this document")
        _documentElement = null
        _childNodes.elements.remove(node)
        (node as? NodeImpl)?.let { it.parentNode = null }
        return node
    }

    override fun replaceChild(oldChild: INode, newChild: INode): INode {
        if (oldChild != _documentElement) throw DOMException("Old node not found in document")
        checkNode(newChild)
        if (newChild !is ElementImpl) throw UnsupportedOperationException("Only element children to root supported for now")
        _documentElement = newChild
        return oldChild
    }

    override fun createDocumentFragment(): IDocumentFragment {
        return DocumentFragmentImpl(this)
    }

    override fun createElement(localName: String): IElement {
        return ElementImpl(this, null, localName, null)
    }

    override fun createElementNS(namespaceURI: String, qualifiedName: String): IElement {
        val localName = qualifiedName.substringAfterLast(':', qualifiedName)
        val prefix = qualifiedName.substringBeforeLast(':', "").takeUnless { it.isEmpty() }
        return ElementImpl(this, namespaceURI, localName, prefix)
    }

    override fun createAttribute(localName: String): IAttr {
        return AttrImpl(this, null, localName, null, "")
    }

    override fun createAttributeNS(namespace: String?, qualifiedName: String): IAttr {
        val localName = qualifiedName.substringAfterLast(':', qualifiedName)
        val prefix = qualifiedName.substringBeforeLast(':', "").takeUnless { it.isEmpty() }
        return AttrImpl(this, namespace, localName, prefix, "")
    }

    override fun createTextNode(data: String): IText {
        return TextImpl(this, data)
    }

    override fun createCDATASection(data: String): ICDATASection {
        return CDATASectionImpl(this, data)
    }

    override fun createComment(data: String): IComment {
        return CommentImpl(this, data)
    }

    override fun createProcessingInstruction(target: String, data: String): IProcessingInstruction {
        return ProcessingInstructionImpl(this, target, data)
    }

    override fun lookupPrefix(namespace: String): String? {
        return (_documentElement ?: return null).lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        return (_documentElement ?: return null).lookupNamespaceURI(prefix)
    }

    override fun toString(): String = when (val e = _documentElement) {
        null -> "<Empty Document>"
        else -> e.toString()
    }

    companion object {
        fun coerce(document: Document1): DocumentImpl {
            return (document as? DocumentImpl) ?: throw IllegalArgumentException("Documents can not be adopted")
        }

        fun coerce(document: Document2): DocumentImpl {
            return (document as? DocumentImpl) ?: throw IllegalArgumentException("Documents can not be adopted")
        }
    }
}

internal fun Node1.checkNode(node: Node1): NodeImpl {
    if (node.ownerDocument != ownerDocument) throw DOMException("Node not owned by this document")
    if (node !is NodeImpl) throw DOMException("Unexpected node implementation, try importing")
    return node
}

internal fun Node2.checkNode(node: Node2): NodeImpl {
    if (node.getOwnerDocument() != getOwnerDocument()) throw DOMException("Node not owned by this document")
    if (node !is NodeImpl) throw DOMException("Unexpected node implementation, try importing")
    return node
}

internal fun INode.checkNode(node: INode): NodeImpl {
    if (node.getOwnerDocument() != getOwnerDocument()) throw DOMException("Node not owned by this document")
    if (node !is NodeImpl) throw DOMException("Unexpected node implementation, try importing")
    return node
}
