/*
 * Copyright (c) 2021.
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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XSLocalComplexType.Serializer::class)
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
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
    @XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    class SerialDelegate(
        val mixed: Boolean? = null,
        val complexContent: XSComplexContent? = null,
        val simpleContent: XSSimpleContent? = null,
        val term: XSComplexContent.XSIDerivationParticle? = null,
        val asserts: List<XSAssert> = emptyList(),
        val atributes: List<XSLocalAttribute> = emptyList(),
        val atributeGroups: List<XSAttributeGroupRef> = emptyList(),
        val anyAttribute: XSAnyAttribute? = null,
        val openContent: XSOpenContent? = null,
        val defaultAttributesApply: Boolean? = null,
        @XmlId
        val id: VID? = null,
        @XmlBefore("*")
        val annotation: XSAnnotation? = null,
        @XmlOtherAttributes
        val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
    ) {
        fun toLocalComplexType(): XSLocalComplexType {
            // TODO verify
            return when {
                simpleContent != null -> XSLocalComplexTypeSimple(
                    mixed = mixed,
                    defaultAttributesApply = defaultAttributesApply,
                    content = simpleContent,
                    id = id,
                    annotation = annotation,
                    otherAttrs = otherAttrs,
                )
                complexContent != null -> XSLocalComplexTypeComplex(
                    mixed = mixed,
                    defaultAttributesApply = defaultAttributesApply,
                    content = complexContent,
                    id = id,
                    annotation = annotation,
                    otherAttrs = otherAttrs,
                )
                else -> XSLocalComplexTypeShorthand(
                    mixed = mixed,
                    defaultAttributesApply = defaultAttributesApply,
                    term = term,
                    asserts = asserts,
                    attributes = atributes,
                    attributeGroups = atributeGroups,
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

