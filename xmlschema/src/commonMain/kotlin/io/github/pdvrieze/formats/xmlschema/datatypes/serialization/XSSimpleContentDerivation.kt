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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.model.I_Assertions
import io.github.pdvrieze.formats.xmlschema.types.I_AttributeContainer
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexType
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

@Serializable
sealed class XSSimpleContentDerivation(
    override val id: VID?,
    override val attributes: List<XSLocalAttribute>,
    override val attributeGroups: List<XSAttributeGroupRef>,
    override val anyAttribute: XSAnyAttribute?,
    override val asserts: List<XSAssert>,
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>,
) : XSComplexType.Derivation, T_ComplexType.SimpleDerivation, I_Assertions, I_AttributeContainer {
    abstract val base: QName?
    abstract val derivationMethod: ComplexTypeModel.DerivationMethod
}
