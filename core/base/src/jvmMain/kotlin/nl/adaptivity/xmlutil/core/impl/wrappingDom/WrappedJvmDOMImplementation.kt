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

import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.DOMVersion
import nl.adaptivity.xmlutil.dom2.DocumentType
import nl.adaptivity.xmlutil.dom2.SupportedFeatures
import javax.xml.parsers.DocumentBuilderFactory

internal object WrappedJvmDOMImplementation : DOMImplementation {
    val delegate: PlatformDOMImplementation =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().domImplementation

    override val supportsWhitespaceAtToplevel: Boolean get() = true

    override fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): WrappedJvmDocumentType {
        return delegate.createDocumentType(qualifiedName, publicId, systemId).wrap()
    }

    override fun hasFeature(feature: String, version: String?): Boolean {
        return delegate.hasFeature(feature, version)
    }

    override fun hasFeature(feature: SupportedFeatures, version: DOMVersion?): Boolean {
        return version == null || feature.isSupportedVersion(version)
    }


    override fun getFeature(feature: String, version: String): Any? {
        return delegate.getFeature(feature, version)
    }

    override fun createDocument(
        namespace: String?,
        qualifiedName: String?,
        documentType: PlatformDocumentType?
    ): WrappedJvmDocument {
        val dt = documentType?.unWrap()
        return delegate.createDocument(namespace, qualifiedName, dt).wrap()
    }

    override fun createDocument(
        namespace: String?,
        qualifiedName: String?,
        documentType: DocumentType?
    ): WrappedJvmDocument {
        val dt = documentType?.unWrap() as PlatformDocumentType?
        return delegate.createDocument(namespace, qualifiedName, dt).wrap()
    }
}
