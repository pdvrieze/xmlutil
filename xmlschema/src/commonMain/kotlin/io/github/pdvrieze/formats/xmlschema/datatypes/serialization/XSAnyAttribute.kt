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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
@XmlSerialName("anyAttribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAnyAttribute(
    override val annotations: List<XSAnnotation> = emptyList(),
    override val id: ID? = null,
    override val notQName: T_QNameListA? = null,

    override val namespace: T_NamespaceList? = null,
    override val notNamespace: T_NotNamespaceList? = null,
    @XmlElement(false)
    @XmlSerialName("processContents", "", "")
    override val processContents: T_ProcessContents = T_ProcessContents.STRICT,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_AnyAttribute {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSAnyAttribute

        if (annotations != other.annotations) return false
        if (id != other.id) return false
        if (notQName != other.notQName) return false
        if (namespace != other.namespace) return false
        if (notNamespace != other.notNamespace) return false
        if (processContents != other.processContents) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = annotations.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (notQName?.hashCode() ?: 0)
        result = 31 * result + (namespace?.hashCode() ?: 0)
        result = 31 * result + (notNamespace?.hashCode() ?: 0)
        result = 31 * result + processContents.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
