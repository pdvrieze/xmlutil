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

@file:UseSerializers(QNameSerializer::class, CompactFragmentSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment

@XmlSerialName("complexContent", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
@Serializable
class XSComplexContent(
    override val id: VID? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap(),
    override val annotations: List<XSAnnotation> = emptyList(),
    val derivation: RestrictionExtensionChoice

) : T_Annotated, G_ComplexTypeModel.ComplexContent {
    @Serializable
    sealed class RestrictionExtensionChoice

    @XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class Restriction(
        override val base: QName,
        override val id: VID? = null,
        override val annotations: List<XSAnnotation> = emptyList(),
        override val openContents: List<XSOpenContent> = emptyList(),
        override val groups: List<XSGroupRef> = emptyList(), // TODO shouldn't be lists
        override val alls: List<XSAll> = emptyList(),
        override val choices: List<XSChoice> = emptyList(),
        override val sequences: List<XSSequence> = emptyList(),
        override val asserts: List<T_Assertion> = emptyList(),
        override val attributes: List<XSLocalAttribute> = emptyList(),
        override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        override val anyAttribute: XSAnyAttribute? = null,
        override val simpleTypes: List<XSLocalSimpleType> = emptyList(),
        override val facets: List<XSFacet> = emptyList(),
        override val otherContents: List<CompactFragment> = emptyList(),
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()

    ) : RestrictionExtensionChoice(), T_ComplexRestrictionType

    @XmlSerialName("extension", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class Extension(
        override val base: QName,
        override val id: VID? = null,
        override val groups: List<XSGroupRef> = emptyList(),
        override val alls: List<XSAll> = emptyList(),
        override val choices: List<XSChoice> = emptyList(),
        override val sequences: List<XSSequence> = emptyList(),
        override val asserts: List<XSAssert> = emptyList(),
        override val attributes: List<XSLocalAttribute> = emptyList(),
        override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        override val anyAttribute: XSAnyAttribute? = null,
        override val annotations: List<XSAnnotation> = emptyList(),
        override val openContents: List<XSOpenContent> = emptyList(),
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()

    ) : RestrictionExtensionChoice(), T_ExtensionType


}

