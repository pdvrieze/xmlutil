/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.test.sunExpected

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants.XS_NAMESPACE
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import nl.adaptivity.xmlutil.QName

object AGNameDefaults {
    val ns = "AttrGroup/name"

    val ag = XSAttributeGroup(
        name = NCName("aGr"),
        attributes = listOf(
            XSLocalAttribute(
                name = NCName("number"),
                type = QName(XS_NAMESPACE, "integer", "xsd"),
                use = XSAttrUse.REQUIRED
            ),
            XSLocalAttribute(name= NCName("height"), type = QName(XS_NAMESPACE, "decimal", "xsd"))
        )
    )
    val expectedSchema = XSSchema(
        targetNamespace = AnyURI(ns),
        elements = listOf(
            XSElement(name= NCName("root")),
            XSElement(
                name = NCName("elementWithAttr"),
                localType = XSLocalComplexTypeShorthand(
                    attributes = listOf(
                        XSLocalAttribute(
                            name = NCName("good"),
                            type = QName(XS_NAMESPACE, "string", "xsd")
                        )
                    ),
                    attributeGroups = listOf(
                        XSAttributeGroupRef(ref = QName(ns, "aGr", "tn"))
                    ),
                )
            )
        ),
        attributeGroups = listOf(ag)
    )

}