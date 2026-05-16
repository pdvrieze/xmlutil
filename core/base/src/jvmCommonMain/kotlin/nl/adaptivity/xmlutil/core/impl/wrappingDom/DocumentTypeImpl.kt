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

import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.DocumentType

internal class DocumentTypeImpl(delegate: PlatformDocumentType) : AbstractNodeImpl<PlatformDocumentType>(delegate),
    DocumentType {

    override fun getName(): String = delegate.name

    @Deprecated("No-op for now")
    override fun getEntities(): WrappingNamedNodeMap = WrappingNamedNodeMap(delegate.entities)

    @Deprecated("No-op for now")
    override fun getNotations(): WrappingNamedNodeMap {
        return WrappingNamedNodeMap(delegate.notations)
    }

    override fun getNodeValue(): Nothing? = null
    override fun setNodeValue(value: String?) {
        delegate.nodeValue = value
    }

    override fun getAttributes(): Nothing? = null

    override fun getPublicId(): String = delegate.publicId

    override fun getSystemId(): String = delegate.systemId

    @Deprecated("No-op for now")
    override fun getInternalSubset(): String = delegate.internalSubset

    override fun getFirstChild(): Nothing? = null

    override fun getLastChild(): Nothing? = null

    override fun appendChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in documenttype")

    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in documenttype")

    override fun removeChild(node: PlatformNode): Nothing =
        throw UnsupportedOperationException("No children in documenttype")

    override fun cloneNode(deep: Boolean): DocumentTypeImpl {
        return DocumentTypeImpl(delegate.cloneNode(deep) as PlatformDocumentType)
    }
}
