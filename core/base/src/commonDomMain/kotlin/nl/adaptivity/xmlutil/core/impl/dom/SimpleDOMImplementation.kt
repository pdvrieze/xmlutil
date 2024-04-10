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

import nl.adaptivity.xmlutil.core.impl.idom.IDOMImplementation
import nl.adaptivity.xmlutil.core.impl.idom.IDocument
import nl.adaptivity.xmlutil.core.impl.idom.IDocumentType

internal object SimpleDOMImplementation : IDOMImplementation {
    override val supportsWhitespaceAtToplevel: Boolean get() = true

    override fun createDocument(namespace: String?, qualifiedName: String): IDocument =
        createDocument(namespace, qualifiedName, null)

    override fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): IDocumentType {
        return DocumentTypeImpl(DocumentImpl(null), qualifiedName, publicId, systemId)
    }

    override fun createDocument(namespace: String?, qualifiedName: String?, documentType: IDocumentType?): IDocument {
        return DocumentImpl(documentType).also {
            (documentType as DocumentTypeImpl?)?.ownerDocument = it
            if (qualifiedName != null) {
                val elem = when (namespace) {
                    null -> it.createElement(qualifiedName)
                    else -> it.createElementNS(namespace, qualifiedName)
                }
                it.appendChild(elem)
            } else {
                require(namespace.isNullOrEmpty()) { "Creating documents with a namespace but no qualified name is not possible" }
            }
        }
    }
}
