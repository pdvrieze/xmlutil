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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("annotation", XSD_NS_URI, XSD_PREFIX)
class XSAnnotation : XSOpenAttrsBase {
    init {
        otherAttrs.keys.forEach { it ->
            require(it.prefix.isNotEmpty() && it.namespaceURI.isNotEmpty()) { "Other attributes must not be in the default namespace: $it" }
        }
    }

    val documentationElements: List<XSDocumentation>
    val appInfos: List<XSAppInfo>

    @XmlId
    val id: VID?

    constructor(
        documentationElements: List<XSDocumentation> = emptyList(),
        appInfos: List<XSAppInfo> = emptyList(),
        id: VID? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(otherAttrs) {
        this.documentationElements = documentationElements
        this.appInfos = appInfos
        this.id = id
    }

}

