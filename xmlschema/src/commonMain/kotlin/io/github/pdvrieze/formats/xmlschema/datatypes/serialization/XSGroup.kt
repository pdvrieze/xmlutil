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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_NamedGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("group", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGroup(
    override val name: NCName,
    override val id: ID? = null,
    override val ref: QName? = null,
    override val particle: Particle,
    override val annotations: List<XSAnnotation>,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String>
) : G_Redefinable.Group, T_NamedGroup {
    override val minOccurs: Nothing? get() = null
    override val maxOccurs: Nothing? get() = null

    @Serializable
    sealed class Particle : T_NamedGroup.NG_Particle

    @XmlSerialName("all", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class All(
        override val annotations: List<XSAnnotation> = emptyList(),
        override val elements: List<XSLocalElement> = emptyList(),
        override val anys: List<XSAny> = emptyList(),
        override val groups: List<XSGroupRef> = emptyList(),
        @XmlOtherAttributes
        override val otherAttributes: Map<QName, String> = emptyMap()

    ): Particle(), T_NamedGroup.All

    @XmlSerialName("choice", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class Choice(
        override val elements: List<XSLocalElement> = emptyList(),
        override val groups: List<XSGroupRef> = emptyList(),
        override val choices: List<XSChoice> = emptyList(),
        override val sequences: List<XSSequence> = emptyList(),
        override val anys: List<XSAny> = emptyList(),
        override val annotations: List<XSAnnotation> = emptyList(),
        override val id: ID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : Particle(), T_NamedGroup.Choice

    @XmlSerialName("sequence", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class Sequence(
        override val elements: List<XSLocalElement> = emptyList(),
        override val groups: List<XSGroupRef> = emptyList(),
        override val choices: List<XSChoice> = emptyList(),
        override val sequences: List<XSSequence> = emptyList(),
        override val anys: List<XSAny> = emptyList(),
        override val annotations: List<XSAnnotation> = emptyList(),
        override val id: ID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ): Particle(), T_NamedGroup.Sequence

}
