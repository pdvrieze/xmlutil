/*
 * Copyright (c) 2023-2025.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("redefine", XSD_NS_URI, XSD_PREFIX)
class XSRedefine : XSOpenAttrsBase {
    val simpleTypes: List<XSGlobalSimpleType>
    val complexTypes: List<XSGlobalComplexType>
    val groups: List<XSGroup>
    val attributeGroups: List<XSAttributeGroup>
    val schemaLocation: VAnyURI
    @XmlId
    final val id: VID?

    @XmlBefore("*")
    val annotations: List<XSAnnotation>

    constructor(
        simpleTypes: List<XSGlobalSimpleType> = emptyList(),
        complexTypes: List<XSGlobalComplexType> = emptyList(),
        groups: List<XSGroup> = emptyList(),
        attributeGroups: List<XSAttributeGroup> = emptyList(),
        schemaLocation: VAnyURI,
        id: VID? = null,
        annotation: XSAnnotation?,
        otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) : super(otherAttrs) {
        this.simpleTypes = simpleTypes
        this.complexTypes = complexTypes
        this.groups = groups
        this.attributeGroups = attributeGroups
        this.schemaLocation = schemaLocation
        this.id = id
        this.annotations = listOfNotNull(annotation)
    }

    constructor(
        simpleTypes: List<XSGlobalSimpleType> = emptyList(),
        complexTypes: List<XSGlobalComplexType> = emptyList(),
        groups: List<XSGroup> = emptyList(),
        attributeGroups: List<XSAttributeGroup> = emptyList(),
        schemaLocation: VAnyURI,
        id: VID? = null,
        annotations: List<XSAnnotation> = emptyList(),
        otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) : super(otherAttrs) {
        this.simpleTypes = simpleTypes
        this.complexTypes = complexTypes
        this.groups = groups
        this.attributeGroups = attributeGroups
        this.schemaLocation = schemaLocation
        this.id = id
        this.annotations = annotations
    }
}
