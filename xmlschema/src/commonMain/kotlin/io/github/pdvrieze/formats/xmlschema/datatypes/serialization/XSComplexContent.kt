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
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment

@XmlSerialName("complexContent", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
@Serializable
class XSComplexContent(
    override val id: VID? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap(),
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    override val derivation: XSDerivationBase
) : IXSComplexContent, T_ComplexType.ComplexContent {
    @Serializable
    sealed class XSDerivationBase: T_ComplexDerivation {
        abstract override val groups: List<XSGroupRef>
        abstract override val alls: List<XSAll>
        abstract override val choices: List<XSChoice>
        abstract override val sequences: List<XSSequence>
        abstract override val asserts: List<XSAssertionFacet>
        abstract override val attributes: List<XSLocalAttribute>
        abstract override val attributeGroups: List<XSAttributeGroupRef>
        abstract val derivationMethod: ComplexTypeModel.DerivationMethod
    }

    @XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class XSRestriction(
        override val base: QName,
        override val id: VID? = null,
        override val annotation: XSAnnotation? = null,

        override val openContents: List<XSOpenContent> = emptyList(),
        override val groups: List<XSGroupRef> = emptyList(), // TODO shouldn't be lists
        override val alls: List<XSAll> = emptyList(),
        override val choices: List<XSChoice> = emptyList(),
        override val sequences: List<XSSequence> = emptyList(),
        override val asserts: List<XSAssertionFacet> = emptyList(),
        override val attributes: List<XSLocalAttribute> = emptyList(),
        override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        override val anyAttribute: XSAnyAttribute? = null,
        override val simpleType: XSLocalSimpleType? = null,
        override val facets: List<XSFacet> = emptyList(),
        override val otherContents: List<CompactFragment> = emptyList(),
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSDerivationBase(), T_ComplexRestrictionType {
        override val derivationMethod: ComplexTypeModel.DerivationMethod get() = ComplexTypeModel.DerivationMethod.RESTRICION
    }

    @XmlSerialName("extension", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class XSExtension(
        override val base: QName,
        override val id: VID? = null,
        override val groups: List<XSGroupRef> = emptyList(),
        override val alls: List<XSAll> = emptyList(),
        override val choices: List<XSChoice> = emptyList(),
        override val sequences: List<XSSequence> = emptyList(),
        override val asserts: List<XSAssertionFacet> = emptyList(),
        override val attributes: List<XSLocalAttribute> = emptyList(),
        override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        override val anyAttribute: XSAnyAttribute? = null,
        override val annotation: XSAnnotation? = null,

        override val openContents: List<XSOpenContent> = emptyList(),
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSDerivationBase(), T_ComplexExtensionType {
        override val derivationMethod: ComplexTypeModel.DerivationMethod get() = ComplexTypeModel.DerivationMethod.EXTENSION
    }

}

