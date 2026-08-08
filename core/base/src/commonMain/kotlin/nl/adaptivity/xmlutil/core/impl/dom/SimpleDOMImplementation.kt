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

import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom2.DOMVersion
import nl.adaptivity.xmlutil.dom2.DocumentType
import nl.adaptivity.xmlutil.dom2.SupportedFeatures
import nl.adaptivity.xmlutil.dom2.impl.AbstractDOMImplementation

internal object SimpleDOMImplementation : AbstractDOMImplementation() {
    override val supportsWhitespaceAtToplevel: Boolean get() = true


    override fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentTypeImpl {
        return DocumentTypeImpl(null, qualifiedName, publicId, systemId)
    }

    override fun createDocument(namespace: String?, qualifiedName: String?, documentType: DocumentType?): DocumentImpl {
        return DocumentImpl(documentType).also { doc ->
            if (documentType != null) {
                if (documentType.getOwnerDocument() != null) throw DOMException.wrongDocumentErr("Document type already has an owner document")
                if (documentType !is DocumentTypeImpl) throw DOMException.wrongDocumentErr("Incorrect document type implementation")
                documentType.setOwnerDocument(doc)
            }
            if (!qualifiedName.isNullOrBlank()) {
                val elem = when (namespace) {
                    null -> doc.createElement(qualifiedName)
                    else -> doc.createElementNS(namespace, qualifiedName)
                }
                check (elem.getOwnerDocument() == doc) { "Owner document mismatch" }
                doc.appendChild(elem)
            } else if (!namespace.isNullOrEmpty()) {
                throw DOMException.namespaceErr("Creating documents with a namespace but no qualified name is not possible")
            }
        }
    }

    override fun hasFeature(feature: String, version: String?): Boolean {
        if (feature.isEmpty()) return false
        val featureName = when (feature[0]) {
            '+' -> feature.substring(1)
            else -> feature
        }
        val feature = SupportedFeatures.entries.firstOrNull { it.strName == featureName } ?: return false
        val version = DOMVersion.entries.firstOrNull { it.strName == version }
        return hasFeature(feature, version)
    }

    override fun hasFeature(
        feature: SupportedFeatures,
        version: DOMVersion?
    ): Boolean {
        if (version != null) return feature.isSupportedVersion(version)
        return true
    }

    override fun getFeature(feature: String, version: String): Any? {
        return when {
            hasFeature(feature, version) -> this
            else -> null
        }
    }
}
