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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VBoolean
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

class XSGlobalComplexTypeComplex(
    name: VNCName,
    mixed: Boolean?,
    abstract: Boolean?,
    final: Set<VDerivationControl.Complex>?,
    block: Set<VDerivationControl.Complex>?,
    defaultAttributesApply: Boolean?,
    override val content: XSComplexContent,
    id: VID? = null,
    annotation: XSAnnotation? = null,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSGlobalComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotation,
    otherAttrs
), XSComplexType.Complex {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed?.let(::VBoolean),
            abstract = abstract?.let(::VBoolean),
            final = final,
            block = block,
            complexContent = content,
            id = id,
            defaultAttributesApply = defaultAttributesApply,
            annotation = annotation,
            otherAttrs = otherAttrs,
            term = null
        )
    }
}
