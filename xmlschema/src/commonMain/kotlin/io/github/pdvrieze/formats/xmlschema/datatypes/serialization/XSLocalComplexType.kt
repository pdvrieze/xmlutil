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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VBoolean
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XSLocalComplexType.Serializer::class)
@XmlSerialName("complexType", XSD_NS_URI, XSD_PREFIX)
sealed class XSLocalComplexType(
    override val mixed: Boolean? = null,
    override val defaultAttributesApply: Boolean? = null,
    @XmlId
    override val id: VID? = null,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : XSLocalType(), XSComplexType, XSI_Annotated {
    abstract override val content: XSI_ComplexContent

    protected abstract fun toSerialDelegate(): SerialDelegate

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSLocalComplexType

        if (mixed != other.mixed) return false
        if (defaultAttributesApply != other.defaultAttributesApply) return false
        if (id != other.id) return false
        if (annotation != other.annotation) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mixed?.hashCode() ?: 0
        result = 31 * result + (defaultAttributesApply?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + otherAttrs.hashCode()
        return result
    }

    @Serializable
    @XmlSerialName("complexType", XSD_NS_URI, XSD_PREFIX)
    class SerialDelegate(
        val mixed: VBoolean? = null,
        val complexContent: XSComplexContent? = null,
        val simpleContent: XSSimpleContent? = null,
        @XmlBefore("attributes", "attributeGroups")
        val term: XSComplexContent.XSIDerivationParticle? = null,
        val asserts: List<XSAssert> = emptyList(),
        @XmlBefore("anyAttribute")
        val attributes: List<XSLocalAttribute> = emptyList(),
        @XmlBefore("anyAttribute")
        val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
        @XmlBefore("asserts")
        val anyAttribute: XSAnyAttribute? = null,
        @XmlBefore("term")
        val openContent: XSOpenContent? = null,
        val defaultAttributesApply: VBoolean? = null,
        @XmlId
        val id: VID? = null,
        @XmlBefore("*")
        val annotation: XSAnnotation? = null,
        @XmlOtherAttributes
        val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
    ) {
        fun toLocalComplexType(): XSLocalComplexType {
            if (simpleContent !=null || complexContent != null) {
                require(asserts.isEmpty()) { "Shorthand as well as non-shorthand definition of complex type" }
                require(attributes.isEmpty()) { "Shorthand as well as non-shorthand definition of complex type" }
                require(attributeGroups.isEmpty()) { "Shorthand as well as non-shorthand definition of complex type" }
                require(anyAttribute == null) { "Shorthand as well as non-shorthand definition of complex type" }
                require(openContent == null) { "Shorthand as well as non-shorthand definition of complex type" }
                require(term == null) { "Shorthand as well as non-shorthand definition of complex type" }
            }

            // TODO verify
            return when {
                simpleContent != null -> XSLocalComplexTypeSimple(
                    mixed = mixed?.value,
                    defaultAttributesApply = defaultAttributesApply?.value,
                    content = simpleContent,
                    id = id,
                    annotation = annotation,
                    otherAttrs = otherAttrs,
                ).also { require(complexContent==null) { "Complex types can not be both simple and complex" } }
                complexContent != null -> XSLocalComplexTypeComplex(
                    mixed = mixed?.value,
                    defaultAttributesApply = defaultAttributesApply?.value,
                    content = complexContent,
                    id = id,
                    annotation = annotation,
                    otherAttrs = otherAttrs,
                )
                else -> XSLocalComplexTypeShorthand(
                    mixed = mixed?.value,
                    defaultAttributesApply = defaultAttributesApply?.value,
                    term = term,
                    asserts = asserts,
                    attributes = attributes,
                    attributeGroups = attributeGroups,
                    anyAttribute = anyAttribute,
                    openContent = openContent,
                    id = id,
                    annotation = annotation,
                    otherAttrs = otherAttrs,
                )
            }

        }

    }

    companion object Serializer: KSerializer<XSLocalComplexType> {
        private val delegateSerializer = SerialDelegate.serializer()

        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            SerialDescriptor("io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalComplexType", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: XSLocalComplexType) {
            delegateSerializer.serialize(encoder, value.toSerialDelegate())
        }

        override fun deserialize(decoder: Decoder): XSLocalComplexType {
            return delegateSerializer.deserialize(decoder).toLocalComplexType()
        }

    }
}

