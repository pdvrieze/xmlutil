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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

class XSLocalComplexTypeSimple(
    mixed: Boolean?,
    defaultAttributesApply: Boolean?,
    override val simpleContent: XSSimpleContent,
    id: VID? = null,
    annotation: XSAnnotation? = null,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSLocalComplexType(
    mixed,
    defaultAttributesApply,
    id,
    annotation,
    otherAttrs
), XSComplexType.Simple {

    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            mixed = mixed,
            defaultAttributesApply = defaultAttributesApply,
            simpleContent = simpleContent,
            id = id,
            annotation = annotation,
            otherAttrs = otherAttrs
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as XSLocalComplexTypeSimple

        if (simpleContent != other.simpleContent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + simpleContent.hashCode()
        return result
    }


}
