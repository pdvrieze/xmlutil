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
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.impl.AbstractAttrStorage
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocument
import nl.adaptivity.xmlutil.dom2.impl.AbstractNodeList
import nl.adaptivity.xmlutil.dom2.impl.LinearNodeStorage
import nl.adaptivity.xmlutil.isXmlWhitespace

@XmlUtilInternal
public class DocumentImpl private constructor(doctype: DocumentTypeImpl?) :
    AbstractDocument<NodeImpl, ParentNodeImpl>({
        NodeStorage(it as DocumentImpl)
    }), ParentNodeImpl, Document {

    init {
        if (doctype?.getOwnerDocument() != null) throw DOMException.wrongDocumentErr("Document type already used for a different document")
        doctype?.setOwnerDocument(this)
    }

    private val _doctype = doctype
    override val self: DocumentImpl get() = this

    private val docId = nextDocId()

    internal constructor(doctype1: PlatformDocumentType?) : this(doctype = doctype1?.let(DocumentTypeImpl::coerce))

    override fun getDoctype(): DocumentTypeImpl? = _doctype

    override fun getImplementation(): DOMImplementation = SimpleDOMImplementation

    private var _documentElement: ElementImpl? = null
    override fun getDocumentElement(): ElementImpl? = _documentElement

    private var _inputEncoding: String = "UTF-8"

    override fun getInputEncoding(): String = _inputEncoding

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

    override fun importNode(
        node: PlatformNode,
        deep: Boolean
    ): NodeImpl {
        return super.importNode(node, deep)
    }

    override fun cloneNode(deep: Boolean): DocumentImpl {
        return DocumentImpl(doctype = _doctype?.cloneNode(deep) as DocumentTypeImpl?).also { d ->
            if (deep) {
                for (c in getChildNodes()) d.appendChild(
                    c.cloneNode(deep)
                )
            }
        }
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

    private class NodeStorage(private val document: DocumentImpl): LinearNodeStorage<NodeImpl, ParentNodeImpl>(StorageAdapter(document)), AbstractNodeList<NodeImpl, ParentNodeImpl> {
        private var docElem: ElementImpl?
            get() = document._documentElement
            set(value) { document._documentElement = value}

        override fun appendChild(parent: ParentNodeImpl, node: NodeImpl) {
            when (node) {
                is ElementImpl -> when (docElem) {
                    null -> docElem = node
                    else -> throw DOMException.hierarchyRequestErr("Documents may only have one root element")

                }

                is CDATASectionImpl -> throw DOMException.hierarchyRequestErr("CDATA sections cannot be added directly to a document")

                is TextImpl if (! isXmlWhitespace(node.getData())) ->
                    throw DOMException.hierarchyRequestErr("Non-whitespace text nodes cannot be added directly to a document")
            }
            super.appendChild(parent, node)
        }

        override fun removeChild(parent: ParentNodeImpl, node: NodeImpl): NodeImpl {
            if (node === docElem) docElem = null
            return super.removeChild(parent, node)
        }

        override fun replaceChild(parent: ParentNodeImpl, newChild: NodeImpl, oldChild: NodeImpl): NodeImpl {
            if (oldChild === docElem) docElem = null

            when (newChild) {
                is ElementImpl -> when (docElem) {
                    null -> docElem = newChild
                    else -> throw DOMException.hierarchyRequestErr("Document may only have one root element")
                }

                is CDATASectionImpl -> throw DOMException.hierarchyRequestErr("CDATA sections cannot be added directly to a document")

                is TextImpl if (! isXmlWhitespace(newChild.getData())) ->
                    throw DOMException.hierarchyRequestErr("Non-whitespace text nodes cannot be added directly to a document")
            }

            return super.replaceChild(parent, newChild, oldChild)
        }
    }

    internal val storageAdapter: StorageAdapter get() = (nodeStorage as NodeStorage).adapter as StorageAdapter

    internal class StorageAdapter(private val ownerDocument: DocumentImpl): LinearNodeStorage.Adapter<NodeImpl, ParentNodeImpl>, AbstractAttrStorage.Adapter<AttrImpl> {
        override fun checkTypeAndOwner(node: PlatformNode): NodeImpl = when (node) {
            !is NodeImpl -> throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
            else if node.getOwnerDocument() != ownerDocument -> throw DOMException.wrongDocumentErr("Node not owned by this document")
            else -> node
        }

        override fun checkAttr(a: PlatformNode): AttrImpl {
            return when (a) {
                is AttrImpl -> a
                else -> throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
            }
        }
    }

}
