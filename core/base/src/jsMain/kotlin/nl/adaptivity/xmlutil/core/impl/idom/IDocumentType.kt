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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom.DocumentType as DocumentType1
import nl.adaptivity.xmlutil.dom2.DocumentType as DocumentType2
import org.w3c.dom.DocumentType as DomDocumentType

public interface IDocumentType : INode, DocumentType1, DocumentType2 {
    override val delegate: DomDocumentType

    override fun getName(): String = name

    override fun getPublicId(): String = publicId

    override fun getSystemId(): String = systemId

}
