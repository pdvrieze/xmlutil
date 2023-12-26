/*
 * Copyright (c) 2022.
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

import nl.adaptivity.xmlutil.core.impl.idom.IDocumentType
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom.DocumentType as DocumentType1
import nl.adaptivity.xmlutil.dom2.DocumentType as DocumentType2

internal class DocumentTypeImpl(
    ownerDocument: DocumentImpl,
    override val name: String,
    override val publicId: String,
    override val systemId: String
) : NodeImpl(ownerDocument), IDocumentType {
    constructor(original: DocumentType1) : this(
        DocumentImpl.coerce(original.ownerDocument),
        original.name,
        original.publicId,
        original.systemId
    )

    constructor(original: DocumentType2) : this(
        DocumentImpl.coerce(original.getOwnerDocument()),
        original.getName(),
        original.getPublicId(),
        original.getSystemId()
    )

    override var parentNode: INode? = null

    override val nodetype: NodeType get() = NodeType.DOCUMENT_TYPE_NODE

    override fun getNodeName(): String = getName()

    override fun getChildNodes(): INodeList = EmptyNodeList

    override fun getFirstChild(): Nothing? = null

    override fun getLastChild(): Nothing? = null

    override fun getTextContent(): String? = null

    override fun setTextContent(value: String) {
        throw UnsupportedOperationException("Documents have no (direct) text content")
    }

    override fun lookupPrefix(namespace: String): String? {
        return getParentNode()?.lookupPrefix(namespace)
    }

    override fun lookupNamespaceURI(prefix: String): String? {
        return getParentNode()?.lookupNamespaceURI(prefix)
    }

    override fun appendChild(node: INode): Nothing {
        throw DOMException("Document types have no children")
    }

    override fun replaceChild(oldChild: INode, newChild: INode): Nothing {
        throw DOMException("Document types have no children")
    }

    override fun removeChild(node: INode): Nothing {
        throw DOMException("Document types have no children")
    }

    companion object {
        fun coerce(doctype: DocumentType1): DocumentTypeImpl {
            return doctype as? DocumentTypeImpl ?: DocumentTypeImpl(doctype)
        }

        fun coerce(doctype: DocumentType2): DocumentTypeImpl {
            return doctype as? DocumentTypeImpl ?: DocumentTypeImpl(doctype)
        }

    }
}
