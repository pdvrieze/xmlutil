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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSVER_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
abstract class XSOpenAttrsBase(
    @XmlOtherAttributes final override val otherAttrs: Map<@Serializable(with = QNameSerializer::class) QName, String> = emptyMap(),
    @XmlSerialName("minVersion", XMLConstants.XSVER_NS_URI, XSVER_PREFIX)
    override val vcMinVersion: SchemaVersion? = null,
    @XmlSerialName("maxVersion", XMLConstants.XSVER_NS_URI, XSVER_PREFIX)
    override val vcMaxVersion: SchemaVersion? = null,
    @XmlSerialName("typeAvailable", XMLConstants.XSVER_NS_URI, XSVER_PREFIX)
    override val vcTypeAvailable: List<SerializableQName>? = null,
    @XmlSerialName("typeUnavailable", XMLConstants.XSVER_NS_URI, XSVER_PREFIX)
    override val vcTypeUnAvailable: List<SerializableQName>? = null,
    @XmlSerialName("facetAvailable", XMLConstants.XSVER_NS_URI, XSVER_PREFIX)
    override val vcFacetAvailable: List<SerializableQName>? = null,
    @XmlSerialName("facetUnavailable", XMLConstants.XSVER_NS_URI, XSVER_PREFIX)
    override val vcFacetUnAvailable: List<SerializableQName>? = null,
) : XSI_OpenAttrs {

    init {
        for (attrName in otherAttrs.keys) {
            require(attrName.namespaceURI.let { it.isNotEmpty() && it != XSD_NS_URI }) {
                "Invalid \"open\" attribute name $attrName"
            }
        }
    }


}
