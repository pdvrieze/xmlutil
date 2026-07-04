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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom.PlatformDocumentType

public fun DOMImplementation.createDocument(namespace: String? = null, qualifiedName: String? = null, documentType: DocumentType? = null): Document {
    return createDocument(namespace, qualifiedName, documentType)
}

public expect interface DOMImplementation : PlatformDOMImplementation {
    public val supportsWhitespaceAtToplevel: Boolean

    /**
     * @since DOM Level 2
     */
    public fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType

    public fun createDocument(namespace: String?, qualifiedName: String?, documentType: PlatformDocumentType?): Document
    /**
     * @since DOM Level 2
     */
    public fun createDocument(namespace: String?, qualifiedName: String?, documentType: DocumentType?): Document

    /**
     * @since DOM Level 3
     */
    public fun getFeature(feature: String, version: String): Any?

    /**
     * @since DOM Level 1
     */
    public fun hasFeature(feature: String, version: String?): Boolean

    public fun hasFeature(feature: SupportedFeatures, version: DOMVersion?): Boolean


}

public enum class SupportedFeatures(public val strName: String) {
    CORE("Core") {
        override fun isSupportedVersion(version: DOMVersion): Boolean = true
    },

    XML("XML") {
        override fun isSupportedVersion(version: DOMVersion): Boolean = true
    }
    ;

    public abstract fun isSupportedVersion(version: DOMVersion): Boolean
}

public enum class DOMVersion(public val strName: String) {
    V1("1.0"),
    V2("2.0"),
    V3("3.0"),
}
