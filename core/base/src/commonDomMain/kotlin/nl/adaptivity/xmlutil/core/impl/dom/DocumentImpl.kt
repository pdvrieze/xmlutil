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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.*
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocument
import nl.adaptivity.xmlutil.dom2.impl.AbstractNodeList
import nl.adaptivity.xmlutil.dom2.impl.AbstractNodeStorage
import nl.adaptivity.xmlutil.dom2.impl.SiblingIterator
import nl.adaptivity.xmlutil.isXmlWhitespace

@XmlUtilInternal
public class DocumentImpl private constructor(private val doctype: DocumentTypeImpl?) :
    AbstractDocument<NodeImpl, ParentNodeImpl>({ (it as DocumentImpl).NodeStorage() }), ParentNodeImpl, Document {

    init {
        if (doctype?.getOwnerDocument() != null) throw DOMException.wrongDocumentErr("Document type already used for a different document")
        doctype?.setOwnerDocument(this)
    }

    override val self: DocumentImpl get() = this

    private inner class NodeStorage(): AbstractNodeStorage<NodeImpl, ParentNodeImpl>, AbstractNodeList<NodeImpl, ParentNodeImpl> {
        override fun getNodeList(): AbstractNodeList<NodeImpl, ParentNodeImpl> = this

        @XmlUtilInternal
        override fun checkTypeAndOwner(parent: ParentNodeImpl, node: PlatformNode): ElementImpl {
            if (node !is ElementImpl) throw DOMException.wrongDocumentErr("Only elements of the same type can be added to the document")
            if (node.ownerDocument != parent) throw DOMException.wrongDocumentErr("Node not owned by this document")
            return node
        }

        override fun appendChild(parent: ParentNodeImpl, node: NodeImpl) {
            when (node) {
                is TextImpl -> when {
                    isXmlWhitespace(node.getData()) -> return

                    else -> throw DOMException.notSupportedErr("Non-whitespace nodes cannot be added directly to a document")
                }

                is DocumentFragmentImpl -> node.getChildNodes().forEach { appendChild(parent, it as NodeImpl) }
            }

            if (_documentElement != null) throw DOMException.hierarchyRequestErr("Only one root element is supported for now")


            _documentElement = node as ElementImpl
        }

        override fun removeChild(parent: ParentNodeImpl, node: NodeImpl): NodeImpl {
            if (node !== _documentElement) throw DOMException.notFoundErr("Node is not a child of this document")
            _documentElement = null
            return node
        }

        override fun replaceChild(parent: ParentNodeImpl, newChild: NodeImpl, oldChild: NodeImpl): NodeImpl {
            if (newChild.ownerDocument != this@DocumentImpl) throw DOMException.wrongDocumentErr("Node not owned by this document")

            _documentElement = newChild as ElementImpl
            return oldChild
        }

        override fun iterator(): Iterator<NodeImpl> {
            return _documentElement?.let { SiblingIterator(it) } ?: emptyList<Nothing>().iterator()
        }
    }

    private val docId = nextDocId()

    internal constructor(doctype1: PlatformDocumentType?) : this(doctype = doctype1?.let(DocumentTypeImpl::coerce))

    override fun getDoctype(): DocumentTypeImpl? = doctype

    override fun getImplementation(): DOMImplementation = SimpleDOMImplementation

    private var _documentElement: ElementImpl? = null
    override fun getDocumentElement(): ElementImpl? = _documentElement

    override var characterSet: String = "UTF-8"

    override fun getInputEncoding(): String = characterSet

    override fun getParentNode(): Nothing? = null
    override fun getParentElement(): Nothing? = null

    private val _childNodes: NodeListImpl = NodeListImpl()

    override fun getChildNodes(): INodeListImpl = _childNodes

    override fun getFirstChild(): NodeImpl? = _childNodes.elements.firstOrNull()

    override fun getLastChild(): NodeImpl? = _childNodes.elements.lastOrNull()

    override fun getPreviousSibling(): NodeImpl? = null

    override fun getNextSibling(): NodeImpl? = null

    override fun getTextContent(): String? = null

    override fun setTextContent(value: String) {
        throw DOMException.notSupportedErr("Documents have no (direct) text content")
    }

    override fun adoptNode(node: PlatformNode): NodeImpl {
        if (node !is NodeImpl) throw DOMException.notSupportedErr("node is of a different implementation and cannot be adopted")

        return adoptNodeImpl(node)
    }

    override fun createDocumentFragment(): DocumentFragmentImpl {
        return DocumentFragmentImpl(this)
    }

    override fun createElement(localName: String): ElementImpl {
        if (localName.isEmpty()) throw DOMException.invalidCharacterErr("Element name cannot be empty")
        if (localName.indexOf(':') >= 0) throw DOMException.namespaceErr("Prefix in name without namespace uri")
        return ElementImpl(this, null, localName, null)
    }

    override fun createElementNS(namespaceURI: String, qualifiedName: String): ElementImpl {
        val localName = qualifiedName.substringAfterLast(':', qualifiedName)
        if (localName.isEmpty()) throw DOMException.invalidCharacterErr("Element name cannot be empty")
        val prefix = qualifiedName.substringBeforeLast(':', "").takeUnless { it.isEmpty() }?.also {
            if (namespaceURI.isEmpty()) throw DOMException.namespaceErr("Missing namespace in presence of a prefix")
        }
        return ElementImpl(this, namespaceURI, localName, prefix)
    }

    override fun createAttribute(localName: String): AttrImpl {
        return AttrImpl(this, null, localName, null, "")
    }

    override fun createAttributeNS(namespace: String?, qualifiedName: String): AttrImpl {
        val localName = qualifiedName.substringAfterLast(':', qualifiedName)
        val prefix = qualifiedName.substringBeforeLast(':', "").takeUnless { it.isEmpty() }
        return AttrImpl(this, namespace, localName, prefix, "")
    }

    override fun createTextNode(data: String): TextImpl {
        return TextImpl(this, data)
    }

    override fun createCDATASection(data: String): CDATASectionImpl {
        return CDATASectionImpl(this, data)
    }

    override fun createComment(data: String): CommentImpl {
        return CommentImpl(this, data)
    }

    override fun createProcessingInstruction(target: String, data: String): ProcessingInstructionImpl {
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
        else -> "document<$docId>"
//        else -> e.toString()
    }

    public companion object {
        private var nextDocId: Int = 1

        private fun nextDocId(): Int = nextDocId++

        internal fun coerce(document: PlatformDocument): DocumentImpl {
            return (document as? DocumentImpl) ?: throw DOMException.notSupportedErr("Documents can not be adopted")
        }
    }
}

internal fun PlatformNode.checkNode(node: PlatformNode): NodeImpl {
    if (node is DocumentImpl) return node
    if (getOwnerDocument() != node.getOwnerDocument()) throw DOMException.wrongDocumentErr("Node (${node.getNodetype()}) not owned by this document (${getOwnerDocument()} != ${node.getOwnerDocument()})")
    when (node) {
        !is NodeImpl -> throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
    }
//    if (node !is NodeImpl) throw DOMException("Unexpected node implementation, try importing")
    return node
}

internal fun Node.checkNode(node: Node): NodeImpl {
    if (node is DocumentImpl) return node
    if (ownerDocument != node.getOwnerDocument()) throw DOMException.wrongDocumentErr("Node (${node.getNodetype()}) not owned by this document")
    if (node !is LeafNodeImpl) throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
    return node
}
