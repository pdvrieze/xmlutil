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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SimpleDerivation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SimpleRestrictionModels
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.CompactFragment

@Serializable
@XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class XSSimpleRestriction(
    @XmlElement(false)
    @Serializable(QNameSerializer::class)
    val base: QName? = null, // Rrequiers an embedded restriction
    @XmlElement(false)
    override val id: VID? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    override val simpleTypes: List<XSLocalSimpleType> = emptyList(),
    override val facets: List<XSFacet> = emptyList(),
    @XmlValue(true)
    override val otherContents: List<@Serializable(CompactFragmentSerializer::class) CompactFragment>,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap(),
) : XSSimpleDerivation(), T_Annotated,
    G_SimpleRestrictionModels,
    G_SimpleDerivation.Restriction
