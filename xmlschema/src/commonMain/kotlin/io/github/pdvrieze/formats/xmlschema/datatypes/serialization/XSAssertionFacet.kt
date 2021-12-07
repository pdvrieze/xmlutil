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
import io.github.pdvrieze.formats.xmlschema.datatypes.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Assertion
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_NoFixedFacet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_XPathDefaultNamespace
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("assertion", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAssertionFacet : T_NoFixedFacet, T_Assertion {
    constructor(
        test: XPathExpression,
        id: ID? = null,
        xPathDefaultNamespace: T_XPathDefaultNamespace? = null,
        annotations: List<XSAnnotation> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(id,annotations, otherAttrs) {
        this.test = test
        this.xPathDefaultNamespace = xPathDefaultNamespace
    }

    override val xPathDefaultNamespace: T_XPathDefaultNamespace?

    override val value: XPathExpression
        get() = test

    override val test: XPathExpression
}