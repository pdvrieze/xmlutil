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
    override val minOccurs: ULong = 1L.toULong(),
    override val maxOccurs: T_AllNNI = T_AllNNI.Value(1L.toULong()),
    val alls: List<XSAll>,
    val choices: List<XSChoice>,
    val sequences: List<XSSequence>,
    override val annotations: List<XSAnnotation>,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String>
) : G_Redefinable.Group {

}
