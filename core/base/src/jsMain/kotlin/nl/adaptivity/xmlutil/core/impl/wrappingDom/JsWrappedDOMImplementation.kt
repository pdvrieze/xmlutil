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

import kotlinx.browser.document
import nl.adaptivity.xmlutil.core.impl.dom.SimpleDOMImplementation
import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom2.DOMVersion
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.DocumentType
import nl.adaptivity.xmlutil.dom2.SupportedFeatures
import org.w3c.dom.parsing.DOMParser
import nl.adaptivity.xmlutil.dom2.DOMImplementation as DOMImplementation2
import org.w3c.dom.Document as DomDocument

internal object JsWrappedDOMImplementation : DOMImplementation2 {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    val delegate: PlatformDOMImplementation by lazy {
        runCatching { document.implementation }
            .recoverCatching { DOMParser().parseFromString("<root></root>", "text/xml").implementation }
            .map { it as PlatformDOMImplementation }
            .getOrDefault(SimpleDOMImplementation)
    }

    override val supportsWhitespaceAtToplevel: Boolean get() = true

    override fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType {
        return delegate.createDocumentType(qualifiedName, publicId, systemId).wrap() as DocumentType
    }

    override fun createDocument(
        namespace: String?,
        qualifiedName: String?,
        documentType: PlatformDocumentType?
    ): Document {
        if (documentType != null && documentType !is JsWrappedDocumentType) {
            throw IllegalArgumentException("Only JsWrappedDocumentType is supported")
        }
        return createDocument(
            namespace,
            qualifiedName,
            documentType?.unWrap() as? DocumentType
        )
    }

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    override fun createDocument(namespace: String?, qualifiedName: String?, documentType: DocumentType?): Document {
        val documentType1 = documentType?.unWrap() as? PlatformDocumentType
        return when (val d = delegate.createDocument(namespace, qualifiedName, documentType1)) {
            is Document -> d
            else -> JsWrappedDocument(d as DomDocument)
        }
    }

    override fun hasFeature(feature: String, version: String?): Boolean {
        val f = SupportedFeatures.entries.firstOrNull { it.strName == feature } ?: return false
        val v = when {
            version.isNullOrEmpty() -> null
            else -> DOMVersion.entries.firstOrNull { it.strName == version } ?: return false
        }
        return hasFeature(f, v)
    }

    override fun hasFeature(feature: SupportedFeatures, version: DOMVersion?): Boolean {
        return version == null || feature.isSupportedVersion(version)
    }

    override fun getFeature(feature: String, version: String): Any? {
        return delegate.asDynamic().getFeature(feature, version)
    }
}
