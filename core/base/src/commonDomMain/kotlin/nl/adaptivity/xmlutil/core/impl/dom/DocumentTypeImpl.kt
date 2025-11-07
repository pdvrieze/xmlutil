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

import nl.adaptivity.xmlutil.core.impl.idom.IDocumentType
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom2.NodeType
import nl.adaptivity.xmlutil.dom.PlatformDocumentType as DocumentType1

internal class DocumentTypeImpl(
    var maybeOwnerDocument: DocumentImpl?,
    private val name: String,
    private val publicId: String,
    private val systemId: String
) : NodeImpl(), IDocumentType {
    constructor(original: DocumentType1) : this(
        DocumentImpl.coerce(original.getOwnerDocument()),
        original.getName(),
        original.getPublicId(),
        original.getSystemId()
    )

    override fun getOwnerDocument(): DocumentImpl = maybeOwnerDocument!!

    override fun setOwnerDocument(ownerDocument: DocumentImpl) {
        this.maybeOwnerDocument = ownerDocument
    }

    override fun getName(): String = name

    override fun getPublicId(): String = publicId

    override fun getSystemId(): String = systemId

    override fun getNodetype(): NodeType = NodeType.DOCUMENT_TYPE_NODE

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

    companion object {
        fun coerce(doctype: DocumentType1): DocumentTypeImpl {
            return doctype as? DocumentTypeImpl ?: DocumentTypeImpl(doctype)
        }

    }
}
