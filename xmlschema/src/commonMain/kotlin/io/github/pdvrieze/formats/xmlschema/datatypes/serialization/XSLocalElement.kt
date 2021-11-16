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
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_IdentityConstraint
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_NestedParticle
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Particle
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("element", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSLocalElement(
    @Serializable(SchemaEnumSetSerializer::class)
    override val block: Set<T_BlockSet>,
    override val default: String? = null,
    override val fixed: String? = null,
    override val form: T_FormChoice,
    override val id: ID? = null,
    override val maxOccurs: T_AllNNI = T_AllNNI(1),
    override val minOccurs: ULong = 1.toULong(),
    override val nillable: Boolean? = false,
    override val ref: QName? = null,
    override val targetNamespace: AnyURI? = null,
    override val type: QName? = null,

    override val annotations: List<XSAnnotation> = emptyList(),
    override val simpleTypes: List<T_LocalSimpleType> = emptyList(),
    override val complexTypes: List<T_ComplexType_Base> = emptyList(),
    override val alternatives: List<T_AltType> = emptyList(),
    override val uniques: List<G_IdentityConstraint.Unique> = emptyList(),
    override val keys: List<G_IdentityConstraint.Key> = emptyList(),
    override val keyref: List<G_IdentityConstraint.Keyref> = emptyList(),
    override val otherAttrs: Map<QName, String> = emptyMap(),
) : T_LocalElement, G_NestedParticle.Element, G_Particle.Element
