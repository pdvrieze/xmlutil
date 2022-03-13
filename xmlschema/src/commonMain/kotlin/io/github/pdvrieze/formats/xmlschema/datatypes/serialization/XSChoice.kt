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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_NestedParticle
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Particle
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_TypeDefParticle
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_ExplicitGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("choice", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSChoice(
    override val minOccurs: ULong = 1.toULong(),
    override val maxOccurs: T_AllNNI = T_AllNNI(1),
    override val elements: List<XSLocalElement> = emptyList(),
    override val groups: List<XSGroupRef> = emptyList(),
    override val choices: List<XSChoice> = emptyList(),
    override val sequences: List<XSSequence> = emptyList(),
    override val anys: List<XSAny> = emptyList(),
    override val annotations: List<XSAnnotation> = emptyList(),
    override val id: ID? = null,
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_ExplicitGroup, G_TypeDefParticle.Choice, G_NestedParticle.Choice, G_Particle.All
