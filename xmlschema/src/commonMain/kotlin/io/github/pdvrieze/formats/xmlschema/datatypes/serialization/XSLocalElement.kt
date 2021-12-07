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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_NestedParticle
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Particle
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("element", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSLocalElement(
    @XmlBefore("type")
    override val name: VNCName? = null,
    @Serializable(SchemaEnumSetSerializer::class)
    override val block: T_BlockSet? = null,
    override val default: String? = null,
    override val fixed: String? = null,
    @XmlElement(false)
    override val form: T_FormChoice? = null,
    override val id: VID? = null,
    override val maxOccurs: T_AllNNI? = null,
    override val minOccurs: VNonNegativeInteger? = null,
    override val nillable: Boolean? = false,
    @XmlElement(false)
    override val ref: QName? = null,
    override val targetNamespace: VAnyURI? = null,
    @XmlElement(false)
    override val type: QName? = null,

    override val annotations: List<XSAnnotation> = emptyList(),
    override val localType: XSLocalType? = null,
    override val alternatives: List<T_AltType> = emptyList(),
    override val uniques: List<XSUnique> = emptyList(),
    override val keys: List<XSKey> = emptyList(),
    override val keyref: List<XSKeyref> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap(),
) : T_LocalElement, G_NestedParticle.Element, G_Particle.Element {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSLocalElement

        if (name != other.name) return false
        if (block != other.block) return false
        if (default != other.default) return false
        if (fixed != other.fixed) return false
        if (form != other.form) return false
        if (id != other.id) return false
        if (maxOccurs != other.maxOccurs) return false
        if (minOccurs != other.minOccurs) return false
        if (nillable != other.nillable) return false
        if (ref != other.ref) return false
        if (targetNamespace != other.targetNamespace) return false
        if (type != other.type) return false
        if (annotations != other.annotations) return false
        if (localType != other.localType) return false
        if (alternatives != other.alternatives) return false
        if (uniques != other.uniques) return false
        if (keys != other.keys) return false
        if (keyref != other.keyref) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (block?.hashCode() ?: 0)
        result = 31 * result + (default?.hashCode() ?: 0)
        result = 31 * result + (fixed?.hashCode() ?: 0)
        result = 31 * result + (form?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + maxOccurs.hashCode()
        result = 31 * result + minOccurs.hashCode()
        result = 31 * result + (nillable?.hashCode() ?: 0)
        result = 31 * result + (ref?.hashCode() ?: 0)
        result = 31 * result + (targetNamespace?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + annotations.hashCode()
        result = 31 * result + (localType?.hashCode() ?: 0)
        result = 31 * result + alternatives.hashCode()
        result = 31 * result + uniques.hashCode()
        result = 31 * result + keys.hashCode()
        result = 31 * result + keyref.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
