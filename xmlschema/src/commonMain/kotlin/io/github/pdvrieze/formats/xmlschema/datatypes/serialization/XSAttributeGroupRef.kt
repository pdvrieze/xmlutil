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
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("attributeGroup", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
@Serializable
class XSAttributeGroupRef : XSAnnotatedBase {

    val ref: QName

    constructor(
        ref: QName,
        id: VID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(id, annotation, otherAttrs) {
        this.ref = ref
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSAttributeGroupRef

        if (id != other.id) return false
        if (ref != other.ref) return false
        if (annotation != other.annotation) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + ref.hashCode()
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
