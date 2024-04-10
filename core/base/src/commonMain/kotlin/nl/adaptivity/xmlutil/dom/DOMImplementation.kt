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

package nl.adaptivity.xmlutil.dom

@Suppress("DEPRECATION")
@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.DOMImplementation", "nl.adaptivity.xmlutil.dom2")
)
public expect interface DOMImplementation {
    public fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): DocumentType
    public fun createDocument(namespace: String?, qualifiedName: String?, documentType: DocumentType?): Document
}

@Suppress("DEPRECATION", "EXTENSION_SHADOWED_BY_MEMBER", "KotlinRedundantDiagnosticSuppress")
public fun DOMImplementation.createDocument(namespace: String?, qualifiedName: String): Document =
    createDocument(namespace, qualifiedName, null)
