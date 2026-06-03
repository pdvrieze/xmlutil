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

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.dom2.DocumentType
import org.w3c.dom.DocumentType as DomDocumentType

@JsName("DocumentType")
public actual external interface PlatformDocumentType : PlatformNode {
/*
    public val name: String
    public val publicId: String
    public val systemId: String
*/
}

public actual fun PlatformDocumentType.getOwnerDocument(): PlatformDocument? = when (this) {
    is DocumentType -> getOwnerDocument()
    else -> unsafeCast<DomDocumentType>().ownerDocument.asDynamic()
}

public actual fun PlatformDocumentType.getName(): String = when (this) {
    is DocumentType -> getName()
    else -> unsafeCast<DomDocumentType>().name
}

public actual fun PlatformDocumentType.getPublicId(): String = when (this) {
    is DocumentType -> getPublicId()
    else -> unsafeCast<DomDocumentType>().publicId
}

public actual fun PlatformDocumentType.getSystemId(): String = when (this) {
    is DocumentType -> getSystemId()
    else -> unsafeCast<DomDocumentType>().systemId
}
