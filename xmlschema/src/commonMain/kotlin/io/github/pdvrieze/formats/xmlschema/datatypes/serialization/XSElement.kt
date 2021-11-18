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
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_IdentityConstraint
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SchemaTop
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("element", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSElement(
    override val name: NCName,
    @Serializable(SchemaEnumSetSerializer::class)
    override val block: T_BlockSet? = null,
    override val default: String? = null,
    override val fixed: String? = null,
    override val id: ID? = null,
    override val nillable: Boolean? = false,
    @XmlElement(false)
    override val type: QName? = null,
    override val abstract: Boolean = false,

    @XmlElement(false)
    override val substitutionGroup: List<QName> = emptyList(),
    @XmlElement(false)
    override val final: T_DerivationSet = emptySet(),
    override val annotations: List<XSAnnotation> = emptyList(),
    override val simpleTypes: List<XSLocalSimpleType> = emptyList(),
    override val complexTypes: List<XSLocalComplexType> = emptyList(),
    override val alternatives: List<T_AltType> = emptyList(),
    override val uniques: List<G_IdentityConstraint.Unique> = emptyList(),
    override val keys: List<G_IdentityConstraint.Key> = emptyList(),
    override val keyref: List<G_IdentityConstraint.Keyref> = emptyList(),
    override val otherAttrs: Map<QName, String> = emptyMap(),
): G_SchemaTop.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSElement

        if (name != other.name) return false
        if (block != other.block) return false
        if (default != other.default) return false
        if (fixed != other.fixed) return false
        if (id != other.id) return false
        if (nillable != other.nillable) return false
        if (type != other.type) return false
        if (abstract != other.abstract) return false
        if (substitutionGroup != other.substitutionGroup) return false
        if (final != other.final) return false
        if (annotations != other.annotations) return false
        if (simpleTypes != other.simpleTypes) return false
        if (complexTypes != other.complexTypes) return false
        if (alternatives != other.alternatives) return false
        if (uniques != other.uniques) return false
        if (keys != other.keys) return false
        if (keyref != other.keyref) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (block?.hashCode() ?: 0)
        result = 31 * result + (default?.hashCode() ?: 0)
        result = 31 * result + (fixed?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (nillable?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + abstract.hashCode()
        result = 31 * result + substitutionGroup.hashCode()
        result = 31 * result + final.hashCode()
        result = 31 * result + annotations.hashCode()
        result = 31 * result + simpleTypes.hashCode()
        result = 31 * result + complexTypes.hashCode()
        result = 31 * result + alternatives.hashCode()
        result = 31 * result + uniques.hashCode()
        result = 31 * result + keys.hashCode()
        result = 31 * result + keyref.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
