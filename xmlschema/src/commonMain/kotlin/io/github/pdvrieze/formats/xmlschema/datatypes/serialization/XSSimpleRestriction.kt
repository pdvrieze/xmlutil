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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.impl.invariant
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlIgnoreWhitespace(true)
@XmlSerialName("restriction", XSD_NS_URI, XSD_PREFIX)
class XSSimpleRestriction : XSSimpleDerivation, XSI_Annotated {

    @XmlElement(false)
    val base: SerializableQName?
    @XmlBefore("facets")
    val simpleType: XSLocalSimpleType?
    val facets: List<XSFacet>

    @XmlValue(true)
    val otherContents: List<CompactFragment>

    // Requires an embedded restriction
    constructor(
        base: QName? = null,
        simpleType: XSLocalSimpleType? = null,
        facets: List<XSFacet> = emptyList(),
        otherContents: List<CompactFragment> = emptyList(),
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) : super(id, annotation, otherAttrs) {
        invariant(base == null || ':' !in base.localPart) {
            "Invalid QName"
        }
        this.base = base
        this.simpleType = simpleType
        this.facets = facets
        this.otherContents = otherContents
    }

    // Requires an embedded restriction
    constructor(
        simpleType: XSLocalSimpleType?,
        facets: List<XSFacet>,
    ) : this(base = null, simpleType = simpleType, facets = facets)
}

