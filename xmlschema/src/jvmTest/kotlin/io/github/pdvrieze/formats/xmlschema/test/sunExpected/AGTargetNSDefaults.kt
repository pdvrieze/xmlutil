/*
 * Copyright (c) 2023.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants

object AGTargetNSDefaults {
    val ns = "AttrGroup/targetNS"

    val ag = XSAttributeGroup(
        name = VNCName("aGr"),
        attributes = listOf(
            XSLocalAttribute(
                name = VNCName("number"),
                use = XSAttrUse.REQUIRED,
                type = QName(XMLConstants.XSD_NS_URI, "integer", "xsd")
            ),
            XSLocalAttribute(
                name = VNCName("height"),
                type = QName(XMLConstants.XSD_NS_URI, "decimal", "xsd")
            )
        )
    )
    val expectedSchema = XSSchema(
        targetNamespace = ns.toAnyUri(),
        elements = listOf(
            XSGlobalElement(name = VNCName("root")),

            XSGlobalElement(
                name = VNCName("elementWithAttr"),
                localType = XSLocalComplexTypeShorthand(
                    attributes = listOf(
                        XSLocalAttribute(
                            name = VNCName("good"),
                            type = QName(XMLConstants.XSD_NS_URI, "string", "xsd")
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
