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
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Redefinable
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XSTopLevelComplexType.Serializer::class)
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
abstract class XSTopLevelComplexType(
    override val name: NCName,
    override val mixed: Boolean?,
    override val abstract: Boolean,
    override val final: T_DerivationSet,
    override val block: T_DerivationSet,
    override val defaultAttributesApply: Boolean?,
    override val id: ID? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_TopLevelComplexType_Base, G_Redefinable.ComplexType {
    abstract override val content: G_ComplexTypeModel.Base

    protected abstract fun toSerialDelegate(): SerialDelegate

    @Serializable
    class SerialDelegate(
        val name: NCName,
        val mixed: Boolean? = null,
        val abstract: Boolean = false,
        @XmlElement(false)
        val final: T_DerivationSet = emptySet(),
        @XmlElement(false)
        val block: T_DerivationSet = emptySet(),
        val complexContent: XSComplexContent? = null,
        val simpleContent: XSSimpleContent? = null,
        val groups: List<XSGroupRef> = emptyList(),
        val alls: List<XSAll> = emptyList(),
        val choices: List<XSChoice> = emptyList(),
        val sequences: List<XSSequence> = emptyList(),
        val asserts: List<XSAssert> = emptyList(),
        val atributes: List<XSLocalAttribute> = emptyList(),
        val atributeGroups: List<XSAttributeGroupRef> = emptyList(),
        val anyAttribute: XSAnyAttribute? = null,
        val openContents: List<XSOpenContent> = emptyList(),
        val defaultAttributesApply: Boolean? = null,
        val id: ID? = null,
        val annotations: List<XSAnnotation> = emptyList(),
        @XmlOtherAttributes
        val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    ) {
        fun toTopLevelComplexType(): XSTopLevelComplexType {
            // TODO verify
            return when {
                simpleContent != null -> XSTopLevelComplexTypeSimple(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    content = simpleContent,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
                complexContent != null -> XSTopLevelComplexTypeComplex(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    content = complexContent,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
                else -> XSTopLevelComplexTypeShorthand(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    groups = groups,
                    alls = alls,
                    choices = choices,
                    sequences = sequences,
                    asserts = asserts,
                    attributes = atributes,
                    attributeGroups = atributeGroups,
                    anyAttribute = anyAttribute,
                    openContents = openContents,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
            }

        }

    }

    companion object Serializer : KSerializer<XSTopLevelComplexType> {
        private val delegateSerializer = SerialDelegate.serializer()

        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            SerialDescriptor("complexType", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: XSTopLevelComplexType) {
            delegateSerializer.serialize(encoder, value.toSerialDelegate())
        }

        override fun deserialize(decoder: Decoder): XSTopLevelComplexType {
            return delegateSerializer.deserialize(decoder).toTopLevelComplexType()
        }

    }
}

class XSTopLevelComplexTypeComplex(
    name: NCName,
    mixed: Boolean?,
    abstract: Boolean,
    final: T_DerivationSet,
    block: T_DerivationSet,
    defaultAttributesApply: Boolean?,
    override val content: XSComplexContent,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Complex {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract = abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            complexContent = content,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }
}

class XSTopLevelComplexTypeSimple(
    name: NCName,
    mixed: Boolean?,
    abstract: Boolean,
    final: T_DerivationSet,
    block: T_DerivationSet,
    defaultAttributesApply: Boolean?,
    override val content: XSSimpleContent,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Simple {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract = abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            simpleContent = content,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }
}

class XSTopLevelComplexTypeShorthand(
    name: NCName,
    mixed: Boolean?,
    abstract: Boolean,
    final: T_DerivationSet,
    block: T_DerivationSet,
    defaultAttributesApply: Boolean?,
    override val groups: List<XSGroupRef>,
    override val alls: List<XSAll>,
    override val choices: List<XSChoice>,
    override val sequences: List<XSSequence>,
    override val asserts: List<XSAssert>,
    override val attributes: List<XSLocalAttribute>,
    override val attributeGroups: List<XSAttributeGroupRef>,
    override val anyAttribute: XSAnyAttribute?,
    override val openContents: List<XSOpenContent>,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Shorthand {
    override val content: G_ComplexTypeModel.Shorthand get() = this

    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract = abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            groups = groups,
            alls = alls,
            choices = choices,
            sequences = sequences,
            asserts = asserts,
            atributes = attributes,
            atributeGroups = attributeGroups,
            anyAttribute = anyAttribute,
            openContents = openContents,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }

}
