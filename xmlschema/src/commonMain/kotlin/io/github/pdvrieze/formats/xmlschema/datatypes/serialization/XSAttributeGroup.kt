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

@file:UseSerializers(QNameSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Redefinable
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_NamedAttributeGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("attributeGroup", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAttributeGroup(
    override val name: NCName,
    override val id: ID? = null,
    override val attributes: List<XSLocalAttribute> = emptyList(),
    override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
    override val anyAttribute: XSAnyAttribute? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : G_Redefinable.AttributeGroup, T_NamedAttributeGroup {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSAttributeGroup

        if (name != other.name) return false
        if (id != other.id) return false
        if (attributes != other.attributes) return false
        if (attributeGroups != other.attributeGroups) return false
        if (anyAttribute != other.anyAttribute) return false
        if (annotations != other.annotations) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + attributes.hashCode()
        result = 31 * result + attributeGroups.hashCode()
        result = 31 * result + (anyAttribute?.hashCode() ?: 0)
        result = 31 * result + annotations.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
