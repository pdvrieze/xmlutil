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
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("group", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGroup(
    override val name: VNCName,
    @XmlId
    override val id: VID? = null,
    val content: XSGroupElement,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_NamedGroup, I_NamedAttrs {

    override val particle: T_NamedGroup.Particle
        get() = TODO("not implemented")
    override val targetNamespace: Nothing? get() = null

    @Serializable
    sealed class XSGroupElement : T_NamedGroup.Particle, XSI_Annotated {
        override val minOccurs: Nothing? get() = null
        override val maxOccurs: Nothing? get() = null
    }

    @XmlSerialName("all", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class All(
        override val particles: List<XSLocalElement> = emptyList(),
        @XmlBefore("*")
        override val annotation: XSAnnotation? = null,
        @XmlId
        override val id: VID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSGroupElement(), T_NamedGroup.All {
    }

    @XmlSerialName("choice", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class Choice(
        override val particles: List<XSI_NestedParticle>,
        @XmlBefore("*")
        override val annotation: XSAnnotation? = null,
        @XmlId
        override val id: VID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSGroupElement(), T_NamedGroup.Choice

    @XmlSerialName("sequence", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    @Serializable
    class Sequence(
        override val particles: List<XSI_NestedParticle>,
        @XmlBefore("*")
        override val annotation: XSAnnotation? = null,
        @XmlId
        override val id: VID? = null,
        @XmlOtherAttributes
        override val otherAttrs: Map<QName, String> = emptyMap()
    ) : XSGroupElement(), T_NamedGroup.Sequence

}
