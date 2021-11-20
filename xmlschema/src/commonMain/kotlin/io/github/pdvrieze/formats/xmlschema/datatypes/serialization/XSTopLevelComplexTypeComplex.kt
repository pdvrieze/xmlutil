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

import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_DerivationSet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TopLevelComplexType_Complex
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

class XSTopLevelComplexTypeComplex(
    name: NCName,
    mixed: Boolean?,
    abstract: Boolean,
    final: T_DerivationSet,
    block: T_DerivationSet,
    defaultAttributesApply: Boolean?,
    override val content: XSComplexContent,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Complex {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract = abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            complexContent = content,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }
}
