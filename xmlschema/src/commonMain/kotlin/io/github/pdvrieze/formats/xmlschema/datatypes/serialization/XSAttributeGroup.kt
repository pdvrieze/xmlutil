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

@file:UseSerializers(QNameSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("attributeGroup", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAttributeGroup(
    val name: VNCName,
    @XmlId
    override val id: VID? = null,
    val attributes: List<XSLocalAttribute> = emptyList(),
    val attributeGroups: List<XSAttributeGroupRef> = emptyList(),
    val anyAttribute: XSAnyAttribute? = null,
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap()
) : XSI_Annotated {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSAttributeGroup

        if (name != other.name) return false
        if (id != other.id) return false
        if (attributes != other.attributes) return false
        if (attributeGroups != other.attributeGroups) return false
        if (anyAttribute != other.anyAttribute) return false
        if (annotation != other.annotation) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + attributes.hashCode()
        result = 31 * result + attributeGroups.hashCode()
        result = 31 * result + (anyAttribute?.hashCode() ?: 0)
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
