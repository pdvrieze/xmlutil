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
import nl.adaptivity.xmlutil.core.impl.idom.INamedNodeMap
import org.w3c.dom.DocumentType

internal class DocumentTypeImpl(delegate: DocumentType) : NodeImpl<DocumentType>(delegate), IDocumentType {
    override fun getName(): String = delegate.name

    override fun getEntities(): INamedNodeMap = WrappingNamedNodeMap(delegate.entities)

    override fun getNotations(): INamedNodeMap {
        return WrappingNamedNodeMap(delegate.notations)
    }

    override fun getPublicId(): String = delegate.publicId

    override fun getSystemId(): String = delegate.systemId

    override fun getInternalSubset(): String = delegate.internalSubset

    override fun getFirstChild(): Nothing? = null

    override fun getLastChild(): Nothing? = null
}
