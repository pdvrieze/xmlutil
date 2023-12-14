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

class XSGlobalComplexTypeShorthand(
    name: VNCName,
    mixed: Boolean?,
    abstract: Boolean?,
    final: Set<VDerivationControl.Complex>?,
    block: Set<VDerivationControl.Complex>?,
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
), XSComplexType.Shorthand {
    override val content: XSGlobalComplexTypeShorthand get() = this
    override val derivation: XSGlobalComplexTypeShorthand get() = this

    override val base: Nothing? get() = null

    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed?.let(::VBoolean),
            abstract = abstract?.let(::VBoolean),
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            term = term,
            asserts = asserts,
            attributes = attributes,
            attributeGroups = attributeGroups,
            anyAttribute = anyAttribute,
            openContent = openContent,
            id = id,
            annotation = annotation,
            otherAttrs = otherAttrs
        )
    }

    override fun toString(): String = buildString {
        append("XSGlobalComplexTypeShorthand(")
        append(name)
        if(term!=null) { append(", term=$term, ") }
        if(asserts.isNotEmpty()) { append(", asserts=$asserts") }
        if(attributes.isNotEmpty()) { append(", attributes=$attributes") }
        if(attributeGroups.isNotEmpty()) { append(", attributeGroups=$attributeGroups") }
        if(anyAttribute!=null) { append(", anyAttribute=$anyAttribute") }
        if(openContent!=null) { append(", openContent=$openContent") }
        append(")")
    }

}
