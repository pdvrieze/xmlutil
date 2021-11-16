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

import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Assertions
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_AttrDecls
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Annotated
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalAttribute
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

@Serializable
sealed class XSSimpleContentDerivation(
    val base: @Serializable(QNameSerializer::class) QName,
    override val id: ID? = null,
    override val attributes: List<T_LocalAttribute> = emptyList(),
    override val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
    override val anyAttribute: XSAnyAttribute? = null,
    override val asserts: List<XSAssert> = emptyList(),
    override val annotations: List<XSAnnotation> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
): T_Annotated, G_Assertions, G_AttrDecls {
}
