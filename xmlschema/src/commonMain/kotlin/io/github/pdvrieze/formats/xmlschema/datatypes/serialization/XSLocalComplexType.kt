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
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XSLocalComplexType.Serializer::class)
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
abstract class XSLocalComplexType(
    override val mixed: Boolean? = null,
    override val defaultAttributesApply: Boolean? = null,
    override val id: ID? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_LocalComplexType_Base, G_Redefinable.ComplexType {
    abstract override val content: G_ComplexTypeModel.Base

    protected abstract fun toSerialDelegate(): SerialDelegate

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSLocalComplexType

        if (mixed != other.mixed) return false
        if (defaultAttributesApply != other.defaultAttributesApply) return false
        if (id != other.id) return false
        if (annotations != other.annotations) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mixed?.hashCode() ?: 0
        result = 31 * result + (defaultAttributesApply?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + annotations.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }

    @Serializable
    class SerialDelegate(
        val mixed: Boolean? = null,
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
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
                complexContent != null -> XSLocalComplexTypeComplex(
                    mixed = mixed,
                    defaultAttributesApply = defaultAttributesApply,
                    content = complexContent,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
                else -> XSLocalComplexTypeShorthand(
                    mixed = mixed,
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

    companion object Serializer: KSerializer<XSLocalComplexType> {
        private val delegateSerializer = SerialDelegate.serializer()

        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            SerialDescriptor("complexType", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: XSLocalComplexType) {
            delegateSerializer.serialize(encoder, value.toSerialDelegate())
        }

        override fun deserialize(decoder: Decoder): XSLocalComplexType {
            return delegateSerializer.deserialize(decoder).toLocalComplexType()
        }

    }
}

class XSLocalComplexTypeComplex(
    mixed: Boolean? = null,
    defaultAttributesApply: Boolean? = null,
    override val content: XSComplexContent,
    id: ID? = null,
    annotations: List<XSAnnotation> = emptyList(),
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : XSLocalComplexType(
    mixed,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_LocalComplexType_Complex {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            mixed = mixed,
            defaultAttributesApply = defaultAttributesApply,
            complexContent = content,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XSLocalComplexTypeComplex

        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }

}

class XSLocalComplexTypeSimple(
    mixed: Boolean?,
    defaultAttributesApply: Boolean?,
    override val content: XSSimpleContent,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSLocalComplexType(
    mixed,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_LocalComplexType_Simple  {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            mixed = mixed,
            defaultAttributesApply = defaultAttributesApply,
            simpleContent = content,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XSLocalComplexTypeSimple

        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }


}

class XSLocalComplexTypeShorthand(
    mixed: Boolean? = null,
    defaultAttributesApply: Boolean? = null,
    override val groups: List<XSGroupRef> = emptyList(),
    override val alls: List<XSAll> = emptyList(),
    override val choices: List<XSChoice> = emptyList(),
    override val sequences: List<XSSequence> = emptyList(),
    override val asserts: List<XSAssert> = emptyList(),
    override val attributes: List<XSLocalAttribute> = emptyList(),
    override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
    override val anyAttribute: XSAnyAttribute? = null,
    override val openContents: List<XSOpenContent> = emptyList(),
    id: ID? = null,
    annotations: List<XSAnnotation> = emptyList(),
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : XSLocalComplexType(
    mixed,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_LocalComplexType_Shorthand {
    override val content: G_ComplexTypeModel.Shorthand get() = this

    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            mixed = mixed,
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
            otherAttrs = otherAttrs,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XSLocalComplexTypeShorthand

        if (groups != other.groups) return false
        if (alls != other.alls) return false
        if (choices != other.choices) return false
        if (sequences != other.sequences) return false
        if (asserts != other.asserts) return false
        if (attributes != other.attributes) return false
        if (attributeGroups != other.attributeGroups) return false
        if (anyAttribute != other.anyAttribute) return false
        if (openContents != other.openContents) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + groups.hashCode()
        result = 31 * result + alls.hashCode()
        result = 31 * result + choices.hashCode()
        result = 31 * result + sequences.hashCode()
        result = 31 * result + asserts.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + attributeGroups.hashCode()
        result = 31 * result + (anyAttribute?.hashCode() ?: 0)
        result = 31 * result + openContents.hashCode()
        return result
    }

}
