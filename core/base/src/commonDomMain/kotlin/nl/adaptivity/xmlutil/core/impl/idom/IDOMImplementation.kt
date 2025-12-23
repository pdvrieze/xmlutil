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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.core.impl.dom.DocumentTypeImpl
import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom2.DOMVersion
import nl.adaptivity.xmlutil.dom2.SupportedFeatures

public interface IDOMImplementation : PlatformDOMImplementation {
    override fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): IDocumentType

    override fun createDocument(namespace: String?, qualifiedName: String?, documentType: PlatformDocumentType?): IDocument =
        createDocument(namespace, qualifiedName, documentType?.let(DocumentTypeImpl.Companion::coerce))

    public fun createDocument(namespace: String?, qualifiedName: String?, documentType: IDocumentType?): IDocument

    public override fun hasFeature(feature: String, version: String?): Boolean {
        val f = SupportedFeatures.entries.firstOrNull { it.strName == feature } ?: return false
        val v = when {
            version.isNullOrEmpty() -> null
            else -> DOMVersion.entries.firstOrNull { it.strName == version } ?: return false
        }
        return hasFeature(f, v)
    }

    public override fun hasFeature(feature: SupportedFeatures, version: DOMVersion?): Boolean {
        return version == null || feature.isSupportedVersion(version)
    }

}
