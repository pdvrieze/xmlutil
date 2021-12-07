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
    val base: @Serializable(QNameSerializer::class) QName?,
    override val id: ID?,
    override val attributes: List<XSLocalAttribute>,
    override val attributeGroups: List<XSAttributeGroupRef>,
    override val anyAttribute: XSAnyAttribute?,
    override val asserts: List<XSAssert>,
    override val annotations: List<XSAnnotation>,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>,
) : T_Annotated, G_Assertions, G_AttrDecls
