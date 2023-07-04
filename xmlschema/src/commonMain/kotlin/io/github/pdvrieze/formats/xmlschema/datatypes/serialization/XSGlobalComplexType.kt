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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
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
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XSGlobalComplexType.Serializer::class)
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
sealed class XSGlobalComplexType(
    override val name: VNCName,
    override val mixed: Boolean?,
    override val abstract: Boolean?,
    override val final: Set<T_TypeDerivationControl.ComplexBase>?,
    override val block: Set<T_TypeDerivationControl.ComplexBase>?,
    override val defaultAttributesApply: Boolean?,
    override val id: VID? = null,
    override val annotation: XSAnnotation? = null,

    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : XSComplexType, XSGlobalType, T_GlobalComplexType_Base {
    abstract override val content: XSI_ComplexContent

    override val targetNamespace: Nothing? get() = null

    protected abstract fun toSerialDelegate(): SerialDelegate

    @Serializable
    @XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    class SerialDelegate(
        val name: VNCName,
        val mixed: Boolean? = null,
        val abstract: Boolean? = null,
        @XmlElement(false)
        @Serializable(ComplexDerivationSerializer::class)
        val final: Set<@Contextual T_TypeDerivationControl.ComplexBase>? = null,
        @XmlElement(false)
        @Serializable(ComplexDerivationSerializer::class)
        val block: Set<@Contextual T_TypeDerivationControl.ComplexBase>? = null,
        val complexContent: XSComplexContent? = null,
        val simpleContent: XSSimpleContent? = null,
        @Serializable
        val term: XSComplexContent.XSIDerivationParticle?/* = null*/,
        val asserts: List<XSAssert> = emptyList(),
        val atributes: List<XSLocalAttribute> = emptyList(),
        val atributeGroups: List<XSAttributeGroupRef> = emptyList(),
        val anyAttribute: XSAnyAttribute? = null,
        val openContent: XSOpenContent? = null,
        val defaultAttributesApply: Boolean? = null,
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

