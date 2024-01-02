/*
 * Copyright (c) 2023.
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
import org.w3c.dom.DOMImplementation
import javax.xml.parsers.DocumentBuilderFactory

internal object DOMImplementationImpl : IDOMImplementation {
    val delegate: DOMImplementation =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().domImplementation

    override val supportsWhitespaceAtToplevel: Boolean get() = true

    override fun createDocumentType(qualifiedName: String, publicId: String, systemId: String): IDocumentType {
        return delegate.createDocumentType(qualifiedName, publicId, systemId).wrap()
    }

    override fun createDocument(namespace: String?, qualifiedName: String?, documentType: IDocumentType?): IDocument {
        return delegate.createDocument(namespace, qualifiedName, documentType).wrap()
    }

    override fun hasFeature(feature: String, version: String?): Boolean {
        return delegate.hasFeature(feature, version)
    }

    override fun getFeature(feature: String, version: String?): Any {
        return delegate.getFeature(feature, version)
    }
}
