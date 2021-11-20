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
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Annotated
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_ContentMode
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("defaultOpenContent", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSDefaultOpenContent(
    val appliesToEmpty: Boolean = false,
    val mode: T_ContentMode = T_ContentMode.INTERLEAVE,
    @XmlValue(true)
    val content: List<@Serializable(CompactFragmentSerializer::class) CompactFragment> = emptyList(),
    override val annotations: List<XSAnnotation> = emptyList(),
    override val id: ID? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(with = QNameSerializer::class) QName, String> = emptyMap()
): T_Annotated
