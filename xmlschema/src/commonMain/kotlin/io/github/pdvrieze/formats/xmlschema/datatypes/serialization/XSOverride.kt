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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("override", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSOverride : XSAnnotatedBase {
    val schemaLocation: VAnyURI
    val simpleTypes: List<XSGlobalSimpleType>
    val complexTypes: List<XSGlobalComplexType>
    val groups: List<XSGroup>
    val attributeGroups: List<XSAttributeGroup>
    val elements: List<XSGlobalElement>
    val attributes: List<XSGlobalAttribute>
    val notations: List<XSNotation>

    constructor(
        schemaLocation: VAnyURI,
        simpleTypes: List<XSGlobalSimpleType> = emptyList(),
        complexTypes: List<XSGlobalComplexType> = emptyList(),
        groups: List<XSGroup> = emptyList(),
        attributeGroups: List<XSAttributeGroup> = emptyList(),
        elements: List<XSGlobalElement> = emptyList(),
        attributes: List<XSGlobalAttribute> = emptyList(),
        notations: List<XSNotation> = emptyList(),
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) : super(id, annotation, otherAttrs) {
        this.schemaLocation = schemaLocation
        this.simpleTypes = simpleTypes
        this.complexTypes = complexTypes
        this.groups = groups
        this.attributeGroups = attributeGroups
        this.elements = elements
        this.attributes = attributes
        this.notations = notations
    }
}
