/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.types.I_AttributeContainer
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleRestrictionType
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSimpleContentRestriction: XSSimpleContentDerivation, I_AttributeContainer, SimpleRestrictionModel,
    T_SimpleRestrictionType {

    constructor(
        simpleType: XSLocalSimpleType? = null,
        facets: List<XSFacet> = emptyList(),
        base: QName,
        id: VID? = null,
        attributes: List<XSLocalAttribute> = emptyList(),
        attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        anyAttribute: XSAnyAttribute? = null,
        assertions: List<XSAssert> = emptyList(),
        annotation: XSAnnotation? = null,
        otherContents: List<CompactFragment> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ): super(id, attributes, attributeGroups, anyAttribute, assertions, annotation, otherAttrs) {
        this.base = base
        this.simpleType = simpleType
        this.facets = facets
        this.otherContents = otherContents
    }

    override val base: @Serializable(QNameSerializer::class) QName

    override val simpleType: XSLocalSimpleType?

    override val facets: List<XSFacet>

    override val otherContents: List<@Serializable(CompactFragmentSerializer::class) CompactFragment>

}
