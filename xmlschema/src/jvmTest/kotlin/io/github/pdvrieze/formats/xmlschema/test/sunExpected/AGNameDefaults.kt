/*
 * Copyright (c) 2023-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xmlschema.test.sunExpected

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import io.github.pdvrieze.xml.schematypes.values.toAnyUri
import nl.adaptivity.xmlutil.XMLConstants

object AGNameDefaults {
    val ns = "AttrGroup/name"

    val ag = XSAttributeGroup(
        name = XsdNCName("aGr"),
        attributes = listOf(
            XSLocalAttribute(
                name = XsdNCName("number"),
                use = XSAttrUse.REQUIRED,
                type = XsdQName(XMLConstants.XSD_NS_URI, "integer", "xsd")
            ),
            XSLocalAttribute(
                name = XsdNCName("height"),
                type = XsdQName(XMLConstants.XSD_NS_URI, "decimal", "xsd")
            )
        )
    )
    val expectedSchema = XSSchema(
        targetNamespace = ns.toAnyUri(),
        elements = listOf(
            XSGlobalElement(name = XsdNCName("root")),
            XSGlobalElement(
                name = XsdNCName("elementWithAttr"),
                localType = XSLocalComplexTypeShorthand(
                    attributes = listOf(
                        XSLocalAttribute(
                            name = XsdNCName("good"),
                            type = XsdQName(XMLConstants.XSD_NS_URI, "string", "xsd")
                        )
                    ),
                    attributeGroups = listOf(
                        XSAttributeGroupRef(ref = XsdQName(ns, "aGr", "tn"))
                    ),
                )
            )
        ),
        attributeGroups = listOf(ag)
    )

}
