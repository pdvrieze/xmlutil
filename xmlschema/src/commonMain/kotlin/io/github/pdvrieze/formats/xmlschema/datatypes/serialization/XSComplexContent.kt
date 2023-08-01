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

@file:UseSerializers(QNameSerializer::class, CompactFragmentSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSAssertionFacet
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.CompactFragment

@XmlSerialName("complexContent", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
@Serializable
class XSComplexContent(
    @XmlId
    override val id: VID? = null,
    val mixed: Boolean? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap(),
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    override val derivation: XSComplexDerivationBase
) : XSI_ComplexContent.Complex, T_ComplexType.ComplexContent {
    @Serializable
    sealed class XSComplexDerivationBase: XSI_ComplexDerivation {
        abstract override val term: XSIDerivationParticle?
        abstract override val asserts: List<XSAssertionFacet>
        abstract override val attributes: List<XSLocalAttribute>
        abstract override val attributeGroups: List<XSAttributeGroupRef>
        abstract val derivationMethod: T_DerivationControl.ComplexBase
    }

    @Serializable
    sealed interface XSIDerivationParticle: T_Particle {
        override val minOccurs: VNonNegativeInteger?
        override val maxOccurs: T_AllNNI?
    }

    @Serializable
    sealed interface XSIDirectParticle: XSIDerivationParticle, T_Particle

    @XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class XSRestriction(
        override val base: QName,
        @XmlId
        override val id: VID? = null,
        override val annotation: XSAnnotation? = null,

        override val openContent: XSOpenContent? = null,
        override val term: XSIDerivationParticle? = null,
        override val asserts: List<XSAssertionFacet> = emptyList(),
        override val attributes: List<XSLocalAttribute> = emptyList(),
        override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        override val anyAttribute: XSAnyAttribute? = null,
        override val simpleType: XSLocalSimpleType? = null,
        override val otherContents: List<CompactFragment> = emptyList(),
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSComplexDerivationBase(), T_ComplexRestrictionType {
        override val derivationMethod: T_DerivationControl.ComplexBase get() = T_DerivationControl.RESTRICTION
        override val facets: List<Nothing> = emptyList()

    }

    @XmlSerialName("extension", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class XSExtension(
        override val base: QName,
        @XmlId
        override val id: VID? = null,
        override val term: XSIDerivationParticle? = null,

        override val asserts: List<XSAssertionFacet> = emptyList(),
        override val attributes: List<XSLocalAttribute> = emptyList(),
        override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        override val anyAttribute: XSAnyAttribute? = null,
        override val annotation: XSAnnotation? = null,

        override val openContent: XSOpenContent? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSComplexDerivationBase(), T_ComplexExtensionType {
        override val derivationMethod: T_DerivationControl.ComplexBase get() = T_DerivationControl.EXTENSION
    }

}

