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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName("any", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAny(
    @XmlElement(false)
    @XmlSerialName("processContents", "", "")
    override val processContents: T_ProcessContents? = null,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlElement(false)
    override val notQName: T_QNameList? = null,
    @XmlElement(false)
    override val namespace: T_NamespaceList? = null,
    @XmlElement(false)
    override val notNamespace: T_NotNamespaceList? = null,
    @XmlId
    override val id: VID? = null,
    override val minOccurs: VNonNegativeInteger? = null,
    override val maxOccurs: T_AllNNI? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()

) : T_AnyElement, XSI_AllParticle
