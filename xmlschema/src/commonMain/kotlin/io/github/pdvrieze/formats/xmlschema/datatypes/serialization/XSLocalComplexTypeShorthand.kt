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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalComplexType_Shorthand
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

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
    id: VID? = null,
    annotation: XSAnnotation? = null,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : XSLocalComplexType(
    mixed,
    defaultAttributesApply,
    id,
    annotation,
    otherAttrs
), IXSComplexTypeShorthand, T_LocalComplexType_Shorthand {

    override val name: Nothing? get() = null
    override val targetNamespace: Nothing? get() = null

    override val content: IXSComplexTypeShorthand get() = this

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
            annotation = annotation,
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
