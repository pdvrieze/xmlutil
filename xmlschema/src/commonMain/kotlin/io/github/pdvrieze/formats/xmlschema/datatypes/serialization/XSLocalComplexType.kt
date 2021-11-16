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
    override val mixed: Boolean,
    override val defaultAttributesApply: Boolean,
    override val id: ID? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_LocalComplexType_Base, G_Redefinable.ComplexType {
    abstract override val content: G_ComplexTypeModel.Base

    protected abstract fun toSerialDelegate(): SerialDelegate

    @Serializable
    class SerialDelegate(
        val mixed: Boolean,
        val complexContent: XSComplexContent? = null,
        val simpleContent: XSSimpleContent? = null,
        val groups: List<XSGroupRef> = emptyList(),
        val alls: List<XSAll> = emptyList(),
        val choices: List<XSChoice> = emptyList(),
        val sequences: List<XSSequence> = emptyList(),
        val asserts: List<XSAssert> = emptyList(),
        val atributes: List<T_LocalAttribute> = emptyList(),
        val atributeGroups: List<XSAttributeGroupRef> = emptyList(),
        val anyAttribute: XSAnyAttribute? = null,
        val openContents: List<XSOpenContent> = emptyList(),
        val defaultAttributesApply: Boolean,
        val id: ID?,
        val annotations: List<XSAnnotation>,
        @XmlOtherAttributes
        val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
    ) {
        fun toLocalComplexType(): XSLocalComplexType {
            // TODO verify
            return when {
                simpleContent!=null -> XSLocalComplexTypeSimple(
                    mixed = mixed,
                    defaultAttributesApply = defaultAttributesApply,
                    content = simpleContent,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
                complexContent!=null -> XSLocalComplexTypeComplex(
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
    mixed: Boolean,
    defaultAttributesApply: Boolean,
    override val content: XSComplexContent,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
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
}

class XSLocalComplexTypeSimple(
    mixed: Boolean,
    defaultAttributesApply: Boolean,
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
}

class XSLocalComplexTypeShorthand(
    mixed: Boolean,
    defaultAttributesApply: Boolean,
    override val groups: List<XSGroupRef>,
    override val alls: List<XSAll>,
    override val choices: List<XSChoice>,
    override val sequences: List<XSSequence>,
    override val asserts: List<XSAssert>,
    override val attributes: List<T_LocalAttribute>,
    override val attributeGroups: List<XSAttributeGroupRef>,
    override val anyAttribute: XSAnyAttribute?,
    override val openContents: List<XSOpenContent>,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
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

}
