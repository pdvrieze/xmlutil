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

import kotlinx.browser.document
import nl.adaptivity.xmlutil.core.impl.idom.IDOMImplementation
import nl.adaptivity.xmlutil.core.impl.idom.IDocument
import nl.adaptivity.xmlutil.core.impl.idom.IDocumentType
import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom2.DOMVersion
import nl.adaptivity.xmlutil.dom2.SupportedFeatures
import org.w3c.dom.DOMImplementation
import org.w3c.dom.parsing.DOMParser

internal object DOMImplementationImpl : IDOMImplementation {
    val delegate: DOMImplementation by lazy {
        runCatching { document.implementation }
            .recoverCatching { DOMParser().parseFromString("<root></root>", "text/xml").implementation }
            .getOrThrow()
    }

    override val supportsWhitespaceAtToplevel: Boolean get() = true

    override fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): IDocumentType {
        return delegate.createDocumentType(qualifiedName, publicId, systemId).wrap()
    }

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    override fun createDocument(namespace: String?, qualifiedName: String?, documentType: IDocumentType?): IDocument {
        val documentType1 = documentType?.unWrap() as? PlatformDocumentType
        return (delegate as PlatformDOMImplementation).createDocument(namespace, qualifiedName, documentType1)
            .wrap() as IDocument
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
