/*
 * Copyright (c) 2023-2025.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VBoolean
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment

@XmlSerialName("complexContent", XSD_NS_URI, XSD_PREFIX)
@Serializable
class XSComplexContent(
    @XmlId
    override val id: VID? = null,
    @SerialName("mixed")
    private val _mixed: VBoolean? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<SerializableQName, String> = emptyMap(),
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    override val derivation: XSComplexDerivationBase
) : XSI_ComplexContent {

    val mixed: Boolean? get() = _mixed?.value

    @Serializable
    sealed class XSComplexDerivationBase : XSAnnotatedBase, XSI_ComplexDerivation {
        @XmlBefore("attributes", "attributeGroups")
        final override val term: XSIDerivationParticle?
        final override val asserts: List<XSAssert>
        @XmlBefore("anyAttribute")
        final override val attributes: List<XSLocalAttribute>
        @XmlBefore("anyAttribute")
        final override val attributeGroups: List<XSAttributeGroupRef>
        @XmlBefore("asserts")
        final override val anyAttribute: XSAnyAttribute?
        @XmlBefore("term")
        final override val openContent: XSOpenContent?
        final override val base: SerializableQName?
        abstract val derivationMethod: VDerivationControl.Complex

        constructor(
            base: QName?,
            term: XSIDerivationParticle?,
            attributes: List<XSLocalAttribute>,
            attributeGroups: List<XSAttributeGroupRef>,
            asserts: List<XSAssert>,
            anyAttribute: XSAnyAttribute?,
            openContent: XSOpenContent?,
            id: VID?,
            annotation: XSAnnotation?,
            otherAttrs: Map<SerializableQName, String>
        ) : super(id, annotation, otherAttrs) {
            this.term = term
            this.asserts = asserts
            this.attributes = attributes
            this.attributeGroups = attributeGroups
            this.anyAttribute = anyAttribute
            this.openContent = openContent
            this.base = base
        }
    }

    @Serializable
    sealed interface XSIDerivationParticle {
        /** Optional, default 1 */
        val minOccurs: VNonNegativeInteger?

        /** Optional, default 1 */
        val maxOccurs: VAllNNI?
    }

    @XmlSerialName("restriction", XSD_NS_URI, XSD_PREFIX)
    @Serializable
    class XSRestriction : XSComplexDerivationBase {
        val simpleType: XSLocalSimpleType?
        val otherContents: List<CompactFragment>

        constructor(
            simpleType: XSLocalSimpleType? = null,
            otherContents: List<CompactFragment> = emptyList(),
            base: QName,
            term: XSIDerivationParticle? = null,
            attributes: List<XSLocalAttribute> = emptyList(),
            attributeGroups: List<XSAttributeGroupRef> = emptyList(),
            asserts: List<XSAssert> = emptyList(),
            anyAttribute: XSAnyAttribute? = null,
            openContent: XSOpenContent? = null,
            id: VID? = null,
            annotation: XSAnnotation? = null,
            otherAttrs: Map<QName, String> = emptyMap()
        ) : super(
            base,
            term,
            attributes,
            attributeGroups,
            asserts,
            anyAttribute,
            openContent,
            id,
            annotation,
            otherAttrs
        ) {
            this.simpleType = simpleType
            this.otherContents = otherContents
        }

        /**
         * Mark the derivation as restriction
         */
        override val derivationMethod: VDerivationControl.Complex get() = VDerivationControl.RESTRICTION
    }

    @XmlSerialName("extension", XSD_NS_URI, XSD_PREFIX)
    @Serializable
    class XSExtension : XSComplexDerivationBase {
        constructor(
            base: QName,
            term: XSIDerivationParticle? = null,
            attributes: List<XSLocalAttribute> = emptyList(),
            attributeGroups: List<XSAttributeGroupRef> = emptyList(),
            asserts: List<XSAssert> = emptyList(),
            anyAttribute: XSAnyAttribute? = null,
            openContent: XSOpenContent? = null,
            id: VID? = null,
            annotation: XSAnnotation? = null,
            otherAttrs: Map<QName, String> = emptyMap()
        ) : super(
            base, term, attributes, attributeGroups, asserts, anyAttribute, openContent, id, annotation, otherAttrs
        )

        /**
         * Mark the derivation as extension
         */
        override val derivationMethod: VDerivationControl.Complex get() = VDerivationControl.EXTENSION
    }

}

