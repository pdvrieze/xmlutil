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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

@Serializable(XSGlobalComplexType.Serializer::class)
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
sealed class XSGlobalComplexType(
    override val name: VNCName,
    override val mixed: Boolean?,
    val abstract: Boolean?,
    val final: Set<VDerivationControl.Complex>?,
    val block: Set<VDerivationControl.Complex>?,
    override val defaultAttributesApply: Boolean?,
    @XmlId
    override val id: VID? = null,
    override val annotation: XSAnnotation? = null,

    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : XSComplexType, XSGlobalType, XSI_Annotated {
    abstract override val content: XSI_ComplexContent

    protected abstract fun toSerialDelegate(): SerialDelegate

    @Serializable
    @XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    class SerialDelegate(
        val name: VNCName,
        val mixed: Boolean? = null,
        val abstract: Boolean? = null,
        @XmlElement(false)
        @Serializable(ComplexDerivationSerializer::class)
        val final: Set<@Contextual VDerivationControl.Complex>? = null,
        @XmlElement(false)
        @Serializable(ComplexDerivationSerializer::class)
        val block: Set<@Contextual VDerivationControl.Complex>? = null,
        val complexContent: XSComplexContent? = null,
        val simpleContent: XSSimpleContent? = null,
//        @Serializable
        val term: XSComplexContent.XSIDerivationParticle?/* = null*/,
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
        val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    ) {
        fun toTopLevelComplexType(): XSGlobalComplexType {
            // TODO verify
            return when {
                simpleContent != null -> XSGlobalComplexTypeSimple(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    content = simpleContent,
                    id = id,
                    annotation = annotation,
                    otherAttrs = otherAttrs,
                )

                complexContent != null -> XSGlobalComplexTypeComplex(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    content = complexContent,
                    id = id,
                    annotation = annotation,
                    otherAttrs = otherAttrs,
                )

                else -> XSGlobalComplexTypeShorthand(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
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

    companion object Serializer : KSerializer<XSGlobalComplexType> {
        private val delegateSerializer = SerialDelegate.serializer()

        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            SerialDescriptor("io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalComplexType", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: XSGlobalComplexType) {
            delegateSerializer.serialize(encoder, value.toSerialDelegate())
        }

        override fun deserialize(decoder: Decoder): XSGlobalComplexType {
            return delegateSerializer.deserialize(decoder).toTopLevelComplexType()
        }

    }
}

