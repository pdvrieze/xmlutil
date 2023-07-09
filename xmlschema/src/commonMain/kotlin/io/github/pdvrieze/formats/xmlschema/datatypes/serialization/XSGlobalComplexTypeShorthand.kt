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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationControl
import io.github.pdvrieze.formats.xmlschema.types.T_TopLevelComplexType_Shorthand
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer

class XSGlobalComplexTypeShorthand(
    name: VNCName,
    mixed: Boolean?,
    abstract: Boolean?,
    final: Set<T_DerivationControl.ComplexBase>?,
    block: Set<T_DerivationControl.ComplexBase>?,
    defaultAttributesApply: Boolean?,
    override val term: XSComplexContent.XSIDerivationParticle? = null,
    override val asserts: List<XSAssert>,
    override val attributes: List<XSLocalAttribute>,
    override val attributeGroups: List<XSAttributeGroupRef>,
    override val anyAttribute: XSAnyAttribute?,
    override val openContent: XSOpenContent?,
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
), XSComplexType.Shorthand, T_TopLevelComplexType_Shorthand {
    override val content: XSGlobalComplexTypeShorthand get() = this
    override val derivation: XSGlobalComplexTypeShorthand get() = this

    override val base: Nothing? get() = null

    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract = abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            term = term,
            asserts = asserts,
            atributes = attributes,
            atributeGroups = attributeGroups,
            anyAttribute = anyAttribute,
            openContent = openContent,
            id = id,
            annotation = annotation,
            otherAttrs = otherAttrs
        )
    }

}
