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

import io.github.pdvrieze.formats.xmlschema.impl.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSimpleContentRestriction: XSSimpleContentDerivation, SimpleRestrictionModel {

    constructor(
        simpleType: XSLocalSimpleType? = null,
        facets: List<XSFacet> = emptyList(),
        base: QName? = null,
        id: VID? = null,
        attributes: List<XSLocalAttribute> = emptyList(),
        attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        anyAttribute: XSAnyAttribute? = null,
        asserts: List<XSAssert> = emptyList(),
        annotation: XSAnnotation? = null,
        otherContents: List<CompactFragment> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(id, attributes, attributeGroups, anyAttribute, asserts, annotation, otherAttrs) {
        this.base = base
        this.simpleType = simpleType
        this.facets = facets
        this.otherContents = otherContents
    }

    override val base: SerializableQName?

    override val simpleType: XSLocalSimpleType?

    override val facets: List<XSFacet>

    val otherContents: List<@Serializable(CompactFragmentSerializer::class) CompactFragment>

    /**
     * Mark the derivation as restriction
     */
    @Deprecated("Use 'is XSSimpleContentRestriction' instead")
    override val derivationMethod: VDerivationControl.RESTRICTION get() = VDerivationControl.RESTRICTION
}
