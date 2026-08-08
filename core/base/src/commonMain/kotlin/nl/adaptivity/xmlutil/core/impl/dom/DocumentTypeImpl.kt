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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.getName
import nl.adaptivity.xmlutil.dom.getPublicId
import nl.adaptivity.xmlutil.dom.getSystemId
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocumentType

@XmlUtilInternal
internal class DocumentTypeImpl internal constructor(
    maybeOwnerDocument: DocumentImpl?,
    name: String,
    publicId: String,
    systemId: String
) : AbstractDocumentType<NodeImpl, ParentNodeImpl>(maybeOwnerDocument), NodeImpl, DocumentType {
    private val _name = name
    private val _publicId = publicId
    private val _systemId = systemId

    internal constructor(original: PlatformDocumentType) : this(
        null/*original.getOwnerDocument()?.let { DocumentImpl.coerce(it) }*/,
        original.getName(),
        original.getPublicId(),
        original.getSystemId()
    )

    override fun getOwnerDocument(): DocumentImpl? {
        return super.getOwnerDocument() as DocumentImpl?
    }

    override fun getName(): String = _name

    override fun getPublicId(): String = _publicId

    override fun getSystemId(): String = _systemId

    override fun getEntities(): NamedNodeMap<Entity> {
        return EmptyNamedNodeMap
    }

    override fun getNotations(): NamedNodeMap<Notation> {
        return EmptyNamedNodeMap
    }

    override fun cloneNode(deep: Boolean): AbstractDocumentType<NodeImpl, ParentNodeImpl> {
        return DocumentTypeImpl(null, getName(), getPublicId(), getSystemId())
    }

    public companion object {
        internal fun coerce(doctype: PlatformDocumentType): DocumentTypeImpl {
            return (doctype as? DocumentTypeImpl)?.takeIf { it.getOwnerDocument() == null } ?: DocumentTypeImpl(doctype)
        }

    }
}
