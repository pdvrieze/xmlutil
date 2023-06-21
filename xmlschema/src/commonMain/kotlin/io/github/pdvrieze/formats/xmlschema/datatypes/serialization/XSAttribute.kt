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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_AttributeBase
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("attribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
abstract class XSAttribute(
    final override val default: String? = null,
    final override val fixed: String? = null,
    final override val id: VID? = null,
    final override val type: SerializableQName? = null,
    final override val inheritable: Boolean? = null,
    override val annotation: XSAnnotation? = null,
    final override val simpleType: XSLocalSimpleType? = null,
    @XmlOtherAttributes
    final override val otherAttrs: Map<SerializableQName, String> = emptyMap(),
) : T_AttributeBase {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSAttribute

        if (default != other.default) return false
        if (fixed != other.fixed) return false
        if (id != other.id) return false
        if (type != other.type) return false
        if (inheritable != other.inheritable) return false
        if (annotation != other.annotation) return false
        if (simpleType != other.simpleType) return false
        if (otherAttrs != other.otherAttrs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = default?.hashCode() ?: 0
        result = 31 * result + (fixed?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (inheritable?.hashCode() ?: 0)
        result = 31 * result + (annotation?.hashCode() ?: 0)
        result = 31 * result + (simpleType?.hashCode() ?: 0)
        result = 31 * result + otherAttrs.hashCode()
        return result
    }
}
