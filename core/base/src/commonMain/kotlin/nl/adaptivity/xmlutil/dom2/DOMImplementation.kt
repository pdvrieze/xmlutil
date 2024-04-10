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

package nl.adaptivity.xmlutil.dom2

public interface DOMImplementation {
    public val supportsWhitespaceAtToplevel: Boolean

    public fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType
    public fun createDocument(namespace: String? = null, qualifiedName: String? = null, documentType: DocumentType? = null): Document

    @OptIn(ExperimentalStdlibApi::class)
    public fun hasFeature(feature: String, version:String?): Boolean {
        val f = SupportedFeatures.entries.firstOrNull { it.strName == feature } ?: return false
        val v = when {
            version.isNullOrEmpty() -> null
            else -> DOMVersion.entries.firstOrNull { it.strName == version } ?: return false
        }
        return hasFeature(f, v)
    }

    public fun hasFeature(feature: SupportedFeatures, version: DOMVersion?): Boolean {
        return version == null || feature.isSupportedVersion(version)
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

}
